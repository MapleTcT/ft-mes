#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
STREAMING = ROOT / "streaming"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

REQUIRED_FILES = [
    "streaming/pom.xml",
    "streaming/README.md",
    "streaming/bpi-stream-engine/pom.xml",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryCandidateProjector.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryKeyedBroadcastFunction.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryOperatorStateCodec.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRulePublicationMapper.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRoutingControlCodec.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundarySignalRouter.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/PointCatalogKafkaDecodeFunction.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/PointCatalogRuntimeValidator.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/RuleRuntimeReadinessProjector.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/RuleRuntimeReadinessKafkaSerializationSchema.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/ProductionContextTimeline.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/ProductionContextJoinFunction.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/ProductionContextJoinStateCodec.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/ContextualTelemetryPointCodec.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiKafkaJob.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiKafkaJobConfig.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/TelemetryDataQualityFunction.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/TelemetryDataQualityIssue.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/TelemetrySequenceStateCodec.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiDataQualityFlinkReplay.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiKafkaAcceptanceReplay.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiKafkaAcceptanceReplayConfig.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiKafkaAcceptanceScenario.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRulePublicationLifecycleFunction.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRuleRoutingBroadcastFunction.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRuleUpdateCodec.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryReplayEngine.java",
    "services/bpi-service/batch-rule-runtime/src/main/java/com/mapletct/ftmes/bpi/rules/BoundaryTimingPolicy.java",
    "streaming/bpi-stream-engine/src/test/java/com/mapletct/ftmes/bpi/stream/BoundaryKeyedBroadcastHarnessTest.java",
    "streaming/bpi-stream-engine/src/test/java/com/mapletct/ftmes/bpi/stream/BoundaryReplayEngineTest.java",
    "streaming/bpi-stream-engine/src/test/java/com/mapletct/ftmes/bpi/stream/BpiKafkaAcceptanceScenarioTest.java",
    "streaming/bpi-stream-engine/src/test/java/com/mapletct/ftmes/bpi/stream/BpiRuleApplicationFlinkKafkaAcceptanceTest.java",
    "streaming/bpi-stream-engine/src/test/java/com/mapletct/ftmes/bpi/stream/TelemetryDataQualityFunctionTest.java",
    "streaming/bpi-stream-engine/src/test/java/com/mapletct/ftmes/bpi/stream/BpiDataQualityFlinkReplayTest.java",
    "deploy/bpi-streaming/scripts/run-rule-application-flink-acceptance.sh",
    "deploy/bpi-streaming/scripts/run-data-quality-flink-replay.sh",
    "docs/testing/bpi-flink-operator-acceptance.md",
    "docs/testing/bpi-rule-timing-acceptance.md",
    "docs/testing/bpi-rule-publication-routing-acceptance.md",
    "docs/testing/bpi-production-context-join-acceptance.md",
    "docs/testing/bpi-stream-replay-acceptance.md",
    "docs/testing/bpi-kafka-flink-topology-acceptance.md",
    "docs/testing/bpi-kafka-cluster-replay-acceptance.md",
    "metadata/bpi-flink-operator-acceptance.json",
    "metadata/bpi-rule-timing-acceptance.json",
    "metadata/bpi-rule-publication-routing-acceptance.json",
    "metadata/bpi-production-context-join-acceptance.json",
    "metadata/bpi-stream-replay-acceptance.json",
    "metadata/bpi-kafka-flink-topology-acceptance.json",
    "metadata/bpi-rule-application-flink-kafka-acceptance.json",
    "metadata/bpi-kafka-cluster-replay-acceptance.json",
]


