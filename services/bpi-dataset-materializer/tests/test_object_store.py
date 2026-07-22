from __future__ import annotations

import hashlib
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch
from uuid import UUID

from bpi_dataset_materializer.config import Settings
from bpi_dataset_materializer.models import MaterializationClaim, ParquetArtifact
from bpi_dataset_materializer.object_store import (
    DatasetObjectStore,
    ObjectStoreContractError,
)


PAYLOAD = b"deterministic-parquet-bytes"


class FakeResponse:
    def __init__(self, payload: bytes):
        self.payload = payload
        self.closed = False
        self.released = False

    def stream(self, amt: int):
        for offset in range(0, len(self.payload), amt):
            yield self.payload[offset : offset + amt]

    def close(self):
        self.closed = True

    def release_conn(self):
        self.released = True


class FakeMinio:
    def __init__(self, payload: bytes = PAYLOAD, versioning: str | None = "Enabled"):
        self.payload = payload
        self.versioning = versioning
        self.response: FakeResponse | None = None
        self.get_call = None

    def bucket_exists(self, bucket: str) -> bool:
        return True

    def get_bucket_versioning(self, bucket: str):
        return SimpleNamespace(status=self.versioning)

    def stat_object(self, bucket: str, key: str, version_id: str | None = None):
        return SimpleNamespace(
            size=len(PAYLOAD),
            metadata={
                "x-amz-meta-content-sha256": hashlib.sha256(PAYLOAD).hexdigest(),
                "x-amz-meta-manifest-checksum": "a" * 64,
                "x-amz-meta-artifact-schema-version": "bpi.dataset-parquet.v1",
                "x-amz-meta-materializer-version": "bpi-dataset-materializer/0.1.0",
            },
            object_name=key,
            version_id="version/one+two",
        )

    def get_object(self, bucket: str, key: str, version_id: str | None = None):
        self.get_call = (bucket, key, version_id)
        self.response = FakeResponse(self.payload)
        return self.response


def settings() -> Settings:
    return Settings(
        enabled=True,
        database_url="postgresql://worker:test@db/bpi",
        minio_endpoint="minio:9000",
        minio_secure=False,
        minio_access_key="worker",
        minio_secret_key="secret-value",
        minio_bucket="bpi-datasets",
        poll_interval_seconds=2,
        claim_timeout_seconds=300,
        max_attempts=3,
        health_port=19093,
        work_directory="/tmp/bpi-materializer-test",
    )


def claim() -> MaterializationClaim:
    return MaterializationClaim(
        id=UUID("00000000-0000-0000-0000-000000000201"),
        tenant_id="tenant-a",
        snapshot_id=UUID("00000000-0000-0000-0000-000000000101"),
        dataset_id=UUID("00000000-0000-0000-0000-000000000301"),
        dataset_code="SUGAR_BATCH_QUALITY",
        dataset_version="1.0.0",
        plant_id="PLANT-01",
        artifact_schema_version="bpi.dataset-parquet.v1",
        materializer_version="bpi-dataset-materializer/0.1.0",
        manifest_checksum="a" * 64,
        manifest_schema_version="bpi.dataset-manifest.v1",
        definition_checksum="b" * 64,
        included_count=1,
        feature_refs=(),
        label_refs=(),
        claim_token=UUID("00000000-0000-0000-0000-000000000401"),
        revision=2,
        attempt_count=1,
    )


class DatasetObjectStoreTest(unittest.TestCase):
    def artifact(self, path: Path) -> ParquetArtifact:
        path.write_bytes(PAYLOAD)
        return ParquetArtifact(
            path=path,
            content_sha256=hashlib.sha256(PAYLOAD).hexdigest(),
            byte_size=len(PAYLOAD),
            row_count=1,
            schema_json={},
            metadata={},
        )

    def test_reuses_only_versioned_object_with_matching_downloaded_bytes(self):
        client = FakeMinio()
        with patch(
            "bpi_dataset_materializer.object_store.Minio",
            return_value=client,
        ), tempfile.TemporaryDirectory() as directory:
            store = DatasetObjectStore(settings())
            store.validate_bucket()
            stored = store.ensure_uploaded(
                claim(), self.artifact(Path(directory) / "dataset.parquet"))

        self.assertEqual("version/one+two", stored.version_id)
        self.assertTrue(stored.uri.endswith("?versionId=version%2Fone%2Btwo"))
        self.assertEqual("version/one+two", client.get_call[2])
        self.assertTrue(client.response.closed)
        self.assertTrue(client.response.released)

    def test_rejects_corrupted_object_even_when_metadata_matches(self):
        client = FakeMinio(payload=b"different-object-content")
        with patch(
            "bpi_dataset_materializer.object_store.Minio",
            return_value=client,
        ), tempfile.TemporaryDirectory() as directory:
            store = DatasetObjectStore(settings())
            with self.assertRaisesRegex(
                ObjectStoreContractError, "content checksum mismatch"
            ):
                store.ensure_uploaded(
                    claim(), self.artifact(Path(directory) / "dataset.parquet"))

    def test_rejects_bucket_without_versioning(self):
        client = FakeMinio(versioning=None)
        with patch(
            "bpi_dataset_materializer.object_store.Minio",
            return_value=client,
        ):
            store = DatasetObjectStore(settings())
            with self.assertRaisesRegex(
                ObjectStoreContractError, "versioning enabled"
            ):
                store.validate_bucket()


if __name__ == "__main__":
    unittest.main()
