#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_EXPECTED_HOST = "100.99.133.43"

REPORTS = {
    "test-environment": ROOT / "metadata/test-environment-smoke.json",
    "postgres-runtime": ROOT / "metadata/postgres-runtime-smoke.json",
    "nacos-config": ROOT / "metadata/nacos-config-drift-smoke.json",
    "keycloak-jwt": ROOT / "metadata/keycloak-jwt-runtime-smoke.json",
    "minio-runtime": ROOT / "metadata/minio-runtime-smoke.json",
}

DOC_REQUIREMENTS = {
    "docs/runtime-validation-scope.md": [
        "metadata/test-environment-smoke.json",
        "metadata/postgres-runtime-smoke.json",
        "metadata/nacos-config-drift-smoke.json",
        "HTTP HEAD",
    ],
    "docs/project-goal-acceptance.md": [
        "metadata/postgres-runtime-smoke.json",
        "metadata/keycloak-jwt-runtime-smoke.json",
        "metadata/nacos-config-drift-smoke.json",
    ],
    "docs/production-migration-readiness.md": [
        "metadata/test-environment-smoke.json",
        "metadata/postgres-runtime-smoke.json",
        "metadata/nacos-config-drift-smoke.json",
        "metadata/keycloak-jwt-runtime-smoke.json",
        "metadata/minio-runtime-smoke.json",
        "HTTP HEAD",
    ],
}

COMMON_REQUIRED_KEYS = {
    "schemaVersion",
    "generatedAt",
    "repoCommit",
    "database",
    "summary",
}

REQUIRED_CHECKS = {
    "test-environment": {
        "http-frontend-18080",
        "http-secondary-18070",
        "ssh-reachable",
        "container-up-adp-mes-newbase-nginx-1",
        "container-up-adp-mes-newbase-gateway-1",
        "container-up-adp-mes-newbase-postgres-1",
        "container-up-adp-mes-newbase-nacos-1",
        "container-up-adp-mes-newbase-keycloak-1",
        "container-up-adp-mes-newbase-minio-1",
    },
    "postgres-runtime": {
        "ssh-target-reachable",
        "postgres-container-reachable",
        "postgres-version-detected",
        "postgres-public-table-volume",
        "expected-tables-present",
        "compatibility-columns-present",
        "compatibility-indexes-present",
        "postgres-extension-baseline",
    },
    "keycloak-jwt": {
        "realm-public-key-present",
        "realm-enabled",
        "realm-direct-grant-flow",
        "nacos-jwt-public-key-synced",
        "nacos-keycloak-healthy-instance",
        "client-scope-supos-present",
        "readonly-property-file-component-present",
        "lfy-grant-flow-present",
        "client-pc_dt-enabled",
        "client-pc_dt-direct-grant",
        "client-mobile_dt-enabled",
        "client-mobile_dt-direct-grant",
        "supos-mapper-userName",
        "supos-mapper-userId",
        "supos-mapper-departmentCode",
        "supos-mapper-companyId",
        "supos-mapper-staffName",
        "gateway-login-admin",
        "gateway-menu-current-user",
    },
    "minio-runtime": {
        "ssh-target-reachable",
        "minio-container-reachable",
        "minio-alias-auth",
        "bucket-discovery",
        "expected-bucket-dtbucket",
        "expected-bucket-system001bucket",
        "bucket-inventory-dtbucket",
        "bucket-inventory-system001bucket",
    },
}

EXPECTED_TEST_CONTAINERS = {
    "adp-mes-newbase-nginx-1",
    "adp-mes-newbase-gateway-1",
    "adp-mes-newbase-postgres-1",
    "adp-mes-newbase-nacos-1",
    "adp-mes-newbase-keycloak-1",
    "adp-mes-newbase-minio-1",
}
EXPECTED_TEST_HTTP_LABELS = {"frontend-18080", "secondary-18070"}
MAX_TEST_HTTP_SAMPLE_BYTES = 4096

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


def read_json(path: Path, failures: list[str]) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing runtime smoke report: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{path.relative_to(ROOT)} must be a JSON object")
        return {}
    return data


def check_secret_hygiene(path: Path, failures: list[str]) -> None:
    text = path.read_text(encoding="utf-8")
    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in text:
            fail(failures, f"{path.relative_to(ROOT)} contains forbidden secret literal: {literal}")
    if SECRET_ASSIGNMENT_RE.search(text):
        fail(failures, f"{path.relative_to(ROOT)} appears to contain a non-placeholder secret assignment")


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


