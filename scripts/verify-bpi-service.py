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
    "services/bpi-service/wms-adapter/src/main/java/com/mapletct/ftmes/bpiwmsadapter/WmsReversalCommandProcessor.java",
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
    "services/bpi-service/app/src/main/resources/db/migration/V24__bpi_batch_force_close_workflow.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V25__bpi_wms_inbound_reversal_workflow.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V26__bpi_dataset_manifest_workbench.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V27__bpi_dataset_parquet_materialization.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V28__bpi_dataset_iceberg_catalog_publication.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V29__bpi_dataset_object_lock_recovery_archive.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V30__bpi_dataset_mlflow_registration.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V31__bpi_dataset_training_readiness.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V32__bpi_dataset_process_signal_windows.sql",
    "services/bpi-service/app/src/main/resources/db/migration/V33__bpi_function_execution_privilege_hardening.sql",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiTelemetryPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiRulePostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiRuleOutboxKafkaPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/CandidateEventMapper.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/CandidateService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/BatchCommandService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/ForceCloseTaskView.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/BpiPostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/IdempotencyRecord.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/CandidateController.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/BatchController.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/ForceCloseCommand.java",
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
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/WmsInboundReversalService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/DatasetService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/DatasetManifestBuilder.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/DatasetManifestProcessor.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/ProcessSignalWindowBuilder.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/DatasetTrainingReadinessBuilder.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/application/DatasetTrainingReadinessService.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/dataset/DatasetManifestDispatcher.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/dataset/DatasetManifestProperties.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/DatasetPostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/ProcessSignalWindowPostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/DatasetTrainingReadinessPostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/DatasetController.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/DatasetTrainingReadinessCommand.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/WmsInboundReversalTaskView.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/WmsInboundReversalPostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/WmsInboundReversalCommand.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/BatchReleasePostgresRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/integration/Phase2IntegrationKafkaRecordProcessor.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/integration/WmsInboundOutboxRepository.java",
    "services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/InternalPhase2IntegrationController.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/infrastructure/candidate/BpiCandidateKafkaConfigurationTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/application/DatasetManifestBuilderTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/application/ProcessSignalWindowBuilderTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiDatasetManifestPostgresAcceptanceTest.java",
    "services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/application/DatasetTrainingReadinessBuilderTest.java",
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
    "docs/testing/bpi-wms-inbound-reversal-acceptance.md",
    "metadata/bpi-wms-inbound-reversal-acceptance.json",
    "docs/testing/bpi-dataset-manifest-acceptance.md",
    "docs/backend-table-audit/bpi-dataset-manifest.md",
    "metadata/bpi-dataset-manifest-acceptance.json",
    "docs/testing/bpi-dataset-materialization-acceptance.md",
    "metadata/bpi-dataset-materialization-acceptance.json",
    "docs/testing/bpi-dataset-mlflow-registration-acceptance.md",
    "docs/backend-table-audit/bpi-dataset-mlflow-registration.md",
    "metadata/bpi-dataset-mlflow-registration-acceptance.json",
    "metadata/bpi-integrated-upgrade-v30-redeploy-target.json",
    "metadata/bpi-dataset-mlflow-failed-target.png",
    "metadata/bpi-dataset-mlflow-registered-target.png",
    "metadata/bpi-dataset-mlflow-registered-mobile-target.png",
    "docs/plans/2026-07-23-bpi-phase3cb-training-readiness-design.md",
    "docs/testing/bpi-dataset-training-readiness-acceptance.md",
    "docs/backend-table-audit/bpi-dataset-training-readiness.md",
    "metadata/bpi-dataset-training-readiness-acceptance.json",
    "metadata/bpi-dataset-training-readiness-assess-target.json",
    "metadata/bpi-dataset-training-readiness-restart-target.json",
    "metadata/bpi-integrated-upgrade-v31-target.json",
    "metadata/bpi-dataset-training-readiness-blocked-target.png",
    "metadata/bpi-dataset-training-readiness-blocked-mobile-target.png",
    "metadata/bpi-dataset-training-readiness-restart-target.png",
    "docs/testing/bpi-dataset-process-signal-window-acceptance.md",
    "docs/backend-table-audit/bpi-dataset-process-signal-window.md",
    "metadata/bpi-dataset-process-signal-window-acceptance.json",
    "metadata/bpi-integrated-upgrade-v33-target.json",
    "metadata/bpi-dataset-process-signal-window-desktop-target.png",
    "metadata/bpi-dataset-process-signal-window-mobile-target.png",
    "deploy/docker/scripts/adp-bpi-dataset-manifest-target-acceptance.js",
    "deploy/docker/scripts/adp-bpi-dataset-materialization-target-acceptance.js",
    "deploy/docker/scripts/adp-bpi-dataset-training-readiness-target-acceptance.js",
    "deploy/docker/scripts/bpi-dataset-manifest-target-fixture.sql",
    "deploy/docker/scripts/bpi-dataset-process-window-target-fixture.sql",
    "deploy/docker/scripts/bpi-dataset-manifest-target-verification.sql",
    "deploy/docker/scripts/bpi-dataset-materialization-target-verification.sql",
    "deploy/docker/scripts/bpi-dataset-training-readiness-target-verification.sql",
    "deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql",
    "docs/testing/bpi-formal-identity-wms-reversal-acceptance.md",
    "metadata/bpi-formal-identity-wms-reversal-acceptance.json",
    "metadata/bpi-formal-identity-wms-reversal-pending.png",
    "metadata/bpi-formal-identity-wms-reversal-approved.png",
    "deploy/docker/scripts/adp-bpi-formal-identity-wms-reversal-acceptance.js",
    "deploy/docker/scripts/adp-bpi-wms-inbound-reversal-target-acceptance.js",
    "deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-fixture.sql",
    "deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-verification.sql",
    "deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-cleanup.sql",
    "docs/testing/bpi-formal-identity-wms-roundtrip-acceptance.md",
    "metadata/bpi-formal-identity-wms-roundtrip-acceptance.json",
    "metadata/bpi-formal-identity-wms-roundtrip-pending.png",
    "metadata/bpi-formal-identity-wms-roundtrip-approved.png",
    "metadata/bpi-formal-identity-wms-roundtrip-completed.png",
    "metadata/bpi-material-wms-reversal-schema-upgrade.json",
    "metadata/bpi-material-wms-target-deployment.json",
    "deploy/docker/scripts/apply-material-wms-reversal-expand-only-target.js",
    "deploy/docker/scripts/deploy-material-wms-target.js",
    "deploy/docker/scripts/bpi-wms-formal-roundtrip-verification.sql",
    "deploy/docker/scripts/bpi-wms-formal-roundtrip-material-verification.sql",
    "deploy/docker/scripts/bpi-wms-formal-roundtrip-material-cleanup.sql",
    "metadata/bpi-quality-release-wms-target-acceptance.json",
    "metadata/bpi-quality-release-wms-target.png",
    "metadata/bpi-quality-release-wms-live-target.png",
    "metadata/bpi-quality-release-wms-live-target-bottom.png",
    "docs/testing/bpi-wms-reconciliation-acceptance.md",
    "metadata/bpi-wms-reconciliation-target-acceptance.json",
    "metadata/bpi-wms-reconciliation-target.png",
    "docs/testing/bpi-wms-outage-recovery-acceptance.md",
    "metadata/bpi-wms-outage-recovery-target-acceptance.json",
    "metadata/bpi-wms-outage-recovery-target.png",
    "docs/testing/bpi-force-close-acceptance.md",
    "metadata/bpi-force-close-target-acceptance.json",
    "metadata/bpi-force-close-pending-target.png",
    "metadata/bpi-force-close-completed-target.png",
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
    "deploy/docker/scripts/adp-bpi-wms-reconciliation-acceptance.js",
    "deploy/docker/scripts/bpi-wms-reconciliation-fixture.sql",
    "deploy/docker/scripts/bpi-wms-reconciliation-verification.sql",
    "deploy/docker/scripts/bpi-wms-reconciliation-cleanup.sql",
    "deploy/docker/scripts/adp-bpi-wms-outage-recovery-acceptance.js",
    "deploy/docker/scripts/generate-bpi-wms-outage-fixture.js",
    "deploy/docker/scripts/test-bpi-wms-outage-fixture.js",
    "deploy/docker/scripts/run-bpi-wms-outage-recovery-target.js",
    "deploy/docker/scripts/bpi-wms-outage-recovery-fixture.sql",
    "deploy/docker/scripts/bpi-wms-outage-recovery-verification.sql",
    "deploy/docker/scripts/bpi-wms-outage-recovery-cleanup.sql",
    "deploy/docker/scripts/bpi-wms-outage-recovery-material-verification.sql",
    "deploy/docker/scripts/bpi-wms-outage-recovery-material-cleanup.sql",
    "deploy/docker/scripts/adp-bpi-force-close-acceptance.js",
    "deploy/docker/scripts/bpi-force-close-acceptance-fixture.sql",
    "deploy/docker/scripts/bpi-force-close-acceptance-verification.sql",
    "deploy/docker/scripts/bpi-force-close-acceptance-cleanup.sql",
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
            "bpi.wms.completion-inbound-reversal-command.dlq.v1",
            "wms.completion-inbound-reversal.receipt.v1",
        ],
        failures,
    )
    require_text(
        SERVICE / "wms-adapter/src/main/java/com/mapletct/ftmes/bpiwmsadapter/WmsReversalCommandProcessor.java",
        [
            "findReversalByIdempotency",
            "createCompletionInboundReversal",
            "WMS_REVERSAL_IDEMPOTENCY_CONFLICT",
            "material-wms acknowledged reversal creation but exact lookup did not find the red document",
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
        SERVICE / "app/src/main/resources/db/migration/V24__bpi_batch_force_close_workflow.sql",
        [
            "bpi_batch_force_close_tasks",
            "PENDING_APPROVAL",
            "COMPLETED",
            "uq_bpi_batch_force_close_pending",
            "chk_bpi_batch_force_close_decision",
            "FOREIGN KEY (tenant_id, batch_id)",
            "GRANT SELECT, INSERT, UPDATE, DELETE ON bpi.bpi_batch_force_close_tasks TO bpi_service",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V25__bpi_wms_inbound_reversal_workflow.sql",
        [
            "bpi_wms_inbound_reversal_tasks",
            "INBOUND_REVERSING",
            "INBOUND_REVERSED",
            "uq_bpi_wms_reversal_active_batch",
            "WMS_COMPLETION_INBOUND_REVERSAL_COMMAND",
            "original_document_id",
            "reversal_document_id",
            "reject_shadow_wms_command",
            "DROP INDEX IF EXISTS bpi.uq_bpi_outbox_rule_lifecycle",
            "WHERE aggregate_type = 'RULE_VERSION'",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V26__bpi_dataset_manifest_workbench.sql",
        [
            "bpi_dataset_definitions",
            "bpi_dataset_snapshots",
            "bpi_dataset_snapshot_samples",
            "MANIFEST_READY",
            "materialization_state = 'NOT_STARTED'",
            "CHECK (artifact_uri IS NULL)",
            "feature_cutoff = prediction_time",
            "trg_bpi_dataset_definition_immutable",
            "trg_bpi_dataset_snapshot_transition",
            "trg_bpi_dataset_sample_immutable",
            "FOR EACH ROW EXECUTE FUNCTION bpi.guard_dataset_snapshot_transition()",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V32__bpi_dataset_process_signal_windows.sql",
        [
            "process_signal_windows jsonb",
            "bpi_dataset_process_signal_window_facts",
            "reject_dataset_process_signal_window_mutation",
            "bpi-training-readiness/batch-start-boundary-v2",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/resources/db/migration/V33__bpi_function_execution_privilege_hardening.sql",
        [
            "REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA bpi FROM PUBLIC",
            "ALTER DEFAULT PRIVILEGES IN SCHEMA bpi",
            "REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/DatasetManifestBuilder.java",
        [
            "ALLOWED_CONTEXT_FEATURE_REFS",
            "ALLOWED_LABEL_REFS",
            "processSignalWindows and process.window.* featureRefs must match exactly.",
            "AT_OR_BEFORE_PREDICTION_TIME",
            "LABEL_DELAY_EXCEEDED",
            "CONFIDENCE_BELOW_THRESHOLD",
            'boundary.put("deliveryState", "MANIFEST_ONLY")',
            'boundary.put("materializationState", "NOT_STARTED")',
            'boundary.put("icebergReady", false)',
            'boundary.put("mlflowRegistered", false)',
            'boundary.put("modelTrained", false)',
        ],
        failures,
    )
    require_text(
        SERVICE
        / "app/src/main/java/com/mapletct/ftmes/bpi/application/ProcessSignalWindowBuilder.java",
        [
            "WINDOW_BINDING_MISSING",
            "WINDOW_POINT_NOT_READY",
            "WINDOW_SAMPLE_COUNT_BELOW_MINIMUM",
            "WINDOW_MAX_GAP_EXCEEDED",
            "WINDOW_METRIC_UNAVAILABLE",
            'case "MEAN"',
            'case "TRUE_RATIO"',
        ],
        failures,
    )
    require_text(
        SERVICE
        / (
            "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/"
            "ProcessSignalWindowPostgresRepository.java"
        ),
        [
            "event.ingest_time <= :freezeAt",
            "ingest_time <= prediction_time",
            "available_at_prediction",
            "sample_time <= prediction_time",
            "maximum_observed_gap_seconds",
        ],
        failures,
    )
    require_text(
        SERVICE
        / (
            "app/src/test/java/com/mapletct/ftmes/bpi/application/"
            "ProcessSignalWindowBuilderTest.java"
        ),
        [
            "buildsDeterministicReadyNumericWindow",
            "failsClosedForCoverageUnitTypeAndCalibrationProblems",
            "computesBooleanTrueRatioWithoutCalibrationRequirement",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/DatasetService.java",
        [
            "A new dataset definition must use If-Match 0.",
            "Snapshot lines must be a subset of the immutable dataset definition.",
            "Every selected line requires an APPROVED shadow run",
            "freezeAt must not be in the future.",
            '"deliveryBoundary", "MANIFEST_ONLY"',
            "reserveIdempotency",
            "completeIdempotency",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/DatasetPostgresRepository.java",
        [
            "FOR UPDATE SKIP LOCKED",
            "bpi.bpi_dataset_snapshot_samples",
            "DATASET_MANIFEST_READY",
            "review.superseded_at > :freezeAt",
            "batch.plant_id = run.plant_id AND batch.line_id = run.line_id",
            "SELECT plant_id FROM bpi.bpi_dataset_definitions",
            "manifest_checksum",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/DatasetController.java",
        [
            'GetMapping("/bpi/v1/datasets")',
            'PostMapping("/bpi/v1/datasets")',
            'PostMapping("/bpi/v1/datasets/{datasetId}/snapshots")',
            'GetMapping("/bpi/v1/dataset-snapshots/{snapshotId}")',
            "hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')",
            "Idempotent-Replay",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiDatasetManifestPostgresAcceptanceTest.java",
        [
            "apiWorkerAndPostgresProveLeakageSafeReproducibleManifestOnlySnapshot",
            "ADP_E2E_BPI_DATASET_",
            'jsonPath("$.data.state").value("MANIFEST_READY")',
            'containsEntry("cutoff_safe", 3)',
            'containsEntry("leaked", 0)',
            "CONFIDENCE_BELOW_THRESHOLD",
            "LABEL_DELAY_EXCEEDED",
            "secondChecksum).isEqualTo(firstChecksum",
            "hasMessageContaining(\"immutable\")",
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
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/application/WmsInboundReversalService.java",
        [
            "Only an INBOUNDED batch can request completion-inbound reversal.",
            "WMS reversal approval must be completed by a different administrator.",
            "Original WMS command does not match the accepted batch and inbound link.",
            "WMS reversal receipt cannot precede durable command publication.",
            "BatchState.INBOUND_REVERSING",
            "BatchState.INBOUND_REVERSED",
            "outboxProperties.reversalTopic()",
            "Idempotency-Key must not exceed 128 characters.",
            "If-Match revision is outside the supported range.",
        ],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiQualityReleaseWmsPostgresAcceptanceTest.java",
        [
            "approvedFourEyeReversalPersistsOneRedCommandAndOneDurableReceipt",
            "rejectedReversalRestoresInboundStateAndAllowsANewRequest",
            "bpi_wms_inbound_reversal_tasks",
            "WMS_COMPLETION_INBOUND_REVERSAL_COMMAND",
            "WMS-REVERSAL-SECOND-APPROVE-",
            "INBOUND_REVERSING|9|ACCEPTED|REVERSAL_PENDING",
        ],
        failures,
    )
    require_text(
        ROOT / "backend/source-modules/batch-intelligence-adapter/src/main/java/com/mapletct/ftmes/bpiadapter/BpiRoutePolicy.java",
        ["wms/reversal", "wms/(?:reconcile|reversal)",
         "datasets|dataset-snapshots/", "datasets(?:/"],
        failures,
    )
    require_text(
        ROOT / "backend/source-modules/batch-intelligence-adapter/src/test/java/com/mapletct/ftmes/bpiadapter/BpiProxyControllerTest.java",
        ["forwardsWmsReversalReadAndCommandWithInternalIdentityAndConcurrencyHeaders",
         "forwardsDatasetDefinitionSnapshotAndManifestReadsThroughExactRoutes",
         "wms-reversal-request-1", "dataset-snapshot-command-1", "Idempotent-Replay"],
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
         "BATCH_SUSPENDED", "BATCH_RESUMED", "insertBatchAudit", "requestForceClose",
         "approveForceClose", "BATCH_FORCE_CLOSE_REQUESTED", "BATCH_FORCE_CLOSED",
         "different administrator", "hasPendingForceClose"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/BpiPostgresRepository.java",
        ["bpi_batch_force_close_tasks", "lockPendingForceClose", "approveForceCloseTask",
         "forceCloseBatch", "NOT EXISTS", "PENDING_APPROVAL"],
        failures,
    )
    require_text(
        SERVICE / "app/src/main/java/com/mapletct/ftmes/bpi/interfaces/rest/BatchController.java",
        ["/bpi/v1/batches/{batchId}/suspend", "/bpi/v1/batches/{batchId}/resume",
         "/bpi/v1/batches/{batchId}/force-close", "latestForceCloseTask",
         "ResponseEntity.accepted"],
        failures,
    )
    require_text(
        SERVICE / "app/src/test/java/com/mapletct/ftmes/bpi/BpiPostgresAcceptanceTest.java",
        ["forceCloseRequiresIndependentApprovalAndPersistsRecoverableTaskAndAuditTrail",
         "force-close-self-approve-", "force-close-changed-boundary-",
         "BATCH_FORCE_CLOSE_REQUESTED|ACTIVE|ACTIVE",
         "BATCH_FORCE_CLOSED|ACTIVE|CLOSED_RAW", "bpi_batch_force_close_tasks"],
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
            "qcs-quality-gate-outbox:",
            "QCS_BPI_OUTBOX_ENABLED: ${QCS_BPI_OUTBOX_ENABLED:-false}",
            "QCS_BPI_OUTBOX_BPI_BASE_URL: http://bpi-service:19091",
            "BPI_WMS_OUTBOX_ENABLED",
            "BPI_WMS_OUTBOX_TOPIC",
            "bpi-mlflow-artifact-bootstrap:",
            "bpi-mlflow-postgres:",
            "bpi-mlflow:",
            "bpi-dataset-mlflow-registrar:",
            "BPI_MLFLOW_ARTIFACT_BOOTSTRAP_ENABLED: ${BPI_MLFLOW_ARTIFACT_BOOTSTRAP_ENABLED:-false}",
            "BPI_DATASET_MLFLOW_REGISTRAR_ENABLED: ${BPI_DATASET_MLFLOW_REGISTRAR_ENABLED:-false}",
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
            "stage_runtime_deployment_manifests",
            "validate_release_deployment_manifests",
            "BPI_INTEGRATED_ALLOW_ALREADY_MIGRATED",
            "REDEPLOY_APPLICATIONS_ON_EXISTING_BPI_SCHEMA",
            "migrationMode",
            "docker-compose-before-v",
            "bpi-minio-runtime-before-v",
            "--profile bpi-ml",
            "ensure-bpi-mlflow-registrar-role.sh",
            "BPI_MLFLOW_ARTIFACT_BOOTSTRAP_ENABLED=false",
            "BPI_DATASET_MLFLOW_REGISTRAR_ENABLED=false",
            "bpi.bpi_dataset_mlflow_registrations",
            "10004:10004",
            "10005:10005",
        ],
        failures,
    )
    require_text(
        ROOT / "services/bpi-mlflow/Dockerfile",
        [
            "PIP_INDEX_URL",
            "USER 10004:10004",
        ],
        failures,
    )
    require_text(
        ROOT / "services/bpi-mlflow/requirements.runtime.txt",
        ["mlflow==3.14.0", "psycopg2-binary", "boto3"],
        failures,
    )
    require_text(
        ROOT / "services/bpi-dataset-mlflow-registrar/Dockerfile",
        ["USER 10005:10005"],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/adp-bpi-dataset-mlflow-target-acceptance.js",
        [
            "request-failure",
            "retry-registered",
            "MLFLOW_TRANSPORT_ERROR",
            'data-mlflow-state="REGISTERED"',
            "productionActivationAllowed === false",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql",
        [
            "target_dataset_mlflow_registrations",
            "DELETE FROM bpi.bpi_dataset_mlflow_registrations",
            "mlflowRegistrations",
        ],
        failures,
    )
    materializer_dockerfile = ROOT / "services/bpi-dataset-materializer/Dockerfile"
    require_text(
        materializer_dockerfile,
        [
            "pip install --requirement requirements.runtime.txt",
            "pip install --no-build-isolation --no-deps .",
            "USER 10001:10001",
        ],
        failures,
    )
    if "--mount=" in materializer_dockerfile.read_text(encoding="utf-8"):
        fail("BPI dataset materializer Dockerfile must not require BuildKit-only mounts", failures)
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

    reversal_acceptance = json.loads(
        (ROOT / "metadata/bpi-wms-inbound-reversal-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if reversal_acceptance.get("status") != (
            "PASS_LOCAL_BROWSER_API_POSTGRES_PROTOCOL_ONLY"):
        fail("BPI WMS reversal must retain its local protocol-only boundary", failures)
    if (reversal_acceptance.get("database") != "PostgreSQL 16.13"
            or reversal_acceptance.get("flywayVersion") != 25
            or reversal_acceptance.get("phase1Mode") != "SHADOW_ONLY"
            or reversal_acceptance.get("phase2Mode") != "DISABLED_BY_DEFAULT"
            or reversal_acceptance.get("externalSystemParticipated") is not False
            or reversal_acceptance.get("productionActivationAllowed") is not False):
        fail("BPI WMS reversal runtime or activation boundary is incomplete", failures)
    reversal_summary = reversal_acceptance.get("summary", {})
    expected_reversal_summary = {
        "testedFeatures": 11,
        "pass": 9,
        "fail": 0,
        "blocked": 2,
        "browserTests": 19,
        "browserFailures": 0,
        "simulatorTests": 14,
        "simulatorFailures": 0,
        "postgresAcceptanceTests": 10,
        "postgresAcceptanceFailures": 0,
        "focusedPostgresReversalTests": 2,
        "java8AdapterTests": 32,
        "java8AdapterFailures": 0,
        "unexpectedConsoleErrors": 0,
        "pageErrors": 0,
        "requestFailures": 0,
        "horizontalOverflowFailures": 0,
    }
    if any(reversal_summary.get(key) != value
           for key, value in expected_reversal_summary.items()):
        fail("BPI WMS reversal automated evidence summary is incomplete", failures)
    reversal_items = reversal_acceptance.get("items", [])
    if (len(reversal_items) != 11
            or sum(item.get("status") == "PASS" for item in reversal_items) != 9
            or sum(item.get("status") == "BLOCKED" for item in reversal_items) != 2):
        fail("BPI WMS reversal item statuses do not match the summary", failures)
    reversal_browser = reversal_acceptance.get("browser", {})
    if (reversal_browser.get("operationId") != "commandWmsInboundReversal"
            or reversal_browser.get("requestApprovalModes") != ["REQUEST", "APPROVE"]
            or reversal_browser.get("ifMatchRevisions") != ["7", "8"]
            or reversal_browser.get("originalDocumentId")
                != "WMS-IN-ADP-E2E-0001"
            or reversal_browser.get("reversalDocumentId")
                != "WMS-RED-ADP-E2E-0001"
            or reversal_browser.get("finalBatchState") != "INBOUND_REVERSED"
            or reversal_browser.get("finalWmsStatus") != "REVERSED"):
        fail("BPI WMS reversal browser evidence is incomplete", failures)
    reversal_postgres = reversal_acceptance.get("postgres", {})
    if (reversal_postgres.get("acceptedProjection")
            != "INBOUND_REVERSED|7|ACCEPTED|REVERSED"
            or reversal_postgres.get("rejectedProjectionBeforeRetry")
                != "INBOUNDED|7|ACCEPTED|REVERSAL_FAILED"
            or reversal_postgres.get("retryApprovedProjection")
                != "INBOUND_REVERSING|9|ACCEPTED|REVERSAL_PENDING"
            or reversal_postgres.get("retryReversalCommandCount") != 2
            or reversal_postgres.get("acceptedCounts") != {
                "reversalTasks": 1,
                "inboxEvents": 3,
                "stateEvents": 6,
                "auditEvents": 6,
            }):
        fail("BPI WMS reversal PostgreSQL evidence is incomplete", failures)
    if any(reversal_acceptance.get("defaults", {}).values()):
        fail("BPI WMS reversal activation defaults must remain false", failures)
    if any(reversal_acceptance.get("cleanup", {}).values()):
        fail("BPI WMS reversal cleanup must leave zero fixture rows", failures)

    dataset_acceptance = json.loads(
        (ROOT / "metadata/bpi-dataset-manifest-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if (dataset_acceptance.get("status")
            != "PASS_TARGET_BROWSER_API_POSTGRES_MANIFEST_ONLY_CLEANED"
            or dataset_acceptance.get("database") != "PostgreSQL 15.18"
            or dataset_acceptance.get("flywayVersion") != 26
            or dataset_acceptance.get("phase") != "3A_MANIFEST_ONLY"
            or dataset_acceptance.get("productionActivationAllowed") is not False
            or len(dataset_acceptance.get("repoBaseCommit", "")) != 40):
        fail("BPI dataset manifest runtime or phase boundary is incomplete", failures)
    dataset_summary = dataset_acceptance.get("summary", {})
    expected_dataset_summary = {
        "testedFeatures": 10,
        "pass": 10,
        "fail": 0,
        "blocked": 0,
        "browserTests": 20,
        "browserFailures": 0,
        "simulatorTests": 15,
        "simulatorFailures": 0,
        "postgresAcceptanceTests": 1,
        "postgresAcceptanceFailures": 0,
        "unexpectedConsoleErrors": 0,
        "pageErrors": 0,
        "requestFailures": 0,
        "horizontalOverflowFailures": 0,
        "targetBrowserRuns": 1,
        "targetBrowserFailures": 0,
        "targetPostgresAcceptanceRuns": 1,
        "targetPostgresAcceptanceFailures": 0,
    }
    if any(dataset_summary.get(key) != value
           for key, value in expected_dataset_summary.items()):
        fail("BPI dataset manifest automated evidence summary is incomplete", failures)
    dataset_items = dataset_acceptance.get("items", [])
    if (len(dataset_items) != 10
            or sum(item.get("status") == "PASS" for item in dataset_items) != 10
            or sum(item.get("status") == "BLOCKED" for item in dataset_items) != 0):
        fail("BPI dataset manifest item statuses do not match the summary", failures)
    dataset_postgres = dataset_acceptance.get("postgres", {})
    if ("MANIFEST_READY" not in dataset_postgres.get("manifestProjection", "")
            or "leaked=0" not in dataset_postgres.get("manifestProjection", "")
            or "cross_plant_rows=0" not in dataset_postgres.get("manifestProjection", "")
            or dataset_postgres.get("exclusionCounts") != {
                "CONFIDENCE_BELOW_THRESHOLD": 1,
                "LABEL_DELAY_EXCEEDED": 1,
                "START_BOUNDARY_OUTSIDE_TOLERANCE": 1,
            }
            or dataset_postgres.get("completionAuditRows") != 3
            or dataset_postgres.get("idempotency") != {
                "rows": 2,
                "completed": 2,
                "responseStatuses": [200, 202],
            }):
        fail("BPI dataset manifest PostgreSQL evidence is incomplete", failures)
    if any(dataset_acceptance.get("cleanup", {}).values()):
        fail("BPI dataset manifest cleanup must leave zero fixture rows", failures)
    dataset_target = dataset_acceptance.get("target", {})
    dataset_target_run = dataset_acceptance.get("targetRun", {})
    dataset_phase_boundary = dataset_target_run.get("phaseBoundary", {})
    if (dataset_target.get("sshHost") != "10.11.100.17"
            or dataset_target.get("adpBaseUrl") != "http://10.11.100.17:18080"
            or dataset_target.get("composeProject") != "adp-mes-newbase"
            or dataset_target.get("deployedRevision")
                != "e116580aa110fd9c6895a4bbd208b533d89e2dee"
            or dataset_target.get("flywayVersion") != 26
            or dataset_target.get("postgresVersion") != "15.18"
            or any(dataset_target.get("writeSwitches", {}).values())
            or not dataset_target_run.get("marker", "").startswith(
                "ADP_E2E_BPI_DATASET_TARGET_")
            or dataset_target_run.get("definition") != "ACTIVE/r1"
            or dataset_target_run.get("snapshot") != "MANIFEST_READY/r3"
            or len(dataset_target_run.get("manifestChecksum", "")) != 64
            or dataset_phase_boundary != {
                "deliveryState": "MANIFEST_ONLY",
                "materializationState": "NOT_STARTED",
                "artifactUri": None,
                "icebergReady": False,
                "mlflowRegistered": False,
                "modelTrained": False,
            }):
        fail("BPI dataset manifest target evidence is incomplete", failures)
    dataset_browser = dataset_acceptance.get("browser", {})
    if (dataset_browser.get("combinedStatus")
            != "PASS_TARGET_BROWSER_API_POSTGRES_MANIFEST_ONLY_CLEANED"
            or dataset_browser.get("consoleErrors") != 0
            or dataset_browser.get("pageErrors") != 0
            or dataset_browser.get("requestFailures") != 0
            or dataset_browser.get("desktopSampleRows") != 3
            or dataset_browser.get("mobileSampleRows") != 3
            or dataset_browser.get("mobileWidths") != "390/390/390"):
        fail("BPI dataset manifest target browser evidence is incomplete", failures)
    dataset_limitations = " ".join(dataset_acceptance.get("limitations", [])).lower()
    for boundary in ("production-volume", "iceberg", "mlflow", "model training",
                     "physical-device"):
        if boundary not in dataset_limitations:
            fail(f"BPI dataset manifest limitation is missing: {boundary}", failures)

    materialization_acceptance = json.loads(
        (ROOT / "metadata/bpi-dataset-materialization-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if (materialization_acceptance.get("status")
            != "PASS_TARGET_BROWSER_API_POSTGRES_MINIO_FAILURE_RETRY_RESTART_CLEANED"
            or materialization_acceptance.get("database") != "PostgreSQL 15.18"
            or materialization_acceptance.get("flywayVersion") != 27
            or materialization_acceptance.get("phase")
                != "3B_A_VERSION_PINNED_PARQUET"
            or materialization_acceptance.get("productionActivationAllowed") is not False
            or len(materialization_acceptance.get("repoBaseCommit", "")) != 40
            or len(materialization_acceptance.get("runtimeRevision", "")) != 40):
        fail("BPI dataset materialization target identity is incomplete", failures)
    materialization_summary = materialization_acceptance.get("summary", {})
    expected_materialization_summary = {
        "testedFeatures": 10,
        "pass": 10,
        "fail": 0,
        "blocked": 0,
        "notApplicable": 0,
        "targetBrowserRuns": 4,
        "targetBrowserFailures": 0,
        "unexpectedConsoleErrors": 0,
        "pageErrors": 0,
        "requestFailures": 0,
        "horizontalOverflowFailures": 0,
        "postgresAcceptanceRuns": 1,
        "minioExactVersionAcceptanceRuns": 1,
        "serviceRestartReadRuns": 1,
        "cleanupRuns": 1,
    }
    if any(materialization_summary.get(key) != value
           for key, value in expected_materialization_summary.items()):
        fail("BPI dataset materialization summary is incomplete", failures)
    materialization_items = materialization_acceptance.get("items", [])
    if (len(materialization_items) != 10
            or sum(item.get("status") == "PASS"
                   for item in materialization_items) != 10):
        fail("BPI dataset materialization item statuses are incomplete", failures)
    materialization_run = materialization_acceptance.get("targetRun", {})
    if (not materialization_run.get("marker", "").startswith(
                "ADP_E2E_BPI_PARQUET_")
            or materialization_run.get("stateSequence") != [
                "QUEUED", "WRITING", "FAILED", "QUEUED", "WRITING", "READY"]
            or materialization_run.get("finalRevision") != 6
            or materialization_run.get("attemptCount") != 2
            or materialization_run.get("failureCode") != "MATERIALIZATION_ERROR"
            or materialization_run.get("byteSize") != 11341
            or materialization_run.get("rowCount") != 1
            or materialization_run.get("schemaFieldCount") != 26
            or materialization_run.get("auditCounts") != {
                "DATASET_MATERIALIZATION_QUEUED": 1,
                "DATASET_MATERIALIZATION_WRITING": 2,
                "DATASET_MATERIALIZATION_FAILED": 1,
                "DATASET_MATERIALIZATION_RETRIED": 1,
                "DATASET_MATERIALIZATION_READY": 1,
            }
            or materialization_run.get("idempotency") != {
                "rows": 2,
                "completed": 2,
                "responseStatuses": [202, 202],
            }):
        fail("BPI dataset materialization PostgreSQL evidence is incomplete", failures)
    materialization_minio = materialization_acceptance.get("minio", {})
    if (materialization_minio.get("privacy") != "private"
            or materialization_minio.get("versioning") != "enabled"
            or materialization_minio.get("objectContentVerified") is not True
            or materialization_minio.get("workerDeleteResult") != "AccessDenied"
            or materialization_minio.get("administratorCleanup") != {
                "versionsBefore": 1,
                "versionsAfter": 0,
                "exactVersionReadableAfterDelete": False,
            }
            or materialization_minio.get("finalBucketObjectVersionCount") != 0
            or materialization_minio.get("wormOrObjectLockClaimed") is not False):
        fail("BPI dataset materialization MinIO evidence is incomplete", failures)
    materialization_cleanup = materialization_acceptance.get("cleanup", {})
    expected_zero_cleanup = {
        key: value for key, value in materialization_cleanup.items()
        if key not in {"capturedIdempotencyRowsDeleted"}
    }
    if (any(expected_zero_cleanup.values())
            or materialization_cleanup.get("capturedIdempotencyRowsDeleted") != 4):
        fail("BPI dataset materialization cleanup is incomplete", failures)
    materialization_target = materialization_acceptance.get("target", {})
    if (materialization_target.get("sshHost") != "10.11.100.17"
            or materialization_target.get("adpBaseUrl")
                != "http://10.11.100.17:18080"
            or materialization_target.get("composeProject") != "adp-mes-newbase"
            or materialization_target.get("flywayVersion") != 27
            or materialization_target.get("health", {}).get(
                "materializerContainerCount") != 0
            or any(materialization_target.get("defaultOff", {}).values())):
        fail("BPI dataset materialization target was not restored default-off", failures)
    materialization_boundaries = materialization_acceptance.get("boundaries", {})
    if (materialization_boundaries.get("manifestDelivery") != "MANIFEST_ONLY"
            or any(value for key, value in materialization_boundaries.items()
                   if key != "manifestDelivery")):
        fail("BPI dataset materialization overclaims a downstream boundary", failures)

    mlflow_acceptance = json.loads(
        (ROOT / "metadata/bpi-dataset-mlflow-registration-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if (mlflow_acceptance.get("status")
            != "PASS_TARGET_BROWSER_API_POSTGRES_MLFLOW_MINIO_RESTART_CLEANED"
            or mlflow_acceptance.get("database") != "PostgreSQL 15.18"
            or mlflow_acceptance.get("flywayVersion") != 30
            or mlflow_acceptance.get("phase")
                != "3C_A_MLFLOW_DATASET_REGISTRATION"
            or mlflow_acceptance.get("productionActivationAllowed") is not False
            or mlflow_acceptance.get("repoBaseCommit")
                != "27ab9a5260c197b30747f405741dcdb6105187b1"
            or mlflow_acceptance.get("runtimeRevision")
                != "27ab9a5260c197b30747f405741dcdb6105187b1"):
        fail("BPI MLflow Dataset Input target identity is incomplete", failures)
    mlflow_summary = mlflow_acceptance.get("summary", {})
    expected_mlflow_summary = {
        "testedFeatures": 12,
        "pass": 12,
        "fail": 0,
        "blocked": 0,
        "notApplicable": 0,
        "targetBrowserRuns": 4,
        "targetBrowserFailures": 0,
        "unexpectedConsoleErrors": 0,
        "pageErrors": 0,
        "requestFailures": 0,
        "horizontalOverflowFailures": 0,
        "postgresAcceptanceRuns": 1,
        "mlflowAcceptanceRuns": 1,
        "minioPolicyAcceptanceRuns": 1,
        "restartAcceptanceRuns": 1,
        "cleanupRuns": 1,
    }
    if mlflow_summary != expected_mlflow_summary:
        fail("BPI MLflow Dataset Input summary is incomplete", failures)
    mlflow_items = mlflow_acceptance.get("items", [])
    if (len(mlflow_items) != 12
            or sum(item.get("status") == "PASS" for item in mlflow_items) != 12):
        fail("BPI MLflow Dataset Input item statuses are incomplete", failures)
    mlflow_run = mlflow_acceptance.get("targetRun", {})
    if (mlflow_run.get("marker")
            != "ADP_E2E_BPI_MLFLOW_20260723_022000_A1"
            or mlflow_run.get("registrationId")
                != "df8653ea-1aac-43c6-bba5-cd7f8c6a5ead"
            or mlflow_run.get("runId")
                != "c84549b0748d413291c9018096da9a80"
            or mlflow_run.get("stateSequence") != [
                "QUEUED", "REGISTERING", "FAILED", "QUEUED",
                "REGISTERING", "REGISTERED"]
            or mlflow_run.get("finalRevision") != 6
            or mlflow_run.get("attemptCount") != 2
            or mlflow_run.get("failureCode") != "MLFLOW_TRANSPORT_ERROR"
            or len(mlflow_run.get("semanticChecksum", "")) != 64
            or len(mlflow_run.get("datasetDigest", "")) != 16):
        fail("BPI MLflow Dataset Input target run is incomplete", failures)
    mlflow_postgres = mlflow_acceptance.get("postgres", {})
    mlflow_metadata = mlflow_postgres.get("registrationMetadata", {})
    if (mlflow_postgres.get("idempotency") != {
                "rows": 2,
                "completed": 2,
                "responseStatuses": [202, 202],
            }
            or mlflow_metadata != {
                "sourceFactsVerified": True,
                "datasetInputVerified": True,
                "lineageVerified": True,
                "modelTrained": False,
                "modelRegistered": False,
                "onlineInferenceEnabled": False,
                "productionActivationAllowed": False,
            }):
        fail("BPI MLflow Dataset Input PostgreSQL evidence is incomplete", failures)
    mlflow_external = mlflow_acceptance.get("mlflow", {})
    if (mlflow_external.get("version") != "3.14.0"
            or mlflow_external.get("runStatus") != "FINISHED"
            or mlflow_external.get("runCount") != 1
            or mlflow_external.get("datasetCount") != 1
            or mlflow_external.get("inputCount") != 1
            or mlflow_external.get("inputContext") != "training_candidate"
            or "?versionId=" not in mlflow_external.get("source", "")
            or mlflow_external.get("registeredModelCount") != 0
            or mlflow_external.get("modelVersionCount") != 0
            or mlflow_external.get("loggedModelCount") != 0
            or mlflow_external.get("restartRunCount") != 1):
        fail("BPI MLflow external readback evidence is incomplete", failures)
    if mlflow_acceptance.get("minio") != {
            "artifactBucketPrivate": True,
            "scopedListOwnBucket": True,
            "scopedAdmin": False,
            "scopedRecoveryList": False,
            "scopedRecoveryDelete": False,
        }:
        fail("BPI MLflow MinIO least-privilege evidence is incomplete", failures)
    mlflow_browser = mlflow_acceptance.get("browser", {})
    if (mlflow_browser.get("combinedStatus")
            != "PASS_TARGET_BROWSER_API_POSTGRES_MLFLOW_MINIO_RESTART_CLEANED"
            or any(mlflow_browser.get(key) for key in (
                "consoleErrors", "pageErrors", "requestFailures"))
            or mlflow_browser.get("mobileWidths") != "390/390/390"
            or mlflow_browser.get("mobileDrawerWidths") != "389/389"):
        fail("BPI MLflow target browser evidence is incomplete", failures)
    for screenshot in mlflow_browser.get("evidence", {}).values():
        screenshot_path = ROOT / screenshot.get("path", "")
        if not screenshot_path.is_file():
            fail(f"BPI MLflow screenshot is missing: {screenshot.get('path', '')}", failures)
        elif hashlib.sha256(screenshot_path.read_bytes()).hexdigest() != screenshot.get(
                "sha256"):
            fail(f"BPI MLflow screenshot hash does not match: {screenshot.get('path', '')}", failures)
    mlflow_cleanup = mlflow_acceptance.get("cleanup", {})
    if (mlflow_cleanup.get("defaultOffFlagsVerified") is not True
            or any(value for key, value in mlflow_cleanup.items()
                   if key != "defaultOffFlagsVerified")):
        fail("BPI MLflow target cleanup is incomplete", failures)
    mlflow_target = mlflow_acceptance.get("target", {})
    if (mlflow_target.get("sshHost") != "10.11.100.17"
            or mlflow_target.get("composeProject") != "adp-mes-newbase"
            or mlflow_target.get("flywayVersion") != 30
            or mlflow_target.get("health", {}).get("optionalRunningSidecars") != 0
            or any(mlflow_target.get("defaultOff", {}).values())):
        fail("BPI MLflow target runtime was not restored default-off", failures)
    mlflow_boundaries = mlflow_acceptance.get("boundaries", {})
    if (mlflow_boundaries.get("mlflowDatasetInputRegistered") is not True
            or mlflow_boundaries.get("sourceFactsVerified") is not True
            or mlflow_boundaries.get("datasetLineageVerified") is not True
            or any(value for key, value in mlflow_boundaries.items()
                   if key not in {
                       "mlflowDatasetInputRegistered",
                       "sourceFactsVerified",
                       "datasetLineageVerified",
                   })):
        fail("BPI MLflow acceptance overclaims a model or production boundary", failures)
    mlflow_upgrade = json.loads(
        (ROOT / "metadata/bpi-integrated-upgrade-v30-redeploy-target.json").read_text(
            encoding="utf-8"
        )
    )
    if (mlflow_upgrade.get("status") != "PASS"
            or mlflow_upgrade.get("phase") != "COMPLETE"
            or mlflow_upgrade.get("releaseCommit")
                != "27ab9a5260c197b30747f405741dcdb6105187b1"
            or mlflow_upgrade.get("database", {}).get("afterFlywayVersion") != 30
            or mlflow_upgrade.get("database", {}).get("migrationMode")
                != "VALIDATE_EXISTING_SCHEMA"):
        fail("BPI V30 controlled redeploy evidence is incomplete", failures)

    readiness_acceptance = json.loads(
        (ROOT / "metadata/bpi-dataset-training-readiness-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    readiness_commit = "89044926c1335f8028c624b99fd7ecb57d771f2b"
    if (readiness_acceptance.get("schemaVersion") != 1
            or readiness_acceptance.get("status")
                != "PASS_TARGET_BROWSER_API_POSTGRES_RESTART_NO_MODEL_SIDE_EFFECT_CLEANED"
            or readiness_acceptance.get("database") != "PostgreSQL 15.18"
            or readiness_acceptance.get("databaseName") != "ft_mes_bpi"
            or readiness_acceptance.get("flywayVersion") != 31
            or readiness_acceptance.get("phase")
                != "3C_B_OFFLINE_TRAINING_READINESS_ASSESSMENT"
            or readiness_acceptance.get("repoBaseCommit") != readiness_commit
            or readiness_acceptance.get("runtimeRevision") != readiness_commit
            or readiness_acceptance.get("productionActivationAllowed") is not False):
        fail("BPI training-readiness target identity is incomplete", failures)
    expected_readiness_summary = {
        "testedFeatures": 12,
        "pass": 12,
        "fail": 0,
        "blocked": 0,
        "notApplicable": 0,
        "targetBrowserRuns": 2,
        "targetBrowserFailures": 0,
        "unexpectedConsoleErrors": 0,
        "pageErrors": 0,
        "requestFailures": 0,
        "horizontalOverflowFailures": 0,
        "postgresAcceptanceRuns": 1,
        "mlflowNoSideEffectRuns": 1,
        "restartAcceptanceRuns": 1,
        "cleanupRuns": 1,
    }
    if readiness_acceptance.get("summary") != expected_readiness_summary:
        fail("BPI training-readiness summary is incomplete", failures)
    readiness_items = readiness_acceptance.get("items", [])
    if (len(readiness_items) != 12
            or sum(item.get("status") == "PASS" for item in readiness_items) != 12):
        fail("BPI training-readiness item statuses are incomplete", failures)

    readiness_run = readiness_acceptance.get("targetRun", {})
    expected_readiness_blockers = [
        "PROCESS_SIGNAL_WINDOWS_MISSING",
        "INCLUDED_SAMPLE_COUNT_BELOW_MINIMUM",
        "DISTINCT_BATCH_COUNT_BELOW_MINIMUM",
        "PRODUCTION_DAY_COVERAGE_BELOW_MINIMUM",
        "PRODUCTION_SPLIT_GROUPS_BELOW_MINIMUM",
        "EXCLUDED_RATIO_ABOVE_MAXIMUM",
        "START_ACCEPTED_LABEL_COUNT_BELOW_MINIMUM",
        "START_REJECTED_LABEL_COUNT_BELOW_MINIMUM",
    ]
    expected_readiness_observed = {
        "includedSampleCount": 1,
        "distinctBatchCount": 1,
        "distinctProductionDayCount": 1,
        "productionSplitGroupCount": 1,
        "excludedRatio": 0.666667,
        "startAcceptedLabelCount": 1,
        "startRejectedLabelCount": 0,
        "signalWindowFeatureCount": 0,
    }
    expected_readiness_required = {
        "minimumIncludedSamples": 200,
        "minimumDistinctBatches": 200,
        "minimumProductionDays": 7,
        "minimumProductionSplitGroups": 2,
        "maximumExcludedRatio": 0.2,
        "minimumStartAcceptedLabels": 100,
        "minimumStartRejectedLabels": 10,
        "minimumSignalWindowFeatureRefs": 2,
    }
    expected_readiness_phase_boundary = {
        "assessmentOnly": True,
        "trainingStarted": False,
        "modelCreated": False,
        "modelRegistered": False,
        "onlineInferenceEnabled": False,
        "productionActivationAllowed": False,
    }
    if (readiness_run.get("marker")
            != "ADP_E2E_BPI_READINESS_20260723_091500_A1"
            or readiness_run.get("tenantId") != "1000"
            or readiness_run.get("plantId") != "PLANT-01"
            or readiness_run.get("lineId") != "LINE-S07-01"
            or readiness_run.get("datasetId")
                != "9f1d384e-ff7b-4183-a8f1-5a6ed3c901dd"
            or readiness_run.get("snapshotId")
                != "39885522-9531-4177-a671-f330742ff2ce"
            or readiness_run.get("materializationId")
                != "5142b48a-14a2-4b59-b71a-3e87fef623df"
            or readiness_run.get("catalogPublicationId")
                != "05b377d4-f35a-4310-a8e3-3a27dc6c5f19"
            or readiness_run.get("retentionArchiveId")
                != "3b4adef5-8323-4378-ac49-86a507228c5b"
            or readiness_run.get("registrationId")
                != "21edf8aa-b354-41f7-8703-0c42fc2984f1"
            or readiness_run.get("mlflowRunId")
                != "11bc3662fe474c07b2e2285b64754bc1"
            or readiness_run.get("assessmentIds") != [
                "3b4bb80a-5a96-4fcb-81a3-0230340fb311",
                "f961a15f-ee22-42c6-9702-7136d9cab007",
            ]
            or readiness_run.get("objectiveCode")
                != "BATCH_START_BOUNDARY_REVIEW_RISK"
            or readiness_run.get("policyVersion")
                != "bpi-training-readiness/batch-start-boundary-v1"
            or readiness_run.get("stateSequence") != ["BLOCKED", "BLOCKED"]
            or readiness_run.get("assessmentSequences") != [1, 2]
            or readiness_run.get("assessmentChecksum")
                != "91d8726c265720758ed796a7dac297f6fd15b61329cef8464e2afe80f5d0ef98"
            or readiness_run.get("gateCount") != 19
            or readiness_run.get("blockerCount") != 8
            or readiness_run.get("blockerCodes") != expected_readiness_blockers
            or readiness_run.get("observed") != expected_readiness_observed
            or readiness_run.get("required") != expected_readiness_required
            or readiness_run.get("phaseBoundary")
                != expected_readiness_phase_boundary):
        fail("BPI training-readiness immutable gate evidence is incomplete", failures)

    if readiness_acceptance.get("postgres") != {
            "table": "bpi.bpi_dataset_training_readiness_assessments",
            "projection": (
                "BLOCKED/BLOCKED|sequence=1,2|revision=1,1|gates=19,19|"
                "blockers=8,8|included=1,1|signalWindows=0,0"
            ),
            "assessmentRows": 2,
            "auditRows": 2,
            "idempotency": {
                "rows": 2,
                "completed": 2,
                "responseStatuses": [200, 200],
                "sameKeyReplayAddedRows": 0,
            },
            "immutableUpdateRejected": True,
        }:
        fail("BPI training-readiness PostgreSQL evidence is incomplete", failures)
    if readiness_acceptance.get("mlflow") != {
            "runCountBefore": 1,
            "runCountAfter": 1,
            "datasetCountBefore": 1,
            "datasetCountAfter": 1,
            "inputCountBefore": 1,
            "inputCountAfter": 1,
            "registeredModelCountBefore": 0,
            "registeredModelCountAfter": 0,
            "modelVersionCountBefore": 0,
            "modelVersionCountAfter": 0,
            "loggedModelCountBefore": 0,
            "loggedModelCountAfter": 0,
            "assessmentCreatedExternalRun": False,
            "assessmentCreatedModel": False,
        }:
        fail("BPI training-readiness MLflow no-side-effect evidence is incomplete", failures)

    readiness_browser = readiness_acceptance.get("browser", {})
    if (readiness_browser.get("route")
            != "http://10.11.100.17:18080/bpi/#/datasets"
            or readiness_browser.get("assessmentRunnerStatus")
                != "PASS_PENDING_DATABASE_MLFLOW_RESTART_VERIFICATION_AND_CLEANUP"
            or readiness_browser.get("restartRunnerStatus")
                != "PASS_BLOCKED_STATE_REDISCOVERED_AFTER_RESTART"
            or readiness_browser.get("combinedStatus")
                != "PASS_TARGET_BROWSER_API_POSTGRES_RESTART_NO_MODEL_SIDE_EFFECT_CLEANED"
            or any(readiness_browser.get(key) for key in (
                "consoleErrors", "pageErrors", "requestFailures"))
            or readiness_browser.get("mobileWidths") != "390/390/390"
            or readiness_browser.get("mobileDrawerWidths") != "389/389"):
        fail("BPI training-readiness browser evidence is incomplete", failures)
    expected_readiness_screenshots = {
        "blockedScreenshot": {
            "path": "metadata/bpi-dataset-training-readiness-blocked-target.png",
            "sha256": "4bb2c8a14e19baf1cb082f03535818f9c1cce9ff03e6a7f42f6f805d2d428093",
        },
        "blockedMobileScreenshot": {
            "path": "metadata/bpi-dataset-training-readiness-blocked-mobile-target.png",
            "sha256": "730a69536b3364b980079674662c1ebca61896ab60b58777ff51fe5665ebc0a4",
        },
        "restartScreenshot": {
            "path": "metadata/bpi-dataset-training-readiness-restart-target.png",
            "sha256": "77c52e075185a9a726f9dc9d21b7e11c9772600ae9c4f85b851b3c6a17d3a683",
        },
    }
    if readiness_browser.get("evidence") != expected_readiness_screenshots:
        fail("BPI training-readiness screenshot manifest is incomplete", failures)
    for screenshot in expected_readiness_screenshots.values():
        screenshot_path = ROOT / screenshot.get("path", "")
        if not screenshot_path.is_file():
            fail(
                f"BPI training-readiness screenshot is missing: "
                f"{screenshot.get('path', '')}",
                failures,
            )
        elif hashlib.sha256(screenshot_path.read_bytes()).hexdigest() != screenshot.get(
                "sha256"):
            fail(
                f"BPI training-readiness screenshot hash does not match: "
                f"{screenshot.get('path', '')}",
                failures,
            )

    expected_readiness_cleanup = {
        "datasetDefinitionRows": 0,
        "datasetSnapshotRows": 0,
        "datasetSampleRows": 0,
        "datasetMaterializationRows": 0,
        "datasetCatalogPublicationRows": 0,
        "datasetRetentionArchiveRows": 0,
        "datasetMlflowRegistrationRows": 0,
        "datasetTrainingReadinessRows": 0,
        "auditRows": 0,
        "idempotencyRows": 0,
        "fixtureMarkerRows": 0,
        "sourceObjectVersions": 0,
        "trainingWarehouseObjectVersions": 0,
        "archiveObjectVersions": 0,
        "polarisTargetTableExists": False,
        "polarisTargetNamespaceExists": False,
        "mlflowTemporaryVolumes": 0,
        "optionalRunningSidecars": 0,
        "defaultOffFlagsVerified": True,
        "formalComposeOnly": True,
    }
    if readiness_acceptance.get("cleanup") != expected_readiness_cleanup:
        fail("BPI training-readiness cleanup evidence is incomplete", failures)
    expected_readiness_boundaries = {
        "offlineTrainingReadinessAssessed": True,
        "offlineTrainingEligible": False,
        "assessmentOnly": True,
        "trainingStarted": False,
        "modelCreated": False,
        "modelRegistered": False,
        "modelApproved": False,
        "onlineInferenceEnabled": False,
        "productionActivationAllowed": False,
        "processSignalWindowFeaturesAvailable": False,
        "productionVolumeAccepted": False,
        "mlflowProductionRbacSsoAccepted": False,
        "fullSiteDisasterRecoveryAccepted": False,
        "productionCapacityTested": False,
        "continuousFieldRunTested": False,
        "externalErpWmsTested": False,
    }
    if readiness_acceptance.get("boundaries") != expected_readiness_boundaries:
        fail("BPI training-readiness acceptance overclaims a phase boundary", failures)

    readiness_target = readiness_acceptance.get("target", {})
    expected_readiness_default_off = {
        "BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED": False,
        "BPI_DATASET_SOURCE_READER_ENABLED": False,
        "BPI_DATASET_MATERIALIZER_ENABLED": False,
        "BPI_POLARIS_ENABLED": False,
        "BPI_POLARIS_DROP_WITH_PURGE_ENABLED": False,
        "BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED": False,
        "BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED": False,
        "BPI_DATASET_CATALOG_PUBLISHER_ENABLED": False,
        "BPI_DATASET_RECOVERY_BUCKET_BOOTSTRAP_ENABLED": False,
        "BPI_DATASET_RETENTION_ARCHIVER_ENABLED": False,
        "BPI_MLFLOW_ARTIFACT_BOOTSTRAP_ENABLED": False,
        "BPI_DATASET_MLFLOW_REGISTRAR_ENABLED": False,
    }
    if (readiness_target.get("sshHost") != "10.11.100.17"
            or readiness_target.get("adpBaseUrl")
                != "http://10.11.100.17:18080"
            or readiness_target.get("composeProject") != "adp-mes-newbase"
            or readiness_target.get("runtimeDirectory")
                != "/home/v6/adp-mes-docker-newbase-20260611-181921"
            or readiness_target.get("releaseDirectory")
                != "/home/v6/ft-mes-bpi-release-v31-89044926"
            or readiness_target.get("flywayVersion") != 31
            or readiness_target.get("composeConfigFiles") != [
                "/home/v6/adp-mes-docker-newbase-20260611-181921/"
                "deploy/docker/docker-compose.yml"
            ]
            or readiness_target.get("health") != {
                "bpiService": "healthy",
                "bpiAdapter": "healthy",
                "bpiWmsAdapter": "healthy",
                "bpiWebHttpStatus": 200,
                "optionalRunningSidecars": 0,
            }
            or readiness_target.get("defaultOff") != expected_readiness_default_off):
        fail("BPI training-readiness target runtime was not restored", failures)

    readiness_upgrade_path = ROOT / readiness_target.get(
        "upgradeReportArtifact", ""
    )
    if not readiness_upgrade_path.is_file():
        fail("BPI V31 controlled upgrade artifact is missing", failures)
    else:
        readiness_upgrade_bytes = readiness_upgrade_path.read_bytes()
        if hashlib.sha256(readiness_upgrade_bytes).hexdigest() != readiness_target.get(
                "upgradeReportSha256"):
            fail("BPI V31 controlled upgrade artifact hash does not match", failures)
        readiness_upgrade = json.loads(readiness_upgrade_bytes)
        if (readiness_upgrade.get("status") != "PASS"
                or readiness_upgrade.get("phase") != "COMPLETE"
                or readiness_upgrade.get("releaseCommit") != readiness_commit
                or readiness_upgrade.get("database", {}).get("engine")
                    != "PostgreSQL"
                or readiness_upgrade.get("database", {}).get("beforeFlywayVersion")
                    != 30
                or readiness_upgrade.get("database", {}).get("afterFlywayVersion")
                    != 31
                or readiness_upgrade.get("database", {}).get("expectedFlywayVersion")
                    != 31
                or readiness_upgrade.get("database", {}).get("migrationMode")
                    != "APPLY_EXPANSION"
                or readiness_upgrade.get("database", {}).get("migrationApplied")
                    is not True
                or readiness_upgrade.get("database", {}).get("schemaDowngradeAllowed")
                    is not False):
            fail("BPI V31 controlled upgrade evidence is incomplete", failures)

    process_window_acceptance = json.loads(
        (
            ROOT
            / "metadata/bpi-dataset-process-signal-window-acceptance.json"
        ).read_text(encoding="utf-8")
    )
    process_window_commit = "f7db2f98e82d481f6c53c5fa7539ac52c812e28f"
    process_window_target = process_window_acceptance.get("target", {})
    if (
        process_window_acceptance.get("status")
        != "PASS_TARGET_BROWSER_API_POSTGRES_PROCESS_WINDOWS_CLEANED_DEFAULT_OFF"
        or process_window_acceptance.get("phase")
        != "3C_C_PROCESS_SIGNAL_WINDOWS"
        or process_window_acceptance.get("repoCommit") != process_window_commit
        or process_window_acceptance.get("releaseCommit") != process_window_commit
        or process_window_target.get("host") != "10.11.100.17"
        or process_window_target.get("composeProject") != "adp-mes-newbase"
        or process_window_target.get("database") != "PostgreSQL 15.18"
        or process_window_target.get("databaseName") != "ft_mes_bpi"
        or process_window_target.get("flywayVersion") != 33
    ):
        fail("BPI process-window target identity is incomplete", failures)

    expected_process_window_summary = {
        "testedFeatures": 12,
        "pass": 12,
        "fail": 0,
        "blocked": 0,
        "notApplicable": 0,
        "browserRuns": 1,
        "browserFailures": 0,
        "postgresVerificationRuns": 1,
        "postgresVerificationFailures": 0,
        "unexpectedConsoleErrors": 0,
        "pageErrors": 0,
        "requestFailures": 0,
        "horizontalOverflowFailures": 0,
    }
    if process_window_acceptance.get("summary") != expected_process_window_summary:
        fail("BPI process-window acceptance summary is incomplete", failures)
    process_window_items = process_window_acceptance.get("items", [])
    if (
        len(process_window_items) != 12
        or sum(item.get("status") == "PASS" for item in process_window_items) != 12
    ):
        fail("BPI process-window item statuses are incomplete", failures)

    process_window_deployment = process_window_acceptance.get("deployment", {})
    if (
        process_window_deployment.get("strategy") != "INTEGRATED_EXPAND_ONLY"
        or process_window_deployment.get("databaseBefore") != 32
        or process_window_deployment.get("databaseAfter") != 33
        or process_window_deployment.get("upgradeStatus") != "PASS"
        or process_window_deployment.get("upgradePhase") != "COMPLETE"
        or process_window_deployment.get("activeServicesHealthy") != 3
        or process_window_deployment.get("optionalServicesRunning") != 0
        or process_window_deployment.get("webStatus") != 200
    ):
        fail("BPI process-window deployment evidence is incomplete", failures)

    process_window_browser = process_window_acceptance.get("browser", {})
    if (
        process_window_browser.get("definitionStatus") != 200
        or process_window_browser.get("snapshotStatus") != 202
        or process_window_browser.get("snapshotState") != "MANIFEST_READY"
        or process_window_browser.get("snapshotRevision") != 3
        or process_window_browser.get("processWindowDefinitions") != 2
        or process_window_browser.get("processWindowFacts") != 6
        or process_window_browser.get("readyFacts") != 2
        or process_window_browser.get("blockedFacts") != 4
        or any(
            process_window_browser.get(key)
            for key in ("consoleErrors", "pageErrors", "requestFailures")
        )
        or process_window_browser.get("mobile", {}).get("viewport") != 390
        or process_window_browser.get("mobile", {}).get("bodyWidth") != 390
        or process_window_browser.get("mobile", {}).get("documentWidth") != 390
    ):
        fail("BPI process-window browser evidence is incomplete", failures)
    for viewport_name in ("desktop", "mobile"):
        screenshot = process_window_browser.get(viewport_name, {})
        screenshot_path = ROOT / screenshot.get("screenshot", "")
        if not screenshot_path.is_file():
            fail(
                f"BPI process-window {viewport_name} screenshot is missing",
                failures,
            )
        elif hashlib.sha256(screenshot_path.read_bytes()).hexdigest() != screenshot.get(
            "sha256"
        ):
            fail(
                f"BPI process-window {viewport_name} screenshot hash does not match",
                failures,
            )

    process_window_postgres = process_window_acceptance.get("postgres", {})
    process_window_facts = process_window_postgres.get("processWindows", {})
    process_window_flow = process_window_postgres.get("flow", {})
    process_window_pump = process_window_postgres.get("pump", {})
    if (
        process_window_postgres.get("definitions") != 1
        or process_window_postgres.get("snapshots") != 1
        or process_window_postgres.get("samples", {}).get("total") != 3
        or process_window_postgres.get("samples", {}).get("included") != 1
        or process_window_postgres.get("samples", {}).get("excluded") != 2
        or process_window_postgres.get("samples", {}).get("labelLeakageRows") != 0
        or process_window_postgres.get("samples", {}).get("crossPlantRows") != 0
        or process_window_facts.get("total") != 6
        or process_window_facts.get("ready") != 2
        or process_window_facts.get("blocked") != 4
        or process_window_facts.get("cutoffSafe") != 6
        or process_window_facts.get("checksumsValid") != 6
        or process_window_flow.get("sourcePointCount") != 4
        or process_window_flow.get("acceptedSampleCount") != 3
        or process_window_flow.get("lateAvailabilityCount") != 1
        or process_window_flow.get("numericValue") != 20
        or process_window_pump.get("sourcePointCount") != 2
        or process_window_pump.get("acceptedSampleCount") != 2
        or process_window_pump.get("numericValue") != 0.5
        or process_window_postgres.get("immutableUpdateRejected") is not True
    ):
        fail("BPI process-window PostgreSQL evidence is incomplete", failures)

    process_window_cleanup = process_window_acceptance.get("cleanup", {})
    if (
        not process_window_cleanup
        or any(value != 0 for value in process_window_cleanup.values())
    ):
        fail("BPI process-window cleanup evidence is incomplete", failures)
    process_window_boundaries = process_window_acceptance.get("boundaries", {})
    if (
        not process_window_boundaries
        or any(value is not False for value in process_window_boundaries.values())
    ):
        fail("BPI process-window acceptance overclaims a phase boundary", failures)

    process_window_upgrade_path = ROOT / process_window_deployment.get(
        "upgradeReport", ""
    )
    if not process_window_upgrade_path.is_file():
        fail("BPI V33 controlled upgrade artifact is missing", failures)
    else:
        process_window_upgrade_bytes = process_window_upgrade_path.read_bytes()
        if hashlib.sha256(process_window_upgrade_bytes).hexdigest() != (
            process_window_deployment.get("upgradeReportSha256")
        ):
            fail("BPI V33 controlled upgrade artifact hash does not match", failures)
        process_window_upgrade = json.loads(process_window_upgrade_bytes)
        process_window_upgrade_database = process_window_upgrade.get("database", {})
        if (
            process_window_upgrade.get("status") != "PASS"
            or process_window_upgrade.get("phase") != "COMPLETE"
            or process_window_upgrade.get("strategy") != "INTEGRATED_EXPAND_ONLY"
            or process_window_upgrade.get("releaseCommit") != process_window_commit
            or process_window_upgrade_database.get("engine") != "PostgreSQL"
            or process_window_upgrade_database.get("beforeFlywayVersion") != 32
            or process_window_upgrade_database.get("afterFlywayVersion") != 33
            or process_window_upgrade_database.get("expectedFlywayVersion") != 33
            or process_window_upgrade_database.get("migrationMode")
            != "APPLY_EXPANSION"
            or process_window_upgrade_database.get("migrationApplied") is not True
            or process_window_upgrade_database.get("schemaDowngradeAllowed")
            is not False
        ):
            fail("BPI V33 controlled upgrade evidence is incomplete", failures)

    formal_reversal = json.loads(
        (ROOT / "metadata/bpi-formal-identity-wms-reversal-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    formal_target = formal_reversal.get("target", {})
    formal_scope = formal_reversal.get("scope", {})
    formal_identity = formal_reversal.get("identity", {})
    formal_safety = formal_reversal.get("safety", {})
    expected_disabled_phase2 = (
        "BPI_PHASE2_INTEGRATION_ENABLED:false,"
        "BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED:false,"
        "BPI_PHASE2_KAFKA_ENABLED:false,"
        "BPI_WMS_OUTBOX_ENABLED:false,"
        "BPI_WMS_ADAPTER_ENABLED:false,"
        "QCS_BPI_OUTBOX_ENABLED:false,"
    )
    expected_active_phase2 = expected_disabled_phase2.replace(
        "BPI_PHASE2_INTEGRATION_ENABLED:false,",
        "BPI_PHASE2_INTEGRATION_ENABLED:true,",
        1,
    )
    if (formal_reversal.get("status") != (
            "PASS_TARGET_FORMAL_IDENTITY_WMS_REVERSAL_TWO_BROWSER_SESSIONS_CLEANED")
            or formal_reversal.get("database") != "PostgreSQL"
            or formal_reversal.get("repoCommit")
                != "80cf094a415c4ae33541f08da8b640fc15c52098"
            or not formal_reversal.get("marker", "").startswith(
                "ADP_BPI_FORMAL_WMS_REVERSAL_")
            or formal_target.get("sshHost") != "10.11.100.17"
            or formal_target.get("adpBaseUrl") != "http://10.11.100.17:18080"
            or formal_scope != {
                "tenantId": "1000",
                "plantId": "PLANT-01",
                "lineId": "LINE-S07-01",
            }):
        fail("BPI formal WMS reversal target, source, or scope evidence is incomplete", failures)
    formal_current_user = formal_identity.get("currentUser", {})
    if (formal_identity.get("requesterLogin") != "admin"
            or formal_identity.get("requesterSubject") != "legacy-ticket:admin"
            or formal_identity.get("approverLogin") == "admin"
            or formal_identity.get("approverSubject")
                == formal_identity.get("requesterSubject")
            or formal_identity.get("roleCode") != "systemRole"
            or formal_identity.get("password") != "REDACTED"
            or formal_current_user.get("loginStatus") != 200
            or formal_current_user.get("currentUserStatus") != 200
            or formal_current_user.get("username")
                != formal_identity.get("approverLogin")
            or formal_current_user.get("cid") != 1000
            or "systemRole" not in formal_current_user.get("roles", [])
            or formal_current_user.get("ticketMode") != "LEGACY_UUID"):
        fail("BPI formal WMS reversal identity evidence is incomplete", failures)
    if (formal_safety.get("temporaryFormalIdentity") is not True
            or formal_safety.get("adapterScopeUsesIsolatedComposeOverride") is not True
            or formal_safety.get("baseRuntimeEnvEdited") is not False
            or formal_safety.get("adapterImageMustRemainExact") is not True
            or formal_safety.get("watchdogSeconds") != 900
            or formal_safety.get("phase2BaseExpectedEnabled") is not False
            or formal_safety.get("phase2CommandGateTemporarilyEnabled") is not True
            or formal_safety.get("kafkaIngressExpectedEnabled") is not False
            or formal_safety.get("wmsOutboxExpectedEnabled") is not False
            or formal_safety.get("externalWmsReceiptExpected") is not False
            or formal_safety.get("cleanupByMarkerAndIdentityOnly") is not True):
        fail("BPI formal WMS reversal safety boundary is incomplete", failures)

    formal_stages = formal_reversal.get("stages", {})
    formal_precheck = formal_stages.get("adapterPrecheck", {})
    formal_activated = formal_stages.get("adapterActivated", {})
    formal_restored = formal_stages.get("adapterRestored", {})
    if (formal_precheck != formal_restored
            or formal_precheck.get("adapterHealth") != "healthy"
            or formal_precheck.get("serviceHealth") != "healthy"
            or formal_precheck.get("phase2State") != expected_disabled_phase2
            or formal_activated.get("phase2State") != expected_active_phase2
            or formal_activated.get("adapterImageId")
                != formal_precheck.get("adapterImageId")
            or formal_activated.get("serviceImageId")
                != formal_precheck.get("serviceImageId")
            or formal_identity.get("approverLogin", "")
                not in formal_activated.get("adapterSubjectScopeRules", "")):
        fail("BPI formal WMS reversal isolated activation or restoration is incomplete", failures)

    formal_operations = formal_reversal.get("operations", {})
    identity_operations = formal_operations.get("identityProvisioning", {})
    reversal_operations = formal_operations.get("reversal", {})
    formal_request = reversal_operations.get("request", {})
    formal_rejection = reversal_operations.get("sameActorRejection", {})
    formal_approval = reversal_operations.get("approval", {})
    request_result = formal_request.get("response", {})
    approval_result = formal_approval.get("response", {})
    if (any(identity_operations.get(key, {}).get("status") != 200
            for key in ("person", "user", "role"))
            or identity_operations.get("user", {}).get("password") != "REDACTED"
            or reversal_operations.get("unauthenticatedRead", {}).get("status") != 401
            or formal_request.get("status") != 202
            or formal_request.get("request", {}).get("payload", {}).get("approvalMode")
                != "REQUEST"
            or formal_request.get("request", {}).get("ifMatch") != "4"
            or request_result.get("state") != "PENDING_APPROVAL"
            or request_result.get("revision") != 1
            or request_result.get("batchRevision") != 5
            or request_result.get("requestedBy") != formal_identity.get("requesterSubject")
            or formal_rejection.get("status") != 403
            or formal_rejection.get("request", {}).get("payload", {}).get("approvalMode")
                != "APPROVE"
            or "different administrator" not in formal_rejection.get(
                "response", {}).get("detail", "")
            or formal_approval.get("status") != 202
            or formal_approval.get("request", {}).get("payload", {}).get("approvalMode")
                != "APPROVE"
            or formal_approval.get("request", {}).get("ifMatch") != "5"
            or approval_result.get("state") != "PENDING_WMS"
            or approval_result.get("revision") != 2
            or approval_result.get("batchRevision") != 6
            or approval_result.get("requestedBy") != formal_identity.get("requesterSubject")
            or approval_result.get("decidedBy") != formal_identity.get("approverSubject")
            or approval_result.get("requestedBy") == approval_result.get("decidedBy")
            or approval_result.get("outboxStatus") != "PENDING"):
        fail("BPI formal WMS reversal browser command or four-eye evidence is incomplete", failures)

    formal_browser = formal_reversal.get("browser", {})
    formal_browser_runtime = formal_browser.get("browser", {})
    formal_browser_geometry = formal_browser_runtime.get("geometry", {})
    if (formal_browser.get("status") != "PASS"
            or formal_browser.get("database") != "PostgreSQL"
            or formal_browser.get("marker") != formal_reversal.get("marker")
            or formal_browser.get("loginStatus") != 200
            or formal_browser.get("approverLoginStatus") != 200
            or formal_browser.get("actors", {}).get("requesterSubject")
                != formal_identity.get("requesterSubject")
            or formal_browser.get("actors", {}).get("approverSubject")
                != formal_identity.get("approverSubject")
            or formal_browser.get("operations") != reversal_operations
            or len(formal_browser_runtime.get("consoleErrors", [])) != 1
            or len(formal_browser_runtime.get("bpiHttpErrors", [])) != 1
            or formal_browser_runtime.get("bpiHttpErrors", [{}])[0].get("status") != 403
            or any(formal_browser_runtime.get(key) for key in (
                "pageErrors", "requestFailures", "unexpectedBpiHttpErrors"))
            or formal_browser_geometry.get("viewportWidth")
                != formal_browser_geometry.get("documentWidth")
            or formal_browser_geometry.get("drawerWidth") != 680
            or formal_browser.get("error") is not None):
        fail("BPI formal WMS reversal browser, expected 403, or layout evidence is incomplete", failures)

    formal_postgres = formal_reversal.get("postgres", {})
    formal_fixture = formal_postgres.get("fixture", {})
    formal_pending = formal_postgres.get("pending", {})
    formal_approved = formal_postgres.get("approved", {})
    pending_batch = formal_pending.get("batch", {})
    pending_task = formal_pending.get("reversalTask", {})
    pending_blue = formal_pending.get("originalInbound", {})
    approved_batch = formal_approved.get("batch", {})
    approved_task = formal_approved.get("reversalTask", {})
    approved_blue = formal_approved.get("originalInbound", {})
    approved_red = formal_approved.get("reversalOutbox", {})
    if (formal_fixture.get("batchState") != "INBOUNDED"
            or formal_fixture.get("batchRevision") != 4
            or formal_fixture.get("commandsEnabled") is not True
            or formal_fixture.get("wmsLinkEnabled") is not True
            or pending_batch.get("state") != "INBOUNDED"
            or pending_batch.get("revision") != 5
            or pending_batch.get("wmsStatus") != "INBOUNDED"
            or pending_batch.get("isShadow") is not False
            or pending_task.get("state") != "PENDING_APPROVAL"
            or pending_task.get("revision") != 1
            or pending_task.get("requestedBy") != formal_identity.get("requesterSubject")
            or pending_task.get("decidedBy") is not None
            or formal_pending.get("reversalOutbox") is not None
            or formal_pending.get("idempotencyRows") != 1
            or formal_pending.get("reversalOutboxRows") != 0
            or formal_pending.get("reversalInboxRows") != 0
            or approved_batch.get("state") != "INBOUND_REVERSING"
            or approved_batch.get("revision") != 6
            or approved_batch.get("wmsStatus") != "REVERSAL_PENDING"
            or approved_task.get("state") != "PENDING_WMS"
            or approved_task.get("revision") != 2
            or approved_task.get("requestedBy") != formal_identity.get("requesterSubject")
            or approved_task.get("decidedBy") != formal_identity.get("approverSubject")
            or formal_approved.get("idempotencyRows") != 2
            or formal_approved.get("reversalOutboxRows") != 1
            or formal_approved.get("reversalInboxRows") != 0):
        fail("BPI formal WMS reversal pending or approved PostgreSQL projection is incomplete", failures)
    if (pending_blue != approved_blue
            or pending_blue.get("commandEventId")
                != formal_fixture.get("originalCommandEventId")
            or pending_blue.get("documentId")
                != formal_fixture.get("originalDocumentId")
            or pending_blue.get("payloadSha256")
                != formal_fixture.get("originalPayloadSha256")
            or request_result.get("originalCommandEventId")
                != formal_fixture.get("originalCommandEventId")
            or approval_result.get("originalCommandEventId")
                != formal_fixture.get("originalCommandEventId")
            or request_result.get("originalDocumentId")
                != formal_fixture.get("originalDocumentId")
            or approval_result.get("originalDocumentId")
                != formal_fixture.get("originalDocumentId")):
        fail("BPI formal WMS reversal original blue document is not proven immutable", failures)
    if (approved_red.get("id") != approval_result.get("reversalCommandEventId")
            or approved_red.get("id") == formal_fixture.get("originalCommandEventId")
            or approved_red.get("eventType")
                != "WMS_COMPLETION_INBOUND_REVERSAL_COMMAND"
            or approved_red.get("topic")
                != "bpi.wms.completion-inbound-reversal-command.v1"
            or approved_red.get("status") != "PENDING"
            or approved_red.get("revision") != 1
            or approved_red.get("attemptCount") != 0
            or approved_red.get("payloadBytes") != 994
            or approved_red.get("headers", {}).get("event_id") != approved_red.get("id")
            or approved_red.get("headers", {}).get("idempotency_key")
                != approval_result.get("reversalIdempotencyKey")):
        fail("BPI formal WMS reversal append-only red command evidence is incomplete", failures)
    if ([event.get("action") for event in formal_approved.get("stateEvents", [])] != [
            "WMS_INBOUND_REVERSAL_REQUESTED", "WMS_INBOUND_REVERSAL_APPROVED"]
            or [event.get("revision") for event in formal_approved.get("stateEvents", [])]
                != [5, 6]
            or [event.get("action") for event in formal_approved.get("auditEvents", [])]
                != ["WMS_INBOUND_REVERSAL_REQUESTED", "WMS_INBOUND_REVERSAL_APPROVED"]):
        fail("BPI formal WMS reversal state and audit history is incomplete", failures)

    formal_identity_final = formal_postgres.get("identityFinal", {})
    if (formal_identity_final.get("person", {}).get("valid") != 0
            or formal_identity_final.get("user", {}).get("valid") != 0
            or formal_identity_final.get("roleUser") is not None
            or formal_identity_final.get("authRoles") != []):
        fail("BPI formal WMS reversal temporary identity cleanup is incomplete", failures)
    formal_checks = {
        check.get("name"): check for check in formal_reversal.get("checks", [])
    }
    required_formal_checks = {
        "requester persisted a pending reversal without creating a red command",
        "pending state preserves the accepted blue document byte-for-byte",
        "formal approver used a separate ADP browser session through the adapter",
        "PostgreSQL records distinct requester and approver identities",
        "approval appends one durable red command without mutating the blue document",
        "temporary formal identity has no active residual binding",
        "BPI marker cleanup has zero residual rows",
        "adapter scope and image restored exactly",
        "Phase 2 and write-back switches remain disabled",
    }
    marker_cleanup = formal_checks.get(
        "BPI marker cleanup has zero residual rows", {}).get("detail", {})
    restore_detail = formal_checks.get(
        "adapter scope and image restored exactly", {}).get("detail", {})
    if (len(formal_checks) != 20
            or not required_formal_checks.issubset(formal_checks)
            or any(check.get("passed") is not True for check in formal_checks.values())
            or any(value != 0 for value in marker_cleanup.values())
            or restore_detail.get("before") != restore_detail.get("restored")
            or formal_reversal.get("issues") != []
            or not any("residualRows=0" in line
                       for line in formal_reversal.get("cleanup", {}).get("bpi", []))
            or formal_reversal.get("cleanup", {}).get("adapter", {}).get(
                "watchdogRestored") != "false"):
        fail("BPI formal WMS reversal cleanup or final checks are incomplete", failures)
    for screenshot in formal_browser.get("screenshots", {}).values():
        screenshot_path = ROOT / screenshot.get("path", "")
        if not screenshot_path.is_file():
            fail(f"BPI formal WMS reversal screenshot is missing: {screenshot.get('path', '')}", failures)
        elif hashlib.sha256(screenshot_path.read_bytes()).hexdigest() != screenshot.get("sha256"):
            fail(f"BPI formal WMS reversal screenshot hash does not match: {screenshot.get('path', '')}", failures)

    material_schema_upgrade = json.loads(
        (ROOT / "metadata/bpi-material-wms-reversal-schema-upgrade.json").read_text(
            encoding="utf-8"
        )
    )
    schema_before = material_schema_upgrade.get("before", {})
    schema_after = material_schema_upgrade.get("after", {})
    schema_flags = (
        "reversalColumn",
        "documentTypeConstraint",
        "transactionTypeConstraint",
        "reversalForeignKey",
        "reversalUniqueIndex",
    )
    if (material_schema_upgrade.get("status") != "PASS_APPLIED_EXPAND_ONLY"
            or material_schema_upgrade.get("strategy") != "EXPAND_ONLY"
            or material_schema_upgrade.get("target", {}).get("host") != "10.11.100.17"
            or material_schema_upgrade.get("target", {}).get("database") != "adp"
            or len(material_schema_upgrade.get("migration", {}).get("sha256", "")) != 64
            or len(material_schema_upgrade.get("backup", {}).get("sha256", "")) != 64
            or any(schema_before.get(key) is not False for key in schema_flags)
            or any(schema_after.get(key) is not True for key in schema_flags)
            or schema_before.get("unsupportedDocumentTypes") != []
            or schema_before.get("unsupportedTransactionTypes") != []
            or schema_after.get("unsupportedDocumentTypes") != []
            or schema_after.get("unsupportedTransactionTypes") != []
            or material_schema_upgrade.get("issues") != []):
        fail("material-wms reversal expand-only target schema evidence is incomplete", failures)

    material_deployment = json.loads(
        (ROOT / "metadata/bpi-material-wms-target-deployment.json").read_text(
            encoding="utf-8"
        )
    )
    material_artifact = material_deployment.get("artifact", {})
    material_deploy_target = material_deployment.get("target", {})
    if (material_deployment.get("status") != "PASS_BACKUP_RESTART_VERIFIED"
            or material_deploy_target.get("host") != "10.11.100.17"
            or material_deploy_target.get("composeProject") != "adp-mes-newbase"
            or material_artifact.get("sha256") != material_deployment.get("deployedSha256")
            or len(material_artifact.get("sha256", "")) != 64
            or material_artifact.get("sizeBytes", 0) <= 0
            or len(material_deployment.get("backup", {}).get("previousSha256", "")) != 64
            or material_deployment.get("routeProbe") != "PASS_AUTH_GATE"
            or material_deployment.get("rollbackRestored") is not False
            or material_deployment.get("issues") != []):
        fail("material-wms target JAR backup, deployment, or route evidence is incomplete", failures)

    formal_roundtrip = json.loads(
        (ROOT / "metadata/bpi-formal-identity-wms-roundtrip-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    roundtrip_target = formal_roundtrip.get("target", {})
    roundtrip_scope = formal_roundtrip.get("scope", {})
    roundtrip_identity = formal_roundtrip.get("identity", {})
    roundtrip_safety = formal_roundtrip.get("safety", {})
    expected_roundtrip_phase2 = (
        "BPI_PHASE2_INTEGRATION_ENABLED:true,"
        "BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED:false,"
        "BPI_PHASE2_KAFKA_ENABLED:true,"
        "BPI_WMS_OUTBOX_ENABLED:true,"
        "BPI_WMS_ADAPTER_ENABLED:true,"
        "QCS_BPI_OUTBOX_ENABLED:false,"
    )
    if (formal_roundtrip.get("status")
            != "PASS_TARGET_FORMAL_IDENTITY_INTERNAL_WMS_ROUNDTRIP_CLEANED"
            or formal_roundtrip.get("database") != "PostgreSQL"
            or formal_roundtrip.get("mode") != "TARGET_INTERNAL_WMS_ROUNDTRIP"
            or not formal_roundtrip.get("marker", "").startswith(
                "ADP_BPI_FORMAL_WMS_REVERSAL_")
            or roundtrip_target.get("sshHost") != "10.11.100.17"
            or roundtrip_target.get("adpBaseUrl") != "http://10.11.100.17:18080"
            or roundtrip_scope != {
                "tenantId": "1000",
                "plantId": "PLANT-01",
                "lineId": "LINE-S07-01",
            }
            or roundtrip_identity.get("requesterSubject") != "legacy-ticket:admin"
            or roundtrip_identity.get("approverSubject")
                == roundtrip_identity.get("requesterSubject")
            or roundtrip_identity.get("password") != "REDACTED"
            or roundtrip_safety.get("internalMaterialWmsRoundTripExpected") is not True
            or roundtrip_safety.get("externalWmsReceiptExpected") is not False
            or roundtrip_safety.get("isolatedKafkaResources") is not True
            or roundtrip_safety.get("baseRuntimeEnvEdited") is not False
            or roundtrip_safety.get("cleanupByMarkerAndIdentityOnly") is not True):
        fail("BPI formal internal WMS roundtrip boundary or identity evidence is incomplete", failures)

    roundtrip_stages = formal_roundtrip.get("stages", {})
    roundtrip_precheck = roundtrip_stages.get("adapterPrecheck", {})
    roundtrip_active = roundtrip_stages.get("adapterActivated", {})
    roundtrip_restored = roundtrip_stages.get("adapterRestored", {})
    if (roundtrip_precheck != roundtrip_restored
            or roundtrip_precheck.get("phase2State") != expected_disabled_phase2
            or roundtrip_active.get("phase2State") != expected_roundtrip_phase2
            or any(roundtrip_precheck.get(key) != "healthy" for key in (
                "adapterHealth", "serviceHealth", "wmsAdapterHealth"))
            or roundtrip_stages.get("wmsPaused", {}).get("wmsAdapterHealth") != "exited"
            or roundtrip_stages.get("wmsResumed", {}).get("wmsAdapterHealth") != "healthy"):
        fail("BPI formal internal WMS isolated activation or exact restoration is incomplete", failures)

    roundtrip_operations = formal_roundtrip.get("operations", {}).get("reversal", {})
    roundtrip_request = roundtrip_operations.get("request", {})
    roundtrip_rejection = roundtrip_operations.get("sameActorRejection", {})
    roundtrip_approval = roundtrip_operations.get("approval", {})
    if (roundtrip_operations.get("unauthenticatedRead", {}).get("status") != 401
            or roundtrip_request.get("status") != 202
            or roundtrip_request.get("response", {}).get("state") != "PENDING_APPROVAL"
            or roundtrip_request.get("response", {}).get("batchRevision") != 5
            or roundtrip_rejection.get("status") != 403
            or roundtrip_approval.get("status") != 202
            or roundtrip_approval.get("response", {}).get("state") != "PENDING_WMS"
            or roundtrip_approval.get("response", {}).get("batchRevision") != 6
            or roundtrip_approval.get("response", {}).get("requestedBy")
                == roundtrip_approval.get("response", {}).get("decidedBy")):
        fail("BPI formal internal WMS request, separation, or approval evidence is incomplete", failures)

    expected_kafka_final = {
        "blueCommand": "1",
        "blueCommandDlq": "0",
        "blueReceipt": "1",
        "blueReceiptDlq": "0",
        "redCommand": "1",
        "redCommandDlq": "0",
        "redReceipt": "1",
        "redReceiptDlq": "0",
        "qcs": "0",
        "qcsDlq": "0",
        "wmsLag": "0",
        "receiptLag": "0",
    }
    if formal_roundtrip.get("kafka", {}).get("final") != expected_kafka_final:
        fail("BPI formal internal WMS Kafka counts, DLQ, or lag evidence is incomplete", failures)

    roundtrip_postgres = formal_roundtrip.get("postgres", {})
    bpi_roundtrip = roundtrip_postgres.get("roundTrip", {})
    bpi_roundtrip_batch = bpi_roundtrip.get("batch", {})
    bpi_roundtrip_blue = bpi_roundtrip.get("blue", {})
    bpi_roundtrip_red = bpi_roundtrip.get("red", {})
    expected_roundtrip_actions = [
        "WMS_INBOUND_ACCEPTED",
        "WMS_INBOUND_REVERSAL_REQUESTED",
        "WMS_INBOUND_REVERSAL_APPROVED",
        "WMS_INBOUND_REVERSAL_ACCEPTED",
    ]
    if (bpi_roundtrip_batch.get("state") != "INBOUND_REVERSED"
            or bpi_roundtrip_batch.get("revision") != 7
            or bpi_roundtrip_batch.get("wmsStatus") != "REVERSED"
            or bpi_roundtrip_blue.get("status") != "ACCEPTED"
            or bpi_roundtrip_blue.get("outboxStatus") != "PUBLISHED"
            or bpi_roundtrip_red.get("state") != "COMPLETED"
            or bpi_roundtrip_red.get("revision") != 3
            or bpi_roundtrip_red.get("outboxStatus") != "PUBLISHED"
            or bpi_roundtrip_red.get("originalCommandEventId")
                != bpi_roundtrip_blue.get("commandEventId")
            or [event.get("action") for event in bpi_roundtrip.get("stateEvents", [])]
                != expected_roundtrip_actions
            or bpi_roundtrip.get("auditActions") != expected_roundtrip_actions
            or bpi_roundtrip.get("blueInboxRows") != 1
            or bpi_roundtrip.get("redInboxRows") != 1
            or bpi_roundtrip.get("reversalApiIdempotencyRows") != 2):
        fail("BPI formal internal WMS PostgreSQL terminal projection is incomplete", failures)

    material_roundtrip = roundtrip_postgres.get("materialRoundTrip", {})
    material_blue = material_roundtrip.get("blue", {})
    material_red = material_roundtrip.get("red", {})
    material_transactions = material_roundtrip.get("transactions", [])
    material_stock = material_roundtrip.get("stock", {})
    if (material_blue.get("status") != "REVERSED"
            or material_red.get("status") != "POSTED"
            or material_red.get("reversalOfDocumentId") != material_blue.get("id")
            or [item.get("type") for item in material_transactions] != [
                "COMPLETION_INBOUND", "COMPLETION_INBOUND_REVERSAL"]
            or [item.get("onHandDelta") for item in material_transactions]
                != [12.345, -12.345]
            or material_stock != {"onHand": 0, "available": 0, "hold": 0}):
        fail("material-wms blue/red document, transaction, or net-stock evidence is incomplete", failures)

    completed_browser = formal_roundtrip.get("completedBrowser", {})
    completed_geometry = completed_browser.get("geometry", {})
    completed_task = completed_browser.get("task", {})
    completed_screenshot = completed_browser.get("screenshot", {})
    if (completed_browser.get("status") != "PASS"
            or completed_browser.get("route") != "/bpi/#/batches"
            or completed_browser.get("release", {}).get("batch", {}).get("state")
                != "INBOUND_REVERSED"
            or completed_task.get("state") != "COMPLETED"
            or completed_task.get("reversalDocumentId") != bpi_roundtrip_red.get("documentId")
            or any(completed_browser.get(key) for key in (
                "consoleErrors", "pageErrors", "requestFailures", "bpiHttpErrors"))
            or completed_geometry.get("viewportWidth")
                != completed_geometry.get("documentWidth")
            or completed_geometry.get("drawerWidth") != 680):
        fail("BPI formal internal WMS completed browser evidence is incomplete", failures)
    completed_screenshot_path = ROOT / completed_screenshot.get("path", "")
    if not completed_screenshot_path.is_file():
        fail("BPI formal internal WMS completed screenshot is missing", failures)
    elif hashlib.sha256(completed_screenshot_path.read_bytes()).hexdigest() != (
            completed_screenshot.get("sha256")):
        fail("BPI formal internal WMS completed screenshot hash does not match", failures)
    for screenshot in formal_roundtrip.get("browser", {}).get("screenshots", {}).values():
        screenshot_path = ROOT / screenshot.get("path", "")
        if not screenshot_path.is_file():
            fail(f"BPI formal internal WMS screenshot is missing: {screenshot.get('path', '')}", failures)
        elif hashlib.sha256(screenshot_path.read_bytes()).hexdigest() != screenshot.get("sha256"):
            fail(f"BPI formal internal WMS screenshot hash does not match: {screenshot.get('path', '')}", failures)

    roundtrip_cleanup = formal_roundtrip.get("cleanup", {})
    if (len(formal_roundtrip.get("checks", [])) != 29
            or any(check.get("passed") is not True
                   for check in formal_roundtrip.get("checks", []))
            or any(value != 0 for value in roundtrip_postgres.get("bpiFinal", {}).values())
            or any(value != 0 for value in roundtrip_postgres.get("materialFinal", {}).values())
            or roundtrip_postgres.get("identityFinal", {}).get("person", {}).get("valid") != 0
            or roundtrip_postgres.get("identityFinal", {}).get("user", {}).get("valid") != 0
            or roundtrip_postgres.get("identityFinal", {}).get("roleUser") is not None
            or roundtrip_postgres.get("identityFinal", {}).get("authRoles") != []
            or not any("residualRows=0" in line for line in roundtrip_cleanup.get("bpi", []))
            or not any("residualRows=0" in line for line in roundtrip_cleanup.get("material", []))
            or roundtrip_cleanup.get("adapter", {}).get("isolatedKafkaCleaned") != "true"
            or roundtrip_cleanup.get("adapter", {}).get("watchdogRestored") != "false"
            or formal_roundtrip.get("issues") != []):
        fail("BPI formal internal WMS cleanup or final checks are incomplete", failures)

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

    force_close = json.loads(
        (ROOT / "metadata/bpi-force-close-target-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    force_target = force_close.get("target", {})
    force_source = force_close.get("source", {})
    force_deployment = force_close.get("deployment", {})
    force_runtime = force_close.get("runtime", {})
    force_scenario = force_close.get("scenario", {})
    if (force_close.get("status") != "PASS_TARGET_CONTROLLED_BROWSER_API_POSTGRES_CLEANED"
            or force_close.get("database") != "PostgreSQL"
            or force_target.get("host") != "10.11.100.17"
            or force_target.get("composeProject") != "adp-mes-newbase"
            or force_target.get("databaseName") != "ft_mes_bpi"
            or force_target.get("schema") != "bpi"
            or force_target.get("flywayVersion") != 24
            or len(force_source.get("serviceImplementationCommit", "")) != 40
            or len(force_source.get("adapterImplementationCommit", "")) != 40
            or force_deployment.get("strategy") != (
                "INTEGRATED_EXPAND_ONLY_V24_THEN_ADAPTER_ONLY_ROUTE_FIX")
            or force_deployment.get("otherBpiContainersRecreatedForRouteFix") is not False
            or not force_scenario.get("marker", "").startswith("ADP_E2E_")
            or not force_scenario.get("batchId")
            or not force_scenario.get("taskId")
            or force_scenario.get("requester") == force_scenario.get("approver")):
        fail("BPI force-close target, source, deployment, or actor evidence is incomplete", failures)
    adapter_tests = force_runtime.get("adapter", {}).get("java8Tests", {})
    java17_tests = force_runtime.get("java17ReactorTests", {})
    if (force_runtime.get("service", {}).get("health") != "healthy"
            or force_runtime.get("adapter", {}).get("health") != "healthy"
            or force_runtime.get("wmsAdapter", {}).get("health") != "healthy"
            or java17_tests.get("tests") != 115
            or java17_tests.get("failures") != 0
            or java17_tests.get("errors") != 0
            or java17_tests.get("skipped") != 43
            or adapter_tests.get("tests") != 31
            or any(adapter_tests.get(key) != 0 for key in (
                "failures", "errors", "skipped"))):
        fail("BPI force-close runtime or Java 8 adapter test evidence is incomplete", failures)
    force_browser = force_close.get("browser", {})
    force_request = force_browser.get("request", {})
    force_rejection = force_browser.get("sameActorRejection", {})
    force_approval = force_browser.get("independentApproval", {})
    force_final = force_browser.get("final", {})
    force_errors = force_browser.get("errors", {})
    force_geometry = force_browser.get("geometry", {})
    if (force_browser.get("loginStatus") != 200
            or force_browser.get("unauthenticatedRead", {}).get("status") != 401
            or force_request.get("status") != 202
            or force_request.get("resultState") != "PENDING_APPROVAL"
            or force_request.get("taskRevision") != 1
            or force_request.get("batchRevision") != 2
            or force_rejection.get("status") != 403
            or force_rejection.get("expectedSecurityEvidence") is not True
            or force_approval.get("status") != 202
            or force_approval.get("resultState") != "COMPLETED"
            or force_approval.get("taskRevision") != 2
            or force_approval.get("batchRevision") != 3
            or force_final.get("batchState") != "CLOSED_RAW"
            or force_final.get("batchRevision") != 3
            or force_final.get("taskState") != "COMPLETED"
            or force_final.get("taskRevision") != 2
            or force_final.get("qualityGate") != "NOT_APPLICABLE"
            or force_final.get("wmsStatus") != "NOT_REQUESTED"
            or force_final.get("timelineActions") != [
                "BATCH_FORCE_CLOSE_REQUESTED", "BATCH_FORCE_CLOSED"]
            or force_errors.get("expectedConsoleErrors") != 1
            or force_errors.get("expectedBpiHttpErrors") != 1
            or force_errors.get("expectedStatus") != 403
            or any(force_errors.get(key) != 0 for key in (
                "unexpectedConsoleErrors", "pageErrors", "requestFailures",
                "unexpectedBpiHttpErrors"))
            or force_geometry.get("viewportWidth") != force_geometry.get("documentWidth")
            or force_geometry.get("drawerWidth") != 680):
        fail("BPI force-close browser, security, or final-state evidence is incomplete", failures)
    force_postgres = force_close.get("postgres", {})
    force_pending = force_postgres.get("pending", {})
    force_persisted = force_postgres.get("final", {})
    force_cleanup = force_postgres.get("cleanup", {})
    if (force_pending.get("batchState") != "ACTIVE"
            or force_pending.get("batchRevision") != 2
            or force_pending.get("batchEndTime") is not None
            or force_pending.get("taskState") != "PENDING_APPROVAL"
            or force_pending.get("taskRevision") != 1
            or force_pending.get("idempotencyRows") != 1
            or any(force_pending.get(key) != 0 for key in (
                "qualityGateRows", "wmsLinkRows", "outboxRows"))
            or force_persisted.get("batchState") != "CLOSED_RAW"
            or force_persisted.get("batchRevision") != 3
            or force_persisted.get("taskState") != "COMPLETED"
            or force_persisted.get("taskRevision") != 2
            or force_persisted.get("requesterDiffersFromApprover") is not True
            or force_persisted.get("stateEventRevisions") != [2, 3]
            or force_persisted.get("auditRevisionTransitions") != ["1->2", "2->3"]
            or force_persisted.get("idempotencyRows") != 2
            or any(force_persisted.get(key) != 0 for key in (
                "qualityGateRows", "wmsLinkRows", "outboxRows"))
            or force_cleanup.get("residualRows") != 0
            or force_cleanup.get("temporaryCommandsFlagDeleted") is not True
            or force_cleanup.get("nonMarkerRowsDeleted") != 0):
        fail("BPI force-close pending, final, or cleanup PostgreSQL evidence is incomplete", failures)
    force_safety = force_close.get("finalSafetyState", {})
    if (force_safety.get("BPI_PHASE2_INTEGRATION_ENABLED") is not False
            or force_safety.get("BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED") is not False
            or force_safety.get("BPI_PHASE2_KAFKA_ENABLED") is not False
            or force_safety.get("BPI_WMS_OUTBOX_ENABLED") is not False
            or force_safety.get("BPI_WMS_ADAPTER_ENABLED") is not False):
        fail("BPI force-close final integration safety switches are not closed", failures)
    force_evidence = force_close.get("evidence", {})
    for key in (
            "browserReportSha256", "pendingPostgresSha256", "finalPostgresSha256",
            "fixtureSha256", "cleanupSha256", "adapterUpgradeSha256",
            "runtimeImagesSha256"):
        if len(force_evidence.get(key, "")) != 64:
            fail(f"BPI force-close evidence hash is incomplete: {key}", failures)
    force_screenshots = force_browser.get("screenshots", [])
    if len(force_screenshots) != 2:
        fail("BPI force-close target screenshots are incomplete", failures)
    for screenshot in force_screenshots:
        screenshot_path = ROOT / screenshot.get("path", "")
        if not screenshot_path.is_file():
            fail(f"BPI force-close screenshot is missing: {screenshot.get('path', '')}", failures)
        elif hashlib.sha256(screenshot_path.read_bytes()).hexdigest() != screenshot.get("sha256"):
            fail(f"BPI force-close screenshot hash does not match: {screenshot.get('path', '')}", failures)
    if "G-021 remains PARTIAL" not in force_close.get("scopeBoundary", {}).get("notClosed", ""):
        fail("BPI force-close evidence must retain the incomplete G-021 boundary", failures)

    outage_recovery = json.loads(
        (ROOT / "metadata/bpi-wms-outage-recovery-target-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    outage_target = outage_recovery.get("target", {})
    outage_fixture = outage_recovery.get("fixture", {})
    first_delivery = outage_recovery.get("firstDelivery", {})
    outage_browser = outage_recovery.get("browser", {})
    outage_kafka = outage_recovery.get("kafka", {})
    outage_cleanup = outage_recovery.get("cleanup", {})
    if (outage_recovery.get("status") != "PASS_TARGET_OUTAGE_RECOVERY_CLEANED"
            or outage_recovery.get("database") != "PostgreSQL"
            or outage_target.get("host") != "10.11.100.17"
            or outage_target.get("composeProject") != "adp-mes-newbase"
            or not outage_fixture.get("batchId")
            or not outage_fixture.get("commandEventId")
            or not outage_fixture.get("wmsIdempotencyKey")
            or len(outage_fixture.get("payloadSha256", "")) != 64):
        fail("BPI WMS outage recovery target or fixture evidence is incomplete", failures)
    if (first_delivery.get("materialServiceState") != "exited"
            or first_delivery.get("outbox") != "PUBLISHED|3|1|0"
            or sum(first_delivery.get("commandDlqDelta", {}).values()) != 1
            or first_delivery.get("commandEventHeaderVerified") is not True
            or first_delivery.get("materialDocumentRows") != 0):
        fail("BPI WMS outage first-delivery fail-closed evidence is incomplete", failures)
    outage_after = outage_browser.get("after", {})
    outage_batch = outage_after.get("batch", {})
    outage_wms = outage_after.get("wmsInbound", {})
    outage_errors = outage_browser.get("browser", {})
    if (outage_browser.get("status") != "PASS_BROWSER_API_DURABLE_RECEIPT"
            or outage_browser.get("loginStatus") != 200
            or outage_browser.get("reconciliation", {}).get("status") != 200
            or outage_browser.get("commandEventId") != outage_fixture.get("commandEventId")
            or outage_browser.get("wmsIdempotencyKey") != outage_fixture.get("wmsIdempotencyKey")
            or outage_batch.get("state") != "INBOUNDED"
            or outage_batch.get("revision") != 4
            or outage_wms.get("status") != "ACCEPTED"
            or outage_wms.get("revision") != 3
            or outage_wms.get("deliveryAttemptCount") != 2
            or outage_wms.get("reconciliationCount") != 1
            or not outage_wms.get("documentId")
            or any(outage_errors.get(key) for key in (
                "consoleErrors", "pageErrors", "requestFailures", "bpiHttpErrors"))):
        fail("BPI WMS outage recovery browser or durable-receipt evidence is incomplete", failures)
    outage_deltas = outage_kafka.get("deltas", {})
    outage_lag = outage_kafka.get("consumerLag", {})
    if (outage_deltas != {"command": 2, "commandDlq": 1, "receipt": 1}
            or outage_lag.get("wms") != 0
            or outage_lag.get("receipt") != 0):
        fail("BPI WMS outage recovery Kafka offsets or final lag are incomplete", failures)
    outage_persistence = outage_recovery.get("persistence", {})
    bpi_evidence = str(outage_persistence.get("bpi", ""))
    material_evidence = str(outage_persistence.get("material", ""))
    for fragment in (
            '"batchState" : "INBOUNDED"',
            '"manualRetryCount" : 1',
            '"totalAttemptCount" : 2',
            '"outboxRows" : 1'):
        if fragment not in bpi_evidence:
            fail(f"BPI WMS outage PostgreSQL evidence is missing: {fragment}", failures)
    for fragment in (
            '"documents" : 1',
            '"lines" : 1',
            '"transactions" : 1',
            '"stockRows" : 1',
            '"onHandQuantity" : 12.345000'):
        if fragment not in material_evidence:
            fail(f"BPI WMS outage material PostgreSQL evidence is missing: {fragment}", failures)
    if (outage_cleanup.get("environmentRestored") is not True
            or outage_cleanup.get("servicesRestored") is not True
            or outage_cleanup.get("residualRows") != 0
            or outage_cleanup.get("errors") != []):
        fail("BPI WMS outage recovery cleanup evidence is incomplete", failures)
    outage_screenshot = ROOT / outage_browser.get("screenshot", "")
    if not outage_screenshot.is_file():
        fail("BPI WMS outage recovery screenshot is missing", failures)
    elif hashlib.sha256(outage_screenshot.read_bytes()).hexdigest() != outage_browser.get(
            "screenshotSha256"):
        fail("BPI WMS outage recovery screenshot hash does not match", failures)

    if failures:
        print("\n".join(f"ERROR: {item}" for item in failures), file=sys.stderr)
        return 1
    print("BPI service structure, PostgreSQL ownership, and shadow-only boundaries verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
