#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/wom-toolbar-action-coverage.json"
DOC_PATH = ROOT / "docs/wom-toolbar-action-coverage.md"
PRODUCTION_MATRIX_PATH = ROOT / "metadata/production-module-test-cases.json"
ROW_SMOKE_PATH = ROOT / "metadata/wom-toolbar-row-smoke.json"
STATIC_SCRIPT_REQUIREMENTS = {
    "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body.js": [
        "WOM.custom.randon1575958246066",
        "ec.common.tableNo",
        "accessibleWindows",
        "installWindowVisibleTextFallback",
        "translateMessageArg",
        "showMessageWithWomBodyFallback",
        "installToolbarFallbacks",
        "installEmptySearchSelectFallback",
        "blockEmptySearchSelectEvent",
        "data-adp-wom-empty-search-select",
        "当前无可选筛选字段，默认查询全部",
        "生产过程追溯服务未部署或暂不可用！",
        "二维码生成页面未部署或暂不可用！",
    ],
    "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body-es5.js": [
        "WOM.custom.randon1575958246066",
        "ec.common.tableNo",
        "accessibleWindows",
        "installWindowVisibleTextFallback",
        "translateMessageArg",
        "showMessageWithWomBodyFallback",
        "installToolbarFallbacks",
        "installEmptySearchSelectFallback",
        "blockEmptySearchSelectEvent",
        "data-adp-wom-empty-search-select",
        "当前无可选筛选字段，默认查询全部",
        "生产过程追溯服务未部署或暂不可用！",
        "二维码生成页面未部署或暂不可用！",
    ],
    "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/i18n-value.js": [
        "WOM.custom.randon1575958246066",
        "ec.common.tableNo",
        "accessibleWindows",
        "installWindowVisibleTextFallback",
        "translateMessageArg",
        "showMessageWithWomFallback",
        "installFallbacks",
        "__ADP_WOM_MAKETASKLIST_DOM_TRANSLATE__",
    ],
}

EXPECTED_ACTIONS = {
    "start": "开始",
    "hold": "保持",
    "restart": "重启",
    "stop": "结束",
    "advance-release": "提前放料",
    "manufacturing-inspection": "请检",
    "process-traceability": "生产过程追溯",
    "generate-qrcode": "生成二维码",
}

ALLOWED_STATUSES = {"PASS", "BLOCKED", "NOT_VERIFIED", "NOT_APPLICABLE"}
ALLOWED_CLICK_EVIDENCE = {
    "NORMAL_MOUSE_CLICK",
    "PAGE_CONTEXT_RUNTIME_EVENT",
    "DEPENDENCY_BLOCKED",
    "NOT_VERIFIED",
    "NOT_APPLICABLE",
}
REQUIRED_TOP_LEVEL_KEYS = {
    "schemaVersion",
    "reportKind",
    "generatedAt",
    "database",
    "module",
    "route",
    "sourceReports",
    "runtimeButtonEvidence",
    "summary",
    "actions",
}
REQUIRED_ACTION_KEYS = {
    "id",
    "label",
    "buttonCode",
    "apiEndpoints",
    "requiresPersistence",
    "acceptanceStatus",
    "clickEvidence",
    "productionCaseIds",
    "targetTables",
    "marker",
    "verificationSql",
    "sourceEvidenceRefs",
    "externalEvidenceRefs",
    "recheckCommands",
    "issues",
}


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
        fail(failures, f"missing JSON: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as exc:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {exc}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{path.relative_to(ROOT)} must be a JSON object")
        return {}
    return data


def check_repo_refs(owner: str, refs: list[Any], failures: list[str]) -> None:
    if not refs:
        fail(failures, f"{owner} must include sourceEvidenceRefs")
        return
    for ref in refs:
        if not isinstance(ref, str) or not ref.strip():
            fail(failures, f"{owner} sourceEvidenceRefs must contain non-empty strings")
            continue
        path = Path(ref)
        if path.is_absolute() or ".." in path.parts:
            fail(failures, f"{owner} sourceEvidenceRefs must stay inside repository: {ref}")
            continue
        if not (ROOT / path).exists():
            fail(failures, f"{owner} sourceEvidenceRef does not exist: {ref}")


