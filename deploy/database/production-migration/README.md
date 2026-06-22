# Production Migration Preflight

This directory is the durable entry point for production data-migration preparation.
It is intentionally PostgreSQL-first and contains templates only. It is not proof
that production migration is ready.

## Scope

Use these assets before any production cutover rehearsal:

- inventory the target PostgreSQL schema;
- inventory the migration source row counts;
- collect row counts for agreed business tables;
- compare source and target row counts;
- define and compare checksum queries for critical tables;
- keep credentials outside git;
- write rehearsal outputs under an external report directory.

Oracle may appear only as a legacy source type in planning notes or environment
templates. The default runtime and target database stay PostgreSQL.

## Files

```text
.env.example                         # Placeholder-only environment template
table-list.example.txt               # Example table list for preflight SQL
source-checksums.example.tsv          # Example source checksum report
target-checksums.example.tsv          # Example target checksum report
migration-evidence.example.json       # Database migration rehearsal evidence manifest template
scripts/run-target-preflight.sh       # Safe PostgreSQL target inventory runner
scripts/run-source-inventory.sh       # Safe source row-count inventory runner
scripts/compare-row-counts.py         # Source/target row-count comparison
scripts/compare-checksums.py          # Source/target checksum comparison
scripts/validate-migration-evidence.py # Evidence manifest validator
sql/target-schema-inventory.sql       # Target schema inventory
sql/target-row-counts-template.sql    # Row count template, used by the runner
sql/target-checksum-template.sql      # Manual checksum query template
```

## Usage

Copy the template outside git or into a local ignored `.env`:

```bash
cp deploy/database/production-migration/.env.example /secure/path/adp-prod-migration.env
```

Run a target-only preflight against a PostgreSQL rehearsal database:

```bash
ADP_PROD_MIGRATION_ENV=/secure/path/adp-prod-migration.env \
  deploy/database/production-migration/scripts/run-target-preflight.sh
```

Run a read-only source row-count inventory. `SOURCE_DB_TYPE=postgresql` is the
normal path. `SOURCE_DB_TYPE=oracle-legacy` is allowed only for a legacy source
snapshot during migration planning:

```bash
ADP_PROD_MIGRATION_ENV=/secure/path/adp-prod-migration.env \
  deploy/database/production-migration/scripts/run-source-inventory.sh
```

Compare source and target row counts after both reports exist:

```bash
python3 deploy/database/production-migration/scripts/compare-row-counts.py \
  --source /tmp/adp-production-migration-preflight/source-row-counts.tsv \
  --target /tmp/adp-production-migration-preflight/target-row-counts.tsv \
  --output-dir /tmp/adp-production-migration-preflight
```

Compare source and target checksums after agreed critical-table checksum reports
exist:

```bash
python3 deploy/database/production-migration/scripts/compare-checksums.py \
  --source /tmp/adp-production-migration-preflight/source-checksums.tsv \
  --target /tmp/adp-production-migration-preflight/target-checksums.tsv \
  --output-dir /tmp/adp-production-migration-preflight
```

Checksum TSV inputs must include `table_name` and `checksum`. `row_count` and
`status` are recommended. The comparison script writes
`checksum-comparison.tsv` and `checksum-comparison.json`, and fails by default
when source/target tables are missing, row counts differ, checksums differ, or a
report row has a non-OK status.

Validate a database migration rehearsal evidence manifest. The checked-in
example is intentionally only `PLANNED` evidence; strict mode must fail until a
real rehearsal package replaces template references with source inventory,
target preflight, row-count comparison, checksum comparison, target PostgreSQL
runtime smoke, and rollback linkage evidence:

```bash
python3 deploy/database/production-migration/scripts/validate-migration-evidence.py \
  --evidence deploy/database/production-migration/migration-evidence.example.json

python3 deploy/database/production-migration/scripts/validate-migration-evidence.py \
  --evidence /secure/path/adp-db-migration-evidence.json \
  --strict-ready
```

These scripts do not mutate the database. Inventory runners write files to
`ADP_MIGRATION_REPORT_DIR` and exit if required connection variables are
missing.

## Readiness Rule

The production migration track remains `PLANNED` until at least one rehearsal
report proves:

- source inventory has been captured;
- target PostgreSQL schema exists;
- row-count and checksum checks match the accepted source snapshot;
- the database migration evidence manifest passes strict READY validation;
- rollback has been rehearsed;
- business smoke signoff is complete.
