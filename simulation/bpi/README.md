# BPI Phase 1 Simulator

This deterministic HTTP simulator exercises the BPI interaction and API state machine before real
JetLinks, Kafka, Flink and PostgreSQL integration is available.

It creates only in-memory `shadow=true` batch facts and deterministic topology/rule results. It never
calls WOM, QCS, WMS, PLC/DCS or an external database, and a passing simulation is not production
persistence acceptance.

Run the automated acceptance flow:

```bash
make bpi-api-contract-check
make bpi-simulation-test
```

The minimum transfer-cell pilot is defined once in
`simulation/bpi/scenarios/minimum-transfer-cell-v1.json`. It contains the reviewed topology,
five JetLinks point bindings, START/END rules, continuous sample phases and the expected boundary
samples. The same file can generate the IoT pilot mapping:

```bash
node simulation/bpi/minimum-line-scenario.js validate \
  simulation/bpi/scenarios/minimum-transfer-cell-v1.json
node simulation/bpi/minimum-line-scenario.js mapping \
  simulation/bpi/scenarios/minimum-transfer-cell-v1.json
```

The scenario always requires an active WOM production context and keeps WOM/QCS/WMS/PLC-DCS
writeback disabled. Passing it proves only the controlled shadow-batch model; target acceptance
still requires MQTT, JetLinks, Kafka/Flink, the BPI browser and PostgreSQL evidence.

The fructose-line pilot is defined in
`simulation/bpi/scenarios/fructose-jet-saccharification-v1.json`. It models the `喷射 -> 糖化`
handover with twelve process signals sampled every four seconds (15 samples per minute). Its four
cases cover a normal handover, an eight-second flow dropout, a Baumé excursion that must not split a
process, and a production-order plus route switch that must close the old process immediately.

```bash
node simulation/bpi/fructose-line-scenario.js validate \
  simulation/bpi/scenarios/fructose-jet-saccharification-v1.json
node simulation/bpi/fructose-line-scenario.js envelopes \
  simulation/bpi/scenarios/fructose-jet-saccharification-v1.json NORMAL_HANDOVER
```

Run the service manually:

```bash
BPI_SIM_PORT=19090 node simulation/bpi/server.js
curl http://127.0.0.1:19090/health
```
