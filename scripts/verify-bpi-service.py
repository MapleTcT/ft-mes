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
    "services/bpi-service/wms-adapter/pom.xml",
    "services/bpi-service/wms-adapter/Dockerfile",
    "services/bpi-service/wms-adapter/Dockerfile.runtime",
    "services/bpi-service/wms-adapter/src/main/resources/application.yml",
    "services/bpi-service/wms-adapter/src/main/java/com/mapletct/ftmes/bpiwmsadapter/WmsCommandProcessor.java",
    "services/bpi-service/wms-adapter/src/main/java/com/mapletct/ftmes/bpiwmsadapter/MaterialWmsHttpClient.java",
    "services/bpi-service/wms-adapter/src/test/java/com/mapletct/ftmes/bpiwmsadapter/WmsCommandProcessorTest.java",
    "deploy/docker/postgres/init/192-material-wms-bpi-idempotency.sql",
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
    "services/bpi-service/app/src/main/resources/db/migration/V20__bpi_shadow_run_acceptance.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V21__bpi_feature_flag_governance.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V22__bpi_source_sequence_evidence.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V23__bpi_quality_release_wms_inbound.sql",
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
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/ShadowRunService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/ShadowRunPostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/ShadowRunController.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/ShadowRunCreateCommand.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/ShadowRunBatchReviewCommand.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiShadowRunPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiQualityReleaseWmsPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/BatchReleaseService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/BatchReleasePostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/integration/Phase2IntegrationKafkaRecordProcessor.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/integration/WmsInboundOutboxRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/InternalPhase2IntegrationController.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/infrastructure/candidate/BpiCandidateKafkaConfigurationTest.java",
    "services/bpi-service/batch-rule-runtime/src/main/java/com/mapletct/ftmes/bpi/rules/BoundaryWindowEvaluator.java",
    "services/bpi-service/batch-rule-runtime/src/test/java/com/mapletct/ftmes/bpi/rules/BoundaryWindowEvaluatorTest.java",
    "contracts/bpi-api/service-phase1-profile.json",
    "contracts/bpi-api/service-phase2-profile.json",
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
    "docs/testing/bpi-shadow-run-acceptance.md",
    "metadata/bpi-shadow-run-acceptance.json",
    "docs/backend-table-audit/bpi-quality-release-wms-inbound.md",
    "docs/testing/bpi-quality-release-wms-inbound-acceptance.md",
    "metadata/bpi-quality-release-wms-inbound-acceptance.json",
    "metadata/bpi-quality-release-wms-target-acceptance.json",
    "metadata/bpi-quality-release-wms-target.png",
    "metadata/bpi-quality-release-wms-live-target.png",
    "metadata/bpi-quality-release-wms-live-target-bottom.png",
    "metadata/bpi-shadow-run-acceptance.png",
    "docs/testing/bpi-flink-data-quality-acceptance.md",
    "metadata/bpi-flink-data-quality-acceptance.json",
    "deploy/docker/scripts/adp-bpi-version-lifecycle-acceptance.js",
    "deploy/docker/scripts/adp-bpi-point-calibration-acceptance.js",
    "deploy/docker/scripts/adp-bpi-point-calibration-pagination-acceptance.js",
    "deploy/docker/scripts/adp-bpi-point-catalog-pagination-acceptance.js",
    "deploy/docker/scripts/adp-bpi-data-quality-acceptance.js",
    "deploy/docker/scripts/adp-bpi-shadow-run-acceptance.js",
    "deploy/docker/scripts/adp-bpi-quality-release-target-acceptance.js",
    "deploy/docker/scripts/bpi-shadow-run-acceptance-fixture.sql",
    "deploy/docker/scripts/bpi-shadow-run-acceptance-verification.sql",
    "deploy/docker/scripts/bpi-shadow-run-acceptance-cleanup.sql",
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
    if modules != {"../../contracts/bpi-events", "batch-rule-runtime", "app", "wms-adapter"}:
        fail(f"unexpected BPI reactor modules: {sorted(modules)}", failures)

    require_text(
        SERVICE / "wms-adapter/src/main/resources/application.yml",
        [
            "BPI_WMS_ADAPTER_ENABLED:false",
            "BPI_WMS_ADAPTER_MATERIAL_API_KEY:_DISABLED_",
            "BPI_WMS_ADAPTER_ROUTES:_DENY_ALL_",
            "bpi.wms.completion-inbound-command.dlq.v1",
        ],
        failures,
    )
    require_text(
        SERVICE / "wms-adapter/src/main/java/com/mapletct/ftmes/bpiwmsadapter/WmsCommandProcessor.java",
        [
            "findByIdempotency",
            "createCompletionInbound",
            "WMS_IDEMPOTENCY_CONFLICT",
            "material-wms acknowledged creation but exact lookup did not find the document",
        ],
        failures,
    )

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
        SERVICE / "app/src/main/resources/db/migration/V20__bpi_shadow_run_acceptance.sql",
        [
            "bpi_shadow_runs",
            "bpi_shadow_run_batch_reviews",
            "minimum_duration_days BETWEEN 7 AND 14",
            "minimum_boundary_agreement BETWEEN 0.950000 AND 1.000000",
            "uq_bpi_shadow_run_active_scope",
            "WHERE state = 'RUNNING'",
            "uq_bpi_shadow_review_active_batch",
            "WHERE state = 'ACTIVE'",
            "GRANT SELECT, INSERT, UPDATE ON bpi.bpi_shadow_runs TO bpi_service",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V23__bpi_quality_release_wms_inbound.sql",
        [
            "bpi_quality_gates",
            "bpi_quality_links",
            "bpi_wms_inbound_links",
            "fk_bpi_wms_inbound_outbox_tenant",
            "WMS_COMPLETION_INBOUND_COMMAND",
            "reject_shadow_wms_command",
            "trg_bpi_reject_shadow_wms_command",
            "'bpi.qcs-link', false",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/BatchReleaseService.java",
        [
            "BatchState.CLOSED_RAW",
            "BatchState.WAIT_QA",
            "BatchState.RELEASED",
            "BatchState.REJECTED",
            "BatchState.INBOUNDED",
            "WMS receipt cannot precede durable command publication.",
            "WMS receipt status must be ACCEPTED or REJECTED.",
            "Accepted WMS receipts require document_id.",
            "!batch.shadow()",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/ShadowRunService.java",
        [
            "A new shadow run must use If-Match 0.",
            "A shadow run must pin a PUBLISHED rule version.",
            "Only a CLOSED_RAW shadow batch can be reviewed.",
            "minimum duration and batch review count",
            "Shadow run decisions require an administrator other than the creator.",
            "Shadow run approval gates are not satisfied",
            "reserveIdempotency",
            "completeIdempotency",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/ShadowRunPostgresRepository.java",
        [
            '"PUBLISHED".equals(rs.getString("publication_status"))',
            '"APPLIED".equals(rs.getString("application_status"))',
            '"READY".equals(rs.getString("runtime_readiness_status"))',
            "BOUNDARY_AGREEMENT_BELOW_THRESHOLD",
            "CUMULATIVE_QUANTITY_DEVIATION_OUT_OF_TOLERANCE",
            "UNRESOLVED_CRITICAL_DATA_QUALITY",
            "bpi.bpi_point_calibrations",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/ShadowRunController.java",
        [
            "/bpi/v1/shadow-runs",
            "/bpi/v1/shadow-runs/{runId}/batch-reviews",
            "/bpi/v1/shadow-runs/{runId}/start",
            "/bpi/v1/shadow-runs/{runId}/complete",
            "/bpi/v1/shadow-runs/{runId}/approve",
            "/bpi/v1/shadow-runs/{runId}/reject",
            "/bpi/v1/shadow-runs/{runId}/cancel",
            "hasRole('BPI_ADMIN')",
            "Idempotent-Replay",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiShadowRunPostgresAcceptanceTest.java",
        [
            "realShadowRunRequiresPinnedReadinessHumanAgreementQuantityAndIndependentApproval",
            "UNRESOLVED_CRITICAL_DATA_QUALITY",
            'jsonPath("$.data.metrics.boundaryAgreement").value(0.95)',
            'isEqualTo("APPROVED|14|shadow-author|shadow-admin")',
            "DELETE FROM bpi.bpi_shadow_run_batch_reviews",
            "DELETE FROM bpi.bpi_shadow_runs",
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
        ROOT / "deploy/docker/scripts/adp-bpi-shadow-run-acceptance.js",
        [
            "BPI_TIME_COMPRESSION_DAYS",
            "expectedApprover",
            "/bpi-api/shadow-runs/",
            "first batch deliberate 61 second end deviation",
            "UNRESOLVED_CRITICAL_DATA_QUALITY",
            "qualityGate=NOT_APPLICABLE",
            "requestFailures",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/bpi-shadow-run-acceptance-cleanup.sql",
        [
            "target_shadow_runs",
            "bpi_shadow_run_batch_reviews",
            "bpi_api_idempotency",
            "bpi_point_calibrations",
            "remaining",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/bpi-shadow-run-acceptance-verification.sql",
        [
            "active_reviews",
            "boundary_agreement",
            "cumulative_quantity_deviation_percent",
            "qualityNotApplicable",
            "wmsNotRequested",
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
            "BPI_PHASE2_INTEGRATION_ENABLED:false",
            "BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED:false",
            "BPI_PHASE2_KAFKA_ENABLED:false",
            "BPI_PHASE2_ALLOWED_TENANT_IDS:_DENY_ALL_",
            "BPI_WMS_OUTBOX_ENABLED:false",
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
            "BPI_PHASE2_INTEGRATION_ENABLED",
            "BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED",
            "BPI_PHASE2_KAFKA_ENABLED",
            "BPI_PHASE2_ALLOWED_TENANT_IDS",
            "BPI_PHASE2_ALLOWED_PLANT_IDS",
            "BPI_PHASE2_ALLOWED_LINE_IDS",
            "BPI_WMS_OUTBOX_ENABLED",
            "BPI_WMS_OUTBOX_TOPIC",
            "profiles: [\"bpi\"]",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/upgrade-bpi-integrated-expand-only.sh",
        [
            "UPGRADE_INTEGRATED_BPI_EXPAND_ONLY",
            "pg_dump -Fc",
            "docker image tag",
            "schemaDowngradeAllowed",
            "BPI_PHASE2_INTEGRATION_ENABLED",
            "bpi.qcs-link=false",
            "bpi.wms-link=false",
            "rsync -a --delete",
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
        [
            "<classifier>exec</classifier>",
            "<artifactId>spring-kafka</artifactId>",
            "<spring.datasource.hikari.maximum-pool-size>2</spring.datasource.hikari.maximum-pool-size>",
            "<spring.datasource.hikari.minimum-idle>0</spring.datasource.hikari.minimum-idle>",
        ],
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
    if data_quality_acceptance.get("status") != "PASS_TARGET_FLINK_KAFKA_POSTGRES_BROWSER_CLEANUP":
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
    if (final_consumer.get("enabled") is not False
            or final_consumer.get("activeMembers") != 0
            or final_consumer.get("businessRecordLag") != 0):
        fail("BPI data-quality target consumer must finish disabled with zero members and zero business lag", failures)
    if final_consumer.get("reportedLag", 0) > 0 and not final_consumer.get("lagExplanation"):
        fail("BPI data-quality target consumer must explain non-business transaction-control lag", failures)
    if final_consumer.get("scopeAllowlist") != "_DENY_ALL_":
        fail("BPI data-quality target consumer must finish with deny-all scope", failures)
    target_dlq = data_quality_target.get("dlq", {})
    if target_dlq.get("finalFlinkMarkerRecords") != 0:
        fail("BPI data-quality target DLQ must remain empty for the accepted Flink marker", failures)
    if target_dlq.get("totalEndOffset", 0) > 0 and (
            not target_dlq.get("retainedDiagnosticMarker")
            or not target_dlq.get("retainedReason")):
        fail("BPI data-quality target DLQ diagnostics must remain attributable and explained", failures)
    cleanup = data_quality_target.get("cleanup", {})
    if not cleanup or any(value != 0 for value in cleanup.values()):
        fail("BPI data-quality target marker rows must be fully cleaned", failures)

    automatic_producer = data_quality_acceptance.get("automaticProducerAcceptance", {})
    flink_data_quality = json.loads(
        (ROOT / "metadata/bpi-flink-data-quality-acceptance.json").read_text(encoding="utf-8")
    )
    if (automatic_producer.get("status") != "PASS"
            or automatic_producer.get("marker") != flink_data_quality.get("finalMarker", {}).get("value")
            or automatic_producer.get("automaticEvents") != 4
            or automatic_producer.get("postgresIncidents") != 4
            or automatic_producer.get("browserRows") != 4
            or automatic_producer.get("browserErrors") != 0
            or automatic_producer.get("markerCleanup") != "PASS"):
        fail("BPI data-quality automatic producer acceptance must preserve Flink, persistence, browser and cleanup proof", failures)

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

    shadow_acceptance = json.loads(
        (ROOT / "metadata/bpi-shadow-run-acceptance.json").read_text(encoding="utf-8")
    )
    if shadow_acceptance.get("status") != "PASS_CONTROLLED_TARGET_TIME_COMPRESSED_CLEANED":
        fail("BPI shadow-run acceptance must retain controlled, time-compressed and cleaned scope", failures)
    if shadow_acceptance.get("database") != "PostgreSQL":
        fail("BPI shadow-run acceptance must identify PostgreSQL", failures)
    shadow_environment = shadow_acceptance.get("environment", {})
    if (shadow_environment.get("flywayVersion") != 20
            or shadow_environment.get("serviceHealth") != "healthy"
            or shadow_environment.get("adapterHealth") != "healthy"):
        fail("BPI shadow-run target runtime evidence is incomplete", failures)
    shadow_summary = shadow_acceptance.get("summary", {})
    if (shadow_summary.get("testedFeatures") != 15
            or shadow_summary.get("pass") != 15
            or shadow_summary.get("fail") != 0
            or shadow_summary.get("blocked") != 0
            or shadow_summary.get("productionReadiness") != "PARTIAL_FIELD_DURATION_PENDING"):
        fail("BPI shadow-run acceptance must preserve fifteen passes and the field-duration boundary", failures)
    shadow_time = shadow_acceptance.get("timeCompression", {})
    if (shadow_time.get("applied") is not True
            or shadow_time.get("days") != 8
            or shadow_time.get("fieldDurationEvidence") is not False):
        fail("BPI shadow-run acceptance must not present time compression as field evidence", failures)
    shadow_browser = shadow_acceptance.get("browser", {})
    if (shadow_browser.get("capturedBpiRequests") != 57
            or shadow_browser.get("non2xxResponses") != 0
            or any(shadow_browser.get(key) != 0 for key in (
                "consoleErrors", "pageErrors", "requestFailures"))):
        fail("BPI shadow-run browser evidence is incomplete", failures)
    shadow_persistence = shadow_acceptance.get("persistence", {})
    if (shadow_persistence.get("finalStateBeforeCleanup") != "APPROVED"
            or shadow_persistence.get("finalRevisionBeforeCleanup") != 14
            or shadow_persistence.get("activeReviews") != 10
            or shadow_persistence.get("boundaryAgreement") != 0.95
            or shadow_persistence.get("cumulativeQuantityDeviationPercent") != 0
            or shadow_persistence.get("auditRows") != 16
            or shadow_persistence.get("completedIdempotencyRows") != 10
            or shadow_persistence.get("closedRawBatches") != 10
            or shadow_persistence.get("qualityNotApplicableBatches") != 10
            or shadow_persistence.get("wmsNotRequestedBatches") != 10):
        fail("BPI shadow-run PostgreSQL acceptance evidence is incomplete", failures)
    if any(value != 0 for value in shadow_acceptance.get("cleanup", {}).values()):
        fail("BPI shadow-run marker cleanup must leave zero fixture rows", failures)
    shadow_screenshot_path = ROOT / shadow_browser.get("screenshot", "")
    if shadow_screenshot_path.is_file():
        screenshot_hash = hashlib.sha256(shadow_screenshot_path.read_bytes()).hexdigest()
        if screenshot_hash != shadow_browser.get("screenshotSha256"):
            fail("BPI shadow-run screenshot hash does not match", failures)

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

    quality_wms_acceptance = json.loads(
        (ROOT / "metadata/bpi-quality-release-wms-inbound-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if quality_wms_acceptance.get("status") != "PASS_LOCAL_POSTGRES_CONTRACTS_NOT_TARGET_ACTIVATED":
        fail("BPI QCS/WMS acceptance must retain its local-only non-activated boundary", failures)
    if (quality_wms_acceptance.get("database") != "PostgreSQL 16.13"
            or quality_wms_acceptance.get("flywayVersion") != 23
            or quality_wms_acceptance.get("phase1Mode") != "SHADOW_ONLY"
            or quality_wms_acceptance.get("phase2Mode") != "DISABLED_BY_DEFAULT"):
        fail("BPI QCS/WMS PostgreSQL/runtime boundary is incomplete", failures)
    quality_wms_summary = quality_wms_acceptance.get("summary", {})
    if (quality_wms_summary.get("testedFeatures") != 10
            or quality_wms_summary.get("pass") != 8
            or quality_wms_summary.get("fail") != 0
            or quality_wms_summary.get("blocked") != 2
            or quality_wms_summary.get("postgresAcceptanceTests") != 4
            or quality_wms_summary.get("postgresAcceptanceFailures") != 0):
        fail("BPI QCS/WMS acceptance summary must preserve eight passes and two target blockers", failures)
    quality_wms_items = quality_wms_acceptance.get("items", [])
    if (len(quality_wms_items) != 10
            or sum(item.get("status") == "PASS" for item in quality_wms_items) != 8
            or sum(item.get("status") == "BLOCKED" for item in quality_wms_items) != 2):
        fail("BPI QCS/WMS item statuses do not match the acceptance summary", failures)
    if any(quality_wms_acceptance.get("defaults", {}).values()):
        fail("BPI QCS/WMS activation defaults must remain false", failures)
    if any(quality_wms_acceptance.get("cleanup", {}).values()):
        fail("BPI QCS/WMS marker cleanup must leave zero fixture rows", failures)
    if len(quality_wms_acceptance.get("repoCommit", "")) != 40:
        fail("BPI QCS/WMS acceptance must point to the exact source commit", failures)

    quality_wms_target = json.loads(
        (ROOT / "metadata/bpi-quality-release-wms-target-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if quality_wms_target.get("status") != (
            "PASS_TARGET_CONTROLLED_QCS_EVENT_KAFKA_WMS_POSTGRES_BROWSER_CLEANED"):
        fail("BPI target QCS/WMS acceptance must record the controlled full-chain result", failures)
    target = quality_wms_target.get("target", {})
    source = quality_wms_target.get("source", {})
    upgrade = quality_wms_target.get("upgrade", {})
    if (target.get("host") != "10.11.100.17"
            or target.get("database") != "PostgreSQL 15.18"
            or target.get("databaseName") != "ft_mes_bpi"
            or target.get("schema") != "bpi"
            or len(source.get("acceptanceCommit", "")) != 40
            or len(source.get("deployedImageCommit", "")) != 40
            or upgrade.get("strategy") != "INTEGRATED_EXPAND_ONLY"
            or upgrade.get("beforeFlywayVersion") != 22
            or upgrade.get("afterFlywayVersion") != 23
            or upgrade.get("schemaDowngradeAllowed") is not False):
        fail("BPI target V23 source, database, or expand-only evidence is incomplete", failures)
    runtime = quality_wms_target.get("runtime", {})
    if (runtime.get("service", {}).get("health") != "healthy"
            or runtime.get("adapter", {}).get("health") != "healthy"
            or runtime.get("wmsAdapter", {}).get("health") != "healthy"
            or runtime.get("material", {}).get("state") != "running"
            or not runtime.get("material", {}).get("jarSha256")
            or not runtime.get("uiIndexSha256")
            or not runtime.get("ui", {}).get("cssSha256")
            or not runtime.get("ui", {}).get("jsSha256")):
        fail("BPI target V23 runtime evidence is incomplete", failures)
    safety = quality_wms_target.get("safety", {})
    flags = safety.get("featureFlags", {})
    if (safety.get("phase2IntegrationEnabled") is not False
            or safety.get("wmsOutboxEnabled") is not False
            or safety.get("protobufHttpIngressEnabled") is not False
            or safety.get("phase2KafkaEnabled") is not False
            or safety.get("wmsAdapterEnabled") is not False
            or safety.get("allowedTenantIds") != "_DENY_ALL_"
            or safety.get("allowedPlantIds") != "_DENY_ALL_"
            or safety.get("allowedLineIds") != "_DENY_ALL_"
            or safety.get("wmsAdapterRoutes") != "_DENY_ALL_"
            or flags.get("bpi.auto-confirm") is not False
            or flags.get("bpi.qcs-link") is not False
            or flags.get("bpi.shadow-only") is not True
            or flags.get("bpi.wms-link") is not False
            or safety.get("disabledIngressProbe", {}).get("authenticatedStatus") != 403):
        fail("BPI target Phase 2 activation guards are incomplete", failures)
    previous_browser = quality_wms_target.get("previousBrowserBaseline", {})
    if (previous_browser.get("loginStatus") != 200
            or previous_browser.get("releaseRequest", {}).get("status") != 200
            or previous_browser.get("negativeChecks", {}).get("unauthenticatedStatus") != 401
            or previous_browser.get("negativeChecks", {}).get("nestedRouteStatus") != 403
            or previous_browser.get("beforeRestart") != "PASS"
            or previous_browser.get("afterServiceAdapterRestart") != "PASS"
            or any(previous_browser.get(key) != 0 for key in (
                "consoleErrors", "pageErrors", "requestFailures", "bpiHttpErrors"))):
        fail("BPI target historical batch-release browser baseline is incomplete", failures)
    browser = quality_wms_target.get("browser", {})
    empty_evidence = browser.get("emptyEvidenceBox", {})
    api_requests = browser.get("apiRequests", [])
    if (browser.get("loginUser") != "admin"
            or browser.get("state") != "INBOUNDED"
            or browser.get("revision") != 4
            or browser.get("qualityGate") != "ACCEPTED"
            or browser.get("wmsStatus") != "INBOUNDED"
            or not browser.get("documentId")
            or len(api_requests) != 5
            or any(request.get("status") != 200 for request in api_requests)
            or any(browser.get(key) != 0 for key in (
                "consoleErrors", "pageErrors", "requestFailures", "bpiHttpErrors"))
            or empty_evidence.get("horizontal") is not True
            or empty_evidence.get("width") != 639
            or empty_evidence.get("height") != 58):
        fail("BPI target controlled full-chain browser evidence is incomplete", failures)
    postgres = quality_wms_target.get("postgresAcceptance", {})
    markers = postgres.get("markers", [])
    if (postgres.get("tests") != 4
            or any(postgres.get(key) != 0 for key in ("failures", "errors", "skipped"))
            or len(markers) != 4
            or any(marker.get("residualRows") != 0 for marker in markers)
            or len(postgres.get("tablesCheckedAfterCleanup", [])) != 12
            or postgres.get("postTestResidualRows") != 0
            or postgres.get("baselineBatchUnchanged", {}).get("revision") != 2):
        fail("BPI target PostgreSQL marker or cleanup evidence is incomplete", failures)
    target_summary = quality_wms_target.get("summary", {})
    target_items = quality_wms_target.get("items", [])
    if (target_summary.get("testedFeatures") != 18
            or target_summary.get("pass") != 16
            or target_summary.get("fail") != 0
            or target_summary.get("blocked") != 2
            or len(target_items) != 18
            or sum(item.get("status") == "PASS" for item in target_items) != 16
            or sum(item.get("status") == "BLOCKED" for item in target_items) != 2):
        fail("BPI target QCS/WMS summary must preserve sixteen passes and two blockers", failures)

    latest_live = quality_wms_target.get("latestLiveAcceptance", {})
    qcs_ingress = latest_live.get("qcsIngress", {})
    kafka = latest_live.get("kafka", {})
    idempotency = latest_live.get("idempotency", {})
    cleanup = latest_live.get("cleanup", {})
    if (qcs_ingress.get("status") != 201
            or "controlled" not in str(qcs_ingress.get("source", "")).lower()
            or latest_live.get("stateTransitions") != [
                "CLOSED_RAW/r1", "WAIT_QA/r2", "RELEASED/r3", "INBOUNDED/r4"]
            or kafka.get("partitions") != 3
            or kafka.get("replicationFactor") != 3
            or kafka.get("minInSyncReplicas") != 2
            or kafka.get("consumerLagAfterAcceptance") != 0
            or idempotency.get("queryFirst") is not True
            or "unchanged" not in str(idempotency.get("identicalQcsReplay", "")).lower()
            or "unchanged" not in str(idempotency.get("forcedKafkaReplay", "")).lower()
            or cleanup.get("switchesDisabledBeforeCleanup") is not True
            or cleanup.get("materialResidual") != "0/0/0/0"
            or cleanup.get("bpiResidual") != "0/0/0/0/0"):
        fail("BPI target live Kafka/WMS/idempotency/cleanup evidence is incomplete", failures)

    limitations_text = " ".join(quality_wms_target.get("limitations", [])).lower()
    if "external qcs" not in limitations_text or "external erp/wms" not in limitations_text:
        fail("BPI target acceptance must retain external QCS and ERP/WMS boundaries", failures)

    historical_screenshot = ROOT / previous_browser.get("screenshot", "")
    if not historical_screenshot.is_file():
        fail("BPI target historical QCS/WMS screenshot is missing", failures)
    elif hashlib.sha256(historical_screenshot.read_bytes()).hexdigest() != previous_browser.get(
            "screenshotSha256"):
        fail("BPI target historical QCS/WMS screenshot hash does not match", failures)
    screenshots = browser.get("screenshots", [])
    if len(screenshots) != 2:
        fail("BPI target controlled full-chain screenshots are incomplete", failures)
    for screenshot in screenshots:
        screenshot_path = ROOT / screenshot.get("path", "")
        if not screenshot_path.is_file():
            fail(f"BPI target screenshot is missing: {screenshot.get('path', '')}", failures)
        elif hashlib.sha256(screenshot_path.read_bytes()).hexdigest() != screenshot.get("sha256"):
            fail(f"BPI target screenshot hash does not match: {screenshot.get('path', '')}", failures)

    if failures:
        print("\n".join(f"ERROR: {item}" for item in failures), file=sys.stderr)
        return 1
    print("BPI service structure, PostgreSQL ownership, and shadow-only boundaries verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
