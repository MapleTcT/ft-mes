import unittest

from bpi_dataset_catalog_publisher.source_object import SourceObjectContractError
from bpi_dataset_retention_archiver.archive_store import RetentionArchiveContractError
from bpi_dataset_retention_archiver.worker import failure_code, sanitize_error


class WorkerErrorTest(unittest.TestCase):
    def test_failure_codes_preserve_archive_boundary(self) -> None:
        self.assertEqual(
            "SOURCE_OBJECT_CONTRACT_VIOLATION",
            failure_code(SourceObjectContractError("source drift")),
        )
        self.assertEqual(
            "RETENTION_ARCHIVE_CONTRACT_VIOLATION",
            failure_code(RetentionArchiveContractError("lock drift")),
        )

    def test_secrets_are_redacted(self) -> None:
        detail = sanitize_error(
            RuntimeError(
                "credential=client:secret password=hunter2 "
                "access_token=oauth-token Authorization: Bearer jwt-token "
                "http://user:pass@minio"
            )
        )
        self.assertNotIn("client:secret", detail)
        self.assertNotIn("hunter2", detail)
        self.assertNotIn("oauth-token", detail)
        self.assertNotIn("jwt-token", detail)
        self.assertNotIn("user:pass", detail)
        self.assertIn("[REDACTED]", detail)


if __name__ == "__main__":
    unittest.main()
