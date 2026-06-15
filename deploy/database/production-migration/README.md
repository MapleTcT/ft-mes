# Production Migration Preflight

This directory is the durable entry point for production data-migration preparation.
It is intentionally PostgreSQL-first and contains templates only. It is not proof
that production migration is ready.

## Scope

Use these assets before any production cutover rehearsal:

- inventory the target PostgreSQL schema;
- collect row counts for agreed business tables;
- define checksum queries for critical tables;
- keep credentials outside git;
- write rehearsal outputs under an external report directory.

Oracle may appear only as a legacy source type in planning notes or environment
templates. The default runtime and target database stay PostgreSQL.

## Files

```text
.env.example                         # Placeholder-only environment template
table-list.example.txt               # Example table list for preflight SQL
scripts/run-target-preflight.sh       # Safe PostgreSQL target inventory runner
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

The script does not mutate the database. It writes inventory files to
`ADP_MIGRATION_REPORT_DIR` and exits if required connection variables are
missing.

## Readiness Rule

The production migration track remains `PLANNED` until at least one rehearsal
report proves:

- source inventory has been captured;
- target PostgreSQL schema exists;
- row-count and checksum checks match the accepted source snapshot;
- rollback has been rehearsed;
- business smoke signoff is complete.
