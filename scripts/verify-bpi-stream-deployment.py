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
    "deploy/bpi-streaming/scripts/run-postgres-replay.sh",
    "deploy/bpi-runtime/scripts/browser-joint-acceptance.js",
    "deploy/bpi-runtime/sql/joint-acceptance-seed.sql",
    "deploy/bpi-runtime/sql/joint-acceptance-verify.sql",
    "deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql",
    "docs/testing/bpi-test-environment-deployment-readiness.md",
    "docs/testing/bpi-browser-kafka-postgres-joint-acceptance.md",
    "docs/testing/bpi-kafka-postgres-replay-acceptance.md",
    "metadata/bpi-test-host-capacity-preflight.json",
    "metadata/bpi-kafka-postgres-replay-acceptance.json",
    "metadata/bpi-test-environment-acceptance.json",
    "metadata/bpi-browser-kafka-postgres-joint-acceptance.json",
    "backend/source-modules/mes-production-context-outbox/README.md",
    "deploy/docker/postgres/init/176-wom-bpi-production-context-outbox.sql",
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

    topic_script = (ROOT / "deploy/bpi-streaming/scripts/create-topics.sh").read_text(encoding="utf-8")
    if "bpi.batch.candidate.dlq.v1" not in topic_script:
        failures.append("BPI topic initialization must create the candidate DLQ")
    if "bpi.boundary.rule-application.v1" not in topic_script:
        failures.append("BPI topic initialization must create the rule application topic")
    if "bpi.boundary.rule-application.dlq.v1" not in topic_script:
        failures.append("BPI topic initialization must create the rule application DLQ")

    smoke_script = (ROOT / "deploy/bpi-streaming/scripts/smoke-cluster.sh").read_text(
        encoding="utf-8"
    )
    if '--topic "$topic" </dev/null' not in smoke_script:
        failures.append(
            "BPI cluster smoke must isolate kafka-topics stdin so every configured topic is checked"
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

    cleanup = (
        ROOT / "deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql"
    ).read_text(encoding="utf-8")
    for marker in (
        "BEGIN;",
        "created_by = :'marker'",
        "order_id = 'MO-' || :'marker'",
        "remaining",
        "COMMIT;",
    ):
        if marker not in cleanup:
            failures.append(f"BPI joint cleanup is missing scope marker: {marker}")

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
