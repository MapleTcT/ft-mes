#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/production-cutover-gate.json"

REQUIRED_GATE_IDS = {
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

REQUIRED_LEDGER_PATHS = {
    "migrationReadiness": "metadata/production-migration-readiness.json",
    "platformValidationSmoke": "metadata/platform-validation-smoke.json",
    "productionBlockers": "metadata/production-module-blockers.json",
    "productionBacklog": "metadata/production-module-backlog.json",
    "businessDependencyReadiness": "metadata/business-dependency-readiness-smoke.json",
    "businessPackageScan": "metadata/business-dependency-package-scan.json",
    "productionExportReadiness": "metadata/production-export-readiness-smoke.json",
    "businessSmokeSignoffTemplate": "deploy/business-smoke/production-migration/business-smoke-signoff.example.json",
}

SOURCE_EVIDENCE_PATHS = {
    "migrationReadiness": "metadata/production-migration-readiness.json",
    "testEnvironmentSmoke": "metadata/test-environment-smoke.json",
    "platformValidationSmoke": "metadata/platform-validation-smoke.json",
    "postgresRuntimeSmoke": "metadata/postgres-runtime-smoke.json",
    "nacosConfigSmoke": "metadata/nacos-config-drift-smoke.json",
    "keycloakJwtSmoke": "metadata/keycloak-jwt-runtime-smoke.json",
    "minioRuntimeSmoke": "metadata/minio-runtime-smoke.json",
    "productionBlockers": "metadata/production-module-blockers.json",
    "productionBacklog": "metadata/production-module-backlog.json",
    "businessDependencyReadiness": "metadata/business-dependency-readiness-smoke.json",
    "businessPackageScan": "metadata/business-dependency-package-scan.json",
    "productionExportReadiness": "metadata/production-export-readiness-smoke.json",
}

ALLOWED_REPORT_STATUSES = {"NOT_READY_FOR_PRODUCTION_CUTOVER", "READY_FOR_PRODUCTION_CUTOVER"}
ALLOWED_GATE_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
PLACEHOLDER_EVIDENCE_MARKERS = (
    ".example",
    "example.",
    "-example",
    "_example",
    "template",
    "sample",
)
PLACEHOLDER_VALUES = {"TBD", "TODO", "CHANGE_ME"}
TEST_ENVIRONMENT_EVIDENCE_MARKERS = (
    "100.99.133.43",
    "10.11.100.17",
    "222.88.185.146",
    "ubuntu-test",
    "v6-2288H-V6",
    "metadata/test-environment-smoke.json",
)

REQUIRED_READY_COMMAND_FRAGMENTS = {
    "postgres-data-migration": [
        "production-source-inventory",
        "production-target-preflight",
        "production-rowcount-compare",
        "production-checksum-compare",
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
    "business-smoke-signoff": ["production-business-smoke-signoff-ready-check"],
}

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


def source_status(data: dict[str, Any]) -> str | None:
    if isinstance(data.get("status"), str):
        return data["status"]
    summary = as_dict(data.get("summary"))
    if isinstance(summary.get("status"), str):
        return summary["status"]
    for key in ("blockers", "blockedCases", "blockedDependencies", "blocked"):
        value = summary.get(key)
        if isinstance(value, int) and value > 0:
            return "BLOCKED"
    if isinstance(data.get("overallStatus"), str):
        return data["overallStatus"]
    if data.get("ok") is True:
        return "PASS"
    if data.get("ok") is False:
        return "FAIL"
    return None


def compact_metrics(key: str, data: dict[str, Any]) -> dict[str, Any]:
    summary = as_dict(data.get("summary"))
    environment = as_dict(data.get("environment"))
    target = as_dict(data.get("target"))
    metric_keys_by_source = {
        "testEnvironmentSmoke": [
            "totalChecks",
            "pass",
            "fail",
            "expectedContainerCount",
            "runningExpectedContainers",
        ],
        "productionBlockers": ["blockers", "blockedCases"],
        "productionBacklog": ["totalItems", "blocked", "failBacklog"],
        "businessDependencyReadiness": ["dependencies", "ready", "blocked"],
        "businessPackageScan": [
            "filesVisited",
            "archivesScanned",
            "nestedArchivesScanned",
            "archiveEntriesScanned",
            "blockedDependencies",
            "candidateDependencies",
        ],
        "productionExportReadiness": [
            "targets",
            "pagePass",
            "visibleExportActions",
            "runtimeExportActions",
            "verifiedDataExports",
            "ready",
            "actionRequired",
            "blocked",
        ],
        "migrationReadiness": ["totalTracks", "ready", "planned", "blocked", "notStarted"],
    }
    if key == "platformValidationSmoke":
        return {
            "ok": data.get("ok"),
            "total": data.get("total"),
            "passed": data.get("passed"),
            "failed": data.get("failed"),
            "baseUrl": data.get("baseUrl"),
            "browserBaseUrl": data.get("browserBaseUrl"),
        }
    if key == "testEnvironmentSmoke":
        metrics = {name: summary.get(name) for name in metric_keys_by_source[key]}
        metrics["sshHost"] = environment.get("sshHost")
        metrics["baseUrl"] = environment.get("baseUrl")
        return metrics
    if key == "businessDependencyReadiness":
        metrics = {name: summary.get(name) for name in metric_keys_by_source[key]}
        metrics["sshHost"] = target.get("sshHost")
        metrics["baseUrl"] = target.get("baseUrl")
        return metrics
    if key in metric_keys_by_source:
        return {name: summary.get(name) for name in metric_keys_by_source[key]}
    return dict(summary)


def source_evidence_entry(key: str, relative_path: str, failures: list[str]) -> dict[str, Any]:
    data = read_json(ROOT / relative_path, f"source evidence {key}", failures)
    return {
        "path": relative_path,
        "generatedAt": data.get("generatedAt"),
        "status": source_status(data),
        "metrics": compact_metrics(key, data),
    }


def expected_source_evidence(failures: list[str]) -> dict[str, Any]:
    return {
        key: source_evidence_entry(key, relative_path, failures)
        for key, relative_path in SOURCE_EVIDENCE_PATHS.items()
    }


def check_source_evidence(data: dict[str, Any], failures: list[str]) -> None:
    actual = data.get("sourceEvidence")
    if not isinstance(actual, dict):
        fail(failures, "production cutover gate must include sourceEvidence")
        return
    expected = expected_source_evidence(failures)
    for key, expected_entry in expected.items():
        if actual.get(key) != expected_entry:
            fail(failures, f"sourceEvidence.{key} is stale or incorrect; update production cutover gate evidence")
    extra_keys = sorted(set(actual) - set(expected))
    if extra_keys:
        fail(failures, "production cutover gate sourceEvidence contains unknown keys: " + ", ".join(extra_keys))


def is_placeholder_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    stripped = value.strip()
    if not stripped or stripped in PLACEHOLDER_VALUES:
        return True
    lowered = stripped.lower()
    name = Path(stripped).name.lower()
    return any(marker in lowered or marker in name for marker in PLACEHOLDER_EVIDENCE_MARKERS)


def is_test_environment_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    lowered = value.strip().lower()
    return any(marker.lower() in lowered for marker in TEST_ENVIRONMENT_EVIDENCE_MARKERS)


def readiness_track_statuses(readiness_ledger: dict[str, Any], failures: list[str]) -> dict[str, str]:
    tracks = readiness_ledger.get("tracks")
    if not isinstance(tracks, list):
        fail(failures, "production migration readiness ledger must include tracks list")
        return {}

    statuses: dict[str, str] = {}
    for index, track in enumerate(tracks):
        if not isinstance(track, dict):
            fail(failures, f"production migration readiness tracks[{index}] must be an object")
            continue
        track_id = str(track.get("id", "")).strip()
        status = str(track.get("status", "")).strip()
        if not track_id:
            fail(failures, f"production migration readiness tracks[{index}] missing id")
            continue
        statuses[track_id] = status
    return statuses


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


def check_secret_hygiene(path: Path, failures: list[str]) -> None:
    text = path.read_text(encoding="utf-8")
    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in text:
            fail(failures, f"{path.relative_to(ROOT)} contains forbidden secret literal: {literal}")
    if SECRET_ASSIGNMENT_RE.search(text):
        fail(failures, f"{path.relative_to(ROOT)} appears to contain a non-placeholder secret assignment")


def check_repo_artifact(ref: Any, context: str, failures: list[str]) -> None:
    if not isinstance(ref, str) or not ref.strip():
        fail(failures, f"{context} must be a non-empty repo-relative path")
        return
    path = Path(ref)
    if path.is_absolute() or ".." in path.parts:
        fail(failures, f"{context} must stay inside the repository: {ref}")
        return
    if not (ROOT / path).exists():
        fail(failures, f"{context} references missing artifact: {ref}")


def production_blocker_count(blocker_ledger: dict[str, Any], failures: list[str]) -> int:
    blockers = blocker_ledger.get("blockers")
    if not isinstance(blockers, list):
        fail(failures, "production blocker ledger must include blockers list")
        return 0
    count = 0
    for index, blocker in enumerate(blockers):
        if not isinstance(blocker, dict):
            fail(failures, f"production blocker ledger blockers[{index}] must be an object")
            continue
        if blocker.get("status") != "BLOCKED":
            fail(failures, f"production blocker {blocker.get('caseId')} must remain BLOCKED until resolved")
        count += 1
    summary = as_dict(blocker_ledger.get("summary"))
    if isinstance(summary.get("blockers"), int) and summary["blockers"] != count:
        fail(failures, "production blocker ledger summary.blockers must match blockers length")
    if isinstance(summary.get("blockedCases"), int) and summary["blockedCases"] != count:
        fail(failures, "production blocker ledger summary.blockedCases must match blockers length")
    return count


def production_backlog_count(backlog_ledger: dict[str, Any], failures: list[str]) -> int:
    items = backlog_ledger.get("items")
    if not isinstance(items, list):
        fail(failures, "production backlog ledger must include items list")
        return 0
    count = 0
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            fail(failures, f"production backlog ledger items[{index}] must be an object")
            continue
        if item.get("status") not in {"BLOCKED", "FAIL_BACKLOG"}:
            fail(failures, f"production backlog {item.get('id')} must remain BLOCKED/FAIL_BACKLOG until resolved")
        count += 1
    summary = as_dict(backlog_ledger.get("summary"))
    if isinstance(summary.get("totalItems"), int) and summary["totalItems"] != count:
        fail(failures, "production backlog ledger summary.totalItems must match items length")
    if backlog_ledger.get("overallStatus") != "OPEN":
        fail(failures, "production backlog ledger overallStatus must remain OPEN while unresolved items exist")
    return count


def check_gate(index: int, gate: Any, failures: list[str]) -> str | None:
    if not isinstance(gate, dict):
        fail(failures, f"gates[{index}] must be an object")
        return None

    gate_id = str(gate.get("id", "")).strip()
    if gate_id not in REQUIRED_GATE_IDS:
        fail(failures, f"unknown production cutover gate id: {gate_id!r}")
        return None

    for key in ("id", "status", "requiredBeforeCutover", "currentEvidence", "readyEvidenceCommands", "readyCriteria", "blockingReason"):
        if key not in gate:
            fail(failures, f"{gate_id} missing required key: {key}")

    if gate.get("status") not in ALLOWED_GATE_STATUSES:
        fail(failures, f"{gate_id} has invalid status: {gate.get('status')!r}")
    if gate.get("requiredBeforeCutover") is not True:
        fail(failures, f"{gate_id} must be requiredBeforeCutover=true")

    for ref in as_list(gate.get("currentEvidence")):
        check_repo_artifact(ref, f"{gate_id}.currentEvidence", failures)
    commands = as_list(gate.get("readyEvidenceCommands"))
    if not commands:
        fail(failures, f"{gate_id} must include readyEvidenceCommands")
    command_text = "\n".join(str(command) for command in commands)
    for fragment in REQUIRED_READY_COMMAND_FRAGMENTS[gate_id]:
        if fragment not in command_text:
            fail(failures, f"{gate_id} readyEvidenceCommands missing required fragment: {fragment}")

    if not as_list(gate.get("readyCriteria")):
        fail(failures, f"{gate_id} must include readyCriteria")
    if gate.get("status") != "READY" and not str(gate.get("blockingReason", "")).strip():
        fail(failures, f"{gate_id} non-READY gate must include blockingReason")
    if gate.get("status") == "READY" and str(gate.get("blockingReason", "")).strip():
        fail(failures, f"{gate_id} READY gate must not include blockingReason")
    if gate.get("status") == "READY":
        for ref_index, ref in enumerate(as_list(gate.get("currentEvidence"))):
            if is_placeholder_evidence_ref(ref):
                fail(
                    failures,
                    f"{gate_id} READY currentEvidence[{ref_index}] must reference real cutover evidence, not a template/example: {ref}",
                )
            if is_test_environment_evidence_ref(ref):
                fail(
                    failures,
                    f"{gate_id} READY currentEvidence[{ref_index}] must reference production cutover evidence, not test-environment evidence: {ref}",
                )
    return gate_id


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "repoCommit",
        "database",
        "status",
        "sourceLedgers",
        "sourceEvidence",
        "summary",
        "gates",
        "releaseRule",
    ):
        if key not in data:
            fail(failures, f"production cutover gate missing top-level key: {key}")

    if data.get("database") != "PostgreSQL":
        fail(failures, "production cutover gate database must remain PostgreSQL")
    if data.get("status") not in ALLOWED_REPORT_STATUSES:
        fail(failures, f"invalid production cutover gate status: {data.get('status')!r}")
    if not isinstance(data.get("repoCommit"), str) or len(str(data.get("repoCommit", "")).strip()) < 7:
        fail(failures, "repoCommit must identify the code baseline")

    source_ledgers = as_dict(data.get("sourceLedgers"))
    for key, expected in REQUIRED_LEDGER_PATHS.items():
        if source_ledgers.get(key) != expected:
            fail(failures, f"sourceLedgers.{key} must equal {expected}")
        check_repo_artifact(expected, f"sourceLedgers.{key}", failures)
    for ref in as_list(source_ledgers.get("runtimeSmokeReports")):
        check_repo_artifact(ref, "sourceLedgers.runtimeSmokeReports", failures)

    gates = data.get("gates")
    if not isinstance(gates, list):
        fail(failures, "gates must be a list")
        return
    seen: set[str] = set()
    statuses: list[str] = []
    for index, gate in enumerate(gates):
        gate_id = check_gate(index, gate, failures)
        if not gate_id:
            continue
        if gate_id in seen:
            fail(failures, f"duplicate production cutover gate id: {gate_id}")
        seen.add(gate_id)
        if isinstance(gate, dict):
            statuses.append(str(gate.get("status")))

    missing = sorted(REQUIRED_GATE_IDS - seen)
    if missing:
        fail(failures, "production cutover gate missing required gates: " + ", ".join(missing))

    migration_readiness = read_json(ROOT / REQUIRED_LEDGER_PATHS["migrationReadiness"], "production migration readiness ledger", failures)
    production_blockers = read_json(ROOT / REQUIRED_LEDGER_PATHS["productionBlockers"], "production blocker ledger", failures)
    production_backlog = read_json(ROOT / REQUIRED_LEDGER_PATHS["productionBacklog"], "production backlog ledger", failures)
    blocker_count = production_blocker_count(production_blockers, failures) if production_blockers else 0
    backlog_count = production_backlog_count(production_backlog, failures) if production_backlog else 0
    track_statuses = readiness_track_statuses(migration_readiness, failures) if migration_readiness else {}
    for gate in gates:
        if not isinstance(gate, dict):
            continue
        gate_id = str(gate.get("id", "")).strip()
        if gate_id not in REQUIRED_GATE_IDS:
            continue
        track_status = track_statuses.get(gate_id)
        if track_status is None:
            fail(failures, f"production migration readiness ledger missing track for cutover gate: {gate_id}")
        elif gate.get("status") != track_status:
            fail(
                failures,
                f"{gate_id} cutover gate status {gate.get('status')!r} must match migration readiness track status {track_status!r}",
            )

    summary = as_dict(data.get("summary"))
    expected_summary = {
        "totalGates": len(seen),
        "ready": statuses.count("READY"),
        "planned": statuses.count("PLANNED"),
        "blocked": statuses.count("BLOCKED"),
        "notStarted": statuses.count("NOT_STARTED"),
        "productionBlockers": blocker_count,
        "productionBacklogItems": backlog_count,
    }
    for key, expected in expected_summary.items():
        if summary.get(key) != expected:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {expected}")

    if blocker_count > 0:
        business_gate = next((gate for gate in gates if isinstance(gate, dict) and gate.get("id") == "business-smoke-signoff"), {})
        if business_gate.get("status") != "BLOCKED":
            fail(failures, "business-smoke-signoff gate must be BLOCKED while production blockers exist")
        if data.get("status") == "READY_FOR_PRODUCTION_CUTOVER":
            fail(failures, "production cutover cannot be READY while production blockers exist")

    if backlog_count > 0:
        business_gate = next((gate for gate in gates if isinstance(gate, dict) and gate.get("id") == "business-smoke-signoff"), {})
        if business_gate.get("status") != "BLOCKED":
            fail(failures, "business-smoke-signoff gate must be BLOCKED while production backlog exists")
        if data.get("status") == "READY_FOR_PRODUCTION_CUTOVER":
            fail(failures, "production cutover cannot be READY while production backlog exists")

    if migration_readiness and migration_readiness.get("status") != "READY_FOR_PRODUCTION_MIGRATION":
        if data.get("status") == "READY_FOR_PRODUCTION_CUTOVER":
            fail(failures, "cutover cannot be READY while migration readiness is not READY")

    if data.get("status") == "READY_FOR_PRODUCTION_CUTOVER":
        if any(status != "READY" for status in statuses):
            fail(failures, "READY_FOR_PRODUCTION_CUTOVER requires every cutover gate to be READY")
    else:
        if all(status == "READY" for status in statuses) and blocker_count == 0 and backlog_count == 0:
            fail(failures, "all gates are READY and no blockers remain; cutover gate should be READY")
        if "Do not cut over production" not in str(data.get("releaseRule", "")):
            fail(failures, "non-ready cutover gate must include an explicit no-cutover releaseRule")

    check_source_evidence(data, failures)


def main() -> int:
    failures: list[str] = []
    data = read_json(REPORT_PATH, "production cutover gate", failures)
    if REPORT_PATH.exists():
        check_secret_hygiene(REPORT_PATH, failures)
    if data:
        check_report(data, failures)
    if failures:
        print(f"Production cutover gate verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Production cutover gate verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
