package com.mapletct.ftmes.bpi.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import kafka.server.KafkaConfig;
import kafka.server.KafkaRaftServer;
import kafka.tools.StorageTool;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.configuration.RestartStrategyOptions;
import org.apache.flink.core.execution.CheckpointType;
import org.apache.flink.runtime.checkpoint.CheckpointStatsSnapshot;
import org.apache.flink.runtime.execution.ExecutionState;
import org.apache.flink.runtime.executiongraph.AccessExecutionGraph;
import org.apache.flink.runtime.minicluster.MiniCluster;
import org.apache.flink.runtime.minicluster.MiniClusterConfiguration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.common.utils.AppInfoParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "BPI_FLINK_RULE_ACCEPTANCE_ENABLED", matches = "(?i)true")
class BpiRuleApplicationFlinkKafkaAcceptanceTest {

    private static final Duration WAIT = Duration.ofSeconds(45);
    private static final Duration NO_RECORD_WINDOW = Duration.ofSeconds(1);

    @TempDir
    Path tempDir;

    @Test
    @Timeout(value = 4, unit = TimeUnit.MINUTES)
    void checkpointCommitControlsVisibilityAndTaskManagerRestartRestoresRuleState()
            throws Exception {
        try (BrokerHandle broker = BrokerHandle.start(tempDir.resolve("kafka"))) {
            String bootstrapServers = broker.bootstrapServers();
            String marker = "ADP_E2E_FLINK_RULE_"
                    + UUID.randomUUID().toString().replace("-", "");
            Topics topics = Topics.create(marker);
            BpiKafkaJobConfig jobConfig = jobConfig(bootstrapServers, marker, topics);
            Configuration flink = flinkConfiguration(tempDir.resolve("checkpoints"));
            MiniCluster cluster = new MiniCluster(new MiniClusterConfiguration.Builder()
                    .setConfiguration(flink)
                    .setNumTaskManagers(1)
                    .setNumSlotsPerTaskManager(8)
                    .withRandomPorts()
                    .build());

            JobID currentJob = null;
            try (AdminClient admin = admin(bootstrapServers);
                 KafkaProducer<byte[], byte[]> producer = producer(bootstrapServers);
                 KafkaConsumer<byte[], byte[]> uncommitted = consumer(
                         bootstrapServers, topics.application(), "read_uncommitted");
                 KafkaConsumer<byte[], byte[]> committed = consumer(
                         bootstrapServers, topics.application(), "read_committed")) {
                createTopics(admin, topics.all());
                cluster.start();

                BpiKafkaAcceptanceScenario.Scenario scenario = BpiKafkaAcceptanceScenario.create(
                        marker, Instant.parse("2026-07-14T01:00:00Z"));
                BoundaryRulePublicationV1 active = scenario.publication();

                JobID interruptedJob = submit(
                        cluster, flink, jobConfig, marker + "-interrupted");
                currentJob = interruptedJob;
                awaitStatus(cluster, interruptedJob, JobStatus.RUNNING);
                awaitAllTasksRunning(cluster, interruptedJob);
                publish(producer, topics.rules(), scenario.ruleKey(), active);

                BoundaryRuleApplicationV1 abortedApplication = awaitApplication(
                        uncommitted,
                        active.getEventId(),
                        BoundaryRuleApplicationStatusV1.APPLIED,
                        Set.of());
                assertEquals(0, checkpointStats(cluster, interruptedJob)
                        .getCounts().getNumberOfCompletedCheckpoints());
                assertNoMarkerApplication(committed, marker, NO_RECORD_WINDOW);

                cluster.cancelJob(interruptedJob).get(WAIT.toSeconds(), TimeUnit.SECONDS);
                awaitStatus(cluster, interruptedJob, JobStatus.CANCELED);
                currentJob = null;
                assertNoMarkerApplication(committed, marker, NO_RECORD_WINDOW);

                JobID recoveredJob = submit(
                        cluster, flink, jobConfig, marker + "-recovered");
                currentJob = recoveredJob;
                awaitStatus(cluster, recoveredJob, JobStatus.RUNNING);
                awaitAllTasksRunning(cluster, recoveredJob);

                BoundaryRuleApplicationV1 replayedApplication = awaitApplication(
                        uncommitted,
                        active.getEventId(),
                        BoundaryRuleApplicationStatusV1.APPLIED,
                        Set.of(abortedApplication.getEventId()));
                assertNotEquals(
                        abortedApplication.getEventId(), replayedApplication.getEventId());
                long activeCheckpoint = triggerCheckpoint(cluster, recoveredJob);
                BoundaryRuleApplicationV1 committedActive = awaitApplication(
                        committed,
                        active.getEventId(),
                        BoundaryRuleApplicationStatusV1.APPLIED,
                        Set.of());

                BoundaryRulePublicationV1 inactive = scenario.inactivePublication(
                        Instant.parse("2026-07-14T01:00:01Z"));
                publish(producer, topics.rules(), scenario.ruleKey(), inactive);
                awaitApplication(
                        uncommitted,
                        inactive.getEventId(),
                        BoundaryRuleApplicationStatusV1.APPLIED,
                        Set.of());
                long inactiveCheckpoint = triggerCheckpoint(cluster, recoveredJob);
                BoundaryRuleApplicationV1 committedInactive = awaitApplication(
                        committed,
                        inactive.getEventId(),
                        BoundaryRuleApplicationStatusV1.APPLIED,
                        Set.of());

                cluster.terminateTaskManager(0).get(WAIT.toSeconds(), TimeUnit.SECONDS);
                cluster.startTaskManager();
                awaitStatus(cluster, recoveredJob, JobStatus.RUNNING);
                awaitAllTasksRunning(cluster, recoveredJob);
                long restoredCheckpoint = awaitRestoredCheckpoint(cluster, recoveredJob);

                BoundaryRulePublicationV1 reactivated = inactive.toBuilder()
                        .setEventId(marker + "-RULE-REACTIVATED")
                        .setActive(true)
                        .setPublishedAtMs(Instant.parse("2026-07-14T01:00:02Z").toEpochMilli())
                        .build();
                publish(producer, topics.rules(), scenario.ruleKey(), reactivated);
                awaitApplication(
                        uncommitted,
                        reactivated.getEventId(),
                        BoundaryRuleApplicationStatusV1.REJECTED,
                        Set.of());
                long rejectionCheckpoint = triggerCheckpoint(cluster, recoveredJob);
                BoundaryRuleApplicationV1 committedRejection = awaitApplication(
                        committed,
                        reactivated.getEventId(),
                        BoundaryRuleApplicationStatusV1.REJECTED,
                        Set.of());

                assertEquals(
                        "RULE_REACTIVATION_REQUIRES_NEW_VERSION",
                        committedRejection.getErrorCode());
                assertTrue(restoredCheckpoint >= inactiveCheckpoint);
                CheckpointStatsSnapshot finalStats = checkpointStats(cluster, recoveredJob);
                assertTrue(finalStats.getCounts().getNumberOfCompletedCheckpoints() >= 3);
                assertTrue(finalStats.getCounts().getNumberOfRestoredCheckpoints() >= 1);
                assertNoMarkerApplication(committed, marker, NO_RECORD_WINDOW);

                writeEvidence(
                        broker.mode(),
                        marker,
                        recoveredJob,
                        abortedApplication,
                        List.of(committedActive, committedInactive, committedRejection),
                        activeCheckpoint,
                        inactiveCheckpoint,
                        rejectionCheckpoint,
                        restoredCheckpoint,
                        finalStats);
            } finally {
                cancelIfRunning(cluster, currentJob);
                if (cluster.isRunning()) {
                    cluster.closeAsync().get(WAIT.toSeconds(), TimeUnit.SECONDS);
                }
                try (AdminClient cleanup = admin(bootstrapServers)) {
                    cleanup.deleteTopics(topics.all()).all().get(WAIT.toSeconds(), TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // Unique acceptance topics disappear with the test broker if cleanup races shutdown.
                }
            }
        }
    }

    private static Configuration flinkConfiguration(Path checkpointDirectory) throws Exception {
        Files.createDirectories(checkpointDirectory);
        Configuration configuration = new Configuration();
        configuration.set(RestOptions.PORT, 0);
        configuration.set(
                CheckpointingOptions.CHECKPOINTS_DIRECTORY,
                checkpointDirectory.toUri().toString());
        configuration.set(CheckpointingOptions.MAX_RETAINED_CHECKPOINTS, 3);
        configuration.set(RestartStrategyOptions.RESTART_STRATEGY, "fixed-delay");
        configuration.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS, 5);
        configuration.set(
                RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_DELAY,
                Duration.ofMillis(250));
        return configuration;
    }

