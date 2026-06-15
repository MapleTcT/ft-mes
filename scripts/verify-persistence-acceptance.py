#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/persistence-acceptance.json"

REQUIRED_DOCS = [
    ROOT / "docs/functional-persistence-acceptance.md",
    ROOT / "docs/frontend-functional-test-report.md",
    ROOT / "docs/backend-table-audit/persistence-acceptance.md",
]

FUNCTIONAL_RULE_REQUIRED_PHRASES = [
    "不是继续补治理层",
    "不允许只看代码判断功能是否可用",
    "必须通过浏览器或等效前端 E2E 方式访问页面",
    "PostgreSQL",
    "ADP_E2E_YYYYMMDD_HHMMSS_xxx",
    "metadata/persistence-acceptance.json",
]

SUMMARY_KEYS = {
    "testedFeatures": "all",
    "pass": "PASS",
    "fail": "FAIL",
    "blocked": "BLOCKED",
    "notApplicable": "NOT_APPLICABLE",
}

ITEM_REQUIRED_KEYS = [
    "module",
    "route",
    "operation",
    "api",
    "method",
    "requiresPersistence",
    "backendEntry",
    "tables",
    "marker",
    "verificationSql",
    "status",
    "evidence",
    "issues",
]

ALLOWED_STATUSES = {"PASS", "FAIL", "BLOCKED", "NOT_APPLICABLE"}


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_json(failures: list[str]) -> dict:
    try:
        with REPORT_PATH.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(f"missing report: {REPORT_PATH.relative_to(ROOT)}", failures)
        return {}
    except json.JSONDecodeError as error:
        fail(f"invalid JSON in {REPORT_PATH.relative_to(ROOT)}: {error}", failures)
        return {}
    if not isinstance(data, dict):
        fail("persistence acceptance report must be a JSON object", failures)
        return {}
    return data


def check_docs(failures: list[str]) -> None:
    for path in REQUIRED_DOCS:
        if not path.exists():
            fail(f"required acceptance document missing: {path.relative_to(ROOT)}", failures)

    functional = ROOT / "docs/functional-persistence-acceptance.md"
    if functional.exists():
        text = functional.read_text(encoding="utf-8")
        for phrase in FUNCTIONAL_RULE_REQUIRED_PHRASES:
            if phrase not in text:
                fail(
                    "functional persistence acceptance rules are missing required phrase: "
                    f"{phrase}",
                    failures,
                )

    frontend = ROOT / "docs/frontend-functional-test-report.md"
    if frontend.exists():
        text = frontend.read_text(encoding="utf-8")
        required = "| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 验收状态 | 问题 |"
        if required not in text:
            fail("frontend functional report is missing the required summary table", failures)

    backend = ROOT / "docs/backend-table-audit/persistence-acceptance.md"
    if backend.exists():
        text = backend.read_text(encoding="utf-8")
        required = "| 业务动作 | 前端入口 | API endpoint | 后端入口 | 目标表 | 验收 SQL | 实际结果 | 状态 |"
        if required not in text:
            fail("backend persistence report is missing the required acceptance table", failures)


def check_item(index: int, item: object, failures: list[str]) -> str | None:
    if not isinstance(item, dict):
        fail(f"items[{index}] must be an object", failures)
        return None

    for key in ITEM_REQUIRED_KEYS:
        if key not in item:
            fail(f"items[{index}] missing required key: {key}", failures)

    status = item.get("status")
    if status not in ALLOWED_STATUSES:
        fail(f"items[{index}] has invalid status: {status!r}", failures)
        return None

    if not isinstance(item.get("requiresPersistence"), bool):
        fail(f"items[{index}].requiresPersistence must be boolean", failures)

    if not isinstance(item.get("tables"), list):
        fail(f"items[{index}].tables must be a list", failures)

    if not isinstance(item.get("issues"), list):
        fail(f"items[{index}].issues must be a list", failures)

    if status == "PASS":
        for key in ("module", "route", "operation", "api", "method", "backendEntry", "evidence"):
            if not str(item.get(key, "")).strip():
                fail(f"items[{index}] PASS must include non-empty {key}", failures)
        if item.get("requiresPersistence"):
            if not item.get("tables"):
                fail(f"items[{index}] persisted PASS must include target tables", failures)
            for key in ("marker", "verificationSql"):
                if not str(item.get(key, "")).strip():
                    fail(f"items[{index}] persisted PASS must include {key}", failures)
        elif not str(item.get("evidence", "")).strip():
            fail(f"items[{index}] non-persistent PASS must explain evidence", failures)

    if status in {"FAIL", "BLOCKED"} and not item.get("issues"):
        fail(f"items[{index}] {status} must include at least one issue", failures)

    if status == "NOT_APPLICABLE" and not str(item.get("evidence", "")).strip():
        fail(f"items[{index}] NOT_APPLICABLE must explain why persistence is not required", failures)

    return status


def check_report(data: dict, failures: list[str]) -> None:
    for key in ("generatedAt", "repoCommit", "database", "summary", "items"):
        if key not in data:
            fail(f"report missing required top-level key: {key}", failures)

    if data.get("database") != "PostgreSQL":
        fail("persistence acceptance database must remain PostgreSQL", failures)

    repo_commit = data.get("repoCommit")
    if not isinstance(repo_commit, str) or len(repo_commit.strip()) < 7:
        fail("repoCommit must identify the code baseline used for acceptance", failures)

    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail("summary must be an object", failures)
        summary = {}
    for key in SUMMARY_KEYS:
        if not isinstance(summary.get(key), int) or summary.get(key) < 0:
            fail(f"summary.{key} must be a non-negative integer", failures)

    items = data.get("items")
    if not isinstance(items, list):
        fail("items must be a list", failures)
        return

    statuses: list[str] = []
    for index, item in enumerate(items):
        status = check_item(index, item, failures)
        if status:
            statuses.append(status)

    expected = {
        "testedFeatures": len(items),
        "pass": statuses.count("PASS"),
        "fail": statuses.count("FAIL"),
        "blocked": statuses.count("BLOCKED"),
        "notApplicable": statuses.count("NOT_APPLICABLE"),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(f"summary.{key}={summary.get(key)!r} does not match expected {value}", failures)


def main() -> int:
    failures: list[str] = []
    check_docs(failures)
    data = read_json(failures)
    if data:
        check_report(data, failures)
    if failures:
        print(f"Persistence acceptance verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Persistence acceptance verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
