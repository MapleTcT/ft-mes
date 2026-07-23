from __future__ import annotations

import hashlib
import json
import math
from datetime import UTC, date, datetime
from decimal import Decimal
from pathlib import Path
from typing import Any

import pyarrow as pa
import pyarrow.parquet as pq
from minio import Minio

from .config import Settings
from .models import PublicationClaim, VerifiedSource


class SourceObjectContractError(RuntimeError):
    pass


def _schema_contract(schema: pa.Schema) -> list[dict[str, Any]]:
    return [
        {
            "name": field.name,
            "type": _parquet_logical_type(field.type),
            "nullable": field.nullable,
        }
        for field in schema
    ]


def _parquet_logical_type(data_type: pa.DataType) -> str:
    """Normalize nested physical field aliases added during Parquet round trips."""
    if pa.types.is_map(data_type):
        return (
            "map<"
            f"{_parquet_logical_type(data_type.key_type)}, "
            f"{_parquet_logical_type(data_type.item_type)}"
            ">"
        )
    if pa.types.is_list(data_type):
        return f"list<{_parquet_logical_type(data_type.value_type)}>"
    if pa.types.is_large_list(data_type):
        return f"large_list<{_parquet_logical_type(data_type.value_type)}>"
    if pa.types.is_struct(data_type):
        fields = ", ".join(
            f"{field.name}: {_parquet_logical_type(field.type)}"
            for field in data_type
        )
        return f"struct<{fields}>"
    return str(data_type)


def iceberg_schema_contract(schema: pa.Schema) -> list[tuple[str, str, bool]]:
    """Return the logical Iceberg contract represented by an Arrow schema."""
    return [
        (field.name, _iceberg_logical_type(field.type), field.nullable)
        for field in schema
    ]


def _iceberg_logical_type(data_type: pa.DataType) -> str:
    # PyIceberg exposes Iceberg string/binary columns as Arrow large_* types.
    if pa.types.is_string(data_type) or pa.types.is_large_string(data_type):
        return "string"
    if pa.types.is_binary(data_type) or pa.types.is_large_binary(data_type):
        return "binary"
    if pa.types.is_map(data_type):
        return (
            "map<"
            f"{_iceberg_logical_type(data_type.key_type)},"
            f"{_iceberg_logical_type(data_type.item_type)}"
            ">"
        )
    if pa.types.is_list(data_type) or pa.types.is_large_list(data_type):
        return f"list<{_iceberg_logical_type(data_type.value_type)}>"
    if pa.types.is_struct(data_type):
        fields = ",".join(
            f"{field.name}:{_iceberg_logical_type(field.type)}"
            for field in data_type
        )
        return f"struct<{fields}>"
    return str(data_type)


def _canonical_value(value: Any) -> Any:
    if value is None or isinstance(value, (str, int, bool)):
        return value
    if isinstance(value, float):
        if not math.isfinite(value):
            raise SourceObjectContractError("non-finite values cannot be checksummed")
        return format(value, ".17g")
    if isinstance(value, Decimal):
        return format(value, "f")
    if isinstance(value, datetime):
        if value.tzinfo is None:
            raise SourceObjectContractError("naive timestamps cannot be checksummed")
        return value.astimezone(UTC).isoformat(timespec="microseconds").replace(
            "+00:00", "Z"
        )
    if isinstance(value, date):
        return value.isoformat()
    if isinstance(value, bytes):
        return value.hex()
    if isinstance(value, dict):
        return {
            str(key): _canonical_value(item)
            for key, item in sorted(value.items(), key=lambda entry: str(entry[0]))
        }
    if isinstance(value, (list, tuple)):
        return [_canonical_value(item) for item in value]
    return str(value)


def semantic_checksum(table: pa.Table) -> str:
    normalized = table.combine_chunks().replace_schema_metadata(None)
    digest = hashlib.sha256()
    schema_payload = json.dumps(
        iceberg_schema_contract(normalized.schema),
        ensure_ascii=True,
        separators=(",", ":"),
        sort_keys=True,
    ).encode()
    digest.update(len(schema_payload).to_bytes(8, "big"))
    digest.update(schema_payload)
    rows = [
        json.dumps(
            _canonical_value(row),
            ensure_ascii=True,
            separators=(",", ":"),
            sort_keys=True,
        ).encode()
        for row in normalized.to_pylist()
    ]
    for row in sorted(rows):
        digest.update(len(row).to_bytes(8, "big"))
        digest.update(row)
    return digest.hexdigest()


