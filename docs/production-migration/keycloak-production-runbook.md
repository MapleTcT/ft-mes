# Keycloak Production Runbook

## Status

Status: `PLANNED`

当前仓库已经提供 Keycloak source/target realm inventory 和对账工具，但还没有
生产或生产副本的实际导出、导入和登录 smoke 证据。该文档不能作为生产
Keycloak 迁移完成证明。

## Tooling

| Asset | Purpose | Status |
| --- | --- | --- |
| `deploy/keycloak/production-migration/.env.example` | Placeholder-only operator env template | READY |
| `deploy/keycloak/production-migration/scripts/export-realm-inventory.sh` | Read source or target realm metadata through Keycloak Admin API | READY |
| `deploy/keycloak/production-migration/scripts/normalize-realm-inventory.py` | Write sanitized realm/client/role/scope inventory files | READY |
| `deploy/keycloak/production-migration/scripts/compare-realm-inventory.py` | Compare source/target realm inventory | READY |
| `deploy/keycloak/production-migration/keycloak-migration-evidence.example.json` | Evidence manifest template for realm inventory, comparison, backup/restore, secret rotation, JWT/Nacos sync, post-migration auth smoke and rollback linkage | READY |
| `deploy/keycloak/production-migration/scripts/validate-keycloak-migration-evidence.py` | Strict READY validator for Keycloak migration evidence | READY |
| `deploy/docker/scripts/adp-keycloak-jwt-runtime-smoke.js` | Verifies the running test Keycloak realm, Nacos JWT public-key sync, Nacos service registration, and gateway login/menu chain without writing secrets to the report | READY |

```bash
ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-source-export
ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-target-export
make production-keycloak-compare KEYCLOAK_MIGRATION_REPORT_DIR=/tmp/adp-keycloak-migration-preflight
make production-keycloak-migration-evidence-check
make production-keycloak-migration-ready-check KEYCLOAK_MIGRATION_EVIDENCE=/secure/path/adp-keycloak-migration-evidence.json
```

Test-environment runtime smoke:

```bash
make smoke-keycloak-jwt
```

Latest test evidence is stored in `metadata/keycloak-jwt-runtime-smoke.json`.
Against `100.99.133.43`, the smoke passed 19/19 checks: realm `dt` is enabled,
the `lfy grant` direct-grant flow is active, `pc_dt` and `mobile_dt` are enabled,
the `supos` client scope has the required mappers, Nacos group `prod` has healthy
Keycloak instances, the Keycloak public-key hash matches the Nacos JWT config
hash, and gateway login plus current-user menu loading passed. This proves the
current test runtime chain; it is not production realm import or backup/restore
evidence.

## Evidence Manifest

Use `deploy/keycloak/production-migration/keycloak-migration-evidence.example.json`
as the shape for the real production or rehearsal evidence file. The real file
must stay outside git if it contains operational endpoints, ticket links,
backup references, rotation records or smoke artifacts.

The strict-ready check requires source realm inventory, target realm inventory,
realm comparison review, Keycloak database backup/restore rehearsal, admin and
client secret rotation, JWT public key sync to Nacos, post-migration login /
current-user / menu / RBAC / organization smoke, and rollback or database
restore evidence. It rejects `.example`, `template` and `sample` evidence paths
as production READY evidence.

## Required Assets

| Asset | Evidence | Owner |
| --- | --- | --- |
| Realm inventory | `source-realm-inventory.json` and `target-realm-inventory.json` from production-like endpoints | Identity owner |
| Client list and redirect URI review | `realm-inventory-comparison.json` and signed redirect URI review | Identity owner |
| Client secret rotation plan | TBD | Identity owner |
| Admin account rotation plan | TBD | Security owner |
| PostgreSQL backup and restore rehearsal | TBD | DBA |
| JWT public key sync proof in Nacos | TBD | Runtime owner |

## Rehearsal Steps

1. Export source realm inventory with `production-keycloak-source-export`.
2. Export target pre-state realm inventory with `production-keycloak-target-export`.
3. Restore Keycloak database backup into the rehearsal target.
4. Import the realm or restore the Keycloak production-style database backup.
5. Export target post-import inventory and run `production-keycloak-compare`.
6. Review clients, redirect URIs, roles, client scopes and user storage components.
7. Rotate admin credentials and client secrets in rehearsal.
8. Run `sync-keycloak-jwt-public-key.sh` against the target realm.
9. Restart gateway/auth consumers and run login/current-user/menu/RBAC smoke.

## Expected Evidence

| Evidence | Default Path |
| --- | --- |
| Source realm inventory | `/tmp/adp-keycloak-migration-preflight/source-realm-inventory.json` |
| Target realm inventory | `/tmp/adp-keycloak-migration-preflight/target-realm-inventory.json` |
| Client inventory | `/tmp/adp-keycloak-migration-preflight/source-clients.tsv` and `target-clients.tsv` |
| Realm comparison | `/tmp/adp-keycloak-migration-preflight/realm-inventory-comparison.json` |
| Keycloak migration evidence manifest | `/secure/path/adp-keycloak-migration-evidence.json` |
| JWT public key sync proof | Nacos diff showing `supfusion.cloud.jwt.secret` updated from target realm public key |
| Runtime Keycloak/JWT smoke | `metadata/keycloak-jwt-runtime-smoke.json` |
| Auth smoke proof | Login, current user, menu, RBAC authority and organization smoke output |

## Acceptance

Keycloak production readiness is not `READY` until login, current user, menu,
RBAC authority and organization smoke pass after the production-style import,
and `make production-keycloak-migration-ready-check` passes against real
non-template evidence.
