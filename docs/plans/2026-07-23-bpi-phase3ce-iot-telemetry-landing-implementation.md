# BPI Phase 3C-E IoT Telemetry Landing Implementation Plan

**Goal:** Persist explicitly scoped `MapleTcT/iot` telemetry from Kafka and expose PostgreSQL-backed field-pilot coverage on the existing Shadow Run workbench.

**Architecture:** Add a default-off BPI service Kafka consumer with a deny-all default scope and DLQ. Reuse the existing telemetry transaction and immutable PostgreSQL tables. Extend the Shadow Run read model instead of creating another campaign lifecycle.

**Tech Stack:** Java 17, Spring Boot, Spring Kafka, Protobuf, PostgreSQL 15, OpenAPI, TypeScript/Vite, Node simulation, Playwright, JetLinks/MQTT.

**Status:** Implementation and controlled target acceptance are complete at product commit
`8c9c4192b17953c48208efd31ef6528de04d96c6` and acceptance-harness commit
`988868f539cfd9ed5b0127edb621e799a509bad0`. Physical field qualification remains a later gate.

## Task 1: Kafka ingress contract and persistence

Status: `COMPLETED`

Files:

- `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/telemetry/*`
- `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/TelemetryIngestionService.java`
- `services/bpi-service/app/src/main/resources/application.yml`
- `deploy/docker/docker-compose.yml`
- `deploy/bpi-streaming/scripts/create-topics.sh`

Acceptance:

1. Valid Protobuf is persisted transactionally.
2. Exact replay is idempotent.
3. Scope, topic, key, payload, contract and poison records fail closed.
4. Offset is acknowledged only after persistence.
5. Consumer and HTTP ingress remain disabled by default.

## Task 2: PostgreSQL telemetry coverage projection

Status: `COMPLETED`

Files:

- `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/ShadowRunTelemetryCoverage.java`
- `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/ShadowRunView.java`
- `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/ShadowRunPostgresRepository.java`
- `services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiShadowRunPostgresAcceptanceTest.java`

Acceptance:

1. DRAFT run reports an unstarted window.
2. Matching telemetry increments observed, authoritative, calibrated and GOOD counts.
3. Wrong line/device/property/calibration and pre-start data do not count.
4. gap/out-of-order events remain visible and block approval.
5. No duplicate lifecycle or editable coverage table is added.

## Task 3: API, simulation and operator UI

Status: `COMPLETED`

Files:

- `contracts/bpi-api/openapi.json`
- `frontend/apps/bpi/src/types.ts`
- `frontend/apps/bpi/src/main.ts`
- `frontend/apps/bpi/src/styles.css`
- `simulation/bpi/server.js`
- related API, simulator and browser tests

Acceptance:

1. OpenAPI, Java, simulation and TypeScript use the same additive response.
2. Desktop and 390px workbenches show PostgreSQL evidence and blockers.
3. No page control can mutate coverage or start training.
4. Browser console, page and network errors remain zero.

## Task 4: Target MQTT/PostgreSQL acceptance

Status: `COMPLETED_CONTROLLED_SOURCE`

Files:

- target acceptance scripts under `deploy/docker/scripts/`
- `docs/testing/bpi-iot-telemetry-landing-acceptance.md`
- `metadata/bpi-iot-telemetry-landing-acceptance.json`

Acceptance:

1. Controlled MQTT travels through JetLinks, Kafka and BPI service.
2. PostgreSQL event/point rows, consumer offsets, API and UI agree.
3. Replay, DLQ, restart and exact cleanup are verified.
4. Evidence states that controlled MQTT is not physical field qualification.

## Task 5: Governance and landing

Status: `IN_PROGRESS`

1. Update project objective, gap, persistence and frontend ledgers.
2. Run focused Java/PostgreSQL/simulation/browser checks and full `make ci`.
3. Review the diff, commit, push, merge `main`, and verify GitHub Verify.
