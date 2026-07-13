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
    "deploy/bpi-streaming/scripts/run-postgres-replay.sh",
    "docs/testing/bpi-test-environment-deployment-readiness.md",
    "docs/testing/bpi-kafka-postgres-replay-acceptance.md",
    "metadata/bpi-test-host-capacity-preflight.json",
    "metadata/bpi-kafka-postgres-replay-acceptance.json",
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
        "BPI_CANDIDATE_DLQ_TOPIC",
        "BPI_RULE_APPLICATION_TOPIC",
        "BPI_RULE_APPLICATION_DLQ_TOPIC",
        "127.0.0.1",
    ):
        if marker not in compose:
            failures.append(f"BPI streaming Compose is missing marker: {marker}")

    if "deploy/docker/docker-compose.yml" in compose:
        failures.append("BPI streaming Compose must not include the legacy ADP Compose file")
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
