# BPI API Contracts

This directory is the public integration boundary for BPI.

- `openapi.json` describes synchronous browser and adapter APIs.
- `asyncapi.json` catalogs Kafka topics and their Protobuf messages.
- `simulation-profile.json` lists the operations implemented by the deterministic Phase 1 simulator.
- `service-phase1-profile.json` lists the smaller set implemented by the real Java 17/PostgreSQL service.

All state-changing HTTP operations require `Idempotency-Key` and `If-Match`. The simulator is not a
production backend and does not prove JetLinks, Kafka, Flink or PostgreSQL connectivity. The service profile
proves the shadow-batch vertical slice plus the trusted telemetry fact ingress. It explicitly excludes
WOM/QCS/WMS/PLC/DCS writes and does not yet prove Kafka or Flink runtime connectivity.

Validation:

```bash
make bpi-api-contract-check
make bpi-simulation-test
```
