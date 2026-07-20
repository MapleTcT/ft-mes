#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REQUIRED_FILES = [
    "deploy/bpi-streaming/.env.example",
    "deploy/bpi-streaming/docker-compose.yml",
    "deploy/bpi-streaming/README.md",
    "deploy/bpi-streaming/scripts/create-topics.sh",
    "deploy/bpi-streaming/scripts/preflight.sh",
    "deploy/bpi-streaming/scripts/smoke-cluster.sh",
    "deploy/bpi-streaming/scripts/run-replay.sh",
    "deploy/bpi-streaming/scripts/run-joint-replay.sh",
    "deploy/bpi-streaming/scripts/run-rule-deactivation.sh",
    "deploy/bpi-streaming/scripts/run-rule-lifecycle-evidence.sh",
    "deploy/bpi-streaming/scripts/run-postgres-replay.sh",
    "deploy/bpi-streaming/scripts/start-jobmanager.sh",
    "deploy/bpi-streaming/scripts/capture-upgrade-savepoint.sh",
    "deploy/bpi-streaming/scripts/restore-from-savepoint.sh",
    "deploy/bpi-streaming/scripts/verify-savepoint-restore.sh",
    "deploy/bpi-runtime/scripts/browser-joint-acceptance.js",
    "deploy/bpi-runtime/scripts/browser-live-batch-governance-acceptance.js",
    "deploy/bpi-runtime/scripts/browser-topology-rule-acceptance.js",
    "deploy/bpi-runtime/scripts/browser-point-catalog-acceptance.js",
    "deploy/bpi-runtime/scripts/upgrade-expand-only.sh",
    "deploy/bpi-runtime/sql/joint-acceptance-seed.sql",
    "deploy/bpi-runtime/sql/joint-acceptance-verify.sql",
    "deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql",
    "deploy/bpi-runtime/sql/live-batch-rule-simulation-seed.sql",
    "deploy/bpi-runtime/sql/live-batch-rule-simulation-cleanup.sql",
    "docs/testing/bpi-test-environment-deployment-readiness.md",
    "docs/testing/bpi-browser-kafka-postgres-joint-acceptance.md",
    "docs/testing/bpi-kafka-postgres-replay-acceptance.md",
    "docs/testing/bpi-target-topology-rule-acceptance.md",
    "docs/testing/bpi-point-catalog-readiness-acceptance.md",
    "docs/testing/bpi-point-catalog-kafka-sync-acceptance.md",
    "metadata/bpi-test-host-capacity-preflight.json",
    "metadata/bpi-kafka-postgres-replay-acceptance.json",
    "metadata/bpi-test-environment-acceptance.json",
    "metadata/bpi-browser-kafka-postgres-joint-acceptance.json",
    "metadata/bpi-target-topology-rule-acceptance.json",
    "metadata/bpi-point-catalog-readiness-acceptance.json",
    "metadata/bpi-point-catalog-kafka-sync-acceptance.json",
    "backend/source-modules/mes-production-context-outbox/README.md",
    "deploy/docker/postgres/init/176-wom-bpi-production-context-outbox.sql",
    "deploy/docker/postgres/init/177-wom-bpi-context-revision-clock-floor.sql",
]


