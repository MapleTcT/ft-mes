# BPI rule publication and signal routing acceptance

## Result

The BPI event contract now includes `BoundaryRulePublicationV1`, immutable rule timing and
condition data, topology-local signal bindings, and the optional typed `batch_id` on
`ProductionContextEventV1`. The AsyncAPI catalog exposes
`bpi.boundary.rule-publication.v1` as the compacted control-plane topic consumed by Flink
broadcast state.

The deterministic Java 17 mapping layer performs the following before a point can enter the
boundary evaluator:

- validates tenant, plant, line, locality, topology, rule version, timing and evidence enums;
- requires every configured evidence signal to have exactly one device/property binding;
- rejects duplicate device/property bindings and bindings to unknown signals;
- enforces exact published unit codes and isolates only the affected point;
- maps only numeric and boolean values with supported quality codes;
- resolves START from a typed active order and END from a typed active batch identity;
- resolves production context by signal event time and monotonic line-scope revision, so a future
  order revision cannot leak into older telemetry;
- treats identical context replay as idempotent and conflicting event/revision identity as an error.

## Automated evidence

`BoundarySignalRouterTest` covers six publication/routing cases and
`ProductionContextTimelineTest` covers four point-in-time context cases. Together they are
**10/10 PASS** for this milestone under the Java 17 streaming reactor. The current aggregate is
recorded in `docs/testing/bpi-kafka-flink-topology-acceptance.md`.

```bash
JAVA_HOME=<jdk17> mvn -f streaming/pom.xml -pl bpi-stream-engine -am test
python3 scripts/verify-bpi-api-contracts.py
```

## Limits

- A later milestone now wires KafkaSource/KafkaSink, checkpoint transactions, the keyed context join
  and bounded telemetry wait; real cluster acceptance remains open.
- The publication checksum is required but is not yet recomputed against a canonical rule payload.
- Schema Registry publication and compatibility checks have not run against a live registry.
- This is deterministic simulation evidence, not a real broker, Flink cluster or production line acceptance.
