from __future__ import annotations

import unittest
from dataclasses import replace
from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

import pyarrow as pa

from bpi_dataset_catalog_publisher.catalog import (
    CatalogContractError,
    IcebergCatalogPublisher,
)
from bpi_dataset_catalog_publisher.config import Settings
from bpi_dataset_catalog_publisher.models import PublicationClaim, VerifiedSource
from bpi_dataset_catalog_publisher.source_object import semantic_checksum


class FakeIcebergSchema:
    def __init__(self, schema: pa.Schema):
        self._schema = schema
        self._ids = {name: index + 1 for index, name in enumerate(schema.names)}
        self._names = {value: key for key, value in self._ids.items()}

    def as_arrow(self) -> pa.Schema:
        return self._schema

    def find_field(self, field_id: int):
        return SimpleNamespace(name=self._names[field_id])

    def id_for(self, name: str) -> int:
        return self._ids[name]


class FakeUpdateSpec:
    def __init__(self, table):
        self._table = table
        self._fields = []

    def add_identity(self, source: str):
        self._fields.append((source, "identity", source))
        return self

    def add_field(self, source: str, transform: str, name: str):
        self._fields.append((source, transform, name))
        return self

    def commit(self) -> None:
        self._table.set_partition_fields(self._fields)


class FakeTable:
    def __init__(self, schema: pa.Schema, properties: dict[str, str]):
        self.properties = dict(properties)
        self._schema = FakeIcebergSchema(schema)
        self._spec_fields = []
        self._snapshots = []
        self._rows = None
        self.append_count = 0
        self.metadata = SimpleNamespace(current_schema_id=0, default_spec_id=0)
        self.metadata_location = "s3://bpi-iceberg/table/metadata/v1.metadata.json"

    def schema(self):
        return self._schema

    def spec(self):
        return SimpleNamespace(fields=self._spec_fields)

    def set_partition_fields(self, fields) -> None:
        self._spec_fields = [
            SimpleNamespace(
                source_id=self._schema.id_for(source),
                transform=transform,
                name=name,
            )
            for source, transform, name in fields
        ]
        self.metadata.default_spec_id = 1

    def update_spec(self):
        return FakeUpdateSpec(self)

    def append(self, table: pa.Table, snapshot_properties: dict[str, str]):
        self.append_count += 1
        self._rows = table
        self._snapshots.append(
            SimpleNamespace(
                snapshot_id=7000 + self.append_count,
                schema_id=0,
                summary=SimpleNamespace(
                    additional_properties=dict(snapshot_properties)
                ),
            )
        )

    def refresh(self):
        return self

    def snapshots(self):
        return list(self._snapshots)

    def snapshot_by_id(self, snapshot_id: int):
        return next(
            (item for item in self._snapshots if item.snapshot_id == snapshot_id),
            None,
        )

    def scan(self, row_filter, snapshot_id: int):
        if self.snapshot_by_id(snapshot_id) is None:
            raise AssertionError("unknown snapshot")
        return SimpleNamespace(to_arrow=lambda: self._rows)


class FakeCatalog:
    def __init__(self, iceberg_arrow_strings: bool = False):
        self.table = None
        self.namespaces = []
        self.iceberg_arrow_strings = iceberg_arrow_strings

    def list_namespaces(self):
        return list(self.namespaces)

    def create_namespace_if_not_exists(self, namespace):
        value = tuple(namespace)
        if value not in self.namespaces:
            self.namespaces.append(value)

    def create_table_if_not_exists(self, identifier, schema, properties):
        if self.table is None:
            if self.iceberg_arrow_strings:
                schema = pa.schema(
                    [
                        pa.field(
                            field.name,
                            pa.large_string()
                            if pa.types.is_string(field.type)
                            else field.type,
                            nullable=field.nullable,
                        )
                        for field in schema
                    ]
                )
            self.table = FakeTable(schema, properties)
        return self.table

    def load_table(self, identifier):
        if self.table is None:
            raise AssertionError("table is missing")
        return self.table


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