def main() -> int:
    failures: list[str] = []
    for relative in REQUIRED_FILES:
        if not (ROOT / relative).is_file():
            failures.append(f"missing required BPI streaming file: {relative}")

    if failures:
        print("\n".join(f"ERROR: {item}" for item in failures), file=sys.stderr)
        return 1

    parent = ET.parse(STREAMING / "pom.xml").getroot()
    release = parent.findtext("m:properties/m:maven.compiler.release", namespaces=NS)
    flink_version = parent.findtext("m:properties/m:flink.version", namespaces=NS)
    kafka_connector_version = parent.findtext(
        "m:properties/m:flink.kafka.connector.version", namespaces=NS
    )
    kafka_version = parent.findtext("m:properties/m:kafka.version", namespaces=NS)
    modules = {item.text for item in parent.findall("m:modules/m:module", NS)}
    expected_modules = {
        "../contracts/bpi-events",
        "../services/bpi-service/batch-rule-runtime",
        "bpi-stream-engine",
    }
    if release != "17":
        failures.append(f"BPI streaming Java release must remain 17, found {release!r}")
    if flink_version != "2.2.1":
        failures.append(f"BPI streaming Flink baseline must remain 2.2.1, found {flink_version!r}")
    if kafka_connector_version != "5.0.0-2.2":
        failures.append(
            "BPI streaming Kafka connector baseline must remain 5.0.0-2.2, "
            f"found {kafka_connector_version!r}"
        )
    if kafka_version != "4.2.0":
        failures.append(
            f"BPI streaming Kafka runtime baseline must remain 4.2.0, found {kafka_version!r}"
        )
    if modules != expected_modules:
        failures.append(f"unexpected BPI streaming reactor modules: {sorted(modules)}")

    replay_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryReplayEngine.java").read_text(encoding="utf-8")
    for marker in ("BoundaryWindowEvaluator.onObservation", "advanceEventTime", "finalWatermark cannot precede"):
        if marker not in replay_source:
            failures.append(f"BoundaryReplayEngine is missing required behavior marker {marker!r}")

    projector_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryCandidateProjector.java").read_text(encoding="utf-8")
    for marker in ("CandidateKeyFactory.startKey", "CandidateKeyFactory.endKey", "BatchCandidateV1.newBuilder"):
        if marker not in projector_source:
            failures.append(f"BoundaryCandidateProjector is missing contract marker {marker!r}")

    operator_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryKeyedBroadcastFunction.java").read_text(encoding="utf-8")
    for marker in (
        "KeyedBroadcastProcessFunction",
        "ValueState<byte[]>",
        "registerEventTimeTimer",
        "MAX_BUFFERED_OBSERVATIONS",
        "recalculate(",
        "observationHistoryComplete",
        "withWindowAndObservations",
        "LATE_EVENT_REVISION_REQUIRED",
        "StateTtlConfig",
        "toByteArray",
    ):
        if marker not in operator_source:
            failures.append(f"BoundaryKeyedBroadcastFunction is missing runtime marker {marker!r}")

    publication_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRulePublicationMapper.java").read_text(encoding="utf-8")
    for marker in (
        "BoundaryRulePublicationV1",
        "BoundaryTimingPolicy",
        "duplicate product/device/property binding",
        "binding.getProductId()",
        "binding.getCalibrationVersion()",
    ):
        if marker not in publication_source:
            failures.append(f"BoundaryRulePublicationMapper is missing contract marker {marker!r}")

    router_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundarySignalRouter.java").read_text(encoding="utf-8")
    for marker in ("UNIT_MISMATCH", "CONTEXT_NOT_EFFECTIVE", "getBatchId", "getSampleTimeMs"):
        if marker not in router_source:
            failures.append(f"BoundarySignalRouter is missing routing marker {marker!r}")

    timeline_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/ProductionContextTimeline.java").read_text(encoding="utf-8")
    for marker in ("getContextRevision", "getEffectiveFromMs", "getEffectiveToMs", "getActive"):
        if marker not in timeline_source:
            failures.append(f"ProductionContextTimeline is missing point-in-time marker {marker!r}")

    join_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/ProductionContextJoinFunction.java").read_text(encoding="utf-8")
    for marker in ("KeyedCoProcessFunction", "CONTEXT_WAIT_EXPIRED", "registerEventTimeTimer", "ContextualTelemetryPointCodec.encode"):
        if marker not in join_source:
            failures.append(f"ProductionContextJoinFunction is missing join marker {marker!r}")

    job_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiKafkaJob.java").read_text(encoding="utf-8")
    for marker in (
        "CheckpointingMode.EXACTLY_ONCE",
        "bpi-production-context-join-v1",
        "bpi-rule-lifecycle-v1",
        "bpi-boundary-indexed-routing-v1",
        "bpi-kafka-point-catalog-source-v1",
        "bpi-telemetry-data-quality-v1",
        "BoundaryRoutingControlCodec::pointCatalog",
        "PointCatalogKafkaDecodeFunction.ISSUES",
        "BpiKafkaIO.candidateSink",
        "BpiKafkaIO.dataQualitySink",
        "BpiKafkaIO.ruleApplicationSink",
        "BpiKafkaIO.ruleRuntimeReadinessSink",
        "ruleWatermarks",
        "contextualWatermarks",
        "boundaryStateTtl",
    ):
        if marker not in job_source:
            failures.append(f"BpiKafkaJob is missing production topology marker {marker!r}")

    detector_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/TelemetryDataQualityFunction.java").read_text(encoding="utf-8")
    for marker in (
        "SOURCE_SEQUENCE_GAP",
        "SOURCE_SEQUENCE_DUPLICATE",
        "SOURCE_SEQUENCE_CONFLICT",
        "SOURCE_EPOCH_REGRESSION",
        "CLOCK_DRIFT",
        "POINT_QUALITY_",
        "hasAuthoritativeSequence",
        "StateTtlConfig",
    ):
        if marker not in detector_source:
            failures.append(f"TelemetryDataQualityFunction is missing detector marker {marker!r}")

    replay_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiDataQualityFlinkReplay.java").read_text(encoding="utf-8")
    for marker in (
        'ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"',
        "positionAtEnd",
        "matching data-quality records are not exactly once",
        "telemetry-data-quality",
        "inactiveContext",
    ):
        if marker not in replay_source:
            failures.append(f"BPI Flink data-quality replay is missing marker {marker!r}")

    replay_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BpiKafkaAcceptanceReplay.java").read_text(encoding="utf-8")
    for marker in (
        'ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"',
        "positionAtEnd",
        "candidateCount != 1",
        "matchesIssue",
        "inactivePublication",
    ):
        if marker not in replay_source:
            failures.append(f"BPI Kafka acceptance replay is missing marker {marker!r}")

    forbidden = ("jdbc:oracle", "oracle.jdbc", "com.supcon")
    for path in STREAMING.rglob("*"):
        if path.is_file() and path.suffix in {".java", ".xml", ".md"} and "target" not in path.parts:
            lowered = path.read_text(encoding="utf-8").lower()
            for marker in forbidden:
                if marker in lowered:
                    failures.append(f"{path.relative_to(ROOT)} contains forbidden legacy marker {marker!r}")

    acceptance = json.loads((ROOT / "metadata/bpi-stream-replay-acceptance.json").read_text(encoding="utf-8"))
    if acceptance.get("flink") != "2.2.1":
        failures.append("BPI stream acceptance must identify Flink 2.2.1")
    if acceptance.get("summary", {}).get("fail") != 0:
        failures.append("BPI stream acceptance must not contain failed items")
    flink_acceptance = json.loads(
        (ROOT / "metadata/bpi-flink-operator-acceptance.json").read_text(encoding="utf-8")
    )
    if flink_acceptance.get("flink") != "2.2.1":
        failures.append("BPI Flink operator acceptance must identify Flink 2.2.1")
    if flink_acceptance.get("summary", {}).get("fail") != 0:
        failures.append("BPI Flink operator acceptance must not contain failed items")
    topology_acceptance = json.loads(
        (ROOT / "metadata/bpi-kafka-flink-topology-acceptance.json").read_text(encoding="utf-8")
    )
    if topology_acceptance.get("status") != "LOCAL_MINICLUSTER_ACCEPTED_TARGET_CLUSTER_PENDING":
        failures.append("BPI Kafka/Flink topology acceptance status must remain explicit")
    topology_summary = topology_acceptance.get("summary", {})
    if topology_summary.get("streamFail") != 0:
        failures.append("BPI Kafka/Flink topology acceptance must not contain failed stream tests")
    if topology_summary.get("localMiniClusterAccepted") is not True:
        failures.append("BPI Kafka/Flink topology acceptance must record local MiniCluster acceptance")
    if topology_summary.get("targetClusterAccepted") is not False:
        failures.append("BPI Kafka/Flink topology acceptance must not claim target cluster acceptance")
    if topology_summary.get("flinkKafkaRuntimeAcceptancePass") != 1:
        failures.append("BPI Kafka/Flink topology acceptance must record the runtime acceptance pass")

    rule_application_acceptance = json.loads(
        (ROOT / "metadata/bpi-rule-application-flink-kafka-acceptance.json").read_text(
            encoding="utf-8"
        )
    )
    if rule_application_acceptance.get("status") != "PASS_LOCAL_FLINK_MINICLUSTER_KAFKA":
        failures.append("BPI rule-application Flink/Kafka acceptance must be an explicit local PASS")
    if rule_application_acceptance.get("kafkaClientVersion") != "4.2.0":
        failures.append("BPI rule-application Flink/Kafka acceptance must use Kafka 4.2.0")
    checkpoint_evidence = rule_application_acceptance.get("checkpoints", {})
    if checkpoint_evidence.get("completedCount", 0) < 3:
        failures.append("BPI rule-application acceptance requires at least three completed checkpoints")
    if checkpoint_evidence.get("restoredCount", 0) < 1:
        failures.append("BPI rule-application acceptance requires a restored checkpoint")
    pre_checkpoint = rule_application_acceptance.get("preCheckpoint", {})
    if pre_checkpoint.get("readCommittedVisible") is not False:
        failures.append("BPI rule-application acceptance must prove pre-checkpoint invisibility")
    if pre_checkpoint.get("interruptedTransactionCommitted") is not False:
        failures.append("BPI rule-application acceptance must prove canceled transaction rollback")
    if pre_checkpoint.get("readCommittedReadinessVisible") is not False:
        failures.append("BPI runtime-readiness acceptance must prove pre-checkpoint invisibility")
    committed_readiness = rule_application_acceptance.get("committedRuntimeReadiness", [])
    readiness_statuses = {item.get("status") for item in committed_readiness}
    if readiness_statuses != {"READY", "INACTIVE"}:
        failures.append("BPI Flink acceptance must commit independent READY and INACTIVE receipts")
    if any(not item.get("pointCatalogEventId") for item in committed_readiness):
        failures.append("BPI Flink readiness receipts must retain point-catalog evidence")
    cluster_replay = json.loads(
        (ROOT / "metadata/bpi-kafka-cluster-replay-acceptance.json").read_text(encoding="utf-8")
    )
    if cluster_replay.get("status") != "HARNESS_READY_CLUSTER_BLOCKED_DISK":
        failures.append("BPI Kafka cluster replay must not claim live acceptance while disk is blocked")
    if cluster_replay.get("summary", {}).get("liveClusterAccepted") is not False:
        failures.append("BPI Kafka cluster replay cannot claim live cluster acceptance")
    if cluster_replay.get("summary", {}).get("postgresMarkerAccepted") is not False:
        failures.append("BPI Kafka cluster replay cannot claim PostgreSQL marker acceptance")

    if failures:
        print("\n".join(f"ERROR: {item}" for item in failures), file=sys.stderr)
        return 1
    print("BPI streaming Java 17 boundary and deterministic replay structure verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
