#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "metadata/basic-config-action-matrix.json"
DOC_PATH = ROOT / "docs/basic-config-action-matrix.md"
COVERAGE_PATH = ROOT / "metadata/basic-config-coverage.json"
BUILTINS_PATH = ROOT / "metadata/systemconfig-builtins-readiness-smoke.json"
RUNTIME_CONFIG_PATH = ROOT / "metadata/runtime-configuration-readiness-smoke.json"
CUSTOM_PROPERTY_PATH = ROOT / "metadata/custom-property-persistence-acceptance.json"
ENTITY_MODEL_PERSISTENCE_PATH = ROOT / "metadata/entity-model-config-crud-persistence-acceptance.json"
ENTITY_MODEL_FIELD_PERSISTENCE_PATH = ROOT / "metadata/entity-model-field-persistence-acceptance.json"
ENTITY_MODEL_FIELD_TYPE_MATRIX_PATH = ROOT / "metadata/entity-model-field-type-matrix-acceptance.json"
ENTITY_MODEL_OBJECT_ASSOCIATION_PATH = ROOT / "metadata/entity-model-object-association-acceptance.json"
ENTITY_MODEL_FIELD_DELETE_PATH = ROOT / "metadata/entity-model-field-delete-persistence-acceptance.json"
PRODUCTION_MATRIX_PATH = ROOT / "metadata/production-module-test-cases.json"

ALLOWED_STATUSES = {
    "PASS",
    "READ_ONLY_GUARDED",
    "CONTROLLED_MARKER_REQUIRED",
    "PLANNED",
    "BLOCKED",
}
SUMMARY_KEYS = {
    "PASS": "pass",
    "READ_ONLY_GUARDED": "readOnlyGuarded",
    "CONTROLLED_MARKER_REQUIRED": "controlledMarkerRequired",
    "PLANNED": "planned",
    "BLOCKED": "blocked",
}
REQUIRED_ACTION_IDS = {
    "systemcode-dictionary-crud",
    "systemconfig-app-catalog-crud",
    "builtin-identity-directory",
    "builtin-print-auth",
    "builtin-ak-sk",
    "builtin-password-policy",
    "builtin-qcs-runtime-config",
    "builtin-rm-runtime-config",
    "builtin-baseset-runtime-config",
    "custom-property-model-mapping-toggle",
    "entity-model-create",
    "entity-model-edit",
    "entity-model-delete",
    "entity-model-postgres-physical-table-autocreate",
    "entity-model-postgres-field-sync",
    "entity-model-postgres-field-delete",
    "nacos-production-export-diff",
    "keycloak-production-realm-migration",
}
READ_ONLY_CATALOGS = {
    "用户目录": "identity-directory",
    "打印服务授权": "license-or-entitlement",
    "AK/SK凭证管理": "secret",
    "密码配置": "security-policy",
}
CONTROLLED_CATALOGS = {
    "质量检验配置": "business-runtime-config",
    "RM.ocd.RM": "business-runtime-config",
    "BaseSet.ocd.BaseSet": "business-runtime-config",
}
FORBIDDEN_TEXT = {
    "ft" + "123456789",
    "Authorization: Bearer",
    "access_token",
    "refresh_token",
    "AKIA",
}
ENTITY_MODEL_CONTRACTS = {
    "entity-model-create": {
        "required_before": {"dedicated-marker", "rollback-plan", "runtime-page-regression"},
        "required_tables": {"ec_entity", "ec_model", "ec_property", "information_schema.tables"},
        "minimum_fragments": {"ADP_E2E", "HTTP", "PostgreSQL", "runtime"},
        "recheck_fragments": {"smoke-runtime-configuration", "basic-config-action-matrix-check"},
    },
    "entity-model-edit": {
        "required_before": {"dedicated-marker", "before-after-sql", "rollback-plan", "runtime-page-regression"},
        "required_tables": {"ec_entity", "ec_model", "ec_property", "information_schema.tables"},
        "minimum_fragments": {"ADP_E2E", "HTTP", "PostgreSQL", "runtime"},
        "recheck_fragments": {"smoke-runtime-configuration", "basic-config-action-matrix-check"},
    },
    "entity-model-delete": {
        "required_before": {"dedicated-marker", "before-after-sql", "rollback-plan", "menu-runtime-regression"},
        "required_tables": {
            "ec_entity",
            "ec_model",
            "ec_property",
            "information_schema.tables",
        },
        "minimum_fragments": {"ADP_E2E", "HTTP", "PostgreSQL", "menu", "runtime"},
        "recheck_fragments": {"smoke-runtime-configuration", "menu-smoke", "basic-config-action-matrix-check"},
    },
}
CONTROLLED_RUNTIME_CONFIG_CONTRACTS = {
    "builtin-qcs-runtime-config": {
        "required_before": {"business-owner-approval", "dedicated-marker", "rollback-plan", "qcs-report-regression"},
        "required_pass_cases": {
            "PROD-018",
            "PROD-019",
            "PROD-031",
            "PROD-032",
            "PROD-033",
            "PROD-034",
            "PROD-035",
            "PROD-036",
            "PROD-037",
            "PROD-038",
            "PROD-039",
            "PROD-040",
            "PROD-041",
            "PROD-042",
        },
        "blocked_impact_cases": set(),
        "required_tables": {"systemconfig_config_info", "systemconfig_config_version"},
        "minimum_fragments": {"ADP_E2E", "PostgreSQL", "QCS", "rollback"},
    },
    "builtin-rm-runtime-config": {
        "required_before": {"business-owner-approval", "dedicated-marker", "rollback-plan", "rm-formula-regression"},
        "required_pass_cases": {"PROD-008", "PROD-009", "PROD-010", "PROD-043"},
        "blocked_impact_cases": set(),
        "required_tables": {"systemconfig_config_info", "systemconfig_config_version"},
        "minimum_fragments": {"ADP_E2E", "PostgreSQL", "RM", "rollback"},
    },
    "builtin-baseset-runtime-config": {
        "required_before": {"business-owner-approval", "dedicated-marker", "rollback-plan", "wom-qcs-regression"},
        "required_pass_cases": {"PROD-001", "PROD-002", "PROD-003", "PROD-004", "PROD-022", "PROD-031", "PROD-035", "PROD-036"},
        "blocked_impact_cases": set(),
        "required_tables": {"systemconfig_config_info", "systemconfig_config_version"},
        "minimum_fragments": {"ADP_E2E", "PostgreSQL", "BaseSet", "WOM", "QCS", "rollback"},
    },
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
        fail(failures, f"{owner} evidenceRefs must contain non-empty paths")
        return
    path = Path(path_text)
    if path.is_absolute() or ".." in path.parts:
        fail(failures, f"{owner} evidenceRef must stay inside repo: {path_text}")
        return
    if not (ROOT / path).exists():
        fail(failures, f"{owner} evidenceRef does not exist: {path_text}")


def joined_list_text(value: Any) -> str:
    return "\n".join(str(item) for item in as_list(value))


def require_contract_list(
    contract: dict[str, Any],
    field: str,
    required_fragments: set[str],
    failures: list[str],
    action_id: str,
) -> None:
    values = as_list(contract.get(field))
    if not values:
        fail(failures, f"{action_id} acceptanceContract.{field} must be a non-empty list")
        return
    text = joined_list_text(values)
    for fragment in sorted(required_fragments):
        if fragment not in text:
            fail(failures, f"{action_id} acceptanceContract.{field} missing required fragment: {fragment}")


def check_secret_hygiene(failures: list[str]) -> None:
    for path in (MATRIX_PATH, DOC_PATH):
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
    for fragment in (
        "metadata/basic-config-action-matrix.json",
        "make basic-config-action-matrix-check",
        "READ_ONLY_GUARDED",
        "CONTROLLED_MARKER_REQUIRED",
        "ADP_E2E_*",
        "PostgreSQL",
        "entity-model-field-type-matrix-acceptance.json",
        "Nacos",
        "Keycloak",
    ):
        if fragment not in text:
            fail(failures, f"basic config action matrix document missing required text: {fragment}")


def action_by_id(matrix: dict[str, Any]) -> dict[str, dict[str, Any]]:
    actions: dict[str, dict[str, Any]] = {}
    for action in as_list(matrix.get("actions")):
        if isinstance(action, dict) and action.get("id"):
            actions[str(action["id"])] = action
    return actions


def coverage_area_by_id(coverage: dict[str, Any]) -> dict[str, dict[str, Any]]:
    areas: dict[str, dict[str, Any]] = {}
    for area in as_list(coverage.get("areas")):
        if isinstance(area, dict) and area.get("id"):
            areas[str(area["id"])] = area
    return areas


def check_summary(matrix: dict[str, Any], actions: list[dict[str, Any]], failures: list[str]) -> None:
    summary = matrix.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return
    expected = {
        "totalActions": len(actions),
        "pass": 0,
        "readOnlyGuarded": 0,
        "controlledMarkerRequired": 0,
        "planned": 0,
        "blocked": 0,
    }
    for action in actions:
        status = action.get("status")
        if status in SUMMARY_KEYS:
            expected[SUMMARY_KEYS[status]] += 1
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def check_action_shape(action: dict[str, Any], failures: list[str]) -> None:
    action_id = str(action.get("id", "")).strip()
    status = action.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{action_id or '<missing id>'} has invalid status: {status!r}")
    for key in ("id", "areaId", "title", "mutationPolicy", "riskLevel"):
        if not str(action.get(key, "")).strip():
            fail(failures, f"{action_id or '<missing id>'} must include non-empty {key}")
    if not isinstance(action.get("requiresPersistence"), bool):
        fail(failures, f"{action_id} requiresPersistence must be boolean")
    if not as_list(action.get("evidenceRefs")):
        fail(failures, f"{action_id} must include evidenceRefs")
    for evidence_ref in as_list(action.get("evidenceRefs")):
        validate_relative_path(evidence_ref, failures, action_id)

    if status == "PASS":
        if not str(action.get("markerPattern", "")).startswith("ADP_E2E_"):
            fail(failures, f"{action_id} PASS must include ADP_E2E markerPattern")
        if action.get("requiresPersistence") and not as_list(action.get("tables")):
            fail(failures, f"{action_id} PASS persistence action must include tables")
        if not as_list(action.get("acceptedOperations")):
            fail(failures, f"{action_id} PASS must include acceptedOperations")
        if as_list(action.get("remainingGaps")):
            fail(failures, f"{action_id} PASS must not include remainingGaps")
    else:
        if not as_list(action.get("remainingGaps")):
            fail(failures, f"{action_id} non-PASS action must include remainingGaps")

    if status in {"READ_ONLY_GUARDED", "CONTROLLED_MARKER_REQUIRED"}:
        if action.get("mutationAllowedInGenericSmoke") is not False:
            fail(failures, f"{action_id} must explicitly forbid generic smoke mutation")
        if action.get("mutationPolicy") == "marker-allowed":
            fail(failures, f"{action_id} guarded action cannot use marker-allowed mutation policy")
        required_before = set(str(item) for item in as_list(action.get("requiredBeforeMutation")))
        for required in ("dedicated-marker", "rollback-plan"):
            if required not in required_before:
                fail(failures, f"{action_id} must require {required} before mutation")
    if status == "PLANNED":
        if not as_list(action.get("requiredBeforeMutation")):
            fail(failures, f"{action_id} PLANNED action must include requiredBeforeMutation")


def check_coverage_alignment(matrix: dict[str, Any], failures: list[str]) -> None:
    coverage = read_json(COVERAGE_PATH, failures, "basic config coverage report")
    if not coverage:
        return
    if coverage.get("overallStatus") == "PASS" and matrix.get("overallStatus") != "PASS":
        fail(failures, "action matrix cannot remain non-PASS when coverage is PASS")
    if coverage.get("overallStatus") != "PASS" and matrix.get("overallStatus") == "PASS":
        fail(failures, "action matrix must stay non-PASS while coverage is incomplete")
    areas = coverage_area_by_id(coverage)
    required_area_status = {
        "systemcode-dictionary": "PASS",
        "systemconfig-app-catalog-value": "PASS",
        "systemconfig-built-in-catalogs": "PARTIAL",
        "configuration-entity-runtime": "PASS",
        "configuration-physical-model-table": "PASS",
        "configuration-physical-model-field": "PASS",
        "nacos-keycloak-production-config": "PARTIAL",
    }
    for area_id, expected_status in required_area_status.items():
        area = areas.get(area_id)
        if not area:
            fail(failures, f"coverage missing area required by action matrix: {area_id}")
        elif area.get("status") != expected_status:
            fail(failures, f"coverage area {area_id} must remain {expected_status} for current action matrix")


def check_builtin_alignment(actions: dict[str, dict[str, Any]], failures: list[str]) -> None:
    report = read_json(BUILTINS_PATH, failures, "systemconfig built-in catalog smoke report")
    if not report:
        return
    if report.get("status") != "PASS":
        fail(failures, "built-in catalog smoke must PASS before action matrix can classify built-in catalogs")
    catalogs = {
        str(catalog.get("name")): catalog
        for catalog in as_list(report.get("expectedCatalogs"))
        if isinstance(catalog, dict)
    }
    for catalog_name, classification in {**READ_ONLY_CATALOGS, **CONTROLLED_CATALOGS}.items():
        catalog = catalogs.get(catalog_name)
        if not catalog:
            fail(failures, f"built-in catalog smoke missing catalog used by action matrix: {catalog_name}")
            continue
        if catalog.get("classification") != classification:
            fail(failures, f"built-in catalog {catalog_name} classification must be {classification}")
        if catalog.get("observed") is not True:
            fail(failures, f"built-in catalog {catalog_name} must be observed")
        if catalog.get("mutationAllowedInSmoke") is not False:
            fail(failures, f"built-in catalog {catalog_name} must forbid mutation in smoke")

    for action in actions.values():
        catalog_name = action.get("catalogName")
        if not catalog_name:
            continue
        catalog = catalogs.get(str(catalog_name))
        if not catalog:
            fail(failures, f"{action.get('id')} catalogName is not backed by built-in smoke: {catalog_name}")
            continue
        if action.get("classification") != catalog.get("classification"):
            fail(failures, f"{action.get('id')} classification must match built-in smoke")


def check_runtime_alignment(actions: dict[str, dict[str, Any]], failures: list[str]) -> None:
    report = read_json(RUNTIME_CONFIG_PATH, failures, "runtime configuration readiness smoke report")
    if not report:
        return
    if report.get("status") != "PASS":
        fail(failures, "runtime configuration readiness smoke must PASS before entity/model actions can be PLANNED")
    summary = report.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "runtime configuration readiness smoke summary must be an object")
        return
    for key in ("ecEntityRows", "ecModelRows", "ecFieldRows", "ecViewRows", "runtimeViewRows"):
        if int(summary.get(key) or 0) <= 0:
            fail(failures, f"runtime readiness summary.{key} must be > 0 for entity/model action planning")
    for action_id in ("entity-model-create", "entity-model-edit", "entity-model-delete"):
        action = actions.get(action_id)
        if action and action.get("status") == "PASS":
            refs = set(as_list(action.get("evidenceRefs")))
            if "metadata/runtime-configuration-readiness-smoke.json" not in refs:
                fail(failures, f"{action_id} PASS must keep runtime readiness evidenceRef")


