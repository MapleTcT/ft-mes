#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
SOURCE_REPORT = ROOT / "metadata/production-export-readiness-smoke.json"
JSON_PATH = ROOT / "metadata/production-export-gap-breakdown.json"
DOC_PATH = ROOT / "docs/production-export-gap-breakdown.md"


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def read_json(path: Path) -> dict[str, Any]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        raise SystemExit(f"missing source report: {path.relative_to(ROOT)}")
    if not isinstance(data, dict):
        raise SystemExit(f"source report must be a JSON object: {path.relative_to(ROOT)}")
    return data


def bool_from_count(values: list[Any]) -> bool:
    return len(values) > 0


def classify_fix(item: dict[str, Any]) -> str:
    page = as_dict(item.get("page"))
    layout = as_dict(item.get("layout"))
    download = as_dict(item.get("download"))
    query_export = as_dict(item.get("queryExport"))
    source_audit = as_dict(item.get("sourceAudit"))
    classification = str(source_audit.get("classification", "UNKNOWN"))

    if item.get("status") == "READY":
        return "READY_NO_FIX_REQUIRED"
    if page.get("ok") is not True:
        return "PAGE_RUNTIME_BLOCKER"
    if as_dict(page.get("exportClick")).get("verifiedDataExport") is not True and (
        bool_from_count(as_list(layout.get("exportButtonCandidates"))) or bool_from_count(as_list(page.get("exportLabels")))
    ):
        return "PROVE_BROWSER_EXPORT_CLICK_FILE_RESPONSE"
    if query_export.get("status") in {0} or int(query_export.get("status") or 0) >= 500:
        return "FIX_BACKEND_QUERY_EXPORT_ERROR_AND_EXPOSE_FRONTEND_ACTION"
    if query_export.get("magic") == "JSON":
        return "ENABLE_BACKEND_QUERY_EXPORT_FILE_MODE_AND_FRONTEND_ACTION"
    if classification == "NO_TARGET_EXPORT_SOURCE_FOUND":
        return "ADD_TARGET_EXPORT_HOOK_RUNTIME_BUTTON_AND_BACKEND"
    if classification == "DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY":
        if int(download.get("bodySize") or 0) > 0:
            return "DISTINGUISH_IMPORT_TEMPLATE_FROM_LIST_DATA_EXPORT"
        return "ENABLE_TARGET_EXPORT_ACTION_AND_LIST_DATA_FILE"
    if bool_from_count(as_list(layout.get("exportButtonCandidates"))) or bool_from_count(as_list(page.get("exportLabels"))):
        return "PROVE_BROWSER_FILE_RESPONSE_AND_SOURCE_HOOK"
    return "EXPORT_ACCEPTANCE_GAP"


def summarize_item(item: dict[str, Any]) -> dict[str, Any]:
    page = as_dict(item.get("page"))
    export_click = as_dict(page.get("exportClick"))
    layout = as_dict(item.get("layout"))
    download = as_dict(item.get("download"))
    query_export = as_dict(item.get("queryExport"))
    source_audit = as_dict(item.get("sourceAudit"))
    contract = as_dict(item.get("acceptanceContract"))
    verification = as_dict(download.get("verification"))

    return {
        "id": item.get("id"),
        "label": item.get("label"),
        "route": item.get("route"),
        "viewCode": item.get("viewCode"),
        "status": item.get("status"),
        "pageOk": page.get("ok") is True,
        "navigationStatus": page.get("navigationStatus"),
        "visibleExportAction": bool_from_count(as_list(page.get("exportLabels"))),
        "visibleExportLabels": as_list(page.get("exportLabels")),
        "browserExportClickStatus": export_click.get("status"),
        "browserExportClickVerified": export_click.get("verifiedDataExport") is True,
        "browserExportClickBodySize": int(export_click.get("bodySize") or 0),
        "browserExportClickMagic": export_click.get("magic"),
        "browserExportClickIssue": export_click.get("issue"),
        "runtimeExportAction": bool_from_count(as_list(layout.get("exportButtonCandidates"))),
        "runtimeExportCandidates": as_list(layout.get("exportButtonCandidates")),
        "layoutHasPayload": layout.get("hasPayload") is True,
        "downloadStatus": download.get("status"),
        "downloadBodySize": int(download.get("bodySize") or 0),
        "downloadMagic": download.get("magic"),
        "queryExportStatus": query_export.get("status"),
        "queryExportBodySize": int(query_export.get("bodySize") or 0),
        "queryExportMagic": query_export.get("magic"),
        "queryExportVerifiedDataExport": query_export.get("verifiedDataExport") is True,
        "queryExportIssue": query_export.get("issue"),
        "verifiedDataExport": download.get("verifiedDataExport") is True,
        "sourceClassification": source_audit.get("classification"),
        "sourceTargetExportMatches": len(as_list(source_audit.get("targetExportMatches"))),
        "sourceDownloadTemplateMatches": len(as_list(source_audit.get("downloadTemplateMatches"))),
        "acceptanceFailures": as_list(verification.get("failureReasons")),
        "currentGaps": as_list(contract.get("currentGaps")),
        "fixClass": classify_fix(item),
        "recheckCommand": contract.get("recheckCommand"),
    }


