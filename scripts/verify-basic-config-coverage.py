#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
COVERAGE_PATH = ROOT / "metadata/basic-config-coverage.json"
DOC_PATH = ROOT / "docs/basic-config-coverage.md"
PERSISTENCE_PATH = ROOT / "metadata/persistence-acceptance.json"
SYSCONFIG_PATH = ROOT / "metadata/systemconfig-persistence-acceptance.json"
SYSCONFIG_BUILTINS_PATH = ROOT / "metadata/systemconfig-builtins-readiness-smoke.json"
RUNTIME_CONFIG_PATH = ROOT / "metadata/runtime-configuration-readiness-smoke.json"
CUSTOM_PROPERTY_PATH = ROOT / "metadata/custom-property-persistence-acceptance.json"
ENTITY_MODEL_PERSISTENCE_PATH = ROOT / "metadata/entity-model-config-crud-persistence-acceptance.json"
NACOS_CONFIG_PATH = ROOT / "metadata/nacos-config-drift-smoke.json"
KEYCLOAK_JWT_PATH = ROOT / "metadata/keycloak-jwt-runtime-smoke.json"

ALLOWED_STATUSES = {"PASS", "PARTIAL", "BLOCKED", "NOT_RUN", "NOT_APPLICABLE"}
REQUIRED_AREA_IDS = {
    "systemcode-dictionary",
    "systemconfig-app-catalog-value",
    "systemconfig-built-in-catalogs",
    "configuration-entity-runtime",
    "configuration-physical-model-table",
    "nacos-keycloak-production-config",
}
SUMMARY_KEYS_BY_STATUS = {
    "PASS": "pass",
    "PARTIAL": "partial",
    "BLOCKED": "blocked",
    "NOT_RUN": "notRun",
    "NOT_APPLICABLE": "notApplicable",
}
FORBIDDEN_TEXT = {
    "ft" + "123456789",
    "Authorization: Bearer",
    "access_token",
    "refresh_token",
}


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


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
        fail(failures, f"{owner} must contain non-empty evidenceRefs")
        return
    path = Path(path_text)
    if path.is_absolute() or ".." in path.parts:
        fail(failures, f"{owner} evidenceRef must stay inside the repository: {path_text}")
        return
    if not (ROOT / path).exists():
        fail(failures, f"{owner} evidenceRef does not exist: {path_text}")


def check_secret_hygiene(failures: list[str]) -> None:
    for path in (COVERAGE_PATH, DOC_PATH):
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        for forbidden in FORBIDDEN_TEXT:
            if forbidden in text:
                fail(failures, f"{path.relative_to(ROOT)} contains forbidden secret-like text: {forbidden}")


def check_doc(failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"required document missing: {DOC_PATH.relative_to(ROOT)}")
        return
    text = DOC_PATH.read_text(encoding="utf-8")
    required_fragments = [
        "G-012",
        "metadata/basic-config-coverage.json",
        "make basic-config-coverage-check",
        "systemconfig-persistence-acceptance.json",
        "systemconfig-builtins-readiness-smoke.json",
        "runtime-configuration-readiness-smoke.json",
        "entity-model-config-crud-persistence-acceptance.json",
        "configuration-physical-model-table",
        "nacos-config-drift-smoke.json",
        "keycloak-jwt-runtime-smoke.json",
        "PARTIAL",
        "PostgreSQL",
    ]
    for fragment in required_fragments:
        if fragment not in text:
            fail(failures, f"basic config coverage document missing required text: {fragment}")


def check_summary(data: dict[str, Any], areas: list[dict[str, Any]], failures: list[str]) -> None:
    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return
    expected: dict[str, int] = {
        "totalAreas": len(areas),
        "pass": 0,
        "partial": 0,
        "blocked": 0,
        "notRun": 0,
        "notApplicable": 0,
    }
    for area in areas:
        status = area.get("status")
        if status in SUMMARY_KEYS_BY_STATUS:
            expected[SUMMARY_KEYS_BY_STATUS[status]] += 1
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def check_area(area: dict[str, Any], failures: list[str]) -> None:
    area_id = str(area.get("id", "")).strip()
    status = area.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{area_id or '<missing id>'} has invalid status: {status!r}")
    for key in ("id", "title", "route"):
        if not str(area.get(key, "")).strip():
            fail(failures, f"{area_id or '<missing id>'} must include non-empty {key}")
    if not isinstance(area.get("requiresPersistence"), bool):
        fail(failures, f"{area_id} requiresPersistence must be boolean")
    if not as_list(area.get("evidenceRefs")):
        fail(failures, f"{area_id} must include evidenceRefs")
    for evidence_ref in as_list(area.get("evidenceRefs")):
        validate_relative_path(evidence_ref, failures, area_id)
    if status == "PASS":
        if area.get("requiresPersistence") and not as_list(area.get("tables")):
            fail(failures, f"{area_id} PASS persistence area must include target tables")
        if not as_list(area.get("acceptedOperations")):
            fail(failures, f"{area_id} PASS must include acceptedOperations")
        if as_list(area.get("remainingGaps")):
            fail(failures, f"{area_id} PASS must not include remainingGaps")
    elif status != "NOT_APPLICABLE" and not as_list(area.get("remainingGaps")):
        fail(failures, f"{area_id} {status} must include remainingGaps")


