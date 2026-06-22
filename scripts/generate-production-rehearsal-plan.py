#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
JSON_PATH = ROOT / "metadata/production-rehearsal-plan.json"
DOC_PATH = ROOT / "docs/production-migration/rehearsal-plan.md"

SOURCE_LEDGER_PATHS = {
    "migrationReadiness": "metadata/production-migration-readiness.json",
    "cutoverGate": "metadata/production-cutover-gate.json",
    "productionBlockers": "metadata/production-module-blockers.json",
    "productionBacklog": "metadata/production-module-backlog.json",
    "testEnvironmentSmoke": "metadata/test-environment-smoke.json",
    "platformValidationSmoke": "metadata/platform-validation-smoke.json",
    "postgresRuntimeSmoke": "metadata/postgres-runtime-smoke.json",
    "nacosConfigSmoke": "metadata/nacos-config-drift-smoke.json",
    "keycloakJwtSmoke": "metadata/keycloak-jwt-runtime-smoke.json",
    "minioRuntimeSmoke": "metadata/minio-runtime-smoke.json",
    "businessDependencyReadiness": "metadata/business-dependency-readiness-smoke.json",
    "businessPackageScan": "metadata/business-dependency-package-scan.json",
    "productionExportReadiness": "metadata/production-export-readiness-smoke.json",
}

TRACK_ORDER = [
    "postgres-data-migration",
    "rollback-plan",
    "license-strategy",
    "minio-file-migration",
    "keycloak-production-db",
    "nacos-runtime-patch",
    "ports-domain-tls",
    "security-hardening",
    "business-smoke-signoff",
]

RUNTIME_REPORTS_BY_TRACK = {
    "postgres-data-migration": ["metadata/postgres-runtime-smoke.json"],
    "minio-file-migration": ["metadata/minio-runtime-smoke.json"],
    "keycloak-production-db": ["metadata/keycloak-jwt-runtime-smoke.json"],
    "nacos-runtime-patch": [
        "metadata/nacos-config-drift-smoke.json",
        "metadata/runtime-patch-manifest.json",
    ],
    "ports-domain-tls": ["metadata/test-environment-smoke.json"],
    "business-smoke-signoff": [
        "metadata/platform-validation-smoke.json",
        "metadata/business-dependency-readiness-smoke.json",
        "metadata/business-dependency-package-scan.json",
        "metadata/production-export-readiness-smoke.json",
        "metadata/production-module-blockers.json",
        "metadata/production-module-backlog.json",
    ],
}


def read_json(relative_path: str) -> dict[str, Any]:
    path = ROOT / relative_path
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return {}
    if not isinstance(data, dict):
        return {}
    return data


def git_head() -> str:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "HEAD"],
            cwd=ROOT,
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except (OSError, subprocess.CalledProcessError):
        return "unknown"


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
        "cutoverGate": [
            "totalGates",
            "ready",
            "planned",
            "blocked",
            "notStarted",
            "productionBlockers",
            "productionBacklogItems",
        ],
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


def source_evidence_entry(key: str, relative_path: str) -> dict[str, Any]:
    data = read_json(relative_path)
    return {
        "path": relative_path,
        "generatedAt": data.get("generatedAt"),
        "status": source_status(data),
        "metrics": compact_metrics(key, data),
    }


def build_source_evidence() -> dict[str, Any]:
    return {
        key: source_evidence_entry(key, relative_path)
        for key, relative_path in SOURCE_LEDGER_PATHS.items()
    }


def track_status(readiness_status: str, cutover_status: str) -> str:
    if readiness_status == "READY" and cutover_status == "READY":
        return "READY_TO_REHEARSE"
    if readiness_status == "BLOCKED" or cutover_status == "BLOCKED":
        return "BLOCKED"
    if readiness_status == "NOT_STARTED" or cutover_status == "NOT_STARTED":
        return "NOT_STARTED"
    return "PLANNED"


def track_by_id(readiness: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(track.get("id")): track
        for track in as_list(readiness.get("tracks"))
        if isinstance(track, dict) and track.get("id")
    }


def gate_by_id(cutover: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(gate.get("id")): gate
        for gate in as_list(cutover.get("gates"))
        if isinstance(gate, dict) and gate.get("id")
    }


def smoke_environment(test_environment: dict[str, Any]) -> dict[str, Any]:
    environment = as_dict(test_environment.get("environment"))
    return {
        "sshHost": environment.get("sshHost"),
        "sshUser": environment.get("sshUser"),
        "baseUrl": environment.get("baseUrl"),
        "secondaryBaseUrl": environment.get("secondaryBaseUrl"),
        "sourceReport": "metadata/test-environment-smoke.json",
    }


