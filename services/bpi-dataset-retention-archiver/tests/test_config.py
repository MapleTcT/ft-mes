from __future__ import annotations

import os
import unittest
from unittest.mock import patch

from bpi_dataset_retention_archiver.config import ConfigurationError, Settings


class SettingsTest(unittest.TestCase):
    def test_disabled_defaults_are_safe(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            settings = Settings.from_environment()
        self.assertFalse(settings.enabled)
        self.assertEqual("GOVERNANCE", settings.retention_mode)
        self.assertEqual(30, settings.retention_days)
        self.assertFalse(settings.legal_hold_enabled)
        self.assertEqual("bpi-dataset-recovery", settings.recovery_bucket)

    def test_enabled_worker_requires_database_and_minio_credentials(self) -> None:
        with patch.dict(
            os.environ,
            {"BPI_DATASET_RETENTION_ARCHIVER_ENABLED": "true"},
            clear=True,
        ):
            with self.assertRaises(ConfigurationError) as raised:
                Settings.from_environment()
        self.assertIn("required when the retention archiver is enabled", str(raised.exception))

    def test_object_lock_policy_is_bounded_and_explicit(self) -> None:
        environment = {
            "BPI_DATASET_RETENTION_ARCHIVER_ENABLED": "true",
            "BPI_DATASET_RETENTION_ARCHIVER_DATABASE_URL": "postgresql://test",
            "BPI_DATASET_RETENTION_MINIO_ENDPOINT": "https://minio.example.test",
            "BPI_DATASET_RETENTION_MINIO_ACCESS_KEY": "retention-archiver",
            "BPI_DATASET_RETENTION_MINIO_SECRET_KEY": "secret",
            "BPI_DATASET_RETENTION_MODE": "compliance",
            "BPI_DATASET_RETENTION_DAYS": "365",
            "BPI_DATASET_RETENTION_LEGAL_HOLD": "true",
        }
        with patch.dict(os.environ, environment, clear=True):
            settings = Settings.from_environment()
        self.assertTrue(settings.enabled)
        self.assertEqual("minio.example.test", settings.minio_endpoint)
        self.assertTrue(settings.minio_secure)
        self.assertEqual("COMPLIANCE", settings.retention_mode)
        self.assertEqual(365, settings.retention_days)
        self.assertTrue(settings.legal_hold_enabled)

    def test_invalid_retention_mode_fails_closed(self) -> None:
        with patch.dict(
            os.environ,
            {"BPI_DATASET_RETENTION_MODE": "NONE"},
            clear=True,
        ):
            with self.assertRaises(ConfigurationError):
                Settings.from_environment()


if __name__ == "__main__":
    unittest.main()
