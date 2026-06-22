#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[4]

REQUIRED_PHASE_IDS = {
    "source-inventory",
    "target-preflight",
    "row-count-comparison",
    "checksum-comparison",
    "postgres-runtime-smoke",
    "rollback-link",
}

ALLOWED_TOP_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
ALLOWED_PHASE_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
PLACEHOLDER_EVIDENCE_MARKERS = (
    ".example",
    "example.",
    "-example",
    "_example",
    "template",
    "sample",
)
PLACEHOLDER_VALUES = {"TBD", "TODO", "CHANGE_ME"}
READY_REQUIRED_TOP_FIELDS = ["owner", "repoCommit", "migrationWindow", "sourceSnapshot", "targetSnapshot"]

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


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def text_value(value: Any) -> str:
    return value.strip() if isinstance(value, str) else ""


def is_placeholder(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    stripped = value.strip()
    return not stripped or stripped in PLACEHOLDER_VALUES


def is_template_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    stripped = value.strip()
    if is_placeholder(stripped):
        return True
    lowered = stripped.lower()
    name = Path(stripped).name.lower()
    return any(marker in lowered or marker in name for marker in PLACEHOLDER_EVIDENCE_MARKERS)


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


def external_ref(value: str) -> bool:
    return value.startswith(("http://", "https://", "s3://", "minio://", "evidence://", "artifact://"))


def check_artifact_reference(phase_id: str, value: Any, failures: list[str]) -> None:
    if not isinstance(value, str) or not value.strip():
        fail(failures, f"{phase_id}.evidenceRefs must contain non-empty strings")
        return
    stripped = value.strip()
    if external_ref(stripped):
        return
    if "$" in stripped or "\n" in stripped:
        return
    relative = Path(stripped)
    if relative.is_absolute() or ".." in relative.parts:
        fail(failures, f"{phase_id}.evidenceRefs must be safe repo-relative paths or external evidence URIs: {value}")
        return
    if not (ROOT / relative).exists():
        fail(failures, f"{phase_id}.evidenceRefs references a missing repo artifact: {value}")


def check_real_evidence_reference(phase_id: str, value: Any, failures: list[str]) -> None:
    if is_template_evidence_ref(value):
        fail(
            failures,
            f"{phase_id}.evidenceRefs must reference real migration rehearsal evidence, not a template/example/sample asset",
        )


def summary_int(summary: dict[str, Any], key: str) -> int | None:
    value = summary.get(key)
    return value if isinstance(value, int) else None


def check_ready_summary(phase_id: str, summary: dict[str, Any], failures: list[str]) -> None:
    if phase_id in {"source-inventory", "target-preflight"}:
        total = summary_int(summary, "totalTables")
        errors = summary_int(summary, "errors")
        if total is None or total <= 0:
            fail(failures, f"{phase_id} READY requires summary.totalTables > 0")
        if errors != 0:
            fail(failures, f"{phase_id} READY requires summary.errors == 0")
        return
    if phase_id == "row-count-comparison":
        total = summary_int(summary, "totalTables")
        mismatch = summary_int(summary, "mismatch")
        errors = summary_int(summary, "errors")
        if total is None or total <= 0:
            fail(failures, f"{phase_id} READY requires summary.totalTables > 0")
        if mismatch != 0 or errors != 0:
            fail(failures, f"{phase_id} READY requires zero mismatches and errors")
        return
    if phase_id == "checksum-comparison":
        total = summary_int(summary, "totalTables")
        mismatch = summary_int(summary, "mismatch")
        missing = summary_int(summary, "missing")
        errors = summary_int(summary, "errors")
        if total is None or total <= 0:
            fail(failures, f"{phase_id} READY requires summary.totalTables > 0")
        if mismatch != 0 or missing != 0 or errors != 0:
            fail(failures, f"{phase_id} READY requires zero mismatches, missing tables and errors")
        return
    if phase_id == "postgres-runtime-smoke":
        if summary.get("status") != "PASS":
            fail(failures, f"{phase_id} READY requires summary.status == PASS")
        if summary_int(summary, "checks") in {None, 0}:
            fail(failures, f"{phase_id} READY requires summary.checks > 0")
        if summary.get("checks") != summary.get("passed"):
            fail(failures, f"{phase_id} READY requires all runtime smoke checks to pass")
        return
    if phase_id == "rollback-link" and summary.get("status") != "READY":
        fail(failures, f"{phase_id} READY requires summary.status == READY")


def check_phase(index: int, phase: Any, failures: list[str], strict_ready: bool) -> str | None:
    if not isinstance(phase, dict):
        fail(failures, f"phases[{index}] must be an object")
        return None
    phase_id = text_value(phase.get("id"))
    if not phase_id:
        fail(failures, f"phases[{index}] missing id")
        return None
    status = phase.get("status")
    if status not in ALLOWED_PHASE_STATUSES:
        fail(failures, f"{phase_id} has invalid status: {status!r}")
        return phase_id
    for field in ("title", "evidenceRefs", "commands", "summary", "blockingIssues", "nextActions"):
        if field not in phase:
            fail(failures, f"{phase_id} missing required field: {field}")
    if not isinstance(phase.get("evidenceRefs"), list):
        fail(failures, f"{phase_id}.evidenceRefs must be a list")
    if not isinstance(phase.get("commands"), list):
        fail(failures, f"{phase_id}.commands must be a list")
    if not isinstance(phase.get("summary"), dict):
        fail(failures, f"{phase_id}.summary must be an object")
    if not isinstance(phase.get("blockingIssues"), list):
        fail(failures, f"{phase_id}.blockingIssues must be a list")
    if not isinstance(phase.get("nextActions"), list):
        fail(failures, f"{phase_id}.nextActions must be a list")

    for ref in as_list(phase.get("evidenceRefs")):
        check_artifact_reference(phase_id, ref, failures)

    if status == "READY":
        refs = as_list(phase.get("evidenceRefs"))
        if not refs:
            fail(failures, f"{phase_id} READY requires evidenceRefs")
        for ref in refs:
            check_real_evidence_reference(phase_id, ref, failures)
        if as_list(phase.get("blockingIssues")):
            fail(failures, f"{phase_id} READY must not include blockingIssues")
        check_ready_summary(phase_id, as_dict(phase.get("summary")), failures)
    else:
        if not as_list(phase.get("blockingIssues")):
            fail(failures, f"{phase_id} {status} must include blockingIssues")
        if not as_list(phase.get("nextActions")):
            fail(failures, f"{phase_id} {status} must include nextActions")
        if strict_ready:
            fail(failures, f"{phase_id} must be READY for strict database migration readiness")
    return phase_id


def check_snapshot(label: str, value: Any, failures: list[str], strict_ready: bool) -> None:
    snapshot = as_dict(value)
    if not snapshot:
        fail(failures, f"{label} must be an object")
        return
    for field in ("type", "capturedAt", "evidence"):
        if field not in snapshot:
            fail(failures, f"{label} missing required field: {field}")
    if strict_ready:
        for field in ("type", "capturedAt", "evidence"):
            if is_placeholder(snapshot.get(field)):
                fail(failures, f"{label}.{field} must be non-placeholder for strict database migration readiness")
        check_real_evidence_reference(label, snapshot.get("evidence"), failures)


def check_evidence(data: Any, failures: list[str], strict_ready: bool) -> None:
    if not isinstance(data, dict):
        fail(failures, "migration evidence must be a JSON object")
        return

    for field in (
        "schemaVersion",
        "generatedAt",
        "status",
        "database",
        "environment",
        "owner",
        "repoCommit",
        "migrationWindow",
        "sourceSnapshot",
        "targetSnapshot",
        "phases",
    ):
        if field not in data:
            fail(failures, f"missing top-level field: {field}")

    if data.get("database") != "PostgreSQL":
        fail(failures, "database migration target must remain PostgreSQL")
    if data.get("status") not in ALLOWED_TOP_STATUSES:
        fail(failures, f"invalid top-level status: {data.get('status')!r}")
    if strict_ready and data.get("status") != "READY":
        fail(failures, "top-level status must be READY for strict database migration readiness")

    if strict_ready:
        for field in READY_REQUIRED_TOP_FIELDS:
            if is_placeholder(data.get(field)):
                fail(failures, f"{field} must be non-placeholder for strict database migration readiness")

    check_snapshot("sourceSnapshot", data.get("sourceSnapshot"), failures, strict_ready)
    check_snapshot("targetSnapshot", data.get("targetSnapshot"), failures, strict_ready)

    phases = data.get("phases")
    if not isinstance(phases, list):
        fail(failures, "phases must be a list")
        return

    seen: set[str] = set()
    for index, phase in enumerate(phases):
        phase_id = check_phase(index, phase, failures, strict_ready)
        if not phase_id:
            continue
        if phase_id in seen:
            fail(failures, f"duplicate phase id: {phase_id}")
        seen.add(phase_id)

    missing = sorted(REQUIRED_PHASE_IDS - seen)
    if missing:
        fail(failures, "missing required migration phases: " + ", ".join(missing))
    extra = sorted(seen - REQUIRED_PHASE_IDS)
    if extra:
        fail(failures, "unknown migration phases: " + ", ".join(extra))

    check_secret_hygiene(data, "migrationEvidence", failures)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate ADP production database migration rehearsal evidence.")
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--strict-ready", action="store_true")
    args = parser.parse_args()

    try:
        data = json.loads(args.evidence.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"FAIL: migration evidence file not found: {args.evidence}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as error:
        print(f"FAIL: invalid JSON in migration evidence file: {error}", file=sys.stderr)
        return 1

    failures: list[str] = []
    check_evidence(data, failures, args.strict_ready)
    if failures:
        print(f"Database migration evidence validation failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Database migration evidence validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