def check_systemcode_acceptance(area: dict[str, Any], failures: list[str]) -> None:
    if area.get("status") != "PASS":
        return
    persistence = read_json(PERSISTENCE_PATH, failures, "persistence acceptance report")
    items = as_list(persistence.get("items"))
    systemcode_items = [
        item
        for item in items
        if isinstance(item, dict)
        and item.get("module") == "基础配置-系统编码"
        and item.get("status") == "PASS"
    ]
    if len(systemcode_items) < 6:
        fail(failures, "systemcode-dictionary PASS requires at least 6 PASS persistence items")

    operation_text = "\n".join(str(item.get("operation", "")) for item in systemcode_items)
    required_operations = [
        "新增系统编码字典项",
        "编辑系统编码字典项",
        "新增系统编码值",
        "编辑系统编码值",
        "删除系统编码值",
        "删除系统编码字典项",
    ]
    for operation in required_operations:
        if operation not in operation_text:
            fail(failures, f"systemcode-dictionary missing persistence operation: {operation}")

    tables = {table for item in systemcode_items for table in as_list(item.get("tables"))}
    for table in ("sys_entity", "sys_code"):
        if table not in tables:
            fail(failures, f"systemcode-dictionary PASS requires table evidence: {table}")

    refs = set(as_list(area.get("evidenceRefs")))
    for required_ref in (
        "metadata/persistence-acceptance.json",
        "docs/backend-table-audit/persistence-acceptance.md",
    ):
        if required_ref not in refs:
            fail(failures, f"systemcode-dictionary evidenceRefs must include {required_ref}")


def check_systemconfig_acceptance(area: dict[str, Any], failures: list[str]) -> None:
    if area.get("status") != "PASS":
        return
    report = read_json(SYSCONFIG_PATH, failures, "system config persistence report")
    if report.get("status") != "PASS":
        fail(failures, "systemconfig-app-catalog-value PASS requires systemconfig report status PASS")
    if report.get("route") != "/systemconfig/#/sysconfig":
        fail(failures, "systemconfig-app-catalog-value PASS requires /systemconfig/#/sysconfig route evidence")

    browser = report.get("browser")
    if not isinstance(browser, dict):
        fail(failures, "systemconfig report browser evidence must be an object")
    else:
        if browser.get("navigationStatus") != 200:
            fail(failures, "systemconfig browser navigationStatus must be 200")
        for key in ("consoleMessages", "pageErrors", "requestFailures"):
            if as_list(browser.get(key)):
                fail(failures, f"systemconfig browser {key} must be empty")
        if browser.get("visibleError"):
            fail(failures, "systemconfig browser visibleError must be empty")

    backend_trace = report.get("backendTrace")
    if not isinstance(backend_trace, dict):
        fail(failures, "systemconfig report backendTrace must be an object")
    else:
        for trace_key in ("create", "update", "read", "delete"):
            if trace_key not in backend_trace:
                fail(failures, f"systemconfig backendTrace missing {trace_key}")

    operations = report.get("operations")
    if not isinstance(operations, dict):
        fail(failures, "systemconfig report operations must be an object")
    else:
        for operation_key in ("create", "update", "readByModule", "delete"):
            operation = operations.get(operation_key)
            if not isinstance(operation, dict):
                fail(failures, f"systemconfig operations missing {operation_key}")
                continue
            if operation.get("responseStatus") != 200:
                fail(failures, f"systemconfig operation {operation_key} responseStatus must be 200")
            if operation_key in {"create", "update", "delete"} and not operation.get("verificationSql"):
                fail(failures, f"systemconfig operation {operation_key} must include verificationSql")

    refs = set(as_list(area.get("evidenceRefs")))
    for required_ref in (
        "metadata/systemconfig-persistence-acceptance.json",
        "metadata/persistence-acceptance.json",
    ):
        if required_ref not in refs:
            fail(failures, f"systemconfig-app-catalog-value evidenceRefs must include {required_ref}")


