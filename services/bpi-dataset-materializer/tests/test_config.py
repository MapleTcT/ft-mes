import os
import tomllib
import unittest
from pathlib import Path
from unittest.mock import patch

from bpi_dataset_materializer.config import ConfigurationError, Settings


class SettingsTest(unittest.TestCase):
    def test_runtime_requirements_match_project_pins(self):
        root = Path(__file__).resolve().parents[1]
        project = tomllib.loads((root / "pyproject.toml").read_text())
        requirements = {
            line.strip()
            for line in (root / "requirements.runtime.txt").read_text().splitlines()
            if line.strip() and not line.startswith("setuptools==")
        }
        self.assertEqual(set(project["project"]["dependencies"]), requirements)

    def test_disabled_worker_needs_no_runtime_secrets(self):
        with patch.dict(os.environ, {"BPI_DATASET_MATERIALIZER_ENABLED": "false"}, clear=True):
            settings = Settings.from_environment()
        self.assertFalse(settings.enabled)
        self.assertIsNone(settings.database_url)

    def test_enabled_worker_requires_database_and_object_store(self):
        with patch.dict(os.environ, {"BPI_DATASET_MATERIALIZER_ENABLED": "true"}, clear=True):
            with self.assertRaises(ConfigurationError):
                Settings.from_environment()

    def test_https_endpoint_sets_secure_transport(self):
        environment = {
            "BPI_DATASET_MATERIALIZER_ENABLED": "true",
            "BPI_DATASET_MATERIALIZER_DATABASE_URL": "postgresql://worker:test@db/bpi",
            "BPI_DATASET_MINIO_ENDPOINT": "https://minio.example.test:9000",
            "BPI_DATASET_MINIO_ACCESS_KEY": "worker",
            "BPI_DATASET_MINIO_SECRET_KEY": "test-secret",
        }
        with patch.dict(os.environ, environment, clear=True):
            settings = Settings.from_environment()
        self.assertEqual("minio.example.test:9000", settings.minio_endpoint)
        self.assertTrue(settings.minio_secure)


if __name__ == "__main__":
    unittest.main()
