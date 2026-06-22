#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any
from urllib.parse import urlparse


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPORT_PATH = ROOT / "metadata/platform-validation-smoke.json"
DEFAULT_EXPECTED_HOST = "100.99.133.43"
DEFAULT_EXPECTED_BROWSER_BASE_URL = "http://100.99.133.43:18080"

REQUIRED_RESULT_IDS = {
    "platform-api",
    "home-todo",
    "organization",
    "rbac-authority",
    "menu-pages",
}

REQUIRED_RBAC_MENU_CODES = {"organizationmanage", "personmanage", "role"}

DOC_REQUIREMENTS = {
    "docs/runtime-validation-scope.md": [
        "metadata/platform-validation-smoke.json",
        "make platform-validation-check",
        "ADP_BROWSER_BASE_URL",
    ],
    "docs/project-goal-acceptance.md": [
        "metadata/platform-validation-smoke.json",
        "平台综合 smoke",
        "browser base",
    ],
    "docs/frontend-functional-test-report.md": [
        "metadata/platform-validation-smoke.json",
        "平台 smoke 当前复验",
        "browser base",
    ],
}

FORBIDDEN_SECRET_LITERALS = [
    "ft" + "123456789",
    "Authorization: " + "Bearer",
    "access" + "_token",
    "refresh" + "_token",
    "BEGIN " + "PRIVATE KEY",
    "BEGIN " + "RSA PRIVATE KEY",
    "BEGIN " + "OPENSSH PRIVATE KEY",
    "AK" + "IA",
]

