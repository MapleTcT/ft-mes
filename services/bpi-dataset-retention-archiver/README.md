# BPI Dataset Retention Archiver

This default-off worker creates a two-object recovery package for a verified BPI
Iceberg publication:

1. the exact source Parquet object version;
2. a canonical recovery manifest that freezes source, Iceberg snapshot and
   retention facts.

Both versions are written to the dedicated `bpi-dataset-recovery` Object Lock
bucket and read back before the PostgreSQL task can become `LOCKED`. The worker
does not write to the active Iceberg warehouse and does not claim full-site
disaster recovery.

The worker uses one dedicated MinIO principal with read-only access to the
source dataset prefix and put/get/retention/legal-hold access to the recovery
bucket. It has no delete or governance-bypass permission. Runtime activation,
retention mode, duration and legal hold are environment-controlled and remain
disabled by default.
