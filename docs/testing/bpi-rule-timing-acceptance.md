# BPI rule timing acceptance

## Implemented boundary

- `BoundaryTimingPolicy` makes `allowedLateness`, `watermarkDelay` and `evaluationTimeout` immutable rule-version data.
- Rule wire codec writes version 2 and still reads version 1 using explicit legacy defaults.
- Negative durations, zero timeout and lateness beyond timeout are rejected before publication.
- The Flink operator classifies watermark-behind events as `LATE_EVENT_REPLAY_REQUIRED` inside the allowed-lateness window and `LATE_EVENT_REVISION_REQUIRED` after it.
- Neither path silently mutates an already emitted candidate.

## Current limit

This milestone establishes versioned timing semantics and deterministic routing. It does not yet implement the bounded, event-time-ordered observation buffer needed to recompute an open window. Until that buffer is delivered, `LATE_EVENT_REPLAY_REQUIRED` must be routed to replay processing and cannot be treated as an accepted live observation.

## Verification

```bash
JAVA_HOME=<jdk17> mvn -f streaming/pom.xml -pl bpi-stream-engine -am test
```

The suite covers v2 round trip, v1 compatibility, timing validation, within-window classification, beyond-window classification, event-time timers and checkpoint restore.
