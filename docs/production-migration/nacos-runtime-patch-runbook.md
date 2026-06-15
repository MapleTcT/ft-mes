# Nacos And Runtime Patch Production Runbook

## Status

Status: `PLANNED`

## Runtime Patch Inventory

| Patch Or Config | Current Test Evidence | Production Action | Rollback |
| --- | --- | --- | --- |
| Rendered Nacos configs | `deploy/docker/scripts/render-nacos-configs.py` | Diff against production config export | Restore previous config set |
| Runtime patch bundle | `deploy/docker/scripts/prepare-runtime-patches.sh` | Version and checksum patch files | Redeploy previous bundle |
| Business view runtime JSON | `deploy/docker/postgres/init/065-business-view-runtime-json.sql` | Promote only after page smoke | Revert via documented SQL |

## Required Evidence Before READY

- Production Nacos export before change.
- Rendered target Nacos config diff.
- Runtime patch file list with checksum.
- Restart order and health-check results.
- Page smoke after patch.
- Rollback rehearsal output.

## Restart Order Template

| Order | Component | Health Check | Result |
| ---: | --- | --- | --- |
| 1 | Nacos config publish | Config version visible | TBD |
| 2 | Keycloak/JWT sync consumers | Login and current user | TBD |
| 3 | Gateway | `/inter-api/auth/login` | TBD |
| 4 | Business services | Business route smoke | TBD |
