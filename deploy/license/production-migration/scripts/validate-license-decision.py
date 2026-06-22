#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[4]

REQUIRED_DECISION_IDS = {
    "formal-license",
    "expiry-behavior",
    "emergency-bypass",
    "config-backup-restore",
    "monitoring-audit",
}

ALLOWED_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
ALLOWED_PRODUCTION_MODES = {
    "FORMAL_LICENSE",
    "SUPPORTED_LICENSE_SERVICE",
    "APPROVED_EMERGENCY_BYPASS",
    "NOT_DECIDED",
}
STRICT_READY_ALLOWED_MODES = {
    "FORMAL_LICENSE",
    "SUPPORTED_LICENSE_SERVICE",
    "APPROVED_EMERGENCY_BYPASS",
}
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
    r"(?im)(password|passwd|secret|token|license[_-]?key|access[_-]?key|secret[_-]?key)\s*[:=]\s*"
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


def check_real_evidence_reference(decision_id: str, field: str, value: Any, failures: list[str]) -> None:
    if is_template_evidence_ref(value):
        fail(
            failures,
            f"{decision_id}.{field} must reference real production license evidence, not a template/example/sample asset",
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


def check_artifact_reference(decision_id: str, field: str, value: str, failures: list[str]) -> None:
    if not value or value == "TBD":
        return
    if value.startswith(("http://", "https://", "s3://", "minio://")):
        return
    if "$" in value or "\n" in value:
        return
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        fail(failures, f"{decision_id}.{field} must be a safe repo-relative path or external evidence URI")
        return
    candidate = ROOT / relative
    if not candidate.exists():
        fail(failures, f"{decision_id}.{field} references a missing repo artifact: {value}")


def check_decision(index: int, item: Any, failures: list[str], strict_ready: bool) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"decisions[{index}] must be an object")
        return None

    decision_id = text_value(item.get("id"))
    if not decision_id:
        fail(failures, f"decisions[{index}] missing id")
        return None

    status = item.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{decision_id} has invalid status: {status!r}")
        return decision_id

    for field in ("title", "owner", "decision"):
        if not text_value(item.get(field)):
            fail(failures, f"{decision_id} missing non-empty {field}")

    evidence = text_value(item.get("evidence") or item.get("auditEvidence"))
    check_artifact_reference(decision_id, "evidence", evidence, failures)

    if status == "READY":
        if not text_value(item.get("decision")) or item.get("decision") == "TBD":
            fail(failures, f"{decision_id} READY requires a non-placeholder decision")
        if decision_id != "emergency-bypass" and (not evidence or evidence == "TBD"):
            fail(failures, f"{decision_id} READY requires non-placeholder evidence")
        if evidence and evidence != "TBD":
            check_real_evidence_reference(decision_id, "evidence", evidence, failures)
        if as_list(item.get("blockingIssues")):
            fail(failures, f"{decision_id} READY must not include blockingIssues")
    else:
        if not as_list(item.get("blockingIssues")):
            fail(failures, f"{decision_id} {status} must include blockingIssues")
        if not as_list(item.get("nextActions")):
            fail(failures, f"{decision_id} {status} must include nextActions")
        if strict_ready:
            fail(failures, f"{decision_id} must be READY for strict production license readiness")

    if decision_id == "emergency-bypass" and status == "READY":
        if item.get("allowed") is True:
            for field in ("approver", "maxDuration", "auditEvidence"):
                if not text_value(item.get(field)) or item.get(field) == "TBD":
                    fail(failures, f"emergency-bypass allowed=true requires non-placeholder {field}")
            check_real_evidence_reference("emergency-bypass", "auditEvidence", item.get("auditEvidence"), failures)
        elif item.get("allowed") is not False:
            fail(failures, "emergency-bypass.allowed must be true or false")

    return decision_id


def check_document(data: Any, failures: list[str], strict_ready: bool) -> None:
    if not isinstance(data, dict):
        fail(failures, "license decision must be a JSON object")
        return

    for field in (
        "schemaVersion",
        "generatedAt",
        "environment",
        "status",
        "productionMode",
        "decisionOwner",
        "testBypassPromotedToProduction",
        "decisions",
    ):
        if field not in data:
            fail(failures, f"missing top-level field: {field}")

    if data.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"invalid top-level status: {data.get('status')!r}")
    if data.get("productionMode") not in ALLOWED_PRODUCTION_MODES:
        fail(failures, f"invalid productionMode: {data.get('productionMode')!r}")
    if data.get("testBypassPromotedToProduction") is not False:
        fail(failures, "testBypassPromotedToProduction must be false")

    if strict_ready:
        if data.get("status") != "READY":
            fail(failures, "top-level status must be READY for strict production license readiness")
        if data.get("productionMode") not in STRICT_READY_ALLOWED_MODES:
            fail(failures, "productionMode must be a signed production mode for strict readiness")

    decisions = data.get("decisions")
    if not isinstance(decisions, list):
        fail(failures, "decisions must be a list")
        return

    seen: set[str] = set()
    for index, item in enumerate(decisions):
        decision_id = check_decision(index, item, failures, strict_ready)
        if not decision_id:
            continue
        if decision_id in seen:
            fail(failures, f"duplicate decision id: {decision_id}")
        seen.add(decision_id)

    missing = sorted(REQUIRED_DECISION_IDS - seen)
    if missing:
        fail(failures, "missing required license decisions: " + ", ".join(missing))
    extra = sorted(seen - REQUIRED_DECISION_IDS)
    if extra:
        fail(failures, "unknown license decisions: " + ", ".join(extra))

    check_secret_hygiene(data, "licenseDecision", failures)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate ADP production license strategy decision evidence.")
    parser.add_argument("--decision", type=Path, required=True)
    parser.add_argument("--strict-ready", action="store_true")
    args = parser.parse_args()

    try:
        data = json.loads(args.decision.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"FAIL: license decision file not found: {args.decision}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as error:
        print(f"FAIL: invalid JSON in license decision file: {error}", file=sys.stderr)
        return 1

    failures: list[str] = []
    check_document(data, failures, args.strict_ready)
    if failures:
        print(f"License decision validation failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("License decision validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
