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
JSON_PATH = ROOT / "metadata/goal-gap-register.json"
DOC_PATH = ROOT / "docs/goal-gap-register.md"

SOURCE_LEDGER_PATHS = {
    "projectGoalAcceptance": "metadata/project-goal-acceptance.json",
    "productionModuleTestCases": "metadata/production-module-test-cases.json",
    "productionModuleBlockers": "metadata/production-module-blockers.json",
    "productionModuleBacklog": "metadata/production-module-backlog.json",
    "businessDependencyReadiness": "metadata/business-dependency-readiness-smoke.json",
    "businessPackageScan": "metadata/business-dependency-package-scan.json",
    "productionExportReadiness": "metadata/production-export-readiness-smoke.json",
    "productionMigrationReadiness": "metadata/production-migration-readiness.json",
    "productionCutoverGate": "metadata/production-cutover-gate.json",
    "productionRehearsalPlan": "metadata/production-rehearsal-plan.json",
    "persistenceAcceptance": "metadata/persistence-acceptance.json",
}


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


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


def first_line(value: Any, default: str = "") -> str:
    if isinstance(value, list) and value:
        return str(value[0])
    if isinstance(value, str):
        return value
    return default


def count_status(items: list[dict[str, Any]], key: str) -> dict[str, int]:
    counts: dict[str, int] = {}
    for item in items:
        status = str(item.get(key, "")).strip() or "UNKNOWN"
        counts[status] = counts.get(status, 0) + 1
    return counts


def source_exists(relative_path: str) -> bool:
    path = ROOT / relative_path
    return path.exists() and path.is_file()


def project_goal_gaps(project_goal: dict[str, Any]) -> list[dict[str, Any]]:
    gaps: list[dict[str, Any]] = []
    for item in as_list(project_goal.get("items")):
        if not isinstance(item, dict):
            continue
        status = str(item.get("status", "")).strip()
        if status == "READY":
            continue
        gaps.append(
            {
                "id": item.get("id"),
                "title": item.get("title"),
                "status": status,
                "scope": item.get("scope"),
                "currentEvidence": as_list(item.get("currentEvidence")),
                "blockingIssues": as_list(item.get("blockingIssues")),
                "nextActions": as_list(item.get("nextActions")),
                "artifacts": as_list(item.get("artifacts")),
            }
        )
    return gaps


def production_blockers(blockers: dict[str, Any]) -> list[dict[str, Any]]:
    values: list[dict[str, Any]] = []
    for blocker in as_list(blockers.get("blockers")):
        if not isinstance(blocker, dict):
            continue
        values.append(
            {
                "caseId": blocker.get("caseId"),
                "title": blocker.get("title"),
                "status": blocker.get("status"),
                "category": blocker.get("blockerCategory") or blocker.get("category"),
                "dependency": blocker.get("dependency"),
                "evidenceRefs": as_list(blocker.get("currentEvidenceRefs")),
                "recheckCommands": as_list(blocker.get("recheckCommands")),
                "passCriteria": as_list(blocker.get("passCriteria")),
                "nextActions": as_list(blocker.get("nextActions")),
                "nonSolutions": as_list(blocker.get("nonSolutions")),
            }
        )
    return values


def production_backlog_items(backlog: dict[str, Any]) -> list[dict[str, Any]]:
    values: list[dict[str, Any]] = []
    for item in as_list(backlog.get("items")):
        if not isinstance(item, dict):
            continue
        values.append(
            {
                "id": item.get("id"),
                "title": item.get("title"),
                "status": item.get("status"),
                "category": item.get("category"),
                "disposition": item.get("disposition"),
                "isPostgresCompatibilityGap": item.get("isPostgresCompatibilityGap"),
                "productionCaseIds": as_list(item.get("productionCaseIds")),
                "evidenceRefs": as_list(item.get("evidenceRefs")),
                "recheckCommands": as_list(item.get("recheckCommands")),
                "passCriteria": as_list(item.get("passCriteria")),
                "nextActions": as_list(item.get("nextActions")),
                "nonSolutions": as_list(item.get("nonSolutions")),
            }
        )
    return values


