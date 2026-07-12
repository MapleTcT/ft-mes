# BPI Streaming

This Java 17 reactor contains the BPI event-time processing path. It reuses the
versioned Protobuf contracts from `contracts/bpi-events` and the deterministic
boundary formulas from `services/bpi-service/batch-rule-runtime`.

The current milestone provides a deterministic single-context replay engine
that projects START and END decisions into contract-valid `BatchCandidateV1`
messages. It is the executable semantic baseline for the upcoming Flink
`KeyedBroadcastProcessFunction`; it is not yet the production Kafka data plane.

Run the module with:

```sh
make bpi-stream-test
```

Java 8 remains the baseline for the legacy MES reactor. Only this reactor and
the standalone BPI service require Java 17.
