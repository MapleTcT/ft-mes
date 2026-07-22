from __future__ import annotations

import json
import time
from dataclasses import dataclass
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, build_opener

from .models import RegistrationClaim, RegistrationResult


class MlflowContractError(RuntimeError):
    pass


class MlflowTransportError(RuntimeError):
    pass


@dataclass(frozen=True)
class _HttpFailure(Exception):
    status: int
    body: str


class MlflowTrackingClient:
    def __init__(
        self,
        base_url: str,
        timeout_seconds: int,
        token: str | None = None,
    ):
        self._base_url = base_url.rstrip("/")
        self._timeout_seconds = timeout_seconds
        self._token = token
        self._opener = build_opener()

    def ping(self) -> None:
        try:
            self._request("GET", "/health", expect_json=False)
        except _HttpFailure as exception:
            raise MlflowTransportError(
                f"MLflow health endpoint returned HTTP {exception.status}"
            ) from exception

    def register(self, claim: RegistrationClaim) -> RegistrationResult:
        if not claim.source_facts_verified:
            raise MlflowContractError(
                "frozen registration facts do not match the LOCKED recovery archive"
            )
        experiment_id = self._ensure_experiment(claim.experiment_name)
        run, reused = self._find_registration_run(experiment_id, claim)
        if run is None:
            run = self._create_run(experiment_id, claim)
            self._log_dataset_input(run_id(run), claim)
            self._update_run(run_id(run), "FINISHED")
        else:
            run = self._get_run(run_id(run))
            self._verify_identity_tags(run, claim)
            info = require_object(run, "info")
            status = str(info.get("status"))
            if status == "RUNNING":
                dataset_inputs = self._dataset_inputs(run)
                if not dataset_inputs:
                    self._log_dataset_input(run_id(run), claim)
                else:
                    self._verify_dataset_input(dataset_inputs, claim)
                self._update_run(run_id(run), "FINISHED")
            elif status != "FINISHED":
                raise MlflowContractError(
                    f"MLflow registration run has unsupported status: {status}"
                )
        verified_run = self._get_run(run_id(run))
        self._verify_run(verified_run, claim)
        info = require_object(verified_run, "info")
        return RegistrationResult(
            experiment_id=str(info.get("experiment_id", "")),
            run_id=str(info.get("run_id") or info.get("run_uuid") or ""),
            artifact_uri=str(info.get("artifact_uri", "")),
            dataset_source=claim.source_uri,
            metadata={
                "sourceFactsVerified": True,
                "datasetInputVerified": True,
                "lineageVerified": True,
                "modelTrained": False,
                "modelRegistered": False,
                "onlineInferenceEnabled": False,
                "productionActivationAllowed": False,
                "reusedRun": reused,
                "experimentName": claim.experiment_name,
                "datasetName": claim.dataset_name,
                "datasetDigest": claim.dataset_digest,
                "sourceUri": claim.source_uri,
                "sourceArchiveVersionId": claim.source_archive_version_id,
                "archiveManifestVersionId": claim.archive_manifest_version_id,
            },
        )

    def _ensure_experiment(self, name: str) -> str:
        try:
            response = self._request(
                "GET",
                "/api/2.0/mlflow/experiments/get-by-name",
                query={"experiment_name": name},
            )
            return required_text(require_object(response, "experiment"), "experiment_id")
        except _HttpFailure as exception:
            if exception.status != 404:
                raise MlflowTransportError(
                    f"MLflow experiment lookup returned HTTP {exception.status}"
                ) from exception
        try:
            response = self._request(
                "POST", "/api/2.0/mlflow/experiments/create", {"name": name}
            )
            return required_text(response, "experiment_id")
        except _HttpFailure as exception:
            if exception.status not in {400, 409}:
                raise MlflowTransportError(
                    f"MLflow experiment create returned HTTP {exception.status}"
                ) from exception
            response = self._request(
                "GET",
                "/api/2.0/mlflow/experiments/get-by-name",
                query={"experiment_name": name},
            )
            return required_text(require_object(response, "experiment"), "experiment_id")

    def _find_registration_run(
        self, experiment_id: str, claim: RegistrationClaim
    ) -> tuple[dict[str, Any] | None, bool]:
        escaped_id = str(claim.id).replace("'", "\\'")
        response = self._request(
            "POST",
            "/api/2.0/mlflow/runs/search",
            {
                "experiment_ids": [experiment_id],
                "filter": f"tags.`bpi.registration_id` = '{escaped_id}'",
                "max_results": 2,
            },
        )
        runs = response.get("runs") or []
        if not isinstance(runs, list):
            raise MlflowContractError("MLflow run search returned an invalid runs value")
        if len(runs) > 1:
            raise MlflowContractError(
                "MLflow contains duplicate runs for one BPI registration"
            )
        return (runs[0], True) if runs else (None, False)

    def _create_run(
        self, experiment_id: str, claim: RegistrationClaim
    ) -> dict[str, Any]:
        response = self._request(
            "POST",
            "/api/2.0/mlflow/runs/create",
            {
                "experiment_id": experiment_id,
                "start_time": int(time.time() * 1000),
                "run_name": f"dataset-registration-{claim.id}",
                "tags": [
                    {"key": key, "value": value}
                    for key, value in sorted(self._run_tags(claim).items())
                ],
            },
        )
        return require_object(response, "run")

    def _log_dataset_input(self, run_id_value: str, claim: RegistrationClaim) -> None:
        self._request(
            "POST",
            "/api/2.0/mlflow/runs/log-inputs",
            {
                "run_id": run_id_value,
                "datasets": [
                    {
                        "dataset": {
                            "name": claim.dataset_name,
                            "digest": claim.dataset_digest,
                            "source_type": "s3",
                            "source": canonical_json({"uri": claim.source_uri}),
                            "schema": canonical_json(claim.source_schema_json),
                            "profile": canonical_json(self._profile(claim)),
                        },
                        "tags": [
                            {
                                "key": "mlflow.data.context",
                                "value": "training_candidate",
                            }
                        ],
                    }
                ],
            },
        )

    def _update_run(self, run_id_value: str, status: str) -> None:
        self._request(
            "POST",
            "/api/2.0/mlflow/runs/update",
            {
                "run_id": run_id_value,
                "status": status,
                "end_time": int(time.time() * 1000),
            },
        )

    def _get_run(self, run_id_value: str) -> dict[str, Any]:
        response = self._request(
            "GET", "/api/2.0/mlflow/runs/get", query={"run_id": run_id_value}
        )
        return require_object(response, "run")

    def _verify_run(self, run: dict[str, Any], claim: RegistrationClaim) -> None:
        info = require_object(run, "info")
        if str(info.get("status")) != "FINISHED":
            raise MlflowContractError("MLflow registration run is not FINISHED")
        if not required_text(info, "artifact_uri"):
            raise MlflowContractError("MLflow registration run has no artifact URI")
        self._verify_identity_tags(run, claim)
        self._verify_dataset_input(self._dataset_inputs(run), claim)

    def _verify_identity_tags(
        self, run: dict[str, Any], claim: RegistrationClaim
    ) -> None:
        data = require_object(run, "data")
        tags = pairs(data.get("tags"), "MLflow run tags")
        expected_tags = self._run_tags(claim)
        for key, expected in expected_tags.items():
            if tags.get(key) != expected:
                raise MlflowContractError(f"MLflow run tag mismatch: {key}")

    def _dataset_inputs(self, run: dict[str, Any]) -> list[Any]:
        inputs = require_object(run, "inputs")
        dataset_inputs = inputs.get("dataset_inputs") or []
        if not isinstance(dataset_inputs, list):
            raise MlflowContractError("MLflow dataset inputs value is invalid")
        return dataset_inputs

    def _verify_dataset_input(
        self, dataset_inputs: list[Any], claim: RegistrationClaim
    ) -> None:
        if len(dataset_inputs) != 1:
            raise MlflowContractError(
                "MLflow registration run must contain exactly one dataset input"
            )
        dataset_input = dataset_inputs[0]
        if not isinstance(dataset_input, dict):
            raise MlflowContractError("MLflow dataset input is invalid")
        dataset = require_object(dataset_input, "dataset")
        expected_dataset = {
            "name": claim.dataset_name,
            "digest": claim.dataset_digest,
            "source_type": "s3",
            "source": canonical_json({"uri": claim.source_uri}),
            "schema": canonical_json(claim.source_schema_json),
            "profile": canonical_json(self._profile(claim)),
        }
        for key, expected in expected_dataset.items():
            actual = dataset.get(key)
            if key in {"source", "schema", "profile"}:
                actual = canonicalize_json_text(actual, key)
            if actual != expected:
                raise MlflowContractError(f"MLflow dataset input mismatch: {key}")
        input_tags = pairs(dataset_input.get("tags"), "MLflow dataset input tags")
        if input_tags.get("mlflow.data.context") != "training_candidate":
            raise MlflowContractError("MLflow dataset input context is invalid")

    def _run_tags(self, claim: RegistrationClaim) -> dict[str, str]:
        return {
            "bpi.registration_id": str(claim.id),
            "bpi.tenant_id": claim.tenant_id,
            "bpi.retention_archive_id": str(claim.retention_archive_id),
            "bpi.catalog_publication_id": str(claim.catalog_publication_id),
            "bpi.materialization_id": str(claim.materialization_id),
            "bpi.snapshot_id": str(claim.source_snapshot_id),
            "bpi.dataset_id": str(claim.dataset_id),
            "bpi.dataset_version": claim.dataset_version,
            "bpi.manifest_checksum": claim.manifest_checksum,
            "bpi.source_content_sha256": claim.source_content_sha256,
            "bpi.semantic_checksum": claim.catalog_semantic_checksum,
            "bpi.source_archive_version_id": claim.source_archive_version_id,
            "bpi.archive_manifest_version_id": claim.archive_manifest_version_id,
            "bpi.archive_manifest_sha256": claim.archive_manifest_sha256,
            "bpi.iceberg_snapshot_id": str(claim.iceberg_snapshot_id),
            "bpi.registrar_version": claim.registrar_version,
            "bpi.tracking_profile": claim.tracking_profile,
            "bpi.model_trained": "false",
            "bpi.model_registered": "false",
            "bpi.online_inference_enabled": "false",
            "bpi.production_activation_allowed": "false",
        }

    def _profile(self, claim: RegistrationClaim) -> dict[str, Any]:
        return {
            "rowCount": claim.source_row_count,
            "byteSize": claim.source_byte_size,
            "tableIdentifier": claim.table_identifier,
            "icebergSnapshotId": str(claim.iceberg_snapshot_id),
            "manifestChecksum": claim.manifest_checksum,
            "semanticChecksum": claim.catalog_semantic_checksum,
            "datasetVersion": claim.dataset_version,
            "featureCutoffPolicy": "snapshot-end-exclusive-v1",
            "labelPolicy": "not-defined-model-not-trained",
        }

    def _request(
        self,
        method: str,
        path: str,
        payload: dict[str, Any] | None = None,
        query: dict[str, str] | None = None,
        expect_json: bool = True,
    ) -> dict[str, Any]:
        url = f"{self._base_url}{path}"
        if query:
            url += "?" + urlencode(query)
        body = None if payload is None else canonical_json(payload).encode("utf-8")
        headers = {"Accept": "application/json"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        if self._token:
            headers["Authorization"] = f"Bearer {self._token}"
        request = Request(url, data=body, headers=headers, method=method)
        try:
            with self._opener.open(request, timeout=self._timeout_seconds) as response:
                raw = response.read().decode("utf-8")
        except HTTPError as exception:
            try:
                raw = exception.read().decode("utf-8", errors="replace")
            finally:
                exception.close()
            raise _HttpFailure(exception.code, raw[:1000]) from exception
        except (URLError, TimeoutError, OSError) as exception:
            raise MlflowTransportError(
                f"MLflow request transport failed: {type(exception).__name__}"
            ) from exception
        if not expect_json:
            return {}
        if not raw:
            return {}
        try:
            value = json.loads(raw)
        except json.JSONDecodeError as exception:
            raise MlflowContractError("MLflow returned invalid JSON") from exception
        if not isinstance(value, dict):
            raise MlflowContractError("MLflow returned a non-object response")
        return value


def canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def canonicalize_json_text(value: Any, field: str) -> str:
    if not isinstance(value, str):
        raise MlflowContractError(f"MLflow dataset {field} is not JSON text")
    try:
        return canonical_json(json.loads(value))
    except json.JSONDecodeError as exception:
        raise MlflowContractError(
            f"MLflow dataset {field} contains invalid JSON"
        ) from exception


def require_object(value: dict[str, Any], key: str) -> dict[str, Any]:
    nested = value.get(key)
    if not isinstance(nested, dict):
        raise MlflowContractError(f"MLflow response is missing object: {key}")
    return nested


def required_text(value: dict[str, Any], key: str) -> str:
    text = value.get(key)
    if text is None or not str(text).strip():
        raise MlflowContractError(f"MLflow response is missing text: {key}")
    return str(text)


def run_id(run: dict[str, Any]) -> str:
    info = require_object(run, "info")
    value = info.get("run_id") or info.get("run_uuid")
    if value is None or not str(value).strip():
        raise MlflowContractError("MLflow run has no run ID")
    return str(value)


def pairs(value: Any, label: str) -> dict[str, str]:
    if value is None:
        return {}
    if not isinstance(value, list):
        raise MlflowContractError(f"{label} is invalid")
    result: dict[str, str] = {}
    for item in value:
        if not isinstance(item, dict) or "key" not in item or "value" not in item:
            raise MlflowContractError(f"{label} contains an invalid item")
        result[str(item["key"])] = str(item["value"])
    return result
