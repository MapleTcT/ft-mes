# BPI Runtime Deployment

This directory is the isolated BPI runtime template. The current target environment no longer runs a
separate `ft-mes-bpi-runtime` project: BPI Web, the Java 8 authentication adapter, the Java 17 service
and PostgreSQL are composed with the single `adp-mes-newbase` stack from `deploy/docker`. The official
test page is the same-origin `/bpi/` path on ADP port `18080`; the former standalone `:18091` page is
not a current target entry. Kafka/Flink/MinIO remain isolated in `deploy/bpi-streaming`.

Keep this Compose file for isolated development and migration rehearsal. Target runtime changes must
use the integrated `deploy/docker/docker-compose.yml` composition and must not start a second BPI Web,
service, adapter or PostgreSQL beside it. The isolated template now includes a private MinIO only for
Phase 3B dataset artifacts; Kafka, Flink and their checkpoint MinIO remain in `deploy/bpi-streaming`.

## Isolated start

```bash
make bpi-service-package
make bpi-adapter-package
make bpi-ui-build
cp deploy/bpi-runtime/.env.example deploy/bpi-runtime/.env
# Replace every change-me value, use distinct service/migrator/materializer credentials,
# verify the Keycloak issuer, and set private Kafka addresses.
sh deploy/bpi-runtime/scripts/preflight.sh deploy/bpi-runtime/.env
docker compose --env-file deploy/bpi-runtime/.env \
  -f deploy/bpi-runtime/docker-compose.yml up -d --build
sh deploy/bpi-runtime/scripts/smoke.sh deploy/bpi-runtime/.env
```

The one-shot `bpi-migrate` container uses the same tested application image with the DDL-owning
`bpi_migrator` account and exits after Flyway completes. The long-running service starts afterward
with the DML-only `bpi_service` account. Flyway V27 adds the Parquet materialization job ledger.
The separate Python 3.12 worker runs as UID/GID `10001:10001` with the `bpi_materializer` database
role and a bucket-scoped MinIO identity. It cannot insert snapshots or read `source_payload`.

`BPI_EXPECTED_FLYWAY_VERSION` is the runtime smoke contract for the release and defaults to the
latest repository migration (`27`). Set it explicitly in the target `.env` when preparing a release.
Preflight compares every packaged migration name and checksum in the service JAR with the source
migration directory; a stale JAR, a changed historical migration, or an unexpected migration head
fails before any database action.

Both `BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED` and `BPI_DATASET_MATERIALIZER_ENABLED` default to
`false`. An expand-only deployment builds and records the worker image but leaves the worker stopped
and does not create or change the MinIO bucket. Enable bucket bootstrap once in a separately approved
acceptance window, verify the private bucket, versioning and scoped identity, return bootstrap to
`false`, and only then enable the worker for marker-scoped acceptance. Neither switch is an Iceberg,
MLflow, training, inference or production-readiness claim.

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

Candidate, point-catalog, source-sequence evidence, rule-publication, rule-application, and
runtime-readiness Kafka consumers are disabled by default. Enable them only after setting explicit
tenant, plant, and line allowlists. `_DENY_ALL_` is
the fail-closed default; `*` should be used only for a documented test marker scope. The point-catalog
consumer additionally validates Kafka key, required single-value headers, Protobuf schema version,
content-addressed revision, 5 MiB payload limit and source identity before persistence; failed records
use bounded retry and `iot.point-catalog.snapshot.dlq.v1`.

The source-sequence consumer accepts only content-addressed `SourceSequenceEvidenceV1` Protobuf
events from `iot.source-sequence.evidence.v1`. The record key and single-value headers must match
the exact tenant/source instance/plant/line/product/device/binding fingerprint identity. A point
remains blocked until a fresh `QUALIFIED` event matches its current catalog claim; the boolean
catalog declaration alone never grants readiness. Rejected records use bounded retry and
`iot.source-sequence.evidence.dlq.v1`.

The rule-application listener consumes both control-plane `APPLIED/REJECTED` receipts and the
independent evaluator `READY/DEGRADED/INACTIVE` receipts. The two source topics and their DLQs must
remain distinct: an `APPLIED` control-plane receipt never implies runtime `READY`.

