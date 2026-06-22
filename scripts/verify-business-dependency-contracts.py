#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
CONTRACT_PATH = ROOT / "metadata/business-dependency-contracts.json"
DOC_PATH = ROOT / "docs/business-dependency-contracts.md"
READINESS_PATH = ROOT / "metadata/business-dependency-readiness-smoke.json"
PACKAGE_SCAN_PATH = ROOT / "metadata/business-dependency-package-scan.json"
BLOCKERS_PATH = ROOT / "metadata/production-module-blockers.json"
BACKLOG_PATH = ROOT / "metadata/production-module-backlog.json"

REQUIRED_DEPENDENCIES = {
    "material-service": {
        "blocked_cases": {"PROD-019", "PROD-021", "PROD-022"},
        "endpoint_names": {"checkProdResult", "generateProduceOutSing"},
        "service_aliases": {"material", "MATERIAL", "wms", "WMS", "warehouse", "Warehouse", "inventory", "Inventory"},
        "required_runtime_keys": {
            "minimumHealthyInstances",
            "endpointHttpStatusMustBe2xx",
            "endpointTenantServiceMissingMustBeFalse",
            "mustTraceBackendPath",
            "mustMapTargetTables",
        },
    },
    "process-analysis": {
        "blocked_cases": {"PROD-020"},
        "endpoint_names": {"isProdprocessView", "processBatchViewOut", "analysisiTask", "manualStatActive", "manualStatProcess"},
        "service_aliases": {"ProcessAnalysis", "processanalysis", "PROCESSANALYSIS", "Traceability", "traceability"},
        "required_runtime_keys": {
            "minimumHealthyInstances",
            "endpointHttpStatusMustBe2xx",
            "endpointTenantServiceMissingMustBeFalse",
            "runtimeViewCountMustBePositive",
            "menuCountMustBePositive",
            "tableCountMustBePositive",
            "mustTraceBackendPath",
        },
    },
}
ALLOWED_STATUS = {"READY", "BLOCKED"}
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


def item_by_id(report: dict[str, Any], key: str, item_id: str) -> dict[str, Any] | None:
    for item in as_list(report.get(key)):
        if isinstance(item, dict) and item.get("id") == item_id:
            return item
    return None


def validate_relative_path(path_text: Any, failures: list[str], owner: str) -> None:
    if not isinstance(path_text, str) or not path_text.strip():
        fail(failures, f"{owner} evidenceRefs must contain non-empty repository paths")
        return
    path = Path(path_text)
    if path.is_absolute() or ".." in path.parts:
        fail(failures, f"{owner} evidenceRef must stay inside repo: {path_text}")
        return
    if not (ROOT / path).exists():
        fail(failures, f"{owner} evidenceRef does not exist: {path_text}")


def check_secret_hygiene(failures: list[str]) -> None:
    for path in (CONTRACT_PATH, DOC_PATH):
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        for forbidden in FORBIDDEN_TEXT:
            if forbidden in text:
                fail(failures, f"{path.relative_to(ROOT)} contains forbidden secret-like text: {forbidden}")
        if SECRET_ASSIGNMENT_RE.search(text):
            fail(failures, f"{path.relative_to(ROOT)} appears to contain a non-placeholder secret assignment")


def check_doc(failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"required document missing: {DOC_PATH.relative_to(ROOT)}")
        return
    text = DOC_PATH.read_text(encoding="utf-8")
    for fragment in (
        "metadata/business-dependency-contracts.json",
        "make business-dependency-contract-check",
        "material-service",
        "process-analysis",
        "ADP_E2E_*",
        "PostgreSQL",
        "tenant-service `503`",
        "兼容启动证据",
        "requiredStartupEvidence",
        "make audit-postgres-mappings",
    ):
        if fragment not in text:
            fail(failures, f"business dependency contract document missing required text: {fragment}")


def endpoint_names(item: dict[str, Any], key: str = "endpoints") -> set[str]:
    return {str(endpoint.get("name")) for endpoint in as_list(item.get(key)) if isinstance(endpoint, dict)}


def service_names(item: dict[str, Any]) -> set[str]:
    return {str(service.get("serviceName")) for service in as_list(item.get("services")) if isinstance(service, dict)}


def healthy_count(item: dict[str, Any]) -> int:
    count = 0
    for service in as_list(item.get("services")):
        if isinstance(service, dict):
            value = service.get("healthyHostCount")
            if isinstance(value, int):
                count += value
    return count


def blocked_endpoints(item: dict[str, Any]) -> int:
    count = 0
    for endpoint in as_list(item.get("endpoints")):
        if isinstance(endpoint, dict) and endpoint.get("status") == 503 and endpoint.get("tenantServiceMissing") is True:
            count += 1
    return count