def main() -> int:
    failures: list[str] = []
    for relative in REQUIRED_FILES:
        if not (ROOT / relative).is_file():
            failures.append(f"missing required BPI streaming deployment file: {relative}")

    if failures:
        return report(failures)

    compose = (ROOT / "deploy/bpi-streaming/docker-compose.yml").read_text(encoding="utf-8")
    for marker in (
        "apache/kafka:4.2.0",
        "flink:2.2.1-scala_2.12-java17",
        "KAFKA_PROCESS_ROLES: broker,controller",
        "KAFKA_AUTO_CREATE_TOPICS_ENABLE: \"false\"",
        "KAFKA_MIN_INSYNC_REPLICAS: \"2\"",
        "state.backend.type: rocksdb",
        "execution.checkpointing.dir: s3://",
        "flink-s3-fs-presto-2.2.1.jar",
        "com.mapletct.ftmes.bpi.stream.BpiKafkaJob",
        "com.mapletct.ftmes.bpi.stream.BpiKafkaAcceptanceReplay",
        "com.mapletct.ftmes.bpi.stream.BpiJointAcceptanceReplay",
        "BPI_CANDIDATE_DLQ_TOPIC",
        "BPI_RULE_APPLICATION_TOPIC",
        "BPI_RULE_APPLICATION_DLQ_TOPIC",
        "BPI_RULE_RUNTIME_READINESS_TOPIC",
        "BPI_RULE_RUNTIME_READINESS_DLQ_TOPIC",
        "BPI_FLINK_RESTORE_SAVEPOINT_PATH",
        "start-jobmanager.sh",
        "mes-production-context-outbox",
        "MES_CONTEXT_OUTBOX_ENABLED",
        "ADP_RUNTIME_NETWORK_NAME",
        "127.0.0.1",
    ):
        if marker not in compose:
            failures.append(f"BPI streaming Compose is missing marker: {marker}")

    if "deploy/docker/docker-compose.yml" in compose:
        failures.append("BPI streaming Compose must not include the legacy ADP Compose file")
    if "profiles: [mes-context]" not in compose:
        failures.append("MES context publisher must remain behind the explicit mes-context profile")
    if "MES_CONTEXT_OUTBOX_ENABLED: ${MES_CONTEXT_OUTBOX_ENABLED:-false}" not in compose:
        failures.append("MES context publisher must remain disabled by default")
    if "docker.sock" in compose or "privileged: true" in compose:
        failures.append("BPI streaming Compose must not mount Docker or use privileged containers")

    preflight = (ROOT / "deploy/bpi-streaming/scripts/preflight.sh").read_text(encoding="utf-8")
    for marker in (
        "BPI_MIN_FREE_DISK_GB",
        "deploymentStarted",
        "destructiveActionsPerformed",
        "Docker 20.10.4 or newer",
        "load_env_file",
    ):
        if marker not in preflight:
            failures.append(f"BPI preflight is missing safety marker: {marker}")
    for forbidden in ("docker system prune", "docker volume prune", "rm -rf"):
        if forbidden in preflight:
            failures.append(f"BPI preflight contains destructive command: {forbidden}")

    jobmanager_entrypoint = (
        ROOT / "deploy/bpi-streaming/scripts/start-jobmanager.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        "--fromSavepoint",
        "--allowNonRestoredState",
        "BPI_FLINK_RESTORE_SAVEPOINT_PATH",
        "BPI_FLINK_ALLOW_NON_RESTORED_STATE",
        "s3://*/savepoints/*",
    ):
        if marker not in jobmanager_entrypoint:
            failures.append(f"BPI jobmanager restore entrypoint is missing marker: {marker}")

    savepoint_capture = (
        ROOT / "deploy/bpi-streaming/scripts/capture-upgrade-savepoint.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        "exactly one RUNNING BPI Flink job",
        "--under-replicated-partitions",
        "-type canonical",
        '"jobCancelled": False',
        '"destructiveActionsPerformed": False',
    ):
        if marker not in savepoint_capture:
            failures.append(f"BPI savepoint capture is missing safety marker: {marker}")
    for forbidden in (" cancel ", " stop ", "docker compose down", "docker volume"):
        if forbidden in savepoint_capture:
            failures.append(f"BPI savepoint capture contains destructive marker: {forbidden.strip()}")

    savepoint_restore = (
        ROOT / "deploy/bpi-streaming/scripts/restore-from-savepoint.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        "BPI_STREAM_RESTORE_CONFIRM",
        "RESTORE_BPI_FLINK_FROM_SAVEPOINT",
        "--force-recreate bpi-jobmanager bpi-taskmanager",
        "verify-savepoint-restore.sh",
    ):
        if marker not in savepoint_restore:
            failures.append(f"BPI savepoint restore is missing safety marker: {marker}")
    for forbidden in ("kafka-1 kafka-2 kafka-3", "bpi-minio", "docker compose down", "docker volume"):
        if forbidden in savepoint_restore:
            failures.append(f"BPI savepoint restore crosses persistence boundary: {forbidden}")

    savepoint_verify = (
        ROOT / "deploy/bpi-streaming/scripts/verify-savepoint-restore.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        'restored.get("external_path")',
        'completed.get("trigger_timestamp")',
        "JOB_START_TIME",
        "Kafka point-catalog source",
        "runtime-readiness sink",
        "smoke-cluster.sh",
        '"postRestoreCheckpointId"',
        '"postRestoreCheckpointTriggerTime"',
    ):
        if marker not in savepoint_verify:
            failures.append(f"BPI savepoint verification is missing marker: {marker}")

    runtime_upgrade = (
        ROOT / "deploy/bpi-runtime/scripts/upgrade-expand-only.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        "BPI_RUNTIME_UPGRADE_CONFIRM",
        "UPGRADE_BPI_RUNTIME_EXPAND_ONLY",
        "pg_dump -Fc",
        "docker image tag",
        "--exit-code-from bpi-migrate",
        "BPI_EXPECTED_FLYWAY_VERSION",
        "BPI_RUNTIME_UPGRADE_HEALTH_TIMEOUT_SECONDS",
        "wait_for_service_health",
        '"recoveryRequired"',
        "MIGRATION_APPLIED",
        '"strategy": "EXPAND_ONLY"',
        '"schemaDowngrade": False',
        "smoke.sh",
    ):
        if marker not in runtime_upgrade:
            failures.append(f"BPI runtime expand-only upgrade is missing marker: {marker}")
    for forbidden in ("drop database", "drop schema", "docker volume", "rm -rf"):
        if forbidden in runtime_upgrade.lower():
            failures.append(f"BPI runtime expand-only upgrade contains destructive marker: {forbidden}")

    runtime_smoke = (ROOT / "deploy/bpi-runtime/scripts/smoke.sh").read_text(
        encoding="utf-8"
    )
    for marker in (
        "BPI_RUNTIME_SMOKE_CONNECT_TIMEOUT_SECONDS",
        "BPI_RUNTIME_SMOKE_REQUEST_TIMEOUT_SECONDS",
        "--connect-timeout",
        "--max-time",
    ):
        if marker not in runtime_smoke:
            failures.append(f"BPI runtime smoke is missing bounded request marker: {marker}")

    topic_script = (ROOT / "deploy/bpi-streaming/scripts/create-topics.sh").read_text(encoding="utf-8")
    if "bpi.batch.candidate.dlq.v1" not in topic_script:
        failures.append("BPI topic initialization must create the candidate DLQ")
    if "bpi.boundary.rule-application.v1" not in topic_script:
        failures.append("BPI topic initialization must create the rule application topic")
    if "bpi.boundary.rule-application.dlq.v1" not in topic_script:
        failures.append("BPI topic initialization must create the rule application DLQ")
    if "bpi.boundary.rule-runtime-readiness.v1" not in topic_script:
        failures.append("BPI topic initialization must create the runtime readiness topic")
    if "bpi.boundary.rule-runtime-readiness.dlq.v1" not in topic_script:
        failures.append("BPI topic initialization must create the runtime readiness DLQ")
    for marker in (
        "iot.point-catalog.snapshot.v1",
        "iot.point-catalog.snapshot.dlq.v1",
        "KAFKA_CONFIGS_COMMAND",
        "max.message.bytes=$POINT_CATALOG_MAX_MESSAGE_BYTES",
    ):
        if marker not in topic_script:
            failures.append(f"BPI topic initialization is missing point catalog marker: {marker}")
    for marker in (
        "iot.source-sequence.evidence.v1",
        "iot.source-sequence.evidence.dlq.v1",
        "cleanup.policy=compact",
    ):
        if marker not in topic_script:
            failures.append(f"BPI topic initialization is missing source sequence marker: {marker}")

    smoke_script = (ROOT / "deploy/bpi-streaming/scripts/smoke-cluster.sh").read_text(
        encoding="utf-8"
    )
    if '--topic "$topic" </dev/null' not in smoke_script:
        failures.append(
            "BPI cluster smoke must isolate kafka-topics stdin so every configured topic is checked"
        )
    if '"topics": 14' not in smoke_script:
        failures.append("BPI cluster smoke must report all fourteen configured topics")
    for marker in (
        "kafka-configs.sh",
        "POINT_CATALOG_CONFIGS",
        "pointCatalogConfigEvidence",
        "SOURCE_SEQUENCE_CONFIGS",
        "sourceSequenceConfigEvidence",
    ):
        if marker not in smoke_script:
            failures.append(
                f"BPI cluster smoke must verify point catalog topic config directly: {marker}"
            )

    postgres_replay = (
        ROOT / "deploy/bpi-streaming/scripts/run-postgres-replay.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        "TENANT=TENANT-E2E",
        "BPI_PERSISTENCE_REPLAY_KEEP_MARKER",
        "DLQ_BEFORE",
        "candidateCount",
        "bpi.bpi_inbox_events",
        "bpi.bpi_batch_candidates",
        "cleanup_marker",
    ):
        if marker not in postgres_replay:
            failures.append(f"BPI PostgreSQL replay is missing marker: {marker}")

    joint_replay = (
        ROOT / "deploy/bpi-streaming/scripts/run-joint-replay.sh"
    ).read_text(encoding="utf-8")
    joint_replay += (
        ROOT
        / "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiJointAcceptanceReplay.java"
    ).read_text(encoding="utf-8")
    for marker in (
        "BPI_JOINT_MARKER",
        "BPI_JOINT_TENANT_ID",
        "BPI_JOINT_PLANT_ID",
        "BPI_JOINT_LINE_ID",
        "BPI_BROWSER_PUBLICATION_OUTBOX",
        "matchingDataQualityIssues",
    ):
        if marker not in joint_replay:
            failures.append(f"BPI joint replay is missing safety marker: {marker}")

    joint_seed = (
        ROOT / "deploy/bpi-runtime/sql/joint-acceptance-seed.sql"
    ).read_text(encoding="utf-8")
    for marker in (
        "BEGIN;",
        "disabled_feature_flag_guard",
        "AND NOT enabled",
        "WHERE NOT EXISTS",
        "existing.flag_key = required.required_flag",
        "'productId', :'product_id'",
        "COMMIT;",
    ):
        if marker not in joint_seed:
            failures.append(f"BPI joint seed is missing non-overwrite marker: {marker}")

    deactivation = (
        ROOT / "deploy/bpi-streaming/scripts/run-rule-deactivation.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        "BPI_DEACTIVATE_MARKER",
        "BPI_DEACTIVATE_RULE_CODE",
        "BPI_DEACTIVATE_RULE_VERSION",
        "inactivePublication",
        "APPLIED",
    ):
        if marker not in deactivation:
            failures.append(f"BPI rule deactivation is missing safety marker: {marker}")

    lifecycle_evidence = (
        ROOT / "deploy/bpi-streaming/scripts/run-rule-lifecycle-evidence.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        "BpiRuleLifecycleEvidence",
        "--no-deps",
        "BPI_LIFECYCLE_EVIDENCE_RULE_CODE",
        "BPI_LIFECYCLE_EVIDENCE_RULE_VERSION",
        "runtimeReadyThenInactive",
    ):
        if marker not in lifecycle_evidence:
            failures.append(f"BPI read-only lifecycle evidence is missing marker: {marker}")
    for forbidden in ("BpiRuleDeactivationReplay", "docker compose down", "docker volume"):
        if forbidden in lifecycle_evidence:
            failures.append(
                f"BPI read-only lifecycle evidence contains mutating marker: {forbidden}"
            )

    data_quality_replay = (
        ROOT / "deploy/bpi-streaming/scripts/run-data-quality-flink-replay.sh"
    ).read_text(encoding="utf-8")
    for marker in (
        "BpiDataQualityFlinkReplay",
        "--no-deps",
        "BPI_DQ_REPLAY_TENANT_ID",
        "BPI_DQ_REPLAY_PLANT_ID",
        "BPI_DQ_REPLAY_LINE_ID",
        "BPI_DQ_REPLAY_JOB_JAR",
        'env BPI_JOB_JAR="$REPLAY_JOB_JAR"',
        "replay.get(\"scope\") != expected_scope",
        "jobIdUnchangedDuringReplay",
        "checkpoint ID regressed",
    ):
        if marker not in data_quality_replay:
            failures.append(f"BPI data-quality replay is missing safety marker: {marker}")

    cleanup = (
        ROOT / "deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql"
    ).read_text(encoding="utf-8")
    for marker in (
        "BEGIN;",
        "created_by = :'marker'",
        "\\set order_id 'MO-' :marker",
        "order_id = :'order_id'",
        "runtime_readiness_event_id",
        "remaining",
        "COMMIT;",
    ):
        if marker not in cleanup:
            failures.append(f"BPI joint cleanup is missing scope marker: {marker}")

    joint_browser = (
        ROOT / "deploy/bpi-runtime/scripts/browser-joint-acceptance.js"
    ).read_text(encoding="utf-8")
    for marker in (
        'new Set(["publish", "confirm", "read", "rule-read", "candidate-read", "candidate-absent"])',
        "BPI_ACCEPTANCE_LINE_ID",
        "readCandidate",
        "readCandidateAbsence",
        "drawerBounds",
        "candidate drawer did not expose one actionable PENDING candidate",
        "BPI_ACCEPTANCE_EXPECTED_RUNTIME_STATUS",
        "BPI_ACCEPTANCE_EXPECTED_PUBLISH_STATUS",
        "BPI_ACCEPTANCE_EXPECTED_PUBLISH_DETAIL",
        "BPI_ACCEPTANCE_EXPECTED_PUBLISH_TOAST",
        "publishResponsePromise",
        "rule publication returned",
        "publication toast exposed backend implementation details",
        "publicationBlocked",
        "expectedConsoleErrors",
        "captureExpectedRuntime",
        "runtimeReadinessText",
        "consoleErrors",
        "requestFailures",
    ):
        if marker not in joint_browser:
            failures.append(f"BPI joint browser acceptance is missing marker: {marker}")

    live_batch_browser = (
        ROOT / "deploy/bpi-runtime/scripts/browser-live-batch-governance-acceptance.js"
    ).read_text(encoding="utf-8")
    for marker in (
        "feature-enable",
        "feature-inherit",
        "topology-publish",
        "rule-publish",
        "rules-retire",
        "independentToken",
        "creator topology publication was not rejected",
        "PUBLISHED/APPLIED/READY",
        "运行时 INACTIVE",
        "consoleErrors",
        "requestFailures",
    ):
        if marker not in live_batch_browser:
            failures.append(f"BPI live-batch browser acceptance is missing marker: {marker}")

    live_batch_seed = (
        ROOT / "deploy/bpi-runtime/sql/live-batch-rule-simulation-seed.sql"
    ).read_text(encoding="utf-8")
    live_batch_cleanup = (
        ROOT / "deploy/bpi-runtime/sql/live-batch-rule-simulation-cleanup.sql"
    ).read_text(encoding="utf-8")
    for marker in (
        "BEGIN;",
        "duplicate_marker_guard",
        "TEST_ONLY_RULE_QUALIFICATION",
        "acceptance_marker",
        "COMMIT;",
    ):
        if marker not in live_batch_seed:
            failures.append(f"BPI live-batch simulation seed is missing marker: {marker}")
    for marker in (
        "BEGIN;",
        "created_by = :'marker'",
        "TEST_ONLY_RULE_QUALIFICATION",
        "remaining",
        "COMMIT;",
    ):
        if marker not in live_batch_cleanup:
            failures.append(f"BPI live-batch simulation cleanup is missing marker: {marker}")

    runtime_admission = (
        ROOT
        / "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRulePublicationMapper.java"
    ).read_text(encoding="utf-8")
    runtime_admission += (
        ROOT
        / "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRuleRoutingBroadcastFunction.java"
    ).read_text(encoding="utf-8")
    for marker in (
        "getPointCatalogSnapshotId",
        "getCalibrationEvidenceId",
        "getCalibrationValidUntilMs",
        "POINT_CALIBRATION_EVIDENCE_EXPIRED",
    ):
        if marker not in runtime_admission:
            failures.append(f"BPI runtime admission is missing MES evidence marker: {marker}")

    joint_verify = (
        ROOT / "deploy/bpi-runtime/sql/joint-acceptance-verify.sql"
    ).read_text(encoding="utf-8")
    for marker in (
        "runtime_readiness_status",
        "runtime_readiness_reason_code",
        "runtime_point_catalog_source_revision",
    ):
        if marker not in joint_verify:
            failures.append(f"BPI joint verification SQL is missing runtime marker: {marker}")

    productization_browser = (
        ROOT / "deploy/bpi-runtime/scripts/browser-topology-rule-acceptance.js"
    ).read_text(encoding="utf-8")
    for marker in (
        'new Set(["author", "finalize", "read"])',
        "creator publication must return 422",
        "expectedConsoleErrors",
        "location.url.includes",
        "createRuleResponse.status() !== 200",
        "consoleErrors",
        "requestFailures",
        "ADP_PASSWORD",
    ):
        if marker not in productization_browser:
            failures.append(f"BPI topology/rule browser acceptance is missing marker: {marker}")

    point_catalog_browser = (
        ROOT / "deploy/bpi-runtime/scripts/browser-point-catalog-acceptance.js"
    ).read_text(encoding="utf-8")
    for marker in (
        'new Set(["write", "read", "sync-read", "sync-validate"])',
        "BPI_EXPECTED_POINT_ISSUES",
        "expectedAutomaticTopologyErrors",
        "Idempotent-Replay",
        "POINT_DEVICE_NOT_REGISTERED",
        "POINT_DEVICE_NOT_ACTIVE",
        "POINT_PROPERTY_NOT_AVAILABLE",
        "POINT_CALIBRATION_NOT_VERIFIED",
        "POINT_SOURCE_SEQUENCE_DISABLED",
        "publishAllowed = false",
        "readPersistedAcceptance",
        "ADP_PASSWORD",
    ):
        if marker not in point_catalog_browser:
            failures.append(f"BPI point-catalog browser acceptance is missing marker: {marker}")

    evidence = json.loads(
        (ROOT / "metadata/bpi-test-host-capacity-preflight.json").read_text(encoding="utf-8")
    )
    if evidence.get("status") != "BLOCKED_DISK":
        failures.append("test-host capacity evidence must remain BLOCKED_DISK until a live rerun passes")
    if evidence.get("deploymentStarted") is not False:
        failures.append("capacity evidence cannot claim that deployment started")
    if evidence.get("destructiveActionsPerformed") is not False:
        failures.append("capacity evidence cannot claim destructive cleanup")
    if evidence.get("thresholds", {}).get("minimumFreeDiskGiB", 0) < 25:
        failures.append("BPI streaming free-disk gate must be at least 25 GiB")

    persistence = json.loads(
        (ROOT / "metadata/bpi-kafka-postgres-replay-acceptance.json").read_text(encoding="utf-8")
    )
    if persistence.get("status") != "HARNESS_READY_CLUSTER_BLOCKED_DISK":
        failures.append("Kafka/Flink/PostgreSQL replay must remain blocked until a live target rerun passes")
    if persistence.get("targetEvidence", {}).get("destructiveActionsPerformed") is not False:
        failures.append("Kafka/Flink/PostgreSQL replay cannot claim destructive cleanup")

    target_acceptance = json.loads(
        (ROOT / "metadata/bpi-test-environment-acceptance.json").read_text(encoding="utf-8")
    )
    if target_acceptance.get("status") != "PASS_PHASE1_CONTROLLED":
        failures.append("target BPI acceptance must record PASS_PHASE1_CONTROLLED")
    target_checks = {
        check.get("id"): check.get("status")
        for check in target_acceptance.get("checks", [])
        if isinstance(check, dict)
    }
    if target_checks.get("browser-to-batch-persistence-write-chain") != "PASS":
        failures.append("target BPI acceptance must pass the browser-to-batch write chain")
    if target_checks.get("topology-rule-productization-target") != "PASS":
        failures.append("target BPI acceptance must pass topology/rule productization on V9")

    productization_target = json.loads(
        (ROOT / "metadata/bpi-target-topology-rule-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if productization_target.get("status") != "PASS":
        failures.append("target BPI topology/rule productization acceptance must be PASS")
    if productization_target.get("summary") != {
        "checks": 10,
        "pass": 10,
        "fail": 0,
        "blocked": 0,
    }:
        failures.append("target BPI topology/rule productization must remain 10/10 PASS")
    if productization_target.get("environment", {}).get("flywayVersion") != 9:
        failures.append("target BPI topology/rule productization must retain Flyway V9 evidence")
    if productization_target.get("scope", {}).get("marker") != (
        "ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET"
    ):
        failures.append("target BPI topology/rule productization must retain the verified marker")
    productization_boundaries = " ".join(productization_target.get("boundaries", []))
    for forbidden in ("password=", "token=", "cookie=", "BEGIN PRIVATE KEY"):
        if forbidden.lower() in productization_boundaries.lower():
            failures.append(
                f"target BPI topology/rule acceptance leaks a secret marker: {forbidden}"
            )

    point_catalog_target = json.loads(
        (ROOT / "metadata/bpi-point-catalog-readiness-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if point_catalog_target.get("status") != "PASS_CONTROL_WITH_BLOCKED_SOURCE":
        failures.append("target BPI point-catalog acceptance must preserve the split control/source status")
    environment = point_catalog_target.get("environment", {})
    if environment.get("flywayVersion") != 12 or environment.get("schemaTableCount") != 21:
        failures.append("target BPI point-catalog acceptance must retain Flyway V12 and 21 tables")
    if point_catalog_target.get("marker") != "ADP_E2E_20260715_POINTCAT_02":
        failures.append("target BPI point-catalog acceptance must retain the verified marker")
    persistence = point_catalog_target.get("persistence", {})
    snapshot = persistence.get("snapshot", {})
    if snapshot.get("pointCount") != 1 or snapshot.get("readyPointCount") != 0:
        failures.append("target BPI point-catalog evidence must retain one blocked source point")
    if snapshot.get("duplicateCountAfterReplay") != 1:
        failures.append("target BPI point-catalog replay must not duplicate its snapshot")
    topology = persistence.get("topology", {})
    if topology.get("validationStatus") != "FAILED" or topology.get("publishAllowed") is not False:
        failures.append("target BPI blocked topology must remain failed and unpublishable")
    if set(topology.get("errors", [])) != {
        "POINT_DEVICE_NOT_REGISTERED",
        "POINT_DEVICE_NOT_ACTIVE",
        "POINT_PROPERTY_NOT_AVAILABLE",
        "POINT_CALIBRATION_NOT_VERIFIED",
    }:
        failures.append("target BPI blocked topology must retain all four readiness errors")
    if topology.get("warnings") != ["POINT_SOURCE_SEQUENCE_DISABLED"]:
        failures.append("target BPI blocked topology must retain the source-sequence warning")
    conclusion = point_catalog_target.get("conclusion", {})
    if conclusion.get("control") != "PASS" or conclusion.get("sourceReadiness") != "BLOCKED":
        failures.append("target BPI point-catalog conclusion must not promote the blocked source")
    point_catalog_serialized = json.dumps(point_catalog_target).lower()
    for forbidden in ('"password"', '"token"', '"cookie"', "begin private key"):
        if forbidden in point_catalog_serialized:
            failures.append(f"target BPI point-catalog acceptance leaks a secret marker: {forbidden}")

    point_catalog_sync = json.loads(
        (ROOT / "metadata/bpi-point-catalog-kafka-sync-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if point_catalog_sync.get("status") != "PASS_CONTROL_WITH_BLOCKED_SOURCE":
        failures.append("automatic point-catalog sync must preserve the split control/source status")
    if point_catalog_sync.get("summary") != {
        "checks": 9,
        "pass": 8,
        "fail": 0,
        "blocked": 1,
    }:
        failures.append("automatic point-catalog sync must retain 8 PASS and 1 BLOCKED check")
    sync_environment = point_catalog_sync.get("environment", {})
    if sync_environment.get("configuredTopics") != 10:
        failures.append("automatic point-catalog sync must retain all ten configured topics")
    sync_scope = point_catalog_sync.get("scope", {})
    if sync_scope.get("tenantId") != "1000" or sync_scope.get("plantId") != "PLANT-01":
        failures.append("automatic point-catalog sync must retain the verified ADP scope")
    if sync_scope.get("lineId") != "LINE-S07-01":
        failures.append("automatic point-catalog sync must retain the verified production line")
    if sync_scope.get("sourceRevision") != (
        "sha256:2a218d12d6ed8bea024c38f6d2e06656f20703fadf920256dc98b17c2f151ce5"
    ):
        failures.append("automatic point-catalog sync must retain the verified source revision")
    sync_persistence = point_catalog_sync.get("persistence", {})
    if sync_persistence.get("snapshotCount") != 1 or sync_persistence.get("entryCount") != 1:
        failures.append("automatic point-catalog sync must retain exact snapshot and entry counts")
    if sync_persistence.get("readyPointCount") != 0:
        failures.append("automatic point-catalog sync cannot promote the blocked source point")
    sync_serialized = json.dumps(point_catalog_sync).lower()
    for forbidden in ('"password"', '"token"', '"cookie"', "begin private key"):
        if forbidden in sync_serialized:
            failures.append(f"automatic point-catalog sync acceptance leaks a secret marker: {forbidden}")

    joint_acceptance = json.loads(
        (ROOT / "metadata/bpi-browser-kafka-postgres-joint-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if joint_acceptance.get("status") != "PASS":
        failures.append("BPI browser/Kafka/PostgreSQL joint acceptance must be PASS")
    if joint_acceptance.get("summary") != {
        "checks": 11,
        "pass": 11,
        "fail": 0,
        "blocked": 0,
    }:
        failures.append("BPI joint acceptance summary must remain 11/11 PASS")
    if joint_acceptance.get("scope", {}).get("marker") != "ADP_E2E_20260714_091536_BPI_JOINT":
        failures.append("BPI joint acceptance must retain the verified marker")
    boundaries = " ".join(joint_acceptance.get("boundaries", []))
    for forbidden in ("password=", "token=", "cookie=", "BEGIN PRIVATE KEY"):
        if forbidden.lower() in boundaries.lower():
            failures.append(f"BPI joint acceptance leaks a secret marker: {forbidden}")

    return report(failures)


def report(failures: list[str]) -> int:
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        return 1
    print("BPI streaming deployment assets verified")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
