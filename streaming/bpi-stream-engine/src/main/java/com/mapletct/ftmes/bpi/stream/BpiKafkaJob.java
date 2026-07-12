package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.core.execution.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Instant;

public final class BpiKafkaJob {

    public static final String JOB_NAME = "ft-mes-bpi-batch-boundary-v1";

    private BpiKafkaJob() {
    }

    public static void main(String[] args) throws Exception {
        BpiKafkaJobConfig config = BpiKafkaJobConfig.fromArgs(args);
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        build(environment, config);
        environment.execute(JOB_NAME);
    }

    public static void build(
            StreamExecutionEnvironment environment,
            BpiKafkaJobConfig config) {
        configure(environment, config);

        SingleOutputStreamOperator<byte[]> decodedTelemetry = environment
                .fromSource(
                        BpiKafkaIO.source(config, config.telemetryTopic(), "telemetry"),
                        WatermarkStrategy.noWatermarks(),
                        "Kafka telemetry source")
                .uid("bpi-kafka-telemetry-source-v1")
                .process(new TelemetryKafkaDecodeFunction())
                .name("Decode and validate telemetry")
                .uid("bpi-telemetry-decode-v1");
        SingleOutputStreamOperator<byte[]> telemetry = decodedTelemetry
                .assignTimestampsAndWatermarks(telemetryWatermarks(config))
                .name("Telemetry event-time watermarks")
                .uid("bpi-telemetry-watermarks-v1");

        SingleOutputStreamOperator<byte[]> decodedContexts = environment
                .fromSource(
                        BpiKafkaIO.source(config, config.contextTopic(), "context"),
                        WatermarkStrategy.noWatermarks(),
                        "Kafka production-context source")
                .uid("bpi-kafka-context-source-v1")
                .process(new ProductionContextKafkaDecodeFunction())
                .name("Decode and validate production context")
                .uid("bpi-context-decode-v1");
        SingleOutputStreamOperator<byte[]> contexts = decodedContexts
                .assignTimestampsAndWatermarks(contextWatermarks(config))
                .name("Production-context event-time watermarks")
                .uid("bpi-context-watermarks-v1");

        SingleOutputStreamOperator<byte[]> decodedRules = environment
                .fromSource(
                        BpiKafkaIO.source(config, config.ruleTopic(), "rules"),
                        WatermarkStrategy.noWatermarks(),
                        "Kafka rule-publication source")
                .uid("bpi-kafka-rules-source-v1")
                .process(new BoundaryRuleKafkaDecodeFunction())
                .name("Decode and validate rule publications")
                .uid("bpi-rule-decode-v1");

        SingleOutputStreamOperator<byte[]> timestampedRules = decodedRules
                .assignTimestampsAndWatermarks(ruleWatermarks(config))
                .name("Rule-publication watermarks and idleness")
                .uid("bpi-rule-watermarks-v1");
        SingleOutputStreamOperator<byte[]> rules = timestampedRules
                .keyBy(BpiKafkaJob::ruleScopeKey)
                .process(new BoundaryRulePublicationLifecycleFunction(config.boundaryStateTtl()))
                .name("Enforce immutable rule-version lifecycle")
                .uid("bpi-rule-lifecycle-v1");

        SingleOutputStreamOperator<byte[]> joinedContextual = telemetry
                .keyBy(BpiKafkaJob::telemetryScopeKey)
                .connect(contexts.keyBy(BpiKafkaJob::contextScopeKey))
                .process(new ProductionContextJoinFunction(config.contextWait(), config.contextRetention()))
                .name("Join telemetry to event-time production context")
                .uid("bpi-production-context-join-v1");
        SingleOutputStreamOperator<byte[]> contextual = joinedContextual
                .assignTimestampsAndWatermarks(contextualWatermarks(config))
                .name("Reassign contextual point event time")
                .uid("bpi-contextual-watermarks-v1");

        BroadcastStream<byte[]> routeRules = rules.broadcast(
                BoundaryRuleRoutingBroadcastFunction.PUBLICATIONS,
                BoundaryRuleRoutingBroadcastFunction.ROUTES);
        SingleOutputStreamOperator<BoundaryStreamInput> routedInputs = contextual
                .connect(routeRules)
                .process(new BoundaryRuleRoutingBroadcastFunction())
                .name("Route points through indexed published bindings")
                .uid("bpi-boundary-indexed-routing-v1");
        SingleOutputStreamOperator<BoundaryStreamInput> routed = routedInputs
                .assignTimestampsAndWatermarks(boundaryInputWatermarks(config))
                .name("Boundary-input event-time watermarks")
                .uid("bpi-boundary-input-watermarks-v1");

        SingleOutputStreamOperator<byte[]> ruleUpdates = rules
                .map(new BoundaryRuleUpdateMapper())
                .returns(byte[].class)
                .name("Map scoped boundary rule updates")
                .uid("bpi-boundary-rule-update-v1");
        BroadcastStream<byte[]> evaluatorRules = ruleUpdates.broadcast(
                BoundaryKeyedBroadcastFunction.RULES);

        SingleOutputStreamOperator<byte[]> candidates = routed
                .keyBy(BoundaryStreamInput::keyedLocality)
                .connect(evaluatorRules)
                .process(new BoundaryKeyedBroadcastFunction(config.boundaryStateTtl()))
                .name("Evaluate boundary windows")
                .uid("bpi-boundary-evaluator-v1");

        candidates
                .sinkTo(BpiKafkaIO.candidateSink(config))
                .name("Kafka exactly-once batch-candidate sink")
                .uid("bpi-kafka-candidate-sink-v1");

        DataStream<byte[]> decodeQuality = decodedTelemetry
                .getSideOutput(TelemetryKafkaDecodeFunction.ISSUES)
                .union(
                        decodedContexts.getSideOutput(ProductionContextKafkaDecodeFunction.ISSUES),
                        decodedRules.getSideOutput(BoundaryRuleKafkaDecodeFunction.ISSUES))
                .map(new KafkaIssueMap())
                .returns(byte[].class);
        DataStream<byte[]> joinQuality = joinedContextual
                .getSideOutput(ProductionContextJoinFunction.ISSUES)
                .map(new ContextIssueMap())
                .returns(byte[].class);
        DataStream<byte[]> routingQuality = routedInputs
                .getSideOutput(BoundaryRuleRoutingBroadcastFunction.ISSUES)
                .union(rules.getSideOutput(BoundaryRulePublicationLifecycleFunction.ISSUES))
                .map(new RoutingIssueMap())
                .returns(byte[].class);
        DataStream<byte[]> evaluationQuality = candidates
                .getSideOutput(BoundaryKeyedBroadcastFunction.ISSUES)
                .map(new ProcessingIssueMap())
                .returns(byte[].class);

        decodeQuality
                .union(joinQuality, routingQuality, evaluationQuality)
                .sinkTo(BpiKafkaIO.dataQualitySink(config))
                .name("Kafka exactly-once data-quality sink")
                .uid("bpi-kafka-data-quality-sink-v1");
    }

