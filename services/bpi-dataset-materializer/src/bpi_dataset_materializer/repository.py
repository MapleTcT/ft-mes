from __future__ import annotations

from typing import Any
from uuid import UUID, uuid4

import psycopg
from psycopg.rows import dict_row
from psycopg.types.json import Jsonb

from .config import Settings
from .models import DatasetSample, MaterializationClaim, ParquetArtifact, StoredObject


class LostClaimError(RuntimeError):
    pass


class MaterializationRepository:
    def __init__(self, settings: Settings):
        self._database_url = settings.database_url or ""
        self._schema_version = settings.artifact_schema_version
        self._materializer_version = settings.materializer_version
        self._claim_timeout_seconds = settings.claim_timeout_seconds
        self._max_attempts = settings.max_attempts

    def ping(self) -> None:
        with self._connect() as connection:
            connection.execute("SELECT 1")

    def recover_and_claim(self) -> MaterializationClaim | None:
        claim_token = uuid4()
        with self._connect() as connection:
            exhausted = connection.execute(
                """
                UPDATE bpi.bpi_dataset_materializations
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = 'WORKER_CLAIM_EXHAUSTED',
                       failure_detail = 'Materializer claim expired too many times'
                 WHERE state = 'WRITING'
                   AND artifact_schema_version = %s
                   AND materializer_version = %s
                   AND claimed_at < now() - (%s * interval '1 second')
                   AND attempt_count >= %s
                RETURNING id, tenant_id, snapshot_id, revision - 1 AS before_revision,
                          revision AS after_revision, attempt_count
                """,
                (
                    self._schema_version,
                    self._materializer_version,
                    self._claim_timeout_seconds,
                    self._max_attempts,
                ),
            ).fetchall()
            for recovered in exhausted:
                self._audit_recovery(
                    connection, recovered, "DATASET_MATERIALIZATION_CLAIM_EXHAUSTED")

            requeued = connection.execute(
                """
                UPDATE bpi.bpi_dataset_materializations
                   SET state = 'QUEUED', revision = revision + 1,
                       started_at = NULL, claim_token = NULL, claimed_at = NULL
                 WHERE state = 'WRITING'
                   AND artifact_schema_version = %s
                   AND materializer_version = %s
                   AND claimed_at < now() - (%s * interval '1 second')
                   AND attempt_count < %s
                RETURNING id, tenant_id, snapshot_id, revision - 1 AS before_revision,
                          revision AS after_revision, attempt_count
                """,
                (
                    self._schema_version,
                    self._materializer_version,
                    self._claim_timeout_seconds,
                    self._max_attempts,
                ),
            ).fetchall()
            for recovered in requeued:
                self._audit_recovery(
                    connection, recovered, "DATASET_MATERIALIZATION_CLAIM_RECOVERED")

            claimed = connection.execute(
                """
                WITH selected AS (
                    SELECT materialization.id
                      FROM bpi.bpi_dataset_materializations materialization
                      JOIN bpi.bpi_dataset_snapshots snapshot
                        ON snapshot.tenant_id = materialization.tenant_id
                       AND snapshot.id = materialization.snapshot_id
                     WHERE materialization.state = 'QUEUED'
                       AND materialization.artifact_format = 'PARQUET'
                       AND materialization.artifact_schema_version = %s
                       AND materialization.materializer_version = %s
                       AND snapshot.state = 'MANIFEST_READY'
                       AND snapshot.manifest_checksum = materialization.manifest_checksum
                     ORDER BY materialization.created_at, materialization.id
                     FOR UPDATE OF materialization SKIP LOCKED
                     LIMIT 1
                )
                UPDATE bpi.bpi_dataset_materializations materialization
                   SET state = 'WRITING', revision = revision + 1,
                       started_at = now(), completed_at = NULL,
                       claim_token = %s, claimed_at = now(),
                       attempt_count = attempt_count + 1,
                       failure_code = NULL, failure_detail = NULL
                  FROM selected
                 WHERE materialization.id = selected.id
                RETURNING materialization.id
                """,
                (self._schema_version, self._materializer_version, claim_token),
            ).fetchone()
            if claimed is None:
                return None
            claim = self._load_claim(connection, claimed["id"], claim_token)
            self._insert_audit(
                connection,
                claim,
                "DATASET_MATERIALIZATION_WRITING",
                claim.revision - 1,
                claim.revision,
                "Materialization worker claimed the task",
                {
                    "attemptCount": claim.attempt_count,
                    "artifactSchemaVersion": claim.artifact_schema_version,
                    "materializerVersion": claim.materializer_version,
                },
            )
            return claim

    def load_samples(self, claim: MaterializationClaim) -> list[DatasetSample]:
        with self._connect() as connection:
            active = connection.execute(
                """
                SELECT 1
                  FROM bpi.bpi_dataset_materializations
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'WRITING' AND claim_token = %s
                """,
                (claim.id, claim.tenant_id, claim.claim_token),
            ).fetchone()
            if active is None:
                raise LostClaimError("materialization claim is no longer active")
            rows = connection.execute(
                """
                SELECT snapshot_id, review_id, shadow_run_id, batch_id, batch_no, line_id,
                       prediction_time, feature_cutoff, label_available_at, confidence,
                       split_key, feature_payload, label_payload
                  FROM bpi.bpi_dataset_snapshot_samples
                 WHERE tenant_id = %s AND snapshot_id = %s AND included = true
                 ORDER BY line_id, prediction_time, batch_id, review_id
                """,
                (claim.tenant_id, claim.snapshot_id),
            ).fetchall()
        return [DatasetSample(**row) for row in rows]

    def complete(
        self,
        claim: MaterializationClaim,
        artifact: ParquetArtifact,
        stored: StoredObject,
    ) -> int:
        with self._connect() as connection:
            artifact_metadata = dict(artifact.metadata)
            artifact_metadata.update({
                "objectVersionId": stored.version_id,
                "objectContentVerified": True,
            })
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_materializations
                   SET state = 'READY', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       artifact_uri = %s, object_bucket = %s, object_key = %s,
                       content_sha256 = %s, byte_size = %s, row_count = %s,
                       schema_json = %s, artifact_metadata = %s,
                       failure_code = NULL, failure_detail = NULL
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'WRITING' AND claim_token = %s
                RETURNING revision
                """,
                (
                    stored.uri,
                    stored.bucket,
                    stored.key,
                    artifact.content_sha256,
                    stored.byte_size,
                    artifact.row_count,
                    Jsonb(artifact.schema_json),
                    Jsonb(artifact_metadata),
                    claim.id,
                    claim.tenant_id,
                    claim.claim_token,
                ),
            ).fetchone()
            if updated is None:
                raise LostClaimError("materialization worker lost the claim before completion")
            revision = updated["revision"]
            self._insert_audit(
                connection,
                claim,
                "DATASET_MATERIALIZATION_READY",
                claim.revision,
                revision,
                "Parquet object uploaded and verified",
                {
                    "artifactUri": stored.uri,
                    "contentSha256": artifact.content_sha256,
                    "objectVersionId": stored.version_id,
                    "objectContentVerified": True,
                    "byteSize": stored.byte_size,
                    "rowCount": artifact.row_count,
                    "sourcePayloadIncluded": False,
                    "icebergReady": False,
                    "mlflowRegistered": False,
                    "modelTrained": False,
                },
            )
            return revision

    def fail(self, claim: MaterializationClaim, code: str, detail: str) -> bool:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_materializations
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = %s, failure_detail = %s
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'WRITING' AND claim_token = %s
                RETURNING revision
                """,
                (code[:128], detail[:1000], claim.id, claim.tenant_id, claim.claim_token),
            ).fetchone()
            if updated is None:
                return False
            self._insert_audit(
                connection,
                claim,
                "DATASET_MATERIALIZATION_FAILED",
                claim.revision,
                updated["revision"],
                "Materialization worker failed the task",
                {"failureCode": code[:128], "attemptCount": claim.attempt_count},
            )
            return True

    def _load_claim(
        self,
        connection: psycopg.Connection,
        materialization_id: UUID,
        claim_token: UUID,
    ) -> MaterializationClaim:
        row = connection.execute(
            """
            SELECT materialization.id, materialization.tenant_id,
                   materialization.snapshot_id, snapshot.dataset_id,
                   definition.dataset_code, definition.version AS dataset_version,
                   definition.plant_id, materialization.artifact_schema_version,
                   materialization.materializer_version,
                   materialization.manifest_checksum,
                   snapshot.manifest_schema_version, snapshot.definition_checksum,
                   snapshot.included_count, definition.feature_refs,
                   definition.label_refs, materialization.claim_token,
                   materialization.revision, materialization.attempt_count,
                   snapshot.state AS snapshot_state,
                   snapshot.manifest_checksum AS snapshot_manifest_checksum,
                   definition.checksum AS current_definition_checksum
              FROM bpi.bpi_dataset_materializations materialization
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = materialization.tenant_id
               AND snapshot.id = materialization.snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
             WHERE materialization.id = %s
               AND materialization.state = 'WRITING'
               AND materialization.claim_token = %s
            """,
            (materialization_id, claim_token),
        ).fetchone()
        if row is None:
            raise LostClaimError("materialization claim could not be loaded")
        if row["snapshot_state"] != "MANIFEST_READY":
            raise RuntimeError("dataset snapshot is not manifest ready")
        if row["manifest_checksum"] != row["snapshot_manifest_checksum"]:
            raise RuntimeError("materialization manifest checksum drift detected")
        if row["definition_checksum"] != row["current_definition_checksum"]:
            raise RuntimeError("dataset definition checksum drift detected")
        if row["included_count"] is None:
            raise RuntimeError("dataset snapshot included count is missing")
        return MaterializationClaim(
            id=row["id"],
            tenant_id=row["tenant_id"],
            snapshot_id=row["snapshot_id"],
            dataset_id=row["dataset_id"],
            dataset_code=row["dataset_code"],
            dataset_version=row["dataset_version"],
            plant_id=row["plant_id"],
            artifact_schema_version=row["artifact_schema_version"],
            materializer_version=row["materializer_version"],
            manifest_checksum=row["manifest_checksum"],
            manifest_schema_version=row["manifest_schema_version"],
            definition_checksum=row["definition_checksum"],
            included_count=row["included_count"],
            feature_refs=tuple(row["feature_refs"]),
            label_refs=tuple(row["label_refs"]),
            claim_token=row["claim_token"],
            revision=row["revision"],
            attempt_count=row["attempt_count"],
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
            (recovered["tenant_id"], recovered["snapshot_id"]),
        ).fetchone()
        if scope is None:
            raise RuntimeError("materialization recovery scope is missing")
        connection.execute(
            """
            INSERT INTO bpi.bpi_audit_events
                (id, tenant_id, plant_id, line_id, object_type, object_id, action,
                 actor_id, before_revision, after_revision, reason, trace_id, detail)
            VALUES (%s, %s, %s, NULL, 'DATASET_MATERIALIZATION', %s, %s,
                    'bpi-dataset-materializer', %s, %s,
                    'Automated stale materializer claim recovery', %s, %s)
            """,
            (
                uuid4(),
                recovered["tenant_id"],
                scope["plant_id"],
                recovered["id"],
                action,
                recovered["before_revision"],
                recovered["after_revision"],
                f"materializer-recovery:{recovered['id']}:{recovered['after_revision']}",
                Jsonb({"attemptCount": recovered["attempt_count"]}),
            ),
        )

    def _insert_audit(
        self,
        connection: psycopg.Connection,
        claim: MaterializationClaim,
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
            VALUES (%s, %s, %s, NULL, 'DATASET_MATERIALIZATION', %s, %s,
                    'bpi-dataset-materializer', %s, %s, %s, %s, %s)
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
                f"materializer:{claim.claim_token}",
                Jsonb(detail),
            ),
        )

    def _connect(self) -> psycopg.Connection:
        return psycopg.connect(
            self._database_url,
            connect_timeout=10,
            row_factory=dict_row,
        )