def check_systemconfig_builtins(area: dict[str, Any], failures: list[str]) -> None:
    refs = set(as_list(area.get("evidenceRefs")))
    required_ref = "metadata/systemconfig-builtins-readiness-smoke.json"
    if required_ref not in refs:
        fail(failures, f"systemconfig-built-in-catalogs evidenceRefs must include {required_ref}")

    report = read_json(SYSCONFIG_BUILTINS_PATH, failures, "systemconfig built-in catalog smoke report")
    if not report:
        return
    if report.get("database") != "PostgreSQL":
        fail(failures, "systemconfig built-in catalog smoke database must remain PostgreSQL")
    if report.get("module") != "basic-config":
        fail(failures, "systemconfig built-in catalog smoke module must be basic-config")
    if report.get("areaId") != "systemconfig-built-in-catalogs":
        fail(failures, "systemconfig built-in catalog smoke areaId must be systemconfig-built-in-catalogs")
    if report.get("status") != "PASS":
        fail(failures, "systemconfig built-in catalog smoke status must be PASS")
    if report.get("route") != "/systemconfig/#/sysconfig":
        fail(failures, "systemconfig built-in catalog smoke route must be /systemconfig/#/sysconfig")

    summary = report.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "systemconfig built-in catalog smoke summary must be an object")
    else:
        if summary.get("status") != "PASS":
            fail(failures, "systemconfig built-in catalog smoke summary.status must be PASS")
        if int(summary.get("expectedCatalogs") or 0) < 6:
            fail(failures, "systemconfig built-in catalog smoke must cover at least 6 expected catalogs")
        if summary.get("missingCatalogs") != 0:
            fail(failures, "systemconfig built-in catalog smoke missingCatalogs must be 0")
        if summary.get("detailFailures") != 0:
            fail(failures, "systemconfig built-in catalog smoke detailFailures must be 0")
        if summary.get("dbMissing") != 0:
            fail(failures, "systemconfig built-in catalog smoke dbMissing must be 0")
        if int(summary.get("sensitiveCatalogs") or 0) < 4:
            fail(failures, "systemconfig built-in catalog smoke must classify at least 4 sensitive built-in catalogs")
        if summary.get("mutationAllowedInSmoke") != 0:
            fail(failures, "systemconfig built-in catalog smoke must not allow mutations in read-only smoke")
        if int(summary.get("configItems") or 0) < 1:
            fail(failures, "systemconfig built-in catalog smoke must summarize at least one config item")
        if summary.get("browserErrors") != 0:
            fail(failures, "systemconfig built-in catalog smoke browserErrors must be 0")

    checks = as_list(report.get("checks"))
    required_checks = {
        "browser-route-systemconfig",
        "catalog-list-api",
        "expected-built-in-catalogs-present",
        "catalog-detail-read",
        "catalog-postgres-rows",
        "built-in-edit-policy-boundary",
        "browser-error-free",
    }
    seen_checks = {str(check.get("name")) for check in checks if isinstance(check, dict)}
    missing_checks = sorted(required_checks - seen_checks)
    if missing_checks:
        fail(failures, "systemconfig built-in catalog smoke missing checks: " + ", ".join(missing_checks))
    for check in checks:
        if isinstance(check, dict) and check.get("status") != "PASS":
            fail(failures, f"systemconfig built-in catalog smoke check must PASS: {check.get('name')}")

    expected_catalogs = as_list(report.get("expectedCatalogs"))
    if not expected_catalogs:
        fail(failures, "systemconfig built-in catalog smoke must include expectedCatalogs")
    for catalog in expected_catalogs:
        if not isinstance(catalog, dict):
            fail(failures, "systemconfig built-in catalog expectedCatalogs entries must be objects")
            continue
        if catalog.get("observed") is not True:
            fail(failures, f"systemconfig built-in catalog missing expected catalog: {catalog.get('name')}")
        if not str(catalog.get("catalogId", "")).strip():
            fail(failures, f"systemconfig built-in catalog missing catalogId for {catalog.get('name')}")
        for policy_key in ("classification", "editPolicy", "risk"):
            if not str(catalog.get(policy_key, "")).strip():
                fail(failures, f"systemconfig built-in catalog missing {policy_key} for {catalog.get('name')}")
        if catalog.get("mutationAllowedInSmoke") is not False:
            fail(failures, f"systemconfig built-in catalog mutationAllowedInSmoke must be false for {catalog.get('name')}")

    details = as_list(report.get("details"))
    if len(details) < len(expected_catalogs):
        fail(failures, "systemconfig built-in catalog smoke must read detail for every expected catalog")
    for detail in details:
        if isinstance(detail, dict) and detail.get("ok") is not True:
            fail(failures, f"systemconfig built-in catalog detail read failed: {detail.get('name')}")
        if not isinstance(detail, dict):
            continue
        for policy_key in ("classification", "editPolicy"):
            if not str(detail.get(policy_key, "")).strip():
                fail(failures, f"systemconfig built-in catalog detail missing {policy_key}: {detail.get('name')}")
        if detail.get("mutationAllowedInSmoke") is not False:
            fail(failures, f"systemconfig built-in catalog detail must keep mutationAllowedInSmoke false: {detail.get('name')}")
        config_summaries = as_list(detail.get("configSummaries"))
        if detail.get("configCount") not in (None, "") and int(detail.get("configCount") or 0) != len(config_summaries):
            fail(failures, f"systemconfig built-in catalog configSummaries count mismatch: {detail.get('name')}")
        for config in config_summaries:
            if not isinstance(config, dict):
                fail(failures, f"systemconfig built-in catalog config summary must be object: {detail.get('name')}")
                continue
            if "valueSummary" not in config:
                fail(failures, f"systemconfig built-in catalog config summary must redact values: {detail.get('name')}")

    database_evidence = report.get("databaseEvidence")
    if not isinstance(database_evidence, dict):
        fail(failures, "systemconfig built-in catalog smoke databaseEvidence must be an object")
    else:
        catalog_rows = as_list(database_evidence.get("catalogRows"))
        if len(catalog_rows) < len(expected_catalogs):
            fail(failures, "systemconfig built-in catalog smoke must include PostgreSQL catalog rows for expected catalogs")
        if not str(database_evidence.get("catalogSql", "")).strip():
            fail(failures, "systemconfig built-in catalog smoke must include catalogSql")
        if not str(database_evidence.get("configSql", "")).strip():
            fail(failures, "systemconfig built-in catalog smoke must include configSql")

    evidence = report.get("evidence")
    if not isinstance(evidence, dict):
        fail(failures, "systemconfig built-in catalog smoke evidence must be an object")
    else:
        if "no edit/save" not in str(evidence.get("editPolicy", "")):
            fail(failures, "systemconfig built-in catalog smoke evidence must state no edit/save action")
        if "hash" not in str(evidence.get("valueRedaction", "")).lower():
            fail(failures, "systemconfig built-in catalog smoke evidence must describe value hash redaction")


