#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OPENAPI = ROOT / "contracts/bpi-api/openapi.json"
ASYNCAPI = ROOT / "contracts/bpi-api/asyncapi.json"
PROFILE = ROOT / "contracts/bpi-api/simulation-profile.json"
SERVICE_PROFILE = ROOT / "contracts/bpi-api/service-phase1-profile.json"
PHASE2_PROFILE = ROOT / "contracts/bpi-api/service-phase2-profile.json"
CATALOG = ROOT / "docs/api/bpi-api-catalog.md"
INTERACTION = ROOT / "docs/designs/bpi-interaction-design.md"

REQUIRED_TOPICS = {
    "iot.telemetry.selected.v1": "TelemetryEnvelopeV1",
    "iot.point-catalog.snapshot.v1": "PointCatalogSnapshotV1",
    "mes.production.context.v1": "ProductionContextEventV1",
    "bpi.boundary.rule-publication.v1": "BoundaryRulePublicationV1",
    "bpi.boundary.rule-application.v1": "BoundaryRuleApplicationV1",
    "bpi.boundary.rule-runtime-readiness.v1": "BoundaryRuleRuntimeReadinessV1",
    "bpi.boundary.rule-runtime-readiness.dlq.v1": "BoundaryRuleRuntimeReadinessV1",
    "bpi.batch.candidate.v1": "BatchCandidateV1",
    "bpi.data-quality.v1": "DataQualityEventV1",
    "qcs.batch.quality-gate.v1": "QcsQualityGateV1",
    "qcs.batch.quality-gate.dlq.v1": "QcsQualityGateV1",
    "bpi.wms.completion-inbound-command.v1": "WmsCompletionInboundCommandV1",
    "wms.completion-inbound.receipt.v1": "WmsCompletionInboundReceiptV1",
    "wms.completion-inbound.receipt.dlq.v1": "WmsCompletionInboundReceiptV1",
    "bpi.batch.fact.v1": "BatchFactV1",
    "bpi.training.snapshot.v1": "TrainingSnapshotV1",
}
RESERVED_MESSAGES = {"BatchFactV1": "2", "TrainingSnapshotV1": "3"}


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"cannot load {path.relative_to(ROOT)}: {exc}") from exc


def parameter_refs(operation: dict) -> set[str]:
    return {
        item.get("$ref", "")
        for item in operation.get("parameters", [])
        if isinstance(item, dict)
    }


