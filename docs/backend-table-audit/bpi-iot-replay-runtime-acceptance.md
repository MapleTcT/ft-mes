# BPI IoT replay runtime acceptance

## Scope

This acceptance closes one executable Phase 1 path without mocks:

`IoT-style signals -> boundary replay engine -> BatchCandidateV1 Protobuf -> authenticated HTTP ingress -> PostgreSQL candidate -> confirmation -> shadow batch`

The test reactor is isolated under `acceptance/bpi-runtime`. It combines the stream engine and BPI service only for acceptance testing; Flink is not added to the service runtime dependencies.

## Command

Use Java 17 and a disposable PostgreSQL database:

```bash
BPI_TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:55433/bpi_replay \
BPI_TEST_DATABASE_USER=bpi_replay \
BPI_TEST_DATABASE_PASSWORD=bpi_replay \
mvn -f acceptance/bpi-runtime/pom.xml test
```

The test is skipped when `BPI_TEST_DATABASE_URL` is absent, while the full reactor still compiles in `make ci`.

## Persisted evidence

| Action | API / entry | PostgreSQL evidence | Expected |
|---|---|---|---|
| Replay order, pump and flow signals | `BoundaryReplayEngine.replay` | N/A | One deterministic START candidate |
| Submit Protobuf candidate | `POST /internal/bpi/v1/candidate-events` | `bpi_inbox_events`, `bpi_batch_candidates` | One inbox and one pending candidate |
| Confirm candidate | `POST /bpi/v1/candidates/{id}/confirm` | `bpi_batch_instances`, `bpi_boundary_evidence` | One shadow batch and three evidence rows |

All rows use a unique `ADP_E2E_IOT_REPLAY_*` tenant marker and are removed in test cleanup. The acceptance never writes WOM, QCS or WMS tables.
