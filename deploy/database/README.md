# Database Deployment Notes

This directory documents database transition work that should survive beyond the recovered runtime package.

## Current default

The Docker test profile is PostgreSQL-first. Runtime init scripts are kept under:

```text
deploy/docker/postgres/init/
```

These scripts are intentionally idempotent where possible because the recovered ADP/MES runtime creates part of the schema at startup and the compatibility scripts repair missing tables, views, columns and type shims after that.

The current script inventory is generated into:

```text
docs/postgres-migration-index.md
docs/postgres-migration-watch-rationale.md
metadata/postgres-migration-inventory.json
```

Run `make postgres-migration-index` after adding or changing scripts, and `make postgres-migration-check` before committing. Script names must stay in the `NNN-lowercase-slug.sql` form, numbers must be continuous and unique, destructive table/schema/database reset statements are blocked by the check, and watch statements must either be guarded `DROP ... IF EXISTS` compatibility rebuilds or scoped `DELETE ... WHERE` cleanup statements.

## Legacy Oracle

Oracle is treated as a legacy compatibility source. Use:

```text
deploy/docker/.env.oracle-legacy.example
```

only when a specific module still needs to connect to an Oracle database during comparison or staged migration.

## Promotion path

When a module is promoted into `backend/source-modules`, move its database work from ad-hoc runtime fixes toward module-owned migration scripts:

```text
deploy/database/<module>/postgres/
deploy/database/<module>/oracle-legacy/
```

Keep PostgreSQL as the default path and put Oracle-only SQL in `oracle-legacy` until it can be retired.

## Production migration preparation

Production migration preparation lives under:

```text
deploy/database/production-migration/
```

That directory contains source row-count inventory, target PostgreSQL preflight
templates, source/target row-count comparison, and safe read-only runners. It is
not a production cutover script and it does not make the migration track
`READY`; readiness remains controlled by:

```text
docs/production-migration-readiness.md
metadata/production-migration-readiness.json
make production-migration-readiness-check
```