def main() -> int:
    failures: list[str] = []
    try:
        openapi = load_json(OPENAPI)
        asyncapi = load_json(ASYNCAPI)
        profile = load_json(PROFILE)
        service_profile = load_json(SERVICE_PROFILE)
        phase2_profile = load_json(PHASE2_PROFILE)
    except ValueError as exc:
        print(exc, file=sys.stderr)
        return 1

    if not str(openapi.get("openapi", "")).startswith("3.1"):
        failures.append("openapi.json must use OpenAPI 3.1")
    if asyncapi.get("asyncapi") != "3.0.0":
        failures.append("asyncapi.json must use AsyncAPI 3.0.0")

    operations: dict[str, tuple[str, str, dict]] = {}
    for path, path_item in openapi.get("paths", {}).items():
        for method, operation in path_item.items():
            if not isinstance(operation, dict) or "operationId" not in operation:
                continue
            operation_id = operation["operationId"]
            if operation_id in operations:
                failures.append(f"duplicate operationId: {operation_id}")
            operations[operation_id] = (method.upper(), path, operation)
            if method.lower() == "post":
                refs = parameter_refs(operation)
                for required in (
                    "#/components/parameters/IdempotencyKey",
                    "#/components/parameters/IfMatch",
                ):
                    if required not in refs:
                        failures.append(f"{operation_id} missing {required}")
                responses = operation.get("responses", {})
                if "428" not in responses:
                    failures.append(f"{operation_id} missing 428 response")

    simulated = profile.get("operationIds", [])
    if len(simulated) != len(set(simulated)):
        failures.append("simulation-profile.json contains duplicate operationIds")
    unknown = sorted(set(simulated) - set(operations))
    if unknown:
        failures.append("simulation profile references unknown operations: " + ", ".join(unknown))

    implemented = service_profile.get("operationIds", [])
    if len(implemented) != len(set(implemented)):
        failures.append("service-phase1-profile.json contains duplicate operationIds")
    unknown_implemented = sorted(set(implemented) - set(operations))
    if unknown_implemented:
        failures.append(
            "service Phase 1 profile references unknown operations: " + ", ".join(unknown_implemented)
        )
    if service_profile.get("mode") != "SHADOW_ONLY":
        failures.append("service-phase1-profile.json must remain SHADOW_ONLY in Phase 1")
    required_exclusions = {"WOM", "QCS", "WMS", "PLC", "DCS"}
    if set(service_profile.get("excludedWrites", [])) != required_exclusions:
        failures.append("service Phase 1 profile must exclude WOM/QCS/WMS/PLC/DCS writes")
    internal_endpoints = {
        (item.get("method"), item.get("path"))
        for item in service_profile.get("internalEndpoints", [])
        if isinstance(item, dict)
    }
    required_internal_endpoints = {
        ("POST", "/internal/bpi/v1/candidates"),
        ("POST", "/internal/bpi/v1/telemetry"),
        ("POST", "/internal/bpi/v1/candidate-events"),
    }
    if internal_endpoints != required_internal_endpoints:
        failures.append(
            "service Phase 1 profile must expose only the approved candidate, candidate-event and telemetry internal endpoints"
        )
    if service_profile.get("internalEndpointModes", {}).get("/internal/bpi/v1/telemetry") != (
        "SHORT_LIVED_REPLAY_STAGING"
    ):
        failures.append("telemetry HTTP ingress must remain explicitly marked as replay staging")
    if service_profile.get("internalEndpointModes", {}).get("/internal/bpi/v1/candidate-events") != (
        "SHORT_LIVED_PROTOBUF_BRIDGE"
    ):
        failures.append("candidate Protobuf HTTP ingress must remain explicitly marked as a short-lived bridge")

    phase2_reads = phase2_profile.get("readOperationIds", [])
    if len(phase2_reads) != len(set(phase2_reads)):
        failures.append("service-phase2-profile.json contains duplicate readOperationIds")
    unknown_phase2_reads = sorted(set(phase2_reads) - set(operations))
    if unknown_phase2_reads:
        failures.append(
            "service Phase 2 profile references unknown read operations: "
            + ", ".join(unknown_phase2_reads)
        )
    if phase2_profile.get("mode") != "DISABLED_BY_DEFAULT":
        failures.append("service-phase2-profile.json must remain DISABLED_BY_DEFAULT")
    phase2_endpoints = {
        (item.get("method"), item.get("path"), item.get("message"))
        for item in phase2_profile.get("internalEndpoints", [])
        if isinstance(item, dict)
    }
    required_phase2_endpoints = {
        ("POST", "/internal/bpi/v1/qcs-quality-gates", "QcsQualityGateV1"),
        ("POST", "/internal/bpi/v1/wms-inbound-receipts", "WmsCompletionInboundReceiptV1"),
    }
    if phase2_endpoints != required_phase2_endpoints:
        failures.append("service Phase 2 profile must expose only the approved QCS and WMS bridges")
    phase2_topics = {
        (item.get("topic"), item.get("message"))
        for field in ("inboundTopics", "outboundTopics")
        for item in phase2_profile.get(field, [])
        if isinstance(item, dict)
    }
    required_phase2_topics = {
        ("qcs.batch.quality-gate.v1", "QcsQualityGateV1"),
        ("bpi.wms.completion-inbound-command.v1", "WmsCompletionInboundCommandV1"),
        ("wms.completion-inbound.receipt.v1", "WmsCompletionInboundReceiptV1"),
    }
    if phase2_topics != required_phase2_topics:
        failures.append("service Phase 2 profile topic/message bindings changed unexpectedly")
    required_phase2_gates = {
        "BPI_PHASE2_INTEGRATION_ENABLED=true",
        "bpi.qcs-link=true at the exact tenant/plant/line scope",
        "bpi.wms-link=true at the exact tenant/plant/line scope",
        "batch.is_shadow=false before a WMS command can be inserted",
        "BPI_WMS_OUTBOX_ENABLED=true only after broker/topic/consumer readiness",
    }
    if set(phase2_profile.get("activationGates", [])) != required_phase2_gates:
        failures.append("service Phase 2 activation gates must remain explicit and complete")
    required_phase2_invariants = {
        "Phase 1 service profile remains SHADOW_ONLY",
        "QCS and WMS integration flags remain phase-locked in the runtime UI",
        "Every inbound event is inbox-idempotent and payload-checksummed",
        "Batch transition, integration projection, outbox and audit rows share one PostgreSQL transaction",
        "A WMS receipt cannot precede durable outbox publication",
        "Only an accepted WMS receipt with document_id can transition RELEASED to INBOUNDED",
    }
    if set(phase2_profile.get("safetyInvariants", [])) != required_phase2_invariants:
        failures.append("service Phase 2 safety invariants changed unexpectedly")

    channels = asyncapi.get("channels", {})
    messages = asyncapi.get("components", {}).get("messages", {})
    address_to_message: dict[str, str] = {}
    for channel in channels.values():
        address = channel.get("address")
        refs = channel.get("messages", {})
        if address and refs:
            ref = next(iter(refs.values())).get("$ref", "")
            address_to_message[address] = ref.rsplit("/", 1)[-1]
    for topic, message_name in REQUIRED_TOPICS.items():
        if address_to_message.get(topic) != message_name:
            failures.append(f"topic {topic} must reference {message_name}")

    proto_text = (ROOT / "contracts/bpi-events/src/main/proto/bpi_events_v1.proto").read_text(
        encoding="utf-8"
    )
    for message_name, message in messages.items():
        schema = message.get("payload", {}).get("schema", {})
        if message_name in RESERVED_MESSAGES:
            if schema.get("x-phase") != RESERVED_MESSAGES[message_name]:
                failures.append(f"{message_name} must remain explicitly phase-reserved")
            if schema.get("x-status") != "RESERVED_NOT_IMPLEMENTED":
                failures.append(f"{message_name} must remain RESERVED_NOT_IMPLEMENTED")
        elif f"message {message_name} " not in proto_text:
            failures.append(f"Protobuf message missing: {message_name}")

    for doc in (CATALOG, INTERACTION):
        if not doc.exists():
            failures.append(f"required BPI document missing: {doc.relative_to(ROOT)}")
            continue
        text = doc.read_text(encoding="utf-8")
        for operation_id in simulated:
            if operation_id not in text:
                failures.append(f"{doc.relative_to(ROOT)} missing simulated operation {operation_id}")

    if failures:
        for failure in failures:
            print(f"BPI API contract error: {failure}", file=sys.stderr)
        print(f"BPI API contract verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1

    print(
        "BPI API contract verification passed "
        f"(operations={len(operations)}, simulated={len(simulated)}, "
        f"implemented={len(implemented)}, phase2Reads={len(phase2_reads)}, "
        f"topics={len(REQUIRED_TOPICS)})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
