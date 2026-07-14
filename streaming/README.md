# BPI Streaming

This Java 17 reactor contains the BPI event-time processing path. It reuses the
versioned Protobuf contracts from `contracts/bpi-events` and the deterministic
boundary formulas from `services/bpi-service/batch-rule-runtime`.

The current milestone provides a deterministic replay engine and the production
`BpiKafkaJob` topology. The Flink path stores
versioned `byte[]` keyed/broadcast state, uses event-time timers, emits
`BatchCandidateV1` wire bytes, and routes rejected inputs to a side output. It
also provides a keyed, checkpointed event-time join for point telemetry and
versioned production context. Kafka sources, rule lifecycle/broadcast routing,
candidate output and data-quality output are wired with checkpoint-aware sinks.
The rule-application sink also has local Kafka 4.2 + Flink 2.2.1 MiniCluster
checkpoint and TaskManager-restart acceptance. This is real local runtime
execution, but it is not the target three-broker/Flink/MinIO cluster acceptance.

Run the module with:

```sh
make bpi-stream-test
```

Run the local checkpoint/transaction/restart acceptance with Java 17:

```sh
JAVA_HOME=/path/to/jdk17 make bpi-rule-application-flink-acceptance
```

The command starts a disposable in-process Kafka 4.2 KRaft server by default
and writes `/tmp/bpi-rule-application-flink-kafka-acceptance.json`. Set
`BPI_TEST_KAFKA_BOOTSTRAP_SERVERS` only when a dedicated external test broker
should be used. The test does not require PostgreSQL and does not claim a
browser-to-database round trip.

Build the deployable Java 17 job artifact with:

```sh
JAVA_HOME=/path/to/jdk17 mvn -f streaming/pom.xml -pl bpi-stream-engine -am package
```

The attached artifact is
`streaming/bpi-stream-engine/target/bpi-stream-engine-0.1.0-SNAPSHOT-job.jar` and
declares `com.mapletct.ftmes.bpi.stream.BpiKafkaJob` as its main class. Flink
runtime libraries remain `provided`; the job artifact includes the Kafka
connector, BPI contracts and deterministic rule runtime. Runtime configuration
is documented in `docs/testing/bpi-kafka-flink-topology-acceptance.md`.

Java 8 remains the baseline for the legacy MES reactor. Only this reactor and
the standalone BPI service require Java 17.
