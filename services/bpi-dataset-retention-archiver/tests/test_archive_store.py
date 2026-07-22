from __future__ import annotations

import hashlib
import tempfile
import unittest
from dataclasses import replace
from datetime import UTC, datetime, timedelta
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

import pyarrow as pa
import pyarrow.parquet as pq
from minio.retention import Retention
from minio.error import S3Error

from bpi_dataset_catalog_publisher.source_object import validate_local_source
from bpi_dataset_retention_archiver.archive_store import (
    RetentionArchiveContractError,
    RetentionArchiveStore,
)
from bpi_dataset_retention_archiver.config import Settings
from bpi_dataset_retention_archiver.models import ArchiveClaim


class FakeResponse:
    def __init__(self, payload: bytes):
        self._payload = payload

    def stream(self, amt: int):
        for offset in range(0, len(self._payload), amt):
            yield self._payload[offset : offset + amt]

    def read(self) -> bytes:
        return self._payload

    def close(self) -> None:
        return

    def release_conn(self) -> None:
        return


class FakeMinio:
    def __init__(self, source_bucket: str, source_key: str, source_version: str, payload: bytes):
        self._versions = {}
        self._latest = {}
        self._counter = 0
        self.put_count = 0
        self._store(
            source_bucket,
            source_key,
            source_version,
            payload,
            {},
            None,
            False,
        )

    def _store(self, bucket, key, version, payload, metadata, retention, hold):
        self._versions[(bucket, key, version)] = {
            "payload": payload,
            "metadata": metadata,
            "retention": retention,
            "hold": hold,
        }
        self._latest[(bucket, key)] = version

    def get_object_lock_config(self, _bucket):
        return SimpleNamespace(mode="GOVERNANCE", duration=30, duration_unit="DAYS")

    def stat_object(self, bucket, key, version_id=None):
        version = version_id or self._latest.get((bucket, key))
        if version is None or (bucket, key, version) not in self._versions:
            raise S3Error(
                None, "NoSuchKey", "missing", key, "request", "host", bucket, key
            )
        item = self._versions[(bucket, key, version)]
        return SimpleNamespace(
            size=len(item["payload"]),
            version_id=version,
            object_name=key,
            metadata=item["metadata"],
        )

    def get_object(self, bucket, key, version_id=None):
        version = version_id or self._latest[(bucket, key)]
        return FakeResponse(self._versions[(bucket, key, version)]["payload"])

    def put_object(
        self,
        bucket,
        key,
        data,
        length,
        content_type=None,
        metadata=None,
        retention=None,
        legal_hold=False,
    ):
        del content_type
        payload = data.read(length)
        self._counter += 1
        self.put_count += 1
        version = f"retained-version-{self._counter}"
        self._store(
            bucket,
            key,
            version,
            payload,
            metadata or {},
            retention,
            legal_hold,
        )
        return SimpleNamespace(version_id=version)

    def get_object_retention(self, bucket, key, version_id=None):
        version = version_id or self._latest[(bucket, key)]
        return self._versions[(bucket, key, version)]["retention"]

    def is_object_legal_hold_enabled(self, bucket, key, version_id=None):
        version = version_id or self._latest[(bucket, key)]
        return self._versions[(bucket, key, version)]["hold"]

    def tamper(self, bucket, key, version, payload):
        self._versions[(bucket, key, version)]["payload"] = payload


def settings() -> Settings:
    return Settings(
        enabled=True,
        database_url="postgresql://test",
        minio_endpoint="minio:9000",
        minio_secure=False,
        minio_access_key="retention-archiver",
        minio_secret_key="secret",
        recovery_bucket="bpi-dataset-recovery",
        retention_mode="GOVERNANCE",
        retention_days=30,
        legal_hold_enabled=False,
        poll_interval_seconds=2,
        claim_timeout_seconds=300,
        max_attempts=3,
        health_port=19095,
        work_directory="/tmp",
    )


