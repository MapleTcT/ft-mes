#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPORT_PATH = ROOT / "metadata/business-dependency-readiness-smoke.json"
EXPECTED_BASE_URL = "http://100.99.133.43:18080"
EXPECTED_SSH_HOST = "100.99.133.43"
EXPECTED_SSH_USER = "v6"
EXPECTED_NACOS_CONTAINER = "adp-mes-newbase-nacos-1"
EXPECTED_POSTGRES_CONTAINER = "adp-mes-newbase-postgres-1"
EXPECTED_NACOS_GROUP = "prod"

ALLOWED_STATUSES = {"READY", "ACTION_REQUIRED", "BLOCKED"}

REQUIRED_DEPENDENCIES = {
    "material-service": {
        "service_names": {
            "material",
            "MATERIAL",
            "wms",
            "WMS",
            "warehouse",
            "Warehouse",
            "inventory",
            "Inventory",
        },
        "endpoints": {"checkProdResult", "generateProduceOutSing"},
        "required_database_keys": {"materialLikeTableCount", "materialLikeTables"},
    },
    "process-analysis": {
        "service_names": {
            "ProcessAnalysis",
            "processanalysis",
            "PROCESSANALYSIS",
            "Traceability",
            "traceability",
        },
        "endpoints": {
            "isProdprocessView",
            "processBatchViewOut",
            "analysisiTask",
            "manualStatActive",
            "manualStatProcess",
        },
        "required_database_keys": {
            "processAnalysisTableCount",
            "processAnalysisTables",
            "processAnalysisRuntimeViewCount",
            "processAnalysisMenuCount",
        },
    },
}

