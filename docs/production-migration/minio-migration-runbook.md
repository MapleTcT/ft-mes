# MinIO Migration Runbook

## Status

Status: `PLANNED`

## Inventory

| Bucket | Object Count | Total Size | Inventory Evidence | Owner |
| --- | ---: | ---: | --- | --- |
| `mes` | TBD | TBD | TBD | Storage owner |
| `transfer` | TBD | TBD | TBD | Storage owner |

## Migration Steps

1. Export source bucket inventory with object key, size, etag and last modified time.
2. Run dry-run copy to the target MinIO endpoint.
3. Copy objects with resumable tooling such as `mc mirror`.
4. Compare object counts, total size and etag/hash where available.
5. Perform sampled download validation for business-critical attachments.
6. Keep source buckets read-only until business smoke signoff.

## Validation

| Check | Expected Evidence | Status |
| --- | --- | --- |
| Bucket inventory captured | Source and target inventory files | BLOCKED |
| Object counts match | Count comparison report | BLOCKED |
| Size totals match | Size comparison report | BLOCKED |
| Sampled downloads open | Sample list with result | BLOCKED |
| Rollback path rehearsed | Restore or source-read fallback proof | BLOCKED |

## Secret Handling

Do not commit MinIO access keys or secret keys. Store them in the deployment
secret manager or local secure operator vault only.
