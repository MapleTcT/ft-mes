# Keycloak Production Migration

This folder contains read-only Keycloak realm inventory and comparison tooling.
It creates production-migration evidence; it does not perform realm import,
client secret rotation, or production database backup/restore.

## Files

| File | Purpose |
| --- | --- |
| `.env.example` | Placeholder-only operator env template. Keep real admin credentials outside git. |
| `scripts/export-realm-inventory.sh` | Reads source or target realm metadata through the Keycloak Admin API. |
| `scripts/normalize-realm-inventory.py` | Converts raw Admin API responses into sanitized JSON/TSV inventory files. |
| `scripts/compare-realm-inventory.py` | Compares source and target realm inventory by clients, roles, scopes, components, and JWT key fingerprint. |
| `keycloak-migration-evidence.example.json` | Placeholder-only evidence manifest for realm inventory, comparison, database restore, secret rotation, JWT/Nacos sync, auth smoke and rollback linkage. |
| `scripts/validate-keycloak-migration-evidence.py` | Validates evidence shape, READY rules and secret hygiene. |

## Commands

```bash
ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-source-export
ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-target-export
make production-keycloak-compare KEYCLOAK_MIGRATION_REPORT_DIR=/tmp/adp-keycloak-migration-preflight
make production-keycloak-migration-evidence-check
make production-keycloak-migration-ready-check KEYCLOAK_MIGRATION_EVIDENCE=/secure/path/adp-keycloak-migration-evidence.json
```

The export commands write:

- `source-realm-inventory.json` / `target-realm-inventory.json`
- `source-clients.tsv` / `target-clients.tsv`
- `source-roles.tsv` / `target-roles.tsv`
- `source-client-scopes.tsv` / `target-client-scopes.tsv`
- raw Admin API responses under `raw/source` and `raw/target`

The comparison command writes:

- `realm-inventory-comparison.tsv`
- `realm-inventory-comparison.json`

## Readiness Rule

The Keycloak track remains `PLANNED` until a production-style rehearsal proves
realm export/import, database backup/restore, admin and client secret rotation,
JWT public key synchronization to Nacos, and login/current-user/menu smoke after
the target realm is active. The strict-ready check rejects `.example`,
`template` and `sample` evidence paths, placeholder values, open blocking issues
and non-placeholder secret assignments.
