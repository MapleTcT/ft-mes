# BPI Runtime Deployment

This Compose project runs the BPI console, Java 8 authentication adapter, Java 17 BPI service, and
its PostgreSQL database without changing the legacy ADP/MES Compose project. The adapter joins the
existing ADP network only to validate Keycloak JWKS; it never joins the old application JVMs or
database. Kafka/Flink/MinIO remain in `deploy/bpi-streaming` and are reached through the configured
private bootstrap addresses.

## Start

```bash
make bpi-service-package
make bpi-adapter-package
make bpi-ui-build
cp deploy/bpi-runtime/.env.example deploy/bpi-runtime/.env
# Replace every change-me value, verify the Keycloak issuer, and set private Kafka addresses.
sh deploy/bpi-runtime/scripts/preflight.sh deploy/bpi-runtime/.env
docker compose --env-file deploy/bpi-runtime/.env \
  -f deploy/bpi-runtime/docker-compose.yml up -d --build
sh deploy/bpi-runtime/scripts/smoke.sh deploy/bpi-runtime/.env
```

The one-shot `bpi-migrate` container uses the same tested application image with the DDL-owning
`bpi_migrator` account and exits after Flyway completes. The long-running service starts afterward
with the DML-only `bpi_service` account.

The browser reaches only the same-origin `/bpi-api` path on `bpi-web`. Nginx proxies that path to
the Java 8 adapter. A three-segment access token is validated by Keycloak JWKS. The legacy ADP login
instead returns an opaque `suposTicket`; for that credential the adapter calls only the trusted
internal gateway's current-user endpoint, where the gateway verifies its Redis session and Keycloak
access token. The adapter then applies server-owned role and tenant/plant/line scope mappings,
creates a short-lived internal JWT, and forwards an explicit Phase 1 API allowlist to the Java 17
service. Direct browser access to the internal JWT secret or BPI PostgreSQL is not provided.

The service and web ports bind to `127.0.0.1` by default. A test server may bind the web port to its
Tailscale address. Keep the Java service port private; do not publish the actuator, adapter, internal
API, or PostgreSQL on a public interface.

`BPI_ADAPTER_KEYCLOAK_ISSUER` must equal the `iss` claim in the real ADP access token, not merely a
URL that happens to reach Keycloak. `BPI_ADAPTER_ROLE_RULES` and
`BPI_ADAPTER_SUBJECT_SCOPE_RULES` are mandatory and fail closed when a role or user is not explicitly
mapped. The legacy gateway URL must be an internal service address; never point ticket verification
at a caller-controlled host.

Candidate, rule-publication, and rule-application Kafka consumers are disabled by default. Enable
them only after setting explicit tenant, plant, and line allowlists. `_DENY_ALL_` is the fail-closed
default; `*` should be used only for a documented test marker scope.

## Controlled joint acceptance

The target browser/Kafka/Flink/PostgreSQL chain is reproducible with these marker-scoped assets:

- `sql/joint-acceptance-seed.sql` creates only the topology, rule, golden boundary, history points,
  and line flags required by one acceptance run. It refuses to overwrite existing line flags.
- `scripts/browser-joint-acceptance.js` runs the real `publish`, `confirm`, and post-cleanup `read`
  browser phases. Credentials are supplied only through the process environment and are never
  written to its report.
- `sql/joint-acceptance-verify.sql` reads rule, outbox/application, candidate, batch, evidence,
  state-event, and audit state from PostgreSQL.
- `sql/joint-acceptance-cleanup.sql` removes only rows linked to the exact marker in one transaction.

Before SQL cleanup, publish the typed inactive rule with
`make bpi-stream-rule-deactivate` and require a Flink `APPLIED` receipt. After cleanup, force-recreate
the service from the base `.env` so all three consumers return to disabled and `_DENY_ALL_`, then run
the browser `read` phase. These fixtures must not be used as a production topology/rule authoring
path. See `docs/testing/bpi-browser-kafka-postgres-joint-acceptance.md` for the accepted sequence.

The `bpi-postgres-data` named volume is intentionally retained by `docker compose down`. Removing
that volume, dropping `ft_mes_bpi`, or rolling back Flyway is destructive and requires an explicit
backup and approval.
