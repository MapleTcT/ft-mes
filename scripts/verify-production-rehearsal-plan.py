#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/production-rehearsal-plan.json"
DOC_PATH = ROOT / "docs/production-migration/rehearsal-plan.md"
GENERATOR_PATH = ROOT / "scripts/generate-production-rehearsal-plan.py"

REQUIRED_TRACK_IDS = {
    "postgres-data-migration",
    "rollback-plan",
    "license-strategy",
    "minio-file-migration",
    "keycloak-production-db",
    "nacos-runtime-patch",
    "ports-domain-tls",
    "security-hardening",
    "business-smoke-signoff",
}

ALLOWED_REPORT_STATUSES = {
    "REHEARSAL_BLOCKED",
    "REHEARSAL_PLAN_OPEN",
    "READY_FOR_REHEARSAL",
}
ALLOWED_TRACK_STATUSES = {
    "READY_TO_REHEARSE",
    "PLANNED",
    "BLOCKED",
    "NOT_STARTED",
}
ALLOWED_SOURCE_STATUSES = {
    "PASS",
    "BLOCKED",
    "BLOCKED_NO_IMPLEMENTATION_CANDIDATE",
    "NOT_READY_FOR_PRODUCTION_MIGRATION",
    "NOT_READY_FOR_PRODUCTION_CUTOVER",
}

REQUIRED_TRACK_KEYS = {
    "id",
    "title",
    "rehearsalStatus",
    "readinessStatus",
    "cutoverGateStatus",
    "currentEvidence",
    "requiredEvidence",
    "rehearsalCommands",
    "blockingIssues",
    "nextActions",
    "artifacts",
}

REQUIRED_COMMAND_FRAGMENTS = {
    "postgres-data-migration": [
        "production-source-inventory",
        "production-target-preflight",
        "production-rowcount-compare",
        "production-checksum-compare",
        "production-db-migration-ready-check",
    ],
    "rollback-plan": ["production-rollback-ready-check"],
    "license-strategy": ["production-license-ready-check"],
    "minio-file-migration": [
        "production-minio-source-inventory",
        "production-minio-target-inventory",
        "production-minio-compare",
        "production-minio-migration-ready-check",
    ],
    "keycloak-production-db": [
        "production-keycloak-source-export",
        "production-keycloak-target-export",
        "production-keycloak-compare",
        "production-keycloak-migration-ready-check",
    ],
    "nacos-runtime-patch": [
        "runtime-patch-manifest-check",
        "smoke-nacos-config",
        "production-nacos-runtime-patch-ready-check",
    ],
    "ports-domain-tls": ["production-network-tls-ready-check"],
    "security-hardening": ["production-security-hardening-ready-check"],
    "business-smoke-signoff": [
        "business-package-scan",
        "smoke-business-dependencies",
        "smoke-production-export-readiness",
        "production-business-smoke-signoff-ready-check",
    ],
}

PLACEHOLDER_EVIDENCE_MARKERS = (
    ".example",
    "example.",
    "-example",
    "_example",
    "template",
    "sample",
)
TEST_ENVIRONMENT_EVIDENCE_MARKERS = (
    "100.99.133.43",
    "10.11.100.17",
    "222.88.185.146",
    "ubuntu-test",
    "v6-2288H-V6",
    "metadata/test-environment-smoke.json",
)
FORBIDDEN_SECRET_LITERALS = [
    "ft" + "123456789",
    "BEGIN PRIVATE KEY",
    "BEGIN RSA PRIVATE KEY",
    "BEGIN OPENSSH PRIVATE KEY",
    "AKIA",
]
SECRET_ASSIGNMENT_RE = re.compile(
    r"(?im)^\s*(password|passwd|secret|token|access[_-]?key|secret[_-]?key|license[_-]?key)\s*[:=]\s*"
    r"(?!CHANGE_ME|TBD|TODO|<|\$\{|\$|/secure/)[^\s#]{8,}"
)


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def read_json(path: Path, label: str, failures: list[str]) -> dict[str, Any]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        fail(failures, f"missing {label}: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {label}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{label} must be a JSON object")
        return {}
    return data


