#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[4]

REQUIRED_COMPONENT_IDS = {
    "postgresql-target-database",
    "minio-objects",
    "keycloak-realm-users",
    "nacos-configuration",
    "runtime-patch-set",
    "domain-port-tls-entry",
}

ALLOWED_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
READY_REQUIRED_FIELDS = ["owner", "backupEvidence", "restoreProcedure", "rehearsalEvidence"]
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
    r"(?im)(password|passwd|secret|token|access[_-]?key|secret[_-]?key)\s*[:=]\s*"
    r"(?!CHANGE_ME|TBD|TODO|<|\$\{|\$)[^\s#\"']{8,}"
)


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def text_value(value: Any) -> str:
    return value.strip() if isinstance(value, str) else ""


def is_template_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    stripped = value.strip()
    if not stripped or stripped in {"TBD", "TODO", "CHANGE_ME"}:
        return True
    lowered = stripped.lower()
    name = Path(stripped).name.lower()
    return any(marker in lowered or marker in name for marker in PLACEHOLDER_EVIDENCE_MARKERS)


def check_real_evidence_reference(component_id: str, field: str, value: Any, failures: list[str]) -> None:
    if is_template_evidence_ref(value):
        fail(
            failures,
            f"{component_id}.{field} must reference real rollback rehearsal evidence, not a template/example/sample asset",
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


def check_artifact_reference(component_id: str, field: str, value: str, failures: list[str]) -> None:
    if not value or value == "TBD":
        return
    if value.startswith(("http://", "https://", "s3://", "minio://")):
        return
    if any(value.startswith(prefix) for prefix in ("pg_", "pg_dump", "pg_restore", "mc ", "kubectl ", "docker ")):
        return
    if "$" in value or "\n" in value:
        return
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        fail(failures, f"{component_id}.{field} must be a safe repo-relative path or an external evidence URI")
        return
    candidate = ROOT / relative
    if not candidate.exists():
        fail(failures, f"{component_id}.{field} references a missing repo artifact: {value}")


def check_component(index: int, component: Any, failures: list[str], strict_ready: bool) -> str | None:
    if not isinstance(component, dict):
        fail(failures, f"components[{index}] must be an object")
        return None

    component_id = text_value(component.get("id"))
    if not component_id:
        fail(failures, f"components[{index}] missing id")
        return None
    status = component.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{component_id} has invalid status: {status!r}")
        return component_id

    for field in ("title", "owner", "backupEvidence", "restoreProcedure", "rehearsalEvidence"):
        if field not in component:
            fail(failures, f"{component_id} missing required field: {field}")

    for field in ("backupEvidence", "rehearsalEvidence"):
        check_artifact_reference(component_id, field, text_value(component.get(field)), failures)

    if status == "READY":
        for field in READY_REQUIRED_FIELDS:
            value = text_value(component.get(field))
            if not value or value == "TBD":
                fail(failures, f"{component_id} READY requires non-placeholder {field}")
        for field in ("backupEvidence", "rehearsalEvidence"):
            check_real_evidence_reference(component_id, field, component.get(field), failures)
        if as_list(component.get("blockingIssues")):
            fail(failures, f"{component_id} READY must not include blockingIssues")
    else:
        if not as_list(component.get("blockingIssues")):
            fail(failures, f"{component_id} {status} must include blockingIssues")
        if not as_list(component.get("nextActions")):
            fail(failures, f"{component_id} {status} must include nextActions")
        if strict_ready:
            fail(failures, f"{component_id} must be READY for strict rollback readiness")
    return component_id


def check_evidence(data: Any, failures: list[str], strict_ready: bool) -> None:
    if not isinstance(data, dict):
        fail(failures, "rollback evidence must be a JSON object")
        return

    for field in ("schemaVersion", "generatedAt", "environment", "cutoverWindow", "rollbackDecisionOwner", "status", "components"):
        if field not in data:
            fail(failures, f"missing top-level field: {field}")

    if data.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"invalid top-level status: {data.get('status')!r}")
    if strict_ready and data.get("status") != "READY":
        fail(failures, "top-level status must be READY for strict rollback readiness")

    components = data.get("components")
    if not isinstance(components, list):
        fail(failures, "components must be a list")
        return

    seen: set[str] = set()
    for index, component in enumerate(components):
        component_id = check_component(index, component, failures, strict_ready)
        if not component_id:
            continue
        if component_id in seen:
            fail(failures, f"duplicate component id: {component_id}")
        seen.add(component_id)

    missing = sorted(REQUIRED_COMPONENT_IDS - seen)
    if missing:
        fail(failures, "missing required rollback components: " + ", ".join(missing))
    extra = sorted(seen - REQUIRED_COMPONENT_IDS)
    if extra:
        fail(failures, "unknown rollback components: " + ", ".join(extra))

    check_secret_hygiene(data, "rollbackEvidence", failures)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate ADP production rollback evidence.")
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--strict-ready", action="store_true")
    args = parser.parse_args()

    try:
        data = json.loads(args.evidence.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"FAIL: rollback evidence file not found: {args.evidence}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as error:
        print(f"FAIL: invalid JSON in rollback evidence file: {error}", file=sys.stderr)
        return 1

    failures: list[str] = []
    check_evidence(data, failures, args.strict_ready)
    if failures:
        print(f"Rollback evidence validation failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Rollback evidence validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
