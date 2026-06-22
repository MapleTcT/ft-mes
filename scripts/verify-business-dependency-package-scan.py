#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPORT_PATH = ROOT / "metadata/business-dependency-package-scan.json"

REQUIRED_DEPENDENCIES = {"material-service", "process-analysis"}
ALLOWED_STATUSES = {"BLOCKED_NO_IMPLEMENTATION_CANDIDATE", "CANDIDATE_FOUND"}

DOC_REQUIREMENTS = {
    "docs/backend-table-audit/material-service-dependency-analysis.md": [
        "make business-package-scan",
        "metadata/business-dependency-package-scan.json",
    ],
    "docs/backend-table-audit/processanalysis-dependency-analysis.md": [
        "make business-package-scan",
        "metadata/business-dependency-package-scan.json",
    ],
    "docs/production-module-blockers.md": [
        "make business-package-scan",
        "metadata/business-dependency-package-scan.json",
    ],
}

FORBIDDEN_SECRET_LITERALS = [
    "ft" + "123456789",
    "Authorization: Bearer",
    "access_token",
    "refresh_token",
    "BEGIN PRIVATE KEY",
    "BEGIN RSA PRIVATE KEY",
    "BEGIN OPENSSH PRIVATE KEY",
]

SECRET_ASSIGNMENT_RE = re.compile(
    r"(?im)^\s*(password|passwd|secret|token|access[_-]?key|secret[_-]?key)\s*[:=]\s*"
    r"(?!CHANGE_ME|TBD|TODO|<|\$\{|\$)[^\s#]{8,}"
)


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def read_json(path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing business dependency package scan report: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{path.relative_to(ROOT)} must be a JSON object")
        return {}
    return data


def check_secret_hygiene(report_path: Path, failures: list[str]) -> None:
    text = report_path.read_text(encoding="utf-8")
    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in text:
            fail(failures, f"business dependency package scan report contains forbidden secret literal: {literal}")
    if SECRET_ASSIGNMENT_RE.search(text):
        fail(failures, "business dependency package scan report appears to contain a non-placeholder secret assignment")


def check_docs(failures: list[str]) -> None:
    for relative_path, fragments in DOC_REQUIREMENTS.items():
        path = ROOT / relative_path
        if not path.exists():
            fail(failures, f"required document missing: {relative_path}")
            continue
        text = path.read_text(encoding="utf-8")
        for fragment in fragments:
            if fragment not in text:
                fail(failures, f"{relative_path} missing required text: {fragment}")


def check_hit(dependency_id: str, hit: Any, failures: list[str]) -> None:
    if not isinstance(hit, dict):
        fail(failures, f"{dependency_id} hit must be an object")
        return
    for key in ("dependency", "term", "match_type", "source", "context"):
        if key not in hit:
            fail(failures, f"{dependency_id} hit missing key: {key}")
    if hit.get("dependency") != dependency_id:
        fail(failures, f"{dependency_id} hit has mismatched dependency: {hit.get('dependency')!r}")
    if not str(hit.get("source", "")).strip():
        fail(failures, f"{dependency_id} hit source must be non-empty")


def check_dependency(index: int, item: Any, failures: list[str]) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"dependencies[{index}] must be an object")
        return None

    dependency_id = str(item.get("id", "")).strip()
    if dependency_id not in REQUIRED_DEPENDENCIES:
        fail(failures, f"unknown dependency id in package scan: {dependency_id!r}")
        return None

    for key in (
        "id",
        "title",
        "status",
        "requiredFor",
        "searchCriteria",
        "targetHitCount",
        "moduleHitCount",
        "knownAdjacentHitCount",
        "callerOnlyHitCount",
        "implementationCandidateCount",
        "implementationCandidates",
        "targetHits",
        "moduleHits",
        "knownAdjacentHits",
        "callerOnlyHits",
        "nextAction",
    ):
        if key not in item:
            fail(failures, f"{dependency_id} missing required key: {key}")

    if item.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"{dependency_id} has invalid status: {item.get('status')!r}")
    if not str(item.get("title", "")).strip():
        fail(failures, f"{dependency_id} title must be non-empty")
    if not as_list(item.get("requiredFor")):
        fail(failures, f"{dependency_id} requiredFor must be non-empty")
    if not str(item.get("nextAction", "")).strip():
        fail(failures, f"{dependency_id} nextAction must be non-empty")

    search_criteria = as_dict(item.get("searchCriteria"))
    for key in (
        "targetTerms",
        "moduleTerms",
        "knownAdjacentTerms",
        "callerOnlyHints",
        "archiveContentPathHints",
        "implementationExcludeHints",
    ):
        values = as_list(search_criteria.get(key))
        if not values or not all(str(value).strip() for value in values):
            fail(failures, f"{dependency_id}.searchCriteria.{key} must be a non-empty list")

    for key in ("targetHitCount", "moduleHitCount", "knownAdjacentHitCount", "callerOnlyHitCount", "implementationCandidateCount"):
        if not isinstance(item.get(key), int) or item.get(key, -1) < 0:
            fail(failures, f"{dependency_id}.{key} must be a non-negative integer")

    candidate_count = int(item.get("implementationCandidateCount") or 0)
    candidates = as_list(item.get("implementationCandidates"))
    if item.get("status") == "CANDIDATE_FOUND" and candidate_count <= 0:
        fail(failures, f"{dependency_id} CANDIDATE_FOUND must include implementation candidates")
    if item.get("status") == "BLOCKED_NO_IMPLEMENTATION_CANDIDATE" and candidate_count != 0:
        fail(failures, f"{dependency_id} BLOCKED_NO_IMPLEMENTATION_CANDIDATE must not have candidate count")
    if candidates and candidate_count <= 0:
        fail(failures, f"{dependency_id} implementationCandidates present but count is zero")

    for hit_list_key in ("implementationCandidates", "targetHits", "moduleHits", "knownAdjacentHits", "callerOnlyHits"):
        for hit in as_list(item.get(hit_list_key)):
            check_hit(dependency_id, hit, failures)

    return dependency_id


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in ("schemaVersion", "generatedAt", "repoCommit", "database", "scanRoots", "coverage", "summary", "scannedArchives", "dependencies", "evidence"):
        if key not in data:
            fail(failures, f"package scan report missing top-level key: {key}")

    if data.get("database") != "PostgreSQL":
        fail(failures, "business dependency package scan database must remain PostgreSQL")
    if not isinstance(data.get("repoCommit"), str) or len(str(data.get("repoCommit", "")).strip()) < 7:
        fail(failures, "repoCommit must identify the code baseline used for package scanning")

    scan_roots = as_list(data.get("scanRoots"))
    if not scan_roots:
        fail(failures, "scanRoots must not be empty")
    for index, root in enumerate(scan_roots):
        if not isinstance(root, dict) or not str(root.get("path", "")).strip() or not isinstance(root.get("exists"), bool):
            fail(failures, f"scanRoots[{index}] must include path and exists")
    expected_roots = {"/Users/zhangchu/Documents/MES包"}
    existing_paths = {str(root.get("path")) for root in scan_roots if root.get("exists") is True}
    missing_expected_roots = sorted(expected_roots - existing_paths)
    if missing_expected_roots:
        fail(failures, "package scan must include existing delivery roots: " + ", ".join(missing_expected_roots))
    if not any(path.startswith("/Users/zhangchu/Downloads/ADP/") for path in existing_paths):
        fail(failures, "package scan must include at least one source/config/static root under /Users/zhangchu/Downloads/ADP")

    coverage = as_dict(data.get("coverage"))
    if coverage.get("status") not in {"FULL_BOUNDED", "PARTIAL_BOUNDED"}:
        fail(failures, f"coverage.status has invalid value: {coverage.get('status')!r}")
    if not isinstance(coverage.get("nestedDepth"), int) or coverage.get("nestedDepth", -1) < 1:
        fail(failures, "coverage.nestedDepth must be at least 1")
    limits = as_dict(coverage.get("limits"))
    for key in ("firstPartyArchiveHints", "vendorArchiveHints"):
        values = as_list(coverage.get(key))
        if not values or not all(str(value).strip() for value in values):
            fail(failures, f"coverage.{key} must be a non-empty list")
    for key in (
        "maxTextBytes",
        "maxArchiveEntries",
        "maxArchiveTextEntries",
        "maxNestedArchiveBytes",
        "maxTarScanBytes",
        "maxArchiveHashBytes",
    ):
        if not isinstance(limits.get(key), int) or limits.get(key, -1) < 0:
            fail(failures, f"coverage.limits.{key} must be a non-negative integer")
    if int(limits.get("maxArchiveEntries") or 0) < 2000000:
        fail(failures, "coverage.limits.maxArchiveEntries must be high enough for the current delivery package scale")
    if int(limits.get("maxArchiveTextEntries") or 0) < 100000:
        fail(failures, "coverage.limits.maxArchiveTextEntries must be high enough for the current delivery package scale")
    unscanned_reasons = as_dict(coverage.get("unscannedReasons"))
    for key, value in unscanned_reasons.items():
        if not isinstance(value, int) or value <= 0:
            fail(failures, f"coverage.unscannedReasons.{key} must be a positive integer when present")

    summary = as_dict(data.get("summary"))
    if summary.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"summary.status has invalid value: {summary.get('status')!r}")
    if not isinstance(summary.get("rootsExisting"), int) or summary.get("rootsExisting", 0) <= 0:
        fail(failures, "summary.rootsExisting must be positive")
    if not isinstance(summary.get("filesVisited"), int) or summary.get("filesVisited", 0) <= 0:
        fail(failures, "summary.filesVisited must be positive")
    if summary.get("skippedArchiveEntries", 0) != 0:
        fail(failures, "package scan cannot claim missing service candidates while archive entry paths were skipped")
    if summary.get("skippedArchiveTextEntries", 0) != 0:
        fail(failures, "package scan cannot claim missing service candidates while archive text entries matching archiveContentPathHints were skipped")
    if not isinstance(summary.get("archiveTextEntriesSkippedByPath"), int) or summary.get("archiveTextEntriesSkippedByPath", -1) < 0:
        fail(failures, "summary.archiveTextEntriesSkippedByPath must be a non-negative integer")
    if not isinstance(summary.get("nestedArchivesSkippedByPath"), int) or summary.get("nestedArchivesSkippedByPath", -1) < 0:
        fail(failures, "summary.nestedArchivesSkippedByPath must be a non-negative integer")

    dependencies = data.get("dependencies")
    if not isinstance(dependencies, list):
        fail(failures, "dependencies must be a list")
        return

    seen_ids: set[str] = set()
    statuses: list[str] = []
    for index, item in enumerate(dependencies):
        dependency_id = check_dependency(index, item, failures)
        if not dependency_id:
            continue
        if dependency_id in seen_ids:
            fail(failures, f"duplicate dependency id in package scan: {dependency_id}")
        seen_ids.add(dependency_id)
        statuses.append(str(item.get("status")))

    missing = sorted(REQUIRED_DEPENDENCIES - seen_ids)
    if missing:
        fail(failures, "package scan report missing required dependencies: " + ", ".join(missing))

    expected = {
        "dependencies": len(seen_ids),
        "blockedDependencies": statuses.count("BLOCKED_NO_IMPLEMENTATION_CANDIDATE"),
        "candidateDependencies": statuses.count("CANDIDATE_FOUND"),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")
    computed_status = "CANDIDATE_FOUND" if expected["candidateDependencies"] else "BLOCKED_NO_IMPLEMENTATION_CANDIDATE"
    if summary.get("status") != computed_status:
        fail(failures, f"summary.status={summary.get('status')!r} does not match expected {computed_status}")

    evidence = as_dict(data.get("evidence"))
    if "Read-only" not in str(evidence.get("method", "")):
        fail(failures, "evidence.method must state that the scan is read-only")
    limitation_text = " ".join(as_list(evidence.get("limitations"))).lower()
    if "runtime" not in limitation_text:
        fail(failures, "evidence.limitations must state that runtime verification is still required")
    if "archivecontentpathhints" not in limitation_text:
        fail(failures, "evidence.limitations must describe archiveContentPathHints for archive text scanning")
    if "firstpartyarchivehints" not in limitation_text or "vendor archive" not in limitation_text:
        fail(failures, "evidence.limitations must describe nested archive first-party/vendor filtering")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate ADP business dependency package scan report.")
    parser.add_argument(
        "--report",
        default=str(DEFAULT_REPORT_PATH.relative_to(ROOT)),
        help="Path to metadata/business-dependency-package-scan.json",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    report_path = Path(args.report)
    if not report_path.is_absolute():
        report_path = ROOT / report_path

    failures: list[str] = []
    data = read_json(report_path, failures)
    if report_path.exists():
        check_secret_hygiene(report_path, failures)
    check_docs(failures)
    if data:
        check_report(data, failures)

    if failures:
        print(f"Business dependency package scan verification failed with {len(failures)} issue(s).", file=sys.stderr)
        return 1

    print("Business dependency package scan verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
