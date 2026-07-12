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
This is job-level wiring and Harness acceptance, not a real cluster acceptance.

Run the module with:

```sh
make bpi-stream-test
```

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