def check_nacos_keycloak_runtime(area: dict[str, Any], failures: list[str]) -> None:
    refs = set(as_list(area.get("evidenceRefs")))
    for required_ref in (
        "metadata/nacos-config-drift-smoke.json",
        "metadata/keycloak-jwt-runtime-smoke.json",
    ):
        if required_ref not in refs:
            fail(failures, f"nacos-keycloak-production-config evidenceRefs must include {required_ref}")

    if area.get("status") == "NOT_RUN":
        fail(failures, "nacos-keycloak-production-config cannot stay NOT_RUN when runtime smoke reports are present")
    if area.get("status") == "PASS":
        fail(failures, "nacos-keycloak-production-config must stay non-PASS until production export/diff and Keycloak migration rehearsal exist")

    nacos = read_json(NACOS_CONFIG_PATH, failures, "Nacos config drift smoke report")
    if nacos:
        if nacos.get("database") != "PostgreSQL":
            fail(failures, "Nacos config drift smoke database must remain PostgreSQL")
        target = nacos.get("target")
        if not isinstance(target, dict) or target.get("sshHost") != "100.99.133.43":
            fail(failures, "Nacos config drift smoke target.sshHost must be 100.99.133.43")
        summary = nacos.get("summary")
        if not isinstance(summary, dict):
            fail(failures, "Nacos config drift smoke summary must be an object")
        else:
            if summary.get("status") != "PASS":
                fail(failures, "Nacos config drift smoke summary.status must be PASS")
            if summary.get("missingRemote") != 0:
                fail(failures, "Nacos config drift smoke missingRemote must be 0")
            if summary.get("missingLocal") != 0:
                fail(failures, "Nacos config drift smoke missingLocal must be 0")
            if summary.get("oracleResidueFiles") != 0:
                fail(failures, "Nacos config drift smoke oracleResidueFiles must be 0")
            if summary.get("criticalFail") != 0:
                fail(failures, "Nacos config drift smoke criticalFail must be 0")
            if int(summary.get("criticalPass") or 0) < 10:
                fail(failures, "Nacos config drift smoke must pass at least 10 critical checks")
            expected_services = int(summary.get("expectedServices") or 0)
            healthy_services = int(summary.get("healthyServices") or 0)
            if expected_services < 10 or healthy_services < expected_services:
                fail(failures, "Nacos config drift smoke must have all expected critical services healthy")
        critical_checks = as_list(nacos.get("criticalChecks"))
        required_nacos_checks = {
            "datasource-db-type-postgresql",
            "datasource-host-postgres",
            "gateway-keycloak-client-present",
            "gateway-jwt-secret-present",
            "jwt-secret-present",
            "registry-group-prod",
            "discovery-group-prod",
        }
        seen_nacos_checks = {str(check.get("name")) for check in critical_checks if isinstance(check, dict)}
        missing_nacos_checks = sorted(required_nacos_checks - seen_nacos_checks)
        if missing_nacos_checks:
            fail(failures, "Nacos config drift smoke missing critical checks: " + ", ".join(missing_nacos_checks))
        for check in critical_checks:
            if isinstance(check, dict) and check.get("status") != "PASS":
                fail(failures, f"Nacos config drift smoke critical check must PASS: {check.get('name')}")

    keycloak = read_json(KEYCLOAK_JWT_PATH, failures, "Keycloak/JWT runtime smoke report")
    if keycloak:
        if keycloak.get("database") != "PostgreSQL":
            fail(failures, "Keycloak/JWT runtime smoke database must remain PostgreSQL")
        target = keycloak.get("target")
        if not isinstance(target, dict) or target.get("sshHost") != "100.99.133.43":
            fail(failures, "Keycloak/JWT runtime smoke target.sshHost must be 100.99.133.43")
        if isinstance(target, dict) and target.get("baseUrl") != "http://100.99.133.43:18080":
            fail(failures, "Keycloak/JWT runtime smoke target.baseUrl must be http://100.99.133.43:18080")
        summary = keycloak.get("summary")
        if not isinstance(summary, dict):
            fail(failures, "Keycloak/JWT runtime smoke summary must be an object")
        else:
            if summary.get("status") != "PASS":
                fail(failures, "Keycloak/JWT runtime smoke summary.status must be PASS")
            if summary.get("fail") != 0:
                fail(failures, "Keycloak/JWT runtime smoke fail count must be 0")
            if summary.get("checks") != summary.get("pass"):
                fail(failures, "Keycloak/JWT runtime smoke checks must equal pass")
            if int(summary.get("expectedClients") or 0) < 2:
                fail(failures, "Keycloak/JWT runtime smoke must cover at least 2 expected clients")
            if int(summary.get("nacosHealthyKeycloakHosts") or 0) < 1:
                fail(failures, "Keycloak/JWT runtime smoke must find healthy Keycloak hosts in Nacos")
            if int(summary.get("gatewayMenuTargets") or 0) <= 0:
                fail(failures, "Keycloak/JWT runtime smoke must prove gateway menu targets are available")
            if not str(summary.get("keycloakPublicKeySha256Prefix", "")).strip():
                fail(failures, "Keycloak/JWT runtime smoke must include keycloak public key hash prefix")
            if summary.get("keycloakPublicKeySha256Prefix") != summary.get("nacosJwtSha256Prefix"):
                fail(failures, "Keycloak/JWT runtime smoke public key hash must match Nacos JWT hash")
        checks = as_list(keycloak.get("checks"))
        required_keycloak_checks = {
            "realm-public-key-present",
            "nacos-jwt-public-key-synced",
            "nacos-keycloak-healthy-instance",
            "client-pc_dt-direct-grant",
            "client-mobile_dt-direct-grant",
            "gateway-login-admin",
            "gateway-menu-current-user",
        }
        seen_keycloak_checks = {str(check.get("name")) for check in checks if isinstance(check, dict)}
        missing_keycloak_checks = sorted(required_keycloak_checks - seen_keycloak_checks)
        if missing_keycloak_checks:
            fail(failures, "Keycloak/JWT runtime smoke missing checks: " + ", ".join(missing_keycloak_checks))
        for check in checks:
            if isinstance(check, dict) and check.get("status") != "PASS":
                fail(failures, f"Keycloak/JWT runtime smoke check must PASS: {check.get('name')}")