def check_common(report_id: str, data: dict[str, Any], expected_host: str, failures: list[str]) -> None:
    missing = sorted(COMMON_REQUIRED_KEYS - set(data))
    if missing:
        fail(failures, f"{report_id} missing top-level keys: {', '.join(missing)}")

    if data.get("database") != "PostgreSQL":
        fail(failures, f"{report_id} database must remain PostgreSQL")

    repo_commit = data.get("repoCommit")
    if not isinstance(repo_commit, str) or len(repo_commit.strip()) < 7:
        fail(failures, f"{report_id}.repoCommit must identify the verified baseline")

    summary = as_dict(data.get("summary"))
    if summary.get("status") != "PASS":
        fail(failures, f"{report_id}.summary.status must be PASS")
    if int(summary.get("fail") or 0) != 0:
        fail(failures, f"{report_id}.summary.fail must be 0")

    target = as_dict(data.get("target")) or as_dict(data.get("environment"))
    ssh_host = target.get("sshHost")
    if ssh_host != expected_host:
        fail(failures, f"{report_id} expected sshHost {expected_host}, got {ssh_host!r}")


def check_named_checks(report_id: str, data: dict[str, Any], required_checks: set[str], failures: list[str]) -> None:
    checks = as_list(data.get("checks"))
    if not checks:
        fail(failures, f"{report_id}.checks must be a non-empty list")
        return

    names: set[str] = set()
    pass_count = 0
    for index, check in enumerate(checks):
        if not isinstance(check, dict):
            fail(failures, f"{report_id}.checks[{index}] must be an object")
            continue
        name = check.get("name")
        if not isinstance(name, str) or not name.strip():
            fail(failures, f"{report_id}.checks[{index}] missing name")
            continue
        if name in names:
            fail(failures, f"{report_id} duplicate check name: {name}")
        names.add(name)
        if check.get("status") == "PASS":
            pass_count += 1
        else:
            fail(failures, f"{report_id}.{name} status must be PASS")

    missing_checks = sorted(required_checks - names)
    if missing_checks:
        fail(failures, f"{report_id} missing checks: {', '.join(missing_checks)}")

    summary = as_dict(data.get("summary"))
    expected_total_keys = ("totalChecks", "checks")
    expected_total = next((summary.get(key) for key in expected_total_keys if key in summary), None)
    if expected_total is not None and expected_total != len(checks):
        fail(failures, f"{report_id} summary total {expected_total!r} does not match checks length {len(checks)}")
    if summary.get("pass") != pass_count:
        fail(failures, f"{report_id} summary.pass={summary.get('pass')!r} does not match PASS checks {pass_count}")


def check_test_environment_http_probes(data: dict[str, Any], expected_host: str, failures: list[str]) -> None:
    http_results = as_list(data.get("http"))
    if len(http_results) != len(EXPECTED_TEST_HTTP_LABELS):
        fail(failures, "test-environment http probes must cover frontend-18080 and secondary-18070")

    probes_by_label: dict[str, dict[str, Any]] = {}
    for index, probe in enumerate(http_results):
        if not isinstance(probe, dict):
            fail(failures, f"test-environment.http[{index}] must be an object")
            continue
        label = probe.get("label")
        if label not in EXPECTED_TEST_HTTP_LABELS:
            fail(failures, f"test-environment unknown http probe label: {label!r}")
            continue
        probes_by_label[str(label)] = probe

        method = probe.get("method")
        if method not in {"HEAD", "GET"}:
            fail(failures, f"test-environment {label} method must be HEAD or ranged GET")
        body_bytes = probe.get("bodyBytes")
        if not isinstance(body_bytes, int) or body_bytes < 0:
            fail(failures, f"test-environment {label} bodyBytes must be a non-negative integer")
        elif method == "HEAD" and body_bytes != 0:
            fail(failures, f"test-environment {label} HEAD probe must not read a body")
        elif method == "GET" and body_bytes > MAX_TEST_HTTP_SAMPLE_BYTES:
            fail(failures, f"test-environment {label} GET fallback must read at most {MAX_TEST_HTTP_SAMPLE_BYTES} bytes")

        if probe.get("status") != 200:
            fail(failures, f"test-environment {label} status must be 200")
        if probe.get("ok") is not True:
            fail(failures, f"test-environment {label} ok must be true")

    missing_labels = sorted(EXPECTED_TEST_HTTP_LABELS - set(probes_by_label))
    if missing_labels:
        fail(failures, "test-environment missing http probes: " + ", ".join(missing_labels))

    frontend = probes_by_label.get("frontend-18080")
    if frontend and frontend.get("url") != f"http://{expected_host}:18080":
        fail(failures, "test-environment frontend-18080 url must match current host")
    secondary = probes_by_label.get("secondary-18070")
    if secondary and secondary.get("url") != f"http://{expected_host}:18070":
        fail(failures, "test-environment secondary-18070 url must match current host")

    evidence = as_dict(data.get("evidence"))
    method_text = str(evidence.get("method", ""))
    if "HTTP HEAD probes" not in method_text or "small ranged GET fallback" not in method_text:
        fail(failures, "test-environment evidence.method must document lightweight HTTP HEAD/ranged GET probing")


