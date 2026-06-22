#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[4]

REQUIRED_SERVICE_IDS = {
    "frontend-gateway",
    "keycloak",
    "minio",
    "nacos",
    "postgresql",
}

REQUIRED_CHECK_IDS = {
    "tls-expiry-check",
    "certificate-renewal-dry-run",
    "firewall-boundary-check",
    "proxy-header-check",
    "hsts-decision",
    "external-health-check",
}

ALLOWED_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
READY_REQUIRED_GENERIC_FIELDS = ["owner"]
PLACEHOLDER_EVIDENCE_MARKERS = (
    ".example",
    "example.",
    "-example",
    "_example",
    "template",
    "sample",
)

FORBIDDEN_SECRET_LITERALS = [
    "ft" + "123456789",
    "BEGIN " + "PRIVATE KEY",
    "BEGIN " + "RSA PRIVATE KEY",
    "BEGIN " + "OPENSSH PRIVATE KEY",
    "AK" + "IA",
]

SECRET_ASSIGNMENT_RE = re.compile(
    r"(?im)(password|passwd|secret|token|private[_-]?key|access[_-]?key|secret[_-]?key)\s*[:=]\s*"
    r"(?!CHANGE_ME|TBD|TODO|<|\$\{|\$)[^\s#\"']{8,}"
)


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def text_value(value: Any) -> str:
    return value.strip() if isinstance(value, str) else ""


def is_placeholder(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    stripped = value.strip()
    return not stripped or stripped in {"TBD", "TODO", "CHANGE_ME", "not-exposed"}


def is_template_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    stripped = value.strip()
    if is_placeholder(stripped):
        return True
    lowered = stripped.lower()
    name = Path(stripped).name.lower()
    return any(marker in lowered or marker in name for marker in PLACEHOLDER_EVIDENCE_MARKERS)


def check_real_evidence_reference(item_id: str, field: str, value: Any, failures: list[str]) -> None:
    if is_template_evidence_ref(value):
        fail(
            failures,
            f"{item_id}.{field} must reference real production network/TLS evidence, not a template/example/sample asset",
        )


def check_secret_hygiene(value: Any, path: str, failures: list[str]) -> None:
    if isinstance(value, dict):
        for key, nested in value.items():
            check_secret_hygiene(nested, f"{path}.{key}", failures)
        return
    if isinstance(value, list):
        for index, nested in enumerate(value):
            check_secret_hygiene(nested, f"{path}[{index}]", failures)
        return
    if not isinstance(value, str):
        return

    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in value:
            fail(failures, f"{path} contains a forbidden secret literal")
    if SECRET_ASSIGNMENT_RE.search(value):
        fail(failures, f"{path} appears to contain a non-placeholder secret assignment")


def check_no_placeholders(value: Any, path: str, failures: list[str]) -> None:
    if isinstance(value, dict):
        for key, nested in value.items():
            check_no_placeholders(nested, f"{path}.{key}", failures)
        return
    if isinstance(value, list):
        for index, nested in enumerate(value):
            check_no_placeholders(nested, f"{path}[{index}]", failures)
        return
    if isinstance(value, str) and value.strip() in {"TBD", "TODO", "CHANGE_ME"}:
        fail(failures, f"{path} must not be a placeholder for strict network/TLS readiness")


def check_artifact_reference(item_id: str, field: str, value: str, failures: list[str]) -> None:
    if not value or value == "TBD":
        return
    if value.startswith(("http://", "https://", "s3://", "minio://")):
        return
    if "$" in value or "\n" in value:
        return
    if any(value.startswith(prefix) for prefix in ("curl ", "openssl ", "dig ", "nmap ", "nc ")):
        return
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        fail(failures, f"{item_id}.{field} must be a safe repo-relative path or external evidence URI")
        return
    candidate = ROOT / relative
    if not candidate.exists():
        fail(failures, f"{item_id}.{field} references a missing repo artifact: {value}")


def check_item(
    list_name: str,
    index: int,
    item: Any,
    failures: list[str],
    strict_ready: bool,
    evidence_fields: tuple[str, ...] = ("evidence",),
) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"{list_name}[{index}] must be an object")
        return None

    item_id = text_value(item.get("id"))
    if not item_id:
        fail(failures, f"{list_name}[{index}] missing id")
        return None

    status = item.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{item_id} has invalid status: {status!r}")
        return item_id

    for field in READY_REQUIRED_GENERIC_FIELDS:
        if not text_value(item.get(field)):
            fail(failures, f"{item_id} missing non-empty {field}")

    for field in evidence_fields:
        if field in item:
            check_artifact_reference(item_id, field, text_value(item.get(field)), failures)

    if status == "READY":
        if as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} READY must not include blockingIssues")
        for field in evidence_fields:
            if field in item and is_placeholder(item.get(field)):
                fail(failures, f"{item_id} READY requires non-placeholder {field}")
            if field in item:
                check_real_evidence_reference(item_id, field, item.get(field), failures)
    else:
        if not as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} {status} must include blockingIssues")
        if not as_list(item.get("nextActions")):
            fail(failures, f"{item_id} {status} must include nextActions")
        if strict_ready:
            fail(failures, f"{item_id} must be READY for strict network/TLS readiness")

    return item_id