## Expand-only runtime upgrade

The runtime upgrade helper refuses to run without an explicit confirmation, an absolute protected
backup directory, a running PostgreSQL/service pair and a target Flyway version greater than the
current version. Preflight also rejects placeholder or shared materializer credentials and unsafe
worker polling bounds. The helper stops an existing materializer, creates a custom-format `pg_dump`,
a mode-0600 environment backup and a tagged rollback image before it changes the database:

```bash
BPI_RUNTIME_UPGRADE_BACKUP_DIR=/secure/bpi-upgrade-backups \
BPI_RUNTIME_UPGRADE_CONFIRM=UPGRADE_BPI_RUNTIME_EXPAND_ONLY \
  make bpi-runtime-upgrade-expand-only
```

The worker Dockerfile avoids BuildKit-only directives so the target Docker engine can build it
without installing a host plugin. The checked-in environment templates use the exact
DaoCloud-proxied `python:3.12.13-slim-bookworm` base, avoiding a floating Python tag while remaining
reachable from the target network.

Set `BPI_EXPECTED_FLYWAY_VERSION` to the exact target migration before each expand-only upgrade. The
helper verifies the packaged migration set, builds the service, WMS adapter and dataset materializer
images, provisions or rotates the non-inheriting `bpi_materializer` role, runs only `bpi-migrate`,
reapplies and verifies its exact table privileges, and recreates only the service-side application
containers. The dataset worker and bucket bootstrap stay disabled and stopped. It does not recreate
PostgreSQL, the web container or any named volume. Override the
bounded wait with `BPI_RUNTIME_UPGRADE_HEALTH_TIMEOUT_SECONDS` only when a measured cold start needs
more time. Runtime HTTP checks also use bounded connect/request timeouts so a failed dependency
cannot leave the upgrade command hanging indefinitely.

The JSON report is written as soon as the protected backup and rollback image exist, then refreshed
after migration, service recreation and final smoke. If a later phase fails, keep that non-PASS
report and its referenced artifacts: `phase=MIGRATION_APPLIED` means the schema must stay expanded
even if the previous application image is restored.

Rollback is application-only: keep the expanded schema, keep rule publication/application,
candidate consumers, dataset materializer and dataset bucket bootstrap disabled, select the tagged
rollback service image recorded in the report, recreate only `bpi-service`, and rerun the runtime
smoke. A READY row pins a verified MinIO object version, and application rollback does not delete
that version. This is not an Object Lock/WORM claim; retention-admin operations remain separately
governed. Schema
downgrade, Flyway repair, object overwrite and `DROP` rollback are intentionally unsupported.

The integrated ADP upgrade (`make bpi-integrated-upgrade-expand-only`) also treats deployment
manifests as release artifacts. Before it resolves the new worker services, it validates the clean
release Compose file without changing the runtime. After image builds and protected PostgreSQL/UI
backups succeed, it stores mode-0600 backups of the current runtime Compose, MinIO and Polaris
configuration, then stages only those release manifests. It does not replace the rest of the ADP
runtime tree. The materializer, catalog publisher and their bootstrap services remain disabled and
stopped throughout the expand-only upgrade.

If an independently verified recovery step already applied the exact target Flyway version, the
integrated helper can validate the existing schema and redeploy only the applications. This path is
fail-closed and requires both `BPI_INTEGRATED_ALLOW_ALREADY_MIGRATED=true` and
`BPI_INTEGRATED_ALREADY_MIGRATED_CONFIRM=REDEPLOY_APPLICATIONS_ON_EXISTING_BPI_SCHEMA`. Flyway still
runs in no-op validation mode; a checksum mismatch fails the deployment, and a database version
above the release target is always rejected.

## Integrated target image rollback

The target service and adapter rollback rehearsal uses the integrated ADP Compose stack. It requires
explicit confirmation and two pre-existing rollback images, captures the current image tags and IDs,
keeps the current expand-only Flyway schema, checks core-table counts, runs a real ADP login and `/bpi/#/points` browser
smoke on the rollback images, then restores the exact current images and repeats all checks:

