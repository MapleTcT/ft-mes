#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
HANDOFF_PATH = ROOT / "docs/backend-table-audit-handoff.md"
INDEX_PATH = ROOT / "docs/backend-table-audit/00-index.md"
PERSISTENCE_DOC_PATH = ROOT / "docs/backend-table-audit/persistence-acceptance.md"
PERSISTENCE_JSON_PATH = ROOT / "metadata/persistence-acceptance.json"
FRONTEND_REPORT_PATH = ROOT / "docs/frontend-functional-test-report.md"

EXPECTED_CURRENT_HOST = "100.99.133.43"
EXPECTED_DEFAULT_ACCOUNT = "admin / 123456"

REQUIRED_HANDOFF_FRAGMENTS = {
    "backend/modules/",
    "deploy/docker/postgres/init/",
    "docs/frontend-functional-test-report.md",
    "docs/backend-table-audit/persistence-acceptance.md",
    "metadata/persistence-acceptance.json",
    EXPECTED_CURRENT_HOST,
    EXPECTED_DEFAULT_ACCOUNT,
    "PostgreSQL",
    "幂等 SQL",
    "不要通过清库重建绕过",
    "Controller",
    "Service",
    "Mapper",
}

EXPECTED_INDEX_ROWS = {
    "platform-auth-rbac-org.md",
    "platform-entity-config.md",
    "platform-flow-todo.md",
    "business-quality-lims-qcs.md",
    "business-production.md",
    "business-equipment-energy-ehs.md",
    "persistence-acceptance.md",
    "wom-consumption-record-analysis.md",
    "wom-public-produce-task-created-analysis.md",
    "material-service-dependency-analysis.md",
    "processanalysis-dependency-analysis.md",
}

REQUIRED_STARTED_REPORTS = {
    "business-quality-lims-qcs.md",
    "business-production.md",
    "persistence-acceptance.md",
    "wom-consumption-record-analysis.md",
    "wom-public-produce-task-created-analysis.md",
    "material-service-dependency-analysis.md",
    "processanalysis-dependency-analysis.md",
}

REQUIRED_INDEX_FRAGMENTS = {
    "metadata/persistence-acceptance.json",
    "make persistence-acceptance-check",
    "docs/backend-table-audit/",
    "PostgreSQL",
}

ALLOWED_REPORT_STATUSES = {
    "待开始",
    "已开始",
    "已完成专项解释",
    "测试矩阵已建立",
    "模板已建立",
}


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_text(path: Path, failures: list[str]) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        fail(failures, f"required file missing: {path.relative_to(ROOT)}")
        return ""


def read_json(path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"required JSON missing: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{path.relative_to(ROOT)} must contain a JSON object")
        return {}
    return data


def parse_index_report_rows(text: str, failures: list[str]) -> dict[str, dict[str, str]]:
    rows: dict[str, dict[str, str]] = {}
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped.startswith("| `") or "` |" not in stripped:
            continue
        cells = [cell.strip() for cell in stripped.strip("|").split("|")]
        if len(cells) < 3:
            continue
        report = cells[0].strip("`")
        status = cells[1]
        description = cells[2]
        rows[report] = {"status": status, "description": description}
    if not rows:
        fail(failures, "backend table audit index must contain a report table")
    return rows


def check_handoff(failures: list[str]) -> None:
    text = read_text(HANDOFF_PATH, failures)
    if not text:
        return
    for fragment in sorted(REQUIRED_HANDOFF_FRAGMENTS):
        if fragment not in text:
            fail(failures, f"handoff document missing required fragment: {fragment}")


def check_index(failures: list[str]) -> None:
    text = read_text(INDEX_PATH, failures)
    if not text:
        return
    for fragment in sorted(REQUIRED_INDEX_FRAGMENTS):
        if fragment not in text:
            fail(failures, f"backend table audit index missing required fragment: {fragment}")

    rows = parse_index_report_rows(text, failures)
    missing = sorted(EXPECTED_INDEX_ROWS - rows.keys())
    if missing:
        fail(failures, "backend table audit index missing reports: " + ", ".join(missing))

    for report, row in sorted(rows.items()):
        status = row["status"]
        if status not in ALLOWED_REPORT_STATUSES:
            fail(failures, f"{report} has unsupported audit status: {status}")
        report_path = ROOT / "docs/backend-table-audit" / report
        if report in REQUIRED_STARTED_REPORTS and not report_path.exists():
            fail(failures, f"{report} is started/completed in the index but the report file is missing")
        if status != "待开始" and not row["description"]:
            fail(failures, f"{report} must include a handoff description")


def check_persistence_assets(failures: list[str]) -> None:
    doc_text = read_text(PERSISTENCE_DOC_PATH, failures)
    frontend_text = read_text(FRONTEND_REPORT_PATH, failures)
    data = read_json(PERSISTENCE_JSON_PATH, failures)
    if doc_text:
        for fragment in ("PASS", "FAIL", "BLOCKED", "PostgreSQL", "marker"):
            if fragment not in doc_text:
                fail(failures, f"persistence acceptance document missing required fragment: {fragment}")
    if frontend_text and "metadata/persistence-acceptance.json" not in frontend_text:
        fail(failures, "frontend functional test report must reference metadata/persistence-acceptance.json")
    if not data:
        return
    if data.get("database") != "PostgreSQL":
        fail(failures, "persistence acceptance JSON database must remain PostgreSQL")
    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "persistence acceptance JSON summary must be an object")
        return
    tested = int(summary.get("testedFeatures") or 0)
    passed = int(summary.get("pass") or 0)
    failed = int(summary.get("fail") or 0)
    blocked = int(summary.get("blocked") or 0)
    not_applicable = int(summary.get("notApplicable") or 0)
    if tested <= 0:
        fail(failures, "persistence acceptance must contain tested features")
    if tested != passed + failed + blocked + not_applicable:
        fail(
            failures,
            "persistence acceptance summary counts must add up to testedFeatures: "
            f"{tested} != {passed}+{failed}+{blocked}+{not_applicable}",
        )
    if not isinstance(data.get("items"), list) or len(data.get("items", [])) != tested:
        fail(failures, "persistence acceptance items count must match summary.testedFeatures")


def check_special_reports(failures: list[str]) -> None:
    for report in sorted(REQUIRED_STARTED_REPORTS - {"persistence-acceptance.md"}):
        path = ROOT / "docs/backend-table-audit" / report
        text = read_text(path, failures)
        if not text:
            continue
        if "PostgreSQL" not in text:
            fail(failures, f"{report} must mention PostgreSQL implications")
        if not re.search(r"\b(PASS|BLOCKED|FAIL|NOT_APPLICABLE)\b", text):
            fail(failures, f"{report} must contain an explicit acceptance status")


def main() -> int:
    failures: list[str] = []
    check_handoff(failures)
    check_index(failures)
    check_persistence_assets(failures)
    check_special_reports(failures)
    if failures:
        print(f"Backend table audit handoff verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Backend table audit handoff verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
