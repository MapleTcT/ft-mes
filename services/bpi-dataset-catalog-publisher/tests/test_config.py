import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from bpi_dataset_catalog_publisher.config import ConfigurationError, Settings


class SettingsTest(unittest.TestCase):
    def test_disabled_is_the_default_and_needs_no_credentials(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            settings = Settings.from_environment()
        self.assertFalse(settings.enabled)
        self.assertEqual("ft_mes_bpi", settings.catalog_name)
        self.assertEqual(19094, settings.health_port)

    def test_enabled_requires_source_and_catalog_contracts(self) -> None:
        with patch.dict(
            os.environ,
            {"BPI_DATASET_CATALOG_PUBLISHER_ENABLED": "true"},
            clear=True,
        ):
            with self.assertRaises(ConfigurationError) as raised:
                Settings.from_environment()
        self.assertIn("BPI_DATASET_CATALOG_SOURCE_MINIO_ENDPOINT", str(raised.exception))
        self.assertIn("BPI_ICEBERG_CATALOG_URI", str(raised.exception))

    def test_enabled_normalizes_endpoints_without_exposing_warehouse_keys(self) -> None:
        environment = {
            "BPI_DATASET_CATALOG_PUBLISHER_ENABLED": "true",
            "BPI_DATASET_CATALOG_PUBLISHER_DATABASE_URL": "postgresql://publisher@db/bpi",
            "BPI_DATASET_CATALOG_SOURCE_MINIO_ENDPOINT": "https://source-minio:9000",
            "BPI_DATASET_CATALOG_SOURCE_MINIO_ACCESS_KEY": "source-reader",
            "BPI_DATASET_CATALOG_SOURCE_MINIO_SECRET_KEY": "source-secret",
            "BPI_ICEBERG_CATALOG_URI": "http://polaris:8181/api/catalog/",
            "BPI_ICEBERG_CATALOG_CREDENTIAL": "publisher:secret",
            "BPI_ICEBERG_CATALOG_OAUTH2_SERVER_URI": "http://polaris:8181/api/catalog/v1/oauth/tokens",
            "BPI_ICEBERG_S3_ENDPOINT": "http://warehouse-minio:9000",
        }
        with patch.dict(os.environ, environment, clear=True):
            settings = Settings.from_environment()
        self.assertTrue(settings.enabled)
        self.assertEqual("source-minio:9000", settings.minio_endpoint)
        self.assertTrue(settings.minio_secure)
        self.assertEqual("http://polaris:8181/api/catalog", settings.catalog_uri)
        self.assertEqual("http://warehouse-minio:9000", settings.iceberg_s3_endpoint)

    def test_enabled_reads_a_private_bootstrap_credential_file(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            credential_path = Path(directory) / "publisher-credential"
            credential_path.write_text("generated-client:generated-secret\n", encoding="utf-8")
            credential_path.chmod(0o600)
            environment = {
                "BPI_DATASET_CATALOG_PUBLISHER_ENABLED": "true",
                "BPI_DATASET_CATALOG_PUBLISHER_DATABASE_URL": "postgresql://publisher@db/bpi",
                "BPI_DATASET_CATALOG_SOURCE_MINIO_ENDPOINT": "source-minio:9000",
                "BPI_DATASET_CATALOG_SOURCE_MINIO_ACCESS_KEY": "source-reader",
                "BPI_DATASET_CATALOG_SOURCE_MINIO_SECRET_KEY": "source-secret",
                "BPI_ICEBERG_CATALOG_URI": "http://polaris:8181/api/catalog",
                "BPI_ICEBERG_CATALOG_CREDENTIAL_FILE": str(credential_path),
                "BPI_ICEBERG_CATALOG_OAUTH2_SERVER_URI": "http://polaris:8181/api/catalog/v1/oauth/tokens",
                "BPI_ICEBERG_S3_ENDPOINT": "http://warehouse-minio:9000",
            }
            with patch.dict(os.environ, environment, clear=True):
                settings = Settings.from_environment()
        self.assertEqual("generated-client:generated-secret", settings.catalog_credential)
        self.assertEqual(str(credential_path), settings.catalog_credential_file)

    def test_bootstrap_credential_file_must_be_private(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            credential_path = Path(directory) / "publisher-credential"
            credential_path.write_text("client:secret", encoding="utf-8")
            credential_path.chmod(0o640)
            with patch.dict(
                os.environ,
                {"BPI_ICEBERG_CATALOG_CREDENTIAL_FILE": str(credential_path)},
                clear=True,
            ):
                with self.assertRaises(ConfigurationError) as raised:
                    Settings.from_environment()
        self.assertIn("group/world accessible", str(raised.exception))


if __name__ == "__main__":
    unittest.main()
