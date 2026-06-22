#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
EXPECTED_HOST = "100.99.133.43"
LEGACY_HOST = "10.11.100.17"
LEGACY_RUNTIME_HOSTS = {"10.11.100.17", "222.88.185.146"}
EXPECTED_BASE_URL = f"http://{EXPECTED_HOST}:18080"
EXPECTED_SECONDARY_URL = f"http://{EXPECTED_HOST}:18070"
EXPECTED_BROWSER_BASE_URL = EXPECTED_BASE_URL

RUNTIME_PATHS = [
    ROOT / "Makefile",
    ROOT / "deploy/docker/.env.example",
    ROOT / "deploy/docker/docker-compose.yml",
    ROOT / "deploy/docker/README.md",
    ROOT / "deploy/docker/scripts",
    ROOT / "scripts/verify-platform-validation-smoke.py",
    ROOT / "scripts/verify-runtime-smoke-reports.py",
]

DOC_REQUIREMENTS = {
    ROOT / "docs/runtime-validation-scope.md": [
        EXPECTED_HOST,
        EXPECTED_BASE_URL,
        EXPECTED_BROWSER_BASE_URL,
        "ADP_BROWSER_BASE_URL",
        "HTTP HEAD",
        "metadata/test-environment-smoke.json",
        "metadata/platform-validation-smoke.json",
    ],
    ROOT / "docs/project-goal-acceptance.md": [
        EXPECTED_HOST,
        EXPECTED_BROWSER_BASE_URL,
        "metadata/test-environment-smoke.json",
        "metadata/platform-validation-smoke.json",
    ],
    ROOT / "docs/backend-table-audit-handoff.md": [
        EXPECTED_HOST,
        EXPECTED_BASE_URL,
        f"v6@{EXPECTED_HOST}",
    ],
}

REPORTS = {
    "test-environment": ROOT / "metadata/test-environment-smoke.json",
    "platform-validation": ROOT / "metadata/platform-validation-smoke.json",
    "postgres-runtime": ROOT / "metadata/postgres-runtime-smoke.json",
    "nacos-config": ROOT / "metadata/nacos-config-drift-smoke.json",
    "keycloak-jwt": ROOT / "metadata/keycloak-jwt-runtime-smoke.json",
    "minio-runtime": ROOT / "metadata/minio-runtime-smoke.json",
}
EXPECTED_TEST_HTTP_LABELS = {"frontend-18080", "secondary-18070"}
MAX_TEST_HTTP_SAMPLE_BYTES = 4096


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_text(path: Path, failures: list[str]) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        fail(failures, f"missing required path: {path.relative_to(ROOT)}")
    except UnicodeDecodeError as error:
        fail(failures, f"cannot decode {path.relative_to(ROOT)} as UTF-8: {error}")
    return ""


def iter_files(path: Path) -> list[Path]:
    if path.is_file():
        return [path]
    if path.is_dir():
        return sorted(
            child
            for child in path.rglob("*")
            if child.is_file()
            and child.suffix in {".js", ".py", ".sh", ".yml", ".yaml", ".env", ".example", ".md"}
        )
    return [path]


def require_text(path: Path, fragments: list[str], failures: list[str]) -> None:
    text = read_text(path, failures)
    for fragment in fragments:
        if fragment not in text:
            fail(failures, f"{path.relative_to(ROOT)} missing required current-host text: {fragment}")


def forbid_legacy_runtime_host(failures: list[str]) -> None:
    for base_path in RUNTIME_PATHS:
        for path in iter_files(base_path):
            text = read_text(path, failures)
            for legacy_host in LEGACY_RUNTIME_HOSTS:
                if legacy_host in text:
                    fail(failures, f"{path.relative_to(ROOT)} still uses legacy runtime host {legacy_host}")


def check_makefile(failures: list[str]) -> None:
    require_text(
        ROOT / "Makefile",
        [
            f"ADP_BASE_URL ?= {EXPECTED_BASE_URL}",
            "ADP_BROWSER_BASE_URL ?= $(ADP_BASE_URL)",
            f"ADP_SSH_HOST ?= {EXPECTED_HOST}",
            f"RUNTIME_SMOKE_EXPECTED_HOST ?= {EXPECTED_HOST}",
            f"PLATFORM_VALIDATION_EXPECTED_BROWSER_BASE_URL ?= {EXPECTED_BROWSER_BASE_URL}",
            "test-environment-address-check",
        ],
        failures,
    )


