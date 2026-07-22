from __future__ import annotations

import hashlib
from urllib.parse import quote

from minio import Minio
from minio.error import S3Error

from .config import Settings
from .models import MaterializationClaim, ParquetArtifact, StoredObject


class ObjectStoreContractError(RuntimeError):
    pass


class DatasetObjectStore:
    def __init__(self, settings: Settings):
        if not settings.minio_endpoint:
            raise ValueError("MinIO endpoint is required")
        if not settings.minio_access_key or not settings.minio_secret_key:
            raise ValueError("MinIO credentials are required")
        self._bucket = settings.minio_bucket
        self._client = Minio(
            settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
        )

    def validate_bucket(self) -> None:
        if not self._client.bucket_exists(self._bucket):
            raise ObjectStoreContractError(
                f"private dataset bucket {self._bucket!r} does not exist")
        versioning = self._client.get_bucket_versioning(self._bucket)
        if versioning.status != "Enabled":
            raise ObjectStoreContractError(
                f"private dataset bucket {self._bucket!r} must have versioning enabled")

    def ensure_uploaded(
        self,
        claim: MaterializationClaim,
        artifact: ParquetArtifact,
    ) -> StoredObject:
        version_segment = claim.materializer_version.replace("/", "-")
        key = (
            f"datasets/{claim.snapshot_id}/{claim.manifest_checksum}/"
            f"{version_segment}/{artifact.content_sha256}.parquet"
        )
        expected_metadata = {
            "content-sha256": artifact.content_sha256,
            "manifest-checksum": claim.manifest_checksum,
            "artifact-schema-version": claim.artifact_schema_version,
            "materializer-version": claim.materializer_version,
        }
        existing = self._stat_or_none(key)
        if existing is not None:
            version_id = self._require_version_id(existing)
            self._verify(
                key, version_id, existing, artifact.byte_size,
                artifact.content_sha256, expected_metadata)
            return StoredObject(
                self._bucket, key, self._uri(key, version_id), existing.size, version_id)

        uploaded = self._client.fput_object(
            self._bucket,
            key,
            str(artifact.path),
            content_type="application/vnd.apache.parquet",
            metadata=expected_metadata,
        )
        version_id = uploaded.version_id
        if not version_id:
            raise ObjectStoreContractError(
                "MinIO upload did not return an immutable object version")
        stored = self._client.stat_object(self._bucket, key, version_id=version_id)
        self._verify(
            key, version_id, stored, artifact.byte_size,
            artifact.content_sha256, expected_metadata)
        return StoredObject(
            self._bucket, key, self._uri(key, version_id), stored.size, version_id)

    def _stat_or_none(self, key: str):
        try:
            return self._client.stat_object(self._bucket, key)
        except S3Error as exception:
            if exception.code in {"NoSuchKey", "NoSuchObject", "NotFound"}:
                return None
            raise

    def _verify(
        self,
        key: str,
        version_id: str,
        stat,
        expected_size: int,
        expected_sha256: str,
        expected_metadata: dict[str, str],
    ) -> None:
        if stat.size != expected_size:
            raise ObjectStoreContractError(
                f"object size mismatch for {stat.object_name}: {stat.size} != {expected_size}")
        actual = {
            key.lower().removeprefix("x-amz-meta-"): value
            for key, value in (stat.metadata or {}).items()
        }
        mismatches = [
            key for key, value in expected_metadata.items() if actual.get(key) != value
        ]
        if mismatches:
            raise ObjectStoreContractError(
                f"object metadata mismatch for {stat.object_name}: {','.join(sorted(mismatches))}")
        actual_sha256 = self._content_sha256(key, version_id)
        if actual_sha256 != expected_sha256:
            raise ObjectStoreContractError(
                f"object content checksum mismatch for {stat.object_name}")

    def _content_sha256(self, key: str, version_id: str) -> str:
        response = self._client.get_object(
            self._bucket, key, version_id=version_id)
        digest = hashlib.sha256()
        try:
            for chunk in response.stream(amt=1024 * 1024):
                digest.update(chunk)
        finally:
            response.close()
            response.release_conn()
        return digest.hexdigest()

    def _require_version_id(self, stat) -> str:
        version_id = getattr(stat, "version_id", None)
        if not version_id:
            raise ObjectStoreContractError(
                f"object {stat.object_name} has no immutable version id")
        return version_id

    def _uri(self, key: str, version_id: str) -> str:
        return (
            f"s3://{self._bucket}/{key}?versionId="
            f"{quote(version_id, safe='')}"
        )
