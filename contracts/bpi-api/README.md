# BPI API Contracts

This directory is the public integration boundary for BPI.

- `openapi.json` describes synchronous browser and adapter APIs.
- `asyncapi.json` catalogs Kafka topics and their Protobuf messages.
- `simulation-profile.json` lists the operations implemented by the deterministic Phase 1 simulator.

All state-changing HTTP operations require `Idempotency-Key` and `If-Match`. The simulator is not a
production backend and does not prove JetLinks, Kafka, Flink or PostgreSQL connectivity.

Validation:

```bash
make bpi-api-contract-check
make bpi-simulation-test
```