SECRET_ASSIGNMENT_RE = re.compile(
    r"(?im)^\s*\"?(password|passwd|secret|token|access[_-]?key|secret[_-]?key)\"?\s*[:=]\s*"
    r"\"?(?!CHANGE_ME|TBD|TODO|<|\$\{|\$|redacted|not written)[^\s\",#}]{8,}"
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
        fail(failures, f"missing platform validation smoke report: {report_path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {report_path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, "platform validation smoke report must be a JSON object")
        return {}
    return data


def check_secret_hygiene(report_path: Path, failures: list[str]) -> None:
    text = report_path.read_text(encoding="utf-8")
    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in text:
            fail(failures, f"{report_path.relative_to(ROOT)} contains forbidden secret literal: {literal}")
    if SECRET_ASSIGNMENT_RE.search(text):
        fail(failures, f"{report_path.relative_to(ROOT)} appears to contain a non-placeholder secret assignment")


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


def normalize_base_url(value: str) -> str:
    return value.rstrip("/")


def check_browser_base_url(data: dict[str, Any], expected_browser_base_url: str | None, failures: list[str]) -> None:
    browser_base_url = data.get("browserBaseUrl")
    if not isinstance(browser_base_url, str) or not browser_base_url.strip():
        fail(failures, "platform validation browserBaseUrl must be present and non-empty")
        return

    parsed = urlparse(browser_base_url)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        fail(failures, "platform validation browserBaseUrl must be an absolute HTTP(S) URL")
    if parsed.path not in {"", "/"} or parsed.params or parsed.query or parsed.fragment:
        fail(failures, "platform validation browserBaseUrl must be a base URL without path/query/fragment")

    if expected_browser_base_url:
        if normalize_base_url(browser_base_url) != normalize_base_url(expected_browser_base_url):
            fail(
                failures,
                "platform validation browserBaseUrl must be "
                f"{normalize_base_url(expected_browser_base_url)}",
            )


def check_top_level(
    data: dict[str, Any],
    expected_host: str,
    expected_browser_base_url: str | None,
    failures: list[str],
) -> None:
    for key in ("schemaVersion", "generatedAt", "baseUrl", "browserBaseUrl", "username", "ok", "total", "passed", "failed", "skipped", "prerequisites", "results"):
        if key not in data:
            fail(failures, f"platform validation report missing top-level key: {key}")

    if data.get("schemaVersion") != 1:
        fail(failures, "platform validation schemaVersion must be 1")
    if data.get("baseUrl") != f"http://{expected_host}:18080":
        fail(failures, f"platform validation baseUrl must be http://{expected_host}:18080")
    check_browser_base_url(data, expected_browser_base_url, failures)
    if data.get("ok") is not True:
        fail(failures, "platform validation ok must be true")
    if data.get("failed") != 0:
        fail(failures, "platform validation failed must be 0")
    if data.get("skipped") != 0:
        fail(failures, "platform validation skipped must be 0")

    prerequisites = as_list(data.get("prerequisites"))
    results = as_list(data.get("results"))
    all_results = prerequisites + results
    if data.get("total") != len(all_results):
        fail(failures, "platform validation total must match prerequisites plus results length")
    passed = sum(
        1
        for result in all_results
        if isinstance(result, dict) and result.get("status") == "passed" and result.get("ok") is True
    )
    if data.get("passed") != passed:
        fail(failures, "platform validation passed must match passed prerequisite/result count")

    prerequisite_ids = {
        str(result.get("id"))
        for result in prerequisites
        if isinstance(result, dict) and result.get("id")
    }
    if "playwright" not in prerequisite_ids:
        fail(failures, "platform validation prerequisites must include playwright")
    for prerequisite in prerequisites:
        if not isinstance(prerequisite, dict):
            fail(failures, "platform validation prerequisite entries must be objects")
            continue
        prerequisite_id = str(prerequisite.get("id", "")).strip() or "<missing>"
        if prerequisite.get("status") != "passed" or prerequisite.get("ok") is not True:
            fail(failures, f"platform validation prerequisite must pass: {prerequisite_id}")


def check_result_shell(result_id: str, result: dict[str, Any], failures: list[str]) -> None:
    for key in ("id", "title", "requiredFor", "status", "ok", "command", "exitCode", "reportPath", "summary"):
        if key not in result:
            fail(failures, f"{result_id} missing required key: {key}")
    if result.get("status") != "passed":
        fail(failures, f"{result_id} status must be passed")
    if result.get("ok") is not True:
        fail(failures, f"{result_id} ok must be true")
    if result.get("exitCode") != 0:
        fail(failures, f"{result_id} exitCode must be 0")
    if result.get("signal") not in (None, ""):
        fail(failures, f"{result_id} signal must be null")
    if result.get("error") not in (None, ""):
        fail(failures, f"{result_id} error must be null")
    if not str(result.get("command", "")).startswith("node deploy/docker/scripts/"):
        fail(failures, f"{result_id} command must be a checked repository smoke script")


def check_platform_api(summary: dict[str, Any], failures: list[str]) -> None:
    if int(summary.get("total") or 0) < 16:
        fail(failures, "platform-api total must be at least 16")
    if summary.get("failed") != 0:
        fail(failures, "platform-api failed must be 0")
    if summary.get("passed") != summary.get("total"):
        fail(failures, "platform-api passed must equal total")


def check_home_todo(summary: dict[str, Any], failures: list[str]) -> None:
    if summary.get("ok") is not True:
        fail(failures, "home-todo summary.ok must be true")
    if summary.get("navigationStatus") != 200:
        fail(failures, "home-todo navigationStatus must be 200")
    if summary.get("navigationError") is not None:
        fail(failures, "home-todo navigationError must be null")
    if summary.get("visibleError") is not None:
        fail(failures, "home-todo visibleError must be null")
    for key in ("networkErrorCount", "consoleErrorCount", "pageErrorCount"):
        if summary.get(key) != 0:
            fail(failures, f"home-todo {key} must be 0")


def check_organization(summary: dict[str, Any], failures: list[str]) -> None:
    if summary.get("ok") is not True:
        fail(failures, "organization summary.ok must be true")
    if as_list(summary.get("failedApis")):
        fail(failures, "organization failedApis must be empty")
    if summary.get("browserOk") is not True:
        fail(failures, "organization browserOk must be true")
    if summary.get("browserNetworkErrorCount") != 0:
        fail(failures, "organization browserNetworkErrorCount must be 0")
    if summary.get("browserVisibleError") is not None:
        fail(failures, "organization browserVisibleError must be null")
    department = as_dict(summary.get("selectedDepartment"))
    if not department.get("id") or not department.get("name") or not department.get("companyId"):
        fail(failures, "organization selectedDepartment must include id, name, and companyId")


def check_rbac_authority(summary: dict[str, Any], failures: list[str]) -> None:
    if int(summary.get("total") or 0) < 9:
        fail(failures, "rbac-authority total must be at least 9")
    if summary.get("failed") != 0:
        fail(failures, "rbac-authority failed must be 0")
    if summary.get("passed") != summary.get("total"):
        fail(failures, "rbac-authority passed must equal total")
    if int(summary.get("menuCount") or 0) <= 0:
        fail(failures, "rbac-authority menuCount must be positive")
    selected_codes = {
        str(menu.get("code"))
        for menu in as_list(summary.get("selectedMenus"))
        if isinstance(menu, dict) and menu.get("code")
    }
    missing_codes = sorted(REQUIRED_RBAC_MENU_CODES - selected_codes)
    if missing_codes:
        fail(failures, "rbac-authority selectedMenus missing codes: " + ", ".join(missing_codes))


def check_menu_pages(summary: dict[str, Any], min_menu_pages: int, failures: list[str]) -> None:
    if int(summary.get("total") or 0) < min_menu_pages:
        fail(failures, f"menu-pages total must be at least {min_menu_pages}")
    if summary.get("failed") != 0:
        fail(failures, "menu-pages failed must be 0")
    if summary.get("passed") != summary.get("total"):
        fail(failures, "menu-pages passed must equal total")


def check_results(data: dict[str, Any], min_menu_pages: int, failures: list[str]) -> None:
    results = as_list(data.get("results"))
    by_id: dict[str, dict[str, Any]] = {}
    for index, result in enumerate(results):
        if not isinstance(result, dict):
            fail(failures, f"results[{index}] must be an object")
            continue
        result_id = str(result.get("id", "")).strip()
        if not result_id:
            fail(failures, f"results[{index}] missing id")
            continue
        if result_id in by_id:
            fail(failures, f"duplicate result id: {result_id}")
        by_id[result_id] = result
        check_result_shell(result_id, result, failures)

    missing = sorted(REQUIRED_RESULT_IDS - set(by_id))
    if missing:
        fail(failures, "platform validation missing result ids: " + ", ".join(missing))

    validators = {
        "platform-api": lambda summary: check_platform_api(summary, failures),
        "home-todo": lambda summary: check_home_todo(summary, failures),
        "organization": lambda summary: check_organization(summary, failures),
        "rbac-authority": lambda summary: check_rbac_authority(summary, failures),
        "menu-pages": lambda summary: check_menu_pages(summary, min_menu_pages, failures),
    }
    for result_id, validator in validators.items():
        if result_id in by_id:
            validator(as_dict(by_id[result_id].get("summary")))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate committed ADP platform validation smoke report.")
    parser.add_argument("--report", default=str(DEFAULT_REPORT_PATH), help="Path to platform validation smoke report JSON")
    parser.add_argument("--expected-host", default=DEFAULT_EXPECTED_HOST, help="Expected current ADP test host")
    parser.add_argument(
        "--expected-browser-base-url",
        default=DEFAULT_EXPECTED_BROWSER_BASE_URL,
        help="Expected browser base URL recorded by the platform smoke report",
    )
    parser.add_argument("--min-menu-pages", type=int, default=5, help="Minimum sampled menu browser pages required")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    failures: list[str] = []
    report_path = Path(args.report)
    if not report_path.is_absolute():
        report_path = ROOT / report_path

    check_docs(failures)
    data = read_json(report_path, failures)
    if report_path.exists():
        check_secret_hygiene(report_path, failures)
    if data:
        check_top_level(data, args.expected_host, args.expected_browser_base_url, failures)
        check_results(data, args.min_menu_pages, failures)

    if failures:
        print(f"Platform validation smoke verification failed with {len(failures)} issue(s).", file=sys.stderr)
        return 1

    print("Platform validation smoke verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
