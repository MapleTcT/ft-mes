#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
BACKLOG_PATH = ROOT / "metadata/production-module-backlog.json"
DOC_PATH = ROOT / "docs/production-module-backlog.md"
PERSISTENCE_PATH = ROOT / "metadata/persistence-acceptance.json"
BLOCKERS_PATH = ROOT / "metadata/production-module-blockers.json"
NOOP_ANALYSIS_PATH = ROOT / "metadata/wom-public-produce-task-created-analysis.json"
WOM_TOOLBAR_PATH = ROOT / "metadata/wom-toolbar-action-coverage.json"

ALLOWED_STATUS = {"FAIL_BACKLOG", "BLOCKED"}
ALLOWED_CATEGORIES = {
    "false-success-api",
    "external-client-required",
    "missing-service-package",
    "missing-export-implementation",
    "composite-service-and-scope",
    "product-scope-confirmation",
    "missing-runtime-endpoint",
}
ALLOWED_DISPOSITIONS = {"module-backlog", "external-dependency", "product-decision"}
REQUIRED_TOP_LEVEL_KEYS = [
    "schemaVersion",
    "generatedAt",
    "repoCommit",
    "database",
    "module",
    "overallStatus",
    "sourceReports",
    "summary",
    "items",
]
REQUIRED_ITEM_KEYS = [
    "id",
    "title",
    "status",
    "category",
    "disposition",
    "isPostgresCompatibilityGap",
    "sourceStatus",
    "productionCaseIds",
    "persistenceCoverage",
    "evidenceRefs",
    "recheckCommands",
    "passCriteria",
    "nextActions",
    "nonSolutions",
]


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def read_json(path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing JSON file: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{path.relative_to(ROOT)} must be a JSON object")
        return {}
    return data


def coverage_key(item: dict[str, Any]) -> tuple[str, str, str, str]:
    return (
        str(item.get("status", "")).strip(),
        str(item.get("module", "")).strip(),
        str(item.get("operation", "")).strip(),
        str(item.get("api", "")).strip(),
    )


def check_doc(expected_ids: set[str], failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"missing backlog document: {DOC_PATH.relative_to(ROOT)}")
        return
    text = DOC_PATH.read_text(encoding="utf-8")
    for fragment in (
        "# 生产模块 Backlog 账本",
        "make production-module-backlog-check",
        "FAIL_BACKLOG",
        "public `produceTaskCreated` no-op",
        "已显式禁用",
    ):
        if fragment not in text:
            fail(failures, f"production backlog document missing required text: {fragment}")
    for item_id in sorted(expected_ids):
        if item_id not in text:
            fail(failures, f"production backlog document missing item {item_id}")


def check_refs(item_id: str, refs: list[Any], failures: list[str]) -> None:
    if not refs:
        fail(failures, f"{item_id} must include evidenceRefs")
        return
    for ref in refs:
        if not isinstance(ref, str) or not ref.strip():
            fail(failures, f"{item_id}.evidenceRefs must contain non-empty repository paths")
            continue
        path = Path(ref)
        if path.is_absolute() or ".." in path.parts:
            fail(failures, f"{item_id} evidence ref must stay inside the repository: {ref}")
            continue
        if not (ROOT / path).exists():
            fail(failures, f"{item_id} evidence ref does not exist: {ref}")


def check_item(
    index: int,
    item: Any,
    failures: list[str],
) -> tuple[str, list[tuple[str, str, str, str]], list[str], list[str]]:
    if not isinstance(item, dict):
        fail(failures, f"items[{index}] must be an object")
        return "", [], [], []

    for key in REQUIRED_ITEM_KEYS:
        if key not in item:
            fail(failures, f"items[{index}] missing required key: {key}")

    item_id = str(item.get("id", "")).strip()
    if not item_id:
        fail(failures, f"items[{index}].id must not be empty")

    status = item.get("status")
    if status not in ALLOWED_STATUS:
        fail(failures, f"{item_id} has invalid status: {status!r}")

    category = item.get("category")
    if category not in ALLOWED_CATEGORIES:
        fail(failures, f"{item_id} has invalid category: {category!r}")

    disposition = item.get("disposition")
    if disposition not in ALLOWED_DISPOSITIONS:
        fail(failures, f"{item_id} has invalid disposition: {disposition!r}")

    if not isinstance(item.get("isPostgresCompatibilityGap"), bool):
        fail(failures, f"{item_id}.isPostgresCompatibilityGap must be boolean")

    for key in ("title", "sourceStatus"):
        if not str(item.get(key, "")).strip():
            fail(failures, f"{item_id}.{key} must not be empty")

    for key in ("productionCaseIds", "persistenceCoverage", "evidenceRefs", "recheckCommands", "passCriteria", "nextActions", "nonSolutions"):
        if not isinstance(item.get(key), list):
            fail(failures, f"{item_id}.{key} must be a list")

    for key in ("evidenceRefs", "recheckCommands", "passCriteria", "nextActions", "nonSolutions"):
        if not as_list(item.get(key)):
            fail(failures, f"{item_id}.{key} must not be empty")

    check_refs(item_id, as_list(item.get("evidenceRefs")), failures)

    text = json.dumps(item, ensure_ascii=False).lower()
    if item.get("status") == "FAIL_BACKLOG":
        if "http 200" not in text or "postgresql" not in text:
            fail(failures, f"{item_id} FAIL_BACKLOG must explain HTTP success plus PostgreSQL proof")
        if "nonSolutions" in item and "producetaskcreated2" not in text.lower():
            fail(failures, f"{item_id} must explicitly forbid using produceTaskCreated2 as replacement proof")
    if item.get("category") == "missing-service-package":
        if "business-dependency-readiness-smoke" not in text or "business-dependency-package-scan" not in text:
            fail(failures, f"{item_id} missing-service-package must reference dependency readiness and package scan")
    if item.get("category") == "missing-export-implementation" and "not_applicable" not in text:
        fail(failures, f"{item_id} export backlog must explain PostgreSQL persistence is NOT_APPLICABLE")
    if item.get("category") == "missing-runtime-endpoint":
        if "wom-toolbar-action-coverage" not in text or "404" not in text:
            fail(failures, f"{item_id} runtime-endpoint backlog must reference WOM toolbar coverage and 404 evidence")
    if item.get("isPostgresCompatibilityGap") is True and "sql" not in text and "postgres" not in text:
        fail(failures, f"{item_id} PostgreSQL gap must reference SQL/PostgreSQL handling")

    coverage: list[tuple[str, str, str, str]] = []
    for coverage_item in as_list(item.get("persistenceCoverage")):
        if not isinstance(coverage_item, dict):
            fail(failures, f"{item_id}.persistenceCoverage entries must be objects")
            continue
        key = coverage_key(coverage_item)
        if not all(key):
            fail(failures, f"{item_id}.persistenceCoverage entry must include status/module/operation/api")
        coverage.append(key)

    case_ids = [str(case_id).strip() for case_id in as_list(item.get("productionCaseIds")) if str(case_id).strip()]
    toolbar_ids: list[str] = []
    if "womToolbarActionIds" in item:
        if not isinstance(item.get("womToolbarActionIds"), list):
            fail(failures, f"{item_id}.womToolbarActionIds must be a list when present")
        toolbar_ids = [str(action_id).strip() for action_id in as_list(item.get("womToolbarActionIds")) if str(action_id).strip()]

    return item_id, coverage, case_ids, toolbar_ids


def check_summary(backlog: dict[str, Any], items: list[dict[str, Any]], failures: list[str]) -> None:
    summary = backlog.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return

    expected = {
        "totalItems": len(items),
        "failBacklog": sum(1 for item in items if item.get("status") == "FAIL_BACKLOG"),
        "blocked": sum(1 for item in items if item.get("status") == "BLOCKED"),
        "postgresCompatibilityGap": sum(1 for item in items if item.get("isPostgresCompatibilityGap") is True),
        "moduleBacklog": sum(1 for item in items if item.get("disposition") == "module-backlog"),
        "missingServicePackage": sum(1 for item in items if item.get("category") == "missing-service-package"),
        "externalClientRequired": sum(1 for item in items if item.get("category") == "external-client-required"),
        "missingExportImplementation": sum(1 for item in items if item.get("category") == "missing-export-implementation"),
        "compositeServiceAndScope": sum(1 for item in items if item.get("category") == "composite-service-and-scope"),
        "productScopeConfirmation": sum(1 for item in items if item.get("category") == "product-scope-confirmation"),
        "falseSuccessApi": sum(1 for item in items if item.get("category") == "false-success-api"),
        "missingRuntimeEndpoint": sum(1 for item in items if item.get("category") == "missing-runtime-endpoint"),
    }
    for key, expected_value in expected.items():
        if summary.get(key) != expected_value:
            fail(failures, f"summary.{key} expected {expected_value}, got {summary.get(key)!r}")


def main() -> int:
    failures: list[str] = []
    backlog = read_json(BACKLOG_PATH, failures)
    persistence = read_json(PERSISTENCE_PATH, failures)
    blockers = read_json(BLOCKERS_PATH, failures)
    noop_analysis = read_json(NOOP_ANALYSIS_PATH, failures)
    wom_toolbar = read_json(WOM_TOOLBAR_PATH, failures)

    for key in REQUIRED_TOP_LEVEL_KEYS:
        if key not in backlog:
            fail(failures, f"backlog missing top-level key: {key}")

    if backlog.get("database") != "PostgreSQL":
        fail(failures, "production module backlog database must remain PostgreSQL")
    if backlog.get("module") != "production":
        fail(failures, "production module backlog module must be production")
    if backlog.get("overallStatus") != "OPEN":
        fail(failures, "production module backlog must remain OPEN while items are unresolved")

    for required_report in (
        "metadata/persistence-acceptance.json",
        "metadata/production-module-test-cases.json",
        "metadata/production-module-blockers.json",
        "metadata/wom-toolbar-action-coverage.json",
    ):
        if required_report not in as_list(backlog.get("sourceReports")):
            fail(failures, f"sourceReports missing {required_report}")

    items = backlog.get("items")
    if not isinstance(items, list):
        fail(failures, "items must be a list")
        items = []

    seen_ids: set[str] = set()
    covered_persistence: set[tuple[str, str, str, str]] = set()
    covered_cases: set[str] = set()
    covered_toolbar_actions: set[str] = set()
    typed_items = [item for item in items if isinstance(item, dict)]
    for index, item in enumerate(items):
        item_id, coverage, case_ids, toolbar_ids = check_item(index, item, failures)
        if not item_id:
            continue
        if item_id in seen_ids:
            fail(failures, f"duplicate backlog item id: {item_id}")
        seen_ids.add(item_id)
        covered_persistence.update(coverage)
        covered_cases.update(case_ids)
        covered_toolbar_actions.update(toolbar_ids)

    expected_persistence = {
        coverage_key(item)
        for item in as_list(persistence.get("items"))
        if isinstance(item, dict) and item.get("status") in {"FAIL", "BLOCKED"}
    }
    missing_persistence = sorted(expected_persistence - covered_persistence)
    if missing_persistence:
        fail(failures, "backlog missing persistence FAIL/BLOCKED coverage: " + json.dumps(missing_persistence, ensure_ascii=False))

    expected_blocked_cases = {
        str(blocker.get("caseId", "")).strip()
        for blocker in as_list(blockers.get("blockers"))
        if isinstance(blocker, dict) and blocker.get("status") == "BLOCKED"
    }
    expected_blocked_cases.discard("")
    missing_cases = sorted(expected_blocked_cases - covered_cases)
    if missing_cases:
        fail(failures, f"backlog missing production blocked cases: {missing_cases}")

    expected_toolbar_actions: set[str] = set()
    for action in as_list(wom_toolbar.get("actions")):
        if not isinstance(action, dict) or action.get("acceptanceStatus") != "BLOCKED":
            continue
        action_id = str(action.get("id", "")).strip()
        if not action_id:
            fail(failures, "WOM toolbar BLOCKED action must include id")
            continue
        action_cases = {str(case_id).strip() for case_id in as_list(action.get("productionCaseIds")) if str(case_id).strip()}
        if action_cases and action_cases & covered_cases:
            continue
        expected_toolbar_actions.add(action_id)
    missing_toolbar_actions = sorted(expected_toolbar_actions - covered_toolbar_actions)
    if missing_toolbar_actions:
        fail(failures, f"backlog missing WOM toolbar BLOCKED action coverage: {missing_toolbar_actions}")

    noop_item = next((item for item in typed_items if item.get("id") == "PROD-ACTION-007"), None)
    if noop_item is None:
        fail(failures, "backlog must contain PROD-ACTION-007 for public produceTaskCreated product decision")
    else:
        if noop_item.get("status") != "BLOCKED":
            fail(failures, "PROD-ACTION-007 must remain BLOCKED while product decision is pending")
        if noop_item.get("category") != "product-scope-confirmation":
            fail(failures, "PROD-ACTION-007 must remain category=product-scope-confirmation")
        if noop_analysis.get("status") != "BLOCKED_EXPLICIT_DISABLED":
            fail(failures, "wom public produceTaskCreated analysis must remain BLOCKED_EXPLICIT_DISABLED")
        if noop_analysis.get("classification") != "explicit-disabled-pending-product-decision":
            fail(failures, "wom public produceTaskCreated analysis must classify explicit disabled pending product decision")
        if noop_analysis.get("isPostgresCompatibilityGap") is not False:
            fail(failures, "wom public produceTaskCreated analysis must state this is not a PostgreSQL compatibility gap")
        latest_probe = noop_item.get("latestProbe")
        if not isinstance(latest_probe, dict) or latest_probe.get("status") != "EXPLICIT_REJECTION_NO_PERSISTENCE":
            fail(failures, "PROD-ACTION-007 latestProbe must be EXPLICIT_REJECTION_NO_PERSISTENCE")

    check_summary(backlog, typed_items, failures)
    check_doc(seen_ids, failures)

    if failures:
        return 1
    print("Production module backlog verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