def build_breakdown(report: dict[str, Any]) -> dict[str, Any]:
    items = [summarize_item(item) for item in as_list(report.get("items")) if isinstance(item, dict)]
    status_counts = Counter(str(item.get("status") or "UNKNOWN") for item in items)
    source_counts = Counter(str(item.get("sourceClassification") or "UNKNOWN") for item in items)
    fix_counts = Counter(str(item.get("fixClass") or "UNKNOWN") for item in items)
    current_gap_counts: Counter[str] = Counter()
    for item in items:
        for gap in as_list(item.get("currentGaps")):
            current_gap_counts[str(gap)] += 1

    return {
        "schemaVersion": 1,
        "generatedAt": report.get("generatedAt"),
        "sourceReport": "metadata/production-export-readiness-smoke.json",
        "sourceReportGeneratedAt": report.get("generatedAt"),
        "repoCommit": report.get("repoCommit"),
        "database": "PostgreSQL",
        "module": "production",
        "caseId": "PROD-023",
        "summary": {
            "status": as_dict(report.get("summary")).get("status"),
            "targets": len(items),
            "statusCounts": dict(sorted(status_counts.items())),
            "pagePass": sum(1 for item in items if item.get("pageOk")),
            "visibleExportActions": sum(1 for item in items if item.get("visibleExportAction")),
            "browserExportClicks": sum(1 for item in items if item.get("browserExportClickVerified")),
            "runtimeExportActions": sum(1 for item in items if item.get("runtimeExportAction")),
            "nonEmptyDownloads": sum(1 for item in items if int(item.get("downloadBodySize") or 0) > 0),
            "backendQueryExportWorkbooks": sum(1 for item in items if item.get("queryExportVerifiedDataExport")),
            "backendQueryExportErrors": sum(
                1
                for item in items
                if item.get("queryExportStatus") in {0} or int(item.get("queryExportStatus") or 0) >= 500
            ),
            "verifiedDataExports": sum(1 for item in items if item.get("verifiedDataExport")),
            "sourceClassifications": dict(sorted(source_counts.items())),
            "fixClasses": dict(sorted(fix_counts.items())),
            "currentGapCounts": dict(sorted(current_gap_counts.items())),
        },
        "items": items,
        "acceptanceRule": {
            "persistence": "NOT_APPLICABLE: list export must not create or mutate business rows.",
            "passRequires": [
                "真实浏览器页面存在可见导出动作。",
                "layoutJson runtime 元数据存在目标导出动作。",
                "源码或 runtime 配置存在目标 view/route 绑定的 export hook。",
            "后端返回非空 XLS/XLSX 列表数据文件。",
            "真实浏览器点击导出后收到非空 XLS/XLSX 文件响应。",
            "download.verifiedDataExport=true。",
            ],
            "nonSolutions": [
                "不能把 import/downloadXls 模板当作列表数据导出。",
                "不能只用 HTTP 200 或空文件判断导出可用。",
                "不能只因为平台通用 export 模板存在就把目标页面判为可导出。",
            ],
        },
    }


def md_escape(value: Any) -> str:
    text = str(value if value is not None else "")
    return text.replace("|", "\\|").replace("\n", "<br>")


def status_icon(value: bool) -> str:
    return "YES" if value else "NO"