def check_custom_property_acceptance(actions: dict[str, dict[str, Any]], failures: list[str]) -> None:
    action = actions.get("custom-property-model-mapping-toggle")
    if not action:
        return
    if action.get("status") != "PASS":
        fail(failures, "custom-property-model-mapping-toggle must stay PASS after committed marker evidence exists")
        return
    refs = set(as_list(action.get("evidenceRefs")))
    for required_ref in (
        "metadata/custom-property-persistence-acceptance.json",
        "deploy/docker/scripts/adp-custom-property-persistence-acceptance.js",
        "deploy/docker/postgres/init/169-custom-property-project-property-compat.sql",
    ):
        if required_ref not in refs:
            fail(failures, f"custom-property-model-mapping-toggle evidenceRefs must include {required_ref}")
    report = read_json(CUSTOM_PROPERTY_PATH, failures, "custom property persistence acceptance report")
    if not report:
        return
    if report.get("status") != "PASS":
        fail(failures, "custom property persistence acceptance report must PASS")
    if report.get("database") != "PostgreSQL":
        fail(failures, "custom property persistence acceptance database must remain PostgreSQL")
    if report.get("route") != "/supplant/#/customFieldModelManage":
        fail(failures, "custom property persistence acceptance must use /supplant/#/customFieldModelManage")
    marker = str(report.get("marker", ""))
    if not marker.startswith("ADP_E2E_") or "CUSTOM_PROPERTY" not in marker:
        fail(failures, "custom property persistence acceptance marker must be ADP_E2E_*_CUSTOM_PROPERTY")

    checks = as_list(report.get("checks"))
    required_checks = {
        "browser-page-open",
        "model-mapping-created",
        "model-mapping-edited",
        "runtime-property-synced",
        "project-property-synced",
        "model-mapping-display-restored",
        "runtime-property-display-restored",
        "project-property-display-restored",
        "model-mapping-disabled",
        "runtime-property-disabled",
        "project-property-disabled",
        "controlled-cleanup",
        "final-readiness",
    }
    seen_checks = {str(check.get("name")) for check in checks if isinstance(check, dict)}
    missing_checks = sorted(required_checks - seen_checks)
    if missing_checks:
        fail(failures, "custom property acceptance missing checks: " + ", ".join(missing_checks))
    for check in checks:
        if isinstance(check, dict) and check.get("status") != "PASS":
            fail(failures, f"custom property acceptance check must PASS: {check.get('name')}")
    if as_list(report.get("issues")):
        fail(failures, "custom property persistence acceptance issues must be empty")

    backend_trace = report.get("backendTrace")
    if not isinstance(backend_trace, dict):
        fail(failures, "custom property acceptance backendTrace must be an object")
    else:
        trace_text = json.dumps(backend_trace, ensure_ascii=False)
        for fragment in (
            "ModelManageController",
            "ModelServiceFoundationImpl",
            "CustomPropertyModelMappingMapper",
            "PropertyProjectMapper",
        ):
            if fragment not in trace_text:
                fail(failures, f"custom property backendTrace missing {fragment}")

    evidence = report.get("databaseEvidence")
    if not isinstance(evidence, dict):
        fail(failures, "custom property acceptance databaseEvidence must be an object")
    else:
        for state_key in ("before", "afterEnable", "afterEdit", "afterRestore", "afterDisable", "afterCleanup"):
            if state_key not in evidence:
                fail(failures, f"custom property databaseEvidence missing {state_key}")
        after_cleanup = evidence.get("afterCleanup")
        if isinstance(after_cleanup, dict):
            if as_list(after_cleanup.get("modelMapping")):
                fail(failures, "custom property controlled cleanup must leave no model mapping rows")
            runtime_rows = as_list(after_cleanup.get("runtimeProperty"))
            if not runtime_rows or any(row.get("projCustomInUse") != "false" for row in runtime_rows if isinstance(row, dict)):
                fail(failures, "custom property controlled cleanup must restore runtime_property projCustomInUse=false")
        sql_text = json.dumps(evidence.get("verificationSql", {}), ensure_ascii=False)
        for table in ("base_cp_model_mapping", "runtime_property", "project_property", "base_cp_view_mapping"):
            if table not in sql_text:
                fail(failures, f"custom property verificationSql missing table {table}")