def check_deploy_defaults(failures: list[str]) -> None:
    require_text(
        ROOT / "deploy/docker/.env.example",
        [
            f"ADP_PUBLIC_HOST={EXPECTED_HOST}",
            f"SUPOS_ADDRESS={EXPECTED_BASE_URL}",
            "SUPOS_SYSTEM_DB_TYPE=postgresql",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/docker-compose.yml",
        [
            f"SUPOS_ADDRESS: ${{SUPOS_ADDRESS:-{EXPECTED_BASE_URL}}}",
            f"ADP_PUBLIC_HOST: ${{ADP_PUBLIC_HOST:-{EXPECTED_HOST}}}",
        ],
        failures,
    )
    require_text(
        ROOT / "deploy/docker/scripts/render-nacos-configs.py",
        [
            f"${{SUPOS_ADDRESS:{EXPECTED_BASE_URL}}}",
            f"env('ADP_PUBLIC_HOST', '{EXPECTED_HOST}')",
            f'env("SUPOS_ADDRESS", "{EXPECTED_BASE_URL}")',
        ],
        failures,
    )


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def read_json(path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing report: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{path.relative_to(ROOT)} must be a JSON object")
        return {}
    return data


def check_report_text(path: Path, failures: list[str]) -> None:
    text = read_text(path, failures)
    for legacy_host in LEGACY_RUNTIME_HOSTS:
        if legacy_host in text:
            fail(failures, f"{path.relative_to(ROOT)} current smoke report still contains legacy host {legacy_host}")
    if EXPECTED_HOST not in text:
        fail(failures, f"{path.relative_to(ROOT)} current smoke report must mention {EXPECTED_HOST}")


def check_test_environment_http_probes(test_env: dict[str, Any], failures: list[str]) -> None:
    http_results = test_env.get("http")
    if not isinstance(http_results, list):
        fail(failures, "test-environment report must include an http probe list")
        return

    probes_by_label: dict[str, dict[str, Any]] = {}
    for index, probe in enumerate(http_results):
        if not isinstance(probe, dict):
            fail(failures, f"test-environment http[{index}] must be an object")
            continue
        label = probe.get("label")
        if label not in EXPECTED_TEST_HTTP_LABELS:
            fail(failures, f"test-environment report has unknown http probe label: {label!r}")
            continue
        probes_by_label[str(label)] = probe

        method = probe.get("method")
        if method not in {"HEAD", "GET"}:
            fail(failures, f"test-environment {label} method must be HEAD or ranged GET")
        body_bytes = probe.get("bodyBytes")
        if not isinstance(body_bytes, int) or body_bytes < 0:
            fail(failures, f"test-environment {label} bodyBytes must be a non-negative integer")
        elif method == "HEAD" and body_bytes != 0:
            fail(failures, f"test-environment {label} HEAD probe must not read a response body")
        elif method == "GET" and body_bytes > MAX_TEST_HTTP_SAMPLE_BYTES:
            fail(failures, f"test-environment {label} GET fallback must read at most {MAX_TEST_HTTP_SAMPLE_BYTES} bytes")

        if probe.get("status") != 200:
            fail(failures, f"test-environment {label} status must be 200")
        if probe.get("ok") is not True:
            fail(failures, f"test-environment {label} ok must be true")

    missing_labels = sorted(EXPECTED_TEST_HTTP_LABELS - set(probes_by_label))
    if missing_labels:
        fail(failures, "test-environment report missing http probes: " + ", ".join(missing_labels))

    frontend = probes_by_label.get("frontend-18080")
    if frontend and frontend.get("url") != EXPECTED_BASE_URL:
        fail(failures, "test-environment frontend-18080 url must use current baseUrl")
    secondary = probes_by_label.get("secondary-18070")
    if secondary and secondary.get("url") != EXPECTED_SECONDARY_URL:
        fail(failures, "test-environment secondary-18070 url must use current secondaryBaseUrl")

    evidence = as_dict(test_env.get("evidence"))
    method_text = str(evidence.get("method", ""))
    if "HTTP HEAD probes" not in method_text or "small ranged GET fallback" not in method_text:
        fail(failures, "test-environment report must document HTTP HEAD probes and small ranged GET fallback")


def check_runtime_reports(failures: list[str]) -> None:
    for path in REPORTS.values():
        check_report_text(path, failures)

    test_env = read_json(REPORTS["test-environment"], failures)
    environment = as_dict(test_env.get("environment"))
    if environment.get("sshHost") != EXPECTED_HOST:
        fail(failures, "test-environment report must use current sshHost")
    if environment.get("baseUrl") != EXPECTED_BASE_URL:
        fail(failures, "test-environment report must use current baseUrl")
    if environment.get("secondaryBaseUrl") != EXPECTED_SECONDARY_URL:
        fail(failures, "test-environment report must use current secondaryBaseUrl")
    check_test_environment_http_probes(test_env, failures)

    platform = read_json(REPORTS["platform-validation"], failures)
    if platform.get("baseUrl") != EXPECTED_BASE_URL:
        fail(failures, "platform-validation report must use current baseUrl")
    if platform.get("browserBaseUrl") != EXPECTED_BROWSER_BASE_URL:
        fail(failures, "platform-validation report must use current browserBaseUrl")

    for report_id in ("postgres-runtime", "nacos-config", "keycloak-jwt", "minio-runtime"):
        data = read_json(REPORTS[report_id], failures)
        target = as_dict(data.get("target")) or as_dict(data.get("environment"))
        if target.get("sshHost") != EXPECTED_HOST:
            fail(failures, f"{report_id} report must use current sshHost")

    keycloak = read_json(REPORTS["keycloak-jwt"], failures)
    keycloak_target = as_dict(keycloak.get("target")) or as_dict(keycloak.get("environment"))
    if keycloak_target.get("baseUrl") != EXPECTED_BASE_URL:
        fail(failures, "keycloak-jwt report must use current baseUrl")


def check_docs(failures: list[str]) -> None:
    for path, required in DOC_REQUIREMENTS.items():
        require_text(path, required, failures)

    runtime_scope = read_text(ROOT / "docs/runtime-validation-scope.md", failures)
    historical_note_pattern = re.compile(
        rf"2026-06-15[^\n]*{re.escape(LEGACY_HOST)}[^\n]*2026-06-20[^\n]*{re.escape(EXPECTED_HOST)}"
    )
    if LEGACY_HOST in runtime_scope and not historical_note_pattern.search(runtime_scope):
        fail(
            failures,
            "docs/runtime-validation-scope.md may mention the legacy host only as historical evidence paired with the current host",
        )


def main() -> int:
    failures: list[str] = []
    forbid_legacy_runtime_host(failures)
    check_makefile(failures)
    check_deploy_defaults(failures)
    check_runtime_reports(failures)
    check_docs(failures)

    if failures:
        print(f"Test environment address verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print(f"Test environment address verification passed for {EXPECTED_HOST}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