def render_doc(data: dict[str, Any]) -> str:
    summary = as_dict(data.get("summary"))
    recheck_command = next(
        (
            str(item.get("recheckCommand"))
            for item in as_list(data.get("items"))
            if isinstance(item, dict) and str(item.get("recheckCommand", "")).strip()
        ),
        "make smoke-production-export-readiness ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 PRODUCTION_EXPORT_SMOKE_OUTPUT=metadata/production-export-readiness-smoke.json ADP_PAGE_TIMEOUT_MS=120000 ADP_API_TIMEOUT_MS=30000",
    )
    lines: list[str] = [
        "# 生产导出缺口明细",
        "",
        f"源报告：`{data.get('sourceReport')}`",
        f"源报告时间：`{data.get('sourceReportGeneratedAt')}`",
        f"状态：`{summary.get('status')}`",
        "",
        "本文件由 `scripts/generate-production-export-gap-breakdown.py` 从生产导出 readiness smoke 生成。它拆解 `PROD-023` 的逐目标缺口，并允许单个目标在真实浏览器点击和文件响应闭环后先行 READY；只要仍有未完成目标，PROD-023 总体仍保持 BLOCKED。",
        "",
        "## 摘要",
        "",
        "| 指标 | 数量 |",
        "| --- | ---: |",
        f"| 目标页面 | {summary.get('targets')} |",
        f"| 页面可达 | {summary.get('pagePass')} |",
        f"| 可见导出动作 | {summary.get('visibleExportActions')} |",
        f"| 浏览器点击导出工作簿 | {summary.get('browserExportClicks')} |",
        f"| runtime 导出动作 | {summary.get('runtimeExportActions')} |",
        f"| 非空下载响应 | {summary.get('nonEmptyDownloads')} |",
        f"| 后端 query export workbook | {summary.get('backendQueryExportWorkbooks')} |",
        f"| 后端 query export 错误 | {summary.get('backendQueryExportErrors')} |",
        f"| 已证明列表数据导出 | {summary.get('verifiedDataExports')} |",
        "",
        "## 逐目标缺口",
        "",
        "| 目标 | 状态 | 页面 | 可见导出 | 浏览器点击 | Runtime 导出 | 下载 | Query Export | 源码分类 | 修复分类 | 当前缺口 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in as_list(data.get("items")):
        if not isinstance(item, dict):
            continue
        download = f"{item.get('downloadStatus')} / {item.get('downloadBodySize')} bytes / {item.get('downloadMagic')}"
        browser_click = f"{item.get('browserExportClickStatus')} / {item.get('browserExportClickBodySize')} bytes / {item.get('browserExportClickMagic')}"
        query_export = f"{item.get('queryExportStatus')} / {item.get('queryExportBodySize')} bytes / {item.get('queryExportMagic')}"
        gaps = "<br>".join(md_escape(gap) for gap in as_list(item.get("currentGaps"))) or "-"
        lines.append(
            "| "
            + " | ".join(
                [
                    md_escape(item.get("id")),
                    md_escape(item.get("status")),
                    status_icon(item.get("pageOk") is True),
                    status_icon(item.get("visibleExportAction") is True),
                    md_escape(browser_click),
                    status_icon(item.get("runtimeExportAction") is True),
                    md_escape(download),
                    md_escape(query_export),
                    md_escape(item.get("sourceClassification")),
                    md_escape(item.get("fixClass")),
                    gaps,
                ]
            )
            + " |"
        )

    lines.extend(
        [
            "",
            "## 验收口径",
            "",
            "- 文件导出本身不应该写业务表，落库状态为 `NOT_APPLICABLE`。",
            "- 单目标 PASS 必须同时证明：真实页面有可见导出动作、正常点击导出后收到非空 XLS/XLSX 文件响应、layoutJson 有 runtime 导出动作、源码或 runtime 有目标导出 hook、后端返回非空 XLS/XLSX 列表数据文件。",
            "- `downloadXls` 的导入模板、`*-query exportFlag=true` 返回 JSON/500、空文件、HTTP 200、平台通用 export 模板都不能单独证明生产列表导出可用。",
            "",
            "## 复验命令",
            "",
            "```bash",
            recheck_command,
            "make production-export-gap-breakdown",
            "make production-export-gap-breakdown-check",
            "```",
            "",
        ]
    )
    return "\n".join(lines)


def write_outputs(data: dict[str, Any]) -> None:
    JSON_PATH.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    DOC_PATH.write_text(render_doc(data), encoding="utf-8")


def check_outputs(data: dict[str, Any]) -> int:
    expected_json = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    expected_doc = render_doc(data)
    failures: list[str] = []
    if not JSON_PATH.exists():
        failures.append(f"missing {JSON_PATH.relative_to(ROOT)}")
    elif JSON_PATH.read_text(encoding="utf-8") != expected_json:
        failures.append(f"{JSON_PATH.relative_to(ROOT)} is stale")
    if not DOC_PATH.exists():
        failures.append(f"missing {DOC_PATH.relative_to(ROOT)}")
    elif DOC_PATH.read_text(encoding="utf-8") != expected_doc:
        failures.append(f"{DOC_PATH.relative_to(ROOT)} is stale")
    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1
    print("Production export gap breakdown is current.")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate per-target production export gap breakdown.")
    parser.add_argument("--check", action="store_true", help="Check generated docs/metadata are current")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    report = read_json(SOURCE_REPORT)
    data = build_breakdown(report)
    if args.check:
        return check_outputs(data)
    write_outputs(data)
    print(f"Wrote {JSON_PATH.relative_to(ROOT)}")
    print(f"Wrote {DOC_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
