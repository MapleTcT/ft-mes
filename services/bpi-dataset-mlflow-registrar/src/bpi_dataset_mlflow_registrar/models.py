from __future__ import annotations

from dataclasses import dataclass
from typing import Any
from uuid import UUID


@dataclass(frozen=True)
class RegistrationClaim:
    id: UUID
    tenant_id: str
    retention_archive_id: UUID
    catalog_publication_id: UUID
    materialization_id: UUID
    source_snapshot_id: UUID
    dataset_id: UUID
    dataset_code: str
    dataset_version: str
    plant_id: str
    line_ids: tuple[str, ...]
    registrar_version: str
    tracking_profile: str
    manifest_checksum: str
    source_content_sha256: str
    source_object_version_id: str
    source_byte_size: int
    source_row_count: int
    source_schema_json: dict[str, Any]
    table_identifier: str
    iceberg_snapshot_id: int
    catalog_semantic_checksum: str
    archive_bucket: str
    source_archive_object_key: str
    source_archive_version_id: str
    archive_manifest_object_key: str
    archive_manifest_version_id: str
    archive_manifest_sha256: str
    experiment_name: str
    dataset_name: str
    dataset_digest: str
    source_facts_verified: bool
    claim_token: UUID
    revision: int
    attempt_count: int

    @property
    def source_uri(self) -> str:
        return (
            f"s3://{self.archive_bucket}/{self.source_archive_object_key}"
            f"?versionId={self.source_archive_version_id}"
        )


@dataclass(frozen=True)
class RegistrationResult:
    experiment_id: str
    run_id: str
    artifact_uri: str
    dataset_source: str
    metadata: dict[str, Any]
