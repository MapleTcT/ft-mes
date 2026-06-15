#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/project-goal-acceptance.json"
DOC_PATH = ROOT / "docs/project-goal-acceptance.md"

REQUIRED_ITEM_IDS = {
    "G-001",
    "G-002",
    "G-003",
    "G-004",
    "G-005",
    "G-006",
    "G-007",
    "G-008",
    "G-009",
    "G-010",
    "G-011",
    "G-012",
    "G-013",
    "G-014",
    "G-015",
    "G-016",
    "G-017",
    "G-018",
    "G-019",
    "G-020",
}

ALLOWED_STATUSES = {"READY", "PARTIAL", "BLOCKED", "FAIL", "NOT_STARTED"}
ALLOWED_OVERALL_STATUSES = {"IN_PROGRESS_NOT_COMPLETE", "COMPLETE"}
REQUIRED_ITEM_KEYS = [
    "id",
    "title",
    "status",
    "scope",
    "artifacts",
    "currentEvidence",
    "blockingIssues",
    "nextActions",
]


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def read_json(failures: list[str]) -> dict[str, Any]:
    try:
        with REPORT_PATH.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing goal acceptance report: {REPORT_PATH.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {REPORT_PATH.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, "goal acceptance report must be a JSON object")
        return {}
    return data


def check_doc(failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"required document missing: {DOC_PATH.relative_to(ROOT)}")
        return
    text = DOC_PATH.read_text(encoding="utf-8")
    required_fragments = [
        "IN_PROGRESS_NOT_COMPLETE",
        "| ID | 目标项 | 状态 | 当前证据 | 缺口 |",
        "生产模块完整功能",
        "Oracle 替换为 PostgreSQL 默认路径",
        "PostgreSQL 缺口进入幂等 SQL/backlog",
        "生产迁移前置项",
        "make project-goal-acceptance-check",
    ]
    for fragment in required_fragments:
        if fragment not in text:
            fail(failures, f"goal acceptance document missing required text: {fragment}")


def check_artifact(item_id: str, artifact: Any, failures: list[str]) -> None:
    if not isinstance(artifact, str) or not artifact.strip():
        fail(failures, f"{item_id}.artifacts must contain non-empty relative paths")
        return
    path = Path(artifact)
    if path.is_absolute() or ".." in path.parts:
        fail(failures, f"{item_id} artifact must stay inside the repository: {artifact}")
        return
    if not (ROOT / path).exists():
        fail(failures, f"{item_id} artifact does not exist: {artifact}")


def check_item(index: int, item: Any, failures: list[str]) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"items[{index}] must be an object")
        return None

    for key in REQUIRED_ITEM_KEYS:
        if key not in item:
            fail(failures, f"items[{index}] missing required key: {key}")

    item_id = str(item.get("id", "")).strip()
    status = item.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{item_id or f'items[{index}]'} has invalid status: {status!r}")
        return None

    for key in ("title", "scope"):
        if not str(item.get(key, "")).strip():
            fail(failures, f"{item_id} must include non-empty {key}")

    for key in ("artifacts", "currentEvidence", "blockingIssues", "nextActions"):
        if not isinstance(item.get(key), list):
            fail(failures, f"{item_id}.{key} must be a list")

    artifacts = as_list(item.get("artifacts"))
    for artifact in artifacts:
        check_artifact(item_id, artifact, failures)

    if status == "READY":
        if not artifacts:
            fail(failures, f"{item_id} READY must include at least one artifact")
        if not as_list(item.get("currentEvidence")):
            fail(failures, f"{item_id} READY must include currentEvidence")
        if as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} READY must not include blockingIssues")
    else:
        if not as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} {status} must include blockingIssues")
        if not as_list(item.get("nextActions")):
            fail(failures, f"{item_id} {status} must include nextActions")

    return status


def check_summary(data: dict[str, Any], failures: list[str]) -> None:
    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return

    items = as_list(data.get("items"))
    statuses = [item.get("status") for item in items if isinstance(item, dict)]
    expected = {
        "totalItems": len(items),
        "ready": statuses.count("READY"),
        "partial": statuses.count("PARTIAL"),
        "blocked": statuses.count("BLOCKED"),
        "fail": statuses.count("FAIL"),
        "notStarted": statuses.count("NOT_STARTED"),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "generatedFromCommit",
        "overallStatus",
        "databaseDefault",
        "summary",
        "items",
    ):
        if key not in data:
            fail(failures, f"report missing top-level key: {key}")

    if data.get("databaseDefault") != "PostgreSQL":
        fail(failures, "goal acceptance databaseDefault must remain PostgreSQL")

    if data.get("overallStatus") not in ALLOWED_OVERALL_STATUSES:
        fail(failures, f"invalid overallStatus: {data.get('overallStatus')!r}")

    commit = data.get("generatedFromCommit")
    if not isinstance(commit, str) or len(commit.strip()) < 7:
        fail(failures, "generatedFromCommit must identify the code baseline")

    items = data.get("items")
    if not isinstance(items, list):
        fail(failures, "items must be a list")
        return

    seen_ids: set[str] = set()
    statuses: list[str] = []
    for index, item in enumerate(items):
        status = check_item(index, item, failures)
        if not isinstance(item, dict):
            continue
        item_id = str(item.get("id", "")).strip()
        if item_id in seen_ids:
            fail(failures, f"duplicate item id: {item_id}")
        seen_ids.add(item_id)
        if status:
            statuses.append(status)

    missing = sorted(REQUIRED_ITEM_IDS - seen_ids)
    if missing:
        fail(failures, "goal acceptance report missing required items: " + ", ".join(missing))

    extra = sorted(seen_ids - REQUIRED_ITEM_IDS)
    if extra:
        fail(failures, "goal acceptance report contains unknown items: " + ", ".join(extra))

    check_summary(data, failures)

    if data.get("overallStatus") == "COMPLETE" and any(status != "READY" for status in statuses):
        fail(failures, "overall COMPLETE requires every goal item to be READY")

    if data.get("overallStatus") == "IN_PROGRESS_NOT_COMPLETE" and all(status == "READY" for status in statuses):
        fail(failures, "overall status should be COMPLETE when every goal item is READY")


def main() -> int:
    failures: list[str] = []
    check_doc(failures)
    data = read_json(failures)
    if data:
        check_report(data, failures)
    if failures:
        print(f"Project goal acceptance verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Project goal acceptance verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