def check_service(item: Any, failures: list[str], strict_ready: bool) -> str | None:
    item_id = check_item("exposedServices", 0, item, failures, strict_ready)
    if not item_id or not isinstance(item, dict):
        return item_id

    for field in ("title", "exposure", "protocol", "externalPort", "internalTarget", "tlsMode"):
        if not text_value(item.get(field)):
            fail(failures, f"{item_id} missing non-empty {field}")

    exposure = text_value(item.get("exposure")).lower()
    protocol = text_value(item.get("protocol")).upper()
    external_port = text_value(item.get("externalPort"))
    tls_mode = text_value(item.get("tlsMode")).upper()
    risk_evidence = text_value(item.get("riskAcceptanceEvidence"))

    if strict_ready:
        if item_id == "frontend-gateway":
            if exposure != "public":
                fail(failures, "frontend-gateway must be public in strict production readiness")
            if protocol != "HTTPS":
                fail(failures, "frontend-gateway must expose HTTPS in strict production readiness")
            if external_port != "443":
                fail(failures, "frontend-gateway strict readiness expects externalPort 443")
            if tls_mode not in {"TERMINATED_AT_PROXY", "END_TO_END"}:
                fail(failures, "frontend-gateway strict readiness requires proxy or end-to-end TLS")

        if item_id in {"nacos", "postgresql"} and exposure == "public" and not risk_evidence:
            fail(failures, f"{item_id} must not be public without signed riskAcceptanceEvidence")

        if item_id == "postgresql" and external_port not in {"not-exposed", "private", "vpn-only"}:
            if exposure == "public" or not risk_evidence:
                fail(failures, "postgresql externalPort must stay private or include riskAcceptanceEvidence")

    if risk_evidence:
        check_artifact_reference(item_id, "riskAcceptanceEvidence", risk_evidence, failures)
        if item.get("status") == "READY" or strict_ready:
            check_real_evidence_reference(item_id, "riskAcceptanceEvidence", risk_evidence, failures)

    return item_id


def check_tls_certificate(index: int, item: Any, failures: list[str], strict_ready: bool) -> str | None:
    item_id = check_item(
        "tlsCertificates",
        index,
        item,
        failures,
        strict_ready,
        evidence_fields=("autoRenewalEvidence",),
    )
    if not item_id or not isinstance(item, dict):
        return item_id

    for field in ("issuer", "storage", "renewalMethod", "expiryMonitoring"):
        if not text_value(item.get(field)):
            fail(failures, f"{item_id} missing non-empty {field}")
    domains = as_list(item.get("domains"))
    if not domains:
        fail(failures, f"{item_id} must list certificate domains")
    if strict_ready and any(is_placeholder(domain) for domain in domains):
        fail(failures, f"{item_id} strict readiness requires non-placeholder certificate domains")
    return item_id


