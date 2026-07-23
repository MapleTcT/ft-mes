#!/usr/bin/env python3
"""Verify one target Parquet v2 artifact from PostgreSQL through exact MinIO bytes."""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from datetime import UTC, datetime
from decimal import Decimal
from pathlib import Path
from typing import Any

import psycopg
import pyarrow as pa
import pyarrow.parquet as pq
from minio import Minio
from psycopg.rows import dict_row


SCHEMA_VERSION = "bpi.dataset-parquet.v2"
MATERIALIZER_VERSION = "bpi-dataset-materializer/0.2.0"
PROCESS_WINDOW_COLUMN = "feature_process_window_values"
PROCESS_WINDOW_LAYOUT = "MAP_STRING_DECIMAL_24_6"
EXPECTED_VALUES = {
    "process.window.flow_instant.mean_60s": Decimal("20.000000"),
    "process.window.pump_running.true_ratio_30s": Decimal("0.500000"),
}


def required(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"{name} is required")
    return value


def boolean(name: str, default: bool = False) -> bool:
    value = os.getenv(name, str(default)).strip().lower()
    if value in {"true", "1", "yes", "on"}:
        return True
    if value in {"false", "0", "no", "off"}:
        return False
    raise RuntimeError(f"{name} must be true or false")


def load_target(marker: str) -> dict[str, Any]:
    database_url = os.getenv("BPI_DATASET_MATERIALIZER_DATABASE_URL", "").strip()
    connection_options: dict[str, Any] = {"row_factory": dict_row}
    if database_url:
        connection_options["conninfo"] = database_url
    with psycopg.connect(**connection_options) as connection:
        rows = connection.execute(
            """
            SELECT definition.id AS dataset_id,
                   snapshot.id AS snapshot_id,
                   snapshot.manifest_checksum AS snapshot_manifest_checksum,
                   materialization.id AS materialization_id,
                   materialization.state,
                   materialization.artifact_schema_version,
                   materialization.materializer_version,
                   materialization.manifest_checksum,
                   materialization.object_bucket,
                   materialization.object_key,
                   materialization.content_sha256,
                   materialization.byte_size,
                   materialization.row_count,
                   materialization.schema_json,
                   materialization.artifact_metadata
              FROM bpi.bpi_dataset_definitions definition
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = definition.tenant_id
               AND snapshot.dataset_id = definition.id
              JOIN bpi.bpi_dataset_materializations materialization
                ON materialization.tenant_id = snapshot.tenant_id
               AND materialization.snapshot_id = snapshot.id
             WHERE definition.tenant_id = '1000'
               AND definition.dataset_code = %s
               AND materialization.artifact_schema_version = %s
               AND materialization.materializer_version = %s
             ORDER BY snapshot.snapshot_version DESC, materialization.created_at DESC
            """,
            (marker, SCHEMA_VERSION, MATERIALIZER_VERSION),
        ).fetchall()
    if len(rows) != 1:
        raise RuntimeError(
            f"expected exactly one target v2 materialization, found {len(rows)}")
    return rows[0]


def verify_database(row: dict[str, Any]) -> tuple[str, list[str]]:
    if row["state"] != "READY":
        raise RuntimeError(f"materialization state is {row['state']}, expected READY")
    if row["manifest_checksum"] != row["snapshot_manifest_checksum"]:
        raise RuntimeError("materialization manifest checksum does not match the snapshot")
    if row["row_count"] != 1 or row["byte_size"] <= 0:
        raise RuntimeError("materialization row count or byte size is invalid")
    if not row["content_sha256"] or len(row["content_sha256"]) != 64:
        raise RuntimeError("materialization SHA-256 is invalid")

    schema = row["schema_json"]
    metadata = row["artifact_metadata"]
    if not isinstance(schema, dict) or not isinstance(metadata, dict):
        raise RuntimeError("materialization schema or artifact metadata is missing")
    refs = schema.get("processWindowFeatureRefs")
    if refs != sorted(EXPECTED_VALUES):
        raise RuntimeError(f"unexpected process-window refs: {refs}")
    map_fields = [
        field
        for field in schema.get("fields", [])
        if field.get("name") == PROCESS_WINDOW_COLUMN
    ]
    if map_fields != [{
        "name": PROCESS_WINDOW_COLUMN,
        "type": "map<string, decimal128(24, 6)>",
        "nullable": False,
    }]:
        raise RuntimeError(f"unexpected process-window field contract: {map_fields}")
    if metadata.get("processWindowFeatureLayout") != PROCESS_WINDOW_LAYOUT:
        raise RuntimeError("artifact process-window layout is invalid")
    if metadata.get("objectContentVerified") is not True:
        raise RuntimeError("artifact metadata does not prove exact object verification")
    version_id = metadata.get("objectVersionId")
    if not isinstance(version_id, str) or not version_id:
        raise RuntimeError("artifact objectVersionId is missing")
    return version_id, refs


