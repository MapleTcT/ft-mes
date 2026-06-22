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
scripts/prepare-qcs-static-assets.sh
scripts/prepare-eam-static-assets.sh
scripts/prepare-runtime-patches.sh /home/v6/adp-mes-docker/runtime/bap-server
docker compose --env-file .env up -d postgres redis mongo zookeeper kafka nacos keycloak minio
scripts/init-keycloak-realm.sh
scripts/sync-keycloak-jwt-public-key.sh
docker compose --env-file .env up -d
docker compose --env-file .env restart nginx
docker compose --env-file .env ps
```

从仓库根目录也可以使用统一入口：

```bash
make compose-config
make render-config
make up-infra
make up
make ps
```

Open:

```text
http://100.99.133.43:18080/
```

Default test login:

```text
admin / 123456
```

## Smoke Verification

After the stack starts, run the API and frontend menu smoke checks from the repository root:

```bash
NODE_PATH=/Users/zhangchu/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules \
  /Users/zhangchu/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node \
  deploy/docker/scripts/adp-platform-api-smoke.js

ADP_OUTPUT_DIR=/tmp/adp-menu-smoke-current \
NODE_PATH=/Users/zhangchu/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules \
  /Users/zhangchu/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node \
  deploy/docker/scripts/adp-menu-smoke.js

ADP_OUTPUT_DIR=/tmp/adp-home-todo-smoke-current \
NODE_PATH=/Users/zhangchu/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules \
  /Users/zhangchu/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node \
  deploy/docker/scripts/adp-home-todo-smoke.js
```

Optional environment variables:

```text
ADP_BASE_URL=http://100.99.133.43:18080
ADP_USERNAME=admin
ADP_PASSWORD=123456
ADP_HEADLESS=false
ADP_SCREENSHOTS=failures|all|none
ADP_API_SMOKE_OUTPUT=/tmp/adp-platform-api-smoke.json
```

`adp-platform-api-smoke.js` checks the login path plus the platform endpoints most likely to regress during PostgreSQL and standalone-Docker migration. `adp-menu-smoke.js` reads the current user's menu tree from the server, opens every navigable frontend menu, and fails on document/XHR/fetch 4xx/5xx responses, browser console errors, page errors, and visible system-error text. `adp-home-todo-smoke.js` logs in through the real frontend and clicks the top workbench Todo tab, covering shell interactions that are not represented by menu URLs.

`scripts/init-keycloak-realm.sh` creates the ADP business Keycloak realm (`dt` by default), `pc_dt`/`mobile_dt` clients, the `supos` token scope and the bundled `readonly-property-file` user storage provider. It is idempotent and can be rerun after Keycloak volume resets. `scripts/sync-keycloak-jwt-public-key.sh` reads that realm public key into `nacos-rendered/supfusion-jwt-common.properties` and publishes the Nacos config so Java services can populate `UserContext` from Keycloak access tokens.

The Docker test profile sets `SUPPLANT_LICENSE_ENABLED=true`. In this recovered gateway, that prevents the software-dongle `LicenseFilter` and license refresh task from registering, so `/msService/ec/**` and `/msService/servicemanager/**` do not depend on Redis or hardware license data. `test-license-seed` is kept only as a manual fallback if you deliberately re-enable the legacy license filter with `SUPPLANT_LICENSE_ENABLED=false`.

`scripts/patch-operatetools-standalone-app-list.py` keeps the Manage App page usable when the optional `installer` service is not present in the base-platform Docker profile. The original method already falls back to the local `supos_app` table; the patch prevents the missing optional service from emitting repeated ERROR stack traces during page smoke tests.

## Database Profile

The Docker profile is PostgreSQL-first. `.env.example` and the built-in Compose defaults both point to `postgres:5432`.

If a legacy module must temporarily connect to Oracle, copy the relevant values from `.env.oracle-legacy.example` into `.env` and document the reason in the migration notes for that module. Oracle should be explicit, not the silent default.

## PostgreSQL Note

The Docker profile uses PostgreSQL by default, adds an external PostgreSQL JDBC driver to each Java service classpath, and uses `prepare-runtime-patches.sh` to inject PostgreSQL DBP classes, generated `postgresql` mapper directories, recovered-package runtime fixes, and the current EAM static-page compatibility patch. The mapper generation is mechanical and records risky SQL patterns in `runtime/postgres-patch-report.json`; any remaining failures should be fixed from that report and container logs.

For the recovered binaries, run the PostgreSQL compatibility SQL in `postgres/init/004-041*.sql` after the recovered runtime creates legacy auth/RBAC tables. These scripts seed the admin/auth baseline, repair RBAC initialization metadata, convert Boolean-backed permission columns, backfill RBAC operation codes, and add compatibility shims for legacy MyBatis fragments that still compare or aggregate Boolean fields as `0/1`.

Use `scripts/audit-postgres-mappings.py` against both source folders and patched runtime JARs before handoff. A clean migration has `findingCount: 0` for loadable mapper/SQL files.
