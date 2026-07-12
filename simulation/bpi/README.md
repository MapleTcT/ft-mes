# BPI Phase 1 Simulator

This deterministic HTTP simulator exercises the BPI interaction and API state machine before real
JetLinks, Kafka, Flink and PostgreSQL integration is available.

It creates only in-memory `shadow=true` batch facts. It never calls WOM, QCS, WMS, PLC/DCS or an
external database, and a passing simulation is not production persistence acceptance.

Run the automated acceptance flow:

```bash
make bpi-api-contract-check
make bpi-simulation-test
```

Run the service manually:

```bash
BPI_SIM_PORT=19090 node simulation/bpi/server.js
curl http://127.0.0.1:19090/health
```