def source_and_claim():
    materialization_id = uuid4()
    table = pa.Table.from_pylist(
        [
            {
                "batch_id": "BATCH-001",
                "prediction_time": datetime(2026, 7, 22, 4, 0, tzinfo=UTC),
                "plant_id": "PLANT-01",
                "source_materialization_id": str(materialization_id),
            }
        ],
        schema=pa.schema(
            [
                pa.field("batch_id", pa.string(), nullable=False),
                pa.field(
                    "prediction_time",
                    pa.timestamp("us", tz="UTC"),
                    nullable=False,
                ),
                pa.field("plant_id", pa.string(), nullable=False),
                pa.field("source_materialization_id", pa.string(), nullable=False),
            ]
        ),
    )
    claim = PublicationClaim(
        id=uuid4(),
        tenant_id="TENANT-01",
        materialization_id=materialization_id,
        source_snapshot_id=uuid4(),
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
        source_content_sha256="b" * 64,
        source_object_version_id="version-001",
        source_byte_size=4096,
        source_row_count=1,
        source_schema_json={"fields": []},
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
    return claim, VerifiedSource(
        path=Path("/tmp/source.parquet"),
        table=table,
        semantic_checksum=semantic_checksum(table),
    )


class IcebergCatalogPublisherTest(unittest.TestCase):
    def test_arrow_string_width_is_the_same_iceberg_schema_contract(self) -> None:
        catalog = FakeCatalog(iceberg_arrow_strings=True)
        publisher = IcebergCatalogPublisher(settings(), catalog)
        claim, source = source_and_claim()

        commit = publisher.ensure_commit(claim, source)
        verification = publisher.verify(claim, source, commit)

        self.assertEqual(1, verification.row_count)
        self.assertEqual(source.semantic_checksum, verification.semantic_checksum)

    def test_append_reconcile_and_exact_snapshot_verification(self) -> None:
        catalog = FakeCatalog()
        publisher = IcebergCatalogPublisher(settings(), catalog)
        claim, source = source_and_claim()

        commit = publisher.ensure_commit(claim, source)
        verification = publisher.verify(claim, source, commit)
        replay_commit = publisher.ensure_commit(claim, source)

        self.assertEqual(commit, replay_commit)
        self.assertEqual(1, catalog.table.append_count)
        self.assertEqual(1, verification.row_count)
        self.assertEqual(source.semantic_checksum, verification.semantic_checksum)
        self.assertTrue(verification.metadata["catalogSnapshotVerified"])
        self.assertFalse(verification.metadata["mlflowRegistered"])
        self.assertEqual(1, commit.partition_spec_id)
        self.assertEqual(
            [
                ("bpi_training",),
                ("bpi_training", "tenant_deadbeefdeadbeef"),
            ],
            catalog.namespaces,
        )

    def test_persisted_snapshot_must_reconcile_to_same_publication(self) -> None:
        catalog = FakeCatalog()
        publisher = IcebergCatalogPublisher(settings(), catalog)
        claim, source = source_and_claim()
        commit = publisher.ensure_commit(claim, source)
        drifted = replace(claim, iceberg_snapshot_id=commit.snapshot_id + 1)
        with self.assertRaises(CatalogContractError):
            publisher.ensure_commit(drifted, source)

    def test_snapshot_property_drift_is_rejected(self) -> None:
        catalog = FakeCatalog()
        publisher = IcebergCatalogPublisher(settings(), catalog)
        claim, source = source_and_claim()
        publisher.ensure_commit(claim, source)
        catalog.table._snapshots[0].summary.additional_properties[
            "bpi.source-content-sha256"
        ] = "f" * 64
        with self.assertRaises(CatalogContractError):
            publisher.ensure_commit(claim, source)


if __name__ == "__main__":
    unittest.main()