def production_cases_by_id(matrix: dict[str, Any], failures: list[str]) -> dict[str, dict[str, Any]]:
    cases = matrix.get("cases")
    if not isinstance(cases, list):
        fail(failures, "production module matrix must contain cases list")
        return {}
    result: dict[str, dict[str, Any]] = {}
    for index, case in enumerate(cases):
        if not isinstance(case, dict):
            fail(failures, f"production cases[{index}] must be an object")
            continue
        case_id = str(case.get("id", "")).strip()
        if not case_id:
            fail(failures, f"production cases[{index}] missing id")
            continue
        result[case_id] = case
    return result


def check_action(
    action: Any,
    index: int,
    production_cases: dict[str, dict[str, Any]],
    failures: list[str],
) -> dict[str, Any] | None:
    if not isinstance(action, dict):
        fail(failures, f"actions[{index}] must be an object")
        return None

    for key in REQUIRED_ACTION_KEYS:
        if key not in action:
            fail(failures, f"actions[{index}] missing required key: {key}")

    action_id = str(action.get("id", "")).strip()
    if action_id not in EXPECTED_ACTIONS:
        fail(failures, f"unexpected toolbar action id: {action_id!r}")
    elif action.get("label") != EXPECTED_ACTIONS[action_id]:
        fail(failures, f"{action_id} label must be {EXPECTED_ACTIONS[action_id]!r}")

    status = action.get("acceptanceStatus")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{action_id} has invalid acceptanceStatus: {status!r}")

    click_evidence = action.get("clickEvidence")
    if click_evidence not in ALLOWED_CLICK_EVIDENCE:
        fail(failures, f"{action_id} has invalid clickEvidence: {click_evidence!r}")

    if not isinstance(action.get("requiresPersistence"), bool):
        fail(failures, f"{action_id}.requiresPersistence must be boolean")

    for key in ("apiEndpoints", "productionCaseIds", "targetTables", "sourceEvidenceRefs", "externalEvidenceRefs", "recheckCommands", "issues"):
        if not isinstance(action.get(key), list):
            fail(failures, f"{action_id}.{key} must be a list")

    if not as_list(action.get("apiEndpoints")):
        fail(failures, f"{action_id} must list the endpoint(s) it exercises or expects")
    if not as_list(action.get("recheckCommands")):
        fail(failures, f"{action_id} must include recheckCommands")

    check_repo_refs(action_id, as_list(action.get("sourceEvidenceRefs")), failures)

    case_ids = [str(case_id) for case_id in as_list(action.get("productionCaseIds"))]
    for case_id in case_ids:
        case = production_cases.get(case_id)
        if case is None:
            fail(failures, f"{action_id} references missing production case {case_id}")
            continue
        case_status = case.get("acceptanceStatus")
        if status == "PASS" and case_status != "PASS":
            fail(failures, f"{action_id} PASS references non-PASS production case {case_id}: {case_status}")
        if status == "BLOCKED" and case_status != "BLOCKED":
            fail(failures, f"{action_id} BLOCKED references non-BLOCKED production case {case_id}: {case_status}")

    if status == "PASS":
        if action.get("requiresPersistence") is True:
            if not as_list(action.get("targetTables")):
                fail(failures, f"{action_id} persisted PASS must include targetTables")
            if not str(action.get("marker", "")).strip():
                fail(failures, f"{action_id} persisted PASS must include a marker")
            if not str(action.get("verificationSql", "")).strip():
                fail(failures, f"{action_id} persisted PASS must include verificationSql")
        if click_evidence not in {"NORMAL_MOUSE_CLICK", "PAGE_CONTEXT_RUNTIME_EVENT"}:
            fail(failures, f"{action_id} PASS must include concrete frontend click/page-context evidence")

    if status in {"BLOCKED", "NOT_VERIFIED"}:
        if not as_list(action.get("issues")):
            fail(failures, f"{action_id} {status} must include issues")
        if action.get("requiresPersistence") is True and status == "NOT_VERIFIED" and str(action.get("verificationSql", "")).strip():
            fail(failures, f"{action_id} NOT_VERIFIED must not include verificationSql as if persistence were proven")

    if action_id == "generate-qrcode" and status == "PASS":
        text = json.dumps(action, ensure_ascii=False).lower()
        if "generateqrcode" not in text or "backfill-printinfo" not in text:
            fail(failures, "generate-qrcode PASS must prove generateQrCode and backfill-printInfo or explicitly split print-backfill as NOT_APPLICABLE")

    return action


