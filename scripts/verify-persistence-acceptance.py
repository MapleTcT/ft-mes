#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/persistence-acceptance.json"
NOOP_ANALYSIS_DOC = ROOT / "docs/backend-table-audit/wom-public-produce-task-created-analysis.md"
NOOP_ANALYSIS_JSON = ROOT / "metadata/wom-public-produce-task-created-analysis.json"
BAD_QUANTITY_ANALYSIS_DOC = ROOT / "docs/backend-table-audit/wom-bad-quantity-analysis.md"
BAD_QUANTITY_ANALYSIS_JSON = ROOT / "metadata/wom-bad-quantity-analysis.json"
MATERIAL_ANALYSIS_DOC = ROOT / "docs/backend-table-audit/material-service-dependency-analysis.md"
MATERIAL_ANALYSIS_JSON = ROOT / "metadata/material-service-dependency-analysis.json"

REQUIRED_DOCS = [
    ROOT / "docs/functional-persistence-acceptance.md",
    ROOT / "docs/frontend-functional-test-report.md",
    ROOT / "docs/backend-table-audit/persistence-acceptance.md",
]

FUNCTIONAL_RULE_REQUIRED_PHRASES = [
    "不是继续补治理层",
    "不允许只看代码判断功能是否可用",
    "必须通过浏览器或等效前端 E2E 方式访问页面",
    "PostgreSQL",
    "ADP_E2E_YYYYMMDD_HHMMSS_xxx",
    "metadata/persistence-acceptance.json",
    "不能因为启动失败就跳到写文档结束",
    "业务动作是否真实落库",
    "可交接、可复验、可继续推进",
]

SUMMARY_KEYS = {
    "testedFeatures": "all",
    "pass": "PASS",
    "fail": "FAIL",
    "blocked": "BLOCKED",
    "notApplicable": "NOT_APPLICABLE",
}

ITEM_REQUIRED_KEYS = [
    "module",
    "route",
    "operation",
    "api",
    "method",
    "requiresPersistence",
    "backendEntry",
    "tables",
    "marker",
    "verificationSql",
    "status",
    "evidence",
    "issues",
]

ALLOWED_STATUSES = {"PASS", "FAIL", "BLOCKED", "NOT_APPLICABLE"}


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_json(failures: list[str]) -> dict:
    try:
        with REPORT_PATH.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(f"missing report: {REPORT_PATH.relative_to(ROOT)}", failures)
        return {}
    except json.JSONDecodeError as error:
        fail(f"invalid JSON in {REPORT_PATH.relative_to(ROOT)}: {error}", failures)
        return {}
    if not isinstance(data, dict):
        fail("persistence acceptance report must be a JSON object", failures)
        return {}
    return data


def read_json_path(path: Path, failures: list[str]) -> dict:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(f"missing analysis report: {path.relative_to(ROOT)}", failures)
        return {}
    except json.JSONDecodeError as error:
        fail(f"invalid JSON in {path.relative_to(ROOT)}: {error}", failures)
        return {}
    if not isinstance(data, dict):
        fail(f"analysis report must be a JSON object: {path.relative_to(ROOT)}", failures)
        return {}
    return data


def item_text(item: dict) -> str:
    return json.dumps(item, ensure_ascii=False, sort_keys=True).lower()


def require_refs(index: int, text: str, paths: list[Path], failures: list[str]) -> None:
    for path in paths:
        rel = str(path.relative_to(ROOT)).lower()
        if rel not in text:
            fail(f"items[{index}] unresolved evidence must reference {rel}", failures)


def require_doc_phrases(path: Path, phrases: list[str], failures: list[str]) -> None:
    if not path.exists():
        fail(f"missing analysis document: {path.relative_to(ROOT)}", failures)
        return
    text = path.read_text(encoding="utf-8")
    for phrase in phrases:
        if phrase not in text:
            fail(
                f"{path.relative_to(ROOT)} missing required analysis phrase: {phrase}",
                failures,
            )