def current_blockers(blockers: list[Any], gate: dict[str, Any]) -> list[str]:
    values = [str(item) for item in blockers if str(item).strip()]
    blocking_reason = str(gate.get("blockingReason", "")).strip()
    if blocking_reason:
        values.append(blocking_reason)
    return values


def build_plan() -> dict[str, Any]:
    readiness = read_json(SOURCE_LEDGER_PATHS["migrationReadiness"])
    cutover = read_json(SOURCE_LEDGER_PATHS["cutoverGate"])
    production_blockers = read_json(SOURCE_LEDGER_PATHS["productionBlockers"])
    production_backlog = read_json(SOURCE_LEDGER_PATHS["productionBacklog"])
    test_environment = read_json(SOURCE_LEDGER_PATHS["testEnvironmentSmoke"])

    readiness_tracks = track_by_id(readiness)
    cutover_gates = gate_by_id(cutover)

    tracks: list[dict[str, Any]] = []
    for track_id in TRACK_ORDER:
        readiness_track = readiness_tracks.get(track_id, {})
        cutover_gate = cutover_gates.get(track_id, {})
        rehearsal_status = track_status(
            str(readiness_track.get("status", "")),
            str(cutover_gate.get("status", "")),
        )
        artifacts = list(dict.fromkeys(
            [str(item) for item in as_list(readiness_track.get("artifacts"))]
            + [str(item) for item in as_list(cutover_gate.get("currentEvidence"))]
            + RUNTIME_REPORTS_BY_TRACK.get(track_id, [])
        ))
        tracks.append(
            {
                "id": track_id,
                "title": readiness_track.get("title") or track_id,
                "rehearsalStatus": rehearsal_status,
                "readinessStatus": readiness_track.get("status"),
                "cutoverGateStatus": cutover_gate.get("status"),
                "currentEvidence": as_list(readiness_track.get("currentEvidence")),
                "requiredEvidence": as_list(readiness_track.get("requiredEvidence")),
                "rehearsalCommands": as_list(cutover_gate.get("readyEvidenceCommands")),
                "blockingIssues": current_blockers(as_list(readiness_track.get("blockingIssues")), cutover_gate),
                "nextActions": as_list(readiness_track.get("nextActions")),
                "artifacts": artifacts,
            }
        )

    statuses = [str(track["rehearsalStatus"]) for track in tracks]
    status = "REHEARSAL_PLAN_OPEN"
    if "BLOCKED" in statuses:
        status = "REHEARSAL_BLOCKED"
    elif tracks and all(value == "READY_TO_REHEARSE" for value in statuses):
        status = "READY_FOR_REHEARSAL"

    backlog_summary = as_dict(production_backlog.get("summary"))
    cutover_summary = as_dict(cutover.get("summary"))
    blocker_summary = as_dict(production_blockers.get("summary"))

    return {
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "repoCommit": git_head(),
        "database": "PostgreSQL",
        "status": status,
        "environment": smoke_environment(test_environment),
        "sourceLedgers": SOURCE_LEDGER_PATHS,
        "sourceEvidence": build_source_evidence(),
        "summary": {
            "totalTracks": len(tracks),
            "readyToRehearse": statuses.count("READY_TO_REHEARSE"),
            "planned": statuses.count("PLANNED"),
            "blocked": statuses.count("BLOCKED"),
            "notStarted": statuses.count("NOT_STARTED"),
            "productionBlockers": cutover_summary.get("productionBlockers")
            or blocker_summary.get("blockers")
            or blocker_summary.get("blockedCases")
            or 0,
            "productionBacklogItems": cutover_summary.get("productionBacklogItems")
            or backlog_summary.get("totalItems")
            or 0,
        },
        "tracks": tracks,
        "releaseRule": (
            "Do not promote production from this plan. A production rehearsal is ready only after every track is "
            "READY_TO_REHEARSE, real non-template evidence files exist, production blockers are zero, and the cutover "
            "gate is READY_FOR_PRODUCTION_CUTOVER."
        ),
    }


