from __future__ import annotations

import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from socketserver import TCPServer
from urllib.parse import parse_qs, urlparse
from uuid import UUID

from bpi_dataset_mlflow_registrar.mlflow_client import (
    MlflowContractError,
    MlflowTrackingClient,
)
from bpi_dataset_mlflow_registrar.models import RegistrationClaim


def claim() -> RegistrationClaim:
    return RegistrationClaim(
        id=UUID("00000000-0000-0000-0000-000000000301"),
        tenant_id="tenant-a",
        retention_archive_id=UUID("00000000-0000-0000-0000-000000000291"),
        catalog_publication_id=UUID("00000000-0000-0000-0000-000000000281"),
        materialization_id=UUID("00000000-0000-0000-0000-000000000271"),
        source_snapshot_id=UUID("00000000-0000-0000-0000-000000000261"),
        dataset_id=UUID("00000000-0000-0000-0000-000000000251"),
        dataset_code="BPI_BATCH_FEATURES",
        dataset_version="v1",
        plant_id="plant-a",
        line_ids=("line-a",),
        registrar_version="bpi-dataset-mlflow-registrar/0.1.0",
        tracking_profile="bpi-mlflow-dataset-v1",
        manifest_checksum="a" * 64,
        source_content_sha256="b" * 64,
        source_object_version_id="source-v1",
        source_byte_size=1024,
        source_row_count=12,
        source_schema_json={"fields": [{"name": "flow", "type": "double"}]},
        table_identifier="ft_mes_bpi.datasets.batch_features",
        iceberg_snapshot_id=9223372036854775000,
        catalog_semantic_checksum="c" * 64,
        archive_bucket="bpi-dataset-recovery",
        source_archive_object_key="tenant-a/archive/source.parquet",
        source_archive_version_id="locked-source-v1",
        archive_manifest_object_key="tenant-a/archive/manifest.json",
        archive_manifest_version_id="locked-manifest-v1",
        archive_manifest_sha256="d" * 64,
        experiment_name="ft-mes-bpi-training-candidates-tenant-a",
        dataset_name="BPI_BATCH_FEATURES",
        dataset_digest="c" * 16,
        source_facts_verified=True,
        claim_token=UUID("00000000-0000-0000-0000-000000000399"),
        revision=2,
        attempt_count=1,
    )


class FakeMlflow:
    def __init__(self) -> None:
        self.experiment: dict | None = None
        self.runs: dict[str, dict] = {}
        self.create_count = 0
        state = self

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self) -> None:  # noqa: N802
                parsed = urlparse(self.path)
                if parsed.path == "/health":
                    self.reply_text(200, "OK")
                    return
                if parsed.path.endswith("/experiments/get-by-name"):
                    if state.experiment is None:
                        self.reply(404, {"error_code": "RESOURCE_DOES_NOT_EXIST"})
                    else:
                        self.reply(200, {"experiment": state.experiment})
                    return
                if parsed.path.endswith("/runs/get"):
                    run_id = parse_qs(parsed.query)["run_id"][0]
                    self.reply(200, {"run": state.runs[run_id]})
                    return
                self.reply(404, {})

            def do_POST(self) -> None:  # noqa: N802
                payload = self.payload()
                if self.path.endswith("/experiments/create"):
                    state.experiment = {
                        "experiment_id": "31",
                        "name": payload["name"],
                    }
                    self.reply(200, {"experiment_id": "31"})
                    return
                if self.path.endswith("/runs/search"):
                    matching = []
                    for run in state.runs.values():
                        tags = {item["key"]: item["value"] for item in run["data"]["tags"]}
                        if tags.get("bpi.registration_id") == str(claim().id):
                            matching.append(run)
                    self.reply(200, {"runs": matching})
                    return
                if self.path.endswith("/runs/create"):
                    state.create_count += 1
                    run_id = f"run-{state.create_count}"
                    state.runs[run_id] = {
                        "info": {
                            "run_id": run_id,
                            "experiment_id": payload["experiment_id"],
                            "status": "RUNNING",
                            "artifact_uri": f"mlflow-artifacts:/31/{run_id}/artifacts",
                        },
                        "data": {"tags": payload["tags"]},
                        "inputs": {"dataset_inputs": []},
                    }
                    self.reply(200, {"run": state.runs[run_id]})
                    return
                if self.path.endswith("/runs/log-inputs"):
                    state.runs[payload["run_id"]]["inputs"] = {
                        "dataset_inputs": payload["datasets"]
                    }
                    self.reply(200, {})
                    return
                if self.path.endswith("/runs/update"):
                    state.runs[payload["run_id"]]["info"]["status"] = payload["status"]
                    self.reply(200, {})
                    return
                self.reply(404, {})

            def payload(self) -> dict:
                size = int(self.headers.get("Content-Length", "0"))
                return json.loads(self.rfile.read(size) or b"{}")

            def reply(self, status: int, payload: dict) -> None:
                body = json.dumps(payload).encode()
                self.send_response(status)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def reply_text(self, status: int, payload: str) -> None:
                body = payload.encode()
                self.send_response(status)
                self.send_header("Content-Type", "text/plain")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, _format: str, *_args) -> None:
                return

        class LocalServer(ThreadingHTTPServer):
            def server_bind(self) -> None:
                TCPServer.server_bind(self)
                self.server_name = "localhost"
                self.server_port = self.server_address[1]

        self.server = LocalServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    @property
    def url(self) -> str:
        return f"http://127.0.0.1:{self.server.server_port}"

    def __enter__(self) -> "FakeMlflow":
        self.thread.start()
        return self

    def __exit__(self, *_args) -> None:
        self.server.shutdown()
        self.server.server_close()


class MlflowTrackingClientTest(unittest.TestCase):
    def test_accepts_plain_text_health_response(self) -> None:
        with FakeMlflow() as mlflow:
            MlflowTrackingClient(mlflow.url, 2).ping()

    def test_registers_and_reuses_one_verified_dataset_input(self) -> None:
        with FakeMlflow() as mlflow:
            client = MlflowTrackingClient(mlflow.url, 2)
            first = client.register(claim())
            second = client.register(claim())

        self.assertEqual("31", first.experiment_id)
        self.assertEqual("run-1", first.run_id)
        self.assertFalse(first.metadata["reusedRun"])
        self.assertTrue(second.metadata["reusedRun"])
        self.assertEqual(1, mlflow.create_count)
        self.assertIn("versionId=locked-source-v1", first.dataset_source)
        self.assertFalse(first.metadata["modelTrained"])
        self.assertFalse(first.metadata["productionActivationAllowed"])

    def test_rejects_dataset_input_drift_on_recovery(self) -> None:
        with FakeMlflow() as mlflow:
            client = MlflowTrackingClient(mlflow.url, 2)
            first = client.register(claim())
            dataset = mlflow.runs[first.run_id]["inputs"]["dataset_inputs"][0]["dataset"]
            dataset["digest"] = "drifted-digest"
            with self.assertRaises(MlflowContractError):
                client.register(claim())

    def test_resumes_a_crash_after_run_creation_without_duplicate_run(self) -> None:
        with FakeMlflow() as mlflow:
            client = MlflowTrackingClient(mlflow.url, 2)
            first = client.register(claim())
            run = mlflow.runs[first.run_id]
            run["info"]["status"] = "RUNNING"
            run["inputs"]["dataset_inputs"] = []

            recovered = client.register(claim())

        self.assertEqual(first.run_id, recovered.run_id)
        self.assertTrue(recovered.metadata["reusedRun"])
        self.assertEqual(1, mlflow.create_count)


if __name__ == "__main__":
    unittest.main()