def check_dependency_shape(dependency: dict[str, Any], failures: list[str]) -> None:
    dependency_id = str(dependency.get("id", "")).strip()
    expected = REQUIRED_DEPENDENCIES.get(dependency_id)
    if expected is None:
        fail(failures, f"unknown dependency contract id: {dependency_id!r}")
        return

    for key in (
        "id",
        "title",
        "status",
        "requiredFor",
        "serviceAliases",
        "requiredPackageEvidence",
        "requiredRuntimeEvidence",
        "requiredStartupEvidence",
        "requiredEndpoints",
        "requiredDatabaseEvidence",
        "acceptanceStages",
        "evidenceRefs",
        "recheckCommands",
        "nonSolutions",
    ):
        if key not in dependency:
            fail(failures, f"{dependency_id} missing required key: {key}")

    if dependency.get("status") not in ALLOWED_STATUS:
        fail(failures, f"{dependency_id} has invalid status: {dependency.get('status')!r}")
    if not str(dependency.get("title", "")).strip():
        fail(failures, f"{dependency_id} title must not be empty")

    required_for = {str(value) for value in as_list(dependency.get("requiredFor"))}
    missing_cases = sorted(expected["blocked_cases"] - required_for)
    if missing_cases:
        fail(failures, f"{dependency_id}.requiredFor missing blocked cases: " + ", ".join(missing_cases))

    aliases = {str(value) for value in as_list(dependency.get("serviceAliases"))}
    if not expected["service_aliases"].issubset(aliases):
        fail(failures, f"{dependency_id}.serviceAliases missing expected aliases")

    runtime = as_dict(dependency.get("requiredRuntimeEvidence"))
    for key in expected["required_runtime_keys"]:
        if key not in runtime:
            fail(failures, f"{dependency_id}.requiredRuntimeEvidence missing {key}")
    if runtime.get("nacosGroup") != "prod":
        fail(failures, f"{dependency_id}.requiredRuntimeEvidence.nacosGroup must be prod")
    if int(runtime.get("minimumHealthyInstances") or 0) < 1:
        fail(failures, f"{dependency_id} must require at least one healthy Nacos instance")
    if runtime.get("endpointHttpStatusMustBe2xx") is not True:
        fail(failures, f"{dependency_id} must require dependency endpoints to return HTTP 2xx")
    if runtime.get("endpointTenantServiceMissingMustBeFalse") is not True:
        fail(failures, f"{dependency_id} must require tenant-service missing to be false")

    startup = as_dict(dependency.get("requiredStartupEvidence"))
    for key in (
        "moduleIntakePrecheck",
        "postgresDefaultRuntime",
        "oracleLegacyOnly",
        "nacosConfigRendered",
        "dockerComposeRegistration",
        "runtimePatchManifestUpdated",
        "rollbackOrDisablePlan",
        "startupSmokeCommands",
        "notEnoughEvidence",
    ):
        if key not in startup:
            fail(failures, f"{dependency_id}.requiredStartupEvidence missing {key}")
    for key in (
        "moduleIntakePrecheck",
        "postgresDefaultRuntime",
        "oracleLegacyOnly",
        "nacosConfigRendered",
        "dockerComposeRegistration",
        "runtimePatchManifestUpdated",
        "rollbackOrDisablePlan",
    ):
        if startup.get(key) is not True:
            fail(failures, f"{dependency_id}.requiredStartupEvidence.{key} must be true")
    startup_commands = as_list(startup.get("startupSmokeCommands"))
    for fragment in (
        "make module-intake-check",
        "make render-config",
        "make smoke-business-dependencies",
        "make audit-postgres-mappings",
    ):
        if not any(fragment in str(command) for command in startup_commands):
            fail(failures, f"{dependency_id}.requiredStartupEvidence.startupSmokeCommands missing {fragment}")
    if not as_list(startup.get("notEnoughEvidence")):
        fail(failures, f"{dependency_id}.requiredStartupEvidence.notEnoughEvidence must not be empty")

    package = as_dict(dependency.get("requiredPackageEvidence"))
    if package.get("statusBeforePackage") != "BLOCKED_NO_IMPLEMENTATION_CANDIDATE":
        fail(failures, f"{dependency_id} must document current package-scan blocker status")
    if package.get("candidateStatusAfterPackage") != "CANDIDATE_FOUND":
        fail(failures, f"{dependency_id} must require CANDIDATE_FOUND after package intake")
    if not as_list(package.get("requiredTerms")):
        fail(failures, f"{dependency_id}.requiredPackageEvidence.requiredTerms must not be empty")
    if not as_list(package.get("notEnoughEvidence")):
        fail(failures, f"{dependency_id}.requiredPackageEvidence.notEnoughEvidence must not be empty")

    endpoint_set = endpoint_names(dependency, "requiredEndpoints")
    missing_endpoints = sorted(expected["endpoint_names"] - endpoint_set)
    if missing_endpoints:
        fail(failures, f"{dependency_id} missing required endpoint contracts: " + ", ".join(missing_endpoints))
    for endpoint in as_list(dependency.get("requiredEndpoints")):
        if not isinstance(endpoint, dict):
            fail(failures, f"{dependency_id}.requiredEndpoints entries must be objects")
            continue
        if endpoint.get("method") not in {"GET", "POST"}:
            fail(failures, f"{dependency_id}.{endpoint.get('name')} method must be GET or POST")
        if not str(endpoint.get("path", "")).startswith("/msService/"):
            fail(failures, f"{dependency_id}.{endpoint.get('name')} path must be an /msService route")

    if len(as_list(dependency.get("acceptanceStages"))) < 5:
        fail(failures, f"{dependency_id}.acceptanceStages must include enough handoff steps")
    for required_stage in ("package-candidate-scan", "module-intake-precheck", "nacos-healthy-service", "authenticated-endpoint-probes"):
        if required_stage not in as_list(dependency.get("acceptanceStages")):
            fail(failures, f"{dependency_id}.acceptanceStages missing {required_stage}")
    if not any("postgresql" in str(stage).lower() for stage in as_list(dependency.get("acceptanceStages")) + as_list(dependency.get("requiredDatabaseEvidence"))):
        fail(failures, f"{dependency_id} must include PostgreSQL database evidence requirements")

    for list_key in ("requiredDatabaseEvidence", "evidenceRefs", "recheckCommands", "nonSolutions"):
        if not as_list(dependency.get(list_key)):
            fail(failures, f"{dependency_id}.{list_key} must not be empty")
    for evidence_ref in as_list(dependency.get("evidenceRefs")):
        validate_relative_path(evidence_ref, failures, dependency_id)