    private static BpiKafkaJobConfig jobConfig(
            String bootstrapServers,
            String marker,
            Topics topics) {
        return BpiKafkaJobConfig.from(Map.ofEntries(
                Map.entry("bootstrap-servers", bootstrapServers),
                Map.entry("group-prefix", "bpi-flink-acceptance-" + marker),
                Map.entry("deployment-id", "flink-acceptance-" + marker),
                Map.entry("telemetry-topic", topics.telemetry()),
                Map.entry("point-catalog-topic", topics.pointCatalog()),
                Map.entry("context-topic", topics.context()),
                Map.entry("rule-topic", topics.rules()),
                Map.entry("rule-application-topic", topics.application()),
                Map.entry("rule-runtime-readiness-topic", topics.runtimeReadiness()),
                Map.entry("candidate-topic", topics.candidate()),
                Map.entry("data-quality-topic", topics.quality()),
                Map.entry("checkpoint-interval-ms", "600000"),
                Map.entry("checkpoint-timeout-ms", "660000"),
                Map.entry("checkpoint-min-pause-ms", "0"),
                Map.entry("transaction-timeout-ms", "840000"),
                Map.entry("source-idleness-ms", "1000"),
                Map.entry("parallelism", "1")));
    }

    private static JobID submit(
            MiniCluster cluster,
            Configuration flink,
            BpiKafkaJobConfig config,
            String jobName) throws Exception {
        StreamExecutionEnvironment environment =
                StreamExecutionEnvironment.getExecutionEnvironment(flink);
        BpiKafkaJob.build(environment, config);
        StreamGraph graph = environment.getStreamGraph();
        graph.setJobName(jobName);
        return cluster.submitJob(graph)
                .get(WAIT.toSeconds(), TimeUnit.SECONDS)
                .getJobID();
    }

