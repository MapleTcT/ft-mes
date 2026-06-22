#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPORT = ROOT / "metadata/entity-model-config-crud-readiness-probe.json"
DOC_PATH = ROOT / "docs/basic-config-action-matrix.md"
MATRIX_PATH = ROOT / "metadata/basic-config-action-matrix.json"
PERSISTENCE_ACCEPTANCE_PATH = ROOT / "metadata/entity-model-config-crud-persistence-acceptance.json"

FORBIDDEN_TEXT = {
    "ft" + "123456789",
    "Authorization: Bearer",
    "access_token",
    "refresh_token",
    "AKIA",
}

REQUIRED_ITEMS = {
    "entity/model create": {
        "marker": "ADP_E2E_*_EC_CREATE",
        "tables": {"ec_entity", "ec_model", "ec_field", "ec_view"},
        "backend": {"EntityController.save", "ModelController.save"},
    },
    "entity/model edit": {
        "marker": "ADP_E2E_*_EC_EDIT",
        "tables": {"ec_entity", "ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view"},
        "backend": {"EntityController.save", "ModelController.save"},
    },
    "entity/model delete or disable": {
        "marker": "ADP_E2E_*_EC_DELETE",
        "tables": {"ec_entity", "ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view", "runtime_button"},
        "backend": {"EntityController.deleteChoise", "ModelController.deleteChoise"},
    },
}