def check_public_produce_noop(index: int, item: dict, failures: list[str]) -> None:
    text = item_text(item)
    require_refs(index, text, [NOOP_ANALYSIS_DOC, NOOP_ANALYSIS_JSON], failures)
    require_doc_phrases(
        NOOP_ANALYSIS_DOC,
        [
            "当前不能作为生产工单/制造指令单创建入口验收",
            "但 PostgreSQL",
            "前后 marker 计数均为 `0`",
            "已禁用",
            "禁止用该接口作为任何创建能力的 PASS 证据",
        ],
        failures,
    )

    data = read_json_path(NOOP_ANALYSIS_JSON, failures)
    if not data:
        return
    if data.get("status") != "BLOCKED_EXPLICIT_DISABLED":
        fail("public produceTaskCreated analysis must remain BLOCKED_EXPLICIT_DISABLED", failures)
    if data.get("classification") != "explicit-disabled-pending-product-decision":
        fail("public produceTaskCreated analysis must classify explicit disabled pending product decision", failures)
    if data.get("isPostgresCompatibilityGap") is not False:
        fail("public produceTaskCreated no-op must not be classified as a PostgreSQL gap", failures)
    if "producetaskcreated2" not in str(data.get("acceptedReplacementEndpoint", "")).lower():
        fail("public produceTaskCreated analysis must name produceTaskCreated2 as the accepted replacement", failures)
    if not any(entry.get("id") == "PROD-ACTION-007" for entry in data.get("backlog", []) if isinstance(entry, dict)):
        fail("public produceTaskCreated analysis must keep backlog id PROD-ACTION-007", failures)
    evidence = data.get("evidence", {})
    if isinstance(evidence, dict):
        if evidence.get("httpStatus") != 200:
            fail("public produceTaskCreated disabled evidence must record the gateway HTTP 200 transport", failures)
        if "code=400" not in str(evidence.get("databaseResult", "")):
            fail("public produceTaskCreated disabled evidence must record explicit business code=400", failures)
        if "0" not in str(evidence.get("databaseResult", "")):
            fail("public produceTaskCreated no-op evidence must record unchanged marker count", failures)
        if evidence.get("latestProbeStatus") != "EXPLICIT_REJECTION_NO_PERSISTENCE":
            fail("public produceTaskCreated latest probe must be EXPLICIT_REJECTION_NO_PERSISTENCE", failures)
    else:
        fail("public produceTaskCreated analysis must include evidence object", failures)


def check_bad_quantity_blocker(index: int, item: dict, failures: list[str]) -> None:
    text = item_text(item)
    require_refs(index, text, [BAD_QUANTITY_ANALYSIS_DOC, BAD_QUANTITY_ANALYSIS_JSON], failures)
    require_doc_phrases(
        BAD_QUANTITY_ANALYSIS_DOC,
        [
            "do not expose a standalone bad-quantity registration field",
            "No recovered QCS/WOM table column",
        ],
        failures,
    )
    data = read_json_path(BAD_QUANTITY_ANALYSIS_JSON, failures)
    if data and data.get("status") != "NO_DEDICATED_BAD_QUANTITY_ENTRY_FOUND":
        fail("bad quantity analysis must keep NO_DEDICATED_BAD_QUANTITY_ENTRY_FOUND status", failures)


def check_material_service_blocker(index: int, item: dict, failures: list[str]) -> None:
    text = item_text(item)
    require_refs(index, text, [MATERIAL_ANALYSIS_DOC, MATERIAL_ANALYSIS_JSON], failures)
    require_doc_phrases(
        MATERIAL_ANALYSIS_DOC,
        [
            "called `material` tenant service is not deployed",
            "not a PostgreSQL mapper issue",
            "Nacos aliases have `healthyHostCount=0`",
        ],
        failures,
    )
    data = read_json_path(MATERIAL_ANALYSIS_JSON, failures)
    if not data:
        return
    if data.get("status") != "BLOCKED_MISSING_MATERIAL_SERVICE":
        fail("material service analysis must keep BLOCKED_MISSING_MATERIAL_SERVICE status", failures)
    env = data.get("environment", {})
    if isinstance(env, dict) and env.get("sshHost") != "100.99.133.43":
        fail("material service analysis must reflect the current test host 100.99.133.43", failures)
    if not data.get("sourceDependencies"):
        fail("material service analysis must include sourceDependencies", failures)


def check_visible_create_blocker(index: int, item: dict, failures: list[str]) -> None:
    text = item_text(item)
    for phrase in ("maketasklist", "producetaskcreated2", "runtime_button", "rbac_menuinfo"):
        if phrase not in text:
            fail(f"items[{index}] visible create blocker must include {phrase} evidence", failures)
    if "产品" not in str(item.get("issues", "")) and "product" not in text:
        fail(f"items[{index}] visible create blocker must require a product/UX decision", failures)


def check_unresolved_item(index: int, item: dict, status: str, failures: list[str]) -> None:
    if status not in {"FAIL", "BLOCKED"}:
        return

    text = item_text(item)
    matched = False
    if status in {"FAIL", "BLOCKED"} and "public/wom/producetask/producetask/producetaskcreated" in text:
        check_public_produce_noop(index, item, failures)
        matched = True
    if status == "BLOCKED" and ("可见入口" in text or "visible manual" in text):
        check_visible_create_blocker(index, item, failures)
        matched = True
    if status == "BLOCKED" and ("bad-quantity" in text or "不良数" in text or "坏品" in text):
        check_bad_quantity_blocker(index, item, failures)
        matched = True
    if status == "BLOCKED" and ("material-service" in text or "material 服务" in text or "servicename=material" in text):
        check_material_service_blocker(index, item, failures)
        matched = True

    if not matched:
        fail(
            f"items[{index}] {status} must be tied to a known专项分析/backlog or explicit product blocker",
            failures,
        )


