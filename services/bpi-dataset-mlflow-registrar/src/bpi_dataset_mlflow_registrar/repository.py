from __future__ import annotations

from typing import Any
from uuid import UUID, uuid4

import psycopg
from psycopg.rows import dict_row
from psycopg.types.json import Jsonb

from .config import Settings
from .errors import LostClaimError
from .models import RegistrationClaim, RegistrationResult


class MlflowRegistrationRepository:
    def __init__(self, settings: Settings):
        self._database_url = settings.database_url or ""
        self._registrar_version = settings.registrar_version
        self._tracking_profile = settings.tracking_profile
        self._claim_timeout_seconds = settings.claim_timeout_seconds
        self._max_attempts = settings.max_attempts

    def ping(self) -> None:
        with self._connect() as connection:
            connection.execute("SELECT 1")

    def recover_and_claim(self) -> RegistrationClaim | None:
        claim_token = uuid4()
        with self._connect() as connection:
            exhausted = connection.execute(
                """
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = 'WORKER_CLAIM_EXHAUSTED',
                       failure_detail = 'MLflow registrar claim expired too many times'
                 WHERE state = 'REGISTERING'
                   AND registrar_version = %s AND tracking_profile = %s
                   AND claimed_at < now() - (%s * interval '1 second')
                   AND attempt_count >= %s
                RETURNING id, tenant_id, source_snapshot_id,
                          revision - 1 AS before_revision,
                          revision AS after_revision, attempt_count
                """,
                (
                    self._registrar_version,
                    self._tracking_profile,
                    self._claim_timeout_seconds,
                    self._max_attempts,
                ),
            ).fetchall()
            for recovered in exhausted:
                self._audit_recovery(
                    connection,
                    recovered,
                    "DATASET_MLFLOW_REGISTRATION_CLAIM_EXHAUSTED",
                )

            requeued = connection.execute(
                """
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'QUEUED', revision = revision + 1,
                       started_at = NULL, completed_at = NULL,
                       claim_token = NULL, claimed_at = NULL,
                       failure_code = NULL, failure_detail = NULL
                 WHERE state = 'REGISTERING'
                   AND registrar_version = %s AND tracking_profile = %s
                   AND claimed_at < now() - (%s * interval '1 second')
                   AND attempt_count < %s
                RETURNING id, tenant_id, source_snapshot_id,
                          revision - 1 AS before_revision,
                          revision AS after_revision, attempt_count
                """,
                (
                    self._registrar_version,
                    self._tracking_profile,
                    self._claim_timeout_seconds,
                    self._max_attempts,
                ),
            ).fetchall()
            for recovered in requeued:
                self._audit_recovery(
                    connection,
                    recovered,
                    "DATASET_MLFLOW_REGISTRATION_CLAIM_RECOVERED",
                )

            claimed = connection.execute(
                """
                WITH selected AS (
                    SELECT registration.id
                      FROM bpi.bpi_dataset_mlflow_registrations registration
                     WHERE registration.state = 'QUEUED'
                       AND registration.registrar_version = %s
                       AND registration.tracking_profile = %s
                     ORDER BY registration.created_at, registration.id
                     FOR UPDATE OF registration SKIP LOCKED
                     LIMIT 1
                )
                UPDATE bpi.bpi_dataset_mlflow_registrations registration
                   SET state = 'REGISTERING', revision = revision + 1,
                       started_at = now(), completed_at = NULL,
                       claim_token = %s, claimed_at = now(),
                       attempt_count = attempt_count + 1,
                       failure_code = NULL, failure_detail = NULL
                  FROM selected
                 WHERE registration.id = selected.id
                RETURNING registration.id
                """,
                (self._registrar_version, self._tracking_profile, claim_token),
            ).fetchone()
            if claimed is None:
                return None
            claim = self._load_claim(connection, claimed["id"], claim_token)
            self._insert_audit(
                connection,
                claim,
                "DATASET_MLFLOW_REGISTRATION_REGISTERING",
                claim.revision - 1,
                claim.revision,
                "MLflow registrar claimed the immutable dataset registration task",
                {
                    "attemptCount": claim.attempt_count,
                    "experimentName": claim.experiment_name,
                    "datasetName": claim.dataset_name,
                    "datasetDigest": claim.dataset_digest,
                    "modelTrained": False,
                    "productionActivationAllowed": False,
                },
            )
            return claim

    def complete(self, claim: RegistrationClaim, result: RegistrationResult) -> int:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'REGISTERED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       mlflow_experiment_id = %s, mlflow_run_id = %s,
                       mlflow_artifact_uri = %s, mlflow_dataset_source = %s,
                       registration_metadata = %s,
                       failure_code = NULL, failure_detail = NULL
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'REGISTERING' AND claim_token = %s
                RETURNING revision
                """,
                (
                    result.experiment_id,
                    result.run_id,
                    result.artifact_uri,
                    result.dataset_source,
                    Jsonb(result.metadata),
                    claim.id,
                    claim.tenant_id,
                    claim.claim_token,
                ),
            ).fetchone()
            if updated is None:
                raise LostClaimError(
                    "MLflow registrar lost the claim before registration completion"
                )
            revision = updated["revision"]
            self._insert_audit(
                connection,
                claim,
                "DATASET_MLFLOW_REGISTRATION_REGISTERED",
                revision - 1,
                revision,
                "MLflow dataset input and immutable lineage verification completed",
                result.metadata
                | {
                    "mlflowExperimentId": result.experiment_id,
                    "mlflowRunId": result.run_id,
                    "mlflowDatasetSource": result.dataset_source,
                },
            )
            return revision

    def fail(self, claim: RegistrationClaim, code: str, detail: str) -> bool:
        with self._connect() as connection:
            updated = connection.execute(
                """
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       mlflow_experiment_id = NULL, mlflow_run_id = NULL,
                       mlflow_artifact_uri = NULL, mlflow_dataset_source = NULL,
                       registration_metadata = NULL,
                       failure_code = %s, failure_detail = %s
                 WHERE id = %s AND tenant_id = %s
                   AND state = 'REGISTERING' AND claim_token = %s
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
                "DATASET_MLFLOW_REGISTRATION_FAILED",
                revision - 1,
                revision,
                "MLflow registrar failed the dataset registration task",
                {
                    "failureCode": code[:128],
                    "attemptCount": claim.attempt_count,
                    "modelTrained": False,
                    "productionActivationAllowed": False,
                },
            )
            return True

    def _load_claim(
        self,
        connection: psycopg.Connection,
        registration_id: UUID,
        claim_token: UUID,
    ) -> RegistrationClaim:
        row = connection.execute(
            """
            SELECT registration.*,
                   archive.state AS archive_state,
                   archive.catalog_publication_id AS archive_publication_id,
                   archive.source_snapshot_id AS archive_snapshot_id,
                   archive.source_materialization_id AS archive_materialization_id,
                   archive.manifest_checksum AS archive_manifest_checksum,
                   archive.source_content_sha256 AS archive_content_sha256,
                   archive.source_object_version_id AS archive_source_object_version_id,
                   archive.source_byte_size AS archive_source_byte_size,
                   archive.source_row_count AS archive_source_row_count,
                   archive.source_schema_json AS archive_source_schema_json,
                   archive.table_identifier AS archive_table_identifier,
                   archive.iceberg_snapshot_id AS archive_iceberg_snapshot_id,
                   archive.catalog_semantic_checksum AS archive_semantic_checksum,
                   archive.archive_bucket AS locked_archive_bucket,
                   archive.source_archive_object_key AS locked_source_archive_object_key,
                   archive.source_archive_version_id AS locked_source_archive_version_id,
                   archive.archive_manifest_object_key AS locked_manifest_object_key,
                   archive.archive_manifest_version_id AS locked_manifest_version_id,
                   archive.archive_manifest_sha256 AS locked_manifest_sha256,
                   archive.verified_row_count AS archive_verified_row_count,
                   archive.verified_semantic_checksum AS archive_verified_semantic_checksum,
                   archive.archive_metadata,
                   snapshot.dataset_id, snapshot.line_ids,
                   definition.dataset_code, definition.version AS dataset_version,
                   definition.plant_id
              FROM bpi.bpi_dataset_mlflow_registrations registration
              JOIN bpi.bpi_dataset_retention_archives archive
                ON archive.tenant_id = registration.tenant_id
               AND archive.id = registration.retention_archive_id
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = registration.tenant_id
               AND snapshot.id = registration.source_snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
             WHERE registration.id = %s AND registration.state = 'REGISTERING'
               AND registration.claim_token = %s
            """,
            (registration_id, claim_token),
        ).fetchone()
        if row is None:
            raise LostClaimError("MLflow dataset registration claim could not be loaded")

        frozen_pairs = (
            (row["catalog_publication_id"], row["archive_publication_id"]),
            (row["source_snapshot_id"], row["archive_snapshot_id"]),
            (row["source_materialization_id"], row["archive_materialization_id"]),
            (row["manifest_checksum"], row["archive_manifest_checksum"]),
            (row["source_content_sha256"], row["archive_content_sha256"]),
            (row["source_object_version_id"], row["archive_source_object_version_id"]),
            (row["source_byte_size"], row["archive_source_byte_size"]),
            (row["source_row_count"], row["archive_source_row_count"]),
            (row["source_schema_json"], row["archive_source_schema_json"]),
            (row["table_identifier"], row["archive_table_identifier"]),
            (row["iceberg_snapshot_id"], row["archive_iceberg_snapshot_id"]),
            (row["catalog_semantic_checksum"], row["archive_semantic_checksum"]),
            (row["archive_bucket"], row["locked_archive_bucket"]),
            (row["source_archive_object_key"], row["locked_source_archive_object_key"]),
            (row["source_archive_version_id"], row["locked_source_archive_version_id"]),
            (row["archive_manifest_object_key"], row["locked_manifest_object_key"]),
            (row["archive_manifest_version_id"], row["locked_manifest_version_id"]),
            (row["archive_manifest_sha256"], row["locked_manifest_sha256"]),
        )
        metadata = row["archive_metadata"] or {}
        source_facts_verified = (
            row["registrar_version"] == self._registrar_version
            and row["tracking_profile"] == self._tracking_profile
            and row["archive_state"] == "LOCKED"
            and all(left == right for left, right in frozen_pairs)
            and row["archive_verified_row_count"] == row["source_row_count"]
            and row["archive_verified_semantic_checksum"]
            == row["catalog_semantic_checksum"]
            and metadata.get("objectLockVerified") is True
            and metadata.get("recoveryVerified") is True
            and row["dataset_digest"] == row["catalog_semantic_checksum"][:16]
        )
        return RegistrationClaim(
            id=row["id"],
            tenant_id=row["tenant_id"],
            retention_archive_id=row["retention_archive_id"],
            catalog_publication_id=row["catalog_publication_id"],
            materialization_id=row["source_materialization_id"],
            source_snapshot_id=row["source_snapshot_id"],
            dataset_id=row["dataset_id"],
            dataset_code=row["dataset_code"],
            dataset_version=row["dataset_version"],
            plant_id=row["plant_id"],
            line_ids=tuple(row["line_ids"]),
            registrar_version=row["registrar_version"],
            tracking_profile=row["tracking_profile"],
            manifest_checksum=row["manifest_checksum"],
            source_content_sha256=row["source_content_sha256"],
            source_object_version_id=row["source_object_version_id"],
            source_byte_size=row["source_byte_size"],
            source_row_count=row["source_row_count"],
            source_schema_json=row["source_schema_json"],
            table_identifier=row["table_identifier"],
            iceberg_snapshot_id=row["iceberg_snapshot_id"],
            catalog_semantic_checksum=row["catalog_semantic_checksum"],
            archive_bucket=row["archive_bucket"],
            source_archive_object_key=row["source_archive_object_key"],
            source_archive_version_id=row["source_archive_version_id"],
            archive_manifest_object_key=row["archive_manifest_object_key"],
            archive_manifest_version_id=row["archive_manifest_version_id"],
            archive_manifest_sha256=row["archive_manifest_sha256"],
            experiment_name=row["experiment_name"],
            dataset_name=row["dataset_name"],
            dataset_digest=row["dataset_digest"],
            source_facts_verified=source_facts_verified,
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
            (recovered["tenant_id"], recovered["source_snapshot_id"]),
        ).fetchone()
        if scope is None:
            raise RuntimeError("MLflow registration recovery scope is missing")
        connection.execute(
            """
            INSERT INTO bpi.bpi_audit_events
                (id, tenant_id, plant_id, line_id, object_type, object_id, action,
                 actor_id, before_revision, after_revision, reason, trace_id, detail)
            VALUES (%s, %s, %s, NULL, 'DATASET_MLFLOW_REGISTRATION', %s, %s,
                    'bpi-dataset-mlflow-registrar', %s, %s,
                    'Automated stale MLflow registrar claim recovery', %s, %s)
            """,
            (
                uuid4(),
                recovered["tenant_id"],
                scope["plant_id"],
                recovered["id"],
                action,
                recovered["before_revision"],
                recovered["after_revision"],
                f"mlflow-registrar-recovery:{recovered['id']}:{recovered['after_revision']}",
                Jsonb({"attemptCount": recovered["attempt_count"]}),
            ),
        )

    def _insert_audit(
        self,
        connection: psycopg.Connection,
        claim: RegistrationClaim,
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
            VALUES (%s, %s, %s, NULL, 'DATASET_MLFLOW_REGISTRATION', %s, %s,
                    'bpi-dataset-mlflow-registrar', %s, %s, %s, %s, %s)
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
                f"mlflow-registrar:{claim.claim_token}",
                Jsonb(detail),
            ),
        )

    def _connect(self) -> psycopg.Connection:
        return psycopg.connect(
            self._database_url,
            connect_timeout=10,
            row_factory=dict_row,
        )
