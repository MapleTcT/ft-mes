#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPORT_PATH = ROOT / "metadata/production-export-readiness-smoke.json"
EXPECTED_BASE_URL = "http://100.99.133.43:18080"
EXPECTED_BROWSER_BASE_URL = "http://222.88.185.146:18080"
LEGACY_DIRECT_BROWSER_BASE_URL = "http://100.99.133.43:18080"
EXPECTED_BROWSER_BASE_URLS = {
    EXPECTED_BROWSER_BASE_URL,
    LEGACY_DIRECT_BROWSER_BASE_URL,
}

EXPECTED_TARGETS = {
    "wom-make-task",
    "rm-batch-formula",
    "wts-work-permit",
    "qcs-inspect-report",
    "qcs-unqualified-deal",
    "qcs-inspect-release",
}

EXPECTED_SOURCE_AUDIT_CLASSIFICATIONS = {
    "NO_TARGET_EXPORT_SOURCE_FOUND",
    "DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY",
    "TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF",
}

DOC_REQUIREMENTS = {
    "docs/production-module-functional-test-cases.md": [
        "metadata/production-export-readiness-smoke.json",
        "make smoke-production-export-readiness",
        EXPECTED_BASE_URL,
        EXPECTED_BROWSER_BASE_URL,
    ],
    "docs/backend-table-audit/business-production.md": [
        "metadata/production-export-readiness-smoke.json",
        "downloadXls",
    ],
    "docs/production-module-blockers.md": [
        "make smoke-production-export-readiness",
        "PROD-023",
        EXPECTED_BROWSER_BASE_URL,
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


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def read_json(report_path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with report_path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing production export readiness report: {report_path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {report_path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, "production export readiness report must be a JSON object")
        return {}
    return data


def check_secret_hygiene(report_path: Path, failures: list[str]) -> None:
    text = report_path.read_text(encoding="utf-8")
    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in text:
            fail(failures, f"production export readiness report contains forbidden secret literal: {literal}")
    if SECRET_ASSIGNMENT_RE.search(text):
        fail(failures, "production export readiness report appears to contain a non-placeholder secret assignment")


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


def check_page(target_id: str, page: Any, expected_browser_base_urls: set[str], failures: list[str]) -> None:
    if not isinstance(page, dict):
        fail(failures, f"{target_id}.page must be an object")
        return
    for key in (
        "url",
        "navigationStatus",
        "navigationError",
        "visibleError",
        "consoleErrors",
        "pageErrors",
        "requestFailures",
        "networkErrors",
        "exportLabels",
        "downloadLikeLabels",
        "exportClick",
        "ok",
    ):
        if key not in page:
            fail(failures, f"{target_id}.page missing {key}")
    if not isinstance(page.get("ok"), bool):
        fail(failures, f"{target_id}.page.ok must be boolean")
    page_url = str(page.get("url", ""))
    if not any(page_url.startswith(f"{base_url}/msService/") for base_url in expected_browser_base_urls):
        allowed = ", ".join(sorted(expected_browser_base_urls))
        fail(failures, f"{target_id}.page.url must use an allowed browser base URL: {allowed}")
    if page.get("navigationStatus") is not None and not isinstance(page.get("navigationStatus"), int):
        fail(failures, f"{target_id}.page.navigationStatus must be integer or null")
    for key in ("consoleErrors", "pageErrors", "requestFailures", "networkErrors", "exportLabels", "downloadLikeLabels"):
        if not isinstance(page.get(key), list):
            fail(failures, f"{target_id}.page.{key} must be a list")
    check_export_click(target_id, page.get("exportClick"), failures)


def check_export_click(target_id: str, export_click: Any, failures: list[str]) -> None:
    if not isinstance(export_click, dict):
        fail(failures, f"{target_id}.page.exportClick must be an object")
        return
    for key in (
        "attempted",
        "clickedSelector",
        "clickedLabel",
        "status",
        "error",
        "file",
        "bodySize",
        "magic",
        "verifiedDataExport",
        "issue",
    ):
        if key not in export_click:
            fail(failures, f"{target_id}.page.exportClick missing {key}")
    if not isinstance(export_click.get("attempted"), bool):
        fail(failures, f"{target_id}.page.exportClick.attempted must be boolean")
    if not isinstance(export_click.get("status"), str) or not export_click.get("status"):
        fail(failures, f"{target_id}.page.exportClick.status must be a non-empty string")
    if not isinstance(export_click.get("bodySize"), int) or export_click.get("bodySize", -1) < 0:
        fail(failures, f"{target_id}.page.exportClick.bodySize must be a non-negative integer")
    if export_click.get("magic") not in {"EMPTY", "OLE_XLS", "ZIP_XLSX", "HTML", "JSON", "UNKNOWN_BINARY"}:
        fail(failures, f"{target_id}.page.exportClick.magic has invalid value: {export_click.get('magic')!r}")
    if not isinstance(export_click.get("verifiedDataExport"), bool):
        fail(failures, f"{target_id}.page.exportClick.verifiedDataExport must be boolean")
    file_evidence = export_click.get("file")
    if file_evidence is not None:
        if not isinstance(file_evidence, dict):
            fail(failures, f"{target_id}.page.exportClick.file must be an object or null")
        else:
            for key in ("source", "bodySize", "magic", "verifiedDataExport"):
                if key not in file_evidence:
                    fail(failures, f"{target_id}.page.exportClick.file missing {key}")
            if file_evidence.get("verifiedDataExport") != export_click.get("verifiedDataExport"):
                fail(failures, f"{target_id}.page.exportClick.file.verifiedDataExport must match page.exportClick")
    if export_click.get("verifiedDataExport") is False and not export_click.get("issue"):
        fail(failures, f"{target_id}.page.exportClick.issue must explain unverified browser click export")


def check_layout(target_id: str, layout: Any, failures: list[str]) -> None:
    if not isinstance(layout, dict):
        fail(failures, f"{target_id}.layout must be an object")
        return
    for key in ("method", "path", "status", "hasPayload", "exportButtonCandidates", "downloadXlsFields", "buttonTexts"):
        if key not in layout:
            fail(failures, f"{target_id}.layout missing {key}")
    if layout.get("method") != "GET":
        fail(failures, f"{target_id}.layout.method must be GET")
    if not str(layout.get("path", "")).startswith("/msService/baseService/view/layoutJson"):
        fail(failures, f"{target_id}.layout.path must be layoutJson")
    if not isinstance(layout.get("status"), int):
        fail(failures, f"{target_id}.layout.status must be integer")
    if not isinstance(layout.get("hasPayload"), bool):
        fail(failures, f"{target_id}.layout.hasPayload must be boolean")
    for key in ("exportButtonCandidates", "downloadXlsFields", "buttonTexts"):
        if not isinstance(layout.get(key), list):
            fail(failures, f"{target_id}.layout.{key} must be a list")


def check_download(target_id: str, download: Any, failures: list[str]) -> None:
    if not isinstance(download, dict):
        fail(failures, f"{target_id}.download must be an object")
        return
    for key in (
        "method",
        "path",
        "status",
        "contentType",
        "contentDisposition",
        "bodySize",
        "first16Hex",
        "magic",
        "verification",
        "verifiedDataExport",
        "issue",
    ):
        if key not in download:
            fail(failures, f"{target_id}.download missing {key}")
    if download.get("method") != "GET":
        fail(failures, f"{target_id}.download.method must be GET")
    if "/downloadXls" not in str(download.get("path", "")):
        fail(failures, f"{target_id}.download.path must probe downloadXls")
    if not isinstance(download.get("status"), int):
        fail(failures, f"{target_id}.download.status must be integer")
    if not isinstance(download.get("bodySize"), int) or download.get("bodySize", -1) < 0:
        fail(failures, f"{target_id}.download.bodySize must be a non-negative integer")
    if download.get("magic") not in {"EMPTY", "OLE_XLS", "ZIP_XLSX", "HTML", "JSON", "UNKNOWN_BINARY"}:
        fail(failures, f"{target_id}.download.magic has invalid value: {download.get('magic')!r}")
    if not isinstance(download.get("verifiedDataExport"), bool):
        fail(failures, f"{target_id}.download.verifiedDataExport must be boolean")
    verification = download.get("verification")
    if not isinstance(verification, dict):
        fail(failures, f"{target_id}.download.verification must be an object")
    else:
        if not isinstance(verification.get("verifiedDataExport"), bool):
            fail(failures, f"{target_id}.download.verification.verifiedDataExport must be boolean")
        elif verification.get("verifiedDataExport") != download.get("verifiedDataExport"):
            fail(failures, f"{target_id}.download.verifiedDataExport must match download.verification.verifiedDataExport")
        checks = verification.get("checks")
        if not isinstance(checks, list) or len(checks) < 5:
            fail(failures, f"{target_id}.download.verification.checks must contain the export acceptance checks")
        else:
            seen_checks: set[str] = set()
            for check in checks:
                if not isinstance(check, dict):
                    fail(failures, f"{target_id}.download.verification.checks entries must be objects")
                    continue
                name = str(check.get("name", "")).strip()
                if not name:
                    fail(failures, f"{target_id}.download.verification.checks entry missing name")
                seen_checks.add(name)
                if not isinstance(check.get("passed"), bool):
                    fail(failures, f"{target_id}.download.verification.checks.{name}.passed must be boolean")
                if not isinstance(check.get("evidence"), list):
                    fail(failures, f"{target_id}.download.verification.checks.{name}.evidence must be a list")
            required_checks = {
                "visibleExportAction",
                "runtimeExportAction",
                "targetExportSourceHook",
                "browserExportClick",
                "successfulFileResponse",
                "nonEmptyWorkbook",
                "backendQueryExport",
            }
            missing_checks = sorted(required_checks - seen_checks)
            if missing_checks:
                fail(failures, f"{target_id}.download.verification.checks missing {missing_checks}")
        if not isinstance(verification.get("failureReasons"), list):
            fail(failures, f"{target_id}.download.verification.failureReasons must be a list")
        if download.get("verifiedDataExport") is False and not verification.get("failureReasons"):
            fail(failures, f"{target_id}.download.verification.failureReasons must explain unverified export")


def check_query_export(target_id: str, query_export: Any, failures: list[str]) -> None:
    if not isinstance(query_export, dict):
        fail(failures, f"{target_id}.queryExport must be an object")
        return
    for key in (
        "method",
        "path",
        "requestPayload",
        "status",
        "contentType",
        "contentDisposition",
        "bodySize",
        "first16Hex",
        "magic",
        "jsonCode",
        "jsonMessage",
        "textSnippet",
        "verifiedDataExport",
        "issue",
    ):
        if key not in query_export:
            fail(failures, f"{target_id}.queryExport missing {key}")
    if query_export.get("method") != "POST":
        fail(failures, f"{target_id}.queryExport.method must be POST")
    if not str(query_export.get("path", "")).endswith("-query"):
        fail(failures, f"{target_id}.queryExport.path must probe the list -query endpoint")
    if not isinstance(query_export.get("requestPayload"), dict):
        fail(failures, f"{target_id}.queryExport.requestPayload must be an object")
    elif query_export.get("requestPayload", {}).get("exportFlag") is not True:
        fail(failures, f"{target_id}.queryExport.requestPayload.exportFlag must be true")
    if not isinstance(query_export.get("status"), int):
        fail(failures, f"{target_id}.queryExport.status must be integer")
    if not isinstance(query_export.get("bodySize"), int) or query_export.get("bodySize", -1) < 0:
        fail(failures, f"{target_id}.queryExport.bodySize must be a non-negative integer")
    if query_export.get("magic") not in {"EMPTY", "OLE_XLS", "ZIP_XLSX", "HTML", "JSON", "UNKNOWN_BINARY"}:
        fail(failures, f"{target_id}.queryExport.magic has invalid value: {query_export.get('magic')!r}")
    if not isinstance(query_export.get("verifiedDataExport"), bool):
        fail(failures, f"{target_id}.queryExport.verifiedDataExport must be boolean")
    if query_export.get("verifiedDataExport") is False and not query_export.get("issue"):
        fail(failures, f"{target_id}.queryExport.issue must explain unverified query export")
    if query_export.get("verifiedDataExport") is True and query_export.get("magic") not in {"OLE_XLS", "ZIP_XLSX"}:
        fail(failures, f"{target_id}.queryExport verified export must be an XLS/XLSX workbook")


def check_source_match(target_id: str, key: str, match: Any, failures: list[str]) -> None:
    if not isinstance(match, dict):
        fail(failures, f"{target_id}.sourceAudit.{key} entries must be objects")
        return
    for required_key in ("path", "line", "matched", "snippet"):
        if required_key not in match:
            fail(failures, f"{target_id}.sourceAudit.{key} entry missing {required_key}")
    if not str(match.get("path", "")).strip():
        fail(failures, f"{target_id}.sourceAudit.{key} entry path must be present")
    if not isinstance(match.get("line"), int) or match.get("line", 0) <= 0:
        fail(failures, f"{target_id}.sourceAudit.{key} entry line must be a positive integer")
    if not isinstance(match.get("matched"), list) or not match.get("matched"):
        fail(failures, f"{target_id}.sourceAudit.{key} entry matched must be a non-empty list")
    if not str(match.get("snippet", "")).strip():
        fail(failures, f"{target_id}.sourceAudit.{key} entry snippet must be present")


def check_source_audit(target_id: str, source_audit: Any, download: Any, failures: list[str]) -> None:
    if not isinstance(source_audit, dict):
        fail(failures, f"{target_id}.sourceAudit must be an object")
        return
    for key in (
        "searchedRoots",
        "sourceFiles",
        "downloadTemplateMatches",
        "targetExportMatches",
        "genericExportFrameworkAvailable",
        "genericExportFrameworkMatches",
        "classification",
        "issues",
    ):
        if key not in source_audit:
            fail(failures, f"{target_id}.sourceAudit missing {key}")
    if not isinstance(source_audit.get("searchedRoots"), list) or not source_audit.get("searchedRoots"):
        fail(failures, f"{target_id}.sourceAudit.searchedRoots must be a non-empty list")
    if not isinstance(source_audit.get("sourceFiles"), list) or not source_audit.get("sourceFiles"):
        fail(failures, f"{target_id}.sourceAudit.sourceFiles must be a non-empty list")
    for source_file in as_list(source_audit.get("sourceFiles")):
        if not isinstance(source_file, dict):
            fail(failures, f"{target_id}.sourceAudit.sourceFiles entries must be objects")
            continue
        if not str(source_file.get("path", "")).strip():
            fail(failures, f"{target_id}.sourceAudit.sourceFiles entry path must be present")
        if not isinstance(source_file.get("exists"), bool):
            fail(failures, f"{target_id}.sourceAudit.sourceFiles entry exists must be boolean")
    for key in ("downloadTemplateMatches", "targetExportMatches", "genericExportFrameworkMatches"):
        if not isinstance(source_audit.get(key), list):
            fail(failures, f"{target_id}.sourceAudit.{key} must be a list")
            continue
        for match in as_list(source_audit.get(key)):
            check_source_match(target_id, key, match, failures)
    if not isinstance(source_audit.get("genericExportFrameworkAvailable"), bool):
        fail(failures, f"{target_id}.sourceAudit.genericExportFrameworkAvailable must be boolean")
    if source_audit.get("genericExportFrameworkAvailable") is not True:
        fail(failures, f"{target_id}.sourceAudit must prove the generic export framework exists")
    classification = source_audit.get("classification")
    if classification not in EXPECTED_SOURCE_AUDIT_CLASSIFICATIONS:
        fail(failures, f"{target_id}.sourceAudit.classification has invalid value: {classification!r}")
    if not isinstance(source_audit.get("issues"), list):
        fail(failures, f"{target_id}.sourceAudit.issues must be a list")
    if classification != "TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF" and not as_list(source_audit.get("issues")):
        fail(failures, f"{target_id}.sourceAudit must explain why no target export source is accepted")
    if classification == "TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF" and not as_list(source_audit.get("targetExportMatches")):
        fail(failures, f"{target_id}.sourceAudit target export classification requires targetExportMatches")
    if classification != "TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF" and as_list(source_audit.get("targetExportMatches")):
        fail(failures, f"{target_id}.sourceAudit has targetExportMatches but non-target-export classification")
    if int(as_dict(download).get("bodySize") or 0) > 0 and not as_list(source_audit.get("downloadTemplateMatches")):
        fail(failures, f"{target_id}.sourceAudit must classify non-empty downloadXls evidence with target source/runtime matches")


def check_acceptance_contract(target_id: str, contract: Any, item: dict[str, Any], failures: list[str]) -> None:
    if not isinstance(contract, dict):
        fail(failures, f"{target_id}.acceptanceContract must be an object")
        return
    for key in (
        "targetId",
        "persistence",
        "requiredFrontendRuntime",
        "requiredBackend",
        "requiredSourceEvidence",
        "minimumPassConditions",
        "currentGaps",
        "recheckCommand",
    ):
        if key not in contract:
            fail(failures, f"{target_id}.acceptanceContract missing {key}")
    if contract.get("targetId") != target_id:
        fail(failures, f"{target_id}.acceptanceContract.targetId must match item id")
    if "NOT_APPLICABLE" not in str(contract.get("persistence", "")):
        fail(failures, f"{target_id}.acceptanceContract.persistence must explain export persistence is NOT_APPLICABLE")
    required_text = " ".join(
        str(contract.get(key, ""))
        for key in ("requiredFrontendRuntime", "requiredBackend", "requiredSourceEvidence", "recheckCommand")
    ).lower()
    for fragment in ("export", "download", "source", "smoke-production-export-readiness"):
        if fragment not in required_text:
            fail(failures, f"{target_id}.acceptanceContract must mention {fragment}")
    conditions = contract.get("minimumPassConditions")
    if not isinstance(conditions, list) or len(conditions) < 7:
        fail(failures, f"{target_id}.acceptanceContract.minimumPassConditions must be a detailed list")
    else:
        joined_conditions = " ".join(str(value) for value in conditions)
        for fragment in (
            "page.exportLabels",
            "page.exportClick.verifiedDataExport",
            "layout.exportButtonCandidates",
            "sourceAudit.classification",
            "bodySize",
            "queryExport.verifiedDataExport",
            "download.verifiedDataExport",
        ):
            if fragment not in joined_conditions:
                fail(failures, f"{target_id}.acceptanceContract.minimumPassConditions missing {fragment}")
    current_gaps = contract.get("currentGaps")
    if not isinstance(current_gaps, list):
        fail(failures, f"{target_id}.acceptanceContract.currentGaps must be a list")
    elif item.get("status") != "READY" and not current_gaps:
        fail(failures, f"{target_id}.acceptanceContract.currentGaps must explain non-READY targets")


def check_item(index: int, item: Any, failures: list[str]) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"items[{index}] must be an object")
        return None
    target_id = str(item.get("id", "")).strip()
    if not target_id:
        fail(failures, f"items[{index}] missing id")
        return None
    if target_id not in EXPECTED_TARGETS:
        fail(failures, f"unknown production export target id: {target_id}")

    for key in ("label", "route", "viewCode", "page", "layout", "download", "queryExport", "sourceAudit", "acceptanceContract", "status", "conclusion", "issues"):
        if key not in item:
            fail(failures, f"{target_id} missing required key: {key}")
    if item.get("status") not in {"READY", "ACTION_REQUIRED", "BLOCKED"}:
        fail(failures, f"{target_id}.status has invalid value: {item.get('status')!r}")
    if not str(item.get("route", "")).startswith("/msService/"):
        fail(failures, f"{target_id}.route must be an /msService path")
    if not str(item.get("viewCode", "")).strip():
        fail(failures, f"{target_id}.viewCode must be present")
    if not isinstance(item.get("issues"), list):
        fail(failures, f"{target_id}.issues must be a list")
    if item.get("status") == "BLOCKED" and not as_list(item.get("issues")):
        fail(failures, f"{target_id} BLOCKED must include issues")
    if "NOT_APPLICABLE" not in str(item.get("conclusion", "")):
        fail(failures, f"{target_id}.conclusion must state persistence is NOT_APPLICABLE")

    check_page(target_id, item.get("page"), EXPECTED_BROWSER_BASE_URLS, failures)
    check_layout(target_id, item.get("layout"), failures)
    check_download(target_id, item.get("download"), failures)
    check_query_export(target_id, item.get("queryExport"), failures)
    check_source_audit(target_id, item.get("sourceAudit"), item.get("download"), failures)
    check_acceptance_contract(target_id, item.get("acceptanceContract"), item, failures)
    return target_id


def check_summary(data: dict[str, Any], failures: list[str]) -> None:
    items = [item for item in as_list(data.get("items")) if isinstance(item, dict)]
    summary = as_dict(data.get("summary"))
    expected = {
        "targets": len(items),
        "pagePass": sum(1 for item in items if as_dict(item.get("page")).get("ok") is True),
        "visibleExportActions": sum(1 for item in items if as_list(as_dict(item.get("page")).get("exportLabels"))),
        "runtimeExportActions": sum(1 for item in items if as_list(as_dict(item.get("layout")).get("exportButtonCandidates"))),
        "nonEmptyDownloads": sum(1 for item in items if int(as_dict(item.get("download")).get("bodySize") or 0) > 0),
        "backendQueryExportWorkbooks": sum(1 for item in items if as_dict(item.get("queryExport")).get("verifiedDataExport") is True),
        "backendQueryExportErrors": sum(
            1
            for item in items
            if as_dict(item.get("queryExport")).get("status") in {0}
            or int(as_dict(item.get("queryExport")).get("status") or 0) >= 500
        ),
        "verifiedDataExports": sum(1 for item in items if as_dict(item.get("download")).get("verifiedDataExport") is True),
        "ready": sum(1 for item in items if item.get("status") == "READY"),
        "actionRequired": sum(1 for item in items if item.get("status") == "ACTION_REQUIRED"),
        "blocked": sum(1 for item in items if item.get("status") == "BLOCKED"),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")
    computed_status = "READY" if expected["verifiedDataExports"] == len(items) and items else "BLOCKED"
    if summary.get("status") != computed_status:
        fail(failures, f"summary.status={summary.get('status')!r} does not match expected {computed_status}")


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "repoCommit",
        "database",
        "module",
        "caseId",
        "status",
        "apiBaseUrl",
        "browserBaseUrl",
        "target",
        "summary",
        "items",
        "evidence",
    ):
        if key not in data:
            fail(failures, f"report missing required top-level key: {key}")
    if data.get("database") != "PostgreSQL":
        fail(failures, "production export readiness report database must remain PostgreSQL")
    if data.get("module") != "production":
        fail(failures, "production export readiness report must identify module=production")
    if data.get("caseId") != "PROD-023":
        fail(failures, "production export readiness report must be tied to PROD-023")
    repo_commit = data.get("repoCommit")
    if not isinstance(repo_commit, str) or len(repo_commit.strip()) < 7:
        fail(failures, "repoCommit must identify the code baseline used for export readiness tracking")

    target = as_dict(data.get("target"))
    for key in ("baseUrl", "browserBaseUrl", "username"):
        if not str(target.get(key, "")).strip():
            fail(failures, f"target.{key} must be present")
    if target.get("baseUrl") != EXPECTED_BASE_URL:
        fail(failures, f"target.baseUrl must be {EXPECTED_BASE_URL}")
    if target.get("browserBaseUrl") not in EXPECTED_BROWSER_BASE_URLS:
        allowed = ", ".join(sorted(EXPECTED_BROWSER_BASE_URLS))
        fail(failures, f"target.browserBaseUrl must be one of: {allowed}")
    if data.get("apiBaseUrl") != target.get("baseUrl"):
        fail(failures, "top-level apiBaseUrl must match target.baseUrl")
    if data.get("browserBaseUrl") != target.get("browserBaseUrl"):
        fail(failures, "top-level browserBaseUrl must match target.browserBaseUrl")

    items = data.get("items")
    if not isinstance(items, list):
        fail(failures, "items must be a list")
        return
    seen: set[str] = set()
    for index, item in enumerate(items):
        target_id = check_item(index, item, failures)
        if not target_id:
            continue
        if target_id in seen:
            fail(failures, f"duplicate production export target id: {target_id}")
        seen.add(target_id)
    missing = sorted(EXPECTED_TARGETS - seen)
    if missing:
        fail(failures, "production export readiness report missing targets: " + ", ".join(missing))
    check_summary(data, failures)
    if data.get("status") != as_dict(data.get("summary")).get("status"):
        fail(failures, "top-level status must match summary.status")

    evidence = as_dict(data.get("evidence"))
    if "real browser" not in str(evidence.get("method", "")).lower():
        fail(failures, "evidence.method must mention real browser probing")
    if "source" not in str(evidence.get("method", "")).lower():
        fail(failures, "evidence.method must mention source/runtime audit")
    if "NOT_APPLICABLE" not in str(evidence.get("persistence", "")):
        fail(failures, "evidence.persistence must explain export persistence is NOT_APPLICABLE")
    if "generic export framework" not in str(evidence.get("sourceAudit", "")).lower():
        fail(failures, "evidence.sourceAudit must explain generic export framework is not enough")
    generic_export_framework = as_dict(evidence.get("genericExportFramework"))
    if generic_export_framework.get("available") is not True:
        fail(failures, "evidence.genericExportFramework.available must be true")
    if not isinstance(generic_export_framework.get("matches"), list) or not generic_export_framework.get("matches"):
        fail(failures, "evidence.genericExportFramework.matches must contain source matches")
    if "not written" not in str(evidence.get("secretHandling", "")):
        fail(failures, "evidence.secretHandling must state that credentials are not written")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate ADP production export readiness smoke assets.")
    parser.add_argument(
        "--report",
        default=str(DEFAULT_REPORT_PATH.relative_to(ROOT)),
        help="Path to metadata/production-export-readiness-smoke.json",
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
        print(f"Production export readiness verification failed with {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Production export readiness verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
