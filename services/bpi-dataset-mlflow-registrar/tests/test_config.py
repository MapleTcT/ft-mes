import os
import unittest
from unittest.mock import patch

from bpi_dataset_mlflow_registrar.config import ConfigurationError, Settings


class SettingsTest(unittest.TestCase):
    def test_disabled_is_the_safe_default(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            settings = Settings.from_environment()
        self.assertFalse(settings.enabled)
        self.assertIsNone(settings.tracking_uri)

    def test_enabled_requires_database_and_tracking_server(self) -> None:
        with patch.dict(
            os.environ,
            {"BPI_DATASET_MLFLOW_REGISTRAR_ENABLED": "true"},
            clear=True,
        ):
            with self.assertRaises(ConfigurationError):
                Settings.from_environment()

    def test_tracking_uri_rejects_embedded_credentials(self) -> None:
        with patch.dict(
            os.environ,
            {
                "BPI_DATASET_MLFLOW_REGISTRAR_ENABLED": "true",
                "BPI_DATASET_MLFLOW_REGISTRAR_DATABASE_URL": "postgresql://db/bpi",
                "BPI_MLFLOW_TRACKING_URI": "http://user:secret@mlflow:5000",
            },
            clear=True,
        ):
            with self.assertRaises(ConfigurationError):
                Settings.from_environment()


if __name__ == "__main__":
    unittest.main()