def check_doc(actions: list[dict[str, Any]], failures: list[str]) -> None:
    try:
        text = DOC_PATH.read_text(encoding="utf-8")
    except FileNotFoundError:
        fail(failures, f"missing document: {DOC_PATH.relative_to(ROOT)}")
        return
    for fragment in (
        "# WOM 工具栏动作覆盖账本",
        "make wom-toolbar-action-coverage-check",
        "| 动作 | API | 验收状态 | 点击证据 | 生产用例 | 落库/后端结论 | 问题 |",
        "生成二维码",
        "生产过程追溯",
    ):
        if fragment not in text:
            fail(failures, f"coverage document missing required text: {fragment}")
    for action in actions:
        label = str(action.get("label", ""))
        status = str(action.get("acceptanceStatus", ""))
        if label and label not in text:
            fail(failures, f"coverage document missing action label: {label}")
        if status and status not in text:
            fail(failures, f"coverage document missing status: {status}")


def check_known_blocker(
    row_smoke: dict[str, Any],
    key: str,
    expected_message: str,
    failures: list[str],
) -> None:
    blocker = as_dict(as_dict(row_smoke.get("knownBlockers")).get(key))
    if not blocker:
        fail(failures, f"row smoke knownBlockers.{key} must be present")
        return
    for flag in ("observed", "guardedWithoutRequest", "messageObserved"):
        if blocker.get(flag) is not True:
            fail(failures, f"row smoke knownBlockers.{key}.{flag} must be true")
    if blocker.get("dependencyStatus") != "BLOCKED":
        fail(failures, f"row smoke knownBlockers.{key}.dependencyStatus must remain BLOCKED")
    if blocker.get("message") != expected_message:
        fail(failures, f"row smoke knownBlockers.{key}.message must be {expected_message!r}")