def check_document(data: Any, failures: list[str], strict_ready: bool) -> None:
    if not isinstance(data, dict):
        fail(failures, "network/TLS plan must be a JSON object")
        return

    for field in (
        "schemaVersion",
        "generatedAt",
        "environment",
        "status",
        "owner",
        "publicBaseUrl",
        "domains",
        "tlsCertificates",
        "reverseProxies",
        "exposedServices",
        "firewallRules",
        "validationChecks",
    ):
        if field not in data:
            fail(failures, f"missing top-level field: {field}")

    if data.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"invalid top-level status: {data.get('status')!r}")
    if data.get("environment") != "production":
        fail(failures, "environment must be production")
    if strict_ready:
        if data.get("status") != "READY":
            fail(failures, "top-level status must be READY for strict network/TLS readiness")
        public_base_url = text_value(data.get("publicBaseUrl"))
        if not public_base_url.startswith("https://"):
            fail(failures, "publicBaseUrl must start with https:// for strict network/TLS readiness")

    for field in ("domains", "tlsCertificates", "reverseProxies", "exposedServices", "firewallRules", "validationChecks"):
        if not isinstance(data.get(field), list):
            fail(failures, f"{field} must be a list")

    seen_service_ids: set[str] = set()
    for item in as_list(data.get("exposedServices")):
        service_id = check_service(item, failures, strict_ready)
        if service_id:
            if service_id in seen_service_ids:
                fail(failures, f"duplicate exposed service id: {service_id}")
            seen_service_ids.add(service_id)

    missing_services = sorted(REQUIRED_SERVICE_IDS - seen_service_ids)
    if missing_services:
        fail(failures, "missing required exposed services: " + ", ".join(missing_services))

    seen_check_ids: set[str] = set()
    for index, item in enumerate(as_list(data.get("validationChecks"))):
        check_id = check_item(
            "validationChecks",
            index,
            item,
            failures,
            strict_ready,
            evidence_fields=("commandOrEvidence",),
        )
        if check_id:
            if check_id in seen_check_ids:
                fail(failures, f"duplicate validation check id: {check_id}")
            seen_check_ids.add(check_id)
    missing_checks = sorted(REQUIRED_CHECK_IDS - seen_check_ids)
    if missing_checks:
        fail(failures, "missing required validation checks: " + ", ".join(missing_checks))

    for index, item in enumerate(as_list(data.get("domains"))):
        check_item("domains", index, item, failures, strict_ready)
    for index, item in enumerate(as_list(data.get("tlsCertificates"))):
        check_tls_certificate(index, item, failures, strict_ready)
    for index, item in enumerate(as_list(data.get("reverseProxies"))):
        check_item("reverseProxies", index, item, failures, strict_ready)
    for index, item in enumerate(as_list(data.get("firewallRules"))):
        check_item("firewallRules", index, item, failures, strict_ready)

    if strict_ready:
        if not as_list(data.get("tlsCertificates")):
            fail(failures, "strict readiness requires at least one TLS certificate record")
        check_no_placeholders(data, "networkTlsPlan", failures)

    check_secret_hygiene(data, "networkTlsPlan", failures)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate ADP production network/domain/TLS evidence.")
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--strict-ready", action="store_true")
    args = parser.parse_args()

    try:
        data = json.loads(args.plan.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"FAIL: network/TLS plan file not found: {args.plan}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as error:
        print(f"FAIL: invalid JSON in network/TLS plan file: {error}", file=sys.stderr)
        return 1

    failures: list[str] = []
    check_document(data, failures, args.strict_ready)
    if failures:
        print(f"Network/TLS plan validation failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Network/TLS plan validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
