from __future__ import annotations

import hashlib
import json
import unittest
from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

import pyarrow as pa

from bpi_dataset_catalog_publisher.models import VerifiedSource
from bpi_dataset_catalog_publisher.recovery_rehearsal import (
    IcebergRecoveryRehearsal,
    RecoveryManifest,
    RecoveryPackage,
    RecoveryRehearsalError,
    RecoveryWarehouseCleaner,
    WarehouseObjectVersion,
)
from bpi_dataset_catalog_publisher.source_object import semantic_checksum


def canonical(value) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=True,
        separators=(",", ":"),
        sort_keys=True,
    ).encode()


def manifest_document() -> dict:
    archive_id = uuid4()
    publication_id = uuid4()
    materialization_id = uuid4()
    snapshot_id = uuid4()
    dataset_id = uuid4()
    return {
        "schemaVersion": "bpi.dataset-recovery.v1",
        "archive": {
            "id": str(archive_id),
            "profile": "bpi-dataset-recovery-v1",
            "archiverVersion": "bpi-dataset-retention-archiver/0.1.0",
            "catalogPublicationId": str(publication_id),
            "materializationId": str(materialization_id),
            "snapshotId": str(snapshot_id),
            "datasetId": str(dataset_id),
            "datasetCode": "BOUNDARY-LABELS",
            "datasetVersion": "1.0.0",
            "tenantId": "TENANT-01",
            "plantId": "PLANT-01",
            "lineIds": ["LINE-01"],
        },
        "source": {
            "manifestChecksum": "a" * 64,
            "contentSha256": "b" * 64,
            "originalObjectVersionId": "source-original-version",
            "byteSize": 1024,
            "rowCount": 1,
            "schema": {"fields": []},
            "retainedBucket": "bpi-dataset-recovery",
            "retainedObjectKey": "archives/test/source.parquet",
            "retainedObjectVersionId": "source-retained-version",
        },
        "iceberg": {
            "tableIdentifier": "ft_mes_bpi.bpi_training.tenant_01.dataset_01",
            "snapshotId": "7001",
            "metadataLocation": "s3://warehouse/metadata/v1.json",
            "schemaId": 0,
            "partitionSpecId": 1,
            "verifiedRowCount": 1,
            "semanticChecksum": "c" * 64,
        },
        "retention": {
            "mode": "GOVERNANCE",
            "retainUntil": "2026-07-23T12:00:00.000000Z",
            "legalHoldEnabled": False,
        },
    }


class FakeUpdateSpec:
    def __init__(self, table):
        self.table = table
        self.fields = []

    def add_identity(self, name):
        self.fields.append((name, "identity", name))
        return self

    def add_field(self, name, transform, output_name):
        self.fields.append((name, transform, output_name))
        return self

    def commit(self):
        self.table.spec_fields = list(self.fields)


class FakeRecoveryTable:
    def __init__(self, schema, properties):
        self.schema_value = schema
        self.properties = properties
        self.spec_fields = []
        self.rows = None
        self.snapshot = None

    def spec(self):
        return SimpleNamespace(fields=self.spec_fields)

    def update_spec(self):
        return FakeUpdateSpec(self)

    def append(self, table, snapshot_properties):
        self.rows = table
        self.snapshot = SimpleNamespace(
            snapshot_id=8001,
            summary=SimpleNamespace(additional_properties=snapshot_properties),
        )

    def refresh(self):
        return self

    def current_snapshot(self):
        return self.snapshot

    def scan(self, snapshot_id):
        if self.snapshot is None or snapshot_id != self.snapshot.snapshot_id:
            raise AssertionError("unknown snapshot")
        return SimpleNamespace(to_arrow=lambda: self.rows)

    def location(self):
        archive_id = self.properties["bpi.recovery-archive-id"].replace("-", "")
        return (
            "s3://bpi-iceberg-warehouse/warehouse/bpi_recovery/"
            f"archive_{archive_id}/dataset"
        )


class FakeRecoveryCatalog:
    def __init__(self):
        self.namespaces = set()
        self.tables = {}
        self.purged = []

    def namespace_exists(self, namespace):
        return tuple(namespace) in self.namespaces

    def create_namespace_if_not_exists(self, namespace):
        self.namespaces.add(tuple(namespace))

    def drop_namespace(self, namespace):
        self.namespaces.remove(tuple(namespace))

    def table_exists(self, identifier):
        return tuple(identifier) in self.tables

    def create_table(self, identifier, schema, properties):
        table = FakeRecoveryTable(schema, properties)
        self.tables[tuple(identifier)] = table
        return table

    def load_table(self, identifier):
        return self.tables[tuple(identifier)]

    def purge_table(self, identifier):
        self.purged.append(tuple(identifier))
        del self.tables[tuple(identifier)]

    def list_tables(self, namespace):
        prefix = tuple(namespace)
        return [identifier for identifier in self.tables if identifier[:-1] == prefix]

    def list_namespaces(self, namespace=()):
        prefix = tuple(namespace)
        depth = len(prefix) + 1
        return sorted(
            candidate
            for candidate in self.namespaces
            if len(candidate) == depth and candidate[: len(prefix)] == prefix
        )


