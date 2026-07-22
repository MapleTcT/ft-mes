from __future__ import annotations

from typing import Any
from uuid import UUID, uuid4

import psycopg
from psycopg.rows import dict_row
from psycopg.types.json import Jsonb

from .config import Settings
from .models import ArchiveBundle, ArchiveClaim, ArchiveVerification


class LostClaimError(RuntimeError):
    pass


class RetentionArchiveRepository:
    def __init__(self, settings: Settings):
        self._database_url = settings.database_url or ""
        self._archiver_version = settings.archiver_version
        self._archive_profile = settings.archive_profile
        self._retention_mode = settings.retention_mode
        self._retention_days = settings.retention_days
        self._legal_hold_enabled = settings.legal_hold_enabled
        self._claim_timeout_seconds = settings.claim_timeout_seconds
        self._max_attempts = settings.max_attempts

    def ping(self) -> None:
        with self._connect() as connection:
            connection.execute("SELECT 1")

    def recover_and_claim(self) -> ArchiveClaim | None:
        claim_token = uuid4()
        with self._connect() as connection:
            exhausted = connection.execute(
                """
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = 'WORKER_CLAIM_EXHAUSTED',
                       failure_detail = 'Retention archiver claim expired too many times'
                 WHERE state IN ('ARCHIVING', 'VERIFYING')
                   AND archiver_version = %s AND archive_profile = %s
                   AND claimed_at < now() - (%s * interval '1 second')
                   AND attempt_count >= %s
                RETURNING id, tenant_id, source_snapshot_id,
                          revision - 1 AS before_revision,
                          revision AS after_revision, attempt_count
                """,
                (
                    self._archiver_version,
                    self._archive_profile,
                    self._claim_timeout_seconds,
                    self._max_attempts,
                ),
            ).fetchall()
            for recovered in exhausted:
                self._audit_recovery(
                    connection,
                    recovered,
                    "DATASET_RETENTION_ARCHIVE_CLAIM_EXHAUSTED",
                )

            requeued = connection.execute(
                """
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'QUEUED', revision = revision + 1,
                       started_at = NULL, completed_at = NULL,
                       claim_token = NULL, claimed_at = NULL,
                       failure_code = NULL, failure_detail = NULL
                 WHERE state IN ('ARCHIVING', 'VERIFYING')
                   AND archiver_version = %s AND archive_profile = %s
                   AND claimed_at < now() - (%s * interval '1 second')
                   AND attempt_count < %s
                RETURNING id, tenant_id, source_snapshot_id,
                          revision - 1 AS before_revision,
                          revision AS after_revision, attempt_count
                """,
                (
                    self._archiver_version,
                    self._archive_profile,
                    self._claim_timeout_seconds,
                    self._max_attempts,
                ),
            ).fetchall()
            for recovered in requeued:
                self._audit_recovery(
                    connection,
                    recovered,
                    "DATASET_RETENTION_ARCHIVE_CLAIM_RECOVERED",
                )

            claimed = connection.execute(
                """
                WITH selected AS (
                    SELECT archive.id
                      FROM bpi.bpi_dataset_retention_archives archive
                     WHERE archive.state = 'QUEUED'
                       AND archive.archiver_version = %s
                       AND archive.archive_profile = %s
                     ORDER BY archive.created_at, archive.id
                     FOR UPDATE OF archive SKIP LOCKED
                     LIMIT 1
                )
                UPDATE bpi.bpi_dataset_retention_archives archive
                   SET state = 'ARCHIVING', revision = revision + 1,
                       started_at = now(), completed_at = NULL,
                       claim_token = %s, claimed_at = now(),
                       attempt_count = attempt_count + 1,
                       retention_mode = COALESCE(retention_mode, %s),
                       retain_until = COALESCE(
                           retain_until, now() + (%s * interval '1 day')),
                       legal_hold_enabled = COALESCE(legal_hold_enabled, %s),
                       failure_code = NULL, failure_detail = NULL
                  FROM selected
                 WHERE archive.id = selected.id
                RETURNING archive.id
                """,
                (
                    self._archiver_version,
                    self._archive_profile,
                    claim_token,
                    self._retention_mode,
                    self._retention_days,
                    self._legal_hold_enabled,
                ),
            ).fetchone()
            if claimed is None:
                return None
            claim = self._load_claim(connection, claimed["id"], claim_token)
            self._insert_audit(
                connection,
                claim,
                "DATASET_RETENTION_ARCHIVE_ARCHIVING",
                claim.revision - 1,
                claim.revision,
                "Retention archiver claimed the recovery package task",
                {
                    "attemptCount": claim.attempt_count,
                    "retentionMode": claim.retention_mode,
                    "retainUntil": claim.retain_until.isoformat(),
                    "legalHoldEnabled": claim.legal_hold_enabled,
                },
            )
            return claim

    def mark_verifying(self, claim: ArchiveClaim, bundle: ArchiveBundle) -> int:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'VERIFYING', revision = revision + 1,
                       archive_bucket = %s, archive_prefix = %s,
                       source_archive_object_key = %s,
                       source_archive_version_id = %s,
                       archive_manifest_object_key = %s,
                       archive_manifest_version_id = %s,
                       archive_manifest_sha256 = %s,
                       archive_object_count = %s, archive_total_bytes = %s
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'ARCHIVING' AND claim_token = %s
                RETURNING revision
                """,
                (
                    bundle.bucket,
                    bundle.prefix,
                    bundle.source.object_key,
                    bundle.source.version_id,
                    bundle.manifest.object_key,
                    bundle.manifest.version_id,
                    bundle.manifest.content_sha256,
                    bundle.object_count,
                    bundle.total_bytes,
                    claim.id,
                    claim.tenant_id,
                    claim.claim_token,
                ),
            ).fetchone()
            if updated is None:
                raise LostClaimError(
                    "retention archiver lost the claim before recovery verification"
                )
            revision = updated["revision"]
            self._insert_audit(
                connection,
                claim,
                "DATASET_RETENTION_ARCHIVE_VERIFYING",
                revision - 1,
                revision,
                "Exact retained object versions written; recovery verification started",
                {
                    "archiveBucket": bundle.bucket,
                    "archivePrefix": bundle.prefix,
                    "sourceArchiveVersionId": bundle.source.version_id,
                    "manifestArchiveVersionId": bundle.manifest.version_id,
                    "archiveManifestSha256": bundle.manifest.content_sha256,
                },
            )
            return revision

    def complete(
        self,
        claim: ArchiveClaim,
        verification: ArchiveVerification,
    ) -> int:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'LOCKED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       verified_row_count = %s,
                       verified_semantic_checksum = %s,
                       archive_metadata = %s,
                       failure_code = NULL, failure_detail = NULL
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'VERIFYING' AND claim_token = %s
                   AND archive_manifest_version_id = %s
                RETURNING revision
                """,
                (
                    verification.row_count,
                    verification.semantic_checksum,
                    Jsonb(verification.metadata),
                    claim.id,
                    claim.tenant_id,
                    claim.claim_token,
                    verification.bundle.manifest.version_id,
                ),
            ).fetchone()
            if updated is None:
                raise LostClaimError(
                    "retention archiver lost the claim before completion"
                )
            revision = updated["revision"]
            self._insert_audit(
                connection,
                claim,
                "DATASET_RETENTION_ARCHIVE_LOCKED",
                revision - 1,
                revision,
                "Object Lock and exact-version recovery verification completed",
                verification.metadata
                | {
                    "verifiedRowCount": verification.row_count,
                    "verifiedSemanticChecksum": verification.semantic_checksum,
                },
            )
            return revision

    def fail(self, claim: ArchiveClaim, code: str, detail: str) -> bool:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       verified_row_count = NULL,
                       verified_semantic_checksum = NULL,
                       archive_metadata = NULL,
                       failure_code = %s, failure_detail = %s
                 WHERE id = %s AND tenant_id = %s
                   AND state IN ('ARCHIVING', 'VERIFYING')
                   AND claim_token = %s
                RETURNING revision
                """,
                (
                    code[:128],
                    detail[:1000],
                    claim.id,
                    claim.tenant_id,
                    claim.claim_token,
                ),
            ).fetchone()
            if updated is None:
                return False
            revision = updated["revision"]
            self._insert_audit(
                connection,
                claim,
                "DATASET_RETENTION_ARCHIVE_FAILED",
                revision - 1,
                revision,
                "Retention archiver failed the recovery package task",
                {
                    "failureCode": code[:128],
                    "attemptCount": claim.attempt_count,
                },
            )
            return True

    def _load_claim(
        self,
        connection: psycopg.Connection,
        archive_id: UUID,
        claim_token: UUID,
    ) -> ArchiveClaim:
        row = connection.execute(
            """
            SELECT archive.*,
                   publication.state AS publication_state,
                   publication.materialization_id AS publication_materialization_id,
                   publication.source_snapshot_id AS publication_snapshot_id,
                   publication.manifest_checksum AS publication_manifest_checksum,
                   publication.source_content_sha256 AS publication_content_sha256,
                   publication.source_object_version_id AS publication_object_version_id,
                   publication.source_byte_size AS publication_byte_size,
                   publication.source_row_count AS publication_row_count,
                   publication.source_schema_json AS publication_schema_json,
                   publication.table_identifier AS publication_table_identifier,
                   publication.iceberg_snapshot_id AS publication_iceberg_snapshot_id,
                   publication.iceberg_metadata_location AS publication_metadata_location,
                   publication.iceberg_schema_id AS publication_schema_id,
                   publication.iceberg_partition_spec_id AS publication_partition_spec_id,
                   publication.verified_row_count AS publication_verified_row_count,
                   publication.semantic_checksum AS publication_semantic_checksum,
                   publication.catalog_metadata,
                   materialization.state AS materialization_state,
                   materialization.snapshot_id AS materialization_snapshot_id,
                   materialization.manifest_checksum AS materialization_manifest_checksum,
                   materialization.content_sha256 AS materialization_content_sha256,
                   materialization.byte_size AS materialization_byte_size,
                   materialization.row_count AS materialization_row_count,
                   materialization.schema_json AS materialization_schema_json,
                   materialization.object_bucket,
                   materialization.object_key,
                   materialization.artifact_metadata ->> 'objectVersionId'
                       AS materialization_object_version_id,
                   snapshot.dataset_id, snapshot.line_ids,
                   definition.dataset_code, definition.version AS dataset_version,
                   definition.plant_id
              FROM bpi.bpi_dataset_retention_archives archive
              JOIN bpi.bpi_dataset_catalog_publications publication
                ON publication.tenant_id = archive.tenant_id
               AND publication.id = archive.catalog_publication_id
              JOIN bpi.bpi_dataset_materializations materialization
                ON materialization.tenant_id = archive.tenant_id
               AND materialization.id = archive.source_materialization_id
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = archive.tenant_id
               AND snapshot.id = archive.source_snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
             WHERE archive.id = %s AND archive.state = 'ARCHIVING'
               AND archive.claim_token = %s
            """,
            (archive_id, claim_token),
        ).fetchone()
        if row is None:
            raise LostClaimError("retention archive claim could not be loaded")

        frozen_pairs = (
            (row["source_materialization_id"], row["publication_materialization_id"]),
            (row["source_snapshot_id"], row["publication_snapshot_id"]),
            (row["manifest_checksum"], row["publication_manifest_checksum"]),
            (row["source_content_sha256"], row["publication_content_sha256"]),
            (row["source_object_version_id"], row["publication_object_version_id"]),
            (row["source_byte_size"], row["publication_byte_size"]),
            (row["source_row_count"], row["publication_row_count"]),
            (row["source_schema_json"], row["publication_schema_json"]),
            (row["table_identifier"], row["publication_table_identifier"]),
            (row["iceberg_snapshot_id"], row["publication_iceberg_snapshot_id"]),
            (row["iceberg_metadata_location"], row["publication_metadata_location"]),
            (row["iceberg_schema_id"], row["publication_schema_id"]),
            (row["iceberg_partition_spec_id"], row["publication_partition_spec_id"]),
            (row["catalog_verified_row_count"], row["publication_verified_row_count"]),
            (row["catalog_semantic_checksum"], row["publication_semantic_checksum"]),
            (row["source_snapshot_id"], row["materialization_snapshot_id"]),
            (row["manifest_checksum"], row["materialization_manifest_checksum"]),
            (row["source_content_sha256"], row["materialization_content_sha256"]),
            (row["source_object_version_id"], row["materialization_object_version_id"]),
            (row["source_byte_size"], row["materialization_byte_size"]),
            (row["source_row_count"], row["materialization_row_count"]),
            (row["source_schema_json"], row["materialization_schema_json"]),
        )
        catalog_metadata = row["catalog_metadata"] or {}
        source_facts_verified = (
            row["archiver_version"] == self._archiver_version
            and row["archive_profile"] == self._archive_profile
            and row["publication_state"] == "READY"
            and row["materialization_state"] == "READY"
            and all(left == right for left, right in frozen_pairs)
            and row["catalog_verified_row_count"] == row["source_row_count"]
            and catalog_metadata.get("catalogSnapshotVerified") is True
            and bool(row["object_bucket"])
            and bool(row["object_key"])
        )
        return ArchiveClaim(
            id=row["id"],
            tenant_id=row["tenant_id"],
            catalog_publication_id=row["catalog_publication_id"],
            materialization_id=row["source_materialization_id"],
            source_snapshot_id=row["source_snapshot_id"],
            dataset_id=row["dataset_id"],
            dataset_code=row["dataset_code"],
            dataset_version=row["dataset_version"],
            plant_id=row["plant_id"],
            line_ids=tuple(row["line_ids"]),
            archiver_version=row["archiver_version"],
            archive_profile=row["archive_profile"],
            manifest_checksum=row["manifest_checksum"],
            source_content_sha256=row["source_content_sha256"],
            source_object_version_id=row["source_object_version_id"],
            source_byte_size=row["source_byte_size"],
            source_row_count=row["source_row_count"],
            source_schema_json=row["source_schema_json"],
            table_identifier=row["table_identifier"],
            iceberg_snapshot_id=row["iceberg_snapshot_id"],
            iceberg_metadata_location=row["iceberg_metadata_location"],
            iceberg_schema_id=row["iceberg_schema_id"],
            iceberg_partition_spec_id=row["iceberg_partition_spec_id"],
            catalog_verified_row_count=row["catalog_verified_row_count"],
            catalog_semantic_checksum=row["catalog_semantic_checksum"],
            source_bucket=row["object_bucket"] or "",
            source_object_key=row["object_key"] or "",
            source_facts_verified=source_facts_verified,
            claim_token=row["claim_token"],
            revision=row["revision"],
            attempt_count=row["attempt_count"],
            retention_mode=row["retention_mode"],
            retain_until=row["retain_until"],
            legal_hold_enabled=row["legal_hold_enabled"],
            archive_bucket=row["archive_bucket"],
            archive_prefix=row["archive_prefix"],
            source_archive_object_key=row["source_archive_object_key"],
            source_archive_version_id=row["source_archive_version_id"],
            archive_manifest_object_key=row["archive_manifest_object_key"],
            archive_manifest_version_id=row["archive_manifest_version_id"],
            archive_manifest_sha256=row["archive_manifest_sha256"],
        )

    def _audit_recovery(
        self,
        connection: psycopg.Connection,
        recovered: dict[str, Any],
        action: str,
    ) -> None:
        scope = connection.execute(
            """
            SELECT definition.plant_id
              FROM bpi.bpi_dataset_snapshots snapshot
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
             WHERE snapshot.tenant_id = %s AND snapshot.id = %s
            """,
            (recovered["tenant_id"], recovered["source_snapshot_id"]),
        ).fetchone()
        if scope is None:
            raise RuntimeError("retention archive recovery scope is missing")
        connection.execute(
            """
            INSERT INTO bpi.bpi_audit_events
                (id, tenant_id, plant_id, line_id, object_type, object_id, action,
                 actor_id, before_revision, after_revision, reason, trace_id, detail)
            VALUES (%s, %s, %s, NULL, 'DATASET_RETENTION_ARCHIVE', %s, %s,
                    'bpi-dataset-retention-archiver', %s, %s,
                    'Automated stale retention archiver claim recovery', %s, %s)
            """,
            (
                uuid4(),
                recovered["tenant_id"],
                scope["plant_id"],
                recovered["id"],
                action,
                recovered["before_revision"],
                recovered["after_revision"],
                f"retention-archiver-recovery:{recovered['id']}:{recovered['after_revision']}",
                Jsonb({"attemptCount": recovered["attempt_count"]}),
            ),
        )

    def _insert_audit(
        self,
        connection: psycopg.Connection,
        claim: ArchiveClaim,
        action: str,
        before_revision: int,
        after_revision: int,
        reason: str,
        detail: dict[str, Any],
    ) -> None:
        connection.execute(
            """
            INSERT INTO bpi.bpi_audit_events
                (id, tenant_id, plant_id, line_id, object_type, object_id, action,
                 actor_id, before_revision, after_revision, reason, trace_id, detail)
            VALUES (%s, %s, %s, NULL, 'DATASET_RETENTION_ARCHIVE', %s, %s,
                    'bpi-dataset-retention-archiver', %s, %s, %s, %s, %s)
            """,
            (
                uuid4(),
                claim.tenant_id,
                claim.plant_id,
                claim.id,
                action,
                before_revision,
                after_revision,
                reason,
                f"retention-archiver:{claim.claim_token}",
                Jsonb(detail),
            ),
        )

    def _connect(self) -> psycopg.Connection:
        return psycopg.connect(
            self._database_url,
            connect_timeout=10,
            row_factory=dict_row,
        )