    private static void configure(
            StreamExecutionEnvironment environment,
            BpiKafkaJobConfig config) {
        environment.setRuntimeMode(RuntimeExecutionMode.STREAMING);
        environment.setParallelism(config.parallelism());
        environment.enableCheckpointing(
                config.checkpointInterval().toMillis(),
                CheckpointingMode.EXACTLY_ONCE);
        CheckpointConfig checkpoints = environment.getCheckpointConfig();
        checkpoints.setCheckpointTimeout(config.checkpointTimeout().toMillis());
        checkpoints.setMinPauseBetweenCheckpoints(config.checkpointMinPause().toMillis());
        checkpoints.setMaxConcurrentCheckpoints(1);
        checkpoints.setExternalizedCheckpointRetention(
                ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION);
    }

    private static WatermarkStrategy<byte[]> telemetryWatermarks(BpiKafkaJobConfig config) {
        return WatermarkStrategy
                .<byte[]>forBoundedOutOfOrderness(config.watermarkDelay())
                .withTimestampAssigner((bytes, previous) ->
                        TelemetryPointEventCodec.decode(bytes).eventTime().toEpochMilli())
                .withIdleness(config.sourceIdleness());
    }

    private static WatermarkStrategy<byte[]> contextWatermarks(BpiKafkaJobConfig config) {
        return WatermarkStrategy
                .<byte[]>forBoundedOutOfOrderness(config.watermarkDelay())
                .withTimestampAssigner((bytes, previous) ->
                        ProductionContextWire.decode(bytes).getEffectiveFromMs())
                .withIdleness(config.sourceIdleness());
    }

