from __future__ import annotations

import os
from dataclasses import dataclass
from urllib.parse import urlparse

from . import ARCHIVER_VERSION, ARCHIVE_PROFILE


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


def normalize_minio_endpoint(value: str) -> tuple[str, bool | None]:
    if "://" not in value:
        if "/" in value:
            raise ConfigurationError(
                "BPI_DATASET_RETENTION_MINIO_ENDPOINT must not contain a path"
            )
        return value, None
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise ConfigurationError(
            "BPI_DATASET_RETENTION_MINIO_ENDPOINT must use http or https"
        )
    if parsed.path not in {"", "/"} or parsed.query or parsed.fragment:
        raise ConfigurationError(
            "BPI_DATASET_RETENTION_MINIO_ENDPOINT must not contain a path"
        )
    return parsed.netloc, parsed.scheme == "https"


@dataclass(frozen=True)
class Settings:
    enabled: bool
    database_url: str | None
    minio_endpoint: str | None
    minio_secure: bool
    minio_access_key: str | None
    minio_secret_key: str | None
    recovery_bucket: str
    retention_mode: str
    retention_days: int
    legal_hold_enabled: bool
    poll_interval_seconds: int
    claim_timeout_seconds: int
    max_attempts: int
    health_port: int
    work_directory: str
    archiver_version: str = ARCHIVER_VERSION
    archive_profile: str = ARCHIVE_PROFILE

    @classmethod
    def from_environment(cls) -> "Settings":
        enabled = _boolean("BPI_DATASET_RETENTION_ARCHIVER_ENABLED", False)
        endpoint = os.getenv("BPI_DATASET_RETENTION_MINIO_ENDPOINT", "").strip() or None
        secure = _boolean("BPI_DATASET_RETENTION_MINIO_SECURE", False)
        if endpoint:
            endpoint, inferred = normalize_minio_endpoint(endpoint)
            if inferred is not None:
                secure = inferred
        settings = cls(
            enabled=enabled,
            database_url=(
                os.getenv("BPI_DATASET_RETENTION_ARCHIVER_DATABASE_URL", "").strip()
                or None
            ),
            minio_endpoint=endpoint,
            minio_secure=secure,
            minio_access_key=(
                os.getenv("BPI_DATASET_RETENTION_MINIO_ACCESS_KEY", "").strip()
                or None
            ),
            minio_secret_key=(
                os.getenv("BPI_DATASET_RETENTION_MINIO_SECRET_KEY", "").strip()
                or None
            ),
            recovery_bucket=os.getenv(
                "BPI_DATASET_RECOVERY_BUCKET", "bpi-dataset-recovery"
            ).strip(),
            retention_mode=os.getenv(
                "BPI_DATASET_RETENTION_MODE", "GOVERNANCE"
            ).strip().upper(),
            retention_days=_integer("BPI_DATASET_RETENTION_DAYS", 30, 1, 36500),
            legal_hold_enabled=_boolean("BPI_DATASET_RETENTION_LEGAL_HOLD", False),
            poll_interval_seconds=_integer(
                "BPI_DATASET_RETENTION_ARCHIVER_POLL_SECONDS", 2, 1, 60
            ),
            claim_timeout_seconds=_integer(
                "BPI_DATASET_RETENTION_ARCHIVER_CLAIM_TIMEOUT_SECONDS",
                300,
                30,
                86400,
            ),
            max_attempts=_integer(
                "BPI_DATASET_RETENTION_ARCHIVER_MAX_ATTEMPTS", 3, 1, 20
            ),
            health_port=_integer(
                "BPI_DATASET_RETENTION_ARCHIVER_HEALTH_PORT", 19095, 1024, 65535
            ),
            work_directory=os.getenv(
                "BPI_DATASET_RETENTION_ARCHIVER_WORK_DIRECTORY",
                "/var/lib/bpi-retention-archiver",
            ).strip(),
        )
        settings.validate()
        return settings

    def validate(self) -> None:
        if not self.recovery_bucket or len(self.recovery_bucket) > 63:
            raise ConfigurationError("BPI_DATASET_RECOVERY_BUCKET is invalid")
        if any(character not in "abcdefghijklmnopqrstuvwxyz0123456789.-"
               for character in self.recovery_bucket):
            raise ConfigurationError(
                "BPI_DATASET_RECOVERY_BUCKET contains unsupported characters"
            )
        if self.retention_mode not in {"GOVERNANCE", "COMPLIANCE"}:
            raise ConfigurationError(
                "BPI_DATASET_RETENTION_MODE must be GOVERNANCE or COMPLIANCE"
            )
        if not self.work_directory:
            raise ConfigurationError(
                "BPI_DATASET_RETENTION_ARCHIVER_WORK_DIRECTORY is required"
            )
        if not self.enabled:
            return
        pg_environment = all(
            os.getenv(name, "").strip()
            for name in ("PGHOST", "PGDATABASE", "PGUSER", "PGPASSWORD")
        )
        missing = []
        if not self.database_url and not pg_environment:
            missing.append(
                "BPI_DATASET_RETENTION_ARCHIVER_DATABASE_URL or PG* connection variables"
            )
        required = {
            "BPI_DATASET_RETENTION_MINIO_ENDPOINT": self.minio_endpoint,
            "BPI_DATASET_RETENTION_MINIO_ACCESS_KEY": self.minio_access_key,
            "BPI_DATASET_RETENTION_MINIO_SECRET_KEY": self.minio_secret_key,
        }
        missing.extend(name for name, value in required.items() if not value)
        if missing:
            raise ConfigurationError(
                f"{', '.join(missing)} required when the retention archiver is enabled"
            )