def dependency_readiness_items(readiness: dict[str, Any]) -> list[dict[str, Any]]:
    values: list[dict[str, Any]] = []
    for item in as_list(readiness.get("items")):
        if not isinstance(item, dict):
            continue
        services = [
            {
                "serviceName": service.get("serviceName"),
                "healthyHostCount": service.get("healthyHostCount"),
            }
            for service in as_list(item.get("services"))
            if isinstance(service, dict)
        ]
        endpoints = [
            {
                "name": endpoint.get("name"),
                "method": endpoint.get("method"),
                "path": endpoint.get("path"),
                "status": endpoint.get("status"),
                "tenantServiceMissing": endpoint.get("tenantServiceMissing"),
            }
            for endpoint in as_list(item.get("endpoints"))
            if isinstance(endpoint, dict)
        ]
        values.append(
            {
                "id": item.get("id"),
                "title": item.get("title"),
                "status": item.get("status"),
                "requiredFor": as_list(item.get("requiredFor")),
                "readyWhen": item.get("readyWhen"),
                "services": services,
                "endpoints": endpoints,
                "issues": as_list(item.get("issues")),
                "nextAction": item.get("nextAction"),
            }
        )
    return values


def migration_track_gaps(
    readiness: dict[str, Any],
    cutover: dict[str, Any],
    rehearsal: dict[str, Any],
) -> list[dict[str, Any]]:
    cutover_by_id = {
        str(gate.get("id")): gate
        for gate in as_list(cutover.get("gates"))
        if isinstance(gate, dict) and gate.get("id")
    }
    rehearsal_by_id = {
        str(track.get("id")): track
        for track in as_list(rehearsal.get("tracks"))
        if isinstance(track, dict) and track.get("id")
    }
    values: list[dict[str, Any]] = []
    for track in as_list(readiness.get("tracks")):
        if not isinstance(track, dict):
            continue
        track_id = str(track.get("id", "")).strip()
        if not track_id:
            continue
        gate = as_dict(cutover_by_id.get(track_id))
        rehearsal_track = as_dict(rehearsal_by_id.get(track_id))
        statuses = {
            "readiness": track.get("status"),
            "cutoverGate": gate.get("status"),
            "rehearsal": rehearsal_track.get("rehearsalStatus"),
        }
        if all(status == "READY" or status == "READY_TO_REHEARSE" for status in statuses.values()):
            continue
        values.append(
            {
                "id": track_id,
                "title": track.get("title"),
                "owner": track.get("owner"),
                "statuses": statuses,
                "currentEvidence": as_list(track.get("currentEvidence")),
                "requiredEvidence": as_list(track.get("requiredEvidence")),
                "readyEvidenceCommands": as_list(gate.get("readyEvidenceCommands")),
                "blockingIssues": as_list(track.get("blockingIssues"))
                + ([gate.get("blockingReason")] if gate.get("blockingReason") else []),
                "nextActions": as_list(track.get("nextActions")),
            }
        )
    return values


def production_case_status_summary(matrix: dict[str, Any]) -> dict[str, int]:
    cases = [case for case in as_list(matrix.get("cases")) if isinstance(case, dict)]
    return count_status(cases, "acceptanceStatus")