def check_test_environment(data: dict[str, Any], expected_host: str, failures: list[str]) -> None:
    check_common("test-environment", data, expected_host, failures)
    check_named_checks("test-environment", data, REQUIRED_CHECKS["test-environment"], failures)
    check_test_environment_http_probes(data, expected_host, failures)

    environment = as_dict(data.get("environment"))
    if environment.get("baseUrl") != f"http://{expected_host}:18080":
        fail(failures, "test-environment baseUrl must match current 18080 test entrypoint")
    if environment.get("secondaryBaseUrl") != f"http://{expected_host}:18070":
        fail(failures, "test-environment secondaryBaseUrl must match current 18070 test entrypoint")

    summary = as_dict(data.get("summary"))
    if summary.get("expectedContainerCount") != len(EXPECTED_TEST_CONTAINERS):
        fail(failures, "test-environment expectedContainerCount must cover the six core containers")
    if summary.get("runningExpectedContainers") != len(EXPECTED_TEST_CONTAINERS):
        fail(failures, "test-environment runningExpectedContainers must be six")

    expected_containers = set(as_list(data.get("expectedContainers")))
    if expected_containers != EXPECTED_TEST_CONTAINERS:
        fail(failures, "test-environment expectedContainers must match the core container set")


def check_postgres_runtime(data: dict[str, Any], expected_host: str, failures: list[str]) -> None:
    check_common("postgres-runtime", data, expected_host, failures)
    check_named_checks("postgres-runtime", data, REQUIRED_CHECKS["postgres-runtime"], failures)

    environment = as_dict(data.get("environment"))
    if environment.get("postgresContainer") != "adp-mes-newbase-postgres-1":
        fail(failures, "postgres-runtime must point at adp-mes-newbase-postgres-1")

    summary = as_dict(data.get("summary"))
    if int(summary.get("publicTableCount") or 0) <= 0:
        fail(failures, "postgres-runtime publicTableCount must be positive")
    if int(summary.get("publicViewCount") or 0) <= 0:
        fail(failures, "postgres-runtime publicViewCount must be positive")
    for expected_key, present_key in (
        ("expectedTables", "presentExpectedTables"),
        ("expectedColumns", "presentExpectedColumns"),
        ("expectedIndexes", "presentExpectedIndexes"),
    ):
        if summary.get(expected_key) != summary.get(present_key):
            fail(failures, f"postgres-runtime {present_key} must match {expected_key}")
    if summary.get("missingExpectedTables") != 0:
        fail(failures, "postgres-runtime missingExpectedTables must be 0")


def check_nacos_config(data: dict[str, Any], expected_host: str, failures: list[str]) -> None:
    check_common("nacos-config", data, expected_host, failures)

    target = as_dict(data.get("target"))
    if target.get("nacosContainer") != "adp-mes-newbase-nacos-1":
        fail(failures, "nacos-config must point at adp-mes-newbase-nacos-1")
    if target.get("nacosGroup") != "prod":
        fail(failures, "nacos-config must check Nacos group prod")

    summary = as_dict(data.get("summary"))
    if summary.get("remoteFetched") != summary.get("dataIds"):
        fail(failures, "nacos-config remoteFetched must equal dataIds")
    for key in ("missingRemote", "missingLocal", "oracleResidueFiles", "criticalFail", "failedServices"):
        if summary.get(key) != 0:
            fail(failures, f"nacos-config summary.{key} must be 0")
    if summary.get("criticalPass") != summary.get("criticalChecks"):
        fail(failures, "nacos-config criticalPass must equal criticalChecks")
    if summary.get("healthyServices") != summary.get("expectedServices"):
        fail(failures, "nacos-config healthyServices must equal expectedServices")

    critical_checks = as_list(data.get("criticalChecks"))
    if not critical_checks:
        fail(failures, "nacos-config criticalChecks must be non-empty")
    for index, check in enumerate(critical_checks):
        if not isinstance(check, dict):
            fail(failures, f"nacos-config.criticalChecks[{index}] must be an object")
            continue
        if check.get("status") != "PASS":
            fail(failures, f"nacos-config critical check {check.get('name')!r} must be PASS")

    services = as_dict(data.get("services"))
    if services.get("group") != "prod":
        fail(failures, "nacos-config services.group must be prod")
    if services.get("listFetchStatus") != "PASS":
        fail(failures, "nacos-config service list fetch must be PASS")
    service_items = as_list(services.get("items"))
    if len(service_items) != summary.get("expectedServices"):
        fail(failures, "nacos-config service item count must match expectedServices")
    for item in service_items:
        if not isinstance(item, dict):
            fail(failures, "nacos-config services.items entries must be objects")
            continue
        if item.get("status") != "PASS":
            fail(failures, f"nacos service {item.get('serviceName')!r} must be PASS")
        if int(item.get("healthyHosts") or 0) <= 0:
            fail(failures, f"nacos service {item.get('serviceName')!r} must have healthy hosts")