def check_runtime_configuration_readiness(area: dict[str, Any], failures: list[str]) -> None:
    refs = set(as_list(area.get("evidenceRefs")))
    required_ref = "metadata/runtime-configuration-readiness-smoke.json"
    if required_ref not in refs:
        fail(failures, f"configuration-entity-runtime evidenceRefs must include {required_ref}")
    for required_custom_ref in (
        "metadata/custom-property-persistence-acceptance.json",
        "deploy/docker/scripts/adp-custom-property-persistence-acceptance.js",
        "deploy/docker/postgres/init/169-custom-property-project-property-compat.sql",
    ):
        if required_custom_ref not in refs:
            fail(failures, f"configuration-entity-runtime evidenceRefs must include {required_custom_ref}")
    for required_entity_ref in (
        "metadata/entity-model-config-crud-persistence-acceptance.json",
        "deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js",
        "metadata/entity-model-config-crud-readiness-probe.json",
    ):
        if required_entity_ref not in refs:
            fail(failures, f"configuration-entity-runtime evidenceRefs must include {required_entity_ref}")

    report = read_json(RUNTIME_CONFIG_PATH, failures, "runtime configuration readiness smoke report")
    if not report:
        return
    if report.get("database") != "PostgreSQL":
        fail(failures, "runtime configuration readiness smoke database must remain PostgreSQL")
    if report.get("module") != "basic-config":
        fail(failures, "runtime configuration readiness smoke module must be basic-config")
    if report.get("areaId") != "configuration-entity-runtime":
        fail(failures, "runtime configuration readiness smoke areaId must be configuration-entity-runtime")
    if report.get("status") != "PASS":
        fail(failures, "runtime configuration readiness smoke status must be PASS")
    if report.get("baseUrl") != "http://100.99.133.43:18080":
        fail(failures, "runtime configuration readiness smoke baseUrl must be http://100.99.133.43:18080")
    if report.get("browserBaseUrl") != "http://100.99.133.43:18080":
        fail(failures, "runtime configuration readiness smoke browserBaseUrl must be http://100.99.133.43:18080")

    summary = report.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "runtime configuration readiness smoke summary must be an object")
    else:
        if summary.get("status") != "PASS":
            fail(failures, "runtime configuration readiness smoke summary.status must be PASS")
        if summary.get("apiPassed") != summary.get("apiChecks") or int(summary.get("apiChecks") or 0) < 5:
            fail(failures, "runtime configuration readiness smoke must pass at least 5 API checks")
        if summary.get("browserPassed") != summary.get("browserPages") or int(summary.get("browserPages") or 0) < 4:
            fail(failures, "runtime configuration readiness smoke must pass at least 4 browser pages")
        if summary.get("browserErrors") != 0:
            fail(failures, "runtime configuration readiness smoke browserErrors must be 0")
        for key in (
            "ecEntityRows",
            "ecModelRows",
            "ecFieldRows",
            "ecViewRows",
            "runtimeViewRows",
            "runtimeExtraViewRows",
            "runtimeButtonRows",
        ):
            if int(summary.get(key) or 0) <= 0:
                fail(failures, f"runtime configuration readiness smoke summary.{key} must be > 0")

    checks = as_list(report.get("checks"))
    required_checks = {
        "configuration-api-readiness",
        "configuration-browser-pages",
        "configuration-postgres-metadata",
        "configuration-menu-metadata",
        "configuration-ec-view-samples",
        "configuration-runtime-view-samples",
    }
    seen_checks = {str(check.get("name")) for check in checks if isinstance(check, dict)}
    missing_checks = sorted(required_checks - seen_checks)
    if missing_checks:
        fail(failures, "runtime configuration readiness smoke missing checks: " + ", ".join(missing_checks))
    for check in checks:
        if isinstance(check, dict) and check.get("status") != "PASS":
            fail(failures, f"runtime configuration readiness smoke check must PASS: {check.get('name')}")

    api_results = as_list(report.get("apiResults"))
    if len(api_results) < 5:
        fail(failures, "runtime configuration readiness smoke must include at least 5 API results")
    for result in api_results:
        if isinstance(result, dict) and result.get("ok") is not True:
            fail(failures, f"runtime configuration API result must be ok: {result.get('name')}")

    browser_results = as_list(report.get("browserResults"))
    if len(browser_results) < 4:
        fail(failures, "runtime configuration readiness smoke must include at least 4 browser results")
    for result in browser_results:
        if not isinstance(result, dict):
            continue
        if result.get("ok") is not True:
            fail(failures, f"runtime configuration browser result must be ok: {result.get('name')}")
        if result.get("navigationStatus") != 200:
            fail(failures, f"runtime configuration browser navigationStatus must be 200: {result.get('name')}")

    database_evidence = report.get("databaseEvidence")
    if not isinstance(database_evidence, dict):
        fail(failures, "runtime configuration readiness smoke databaseEvidence must be an object")
    else:
        if len(as_list(database_evidence.get("counts"))) < 10:
            fail(failures, "runtime configuration readiness smoke databaseEvidence must include count rows")
        if len(as_list(database_evidence.get("menuRows"))) < 3:
            fail(failures, "runtime configuration readiness smoke databaseEvidence must include configuration menu rows")
        if len(as_list(database_evidence.get("ecViewRows"))) <= 0:
            fail(failures, "runtime configuration readiness smoke databaseEvidence must include ecViewRows")
        if len(as_list(database_evidence.get("runtimeRows"))) <= 0:
            fail(failures, "runtime configuration readiness smoke databaseEvidence must include runtimeRows")
        for sql_key in ("countSql", "menuSql", "runtimeSql", "ecViewSql"):
            if not str(database_evidence.get(sql_key, "")).strip():
                fail(failures, f"runtime configuration readiness smoke databaseEvidence must include {sql_key}")

    custom_property = read_json(CUSTOM_PROPERTY_PATH, failures, "custom property persistence acceptance report")
    if custom_property:
        if custom_property.get("status") != "PASS":
            fail(failures, "custom property persistence acceptance report must PASS")
        if custom_property.get("database") != "PostgreSQL":
            fail(failures, "custom property persistence acceptance database must remain PostgreSQL")
        if custom_property.get("route") != "/supplant/#/customFieldModelManage":
            fail(failures, "custom property persistence acceptance route must be /supplant/#/customFieldModelManage")
        if as_list(custom_property.get("issues")):
            fail(failures, "custom property persistence acceptance issues must be empty")
        checks = as_list(custom_property.get("checks"))
        required_checks = {
            "model-mapping-created",
            "model-mapping-edited",
            "runtime-property-synced",
            "project-property-synced",
            "controlled-cleanup",
        }
        seen_checks = {str(check.get("name")) for check in checks if isinstance(check, dict)}
        missing_checks = sorted(required_checks - seen_checks)
        if missing_checks:
            fail(failures, "custom property persistence acceptance missing checks: " + ", ".join(missing_checks))
        for check in checks:
            if isinstance(check, dict) and check.get("status") != "PASS":
                fail(failures, f"custom property persistence acceptance check must PASS: {check.get('name')}")

    entity_model = read_json(ENTITY_MODEL_PERSISTENCE_PATH, failures, "entity/model CRUD persistence acceptance report")
    if entity_model:
        if entity_model.get("status") != "PASS":
            fail(failures, "entity/model CRUD persistence acceptance report must PASS")
        if entity_model.get("database") != "PostgreSQL":
            fail(failures, "entity/model CRUD persistence acceptance database must remain PostgreSQL")
        if entity_model.get("route") != "/msService/ec/engine/msManage":
            fail(failures, "entity/model CRUD persistence acceptance route must be /msService/ec/engine/msManage")
        marker = str(entity_model.get("marker", ""))
        if not marker.startswith("ADP_E2E_") or "ENTITY_MODEL" not in marker:
            fail(failures, "entity/model CRUD persistence marker must be ADP_E2E_*_ENTITY_MODEL")
        if as_list(entity_model.get("issues")):
            fail(failures, "entity/model CRUD persistence acceptance issues must be empty")
        summary = entity_model.get("summary")
        if not isinstance(summary, dict):
            fail(failures, "entity/model CRUD persistence summary must be an object")
        else:
            if summary.get("fail") != 0 or summary.get("checks") != summary.get("pass"):
                fail(failures, "entity/model CRUD persistence must pass every check")
            if summary.get("entityRowsAfterCreate") != 1:
                fail(failures, "entity/model CRUD persistence must verify one ec_entity row after create")
            if summary.get("modelRowsAfterCreate") != 1:
                fail(failures, "entity/model CRUD persistence must verify one ec_model row after create")
            if int(summary.get("propertyRowsAfterCreate") or 0) < 3:
                fail(failures, "entity/model CRUD persistence must verify inherent ec_property rows")
            for cleanup_key in ("cleanupEntityRows", "cleanupModelRows", "cleanupPropertyRows"):
                if summary.get(cleanup_key) != 0:
                    fail(failures, f"entity/model CRUD controlled cleanup must leave {cleanup_key}=0")
        checks = as_list(entity_model.get("checks"))
        required_checks = {
            "browser-page-context",
            "http-requests",
            "entity-created",
            "model-created-with-inherent-properties",
            "entity-model-edited",
            "entity-model-deleted-or-disabled",
            "controlled-cleanup",
        }
        seen_checks = {str(check.get("name")) for check in checks if isinstance(check, dict)}
        missing_checks = sorted(required_checks - seen_checks)
        if missing_checks:
            fail(failures, "entity/model CRUD persistence missing checks: " + ", ".join(missing_checks))
        for check in checks:
            if isinstance(check, dict) and check.get("status") != "PASS":
                fail(failures, f"entity/model CRUD persistence check must PASS: {check.get('name')}")
        sql_text = json.dumps(entity_model.get("verificationSql", {}), ensure_ascii=False)
        for table in ("ec_entity", "ec_model", "ec_property", "information_schema.tables"):
            if table not in sql_text:
                fail(failures, f"entity/model CRUD verificationSql missing table {table}")


