from __future__ import annotations

import tempfile
import unittest
from datetime import UTC, datetime, timedelta
from decimal import Decimal
from pathlib import Path
from uuid import UUID

import pyarrow.parquet as pq

from bpi_dataset_materializer.models import DatasetSample, MaterializationClaim
from bpi_dataset_materializer.parquet import build_parquet


SNAPSHOT_ID = UUID("00000000-0000-0000-0000-000000000101")


def claim() -> MaterializationClaim:
    return MaterializationClaim(
        id=UUID("00000000-0000-0000-0000-000000000201"),
        tenant_id="tenant-a",
        snapshot_id=SNAPSHOT_ID,
        dataset_id=UUID("00000000-0000-0000-0000-000000000301"),
        dataset_code="SUGAR_BATCH_QUALITY",
        dataset_version="1.0.0",
        plant_id="PLANT-01",
        artifact_schema_version="bpi.dataset-parquet.v1",
        materializer_version="bpi-dataset-materializer/0.1.0",
        manifest_checksum="a" * 64,
        manifest_schema_version="bpi.dataset-manifest.v1",
        definition_checksum="b" * 64,
        included_count=2,
        feature_refs=("batch.material_code", "batch.order_id"),
        label_refs=("review.reference_quantity", "review.boundary_acceptance"),
        claim_token=UUID("00000000-0000-0000-0000-000000000401"),
        revision=2,
        attempt_count=1,
    )


def sample(index: int, line_id: str) -> DatasetSample:
    start = datetime(2026, 7, 22, 1, index, tzinfo=UTC)
    return DatasetSample(
        snapshot_id=SNAPSHOT_ID,
        review_id=UUID(f"00000000-0000-0000-0000-{500 + index:012d}"),
        shadow_run_id=UUID(f"00000000-0000-0000-0000-{600 + index:012d}"),
        batch_id=UUID(f"00000000-0000-0000-0000-{700 + index:012d}"),
        batch_no=f"BATCH-{index}",
        line_id=line_id,
        prediction_time=start,
        feature_cutoff=start,
        label_available_at=start + timedelta(hours=2),
        confidence=Decimal("1.000000"),
        split_key="2026-07",
        feature_payload={
            "batch.material_code": f"MAT-{index}",
            "batch.order_id": f"ORDER-{index}",
            "batch.stage_code": "SHOULD_NOT_LEAK",
        },
        label_payload={
            "review.reference_quantity": f"{index + 10}.250000",
            "review.boundary_acceptance": {"start": True, "end": index % 2 == 0},
            "batch.automatic_quantity": "999.000000",
        },
    )


class DeterministicParquetTest(unittest.TestCase):
    def test_same_frozen_rows_produce_identical_bytes_and_stable_order(self):
        first_rows = [sample(2, "LINE-B"), sample(1, "LINE-A")]
        second_rows = list(reversed(first_rows))
        with tempfile.TemporaryDirectory() as directory:
            first = build_parquet(claim(), first_rows, Path(directory) / "first.parquet")
            second = build_parquet(claim(), second_rows, Path(directory) / "second.parquet")
            self.assertEqual(first.content_sha256, second.content_sha256)
            self.assertEqual(first.path.read_bytes(), second.path.read_bytes())
            table = pq.read_table(first.path)
        self.assertEqual(["LINE-A", "LINE-B"], table.column("line_id").to_pylist())
        self.assertEqual(2, first.row_count)

    def test_unselected_payload_fields_are_null_and_source_payload_is_absent(self):
        rows = [sample(1, "LINE-A"), sample(2, "LINE-B")]
        with tempfile.TemporaryDirectory() as directory:
            artifact = build_parquet(claim(), rows, Path(directory) / "dataset.parquet")
            table = pq.read_table(artifact.path)
        self.assertNotIn("source_payload", table.column_names)
        self.assertEqual([None, None], table.column("feature_batch_stage_code").to_pylist())
        self.assertEqual([None, None], table.column("label_batch_automatic_quantity").to_pylist())
        self.assertEqual([None, None], table.column("label_review_quantity_accepted").to_pylist())
        self.assertFalse(artifact.metadata["sourcePayloadIncluded"])
        self.assertFalse(artifact.metadata["excludedSamplesIncluded"])


if __name__ == "__main__":
    unittest.main()
