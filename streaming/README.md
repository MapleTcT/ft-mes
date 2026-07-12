# BPI Streaming

This Java 17 reactor contains the BPI event-time processing path. It reuses the
versioned Protobuf contracts from `contracts/bpi-events` and the deterministic
boundary formulas from `services/bpi-service/batch-rule-runtime`.

The current milestone provides both a deterministic single-context replay
engine and a Flink `KeyedBroadcastProcessFunction`. The Flink path stores
versioned `byte[]` keyed/broadcast state, uses event-time timers, emits
`BatchCandidateV1` wire bytes, and routes rejected inputs to a side output. It
also provides a keyed, checkpointed event-time join for point telemetry and
versioned production context. It is not yet wired to the production Kafka
source and sink.

Run the module with:

```sh
make bpi-stream-test
```

Java 8 remains the baseline for the legacy MES reactor. Only this reactor and
the standalone BPI service require Java 17.
