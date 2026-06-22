#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/business-module-intake-requirements.json"
DOC_PATH = ROOT / "docs/business-module-intake-requirements.md"
BACKLOG_PATH = ROOT / "metadata/production-module-backlog.json"
BLOCKERS_PATH = ROOT / "metadata/production-module-blockers.json"
BUSINESS_CONTRACTS_PATH = ROOT / "metadata/business-dependency-contracts.json"
EXPORT_GAP_PATH = ROOT / "metadata/production-export-gap-breakdown.json"
WOM_TOOLBAR_PATH = ROOT / "metadata/wom-toolbar-action-coverage.json"

ALLOWED_STATUS = {"BLOCKED"}
INTAKE_TYPE_BY_CATEGORY = {
    "product-scope-confirmation": "product-decision",
    "missing-runtime-endpoint": "runtime-endpoint-recovery",
    "external-client-required": "external-client-adapter",
    "missing-service-package": "service-package",
    "composite-service-and-scope": "service-package-and-scope-decision",
    "missing-export-implementation": "export-implementation",
}
EXPECTED_DEPENDENCY_CONTRACTS = {
    "PROD-019": {"material-service"},
    "PROD-020": {"process-analysis"},
    "PROD-021": {"material-service"},
    "PROD-022": {"material-service"},
}
REQUIRED_TOP_LEVEL_KEYS = {
    "schemaVersion",
    "generatedAt",
    "repoCommit",
    "database",
    "module",
    "overallStatus",
    "purpose",
    "sourceReports",
    "summary",
    "requirements",
}
REQUIRED_ITEM_KEYS = {
    "id",
    "title",
    "status",
    "category",
    "sourceBacklogId",
    "linkedBlockerCases",
    "requiredIntakeType",
    "dependencyContractIds",
    "requiredPackages",
    "packageSearchTerms",
    "frontendEntrypoints",
    "apiEndpoints",
    "persistenceExpectation",
    "targetTables",
    "requiredEvidence",
    "recheckCommands",
    "passCriteria",
    "nonSolutions",
    "sourceEvidenceRefs",
}
FORBIDDEN_TEXT = {
    "ft" + "123456789",
    "Authorization: Bearer",
    "access_token",
    "refresh_token",
    "BEGIN PRIVATE KEY",
    "BEGIN RSA PRIVATE KEY",
    "BEGIN OPENSSH PRIVATE KEY",
}
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


def read_json(path: Path, failures: list[str], label: str) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing {label}: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{label} must be a JSON object")
        return {}
    return data


def validate_relative_path(path_text: Any, failures: list[str], owner: str) -> None:
    if not isinstance(path_text, str) or not path_text.strip():
        fail(failures, f"{owner} sourceEvidenceRefs must contain non-empty repository paths")
        return
    path = Path(path_text)
    if path.is_absolute() or ".." in path.parts:
        fail(failures, f"{owner} sourceEvidenceRef must stay inside repo: {path_text}")
        return
    if not (ROOT / path).exists():
        fail(failures, f"{owner} sourceEvidenceRef does not exist: {path_text}")


def check_secret_hygiene(failures: list[str]) -> None:
    for path in (REPORT_PATH, DOC_PATH):
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        for forbidden in FORBIDDEN_TEXT:
            if forbidden in text:
                fail(failures, f"{path.relative_to(ROOT)} contains forbidden secret-like text: {forbidden}")
        if SECRET_ASSIGNMENT_RE.search(text):
            fail(failures, f"{path.relative_to(ROOT)} appears to contain a non-placeholder secret assignment")


def check_doc(expected_ids: set[str], failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"required document missing: {DOC_PATH.relative_to(ROOT)}")
        return
    text = DOC_PATH.read_text(encoding="utf-8")
    for fragment in (
        "metadata/business-module-intake-requirements.json",
        "make business-module-intake-requirements-check",
        "PostgreSQL",
        "ADP_E2E_*",
        "真实浏览器",
        "NOT_APPLICABLE",
        "material",
        "ProcessAnalysis",
        "PROD-ACTION-008",
        "PROD-023",
    ):
        if fragment not in text:
            fail(failures, f"business module intake document missing required text: {fragment}")
    for item_id in sorted(expected_ids):
        if item_id not in text:
            fail(failures, f"business module intake document missing item {item_id}")


