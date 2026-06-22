#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/production-module-blockers.json"
MATRIX_PATH = ROOT / "metadata/production-module-test-cases.json"
DOC_PATH = ROOT / "docs/production-module-blockers.md"
BUSINESS_DEPENDENCY_READINESS_PATH = ROOT / "metadata/business-dependency-readiness-smoke.json"
BUSINESS_PACKAGE_SCAN_PATH = ROOT / "metadata/business-dependency-package-scan.json"
PRODUCTION_EXPORT_READINESS_PATH = ROOT / "metadata/production-export-readiness-smoke.json"

ALLOWED_CATEGORIES = {
    "external-client-required",
    "missing-service-package",
    "product-scope-confirmation",
    "composite-service-and-scope",
    "missing-export-implementation",
}

REQUIRED_TOP_LEVEL_KEYS = [
    "schemaVersion",
    "generatedAt",
    "repoCommit",
    "database",
    "module",
    "sourceMatrix",
    "summary",
    "latestEvidence",
    "blockers",
]

REQUIRED_BLOCKER_KEYS = [
    "caseId",
    "title",
    "status",
    "category",
    "blockerCategory",
    "dependency",
    "currentEvidenceRefs",
    "recheckCommands",
    "passCriteria",
    "nextActions",
    "nonSolutions",
]

MATERIAL_CASES = {"PROD-019", "PROD-021", "PROD-022"}
PROCESS_CASES = {"PROD-020"}


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
        fail(failures, f"missing JSON file: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{path.relative_to(ROOT)} must be a JSON object")
        return {}
    return data


def item_by_id(report: dict[str, Any], key: str, item_id: str) -> dict[str, Any] | None:
    for item in as_list(report.get(key)):
        if isinstance(item, dict) and item.get("id") == item_id:
            return item
    return None


def require_ref(case_id: str, blocker: dict[str, Any], expected_ref: Path, failures: list[str]) -> None:
    expected = str(expected_ref.relative_to(ROOT))
    refs = {str(ref) for ref in as_list(blocker.get("currentEvidenceRefs"))}
    if expected not in refs:
        fail(failures, f"{case_id} must include evidence ref {expected}")


def require_required_for(item: dict[str, Any], expected_cases: set[str], label: str, failures: list[str]) -> None:
    required_for = {str(value) for value in as_list(item.get("requiredFor"))}
    missing = sorted(expected_cases - required_for)
    if missing:
        fail(failures, f"{label}.requiredFor missing blocked cases: {missing}")


def healthy_host_count(service: Any) -> int:
    if not isinstance(service, dict):
        return -1
    value = service.get("healthyHostCount")
    return value if isinstance(value, int) else -1


def endpoint_is_blocked(endpoint: Any) -> bool:
    if not isinstance(endpoint, dict):
        return False
    return endpoint.get("status") == 503 and endpoint.get("tenantServiceMissing") is True


def blocked_case_ids(matrix: dict[str, Any], failures: list[str]) -> set[str]:
    cases = matrix.get("cases")
    if not isinstance(cases, list):
        fail(failures, "production module matrix must contain cases list")
        return set()
    blocked: set[str] = set()
    for index, case in enumerate(cases):
        if not isinstance(case, dict):
            continue
        if case.get("acceptanceStatus") == "BLOCKED":
            case_id = str(case.get("id", "")).strip()
            if not case_id:
                fail(failures, f"blocked matrix case at index {index} is missing id")
            else:
                blocked.add(case_id)
    return blocked


def check_doc(expected_case_ids: set[str], failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"required document missing: {DOC_PATH.relative_to(ROOT)}")
        return
    text = DOC_PATH.read_text(encoding="utf-8")
    required_fragments = [
        "# 生产模块阻断项账本",
        "| Case | 阻断类型 | 依赖/决策 | 复验入口 | PASS 条件 | 下一步 |",
        "make production-blocker-check",
    ]
    for fragment in required_fragments:
        if fragment not in text:
            fail(failures, f"production blocker document missing required text: {fragment}")
    for case_id in sorted(expected_case_ids):
        if case_id not in text:
            fail(failures, f"production blocker document missing blocked case {case_id}")


