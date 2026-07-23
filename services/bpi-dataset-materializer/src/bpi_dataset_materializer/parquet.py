from __future__ import annotations

import hashlib
import json
from datetime import datetime
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any, Callable

import pyarrow as pa
import pyarrow.parquet as pq

from .models import DatasetSample, MaterializationClaim, ParquetArtifact


class DatasetContractError(RuntimeError):
    pass


ROW_ORDER = "line_id,prediction_time,batch_id,review_id"
PARQUET_V1 = "bpi.dataset-parquet.v1"
PARQUET_V2 = "bpi.dataset-parquet.v2"
MATERIALIZER_V1 = "bpi-dataset-materializer/0.1.0"
MATERIALIZER_V2 = "bpi-dataset-materializer/0.2.0"
PROCESS_WINDOW_PREFIX = "process.window."
PROCESS_WINDOW_COLUMN = "feature_process_window_values"
PROCESS_WINDOW_LAYOUT = "MAP_STRING_DECIMAL_24_6"


def _canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=True, separators=(",", ":"), sort_keys=True)


def _text(value: Any) -> str | None:
    return None if value is None else str(value)


def _timestamp(value: Any) -> datetime | None:
    if value is None or isinstance(value, datetime):
        return value
    return datetime.fromisoformat(str(value).replace("Z", "+00:00"))


def _decimal(value: Any) -> Decimal | None:
    if value is None:
        return None
    return Decimal(str(value)).quantize(Decimal("0.000001"))


def _process_window_decimal(reference: str, value: Any) -> Decimal:
    if value is None or isinstance(value, bool):
        raise DatasetContractError(
            f"process window feature {reference} must be a finite numeric value")
    try:
        converted = Decimal(str(value))
        if not converted.is_finite():
            raise InvalidOperation
        return converted.quantize(Decimal("0.000001"))
    except (InvalidOperation, ValueError) as exception:
        raise DatasetContractError(
            f"process window feature {reference} must be a finite numeric value"
        ) from exception


def _boolean(value: Any) -> bool | None:
    if value is None or isinstance(value, bool):
        return value
    raise DatasetContractError("boolean dataset value must be true, false, or null")


FEATURE_COLUMNS: dict[str, tuple[str, pa.DataType, Callable[[Any], Any]]] = {
    "batch.order_id": ("feature_batch_order_id", pa.string(), _text),
    "batch.material_code": ("feature_batch_material_code", pa.string(), _text),
    "batch.stage_code": ("feature_batch_stage_code", pa.string(), _text),
    "batch.quantity_unit": ("feature_batch_quantity_unit", pa.string(), _text),
    "rule.version_id": ("feature_rule_version_id", pa.string(), _text),
    "topology.version_id": ("feature_topology_version_id", pa.string(), _text),
    "point_catalog.snapshot_id": (
        "feature_point_catalog_snapshot_id", pa.string(), _text),
}

LABEL_COLUMNS: dict[str, tuple[tuple[str, pa.DataType, Callable[[Any], Any]], ...]] = {
    "review.manual_start_time": ((
        "label_review_manual_start_time", pa.timestamp("us", tz="UTC"), _timestamp),),
    "review.manual_end_time": ((
        "label_review_manual_end_time", pa.timestamp("us", tz="UTC"), _timestamp),),
    "review.reference_quantity": ((
        "label_review_reference_quantity", pa.decimal128(24, 6), _decimal),),
    "review.boundary_acceptance": (
        ("label_review_start_boundary_accepted", pa.bool_(), _boolean),
        ("label_review_end_boundary_accepted", pa.bool_(), _boolean),
    ),
    "review.quantity_acceptance": ((
        "label_review_quantity_accepted", pa.bool_(), _boolean),),
    "batch.automatic_end_time": ((
        "label_batch_automatic_end_time", pa.timestamp("us", tz="UTC"), _timestamp),),
    "batch.automatic_quantity": ((
        "label_batch_automatic_quantity", pa.decimal128(24, 6), _decimal),),
}


