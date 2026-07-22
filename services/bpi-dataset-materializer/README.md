# BPI Dataset Materializer

Python 3.12 worker for Phase 3B-A. It claims frozen dataset materialization jobs
from PostgreSQL, writes deterministic Parquet, uploads a content-addressed object
to a private, versioned MinIO bucket, downloads the exact uploaded version to
verify its SHA-256, and only then marks the job `READY`. The persisted
`artifactUri` pins the MinIO `versionId`; application credentials cannot delete
objects, but Phase 3B-A does not claim MinIO Object Lock/WORM retention.

The worker is disabled unless `BPI_DATASET_MATERIALIZER_ENABLED=true`. It never
creates the bucket and never reads `source_payload` or excluded samples.

Phase 3B-A runs exactly one worker replica. The claim token and timeout recover
a crashed worker, but lease heartbeats and supported horizontal scaling are a
later reliability gate. Do not scale this service above one replica.

Production images contain only runtime code. Unit and integration tests run from
the repository checkout, outside the image.