def check_evidence_refs(case_id: str, refs: list[Any], failures: list[str]) -> None:
    if not refs:
        fail(failures, f"{case_id} must include currentEvidenceRefs")
        return
    for ref in refs:
        if not isinstance(ref, str) or not ref.strip():
            fail(failures, f"{case_id}.currentEvidenceRefs must contain non-empty repository paths")
            continue
        path = Path(ref)
        if path.is_absolute() or ".." in path.parts:
            fail(failures, f"{case_id} evidence ref must stay inside the repository: {ref}")
            continue
        if not (ROOT / path).exists():
            fail(failures, f"{case_id} evidence ref does not exist: {ref}")


def check_blocker(index: int, blocker: Any, failures: list[str]) -> str | None:
    if not isinstance(blocker, dict):
        fail(failures, f"blockers[{index}] must be an object")
        return None

    for key in REQUIRED_BLOCKER_KEYS:
        if key not in blocker:
            fail(failures, f"blockers[{index}] missing required key: {key}")

    case_id = str(blocker.get("caseId", "")).strip()
    if not case_id.startswith("PROD-"):
        fail(failures, f"blockers[{index}].caseId must start with PROD-")

    if blocker.get("status") != "BLOCKED":
        fail(failures, f"{case_id} status must remain BLOCKED")

    category = blocker.get("blockerCategory")
    if category not in ALLOWED_CATEGORIES:
        fail(failures, f"{case_id} has invalid blockerCategory: {category!r}")
    if blocker.get("category") != category:
        fail(failures, f"{case_id}.category must mirror blockerCategory ({blocker.get('category')!r} != {category!r})")

    for key in ("title", "dependency"):
        if not str(blocker.get(key, "")).strip():
            fail(failures, f"{case_id} must include non-empty {key}")

    for key in ("currentEvidenceRefs", "recheckCommands", "passCriteria", "nextActions", "nonSolutions"):
        if not isinstance(blocker.get(key), list):
            fail(failures, f"{case_id}.{key} must be a list")
        elif not as_list(blocker.get(key)):
            fail(failures, f"{case_id}.{key} must not be empty")

    check_evidence_refs(case_id, as_list(blocker.get("currentEvidenceRefs")), failures)

    text = json.dumps(blocker, ensure_ascii=False).lower()
    if case_id in MATERIAL_CASES:
        for required in ("material-service-dependency-analysis", "business-dependency-readiness-smoke", "business-dependency-package-scan"):
            if required not in text:
                fail(failures, f"{case_id} material blocker must reference {required}")
    if case_id in PROCESS_CASES:
        for required in ("processanalysis-dependency-analysis", "business-dependency-readiness-smoke", "business-dependency-package-scan"):
            if required not in text:
                fail(failures, f"{case_id} process blocker must reference {required}")
    if category in {"product-scope-confirmation", "missing-export-implementation", "composite-service-and-scope"}:
        if "product" not in text and "产品" not in text:
            fail(failures, f"{case_id} product-scope blocker must mention product confirmation/scope")
    if "postgresql" not in text and "database" not in text and "not_applicable" not in text:
        fail(failures, f"{case_id} blocker must include database/PostgreSQL pass criteria or a NOT_APPLICABLE explanation")

    return case_id


