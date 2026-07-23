# BPI Dataset Materializer

Python 3.12 worker for the Phase 3B-A artifact boundary and the Phase 3C-G
process-window extension. It claims frozen dataset materialization jobs from
PostgreSQL, writes deterministic Parquet, uploads a content-addressed object to a
private, versioned MinIO bucket, downloads the exact uploaded version to verify
its SHA-256, and only then marks the job `READY`. The persisted `artifactUri`
pins the MinIO `versionId`; application credentials cannot delete objects, but
this worker does not claim MinIO Object Lock/WORM retention.

## Artifact contracts

- Historical `bpi.dataset-parquet.v1` objects remain readable and immutable.
  The deterministic v1 builder remains covered for compatibility, but the
  `0.2.0` worker claims only the exact v2/0.2.0 pair. Before upgrading, operators
  must prove there are no non-terminal v1 jobs. V1 rejects any
  `process.window.*` feature.
- New API requests use `bpi.dataset-parquet.v2` with
  `bpi-dataset-materializer/0.2.0`.
- V2 stores governed process-window values in the non-null
  `feature_process_window_values` column as
  `map<string, decimal128(24, 6)>`. Keys are the original, sorted feature
  references, so punctuation cannot collide through column-name escaping.
- Missing, boolean, text, NaN and infinite process-window values fail closed.
  Unknown feature namespaces also remain unsupported.
- The materializer reads only included immutable `feature_payload` and
  `label_payload` rows. It never reads `source_payload` or excluded samples.

The worker is disabled unless `BPI_DATASET_MATERIALIZER_ENABLED=true`. It never
creates the bucket.

The current deployment contract runs exactly one worker replica. The claim token
and timeout recover a crashed worker, but lease heartbeats and supported
horizontal scaling are a later reliability gate. Do not scale this service above
one replica.

Production images contain only runtime code. Unit and integration tests run from
the repository checkout, outside the image.
