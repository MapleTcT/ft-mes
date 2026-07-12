# BPI production context join acceptance

## Result

`ProductionContextJoinFunction` implements the event-time join between flattened telemetry points
and WOM/BPI production context using Flink keyed state. The operator is keyed by
`tenant|plant|line`; telemetry without a resolvable context waits for a bounded event-time period
instead of being dropped or joined to the latest wall-clock order.

The state and data-plane contracts are explicit:

- `BPJS/v1` stores deterministically ordered context revisions and pending telemetry as `byte[]`;
- each key retains at most 10,000 context events and 10,000 pending points;
- `BPCT/v1` carries the resolved telemetry point and context as stable bytes, avoiding Kryo copies
  of generated Protobuf objects;
- point timestamps come from `sample_time_ms`, falling back to envelope `event_time_ms`;
- identical pending telemetry is idempotent; changed content with the same identity is rejected;
- context event identity and line-scope revision conflicts are isolated to the issue side output;
- a pending point expires only after both input watermarks pass its event-time deadline;
- checkpoint restore preserves pending points and allows a later context event to resolve them.

## Automated evidence

At this milestone the Java 17 streaming reactor was **41/41 PASS**. The current aggregate is
recorded in `docs/testing/bpi-kafka-flink-topology-acceptance.md`. Six tests use Flink's official
`KeyedTwoInputStreamOperatorTestHarness`; two additional tests verify deterministic BPJS encoding
and unknown-version rejection.

```bash
JAVA_HOME=<jdk17> mvn -f streaming/pom.xml -pl bpi-stream-engine -am test
```

## Limits

- A late context flush inherits the context input record timestamp from `KeyedCoProcessFunction`.
  The assembled topology must reassign timestamp from the decoded BPCT telemetry event before the
  boundary evaluator; direct wiring without reassignment is forbidden.
- A later milestone now wires KafkaSource/KafkaSink, source idleness and transactional sinks. Target
  cluster checkpoint storage is still a deployment responsibility and has not been accepted live.
- Pending-buffer load and long-running RocksDB behavior have not been benchmarked.
- This is an official Flink Harness acceptance, not a real Kafka/Flink cluster acceptance.