def check_report(report: dict[str, Any], matrix_blocked_ids: set[str], failures: list[str]) -> None:
    for key in REQUIRED_TOP_LEVEL_KEYS:
        if key not in report:
            fail(failures, f"blocker report missing top-level key: {key}")

    if report.get("database") != "PostgreSQL":
        fail(failures, "production blocker report database must remain PostgreSQL")
    if report.get("module") != "production":
        fail(failures, "production blocker report must identify module=production")
    if report.get("sourceMatrix") != str(MATRIX_PATH.relative_to(ROOT)):
        fail(failures, "production blocker report sourceMatrix must point to metadata/production-module-test-cases.json")

    blockers = report.get("blockers")
    if not isinstance(blockers, list):
        fail(failures, "blockers must be a list")
        return

    seen_case_ids: set[str] = set()
    for index, blocker in enumerate(blockers):
        case_id = check_blocker(index, blocker, failures)
        if not case_id:
            continue
        if case_id in seen_case_ids:
            fail(failures, f"duplicate production blocker caseId: {case_id}")
        seen_case_ids.add(case_id)

    if seen_case_ids != matrix_blocked_ids:
        fail(
            failures,
            "production blocker cases do not match matrix BLOCKED cases: "
            f"expected={sorted(matrix_blocked_ids)} actual={sorted(seen_case_ids)}",
        )

    summary = report.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return

    expected = {
        "blockedCases": len(matrix_blocked_ids),
        "blockers": len(blockers),
        "externalClientRequired": sum(1 for blocker in blockers if isinstance(blocker, dict) and blocker.get("blockerCategory") == "external-client-required"),
        "missingServicePackage": sum(1 for blocker in blockers if isinstance(blocker, dict) and blocker.get("blockerCategory") == "missing-service-package"),
        "productScopeConfirmation": sum(1 for blocker in blockers if isinstance(blocker, dict) and blocker.get("blockerCategory") == "product-scope-confirmation"),
        "compositeServiceAndScope": sum(1 for blocker in blockers if isinstance(blocker, dict) and blocker.get("blockerCategory") == "composite-service-and-scope"),
        "missingExportImplementation": sum(1 for blocker in blockers if isinstance(blocker, dict) and blocker.get("blockerCategory") == "missing-export-implementation"),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def check_dependency_readiness(report: dict[str, Any], blockers: dict[str, dict[str, Any]], failures: list[str]) -> None:
    summary = as_dict(report.get("summary"))
    if summary.get("status") != "BLOCKED":
        fail(failures, "business dependency readiness summary.status must remain BLOCKED while production blockers cite it")
    if summary.get("blocked") != 2 or summary.get("ready") != 0:
        fail(failures, "business dependency readiness summary must show blocked=2 and ready=0")

    material = item_by_id(report, "items", "material-service")
    if material is None:
        fail(failures, "business dependency readiness report missing material-service item")
    else:
        if material.get("status") != "BLOCKED":
            fail(failures, "material-service readiness status must remain BLOCKED")
        require_required_for(material, MATERIAL_CASES, "material-service readiness", failures)
        services = as_list(material.get("services"))
        if not services:
            fail(failures, "material-service readiness must record Nacos service probes")
        if any(healthy_host_count(service) != 0 for service in services):
            fail(failures, "material-service readiness must keep all probed healthyHostCount values at 0")
        endpoints = as_list(material.get("endpoints"))
        if len(endpoints) < 2 or not all(endpoint_is_blocked(endpoint) for endpoint in endpoints):
            fail(failures, "material-service readiness endpoints must remain tenant-service 503 blockers")
        database = as_dict(material.get("database"))
        if not isinstance(database.get("materialLikeTableCount"), int) or database.get("materialLikeTableCount", 0) <= 0:
            fail(failures, "material-service readiness must include discovered PostgreSQL material-like tables")

    process = item_by_id(report, "items", "process-analysis")
    if process is None:
        fail(failures, "business dependency readiness report missing process-analysis item")
    else:
        if process.get("status") != "BLOCKED":
            fail(failures, "process-analysis readiness status must remain BLOCKED")
        require_required_for(process, PROCESS_CASES, "process-analysis readiness", failures)
        services = as_list(process.get("services"))
        if not services:
            fail(failures, "process-analysis readiness must record Nacos service probes")
        if any(healthy_host_count(service) != 0 for service in services):
            fail(failures, "process-analysis readiness must keep all probed healthyHostCount values at 0")
        endpoints = as_list(process.get("endpoints"))
        if len(endpoints) < 3 or not all(endpoint_is_blocked(endpoint) for endpoint in endpoints):
            fail(failures, "process-analysis readiness endpoints must remain tenant-service 503 blockers")
        database = as_dict(process.get("database"))
        for key in ("processAnalysisTableCount", "processAnalysisRuntimeViewCount", "processAnalysisMenuCount"):
            if database.get(key) != 0:
                fail(failures, f"process-analysis readiness database.{key} must remain 0")

    for case_id in sorted(MATERIAL_CASES | PROCESS_CASES):
        blocker = blockers.get(case_id)
        if blocker is not None:
            require_ref(case_id, blocker, BUSINESS_DEPENDENCY_READINESS_PATH, failures)


def check_package_scan(report: dict[str, Any], blockers: dict[str, dict[str, Any]], failures: list[str]) -> None:
    summary = as_dict(report.get("summary"))
    if summary.get("status") != "BLOCKED_NO_IMPLEMENTATION_CANDIDATE":
        fail(failures, "business dependency package scan summary.status must remain BLOCKED_NO_IMPLEMENTATION_CANDIDATE")
    if summary.get("candidateDependencies") != 0:
        fail(failures, "business dependency package scan summary.candidateDependencies must remain 0")
    if summary.get("blockedDependencies") != 2:
        fail(failures, "business dependency package scan summary.blockedDependencies must remain 2")

    expected_dependencies = {
        "material-service": MATERIAL_CASES,
        "process-analysis": PROCESS_CASES,
    }
    for dependency_id, expected_cases in expected_dependencies.items():
        dependency = item_by_id(report, "dependencies", dependency_id)
        if dependency is None:
            fail(failures, f"business dependency package scan missing dependency {dependency_id}")
            continue
        if dependency.get("status") != "BLOCKED_NO_IMPLEMENTATION_CANDIDATE":
            fail(failures, f"{dependency_id} package scan status must remain BLOCKED_NO_IMPLEMENTATION_CANDIDATE")
        require_required_for(dependency, expected_cases, f"{dependency_id} package scan", failures)
        if dependency.get("implementationCandidateCount") != 0:
            fail(failures, f"{dependency_id} package scan implementationCandidateCount must remain 0")
        if as_list(dependency.get("implementationCandidates")):
            fail(failures, f"{dependency_id} package scan implementationCandidates must remain empty")

    for case_id in sorted(MATERIAL_CASES | PROCESS_CASES):
        blocker = blockers.get(case_id)
        if blocker is not None:
            require_ref(case_id, blocker, BUSINESS_PACKAGE_SCAN_PATH, failures)


def check_export_readiness(report: dict[str, Any], blockers: dict[str, dict[str, Any]], failures: list[str]) -> None:
    blocker = blockers.get("PROD-023")
    if blocker is None:
        return
    require_ref("PROD-023", blocker, PRODUCTION_EXPORT_READINESS_PATH, failures)

    if report.get("module") != "production" or report.get("caseId") != "PROD-023":
        fail(failures, "production export readiness report must identify module=production and caseId=PROD-023")
    summary = as_dict(report.get("summary"))
    if summary.get("status") != "BLOCKED":
        fail(failures, "production export readiness summary.status must remain BLOCKED")
    targets = summary.get("targets")
    if not isinstance(targets, int) or targets <= 0:
        fail(failures, "production export readiness summary.targets must be a positive integer")
        targets = 0
    page_pass = summary.get("pagePass")
    if not isinstance(page_pass, int) or page_pass < 0 or page_pass > targets:
        fail(failures, "production export readiness summary.pagePass must be an integer between 0 and targets")
    if summary.get("verifiedDataExports") != 0:
        fail(failures, "production export readiness summary.verifiedDataExports must remain 0")
    blocked = summary.get("blocked")
    action_required = summary.get("actionRequired", 0)
    ready = summary.get("ready", 0)
    if not isinstance(blocked, int) or blocked < 0 or blocked > targets:
        fail(failures, "production export readiness summary.blocked must be an integer between 0 and targets")
        blocked = 0
    if not isinstance(action_required, int) or action_required < 0 or action_required > targets:
        fail(failures, "production export readiness summary.actionRequired must be an integer between 0 and targets")
        action_required = 0
    if not isinstance(ready, int) or ready < 0 or ready > targets:
        fail(failures, "production export readiness summary.ready must be an integer between 0 and targets")
        ready = 0
    if blocked + action_required + ready != targets:
        fail(failures, "production export readiness blocked/actionRequired/ready counts must equal targets")
    if blocked <= 0:
        fail(failures, "production export readiness must still have blocked targets while PROD-023 is BLOCKED")
    if summary.get("visibleExportActions") != 0 or summary.get("runtimeExportActions") != 0:
        fail(failures, "production export readiness must not claim visible/runtime export actions while PROD-023 is BLOCKED")


def check_latest_evidence(
    report: dict[str, Any],
    dependency_readiness: dict[str, Any],
    package_scan: dict[str, Any],
    export_readiness: dict[str, Any],
    failures: list[str],
) -> None:
    latest = as_dict(report.get("latestEvidence"))
    if not latest:
        fail(failures, "production blocker report must include latestEvidence")
        return

    dependency_summary = as_dict(dependency_readiness.get("summary"))
    package_summary = as_dict(package_scan.get("summary"))
    export_summary = as_dict(export_readiness.get("summary"))
    expected = {
        "businessDependencyReadinessGeneratedAt": dependency_readiness.get("generatedAt"),
        "businessDependencyReadinessStatus": dependency_summary.get("status"),
        "businessDependencyReady": dependency_summary.get("ready"),
        "businessDependencyBlocked": dependency_summary.get("blocked"),
        "businessPackageScanGeneratedAt": package_scan.get("generatedAt"),
        "businessPackageScanStatus": package_summary.get("status"),
        "businessPackageScanCandidateDependencies": package_summary.get("candidateDependencies"),
        "productionExportReadinessGeneratedAt": export_readiness.get("generatedAt"),
        "productionExportReadinessStatus": export_summary.get("status"),
        "productionExportVerifiedDataExports": export_summary.get("verifiedDataExports"),
    }
    for key, value in expected.items():
        if latest.get(key) != value:
            fail(failures, f"latestEvidence.{key}={latest.get(key)!r} must match current evidence value {value!r}")
    if latest.get("lastRemoteRecheckAt") != export_readiness.get("generatedAt"):
        fail(failures, "latestEvidence.lastRemoteRecheckAt must match production export readiness generatedAt")


def check_cross_ledgers(
    report: dict[str, Any],
    dependency_readiness: dict[str, Any],
    package_scan: dict[str, Any],
    export_readiness: dict[str, Any],
    failures: list[str],
) -> None:
    blockers = {
        str(blocker.get("caseId")): blocker
        for blocker in as_list(report.get("blockers"))
        if isinstance(blocker, dict) and str(blocker.get("caseId", "")).strip()
    }
    check_dependency_readiness(dependency_readiness, blockers, failures)
    check_package_scan(package_scan, blockers, failures)
    check_export_readiness(export_readiness, blockers, failures)
    check_latest_evidence(report, dependency_readiness, package_scan, export_readiness, failures)


def main() -> int:
    failures: list[str] = []
    matrix = read_json(MATRIX_PATH, failures)
    blocked_ids = blocked_case_ids(matrix, failures) if matrix else set()
    check_doc(blocked_ids, failures)
    report = read_json(REPORT_PATH, failures)
    dependency_readiness = read_json(BUSINESS_DEPENDENCY_READINESS_PATH, failures)
    package_scan = read_json(BUSINESS_PACKAGE_SCAN_PATH, failures)
    export_readiness = read_json(PRODUCTION_EXPORT_READINESS_PATH, failures)
    if report:
        check_report(report, blocked_ids, failures)
    if report and dependency_readiness and package_scan and export_readiness:
        check_cross_ledgers(report, dependency_readiness, package_scan, export_readiness, failures)
    if failures:
        print(f"Production module blocker verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Production module blocker verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