def check_current_evidence(
    contracts: dict[str, dict[str, Any]],
    readiness: dict[str, Any],
    package_scan: dict[str, Any],
    blockers: dict[str, Any],
    backlog: dict[str, Any],
    failures: list[str],
) -> None:
    readiness_summary = as_dict(readiness.get("summary"))
    package_summary = as_dict(package_scan.get("summary"))
    if readiness_summary.get("status") != "BLOCKED" or readiness_summary.get("ready") != 0:
        fail(failures, "business dependency contracts expect current readiness to remain BLOCKED with ready=0")
    if package_summary.get("status") != "BLOCKED_NO_IMPLEMENTATION_CANDIDATE" or package_summary.get("candidateDependencies") != 0:
        fail(failures, "business dependency contracts expect current package scan to have no implementation candidates")

    blocker_cases = {
        str(blocker.get("caseId"))
        for blocker in as_list(blockers.get("blockers"))
        if isinstance(blocker, dict) and blocker.get("status") == "BLOCKED"
    }
    backlog_ids = {
        str(item.get("id"))
        for item in as_list(backlog.get("items"))
        if isinstance(item, dict) and item.get("status") in {"BLOCKED", "FAIL_BACKLOG"}
    }

    for dependency_id, expected in REQUIRED_DEPENDENCIES.items():
        contract = contracts.get(dependency_id)
        if not contract:
            continue
        readiness_item = item_by_id(readiness, "items", dependency_id)
        package_item = item_by_id(package_scan, "dependencies", dependency_id)
        if readiness_item is None:
            fail(failures, f"readiness report missing {dependency_id}")
            continue
        if package_item is None:
            fail(failures, f"package scan report missing {dependency_id}")
            continue
        if contract.get("status") != readiness_item.get("status"):
            fail(failures, f"{dependency_id} contract status must match readiness status")
        if package_item.get("status") != "BLOCKED_NO_IMPLEMENTATION_CANDIDATE":
            fail(failures, f"{dependency_id} package scan must remain BLOCKED_NO_IMPLEMENTATION_CANDIDATE until a package arrives")
        if int(package_item.get("implementationCandidateCount") or 0) != 0:
            fail(failures, f"{dependency_id} package scan candidate count must remain 0 in current contract")
        if not expected["service_aliases"].issubset(service_names(readiness_item)):
            fail(failures, f"{dependency_id} readiness smoke missing expected service probes")
        if healthy_count(readiness_item) != 0:
            fail(failures, f"{dependency_id} readiness smoke should still have healthy service count 0")
        if blocked_endpoints(readiness_item) < len(expected["endpoint_names"]):
            fail(failures, f"{dependency_id} readiness smoke should still show all required endpoints as tenant-service 503")
        missing_endpoint_names = sorted(expected["endpoint_names"] - endpoint_names(readiness_item))
        if missing_endpoint_names:
            fail(failures, f"{dependency_id} readiness smoke missing endpoint probes: " + ", ".join(missing_endpoint_names))
        missing_blockers = sorted(expected["blocked_cases"] - blocker_cases)
        if missing_blockers:
            fail(failures, f"{dependency_id} contracts reference blocker cases not present in blocker ledger: " + ", ".join(missing_blockers))
        for case_id in expected["blocked_cases"]:
            if case_id not in backlog_ids:
                fail(failures, f"{dependency_id} blocked case {case_id} must be covered by production backlog")


