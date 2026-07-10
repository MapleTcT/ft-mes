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
2. Apply `deploy/docker/postgres/init/174-material-wms-completion-inbound.sql`
   to the target PostgreSQL database. The migration is additive and idempotent.
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
7. Run the marker acceptance:

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

## Rollback

1. Stop only the `material` Compose service.
2. Restore the timestamped Compose file and prior JAR, if one existed.
3. Start the previous service definition and verify Nacos/gateway health.
4. Keep the additive PostgreSQL tables in place unless a separately approved
   data-retention change authorizes removal. Do not drop them during an incident
   rollback.

Rollback must not change the default datasource to Oracle and must not delete
non-marker business rows.