def check_row_smoke_alignment(
    report: dict[str, Any],
    actions: list[dict[str, Any]],
    row_smoke: dict[str, Any],
    failures: list[str],
) -> None:
    if row_smoke.get("reportKind") != "wom-toolbar-row-smoke":
        fail(failures, "row smoke reportKind must be wom-toolbar-row-smoke")
    if row_smoke.get("database") != "PostgreSQL":
        fail(failures, "row smoke database must remain PostgreSQL")
    if row_smoke.get("status") != "PASS_WITH_KNOWN_BLOCKERS":
        fail(failures, "row smoke status must be PASS_WITH_KNOWN_BLOCKERS")

    marker = str(row_smoke.get("marker", "")).strip()
    task_id = str(row_smoke.get("taskId", "")).strip()
    generated_at = row_smoke.get("generatedAt")
    if not marker:
        fail(failures, "row smoke marker must be present")
    if not task_id:
        fail(failures, "row smoke taskId must be present")

    runtime_evidence = as_dict(report.get("runtimeButtonEvidence"))
    expected_runtime_values = {
        "latestToolbarRowSmokeStatus": row_smoke.get("status"),
        "latestToolbarRowSmokeGeneratedAt": generated_at,
        "latestToolbarRowSmokeMarker": marker,
        "latestToolbarRowSmokeTaskId": task_id,
        "latestToolbarRowSmokeProcessAnalysisStatus": "GUARDED_WITHOUT_REQUEST",
        "latestToolbarRowSmokeQrStatus": "GUARDED_WITHOUT_REQUEST",
    }
    for key, expected in expected_runtime_values.items():
        if runtime_evidence.get(key) != expected:
            fail(failures, f"runtimeButtonEvidence.{key}={runtime_evidence.get(key)!r} must match latest row smoke value {expected!r}")

    filter_dropdown = as_dict(as_dict(row_smoke.get("operations")).get("filterDropdown"))
    filter_has_real_options = (
        filter_dropdown.get("clicked") is True
        and filter_dropdown.get("dropdownVisible") is True
        and filter_dropdown.get("hasOptions") is True
    )
    filter_empty_fallback = (
        filter_dropdown.get("clicked") is True
        and filter_dropdown.get("emptySelectFallbackApplied") is True
        and filter_dropdown.get("displayText") == "全部"
        and filter_dropdown.get("dropdownVisible") is False
    )
    if not (filter_has_real_options or filter_empty_fallback):
        fail(
            failures,
            "row smoke must prove the left filter dropdown either has real options or falls back to 全部 without opening an empty menu",
        )

    i18n = as_dict(row_smoke.get("i18n"))
    if i18n.get("hasRawWomCustom") is not False or i18n.get("hasTableNoKey") is not False:
        fail(failures, "row smoke must prove raw WOM/custom table i18n keys are not visible")

    check_known_blocker(row_smoke, "processAnalysis", "生产过程追溯服务未部署或暂不可用！", failures)
    check_known_blocker(row_smoke, "qrcode", "二维码生成页面未部署或暂不可用！", failures)

    persistence = as_dict(row_smoke.get("persistence"))
    if task_id and task_id not in json.dumps(persistence, ensure_ascii=False):
        fail(failures, "row smoke persistence evidence must include the latest taskId")
    if "WOM_runState/runing" not in json.dumps(persistence, ensure_ascii=False):
        fail(failures, "row smoke persistence evidence must prove final running state")

    row_ref = str(ROW_SMOKE_PATH.relative_to(ROOT))
    for action in actions:
        action_id = str(action.get("id", "")).strip()
        if row_ref not in as_list(action.get("externalEvidenceRefs")):
            fail(failures, f"{action_id} externalEvidenceRefs must include {row_ref}")
        action_text = json.dumps(action, ensure_ascii=False)
        if marker and marker not in action_text:
            fail(failures, f"{action_id} must mention latest row smoke marker {marker}")

    actions_by_id = {str(action.get("id")): action for action in actions}
    for action_id in ("start", "hold", "restart"):
        action = actions_by_id.get(action_id)
        if not action:
            continue
        if action.get("marker") != marker:
            fail(failures, f"{action_id}.marker must match latest row smoke marker")
        if task_id and task_id not in str(action.get("verificationSql", "")):
            fail(failures, f"{action_id}.verificationSql must query latest row smoke taskId")


def check_static_script_guards(failures: list[str]) -> None:
    for relative_path, snippets in STATIC_SCRIPT_REQUIREMENTS.items():
        path = ROOT / relative_path
        try:
            text = path.read_text(encoding="utf-8")
        except FileNotFoundError:
            fail(failures, f"missing WOM toolbar static script: {relative_path}")
            continue
        for snippet in snippets:
            if snippet not in text:
                fail(failures, f"{relative_path} missing fallback snippet: {snippet}")