def check_entity_model_acceptance_contract(action: dict[str, Any], failures: list[str]) -> None:
    action_id = str(action.get("id", "")).strip()
    expectation = ENTITY_MODEL_CONTRACTS.get(action_id)
    if not expectation or action.get("status") != "PLANNED":
        return

    required_before = set(str(item) for item in as_list(action.get("requiredBeforeMutation")))
    missing_before = sorted(expectation["required_before"] - required_before)
    if missing_before:
        fail(failures, f"{action_id} missing requiredBeforeMutation contract gates: {', '.join(missing_before)}")

    action_tables = set(str(item) for item in as_list(action.get("tables")))
    missing_action_tables = sorted(expectation["required_tables"] - action_tables)
    if missing_action_tables:
        fail(failures, f"{action_id} action tables do not cover contract tables: {', '.join(missing_action_tables)}")

    contract = action.get("acceptanceContract")
    if not isinstance(contract, dict):
        fail(failures, f"{action_id} PLANNED entity/model action must include acceptanceContract")
        return
    if contract.get("targetActionId") != action_id:
        fail(failures, f"{action_id} acceptanceContract.targetActionId must match action id")

    contract_tables = set(str(item) for item in as_list(contract.get("requiredTables")))
    missing_contract_tables = sorted(expectation["required_tables"] - contract_tables)
    if missing_contract_tables:
        fail(failures, f"{action_id} acceptanceContract.requiredTables missing: {', '.join(missing_contract_tables)}")
    if not action_tables.issubset(contract_tables):
        fail(failures, f"{action_id} acceptanceContract.requiredTables must cover action.tables")

    for field in ("scope", "frontendRequirement", "persistenceRequirement"):
        if not str(contract.get(field, "")).strip():
            fail(failures, f"{action_id} acceptanceContract.{field} must be non-empty")
    frontend_text = str(contract.get("frontendRequirement", ""))
    if "Browser" not in frontend_text or "direct SQL" not in frontend_text:
        fail(failures, f"{action_id} acceptanceContract.frontendRequirement must require browser evidence and forbid direct SQL-only evidence")
    persistence_text = str(contract.get("persistenceRequirement", ""))
    for fragment in ("ADP_E2E", "PostgreSQL", "before/after SQL"):
        if fragment not in persistence_text:
            fail(failures, f"{action_id} acceptanceContract.persistenceRequirement missing required fragment: {fragment}")

    require_contract_list(
        contract,
        "apiEvidenceRequired",
        {"HTTP method", "URL", "request payload", "response status"},
        failures,
        action_id,
    )
    require_contract_list(
        contract,
        "backendTraceRequired",
        {"controller or route", "service", "repository/mapper/DAO", "SQL or ORM mapping"},
        failures,
        action_id,
    )
    require_contract_list(contract, "minimumPassConditions", expectation["minimum_fragments"], failures, action_id)
    require_contract_list(contract, "recheckCommands", expectation["recheck_fragments"], failures, action_id)

    rollback_text = joined_list_text(contract.get("rollbackPlan"))
    if not rollback_text:
        fail(failures, f"{action_id} acceptanceContract.rollbackPlan must be a non-empty list")
    elif "Verify" not in rollback_text or not any(fragment in rollback_text for fragment in ("Delete", "Restore", "purge", "remove")):
        fail(failures, f"{action_id} acceptanceContract.rollbackPlan must include cleanup/restore and verification")
    if not as_list(contract.get("currentGaps")):
        fail(failures, f"{action_id} acceptanceContract.currentGaps must be a non-empty list while action remains PLANNED")