class SourceObjectStore:
    def __init__(self, settings: Settings, client: Minio | None = None):
        if not settings.minio_endpoint:
            raise ValueError("MinIO endpoint is required")
        if not settings.minio_access_key or not settings.minio_secret_key:
            raise ValueError("MinIO credentials are required")
        self._client = client or Minio(
            settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
        )

    def download_verified(
        self,
        claim: PublicationClaim,
        destination: Path,
    ) -> VerifiedSource:
        self.download_exact(claim, destination)
        return validate_local_source(claim, destination)

    def download_exact(
        self,
        claim: PublicationClaim,
        destination: Path,
    ) -> Path:
        stat = self._client.stat_object(
            claim.source_bucket,
            claim.source_object_key,
            version_id=claim.source_object_version_id,
        )
        actual_version = getattr(stat, "version_id", None)
        if actual_version != claim.source_object_version_id:
            raise SourceObjectContractError("source object version id does not match")
        if stat.size != claim.source_byte_size:
            raise SourceObjectContractError(
                f"source object size changed: {stat.size} != {claim.source_byte_size}"
            )

        destination.parent.mkdir(parents=True, exist_ok=True)
        response = self._client.get_object(
            claim.source_bucket,
            claim.source_object_key,
            version_id=claim.source_object_version_id,
        )
        digest = hashlib.sha256()
        byte_size = 0
        try:
            with destination.open("wb") as output:
                for chunk in response.stream(amt=1024 * 1024):
                    output.write(chunk)
                    digest.update(chunk)
                    byte_size += len(chunk)
        finally:
            response.close()
            response.release_conn()
        if byte_size != claim.source_byte_size:
            raise SourceObjectContractError(
                f"downloaded source size changed: {byte_size} != {claim.source_byte_size}"
            )
        if digest.hexdigest() != claim.source_content_sha256:
            raise SourceObjectContractError("source object SHA-256 does not match")

        return destination


def validate_local_source(
    claim: PublicationClaim,
    source_path: Path,
) -> VerifiedSource:
    """Validate and enrich an already downloaded exact Parquet object version."""
    parquet_file = pq.ParquetFile(source_path)
    source_table = parquet_file.read()
    if source_table.num_rows != claim.source_row_count:
        raise SourceObjectContractError(
            "source Parquet row count does not match the frozen publication"
        )
    expected_fields = claim.source_schema_json.get("fields")
    if not isinstance(expected_fields, list):
        raise SourceObjectContractError("frozen source schema has no fields contract")
    if _schema_contract(source_table.schema) != expected_fields:
        raise SourceObjectContractError("source Parquet schema does not match")

    metadata = source_table.schema.metadata or {}
    expected_metadata = {
        b"bpi.snapshot_id": str(claim.source_snapshot_id).encode(),
        b"bpi.manifest_checksum": claim.manifest_checksum.encode(),
    }
    for key, expected in expected_metadata.items():
        if metadata.get(key) != expected:
            raise SourceObjectContractError(
                f"source Parquet metadata {key.decode()} does not match"
            )
    if "snapshot_id" not in source_table.column_names:
        raise SourceObjectContractError("source Parquet snapshot_id column is missing")
    if set(source_table.column("snapshot_id").to_pylist()) != {
        str(claim.source_snapshot_id)
    }:
        raise SourceObjectContractError("source Parquet contains another snapshot")

    enriched = source_table
    identity_columns = {
        "tenant_id": claim.tenant_id,
        "plant_id": claim.plant_id,
        "dataset_id": str(claim.dataset_id),
        "source_snapshot_id": str(claim.source_snapshot_id),
        "source_materialization_id": str(claim.materialization_id),
        "source_content_sha256": claim.source_content_sha256,
    }
    collisions = set(identity_columns).intersection(enriched.column_names)
    if collisions:
        raise SourceObjectContractError(
            f"source Parquet already defines publisher columns: {sorted(collisions)}"
        )
    for name, value in identity_columns.items():
        enriched = enriched.append_column(
            name,
            pa.array([value] * enriched.num_rows, type=pa.string()),
        )
    enriched = enriched.replace_schema_metadata(None)
    return VerifiedSource(
        path=source_path,
        table=enriched,
        semantic_checksum=semantic_checksum(enriched),
    )
