from __future__ import annotations

import os
from dataclasses import dataclass
from urllib.parse import urlparse

from . import REGISTRAR_VERSION, TRACKING_PROFILE


class ConfigurationError(ValueError):
    pass


def _boolean(name: str, default: bool) -> bool:
    value = os.getenv(name, str(default)).strip().lower()
    if value in {"1", "true", "yes", "on"}:
        return True
    if value in {"0", "false", "no", "off"}:
        return False
    raise ConfigurationError(f"{name} must be true or false")


def _integer(name: str, default: int, minimum: int, maximum: int) -> int:
    try:
        value = int(os.getenv(name, str(default)))
    except ValueError as exception:
        raise ConfigurationError(f"{name} must be an integer") from exception
    if value < minimum or value > maximum:
        raise ConfigurationError(f"{name} must be between {minimum} and {maximum}")
    return value


def normalize_tracking_uri(value: str) -> str:
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise ConfigurationError("BPI_MLFLOW_TRACKING_URI must use http or https")
    if parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ConfigurationError(
            "BPI_MLFLOW_TRACKING_URI must not contain credentials, query or fragment"
        )
    if parsed.path not in {"", "/"}:
        raise ConfigurationError("BPI_MLFLOW_TRACKING_URI must not contain a path")
    return f"{parsed.scheme}://{parsed.netloc}"


@dataclass(frozen=True)
class Settings:
    enabled: bool
    database_url: str | None
    tracking_uri: str | None
    tracking_token: str | None
    request_timeout_seconds: int
    poll_interval_seconds: int
    claim_timeout_seconds: int
    max_attempts: int
    health_port: int
    registrar_version: str = REGISTRAR_VERSION
    tracking_profile: str = TRACKING_PROFILE

    @classmethod
    def from_environment(cls) -> "Settings":
        enabled = _boolean("BPI_DATASET_MLFLOW_REGISTRAR_ENABLED", False)
        tracking_uri = os.getenv("BPI_MLFLOW_TRACKING_URI", "").strip() or None
        if tracking_uri:
            tracking_uri = normalize_tracking_uri(tracking_uri)
        settings = cls(
            enabled=enabled,
            database_url=(
                os.getenv("BPI_DATASET_MLFLOW_REGISTRAR_DATABASE_URL", "").strip()
                or None
            ),
            tracking_uri=tracking_uri,
            tracking_token=(os.getenv("BPI_MLFLOW_TRACKING_TOKEN", "").strip() or None),
            request_timeout_seconds=_integer(
                "BPI_MLFLOW_REQUEST_TIMEOUT_SECONDS", 20, 1, 300
            ),
            poll_interval_seconds=_integer(
                "BPI_DATASET_MLFLOW_REGISTRAR_POLL_SECONDS", 2, 1, 3600
            ),
            claim_timeout_seconds=_integer(
                "BPI_DATASET_MLFLOW_REGISTRAR_CLAIM_TIMEOUT_SECONDS",
                300,
                30,
                86400,
            ),
            max_attempts=_integer(
                "BPI_DATASET_MLFLOW_REGISTRAR_MAX_ATTEMPTS", 3, 1, 20
            ),
            health_port=_integer(
                "BPI_DATASET_MLFLOW_REGISTRAR_HEALTH_PORT", 19096, 1024, 65535
            ),
        )
        settings.validate()
        return settings

    def validate(self) -> None:
        if not self.enabled:
            return
        pg_environment = all(
            os.getenv(name, "").strip()
            for name in ("PGHOST", "PGDATABASE", "PGUSER", "PGPASSWORD")
        )
        missing = []
        if not self.database_url and not pg_environment:
            missing.append(
                "BPI_DATASET_MLFLOW_REGISTRAR_DATABASE_URL or PG* connection variables"
            )
        if not self.tracking_uri:
            missing.append("BPI_MLFLOW_TRACKING_URI")
        if missing:
            raise ConfigurationError(
                f"{', '.join(missing)} required when the MLflow registrar is enabled"
            )