def load_generator() -> Any:
    spec = importlib.util.spec_from_file_location("generate_production_rehearsal_plan", GENERATOR_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError("cannot load production rehearsal plan generator")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def stable_expected_plan(existing: dict[str, Any], failures: list[str]) -> dict[str, Any]:
    try:
        generator = load_generator()
        expected = generator.build_plan()
    except Exception as error:  # pragma: no cover - defensive CLI guard
        fail(failures, f"cannot build expected production rehearsal plan: {error}")
        return {}
    expected["generatedAt"] = existing.get("generatedAt")
    expected["repoCommit"] = existing.get("repoCommit")
    return expected


def track_map(items: Any, key_name: str, failures: list[str]) -> dict[str, dict[str, Any]]:
    if not isinstance(items, list):
        fail(failures, f"{key_name} must be a list")
        return {}
    result: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            fail(failures, f"{key_name}[{index}] must be an object")
            continue
        item_id = str(item.get("id", "")).strip()
        if not item_id:
            fail(failures, f"{key_name}[{index}] missing id")
            continue
        if item_id in result:
            fail(failures, f"duplicate {key_name} id: {item_id}")
        result[item_id] = item
    return result


def contains_placeholder(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    lowered = value.strip().lower()
    name = Path(lowered).name
    return any(marker in lowered or marker in name for marker in PLACEHOLDER_EVIDENCE_MARKERS)


def contains_test_environment_marker(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    lowered = value.strip().lower()
    return any(marker.lower() in lowered for marker in TEST_ENVIRONMENT_EVIDENCE_MARKERS)


def check_repo_artifact_refs(track_id: str, artifacts: list[Any], failures: list[str]) -> None:
    for artifact in artifacts:
        if not isinstance(artifact, str) or not artifact.strip():
            fail(failures, f"{track_id}.artifacts must contain non-empty strings")
            continue
        if artifact.startswith("/secure/") or artifact.startswith("http://") or artifact.startswith("https://"):
            continue
        path = Path(artifact)
        if path.is_absolute() or ".." in path.parts:
            fail(failures, f"{track_id}.artifacts must stay inside repo or /secure/http evidence paths: {artifact}")
            continue
        if not (ROOT / path).exists():
            fail(failures, f"{track_id}.artifacts references missing repo file: {artifact}")


def expected_rehearsal_status(readiness_status: str, cutover_status: str) -> str:
    if readiness_status == "READY" and cutover_status == "READY":
        return "READY_TO_REHEARSE"
    if readiness_status == "BLOCKED" or cutover_status == "BLOCKED":
        return "BLOCKED"
    if readiness_status == "NOT_STARTED" or cutover_status == "NOT_STARTED":
        return "NOT_STARTED"
    return "PLANNED"


def check_track(
    track_id: str,
    track: dict[str, Any],
    readiness_track: dict[str, Any],
    cutover_gate: dict[str, Any],
    failures: list[str],
) -> None:
    missing = sorted(REQUIRED_TRACK_KEYS - set(track))
    if missing:
        fail(failures, f"{track_id} missing keys: {missing}")

    status = track.get("rehearsalStatus")
    if status not in ALLOWED_TRACK_STATUSES:
        fail(failures, f"{track_id}.rehearsalStatus has invalid value: {status!r}")

    readiness_status = str(readiness_track.get("status", ""))
    cutover_status = str(cutover_gate.get("status", ""))
    if track.get("readinessStatus") != readiness_status:
        fail(failures, f"{track_id}.readinessStatus must match production migration readiness")
    if track.get("cutoverGateStatus") != cutover_status:
        fail(failures, f"{track_id}.cutoverGateStatus must match production cutover gate")
    expected_status = expected_rehearsal_status(readiness_status, cutover_status)
    if track.get("rehearsalStatus") != expected_status:
        fail(failures, f"{track_id}.rehearsalStatus expected {expected_status}, got {track.get('rehearsalStatus')!r}")

    commands = [str(command) for command in as_list(track.get("rehearsalCommands"))]
    command_text = "\n".join(commands)
    for fragment in REQUIRED_COMMAND_FRAGMENTS[track_id]:
        if fragment not in command_text:
            fail(failures, f"{track_id}.rehearsalCommands missing required fragment: {fragment}")

    if not as_list(track.get("requiredEvidence")):
        fail(failures, f"{track_id}.requiredEvidence must not be empty")
    if status in {"PLANNED", "BLOCKED", "NOT_STARTED"}:
        if not as_list(track.get("blockingIssues")):
            fail(failures, f"{track_id} must keep blockingIssues while not ready")
        if not as_list(track.get("nextActions")):
            fail(failures, f"{track_id} must keep nextActions while not ready")

    artifacts = as_list(track.get("artifacts"))
    if not artifacts:
        fail(failures, f"{track_id}.artifacts must not be empty")
    check_repo_artifact_refs(track_id, artifacts, failures)

    if status == "READY_TO_REHEARSE":
        evidence_blob = json.dumps(track, ensure_ascii=False)
        if any(contains_placeholder(value) for value in as_list(track.get("currentEvidence")) + artifacts):
            fail(failures, f"{track_id} READY_TO_REHEARSE must not cite template/example/sample evidence")
        if any(contains_test_environment_marker(value) for value in as_list(track.get("currentEvidence")) + artifacts):
            fail(failures, f"{track_id} READY_TO_REHEARSE must not cite test-environment evidence as production rehearsal evidence")
        if "template" in evidence_blob.lower() or "测试环境" in evidence_blob:
            fail(failures, f"{track_id} READY_TO_REHEARSE must not rely on template or test-environment wording")


def check_summary(report: dict[str, Any], tracks: list[dict[str, Any]], failures: list[str]) -> None:
    summary = as_dict(report.get("summary"))
    if not summary:
        fail(failures, "summary must be an object")
        return
    statuses = [str(track.get("rehearsalStatus")) for track in tracks]
    expected = {
        "totalTracks": len(tracks),
        "readyToRehearse": statuses.count("READY_TO_REHEARSE"),
        "planned": statuses.count("PLANNED"),
        "blocked": statuses.count("BLOCKED"),
        "notStarted": statuses.count("NOT_STARTED"),
    }
    for key, expected_value in expected.items():
        if summary.get(key) != expected_value:
            fail(failures, f"summary.{key} expected {expected_value}, got {summary.get(key)!r}")

    if summary.get("productionBlockers", 0) > 0 and report.get("status") == "READY_FOR_REHEARSAL":
        fail(failures, "report cannot be READY_FOR_REHEARSAL while productionBlockers > 0")
    if summary.get("productionBacklogItems", 0) > 0 and report.get("status") == "READY_FOR_REHEARSAL":
        fail(failures, "report cannot be READY_FOR_REHEARSAL while productionBacklogItems > 0")

    expected_status = "REHEARSAL_PLAN_OPEN"
    if "BLOCKED" in statuses:
        expected_status = "REHEARSAL_BLOCKED"
    elif tracks and all(status == "READY_TO_REHEARSE" for status in statuses):
        expected_status = "READY_FOR_REHEARSAL"
    if report.get("status") != expected_status:
        fail(failures, f"report status expected {expected_status}, got {report.get('status')!r}")


def check_source_evidence(report: dict[str, Any], failures: list[str]) -> None:
    source_ledgers = as_dict(report.get("sourceLedgers"))
    source_evidence = as_dict(report.get("sourceEvidence"))
    if set(source_ledgers) != set(source_evidence):
        fail(failures, "sourceEvidence keys must exactly match sourceLedgers keys")
    for key, ledger_path in source_ledgers.items():
        if not isinstance(ledger_path, str) or not ledger_path:
            fail(failures, f"sourceLedgers.{key} must be a non-empty path")
            continue
        if Path(ledger_path).is_absolute() or ".." in Path(ledger_path).parts:
            fail(failures, f"sourceLedgers.{key} must stay inside the repository")
        if not (ROOT / ledger_path).exists():
            fail(failures, f"sourceLedgers.{key} points to missing file: {ledger_path}")
        evidence = as_dict(source_evidence.get(key))
        if evidence.get("path") != ledger_path:
            fail(failures, f"sourceEvidence.{key}.path must match sourceLedgers.{key}")
        status = evidence.get("status")
        if status not in ALLOWED_SOURCE_STATUSES:
            fail(failures, f"sourceEvidence.{key}.status has unexpected value: {status!r}")
        if not isinstance(evidence.get("metrics"), dict):
            fail(failures, f"sourceEvidence.{key}.metrics must be an object")


def check_doc(report: dict[str, Any], failures: list[str]) -> None:
    try:
        text = DOC_PATH.read_text(encoding="utf-8")
    except FileNotFoundError:
        fail(failures, f"missing document: {DOC_PATH.relative_to(ROOT)}")
        return
    for fragment in (
        "# Production Rehearsal Plan",
        "REHEARSAL_BLOCKED",
        "business-smoke-signoff",
        "Do not promote production from this plan",
        "READY_FOR_PRODUCTION_CUTOVER",
    ):
        if fragment not in text:
            fail(failures, f"rehearsal plan document missing required text: {fragment}")
    for track_id in REQUIRED_TRACK_IDS:
        if track_id not in text:
            fail(failures, f"rehearsal plan document missing track {track_id}")
    if str(report.get("generatedAt")) not in text or str(report.get("repoCommit")) not in text:
        fail(failures, "rehearsal plan document must include generatedAt and repoCommit from JSON")
    check_secret_hygiene(text, "docs/production-migration/rehearsal-plan.md", failures)


def check_secret_hygiene(text: str, label: str, failures: list[str]) -> None:
    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in text:
            fail(failures, f"{label} contains forbidden secret literal")
    match = SECRET_ASSIGNMENT_RE.search(text)
    if match:
        fail(failures, f"{label} appears to contain a concrete secret assignment: {match.group(0)!r}")


def check_report(report: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "repoCommit",
        "database",
        "status",
        "environment",
        "sourceLedgers",
        "sourceEvidence",
        "summary",
        "tracks",
        "releaseRule",
    ):
        if key not in report:
            fail(failures, f"report missing top-level key: {key}")

    if report.get("schemaVersion") != 1:
        fail(failures, "schemaVersion must be 1")
    if report.get("database") != "PostgreSQL":
        fail(failures, "database must remain PostgreSQL")
    if report.get("status") not in ALLOWED_REPORT_STATUSES:
        fail(failures, f"invalid rehearsal report status: {report.get('status')!r}")

    expected = stable_expected_plan(report, failures)
    if expected and report != expected:
        fail(failures, "production rehearsal plan is stale; run make production-rehearsal-plan")

    check_source_evidence(report, failures)

    readiness = read_json(ROOT / "metadata/production-migration-readiness.json", "production migration readiness", failures)
    cutover = read_json(ROOT / "metadata/production-cutover-gate.json", "production cutover gate", failures)
    readiness_tracks = track_map(readiness.get("tracks"), "production migration tracks", failures)
    cutover_gates = track_map(cutover.get("gates"), "production cutover gates", failures)

    tracks = [track for track in as_list(report.get("tracks")) if isinstance(track, dict)]
    report_tracks = track_map(tracks, "rehearsal tracks", failures)
    if set(report_tracks) != REQUIRED_TRACK_IDS:
        fail(failures, f"rehearsal tracks mismatch: expected={sorted(REQUIRED_TRACK_IDS)} actual={sorted(report_tracks)}")

    for track_id in sorted(REQUIRED_TRACK_IDS):
        track = report_tracks.get(track_id, {})
        readiness_track = readiness_tracks.get(track_id, {})
        cutover_gate = cutover_gates.get(track_id, {})
        if not readiness_track:
            fail(failures, f"missing readiness track for {track_id}")
        if not cutover_gate:
            fail(failures, f"missing cutover gate for {track_id}")
        if track:
            check_track(track_id, track, readiness_track, cutover_gate, failures)

    check_summary(report, tracks, failures)
    if "real non-template evidence" not in str(report.get("releaseRule", "")):
        fail(failures, "releaseRule must mention real non-template evidence")
    check_secret_hygiene(json.dumps(report, ensure_ascii=False), "metadata/production-rehearsal-plan.json", failures)
    check_doc(report, failures)


def main() -> int:
    failures: list[str] = []
    report = read_json(REPORT_PATH, "production rehearsal plan", failures)
    if report:
        check_report(report, failures)
    if failures:
        print(f"Production rehearsal plan verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Production rehearsal plan verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
