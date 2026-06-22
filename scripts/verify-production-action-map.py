#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MAP_PATH = ROOT / "metadata/production-module-source-action-map.json"
DOC_PATH = ROOT / "docs/backend-table-audit/business-production-action-map.md"

TOP_LEVEL_KEYS = [
    "schemaVersion",
    "generatedAt",
    "repoCommit",
    "database",
    "module",
    "sourcePackage",
    "liveDiscoveryReport",
    "summary",
    "runtimeFindings",
    "actions",
]

ACTION_KEYS = [
    "id",
    "name",
    "frontendRoute",
    "sourceViewCode",
    "sourceEvent",
    "method",
    "endpoint",
    "controllerEntry",
    "serviceEntry",
    "targetTables",
    "requiresPersistence",
    "acceptanceStatus",
    "evidence",
    "blockers",
]

ALLOWED_ACTION_STATUSES = {"PASS", "PASS_READ_ONLY", "BLOCKED"}
ALLOWED_RUNTIME_STATUSES = {"PASS_READ_ONLY", "PASS_RENDER_ONLY", "FAIL_RUNTIME_LAYOUT", "FAIL_NOT_FOUND"}


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_json(failures: list[str]) -> dict[str, Any]:
    try:
        with MAP_PATH.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing action map: {MAP_PATH.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {MAP_PATH.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, "production action map must be a JSON object")
        return {}
    return data


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def check_docs(failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"required document missing: {DOC_PATH.relative_to(ROOT)}")
        return
    text = DOC_PATH.read_text(encoding="utf-8")
    for required in (
        "metadata/production-module-source-action-map.json",
        "| 动作 | 前端源事件 | API endpoint | 后端入口 | 目标表 | 当前状态 |",
        "layoutJson",
    ):
        if required not in text:
            fail(failures, f"production action map document missing required text: {required}")


def check_runtime_findings(data: dict[str, Any], failures: list[str]) -> None:
    findings = data.get("runtimeFindings")
    if not isinstance(findings, list):
        fail(failures, "runtimeFindings must be a list")
        return
    for index, item in enumerate(findings):
        if not isinstance(item, dict):
            fail(failures, f"runtimeFindings[{index}] must be an object")
            continue
        if item.get("status") not in ALLOWED_RUNTIME_STATUSES:
            fail(failures, f"runtimeFindings[{index}] has invalid status: {item.get('status')!r}")
        for key in ("route", "evidence"):
            if not str(item.get(key, "")).strip():
                fail(failures, f"runtimeFindings[{index}] missing {key}")


def check_actions(data: dict[str, Any], failures: list[str]) -> None:
    actions = data.get("actions")
    if not isinstance(actions, list):
        fail(failures, "actions must be a list")
        return

    seen: set[str] = set()
    for index, action in enumerate(actions):
        if not isinstance(action, dict):
            fail(failures, f"actions[{index}] must be an object")
            continue
        for key in ACTION_KEYS:
            if key not in action:
                fail(failures, f"actions[{index}] missing required key: {key}")

        action_id = str(action.get("id", "")).strip()
        if not action_id.startswith("WOM-PROD-ACTION-"):
            fail(failures, f"actions[{index}] id must start with WOM-PROD-ACTION-")
        if action_id in seen:
            fail(failures, f"duplicate action id: {action_id}")
        seen.add(action_id)

        status = action.get("acceptanceStatus")
        if status not in ALLOWED_ACTION_STATUSES:
            fail(failures, f"{action_id} has invalid acceptanceStatus: {status!r}")
        if not isinstance(action.get("requiresPersistence"), bool):
            fail(failures, f"{action_id}.requiresPersistence must be boolean")
        if not isinstance(action.get("targetTables"), list):
            fail(failures, f"{action_id}.targetTables must be a list")
        if not isinstance(action.get("blockers"), list):
            fail(failures, f"{action_id}.blockers must be a list")
        if action.get("requiresPersistence") and not as_list(action.get("targetTables")):
            fail(failures, f"{action_id} persistent action must include targetTables")
        if status == "PASS":
            if not str(action.get("evidence", "")).strip():
                fail(failures, f"{action_id} PASS must include evidence")
            if action.get("requiresPersistence") and as_list(action.get("blockers")):
                fail(failures, f"{action_id} persisted PASS must not keep blockers")
        if status == "BLOCKED" and not as_list(action.get("blockers")):
            fail(failures, f"{action_id} BLOCKED must include blockers")
        if status == "PASS_READ_ONLY" and action.get("requiresPersistence"):
            fail(failures, f"{action_id} PASS_READ_ONLY cannot require persistence")


def check_summary(data: dict[str, Any], failures: list[str]) -> None:
    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return
    actions = as_list(data.get("actions"))
    findings = as_list(data.get("runtimeFindings"))
    runtime_blocked_actions = [
        item
        for item in actions
        if isinstance(item, dict)
        and item.get("acceptanceStatus") == "BLOCKED"
        and any(
            "react #130" in str(blocker).lower() or "runtime layout" in str(blocker).lower()
            for blocker in as_list(item.get("blockers"))
        )
    ]
    expected = {
        "sourceActions": len(actions),
        "liveRoutesProbed": len(findings),
        "liveRoutesPass": sum(
            1
            for item in findings
            if isinstance(item, dict) and item.get("status") in {"PASS_READ_ONLY", "PASS_RENDER_ONLY"}
        ),
        "liveRoutesWithErrors": sum(
            1 for item in findings if isinstance(item, dict) and item.get("status") in {"FAIL_RUNTIME_LAYOUT", "FAIL_NOT_FOUND"}
        ),
        "actionsReadyForMarkerPersistence": sum(
            1 for item in actions if isinstance(item, dict) and item.get("requiresPersistence") and item.get("acceptanceStatus") == "PASS"
        ),
        "blockedByRuntimeLayout": len(runtime_blocked_actions),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in TOP_LEVEL_KEYS:
        if key not in data:
            fail(failures, f"report missing top-level key: {key}")
    if data.get("database") != "PostgreSQL":
        fail(failures, "production action map database must remain PostgreSQL")
    if data.get("module") != "production":
        fail(failures, "production action map must identify module=production")
    if "WOM_6.1.3.4" not in str(data.get("sourcePackage", "")):
        fail(failures, "sourcePackage must identify the WOM_6.1.3.4 source evidence")
    check_runtime_findings(data, failures)
    check_actions(data, failures)
    check_summary(data, failures)


def main() -> int:
    failures: list[str] = []
    check_docs(failures)
    data = read_json(failures)
    if data:
        check_report(data, failures)
    if failures:
        print(f"Production action map verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Production action map verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
