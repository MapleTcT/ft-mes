from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any
from uuid import UUID


@dataclass(frozen=True)
class MaterializationClaim:
    id: UUID
    tenant_id: str
    snapshot_id: UUID
    dataset_id: UUID
    dataset_code: str
    dataset_version: str
    plant_id: str
    artifact_schema_version: str
    materializer_version: str
    manifest_checksum: str
    manifest_schema_version: str
    definition_checksum: str
    included_count: int
    feature_refs: tuple[str, ...]
    label_refs: tuple[str, ...]
    claim_token: UUID
    revision: int
    attempt_count: int


@dataclass(frozen=True)
class DatasetSample:
    snapshot_id: UUID
    review_id: UUID
    shadow_run_id: UUID
    batch_id: UUID
    batch_no: str
    line_id: str
    prediction_time: datetime
    feature_cutoff: datetime
    label_available_at: datetime
    confidence: Any
    split_key: str
    feature_payload: dict[str, Any]
    label_payload: dict[str, Any]


@dataclass(frozen=True)
class ParquetArtifact:
    path: Path
    content_sha256: str
    byte_size: int
    row_count: int
    schema_json: dict[str, Any]
    metadata: dict[str, Any]


@dataclass(frozen=True)
class StoredObject:
    bucket: str
    key: str
    uri: str
    byte_size: int
    version_id: str
