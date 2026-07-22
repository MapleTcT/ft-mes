import unittest

from bpi_dataset_catalog_publisher.catalog import CatalogContractError
from bpi_dataset_catalog_publisher.source_object import SourceObjectContractError
from bpi_dataset_catalog_publisher.worker import failure_code, sanitize_error


class WorkerErrorTest(unittest.TestCase):
    def test_failure_codes_preserve_contract_boundary(self) -> None:
        self.assertEqual(
            "SOURCE_OBJECT_CONTRACT_VIOLATION",
            failure_code(SourceObjectContractError("sha drift")),
        )
        self.assertEqual(
            "ICEBERG_CATALOG_CONTRACT_VIOLATION",
            failure_code(CatalogContractError("snapshot drift")),
        )

    def test_secrets_are_redacted(self) -> None:
        detail = sanitize_error(
            RuntimeError(
                "credential=client:secret password=hunter2 "
                "access_token=oauth-token Authorization: Bearer jwt-token "
                "http://user:pass@polaris"
            )
        )
        self.assertNotIn("client:secret", detail)
        self.assertNotIn("hunter2", detail)
        self.assertNotIn("user:pass", detail)
        self.assertNotIn("oauth-token", detail)
        self.assertNotIn("jwt-token", detail)
        self.assertIn("[REDACTED]", detail)


if __name__ == "__main__":
    unittest.main()
