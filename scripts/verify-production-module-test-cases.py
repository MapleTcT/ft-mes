#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/production-module-test-cases.json"
DOC_PATH = ROOT / "docs/production-module-functional-test-cases.md"
AUDIT_PATH = ROOT / "docs/backend-table-audit/business-production.md"

ALLOWED_STATUSES = {"PASS", "FAIL", "BLOCKED", "NOT_RUN"}
ALLOWED_ROUTE_SMOKE_STATUSES = {"PASS", "FAIL", "BLOCKED", "NOT_RUN", "NOT_APPLICABLE"}

REQUIRED_COVERAGE_TAGS = {
    "master-data",
    "recipe",
    "process",
    "work-order",
    "instruction",
    "dispatch",
    "material",
    "work-permit",
    "execution",
    "report-work",
    "return-material",
    "quality",
    "traceability",
    "inventory",
    "import-export",
    "status-flow",
    "persistence",
    "postgres",
}

CASE_REQUIRED_KEYS = [
    "id",
    "domain",
    "name",
    "route",
    "operationType",
    "requiresPersistence",
    "routeSmokeStatus",
    "acceptanceStatus",
    "apiEndpoints",
    "backendTraceStatus",
    "targetTables",
    "verificationSql",
    "evidence",
    "issues",
    "nextAction",
    "coverageTags",
]


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_json(failures: list[str]) -> dict[str, Any]:
    try:
        with REPORT_PATH.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing report: {REPORT_PATH.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {REPORT_PATH.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, "production module test cases report must be a JSON object")
        return {}
    return data


def check_docs(failures: list[str]) -> None:
    for path in (DOC_PATH, AUDIT_PATH):
        if not path.exists():
            fail(failures, f"required document missing: {path.relative_to(ROOT)}")

    if DOC_PATH.exists():
        text = DOC_PATH.read_text(encoding="utf-8")
        required = "| ID | 领域 | 用例 | 前端入口 | 是否落库 | 当前状态 | 阻断/下一步 |"
        if required not in text:
            fail(failures, "production functional test document is missing the case matrix table")
        for case_id in ("PROD-001", "PROD-002", "PROD-021", "PROD-022"):
            if case_id not in text:
                fail(failures, f"production functional test document missing case {case_id}")

    if AUDIT_PATH.exists():
        text = AUDIT_PATH.read_text(encoding="utf-8")
        if "## Backlog" not in text:
            fail(failures, "business production audit document must include a Backlog section")


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def check_case(index: int, case: Any, failures: list[str]) -> str | None:
    if not isinstance(case, dict):
        fail(failures, f"cases[{index}] must be an object")
        return None

    for key in CASE_REQUIRED_KEYS:
        if key not in case:
            fail(failures, f"cases[{index}] missing required key: {key}")

    case_id = str(case.get("id", "")).strip()
    if not case_id.startswith("PROD-"):
        fail(failures, f"cases[{index}] id must start with PROD-")

    status = case.get("acceptanceStatus")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{case_id or f'cases[{index}]'} has invalid acceptanceStatus: {status!r}")
        return None

    route_smoke_status = case.get("routeSmokeStatus")
    if route_smoke_status not in ALLOWED_ROUTE_SMOKE_STATUSES:
        fail(failures, f"{case_id} has invalid routeSmokeStatus: {route_smoke_status!r}")

    if not isinstance(case.get("requiresPersistence"), bool):
        fail(failures, f"{case_id}.requiresPersistence must be boolean")

    for list_key in ("apiEndpoints", "targetTables", "issues", "coverageTags"):
        if not isinstance(case.get(list_key), list):
            fail(failures, f"{case_id}.{list_key} must be a list")

    if not str(case.get("name", "")).strip():
        fail(failures, f"{case_id} must include a case name")
    if not str(case.get("domain", "")).strip():
        fail(failures, f"{case_id} must include a domain")
    if not str(case.get("nextAction", "")).strip():
        fail(failures, f"{case_id} must include nextAction")
    if not as_list(case.get("coverageTags")):
        fail(failures, f"{case_id} must include coverageTags")

    if status == "PASS":
        if not str(case.get("evidence", "")).strip():
            fail(failures, f"{case_id} PASS must include evidence")
        if route_smoke_status != "PASS":
            fail(failures, f"{case_id} PASS must have routeSmokeStatus PASS")
        if case.get("requiresPersistence"):
            if not as_list(case.get("targetTables")):
                fail(failures, f"{case_id} persisted PASS must include targetTables")
            if not str(case.get("verificationSql", "")).strip():
                fail(failures, f"{case_id} persisted PASS must include verificationSql")

    if status in {"FAIL", "BLOCKED", "NOT_RUN"} and not as_list(case.get("issues")):
        fail(failures, f"{case_id} {status} must include at least one issue")

    if status == "BLOCKED" and not str(case.get("backendTraceStatus", "")).strip():
        fail(failures, f"{case_id} BLOCKED must include backendTraceStatus")

    return status


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in ("schemaVersion", "generatedAt", "repoCommit", "database", "summary", "cases"):
        if key not in data:
            fail(failures, f"report missing required top-level key: {key}")

    if data.get("database") != "PostgreSQL":
        fail(failures, "production module test matrix database must remain PostgreSQL")

    if data.get("module") != "production":
        fail(failures, "production module test matrix must identify module=production")

    cases = data.get("cases")
    if not isinstance(cases, list):
        fail(failures, "cases must be a list")
        return

    seen_ids: set[str] = set()
    statuses: list[str] = []
    route_smoke_pass = 0
    requires_persistence = 0
    coverage_tags: set[str] = set()

    for index, case in enumerate(cases):
        status = check_case(index, case, failures)
        if not isinstance(case, dict):
            continue
        case_id = str(case.get("id", ""))
        if case_id in seen_ids:
            fail(failures, f"duplicate case id: {case_id}")
        seen_ids.add(case_id)
        if status:
            statuses.append(status)
        if case.get("routeSmokeStatus") == "PASS":
            route_smoke_pass += 1
        if case.get("requiresPersistence") is True:
            requires_persistence += 1
        coverage_tags.update(str(tag) for tag in as_list(case.get("coverageTags")))

    missing_tags = sorted(REQUIRED_COVERAGE_TAGS - coverage_tags)
    if missing_tags:
        fail(failures, "production module test matrix missing coverage tags: " + ", ".join(missing_tags))

    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        summary = {}

    expected = {
        "totalCases": len(cases),
        "pass": statuses.count("PASS"),
        "fail": statuses.count("FAIL"),
        "blocked": statuses.count("BLOCKED"),
        "notRun": statuses.count("NOT_RUN"),
        "requiresPersistence": requires_persistence,
        "routeSmokePass": route_smoke_pass,
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")

    required_ids = {"PROD-001", "PROD-002", "PROD-003", "PROD-009", "PROD-015", "PROD-019", "PROD-021"}
    missing_ids = sorted(required_ids - seen_ids)
    if missing_ids:
        fail(failures, "production module test matrix missing required cases: " + ", ".join(missing_ids))


def main() -> int:
    failures: list[str] = []
    check_docs(failures)
    data = read_json(failures)
    if data:
        check_report(data, failures)
    if failures:
        print(f"Production module test case verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Production module test case verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
