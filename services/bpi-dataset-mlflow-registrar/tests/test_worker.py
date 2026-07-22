import unittest

from bpi_dataset_mlflow_registrar.errors import failure_code, sanitize_error
from bpi_dataset_mlflow_registrar.mlflow_client import (
    MlflowContractError,
    MlflowTransportError,
)


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


if __name__ == "__main__":
    unittest.main()