def check_report(
    report: dict[str, Any],
    production_cases: dict[str, dict[str, Any]],
    row_smoke: dict[str, Any],
    failures: list[str],
) -> None:
    missing = sorted(REQUIRED_TOP_LEVEL_KEYS - set(report))
    if missing:
        fail(failures, "coverage report missing top-level keys: " + ", ".join(missing))

    if report.get("schemaVersion") != 1:
        fail(failures, "schemaVersion must be 1")
    if report.get("reportKind") != "wom-toolbar-action-coverage":
        fail(failures, "reportKind must be wom-toolbar-action-coverage")
    if report.get("database") != "PostgreSQL":
        fail(failures, "database must remain PostgreSQL")
    if report.get("module") != "WOM":
        fail(failures, "module must be WOM")

    for source_report in (
        "metadata/production-module-test-cases.json",
        "metadata/persistence-acceptance.json",
        "metadata/wom-toolbar-row-smoke.json",
    ):
        if source_report not in as_list(report.get("sourceReports")):
            fail(failures, f"sourceReports missing {source_report}")

    runtime_evidence = as_dict(report.get("runtimeButtonEvidence"))
    for key in ("runtimeSql", "toolbarInteractionCompatSql", "i18nStaticOverlay"):
        value = runtime_evidence.get(key)
        if not isinstance(value, str) or not value:
            fail(failures, f"runtimeButtonEvidence.{key} must be present")
        else:
            check_repo_refs(f"runtimeButtonEvidence.{key}", [value], failures)

    raw_actions = report.get("actions")
    if not isinstance(raw_actions, list):
        fail(failures, "actions must be a list")
        return

    actions: list[dict[str, Any]] = []
    for index, raw_action in enumerate(raw_actions):
        action = check_action(raw_action, index, production_cases, failures)
        if action is not None:
            actions.append(action)

    seen_ids = {str(action.get("id")) for action in actions}
    if seen_ids != set(EXPECTED_ACTIONS):
        fail(failures, f"toolbar action set mismatch: expected={sorted(EXPECTED_ACTIONS)} actual={sorted(seen_ids)}")

    status_counts = Counter(str(action.get("acceptanceStatus")) for action in actions)
    direct_click_pass = sum(
        1
        for action in actions
        if action.get("acceptanceStatus") == "PASS" and action.get("clickEvidence") == "NORMAL_MOUSE_CLICK"
    )
    page_context_pass = sum(
        1
        for action in actions
        if action.get("acceptanceStatus") == "PASS" and action.get("clickEvidence") == "PAGE_CONTEXT_RUNTIME_EVENT"
    )
    requires_persistence = sum(1 for action in actions if action.get("requiresPersistence") is True)
    persistence_pass = sum(
        1
        for action in actions
        if action.get("requiresPersistence") is True and action.get("acceptanceStatus") == "PASS"
    )

    expected_summary = {
        "totalActions": len(actions),
        "pass": status_counts["PASS"],
        "blocked": status_counts["BLOCKED"],
        "notVerified": status_counts["NOT_VERIFIED"],
        "notApplicable": status_counts["NOT_APPLICABLE"],
        "requiresPersistence": requires_persistence,
        "persistencePass": persistence_pass,
        "directNormalClickPass": direct_click_pass,
        "pageContextRuntimeEventPass": page_context_pass,
    }
    summary = as_dict(report.get("summary"))
    for key, expected in expected_summary.items():
        if summary.get(key) != expected:
            fail(failures, f"summary.{key} expected {expected}, got {summary.get(key)!r}")

    check_row_smoke_alignment(report, actions, row_smoke, failures)
    check_static_script_guards(failures)
    check_doc(actions, failures)


def main() -> int:
    failures: list[str] = []
    report = read_json(REPORT_PATH, failures)
    matrix = read_json(PRODUCTION_MATRIX_PATH, failures)
    row_smoke = read_json(ROW_SMOKE_PATH, failures)
    if report and matrix and row_smoke:
        check_report(report, production_cases_by_id(matrix, failures), row_smoke, failures)

    if failures:
        print(f"WOM toolbar action coverage verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("WOM toolbar action coverage verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