REQUIRED_BACKEND_TRACE_FRAGMENTS = {
    "/msService/ec/entity/save",
    "/msService/ec/entity/ordinaryDelete",
    "/msService/ec/model/save",
    "/msService/ec/model/ordinaryDelete",
    "/msService/ec/model/formatTableName",
    "EntityService.saveEntity",
    "ModelService.saveModel",
    "Hibernate",
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


def check_secret_hygiene(path: Path, failures: list[str]) -> None:
    if not path.exists():
        return
    text = path.read_text(encoding="utf-8")
    for forbidden in FORBIDDEN_TEXT:
        if forbidden in text:
            fail(failures, f"{path.relative_to(ROOT)} contains forbidden secret-like text: {forbidden}")


def check_doc_and_matrix(report_path: Path, failures: list[str]) -> None:
    rel_report = str(report_path.relative_to(ROOT))
    if not DOC_PATH.exists():
        fail(failures, "missing docs/basic-config-action-matrix.md")
    else:
        text = DOC_PATH.read_text(encoding="utf-8")
        for fragment in (
            rel_report,
            "make entity-model-config-crud-readiness-check",
            "NOT_VERIFIED",
            "READINESS_ONLY",
        ):
            if fragment not in text:
                fail(failures, f"basic config action matrix doc missing required fragment: {fragment}")

    matrix = read_json(MATRIX_PATH, failures, "basic config action matrix")
    if not matrix:
        return
    actions = {
        str(action.get("id")): action
        for action in as_list(matrix.get("actions"))
        if isinstance(action, dict) and action.get("id")
    }
    entity_action_ids = ("entity-model-create", "entity-model-edit", "entity-model-delete")
    statuses = {action_id: actions.get(action_id, {}).get("status") for action_id in entity_action_ids}
    pass_count = sum(1 for status in statuses.values() if status == "PASS")
    if pass_count and pass_count != len(entity_action_ids):
        fail(failures, "entity/model matrix actions must be all PASS or all PLANNED")
    if pass_count == len(entity_action_ids):
        acceptance = read_json(PERSISTENCE_ACCEPTANCE_PATH, failures, "entity/model CRUD persistence acceptance report")
        if acceptance:
            if acceptance.get("status") != "PASS":
                fail(failures, "entity/model CRUD persistence acceptance must PASS before matrix actions can PASS")
            if acceptance.get("database") != "PostgreSQL":
                fail(failures, "entity/model CRUD persistence acceptance database must remain PostgreSQL")
            marker = str(acceptance.get("marker", ""))
            if not marker.startswith("ADP_E2E_") or "ENTITY_MODEL" not in marker:
                fail(failures, "entity/model CRUD persistence marker must be ADP_E2E_*_ENTITY_MODEL")
            summary = acceptance.get("summary")
            if not isinstance(summary, dict) or summary.get("fail") != 0 or summary.get("checks") != summary.get("pass"):
                fail(failures, "entity/model CRUD persistence acceptance must pass all checks")
    for action_id in ("entity-model-create", "entity-model-edit", "entity-model-delete"):
        action = actions.get(action_id)
        if not action:
            fail(failures, f"action matrix missing {action_id}")
            continue
        if pass_count == len(entity_action_ids):
            if action.get("status") != "PASS":
                fail(failures, f"{action_id} must be PASS when persistence acceptance exists")
            refs = set(str(ref) for ref in as_list(action.get("evidenceRefs")))
            if "metadata/entity-model-config-crud-persistence-acceptance.json" not in refs:
                fail(failures, f"{action_id} evidenceRefs must include persistence acceptance report")
        elif action.get("status") != "PLANNED":
            fail(failures, f"{action_id} must stay PLANNED until mutation acceptance exists")
        refs = set(str(ref) for ref in as_list(action.get("evidenceRefs")))
        if rel_report not in refs:
            fail(failures, f"{action_id} evidenceRefs must include {rel_report}")


def check_report_shape(report: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "repoCommit",
        "database",
        "module",
        "goalId",
        "areaId",
        "status",
        "acceptanceConclusion",
        "mutationAttempted",
        "summary",
        "sample",
        "checks",
        "apiResults",
        "browserResults",
        "databaseEvidence",
        "backendTrace",
        "items",
        "safeguards",
    ):
        if key not in report:
            fail(failures, f"report missing top-level key: {key}")
    if report.get("database") != "PostgreSQL":
        fail(failures, "report database must remain PostgreSQL")
    if report.get("module") != "basic-config":
        fail(failures, "report module must be basic-config")
    if report.get("goalId") != "G-012":
        fail(failures, "report goalId must be G-012")
    if report.get("areaId") != "configuration-entity-runtime":
        fail(failures, "report areaId must be configuration-entity-runtime")
    if report.get("status") != "READINESS_ONLY":
        fail(failures, "report status must be READINESS_ONLY, not PASS")
    if report.get("acceptanceConclusion") != "NOT_VERIFIED":
        fail(failures, "report acceptanceConclusion must be NOT_VERIFIED")
    if report.get("mutationAttempted") is not False:
        fail(failures, "report mutationAttempted must be false")
    if not isinstance(report.get("summary"), dict):
        fail(failures, "report summary must be an object")


def check_readiness_checks(report: dict[str, Any], failures: list[str]) -> None:
    checks = as_list(report.get("checks"))
    if len(checks) < 4:
        fail(failures, "readiness report must include at least four checks")
    failed = [check for check in checks if isinstance(check, dict) and check.get("status") != "PASS"]
    if failed:
        fail(failures, "all readiness checks must PASS before this report can guard CRUD planning")

    api_results = as_list(report.get("apiResults"))
    browser_results = as_list(report.get("browserResults"))
    if len(api_results) < 7:
        fail(failures, "readiness report must include entity/model API probes")
    if len(browser_results) < 3:
        fail(failures, "readiness report must include browser page probes")
    for result in api_results + browser_results:
        if not isinstance(result, dict):
            fail(failures, "apiResults/browserResults entries must be objects")
            continue
        if result.get("ok") is not True:
            fail(failures, f"readiness probe result must be ok=true: {result.get('name')}")


def check_database_evidence(report: dict[str, Any], failures: list[str]) -> None:
    evidence = report.get("databaseEvidence")
    if not isinstance(evidence, dict):
        fail(failures, "databaseEvidence must be an object")
        return
    sample = evidence.get("sample")
    if not isinstance(sample, dict):
        fail(failures, "databaseEvidence.sample must identify an existing entity/model")
    else:
        for key in ("moduleCode", "entityCode", "modelCode", "tableName"):
            if not str(sample.get(key, "")).strip():
                fail(failures, f"databaseEvidence.sample missing {key}")

    counts = {
        str(row.get("name")): int(row.get("count") or 0)
        for row in as_list(evidence.get("counts"))
        if isinstance(row, dict) and row.get("name")
    }
    for table in ("ec_module", "ec_entity", "ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view", "runtime_button"):
        if counts.get(table, 0) <= 0:
            fail(failures, f"databaseEvidence count must be >0 for {table}")
    if counts.get("marker_ec_entity", 0) != 0 or counts.get("marker_ec_model", 0) != 0:
        fail(failures, "readiness probe must not leave ADP_E2E entity/model marker rows")

    sql_text = json.dumps(evidence.get("verificationSql", {}), ensure_ascii=False)
    for table in ("ec_entity", "ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view", "runtime_button"):
        if table not in sql_text:
            fail(failures, f"databaseEvidence.verificationSql missing {table}")
    for marker in ("ADP_E2E_%_EC_CREATE", "ADP_E2E_%_EC_EDIT", "ADP_E2E_%_EC_DELETE"):
        if marker not in sql_text:
            fail(failures, f"databaseEvidence.verificationSql missing future marker pattern {marker}")


def check_backend_trace(report: dict[str, Any], failures: list[str]) -> None:
    trace = report.get("backendTrace")
    if not isinstance(trace, dict):
        fail(failures, "backendTrace must be an object")
        return
    trace_text = json.dumps(trace, ensure_ascii=False)
    for fragment in sorted(REQUIRED_BACKEND_TRACE_FRAGMENTS):
        if fragment not in trace_text:
            fail(failures, f"backendTrace missing {fragment}")


def check_items(report: dict[str, Any], failures: list[str]) -> None:
    items = as_list(report.get("items"))
    by_operation = {
        str(item.get("operation")): item
        for item in items
        if isinstance(item, dict) and item.get("operation")
    }
    missing = sorted(set(REQUIRED_ITEMS) - set(by_operation))
    if missing:
        fail(failures, "readiness report missing CRUD items: " + ", ".join(missing))
    if len(items) != len(REQUIRED_ITEMS):
        fail(failures, "readiness report must contain exactly the three entity/model CRUD items")

    for operation, expectation in REQUIRED_ITEMS.items():
        item = by_operation.get(operation)
        if not isinstance(item, dict):
            continue
        if item.get("status") != "NOT_VERIFIED":
            fail(failures, f"{operation} status must be NOT_VERIFIED")
        if item.get("requiresPersistence") is not True:
            fail(failures, f"{operation} requiresPersistence must be true")
        if item.get("mutationAttempted") is not False:
            fail(failures, f"{operation} mutationAttempted must be false")
        if item.get("markerPattern") != expectation["marker"]:
            fail(failures, f"{operation} markerPattern must be {expectation['marker']}")
        tables = set(str(table) for table in as_list(item.get("tables")))
        missing_tables = sorted(expectation["tables"] - tables)
        if missing_tables:
            fail(failures, f"{operation} tables missing: {', '.join(missing_tables)}")
        backend_text = str(item.get("backendEntry", ""))
        for fragment in sorted(expectation["backend"]):
            if fragment not in backend_text:
                fail(failures, f"{operation} backendEntry missing {fragment}")
        if "ADP_E2E" not in str(item.get("verificationSql", "")):
            fail(failures, f"{operation} verificationSql must include ADP_E2E marker lookup")
        if not as_list(item.get("issues")):
            fail(failures, f"{operation} NOT_VERIFIED item must include issues")


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate entity/model config CRUD readiness probe assets.")
    parser.add_argument("--report", default=str(DEFAULT_REPORT), help="Path to entity/model readiness report JSON.")
    args = parser.parse_args()

    report_path = Path(args.report)
    if not report_path.is_absolute():
        report_path = ROOT / report_path

    failures: list[str] = []
    for path in (report_path, DOC_PATH, MATRIX_PATH):
        check_secret_hygiene(path, failures)
    report = read_json(report_path, failures, "entity/model config CRUD readiness probe")
    if report:
        check_report_shape(report, failures)
        check_readiness_checks(report, failures)
        check_database_evidence(report, failures)
        check_backend_trace(report, failures)
        check_items(report, failures)
    check_doc_and_matrix(report_path, failures)

    if failures:
        print(f"Entity/model config CRUD readiness verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Entity/model config CRUD readiness verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