def check_physical_model_table_gap(area: dict[str, Any], failures: list[str]) -> None:
    if not area:
        return
    if area.get("status") != "PARTIAL":
        fail(failures, "configuration-physical-model-table must remain PARTIAL until physical table acceptance exists")
    if area.get("route") != "/msService/ec/engine/msManage":
        fail(failures, "configuration-physical-model-table route must be /msService/ec/engine/msManage")
    if area.get("requiresPersistence") is not True:
        fail(failures, "configuration-physical-model-table requiresPersistence must be true")
    refs = set(as_list(area.get("evidenceRefs")))
    for required_ref in (
        "metadata/entity-model-config-crud-persistence-acceptance.json",
        "deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js",
        "metadata/basic-config-action-matrix.json",
    ):
        if required_ref not in refs:
            fail(failures, f"configuration-physical-model-table evidenceRefs must include {required_ref}")
    tables = set(str(table) for table in as_list(area.get("tables")))
    for required_table in ("ec_model", "ec_property", "information_schema.tables", "generated_model_physical_table"):
        if required_table not in tables:
            fail(failures, f"configuration-physical-model-table tables must include {required_table}")
    if not as_list(area.get("remainingGaps")):
        fail(failures, "configuration-physical-model-table must keep remainingGaps while PARTIAL")
    area_text = json.dumps(area, ensure_ascii=False)
    for fragment in ("information_schema.tables", "PostgreSQL", "物理模型表"):
        if fragment not in area_text:
            fail(failures, f"configuration-physical-model-table missing explanatory fragment: {fragment}")

    report = read_json(ENTITY_MODEL_PERSISTENCE_PATH, failures, "entity/model CRUD persistence acceptance report")
    if not report:
        return
    if report.get("status") != "PASS":
        fail(failures, "entity/model CRUD persistence acceptance report must PASS before physical-table gap can be tracked")
    trace_text = json.dumps(report.get("backendTrace", {}), ensure_ascii=False)
    if "does not auto-create physical model tables" not in trace_text:
        fail(failures, "entity/model CRUD report must explicitly record the physical-table non-claim")
    sql_text = json.dumps(report.get("verificationSql", {}), ensure_ascii=False)
    if "information_schema.tables" not in sql_text:
        fail(failures, "entity/model CRUD report must include information_schema.tables verification SQL")