def text_blob(item: dict[str, Any]) -> str:
    return json.dumps(item, ensure_ascii=False).lower()


def check_summary(data: dict[str, Any], requirements: list[dict[str, Any]], failures: list[str]) -> None:
    summary = as_dict(data.get("summary"))
    if not summary:
        fail(failures, "summary must be an object")
        return
    expected = {
        "requirements": len(requirements),
        "blocked": sum(1 for item in requirements if item.get("status") == "BLOCKED"),
        "productDecision": sum(1 for item in requirements if item.get("requiredIntakeType") == "product-decision"),
        "runtimeEndpointRecovery": sum(1 for item in requirements if item.get("requiredIntakeType") == "runtime-endpoint-recovery"),
        "externalClientAdapter": sum(1 for item in requirements if item.get("requiredIntakeType") == "external-client-adapter"),
        "servicePackage": sum(1 for item in requirements if item.get("requiredIntakeType") == "service-package"),
        "servicePackageAndScopeDecision": sum(1 for item in requirements if item.get("requiredIntakeType") == "service-package-and-scope-decision"),
        "exportImplementation": sum(1 for item in requirements if item.get("requiredIntakeType") == "export-implementation"),
        "postgresCompatibilityGap": 0,
        "requiresRealBrowserProof": sum(1 for item in requirements if "browser" in text_blob(item) or "真实浏览器" in text_blob(item)),
        "requiresPostgreSQLProof": sum(1 for item in requirements if "postgresql" in text_blob(item) and item.get("persistenceExpectation") != "NOT_APPLICABLE_FILE_EXPORT"),
        "requiresFileResponseProof": sum(1 for item in requirements if "file" in text_blob(item) or "workbook" in text_blob(item)),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def check_item_shape(item: dict[str, Any], failures: list[str]) -> None:
    item_id = str(item.get("id", "")).strip()
    for key in REQUIRED_ITEM_KEYS:
        if key not in item:
            fail(failures, f"{item_id or 'requirement'} missing required key: {key}")

    if not item_id:
        fail(failures, "requirement id must not be empty")
    if item.get("status") not in ALLOWED_STATUS:
        fail(failures, f"{item_id} has invalid status: {item.get('status')!r}")
    expected_type = INTAKE_TYPE_BY_CATEGORY.get(str(item.get("category")))
    if expected_type is None:
        fail(failures, f"{item_id} has unknown category: {item.get('category')!r}")
    elif item.get("requiredIntakeType") != expected_type:
        fail(failures, f"{item_id}.requiredIntakeType must be {expected_type}")

    for key in (
        "linkedBlockerCases",
        "dependencyContractIds",
        "requiredPackages",
        "packageSearchTerms",
        "frontendEntrypoints",
        "apiEndpoints",
        "targetTables",
        "requiredEvidence",
        "recheckCommands",
        "passCriteria",
        "nonSolutions",
        "sourceEvidenceRefs",
    ):
        if not isinstance(item.get(key), list):
            fail(failures, f"{item_id}.{key} must be a list")
    for key in ("packageSearchTerms", "frontendEntrypoints", "apiEndpoints", "requiredEvidence", "recheckCommands", "passCriteria", "nonSolutions", "sourceEvidenceRefs"):
        if not as_list(item.get(key)):
            fail(failures, f"{item_id}.{key} must not be empty")

    for ref in as_list(item.get("sourceEvidenceRefs")):
        validate_relative_path(ref, failures, item_id)

    text = text_blob(item)
    if "http 200" in text and "postgresql" not in text and item.get("requiredIntakeType") != "export-implementation":
        fail(failures, f"{item_id} mentions HTTP 200 but does not require PostgreSQL proof")
    if item.get("requiredIntakeType") != "export-implementation" and "not_applicable_file_export" not in text:
        if "postgresql" not in text:
            fail(failures, f"{item_id} must require PostgreSQL evidence or explain why persistence is not applicable")
    if item.get("requiredIntakeType") == "export-implementation":
        if item.get("persistenceExpectation") != "NOT_APPLICABLE_FILE_EXPORT":
            fail(failures, f"{item_id} export implementation must mark persistence as NOT_APPLICABLE_FILE_EXPORT")
        for fragment in ("workbook", "file", "visible export", "runtime"):
            if fragment not in text:
                fail(failures, f"{item_id} export requirement missing file-response proof fragment: {fragment}")
    if item.get("requiredIntakeType") == "runtime-endpoint-recovery":
        for fragment in ("404", "wom-toolbar-action-coverage", "generateqrcode"):
            if fragment not in text:
                fail(failures, f"{item_id} endpoint recovery requirement missing fragment: {fragment}")


def check_cross_refs(
    data: dict[str, Any],
    backlog: dict[str, Any],
    blockers: dict[str, Any],
    contracts: dict[str, Any],
    export_gap: dict[str, Any],
    wom_toolbar: dict[str, Any],
    failures: list[str],
) -> None:
    backlog_items = {
        str(item.get("id")): item
        for item in as_list(backlog.get("items"))
        if isinstance(item, dict) and item.get("status") == "BLOCKED"
    }
    requirements = {
        str(item.get("id")): item
        for item in as_list(data.get("requirements"))
        if isinstance(item, dict)
    }
    if set(requirements) != set(backlog_items):
        fail(
            failures,
            "business module intake requirements must cover exactly current BLOCKED production backlog ids: "
            f"missing={sorted(set(backlog_items) - set(requirements))}, extra={sorted(set(requirements) - set(backlog_items))}",
        )

    blocker_cases = {
        str(item.get("caseId"))
        for item in as_list(blockers.get("blockers"))
        if isinstance(item, dict) and item.get("status") == "BLOCKED"
    }
    contract_ids = {
        str(item.get("id"))
        for item in as_list(contracts.get("dependencies"))
        if isinstance(item, dict)
    }

    export_summary = as_dict(export_gap.get("summary"))
    verified_exports = export_summary.get("verifiedDataExports")
    targets = export_summary.get("targets")
    if export_summary.get("status") != "BLOCKED" or not isinstance(verified_exports, int):
        fail(failures, "PROD-023 intake requirement expects export gap breakdown to remain BLOCKED with an integer verifiedDataExports count")
    elif isinstance(targets, int) and verified_exports >= targets:
        fail(failures, "PROD-023 intake requirement expects at least one export target to remain unresolved while the blocker is active")

    toolbar_text = text_blob(wom_toolbar)
    if "generate-qrcode" not in toolbar_text or "blocked" not in toolbar_text:
        fail(failures, "PROD-ACTION-008 intake requirement expects WOM toolbar coverage to retain blocked generate-qrcode evidence")

    for item_id, item in requirements.items():
        backlog_item = backlog_items.get(item_id, {})
        if item.get("sourceBacklogId") != item_id:
            fail(failures, f"{item_id}.sourceBacklogId must match the requirement id")
        for key in ("title", "status", "category"):
            if item.get(key) != backlog_item.get(key):
                fail(failures, f"{item_id}.{key} must match production-module-backlog.json")
        expected_cases = {str(case_id) for case_id in as_list(backlog_item.get("productionCaseIds"))}
        linked_cases = {str(case_id) for case_id in as_list(item.get("linkedBlockerCases"))}
        if linked_cases != expected_cases:
            fail(failures, f"{item_id}.linkedBlockerCases must match backlog productionCaseIds")
        missing_blockers = sorted(linked_cases - blocker_cases)
        if missing_blockers:
            fail(failures, f"{item_id} links blocker cases absent from production-module-blockers.json: " + ", ".join(missing_blockers))
        expected_contracts = EXPECTED_DEPENDENCY_CONTRACTS.get(item_id, set())
        dependency_contracts = {str(contract_id) for contract_id in as_list(item.get("dependencyContractIds"))}
        if dependency_contracts != expected_contracts:
            fail(failures, f"{item_id}.dependencyContractIds must be {sorted(expected_contracts)}")
        missing_contracts = sorted(dependency_contracts - contract_ids)
        if missing_contracts:
            fail(failures, f"{item_id} references missing business dependency contracts: " + ", ".join(missing_contracts))
        backlog_text = text_blob(backlog_item)
        item_text = text_blob(item)
        for command in as_list(backlog_item.get("recheckCommands")):
            primary_command = str(command).split(" ", 1)[0]
            if primary_command == "manual:":
                continue
            if primary_command and primary_command not in item_text:
                fail(failures, f"{item_id} intake requirement should carry backlog recheck command family: {primary_command}")
        if item_id == "PROD-023" and "production-export-gap-breakdown" not in item_text:
            fail(failures, "PROD-023 must reference production export gap breakdown")
        if item_id == "PROD-ACTION-008" and "wom-qrcode-route-probe" not in item_text:
            fail(failures, "PROD-ACTION-008 must reference WOM QR route probe evidence")
        if "postgresql" in backlog_text and item.get("requiredIntakeType") != "export-implementation" and "postgresql" not in item_text:
            fail(failures, f"{item_id} backlog requires PostgreSQL proof but intake requirement does not")


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    missing = sorted(REQUIRED_TOP_LEVEL_KEYS - set(data))
    if missing:
        fail(failures, "business module intake report missing top-level keys: " + ", ".join(missing))
    if data.get("database") != "PostgreSQL":
        fail(failures, "business module intake database must remain PostgreSQL")
    if data.get("module") != "production":
        fail(failures, "business module intake module must be production")
    if data.get("overallStatus") != "BLOCKED":
        fail(failures, "business module intake overallStatus must remain BLOCKED until all requirements pass")
    for required_report in (
        "metadata/production-module-backlog.json",
        "metadata/production-module-blockers.json",
        "metadata/business-dependency-contracts.json",
        "metadata/production-export-gap-breakdown.json",
        "metadata/wom-toolbar-action-coverage.json",
    ):
        if required_report not in as_list(data.get("sourceReports")):
            fail(failures, f"sourceReports missing {required_report}")

    raw_requirements = data.get("requirements")
    if not isinstance(raw_requirements, list):
        fail(failures, "requirements must be a list")
        return
    requirements = [item for item in raw_requirements if isinstance(item, dict)]
    if len(requirements) != len(raw_requirements):
        fail(failures, "requirements must contain only objects")
    seen: set[str] = set()
    for requirement in requirements:
        item_id = str(requirement.get("id", "")).strip()
        if item_id in seen:
            fail(failures, f"duplicate requirement id: {item_id}")
        seen.add(item_id)
        check_item_shape(requirement, failures)
    check_summary(data, requirements, failures)


def main() -> int:
    failures: list[str] = []
    check_secret_hygiene(failures)
    report = read_json(REPORT_PATH, failures, "business module intake requirements")
    backlog = read_json(BACKLOG_PATH, failures, "production module backlog")
    blockers = read_json(BLOCKERS_PATH, failures, "production module blockers")
    contracts = read_json(BUSINESS_CONTRACTS_PATH, failures, "business dependency contracts")
    export_gap = read_json(EXPORT_GAP_PATH, failures, "production export gap breakdown")
    wom_toolbar = read_json(WOM_TOOLBAR_PATH, failures, "WOM toolbar action coverage")
    expected_ids = {
        str(item.get("id"))
        for item in as_list(backlog.get("items"))
        if isinstance(item, dict) and item.get("status") == "BLOCKED"
    }
    check_doc(expected_ids, failures)
    if report:
        check_report(report, failures)
    if report and backlog and blockers and contracts and export_gap and wom_toolbar:
        check_cross_refs(report, backlog, blockers, contracts, export_gap, wom_toolbar, failures)
    if failures:
        print(f"Business module intake requirements verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Business module intake requirements verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