    private static long triggerCheckpoint(MiniCluster cluster, JobID jobId) throws Exception {
        return cluster.triggerCheckpoint(jobId, CheckpointType.CONFIGURED)
                .get(WAIT.toSeconds(), TimeUnit.SECONDS);
    }

    private static void awaitStatus(MiniCluster cluster, JobID jobId, JobStatus expected)
            throws Exception {
        Instant deadline = Instant.now().plus(WAIT);
        JobStatus last = null;
        while (Instant.now().isBefore(deadline)) {
            last = cluster.getJobStatus(jobId).get(5, TimeUnit.SECONDS);
            if (last == expected) return;
            if (last.isGloballyTerminalState() && last != expected) {
                fail("Flink job reached " + last + " while waiting for " + expected
                        + ": " + failure(cluster, jobId));
            }
            Thread.sleep(100);
        }
        fail("Flink job stayed at " + last + " while waiting for " + expected);
    }

    private static void awaitAllTasksRunning(MiniCluster cluster, JobID jobId)
            throws Exception {
        Instant deadline = Instant.now().plus(WAIT);
        List<String> pending = List.of();
        while (Instant.now().isBefore(deadline)) {
            AccessExecutionGraph graph = cluster.getExecutionGraph(jobId)
                    .get(5, TimeUnit.SECONDS);
            if (graph.getState().isGloballyTerminalState()) {
                fail("Flink job reached " + graph.getState()
                        + " while waiting for all tasks: " + failure(cluster, jobId));
            }
            List<String> current = new ArrayList<>();
            graph.getAllExecutionVertices().forEach(vertex -> {
                if (vertex.getExecutionState() != ExecutionState.RUNNING) {
                    current.add(vertex.getTaskNameWithSubtaskIndex()
                            + "=" + vertex.getExecutionState());
                }
            });
            pending = current;
            if (pending.isEmpty()) return;
            Thread.sleep(100);
        }
        fail("Flink tasks did not all reach RUNNING: " + pending);
    }

