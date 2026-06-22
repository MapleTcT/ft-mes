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
    "rendered-config-baseline",
    "production-nacos-export",
    "nacos-diff-review",
    "runtime-patch-package",
    "publish-window",
    "post-publish-nacos-smoke",
    "post-publish-platform-smoke",
    "rollback-link",
}

ALLOWED_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
PLACEHOLDER_VALUES = {"", "TBD", "TODO", "CHANGE_ME"}
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
    r"(?!CHANGE_ME|TBD|TODO|<|\$\{|\$|/secure/)[^\s#\"']{8,}"
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
    return not isinstance(value, str) or value.strip() in PLACEHOLDER_VALUES


def is_template_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return True
    stripped = value.strip()
    if stripped in PLACEHOLDER_VALUES:
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


def check_no_placeholders(value: Any, path: str, failures: list[str]) -> None:
    if isinstance(value, dict):
        for key, nested in value.items():
            check_no_placeholders(nested, f"{path}.{key}", failures)
        return
    if isinstance(value, list):
        for index, nested in enumerate(value):
            check_no_placeholders(nested, f"{path}[{index}]", failures)
        return
    if isinstance(value, str) and value.strip() in PLACEHOLDER_VALUES:
        fail(failures, f"{path} must not be a placeholder for strict Nacos/runtime readiness")


def check_artifact_reference(phase_id: str, field: str, value: str, failures: list[str]) -> None:
    if not value or value in PLACEHOLDER_VALUES:
        return
    if value.startswith(("http://", "https://", "s3://", "minio://")):
        return
    if "$" in value or "\n" in value:
        return
    if any(
        value.startswith(prefix)
        for prefix in (
            "make ",
            "curl ",
            "docker ",
            "kubectl ",
            "sha256sum ",
            "openssl ",
        )
    ):
        return
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        fail(failures, f"{phase_id}.{field} must be a safe repo-relative path or external evidence URI")
        return
    candidate = ROOT / relative
    if not candidate.exists():
        fail(failures, f"{phase_id}.{field} references a missing repo artifact: {value}")


def check_real_evidence_reference(phase_id: str, field: str, value: Any, failures: list[str]) -> None:
    if is_template_evidence_ref(value):
        fail(
            failures,
            f"{phase_id}.{field} must reference real production Nacos/runtime evidence, not a template/example/sample asset",
        )


def require_int_summary(
    phase_id: str,
    summary: dict[str, Any],
    key: str,
    failures: list[str],
    minimum: int = 0,
) -> int:
    value = summary.get(key)
    if not isinstance(value, int):
        fail(failures, f"{phase_id}.summary.{key} must be an integer")
        return 0
    if value < minimum:
        fail(failures, f"{phase_id}.summary.{key} must be >= {minimum}")
    return value


def check_ready_summary(phase_id: str, summary: dict[str, Any], failures: list[str]) -> None:
    if phase_id == "rendered-config-baseline":
        require_int_summary(phase_id, summary, "renderedDataIds", failures, minimum=1)
        errors = require_int_summary(phase_id, summary, "errors", failures)
        if errors != 0:
            fail(failures, f"{phase_id}.summary.errors must be 0")
    elif phase_id == "production-nacos-export":
        require_int_summary(phase_id, summary, "exportedDataIds", failures, minimum=1)
        require_int_summary(phase_id, summary, "exportedServices", failures, minimum=1)
        errors = require_int_summary(phase_id, summary, "errors", failures)
        if errors != 0:
            fail(failures, f"{phase_id}.summary.errors must be 0")
    elif phase_id == "nacos-diff-review":
        require_int_summary(phase_id, summary, "reviewedDataIds", failures, minimum=1)
        for key in ("criticalDrift", "oracleDefaults", "secretLeaks"):
            value = require_int_summary(phase_id, summary, key, failures)
            if value != 0:
                fail(failures, f"{phase_id}.summary.{key} must be 0")
        if is_placeholder(summary.get("approvedBy")):
            fail(failures, f"{phase_id}.summary.approvedBy must name a real approver or approval record")
    elif phase_id == "runtime-patch-package":
        require_int_summary(phase_id, summary, "manifestEntryCount", failures, minimum=1)
        if is_placeholder(summary.get("packageRef")):
            fail(failures, f"{phase_id}.summary.packageRef must identify the signed package")
        sha256 = text_value(summary.get("sha256"))
        if not re.fullmatch(r"[0-9a-fA-F]{64}", sha256):
            fail(failures, f"{phase_id}.summary.sha256 must be a 64-character SHA-256 digest")
        if is_template_evidence_ref(summary.get("signatureEvidence")):
            fail(failures, f"{phase_id}.summary.signatureEvidence must reference real signature evidence")
    elif phase_id == "publish-window":
        for key in ("windowApproved", "restartSequenceApproved", "rollbackWindowApproved"):
            if summary.get(key) is not True:
                fail(failures, f"{phase_id}.summary.{key} must be true")
    elif phase_id in {"post-publish-nacos-smoke", "post-publish-platform-smoke"}:
        if summary.get("status") != "PASS":
            fail(failures, f"{phase_id}.summary.status must be PASS")
        checks = require_int_summary(phase_id, summary, "checks", failures, minimum=1)
        passed = require_int_summary(phase_id, summary, "passed", failures, minimum=1)
        if checks != passed:
            fail(failures, f"{phase_id}.summary.passed must equal checks")
        if phase_id == "post-publish-nacos-smoke":
            for key in ("oracleResiduals",):
                value = require_int_summary(phase_id, summary, key, failures)
                if value != 0:
                    fail(failures, f"{phase_id}.summary.{key} must be 0")
            require_int_summary(phase_id, summary, "criticalServicesHealthy", failures, minimum=1)
    elif phase_id == "rollback-link":
        if summary.get("status") != "READY":
            fail(failures, f"{phase_id}.summary.status must be READY")
        if is_template_evidence_ref(summary.get("rollbackEvidence")):
            fail(failures, f"{phase_id}.summary.rollbackEvidence must reference real rollback evidence")