def check_keycloak_jwt(data: dict[str, Any], expected_host: str, failures: list[str]) -> None:
    check_common("keycloak-jwt", data, expected_host, failures)
    check_named_checks("keycloak-jwt", data, REQUIRED_CHECKS["keycloak-jwt"], failures)

    target = as_dict(data.get("target"))
    if target.get("baseUrl") != f"http://{expected_host}:18080":
        fail(failures, "keycloak-jwt baseUrl must match current 18080 test entrypoint")
    if target.get("realm") != "dt":
        fail(failures, "keycloak-jwt realm must be dt")
    if target.get("nacosGroup") != "prod":
        fail(failures, "keycloak-jwt Nacos group must be prod")

    summary = as_dict(data.get("summary"))
    if summary.get("keycloakPublicKeySha256Prefix") != summary.get("nacosJwtSha256Prefix"):
        fail(failures, "keycloak-jwt public key hash must match Nacos JWT hash")
    if int(summary.get("nacosHealthyKeycloakHosts") or 0) <= 0:
        fail(failures, "keycloak-jwt must have a healthy Nacos keycloak host")
    if int(summary.get("gatewayMenuTargets") or 0) <= 0:
        fail(failures, "keycloak-jwt must prove gateway current-user menu targets")


def check_minio_runtime(data: dict[str, Any], expected_host: str, failures: list[str]) -> None:
    check_common("minio-runtime", data, expected_host, failures)
    check_named_checks("minio-runtime", data, REQUIRED_CHECKS["minio-runtime"], failures)

    environment = as_dict(data.get("environment"))
    if environment.get("minioContainer") != "adp-mes-newbase-minio-1":
        fail(failures, "minio-runtime must point at adp-mes-newbase-minio-1")

    summary = as_dict(data.get("summary"))
    if summary.get("bucketCount") != 2 or summary.get("inspectedBucketCount") != 2:
        fail(failures, "minio-runtime must discover and inspect two expected buckets")
    if int(summary.get("totalObjects") or 0) <= 0:
        fail(failures, "minio-runtime totalObjects must be positive")

    buckets = as_list(data.get("buckets"))
    bucket_names = {bucket.get("name") for bucket in buckets if isinstance(bucket, dict)}
    if bucket_names != {"dtbucket", "system001bucket"}:
        fail(failures, "minio-runtime buckets must be dtbucket and system001bucket")
    for bucket in buckets:
        if not isinstance(bucket, dict):
            fail(failures, "minio-runtime bucket entries must be objects")
            continue
        if bucket.get("inventoryStatus") != "PASS":
            fail(failures, f"minio bucket {bucket.get('name')!r} inventoryStatus must be PASS")
        if int(bucket.get("parseErrorCount") or 0) != 0:
            fail(failures, f"minio bucket {bucket.get('name')!r} parseErrorCount must be 0")
        prefixes = as_list(bucket.get("sampleObjectKeySha256Prefixes"))
        if not prefixes:
            fail(failures, f"minio bucket {bucket.get('name')!r} must store hashed object-key samples")
        for prefix in prefixes:
            if not isinstance(prefix, str) or not re.fullmatch(r"[0-9a-f]{16}", prefix):
                fail(failures, f"minio bucket {bucket.get('name')!r} sample key prefix must be a 16-char sha256 prefix")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate committed ADP runtime smoke reports.")
    parser.add_argument("--expected-host", default=DEFAULT_EXPECTED_HOST, help="Expected SSH/runtime host for current test reports")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    failures: list[str] = []
    check_docs(failures)

    validators = {
        "test-environment": check_test_environment,
        "postgres-runtime": check_postgres_runtime,
        "nacos-config": check_nacos_config,
        "keycloak-jwt": check_keycloak_jwt,
        "minio-runtime": check_minio_runtime,
    }

    for report_id, path in REPORTS.items():
        data = read_json(path, failures)
        if path.exists():
            check_secret_hygiene(path, failures)
        if data:
            validators[report_id](data, args.expected_host, failures)

    if failures:
        print(f"Runtime smoke report verification failed with {len(failures)} issue(s).", file=sys.stderr)
        return 1

    print("Runtime smoke report verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