    private static long awaitRestoredCheckpoint(MiniCluster cluster, JobID jobId)
            throws Exception {
        Instant deadline = Instant.now().plus(WAIT);
        while (Instant.now().isBefore(deadline)) {
            JobStatus status = cluster.getJobStatus(jobId).get(5, TimeUnit.SECONDS);
            if (status.isGloballyTerminalState()) {
                fail("Flink job terminated during restore: " + status + ": "
                        + failure(cluster, jobId));
            }
            CheckpointStatsSnapshot snapshot = checkpointStats(cluster, jobId);
            if (snapshot.getLatestRestoredCheckpoint() != null) {
                return snapshot.getLatestRestoredCheckpoint().getCheckpointId();
            }
            Thread.sleep(100);
        }
        fail("Flink job did not report a restored checkpoint after TaskManager restart");
        return -1;
    }

    private static CheckpointStatsSnapshot checkpointStats(MiniCluster cluster, JobID jobId)
            throws Exception {
        return cluster.getExecutionGraph(jobId)
                .get(5, TimeUnit.SECONDS)
                .getCheckpointStatsSnapshot();
    }

    private static String failure(MiniCluster cluster, JobID jobId) {
        try {
            AccessExecutionGraph graph = cluster.getExecutionGraph(jobId)
                    .get(5, TimeUnit.SECONDS);
            return graph.getFailureInfo() == null
                    ? "no failure info"
                    : graph.getFailureInfo().getExceptionAsString();
        } catch (Exception error) {
            return error.toString();
        }
    }

    private static void cancelIfRunning(MiniCluster cluster, JobID jobId) {
        if (jobId == null || !cluster.isRunning()) return;
        try {
            JobStatus status = cluster.getJobStatus(jobId).get(5, TimeUnit.SECONDS);
            if (!status.isGloballyTerminalState()) {
                cluster.cancelJob(jobId).get(WAIT.toSeconds(), TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
            // The MiniCluster close below is the final test-owned cleanup boundary.
        }
    }

    private static AdminClient admin(String bootstrapServers) {
        return AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000",
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "30000"));
    }

    private static void createTopics(AdminClient admin, List<String> topics) throws Exception {
        List<NewTopic> definitions = topics.stream()
                .map(topic -> new NewTopic(topic, 1, (short) 1))
                .toList();
        admin.createTopics(definitions).all().get(WAIT.toSeconds(), TimeUnit.SECONDS);
    }

    private static KafkaProducer<byte[], byte[]> producer(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        return new KafkaProducer<>(properties);
    }

