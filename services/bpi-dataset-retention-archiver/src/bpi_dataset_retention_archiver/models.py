from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any
from uuid import UUID


@dataclass(frozen=True)
class ArchiveClaim:
    id: UUID
    tenant_id: str
    catalog_publication_id: UUID
    materialization_id: UUID
    source_snapshot_id: UUID
    dataset_id: UUID
    dataset_code: str
    dataset_version: str
    plant_id: str
    line_ids: tuple[str, ...]
    archiver_version: str
    archive_profile: str
    manifest_checksum: str
    source_content_sha256: str
    source_object_version_id: str
    source_byte_size: int
    source_row_count: int
    source_schema_json: dict[str, Any]
    table_identifier: str
    iceberg_snapshot_id: int
    iceberg_metadata_location: str
    iceberg_schema_id: int
    iceberg_partition_spec_id: int
    catalog_verified_row_count: int
    catalog_semantic_checksum: str
    source_bucket: str
    source_object_key: str
    source_facts_verified: bool
    claim_token: UUID
    revision: int
    attempt_count: int
    retention_mode: str
    retain_until: datetime
    legal_hold_enabled: bool
    archive_bucket: str | None
    archive_prefix: str | None
    source_archive_object_key: str | None
    source_archive_version_id: str | None
    archive_manifest_object_key: str | None
    archive_manifest_version_id: str | None
    archive_manifest_sha256: str | None


@dataclass(frozen=True)
class RetainedObjectVersion:
    object_key: str
    version_id: str
    content_sha256: str
    byte_size: int


@dataclass(frozen=True)
class ArchiveBundle:
    bucket: str
    prefix: str
    source: RetainedObjectVersion
    manifest: RetainedObjectVersion
    object_count: int
    total_bytes: int


@dataclass(frozen=True)
class ArchiveVerification:
    bundle: ArchiveBundle
    recovered_source_path: Path
    row_count: int
    semantic_checksum: str
    metadata: dict[str, Any]