class FakeWarehouseCleaner:
    def __init__(self, stale_versions=0):
        self.object_versions = stale_versions
        self.purge_counts = []

    def assert_empty(self, package):
        if self.object_versions:
            raise RecoveryRehearsalError(
                "isolated recovery warehouse prefix is not empty"
            )

    def has_objects(self, package):
        return self.object_versions > 0

    def validate_table(self, table, package):
        if (
            table.properties.get("bpi.recovery-archive-id")
            != str(package.manifest.archive_id)
            or table.properties.get("bpi.recovery-manifest-sha256")
            != package.manifest_sha256
        ):
            raise RecoveryRehearsalError("table identity mismatch")

    def validated_inventory(self, package, table=None):
        if table is not None:
            self.validate_table(table, package)
            if self.object_versions == 0:
                self.object_versions = 4
        if self.object_versions == 0:
            raise RecoveryRehearsalError("no warehouse objects")
        return tuple(
            WarehouseObjectVersion(
                object_key=f"warehouse/bpi_recovery/object-{index}",
                version_id=f"version-{index}",
                is_delete_marker=False,
            )
            for index in range(self.object_versions)
        )

    def purge_inventory(self, package, inventory):
        count = len(inventory)
        self.object_versions = 0
        self.purge_counts.append(count)
        return count


class FakeObjectResponse:
    def __init__(self, payload):
        self.payload = payload

    def read(self):
        return self.payload

    def close(self):
        return None

    def release_conn(self):
        return None


class FakeWarehouseMinio:
    def __init__(self, objects):
        self.objects = dict(objects)
        self.removed = []

    def list_objects(self, bucket, prefix, recursive, include_version):
        self.last_list = (bucket, prefix, recursive, include_version)
        return [
            SimpleNamespace(
                object_name=key,
                version_id=version_id,
                is_delete_marker=False,
            )
            for key, version_id in sorted(self.objects)
            if key.startswith(prefix)
        ]

    def get_object(self, bucket, object_key, version_id):
        return FakeObjectResponse(self.objects[(object_key, version_id)])

    def remove_object(self, bucket, object_key, version_id):
        self.removed.append((bucket, object_key, version_id))
        del self.objects[(object_key, version_id)]


def recovery_package() -> RecoveryPackage:
    document = manifest_document()
    table = pa.Table.from_pylist(
        [
            {
                "prediction_time": datetime(2026, 7, 22, 4, 0, tzinfo=UTC),
                "plant_id": "PLANT-01",
                "source_materialization_id": document["archive"]["materializationId"],
            }
        ],
        schema=pa.schema(
            [
                pa.field(
                    "prediction_time", pa.timestamp("us", tz="UTC"), nullable=False
                ),
                pa.field("plant_id", pa.string(), nullable=False),
                pa.field("source_materialization_id", pa.string(), nullable=False),
            ]
        ),
    )
    document["iceberg"]["semanticChecksum"] = semantic_checksum(table)
    payload = canonical(document)
    manifest = RecoveryManifest.from_bytes(
        payload, hashlib.sha256(payload).hexdigest()
    )
    return RecoveryPackage(
        manifest=manifest,
        source=VerifiedSource(
            path=Path("/tmp/source.parquet"),
            table=table,
            semantic_checksum=semantic_checksum(table),
        ),
        manifest_sha256=hashlib.sha256(payload).hexdigest(),
        manifest_object_key="archives/test/recovery-manifest.json",
        manifest_version_id="manifest-version",
    )


