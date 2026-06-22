#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[4]

REQUIRED_CONTROL_IDS = {
    "default-admin-password-rotation",
    "test-account-cleanup",
    "test-runtime-bypass-disabled",
    "service-account-least-privilege",
    "database-user-privilege-split",
    "secret-management",
    "client-secret-rotation",
    "container-image-scan",
    "container-runtime-user",
    "host-firewall-review",
    "audit-log-retention",
    "alerting-monitoring",
}

ALLOWED_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
CONTROL_REQUIRED_FIELDS = [
    "id",
    "title",
    "category",
    "status",
    "owner",
    "risk",
    "currentState",
    "requiredEvidence",
    "evidence",
]
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
    "adp" + "123456",
    "adpminio" + "123",
    "adpmongo" + "123",
    "BEGIN " + "PRIVATE KEY",
    "BEGIN " + "RSA PRIVATE KEY",
    "BEGIN " + "OPENSSH PRIVATE KEY",
    "AK" + "IA",
]

SECRET_ASSIGNMENT_RE = re.compile(
    r"(?im)(password|passwd|secret|token|private[_-]?key|access[_-]?key|secret[_-]?key|license[_-]?key)\s*[:=]\s*"
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
    return not stripped or stripped in {"TBD", "TODO", "CHANGE_ME"}


def is_template_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    stripped = value.strip()
    if is_placeholder(stripped):
        return True
    lowered = stripped.lower()
    name = Path(stripped).name.lower()
    return any(marker in lowered or marker in name for marker in PLACEHOLDER_EVIDENCE_MARKERS)


def check_real_evidence_reference(control_id: str, field: str, value: Any, failures: list[str]) -> None:
    if is_template_evidence_ref(value):
        fail(
            failures,
            f"{control_id}.{field} must reference real production security evidence, not a template/example/sample asset",
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
        fail(failures, f"{path} must not be a placeholder for strict security readiness")


def check_artifact_reference(control_id: str, field: str, value: str, failures: list[str]) -> None:
    if not value or value == "TBD":
        return
    if value.startswith(("http://", "https://", "s3://", "minio://")):
        return
    if "$" in value or "\n" in value:
        return
    if any(
        value.startswith(prefix)
        for prefix in (
            "trivy ",
            "grype ",
            "docker ",
            "kubectl ",
            "psql ",
            "SELECT ",
            "SHOW ",
            "curl ",
        )
    ):
        return
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        fail(failures, f"{control_id}.{field} must be a safe repo-relative path or external evidence URI")
        return
    candidate = ROOT / relative
    if not candidate.exists():
        fail(failures, f"{control_id}.{field} references a missing repo artifact: {value}")


def check_control(index: int, control: Any, failures: list[str], strict_ready: bool) -> str | None:
    if not isinstance(control, dict):
        fail(failures, f"controls[{index}] must be an object")
        return None

    control_id = text_value(control.get("id"))
    if not control_id:
        fail(failures, f"controls[{index}] missing id")
        return None

    for field in CONTROL_REQUIRED_FIELDS:
        if field not in control:
            fail(failures, f"{control_id} missing required field: {field}")

    status = control.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{control_id} has invalid status: {status!r}")
        return control_id

    for field in ("title", "category", "owner", "risk", "currentState", "requiredEvidence"):
        if not text_value(control.get(field)):
            fail(failures, f"{control_id} missing non-empty {field}")

    check_artifact_reference(control_id, "evidence", text_value(control.get("evidence")), failures)
    for field in ("scanReport", "grantReview", "rotationRecord", "accountInventory", "secretInventory"):
        if field in control:
            check_artifact_reference(control_id, field, text_value(control.get(field)), failures)

    if status == "READY":
        if is_placeholder(control.get("evidence")):
            fail(failures, f"{control_id} READY requires non-placeholder evidence")
        check_real_evidence_reference(control_id, "evidence", control.get("evidence"), failures)
        for field in ("scanReport", "grantReview", "rotationRecord", "accountInventory", "secretInventory"):
            if field in control and text_value(control.get(field)):
                check_real_evidence_reference(control_id, field, control.get(field), failures)
        if as_list(control.get("blockingIssues")):
            fail(failures, f"{control_id} READY must not include blockingIssues")
    else:
        if not as_list(control.get("blockingIssues")):
            fail(failures, f"{control_id} {status} must include blockingIssues")
        if not as_list(control.get("nextActions")):
            fail(failures, f"{control_id} {status} must include nextActions")
        if strict_ready:
            fail(failures, f"{control_id} must be READY for strict security hardening readiness")

    return control_id


def check_document(data: Any, failures: list[str], strict_ready: bool) -> None:
    if not isinstance(data, dict):
        fail(failures, "security hardening plan must be a JSON object")
        return

    for field in (
        "schemaVersion",
        "generatedAt",
        "environment",
        "status",
        "owner",
        "productionSecretManager",
        "breakGlassOwner",
        "reviewDate",
        "controls",
    ):
        if field not in data:
            fail(failures, f"missing top-level field: {field}")

    if data.get("environment") != "production":
        fail(failures, "environment must be production")
    if data.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"invalid top-level status: {data.get('status')!r}")
    if strict_ready:
        if data.get("status") != "READY":
            fail(failures, "top-level status must be READY for strict security hardening readiness")
        for field in ("productionSecretManager", "breakGlassOwner", "reviewDate"):
            if is_placeholder(data.get(field)):
                fail(failures, f"{field} must be non-placeholder for strict security hardening readiness")

    controls = data.get("controls")
    if not isinstance(controls, list):
        fail(failures, "controls must be a list")
        return

    seen: set[str] = set()
    for index, control in enumerate(controls):
        control_id = check_control(index, control, failures, strict_ready)
        if not control_id:
            continue
        if control_id in seen:
            fail(failures, f"duplicate control id: {control_id}")
        seen.add(control_id)

    missing = sorted(REQUIRED_CONTROL_IDS - seen)
    if missing:
        fail(failures, "missing required security controls: " + ", ".join(missing))
    extra = sorted(seen - REQUIRED_CONTROL_IDS)
    if extra:
        fail(failures, "unknown security controls: " + ", ".join(extra))

    if strict_ready:
        check_no_placeholders(data, "securityHardeningPlan", failures)

    check_secret_hygiene(data, "securityHardeningPlan", failures)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate ADP production security hardening evidence.")
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--strict-ready", action="store_true")
    args = parser.parse_args()

    try:
        data = json.loads(args.plan.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"FAIL: security hardening plan file not found: {args.plan}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as error:
        print(f"FAIL: invalid JSON in security hardening plan file: {error}", file=sys.stderr)
        return 1

    failures: list[str] = []
    check_document(data, failures, args.strict_ready)
    if failures:
        print(f"Security hardening plan validation failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Security hardening plan validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
