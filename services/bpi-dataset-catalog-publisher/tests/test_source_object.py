from __future__ import annotations

import hashlib
import tempfile
import unittest
from dataclasses import replace
from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

import pyarrow as pa
import pyarrow.parquet as pq

from bpi_dataset_catalog_publisher.config import Settings
from bpi_dataset_catalog_publisher.models import PublicationClaim
from bpi_dataset_catalog_publisher.source_object import (
    SourceObjectContractError,
    SourceObjectStore,
    semantic_checksum,
)


class FakeResponse:
    def __init__(self, payload: bytes):
        self._payload = payload
        self.closed = False

    def stream(self, amt: int):
        for offset in range(0, len(self._payload), amt):
            yield self._payload[offset : offset + amt]

    def close(self) -> None:
        self.closed = True

    def release_conn(self) -> None:
        return


class FakeMinio:
    def __init__(self, payload: bytes, version_id: str):
        self.payload = payload
        self.version_id = version_id
        self.calls = []

    def stat_object(self, bucket, key, version_id):
        self.calls.append(("stat", bucket, key, version_id))
        return SimpleNamespace(
            size=len(self.payload),
            version_id=self.version_id,
            object_name=key,
        )

    def get_object(self, bucket, key, version_id):
        self.calls.append(("get", bucket, key, version_id))
        return FakeResponse(self.payload)


def settings() -> Settings:
    return Settings(
        enabled=True,
        database_url="postgresql://test",
        minio_endpoint="minio:9000",
        minio_secure=False,
        minio_access_key="reader",
        minio_secret_key="secret",
        catalog_uri="http://polaris:8181/api/catalog",
        catalog_warehouse="ft_mes_bpi",
        catalog_credential="publisher:secret",
        catalog_oauth2_server_uri="http://polaris:8181/api/catalog/v1/oauth/tokens",
        catalog_realm="POLARIS",
        iceberg_s3_endpoint="http://minio:9000",
        iceberg_s3_region="us-east-1",
        poll_interval_seconds=2,
        claim_timeout_seconds=300,
        max_attempts=3,
        health_port=19094,
        work_directory="/tmp",
    )


def parquet_fixture(directory: Path):
    snapshot_id = uuid4()
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
        [
            {
                "snapshot_id": str(snapshot_id),
                "batch_id": "BATCH-001",
                "prediction_time": datetime(2026, 7, 22, 4, 0, tzinfo=UTC),
            }
        ],
        schema=schema,
    )
    path = directory / "source.parquet"
    pq.write_table(table, path, version="2.6", compression="zstd")
    payload = path.read_bytes()
    fields = [
        {"name": field.name, "type": str(field.type), "nullable": field.nullable}
        for field in schema
    ]
    claim = PublicationClaim(
        id=uuid4(),
        tenant_id="TENANT-01",
        materialization_id=uuid4(),
        source_snapshot_id=snapshot_id,
        dataset_id=uuid4(),
        dataset_code="BOUNDARY-LABELS",
        dataset_version="1.0.0",
        plant_id="PLANT-01",
        catalog_name="ft_mes_bpi",
        catalog_namespace="bpi_training.tenant_deadbeefdeadbeef",
        table_name="dataset_01",
        table_identifier="ft_mes_bpi.bpi_training.tenant_deadbeefdeadbeef.dataset_01",
        publisher_version="bpi-dataset-catalog-publisher/0.1.0",
        manifest_checksum="a" * 64,
        source_content_sha256=hashlib.sha256(payload).hexdigest(),
        source_object_version_id="version-001",
        source_byte_size=len(payload),
        source_row_count=1,
        source_schema_json={"fields": fields},
        source_bucket="bpi-datasets",
        source_object_key="datasets/source.parquet",
        source_facts_verified=True,
        claim_token=uuid4(),
        revision=2,
        attempt_count=1,
        iceberg_snapshot_id=None,
        iceberg_metadata_location=None,
        iceberg_schema_id=None,
        iceberg_partition_spec_id=None,
    )
    return claim, payload


class SourceObjectStoreTest(unittest.TestCase):
    def test_semantic_checksum_uses_iceberg_string_semantics(self) -> None:
        small_string = pa.Table.from_arrays(
            [pa.array(["BATCH-001"], type=pa.string())],
            schema=pa.schema([pa.field("batch_id", pa.string(), nullable=True)]),
        )
        large_string = pa.Table.from_arrays(
            [pa.array(["BATCH-001"], type=pa.large_string())],
            schema=pa.schema(
                [pa.field("batch_id", pa.large_string(), nullable=True)]
            ),
        )

        self.assertEqual(
            semantic_checksum(small_string), semantic_checksum(large_string)
        )

    def test_exact_version_bytes_schema_and_identity_are_verified(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            claim, payload = parquet_fixture(directory)
            client = FakeMinio(payload, claim.source_object_version_id)
            verified = SourceObjectStore(settings(), client).download_verified(
                claim, directory / "download.parquet"
            )
        self.assertEqual(1, verified.table.num_rows)
        self.assertIn("source_materialization_id", verified.table.column_names)
        self.assertEqual(semantic_checksum(verified.table), verified.semantic_checksum)
        self.assertEqual(
            [
                ("stat", "bpi-datasets", "datasets/source.parquet", "version-001"),
                ("get", "bpi-datasets", "datasets/source.parquet", "version-001"),
            ],
            client.calls,
        )

    def test_wrong_exact_version_is_rejected_before_download(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            claim, payload = parquet_fixture(directory)
            client = FakeMinio(payload, "version-other")
            with self.assertRaises(SourceObjectContractError):
                SourceObjectStore(settings(), client).download_verified(
                    claim, directory / "download.parquet"
                )
        self.assertEqual(1, len(client.calls))

    def test_content_sha_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            claim, payload = parquet_fixture(directory)
            claim = replace(claim, source_content_sha256="f" * 64)
            with self.assertRaises(SourceObjectContractError):
                SourceObjectStore(
                    settings(), FakeMinio(payload, claim.source_object_version_id)
                ).download_verified(claim, directory / "download.parquet")


if __name__ == "__main__":
    unittest.main()
