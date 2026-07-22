from __future__ import annotations

import os
from dataclasses import dataclass
from urllib.parse import urlparse

from . import ARTIFACT_SCHEMA_VERSION, MATERIALIZER_VERSION


class ConfigurationError(ValueError):
    pass


def _boolean(name: str, default: bool) -> bool:
    raw = os.getenv(name, str(default)).strip().lower()
    if raw in {"1", "true", "yes", "on"}:
        return True
    if raw in {"0", "false", "no", "off"}:
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


@dataclass(frozen=True)
class Settings:
    enabled: bool
    database_url: str | None
    minio_endpoint: str | None
    minio_secure: bool
    minio_access_key: str | None
    minio_secret_key: str | None
    minio_bucket: str
    poll_interval_seconds: int
    claim_timeout_seconds: int
    max_attempts: int
    health_port: int
    work_directory: str
    artifact_schema_version: str = ARTIFACT_SCHEMA_VERSION
    materializer_version: str = MATERIALIZER_VERSION

    @classmethod
    def from_environment(cls) -> "Settings":
        enabled = _boolean("BPI_DATASET_MATERIALIZER_ENABLED", False)
        endpoint = os.getenv("BPI_DATASET_MINIO_ENDPOINT", "").strip() or None
        secure = _boolean("BPI_DATASET_MINIO_SECURE", False)
        if endpoint:
            endpoint, inferred_secure = normalize_minio_endpoint(endpoint)
            if inferred_secure is not None:
                secure = inferred_secure

        settings = cls(
            enabled=enabled,
            database_url=(os.getenv("BPI_DATASET_MATERIALIZER_DATABASE_URL", "").strip() or None),
            minio_endpoint=endpoint,
            minio_secure=secure,
            minio_access_key=(os.getenv("BPI_DATASET_MINIO_ACCESS_KEY", "").strip() or None),
            minio_secret_key=(os.getenv("BPI_DATASET_MINIO_SECRET_KEY", "").strip() or None),
            minio_bucket=os.getenv("BPI_DATASET_MINIO_BUCKET", "bpi-datasets").strip(),
            poll_interval_seconds=_integer(
                "BPI_DATASET_MATERIALIZER_POLL_SECONDS", 2, 1, 60),
            claim_timeout_seconds=_integer(
                "BPI_DATASET_MATERIALIZER_CLAIM_TIMEOUT_SECONDS", 300, 30, 86400),
            max_attempts=_integer("BPI_DATASET_MATERIALIZER_MAX_ATTEMPTS", 3, 1, 20),
            health_port=_integer(
                "BPI_DATASET_MATERIALIZER_HEALTH_PORT", 19093, 1024, 65535),
            work_directory=os.getenv(
                "BPI_DATASET_MATERIALIZER_WORK_DIRECTORY",
                "/var/lib/bpi-materializer",
            ).strip(),
        )
        settings.validate()
        return settings

    def validate(self) -> None:
        if not self.minio_bucket or len(self.minio_bucket) > 63:
            raise ConfigurationError("BPI_DATASET_MINIO_BUCKET must be a valid bucket name")
        if not self.work_directory:
            raise ConfigurationError("BPI_DATASET_MATERIALIZER_WORK_DIRECTORY is required")
        if not self.enabled:
            return
        pg_environment = all(
            os.getenv(name, "").strip()
            for name in ("PGHOST", "PGDATABASE", "PGUSER", "PGPASSWORD")
        )
        required = {
            "BPI_DATASET_MINIO_ENDPOINT": self.minio_endpoint,
            "BPI_DATASET_MINIO_ACCESS_KEY": self.minio_access_key,
            "BPI_DATASET_MINIO_SECRET_KEY": self.minio_secret_key,
        }
        missing = [name for name, value in required.items() if not value]
        if not self.database_url and not pg_environment:
            missing.append("BPI_DATASET_MATERIALIZER_DATABASE_URL or PG* connection variables")
        if missing:
            raise ConfigurationError(
                f"{', '.join(missing)} required when the materializer is enabled")


def normalize_minio_endpoint(value: str) -> tuple[str, bool | None]:
    if "://" not in value:
        if "/" in value:
            raise ConfigurationError("BPI_DATASET_MINIO_ENDPOINT must not contain a path")
        return value, None
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise ConfigurationError("BPI_DATASET_MINIO_ENDPOINT must use http or https")
    if parsed.path not in {"", "/"} or parsed.query or parsed.fragment:
        raise ConfigurationError("BPI_DATASET_MINIO_ENDPOINT must not contain a path")
    return parsed.netloc, parsed.scheme == "https"