def _process_window_refs(claim: MaterializationClaim) -> tuple[str, ...]:
    return tuple(sorted(
        reference
        for reference in claim.feature_refs
        if reference.startswith(PROCESS_WINDOW_PREFIX)
    ))


def _validate_contract(claim: MaterializationClaim) -> None:
    contract = (claim.artifact_schema_version, claim.materializer_version)
    if contract not in {
        (PARQUET_V1, MATERIALIZER_V1),
        (PARQUET_V2, MATERIALIZER_V2),
    }:
        raise DatasetContractError(
            "unsupported dataset Parquet schema and materializer version pair")
    if claim.artifact_schema_version == PARQUET_V1 and _process_window_refs(claim):
        raise DatasetContractError(
            "bpi.dataset-parquet.v1 does not support process.window.* features")
    if len(claim.feature_refs) != len(set(claim.feature_refs)):
        raise DatasetContractError("dataset feature refs must be unique")


def _schema(claim: MaterializationClaim) -> pa.Schema:
    _validate_contract(claim)
    process_window_refs = set(_process_window_refs(claim))
    unknown_features = (
        set(claim.feature_refs) - set(FEATURE_COLUMNS) - process_window_refs
    )
    unknown_labels = set(claim.label_refs) - set(LABEL_COLUMNS)
    if unknown_features or unknown_labels:
        raise DatasetContractError(
            f"unsupported refs: features={sorted(unknown_features)}, labels={sorted(unknown_labels)}")

    fields = [
        pa.field("snapshot_id", pa.string(), nullable=False),
        pa.field("review_id", pa.string(), nullable=False),
        pa.field("shadow_run_id", pa.string(), nullable=False),
        pa.field("batch_id", pa.string(), nullable=False),
        pa.field("batch_no", pa.string(), nullable=False),
        pa.field("line_id", pa.string(), nullable=False),
        pa.field("prediction_time", pa.timestamp("us", tz="UTC"), nullable=False),
        pa.field("feature_cutoff", pa.timestamp("us", tz="UTC"), nullable=False),
        pa.field("label_available_at", pa.timestamp("us", tz="UTC"), nullable=False),
        pa.field("confidence", pa.decimal128(7, 6), nullable=False),
        pa.field("split_key", pa.string(), nullable=False),
    ]
    fields.extend(pa.field(column[0], column[1]) for column in FEATURE_COLUMNS.values())
    if claim.artifact_schema_version == PARQUET_V2:
        fields.append(pa.field(
            PROCESS_WINDOW_COLUMN,
            pa.map_(pa.string(), pa.decimal128(24, 6)),
            nullable=False,
        ))
    for columns in LABEL_COLUMNS.values():
        fields.extend(pa.field(column[0], column[1]) for column in columns)

    metadata = {
        b"bpi.artifact_schema_version": claim.artifact_schema_version.encode(),
        b"bpi.materializer_version": claim.materializer_version.encode(),
        b"bpi.snapshot_id": str(claim.snapshot_id).encode(),
        b"bpi.manifest_checksum": claim.manifest_checksum.encode(),
        b"bpi.definition_checksum": claim.definition_checksum.encode(),
        b"bpi.feature_refs": _canonical_json(list(claim.feature_refs)).encode(),
        b"bpi.label_refs": _canonical_json(list(claim.label_refs)).encode(),
        b"bpi.row_order": ROW_ORDER.encode(),
    }
    if claim.artifact_schema_version == PARQUET_V2:
        metadata[b"bpi.process_window_feature_layout"] = (
            PROCESS_WINDOW_LAYOUT.encode()
        )
    return pa.schema(fields, metadata=metadata)


