#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/module-intake-latest-basic-modules.json"
DOC_PATH = ROOT / "docs/module-intake-latest-basic-modules.md"

REQUIRED_TOP_LEVEL_KEYS = {
    "schemaVersion",
    "generatedAt",
    "reportKind",
    "source",
    "status",
    "summary",
    "scanPolicy",
    "scanCoverage",
    "categoryCounts",
    "patternCounts",
    "blockingFindings",
    "warningExamples",
    "requiredActions",
    "evidence",
}

ALLOWED_STATUS = {
    "READY_FOR_SOURCE_PROMOTION",
    "REVIEW_REQUIRED_BEFORE_SOURCE_PROMOTION",
    "BLOCKED_FOR_SOURCE_PROMOTION",
}


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_json(path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            value = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing report: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as exc:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {exc}")
        return {}
    if not isinstance(value, dict):
        fail(failures, f"{path.relative_to(ROOT)} must be a JSON object")
        return {}
    return value


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def check_summary(report: dict[str, Any], failures: list[str]) -> None:
    summary = as_dict(report.get("summary"))
    for key in (
        "fileCount",
        "byteCount",
        "pomCount",
        "javaFileCount",
        "sqlFileCount",
        "mapperOrSqlFileCount",
        "binaryCount",
        "zipArchiveCount",
        "unsupportedArchiveCount",
        "nestedFileCount",
        "maxArchiveDepthSeen",
        "textCandidateCount",
        "textScannedCount",
        "textSkippedLargeCount",
        "textSkippedUnavailableCount",
        "textScannedBytes",
        "findingCount",
        "errorCount",
        "warningCount",
    ):
        value = summary.get(key)
        if not isinstance(value, int) or value < 0:
            fail(failures, f"summary.{key} must be a non-negative integer")

    if isinstance(summary.get("findingCount"), int):
        if summary.get("findingCount") != summary.get("errorCount", 0) + summary.get("warningCount", 0):
            fail(failures, "summary.findingCount must equal errorCount + warningCount")

    category_total = sum(value for value in as_dict(report.get("categoryCounts")).values() if isinstance(value, int))
    pattern_total = sum(value for value in as_dict(report.get("patternCounts")).values() if isinstance(value, int))
    if category_total != summary.get("findingCount"):
        fail(failures, "categoryCounts total must equal summary.findingCount")
    if pattern_total != summary.get("findingCount"):
        fail(failures, "patternCounts total must equal summary.findingCount")


def check_scan_policy(report: dict[str, Any], failures: list[str]) -> None:
    policy = as_dict(report.get("scanPolicy"))
    coverage = as_dict(report.get("scanCoverage"))
    summary = as_dict(report.get("summary"))
    if not policy:
        fail(failures, "scanPolicy must be present")
        return
    if not coverage:
        fail(failures, "scanCoverage must be present")
        return
    for key in ("maxTextBytes", "maxNestedArchiveBytes", "maxArchiveDepth"):
        if not isinstance(policy.get(key), int) or policy.get(key) <= 0:
            fail(failures, f"scanPolicy.{key} must be a positive integer")
    if ".7z" not in set(policy.get("unsupportedArchiveSuffixes") or []):
        fail(failures, "scanPolicy.unsupportedArchiveSuffixes must include .7z")
    if ".jar" not in set(policy.get("zipArchiveSuffixes") or []):
        fail(failures, "scanPolicy.zipArchiveSuffixes must include .jar")
    for key in ("textCoverageComplete", "hasUnsupportedArchives", "hasOversizedNestedArchives", "nestedArchivePathsPreserved"):
        if not isinstance(coverage.get(key), bool):
            fail(failures, f"scanCoverage.{key} must be boolean")
    if summary.get("unsupportedArchiveCount") != 1:
        fail(failures, "latest-basic-modules summary.unsupportedArchiveCount must remain 1 until the 7z package is rescanned/repacked")
    if coverage.get("hasUnsupportedArchives") is not True:
        fail(failures, "latest-basic-modules scanCoverage.hasUnsupportedArchives must remain true while the 7z package is present")
    if int(summary.get("nestedFileCount") or 0) <= 0:
        fail(failures, "latest-basic-modules summary.nestedFileCount must prove nested archives were inspected")


def check_status(report: dict[str, Any], failures: list[str]) -> None:
    status = report.get("status")
    if status not in ALLOWED_STATUS:
        fail(failures, f"status must be one of {sorted(ALLOWED_STATUS)}")

    summary = as_dict(report.get("summary"))
    error_count = summary.get("errorCount")
    warning_count = summary.get("warningCount")
    expected = None
    if isinstance(error_count, int) and error_count > 0:
        expected = "BLOCKED_FOR_SOURCE_PROMOTION"
    elif isinstance(warning_count, int) and warning_count > 0:
        expected = "REVIEW_REQUIRED_BEFORE_SOURCE_PROMOTION"
    elif isinstance(error_count, int) and isinstance(warning_count, int):
        expected = "READY_FOR_SOURCE_PROMOTION"
    if expected and status != expected:
        fail(failures, f"status {status!r} must match summary-derived status {expected!r}")


def check_source(report: dict[str, Any], failures: list[str]) -> None:
    source = as_dict(report.get("source"))
    for key in ("id", "label", "pathAtScan", "inputKind", "scanMode", "command"):
        if not source.get(key):
            fail(failures, f"source.{key} must be present")
    command = str(source.get("command", ""))
    if "scripts/precheck-module-intake.py" not in command or "--report-only" not in command:
        fail(failures, "source.command must record the report-only module intake precheck command")
    if source.get("scanMode") != "report-only":
        fail(failures, "source.scanMode must remain report-only for externally supplied package evidence")


def check_findings(report: dict[str, Any], failures: list[str]) -> None:
    summary = as_dict(report.get("summary"))
    blocking_findings = as_list(report.get("blockingFindings"))
    if len(blocking_findings) != summary.get("errorCount"):
        fail(failures, "blockingFindings length must equal summary.errorCount")

    patterns = set()
    categories = set()
    for index, finding in enumerate(blocking_findings):
        if not isinstance(finding, dict):
            fail(failures, f"blockingFindings[{index}] must be an object")
            continue
        for key in ("severity", "category", "pattern", "file", "excerpt"):
            if key not in finding:
                fail(failures, f"blockingFindings[{index}] missing {key}")
        if finding.get("severity") != "error":
            fail(failures, f"blockingFindings[{index}].severity must be error")
        patterns.add(str(finding.get("pattern", "")))
        categories.add(str(finding.get("category", "")))

    required_actions_text = " ".join(str(item) for item in as_list(report.get("requiredActions")))
    if "7z" in patterns and "repackage" not in required_actions_text.lower():
        fail(failures, "7z blocker must have a repackage action")
    if "oracle-jdbc-url" in patterns and "PostgreSQL" not in required_actions_text:
        fail(failures, "oracle-jdbc-url blocker must have a PostgreSQL action")
    if "unsupported-archive" in categories and as_dict(report.get("categoryCounts")).get("unsupported-archive") != 1:
        fail(failures, "unsupported-archive category count must match the current latest-basic-modules scan")


def check_doc(report: dict[str, Any], failures: list[str]) -> None:
    try:
        text = DOC_PATH.read_text(encoding="utf-8")
    except FileNotFoundError:
        fail(failures, f"missing document: {DOC_PATH.relative_to(ROOT)}")
        return
    required_fragments = [
        "最新基础模块准入预检报告",
        "metadata/module-intake-latest-basic-modules.json",
        "make module-intake-candidate-report-check",
        str(report.get("status", "")),
        "XTYsupPlant-WOM V6.1.3.4-220722-C",
        "DbUtils.java",
        "jdbc:oracle:thin",
        "扫描覆盖",
        "文本候选 / 已读取",
        "textCoverageComplete=false",
        "hasUnsupportedArchives=true",
    ]
    for fragment in required_fragments:
        if fragment not in text:
            fail(failures, f"document missing required text: {fragment}")


def main() -> int:
    failures: list[str] = []
    report = read_json(REPORT_PATH, failures)
    if report:
        missing = sorted(REQUIRED_TOP_LEVEL_KEYS - set(report))
        if missing:
            fail(failures, "report missing required top-level keys: " + ", ".join(missing))
        if report.get("schemaVersion") != 1:
            fail(failures, "schemaVersion must be 1")
        if report.get("reportKind") != "module-intake-candidate":
            fail(failures, "reportKind must be module-intake-candidate")
        check_summary(report, failures)
        check_scan_policy(report, failures)
        check_status(report, failures)
        check_source(report, failures)
        check_findings(report, failures)
        check_doc(report, failures)

    if failures:
        print(f"Module intake candidate report verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Module intake candidate report verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