def check_summary(data: dict[str, Any], dependencies: list[dict[str, Any]], failures: list[str]) -> None:
    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return
    expected = {
        "dependencies": len(dependencies),
        "ready": sum(1 for item in dependencies if item.get("status") == "READY"),
        "blocked": sum(1 for item in dependencies if item.get("status") == "BLOCKED"),
        "productionBlockedCases": len(set().union(*(spec["blocked_cases"] for spec in REQUIRED_DEPENDENCIES.values()))),
        "requiredEndpointProbes": sum(len(spec["endpoint_names"]) for spec in REQUIRED_DEPENDENCIES.values()),
        "requiredContractStages": sum(len(as_list(item.get("acceptanceStages"))) for item in dependencies),
        "requiredStartupEvidenceContracts": sum(1 for item in dependencies if isinstance(item.get("requiredStartupEvidence"), dict)),
        "requiredStartupSmokeCommands": sum(len(as_list(as_dict(item.get("requiredStartupEvidence")).get("startupSmokeCommands"))) for item in dependencies),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def check_contract(data: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "repoCommit",
        "database",
        "module",
        "overallStatus",
        "summary",
        "sourceReports",
        "dependencies",
    ):
        if key not in data:
            fail(failures, f"business dependency contract missing top-level key: {key}")
    if data.get("database") != "PostgreSQL":
        fail(failures, "business dependency contract database must remain PostgreSQL")
    if data.get("module") != "production":
        fail(failures, "business dependency contract module must be production")
    if data.get("overallStatus") != "BLOCKED":
        fail(failures, "business dependency contract must remain BLOCKED until dependencies are ready")
    for required_report in (
        "metadata/business-dependency-readiness-smoke.json",
        "metadata/business-dependency-package-scan.json",
        "metadata/production-module-blockers.json",
        "metadata/production-module-backlog.json",
    ):
        if required_report not in as_list(data.get("sourceReports")):
            fail(failures, f"business dependency contract sourceReports missing {required_report}")

    dependencies_raw = data.get("dependencies")
    if not isinstance(dependencies_raw, list):
        fail(failures, "dependencies must be a list")
        return
    dependencies = [item for item in dependencies_raw if isinstance(item, dict)]
    if len(dependencies) != len(dependencies_raw):
        fail(failures, "dependencies must contain only objects")
    contracts = {str(item.get("id")): item for item in dependencies if item.get("id")}
    missing_dependencies = sorted(set(REQUIRED_DEPENDENCIES) - set(contracts))
    extra_dependencies = sorted(set(contracts) - set(REQUIRED_DEPENDENCIES))
    if missing_dependencies:
        fail(failures, "business dependency contract missing dependencies: " + ", ".join(missing_dependencies))
    if extra_dependencies:
        fail(failures, "business dependency contract contains unknown dependencies: " + ", ".join(extra_dependencies))
    check_summary(data, dependencies, failures)
    for dependency in dependencies:
        check_dependency_shape(dependency, failures)


def main() -> int:
    failures: list[str] = []
    check_secret_hygiene(failures)
    check_doc(failures)
    contract = read_json(CONTRACT_PATH, failures, "business dependency contract")
    readiness = read_json(READINESS_PATH, failures, "business dependency readiness report")
    package_scan = read_json(PACKAGE_SCAN_PATH, failures, "business dependency package scan")
    blockers = read_json(BLOCKERS_PATH, failures, "production module blocker report")
    backlog = read_json(BACKLOG_PATH, failures, "production module backlog")
    if contract:
        check_contract(contract, failures)
    if contract and readiness and package_scan and blockers and backlog:
        contracts = {
            str(item.get("id")): item
            for item in as_list(contract.get("dependencies"))
            if isinstance(item, dict) and item.get("id")
        }
        check_current_evidence(contracts, readiness, package_scan, blockers, backlog, failures)
    if failures:
        print(f"Business dependency contract verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Business dependency contract verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
