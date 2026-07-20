# Material/WMS Deployment Runbook

## Scope

Deploy the PostgreSQL-first `material-wms` service without changing existing WOM,
QCS, or Oracle legacy templates. The service registers as Nacos `prod@@material`.

## Build And Verify

```bash
make material-wms-test
make material-wms-package
docker compose -f deploy/docker/docker-compose.yml config --quiet
```

## Deploy

1. Back up the current Compose file and `material-wms.jar` with a timestamp.
2. Apply `deploy/docker/postgres/init/174-material-wms-completion-inbound.sql`,
   `deploy/docker/postgres/init/191-wom-quality-quantity-reporting.sql`, and
   `deploy/docker/postgres/init/192-material-wms-bpi-idempotency.sql` to the target
   PostgreSQL database in order. All are additive/idempotent; migration 192 adds the
   BPI `source_system`/`idempotency_key` contract and exact uniqueness indexes.
3. Stage the built JAR as
   `runtime/bap-server/module-Server/material/manual/material-wms.jar`.
4. Render or verify Nacos metadata: group `prod`, service `material`,
   `supfusion.tenantid=default`, `k8s.service.name=material`, `secure=false`.
5. Start only the material service:

```bash
docker compose -f deploy/docker/docker-compose.yml up -d material
```

6. Confirm Spring startup, one healthy Nacos instance, and all three gateway
   compatibility probes.
7. For BPI integration, set a non-default `MATERIAL_WMS_BPI_API_KEY` in the secret
   store and pass the same value only to `bpi-wms-adapter`. Do not place it in Git,
   logs, screenshots, or browser configuration. Keep these defaults until a bounded
   acceptance or approved cutover window:

```text
BPI_PHASE2_INTEGRATION_ENABLED=false
BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED=false
BPI_PHASE2_KAFKA_ENABLED=false
BPI_WMS_OUTBOX_ENABLED=false
BPI_WMS_ADAPTER_ENABLED=false
BPI_PHASE2_ALLOWED_TENANT_IDS=_DENY_ALL_
BPI_PHASE2_ALLOWED_PLANT_IDS=_DENY_ALL_
BPI_PHASE2_ALLOWED_LINE_IDS=_DENY_ALL_
BPI_WMS_ADAPTER_ROUTES=_DENY_ALL_
```

8. The adapter must query
   `GET /material/wms/completion-inbounds/by-idempotency` before create, create only
   on an exact not-found result, and query the same key again before publishing an
   accepted receipt. Do not blindly retry a POST after timeout.
9. Run the marker acceptance:

```bash
make acceptance-material-wms-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080
```

## Acceptance Gates

- Completion inbound creates held stock.
- Duplicate source detail is idempotent.
- Qualified result moves held stock to available.
- Production issue rejects negative stock and decrements available stock.
- `/msService/material/wms` renders without browser/network errors.
- Marker cleanup returns all five WMS tables to zero marker rows.
- A BPI marker preserves `sourceSystem=BPI`, the exact command idempotency key, and
  `quantityUnit` through document, line, inventory transaction, and stock.
- Identical QCS replay and forced Kafka command replay do not create another
  document, line, transaction, or stock increment.
- BPI enters `INBOUNDED` only after an accepted receipt carries a durable document
  id that can be queried back by the exact idempotency key.
- After acceptance, all Phase 2/outbox/adapter switches are false and all allowlists
  are deny-all.

The controlled target acceptance on 2026-07-20 is recorded in
`metadata/bpi-quality-release-wms-target-acceptance.json`. It proves the internal
`material-wms` path, not an external ERP/WMS reversal or outage drill.

## Rollback

1. Stop only the `material` Compose service.
2. Disable BPI ingress/Kafka/outbox/adapter and restore all integration scopes to
   deny-all before changing images.
3. Restore the timestamped Compose file and prior JAR, if one existed. Restore the
   recorded BPI service/adapter image tags as one compatible set when integration
   components were part of the deployment.
4. Start the previous service definition and verify Nacos/gateway health.
5. Keep the additive PostgreSQL tables and migration-192 columns/indexes in place
   unless a separately approved
   data-retention change authorizes removal. Do not drop them during an incident
   rollback.

Rollback must not change the default datasource to Oracle and must not delete
non-marker business rows.
