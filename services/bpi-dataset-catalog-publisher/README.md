# BPI Dataset Catalog Publisher

Python 3.12 worker for Phase 3B-B. It claims an explicit catalog-publication
request from PostgreSQL, downloads the exact versioned Phase 3B-A Parquet
object, verifies its bytes, SHA-256, row count, schema and frozen manifest
identity, and publishes the enriched rows through Apache Polaris' Iceberg REST
Catalog. A request becomes `READY` only after an exact Iceberg snapshot scan
matches the source row count and semantic checksum.

The service is disabled unless `BPI_DATASET_CATALOG_PUBLISHER_ENABLED=true`.
It does not create a Polaris catalog, does not accept catalog or object-store
coordinates from browser requests, and does not claim WORM retention, MLflow
registration, model training or production readiness.

Object Lock retention is a separate downstream V29 task owned by the default-off
retention archiver. The recovery-rehearsal command in this package consumes only
a verified V29 recovery manifest, writes to an isolated recovery namespace and
must purge its own table, namespace and exact warehouse object versions before
reporting success. Publisher credentials are never reused for that rehearsal.

Its `BPI_DATASET_CATALOG_SOURCE_MINIO_*` identity is read-only and separate
from the Phase 3B-A materializer identity. Polaris owns warehouse credentials;
they are not passed to browser clients or persisted in BPI publication rows.

Phase 3B-B runs one publisher replica. Claim recovery and snapshot-property
reconciliation make restart safe, but supported horizontal scaling remains a
later reliability gate.

`PUBLISHER_VERSION` is an Iceberg table-contract identifier, not an image or
release version. Patch releases and rebuilds must keep it stable. An incompatible
schema, partition-spec or snapshot-property change requires an explicit database
migration and table migration plan before this identifier can change.