def fixture(directory: Path):
    snapshot_id = uuid4()
    materialization_id = uuid4()
    dataset_id = uuid4()
    schema = pa.schema(
        [
            pa.field("snapshot_id", pa.string(), nullable=False),
            pa.field("batch_id", pa.string(), nullable=False),
            pa.field("prediction_time", pa.timestamp("us", tz="UTC"), nullable=False),
        ],
        metadata={
            b"bpi.snapshot_id": str(snapshot_id).encode(),
            b"bpi.manifest_checksum": b"a" * 64,
        },
    )
    table = pa.Table.from_pylist(
        [{
            "snapshot_id": str(snapshot_id),
            "batch_id": "BATCH-001",
            "prediction_time": datetime(2026, 7, 22, 4, 0, tzinfo=UTC),
        }],
        schema=schema,
    )
    path = directory / "source.parquet"
    pq.write_table(table, path, version="2.6", compression="zstd")
    payload = path.read_bytes()
    fields = [
        {"name": field.name, "type": str(field.type), "nullable": field.nullable}
        for field in schema
    ]
    claim = ArchiveClaim(
        id=uuid4(),
        tenant_id="TENANT-01",
        catalog_publication_id=uuid4(),
        materialization_id=materialization_id,
        source_snapshot_id=snapshot_id,
        dataset_id=dataset_id,
        dataset_code="BOUNDARY-LABELS",
        dataset_version="1.0.0",
        plant_id="PLANT-01",
        line_ids=("LINE-S07-01",),
        archiver_version="bpi-dataset-retention-archiver/0.1.0",
        archive_profile="bpi-dataset-recovery-v1",
        manifest_checksum="a" * 64,
        source_content_sha256=hashlib.sha256(payload).hexdigest(),
        source_object_version_id="source-version-001",
        source_byte_size=len(payload),
        source_row_count=1,
        source_schema_json={"fields": fields},
        table_identifier="ft_mes_bpi.bpi_training.tenant_deadbeef.dataset_01",
        iceberg_snapshot_id=9223372036854775001,
        iceberg_metadata_location="s3://warehouse/metadata/v1.metadata.json",
        iceberg_schema_id=0,
        iceberg_partition_spec_id=0,
        catalog_verified_row_count=1,
        catalog_semantic_checksum="",
        source_bucket="bpi-datasets",
        source_object_key="datasets/source.parquet",
        source_facts_verified=True,
        claim_token=uuid4(),
        revision=2,
        attempt_count=1,
        retention_mode="GOVERNANCE",
        retain_until=datetime.now(UTC) + timedelta(days=30),
        legal_hold_enabled=False,
        archive_bucket=None,
        archive_prefix=None,
        source_archive_object_key=None,
        source_archive_version_id=None,
        archive_manifest_object_key=None,
        archive_manifest_version_id=None,
        archive_manifest_sha256=None,
    )
    verified = validate_local_source(claim, path)
    return replace(claim, catalog_semantic_checksum=verified.semantic_checksum), payload


class RetentionArchiveStoreTest(unittest.TestCase):
    def test_exact_source_and_manifest_are_locked_reused_and_recovered(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            claim, payload = fixture(directory)
            client = FakeMinio(
                claim.source_bucket,
                claim.source_object_key,
                claim.source_object_version_id,
                payload,
            )
            store = RetentionArchiveStore(settings(), client)
            source = store.download_source(claim, directory / "downloaded.parquet")
            bundle = store.ensure_bundle(claim, source.path)
            self.assertEqual(2, client.put_count)
            repeated = store.ensure_bundle(claim, source.path)
            self.assertEqual(bundle, repeated)
            self.assertEqual(2, client.put_count, "stable retained versions must be reused")
            verification = store.verify(
                claim, bundle, directory / "recovered.parquet"
            )
        self.assertEqual(1, verification.row_count)
        self.assertEqual(claim.catalog_semantic_checksum, verification.semantic_checksum)
        self.assertTrue(verification.metadata["objectLockVerified"])
        self.assertTrue(verification.metadata["recoveryVerified"])

    def test_tampered_retained_bytes_fail_recovery(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            claim, payload = fixture(directory)
            client = FakeMinio(
                claim.source_bucket,
                claim.source_object_key,
                claim.source_object_version_id,
                payload,
            )
            store = RetentionArchiveStore(settings(), client)
            source = store.download_source(claim, directory / "downloaded.parquet")
            bundle = store.ensure_bundle(claim, source.path)
            client.tamper(
                bundle.bucket,
                bundle.source.object_key,
                bundle.source.version_id,
                b"tampered",
            )
            with self.assertRaises(RetentionArchiveContractError):
                store.verify(claim, bundle, directory / "recovered.parquet")


if __name__ == "__main__":
    unittest.main()
