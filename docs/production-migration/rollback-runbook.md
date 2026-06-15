# Production Rollback Runbook

## Status

Status: `PLANNED`

This runbook is a template. It must be rehearsed before production cutover.

## Rollback Scope

| Area | Backup Evidence | Restore Command Or Procedure | Owner | Rehearsed |
| --- | --- | --- | --- | --- |
| PostgreSQL target database | TBD | `pg_restore --clean --if-exists --dbname "$PGDATABASE" "$BACKUP_FILE"` | DBA | No |
| MinIO objects | TBD | Restore from versioned bucket or preserved source bucket | Storage owner | No |
| Keycloak realm and users | TBD | Import signed realm export and restore database snapshot | Identity owner | No |
| Nacos configuration | TBD | Reapply exported config set and restart affected services | Runtime owner | No |
| Runtime patch set | TBD | Redeploy previous runtime patch bundle and restart services | Runtime owner | No |
| Domain, port and TLS entry | TBD | Repoint reverse proxy or DNS to previous stack | Infrastructure owner | No |

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