    private static KafkaConsumer<byte[], byte[]> consumer(
            String bootstrapServers,
            String topic,
            String isolationLevel) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                "bpi-flink-rule-application-" + isolationLevel + "-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    private static void publish(
            KafkaProducer<byte[], byte[]> producer,
            String topic,
            String key,
            BoundaryRulePublicationV1 publication) throws Exception {
        producer.send(new ProducerRecord<>(
                        topic,
                        key.getBytes(StandardCharsets.UTF_8),
                        publication.toByteArray()))
                .get(10, TimeUnit.SECONDS);
        producer.flush();
    }

    private static BoundaryRuleApplicationV1 awaitApplication(
            KafkaConsumer<byte[], byte[]> consumer,
            String publicationEventId,
            BoundaryRuleApplicationStatusV1 status,
            Set<String> excludedEventIds) throws Exception {
        Instant deadline = Instant.now().plus(WAIT);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(250));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                BoundaryRuleApplicationV1 application =
                        BoundaryRuleApplicationV1.parseFrom(record.value());
                if (publicationEventId.equals(application.getPublicationEventId())
                        && status == application.getStatus()
                        && !excludedEventIds.contains(application.getEventId())) {
                    return application;
                }
            }
        }
        fail("No " + status + " application arrived for " + publicationEventId);
        return BoundaryRuleApplicationV1.getDefaultInstance();
    }

    private static void assertNoMarkerApplication(
            KafkaConsumer<byte[], byte[]> consumer,
            String marker,
            Duration window) throws Exception {
        Instant deadline = Instant.now().plus(window);
        List<String> unexpected = new ArrayList<>();
        while (Instant.now().isBefore(deadline)) {
            for (ConsumerRecord<byte[], byte[]> record : consumer.poll(Duration.ofMillis(100))) {
                BoundaryRuleApplicationV1 application =
                        BoundaryRuleApplicationV1.parseFrom(record.value());
                if (application.getPublicationEventId().startsWith(marker)) {
                    unexpected.add(application.getStatus().name()
                            + ":" + application.getPublicationEventId());
                }
            }
        }
        assertTrue(unexpected.isEmpty(), "unexpected committed applications: " + unexpected);
    }

    private static void writeEvidence(
            String brokerMode,
            String marker,
            JobID jobId,
            BoundaryRuleApplicationV1 aborted,
            List<BoundaryRuleApplicationV1> committed,
            long activeCheckpoint,
            long inactiveCheckpoint,
            long rejectionCheckpoint,
            long restoredCheckpoint,
            CheckpointStatsSnapshot stats) throws Exception {
        String output = System.getenv("BPI_FLINK_ACCEPTANCE_REPORT");
        if (output == null || output.isBlank()) return;
        Path report = Path.of(output).toAbsolutePath();
        Path parent = report.getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            Files.createDirectories(parent);
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("generatedAt", Instant.now().toString());
        evidence.put("status", "PASS_LOCAL_FLINK_MINICLUSTER_KAFKA");
        evidence.put(
                "scope",
                "BPI rule application checkpoint transaction visibility, task-manager restart recovery and lifecycle-state acceptance");
        evidence.put("brokerMode", brokerMode);
        evidence.put("kafkaClientVersion", AppInfoParser.getVersion());
        evidence.put("runtime", Map.of(
                "flink", "2.2.1 MiniCluster",
                "kafka", AppInfoParser.getVersion(),
                "consumerIsolationLevel", "read_committed",
                "checkpointStorage", "test-owned local filesystem"));
        evidence.put(
                "test",
                "BpiRuleApplicationFlinkKafkaAcceptanceTest.checkpointCommitControlsVisibilityAndTaskManagerRestartRestoresRuleState");
        evidence.put("marker", marker);
        evidence.put("jobId", jobId.toHexString());
        evidence.put("preCheckpoint", Map.of(
                "readUncommittedEventId", aborted.getEventId(),
                "readCommittedVisible", false,
                "interruptedTransactionCommitted", false));
        evidence.put("checkpoints", Map.of(
                "active", activeCheckpoint,
                "inactive", inactiveCheckpoint,
                "rejection", rejectionCheckpoint,
                "restored", restoredCheckpoint,
                "completedCount", stats.getCounts().getNumberOfCompletedCheckpoints(),
                "restoredCount", stats.getCounts().getNumberOfRestoredCheckpoints()));
        evidence.put("committedApplications", committed.stream().map(application -> Map.of(
                "eventId", application.getEventId(),
                "publicationEventId", application.getPublicationEventId(),
                "status", application.getStatus().name(),
                "errorCode", application.getErrorCode())).toList());
        evidence.put("assertions", List.of(
                "An interrupted pre-checkpoint Kafka transaction is invisible to read_committed.",
                "APPLIED is visible only after a successful Flink checkpoint.",
                "TaskManager restart restores the terminal inactive lifecycle from checkpoint.",
                "Reactivation of the restored inactive version is committed as REJECTED.",
                "No duplicate committed rule application is observed."));
        evidence.put("limitations", List.of(
                "The default broker is a disposable single-process Kafka KRaft server, not the target three-broker cluster.",
                "Checkpoint storage is a test-owned local filesystem, not the target MinIO/S3 storage.",
                "This acceptance does not include the BPI PostgreSQL consumer or a browser-to-Java round trip.",
                "This acceptance does not claim target-environment deployment, long-running shadow production or production readiness."));
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(report.toFile(), evidence);
    }

    private record BrokerHandle(
            String bootstrapServers,
            String mode,
            KafkaRaftServer embeddedBroker) implements AutoCloseable {

        static BrokerHandle start(Path kafkaDirectory) throws Exception {
            String external = System.getenv("BPI_TEST_KAFKA_BOOTSTRAP_SERVERS");
            if (external != null && !external.isBlank()) {
                return new BrokerHandle(external.trim(), "EXTERNAL", null);
            }
            Files.createDirectories(kafkaDirectory);
            int brokerPort = availablePort();
            int controllerPort = availablePort();
            Path logDirectory = kafkaDirectory.resolve("data");
            Path configFile = kafkaDirectory.resolve("server.properties");
            Properties properties = new Properties();
            properties.put("process.roles", "broker,controller");
            properties.put("node.id", "1");
            properties.put("controller.quorum.voters", "1@127.0.0.1:" + controllerPort);
            properties.put("listeners", "PLAINTEXT://127.0.0.1:" + brokerPort
                    + ",CONTROLLER://127.0.0.1:" + controllerPort);
            properties.put("advertised.listeners", "PLAINTEXT://127.0.0.1:" + brokerPort);
            properties.put(
                    "listener.security.protocol.map",
                    "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT");
            properties.put("controller.listener.names", "CONTROLLER");
            properties.put("inter.broker.listener.name", "PLAINTEXT");
            properties.put("log.dirs", logDirectory.toString());
            properties.put("offsets.topic.replication.factor", "1");
            properties.put("transaction.state.log.replication.factor", "1");
            properties.put("transaction.state.log.min.isr", "1");
            properties.put("transaction.state.log.num.partitions", "1");
            properties.put("group.initial.rebalance.delay.ms", "0");
            try (var output = Files.newOutputStream(configFile)) {
                properties.store(output, "BPI Flink acceptance Kafka 4.2 KRaft broker");
            }

            String clusterId = Uuid.randomUuid().toString();
            int formatExit = StorageTool.execute(new String[] {
                    "format",
                    "--ignore-formatted",
                    "--cluster-id", clusterId,
                    "--config", configFile.toString()
            }, System.err);
            if (formatExit != 0) {
                throw new IllegalStateException(
                        "Kafka storage format failed with exit code " + formatExit);
            }

            KafkaRaftServer embedded = new KafkaRaftServer(
                    KafkaConfig.fromProps(properties), Time.SYSTEM);
            embedded.startup();
            return new BrokerHandle(
                    "127.0.0.1:" + brokerPort,
                    "EMBEDDED_KRAFT_4_2_0",
                    embedded);
        }

        @Override
        public void close() {
            if (embeddedBroker == null) return;
            embeddedBroker.shutdown();
            embeddedBroker.awaitShutdown();
        }

        private static int availablePort() throws IOException {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            }
        }
    }

    private record Topics(
            String telemetry,
            String pointCatalog,
            String context,
            String rules,
            String application,
            String runtimeReadiness,
            String candidate,
            String quality) {

        static Topics create(String marker) {
            String suffix = marker.substring(marker.length() - 12).toLowerCase();
            String prefix = "bpi.acceptance." + suffix;
            return new Topics(
                    prefix + ".telemetry",
                    prefix + ".point-catalog",
                    prefix + ".context",
                    prefix + ".rules",
                    prefix + ".rule-application",
                    prefix + ".rule-runtime-readiness",
                    prefix + ".candidate",
                    prefix + ".quality");
        }

        List<String> all() {
            return List.of(
                    telemetry, pointCatalog, context, rules, application, runtimeReadiness, candidate, quality);
        }
    }
}
