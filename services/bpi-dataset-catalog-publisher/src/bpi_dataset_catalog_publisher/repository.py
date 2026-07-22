from __future__ import annotations

from typing import Any
from uuid import UUID, uuid4

import psycopg
from psycopg.rows import dict_row
from psycopg.types.json import Jsonb

from .config import Settings
from .models import CatalogCommit, CatalogVerification, PublicationClaim


class LostClaimError(RuntimeError):
    pass


class CatalogPublicationRepository:
    def __init__(self, settings: Settings):
        self._database_url = settings.database_url or ""
        self._catalog_name = settings.catalog_name
        self._publisher_version = settings.publisher_version
        self._claim_timeout_seconds = settings.claim_timeout_seconds
        self._max_attempts = settings.max_attempts

    def ping(self) -> None:
        with self._connect() as connection:
            connection.execute("SELECT 1")

    def recover_and_claim(self) -> PublicationClaim | None:
        claim_token = uuid4()
        with self._connect() as connection:
            exhausted = connection.execute(
                """
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = 'WORKER_CLAIM_EXHAUSTED',
                       failure_detail = 'Catalog publisher claim expired too many times'
                 WHERE state IN ('COMMITTING', 'VERIFYING')
                   AND catalog_name = %s AND publisher_version = %s
                   AND claimed_at < now() - (%s * interval '1 second')
                   AND attempt_count >= %s
                RETURNING id, tenant_id, source_snapshot_id,
                          revision - 1 AS before_revision,
                          revision AS after_revision, attempt_count
                """,
                (
                    self._catalog_name,
                    self._publisher_version,
                    self._claim_timeout_seconds,
                    self._max_attempts,
                ),
            ).fetchall()
            for recovered in exhausted:
                self._audit_recovery(
                    connection,
                    recovered,
                    "DATASET_CATALOG_PUBLICATION_CLAIM_EXHAUSTED",
                )

            requeued = connection.execute(
                """
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'QUEUED', revision = revision + 1,
                       started_at = NULL, completed_at = NULL,
                       claim_token = NULL, claimed_at = NULL,
                       failure_code = NULL, failure_detail = NULL
                 WHERE state IN ('COMMITTING', 'VERIFYING')
                   AND catalog_name = %s AND publisher_version = %s
                   AND claimed_at < now() - (%s * interval '1 second')
                   AND attempt_count < %s
                RETURNING id, tenant_id, source_snapshot_id,
                          revision - 1 AS before_revision,
                          revision AS after_revision, attempt_count
                """,
                (
                    self._catalog_name,
                    self._publisher_version,
                    self._claim_timeout_seconds,
                    self._max_attempts,
                ),
            ).fetchall()
            for recovered in requeued:
                self._audit_recovery(
                    connection,
                    recovered,
                    "DATASET_CATALOG_PUBLICATION_CLAIM_RECOVERED",
                )

            claimed = connection.execute(
                """
                WITH selected AS (
                    SELECT publication.id
                      FROM bpi.bpi_dataset_catalog_publications publication
                     WHERE publication.state = 'QUEUED'
                       AND publication.catalog_name = %s
                       AND publication.publisher_version = %s
                     ORDER BY publication.created_at, publication.id
                     FOR UPDATE OF publication SKIP LOCKED
                     LIMIT 1
                )
                UPDATE bpi.bpi_dataset_catalog_publications publication
                   SET state = 'COMMITTING', revision = revision + 1,
                       started_at = now(), completed_at = NULL,
                       claim_token = %s, claimed_at = now(),
                       attempt_count = attempt_count + 1,
                       failure_code = NULL, failure_detail = NULL
                  FROM selected
                 WHERE publication.id = selected.id
                RETURNING publication.id
                """,
                (self._catalog_name, self._publisher_version, claim_token),
            ).fetchone()
            if claimed is None:
                return None
            claim = self._load_claim(connection, claimed["id"], claim_token)
            self._insert_audit(
                connection,
                claim,
                "DATASET_CATALOG_PUBLICATION_COMMITTING",
                claim.revision - 1,
                claim.revision,
                "Catalog publisher claimed the task",
                {
                    "attemptCount": claim.attempt_count,
                    "tableIdentifier": claim.table_identifier,
                    "sourceObjectVersionId": claim.source_object_version_id,
                },
            )
            return claim

    def mark_verifying(self, claim: PublicationClaim, commit: CatalogCommit) -> int:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'VERIFYING', revision = revision + 1,
                       iceberg_snapshot_id = %s,
                       iceberg_metadata_location = %s,
                       iceberg_schema_id = %s,
                       iceberg_partition_spec_id = %s
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'COMMITTING' AND claim_token = %s
                RETURNING revision
                """,
                (
                    commit.snapshot_id,
                    commit.metadata_location,
                    commit.schema_id,
                    commit.partition_spec_id,
                    claim.id,
                    claim.tenant_id,
                    claim.claim_token,
                ),
            ).fetchone()
            if updated is None:
                raise LostClaimError(
                    "catalog publisher lost the claim before verification"
                )
            revision = updated["revision"]
            self._insert_audit(
                connection,
                claim,
                "DATASET_CATALOG_PUBLICATION_VERIFYING",
                revision - 1,
                revision,
                "Iceberg snapshot committed; exact snapshot verification started",
                {
                    "icebergSnapshotId": commit.snapshot_id,
                    "icebergMetadataLocation": commit.metadata_location,
                    "tableIdentifier": claim.table_identifier,
                },
            )
            return revision

    def complete(
        self,
        claim: PublicationClaim,
        verification: CatalogVerification,
    ) -> int:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'READY', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       verified_row_count = %s, semantic_checksum = %s,
                       catalog_metadata = %s,
                       failure_code = NULL, failure_detail = NULL
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'VERIFYING' AND claim_token = %s
                   AND iceberg_snapshot_id = %s
                RETURNING revision
                """,
                (
                    verification.row_count,
                    verification.semantic_checksum,
                    Jsonb(verification.metadata),
                    claim.id,
                    claim.tenant_id,
                    claim.claim_token,
                    verification.commit.snapshot_id,
                ),
            ).fetchone()
            if updated is None:
                raise LostClaimError(
                    "catalog publisher lost the claim before completion"
                )
            revision = updated["revision"]
            self._insert_audit(
                connection,
                claim,
                "DATASET_CATALOG_PUBLICATION_READY",
                revision - 1,
                revision,
                "Exact Iceberg snapshot rows and semantic checksum verified",
                verification.metadata
                | {
                    "icebergSnapshotId": verification.commit.snapshot_id,
                    "verifiedRowCount": verification.row_count,
                    "semanticChecksum": verification.semantic_checksum,
                },
            )
            return revision

    def fail(self, claim: PublicationClaim, code: str, detail: str) -> bool:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = %s, failure_detail = %s
                 WHERE id = %s AND tenant_id = %s
                   AND state IN ('COMMITTING', 'VERIFYING')
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
                "DATASET_CATALOG_PUBLICATION_FAILED",
                revision - 1,
                revision,
                "Catalog publisher failed the task",
                {
                    "failureCode": code[:128],
                    "attemptCount": claim.attempt_count,
                    "tableIdentifier": claim.table_identifier,
                },
            )
            return True

    def _load_claim(
        self,
        connection: psycopg.Connection,
        publication_id: UUID,
        claim_token: UUID,
    ) -> PublicationClaim:
        row = connection.execute(
            """
            SELECT publication.*,
                   materialization.snapshot_id AS materialization_snapshot_id,
                   materialization.state AS materialization_state,
                   materialization.manifest_checksum AS materialization_manifest_checksum,
                   materialization.content_sha256 AS materialization_content_sha256,
                   materialization.byte_size AS materialization_byte_size,
                   materialization.row_count AS materialization_row_count,
                   materialization.schema_json AS materialization_schema_json,
                   materialization.object_bucket,
                   materialization.object_key,
                   materialization.artifact_metadata ->> 'objectVersionId'
                       AS materialization_object_version_id,
                   snapshot.dataset_id, definition.dataset_code,
                   definition.version AS dataset_version, definition.plant_id
              FROM bpi.bpi_dataset_catalog_publications publication
              JOIN bpi.bpi_dataset_materializations materialization
                ON materialization.tenant_id = publication.tenant_id
               AND materialization.id = publication.materialization_id
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = publication.tenant_id
               AND snapshot.id = publication.source_snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
             WHERE publication.id = %s AND publication.state = 'COMMITTING'
               AND publication.claim_token = %s
            """,
            (publication_id, claim_token),
        ).fetchone()
        if row is None:
            raise LostClaimError("catalog publication claim could not be loaded")
        comparisons = {
            "snapshot": (
                row["source_snapshot_id"],
                row["materialization_snapshot_id"],
            ),
            "manifest checksum": (
                row["manifest_checksum"],
                row["materialization_manifest_checksum"],
            ),
            "content SHA-256": (
                row["source_content_sha256"],
                row["materialization_content_sha256"],
            ),
            "object version": (
                row["source_object_version_id"],
                row["materialization_object_version_id"],
            ),
            "byte size": (
                row["source_byte_size"],
                row["materialization_byte_size"],
            ),
            "row count": (
                row["source_row_count"],
                row["materialization_row_count"],
            ),
            "schema": (
                row["source_schema_json"],
                row["materialization_schema_json"],
            ),
        }
        drift = [name for name, values in comparisons.items() if values[0] != values[1]]
        source_facts_verified = (
            row["catalog_name"] == self._catalog_name
            and row["publisher_version"] == self._publisher_version
            and row["materialization_state"] == "READY"
            and not drift
            and row["source_row_count"] > 0
            and bool(row["object_bucket"])
            and bool(row["object_key"])
        )
        return PublicationClaim(
            id=row["id"],
            tenant_id=row["tenant_id"],
            materialization_id=row["materialization_id"],
            source_snapshot_id=row["source_snapshot_id"],
            dataset_id=row["dataset_id"],
            dataset_code=row["dataset_code"],
            dataset_version=row["dataset_version"],
            plant_id=row["plant_id"],
            catalog_name=row["catalog_name"],
            catalog_namespace=row["catalog_namespace"],
            table_name=row["table_name"],
            table_identifier=row["table_identifier"],
            publisher_version=row["publisher_version"],
            manifest_checksum=row["manifest_checksum"],
            source_content_sha256=row["source_content_sha256"],
            source_object_version_id=row["source_object_version_id"],
            source_byte_size=row["source_byte_size"],
            source_row_count=row["source_row_count"],
            source_schema_json=row["source_schema_json"],
            source_bucket=row["object_bucket"] or "",
            source_object_key=row["object_key"] or "",
            source_facts_verified=source_facts_verified,
            claim_token=row["claim_token"],
            revision=row["revision"],
            attempt_count=row["attempt_count"],
            iceberg_snapshot_id=row["iceberg_snapshot_id"],
            iceberg_metadata_location=row["iceberg_metadata_location"],
            iceberg_schema_id=row["iceberg_schema_id"],
            iceberg_partition_spec_id=row["iceberg_partition_spec_id"],
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
            raise RuntimeError("catalog publication recovery scope is missing")
        connection.execute(
            """
            INSERT INTO bpi.bpi_audit_events
                (id, tenant_id, plant_id, line_id, object_type, object_id, action,
                 actor_id, before_revision, after_revision, reason, trace_id, detail)
            VALUES (%s, %s, %s, NULL, 'DATASET_CATALOG_PUBLICATION', %s, %s,
                    'bpi-dataset-catalog-publisher', %s, %s,
                    'Automated stale catalog publisher claim recovery', %s, %s)
            """,
            (
                uuid4(),
                recovered["tenant_id"],
                scope["plant_id"],
                recovered["id"],
                action,
                recovered["before_revision"],
                recovered["after_revision"],
                f"catalog-publisher-recovery:{recovered['id']}:{recovered['after_revision']}",
                Jsonb({"attemptCount": recovered["attempt_count"]}),
            ),
        )

    def _insert_audit(
        self,
        connection: psycopg.Connection,
        claim: PublicationClaim,
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
            VALUES (%s, %s, %s, NULL, 'DATASET_CATALOG_PUBLICATION', %s, %s,
                    'bpi-dataset-catalog-publisher', %s, %s, %s, %s, %s)
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
                f"catalog-publisher:{claim.claim_token}",
                Jsonb(detail),
            ),
        )

    def _connect(self) -> psycopg.Connection:
        return psycopg.connect(
            self._database_url,
            connect_timeout=10,
            row_factory=dict_row,
        )
