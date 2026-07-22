from __future__ import annotations

import hashlib
import json
import os
import re
import sys
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace
from typing import Any
from urllib.parse import urlparse
from uuid import UUID

from minio import Minio
from pyiceberg.catalog import Catalog, load_catalog

from .models import VerifiedSource
from .source_object import semantic_checksum, validate_local_source


RECOVERY_SCHEMA_VERSION = "bpi.dataset-recovery.v1"
RECOVERY_PROFILE = "bpi-dataset-recovery-v1"
RECOVERY_REHEARSAL_VERSION = "bpi-dataset-recovery-rehearsal/0.1.0"
SHA256_PATTERN = re.compile(r"^[0-9a-f]{64}$")


class RecoveryRehearsalError(RuntimeError):
    pass


def _canonical_json(value: dict[str, Any]) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=True,
        separators=(",", ":"),
        sort_keys=True,
    ).encode()


def _sha256(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def _required_environment(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RecoveryRehearsalError(f"{name} is required")
    return value


def _boolean_environment(name: str, default: bool = False) -> bool:
    value = os.getenv(name, str(default)).strip().lower()
    if value in {"true", "1", "yes", "on"}:
        return True
    if value in {"false", "0", "no", "off"}:
        return False
    raise RecoveryRehearsalError(f"{name} must be true or false")


def _http_url(name: str, value: str, *, allow_path: bool) -> str:
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise RecoveryRehearsalError(f"{name} must use http or https")
    if (not allow_path and parsed.path not in {"", "/"}) or parsed.query or parsed.fragment:
        raise RecoveryRehearsalError(f"{name} contains an unsupported URL component")
    return value.rstrip("/")


def _minio_endpoint(value: str) -> tuple[str, bool]:
    if "://" not in value:
        if "/" in value:
            raise RecoveryRehearsalError(
                "BPI_DATASET_RECOVERY_MINIO_ENDPOINT must not contain a path"
            )
        return value, False
    parsed = urlparse(value)
    if (
        parsed.scheme not in {"http", "https"}
        or not parsed.netloc
        or parsed.path not in {"", "/"}
        or parsed.query
        or parsed.fragment
    ):
        raise RecoveryRehearsalError(
            "BPI_DATASET_RECOVERY_MINIO_ENDPOINT must be an HTTP(S) endpoint"
        )
    return parsed.netloc, parsed.scheme == "https"


def _catalog_admin_credential() -> tuple[str, str]:
    credential_file = Path(
        _required_environment("BPI_DATASET_RECOVERY_CATALOG_ADMIN_CREDENTIAL_FILE")
    )
    if not credential_file.is_absolute():
        raise RecoveryRehearsalError(
            "BPI_DATASET_RECOVERY_CATALOG_ADMIN_CREDENTIAL_FILE must be absolute"
        )
    if not credential_file.is_file():
        raise RecoveryRehearsalError("catalog admin credential file does not exist")
    if credential_file.stat().st_mode & 0o077:
        raise RecoveryRehearsalError(
            "catalog admin credential file must not be group/world accessible"
        )
    raw = credential_file.read_text(encoding="utf-8").strip()
    if "\n" in raw or raw.count(":") != 1:
        raise RecoveryRehearsalError(
            "catalog admin credential file must contain clientId:clientSecret"
        )
    client_id, client_secret = raw.split(":", 1)
    if not client_id or not client_secret:
        raise RecoveryRehearsalError("catalog admin credential is incomplete")
    return raw, client_secret


def _uuid(value: Any, name: str) -> UUID:
    try:
        return UUID(str(value))
    except (TypeError, ValueError) as exception:
        raise RecoveryRehearsalError(f"{name} must be a UUID") from exception


def _sha(value: Any, name: str) -> str:
    text = str(value)
    if not SHA256_PATTERN.fullmatch(text):
        raise RecoveryRehearsalError(f"{name} must be a lowercase SHA-256")
    return text


def _integer(value: Any, name: str, *, minimum: int = 0) -> int:
    if isinstance(value, bool):
        raise RecoveryRehearsalError(f"{name} must be an integer")
    try:
        number = int(value)
    except (TypeError, ValueError) as exception:
        raise RecoveryRehearsalError(f"{name} must be an integer") from exception
    if number < minimum:
        raise RecoveryRehearsalError(f"{name} must be at least {minimum}")
    return number


def _mapping(value: Any, name: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise RecoveryRehearsalError(f"{name} must be an object")
    return value


def _text(value: Any, name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise RecoveryRehearsalError(f"{name} must be a non-empty string")
    return value.strip()


def _instant(value: Any, name: str) -> datetime:
    text = _text(value, name)
    try:
        parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
    except ValueError as exception:
        raise RecoveryRehearsalError(f"{name} must be an ISO-8601 instant") from exception
    if parsed.tzinfo is None:
        raise RecoveryRehearsalError(f"{name} must include a timezone")
    return parsed.astimezone(UTC)


@dataclass(frozen=True)
class RehearsalSettings:
    minio_endpoint: str
    minio_secure: bool
    minio_access_key: str
    minio_secret_key: str
    recovery_bucket: str
    warehouse_bucket: str
    manifest_object_key: str
    manifest_version_id: str
    manifest_sha256: str
    catalog_uri: str
    catalog_warehouse: str
    catalog_credential: str
    catalog_oauth2_server_uri: str
    catalog_realm: str
    iceberg_s3_endpoint: str
    iceberg_s3_region: str
    work_directory: Path
    report_path: Path | None
    reconcile_stale: bool
    catalog_name: str = "ft_mes_bpi"

    @classmethod
    def from_environment(cls) -> "RehearsalSettings":
        if not _boolean_environment(
            "BPI_DATASET_RECOVERY_REHEARSAL_ENABLED", False
        ):
            raise RecoveryRehearsalError(
                "BPI_DATASET_RECOVERY_REHEARSAL_ENABLED must be true"
            )
        endpoint, secure = _minio_endpoint(
            _required_environment("BPI_DATASET_RECOVERY_MINIO_ENDPOINT")
        )
        credential, _ = _catalog_admin_credential()
        manifest_sha = _sha(
            _required_environment("BPI_DATASET_RECOVERY_MANIFEST_SHA256"),
            "BPI_DATASET_RECOVERY_MANIFEST_SHA256",
        )
        report_value = os.getenv("BPI_DATASET_RECOVERY_REPORT_PATH", "").strip()
        work_directory = Path(
            os.getenv(
                "BPI_DATASET_RECOVERY_WORK_DIRECTORY",
                "/var/lib/bpi-catalog-publisher",
            ).strip()
        )
        if not work_directory.is_absolute():
            raise RecoveryRehearsalError(
                "BPI_DATASET_RECOVERY_WORK_DIRECTORY must be absolute"
            )
        report_path = Path(report_value) if report_value else None
        if report_path is not None and not report_path.is_absolute():
            raise RecoveryRehearsalError(
                "BPI_DATASET_RECOVERY_REPORT_PATH must be absolute"
            )
        recovery_bucket = _required_environment("BPI_DATASET_RECOVERY_BUCKET")
        warehouse_bucket = _required_environment(
            "BPI_DATASET_RECOVERY_WAREHOUSE_BUCKET"
        )
        if recovery_bucket == warehouse_bucket:
            raise RecoveryRehearsalError(
                "recovery archive and Iceberg warehouse buckets must differ"
            )
        return cls(
            minio_endpoint=endpoint,
            minio_secure=secure,
            minio_access_key=_required_environment(
                "BPI_DATASET_RECOVERY_MINIO_ACCESS_KEY"
            ),
            minio_secret_key=_required_environment(
                "BPI_DATASET_RECOVERY_MINIO_SECRET_KEY"
            ),
            recovery_bucket=recovery_bucket,
            warehouse_bucket=warehouse_bucket,
            manifest_object_key=_required_environment(
                "BPI_DATASET_RECOVERY_MANIFEST_OBJECT_KEY"
            ),
            manifest_version_id=_required_environment(
                "BPI_DATASET_RECOVERY_MANIFEST_VERSION_ID"
            ),
            manifest_sha256=manifest_sha,
            catalog_uri=_http_url(
                "BPI_DATASET_RECOVERY_CATALOG_URI",
                _required_environment("BPI_DATASET_RECOVERY_CATALOG_URI"),
                allow_path=True,
            ),
            catalog_warehouse=_required_environment(
                "BPI_DATASET_RECOVERY_CATALOG_WAREHOUSE"
            ),
            catalog_credential=credential,
            catalog_oauth2_server_uri=_http_url(
                "BPI_DATASET_RECOVERY_CATALOG_OAUTH2_SERVER_URI",
                _required_environment(
                    "BPI_DATASET_RECOVERY_CATALOG_OAUTH2_SERVER_URI"
                ),
                allow_path=True,
            ),
            catalog_realm=_required_environment(
                "BPI_DATASET_RECOVERY_CATALOG_REALM"
            ),
            iceberg_s3_endpoint=_http_url(
                "BPI_DATASET_RECOVERY_ICEBERG_S3_ENDPOINT",
                _required_environment(
                    "BPI_DATASET_RECOVERY_ICEBERG_S3_ENDPOINT"
                ),
                allow_path=False,
            ),
            iceberg_s3_region=_required_environment(
                "BPI_DATASET_RECOVERY_ICEBERG_S3_REGION"
            ),
            work_directory=work_directory,
            report_path=report_path,
            reconcile_stale=_boolean_environment(
                "BPI_DATASET_RECOVERY_RECONCILE_STALE", False
            ),
        )


@dataclass(frozen=True)
class RecoveryManifest:
    archive_id: UUID
    archive_profile: str
    catalog_publication_id: UUID
    materialization_id: UUID
    source_snapshot_id: UUID
    dataset_id: UUID
    dataset_code: str
    dataset_version: str
    tenant_id: str
    plant_id: str
    line_ids: tuple[str, ...]
    manifest_checksum: str
    source_content_sha256: str
    original_object_version_id: str
    source_byte_size: int
    source_row_count: int
    source_schema_json: dict[str, Any]
    retained_bucket: str
    retained_object_key: str
    retained_object_version_id: str
    original_table_identifier: str
    original_snapshot_id: int
    catalog_verified_row_count: int
    catalog_semantic_checksum: str
    retention_mode: str
    retain_until: datetime
    legal_hold_enabled: bool

    @classmethod
    def from_bytes(
        cls,
        payload: bytes,
        expected_sha256: str,
    ) -> "RecoveryManifest":
        if _sha256(payload) != _sha(expected_sha256, "manifest SHA-256"):
            raise RecoveryRehearsalError("recovery manifest SHA-256 does not match")
        try:
            document = json.loads(payload)
        except (UnicodeDecodeError, json.JSONDecodeError) as exception:
            raise RecoveryRehearsalError("recovery manifest is not valid JSON") from exception
        document = _mapping(document, "recovery manifest")
        if _canonical_json(document) != payload:
            raise RecoveryRehearsalError("recovery manifest is not canonical JSON")
        if document.get("schemaVersion") != RECOVERY_SCHEMA_VERSION:
            raise RecoveryRehearsalError("recovery manifest schema version is unsupported")
        archive = _mapping(document.get("archive"), "archive")
        source = _mapping(document.get("source"), "source")
        iceberg = _mapping(document.get("iceberg"), "iceberg")
        retention = _mapping(document.get("retention"), "retention")
        line_ids = archive.get("lineIds")
        if (
            not isinstance(line_ids, list)
            or not line_ids
            or any(not isinstance(item, str) or not item.strip() for item in line_ids)
            or line_ids != sorted(set(line_ids))
        ):
            raise RecoveryRehearsalError(
                "archive.lineIds must be a non-empty sorted unique string list"
            )
        profile = _text(archive.get("profile"), "archive.profile")
        if profile != RECOVERY_PROFILE:
            raise RecoveryRehearsalError("recovery archive profile is unsupported")
        mode = _text(retention.get("mode"), "retention.mode")
        if mode not in {"GOVERNANCE", "COMPLIANCE"}:
            raise RecoveryRehearsalError("retention.mode is unsupported")
        legal_hold = retention.get("legalHoldEnabled")
        if not isinstance(legal_hold, bool):
            raise RecoveryRehearsalError(
                "retention.legalHoldEnabled must be boolean"
            )
        source_schema = _mapping(source.get("schema"), "source.schema")
        if not isinstance(source_schema.get("fields"), list):
            raise RecoveryRehearsalError("source.schema.fields must be a list")
        return cls(
            archive_id=_uuid(archive.get("id"), "archive.id"),
            archive_profile=profile,
            catalog_publication_id=_uuid(
                archive.get("catalogPublicationId"),
                "archive.catalogPublicationId",
            ),
            materialization_id=_uuid(
                archive.get("materializationId"), "archive.materializationId"
            ),
            source_snapshot_id=_uuid(
                archive.get("snapshotId"), "archive.snapshotId"
            ),
            dataset_id=_uuid(archive.get("datasetId"), "archive.datasetId"),
            dataset_code=_text(archive.get("datasetCode"), "archive.datasetCode"),
            dataset_version=_text(
                archive.get("datasetVersion"), "archive.datasetVersion"
            ),
            tenant_id=_text(archive.get("tenantId"), "archive.tenantId"),
            plant_id=_text(archive.get("plantId"), "archive.plantId"),
            line_ids=tuple(line_ids),
            manifest_checksum=_sha(
                source.get("manifestChecksum"), "source.manifestChecksum"
            ),
            source_content_sha256=_sha(
                source.get("contentSha256"), "source.contentSha256"
            ),
            original_object_version_id=_text(
                source.get("originalObjectVersionId"),
                "source.originalObjectVersionId",
            ),
            source_byte_size=_integer(
                source.get("byteSize"), "source.byteSize", minimum=1
            ),
            source_row_count=_integer(
                source.get("rowCount"), "source.rowCount", minimum=0
            ),
            source_schema_json=source_schema,
            retained_bucket=_text(
                source.get("retainedBucket"), "source.retainedBucket"
            ),
            retained_object_key=_text(
                source.get("retainedObjectKey"), "source.retainedObjectKey"
            ),
            retained_object_version_id=_text(
                source.get("retainedObjectVersionId"),
                "source.retainedObjectVersionId",
            ),
            original_table_identifier=_text(
                iceberg.get("tableIdentifier"), "iceberg.tableIdentifier"
            ),
            original_snapshot_id=_integer(
                iceberg.get("snapshotId"), "iceberg.snapshotId", minimum=1
            ),
            catalog_verified_row_count=_integer(
                iceberg.get("verifiedRowCount"),
                "iceberg.verifiedRowCount",
                minimum=0,
            ),
            catalog_semantic_checksum=_sha(
                iceberg.get("semanticChecksum"), "iceberg.semanticChecksum"
            ),
            retention_mode=mode,
            retain_until=_instant(
                retention.get("retainUntil"), "retention.retainUntil"
            ),
            legal_hold_enabled=legal_hold,
        )

    def source_claim(self) -> Any:
        return SimpleNamespace(
            source_snapshot_id=self.source_snapshot_id,
            manifest_checksum=self.manifest_checksum,
            tenant_id=self.tenant_id,
            plant_id=self.plant_id,
            dataset_id=self.dataset_id,
            materialization_id=self.materialization_id,
            source_content_sha256=self.source_content_sha256,
            source_row_count=self.source_row_count,
            source_schema_json=self.source_schema_json,
        )


@dataclass(frozen=True)
class RecoveryPackage:
    manifest: RecoveryManifest
    source: VerifiedSource
    manifest_sha256: str
    manifest_object_key: str
    manifest_version_id: str


class RecoveryPackageLoader:
    def __init__(
        self,
        settings: RehearsalSettings,
        client: Minio | None = None,
    ):
        self._settings = settings
        self._client = client or Minio(
            settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
        )

    def load(self, destination: Path) -> RecoveryPackage:
        manifest_payload = self._download_bytes(
            self._settings.manifest_object_key,
            self._settings.manifest_version_id,
        )
        manifest = RecoveryManifest.from_bytes(
            manifest_payload,
            self._settings.manifest_sha256,
        )
        if manifest.retained_bucket != self._settings.recovery_bucket:
            raise RecoveryRehearsalError(
                "manifest retained bucket does not match the selected recovery bucket"
            )
        self._assert_object(
            manifest,
            self._settings.manifest_object_key,
            self._settings.manifest_version_id,
            self._settings.manifest_sha256,
            len(manifest_payload),
            "RECOVERY_MANIFEST",
        )
        self._assert_object(
            manifest,
            manifest.retained_object_key,
            manifest.retained_object_version_id,
            manifest.source_content_sha256,
            manifest.source_byte_size,
            "SOURCE_PARQUET",
        )
        self._download_file(
            manifest.retained_object_key,
            manifest.retained_object_version_id,
            destination,
            manifest.source_content_sha256,
            manifest.source_byte_size,
        )
        source = validate_local_source(manifest.source_claim(), destination)
        if source.table.num_rows != manifest.catalog_verified_row_count:
            raise RecoveryRehearsalError(
                "recovered row count does not match the original Iceberg verification"
            )
        if source.semantic_checksum != manifest.catalog_semantic_checksum:
            raise RecoveryRehearsalError(
                "recovered semantic checksum does not match the original Iceberg snapshot"
            )
        return RecoveryPackage(
            manifest=manifest,
            source=source,
            manifest_sha256=self._settings.manifest_sha256,
            manifest_object_key=self._settings.manifest_object_key,
            manifest_version_id=self._settings.manifest_version_id,
        )

    def _download_bytes(self, object_key: str, version_id: str) -> bytes:
        response = self._client.get_object(
            self._settings.recovery_bucket,
            object_key,
            version_id=version_id,
        )
        try:
            return response.read()
        finally:
            response.close()
            response.release_conn()

    def _download_file(
        self,
        object_key: str,
        version_id: str,
        destination: Path,
        expected_sha256: str,
        expected_size: int,
    ) -> None:
        response = self._client.get_object(
            self._settings.recovery_bucket,
            object_key,
            version_id=version_id,
        )
        digest = hashlib.sha256()
        byte_size = 0
        try:
            with destination.open("wb") as output:
                for chunk in response.stream(amt=1024 * 1024):
                    output.write(chunk)
                    digest.update(chunk)
                    byte_size += len(chunk)
        finally:
            response.close()
            response.release_conn()
        if byte_size != expected_size or digest.hexdigest() != expected_sha256:
            raise RecoveryRehearsalError(
                "retained source bytes do not match the recovery manifest"
            )

    def _assert_object(
        self,
        manifest: RecoveryManifest,
        object_key: str,
        version_id: str,
        expected_sha256: str,
        expected_size: int,
        object_kind: str,
    ) -> None:
        stat = self._client.stat_object(
            self._settings.recovery_bucket,
            object_key,
            version_id=version_id,
        )
        metadata = {
            str(key).lower(): str(value)
            for key, value in (getattr(stat, "metadata", None) or {}).items()
        }

        def meta(name: str) -> str | None:
            return metadata.get(name) or metadata.get(f"x-amz-meta-{name}")

        if (
            getattr(stat, "version_id", None) != version_id
            or stat.size != expected_size
            or meta("bpi-archive-id") != str(manifest.archive_id)
            or meta("bpi-publication-id") != str(manifest.catalog_publication_id)
            or meta("bpi-content-sha256") != expected_sha256
            or meta("bpi-object-kind") != object_kind
            or meta("bpi-archive-profile") != manifest.archive_profile
        ):
            raise RecoveryRehearsalError(
                f"retained {object_kind.lower()} object metadata does not match"
            )
        retention = self._client.get_object_retention(
            self._settings.recovery_bucket,
            object_key,
            version_id=version_id,
        )
        if retention is None or retention.mode != manifest.retention_mode:
            raise RecoveryRehearsalError("retained object lock mode does not match")
        actual_until = retention.retain_until_date
        if actual_until.tzinfo is None:
            actual_until = actual_until.replace(tzinfo=UTC)
        if actual_until.astimezone(UTC).timestamp() + 1 < manifest.retain_until.timestamp():
            raise RecoveryRehearsalError(
                "retained object expires before the recovery manifest"
            )
        actual_hold = self._client.is_object_legal_hold_enabled(
            self._settings.recovery_bucket,
            object_key,
            version_id=version_id,
        )
        if actual_hold != manifest.legal_hold_enabled:
            raise RecoveryRehearsalError("retained object legal hold does not match")


@dataclass(frozen=True)
class WarehouseObjectVersion:
    object_key: str
    version_id: str
    is_delete_marker: bool


class RecoveryWarehouseCleaner:
    def __init__(self, client: Minio, bucket: str):
        self._client = client
        self._bucket = bucket

    def assert_empty(self, package: RecoveryPackage) -> None:
        if self._inventory(package):
            raise RecoveryRehearsalError(
                "isolated recovery warehouse prefix is not empty"
            )

    def has_objects(self, package: RecoveryPackage) -> bool:
        return bool(self._inventory(package))

    def validate_table(self, table: Any, package: RecoveryPackage) -> None:
        properties = getattr(table, "properties", {})
        if (
            properties.get("bpi.recovery-archive-id")
            != str(package.manifest.archive_id)
            or properties.get("bpi.recovery-manifest-sha256")
            != package.manifest_sha256
        ):
            raise RecoveryRehearsalError(
                "isolated recovery table identity does not match the archive"
            )
        location = str(table.location()).rstrip("/")
        if location != self._expected_location(package):
            raise RecoveryRehearsalError(
                "isolated recovery table location is outside the expected prefix"
            )

    def validated_inventory(
        self,
        package: RecoveryPackage,
        table: Any | None = None,
    ) -> tuple[WarehouseObjectVersion, ...]:
        if table is not None:
            self.validate_table(table, package)
        inventory = self._inventory(package)
        if not inventory:
            raise RecoveryRehearsalError(
                "isolated recovery warehouse prefix contains no object versions"
            )
        metadata_versions = [
            item
            for item in inventory
            if not item.is_delete_marker
            and item.object_key.endswith(".metadata.json")
        ]
        if not metadata_versions:
            raise RecoveryRehearsalError(
                "isolated recovery warehouse prefix has no verifiable metadata"
            )
        for item in metadata_versions:
            document = self._read_json(item)
            properties = _mapping(
                document.get("properties"), "Iceberg metadata properties"
            )
            if (
                properties.get("bpi.recovery-archive-id")
                != str(package.manifest.archive_id)
                or properties.get("bpi.recovery-manifest-sha256")
                != package.manifest_sha256
                or str(document.get("location", "")).rstrip("/")
                != self._expected_location(package)
            ):
                raise RecoveryRehearsalError(
                    "warehouse metadata does not belong to this recovery archive"
                )
        return inventory

    def purge_inventory(
        self,
        package: RecoveryPackage,
        inventory: tuple[WarehouseObjectVersion, ...],
    ) -> int:
        expected_prefix = self._expected_prefix(package)
        if not inventory:
            raise RecoveryRehearsalError("warehouse purge inventory is empty")
        for item in inventory:
            if not item.object_key.startswith(expected_prefix) or not item.version_id:
                raise RecoveryRehearsalError(
                    "warehouse purge inventory escaped the recovery prefix"
                )
            self._client.remove_object(
                self._bucket,
                item.object_key,
                version_id=item.version_id,
            )
        remaining = self._inventory(package)
        if remaining:
            raise RecoveryRehearsalError(
                "physical Iceberg recovery object versions remain after purge"
            )
        return len(inventory)

    def _inventory(
        self, package: RecoveryPackage
    ) -> tuple[WarehouseObjectVersion, ...]:
        expected_prefix = self._expected_prefix(package)
        versions: list[WarehouseObjectVersion] = []
        for item in self._client.list_objects(
            self._bucket,
            prefix=expected_prefix,
            recursive=True,
            include_version=True,
        ):
            object_key = str(getattr(item, "object_name", "") or "")
            version_id = str(getattr(item, "version_id", "") or "")
            if not object_key.startswith(expected_prefix) or not version_id:
                raise RecoveryRehearsalError(
                    "warehouse returned an unsafe or unversioned recovery object"
                )
            versions.append(
                WarehouseObjectVersion(
                    object_key=object_key,
                    version_id=version_id,
                    is_delete_marker=bool(
                        getattr(item, "is_delete_marker", False)
                    ),
                )
            )
        return tuple(
            sorted(
                versions,
                key=lambda item: (
                    item.object_key,
                    item.version_id,
                    item.is_delete_marker,
                ),
            )
        )

    def _read_json(self, item: WarehouseObjectVersion) -> dict[str, Any]:
        response = self._client.get_object(
            self._bucket,
            item.object_key,
            version_id=item.version_id,
        )
        try:
            try:
                value = json.loads(response.read())
            except (UnicodeDecodeError, json.JSONDecodeError) as exception:
                raise RecoveryRehearsalError(
                    "Iceberg recovery metadata is not valid JSON"
                ) from exception
        finally:
            response.close()
            response.release_conn()
        return _mapping(value, "Iceberg recovery metadata")

    def _expected_prefix(self, package: RecoveryPackage) -> str:
        return (
            "warehouse/bpi_recovery/"
            f"archive_{package.manifest.archive_id.hex}/dataset/"
        )

    def _expected_location(self, package: RecoveryPackage) -> str:
        return f"s3://{self._bucket}/{self._expected_prefix(package).rstrip('/')}"


class IcebergRecoveryRehearsal:
    def __init__(
        self,
        catalog: Catalog,
        warehouse: RecoveryWarehouseCleaner,
        reconcile_stale: bool = False,
        catalog_name: str = "ft_mes_bpi",
    ):
        self._catalog = catalog
        self._warehouse = warehouse
        self._reconcile_stale_enabled = reconcile_stale
        self._catalog_name = catalog_name

    def restore(self, package: RecoveryPackage) -> dict[str, Any]:
        manifest = package.manifest
        namespace = ("bpi_recovery", f"archive_{manifest.archive_id.hex}")
        identifier = (*namespace, "dataset")
        reconciled_object_versions = self._reconcile_stale(
            package,
            namespace,
            identifier,
        )
        self._warehouse.assert_empty(package)
        created_namespaces: list[tuple[str, ...]] = []
        created_table = False
        cleanup_inventory: tuple[WarehouseObjectVersion, ...] = ()
        catalog_purged = False
        physical_purge_count = 0
        result: dict[str, Any] | None = None
        primary_error: BaseException | None = None
        try:
            for depth in range(1, len(namespace) + 1):
                candidate = namespace[:depth]
                if not self._catalog.namespace_exists(candidate):
                    self._catalog.create_namespace_if_not_exists(candidate)
                    created_namespaces.append(candidate)
            if self._catalog.table_exists(identifier):
                raise RecoveryRehearsalError(
                    "isolated recovery table already exists; refusing to overwrite"
                )
            table = self._catalog.create_table(
                identifier,
                package.source.table.schema,
                properties={
                    "format-version": "2",
                    "write.parquet.compression-codec": "zstd",
                    "bpi.recovery-archive-id": str(manifest.archive_id),
                    "bpi.recovery-manifest-sha256": package.manifest_sha256,
                    "bpi.original-table-identifier": manifest.original_table_identifier,
                    "bpi.rehearsal-version": RECOVERY_REHEARSAL_VERSION,
                },
            )
            created_table = True
            self._warehouse.validate_table(table, package)
            if table.spec().fields:
                raise RecoveryRehearsalError(
                    "new isolated recovery table unexpectedly has a partition spec"
                )
            (
                table.update_spec()
                .add_identity("plant_id")
                .add_field("prediction_time", "day", "prediction_day")
                .commit()
            )
            table.append(
                package.source.table,
                snapshot_properties={
                    "bpi.recovery-archive-id": str(manifest.archive_id),
                    "bpi.recovery-manifest-sha256": package.manifest_sha256,
                    "bpi.original-catalog-publication-id": str(
                        manifest.catalog_publication_id
                    ),
                    "bpi.original-snapshot-id": str(manifest.original_snapshot_id),
                    "bpi.source-retained-version-id": (
                        manifest.retained_object_version_id
                    ),
                },
            )
            table.refresh()
            current_snapshot = table.current_snapshot()
            if current_snapshot is None:
                raise RecoveryRehearsalError("recovery append produced no snapshot")
            recovery_snapshot_id = current_snapshot.snapshot_id
            first_scan = table.scan(snapshot_id=recovery_snapshot_id).to_arrow()
            reloaded = self._catalog.load_table(identifier)
            self._warehouse.validate_table(reloaded, package)
            time_travel_scan = reloaded.scan(
                snapshot_id=recovery_snapshot_id
            ).to_arrow()
            for label, scanned in (
                ("recovery", first_scan),
                ("time-travel", time_travel_scan),
            ):
                if scanned.num_rows != manifest.catalog_verified_row_count:
                    raise RecoveryRehearsalError(
                        f"{label} row count does not match the archive"
                    )
                if semantic_checksum(scanned) != manifest.catalog_semantic_checksum:
                    raise RecoveryRehearsalError(
                        f"{label} semantic checksum does not match the archive"
                    )
            cleanup_inventory = self._warehouse.validated_inventory(
                package,
                reloaded,
            )
            result = {
                "status": "PASS",
                "rehearsalVersion": RECOVERY_REHEARSAL_VERSION,
                "archiveId": str(manifest.archive_id),
                "catalogPublicationId": str(manifest.catalog_publication_id),
                "manifestObjectKey": package.manifest_object_key,
                "manifestVersionId": package.manifest_version_id,
                "manifestSha256": package.manifest_sha256,
                "sourceObjectKey": manifest.retained_object_key,
                "sourceVersionId": manifest.retained_object_version_id,
                "sourceSha256": manifest.source_content_sha256,
                "retentionMode": manifest.retention_mode,
                "retainUntil": manifest.retain_until.isoformat().replace(
                    "+00:00", "Z"
                ),
                "legalHoldEnabled": manifest.legal_hold_enabled,
                "originalTableIdentifier": manifest.original_table_identifier,
                "originalSnapshotId": str(manifest.original_snapshot_id),
                "recoveryTableIdentifier": ".".join(
                    (self._catalog_name, *identifier)
                ),
                "recoverySnapshotId": str(recovery_snapshot_id),
                "verifiedRowCount": time_travel_scan.num_rows,
                "verifiedSemanticChecksum": semantic_checksum(time_travel_scan),
                "objectLockVerified": True,
                "timeTravelVerified": True,
                "purged": False,
                "physicalPurgeVerified": False,
                "warehouseObjectVersionsPurged": 0,
                "reconciledStaleRecovery": reconciled_object_versions > 0,
                "reconciledStaleObjectVersions": reconciled_object_versions,
                "namespaceCleanupVerified": False,
                "mlflowRegistered": False,
                "modelTrained": False,
            }
        except BaseException as exception:
            primary_error = exception
        cleanup_errors: list[str] = []
        if created_table:
            try:
                if self._catalog.table_exists(identifier):
                    cleanup_table = self._catalog.load_table(identifier)
                    if not cleanup_inventory:
                        cleanup_inventory = self._warehouse.validated_inventory(
                            package,
                            cleanup_table,
                        )
                    else:
                        self._warehouse.validate_table(cleanup_table, package)
                    self._catalog.purge_table(identifier)
                    catalog_purged = True
                elif not cleanup_inventory:
                    cleanup_inventory = self._warehouse.validated_inventory(package)
                    catalog_purged = True
                if self._catalog.table_exists(identifier):
                    raise RecoveryRehearsalError(
                        "recovery table still exists after purge"
                    )
                physical_purge_count = self._warehouse.purge_inventory(
                    package,
                    cleanup_inventory,
                )
                self._warehouse.assert_empty(package)
            except Exception as exception:
                cleanup_errors.append(f"table or physical purge failed: {exception}")
        for candidate in reversed(created_namespaces):
            try:
                if self._catalog.namespace_exists(candidate):
                    self._catalog.drop_namespace(candidate)
                if self._catalog.namespace_exists(candidate):
                    raise RecoveryRehearsalError(
                        f"namespace {'.'.join(candidate)} still exists"
                    )
            except Exception as exception:
                cleanup_errors.append(f"namespace cleanup failed: {exception}")
        if cleanup_errors:
            detail = "; ".join(cleanup_errors)
            if primary_error is not None:
                raise RecoveryRehearsalError(
                    f"{primary_error}; cleanup incomplete: {detail}"
                ) from primary_error
            raise RecoveryRehearsalError(detail)
        if primary_error is not None:
            raise primary_error
        if result is None:
            raise RecoveryRehearsalError("recovery rehearsal produced no result")
        result["purged"] = created_table and catalog_purged and physical_purge_count > 0
        result["physicalPurgeVerified"] = physical_purge_count > 0
        result["warehouseObjectVersionsPurged"] = physical_purge_count
        result["namespaceCleanupVerified"] = all(
            not self._catalog.namespace_exists(candidate)
            for candidate in created_namespaces
        )
        return result

    def _reconcile_stale(
        self,
        package: RecoveryPackage,
        namespace: tuple[str, ...],
        identifier: tuple[str, ...],
    ) -> int:
        table_exists = self._catalog.table_exists(identifier)
        warehouse_objects_exist = self._warehouse.has_objects(package)
        if not table_exists and not warehouse_objects_exist:
            return 0
        if not self._reconcile_stale_enabled:
            if table_exists:
                raise RecoveryRehearsalError(
                    "isolated recovery table already exists; refusing to overwrite"
                )
            raise RecoveryRehearsalError(
                "stale physical recovery objects exist; explicit reconcile is required"
            )
        if table_exists:
            table = self._catalog.load_table(identifier)
            inventory = self._warehouse.validated_inventory(package, table)
            self._catalog.purge_table(identifier)
            if self._catalog.table_exists(identifier):
                raise RecoveryRehearsalError(
                    "stale recovery table still exists after reconciliation purge"
                )
        else:
            inventory = self._warehouse.validated_inventory(package)
        purged = self._warehouse.purge_inventory(package, inventory)
        self._warehouse.assert_empty(package)
        self._drop_empty_recovery_namespaces(namespace)
        return purged

    def _drop_empty_recovery_namespaces(
        self,
        namespace: tuple[str, ...],
    ) -> None:
        parent = namespace[:1]
        if self._catalog.namespace_exists(namespace):
            if self._catalog.list_tables(namespace) or self._catalog.list_namespaces(
                namespace
            ):
                raise RecoveryRehearsalError(
                    "stale recovery namespace is not empty after reconciliation"
                )
            self._catalog.drop_namespace(namespace)
        if self._catalog.namespace_exists(parent):
            if not self._catalog.list_tables(parent) and not self._catalog.list_namespaces(
                parent
            ):
                self._catalog.drop_namespace(parent)


def _load_recovery_catalog(settings: RehearsalSettings) -> Catalog:
    return load_catalog(
        settings.catalog_name,
        type="rest",
        uri=settings.catalog_uri,
        warehouse=settings.catalog_warehouse,
        credential=settings.catalog_credential,
        scope="PRINCIPAL_ROLE:ALL",
        **{
            "oauth2-server-uri": settings.catalog_oauth2_server_uri,
            "header.Polaris-Realm": settings.catalog_realm,
            "py-io-impl": "pyiceberg.io.pyarrow.PyArrowFileIO",
            "s3.endpoint": settings.iceberg_s3_endpoint,
            "s3.region": settings.iceberg_s3_region,
            "s3.resolve-region": "false",
            "s3.force-virtual-addressing": "false",
        },
    )


def run(settings: RehearsalSettings) -> dict[str, Any]:
    settings.work_directory.mkdir(parents=True, exist_ok=True)
    minio_client = Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure,
    )
    with TemporaryDirectory(
        prefix="bpi-recovery-rehearsal-",
        dir=settings.work_directory,
    ) as directory:
        source_path = Path(directory) / "source.parquet"
        package = RecoveryPackageLoader(settings, minio_client).load(source_path)
        result = IcebergRecoveryRehearsal(
            _load_recovery_catalog(settings),
            RecoveryWarehouseCleaner(
                minio_client,
                settings.warehouse_bucket,
            ),
            settings.reconcile_stale,
            settings.catalog_name,
        ).restore(package)
    if settings.report_path is not None:
        settings.report_path.parent.mkdir(parents=True, exist_ok=True)
        settings.report_path.write_text(
            json.dumps(result, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    return result


def _redact(message: str, secrets: list[str]) -> str:
    redacted = message
    for secret in secrets:
        if secret:
            redacted = redacted.replace(secret, "[REDACTED]")
    return redacted[:1000]


def main() -> int:
    settings: RehearsalSettings | None = None
    try:
        settings = RehearsalSettings.from_environment()
        result = run(settings)
        print(json.dumps(result, ensure_ascii=False, sort_keys=True))
        return 0
    except Exception as exception:
        secrets = []
        if settings is not None:
            secrets = [
                settings.minio_access_key,
                settings.minio_secret_key,
                settings.catalog_credential,
            ]
        print(
            json.dumps(
                {
                    "status": "FAIL",
                    "error": _redact(
                        f"{type(exception).__name__}: {exception}", secrets
                    ),
                },
                ensure_ascii=False,
                sort_keys=True,
            ),
            file=sys.stderr,
        )
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