def check_coverage(data: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "repoCommit",
        "database",
        "module",
        "goalId",
        "overallStatus",
        "summary",
        "areas",
    ):
        if key not in data:
            fail(failures, f"coverage report missing top-level key: {key}")
    if data.get("database") != "PostgreSQL":
        fail(failures, "basic config coverage database must remain PostgreSQL")
    if data.get("module") != "basic-config":
        fail(failures, "basic config coverage module must be basic-config")
    if data.get("goalId") != "G-012":
        fail(failures, "basic config coverage goalId must be G-012")

    areas_raw = data.get("areas")
    if not isinstance(areas_raw, list):
        fail(failures, "areas must be a list")
        return
    areas = [area for area in areas_raw if isinstance(area, dict)]
    if len(areas) != len(areas_raw):
        fail(failures, "areas must contain only objects")

    seen_ids = {str(area.get("id", "")).strip() for area in areas}
    missing = sorted(REQUIRED_AREA_IDS - seen_ids)
    extra = sorted(seen_ids - REQUIRED_AREA_IDS)
    if missing:
        fail(failures, "basic config coverage missing areas: " + ", ".join(missing))
    if extra:
        fail(failures, "basic config coverage contains unknown areas: " + ", ".join(extra))

    for area in areas:
        check_area(area, failures)
    check_summary(data, areas, failures)

    incomplete = [
        str(area.get("id"))
        for area in areas
        if area.get("status") not in {"PASS", "NOT_APPLICABLE"}
    ]
    if incomplete and data.get("overallStatus") in {"PASS", "READY", "COMPLETE"}:
        fail(failures, "overallStatus cannot be complete while areas remain incomplete: " + ", ".join(incomplete))
    if not incomplete and data.get("overallStatus") != "PASS":
        fail(failures, "overallStatus should be PASS when all areas are PASS or NOT_APPLICABLE")

    area_by_id = {str(area.get("id")): area for area in areas}
    check_systemcode_acceptance(area_by_id.get("systemcode-dictionary", {}), failures)
    check_systemconfig_acceptance(area_by_id.get("systemconfig-app-catalog-value", {}), failures)

    built_in = area_by_id.get("systemconfig-built-in-catalogs", {})
    if built_in.get("status") == "PASS":
        fail(failures, "systemconfig-built-in-catalogs must stay non-PASS until built-in config edit/save/readback marker tests exist")
    if built_in and len(as_list(built_in.get("observedCatalogs"))) < 4:
        fail(failures, "systemconfig-built-in-catalogs must record observed built-in catalogs")
    if built_in:
        check_systemconfig_builtins(built_in, failures)

    runtime_config = area_by_id.get("configuration-entity-runtime", {})
    if runtime_config:
        check_runtime_configuration_readiness(runtime_config, failures)

    physical_model_table = area_by_id.get("configuration-physical-model-table", {})
    if physical_model_table:
        check_physical_model_table_gap(physical_model_table, failures)

    nacos_keycloak = area_by_id.get("nacos-keycloak-production-config", {})
    if nacos_keycloak:
        check_nacos_keycloak_runtime(nacos_keycloak, failures)


def main() -> int:
    failures: list[str] = []
    check_doc(failures)
    check_secret_hygiene(failures)
    data = read_json(COVERAGE_PATH, failures, "basic config coverage report")
    if data:
        check_coverage(data, failures)
    if failures:
        print(f"Basic config coverage verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Basic config coverage verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
