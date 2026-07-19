#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SERVICE = ROOT / "services/bpi-service"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

REQUIRED_FILES = [
    "services/bpi-service/pom.xml",
    "services/bpi-service/Dockerfile",
    "services/bpi-service/app/pom.xml",
    "services/bpi-service/app/src/main/resources/application.yml",
    "services/bpi-service/app/src/main/resources/db/migration/V1__bpi_phase1_baseline.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V2__bpi_tenant_and_runtime_hardening.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V3__bpi_telemetry_ingress.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V4__bpi_end_boundary_lifecycle.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V5__bpi_rule_simulation_and_topology_scope.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V6__bpi_rule_publication_outbox.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V7__bpi_rule_publication_operations.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V8__bpi_rule_application_receipts.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V9__bpi_topology_productization.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V10__bpi_point_catalog_readiness.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V11__bpi_point_catalog_least_privilege.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V12__bpi_point_catalog_source_property.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V13__bpi_rule_runtime_readiness_receipts.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V14__bpi_rule_approval_workflow.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V15__bpi_rule_retirement_lifecycle.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V16__bpi_point_catalog_repeated_observations.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V17__bpi_point_calibration_governance.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V18__bpi_point_calibration_cursor_index.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V19__bpi_data_quality_incident_workbench.sql",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiTelemetryPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiRulePostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiRuleOutboxKafkaPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/CandidateEventMapper.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/CandidateService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/IdempotencyRecord.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/CandidateController.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/RuleController.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/RuleService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/VersionComparisonService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/RuleApprovalView.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/VersionChangeView.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/VersionComparisonView.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/RulePublicationFactory.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/RulePostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/outbox/RulePublicationOutboxRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/outbox/RulePublicationOutboxDispatcher.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/outbox/RulePublicationKafkaConfiguration.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/RuleRuntimeReadinessReceiptService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/PointCalibrationService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/PointCalibrationCursorCodec.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/PointCalibrationPage.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/PointCatalogCursorCodec.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/PointCatalogPage.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/PointCalibrationPostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/PointCalibrationController.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiPointCalibrationPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiPointCatalogPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/application/RuleApplicationKafkaListener.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/application/RuleRuntimeReadinessKafkaRecordProcessor.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/application/RuleRuntimeReadinessPostgresRepository.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiRuleApplicationKafkaPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/VersionComparisonServiceTest.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/candidate/BpiCandidateEventProperties.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/candidate/BpiCandidateKafkaProperties.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/candidate/BpiCandidateKafkaConfiguration.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/candidate/CandidateKafkaRecordProcessor.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/candidate/CandidateKafkaListener.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/InternalCandidateEventController.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/CandidateEventMapperTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/CandidateKafkaRecordProcessorTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiKafkaPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/DataQualityIncidentCursorCodec.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/DataQualityIncidentService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/DataQualityIngestionService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/DataQualityPostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/dataquality/BpiDataQualityKafkaConfiguration.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/dataquality/DataQualityKafkaRecordProcessor.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/dataquality/DataQualityKafkaListener.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/DataQualityController.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/DataQualityKafkaRecordProcessorTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiDataQualityKafkaPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiDataQualityTargetMarkerProducerTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/infrastructure/candidate/BpiCandidateKafkaConfigurationTest.java",
    "services/bpi-service/batch-rule-runtime/src/main/java/com/mapletct/ftmes/bpi/rules/BoundaryWindowEvaluator.java",
    "services/bpi-service/batch-rule-runtime/src/test/java/com/mapletct/ftmes/bpi/rules/BoundaryWindowEvaluatorTest.java",
    "contracts/bpi-api/service-phase1-profile.json",
    "docs/backend-table-audit/bpi-phase1-persistence.md",
    "docs/backend-table-audit/bpi-telemetry-ingress.md",
    "docs/testing/bpi-boundary-runtime-acceptance.md",
    "docs/backend-table-audit/bpi-candidate-protobuf-ingress.md",
    "metadata/bpi-phase1-persistence-acceptance.json",
    "metadata/bpi-telemetry-persistence-acceptance.json",
    "metadata/bpi-boundary-runtime-acceptance.json",
    "metadata/bpi-candidate-protobuf-persistence-acceptance.json",
    "docs/backend-table-audit/bpi-candidate-kafka-ingress.md",
    "metadata/bpi-candidate-kafka-persistence-acceptance.json",
    "docs/backend-table-audit/bpi-rule-management.md",
    "metadata/bpi-rule-management-acceptance.json",
    "docs/backend-table-audit/bpi-rule-publication-outbox.md",
    "metadata/bpi-rule-publication-outbox-acceptance.json",
    "docs/testing/bpi-rule-runtime-readiness-acceptance.md",
    "metadata/bpi-rule-runtime-readiness-acceptance.json",
    "metadata/bpi-rule-runtime-readiness-target-acceptance.json",
    "metadata/bpi-rule-application-kafka-postgres-acceptance.json",
    "docs/testing/bpi-rule-version-lifecycle-acceptance.md",
    "metadata/bpi-rule-version-lifecycle-acceptance.json",
    "docs/testing/bpi-rule-retirement-acceptance.md",
    "metadata/bpi-rule-retirement-acceptance.json",
    "docs/testing/bpi-point-calibration-governance-acceptance.md",
    "metadata/bpi-point-calibration-governance-acceptance.json",
    "metadata/bpi-point-calibration-governance.png",
    "docs/testing/bpi-point-calibration-pagination-acceptance.md",
    "metadata/bpi-point-calibration-pagination-acceptance.json",
    "metadata/bpi-point-calibration-pagination.png",
    "docs/testing/bpi-point-catalog-pagination-acceptance.md",
    "metadata/bpi-point-catalog-pagination-acceptance.json",
    "metadata/bpi-point-catalog-pagination.png",
    "docs/testing/bpi-data-quality-workbench-acceptance.md",
    "metadata/bpi-data-quality-workbench-acceptance.json",
    "deploy/docker/scripts/adp-bpi-version-lifecycle-acceptance.js",
    "deploy/docker/scripts/adp-bpi-point-calibration-acceptance.js",
    "deploy/docker/scripts/adp-bpi-point-calibration-pagination-acceptance.js",
    "deploy/docker/scripts/adp-bpi-point-catalog-pagination-acceptance.js",
    "deploy/docker/scripts/adp-bpi-data-quality-acceptance.js",
    "deploy/docker/scripts/bpi-point-catalog-pagination-cleanup.sql",
    "deploy/docker/scripts/bpi-version-lifecycle-fixture.sql",
    "deploy/docker/scripts/bpi-version-lifecycle-verification.sql",
    "deploy/docker/scripts/bpi-version-lifecycle-cleanup.sql",
    "deploy/docker/postgres/init/176-bpi-database-role.sh",
]


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)