DOC_REQUIREMENTS = {
    "docs/runtime-validation-scope.md": [
        "make smoke-business-dependencies",
        "metadata/business-dependency-readiness-smoke.json",
        EXPECTED_SSH_HOST,
    ],
    "docs/production-module-functional-test-cases.md": [
        "make smoke-business-dependencies",
        "material-service",
        "process-analysis",
        EXPECTED_SSH_HOST,
    ],
    "docs/backend-table-audit/material-service-dependency-analysis.md": [
        "make smoke-business-dependencies",
        "material-service",
        EXPECTED_SSH_HOST,
    ],
    "docs/backend-table-audit/processanalysis-dependency-analysis.md": [
        "make smoke-business-dependencies",
        "process-analysis",
        EXPECTED_SSH_HOST,
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


def read_json(report_path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with report_path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing business dependency readiness report: {report_path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {report_path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, "business dependency readiness report must be a JSON object")
        return {}
    return data


def check_secret_hygiene(report_path: Path, failures: list[str]) -> None:
    text = report_path.read_text(encoding="utf-8")
    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in text:
            fail(failures, f"business dependency readiness report contains forbidden secret literal: {literal}")
    if SECRET_ASSIGNMENT_RE.search(text):
        fail(failures, "business dependency readiness report appears to contain a non-placeholder secret assignment")


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


def check_service(dependency_id: str, index: int, service: Any, failures: list[str]) -> str | None:
    if not isinstance(service, dict):
        fail(failures, f"{dependency_id}.services[{index}] must be an object")
        return None

    name = service.get("serviceName")
    if not isinstance(name, str) or not name.strip():
        fail(failures, f"{dependency_id}.services[{index}] missing serviceName")
        return None

    for key in ("fetched", "hostCount", "healthyHostCount", "ports", "instanceIds"):
        if key not in service:
            fail(failures, f"{dependency_id}.services[{index}] missing {key}")

    if not isinstance(service.get("fetched"), bool):
        fail(failures, f"{dependency_id}.services[{index}].fetched must be boolean")
    for key in ("hostCount", "healthyHostCount"):
        if not isinstance(service.get(key), int) or service.get(key, -1) < 0:
            fail(failures, f"{dependency_id}.services[{index}].{key} must be a non-negative integer")
    for key in ("ports", "instanceIds"):
        if not isinstance(service.get(key), list):
            fail(failures, f"{dependency_id}.services[{index}].{key} must be a list")

    return name


def check_endpoint(dependency_id: str, index: int, endpoint: Any, failures: list[str]) -> str | None:
    if not isinstance(endpoint, dict):
        fail(failures, f"{dependency_id}.endpoints[{index}] must be an object")
        return None

    name = endpoint.get("name")
    if not isinstance(name, str) or not name.strip():
        fail(failures, f"{dependency_id}.endpoints[{index}] missing endpoint name")
        return None

    method = endpoint.get("method")
    if method not in {"GET", "POST", "PUT", "DELETE", "PATCH"}:
        fail(failures, f"{dependency_id}.{name} has invalid method: {method!r}")

    path = endpoint.get("path")
    if not isinstance(path, str) or not path.startswith("/msService/"):
        fail(failures, f"{dependency_id}.{name} path must be an /msService route")

    status = endpoint.get("status")
    if not isinstance(status, int) or status < 0 or status > 599 or (0 < status < 100):
        fail(failures, f"{dependency_id}.{name} status must be 0 for probe failure or an HTTP status integer")
    if status == 0 and not str(endpoint.get("error", "")).strip():
        fail(failures, f"{dependency_id}.{name} status=0 must include probe error")

    if "tenantServiceMissing" in endpoint and not isinstance(endpoint.get("tenantServiceMissing"), bool):
        fail(failures, f"{dependency_id}.{name}.tenantServiceMissing must be boolean when present")
    if "timeout" in endpoint and not isinstance(endpoint.get("timeout"), bool):
        fail(failures, f"{dependency_id}.{name}.timeout must be boolean when present")

    return name


def has_healthy_service(item: dict[str, Any], expected: dict[str, Any]) -> bool:
    names = expected["service_names"]
    for service in as_list(item.get("services")):
        if not isinstance(service, dict):
            continue
        if service.get("serviceName") in names and int(service.get("healthyHostCount") or 0) > 0:
            return True
    return False


def endpoints_are_successful(item: dict[str, Any]) -> bool:
    endpoints = as_list(item.get("endpoints"))
    if not endpoints:
        return False
    return all(
        isinstance(endpoint, dict)
        and isinstance(endpoint.get("status"), int)
        and 200 <= endpoint.get("status") < 300
        and endpoint.get("tenantServiceMissing") is not True
        for endpoint in endpoints
    )


def check_status_semantics(item: dict[str, Any], expected: dict[str, Any], failures: list[str]) -> None:
    dependency_id = str(item.get("id"))
    status = item.get("status")
    database = as_dict(item.get("database"))

    if status == "READY":
        if not has_healthy_service(item, expected):
            fail(failures, f"{dependency_id} READY must have at least one healthy expected Nacos service")
        if not endpoints_are_successful(item):
            fail(failures, f"{dependency_id} READY endpoints must return HTTP 2xx and not be tenant-service missing")
        if dependency_id == "material-service":
            if int(database.get("materialLikeTableCount") or 0) <= 0:
                fail(failures, "material-service READY must have material/stock database evidence")
        if dependency_id == "process-analysis":
            if int(database.get("processAnalysisTableCount") or 0) <= 0:
                fail(failures, "process-analysis READY must have PostgreSQL process analysis tables")
            if int(database.get("processAnalysisRuntimeViewCount") or 0) <= 0:
                fail(failures, "process-analysis READY must have runtime_view metadata")
            if int(database.get("processAnalysisMenuCount") or 0) <= 0:
                fail(failures, "process-analysis READY must have menu metadata")

    if status == "BLOCKED":
        if not as_list(item.get("issues")):
            fail(failures, f"{dependency_id} BLOCKED must include issues")
        if not str(item.get("nextAction", "")).strip():
            fail(failures, f"{dependency_id} BLOCKED must include nextAction")
        service_missing = not has_healthy_service(item, expected)
        endpoint_missing = any(
            isinstance(endpoint, dict) and endpoint.get("tenantServiceMissing") is True
            for endpoint in as_list(item.get("endpoints"))
        )
        endpoint_probe_failed = any(
            isinstance(endpoint, dict) and (endpoint.get("status") == 0 or bool(endpoint.get("error")))
            for endpoint in as_list(item.get("endpoints"))
        )
        if not service_missing and not endpoint_missing and not endpoint_probe_failed:
            fail(failures, f"{dependency_id} BLOCKED must include service, endpoint, or probe-failure blocker evidence")


def check_dependency(index: int, item: Any, failures: list[str]) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"items[{index}] must be an object")
        return None

    dependency_id = item.get("id")
    if dependency_id not in REQUIRED_DEPENDENCIES:
        fail(failures, f"unknown business dependency id: {dependency_id!r}")
        return None
    expected = REQUIRED_DEPENDENCIES[dependency_id]

    for key in ("id", "title", "status", "requiredFor", "readyWhen", "services", "endpoints", "database", "issues", "nextAction"):
        if key not in item:
            fail(failures, f"{dependency_id} missing required key: {key}")

    if item.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"{dependency_id} has invalid status: {item.get('status')!r}")

    if not str(item.get("title", "")).strip():
        fail(failures, f"{dependency_id} must include title")
    if not str(item.get("readyWhen", "")).strip():
        fail(failures, f"{dependency_id} must include readyWhen")
    if not as_list(item.get("requiredFor")):
        fail(failures, f"{dependency_id} must include requiredFor")

    services = as_list(item.get("services"))
    service_names = {
        name
        for service_index, service in enumerate(services)
        for name in [check_service(dependency_id, service_index, service, failures)]
        if name
    }
    missing_services = sorted(expected["service_names"] - service_names)
    if missing_services:
        fail(failures, f"{dependency_id} missing expected service probes: {', '.join(missing_services)}")

    endpoints = as_list(item.get("endpoints"))
    endpoint_names = {
        name
        for endpoint_index, endpoint in enumerate(endpoints)
        for name in [check_endpoint(dependency_id, endpoint_index, endpoint, failures)]
        if name
    }
    missing_endpoints = sorted(expected["endpoints"] - endpoint_names)
    if missing_endpoints:
        fail(failures, f"{dependency_id} missing expected endpoint probes: {', '.join(missing_endpoints)}")

    database = item.get("database")
    if not isinstance(database, dict):
        fail(failures, f"{dependency_id}.database must be an object")
        database = {}
    missing_database_keys = sorted(expected["required_database_keys"] - set(database))
    if missing_database_keys:
        fail(failures, f"{dependency_id}.database missing keys: {', '.join(missing_database_keys)}")

    for key, value in database.items():
        if key.endswith("Count") and (not isinstance(value, int) or value < 0):
            fail(failures, f"{dependency_id}.database.{key} must be a non-negative integer")
        if key.endswith("Tables") and not isinstance(value, list):
            fail(failures, f"{dependency_id}.database.{key} must be a list")

    check_status_semantics(item, expected, failures)
    return dependency_id


def check_summary(data: dict[str, Any], failures: list[str]) -> None:
    items = [item for item in as_list(data.get("items")) if isinstance(item, dict)]
    statuses = [item.get("status") for item in items]
    expected = {
        "dependencies": len(items),
        "ready": statuses.count("READY"),
        "actionRequired": statuses.count("ACTION_REQUIRED"),
        "blocked": statuses.count("BLOCKED"),
    }
    summary = as_dict(data.get("summary"))
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")

    computed_status = "BLOCKED" if expected["blocked"] else "ACTION_REQUIRED" if expected["actionRequired"] else "READY"
    if summary.get("status") != computed_status:
        fail(failures, f"summary.status={summary.get('status')!r} does not match expected {computed_status}")


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in ("schemaVersion", "generatedAt", "repoCommit", "database", "target", "summary", "items", "evidence"):
        if key not in data:
            fail(failures, f"report missing required top-level key: {key}")

    if data.get("database") != "PostgreSQL":
        fail(failures, "business dependency readiness report database must remain PostgreSQL")

    repo_commit = data.get("repoCommit")
    if not isinstance(repo_commit, str) or len(repo_commit.strip()) < 7:
        fail(failures, "repoCommit must identify the code baseline used for readiness tracking")

    target = as_dict(data.get("target"))
    for key in ("baseUrl", "sshHost", "sshUser", "nacosContainer", "postgresContainer", "nacosGroup"):
        if not str(target.get(key, "")).strip():
            fail(failures, f"target.{key} must be present")
    expected_target = {
        "baseUrl": EXPECTED_BASE_URL,
        "sshHost": EXPECTED_SSH_HOST,
        "sshUser": EXPECTED_SSH_USER,
        "nacosContainer": EXPECTED_NACOS_CONTAINER,
        "postgresContainer": EXPECTED_POSTGRES_CONTAINER,
        "nacosGroup": EXPECTED_NACOS_GROUP,
    }
    for key, expected_value in expected_target.items():
        if target.get(key) != expected_value:
            fail(failures, f"target.{key} must be {expected_value}")

    summary = as_dict(data.get("summary"))
    if summary.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"summary.status has invalid value: {summary.get('status')!r}")

    items = data.get("items")
    if not isinstance(items, list):
        fail(failures, "items must be a list")
        return

    seen_ids: set[str] = set()
    for index, item in enumerate(items):
        dependency_id = check_dependency(index, item, failures)
        if not dependency_id:
            continue
        if dependency_id in seen_ids:
            fail(failures, f"duplicate dependency id: {dependency_id}")
        seen_ids.add(dependency_id)

    missing = sorted(set(REQUIRED_DEPENDENCIES) - seen_ids)
    if missing:
        fail(failures, "business dependency report missing required items: " + ", ".join(missing))

    check_summary(data, failures)

    evidence = as_dict(data.get("evidence"))
    if "Read-only" not in str(evidence.get("method", "")):
        fail(failures, "evidence.method must state that the smoke is read-only")
    if "not written" not in str(evidence.get("secretHandling", "")):
        fail(failures, "evidence.secretHandling must state that credentials are not written")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate ADP business dependency readiness smoke assets.")
    parser.add_argument(
        "--report",
        default=str(DEFAULT_REPORT_PATH.relative_to(ROOT)),
        help="Path to metadata/business-dependency-readiness-smoke.json",
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
        print(f"Business dependency readiness verification failed with {len(failures)} issue(s).", file=sys.stderr)
        return 1

    print("Business dependency readiness verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