def download_exact(row: dict[str, Any], version_id: str, destination: Path) -> None:
    client = Minio(
        required("BPI_DATASET_MINIO_ENDPOINT"),
        access_key=required("BPI_DATASET_MINIO_ACCESS_KEY"),
        secret_key=required("BPI_DATASET_MINIO_SECRET_KEY"),
        secure=boolean("BPI_DATASET_MINIO_SECURE"),
    )
    stat = client.stat_object(
        row["object_bucket"], row["object_key"], version_id=version_id)
    if stat.version_id != version_id:
        raise RuntimeError("MinIO returned a different object version")
    if stat.size != row["byte_size"]:
        raise RuntimeError("MinIO object size does not match PostgreSQL")

    response = client.get_object(
        row["object_bucket"], row["object_key"], version_id=version_id)
    digest = hashlib.sha256()
    size = 0
    try:
        with destination.open("wb") as output:
            for chunk in response.stream(1024 * 1024):
                output.write(chunk)
                digest.update(chunk)
                size += len(chunk)
    finally:
        response.close()
        response.release_conn()
    if size != row["byte_size"] or digest.hexdigest() != row["content_sha256"]:
        raise RuntimeError("downloaded exact object bytes do not match PostgreSQL")


def verify_parquet(row: dict[str, Any], path: Path) -> dict[str, Any]:
    table = pq.read_table(path)
    if table.num_rows != row["row_count"]:
        raise RuntimeError("Parquet row count does not match PostgreSQL")
    field = table.schema.field(PROCESS_WINDOW_COLUMN)
    if not pa.types.is_map(field.type):
        raise RuntimeError("process-window column is not an Arrow map")
    if not pa.types.is_string(field.type.key_type):
        raise RuntimeError("process-window map key is not string")
    item_type = field.type.item_type
    if not pa.types.is_decimal(item_type) \
            or item_type.precision != 24 or item_type.scale != 6:
        raise RuntimeError("process-window map value is not decimal128(24, 6)")

    metadata = table.schema.metadata or {}
    expected_metadata = {
        b"bpi.artifact_schema_version": SCHEMA_VERSION.encode(),
        b"bpi.materializer_version": MATERIALIZER_VERSION.encode(),
        b"bpi.snapshot_id": str(row["snapshot_id"]).encode(),
        b"bpi.manifest_checksum": row["manifest_checksum"].encode(),
        b"bpi.process_window_feature_layout": PROCESS_WINDOW_LAYOUT.encode(),
    }
    for key, expected in expected_metadata.items():
        if metadata.get(key) != expected:
            raise RuntimeError(f"Parquet metadata {key.decode()} does not match")

    values = table.column(PROCESS_WINDOW_COLUMN).to_pylist()
    expected = sorted(EXPECTED_VALUES.items())
    if any(sorted(row_values) != expected for row_values in values):
        raise RuntimeError(f"unexpected process-window values: {values}")
    return {
        "rowCount": table.num_rows,
        "fieldCount": len(table.schema),
        "processWindowValues": {
            reference: format(value, "f") for reference, value in expected
        },
        "metadataVerified": sorted(key.decode() for key in expected_metadata),
    }


def main() -> None:
    marker = required("BPI_ACCEPTANCE_MARKER")
    if not marker.replace("_", "").replace("-", "").isalnum():
        raise RuntimeError("BPI_ACCEPTANCE_MARKER contains unsupported characters")
    target = load_target(marker)
    version_id, refs = verify_database(target)
    with tempfile.TemporaryDirectory(prefix="bpi-parquet-v2-") as directory:
        path = Path(directory) / "target.parquet"
        download_exact(target, version_id, path)
        parquet = verify_parquet(target, path)

    report = {
        "generatedAt": datetime.now(UTC).isoformat().replace("+00:00", "Z"),
        "status": "PASS",
        "marker": marker,
        "database": "PostgreSQL",
        "datasetId": str(target["dataset_id"]),
        "snapshotId": str(target["snapshot_id"]),
        "materializationId": str(target["materialization_id"]),
        "artifactSchemaVersion": target["artifact_schema_version"],
        "materializerVersion": target["materializer_version"],
        "object": {
            "bucket": target["object_bucket"],
            "key": target["object_key"],
            "versionId": version_id,
            "contentSha256": target["content_sha256"],
            "byteSize": target["byte_size"],
        },
        "processWindowFeatureRefs": refs,
        "parquet": parquet,
        "cleanupRequired": True,
    }
    report_path = os.getenv("BPI_ACCEPTANCE_REPORT", "").strip()
    if report_path:
        destination = Path(report_path)
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    print(json.dumps(report, ensure_ascii=False, separators=(",", ":")))


if __name__ == "__main__":
    main()
