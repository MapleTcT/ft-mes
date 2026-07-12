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
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundarySignalRouter.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/ProductionContextTimeline.java",
    "streaming/bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryReplayEngine.java",
    "services/bpi-service/batch-rule-runtime/src/main/java/com/mapletct/ftmes/bpi/rules/BoundaryTimingPolicy.java",
    "streaming/bpi-stream-engine/src/test/java/com/mapletct/ftmes/bpi/stream/BoundaryKeyedBroadcastHarnessTest.java",
    "streaming/bpi-stream-engine/src/test/java/com/mapletct/ftmes/bpi/stream/BoundaryReplayEngineTest.java",
    "docs/testing/bpi-flink-operator-acceptance.md",
    "docs/testing/bpi-rule-timing-acceptance.md",
    "docs/testing/bpi-rule-publication-routing-acceptance.md",
    "docs/testing/bpi-stream-replay-acceptance.md",
    "metadata/bpi-flink-operator-acceptance.json",
    "metadata/bpi-rule-timing-acceptance.json",
    "metadata/bpi-rule-publication-routing-acceptance.json",
    "metadata/bpi-stream-replay-acceptance.json",
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
        "toByteArray",
    ):
        if marker not in operator_source:
            failures.append(f"BoundaryKeyedBroadcastFunction is missing runtime marker {marker!r}")

    publication_source = (STREAMING / "bpi-stream-engine/src/main/java/com/mapletct/ftmes/bpi/stream/BoundaryRulePublicationMapper.java").read_text(encoding="utf-8")
    for marker in ("BoundaryRulePublicationV1", "BoundaryTimingPolicy", "duplicate device/property binding"):
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

    if failures:
        print("\n".join(f"ERROR: {item}" for item in failures), file=sys.stderr)
        return 1
    print("BPI streaming Java 17 boundary and deterministic replay structure verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
