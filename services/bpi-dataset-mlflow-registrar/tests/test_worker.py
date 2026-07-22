import unittest
from types import SimpleNamespace
from uuid import uuid4

from bpi_dataset_mlflow_registrar.errors import failure_code, sanitize_error
from bpi_dataset_mlflow_registrar.mlflow_client import (
    MlflowContractError,
    MlflowTransportError,
)
from bpi_dataset_mlflow_registrar.health import HealthState
from bpi_dataset_mlflow_registrar.worker import MlflowRegistrarWorker


class WorkerErrorTest(unittest.TestCase):
    def test_failure_codes_preserve_tracking_boundary(self) -> None:
        self.assertEqual(
            "MLFLOW_CONTRACT_VIOLATION",
            failure_code(MlflowContractError("dataset mismatch")),
        )
        self.assertEqual(
            "MLFLOW_TRANSPORT_ERROR",
            failure_code(MlflowTransportError("timeout")),
        )

    def test_secrets_are_redacted(self) -> None:
        detail = sanitize_error(
            RuntimeError(
                "password=hunter2 access_token=oauth-token "
                "Authorization: Bearer jwt-token http://user:pass@mlflow"
            )
        )
        self.assertNotIn("hunter2", detail)
        self.assertNotIn("oauth-token", detail)
        self.assertNotIn("jwt-token", detail)
        self.assertNotIn("user:pass", detail)
        self.assertIn("[REDACTED]", detail)

    def test_mlflow_outage_after_claim_is_recorded_as_retryable_failure(self) -> None:
        events = []
        claim = SimpleNamespace(id=uuid4(), source_facts_verified=True)

        class Repository:
            failure = None

            def ping(self):
                events.append("repository.ping")

            def recover_and_claim(self):
                events.append("repository.claim")
                return claim

            def fail(self, failed_claim, code, detail):
                events.append("repository.fail")
                self.failure = (failed_claim, code, detail)
                return True

        class Client:
            def ping(self):
                events.append("mlflow.ping")
                raise MlflowTransportError("tracking server unavailable")

        repository = Repository()
        worker = MlflowRegistrarWorker.__new__(MlflowRegistrarWorker)
        worker._repository = repository
        worker._client = Client()
        worker._health = HealthState(enabled=True)

        with self.assertLogs("bpi-dataset-mlflow-registrar", level="ERROR") as logs:
            worker._cycle()

        self.assertEqual(
            [
                "repository.ping",
                "repository.claim",
                "mlflow.ping",
                "repository.fail",
            ],
            events,
        )
        self.assertIs(repository.failure[0], claim)
        self.assertEqual("MLFLOW_TRANSPORT_ERROR", repository.failure[1])
        self.assertIn("tracking server unavailable", repository.failure[2])
        health = worker._health.snapshot()
        self.assertEqual("DEGRADED", health["status"])
        self.assertIsNone(health["activeRegistrationId"])
        self.assertIn("MLFLOW_TRANSPORT_ERROR", "\n".join(logs.output))


if __name__ == "__main__":
    unittest.main()
