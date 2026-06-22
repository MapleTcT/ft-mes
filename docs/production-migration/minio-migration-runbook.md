# MinIO Migration Runbook

## Status

Status: `PLANNED`

当前仓库已经提供 MinIO 源/目标 inventory 和对象清单对账工具，并补充了测试环境
runtime bucket inventory smoke。测试环境结果只能证明当前运行态 MinIO 可访问、
bucket/object 清单可读，仍不能作为生产迁移完成证明。

## Tooling

| Asset | Purpose | Status |
| --- | --- | --- |
| `deploy/minio/production-migration/.env.example` | Placeholder-only operator env template | READY |
| `deploy/minio/production-migration/bucket-list.example.txt` | Example bucket list; replace with signed production bucket list | READY |
| `deploy/minio/production-migration/scripts/run-bucket-inventory.sh` | Read-only source/target bucket object inventory using `mc ls --recursive --json` | READY |
| `deploy/minio/production-migration/scripts/compare-bucket-inventory.py` | Compare source and target inventories by bucket, key, size, and etag when available | READY |
| `deploy/minio/production-migration/minio-migration-evidence.example.json` | Evidence manifest template for source/target inventory, comparison, dry-run, sampled download, runtime smoke and rollback linkage | READY |
| `deploy/minio/production-migration/scripts/validate-minio-migration-evidence.py` | Strict READY validator for MinIO migration evidence | READY |
| `deploy/docker/scripts/adp-minio-runtime-smoke.js` | Read-only SSH runtime smoke for the current test MinIO container and buckets | READY |
| `metadata/minio-runtime-smoke.json` | Latest `100.99.133.43` test-environment MinIO runtime smoke report | PASS |

```bash
ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-source-inventory
ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-target-inventory
make production-minio-compare MINIO_MIGRATION_REPORT_DIR=/tmp/adp-minio-migration-preflight
make production-minio-migration-evidence-check
make production-minio-migration-ready-check MINIO_MIGRATION_EVIDENCE=/secure/path/adp-minio-migration-evidence.json
make smoke-minio-runtime ADP_SSH_HOST=100.99.133.43
```

## Evidence Manifest

Use `deploy/minio/production-migration/minio-migration-evidence.example.json`
as the shape for the real production or rehearsal evidence file. The real file
must stay outside git if it contains operational endpoints, ticket links,
storage evidence references or object samples.

The strict-ready check requires source bucket inventory, target bucket
inventory, object inventory comparison, migration dry-run, sampled download
verification, post-migration runtime smoke and rollback or source-read fallback
evidence. It rejects `.example`, `template` and `sample` evidence paths as
production READY evidence.

## Test Environment Runtime Smoke

Latest smoke report: `metadata/minio-runtime-smoke.json`

| Environment | Container | Endpoint | Checks | Buckets | Objects | Total Size |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| `100.99.133.43` | `adp-mes-newbase-minio-1` | `http://127.0.0.1:30200` inside container | 8/8 PASS | 2 | 31 | 104285 bytes |

| Bucket | Object Count | Total Size | Status |
| --- | ---: | ---: | --- |
| `dtbucket` | 6 | 54784 bytes | PASS |
| `system001bucket` | 25 | 49501 bytes | PASS |

The smoke reads MinIO credentials from the running container environment only.
It does not write access keys, secret keys, public object URLs or raw object keys
to the committed report. Object samples are stored as SHA-256 key prefixes only.

## Inventory

| Bucket | Object Count | Total Size | Inventory Evidence | Owner |
| --- | ---: | ---: | --- | --- |
| `mes` | TBD | TBD | TBD | Storage owner |
| `transfer` | TBD | TBD | TBD | Storage owner |

## Migration Steps

1. Export source bucket inventory with object key, size, etag and last modified time.
2. Export target bucket inventory before migration so the runbook records target pre-state.
3. Run dry-run copy to the target MinIO endpoint.
4. Copy objects with resumable tooling such as `mc mirror`.
5. Re-export target bucket inventory after migration.
6. Compare object counts, total size and etag/hash where available.
7. Perform sampled download validation for business-critical attachments.
8. Keep source buckets read-only until business smoke signoff.

The copy step must be rehearsed in a non-production target first. Do not run a
write-capable mirror command from this repository without a signed bucket list,
approved maintenance window and rollback plan.

## Expected Evidence

| Evidence | Default Path |
| --- | --- |
| Source object inventory | `/tmp/adp-minio-migration-preflight/source-object-inventory.tsv` |
| Source bucket summary | `/tmp/adp-minio-migration-preflight/source-bucket-summary.tsv` |
| Target object inventory | `/tmp/adp-minio-migration-preflight/target-object-inventory.tsv` |
| Target bucket summary | `/tmp/adp-minio-migration-preflight/target-bucket-summary.tsv` |
| Object comparison TSV | `/tmp/adp-minio-migration-preflight/object-inventory-comparison.tsv` |
| Object comparison JSON | `/tmp/adp-minio-migration-preflight/object-inventory-comparison.json` |

## Validation

| Check | Expected Evidence | Status |
| --- | --- | --- |
| Bucket inventory tooling available | `make runtime-script-check` parses MinIO scripts | PASS |
| Test runtime bucket inventory captured | `metadata/minio-runtime-smoke.json` from `100.99.133.43` | PASS |
| MinIO evidence manifest shape | `make production-minio-migration-evidence-check` | PASS |
| Bucket inventory captured | Source and target inventory files from production-like endpoints | BLOCKED |
| Object counts match | `object-inventory-comparison.json` from real endpoints | BLOCKED |
| Size totals match | `object-inventory-comparison.json` from real endpoints | BLOCKED |
| Sampled downloads open | Sample list with result | BLOCKED |
| Rollback path rehearsed | Restore or source-read fallback proof | BLOCKED |
| Strict MinIO migration READY gate | `make production-minio-migration-ready-check MINIO_MIGRATION_EVIDENCE=/secure/path/adp-minio-migration-evidence.json` | BLOCKED |

## Secret Handling

Do not commit MinIO access keys or secret keys. Store them in the deployment
secret manager or local secure operator vault only.
