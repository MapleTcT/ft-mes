# ADP/MES Docker Test Deployment

This directory runs the recovered Windows ADP/MES package on Linux with Docker Compose.

## Remote Layout

Expected server path:

```text
/home/v6/adp-mes-docker/
  deploy/docker/            # this directory
  runtime/bap-server/       # copied from the original Windows package
  runtime/logs/             # Java service logs
```

## Services

- PostgreSQL, Redis, MongoDB, MinIO and Nginx images from `.env`
- bundled ZooKeeper, Kafka, Nacos 1.2.1 and Keycloak 10.0.1 from `bap-server/assembly`
- Nginx frontend on `${ADP_HTTP_PORT:-18080}`
- ADP gateway on `${ADP_GATEWAY_PORT:-18008}`
- 23 Java services from `bap-server/base-Server`

## Commands

```bash
cd /home/v6/adp-mes-docker/deploy/docker
cp .env.example .env
python3 scripts/render-nacos-configs.py
scripts/prepare-static-placeholders.sh
scripts/prepare-runtime-patches.sh /home/v6/adp-mes-docker/runtime/bap-server
docker compose --env-file .env up -d postgres redis mongo zookeeper kafka nacos keycloak minio
scripts/init-keycloak-realm.sh
scripts/sync-keycloak-jwt-public-key.sh
python3 scripts/patch-postgres-runtime.py \
  --base-server /home/v6/adp-mes-docker/runtime/bap-server/base-Server \
  --report /home/v6/adp-mes-docker/runtime/postgres-patch-report.json
docker compose --env-file .env up -d
docker compose --env-file .env restart nginx
docker compose --env-file .env ps
```

Open:

```text
http://10.11.100.17:18080/
```

Default test login:

```text
admin / 123456
```

`scripts/init-keycloak-realm.sh` creates the ADP business Keycloak realm (`dt` by default), `pc_dt`/`mobile_dt` clients, the `supos` token scope and the bundled `readonly-property-file` user storage provider. It is idempotent and can be rerun after Keycloak volume resets. `scripts/sync-keycloak-jwt-public-key.sh` reads that realm public key into `nacos-rendered/supfusion-jwt-common.properties` and publishes the Nacos config so Java services can populate `UserContext` from Keycloak access tokens.

The Docker test profile sets `SUPPLANT_LICENSE_ENABLED=true`. In this recovered gateway, that prevents the software-dongle `LicenseFilter` and license refresh task from registering, so `/msService/ec/**` and `/msService/servicemanager/**` do not depend on Redis or hardware license data. `test-license-seed` is kept only as a manual fallback if you deliberately re-enable the legacy license filter with `SUPPLANT_LICENSE_ENABLED=false`.

## PostgreSQL Note

The Docker profile uses PostgreSQL by default, adds an external PostgreSQL JDBC driver to each Java service classpath, and provides `patch-postgres-runtime.py` to inject PostgreSQL DBP classes and generated `postgresql` mapper directories into nested runtime JARs. The mapper generation is mechanical and records risky SQL patterns in `runtime/postgres-patch-report.json`; any remaining failures should be fixed from that report and container logs.

For the recovered binaries, run the PostgreSQL compatibility SQL in `postgres/init/004-012*.sql` after the recovered runtime creates legacy auth/RBAC tables. These scripts seed the admin/auth baseline, repair RBAC initialization metadata, convert Boolean-backed permission columns, backfill RBAC operation codes, and add compatibility shims for legacy MyBatis fragments that still compare or aggregate Boolean fields as `0/1`.

Use `scripts/audit-postgres-mappings.py` against both source folders and patched runtime JARs before handoff. A clean migration has `findingCount: 0` for loadable mapper/SQL files.