    private static WatermarkStrategy<byte[]> ruleWatermarks(BpiKafkaJobConfig config) {
        return WatermarkStrategy
                .<byte[]>forBoundedOutOfOrderness(config.watermarkDelay())
                .withTimestampAssigner((bytes, previous) -> {
                    try {
                        return com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1
                                .parseFrom(bytes).getPublishedAtMs();
                    } catch (com.google.protobuf.InvalidProtocolBufferException error) {
                        throw new IllegalStateException(
                                "validated rule publication cannot be decoded", error);
                    }
                })
                .withIdleness(config.sourceIdleness());
    }

    private static WatermarkStrategy<byte[]> contextualWatermarks(BpiKafkaJobConfig config) {
        return WatermarkStrategy
                .<byte[]>forBoundedOutOfOrderness(config.watermarkDelay())
                .withTimestampAssigner((bytes, previous) -> ContextualTelemetryPointCodec
                        .decode(bytes).telemetry().eventTime().toEpochMilli())
                .withIdleness(config.sourceIdleness());
    }

    private static WatermarkStrategy<BoundaryStreamInput> boundaryInputWatermarks(
            BpiKafkaJobConfig config) {
        return WatermarkStrategy
                .<BoundaryStreamInput>forBoundedOutOfOrderness(config.watermarkDelay())
                .withTimestampAssigner((input, previous) -> input.observation().eventTime().toEpochMilli())
                .withIdleness(config.sourceIdleness());
    }

    private static String telemetryScopeKey(byte[] bytes) {
        return TelemetryPointEventCodec.decode(bytes).scopeKey();
    }

    private static String contextScopeKey(byte[] bytes) {
        ProductionContextEventV1 event = ProductionContextWire.decode(bytes);
        return TelemetryPointEvent.contextScopeKey(event);
    }

    private static String ruleScopeKey(byte[] bytes) {
        try {
            return BoundaryRulePublicationSemantics.key(
                    com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1.parseFrom(bytes));
        } catch (com.google.protobuf.InvalidProtocolBufferException error) {
            throw new IllegalStateException("validated rule publication cannot be decoded", error);
        }
    }

    private static long now() {
        return Instant.now().toEpochMilli();
    }

    private static final class KafkaIssueMap implements MapFunction<KafkaDecodeIssue, byte[]> {
        @Override
        public byte[] map(KafkaDecodeIssue value) {
            return DataQualityProjector.project(value, now());
        }
    }

    private static final class ContextIssueMap implements MapFunction<ContextJoinIssue, byte[]> {
        @Override
        public byte[] map(ContextJoinIssue value) {
            return DataQualityProjector.project(value, now());
        }
    }

    private static final class RoutingIssueMap implements MapFunction<BoundaryRoutingIssue, byte[]> {
        @Override
        public byte[] map(BoundaryRoutingIssue value) {
            return DataQualityProjector.project(value, now());
        }
    }

    private static final class ProcessingIssueMap implements MapFunction<BoundaryProcessingIssue, byte[]> {
        @Override
        public byte[] map(BoundaryProcessingIssue value) {
            return DataQualityProjector.project(value, now());
        }
    }
}
