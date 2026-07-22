from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any
from uuid import UUID

import pyarrow as pa


@dataclass(frozen=True)
class PublicationClaim:
    id: UUID
    tenant_id: str
    materialization_id: UUID
    source_snapshot_id: UUID
    dataset_id: UUID
    dataset_code: str
    dataset_version: str
    plant_id: str
    catalog_name: str
    catalog_namespace: str
    table_name: str
    table_identifier: str
    publisher_version: str
    manifest_checksum: str
    source_content_sha256: str
    source_object_version_id: str
    source_byte_size: int
    source_row_count: int
    source_schema_json: dict[str, Any]
    source_bucket: str
    source_object_key: str
    source_facts_verified: bool
    claim_token: UUID
    revision: int
    attempt_count: int
    iceberg_snapshot_id: int | None
    iceberg_metadata_location: str | None
    iceberg_schema_id: int | None
    iceberg_partition_spec_id: int | None


@dataclass(frozen=True)
class VerifiedSource:
    path: Path
    table: pa.Table
    semantic_checksum: str


@dataclass(frozen=True)
class CatalogCommit:
    snapshot_id: int
    metadata_location: str
    schema_id: int
    partition_spec_id: int


@dataclass(frozen=True)
class CatalogVerification:
    commit: CatalogCommit
    row_count: int
    semantic_checksum: str
    metadata: dict[str, Any]
