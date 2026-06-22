# Production Rollback Runbook

## Status

Status: `PLANNED`

This runbook is a template plus a machine-readable evidence gate. It must be
rehearsed before production cutover.

## Tooling

| Asset | Purpose | Status |
| --- | --- | --- |
| `deploy/rollback/production-migration/rollback-evidence.example.json` | Placeholder-only rollback rehearsal evidence manifest | READY |
| `deploy/rollback/production-migration/scripts/validate-rollback-evidence.py` | Validates component coverage, READY rules, artifact paths and secret hygiene | READY |

```bash
make production-rollback-evidence-check
make production-rollback-evidence-check ROLLBACK_EVIDENCE=/secure/path/adp-rollback-evidence.json
make production-rollback-ready-check ROLLBACK_EVIDENCE=/secure/path/adp-rollback-evidence.json
```

`production-rollback-evidence-check` validates the schema and component
coverage. `production-rollback-ready-check` is intentionally stricter and fails
until every rollback component is marked `READY` with non-placeholder backup,
restore and rehearsal evidence.

## Rollback Scope

| Area | Backup Evidence | Restore Command Or Procedure | Owner | Rehearsed |
| --- | --- | --- | --- | --- |
| PostgreSQL target database | TBD | `pg_restore --clean --if-exists --dbname "$PGDATABASE" "$BACKUP_FILE"` | DBA | No |
| MinIO objects | TBD | Restore from versioned bucket or preserved source bucket | Storage owner | No |
| Keycloak realm and users | TBD | Import signed realm export and restore database snapshot | Identity owner | No |
| Nacos configuration | TBD | Reapply exported config set and restart affected services | Runtime owner | No |
| Runtime patch set | TBD | Redeploy previous runtime patch bundle and restart services | Runtime owner | No |
| Domain, port and TLS entry | TBD | Repoint reverse proxy or DNS to previous stack | Infrastructure owner | No |

The required component IDs in the evidence manifest are:

- `postgresql-target-database`
- `minio-objects`
- `keycloak-realm-users`
- `nacos-configuration`
- `runtime-patch-set`
- `domain-port-tls-entry`

## Required Evidence Before READY

- PostgreSQL backup file path, checksum and restore rehearsal output.
- MinIO object rollback or dual-write strategy.
- Keycloak realm export, database backup and restore rehearsal output.
- Nacos config export before cutover and restore command.
- Runtime patch version before and after cutover.
- DNS/reverse-proxy rollback window and responsible person.

## Rehearsal Record

| Date | Environment | Result | Evidence Path | Owner |
| --- | --- | --- | --- | --- |
| TBD | TBD | BLOCKED | TBD | TBD |

## Secret Handling

Do not commit real database passwords, MinIO keys, Keycloak admin credentials,
TLS private keys or bearer tokens in rollback evidence. Store real evidence in a
secure operator path and reference only sanitized summaries or approved external
evidence URIs from this repository.
