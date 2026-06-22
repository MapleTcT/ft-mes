# MinIO Production Migration

This folder contains read-only inventory and comparison tooling for MinIO
object migration. It does not prove production migration is complete; it creates
reviewable evidence for the `minio-file-migration` readiness track.

## Files

| File | Purpose |
| --- | --- |
| `.env.example` | Placeholder-only operator env template. Keep real access keys outside git. |
| `bucket-list.example.txt` | Example bucket list discovered from the ADP/MES package line. |
| `scripts/run-bucket-inventory.sh` | Runs `mc ls --recursive --json` for source or target buckets and writes inventory TSV files. |
| `scripts/normalize-mc-ls-json.py` | Converts MinIO Client JSON lines to object and bucket inventory rows. |
| `scripts/compare-bucket-inventory.py` | Compares source and target object inventories by bucket, key, size, and etag when available. |
| `minio-migration-evidence.example.json` | Placeholder-only evidence manifest for source/target inventory, comparison, dry-run, sample download, runtime smoke and rollback linkage. |
| `scripts/validate-minio-migration-evidence.py` | Validates evidence shape, READY rules and secret hygiene. |

## Commands

```bash
ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-source-inventory
ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-target-inventory
make production-minio-compare MINIO_MIGRATION_REPORT_DIR=/tmp/adp-minio-migration-preflight
make production-minio-migration-evidence-check
make production-minio-migration-ready-check MINIO_MIGRATION_EVIDENCE=/secure/path/adp-minio-migration-evidence.json
```

The inventory commands produce:

- `source-object-inventory.tsv` / `target-object-inventory.tsv`
- `source-bucket-summary.tsv` / `target-bucket-summary.tsv`
- raw `mc ls --recursive --json` evidence under `raw/source` and `raw/target`

The comparison command produces:

- `object-inventory-comparison.tsv`
- `object-inventory-comparison.json`

## Readiness Rule

The MinIO track remains `PLANNED` until source and target inventories are
captured from production-like endpoints, object counts and sizes match, sampled
downloads are verified, runtime smoke passes, and rollback or source-read
fallback is rehearsed. The strict-ready check rejects `.example`, `template`
and `sample` evidence paths, placeholder values, open blocking issues and
non-placeholder secret assignments.