def check_entity_model_persistence_acceptance(
    actions: dict[str, dict[str, Any]],
    failures: list[str],
) -> None:
    action_ids = ("entity-model-create", "entity-model-edit", "entity-model-delete")
    entity_actions = [actions.get(action_id) for action_id in action_ids if actions.get(action_id)]
    if not entity_actions:
        return
    pass_actions = [action for action in entity_actions if action.get("status") == "PASS"]
    if pass_actions and len(pass_actions) != len(action_ids):
        fail(failures, "entity/model CRUD actions must be promoted to PASS together because one acceptance report covers create/edit/delete")
        return
    if not pass_actions:
        return

    for action_id in action_ids:
        action = actions[action_id]
        refs = set(as_list(action.get("evidenceRefs")))
        for required_ref in (
            "metadata/entity-model-config-crud-persistence-acceptance.json",
            "deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js",
            "metadata/runtime-configuration-readiness-smoke.json",
            "metadata/entity-model-config-crud-readiness-probe.json",
        ):
            if required_ref not in refs:
                fail(failures, f"{action_id} evidenceRefs must include {required_ref}")
        action_tables = set(str(item) for item in as_list(action.get("tables")))
        missing_tables = sorted(ENTITY_MODEL_CONTRACTS[action_id]["required_tables"] - action_tables)
        if missing_tables:
            fail(failures, f"{action_id} PASS tables missing acceptance tables: {', '.join(missing_tables)}")
        if not str(action.get("markerPattern", "")).startswith("ADP_E2E_"):
            fail(failures, f"{action_id} PASS must keep ADP_E2E markerPattern")

    report = read_json(ENTITY_MODEL_PERSISTENCE_PATH, failures, "entity/model CRUD persistence acceptance report")
    if not report:
        return
    if report.get("status") != "PASS":
        fail(failures, "entity/model CRUD persistence acceptance report must PASS")
    if report.get("database") != "PostgreSQL":
        fail(failures, "entity/model CRUD persistence acceptance database must remain PostgreSQL")
    if report.get("route") != "/msService/ec/engine/msManage":
        fail(failures, "entity/model CRUD acceptance must use /msService/ec/engine/msManage")
    marker = str(report.get("marker", ""))
    if not marker.startswith("ADP_E2E_") or "ENTITY_MODEL" not in marker:
        fail(failures, "entity/model CRUD acceptance marker must be ADP_E2E_*_ENTITY_MODEL")
    if as_list(report.get("issues")):
        fail(failures, "entity/model CRUD persistence acceptance issues must be empty")
    if report.get("mutationAttempted") is not True:
        fail(failures, "entity/model CRUD acceptance must record mutationAttempted=true")

    summary = report.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "entity/model CRUD acceptance summary must be an object")
    else:
        if int(summary.get("checks") or 0) < 7 or summary.get("checks") != summary.get("pass"):
            fail(failures, "entity/model CRUD acceptance must pass all recorded checks")
        if summary.get("fail") != 0:
            fail(failures, "entity/model CRUD acceptance fail count must be 0")
        expected_counts = {
            "entityRowsAfterCreate": 1,
            "modelRowsAfterCreate": 1,
            "physicalTablesAfterCreate": 1,
            "physicalTablesAfterRename": 1,
            "cleanupEntityRows": 0,
            "cleanupModelRows": 0,
            "cleanupPropertyRows": 0,
            "cleanupPhysicalTables": 0,
        }
        for key, expected in expected_counts.items():
            if summary.get(key) != expected:
                fail(failures, f"entity/model CRUD acceptance summary.{key} must be {expected}")
        if int(summary.get("propertyRowsAfterCreate") or 0) < 3:
            fail(failures, "entity/model CRUD acceptance must prove inherent ec_property rows were created")
        if int(summary.get("physicalColumnsAfterReplay") or 0) < 12:
            fail(failures, "entity/model CRUD acceptance must prove idempotent replay retained at least 12 physical columns")

    checks = as_list(report.get("checks"))
    required_checks = {
        "browser-page-context",
        "http-requests",
        "entity-created",
        "model-created-with-inherent-properties",
        "postgres-physical-table-created",
        "entity-model-edited",
        "postgres-physical-table-renamed-and-expanded",
        "postgres-physical-table-idempotent-replay",
        "entity-model-deleted-or-disabled",
        "physical-table-retained-until-controlled-cleanup",
        "controlled-cleanup",
    }
    seen_checks = {str(check.get("name")) for check in checks if isinstance(check, dict)}
    missing_checks = sorted(required_checks - seen_checks)
    if missing_checks:
        fail(failures, "entity/model CRUD acceptance missing checks: " + ", ".join(missing_checks))
    for check in checks:
        if isinstance(check, dict) and check.get("status") != "PASS":
            fail(failures, f"entity/model CRUD acceptance check must PASS: {check.get('name')}")

    requests = report.get("requests")
    if not isinstance(requests, dict):
        fail(failures, "entity/model CRUD acceptance requests must be an object")
    else:
        identifiers = report.get("identifiers") if isinstance(report.get("identifiers"), dict) else {}
        entity_code = str(identifiers.get("entityCode", ""))
        model_code = str(identifiers.get("modelCode", ""))
        for request_key in (
            "entityCreate",
            "modelCreate",
            "entityEdit",
            "modelEdit",
            "modelIdempotentReplay",
            "modelDelete",
            "entityDelete",
        ):
            request = requests.get(request_key)
            if not isinstance(request, dict):
                fail(failures, f"entity/model CRUD acceptance missing request: {request_key}")
                continue
            if request.get("responseStatus") != 200 or request.get("ok") is not True:
                fail(failures, f"entity/model CRUD request must be HTTP 200 and ok=true: {request_key}")
            payload_text = json.dumps(request.get("requestPayload", {}), ensure_ascii=False)
            if request_key in {"entityCreate", "modelCreate", "entityEdit", "modelEdit", "modelIdempotentReplay"} and marker not in payload_text:
                fail(failures, f"entity/model CRUD request payload must include marker for {request_key}")
            expected_code = model_code if request_key == "modelDelete" else entity_code
            if request_key in {"modelDelete", "entityDelete"} and (not expected_code or expected_code not in payload_text):
                fail(failures, f"entity/model CRUD delete payload must include the report identifier for {request_key}")

    backend_trace = report.get("backendTrace")
    if not isinstance(backend_trace, dict):
        fail(failures, "entity/model CRUD acceptance backendTrace must be an object")
    else:
        trace_text = json.dumps(backend_trace, ensure_ascii=False)
        for fragment in (
            "EntityController",
            "EntityServiceImpl",
            "ModelController",
            "ModelServiceImpl",
            "ModelSyncDBUtils",
            "PostgresModelSyncSupport",
            "DtoUtils",
            "entityDao",
            "modelDao",
            "propertyDao",
        ):
            if fragment not in trace_text:
                fail(failures, f"entity/model CRUD backendTrace missing {fragment}")

    verification_sql = report.get("verificationSql")
    if not isinstance(verification_sql, dict):
        fail(failures, "entity/model CRUD acceptance verificationSql must be an object")
    else:
        sql_text = json.dumps(verification_sql, ensure_ascii=False)
        for table in ("ec_entity", "ec_model", "ec_property", "information_schema.tables", "information_schema.columns"):
            if table not in sql_text:
                fail(failures, f"entity/model CRUD verificationSql missing table {table}")

    states = report.get("states")
    if not isinstance(states, dict):
        fail(failures, "entity/model CRUD acceptance states must be an object")
    else:
        browser = states.get("browser")
        if not isinstance(browser, dict) or browser.get("navigationStatus") != 200:
            fail(failures, "entity/model CRUD browser state must have navigationStatus=200")
        elif any(as_list(browser.get(key)) for key in ("consoleErrors", "pageErrors", "requestFailures", "networkErrors")) or browser.get("visibleError"):
            fail(failures, "entity/model CRUD browser state must not contain visible/console/page/request/network errors")
        for state_key in (
            "before",
            "afterEntityCreate",
            "afterModelCreate",
            "afterEntityEdit",
            "afterModelEdit",
            "afterModelIdempotentReplay",
            "afterModelDelete",
            "afterEntityDelete",
            "afterCleanup",
        ):
            if state_key not in states:
                fail(failures, f"entity/model CRUD states missing {state_key}")


def check_entity_model_physical_table_acceptance(
    actions: dict[str, dict[str, Any]],
    failures: list[str],
) -> None:
    action = actions.get("entity-model-postgres-physical-table-autocreate")
    if not action:
        return
    if action.get("status") != "PASS":
        fail(failures, "entity-model-postgres-physical-table-autocreate must be PASS after physical-table acceptance")
    if action.get("areaId") != "configuration-physical-model-table":
        fail(failures, "entity-model-postgres-physical-table-autocreate areaId must be configuration-physical-model-table")
    if action.get("requiresPersistence") is not True:
        fail(failures, "entity-model-postgres-physical-table-autocreate requiresPersistence must be true")
    refs = set(as_list(action.get("evidenceRefs")))
    for required_ref in (
        "metadata/entity-model-config-crud-persistence-acceptance.json",
        "deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js",
        "metadata/basic-config-coverage.json",
    ):
        if required_ref not in refs:
            fail(failures, f"entity-model physical-table action evidenceRefs must include {required_ref}")
    tables = set(str(table) for table in as_list(action.get("tables")))
    for required_table in ("ec_model", "ec_property", "information_schema.tables", "generated_model_physical_table"):
        if required_table not in tables:
            fail(failures, f"entity-model physical-table action tables must include {required_table}")
    contract = action.get("acceptanceContract")
    if not isinstance(contract, dict):
        fail(failures, "entity-model physical-table action must include acceptanceContract")
    else:
        contract_text = json.dumps(contract, ensure_ascii=False)
        for fragment in ("ModelSyncDBUtils", "information_schema.tables", "ADP_E2E", "physical model table"):
            if fragment not in contract_text:
                fail(failures, f"entity-model physical-table acceptanceContract missing {fragment}")
        if as_list(contract.get("currentGaps")):
            fail(failures, "entity-model physical-table acceptanceContract.currentGaps must be empty after PASS")
    if as_list(action.get("remainingGaps")):
        fail(failures, "entity-model physical-table action remainingGaps must be empty after PASS")

    report = read_json(ENTITY_MODEL_PERSISTENCE_PATH, failures, "entity/model CRUD persistence acceptance report")
    if not report:
        return
    if int(report.get("schemaVersion") or 0) < 2:
        fail(failures, "entity/model physical-table acceptance report schemaVersion must be at least 2")
    if report.get("areaId") != "configuration-physical-model-table":
        fail(failures, "entity/model physical-table acceptance report areaId must be configuration-physical-model-table")
    if report.get("status") != "PASS":
        fail(failures, "entity/model physical-table acceptance report must PASS")
    marker = str(report.get("marker", ""))
    if "ENTITY_MODEL_PHYSICAL_TABLE" not in marker:
        fail(failures, "entity/model physical-table acceptance marker must include ENTITY_MODEL_PHYSICAL_TABLE")
    summary = report.get("summary") if isinstance(report.get("summary"), dict) else {}
    for key, expected in (
        ("physicalTablesAfterCreate", 1),
        ("physicalTablesAfterRename", 1),
        ("cleanupPhysicalTables", 0),
    ):
        if summary.get(key) != expected:
            fail(failures, f"entity/model physical-table acceptance summary.{key} must be {expected}")
    if int(summary.get("physicalColumnsAfterReplay") or 0) < 12:
        fail(failures, "entity/model physical-table acceptance must retain at least 12 columns after idempotent replay")
    required_checks = {
        "postgres-physical-table-created",
        "postgres-physical-table-renamed-and-expanded",
        "postgres-physical-table-idempotent-replay",
        "physical-table-retained-until-controlled-cleanup",
        "controlled-cleanup",
    }
    seen_checks = {
        str(check.get("name"))
        for check in as_list(report.get("checks"))
        if isinstance(check, dict) and check.get("status") == "PASS"
    }
    missing_checks = sorted(required_checks - seen_checks)
    if missing_checks:
        fail(failures, "entity/model physical-table acceptance missing PASS checks: " + ", ".join(missing_checks))
    trace_text = json.dumps(report.get("backendTrace", {}), ensure_ascii=False)
    for fragment in ("ModelSyncDBUtils", "PostgresModelSyncSupport"):
        if fragment not in trace_text:
            fail(failures, f"entity/model physical-table backendTrace missing {fragment}")
    sql_text = json.dumps(report.get("verificationSql", {}), ensure_ascii=False)
    for fragment in ("information_schema.tables", "information_schema.columns"):
        if fragment not in sql_text:
            fail(failures, f"entity/model physical-table acceptance SQL missing {fragment}")
    states = report.get("states") if isinstance(report.get("states"), dict) else {}
    for state_key in ("afterModelCreate", "afterModelEdit", "afterModelIdempotentReplay", "afterCleanup"):
        if state_key not in states:
            fail(failures, f"entity/model physical-table acceptance states missing {state_key}")


