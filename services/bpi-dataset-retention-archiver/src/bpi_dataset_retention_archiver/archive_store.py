from __future__ import annotations

import hashlib
import json
from datetime import UTC, datetime
from io import BytesIO
from pathlib import Path
from types import SimpleNamespace
from typing import Any, BinaryIO

from minio import Minio
from minio.error import S3Error
from minio.retention import Retention

from bpi_dataset_catalog_publisher.source_object import (
    SourceObjectStore,
    validate_local_source,
)

from .config import Settings
from .models import (
    ArchiveBundle,
    ArchiveClaim,
    ArchiveVerification,
    RetainedObjectVersion,
)


class RetentionArchiveContractError(RuntimeError):
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


def _utc_iso(value: datetime) -> str:
    if value.tzinfo is None:
        raise RetentionArchiveContractError("retain-until timestamp must be timezone-aware")
    return value.astimezone(UTC).isoformat(timespec="microseconds").replace(
        "+00:00", "Z"
    )


class RetentionArchiveStore:
    def __init__(self, settings: Settings, client: Minio | None = None):
        if not settings.minio_endpoint:
            raise ValueError("MinIO endpoint is required")
        if not settings.minio_access_key or not settings.minio_secret_key:
            raise ValueError("MinIO credentials are required")
        self._settings = settings
        self._client = client or Minio(
            settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
        )
        source_settings = SimpleNamespace(
            minio_endpoint=settings.minio_endpoint,
            minio_access_key=settings.minio_access_key,
            minio_secret_key=settings.minio_secret_key,
            minio_secure=settings.minio_secure,
        )
        self._source_store = SourceObjectStore(source_settings, self._client)

    def ping(self) -> None:
        self._verify_bucket_lock()

    def download_source(self, claim: ArchiveClaim, destination: Path):
        return self._source_store.download_verified(claim, destination)

    def ensure_bundle(
        self,
        claim: ArchiveClaim,
        source_path: Path,
    ) -> ArchiveBundle:
        self._verify_bucket_lock()
        prefix = self._prefix(claim)
        source_key = f"{prefix}/source.parquet"
        source = self._ensure_object(
            claim,
            object_key=source_key,
            payload_path=source_path,
            payload=None,
            content_sha256=claim.source_content_sha256,
            content_type="application/vnd.apache.parquet",
            object_kind="SOURCE_PARQUET",
            known_version=(
                claim.source_archive_version_id
                if claim.source_archive_object_key == source_key
                else None
            ),
        )
        manifest_payload = self._manifest_payload(claim, source)
        manifest_bytes = _canonical_json(manifest_payload)
        manifest_sha256 = _sha256(manifest_bytes)
        manifest_key = f"{prefix}/recovery-manifest.json"
        manifest = self._ensure_object(
            claim,
            object_key=manifest_key,
            payload_path=None,
            payload=manifest_bytes,
            content_sha256=manifest_sha256,
            content_type="application/json",
            object_kind="RECOVERY_MANIFEST",
            known_version=(
                claim.archive_manifest_version_id
                if claim.archive_manifest_object_key == manifest_key
                and claim.archive_manifest_sha256 == manifest_sha256
                else None
            ),
        )
        return ArchiveBundle(
            bucket=self._settings.recovery_bucket,
            prefix=prefix,
            source=source,
            manifest=manifest,
            object_count=2,
            total_bytes=source.byte_size + manifest.byte_size,
        )

    def verify(
        self,
        claim: ArchiveClaim,
        bundle: ArchiveBundle,
        recovered_source_path: Path,
    ) -> ArchiveVerification:
        if bundle.bucket != self._settings.recovery_bucket:
            raise RetentionArchiveContractError("recovery bucket does not match settings")
        self._download_exact(
            claim,
            bundle.source,
            recovered_source_path,
        )
        verified_source = validate_local_source(claim, recovered_source_path)
        if verified_source.table.num_rows != claim.catalog_verified_row_count:
            raise RetentionArchiveContractError(
                "recovered source row count does not match Iceberg verification"
            )
        if verified_source.semantic_checksum != claim.catalog_semantic_checksum:
            raise RetentionArchiveContractError(
                "recovered source semantic checksum does not match Iceberg verification"
            )

        expected_manifest = _canonical_json(
            self._manifest_payload(claim, bundle.source)
        )
        manifest_bytes = self._download_bytes(claim, bundle.manifest)
        if manifest_bytes != expected_manifest:
            raise RetentionArchiveContractError(
                "recovery manifest does not match the canonical archive contract"
            )
        self._assert_lock(claim, bundle.source.object_key, bundle.source.version_id)
        self._assert_lock(claim, bundle.manifest.object_key, bundle.manifest.version_id)
        return ArchiveVerification(
            bundle=bundle,
            recovered_source_path=recovered_source_path,
            row_count=verified_source.table.num_rows,
            semantic_checksum=verified_source.semantic_checksum,
            metadata={
                "objectLockVerified": True,
                "recoveryVerified": True,
                "sourceVersionVerified": True,
                "manifestVersionVerified": True,
                "archiveProfile": claim.archive_profile,
                "archiveBucket": bundle.bucket,
                "archivePrefix": bundle.prefix,
                "sourceArchiveVersionId": bundle.source.version_id,
                "archiveManifestVersionId": bundle.manifest.version_id,
                "archiveManifestSha256": bundle.manifest.content_sha256,
                "retentionMode": claim.retention_mode,
                "retainUntil": _utc_iso(claim.retain_until),
                "legalHoldEnabled": claim.legal_hold_enabled,
                "mlflowRegistered": False,
                "modelTrained": False,
            },
        )

    def _prefix(self, claim: ArchiveClaim) -> str:
        tenant_hash = hashlib.sha256(claim.tenant_id.encode()).hexdigest()[:16]
        return (
            f"archives/tenant_{tenant_hash}/"
            f"{claim.catalog_publication_id}/{claim.id}"
        )

    def _manifest_payload(
        self,
        claim: ArchiveClaim,
        source: RetainedObjectVersion,
    ) -> dict[str, Any]:
        return {
            "schemaVersion": "bpi.dataset-recovery.v1",
            "archive": {
                "id": str(claim.id),
                "profile": claim.archive_profile,
                "archiverVersion": claim.archiver_version,
                "catalogPublicationId": str(claim.catalog_publication_id),
                "materializationId": str(claim.materialization_id),
                "snapshotId": str(claim.source_snapshot_id),
                "datasetId": str(claim.dataset_id),
                "datasetCode": claim.dataset_code,
                "datasetVersion": claim.dataset_version,
                "tenantId": claim.tenant_id,
                "plantId": claim.plant_id,
                "lineIds": sorted(claim.line_ids),
            },
            "source": {
                "manifestChecksum": claim.manifest_checksum,
                "contentSha256": claim.source_content_sha256,
                "originalObjectVersionId": claim.source_object_version_id,
                "byteSize": claim.source_byte_size,
                "rowCount": claim.source_row_count,
                "schema": claim.source_schema_json,
                "retainedBucket": self._settings.recovery_bucket,
                "retainedObjectKey": source.object_key,
                "retainedObjectVersionId": source.version_id,
            },
            "iceberg": {
                "tableIdentifier": claim.table_identifier,
                "snapshotId": str(claim.iceberg_snapshot_id),
                "metadataLocation": claim.iceberg_metadata_location,
                "schemaId": claim.iceberg_schema_id,
                "partitionSpecId": claim.iceberg_partition_spec_id,
                "verifiedRowCount": claim.catalog_verified_row_count,
                "semanticChecksum": claim.catalog_semantic_checksum,
            },
            "retention": {
                "mode": claim.retention_mode,
                "retainUntil": _utc_iso(claim.retain_until),
                "legalHoldEnabled": claim.legal_hold_enabled,
            },
        }

    def _ensure_object(
        self,
        claim: ArchiveClaim,
        *,
        object_key: str,
        payload_path: Path | None,
        payload: bytes | None,
        content_sha256: str,
        content_type: str,
        object_kind: str,
        known_version: str | None,
    ) -> RetainedObjectVersion:
        if (payload_path is None) == (payload is None):
            raise ValueError("exactly one payload source is required")
        byte_size = payload_path.stat().st_size if payload_path else len(payload or b"")
        version = known_version
        if version is None:
            current = self._current_matching_version(
                claim, object_key, content_sha256, byte_size, object_kind
            )
            if current is not None:
                return current
        else:
            retained = self._matching_version(
                claim,
                object_key,
                version,
                content_sha256,
                byte_size,
                object_kind,
            )
            if retained is None:
                raise RetentionArchiveContractError(
                    "database references a retained object version that no longer matches"
                )
            return retained

        stream: BinaryIO
        if payload_path is not None:
            stream = payload_path.open("rb")
        else:
            stream = BytesIO(payload or b"")
        try:
            result = self._client.put_object(
                self._settings.recovery_bucket,
                object_key,
                stream,
                byte_size,
                content_type=content_type,
                metadata={
                    "bpi-archive-id": str(claim.id),
                    "bpi-publication-id": str(claim.catalog_publication_id),
                    "bpi-content-sha256": content_sha256,
                    "bpi-object-kind": object_kind,
                    "bpi-archive-profile": claim.archive_profile,
                },
                retention=Retention(claim.retention_mode, claim.retain_until),
                legal_hold=claim.legal_hold_enabled,
            )
        finally:
            stream.close()
        if not result.version_id:
            raise RetentionArchiveContractError(
                "Object Lock write did not return an object version id"
            )
        retained = self._matching_version(
            claim,
            object_key,
            result.version_id,
            content_sha256,
            byte_size,
            object_kind,
        )
        if retained is None:
            raise RetentionArchiveContractError(
                "new retained object version failed immediate verification"
            )
        return retained

    def _current_matching_version(
        self,
        claim: ArchiveClaim,
        object_key: str,
        content_sha256: str,
        byte_size: int,
        object_kind: str,
    ) -> RetainedObjectVersion | None:
        try:
            stat = self._client.stat_object(
                self._settings.recovery_bucket, object_key
            )
        except S3Error as exception:
            if exception.code in {"NoSuchKey", "NoSuchObject", "NoSuchBucket"}:
                return None
            raise
        if not stat.version_id:
            return None
        return self._stat_matches(
            claim,
            stat,
            object_key,
            stat.version_id,
            content_sha256,
            byte_size,
            object_kind,
        )

    def _matching_version(
        self,
        claim: ArchiveClaim,
        object_key: str,
        version_id: str,
        content_sha256: str,
        byte_size: int,
        object_kind: str,
    ) -> RetainedObjectVersion | None:
        try:
            stat = self._client.stat_object(
                self._settings.recovery_bucket,
                object_key,
                version_id=version_id,
            )
        except S3Error as exception:
            if exception.code in {"NoSuchKey", "NoSuchObject", "NoSuchVersion"}:
                return None
            raise
        return self._stat_matches(
            claim,
            stat,
            object_key,
            version_id,
            content_sha256,
            byte_size,
            object_kind,
        )

    def _stat_matches(
        self,
        claim: ArchiveClaim,
        stat: Any,
        object_key: str,
        version_id: str,
        content_sha256: str,
        byte_size: int,
        object_kind: str,
    ) -> RetainedObjectVersion | None:
        metadata = {
            str(key).lower(): str(value)
            for key, value in (getattr(stat, "metadata", None) or {}).items()
        }

        def meta(name: str) -> str | None:
            return metadata.get(name) or metadata.get(f"x-amz-meta-{name}")

        if (
            getattr(stat, "version_id", None) != version_id
            or stat.size != byte_size
            or meta("bpi-archive-id") != str(claim.id)
            or meta("bpi-publication-id") != str(claim.catalog_publication_id)
            or meta("bpi-content-sha256") != content_sha256
            or meta("bpi-object-kind") != object_kind
            or meta("bpi-archive-profile") != claim.archive_profile
        ):
            return None
        self._assert_lock(claim, object_key, version_id)
        return RetainedObjectVersion(
            object_key=object_key,
            version_id=version_id,
            content_sha256=content_sha256,
            byte_size=byte_size,
        )

    def _assert_lock(
        self,
        claim: ArchiveClaim,
        object_key: str,
        version_id: str,
    ) -> None:
        retention = self._client.get_object_retention(
            self._settings.recovery_bucket,
            object_key,
            version_id=version_id,
        )
        if retention is None or retention.mode != claim.retention_mode:
            raise RetentionArchiveContractError(
                "retained object mode does not match the archive contract"
            )
        actual_until = retention.retain_until_date
        if actual_until.tzinfo is None:
            actual_until = actual_until.replace(tzinfo=UTC)
        expected_until = claim.retain_until.astimezone(UTC)
        if actual_until.astimezone(UTC).timestamp() + 1 < expected_until.timestamp():
            raise RetentionArchiveContractError(
                "retained object expires before the archive contract"
            )
        actual_hold = self._client.is_object_legal_hold_enabled(
            self._settings.recovery_bucket,
            object_key,
            version_id=version_id,
        )
        if actual_hold != claim.legal_hold_enabled:
            raise RetentionArchiveContractError(
                "retained object legal-hold state does not match"
            )

    def _download_exact(
        self,
        claim: ArchiveClaim,
        retained: RetainedObjectVersion,
        destination: Path,
    ) -> None:
        destination.parent.mkdir(parents=True, exist_ok=True)
        response = self._client.get_object(
            self._settings.recovery_bucket,
            retained.object_key,
            version_id=retained.version_id,
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
        if byte_size != retained.byte_size or digest.hexdigest() != retained.content_sha256:
            raise RetentionArchiveContractError(
                "retained object bytes do not match the frozen recovery contract"
            )
        self._assert_lock(claim, retained.object_key, retained.version_id)

    def _download_bytes(
        self,
        claim: ArchiveClaim,
        retained: RetainedObjectVersion,
    ) -> bytes:
        response = self._client.get_object(
            self._settings.recovery_bucket,
            retained.object_key,
            version_id=retained.version_id,
        )
        try:
            payload = response.read()
        finally:
            response.close()
            response.release_conn()
        if len(payload) != retained.byte_size or _sha256(payload) != retained.content_sha256:
            raise RetentionArchiveContractError(
                "retained manifest bytes do not match the frozen recovery contract"
            )
        self._assert_lock(claim, retained.object_key, retained.version_id)
        return payload

    def _verify_bucket_lock(self) -> None:
        config = self._client.get_object_lock_config(self._settings.recovery_bucket)
        if config.mode != self._settings.retention_mode:
            raise RetentionArchiveContractError(
                "recovery bucket Object Lock mode does not match settings"
            )
        if not config.duration or config.duration < 1:
            raise RetentionArchiveContractError(
                "recovery bucket has no positive default retention"
            )
