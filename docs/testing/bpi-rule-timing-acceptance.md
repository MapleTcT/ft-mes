# BPI rule timing acceptance

## Implemented boundary

- `BoundaryTimingPolicy` makes `allowedLateness`, `watermarkDelay` and `evaluationTimeout` immutable rule-version data.
- Rule wire codec writes version 2 and still reads version 1 using explicit legacy defaults.
- Negative durations, zero timeout and lateness beyond timeout are rejected before publication.
- The Flink operator retains only signals referenced by the active rule, ordered by event time, for at most `evaluationTimeout + allowedLateness` and at most 10,000 observations per key.
- Watermark-behind events inside `allowedLateness` deterministically recompute an open window from retained observations.
- Events beyond `allowedLateness`, or any late evidence after candidate emission, produce `LATE_EVENT_REVISION_REQUIRED` and do not mutate candidate state.
- Event identity is idempotent for identical signal observations and produces `EVENT_ID_CONFLICT` for changed content.
- Keyed state codec writes BPIS version 2 with observation history and reads BPIS version 1 as incomplete history; post-upgrade late events fail closed to revision instead of recomputing from missing evidence.

## Acceptance result

This milestone is **PASS** for bounded late-event recomputation inside the Flink operator. Harness tests prove in-window recomputation, beyond-window revision routing, candidate immutability, duplicate replay idempotency, and checkpoint restoration of observation history.

It is not production Kafka acceptance. The Kafka source must still apply each rule version's watermark delay, the revision side output needs a durable sink and consumer, and a real savepoint upgrade from BPIS/v1 to BPIS/v2 must be rehearsed on the target Flink cluster.

## Verification

```bash
JAVA_HOME=<jdk17> mvn -f streaming/pom.xml -pl bpi-stream-engine -am test
```

The suite covers rule v2 round trip, fail-closed state v1 compatibility, state v2 observation round trip, hard limits, timing validation, in-window recomputation, beyond-window revision, candidate immutability, event-time timers and checkpoint restore.