def check_entity_model_field_acceptance(
    actions: dict[str, dict[str, Any]],
    failures: list[str],
) -> None:
    action_id = "entity-model-postgres-field-sync"
    area_id = "configuration-physical-model-field"
    action = actions.get(action_id)
    if not action:
        return
    if action.get("status") != "PASS":
        fail(failures, f"{action_id} must be PASS after field lifecycle acceptance")
    if action.get("areaId") != area_id:
        fail(failures, f"{action_id} areaId must be {area_id}")
    if action.get("route") != "/msService/ec/engine/msManage":
        fail(failures, f"{action_id} route must be /msService/ec/engine/msManage")
    if action.get("requiresPersistence") is not True:
        fail(failures, f"{action_id} requiresPersistence must be true")

    refs = set(as_list(action.get("evidenceRefs")))
    for required_ref in (
        "metadata/entity-model-field-persistence-acceptance.json",
        "deploy/docker/scripts/adp-entity-model-field-persistence-acceptance.js",
        "metadata/entity-model-field-type-matrix-acceptance.json",
        "deploy/docker/scripts/adp-entity-model-field-type-matrix-acceptance.js",
        "metadata/entity-model-object-association-acceptance.json",
        "deploy/docker/scripts/adp-entity-model-object-association-acceptance.js",
        "metadata/persistence-acceptance.json",
        "metadata/basic-config-coverage.json",
    ):
        if required_ref not in refs:
            fail(failures, f"{action_id} evidenceRefs must include {required_ref}")
    tables = set(str(table) for table in as_list(action.get("tables")))
    for required_table in (
        "ec_property",
        "information_schema.columns",
        "pg_index",
        "pg_constraint",
        "generated_model_physical_table",
    ):
        if required_table not in tables:
            fail(failures, f"{action_id} tables must include {required_table}")

    report = read_json(
        ENTITY_MODEL_FIELD_PERSISTENCE_PATH,
        failures,
        "entity/model PostgreSQL field persistence acceptance report",
    )
    if not report:
        return
    for key, expected in (
        ("database", "PostgreSQL"),
        ("module", "basic-config"),
        ("actionId", action_id),
        ("areaId", area_id),
        ("status", "PASS"),
        ("route", "/msService/ec/engine/msManage"),
    ):
        if report.get(key) != expected:
            fail(failures, f"field persistence report {key} must be {expected}")
    marker = str(report.get("marker", ""))
    if not marker.startswith("ADP_E2E_") or "PG_FIELD" not in marker:
        fail(failures, "field persistence marker must be ADP_E2E_*_PG_FIELD")
    summary = report.get("summary") if isinstance(report.get("summary"), dict) else {}
    for key, expected in (("testedChecks", 33), ("pass", 33), ("fail", 0), ("blocked", 0), ("status", "PASS")):
        if summary.get(key) != expected:
            fail(failures, f"field persistence summary.{key} must be {expected}")

    required_checks = {
        "browser-page-context",
        "create-requests",
        "field-metadata-created",
        "postgres-column-created-and-indexed",
        "marker-row-written",
        "managed-index-disabled-without-column-loss",
        "managed-index-disable-idempotent",
        "managed-index-reenabled",
        "managed-unique-enabled-and-replaces-ordinary-index",
        "managed-unique-enable-idempotent",
        "managed-unique-rejects-duplicate-row",
        "field-renamed-widened-and-data-preserved",
        "managed-unique-renamed-with-column",
        "managed-unique-disabled-and-ordinary-index-restored",
        "managed-unique-disable-idempotent",
        "managed-index-renamed-with-column",
        "external-unique-index-fixture-created",
        "external-unique-index-protected-on-managed-disable",
        "external-index-satisfies-reenable-without-duplicate",
        "external-unique-index-satisfies-property-unique",
        "external-unique-enable-idempotent",
        "external-unique-index-protected-on-property-disable",
        "not-null-enabled",
        "not-null-enable-idempotent",
        "not-null-rejects-null-row",
        "nullable-restored",
        "nullable-restore-idempotent",
        "null-fixture-written",
        "unsafe-not-null-change-rolled-back",
        "null-fixture-cleaned",
        "field-idempotent-replay",
        "unsafe-type-change-rolled-back",
        "controlled-cleanup",
    }
    passed_checks = {
        str(check.get("name"))
        for check in as_list(report.get("checks"))
        if isinstance(check, dict) and check.get("status") == "PASS"
    }
    missing_checks = sorted(required_checks - passed_checks)
    if missing_checks:
        fail(failures, "field persistence acceptance missing PASS checks: " + ", ".join(missing_checks))

    requests = report.get("requests") if isinstance(report.get("requests"), dict) else {}
    for request_key in (
        "entityCreate",
        "modelCreate",
        "fieldCreate",
        "fieldIndexDisable",
        "fieldIndexDisableReplay",
        "fieldIndexEnable",
        "fieldUniqueEnable",
        "fieldUniqueEnableReplay",
        "fieldRenameAndWiden",
        "fieldUniqueDisable",
        "fieldUniqueDisableReplay",
        "fieldOrdinaryIndexRenameToInitial",
        "fieldOrdinaryIndexRenameToFinal",
        "fieldIndexDisableWithExternal",
        "fieldIndexEnableWithExternal",
        "fieldUniqueEnableWithExternal",
        "fieldUniqueEnableWithExternalReplay",
        "fieldUniqueDisableWithExternal",
        "fieldNotNullEnable",
        "fieldNotNullEnableReplay",
        "fieldNullableEnable",
        "fieldNullableEnableReplay",
        "fieldIdempotentReplay",
    ):
        request = requests.get(request_key)
        if not isinstance(request, dict) or request.get("responseStatus") != 200:
            fail(failures, f"field persistence request {request_key} must return HTTP 200")
    unsafe_request = requests.get("unsafeTypeChange")
    if not isinstance(unsafe_request, dict) or unsafe_request.get("responseStatus") != 500:
        fail(failures, "unsafe field type conversion must fail closed with HTTP 500")
    unsafe_not_null = requests.get("unsafeNotNull")
    if not isinstance(unsafe_not_null, dict) or unsafe_not_null.get("responseStatus") != 500:
        fail(failures, "unsafe field NOT NULL change must fail closed with HTTP 500")
    db_actions = report.get("dbActions") if isinstance(report.get("dbActions"), dict) else {}
    for action_key in ("duplicateInsert", "nullInsertWhileNotNull"):
        action_result = db_actions.get(action_key)
        if not isinstance(action_result, dict) or action_result.get("rejected") is not True:
            fail(failures, f"field persistence dbActions.{action_key} must prove PostgreSQL rejection")

    browser = report.get("browser") if isinstance(report.get("browser"), dict) else {}
    if browser.get("navigationStatus") != 200 or browser.get("visibleError"):
        fail(failures, "field persistence browser evidence must load without a visible error")
    for key in ("pageErrors", "requestFailures"):
        if as_list(browser.get(key)):
            fail(failures, f"field persistence browser {key} must be empty")
    unexpected_network = [
        item
        for item in as_list(browser.get("networkErrors"))
        if not isinstance(item, dict) or item.get("expected") is not True
    ]
    if unexpected_network:
        fail(failures, "field persistence browser must not contain unexpected network errors")

    trace_text = json.dumps(report.get("backendTrace", {}), ensure_ascii=False)
    for fragment in (
        "FieldSyncDBUtils",
        "PostgresFieldSyncSupport",
        "ALTER TABLE",
        "ALTER INDEX",
        "DROP INDEX",
        "ADD CONSTRAINT",
        "RENAME CONSTRAINT",
        "DROP CONSTRAINT",
        "SET NOT NULL",
        "DROP NOT NULL",
        "roll back",
    ):
        if fragment not in trace_text:
            fail(failures, f"field persistence backendTrace missing {fragment}")
    sql_text = json.dumps(report.get("verificationSql", {}), ensure_ascii=False)
    for fragment in ("ec_property", "information_schema.columns", "pg_index", "pg_constraint", "drop table"):
        if fragment not in sql_text:
            fail(failures, f"field persistence verificationSql missing {fragment}")
    cleanup = report.get("cleanup") if isinstance(report.get("cleanup"), dict) else {}
    for key in ("property", "model", "entity", "physicalTable"):
        if cleanup.get(key) != 0:
            fail(failures, f"field persistence cleanup.{key} must be 0")
    if report.get("fatalError") is not None:
        fail(failures, "field persistence acceptance fatalError must be null")

    type_matrix = read_json(
        ENTITY_MODEL_FIELD_TYPE_MATRIX_PATH,
        failures,
        "entity/model PostgreSQL field type matrix acceptance report",
    )
    if not type_matrix:
        return
    for key, expected in (
        ("database", "PostgreSQL"),
        ("module", "basic-config"),
        ("actionId", "entity-model-postgres-field-type-matrix"),
        ("areaId", area_id),
        ("status", "PASS"),
        ("route", "/msService/ec/engine/msManage"),
    ):
        if type_matrix.get(key) != expected:
            fail(failures, f"field type matrix report {key} must be {expected}")
    marker = str(type_matrix.get("marker", ""))
    if not marker.startswith("ADP_E2E_") or "PG_TYPE_MATRIX" not in marker:
        fail(failures, "field type matrix marker must be ADP_E2E_*_PG_TYPE_MATRIX")
    summary = type_matrix.get("summary") if isinstance(type_matrix.get("summary"), dict) else {}
    for key, expected in (("testedChecks", 36), ("pass", 36), ("fail", 0), ("blocked", 0), ("status", "PASS")):
        if summary.get(key) != expected:
            fail(failures, f"field type matrix summary.{key} must be {expected}")
    coverage = type_matrix.get("coverage") if isinstance(type_matrix.get("coverage"), dict) else {}
    if coverage.get("scalarFieldInstances") != 24 or len(as_list(coverage.get("distinctDbColumnTypes"))) != 23:
        fail(failures, "field type matrix must retain 24 field instances across 23 scalar DbColumnTypes")
    if "OBJECT" in {str(item) for item in as_list(coverage.get("distinctDbColumnTypes"))}:
        fail(failures, "field type matrix must not claim OBJECT as scalar coverage")
    excluded = as_list(coverage.get("excludedTypes"))
    if len(excluded) != 1 or not isinstance(excluded[0], dict) or excluded[0].get("type") != "OBJECT":
        fail(failures, "field type matrix must preserve OBJECT as the dedicated association-fixture gap")
    passed_matrix_checks = {
        str(item.get("name"))
        for item in as_list(type_matrix.get("checks"))
        if isinstance(item, dict) and item.get("status") == "PASS"
    }
    for check_name in (
        "scalar-marker-row-round-trip",
        "invalid-integer-to-boolean-rolls-back",
        "zero-one-integer-to-boolean-preserves-values",
        "integer-to-bigint-safe-widening",
        "integer-to-numeric-safe-widening",
        "date-to-timestamp-safe-widening",
        "numeric-capacity-reduction-rolls-back",
        "controlled-cleanup",
    ):
        if check_name not in passed_matrix_checks:
            fail(failures, f"field type matrix missing PASS check: {check_name}")
    requests = type_matrix.get("requests") if isinstance(type_matrix.get("requests"), dict) else {}
    for request_key in (
        "entityCreate", "modelCreate", "booleanToInteger", "integerToBoolean", "booleanReplay",
        "integerToLong", "integerToNumeric", "dateToDateTime",
    ):
        request = requests.get(request_key)
        if not isinstance(request, dict) or request.get("responseStatus") != 200:
            fail(failures, f"field type matrix request {request_key} must return HTTP 200")
    for request_key in ("invalidIntegerToBoolean", "unsafeDecimalScale"):
        request = requests.get(request_key)
        if not isinstance(request, dict) or request.get("responseStatus") != 500:
            fail(failures, f"field type matrix request {request_key} must fail closed with HTTP 500")
    browser = type_matrix.get("browser") if isinstance(type_matrix.get("browser"), dict) else {}
    if browser.get("navigationStatus") != 200 or browser.get("visibleError"):
        fail(failures, "field type matrix browser evidence must load without a visible error")
    unexpected_network = [
        item
        for item in as_list(browser.get("networkErrors"))
        if not isinstance(item, dict) or item.get("expected") is not True
    ]
    if unexpected_network or as_list(browser.get("pageErrors")) or as_list(browser.get("requestFailures")):
        fail(failures, "field type matrix browser evidence contains unexpected errors")
    cleanup = type_matrix.get("cleanup") if isinstance(type_matrix.get("cleanup"), dict) else {}
    if any(cleanup.get(key) != 0 for key in ("property", "model", "entity", "physicalTable")):
        fail(failures, "field type matrix cleanup must leave all marker counts at 0")
    if type_matrix.get("fatalError") is not None:
        fail(failures, "field type matrix fatalError must be null")

    object_association = read_json(
        ENTITY_MODEL_OBJECT_ASSOCIATION_PATH,
        failures,
        "entity/model PostgreSQL OBJECT association acceptance report",
    )
    if not object_association:
        return
    for key, expected in (
        ("database", "PostgreSQL"),
        ("module", "basic-config"),
        ("actionId", "entity-model-postgres-object-association"),
        ("areaId", area_id),
        ("status", "PASS"),
        ("route", "/msService/ec/engine/msManage"),
    ):
        if object_association.get(key) != expected:
            fail(failures, f"OBJECT association report {key} must be {expected}")
    marker = str(object_association.get("marker", ""))
    if not marker.startswith("ADP_E2E_") or "PG_OBJECT_ASSOC" not in marker:
        fail(failures, "OBJECT association marker must be ADP_E2E_*_PG_OBJECT_ASSOC")
    summary = object_association.get("summary") if isinstance(object_association.get("summary"), dict) else {}
    for key, expected in (("testedChecks", 10), ("pass", 10), ("fail", 0), ("blocked", 0), ("status", "PASS")):
        if summary.get(key) != expected:
            fail(failures, f"OBJECT association summary.{key} must be {expected}")
    coverage = object_association.get("coverage") if isinstance(object_association.get("coverage"), dict) else {}
    if set(str(item) for item in as_list(coverage.get("associationTypes"))) != {"ONE_TO_ONE", "MANY_TO_ONE"}:
        fail(failures, "OBJECT association coverage must retain ONE_TO_ONE and MANY_TO_ONE")
    if set(str(item) for item in as_list(coverage.get("targetKeyTypes"))) != {"LONG", "TEXT", "BAPCODE"}:
        fail(failures, "OBJECT association coverage must retain LONG, TEXT and BAPCODE targets")
    required_object_checks = {
        "browser-page-context",
        "entity-model-target-and-object-create-requests",
        "object-association-metadata-persistence",
        "postgres-object-physical-column-types",
        "legacy-underscore-association-parameter",
        "marker-row-round-trip-and-logical-association-joins",
        "object-property-replay-is-idempotent",
        "invalid-associated-property-rolls-back",
        "legacy-object-storage-does-not-invent-physical-foreign-keys",
        "controlled-cleanup",
    }
    passed_object_checks = {
        str(item.get("name"))
        for item in as_list(object_association.get("checks"))
        if isinstance(item, dict) and item.get("status") == "PASS"
    }
    missing_object_checks = sorted(required_object_checks - passed_object_checks)
    if missing_object_checks:
        fail(failures, "OBJECT association missing PASS checks: " + ", ".join(missing_object_checks))
    requests = object_association.get("requests") if isinstance(object_association.get("requests"), dict) else {}
    normal_request_keys = {
        "targetEntityCreate",
        "targetModelCreate",
        "createTarget_longKey",
        "createTarget_textKey",
        "createTarget_bapCodeKey",
        "sourceEntityCreate",
        "sourceModelCreate",
        "createObject_oneToOneLong",
        "createObject_manyToOneLong",
        "createObject_oneToOneText",
        "createObject_manyToOneBapCode",
        "objectReplay",
    }
    missing_requests = sorted((normal_request_keys | {"invalidTarget"}) - set(requests))
    if missing_requests:
        fail(failures, "OBJECT association missing requests: " + ", ".join(missing_requests))
    for request_key in sorted(normal_request_keys):
        request = requests.get(request_key)
        if not isinstance(request, dict) or request.get("responseStatus") != 200:
            fail(failures, f"OBJECT association request {request_key} must return HTTP 200")
    invalid_request = requests.get("invalidTarget")
    if not isinstance(invalid_request, dict) or invalid_request.get("responseStatus") != 500:
        fail(failures, "OBJECT association invalidTarget request must fail closed with HTTP 500")
    states = object_association.get("states") if isinstance(object_association.get("states"), dict) else {}
    after_marker = states.get("afterMarkerInsert") if isinstance(states.get("afterMarkerInsert"), dict) else {}
    if as_list(after_marker.get("joinCounts")) != [1, 1, 1, 1]:
        fail(failures, "OBJECT association must retain four successful logical marker joins")
    if after_marker.get("foreignKeyCount") != 0:
        fail(failures, "OBJECT association must not invent a synthetic PostgreSQL foreign key")
    browser = object_association.get("browser") if isinstance(object_association.get("browser"), dict) else {}
    if browser.get("navigationStatus") != 200 or browser.get("visibleError"):
        fail(failures, "OBJECT association browser evidence must load without a visible error")
    unexpected_network = [
        item
        for item in as_list(browser.get("networkErrors"))
        if not isinstance(item, dict) or item.get("expected") is not True
    ]
    if unexpected_network or as_list(browser.get("pageErrors")) or as_list(browser.get("requestFailures")):
        fail(failures, "OBJECT association browser evidence contains unexpected errors")
    if as_list(object_association.get("invalidResidual")) != [0, 0]:
        fail(failures, "OBJECT association invalid target must leave metadata and column residuals at zero")
    cleanup = object_association.get("cleanup") if isinstance(object_association.get("cleanup"), dict) else {}
    if any(cleanup.get(key) != 0 for key in ("property", "model", "entity", "physicalTable")):
        fail(failures, "OBJECT association cleanup must leave all marker counts at 0")
    if object_association.get("fatalError") is not None:
        fail(failures, "OBJECT association fatalError must be null")

    delete_action_id = "entity-model-postgres-field-delete"
    delete_action = actions.get(delete_action_id)
    if not delete_action:
        return
    for key, expected in (
        ("status", "PASS"),
        ("areaId", area_id),
        ("route", "/msService/ec/engine/msManage"),
        ("requiresPersistence", True),
    ):
        if delete_action.get(key) != expected:
            fail(failures, f"{delete_action_id} {key} must be {expected}")
    delete_refs = set(as_list(delete_action.get("evidenceRefs")))
    for required_ref in (
        "metadata/entity-model-field-delete-persistence-acceptance.json",
        "deploy/docker/scripts/adp-entity-model-field-delete-persistence-acceptance.js",
        "metadata/persistence-acceptance.json",
        "metadata/basic-config-coverage.json",
    ):
        if required_ref not in delete_refs:
            fail(failures, f"{delete_action_id} evidenceRefs must include {required_ref}")
    delete_tables = set(str(table) for table in as_list(delete_action.get("tables")))
    for required_table in (
        "ec_property",
        "information_schema.columns",
        "generated_model_physical_table",
    ):
        if required_table not in delete_tables:
            fail(failures, f"{delete_action_id} tables must include {required_table}")
    delete_contract = (
        delete_action.get("acceptanceContract")
        if isinstance(delete_action.get("acceptanceContract"), dict)
        else {}
    )
    if delete_contract.get("targetActionId") != delete_action_id:
        fail(failures, f"{delete_action_id} acceptanceContract.targetActionId must match")
    if as_list(delete_contract.get("currentGaps")):
        fail(failures, f"{delete_action_id} acceptanceContract.currentGaps must be empty after PASS")
    if as_list(delete_action.get("remainingGaps")):
        fail(failures, f"{delete_action_id} remainingGaps must be empty after PASS")

    delete_report = read_json(
        ENTITY_MODEL_FIELD_DELETE_PATH,
        failures,
        "entity/model PostgreSQL field delete acceptance report",
    )
    if not delete_report:
        return
    for key, expected in (
        ("database", "PostgreSQL"),
        ("module", "basic-config"),
        ("actionId", delete_action_id),
        ("areaId", area_id),
        ("status", "PASS"),
        ("route", "/msService/ec/engine/msManage"),
    ):
        if delete_report.get(key) != expected:
            fail(failures, f"field delete report {key} must be {expected}")
    delete_marker = str(delete_report.get("marker", ""))
    if not delete_marker.startswith("ADP_E2E_") or "PG_FIELD_DELETE" not in delete_marker:
        fail(failures, "field delete marker must be ADP_E2E_*_PG_FIELD_DELETE")
    delete_summary = (
        delete_report.get("summary")
        if isinstance(delete_report.get("summary"), dict)
        else {}
    )
    for key, expected in (
        ("testedChecks", 18),
        ("pass", 18),
        ("fail", 0),
        ("blocked", 0),
        ("status", "PASS"),
    ):
        if delete_summary.get(key) != expected:
            fail(failures, f"field delete summary.{key} must be {expected}")
    required_delete_checks = {
        "real-entity-model-page-loaded",
        "irreversible-delete-warning-visible",
        "ordinary-delete-preserves-column-and-data",
        "explicit-delete-removes-metadata-column-and-data",
        "database-dependency-blocks-restrict-delete",
        "metadata-delete-failure-rolls-back-ddl-and-data",
        "physical-column-drift-fails-closed",
        "attachment-metadata-delete-without-physical-column",
        "inherent-primary-key-delete-rejected",
        "browser-has-no-unexpected-errors",
        "controlled-cleanup",
    }
    passed_delete_checks = {
        str(item.get("name"))
        for item in as_list(delete_report.get("checks"))
        if isinstance(item, dict) and item.get("status") == "PASS"
    }
    missing_delete_checks = sorted(required_delete_checks - passed_delete_checks)
    if missing_delete_checks:
        fail(failures, "field delete missing PASS checks: " + ", ".join(missing_delete_checks))
    delete_requests = (
        delete_report.get("requests")
        if isinstance(delete_report.get("requests"), dict)
        else {}
    )
    for request_key in (
        "softDelete",
        "hardDeleteAfterSoft",
        "dependencyRetryDelete",
        "transactionRetryDelete",
        "driftRetryDelete",
        "attachmentDelete",
    ):
        request = delete_requests.get(request_key)
        response = request.get("json") if isinstance(request, dict) and isinstance(request.get("json"), dict) else {}
        if not isinstance(request, dict) or request.get("responseStatus") != 200 or response.get("success") is not True:
            fail(failures, f"field delete request {request_key} must return HTTP 200/success=true")
    rejected_messages = {
        "dependencyBlockedDelete": "数据库视图或其他对象引用",
        "transactionRollbackDelete": "数据库变更已回滚",
        "driftBlockedDelete": "物理结构不一致",
        "inherentDelete": "固有字段不可删除",
    }
    for request_key, message in rejected_messages.items():
        request = delete_requests.get(request_key)
        response = request.get("json") if isinstance(request, dict) and isinstance(request.get("json"), dict) else {}
        if (
            not isinstance(request, dict)
            or request.get("responseStatus") != 200
            or response.get("success") is not False
            or message not in str(response.get("exceptionMsg", ""))
        ):
            fail(failures, f"field delete request {request_key} must return the structured rejection: {message}")
    delete_browser = (
        delete_report.get("browser")
        if isinstance(delete_report.get("browser"), dict)
        else {}
    )
    for key in ("engineNavigationStatus", "entityConfigNavigationStatus", "modelManageNavigationStatus"):
        if delete_browser.get(key) != 200:
            fail(failures, f"field delete browser {key} must be 200")
    if delete_browser.get("deleteWarningVisible") is not True or delete_browser.get("visibleError"):
        fail(failures, "field delete browser must show the irreversible warning without a visible error")
    for key in ("consoleErrors", "pageErrors", "requestFailures", "networkErrors"):
        if as_list(delete_browser.get(key)):
            fail(failures, f"field delete browser {key} must be empty")
    delete_trace = json.dumps(delete_report.get("backendTrace", {}), ensure_ascii=False)
    for fragment in (
        "ordinaryDelete",
        "deletePropertyPhysical",
        "FieldSyncDBUtils",
        "PostgresFieldSyncSupport",
        "DROP COLUMN RESTRICT",
        "Spring transaction",
    ):
        if fragment not in delete_trace:
            fail(failures, f"field delete backendTrace missing {fragment}")
    delete_sql = json.dumps(delete_report.get("verificationSql", {}), ensure_ascii=False)
    for fragment in ("ec_property", "to_jsonb", "create view", "create trigger", "rename column", "drop table"):
        if fragment not in delete_sql.lower():
            fail(failures, f"field delete verificationSql missing {fragment}")
    delete_cleanup = (
        delete_report.get("cleanup")
        if isinstance(delete_report.get("cleanup"), dict)
        else {}
    )
    for key in ("property", "model", "entity", "physicalTable", "dependencyView", "rollbackFunction"):
        if delete_cleanup.get(key) != 0:
            fail(failures, f"field delete cleanup.{key} must be 0")
    if delete_report.get("fatalError") is not None:
        fail(failures, "field delete fatalError must be null")