def _row(claim: MaterializationClaim, sample: DatasetSample) -> dict[str, Any]:
    if sample.snapshot_id != claim.snapshot_id:
        raise DatasetContractError("sample snapshot does not match the materialization claim")
    if sample.feature_cutoff != sample.prediction_time:
        raise DatasetContractError("feature cutoff must equal prediction time")

    row: dict[str, Any] = {
        "snapshot_id": str(sample.snapshot_id),
        "review_id": str(sample.review_id),
        "shadow_run_id": str(sample.shadow_run_id),
        "batch_id": str(sample.batch_id),
        "batch_no": sample.batch_no,
        "line_id": sample.line_id,
        "prediction_time": sample.prediction_time,
        "feature_cutoff": sample.feature_cutoff,
        "label_available_at": sample.label_available_at,
        "confidence": _decimal(sample.confidence),
        "split_key": sample.split_key,
    }
    for ref, (column_name, _data_type, converter) in FEATURE_COLUMNS.items():
        value = sample.feature_payload.get(ref) if ref in claim.feature_refs else None
        row[column_name] = converter(value)
    if claim.artifact_schema_version == PARQUET_V2:
        row[PROCESS_WINDOW_COLUMN] = [
            (reference, _process_window_decimal(
                reference, sample.feature_payload.get(reference)))
            for reference in _process_window_refs(claim)
        ]
    for ref, columns in LABEL_COLUMNS.items():
        selected = ref in claim.label_refs
        value = sample.label_payload.get(ref) if selected else None
        if ref == "review.boundary_acceptance":
            boundary = value if isinstance(value, dict) else {}
            row[columns[0][0]] = _boolean(boundary.get("start")) if selected else None
            row[columns[1][0]] = _boolean(boundary.get("end")) if selected else None
            continue
        column_name, _data_type, converter = columns[0]
        row[column_name] = converter(value)
    return row


def build_parquet(
    claim: MaterializationClaim,
    samples: list[DatasetSample],
    destination: Path,
) -> ParquetArtifact:
    if len(samples) != claim.included_count:
        raise DatasetContractError(
            f"included row count changed: expected={claim.included_count}, actual={len(samples)}")
    ordered = sorted(
        samples,
        key=lambda sample: (
            sample.line_id,
            sample.prediction_time,
            str(sample.batch_id),
            str(sample.review_id),
        ),
    )
    schema = _schema(claim)
    table = pa.Table.from_pylist([_row(claim, sample) for sample in ordered], schema=schema)
    destination.parent.mkdir(parents=True, exist_ok=True)
    pq.write_table(
        table,
        destination,
        version="2.6",
        compression="zstd",
        compression_level=3,
        use_dictionary=False,
        write_statistics=True,
        data_page_version="1.0",
        use_compliant_nested_type=True,
    )
    payload = destination.read_bytes()
    schema_json = {
        "schemaVersion": claim.artifact_schema_version,
        "fields": [
            {"name": field.name, "type": str(field.type), "nullable": field.nullable}
            for field in schema
        ],
        "selectedFeatureRefs": list(claim.feature_refs),
        "processWindowFeatureRefs": list(_process_window_refs(claim)),
        "selectedLabelRefs": list(claim.label_refs),
    }
    metadata = {
        "artifactSchemaVersion": claim.artifact_schema_version,
        "materializerVersion": claim.materializer_version,
        "manifestSchemaVersion": claim.manifest_schema_version,
        "manifestChecksum": claim.manifest_checksum,
        "definitionChecksum": claim.definition_checksum,
        "rowOrder": ROW_ORDER,
        "processWindowFeatureLayout": (
            PROCESS_WINDOW_LAYOUT
            if claim.artifact_schema_version == PARQUET_V2
            else None
        ),
        "compression": "zstd:3",
        "sourcePayloadIncluded": False,
        "excludedSamplesIncluded": False,
        "icebergReady": False,
        "mlflowRegistered": False,
        "modelTrained": False,
    }
    return ParquetArtifact(
        path=destination,
        content_sha256=hashlib.sha256(payload).hexdigest(),
        byte_size=len(payload),
        row_count=table.num_rows,
        schema_json=schema_json,
        metadata=metadata,
    )