def check_phase(index: int, phase: Any, failures: list[str], strict_ready: bool) -> str | None:
    if not isinstance(phase, dict):
        fail(failures, f"phases[{index}] must be an object")
        return None

    phase_id = text_value(phase.get("id"))
    if not phase_id:
        fail(failures, f"phases[{index}] missing id")
        return None
    if phase_id not in REQUIRED_PHASE_IDS:
        fail(failures, f"unknown Nacos/runtime patch phase id: {phase_id}")

    for field in ("title", "status", "owner", "evidenceRefs", "summary"):
        if field not in phase:
            fail(failures, f"{phase_id} missing required field: {field}")

    status = phase.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{phase_id} has invalid status: {status!r}")
        return phase_id

    for field in ("title", "owner"):
        if not text_value(phase.get(field)):
            fail(failures, f"{phase_id} missing non-empty {field}")

    evidence_refs = as_list(phase.get("evidenceRefs"))
    if not evidence_refs:
        fail(failures, f"{phase_id} must include evidenceRefs")
    for ref in evidence_refs:
        if isinstance(ref, str):
            check_artifact_reference(phase_id, "evidenceRefs", ref, failures)
        else:
            fail(failures, f"{phase_id}.evidenceRefs must contain strings")

    summary = as_dict(phase.get("summary"))
    if not summary:
        fail(failures, f"{phase_id} must include summary")

    if status == "READY":
        if as_list(phase.get("blockingIssues")):
            fail(failures, f"{phase_id} READY must not include blockingIssues")
        for ref in evidence_refs:
            check_real_evidence_reference(phase_id, "evidenceRefs", ref, failures)
        check_ready_summary(phase_id, summary, failures)
    else:
        if not as_list(phase.get("blockingIssues")):
            fail(failures, f"{phase_id} {status} must include blockingIssues")
        if not as_list(phase.get("nextActions")):
            fail(failures, f"{phase_id} {status} must include nextActions")
        if strict_ready:
            fail(failures, f"{phase_id} must be READY for strict Nacos/runtime readiness")

    return phase_id


def check_document(data: Any, failures: list[str], strict_ready: bool) -> None:
    if not isinstance(data, dict):
        fail(failures, "Nacos/runtime patch evidence must be a JSON object")
        return

    for field in (
        "schemaVersion",
        "generatedAt",
        "environment",
        "status",
        "owner",
        "repoCommit",
        "targetNacosGroup",
        "publishWindow",
        "runtimePatchManifest",
        "phases",
    ):
        if field not in data:
            fail(failures, f"missing top-level field: {field}")

    if data.get("environment") != "production":
        fail(failures, "environment must be production")
    if data.get("status") not in ALLOWED_STATUSES:
        fail(failures, f"invalid top-level status: {data.get('status')!r}")
    if data.get("targetNacosGroup") != "prod":
        fail(failures, "targetNacosGroup must be prod")

    runtime_manifest = text_value(data.get("runtimePatchManifest"))
    check_artifact_reference("document", "runtimePatchManifest", runtime_manifest, failures)

    if strict_ready:
        if data.get("status") != "READY":
            fail(failures, "top-level status must be READY for strict Nacos/runtime readiness")
        for field in ("generatedAt", "owner", "repoCommit", "publishWindow"):
            if is_placeholder(data.get(field)):
                fail(failures, f"{field} must not be a placeholder for strict Nacos/runtime readiness")

    phases = data.get("phases")
    if not isinstance(phases, list):
        fail(failures, "phases must be a list")
        return

    seen_ids: set[str] = set()
    statuses: list[str] = []
    for index, phase in enumerate(phases):
        phase_id = check_phase(index, phase, failures, strict_ready)
        if phase_id:
            if phase_id in seen_ids:
                fail(failures, f"duplicate Nacos/runtime patch phase id: {phase_id}")
            seen_ids.add(phase_id)
            if isinstance(phase, dict):
                statuses.append(str(phase.get("status")))

    missing = REQUIRED_PHASE_IDS - seen_ids
    if missing:
        fail(failures, f"missing Nacos/runtime patch phases: {', '.join(sorted(missing))}")

    if data.get("status") == "READY":
        if any(status != "READY" for status in statuses):
            fail(failures, "top-level READY requires every phase to be READY")
        if as_list(data.get("openIssues")):
            fail(failures, "top-level READY must not include openIssues")

    if strict_ready:
        check_no_placeholders(data, "document", failures)

    check_secret_hygiene(data, "document", failures)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", required=True, help="Nacos/runtime patch evidence JSON")
    parser.add_argument("--strict-ready", action="store_true", help="require real READY evidence")
    args = parser.parse_args()

    path = Path(args.evidence)
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"FAIL: evidence file not found: {path}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as error:
        print(f"FAIL: invalid JSON in {path}: {error}", file=sys.stderr)
        return 1

    failures: list[str] = []
    check_document(data, failures, args.strict_ready)
    if failures:
        print(f"Nacos/runtime patch evidence validation failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1

    print("Nacos/runtime patch evidence validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
