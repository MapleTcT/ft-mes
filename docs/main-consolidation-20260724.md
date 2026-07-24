# Main Consolidation Baseline (2026-07-24)

This baseline consolidates the deliverable FT MES / ADP / BPI implementation
before local and test-environment cleanup. The base `main` commit was
`d1d33c563651daad9b89794db104015fecadcad1`.

## Included

| Area | Source | Main result |
| --- | --- | --- |
| Factory line reference | `codex/factory-line-ref-postgres-compat` | PostgreSQL `rm_line_formulas` schema and null-safe runtime reference conditions |
| Quality standard reference | `codex/quality-std-ref-layout-json-fix` | Runtime-view link, recovered layout JSON, and BaseService PostgreSQL OID layout patch source |
| RBAC persistence acceptance | `codex/rbac-acceptance-ssh-retry` | Configurable SSH keepalive and bounded retry behavior |
| WOM task-list i18n guards | `codex/wom-maketasklist-i18n-guards` | Recorded as semantically superseded by the newer WOM implementation already on `main` |
| QCS inspection supervision | Preserved runtime repair | Missing supervision columns, indexes, and `qcs_inspects_sv` compatibility view |
| Production list export | Preserved runtime repair | Runtime datagrid reconstruction and export field metadata compatibility |

## PostgreSQL Migrations

| Migration | Purpose |
| --- | --- |
| `199-rm-line-formulas-table.sql` | Restore the formula-to-production-line mapping table |
| `200-hierarchicalmod-factory-line-ref-null-safe-condition.sql` | Make factory-line reference conditions null-safe |
| `201-lims-quality-std-ref-runtime-view-link.sql` | Link the quality-standard reference view to its extra view |
| `202-lims-quality-std-ref-runtime-json.sql` | Restore the quality-standard reference layout JSON |
| `203-qcs-inspect-supervision-main-compat.sql` | Restore QCS supervision fields and view |
| `204-production-list-export-runtime-datagrid.sql` | Rebuild target production export datagrids |
| `205-production-export-propertycode-compat.sql` | Restore export property-code metadata |

## Runtime Verification

The seven migrations were executed twice with `ON_ERROR_STOP=1` against the
PostgreSQL container on `10.11.100.17`. Both passes completed successfully.
Post-run verification returned:

| Check | Result |
| --- | ---: |
| `rm_line_formulas` rows | 1 |
| Null-safe factory reference conditions | 4 |
| Quality-standard layout payload | 2920 bytes |
| `qcs_inspects_sv` rows | 30 |
| Required export functions | 3 |
| Rebuilt target export datagrids | 5 |

The existing production export browser/API evidence remains `READY` for all six
targets in `metadata/production-export-readiness-smoke.json`.

## Deliberately Excluded

- Old WOM static copies from the dirty primary worktree. The current runtime
  implementation and later fixes on `main` supersede them.
- The old physical-table compatibility migration. Current
  `PostgresModelSyncSupport` and `PostgresFieldSyncSupport` own that behavior.
- Generated screenshots containing browser-session metadata.
- Unknown `?/` directories and other untracked worktree residue.

## Cleanup Boundary

The canonical source after this consolidation is GitHub `main`. Do not delete or
reset `/Users/zhangchu/Documents/ADP/adp-source-repo` yet: it remains a dirty
historical recovery worktree. It should only be removed after its remaining
untracked evidence has been archived or explicitly discarded.
