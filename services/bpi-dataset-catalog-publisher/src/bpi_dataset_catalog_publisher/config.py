from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlparse

from . import CATALOG_NAME, PUBLISHER_VERSION


class ConfigurationError(ValueError):
    pass


def _catalog_credential(enabled: bool) -> tuple[str | None, str | None]:
    inline = os.getenv("BPI_ICEBERG_CATALOG_CREDENTIAL", "").strip() or None
    credential_file = (
        os.getenv("BPI_ICEBERG_CATALOG_CREDENTIAL_FILE", "").strip() or None
    )
    if inline and credential_file:
        raise ConfigurationError(
            "BPI_ICEBERG_CATALOG_CREDENTIAL and "
            "BPI_ICEBERG_CATALOG_CREDENTIAL_FILE are mutually exclusive"
        )
    if not credential_file:
        return inline, None
    path = Path(credential_file)
    if not path.is_absolute():
        raise ConfigurationError(
            "BPI_ICEBERG_CATALOG_CREDENTIAL_FILE must be an absolute path"
        )
    if not path.exists():
        if enabled:
            raise ConfigurationError(
                "BPI_ICEBERG_CATALOG_CREDENTIAL_FILE does not exist"
            )
        return None, credential_file
    if path.stat().st_mode & 0o077:
        raise ConfigurationError(
            "BPI_ICEBERG_CATALOG_CREDENTIAL_FILE must not be group/world accessible"
        )
    credential = path.read_text(encoding="utf-8").strip()
    if "\n" in credential or credential.count(":") != 1:
        raise ConfigurationError(
            "BPI_ICEBERG_CATALOG_CREDENTIAL_FILE must contain clientId:clientSecret"
        )
    client_id, client_secret = credential.split(":", 1)
    if not client_id or not client_secret:
        raise ConfigurationError(
            "BPI_ICEBERG_CATALOG_CREDENTIAL_FILE contains an empty credential"
        )
    return credential, credential_file


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


def _url(name: str, value: str | None, *, allow_path: bool) -> str | None:
    if not value:
        return None
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise ConfigurationError(f"{name} must use http or https")
    if (not allow_path and parsed.path not in {"", "/"}) or parsed.query or parsed.fragment:
        raise ConfigurationError(f"{name} contains an unsupported path, query, or fragment")
    return value.rstrip("/")