def production_case_statuses(failures: list[str]) -> dict[str, str]:
    report = read_json(PRODUCTION_MATRIX_PATH, failures, "production module test matrix")
    statuses: dict[str, str] = {}
    for case in as_list(report.get("cases")):
        if isinstance(case, dict) and case.get("id"):
            statuses[str(case["id"])] = str(case.get("acceptanceStatus", ""))
    return statuses


def check_controlled_runtime_config_contract(
    action: dict[str, Any],
    production_statuses: dict[str, str],
    failures: list[str],
) -> None:
    action_id = str(action.get("id", "")).strip()
    expectation = CONTROLLED_RUNTIME_CONFIG_CONTRACTS.get(action_id)
    if not expectation:
        return
    if action.get("status") != "CONTROLLED_MARKER_REQUIRED":
        fail(failures, f"{action_id} must remain CONTROLLED_MARKER_REQUIRED until dedicated marker evidence exists")
        return

    required_before = set(str(item) for item in as_list(action.get("requiredBeforeMutation")))
    missing_before = sorted(expectation["required_before"] - required_before)
    if missing_before:
        fail(failures, f"{action_id} missing requiredBeforeMutation gates: {', '.join(missing_before)}")

    contract = action.get("acceptanceContract")
    if not isinstance(contract, dict):
        fail(failures, f"{action_id} CONTROLLED_MARKER_REQUIRED action must include acceptanceContract")
        return
    if contract.get("targetActionId") != action_id:
        fail(failures, f"{action_id} acceptanceContract.targetActionId must match action id")

    contract_tables = set(str(item) for item in as_list(contract.get("requiredTables")))
    missing_tables = sorted(expectation["required_tables"] - contract_tables)
    if missing_tables:
        fail(failures, f"{action_id} acceptanceContract.requiredTables missing: {', '.join(missing_tables)}")

    required_pass_cases = set(str(item) for item in as_list(contract.get("requiredProductionCaseIds")))
    missing_pass_cases = sorted(expectation["required_pass_cases"] - required_pass_cases)
    if missing_pass_cases:
        fail(failures, f"{action_id} acceptanceContract.requiredProductionCaseIds missing: {', '.join(missing_pass_cases)}")
    for case_id in sorted(required_pass_cases):
        status = production_statuses.get(case_id)
        if status != "PASS":
            fail(failures, f"{action_id} required production case {case_id} must currently be PASS, got {status!r}")

    blocked_cases = set(str(item) for item in as_list(contract.get("blockedImpactCaseIds")))
    missing_blocked_cases = sorted(expectation["blocked_impact_cases"] - blocked_cases)
    if missing_blocked_cases:
        fail(failures, f"{action_id} acceptanceContract.blockedImpactCaseIds missing: {', '.join(missing_blocked_cases)}")
    for case_id in sorted(blocked_cases):
        status = production_statuses.get(case_id)
        if status != "BLOCKED":
            fail(failures, f"{action_id} blocked impact case {case_id} must currently be BLOCKED, got {status!r}")

    for field in ("scope", "frontendRequirement", "persistenceRequirement"):
        if not str(contract.get(field, "")).strip():
            fail(failures, f"{action_id} acceptanceContract.{field} must be non-empty")
    frontend_text = str(contract.get("frontendRequirement", ""))
    if "Browser" not in frontend_text or "generic smoke" not in frontend_text:
        fail(failures, f"{action_id} acceptanceContract.frontendRequirement must require browser evidence and reject generic smoke mutation")
    persistence_text = str(contract.get("persistenceRequirement", ""))
    for fragment in ("ADP_E2E", "PostgreSQL", "before/after"):
        if fragment not in persistence_text:
            fail(failures, f"{action_id} acceptanceContract.persistenceRequirement missing required fragment: {fragment}")

    require_contract_list(
        contract,
        "apiEvidenceRequired",
        {"HTTP method", "URL", "request payload", "response status"},
        failures,
        action_id,
    )
    require_contract_list(contract, "minimumPassConditions", expectation["minimum_fragments"], failures, action_id)
    require_contract_list(
        contract,
        "recheckCommands",
        {"basic-config-action-matrix-check", "production-testcase-check"},
        failures,
        action_id,
    )

    rollback_text = joined_list_text(contract.get("rollbackPlan"))
    if not rollback_text:
        fail(failures, f"{action_id} acceptanceContract.rollbackPlan must be a non-empty list")
    elif "Verify" not in rollback_text or not any(fragment in rollback_text for fragment in ("Restore", "revert", "回滚")):
        fail(failures, f"{action_id} acceptanceContract.rollbackPlan must include restore/revert and verification")
    if not as_list(contract.get("currentGaps")):
        fail(failures, f"{action_id} acceptanceContract.currentGaps must be a non-empty list while action remains CONTROLLED_MARKER_REQUIRED")