```bash
BPI_RUNTIME_ROLLBACK_CONFIRM=ROLLBACK_BPI_RUNTIME_IMAGES_AND_RESTORE \
BPI_ROLLBACK_SERVICE_IMAGE=<previous-service-image> \
BPI_ROLLBACK_ADAPTER_IMAGE=<previous-adapter-image> \
  make bpi-runtime-image-rollback-rehearsal
```

Do not run this against a production stack without a maintenance window and a separately approved
traffic plan. The accepted target rehearsal is recorded in
[`docs/testing/bpi-application-rollback-acceptance.md`](../../docs/testing/bpi-application-rollback-acceptance.md).

## Controlled joint acceptance

The target browser/Kafka/Flink/PostgreSQL chain is reproducible with these marker-scoped assets:

- `sql/joint-acceptance-seed.sql` creates only the topology, rule, golden boundary, history points,
  and missing line flags required by one acceptance run. Existing enabled flags are preserved;
  an existing disabled required flag blocks the seed instead of being overwritten.
- `scripts/browser-joint-acceptance.js` runs the real `publish`, `confirm`, post-cleanup `read`, and
  target-runtime `rule-read` browser phases. Set `BPI_ACCEPTANCE_EXPECTED_RUNTIME_STATUS` to require
  the exact `READY`, `DEGRADED`, or `INACTIVE` state in the rule drawer. Credentials are supplied
  only through the process environment and are never written to its report.
  A source-readiness gate can be verified without weakening it by setting
  `BPI_ACCEPTANCE_EXPECTED_PUBLISH_STATUS=422` and an exact
  `BPI_ACCEPTANCE_EXPECTED_PUBLISH_DETAIL`. Set `BPI_ACCEPTANCE_EXPECTED_PUBLISH_TOAST` as well to
  require the deployed page to render the matching business-facing message; the browser report then
  records both the rejected POST and the visible toast and treats only that matching fail-closed
  result as expected.
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

## Topology and rule target acceptance

`scripts/browser-topology-rule-acceptance.js` exercises the deployed authoring surface with a real
ADP login while keeping credentials only in process memory. Run `author` to create and validate a
marker topology and prove that its creator cannot publish it. Publish revision 2 with a separate
`BPI_ADMIN` identity, then run `finalize` to verify the immutable published version and create the
bound rule draft. A final `read` phase is suitable for post-restart verification. Every phase records
page/API evidence, console errors, request failures, and a screenshot without storing the ticket or
password.

## Point catalog readiness acceptance

`scripts/browser-point-catalog-acceptance.js` exercises the deployed `点位目录` page and the
topology publication hard gate. Its default `write` phase imports one marker-scoped JetLinks status
snapshot through the real UI, verifies idempotent replay, creates a topology bound to that point and
requires an unready point to produce explicit validation errors with no publish action. After a
service restart, run with `BPI_BROWSER_ACTION=read` to prove the same point and blocked topology are
still visible. Source device state must be queried independently; the script must not be used to
label an inactive or uncalibrated device as ready.

For the automatic JetLinks path, run with `BPI_BROWSER_ACTION=sync-read`. This mode performs no
fixture write: it waits for the Kafka-imported source revision in the configured ADP scope and proves
that the real page reads the same PostgreSQL snapshot. The accepted target evidence is documented in
`docs/testing/bpi-point-catalog-kafka-sync-acceptance.md`.

Use `BPI_BROWSER_ACTION=sync-validate` when the automatic snapshot must also be exercised through the
topology hard gate. Set `BPI_EXPECTED_POINT_CATALOG_REVISION` and the exact comma-separated
`BPI_EXPECTED_POINT_ISSUES`; the script reads the live snapshot, creates one marker-scoped draft through
the real UI, and requires the topology validation errors to equal the mapped issue set. This is how the
`m3/h` binding versus JetLinks `m³/h` regression is accepted without hiding calibration or source-sequence
blockers. The marker draft, its audit rows, and idempotency rows must be deleted with a targeted PostgreSQL
transaction after evidence capture. See `docs/testing/bpi-pilot-platform-prerequisites-acceptance.md`.