def require_text(path: Path, snippets: list[str], failures: list[str]) -> None:
    text = path.read_text(encoding="utf-8")
    for snippet in snippets:
        if snippet not in text:
            fail(f"{path.relative_to(ROOT)} is missing {snippet!r}", failures)


def main() -> int:
    failures: list[str] = []
    for relative in REQUIRED_FILES:
        if not (ROOT / relative).is_file():
            fail(f"missing required BPI file: {relative}", failures)

    if failures:
        print("\n".join(f"ERROR: {item}" for item in failures), file=sys.stderr)
        return 1

    parent = ET.parse(SERVICE / "pom.xml").getroot()
    java_version = parent.findtext("m:properties/m:java.version", namespaces=NS)
    boot_version = parent.findtext("m:parent/m:version", namespaces=NS)
    modules = {item.text for item in parent.findall("m:modules/m:module", NS)}
    if java_version != "17":
        fail(f"BPI Java version must remain 17, found {java_version!r}", failures)
    if boot_version != "3.4.7":
        fail(f"BPI Spring Boot baseline must remain 3.4.7, found {boot_version!r}", failures)
    if modules != {"../../contracts/bpi-events", "batch-rule-runtime", "app"}:
        fail(f"unexpected BPI reactor modules: {sorted(modules)}", failures)

    require_text(
        SERVICE / "app/src/main/resources/db/migration/V1__bpi_phase1_baseline.sql",
        [
            "bpi_batch_candidates",
            "bpi_batch_instances",
            "bpi_batch_state_events",
            "bpi_boundary_evidence",
            "bpi_audit_events",
            "bpi_api_idempotency",
            "'bpi.commands', false",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V2__bpi_tenant_and_runtime_hardening.sql",
        [
            "FOREIGN KEY (tenant_id, topology_version_id)",
            "FOREIGN KEY (tenant_id, batch_id)",
            "DROP CONSTRAINT IF EXISTS bpi_batch_state_events_tenant_id_trace_id_action_key",
            "bpi_service",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V3__bpi_telemetry_ingress.sql",
        [
            "bpi_telemetry_source_state",
            "bpi_telemetry_events",
            "bpi_telemetry_points",
            "bpi_telemetry_point_rejects",
            "bpi_telemetry_quarantine",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V4__bpi_end_boundary_lifecycle.sql",
        [
            "CLOSED_RAW",
            "uq_bpi_open_batch_per_line",
            "WHERE state IN ('ACTIVE', 'SUSPENDED')",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V5__bpi_rule_simulation_and_topology_scope.sql",
        [
            "bpi_rule_golden_boundaries",
            "bpi_rule_simulations",
            "latest_simulation_id",
            "'bpi.rule-management', false",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V6__bpi_rule_publication_outbox.sql",
        [
            "bpi_outbox_events",
            "PENDING",
            "DISPATCHING",
            "PUBLISHED",
            "FAILED",
            "idx_bpi_outbox_dispatch",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V13__bpi_rule_runtime_readiness_receipts.sql",
        [
            "runtime_readiness_status",
            "runtime_readiness_event_id",
            "runtime_point_catalog_source_revision",
            "uq_bpi_outbox_runtime_readiness_event",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V14__bpi_rule_approval_workflow.sql",
        [
            "PENDING_APPROVAL",
            "bpi_rule_approval_requests",
            "uq_bpi_rule_approval_pending",
            "WHERE state = 'PENDING'",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V17__bpi_point_calibration_governance.sql",
        [
            "source_claim_ready_point_count",
            "bpi_point_calibrations",
            "uq_bpi_point_calibration_version",
            "chk_bpi_point_calibration_decision",
            "WHERE state = 'APPROVED'",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V18__bpi_point_calibration_cursor_index.sql",
        [
            "idx_bpi_point_calibrations_scope_cursor",
            "submitted_at DESC",
            "id DESC",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V19__bpi_data_quality_incident_workbench.sql",
        [
            "bpi_data_quality_incidents",
            "bpi_data_quality_incident_events",
            "bpi_data_quality_incident_actions",
            "uq_bpi_data_quality_incident_identity",
            "ACKNOWLEDGED",
            "REASSIGNED",
            "GRANT SELECT, INSERT, UPDATE ON bpi.bpi_data_quality_incidents TO bpi_service",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/DataQualityIncidentCursorCodec.java",
        ["bpi.data-quality.incident.cursor.v1", "HmacSHA256", "MessageDigest.isEqual", "MAX_CURSOR_LENGTH"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/DataQualityIncidentService.java",
        [
            "DEFAULT_PAGE_SIZE = 100",
            "MAX_PAGE_SIZE = 200",
            "scopeFingerprint",
            "limit + 1",
            "/acknowledge",
            "/resolve",
            "repository.find(actor, incidentId)",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/DataQualityPostgresRepository.java",
        [
            "ORDER BY affected_batch_count DESC, severity_rank DESC, last_seen DESC, id DESC",
            "A resolved incident cannot be acknowledged.",
            "Only an acknowledged incident can be resolved.",
            "REASSIGNED",
            "REOPENED",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/dataquality/BpiDataQualityKafkaConfiguration.java",
        ["read_committed", "MANUAL_IMMEDIATE", "setCommitRecovered(true)", "setFailIfSendResultIsError(true)"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/dataquality/DataQualityKafkaRecordProcessor.java",
        [
            "DataQualityEventV1.parseFrom",
            "maxPayloadBytes",
            "detected_at_ms is too far in the future",
            "must appear exactly once",
            "rejectSeparator",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/DataQualityController.java",
        [
            "/bpi/v1/data-quality/incidents",
            "/bpi/v1/data-quality/summary",
            "/bpi/v1/data-quality/incidents/{incidentId}/acknowledge",
            "/bpi/v1/data-quality/incidents/{incidentId}/resolve",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiDataQualityKafkaPostgresAcceptanceTest.java",
        [
            "kafkaEventsAggregateIntoAuditedOperatorWorkflowWithoutDeletingRawFacts",
            "incidentQueueUsesSignedScopeBoundSnapshotCutoffPagination",
            'param("cursor", nextCursor).param("search", "other")',
            "tamperedCursor",
            "Idempotent-Replay",
            "RESOLVED",
            "REOPENED",
            "bpi.data-quality.dlq.v1",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiDataQualityTargetMarkerProducerTest.java",
        [
            "EnabledIfEnvironmentVariable",
            "BPI_TARGET_KAFKA_BOOTSTRAP_SERVERS",
            "BPI_TARGET_MARKER",
            "bpi.data-quality.v1",
            "ENABLE_IDEMPOTENCE_CONFIG",
            "schema_version",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/adp-bpi-data-quality-acceptance.js",
        [
            "BPI_ACCEPTANCE_MARKER",
            "/bpi-api/data-quality/",
            "open-data-quality-acknowledge",
            "open-data-quality-resolve",
            "raw marker event was not preserved exactly once",
            "consoleErrors",
            "requestFailures",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/CandidateService.java",
        ["commandsEnabled", "reserveIdempotency", "assertScope(actor, visibleCandidate)",
         "assertIdempotencyReplay", "CANDIDATE_REJECTED", "lockBatchLine", "confirmEnd",
         "END_BOUNDARY_CONFIRMED", "BATCH_CLOSED_RAW"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/IdempotencyRecord.java",
        ["String method", "String resourcePath", "String requestChecksum"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/CandidateController.java",
        ["/bpi/v1/candidates/{candidateId}/confirm", "/bpi/v1/candidates/{candidateId}/reject"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/BatchCommandService.java",
        ["commandsEnabled", "reserveIdempotency", "lockBatch", "transitionBatch",
         "BATCH_SUSPENDED", "BATCH_RESUMED", "insertBatchAudit"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/BatchController.java",
        ["/bpi/v1/batches/{batchId}/suspend", "/bpi/v1/batches/{batchId}/resume"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/RuleService.java",
        [
            "BoundaryWindowEvaluator",
            "findObservations",
            "findGoldenBoundaries",
            "MAX_REPLAY_OBSERVATIONS",
            "simulationChecksum",
            "RULE_SIMULATED",
            "compareTopologies",
            "compareRules",
            "submitApproval",
            "rejectApproval",
            "RULE_APPROVAL_SUBMITTED",
            "RULE_APPROVAL_REJECTED",
            "RULE_PUBLISHED",
            "insertPublication",
            "publicationEventId",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/PointCalibrationService.java",
        [
            "assertConcreteScope",
            "A new point calibration must use If-Match 0.",
            "reviewer other than the submitter",
            "Expired calibration evidence cannot be approved.",
            "POINT_CALIBRATION_REVOKED",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/PointCalibrationController.java",
        [
            "/bpi/v1/point-calibrations",
            "/bpi/v1/point-calibrations/{calibrationId}/approve",
            "/bpi/v1/point-calibrations/{calibrationId}/reject",
            "/bpi/v1/point-calibrations/{calibrationId}/revoke",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/PointCalibrationService.java",
        [
            "DEFAULT_PAGE_SIZE = 50",
            "MAX_PAGE_SIZE = 200",
            "scopeFingerprint",
            "currentTransactionTime",
            "limit + 1",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/PointCalibrationCursorCodec.java",
        [
            "bpi.point-calibration.cursor.v1",
            "HmacSHA256",
            "MessageDigest.isEqual",
            "MAX_CURSOR_LENGTH",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/PointCalibrationPostgresRepository.java",
        [
            "submitted_at <= :snapshotAt",
            "submitted_at < :cursorSubmittedAt",
            "id < :cursorId",
            "ORDER BY submitted_at DESC, id DESC LIMIT :fetchLimit",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/PointCatalogController.java",
        [
            "limit == null",
            "service.current(actorContextFactory.from(jwt), plantId, lineId)",
            "service.currentPage(",
            "page.nextCursor()",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/PointCatalogService.java",
        [
            "DEFAULT_PAGE_SIZE = 100",
            "MAX_PAGE_SIZE = 200",
            "scopeFingerprint",
            "repository.findSnapshot(actor, cursor.snapshotId())",
            "limit + 1",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/PointCatalogCursorCodec.java",
        [
            "bpi.point-catalog.cursor.v1",
            "HmacSHA256",
            "MessageDigest.isEqual",
            "MAX_CURSOR_LENGTH",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/PointCatalogPostgresRepository.java",
        [
            "source_claim_ready_point_count",
            "bpi.bpi_point_calibrations",
            "calibration.valid_from <= s.observed_at",
            "calibration.valid_until > s.observed_at",
            "calibration.valid_from <= CURRENT_TIMESTAMP",
            "calibration.valid_until > CURRENT_TIMESTAMP",
            "(e.product_id, e.device_id, e.property_id)",
            "> (:cursorProductId, :cursorDeviceId, :cursorPropertyId)",
            "ORDER BY e.product_id, e.device_id, e.property_id LIMIT :fetchLimit",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiPointCatalogPostgresAcceptanceTest.java",
        [
            "currentPointCatalogCursorPinsImmutableSnapshotAndSearchScope",
            "firstCursor + \"a\"",
            ".param(\"search\", \"different-search\")",
            ".param(\"limit\", \"201\")",
            "bpi_point_catalog_entries",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiPointCalibrationPostgresAcceptanceTest.java",
        [
            "independentCalibrationApprovalControlsReadinessExpiryAndRevocation",
            "calibration-self-approve-",
            "NOT_YET_EFFECTIVE",
            "calibration-expired-submit-",
            "calibrationListUsesStableScopeBoundKeysetCursor",
            "PAGE-AFTER-SNAPSHOT",
            "REVOKED|3|calibration-author|calibration-reviewer|calibration-reviewer",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/RuleController.java",
        [
            "/bpi/v1/topologies",
            "/bpi/v1/topologies/{topologyId}/compare",
            "/bpi/v1/rules/{ruleId}/compare",
            "/bpi/v1/rules/{ruleId}/simulate",
            "/bpi/v1/rule-simulations/{simulationId}",
            "/bpi/v1/rules/{ruleId}/submit-approval",
            "/bpi/v1/rules/{ruleId}/reject-approval",
            "/bpi/v1/rules/{ruleId}/publish",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiRulePostgresAcceptanceTest.java",
        ["replayEmitsAtTheExactHoldTimerInsteadOfTheWindowEnd", "expectedBoundary", "meanBoundaryErrorSeconds",
         "outboxClaimsRecoverAndReachPublishedOrFailedTerminalState", "publicationStatus",
         "flinkRuntimeReadinessTransitionsPersistAndOlderReceiptCannotOverwriteApiTruth",
         "RULE_RUNTIME_DEGRADED", "RULE_RUNTIME_READY",
         "ruleAndTopologyComparisonUseScopedControlledContent",
         "independentAdministratorCanRejectApprovalBackToDraftWithoutPublication",
         "RULE_APPROVAL_SUBMITTED|2|3", "RULE_APPROVAL_REJECTED|3|4"],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/VersionComparisonServiceTest.java",
        [
            "reportsStableJsonPointersForAddedRemovedAndChangedValues",
            "/bindings/1|CHANGED",
            "/enabled|ADDED",
            "/obsolete|REMOVED",
            "/threshold|CHANGED",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiRuleApplicationKafkaPostgresAcceptanceTest.java",
        [
            "bpi.boundary.rule-runtime-readiness.v1",
            "POINT_DEVICE_NOT_ACTIVE",
            "awaitRuntimeState",
            "RULE_RUNTIME_DEGRADED|3|4",
            "RULE_RUNTIME_READY|4|5",
            "READINESS_DLQ_TOPIC",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiRuleOutboxKafkaPostgresAcceptanceTest.java",
        ["pendingPostgresEventReachesKafkaAndBecomesPublishedExactlyOnce", "outbox_event_id", "PUBLISHED|1|true"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/outbox/RulePublicationOutboxRepository.java",
        ["FOR UPDATE SKIP LOCKED", "Recovered stale dispatcher claim", "markPublished", "markFailed"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/outbox/RulePublicationKafkaConfiguration.java",
        ["ENABLE_IDEMPOTENCE_CONFIG", "ACKS_CONFIG", "bpiRulePublicationKafkaTemplate"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/TelemetryIngestionService.java",
        [
            "BPI_EVENT_INGEST",
            "httpIngressEnabled",
            "lockEventIdentity",
            "findSourceIdentity",
            "SOURCE_EPOCH_REGRESSION",
            "sequence.apply().run()",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/application.yml",
        [
            "BPI_TELEMETRY_HTTP_INGRESS_ENABLED:false",
            "BPI_CANDIDATE_PROTOBUF_HTTP_INGRESS_ENABLED:false",
            "BPI_CANDIDATE_KAFKA_ENABLED:false",
            "BPI_CANDIDATE_KAFKA_ALLOWED_TENANT_IDS:_DENY_ALL_",
            "BPI_RULE_PUBLICATION_OUTBOX_ENABLED:false",
            "BPI_RULE_RUNTIME_READINESS_KAFKA_TOPIC",
            "BPI_RULE_RUNTIME_READINESS_KAFKA_DLQ_TOPIC",
            "BPI_DATA_QUALITY_KAFKA_ENABLED:false",
            "BPI_DATA_QUALITY_KAFKA_ALLOWED_TENANT_IDS:_DENY_ALL_",
            "BPI_DATA_QUALITY_KAFKA_ALLOWED_PLANT_IDS:_DENY_ALL_",
            "BPI_DATA_QUALITY_KAFKA_ALLOWED_LINE_IDS:_DENY_ALL_",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/bpi-version-lifecycle-verification.sql",
        ["'candidates'", "'candidateInbox'", "candidate.candidate_key::text"],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/bpi-version-lifecycle-cleanup.sql",
        [
            "bpi_acceptance_target_candidates",
            "SELECT candidate_key::text FROM bpi_acceptance_target_candidates",
            "'candidates'",
            "'inboxEvents'",
            "'auditEvents'",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/adp-bpi-point-calibration-acceptance.js",
        [
            "expectedConsoleErrorIndexes",
            "same-actor approval must return 422",
            "entry.url === selfApprovalResponse.url()",
            "browser emitted unexpected console errors",
            "non-matching calibration version unexpectedly made a real point READY",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/adp-bpi-point-calibration-pagination-acceptance.js",
        [
            "readAllPages",
            "tamperedCursor",
            "changedScopeParameters",
            "browser lost the point search value while appending a page",
            "browser follow-up request omitted the cursor",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/adp-bpi-point-catalog-pagination-acceptance.js",
        [
            "readAllPages",
            "tamperedCursorStatus",
            "changedSearchStatus",
            "cursor did not remain pinned after a newer snapshot was imported",
            "browser lost the server search value",
            "cleanupRequired",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/bpi-point-catalog-pagination-cleanup.sql",
        [
            "bpi_point_catalog_pagination_targets",
            "bpi.bpi_point_catalog_entries",
            "bpi.bpi_point_catalog_snapshots",
            "bpi.bpi_api_idempotency",
            "bpi.bpi_audit_events",
            "cleanup_ok",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/InternalCandidateEventController.java",
        ["application/x-protobuf", "protobufHttpIngressEnabled", "BatchCandidateV1.parseFrom"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/candidate/BpiCandidateKafkaConfiguration.java",
        [
            "read_committed",
            "MANUAL_IMMEDIATE",
            "setCommitRecovered(true)",
            "setFailIfSendResultIsError(true)",
            "BpiConflictException.class",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/candidate/CandidateKafkaRecordProcessor.java",
        ["candidate_key", "schema_version", "line_id|rule_code", "kafkaProperties.allows"],
        failures,
    )
    require_text(
        SERVICE / "batch-rule-runtime/src/main/java/com/mapletct/ftmes/bpi/rules/BoundaryWindowEvaluator.java",
        ["onObservation", "advanceEventTime", "maxSilence", "firstQuorumEvidenceEvent"],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/docker-compose.yml",
        [
            "bpi-migrate:",
            "bpi-service:",
            "BPI_FLYWAY_ENABLED: \"false\"",
            "BPI_TELEMETRY_HTTP_INGRESS_ENABLED",
            "BPI_CANDIDATE_PROTOBUF_HTTP_INGRESS_ENABLED",
            "BPI_CANDIDATE_KAFKA_ENABLED",
            "BPI_CANDIDATE_KAFKA_ALLOWED_TENANT_IDS",
            "BPI_RULE_PUBLICATION_OUTBOX_ENABLED",
            "BPI_RULE_PUBLICATION_TOPIC",
            "BPI_RULE_RUNTIME_READINESS_KAFKA_TOPIC",
            "BPI_RULE_RUNTIME_READINESS_KAFKA_DLQ_TOPIC",
            "BPI_DATA_QUALITY_KAFKA_ENABLED",
            "BPI_DATA_QUALITY_KAFKA_TOPIC",
            "BPI_DATA_QUALITY_KAFKA_DLQ_TOPIC",
            "BPI_DATA_QUALITY_KAFKA_ALLOWED_TENANT_IDS",
            "profiles: [\"bpi\"]",
        ],
        failures,
    )
    require_text(
        SERVICE / "Dockerfile",
        ["COPY pom.xml pom.xml", "COPY contracts contracts", "bpi-service-*-exec.jar"],
        failures,
    )
    require_text(
        SERVICE / "app/pom.xml",
        ["<classifier>exec</classifier>", "<artifactId>spring-kafka</artifactId>"],
        failures,
    )

    runtime_files = list(SERVICE.rglob("*.java")) + list(SERVICE.rglob("*.sql")) + [
        SERVICE / "app/src/main/resources/application.yml",
        SERVICE / "pom.xml",
        SERVICE / "app/pom.xml",
    ]
    forbidden = ("jdbc:oracle", "oracle.jdbc", "com.supcon")
    for path in runtime_files:
        lowered = path.read_text(encoding="utf-8").lower()
        for marker in forbidden:
            if marker in lowered:
                fail(f"{path.relative_to(ROOT)} contains forbidden legacy marker {marker!r}", failures)

    acceptance = json.loads((ROOT / "metadata/bpi-phase1-persistence-acceptance.json").read_text(encoding="utf-8"))
    database = acceptance.get("database")
    database_engine = database.get("engine") if isinstance(database, dict) else database
    if database_engine != "PostgreSQL":
        fail("BPI persistence acceptance must identify PostgreSQL", failures)
    telemetry_acceptance = json.loads(
        (ROOT / "metadata/bpi-telemetry-persistence-acceptance.json").read_text(encoding="utf-8")
    )
    if telemetry_acceptance.get("database") != "PostgreSQL":
        fail("BPI telemetry acceptance must identify PostgreSQL", failures)
    if telemetry_acceptance.get("summary", {}).get("fail") != 0:
        fail("BPI telemetry acceptance must not contain failed items", failures)
    kafka_acceptance = json.loads(
        (ROOT / "metadata/bpi-candidate-kafka-persistence-acceptance.json").read_text(encoding="utf-8")
    )
    if kafka_acceptance.get("database") != "PostgreSQL":
        fail("BPI Kafka candidate acceptance must identify PostgreSQL", failures)
    if kafka_acceptance.get("summary", {}).get("fail") != 0:
        fail("BPI Kafka candidate acceptance must not contain failed items", failures)
    rule_acceptance = json.loads(
        (ROOT / "metadata/bpi-rule-management-acceptance.json").read_text(encoding="utf-8")
    )
    if rule_acceptance.get("database") != "PostgreSQL":
        fail("BPI rule management acceptance must identify PostgreSQL", failures)
    if rule_acceptance.get("summary", {}).get("fail") != 0:
        fail("BPI rule management acceptance must not contain failed items", failures)
    outbox_acceptance = json.loads(
        (ROOT / "metadata/bpi-rule-publication-outbox-acceptance.json").read_text(encoding="utf-8")
    )
    if outbox_acceptance.get("database") != "PostgreSQL":
        fail("BPI rule publication outbox acceptance must identify PostgreSQL", failures)
    if outbox_acceptance.get("transport") != "Kafka":
        fail("BPI rule publication outbox acceptance must identify Kafka", failures)
    if outbox_acceptance.get("summary", {}).get("fail") != 0:
        fail("BPI rule publication outbox acceptance must not contain failed items", failures)

    data_quality_acceptance = json.loads(
        (ROOT / "metadata/bpi-data-quality-workbench-acceptance.json").read_text(encoding="utf-8")
    )
    if data_quality_acceptance.get("status") != "PASS_TARGET_POSTGRES_KAFKA_BROWSER_CLEANUP":
        fail("BPI data-quality acceptance must retain explicit target lifecycle and cleanup PASS", failures)
    if data_quality_acceptance.get("database") != "PostgreSQL":
        fail("BPI data-quality acceptance must identify PostgreSQL", failures)
    if data_quality_acceptance.get("runtime", {}).get("flywayVersion") != 19:
        fail("BPI data-quality acceptance must prove Flyway V19", failures)
    data_quality_summary = data_quality_acceptance.get("summary", {})
    if (data_quality_summary.get("testedFeatures") != 13
            or data_quality_summary.get("pass") != 13
            or data_quality_summary.get("fail") != 0
            or data_quality_summary.get("targetPending") != 0):
        fail("BPI data-quality acceptance must preserve thirteen passes and no target pending item", failures)
    if data_quality_acceptance.get("persistence", {}).get("rawFactsDeletedOnResolve") != 0:
        fail("BPI data-quality resolution must preserve immutable raw facts", failures)
    data_quality_target = data_quality_acceptance.get("targetEnvironment", {})
    if data_quality_target.get("status") != "PASS":
        fail("BPI data-quality target deployment must retain the target marker PASS", failures)
    if data_quality_target.get("flywayVersion") != 19:
        fail("BPI data-quality target deployment must retain Flyway V19 evidence", failures)
    final_consumer = data_quality_target.get("finalConsumer", {})
    if final_consumer.get("enabled") is not False or final_consumer.get("lag") != 0:
        fail("BPI data-quality target consumer must finish disabled with zero lag", failures)
    if final_consumer.get("scopeAllowlist") != "_DENY_ALL_":
        fail("BPI data-quality target consumer must finish with deny-all scope", failures)
    if data_quality_target.get("dlq", {}).get("totalEndOffset") != 0:
        fail("BPI data-quality target DLQ must remain empty for the accepted marker", failures)
    cleanup = data_quality_target.get("cleanup", {})
    if not cleanup or any(value != 0 for value in cleanup.values()):
        fail("BPI data-quality target marker rows must be fully cleaned", failures)

    readiness_acceptance = json.loads(
        (ROOT / "metadata/bpi-rule-runtime-readiness-acceptance.json").read_text(encoding="utf-8")
    )
    if readiness_acceptance.get("database") != "PostgreSQL":
        fail("BPI runtime-readiness acceptance must identify PostgreSQL", failures)
    if readiness_acceptance.get("status") != "PASS_LOCAL_AND_TARGET_FAIL_CLOSED_SOURCE_BLOCKED":
        fail("BPI runtime-readiness acceptance must retain explicit local and target scope", failures)
    readiness_summary = readiness_acceptance.get("summary", {})
    if (readiness_summary.get("pass") != 5 or readiness_summary.get("fail") != 0
            or readiness_summary.get("blocked") != 1):
        fail("BPI runtime-readiness acceptance must preserve five passes and one source blocker", failures)
    if readiness_summary.get("targetEnvironmentStatus") != "PASS_FAIL_CLOSED_SOURCE_BLOCKED":
        fail("BPI runtime-readiness target status must distinguish fail-closed PASS from source readiness", failures)

    target_readiness = json.loads(
        (ROOT / "metadata/bpi-rule-runtime-readiness-target-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if target_readiness.get("status") != "PASS_FAIL_CLOSED_SOURCE_BLOCKED":
        fail("BPI target readiness acceptance must retain its fail-closed source blocker", failures)
    target_browser = target_readiness.get("browser", {})
    if target_browser.get("publicationStatus") != 422:
        fail("BPI target readiness acceptance must prove HTTP 422 publication rejection", failures)
    if target_browser.get("unexpectedConsoleErrors") != 0 or target_browser.get("pageErrors") != 0:
        fail("BPI target readiness browser acceptance must have zero unexpected errors", failures)
    target_persistence = target_readiness.get("persistence", {})
    if any(target_persistence.get(key) != 0 for key in ("outbox", "candidates", "batches")):
        fail("BPI target source rejection must not create outbox, candidate or batch rows", failures)
    target_catalog = target_readiness.get("pointCatalog", {})
    if target_catalog.get("readyPointCount") != 0 or target_catalog.get("deviceState") != "INACTIVE":
        fail("BPI target source blocker must match the observed inactive non-READY point", failures)
    target_cleanup = target_readiness.get("cleanup", {})
    if any(target_cleanup.get(key) != 0 for key in (
            "topologies", "rules", "goldenBoundaries", "telemetryEvents", "markerFeatureFlags")):
        fail("BPI target readiness marker cleanup must leave no fixture rows", failures)
    if target_cleanup.get("preservedEnabledRuleManagementFlags") != 1:
        fail("BPI target cleanup must preserve the existing enabled rule-management flag", failures)

    kafka_postgres_acceptance = json.loads(
        (ROOT / "metadata/bpi-rule-application-kafka-postgres-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if kafka_postgres_acceptance.get("status") != "PASS_LOCAL_EMBEDDED_KAFKA_POSTGRES":
        fail("BPI rule receipt Kafka/PostgreSQL acceptance must be an explicit local PASS", failures)
    if kafka_postgres_acceptance.get("runtime", {}).get("flywayVersion") != 13:
        fail("BPI rule receipt Kafka/PostgreSQL acceptance must prove Flyway V13", failures)
    database_assertions = kafka_postgres_acceptance.get("databaseAssertions", {})
    if database_assertions.get("finalApplicationStatus") != "APPLIED":
        fail("BPI Kafka/PostgreSQL acceptance must finish with APPLIED control-plane state", failures)
    if database_assertions.get("finalRuntimeReadinessStatus") != "READY":
        fail("BPI Kafka/PostgreSQL acceptance must finish with READY runtime state", failures)

    lifecycle_acceptance = json.loads(
        (ROOT / "metadata/bpi-rule-version-lifecycle-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if lifecycle_acceptance.get("status") != "PASS_CONTROLLED_TARGET":
        fail("BPI version lifecycle acceptance must retain controlled target PASS scope", failures)
    if lifecycle_acceptance.get("database") != "PostgreSQL":
        fail("BPI version lifecycle acceptance must identify PostgreSQL", failures)
    lifecycle_summary = lifecycle_acceptance.get("summary", {})
    if (lifecycle_summary.get("testedFeatures") != 6
            or lifecycle_summary.get("pass") != 6
            or lifecycle_summary.get("fail") != 0
            or lifecycle_summary.get("blocked") != 0):
        fail("BPI version lifecycle acceptance must preserve six passing features", failures)
    lifecycle_browser = lifecycle_acceptance.get("browser", {})
    if (lifecycle_browser.get("unexpectedConsoleErrors") != 0
            or lifecycle_browser.get("pageErrors") != 0
            or lifecycle_browser.get("requestFailures") != 0):
        fail("BPI version lifecycle browser acceptance must have zero unexpected errors", failures)
    if lifecycle_browser.get("expectedConsoleErrors") != 2:
        fail("BPI version lifecycle acceptance must retain two deliberate 422 browser records", failures)
    lifecycle_cleanup = lifecycle_acceptance.get("cleanup", {}).get("remaining", {})
    if any(lifecycle_cleanup.get(key) != 0 for key in (
            "topologies", "rules", "idempotency", "telemetryEvents",
            "catalogSnapshots", "goldenBoundaries")):
        fail("BPI version lifecycle marker cleanup must leave zero fixture rows", failures)

    retirement_acceptance = json.loads(
        (ROOT / "metadata/bpi-rule-retirement-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if retirement_acceptance.get("status") != "PASS_CONTROLLED_TARGET_SHADOW":
        fail("BPI rule retirement acceptance must retain controlled target shadow PASS scope", failures)
    retirement_summary = retirement_acceptance.get("summary", {})
    if (retirement_summary.get("testedFeatures") != 10
            or retirement_summary.get("pass") != 10
            or retirement_summary.get("fail") != 0
            or retirement_summary.get("blocked") != 0):
        fail("BPI rule retirement acceptance must preserve ten passing features", failures)
    retirement_browser = retirement_acceptance.get("browser", {})
    if any(retirement_browser.get(key) != 0 for key in (
            "unexpectedConsoleErrors", "pageErrors", "requestFailures")):
        fail("BPI rule retirement browser acceptance must have zero unexpected errors", failures)
    retirement_persistence = retirement_acceptance.get("candidatePersistence", {})
    if (retirement_persistence.get("candidateRowsBeforeCleanup") != 1
            or retirement_persistence.get("inboxRowsBeforeCleanup") != 1
            or retirement_persistence.get("batchRowsBeforeCleanup") != 0):
        fail("BPI delayed candidate must persist exactly once without creating a batch", failures)
    retirement_cleanup = retirement_acceptance.get("cleanup", {}).get("remaining", {})
    if any(retirement_cleanup.get(key) != 0 for key in (
            "topologies", "rules", "idempotency", "telemetryEvents",
            "catalogSnapshots", "goldenBoundaries", "candidates", "batches",
            "inboxEvents", "outboxEvents", "auditEvents")):
        fail("BPI rule retirement cleanup must leave zero marker rows", failures)

    calibration_acceptance = json.loads(
        (ROOT / "metadata/bpi-point-calibration-governance-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if calibration_acceptance.get("status") != "PASS_CONTROLLED_TARGET_FAIL_CLOSED":
        fail("BPI calibration acceptance must retain controlled fail-closed target scope", failures)
    if calibration_acceptance.get("database") != "PostgreSQL":
        fail("BPI calibration acceptance must identify PostgreSQL", failures)
    if calibration_acceptance.get("environment", {}).get("flywayVersion") != 17:
        fail("BPI calibration acceptance must prove Flyway V17", failures)
    calibration_summary = calibration_acceptance.get("summary", {})
    if (calibration_summary.get("testedFeatures") != 10
            or calibration_summary.get("pass") != 10
            or calibration_summary.get("fail") != 0
            or calibration_summary.get("blocked") != 0):
        fail("BPI calibration acceptance must preserve ten passing controlled features", failures)
    calibration_browser = calibration_acceptance.get("browser", {})
    if calibration_browser.get("expectedConsoleErrors") != 1:
        fail("BPI calibration browser acceptance must retain the deliberate same-actor 422", failures)
    if any(calibration_browser.get(key) != 0 for key in (
            "unexpectedConsoleErrors", "pageErrors", "requestFailures")):
        fail("BPI calibration browser acceptance must have zero unexpected errors", failures)
    calibration_persistence = calibration_acceptance.get("persistence", {})
    if (calibration_persistence.get("finalState") != "REVOKED"
            or calibration_persistence.get("finalRevision") != 3
            or calibration_persistence.get("auditRows") != 3
            or calibration_persistence.get("completedIdempotencyRows") != 3):
        fail("BPI calibration persistence must retain the audited r1-r3 lifecycle", failures)
    if (calibration_persistence.get("matchingEffectiveEvidence") != 0
            or calibration_persistence.get("operationalReadyPointCount") != 0
            or calibration_persistence.get("sourceSequenceEnabled") is not False):
        fail("BPI calibration target record must keep the real pilot fail closed", failures)
    screenshot_path = ROOT / calibration_browser.get("screenshot", "")
    if screenshot_path.is_file():
        screenshot_hash = hashlib.sha256(screenshot_path.read_bytes()).hexdigest()
        if screenshot_hash != calibration_browser.get("screenshotSha256"):
            fail("BPI calibration screenshot hash does not match the acceptance record", failures)

    pagination_acceptance = json.loads(
        (ROOT / "metadata/bpi-point-calibration-pagination-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if pagination_acceptance.get("status") != "PASS_CONTROLLED_TARGET_READ_ONLY":
        fail("BPI calibration pagination acceptance must retain controlled read-only PASS", failures)
    if pagination_acceptance.get("database") != "PostgreSQL":
        fail("BPI calibration pagination acceptance must identify PostgreSQL", failures)
    if pagination_acceptance.get("environment", {}).get("flywayVersion") != 18:
        fail("BPI calibration pagination acceptance must prove Flyway V18", failures)
    pagination_summary = pagination_acceptance.get("summary", {})
    if (pagination_summary.get("testedFeatures") != 8
            or pagination_summary.get("pass") != 8
            or pagination_summary.get("fail") != 0
            or pagination_summary.get("blocked") != 0):
        fail("BPI calibration pagination acceptance must preserve eight passing features", failures)
    pagination_api = pagination_acceptance.get("api", {})
    if (len(pagination_api.get("pages", [])) != 2
            or pagination_api.get("totalItems") != 4
            or pagination_api.get("uniqueItems") != 4
            or pagination_api.get("tamperedCursorStatus") != 422
            or pagination_api.get("changedScopeStatus") != 422):
        fail("BPI calibration pagination API evidence is incomplete", failures)
    pagination_browser = pagination_acceptance.get("browser", {})
    if (pagination_browser.get("loadedItems") != 4
            or pagination_browser.get("uniqueItems") != 4
            or pagination_browser.get("cursorRequests") != 1
            or pagination_browser.get("searchValuePreserved") is not True
            or any(pagination_browser.get(key) != 0 for key in (
                "consoleErrors", "pageErrors", "requestFailures"))):
        fail("BPI calibration pagination browser evidence is incomplete", failures)
    pagination_persistence = pagination_acceptance.get("persistence", {})
    if (pagination_persistence.get("rowCountBefore") != 4
            or pagination_persistence.get("rowCountAfter") != 4
            or pagination_persistence.get("mutatedRows") != 0
            or pagination_persistence.get("indexValid") is not True
            or pagination_persistence.get("indexReady") is not True):
        fail("BPI calibration pagination must retain read-only indexed PostgreSQL proof", failures)
    pagination_regression = pagination_acceptance.get("localRegression", {})
    if (pagination_regression.get("totalJava17Tests") != 83
            or pagination_regression.get("simulationTests") != 9
            or pagination_regression.get("browserE2eTests") != 11):
        fail("BPI calibration pagination regression totals are incomplete", failures)
    pagination_screenshot_path = ROOT / pagination_browser.get("screenshot", "")
    if pagination_screenshot_path.is_file():
        screenshot_hash = hashlib.sha256(pagination_screenshot_path.read_bytes()).hexdigest()
        if screenshot_hash != pagination_browser.get("screenshotSha256"):
            fail("BPI calibration pagination screenshot hash does not match", failures)

    point_pagination = json.loads(
        (ROOT / "metadata/bpi-point-catalog-pagination-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if point_pagination.get("status") != "PASS_TARGET_CLEANED":
        fail("BPI point catalog pagination acceptance must retain cleaned target PASS", failures)
    point_runtime = point_pagination.get("runtime", {})
    if (point_runtime.get("database") != "PostgreSQL 15.18"
            or point_runtime.get("flywayVersion") != 18
            or point_runtime.get("serviceHealth") != "UP"):
        fail("BPI point catalog pagination runtime evidence is incomplete", failures)
    point_api = point_pagination.get("api", {})
    if (len(point_api.get("pages", [])) != 3
            or point_api.get("totalItems") != 5
            or point_api.get("uniqueItems") != 5
            or point_api.get("tamperedCursorStatus") != 422
            or point_api.get("changedSearchStatus") != 422
            or point_api.get("pinnedContinuationSnapshotId") != point_api.get("snapshotId")
            or point_api.get("legacyPointCount") != 1):
        fail("BPI point catalog pagination API evidence is incomplete", failures)
    point_browser = point_pagination.get("browser", {})
    if (len(point_browser.get("loadedIds", [])) != 5
            or len(set(point_browser.get("loadedIds", []))) != 5
            or sum(1 for item in point_browser.get("requests", []) if item.get("hasCursor")) != 2
            or any(point_browser.get(key) for key in (
                "consoleErrors", "pageErrors", "requestFailures"))):
        fail("BPI point catalog pagination browser evidence is incomplete", failures)
    point_cleanup = point_pagination.get("cleanup", {})
    if (point_pagination.get("cleanupRequired") is not False
            or point_cleanup.get("deletedSnapshots") != 2
            or point_cleanup.get("deletedEntries") != 6
            or point_cleanup.get("postgresBaselineRestored") is not True
            or point_cleanup.get("apiBaselineRestored") is not True
            or any(point_cleanup.get(key) != 0 for key in (
                "remainingSnapshots", "remainingEntries",
                "remainingAuditEvents", "remainingIdempotencyRecords"))):
        fail("BPI point catalog pagination marker cleanup is incomplete", failures)
    point_screenshot_path = ROOT / point_pagination.get("screenshot", "")
    if point_screenshot_path.is_file():
        screenshot_hash = hashlib.sha256(point_screenshot_path.read_bytes()).hexdigest()
        if screenshot_hash != point_pagination.get("screenshotSha256"):
            fail("BPI point catalog pagination screenshot hash does not match", failures)

    if failures:
        print("\n".join(f"ERROR: {item}" for item in failures), file=sys.stderr)
        return 1
    print("BPI service structure, PostgreSQL ownership, and shadow-only boundaries verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