def check_docs(failures: list[str]) -> None:
    for path in REQUIRED_DOCS:
        if not path.exists():
            fail(f"required acceptance document missing: {path.relative_to(ROOT)}", failures)

    functional = ROOT / "docs/functional-persistence-acceptance.md"
    if functional.exists():
        text = functional.read_text(encoding="utf-8")
        for phrase in FUNCTIONAL_RULE_REQUIRED_PHRASES:
            if phrase not in text:
                fail(
                    "functional persistence acceptance rules are missing required phrase: "
                    f"{phrase}",
                    failures,
                )

    frontend = ROOT / "docs/frontend-functional-test-report.md"
    if frontend.exists():
        text = frontend.read_text(encoding="utf-8")
        required = "| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 验收状态 | 问题 |"
        if required not in text:
            fail("frontend functional report is missing the required summary table", failures)

    backend = ROOT / "docs/backend-table-audit/persistence-acceptance.md"
    if backend.exists():
        text = backend.read_text(encoding="utf-8")
        required = "| 业务动作 | 前端入口 | API endpoint | 后端入口 | 目标表 | 验收 SQL | 实际结果 | 状态 |"
        if required not in text:
            fail("backend persistence report is missing the required acceptance table", failures)


def check_item(index: int, item: object, failures: list[str]) -> str | None:
    if not isinstance(item, dict):
        fail(f"items[{index}] must be an object", failures)
        return None

    for key in ITEM_REQUIRED_KEYS:
        if key not in item:
            fail(f"items[{index}] missing required key: {key}", failures)

    status = item.get("status")
    if status not in ALLOWED_STATUSES:
        fail(f"items[{index}] has invalid status: {status!r}", failures)
        return None

    if not isinstance(item.get("requiresPersistence"), bool):
        fail(f"items[{index}].requiresPersistence must be boolean", failures)

    if not isinstance(item.get("tables"), list):
        fail(f"items[{index}].tables must be a list", failures)

    if not isinstance(item.get("issues"), list):
        fail(f"items[{index}].issues must be a list", failures)

    if status == "PASS":
        for key in ("module", "route", "operation", "api", "method", "backendEntry", "evidence"):
            if not str(item.get(key, "")).strip():
                fail(f"items[{index}] PASS must include non-empty {key}", failures)
        if item.get("requiresPersistence"):
            if not item.get("tables"):
                fail(f"items[{index}] persisted PASS must include target tables", failures)
            for key in ("marker", "verificationSql"):
                if not str(item.get(key, "")).strip():
                    fail(f"items[{index}] persisted PASS must include {key}", failures)
        elif not str(item.get("evidence", "")).strip():
            fail(f"items[{index}] non-persistent PASS must explain evidence", failures)

    if status in {"FAIL", "BLOCKED"} and not item.get("issues"):
        fail(f"items[{index}] {status} must include at least one issue", failures)
    if isinstance(status, str):
        check_unresolved_item(index, item, status, failures)

    if status == "NOT_APPLICABLE" and not str(item.get("evidence", "")).strip():
        fail(f"items[{index}] NOT_APPLICABLE must explain why persistence is not required", failures)

    return status


def check_report(data: dict, failures: list[str]) -> None:
    for key in ("generatedAt", "repoCommit", "database", "summary", "items"):
        if key not in data:
            fail(f"report missing required top-level key: {key}", failures)

    if data.get("database") != "PostgreSQL":
        fail("persistence acceptance database must remain PostgreSQL", failures)

    repo_commit = data.get("repoCommit")
    if not isinstance(repo_commit, str) or len(repo_commit.strip()) < 7:
        fail("repoCommit must identify the code baseline used for acceptance", failures)

    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail("summary must be an object", failures)
        summary = {}
    for key in SUMMARY_KEYS:
        if not isinstance(summary.get(key), int) or summary.get(key) < 0:
            fail(f"summary.{key} must be a non-negative integer", failures)

    items = data.get("items")
    if not isinstance(items, list):
        fail("items must be a list", failures)
        return

    statuses: list[str] = []
    for index, item in enumerate(items):
        status = check_item(index, item, failures)
        if status:
            statuses.append(status)

    expected = {
        "testedFeatures": len(items),
        "pass": statuses.count("PASS"),
        "fail": statuses.count("FAIL"),
        "blocked": statuses.count("BLOCKED"),
        "notApplicable": statuses.count("NOT_APPLICABLE"),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(f"summary.{key}={summary.get(key)!r} does not match expected {value}", failures)


def main() -> int:
    failures: list[str] = []
    check_docs(failures)
    data = read_json(failures)
    if data:
        check_report(data, failures)
    if failures:
        print(f"Persistence acceptance verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Persistence acceptance verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