def build_register() -> dict[str, Any]:
    ledgers = {key: read_json(path) for key, path in SOURCE_LEDGER_PATHS.items()}
    project_goal = ledgers["projectGoalAcceptance"]
    matrix = ledgers["productionModuleTestCases"]
    blockers = ledgers["productionModuleBlockers"]
    backlog = ledgers["productionModuleBacklog"]
    dependency_readiness = ledgers["businessDependencyReadiness"]
    production_export = ledgers["productionExportReadiness"]
    migration_readiness = ledgers["productionMigrationReadiness"]
    cutover = ledgers["productionCutoverGate"]
    rehearsal = ledgers["productionRehearsalPlan"]

    goal_gaps = project_goal_gaps(project_goal)
    blocker_items = production_blockers(blockers)
    backlog_items = production_backlog_items(backlog)
    dependency_items = dependency_readiness_items(dependency_readiness)
    migration_gaps = migration_track_gaps(migration_readiness, cutover, rehearsal)
    case_status_counts = production_case_status_summary(matrix)

    project_summary = as_dict(project_goal.get("summary"))
    blocker_summary = as_dict(blockers.get("summary"))
    backlog_summary = as_dict(backlog.get("summary"))
    dependency_summary = as_dict(dependency_readiness.get("summary"))
    export_summary = as_dict(production_export.get("summary"))
    migration_summary = as_dict(migration_readiness.get("summary"))
    cutover_summary = as_dict(cutover.get("summary"))
    rehearsal_summary = as_dict(rehearsal.get("summary"))

    status = "IN_PROGRESS_NOT_COMPLETE"
    if not goal_gaps and not blocker_items and not backlog_items and not migration_gaps:
        status = "COMPLETE_REVIEW_REQUIRED"

    return {
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "repoCommit": git_head(),
        "database": "PostgreSQL",
        "status": status,
        "sourceLedgers": SOURCE_LEDGER_PATHS,
        "summary": {
            "goalItems": project_summary.get("totalItems"),
            "readyGoals": project_summary.get("ready"),
            "partialGoals": project_summary.get("partial"),
            "blockedGoals": project_summary.get("blocked"),
            "goalGaps": len(goal_gaps),
            "productionCasesByStatus": case_status_counts,
            "productionBlockedCases": blocker_summary.get("blockedCases"),
            "productionBlockers": blocker_summary.get("blockers"),
            "productionBacklogItems": backlog_summary.get("totalItems"),
            "dependencyStatus": dependency_summary.get("status"),
            "dependencyBlocked": dependency_summary.get("blocked"),
            "productionExportStatus": export_summary.get("status"),
            "productionExportReady": export_summary.get("ready"),
            "productionExportActionRequired": export_summary.get("actionRequired"),
            "productionExportBlocked": export_summary.get("blocked"),
            "productionExportVerifiedDataExports": export_summary.get("verifiedDataExports"),
            "migrationReadinessStatus": migration_readiness.get("status"),
            "migrationTracksPlanned": migration_summary.get("planned"),
            "migrationTracksBlocked": migration_summary.get("blocked"),
            "cutoverGateStatus": cutover.get("status"),
            "cutoverGatesPlanned": cutover_summary.get("planned"),
            "cutoverGatesBlocked": cutover_summary.get("blocked"),
            "rehearsalStatus": rehearsal.get("status"),
            "rehearsalTracksPlanned": rehearsal_summary.get("planned"),
            "rehearsalTracksBlocked": rehearsal_summary.get("blocked"),
        },
        "goalGaps": goal_gaps,
        "productionBlockers": blocker_items,
        "productionBacklog": backlog_items,
        "dependencyReadiness": dependency_items,
        "productionMigrationGaps": migration_gaps,
        "releaseRule": (
            "Do not mark the project complete while any goal gap, production blocker, production backlog item, "
            "business dependency blocker, production export blocker, or production migration gap remains open. "
            "A local PASS must be backed by real browser/API/database or file-response evidence and the relevant "
            "source ledger must be regenerated before this register can change."
        ),
    }


def markdown_list(values: list[Any], limit: int = 3) -> str:
    clean = [str(value).replace("\n", " ").strip() for value in values if str(value).strip()]
    if not clean:
        return ""
    shown = clean[:limit]
    suffix = f" (+{len(clean) - limit})" if len(clean) > limit else ""
    return "<br>".join(shown) + suffix


