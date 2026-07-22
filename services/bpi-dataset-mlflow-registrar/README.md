# BPI Dataset MLflow Registrar

The registrar consumes `QUEUED` rows from
`bpi.bpi_dataset_mlflow_registrations`, logs one immutable dataset input to an
internal MLflow Tracking server, reads it back, and records the verified external
identity in PostgreSQL.

It deliberately has no MinIO, Polaris, Kafka, WOM, QCS, WMS, personnel, or PLC
credentials. The exact retained S3 object URI is metadata; V29 already owns its
Object Lock and recovery verification.

The worker is disabled by default. Enable it only together with the `bpi-ml`
Compose profile and the dedicated `bpi_mlflow_registrar` PostgreSQL role.

Successful registration does not mean a model was trained, registered, approved,
or activated in production.