def normalize_minio_endpoint(value: str) -> tuple[str, bool | None]:
    if "://" not in value:
        if "/" in value:
            raise ConfigurationError(
                "BPI_DATASET_CATALOG_SOURCE_MINIO_ENDPOINT must not contain a path"
            )
        return value, None
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise ConfigurationError(
            "BPI_DATASET_CATALOG_SOURCE_MINIO_ENDPOINT must use http or https"
        )
    if parsed.path not in {"", "/"} or parsed.query or parsed.fragment:
        raise ConfigurationError(
            "BPI_DATASET_CATALOG_SOURCE_MINIO_ENDPOINT must not contain a path"
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
    catalog_uri: str | None
    catalog_warehouse: str
    catalog_credential: str | None
    catalog_oauth2_server_uri: str | None
    catalog_realm: str
    iceberg_s3_endpoint: str | None
    iceberg_s3_region: str
    poll_interval_seconds: int
    claim_timeout_seconds: int
    max_attempts: int
    health_port: int
    work_directory: str
    catalog_credential_file: str | None = None
    catalog_name: str = CATALOG_NAME
    publisher_version: str = PUBLISHER_VERSION

    @classmethod
    def from_environment(cls) -> "Settings":
        enabled = _boolean("BPI_DATASET_CATALOG_PUBLISHER_ENABLED", False)
        catalog_credential, catalog_credential_file = _catalog_credential(enabled)
        minio_endpoint = (
            os.getenv("BPI_DATASET_CATALOG_SOURCE_MINIO_ENDPOINT", "").strip()
            or None
        )
        minio_secure = _boolean(
            "BPI_DATASET_CATALOG_SOURCE_MINIO_SECURE", False
        )
        if minio_endpoint:
            minio_endpoint, inferred_secure = normalize_minio_endpoint(minio_endpoint)
            if inferred_secure is not None:
                minio_secure = inferred_secure
        settings = cls(
            enabled=enabled,
            database_url=(
                os.getenv("BPI_DATASET_CATALOG_PUBLISHER_DATABASE_URL", "").strip()
                or None
            ),
            minio_endpoint=minio_endpoint,
            minio_secure=minio_secure,
            minio_access_key=(
                os.getenv("BPI_DATASET_CATALOG_SOURCE_MINIO_ACCESS_KEY", "").strip()
                or None
            ),
            minio_secret_key=(
                os.getenv("BPI_DATASET_CATALOG_SOURCE_MINIO_SECRET_KEY", "").strip()
                or None
            ),
            catalog_uri=_url(
                "BPI_ICEBERG_CATALOG_URI",
                os.getenv("BPI_ICEBERG_CATALOG_URI", "").strip() or None,
                allow_path=True,
            ),
            catalog_warehouse=os.getenv(
                "BPI_ICEBERG_CATALOG_WAREHOUSE", CATALOG_NAME
            ).strip(),
            catalog_credential=catalog_credential,
            catalog_oauth2_server_uri=_url(
                "BPI_ICEBERG_CATALOG_OAUTH2_SERVER_URI",
                os.getenv("BPI_ICEBERG_CATALOG_OAUTH2_SERVER_URI", "").strip()
                or None,
                allow_path=True,
            ),
            catalog_realm=os.getenv(
                "BPI_ICEBERG_CATALOG_REALM", "POLARIS"
            ).strip(),
            iceberg_s3_endpoint=_url(
                "BPI_ICEBERG_S3_ENDPOINT",
                os.getenv("BPI_ICEBERG_S3_ENDPOINT", "").strip() or None,
                allow_path=False,
            ),
            iceberg_s3_region=os.getenv(
                "BPI_ICEBERG_S3_REGION", "us-east-1"
            ).strip(),
            poll_interval_seconds=_integer(
                "BPI_DATASET_CATALOG_PUBLISHER_POLL_SECONDS", 2, 1, 60
            ),
            claim_timeout_seconds=_integer(
                "BPI_DATASET_CATALOG_PUBLISHER_CLAIM_TIMEOUT_SECONDS",
                300,
                30,
                86400,
            ),
            max_attempts=_integer(
                "BPI_DATASET_CATALOG_PUBLISHER_MAX_ATTEMPTS", 3, 1, 20
            ),
            health_port=_integer(
                "BPI_DATASET_CATALOG_PUBLISHER_HEALTH_PORT", 19094, 1024, 65535
            ),
            work_directory=os.getenv(
                "BPI_DATASET_CATALOG_PUBLISHER_WORK_DIRECTORY",
                "/var/lib/bpi-catalog-publisher",
            ).strip(),
            catalog_credential_file=catalog_credential_file,
        )
        settings.validate()
        return settings

    def validate(self) -> None:
        if not self.catalog_warehouse:
            raise ConfigurationError("BPI_ICEBERG_CATALOG_WAREHOUSE is required")
        if not self.catalog_realm:
            raise ConfigurationError("BPI_ICEBERG_CATALOG_REALM is required")
        if not self.iceberg_s3_region:
            raise ConfigurationError("BPI_ICEBERG_S3_REGION is required")
        if not self.work_directory:
            raise ConfigurationError(
                "BPI_DATASET_CATALOG_PUBLISHER_WORK_DIRECTORY is required"
            )
        if not self.enabled:
            return
        pg_environment = all(
            os.getenv(name, "").strip()
            for name in ("PGHOST", "PGDATABASE", "PGUSER", "PGPASSWORD")
        )
        required = {
            "BPI_DATASET_CATALOG_SOURCE_MINIO_ENDPOINT": self.minio_endpoint,
            "BPI_DATASET_CATALOG_SOURCE_MINIO_ACCESS_KEY": self.minio_access_key,
            "BPI_DATASET_CATALOG_SOURCE_MINIO_SECRET_KEY": self.minio_secret_key,
            "BPI_ICEBERG_CATALOG_URI": self.catalog_uri,
            "BPI_ICEBERG_CATALOG_CREDENTIAL": self.catalog_credential,
            "BPI_ICEBERG_CATALOG_OAUTH2_SERVER_URI": self.catalog_oauth2_server_uri,
            "BPI_ICEBERG_S3_ENDPOINT": self.iceberg_s3_endpoint,
        }
        missing = [name for name, value in required.items() if not value]
        if not self.database_url and not pg_environment:
            missing.append(
                "BPI_DATASET_CATALOG_PUBLISHER_DATABASE_URL or PG* connection variables"
            )
        if missing:
            raise ConfigurationError(
                f"{', '.join(missing)} required when the catalog publisher is enabled"
            )