def render_markdown(register: dict[str, Any]) -> str:
    summary = as_dict(register.get("summary"))
    lines = [
        "# Goal Gap Register",
        "",
        "This file is generated by `scripts/generate-goal-gap-register.py`.",
        "It summarizes the remaining gaps that prevent the ADP/MES repository from being called fully sustainable or production-ready.",
        "",
        "## Summary",
        "",
        "| Field | Value |",
        "| --- | --- |",
        f"| Generated At | `{register.get('generatedAt')}` |",
        f"| Repo Commit | `{register.get('repoCommit')}` |",
        f"| Database Target | `{register.get('database')}` |",
        f"| Status | `{register.get('status')}` |",
        f"| Goal Gaps | `{summary.get('goalGaps')}` |",
        f"| Ready / Partial / Blocked Goals | `{summary.get('readyGoals')} / {summary.get('partialGoals')} / {summary.get('blockedGoals')}` |",
        f"| Production Blocked Cases | `{summary.get('productionBlockedCases')}` |",
        f"| Production Backlog Items | `{summary.get('productionBacklogItems')}` |",
        f"| Business Dependency Status | `{summary.get('dependencyStatus')}` |",
        f"| Production Export Status | `{summary.get('productionExportStatus')}` |",
        f"| Production Export Ready / Action Required / Blocked | `{summary.get('productionExportReady')} / {summary.get('productionExportActionRequired')} / {summary.get('productionExportBlocked')}` |",
        f"| Production Export Verified Data Files | `{summary.get('productionExportVerifiedDataExports')}` |",
        f"| Production Migration Status | `{summary.get('migrationReadinessStatus')}` |",
        f"| Cutover Gate Status | `{summary.get('cutoverGateStatus')}` |",
        f"| Rehearsal Status | `{summary.get('rehearsalStatus')}` |",
        "",
        "## Goal Gaps",
        "",
        "| ID | Status | Title | Main Blocker | Next Action |",
        "| --- | --- | --- | --- | --- |",
    ]
    for gap in as_list(register.get("goalGaps")):
        if not isinstance(gap, dict):
            continue
        lines.append(
            "| `{id}` | `{status}` | {title} | {blocker} | {next_action} |".format(
                id=gap.get("id"),
                status=gap.get("status"),
                title=gap.get("title"),
                blocker=first_line(gap.get("blockingIssues"), ""),
                next_action=first_line(gap.get("nextActions"), ""),
            )
        )

    lines.extend(
        [
            "",
            "## Production Blockers",
            "",
            "| Case | Category | Dependency | Recheck | PASS Criteria |",
            "| --- | --- | --- | --- | --- |",
        ]
    )
    for blocker in as_list(register.get("productionBlockers")):
        if not isinstance(blocker, dict):
            continue
        lines.append(
            "| `{case}` | `{category}` | {dependency} | {recheck} | {criteria} |".format(
                case=blocker.get("caseId"),
                category=blocker.get("category"),
                dependency=blocker.get("dependency"),
                recheck=markdown_list(as_list(blocker.get("recheckCommands")), 2) or "",
                criteria=markdown_list(as_list(blocker.get("passCriteria")), 2) or "",
            )
        )

    lines.extend(
        [
            "",
            "## Production Backlog",
            "",
            "| ID | Status | Category | Disposition | PostgreSQL Gap | PASS Criteria |",
            "| --- | --- | --- | --- | --- | --- |",
        ]
    )
    for item in as_list(register.get("productionBacklog")):
        if not isinstance(item, dict):
            continue
        lines.append(
            "| `{id}` | `{status}` | `{category}` | `{disposition}` | `{pg}` | {criteria} |".format(
                id=item.get("id"),
                status=item.get("status"),
                category=item.get("category"),
                disposition=item.get("disposition"),
                pg=item.get("isPostgresCompatibilityGap"),
                criteria=markdown_list(as_list(item.get("passCriteria")), 2) or "",
            )
        )

    lines.extend(
        [
            "",
            "## Business Dependency Readiness",
            "",
            "| Dependency | Status | Required For | Ready When | Issues |",
            "| --- | --- | --- | --- | --- |",
        ]
    )
    for item in as_list(register.get("dependencyReadiness")):
        if not isinstance(item, dict):
            continue
        lines.append(
            "| `{id}` | `{status}` | {required_for} | {ready_when} | {issues} |".format(
                id=item.get("id"),
                status=item.get("status"),
                required_for=markdown_list(as_list(item.get("requiredFor")), 4),
                ready_when=item.get("readyWhen"),
                issues=markdown_list(as_list(item.get("issues")), 3),
            )
        )

    lines.extend(
        [
            "",
            "## Production Migration Gaps",
            "",
            "| Track | Owner | Readiness | Cutover | Rehearsal | First Required Evidence | First Command |",
            "| --- | --- | --- | --- | --- | --- | --- |",
        ]
    )
    for track in as_list(register.get("productionMigrationGaps")):
        if not isinstance(track, dict):
            continue
        statuses = as_dict(track.get("statuses"))
        lines.append(
            "| `{id}` | `{owner}` | `{readiness}` | `{cutover}` | `{rehearsal}` | {evidence} | {command} |".format(
                id=track.get("id"),
                owner=track.get("owner"),
                readiness=statuses.get("readiness"),
                cutover=statuses.get("cutoverGate"),
                rehearsal=statuses.get("rehearsal"),
                evidence=first_line(track.get("requiredEvidence"), ""),
                command=first_line(track.get("readyEvidenceCommands"), ""),
            )
        )

    lines.extend(
        [
            "",
            "## Source Ledgers",
            "",
            "| Ledger | Path |",
            "| --- | --- |",
        ]
    )
    for key, path in sorted(as_dict(register.get("sourceLedgers")).items()):
        lines.append(f"| `{key}` | `{path}` |")

    lines.extend(
        [
            "",
            "## Release Rule",
            "",
            str(register.get("releaseRule")),
            "",
        ]
    )
    return "\n".join(lines)


