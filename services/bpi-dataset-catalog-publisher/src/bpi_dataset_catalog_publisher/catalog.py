from __future__ import annotations

from typing import Any

import pyarrow as pa
from pyiceberg.catalog import Catalog, load_catalog
from pyiceberg.expressions import EqualTo

from .config import Settings
from .models import (
    CatalogCommit,
    CatalogVerification,
    PublicationClaim,
    VerifiedSource,
)
from .source_object import iceberg_schema_contract, semantic_checksum


class CatalogContractError(RuntimeError):
    pass


SNAPSHOT_PROPERTIES = {
    "publication": "bpi.publication-id",
    "materialization": "bpi.materialization-id",
    "source_snapshot": "bpi.source-snapshot-id",
    "manifest": "bpi.manifest-checksum",
    "source_sha": "bpi.source-content-sha256",
    "source_version": "bpi.source-object-version-id",
    "publisher": "bpi.publisher-version",
}


class IcebergCatalogPublisher:
    def __init__(self, settings: Settings, catalog: Catalog | None = None):
        self._settings = settings
        self._catalog = catalog or load_catalog(
            settings.catalog_name,
            type="rest",
            uri=settings.catalog_uri,
            warehouse=settings.catalog_warehouse,
            credential=settings.catalog_credential,
            scope="PRINCIPAL_ROLE:ALL",
            **{
                "oauth2-server-uri": settings.catalog_oauth2_server_uri,
                "header.Polaris-Realm": settings.catalog_realm,
                "py-io-impl": "pyiceberg.io.pyarrow.PyArrowFileIO",
                "s3.endpoint": settings.iceberg_s3_endpoint,
                "s3.region": settings.iceberg_s3_region,
                "s3.resolve-region": "false",
                "s3.force-virtual-addressing": "false",
            },
        )

    def ping(self) -> None:
        self._catalog.list_namespaces()

    def ensure_commit(
        self,
        claim: PublicationClaim,
        source: VerifiedSource,
    ) -> CatalogCommit:
        namespace = tuple(claim.catalog_namespace.split("."))
        identifier = (*namespace, claim.table_name)
        self._ensure_namespace(namespace)
        table_properties = {
            "format-version": "2",
            "write.parquet.compression-codec": "zstd",
            "write.target-file-size-bytes": str(256 * 1024 * 1024),
            "bpi.dataset-id": str(claim.dataset_id),
            "bpi.publisher-version": claim.publisher_version,
        }
        table = self._catalog.create_table_if_not_exists(
            identifier,
            source.table.schema,
            properties=table_properties,
        )
        self._ensure_partition_spec(table)
        self._validate_table(claim, source, table)

        existing = self._find_snapshot(table, claim)
        if claim.iceberg_snapshot_id is not None:
            if existing is None or existing.snapshot_id != claim.iceberg_snapshot_id:
                raise CatalogContractError(
                    "persisted Iceberg snapshot cannot be reconciled by publication identity"
                )
            return self._commit(table, existing)
        if existing is not None:
            return self._commit(table, existing)

        table.append(
            source.table,
            snapshot_properties=self._snapshot_properties(claim),
        )
        table.refresh()
        committed = self._find_snapshot(table, claim)
        if committed is None:
            raise CatalogContractError(
                "Iceberg append returned without a publication snapshot"
            )
        return self._commit(table, committed)

    def verify(
        self,
        claim: PublicationClaim,
        source: VerifiedSource,
        commit: CatalogCommit,
    ) -> CatalogVerification:
        table = self._catalog.load_table(self._identifier(claim))
        snapshot = table.snapshot_by_id(commit.snapshot_id)
        if snapshot is None:
            raise CatalogContractError("committed Iceberg snapshot is not queryable")
        self._validate_snapshot(snapshot, claim)
        scanned = table.scan(
            row_filter=EqualTo(
                "source_materialization_id", str(claim.materialization_id)
            ),
            snapshot_id=commit.snapshot_id,
        ).to_arrow()
        actual_checksum = semantic_checksum(scanned)
        if scanned.num_rows != claim.source_row_count:
            raise CatalogContractError(
                "Iceberg snapshot row count does not match the source publication"
            )
        if actual_checksum != source.semantic_checksum:
            raise CatalogContractError(
                "Iceberg snapshot semantic checksum does not match the source Parquet"
            )
        return CatalogVerification(
            commit=commit,
            row_count=scanned.num_rows,
            semantic_checksum=actual_checksum,
            metadata={
                "catalogSnapshotVerified": True,
                "sourceVersionVerified": True,
                "manifestChecksumVerified": True,
                "publicationId": str(claim.id),
                "materializationId": str(claim.materialization_id),
                "sourceSnapshotId": str(claim.source_snapshot_id),
                "sourceObjectVersionId": claim.source_object_version_id,
                "tableIdentifier": claim.table_identifier,
                "publisherVersion": claim.publisher_version,
                "icebergReady": True,
                "mlflowRegistered": False,
                "modelTrained": False,
            },
        )

    def _validate_table(self, claim: PublicationClaim, source: VerifiedSource, table) -> None:
        if table.properties.get("bpi.dataset-id") != str(claim.dataset_id):
            raise CatalogContractError("Iceberg table dataset identity does not match")
        if table.properties.get("bpi.publisher-version") != claim.publisher_version:
            raise CatalogContractError("Iceberg table publisher contract does not match")
        if iceberg_schema_contract(
            table.schema().as_arrow()
        ) != iceberg_schema_contract(source.table.schema):
            raise CatalogContractError("Iceberg table schema does not match the source")
        actual_spec = [
            (
                table.schema().find_field(field.source_id).name,
                str(field.transform),
                field.name,
            )
            for field in table.spec().fields
        ]
        if actual_spec != [
            ("plant_id", "identity", "plant_id"),
            ("prediction_time", "day", "prediction_day"),
        ]:
            raise CatalogContractError("Iceberg table partition spec does not match")

    def _ensure_partition_spec(self, table) -> None:
        fields = table.spec().fields
        if fields:
            return
        (
            table.update_spec()
            .add_identity("plant_id")
            .add_field("prediction_time", "day", "prediction_day")
            .commit()
        )
        table.refresh()

    def _ensure_namespace(self, namespace: tuple[str, ...]) -> None:
        for depth in range(1, len(namespace) + 1):
            self._catalog.create_namespace_if_not_exists(namespace[:depth])

    def _find_snapshot(self, table, claim: PublicationClaim):
        matches = []
        for snapshot in table.snapshots():
            summary = snapshot.summary
            if summary is None:
                continue
            if summary.additional_properties.get(
                SNAPSHOT_PROPERTIES["publication"]
            ) == str(claim.id):
                self._validate_snapshot(snapshot, claim)
                matches.append(snapshot)
        if len(matches) > 1:
            raise CatalogContractError(
                "multiple Iceberg snapshots use the same publication identity"
            )
        return matches[0] if matches else None

    def _validate_snapshot(self, snapshot, claim: PublicationClaim) -> None:
        if snapshot.summary is None:
            raise CatalogContractError("Iceberg publication snapshot has no summary")
        actual = snapshot.summary.additional_properties
        expected = self._snapshot_properties(claim)
        mismatches = [key for key, value in expected.items() if actual.get(key) != value]
        if mismatches:
            raise CatalogContractError(
                f"Iceberg publication snapshot properties differ: {sorted(mismatches)}"
            )

    def _commit(self, table, snapshot) -> CatalogCommit:
        schema_id = (
            snapshot.schema_id
            if snapshot.schema_id is not None
            else table.metadata.current_schema_id
        )
        if schema_id is None:
            raise CatalogContractError("Iceberg snapshot schema id is missing")
        return CatalogCommit(
            snapshot_id=snapshot.snapshot_id,
            metadata_location=table.metadata_location,
            schema_id=schema_id,
            partition_spec_id=table.metadata.default_spec_id,
        )

    def _snapshot_properties(self, claim: PublicationClaim) -> dict[str, str]:
        return {
            SNAPSHOT_PROPERTIES["publication"]: str(claim.id),
            SNAPSHOT_PROPERTIES["materialization"]: str(claim.materialization_id),
            SNAPSHOT_PROPERTIES["source_snapshot"]: str(claim.source_snapshot_id),
            SNAPSHOT_PROPERTIES["manifest"]: claim.manifest_checksum,
            SNAPSHOT_PROPERTIES["source_sha"]: claim.source_content_sha256,
            SNAPSHOT_PROPERTIES["source_version"]: claim.source_object_version_id,
            SNAPSHOT_PROPERTIES["publisher"]: claim.publisher_version,
        }

    def _identifier(self, claim: PublicationClaim) -> tuple[str, ...]:
        return (*claim.catalog_namespace.split("."), claim.table_name)
