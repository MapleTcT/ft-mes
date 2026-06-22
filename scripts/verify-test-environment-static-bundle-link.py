#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPORT_PATH = ROOT / "metadata/test-environment-static-bundle-link-smoke.json"
WOM_TOOLBAR_REPORT_PATH = ROOT / "metadata/wom-toolbar-row-smoke.json"
FRONTEND_REPORT_PATH = ROOT / "docs/frontend-functional-test-report.md"
WOM_TOOLBAR_DOC_PATH = ROOT / "docs/wom-toolbar-action-coverage.md"

EXPECTED_DIRECT_BASE = "http://100.99.133.43:18080"
EXPECTED_PUBLIC_BASE = "http://222.88.185.146:18080"
EXPECTED_DIRECT_STATUS = "BLOCKED_BY_DERP_STATIC_BUNDLE_TRANSFER"
EXPECTED_PUBLIC_STATUS = "PASS_WITH_KNOWN_BLOCKERS"
REQUIRED_FAILED_ASSETS = {
    "vendors.echarts.js",
    "vendors.antdicons.js",
    "vendors.sesgis.js",
    "vendors.commons.js",
}


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def read_json(path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing JSON: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{path.relative_to(ROOT)} must be a JSON object")
        return {}
    return data


def read_text(path: Path, failures: list[str]) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        fail(failures, f"missing document: {path.relative_to(ROOT)}")
        return ""


def check_summary(report: dict[str, Any], failures: list[str]) -> None:
    summary = as_dict(report.get("summary"))
    if summary.get("status") != "PARTIAL":
        fail(failures, "summary.status must remain PARTIAL while 100.99 direct browser entry is blocked")
    if summary.get("direct10099") != EXPECTED_DIRECT_STATUS:
        fail(failures, f"summary.direct10099 must be {EXPECTED_DIRECT_STATUS}")
    if summary.get("publicIngress") != EXPECTED_PUBLIC_STATUS:
        fail(failures, f"summary.publicIngress must be {EXPECTED_PUBLIC_STATUS}")

    recommendation = str(summary.get("recommendation", ""))
    for fragment in (EXPECTED_PUBLIC_BASE, "SSH/API/DB", "100.99.133.43"):
        if fragment not in recommendation:
            fail(failures, f"summary.recommendation must mention {fragment!r}")


def check_direct_entry(report: dict[str, Any], failures: list[str]) -> None:
    tailscale = as_dict(report.get("tailscale"))
    if tailscale.get("target") != "100.99.133.43":
        fail(failures, "tailscale.target must be 100.99.133.43")
    observed_path = str(tailscale.get("observedPath", ""))
    if "DERP" not in observed_path or "direct connection not established" not in observed_path:
        fail(failures, "tailscale.observedPath must record DERP relay and missing direct connection")

    direct = as_dict(report.get("direct10099"))
    if direct.get("baseUrl") != EXPECTED_DIRECT_BASE:
        fail(failures, "direct10099.baseUrl must be the current Tailscale test entrypoint")
    if direct.get("browserBaseUrl") != EXPECTED_DIRECT_BASE:
        fail(failures, "direct10099.browserBaseUrl must use 100.99 for the failed direct-entry proof")
    if direct.get("status") != "FAIL":
        fail(failures, "direct10099.status must be FAIL")

    navigation = as_dict(direct.get("navigation"))
    dom_content_loaded = as_dict(navigation.get("domContentLoaded"))
    if dom_content_loaded.get("ok") is not False:
        fail(failures, "direct10099.domContentLoaded.ok must be false for the recorded blocked entry")

    failed_assets: set[str] = set()
    for item in as_list(direct.get("requestFailures")):
        if not isinstance(item, dict):
            fail(failures, "direct10099.requestFailures entries must be objects")
            continue
        if item.get("failure") != "net::ERR_CONTENT_LENGTH_MISMATCH":
            fail(failures, "direct10099 request failure must be net::ERR_CONTENT_LENGTH_MISMATCH")
        url = str(item.get("url", ""))
        for asset in REQUIRED_FAILED_ASSETS:
            if asset in url:
                failed_assets.add(asset)
    missing = sorted(REQUIRED_FAILED_ASSETS - failed_assets)
    if missing:
        fail(failures, "direct10099 missing expected failed static assets: " + ", ".join(missing))


def check_public_ingress(report: dict[str, Any], wom_toolbar: dict[str, Any], failures: list[str]) -> None:
    public = as_dict(report.get("publicIngress"))
    if public.get("baseUrl") != EXPECTED_DIRECT_BASE:
        fail(failures, "publicIngress.baseUrl must keep API/DB base on 100.99")
    if public.get("browserBaseUrl") != EXPECTED_PUBLIC_BASE:
        fail(failures, "publicIngress.browserBaseUrl must be the same-environment public browser ingress")
    if public.get("status") != EXPECTED_PUBLIC_STATUS:
        fail(failures, f"publicIngress.status must be {EXPECTED_PUBLIC_STATUS}")

    marker = str(public.get("marker", ""))
    if not marker.startswith("ADP_E2E_") or "WOMSTART_HOLD_RESTART" not in marker:
        fail(failures, "publicIngress.marker must be a WOMSTART_HOLD_RESTART ADP_E2E marker")
    if public.get("marker") != wom_toolbar.get("marker"):
        fail(failures, "publicIngress.marker must match metadata/wom-toolbar-row-smoke.json")
    if wom_toolbar.get("status") != EXPECTED_PUBLIC_STATUS:
        fail(failures, f"wom-toolbar-row-smoke status must be {EXPECTED_PUBLIC_STATUS}")

    i18n = as_dict(public.get("i18n"))
    if i18n.get("hasRawWomCustom") is not False:
        fail(failures, "publicIngress i18n must prove no raw WOM.custom key is visible")
    if i18n.get("hasTableNoKey") is not False:
        fail(failures, "publicIngress i18n must prove ec.common.tableNo is translated")
    if i18n.get("getTextStart") != "只有【待执行】的指令单可以开始！":
        fail(failures, "publicIngress i18n getTextStart must be the Chinese business warning")

    blockers = as_dict(public.get("knownBlockers"))
    process = as_dict(blockers.get("processAnalysis"))
    qrcode = as_dict(blockers.get("qrcode"))
    toolbar_blockers = as_dict(wom_toolbar.get("knownBlockers"))
    for key, expected_message in (
        ("processAnalysis", "生产过程追溯服务未部署或暂不可用！"),
        ("qrcode", "二维码生成页面未部署或暂不可用！"),
    ):
        blocker = as_dict(blockers.get(key))
        toolbar_blocker = as_dict(toolbar_blockers.get(key))
        for owner, value in (("publicIngress", blocker), ("wom-toolbar-row-smoke", toolbar_blocker)):
            if value.get("observed") is not True:
                fail(failures, f"{owner}.{key} must record the guarded dependency blocker as observed")
            if value.get("guardedWithoutRequest") is not True:
                fail(failures, f"{owner}.{key} must prove the toolbar guard prevented the missing dependency request")
            if value.get("messageObserved") is not True or value.get("message") != expected_message:
                fail(failures, f"{owner}.{key} must record the Chinese guard message {expected_message!r}")
            if value.get("response") is not None:
                fail(failures, f"{owner}.{key} must not keep a current response after the front-end guard intercepts the click")
            if str(value.get("dependencyStatus", "")) != "BLOCKED":
                fail(failures, f"{owner}.{key} must keep the business dependency status as BLOCKED")

    persistence = as_dict(public.get("persistence"))
    task = as_list(persistence.get("task"))
    wait = as_list(persistence.get("wait"))
    toolbar_persistence = as_dict(wom_toolbar.get("persistence"))
    toolbar_task = as_list(toolbar_persistence.get("task"))
    toolbar_wait = as_list(toolbar_persistence.get("wait"))
    if len(task) < 6 or task[3] != "WOM_runState/runing":
        fail(failures, f"publicIngress persistence.task must prove final running state, got {task!r}")
    if len(wait) < 4 or wait[3] != "WOM_runState/runing":
        fail(failures, f"publicIngress persistence.wait must prove final running state, got {wait!r}")
    if toolbar_task:
        if task[:6] != toolbar_task[:6]:
            fail(
                failures,
                "publicIngress persistence.task must match the latest WOM toolbar row smoke task proof, "
                f"got public={task!r}, latest={toolbar_task!r}",
            )
    if toolbar_wait:
        if wait[:4] != toolbar_wait[:4]:
            fail(
                failures,
                "publicIngress persistence.wait must match the latest WOM toolbar row smoke wait proof, "
                f"got public={wait!r}, latest={toolbar_wait!r}",
            )


def check_asset_transfer(report: dict[str, Any], failures: list[str]) -> None:
    evidence = as_list(report.get("assetTransferEvidence"))
    if not evidence:
        fail(failures, "assetTransferEvidence must be present")
        return

    by_host: dict[str, list[dict[str, Any]]] = {}
    for item in evidence:
        if not isinstance(item, dict):
            fail(failures, "assetTransferEvidence entries must be objects")
            continue
        by_host.setdefault(str(item.get("host", "")), []).append(item)

    public_items = by_host.get("222.88.185.146", [])
    direct_items = by_host.get("100.99.133.43", [])
    if len(public_items) < 4:
        fail(failures, "assetTransferEvidence must include public ingress successful downloads")
    if len(direct_items) < 4:
        fail(failures, "assetTransferEvidence must include 100.99 direct download observations")

    for item in public_items:
        if item.get("downloadResult") != "complete":
            fail(failures, f"public ingress asset download must be complete: {item}")
        if not isinstance(item.get("contentLength"), int) or item["contentLength"] <= 0:
            fail(failures, f"public ingress asset must include positive contentLength: {item}")

    partial_direct = [
        item for item in direct_items if str(item.get("downloadResult", "")).startswith("timeout_partial")
    ]
    if not partial_direct:
        fail(failures, "100.99 direct evidence must include at least one timeout_partial static bundle")
    for item in partial_direct:
        received = item.get("receivedBytes")
        content_length = item.get("contentLength")
        if not isinstance(received, int) or not isinstance(content_length, int) or not (0 < received < content_length):
            fail(failures, f"timeout_partial asset must include receivedBytes < contentLength: {item}")


def check_docs(wom_toolbar: dict[str, Any], failures: list[str]) -> None:
    frontend = read_text(FRONTEND_REPORT_PATH, failures)
    toolbar = read_text(WOM_TOOLBAR_DOC_PATH, failures)
    marker = str(wom_toolbar.get("marker", "")).strip()
    if not marker:
        fail(failures, "wom-toolbar-row-smoke must include marker before docs can be checked")
        marker = "ADP_E2E_"
    for owner, text in (
        ("docs/frontend-functional-test-report.md", frontend),
        ("docs/wom-toolbar-action-coverage.md", toolbar),
    ):
        for fragment in (
            "metadata/test-environment-static-bundle-link-smoke.json",
            "DERP",
            "net::ERR_CONTENT_LENGTH_MISMATCH",
            marker,
        ):
            if fragment not in text:
                fail(failures, f"{owner} must document static bundle link evidence fragment: {fragment}")


def check_evidence_files(report: dict[str, Any], failures: list[str]) -> None:
    files = as_dict(report.get("evidenceFiles"))
    for key in ("directReport", "publicReport", "publicScreenshot"):
        value = files.get(key)
        if not isinstance(value, str) or not value.strip():
            fail(failures, f"evidenceFiles.{key} must be a non-empty path")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate browser-entry static bundle link evidence for the current ADP test environment."
    )
    parser.add_argument(
        "--report",
        default=str(DEFAULT_REPORT_PATH),
        help="Path to metadata/test-environment-static-bundle-link-smoke.json.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    report_path = Path(args.report)
    if not report_path.is_absolute():
        report_path = ROOT / report_path

    failures: list[str] = []
    report = read_json(report_path, failures)
    wom_toolbar = read_json(WOM_TOOLBAR_REPORT_PATH, failures)

    if report:
        check_summary(report, failures)
        check_direct_entry(report, failures)
        check_public_ingress(report, wom_toolbar, failures)
        check_asset_transfer(report, failures)
        check_evidence_files(report, failures)
    check_docs(wom_toolbar, failures)

    if failures:
        print(f"Test environment static bundle link verification failed with {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Test environment static bundle link verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