def write_outputs(register: dict[str, Any]) -> None:
    JSON_PATH.parent.mkdir(parents=True, exist_ok=True)
    DOC_PATH.parent.mkdir(parents=True, exist_ok=True)
    JSON_PATH.write_text(json.dumps(register, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    DOC_PATH.write_text(render_markdown(register), encoding="utf-8")


def stable_for_check(register: dict[str, Any], existing: dict[str, Any]) -> dict[str, Any]:
    current = json.loads(json.dumps(register))
    current["generatedAt"] = existing.get("generatedAt")
    return current


def validate_register(register: dict[str, Any], failures: list[str]) -> None:
    for key, path in as_dict(register.get("sourceLedgers")).items():
        if not source_exists(str(path)):
            failures.append(f"source ledger {key} does not exist: {path}")

    goal_gaps = as_list(register.get("goalGaps"))
    production_blockers_values = as_list(register.get("productionBlockers"))
    production_backlog_values = as_list(register.get("productionBacklog"))
    dependency_values = as_list(register.get("dependencyReadiness"))
    migration_values = as_list(register.get("productionMigrationGaps"))
    summary = as_dict(register.get("summary"))

    if summary.get("goalGaps") != len(goal_gaps):
        failures.append("summary.goalGaps does not match goalGaps length")
    if summary.get("productionBlockers") != len(production_blockers_values):
        failures.append("summary.productionBlockers does not match productionBlockers length")
    if summary.get("productionBacklogItems") != len(production_backlog_values):
        failures.append("summary.productionBacklogItems does not match productionBacklog length")

    if goal_gaps and register.get("status") == "COMPLETE_REVIEW_REQUIRED":
        failures.append("register cannot be complete while goal gaps remain")

    for gap in goal_gaps:
        if not isinstance(gap, dict):
            failures.append("goalGaps entries must be objects")
            continue
        if not gap.get("id") or not gap.get("status"):
            failures.append("goal gap entries must include id and status")
        if not as_list(gap.get("blockingIssues")):
            failures.append(f"{gap.get('id')} must include blockingIssues")
        if not as_list(gap.get("nextActions")):
            failures.append(f"{gap.get('id')} must include nextActions")

    for blocker in production_blockers_values:
        if not isinstance(blocker, dict):
            failures.append("productionBlockers entries must be objects")
            continue
        if not blocker.get("caseId") or blocker.get("status") != "BLOCKED":
            failures.append("production blockers must include BLOCKED caseId")
        if not as_list(blocker.get("recheckCommands")):
            failures.append(f"{blocker.get('caseId')} must include recheckCommands")
        if not as_list(blocker.get("passCriteria")):
            failures.append(f"{blocker.get('caseId')} must include passCriteria")

    for item in production_backlog_values:
        if not isinstance(item, dict):
            failures.append("productionBacklog entries must be objects")
            continue
        if not item.get("id") or item.get("status") not in {"BLOCKED", "FAIL_BACKLOG"}:
            failures.append("production backlog entries must include open id/status")
        if not as_list(item.get("passCriteria")):
            failures.append(f"{item.get('id')} must include passCriteria")

    for dependency in dependency_values:
        if not isinstance(dependency, dict):
            failures.append("dependencyReadiness entries must be objects")
            continue
        if dependency.get("status") != "BLOCKED":
            failures.append(f"{dependency.get('id')} should remain BLOCKED until readiness source changes")
        if not as_list(dependency.get("issues")):
            failures.append(f"{dependency.get('id')} must include issues")

    for track in migration_values:
        if not isinstance(track, dict):
            failures.append("productionMigrationGaps entries must be objects")
            continue
        if not track.get("id"):
            failures.append("production migration gap must include id")
        if not as_list(track.get("requiredEvidence")):
            failures.append(f"{track.get('id')} must include requiredEvidence")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="check generated outputs are current")
    args = parser.parse_args()

    register = build_register()
    failures: list[str] = []
    validate_register(register, failures)
    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1

    if args.check:
        if not JSON_PATH.exists() or not DOC_PATH.exists():
            print("Goal gap register outputs are missing. Run: make goal-gap-register", file=sys.stderr)
            return 1
        try:
            existing = json.loads(JSON_PATH.read_text(encoding="utf-8"))
        except json.JSONDecodeError as error:
            print(f"Goal gap register JSON is invalid: {error}", file=sys.stderr)
            return 1
        if existing != stable_for_check(register, existing):
            print("Goal gap register JSON is stale. Run: make goal-gap-register", file=sys.stderr)
            return 1
        expected_doc = render_markdown(existing)
        if DOC_PATH.read_text(encoding="utf-8") != expected_doc:
            print("Goal gap register document is stale. Run: make goal-gap-register", file=sys.stderr)
            return 1
        print("Goal gap register is current.")
        return 0

    write_outputs(register)
    print(f"Wrote {JSON_PATH.relative_to(ROOT)}")
    print(f"Wrote {DOC_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