def render_markdown(plan: dict[str, Any]) -> str:
    env = as_dict(plan.get("environment"))
    summary = as_dict(plan.get("summary"))
    lines = [
        "# Production Rehearsal Plan",
        "",
        "This file is generated by `scripts/generate-production-rehearsal-plan.py`.",
        "It is an execution checklist for collecting production rehearsal evidence.",
        "It does not mark the system ready for production.",
        "",
        "## Summary",
        "",
        "| Field | Value |",
        "| --- | --- |",
        f"| Generated At | `{plan.get('generatedAt')}` |",
        f"| Repo Commit | `{plan.get('repoCommit')}` |",
        f"| Database Target | `{plan.get('database')}` |",
        f"| Status | `{plan.get('status')}` |",
        f"| Test SSH Host | `{env.get('sshHost')}` |",
        f"| Test Base URL | `{env.get('baseUrl')}` |",
        f"| Total Tracks | `{summary.get('totalTracks')}` |",
        f"| Ready To Rehearse | `{summary.get('readyToRehearse')}` |",
        f"| Planned | `{summary.get('planned')}` |",
        f"| Blocked | `{summary.get('blocked')}` |",
        f"| Production Blockers | `{summary.get('productionBlockers')}` |",
        f"| Production Backlog Items | `{summary.get('productionBacklogItems')}` |",
        "",
        "## Source Evidence Freshness",
        "",
        "| Evidence | Path | Generated At | Status | Key Metrics |",
        "| --- | --- | --- | --- | --- |",
    ]
    for key, evidence in sorted(as_dict(plan.get("sourceEvidence")).items()):
        if not isinstance(evidence, dict):
            continue
        metrics = ", ".join(
            f"{metric_key}={metric_value}"
            for metric_key, metric_value in sorted(as_dict(evidence.get("metrics")).items())
        )
        lines.append(
            f"| `{key}` | `{evidence.get('path')}` | `{evidence.get('generatedAt')}` | `{evidence.get('status')}` | {metrics or 'None'} |"
        )

    lines.extend([
        "",
        "## Track Checklist",
        "",
        "| Track | Rehearsal Status | Readiness | Cutover Gate | Evidence Commands | Current Blocker |",
        "| --- | --- | --- | --- | --- | --- |",
    ])
    for track in as_list(plan.get("tracks")):
        if not isinstance(track, dict):
            continue
        commands = "<br>".join(f"`{command}`" for command in as_list(track.get("rehearsalCommands"))) or "`TBD`"
        blockers = "<br>".join(str(item) for item in as_list(track.get("blockingIssues"))) or "None"
        lines.append(
            "| `{id}` | `{status}` | `{readiness}` | `{cutover}` | {commands} | {blockers} |".format(
                id=track.get("id"),
                status=track.get("rehearsalStatus"),
                readiness=track.get("readinessStatus"),
                cutover=track.get("cutoverGateStatus"),
                commands=commands,
                blockers=blockers,
            )
        )

    lines.extend(
        [
            "",
            "## Required Source Ledgers",
            "",
            "| Ledger | Path |",
            "| --- | --- |",
        ]
    )
    for key, value in sorted(as_dict(plan.get("sourceLedgers")).items()):
        lines.append(f"| `{key}` | `{value}` |")

    lines.extend(
        [
            "",
            "## Release Rule",
            "",
            str(plan.get("releaseRule")),
            "",
        ]
    )
    return "\n".join(lines)


def write_outputs(plan: dict[str, Any]) -> None:
    JSON_PATH.parent.mkdir(parents=True, exist_ok=True)
    DOC_PATH.parent.mkdir(parents=True, exist_ok=True)
    JSON_PATH.write_text(json.dumps(plan, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    DOC_PATH.write_text(render_markdown(plan), encoding="utf-8")


def stable_for_check(plan: dict[str, Any], existing: dict[str, Any]) -> dict[str, Any]:
    current = json.loads(json.dumps(plan))
    current["generatedAt"] = existing.get("generatedAt")
    return current


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="check generated outputs are current")
    args = parser.parse_args()

    plan = build_plan()
    if args.check:
        if not JSON_PATH.exists() or not DOC_PATH.exists():
            print("Production rehearsal plan outputs are missing. Run: make production-rehearsal-plan", file=sys.stderr)
            return 1
        try:
            existing = json.loads(JSON_PATH.read_text(encoding="utf-8"))
        except json.JSONDecodeError as error:
            print(f"Production rehearsal plan JSON is invalid: {error}", file=sys.stderr)
            return 1
        if existing != stable_for_check(plan, existing):
            print("Production rehearsal plan JSON is stale. Run: make production-rehearsal-plan", file=sys.stderr)
            return 1
        expected_doc = render_markdown(existing)
        if DOC_PATH.read_text(encoding="utf-8") != expected_doc:
            print("Production rehearsal plan document is stale. Run: make production-rehearsal-plan", file=sys.stderr)
            return 1
        print("Production rehearsal plan is current.")
        return 0

    write_outputs(plan)
    print(f"Wrote {JSON_PATH.relative_to(ROOT)}")
    print(f"Wrote {DOC_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
