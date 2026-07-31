#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "metadata/rm-web-formula-editor-acceptance.json"
EDITOR = ROOT / "deploy/docker/assets/module-static/RM/formula/editor.html"
NGINX = ROOT / "deploy/docker/nginx/adp.conf"
ACCEPTANCE = ROOT / "deploy/docker/scripts/adp-rm-web-formula-editor-persistence-acceptance.js"


def main() -> int:
    failures: list[str] = []
    try:
        data = json.loads(REPORT.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        print(f"FAIL: cannot read {REPORT.relative_to(ROOT)}: {error}", file=sys.stderr)
        return 1

    try:
        editor = EDITOR.read_text(encoding="utf-8")
    except OSError as error:
        failures.append(f"cannot read {EDITOR.relative_to(ROOT)}: {error}")
        editor = ""
    for fragment in (
        'productId: idValue("productId")',
        'batchServerId: idValue("batchServerId")',
    ):
        if fragment not in editor:
            failures.append(f"Web editor must preserve unsafe 64-bit identifier strings: {fragment}")
    if 'productId: numberValue(' in editor or 'batchServerId: numberValue(' in editor:
        failures.append("Web editor must not coerce 64-bit reference identifiers through Number")
    for fragment in (
        "function findAccessToken()",
        'options.headers.Authorization = "Bearer " + token;',
    ):
        if fragment not in editor:
            failures.append(f"Web editor must authenticate API calls from the normal browser session: {fragment}")

    try:
        nginx = NGINX.read_text(encoding="utf-8")
        editor_location = nginx.split(
            "location = /msService/RM/formula/editor {", 1
        )[1].split("}", 1)[0]
        api_location = nginx.split(
            "location ^~ /msService/RM/formula-editor/ {", 1
        )[1].split("}", 1)[0]
        if "auth_request" in editor_location:
            failures.append(
                "RM editor HTML navigation must not require an Authorization header"
            )
        if "auth_request /_adp_rm_auth;" not in api_location:
            failures.append("RM editor business APIs must remain protected")
    except (OSError, IndexError) as error:
        failures.append(f"cannot verify RM editor Nginx auth boundary: {error}")

    try:
        acceptance = ACCEPTANCE.read_text(encoding="utf-8")
        if "extraHTTPHeaders" in acceptance:
            failures.append(
                "RM browser acceptance must not hide normal window.open authentication bugs"
            )
    except OSError as error:
        failures.append(f"cannot read {ACCEPTANCE.relative_to(ROOT)}: {error}")

    summary = data.get("summary", {})
    if data.get("schemaVersion") != 2:
        failures.append("schemaVersion must be 2")
    expected_summary = {
        "status": "PASS_WITH_EXTERNAL_DCS_BLOCKED",
        "webEditor": "PASS",
        "api": "PASS",
        "postgresPersistence": "PASS",
        "testAdapterRetry": "PASS",
        "externalBatchDcs": "BLOCKED",
    }
    for key, expected in expected_summary.items():
        if summary.get(key) != expected:
            failures.append(f"summary.{key} must be {expected}")

    marker = str(data.get("marker", ""))
    if not marker.startswith("ADP_E2E_") or "RM_WEB_FORMULA" not in marker:
        failures.append("marker must be a unique ADP_E2E RM_WEB_FORMULA value")

    browser = data.get("browser", {})
    list_evidence = browser.get("batchFormulaList", {})
    if list_evidence.get("navigationStatus") != 200 or list_evidence.get("webEditorButtonVisible") is not True:
        failures.append("batch formula list must show the real Web editor entry")

    for name in ("desktopEditor", "mobileEditor"):
        evidence = browser.get(name, {})
        for key in ("consoleErrors", "pageErrors", "requestFailures"):
            if evidence.get(key) != 0:
                failures.append(f"browser.{name}.{key} must be 0")
        if evidence.get("horizontalOverflow") is not False:
            failures.append(f"browser.{name}.horizontalOverflow must be false")
        screenshot = ROOT / str(evidence.get("screenshot", ""))
        if not screenshot.is_file() or screenshot.stat().st_size < 1000:
            failures.append(f"browser.{name} screenshot missing or empty")

    api = data.get("api", {})
    for name in ("list", "create", "update"):
        evidence = api.get(name, {})
        if evidence.get("httpStatus") != 200 or evidence.get("code") != 200:
            failures.append(f"api.{name} must have HTTP 200 and code 200")
    references = api.get("references", {})
    for name in ("materials", "batchServers"):
        evidence = references.get(name, {})
        if evidence.get("httpStatus") != 200 or evidence.get("code") != 200 or evidence.get("count", 0) < 1:
            failures.append(f"api.references.{name} must return at least one valid master-data item")
    if api.get("create", {}).get("idempotentRetry") is not True:
        failures.append("create idempotent retry must return the existing revision")
    delivery = api.get("delivery", {})
    if delivery.get("firstAttemptState") != "FAILED" or delivery.get("firstAttemptHttpStatus") != 503:
        failures.append("delivery first attempt must prove a recorded failure")
    if delivery.get("retryState") != "ACKNOWLEDGED" or delivery.get("retryHttpStatus") != 200:
        failures.append("delivery retry must prove acknowledgement")
    if delivery.get("adapter") != "TEST_SIMULATOR":
        failures.append("acceptance must identify the isolated adapter as TEST_SIMULATOR")

    postgres = data.get("postgresql", {})
    if postgres.get("formula", {}).get("version", -1) < 2 or postgres.get("formula", {}).get("valid") is not True:
        failures.append("rm_formulas marker/version readback is incomplete")
    if postgres.get("process", {}).get("count") != 1:
        failures.append("rm_formula_processes marker count must be 1")
    if postgres.get("activity", {}).get("count") != 1:
        failures.append("rm_process_actives marker count must be 1")
    if postgres.get("revisions", {}).get("count", 0) < 3:
        failures.append("revision ledger must contain API and browser saves")
    if postgres.get("deliveryAttempts", {}).get("states") != ["FAILED:503", "ACKNOWLEDGED:200"]:
        failures.append("delivery attempt ledger must preserve failed and acknowledged attempts")

    boundary = data.get("scopeBoundary", {})
    if boundary.get("legacyActiveXRequired") is not False or boundary.get("localhost4433Required") is not False:
        failures.append("Web editor acceptance must not depend on ActiveX or localhost:4433")
    if boundary.get("externalBatchDcsStatus") != "BLOCKED" or not str(boundary.get("externalBatchDcsReason", "")).strip():
        failures.append("real external Batch/DCS acceptance must remain explicitly BLOCKED")

    cleanup = data.get("cleanup", {})
    if cleanup.get("executed") is not True:
        failures.append("marker cleanup must execute")
    for table, count in cleanup.get("counts", {}).items():
        if count != 0:
            failures.append(f"cleanup count for {table} must be 0")

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1
    print(
        "PASS: RM Web formula editor browser/API/PostgreSQL/retry evidence is complete; "
        "real external Batch/DCS acceptance remains BLOCKED"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