class RecoveryRehearsalTest(unittest.TestCase):
    def test_manifest_requires_exact_sha_and_canonical_contract(self) -> None:
        document = manifest_document()
        payload = canonical(document)
        sha = hashlib.sha256(payload).hexdigest()

        parsed = RecoveryManifest.from_bytes(payload, sha)

        self.assertEqual("bpi-dataset-recovery-v1", parsed.archive_profile)
        with self.assertRaisesRegex(RecoveryRehearsalError, "SHA-256"):
            RecoveryManifest.from_bytes(payload, "f" * 64)
        with self.assertRaisesRegex(RecoveryRehearsalError, "canonical"):
            RecoveryManifest.from_bytes(
                json.dumps(document, indent=2).encode(),
                hashlib.sha256(json.dumps(document, indent=2).encode()).hexdigest(),
            )

    def test_restore_time_travel_and_mandatory_purge(self) -> None:
        catalog = FakeRecoveryCatalog()
        warehouse = FakeWarehouseCleaner()
        package = recovery_package()

        result = IcebergRecoveryRehearsal(catalog, warehouse).restore(package)

        self.assertEqual("PASS", result["status"])
        self.assertTrue(result["timeTravelVerified"])
        self.assertTrue(result["purged"])
        self.assertTrue(result["physicalPurgeVerified"])
        self.assertEqual(4, result["warehouseObjectVersionsPurged"])
        self.assertTrue(result["namespaceCleanupVerified"])
        self.assertEqual(1, result["verifiedRowCount"])
        self.assertEqual(package.source.semantic_checksum, result["verifiedSemanticChecksum"])
        self.assertEqual(1, len(catalog.purged))
        self.assertEqual({}, catalog.tables)
        self.assertEqual(set(), catalog.namespaces)
        self.assertEqual([4], warehouse.purge_counts)

    def test_existing_recovery_table_is_never_overwritten(self) -> None:
        catalog = FakeRecoveryCatalog()
        package = recovery_package()
        namespace = ("bpi_recovery", f"archive_{package.manifest.archive_id.hex}")
        catalog.namespaces.update({("bpi_recovery",), namespace})
        catalog.tables[(*namespace, "dataset")] = object()

        with self.assertRaisesRegex(RecoveryRehearsalError, "refusing to overwrite"):
            IcebergRecoveryRehearsal(catalog, FakeWarehouseCleaner()).restore(package)

        self.assertEqual([], catalog.purged)
        self.assertIn((*namespace, "dataset"), catalog.tables)

    def test_stale_physical_prefix_requires_explicit_reconcile(self) -> None:
        package = recovery_package()

        with self.assertRaisesRegex(RecoveryRehearsalError, "explicit reconcile"):
            IcebergRecoveryRehearsal(
                FakeRecoveryCatalog(),
                FakeWarehouseCleaner(stale_versions=2),
            ).restore(package)

    def test_explicit_reconcile_purges_same_archive_before_rehearsal(self) -> None:
        catalog = FakeRecoveryCatalog()
        warehouse = FakeWarehouseCleaner(stale_versions=2)
        package = recovery_package()

        result = IcebergRecoveryRehearsal(
            catalog,
            warehouse,
            reconcile_stale=True,
        ).restore(package)

        self.assertTrue(result["reconciledStaleRecovery"])
        self.assertEqual(2, result["reconciledStaleObjectVersions"])
        self.assertEqual([2, 4], warehouse.purge_counts)

    def test_physical_cleaner_validates_metadata_and_deletes_exact_versions(self) -> None:
        package = recovery_package()
        archive_hex = package.manifest.archive_id.hex
        prefix = f"warehouse/bpi_recovery/archive_{archive_hex}/dataset/"
        location = f"s3://bpi-iceberg-warehouse/{prefix.rstrip('/')}"
        metadata = json.dumps(
            {
                "location": location,
                "properties": {
                    "bpi.recovery-archive-id": str(package.manifest.archive_id),
                    "bpi.recovery-manifest-sha256": package.manifest_sha256,
                },
            }
        ).encode()
        client = FakeWarehouseMinio(
            {
                (f"{prefix}metadata/00000.metadata.json", "metadata-v1"): metadata,
                (f"{prefix}data/00000.parquet", "data-v1"): b"parquet",
            }
        )
        cleaner = RecoveryWarehouseCleaner(client, "bpi-iceberg-warehouse")

        inventory = cleaner.validated_inventory(package)
        purged = cleaner.purge_inventory(package, inventory)

        self.assertEqual(2, purged)
        self.assertEqual({}, client.objects)
        self.assertEqual(2, len(client.removed))
        cleaner.assert_empty(package)

    def test_physical_cleaner_refuses_foreign_metadata(self) -> None:
        package = recovery_package()
        archive_hex = package.manifest.archive_id.hex
        prefix = f"warehouse/bpi_recovery/archive_{archive_hex}/dataset/"
        metadata = json.dumps(
            {
                "location": (
                    f"s3://bpi-iceberg-warehouse/{prefix.rstrip('/')}"
                ),
                "properties": {
                    "bpi.recovery-archive-id": str(package.manifest.archive_id),
                    "bpi.recovery-manifest-sha256": "f" * 64,
                },
            }
        ).encode()
        client = FakeWarehouseMinio(
            {(f"{prefix}metadata/00000.metadata.json", "metadata-v1"): metadata}
        )

        with self.assertRaisesRegex(RecoveryRehearsalError, "does not belong"):
            RecoveryWarehouseCleaner(
                client,
                "bpi-iceberg-warehouse",
            ).validated_inventory(package)

        self.assertEqual(1, len(client.objects))
        self.assertEqual([], client.removed)


if __name__ == "__main__":
    unittest.main()
