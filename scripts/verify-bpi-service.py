#!/usr/bin/env python3
from __future__ import annotations

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
    "deploy/docker/scripts/adp-bpi-version-lifecycle-acceptance.js",
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

    if failures:
        print("\n".join(f"ERROR: {item}" for item in failures), file=sys.stderr)
        return 1
    print("BPI service structure, PostgreSQL ownership, and shadow-only boundaries verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