def check_matrix(matrix: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "repoCommit",
        "database",
        "module",
        "goalId",
        "overallStatus",
        "summary",
        "actions",
    ):
        if key not in matrix:
            fail(failures, f"action matrix missing top-level key: {key}")
    if matrix.get("database") != "PostgreSQL":
        fail(failures, "action matrix database must remain PostgreSQL")
    if matrix.get("module") != "basic-config":
        fail(failures, "action matrix module must be basic-config")
    if matrix.get("goalId") != "G-012":
        fail(failures, "action matrix goalId must be G-012")
    if matrix.get("overallStatus") == "PASS":
        fail(failures, "action matrix must stay non-PASS while guarded/planned actions remain")

    actions_raw = matrix.get("actions")
    if not isinstance(actions_raw, list):
        fail(failures, "actions must be a list")
        return
    actions = [action for action in actions_raw if isinstance(action, dict)]
    if len(actions) != len(actions_raw):
        fail(failures, "actions must contain only objects")

    actions_by_id = action_by_id(matrix)
    missing = sorted(REQUIRED_ACTION_IDS - set(actions_by_id))
    extra = sorted(set(actions_by_id) - REQUIRED_ACTION_IDS)
    if missing:
        fail(failures, "action matrix missing actions: " + ", ".join(missing))
    if extra:
        fail(failures, "action matrix contains unknown actions: " + ", ".join(extra))
    if len(actions_by_id) != len(actions):
        fail(failures, "action matrix action ids must be unique")

    check_summary(matrix, actions, failures)
    for action in actions:
        check_action_shape(action, failures)
    check_coverage_alignment(matrix, failures)
    check_builtin_alignment(actions_by_id, failures)
    check_runtime_alignment(actions_by_id, failures)
    check_custom_property_acceptance(actions_by_id, failures)
    check_entity_model_persistence_acceptance(actions_by_id, failures)
    check_entity_model_physical_table_acceptance(actions_by_id, failures)
    check_entity_model_field_acceptance(actions_by_id, failures)
    production_statuses = production_case_statuses(failures)
    for action_id in (
        "builtin-qcs-runtime-config",
        "builtin-rm-runtime-config",
        "builtin-baseset-runtime-config",
    ):
        action = actions_by_id.get(action_id)
        if action:
            check_controlled_runtime_config_contract(action, production_statuses, failures)
    for action_id in ("entity-model-create", "entity-model-edit", "entity-model-delete"):
        action = actions_by_id.get(action_id)
        if action:
            check_entity_model_acceptance_contract(action, failures)


def main() -> int:
    failures: list[str] = []
    check_secret_hygiene(failures)
    check_doc(failures)
    matrix = read_json(MATRIX_PATH, failures, "basic config action matrix")
    if matrix:
        check_matrix(matrix, failures)
    if failures:
        print(f"Basic config action matrix verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Basic config action matrix verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
