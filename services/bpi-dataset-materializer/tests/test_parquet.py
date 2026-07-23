from __future__ import annotations

import tempfile
import unittest
from dataclasses import replace
from datetime import UTC, datetime, timedelta
from decimal import Decimal
from pathlib import Path
from uuid import UUID

import pyarrow.parquet as pq

from bpi_dataset_materializer.models import DatasetSample, MaterializationClaim
from bpi_dataset_materializer.parquet import (
    DatasetContractError,
    PROCESS_WINDOW_COLUMN,
    PROCESS_WINDOW_LAYOUT,
    build_parquet,
)


SNAPSHOT_ID = UUID("00000000-0000-0000-0000-000000000101")


def claim(
    *,
    artifact_schema_version: str = "bpi.dataset-parquet.v1",
    materializer_version: str = "bpi-dataset-materializer/0.1.0",
    feature_refs: tuple[str, ...] = ("batch.material_code", "batch.order_id"),
) -> MaterializationClaim:
    return MaterializationClaim(
        id=UUID("00000000-0000-0000-0000-000000000201"),
        tenant_id="tenant-a",
        snapshot_id=SNAPSHOT_ID,
        dataset_id=UUID("00000000-0000-0000-0000-000000000301"),
        dataset_code="SUGAR_BATCH_QUALITY",
        dataset_version="1.0.0",
        plant_id="PLANT-01",
        artifact_schema_version=artifact_schema_version,
        materializer_version=materializer_version,
        manifest_checksum="a" * 64,
        manifest_schema_version="bpi.dataset-manifest.v1",
        definition_checksum="b" * 64,
        included_count=2,
        feature_refs=feature_refs,
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
            "process.window.feed_flow.mean_60s": f"{12 + index}.1234567",
            "process.window.feed_pump.true_ratio_30s": Decimal("0.750000"),
        },
        label_payload={
            "review.reference_quantity": f"{index + 10}.250000",
            "review.boundary_acceptance": {"start": True, "end": index % 2 == 0},
            "batch.automatic_quantity": "999.000000",
        },
    )


class DeterministicParquetTest(unittest.TestCase):
    def v2_claim(
        self,
        feature_refs: tuple[str, ...] = (
            "batch.material_code",
            "process.window.feed_pump.true_ratio_30s",
            "process.window.feed_flow.mean_60s",
        ),
    ) -> MaterializationClaim:
        return claim(
            artifact_schema_version="bpi.dataset-parquet.v2",
            materializer_version="bpi-dataset-materializer/0.2.0",
            feature_refs=feature_refs,
        )

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

    def test_v2_preserves_sorted_process_window_refs_in_numeric_map(self):
        rows = [sample(2, "LINE-B"), sample(1, "LINE-A")]
        with tempfile.TemporaryDirectory() as directory:
            first = build_parquet(
                self.v2_claim(), rows, Path(directory) / "first.parquet")
            second = build_parquet(
                self.v2_claim(), list(reversed(rows)),
                Path(directory) / "second.parquet")
            table = pq.read_table(first.path)
            self.assertEqual(first.path.read_bytes(), second.path.read_bytes())

        self.assertEqual(first.content_sha256, second.content_sha256)
        self.assertEqual(
            [
                "process.window.feed_flow.mean_60s",
                "process.window.feed_pump.true_ratio_30s",
            ],
            first.schema_json["processWindowFeatureRefs"],
        )
        self.assertEqual(
            PROCESS_WINDOW_LAYOUT,
            first.metadata["processWindowFeatureLayout"],
        )
        self.assertEqual(
            PROCESS_WINDOW_LAYOUT.encode(),
            table.schema.metadata[b"bpi.process_window_feature_layout"],
        )
        self.assertEqual(
            [
                ("process.window.feed_flow.mean_60s", Decimal("13.123457")),
                ("process.window.feed_pump.true_ratio_30s", Decimal("0.750000")),
            ],
            table.column(PROCESS_WINDOW_COLUMN)[0].as_py(),
        )

    def test_v2_without_process_windows_writes_an_empty_map(self):
        with tempfile.TemporaryDirectory() as directory:
            artifact = build_parquet(
                self.v2_claim(("batch.material_code",)),
                [sample(1, "LINE-A"), sample(2, "LINE-B")],
                Path(directory) / "dataset.parquet",
            )
            table = pq.read_table(artifact.path)
        self.assertEqual([[], []], table.column(PROCESS_WINDOW_COLUMN).to_pylist())

    def test_v1_rejects_process_window_features(self):
        invalid = claim(feature_refs=("process.window.feed_flow.mean_60s",))
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(
                DatasetContractError, "v1 does not support process.window"
            ):
                build_parquet(
                    invalid,
                    [sample(1, "LINE-A"), sample(2, "LINE-B")],
                    Path(directory) / "dataset.parquet",
                )

    def test_v2_rejects_unknown_feature_namespaces(self):
        invalid = self.v2_claim(("telemetry.unversioned.live_value",))
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(DatasetContractError, "unsupported refs"):
                build_parquet(
                    invalid,
                    [sample(1, "LINE-A"), sample(2, "LINE-B")],
                    Path(directory) / "dataset.parquet",
                )

    def test_schema_and_materializer_versions_must_match(self):
        invalid = claim(
            artifact_schema_version="bpi.dataset-parquet.v2",
            materializer_version="bpi-dataset-materializer/0.1.0",
        )
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(DatasetContractError, "version pair"):
                build_parquet(
                    invalid,
                    [sample(1, "LINE-A"), sample(2, "LINE-B")],
                    Path(directory) / "dataset.parquet",
                )

    def test_v2_rejects_duplicate_process_window_refs(self):
        reference = "process.window.feed_flow.mean_60s"
        invalid = self.v2_claim((reference, reference))
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(DatasetContractError, "must be unique"):
                build_parquet(
                    invalid,
                    [sample(1, "LINE-A"), sample(2, "LINE-B")],
                    Path(directory) / "dataset.parquet",
                )

    def test_v2_rejects_null_and_non_finite_process_window_values(self):
        invalid_claim = self.v2_claim(("process.window.feed_flow.mean_60s",))
        for invalid_value in (None, True, "NaN", "Infinity"):
            broken = sample(1, "LINE-A")
            broken_payload = dict(broken.feature_payload)
            broken_payload["process.window.feed_flow.mean_60s"] = invalid_value
            broken = replace(broken, feature_payload=broken_payload)
            with self.subTest(value=invalid_value), tempfile.TemporaryDirectory() as directory:
                with self.assertRaisesRegex(
                    DatasetContractError, "must be a finite numeric value"
                ):
                    build_parquet(
                        invalid_claim,
                        [broken, sample(2, "LINE-B")],
                        Path(directory) / "dataset.parquet",
                    )


if __name__ == "__main__":
    unittest.main()
