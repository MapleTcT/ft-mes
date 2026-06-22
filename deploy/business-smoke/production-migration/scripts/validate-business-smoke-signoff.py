#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[4]

REQUIRED_SMOKE_SUITE_IDS = {
    "platform-login-auth",
    "user-organization-rbac",
    "menu-todo-dashboard",
    "basic-configuration",
    "nacos-keycloak-postgresql-runtime",
    "production-wom-main-flow",
    "quality-qcs-lims",
    "import-export-upload",
    "business-page-coverage",
    "postgres-gap-ledger",
}

REQUIRED_PERSISTENCE_SCOPE_IDS = {
    "platform-crud-persistence",
    "production-write-actions",
    "postgres-gap-closure",
}

REQUIRED_SIGNOFF_ROLES = {
    "business-owner",
    "technical-owner",
    "database-owner",
    "release-owner",
}

ALLOWED_TOP_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
ALLOWED_ITEM_STATUSES = {"PASS", "FAIL", "BLOCKED", "PLANNED", "NOT_APPLICABLE"}
ALLOWED_SIGNOFF_DECISIONS = {"APPROVED", "BLOCKED", "REJECTED", "NOT_APPLICABLE"}
ALLOWED_RISK_ACCEPTANCE_DECISIONS = {"ACCEPTED", "REJECTED"}
DEFAULT_BLOCKER_LEDGER = ROOT / "metadata/production-module-blockers.json"
DEFAULT_BACKLOG_LEDGER = ROOT / "metadata/production-module-backlog.json"
DEFAULT_BASIC_CONFIG_COVERAGE = ROOT / "metadata/basic-config-coverage.json"
DEFAULT_EXPORT_GAP_BREAKDOWN = ROOT / "metadata/production-export-gap-breakdown.json"
PLACEHOLDER_EVIDENCE_MARKERS = (
    ".example",
    "example.",
    "-example",
    "_example",
    "template",
    "sample",
)
TEST_ENVIRONMENT_EVIDENCE_MARKERS = (
    "100.99.133.43",
    "10.11.100.17",
    "222.88.185.146",
    "ubuntu-test",
    "v6-2288H-V6",
    "metadata/test-environment-smoke.json",
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


def is_test_environment_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    lowered = value.strip().lower()
    return any(marker.lower() in lowered for marker in TEST_ENVIRONMENT_EVIDENCE_MARKERS)


def check_real_evidence_reference(item_id: str, field: str, value: Any, failures: list[str]) -> None:
    if is_template_evidence_ref(value):
        fail(
            failures,
            f"{item_id}.{field} must reference real production/rehearsal evidence, not a template/example/sample asset",
        )


def check_production_ready_reference(item_id: str, field: str, value: Any, failures: list[str]) -> None:
    if is_test_environment_evidence_ref(value):
        fail(
            failures,
            f"{item_id}.{field} must reference production/cutover rehearsal evidence, not test-environment evidence",
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
        fail(failures, f"{path} must not be a placeholder for strict business smoke signoff readiness")


def check_artifact_reference(item_id: str, field: str, value: str, failures: list[str]) -> None:
    if not value or value == "TBD":
        return
    if value.startswith(("http://", "https://", "s3://", "minio://")):
        return
    if "$" in value or "\n" in value:
        return
    if any(value.startswith(prefix) for prefix in ("make ", "node ", "psql ", "SELECT ", "curl ")):
        return
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        fail(failures, f"{item_id}.{field} must be a safe repo-relative path or external evidence URI")
        return
    candidate = ROOT / relative
    if not candidate.exists():
        fail(failures, f"{item_id}.{field} references a missing repo artifact: {value}")


def load_json(path: Path, label: str, failures: list[str]) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        fail(failures, f"{label} file not found: {path}")
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {label}: {error}")
    return None


def blocker_case_ids(blocker_ledger: Any, failures: list[str]) -> set[str]:
    if not isinstance(blocker_ledger, dict):
        fail(failures, "production module blocker ledger must be a JSON object")
        return set()

    if blocker_ledger.get("database") != "PostgreSQL":
        fail(failures, "production module blocker ledger database must remain PostgreSQL")

    blockers = blocker_ledger.get("blockers")
    if not isinstance(blockers, list):
        fail(failures, "production module blocker ledger blockers must be a list")
        return set()

    seen: set[str] = set()
    for index, blocker in enumerate(blockers):
        if not isinstance(blocker, dict):
            fail(failures, f"production module blocker ledger blockers[{index}] must be an object")
            continue
        case_id = text_value(blocker.get("caseId"))
        if not case_id:
            fail(failures, f"production module blocker ledger blockers[{index}] missing caseId")
            continue
        if case_id in seen:
            fail(failures, f"production module blocker ledger has duplicate caseId: {case_id}")
        seen.add(case_id)
        if blocker.get("status") != "BLOCKED":
            fail(failures, f"production module blocker {case_id} must remain BLOCKED until resolved")

    summary = blocker_ledger.get("summary") if isinstance(blocker_ledger.get("summary"), dict) else {}
    summary_blockers = summary.get("blockers")
    if isinstance(summary_blockers, int) and summary_blockers != len(seen):
        fail(failures, "production module blocker ledger summary.blockers must match blockers length")
    summary_blocked_cases = summary.get("blockedCases")
    if isinstance(summary_blocked_cases, int) and summary_blocked_cases != len(seen):
        fail(failures, "production module blocker ledger summary.blockedCases must match blockers length")
    return seen


def backlog_item_ids(backlog_ledger: Any, failures: list[str]) -> set[str]:
    if not isinstance(backlog_ledger, dict):
        fail(failures, "production module backlog ledger must be a JSON object")
        return set()

    if backlog_ledger.get("database") != "PostgreSQL":
        fail(failures, "production module backlog ledger database must remain PostgreSQL")

    items = backlog_ledger.get("items")
    if not isinstance(items, list):
        fail(failures, "production module backlog ledger items must be a list")
        return set()

    seen: set[str] = set()
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            fail(failures, f"production module backlog ledger items[{index}] must be an object")
            continue
        item_id = text_value(item.get("id"))
        if not item_id:
            fail(failures, f"production module backlog ledger items[{index}] missing id")
            continue
        if item_id in seen:
            fail(failures, f"production module backlog ledger has duplicate id: {item_id}")
        seen.add(item_id)
        if item.get("status") not in {"FAIL_BACKLOG", "BLOCKED"}:
            fail(failures, f"production module backlog {item_id} must remain FAIL_BACKLOG/BLOCKED until resolved")

    summary = backlog_ledger.get("summary") if isinstance(backlog_ledger.get("summary"), dict) else {}
    summary_items = summary.get("totalItems")
    if isinstance(summary_items, int) and summary_items != len(seen):
        fail(failures, "production module backlog summary.totalItems must match items length")
    return seen


def collect_incomplete_basic_config_area_ids(coverage: Any, failures: list[str]) -> set[str]:
    if not isinstance(coverage, dict):
        fail(failures, "basic config coverage ledger must be a JSON object")
        return set()

    if coverage.get("database") != "PostgreSQL":
        fail(failures, "basic config coverage ledger database must remain PostgreSQL")
    if coverage.get("goalId") != "G-012":
        fail(failures, "basic config coverage ledger must describe G-012")

    areas = coverage.get("areas")
    if not isinstance(areas, list):
        fail(failures, "basic config coverage ledger areas must be a list")
        return set()

    incomplete: set[str] = set()
    for index, area in enumerate(areas):
        if not isinstance(area, dict):
            fail(failures, f"basic config coverage areas[{index}] must be an object")
            continue
        area_id = text_value(area.get("id"))
        if not area_id:
            fail(failures, f"basic config coverage areas[{index}] missing id")
            continue
        if area.get("status") not in {"PASS", "NOT_APPLICABLE"}:
            incomplete.add(area_id)

    if not incomplete and coverage.get("overallStatus") != "PASS":
        fail(failures, "basic config coverage overallStatus must be PASS when all areas are PASS/NOT_APPLICABLE")
    if incomplete and coverage.get("overallStatus") in {"PASS", "READY", "COMPLETE"}:
        fail(failures, "basic config coverage overallStatus cannot be complete while areas are incomplete")
    return incomplete


def unresolved_export_target_ids(export_gap_breakdown: Any, failures: list[str]) -> set[str]:
    if not isinstance(export_gap_breakdown, dict):
        fail(failures, "production export gap breakdown must be a JSON object")
        return set()

    if export_gap_breakdown.get("database") != "PostgreSQL":
        fail(failures, "production export gap breakdown database must remain PostgreSQL")
    if export_gap_breakdown.get("caseId") != "PROD-023":
        fail(failures, "production export gap breakdown must describe PROD-023")

    items = export_gap_breakdown.get("items")
    if not isinstance(items, list):
        fail(failures, "production export gap breakdown items must be a list")
        return set()

    seen: set[str] = set()
    unresolved: set[str] = set()
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            fail(failures, f"production export gap breakdown items[{index}] must be an object")
            continue
        target_id = text_value(item.get("id"))
        if not target_id:
            fail(failures, f"production export gap breakdown items[{index}] missing id")
            continue
        if target_id in seen:
            fail(failures, f"production export gap breakdown has duplicate target id: {target_id}")
        seen.add(target_id)
        if item.get("status") != "READY" or item.get("verifiedDataExport") is not True:
            unresolved.add(target_id)

    summary = export_gap_breakdown.get("summary") if isinstance(export_gap_breakdown.get("summary"), dict) else {}
    summary_targets = summary.get("targets")
    if isinstance(summary_targets, int) and summary_targets != len(seen):
        fail(failures, "production export gap breakdown summary.targets must match items length")
    summary_verified = summary.get("verifiedDataExports")
    if isinstance(summary_verified, int) and summary_verified != len(seen - unresolved):
        fail(failures, "production export gap breakdown summary.verifiedDataExports must match READY target count")
    return unresolved


def issue_blocker_ids(issue: dict[str, Any]) -> set[str]:
    ids = issue.get("blockerCaseIds")
    if isinstance(ids, list):
        return {value for value in ids if isinstance(value, str) and value.strip()}
    return set()


def issue_backlog_ids(issue: dict[str, Any]) -> set[str]:
    ids = issue.get("backlogItemIds")
    if isinstance(ids, list):
        return {value for value in ids if isinstance(value, str) and value.strip()}
    return set()


def issue_basic_config_area_ids(issue: dict[str, Any]) -> set[str]:
    ids = issue.get("basicConfigAreaIds")
    if isinstance(ids, list):
        return {value for value in ids if isinstance(value, str) and value.strip()}
    return set()


def issue_export_target_ids(issue: dict[str, Any]) -> set[str]:
    ids = issue.get("exportTargetIds")
    if isinstance(ids, list):
        return {value for value in ids if isinstance(value, str) and value.strip()}
    return set()


def risk_acceptance_is_signed(issue: dict[str, Any], context: str, failures: list[str], strict_ready: bool) -> bool:
    risk_acceptance = issue.get("riskAcceptance")
    if isinstance(risk_acceptance, str):
        if strict_ready:
            fail(failures, f"{context}.riskAcceptance must be a signed object for strict readiness")
            return False
        return bool(risk_acceptance.strip()) and risk_acceptance.strip() != "TBD"

    if not isinstance(risk_acceptance, dict):
        if strict_ready:
            fail(failures, f"{context}.riskAcceptance must include signed decision/acceptedBy/signedAt/evidence")
        return False

    for field in ("decision", "acceptedBy", "signedAt", "evidence"):
        if field not in risk_acceptance:
            fail(failures, f"{context}.riskAcceptance missing required field: {field}")

    decision = risk_acceptance.get("decision")
    if decision not in ALLOWED_RISK_ACCEPTANCE_DECISIONS:
        fail(failures, f"{context}.riskAcceptance.decision must be ACCEPTED or REJECTED")

    evidence = text_value(risk_acceptance.get("evidence"))
    check_artifact_reference(context, "riskAcceptance.evidence", evidence, failures)
    if strict_ready:
        if decision != "ACCEPTED":
            fail(failures, f"{context}.riskAcceptance.decision must be ACCEPTED for strict readiness")
        for field in ("acceptedBy", "signedAt", "evidence"):
            if is_placeholder(risk_acceptance.get(field)):
                fail(failures, f"{context}.riskAcceptance.{field} must be non-placeholder for strict readiness")
        check_real_evidence_reference(context, "riskAcceptance.evidence", risk_acceptance.get("evidence"), failures)
        check_production_ready_reference(context, "riskAcceptance.evidence", risk_acceptance.get("evidence"), failures)

    return (
        decision == "ACCEPTED"
        and bool(text_value(risk_acceptance.get("acceptedBy")))
        and bool(text_value(risk_acceptance.get("signedAt")))
        and bool(evidence)
        and not is_template_evidence_ref(evidence)
    )


def check_blocker_ledger_gate(
    data: dict[str, Any],
    blocker_ids: set[str],
    failures: list[str],
    strict_ready: bool,
) -> None:
    if not blocker_ids:
        return

    ledger_ref = text_value(data.get("blockerLedger"))
    if ledger_ref != "metadata/production-module-blockers.json":
        fail(failures, "blockerLedger must reference metadata/production-module-blockers.json while production blockers exist")
    check_artifact_reference("businessSmokeSignoff", "blockerLedger", ledger_ref, failures)

    issues = data.get("openIssues")
    if not isinstance(issues, list):
        return

    covered: set[str] = set()
    risk_accepted: set[str] = set()
    for index, issue in enumerate(issues):
        if not isinstance(issue, dict):
            continue
        ids = issue_blocker_ids(issue)
        if not ids:
            continue
        unknown = ids - blocker_ids
        if unknown:
            fail(failures, f"openIssues[{index}].blockerCaseIds contains unknown production blocker ids: {', '.join(sorted(unknown))}")
        covered |= ids & blocker_ids
        if risk_acceptance_is_signed(issue, f"openIssues[{index}]", failures, strict_ready):
            risk_accepted |= ids & blocker_ids

    missing_coverage = blocker_ids - covered
    if missing_coverage:
        fail(failures, "openIssues must cover every production blocker case id: " + ", ".join(sorted(missing_coverage)))

    if data.get("status") == "READY" or strict_ready:
        missing_acceptance = blocker_ids - risk_accepted
        if missing_acceptance:
            fail(
                failures,
                "READY business smoke signoff requires explicit riskAcceptance for production blockers: "
                + ", ".join(sorted(missing_acceptance)),
            )


def check_backlog_ledger_gate(
    data: dict[str, Any],
    backlog_ids: set[str],
    failures: list[str],
    strict_ready: bool,
) -> None:
    if not backlog_ids:
        return

    ledger_ref = text_value(data.get("backlogLedger"))
    if ledger_ref != "metadata/production-module-backlog.json":
        fail(failures, "backlogLedger must reference metadata/production-module-backlog.json while production backlog exists")
    check_artifact_reference("businessSmokeSignoff", "backlogLedger", ledger_ref, failures)

    issues = data.get("openIssues")
    if not isinstance(issues, list):
        return

    covered: set[str] = set()
    risk_accepted: set[str] = set()
    for index, issue in enumerate(issues):
        if not isinstance(issue, dict):
            continue
        ids = issue_backlog_ids(issue)
        if not ids:
            continue
        unknown = ids - backlog_ids
        if unknown:
            fail(failures, f"openIssues[{index}].backlogItemIds contains unknown production backlog ids: {', '.join(sorted(unknown))}")
        covered |= ids & backlog_ids
        if risk_acceptance_is_signed(issue, f"openIssues[{index}]", failures, strict_ready):
            risk_accepted |= ids & backlog_ids

    missing_coverage = backlog_ids - covered
    if missing_coverage:
        fail(failures, "openIssues must cover every production backlog item id: " + ", ".join(sorted(missing_coverage)))

    if data.get("status") == "READY" or strict_ready:
        missing_acceptance = backlog_ids - risk_accepted
        if missing_acceptance:
            fail(
                failures,
                "READY business smoke signoff requires explicit riskAcceptance for production backlog items: "
                + ", ".join(sorted(missing_acceptance)),
            )


def check_basic_config_coverage_gate(
    data: dict[str, Any],
    incomplete_area_ids: set[str],
    failures: list[str],
    strict_ready: bool,
) -> None:
    ledger_ref = text_value(data.get("basicConfigCoverageLedger"))
    if ledger_ref != "metadata/basic-config-coverage.json":
        fail(failures, "basicConfigCoverageLedger must reference metadata/basic-config-coverage.json")
    check_artifact_reference("businessSmokeSignoff", "basicConfigCoverageLedger", ledger_ref, failures)

    issues = data.get("openIssues")
    if not isinstance(issues, list):
        return

    covered: set[str] = set()
    risk_accepted: set[str] = set()
    for index, issue in enumerate(issues):
        if not isinstance(issue, dict):
            continue
        ids = issue_basic_config_area_ids(issue)
        if not ids:
            continue
        unknown = ids - incomplete_area_ids
        if unknown and incomplete_area_ids:
            fail(failures, f"openIssues[{index}].basicConfigAreaIds contains unknown or already-complete basic config area ids: {', '.join(sorted(unknown))}")
        covered |= ids & incomplete_area_ids
        if risk_acceptance_is_signed(issue, f"openIssues[{index}]", failures, strict_ready):
            risk_accepted |= ids & incomplete_area_ids

    if incomplete_area_ids:
        missing_coverage = incomplete_area_ids - covered
        if missing_coverage:
            fail(failures, "openIssues must cover every incomplete basic config area id: " + ", ".join(sorted(missing_coverage)))

    if data.get("status") == "READY" or strict_ready:
        if incomplete_area_ids:
            fail(
                failures,
                "READY business smoke signoff requires basic config coverage to be PASS; incomplete areas: "
                + ", ".join(sorted(incomplete_area_ids)),
            )
        missing_acceptance = incomplete_area_ids - risk_accepted
        if missing_acceptance:
            fail(
                failures,
                "READY business smoke signoff requires explicit riskAcceptance for incomplete basic config areas: "
                + ", ".join(sorted(missing_acceptance)),
            )


def check_export_gap_gate(
    data: dict[str, Any],
    unresolved_export_targets: set[str],
    failures: list[str],
    strict_ready: bool,
) -> None:
    ledger_ref = text_value(data.get("exportGapBreakdown"))
    if ledger_ref != "metadata/production-export-gap-breakdown.json":
        fail(failures, "exportGapBreakdown must reference metadata/production-export-gap-breakdown.json")
    check_artifact_reference("businessSmokeSignoff", "exportGapBreakdown", ledger_ref, failures)

    if not unresolved_export_targets:
        return

    issues = data.get("openIssues")
    if not isinstance(issues, list):
        return

    covered: set[str] = set()
    risk_accepted: set[str] = set()
    for index, issue in enumerate(issues):
        if not isinstance(issue, dict):
            continue
        ids = issue_export_target_ids(issue)
        if not ids:
            continue
        unknown = ids - unresolved_export_targets
        if unknown:
            fail(
                failures,
                f"openIssues[{index}].exportTargetIds contains unknown or already-resolved export target ids: "
                + ", ".join(sorted(unknown)),
            )
        covered |= ids & unresolved_export_targets
        if risk_acceptance_is_signed(issue, f"openIssues[{index}]", failures, strict_ready):
            risk_accepted |= ids & unresolved_export_targets

    missing_coverage = unresolved_export_targets - covered
    if missing_coverage:
        fail(
            failures,
            "openIssues must cover every unresolved production export target id: "
            + ", ".join(sorted(missing_coverage)),
        )

    if data.get("status") == "READY" or strict_ready:
        missing_acceptance = unresolved_export_targets - risk_accepted
        if missing_acceptance:
            fail(
                failures,
                "READY business smoke signoff requires explicit riskAcceptance for production export targets: "
                + ", ".join(sorted(missing_acceptance)),
            )


def check_smoke_suite(index: int, item: Any, failures: list[str], strict_ready: bool) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"smokeSuites[{index}] must be an object")
        return None

    item_id = text_value(item.get("id"))
    if not item_id:
        fail(failures, f"smokeSuites[{index}] missing id")
        return None

    for field in ("title", "status", "routeOrArea", "evidence", "requiresPersistence", "owner"):
        if field not in item:
            fail(failures, f"{item_id} missing required field: {field}")

    status = item.get("status")
    if status not in ALLOWED_ITEM_STATUSES:
        fail(failures, f"{item_id} has invalid status: {status!r}")
        return item_id

    for field in ("title", "routeOrArea", "owner"):
        if not text_value(item.get(field)):
            fail(failures, f"{item_id} missing non-empty {field}")
    if not isinstance(item.get("requiresPersistence"), bool):
        fail(failures, f"{item_id}.requiresPersistence must be boolean")
    check_artifact_reference(item_id, "evidence", text_value(item.get("evidence")), failures)

    if status == "PASS":
        if is_placeholder(item.get("evidence")):
            fail(failures, f"{item_id} PASS requires non-placeholder evidence")
        check_real_evidence_reference(item_id, "evidence", item.get("evidence"), failures)
        if strict_ready:
            check_production_ready_reference(item_id, "evidence", item.get("evidence"), failures)
        if as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} PASS must not include blockingIssues")
    elif status == "NOT_APPLICABLE":
        reason = text_value(item.get("notApplicableReason"))
        if not reason or reason == "TBD":
            fail(failures, f"{item_id} NOT_APPLICABLE requires notApplicableReason")
    else:
        if not as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} {status} must include blockingIssues")
        if not as_list(item.get("nextActions")):
            fail(failures, f"{item_id} {status} must include nextActions")
        if strict_ready:
            fail(failures, f"{item_id} must be PASS or NOT_APPLICABLE for strict business smoke signoff")

    return item_id


def check_persistence_scope(index: int, item: Any, failures: list[str], strict_ready: bool) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"persistenceScopes[{index}] must be an object")
        return None

    item_id = text_value(item.get("id"))
    if not item_id:
        fail(failures, f"persistenceScopes[{index}] missing id")
        return None

    for field in ("title", "status", "tables", "evidence", "owner"):
        if field not in item:
            fail(failures, f"{item_id} missing required field: {field}")

    status = item.get("status")
    if status not in ALLOWED_ITEM_STATUSES:
        fail(failures, f"{item_id} has invalid status: {status!r}")
        return item_id
    if status == "NOT_APPLICABLE":
        fail(failures, f"{item_id} cannot be NOT_APPLICABLE; persistence scopes are required")

    if not as_list(item.get("tables")):
        fail(failures, f"{item_id} must list target tables or table groups")
    if not text_value(item.get("owner")):
        fail(failures, f"{item_id} missing owner")
    check_artifact_reference(item_id, "evidence", text_value(item.get("evidence")), failures)

    if status == "PASS":
        if is_placeholder(item.get("evidence")):
            fail(failures, f"{item_id} PASS requires non-placeholder evidence")
        check_real_evidence_reference(item_id, "evidence", item.get("evidence"), failures)
        if strict_ready:
            check_production_ready_reference(item_id, "evidence", item.get("evidence"), failures)
        if as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} PASS must not include blockingIssues")
    else:
        if not as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} {status} must include blockingIssues")
        if not as_list(item.get("nextActions")):
            fail(failures, f"{item_id} {status} must include nextActions")
        if strict_ready:
            fail(failures, f"{item_id} must be PASS for strict business smoke signoff")

    return item_id


def check_signoff(index: int, item: Any, failures: list[str], strict_ready: bool) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"signoffs[{index}] must be an object")
        return None

    role = text_value(item.get("role"))
    if not role:
        fail(failures, f"signoffs[{index}] missing role")
        return None

    for field in ("name", "decision", "signedAt", "evidence"):
        if field not in item:
            fail(failures, f"{role} signoff missing required field: {field}")
    decision = item.get("decision")
    if decision not in ALLOWED_SIGNOFF_DECISIONS:
        fail(failures, f"{role} signoff has invalid decision: {decision!r}")

    check_artifact_reference(role, "evidence", text_value(item.get("evidence")), failures)

    if strict_ready:
        if decision != "APPROVED":
            fail(failures, f"{role} signoff must be APPROVED for strict business smoke signoff")
        for field in ("name", "signedAt", "evidence"):
            if is_placeholder(item.get(field)):
                fail(failures, f"{role} signoff requires non-placeholder {field}")
        check_real_evidence_reference(role, "evidence", item.get("evidence"), failures)
        check_production_ready_reference(role, "evidence", item.get("evidence"), failures)

    return role


def check_open_issues(data: dict[str, Any], failures: list[str], strict_ready: bool) -> None:
    issues = data.get("openIssues")
    if not isinstance(issues, list):
        fail(failures, "openIssues must be a list")
        return
    for index, issue in enumerate(issues):
        if not isinstance(issue, dict):
            fail(failures, f"openIssues[{index}] must be an object")
            continue
        source = text_value(issue.get("source"))
        if source:
            check_artifact_reference(f"openIssues[{index}]", "source", source, failures)
        blocker_ids = issue.get("blockerCaseIds")
        if blocker_ids is not None and not (
            isinstance(blocker_ids, list) and all(isinstance(case_id, str) and case_id.strip() for case_id in blocker_ids)
        ):
            fail(failures, f"openIssues[{index}].blockerCaseIds must be a non-empty string list when present")
        backlog_ids = issue.get("backlogItemIds")
        if backlog_ids is not None and not (
            isinstance(backlog_ids, list) and all(isinstance(item_id, str) and item_id.strip() for item_id in backlog_ids)
        ):
            fail(failures, f"openIssues[{index}].backlogItemIds must be a non-empty string list when present")
        basic_config_area_ids = issue.get("basicConfigAreaIds")
        if basic_config_area_ids is not None and not (
            isinstance(basic_config_area_ids, list)
            and all(isinstance(area_id, str) and area_id.strip() for area_id in basic_config_area_ids)
        ):
            fail(failures, f"openIssues[{index}].basicConfigAreaIds must be a non-empty string list when present")
        export_target_ids = issue.get("exportTargetIds")
        if export_target_ids is not None and not (
            isinstance(export_target_ids, list)
            and all(isinstance(target_id, str) and target_id.strip() for target_id in export_target_ids)
        ):
            fail(failures, f"openIssues[{index}].exportTargetIds must be a non-empty string list when present")
        if strict_ready and not risk_acceptance_is_signed(issue, f"openIssues[{index}]", failures, strict_ready):
            fail(failures, f"openIssues[{index}] must be empty or include signed riskAcceptance for strict readiness")


def check_document(
    data: Any,
    failures: list[str],
    strict_ready: bool,
    blocker_ids: set[str],
    backlog_ids: set[str],
    incomplete_basic_config_area_ids: set[str],
    unresolved_export_target_ids: set[str],
) -> None:
    if not isinstance(data, dict):
        fail(failures, "business smoke signoff must be a JSON object")
        return

    for field in (
        "schemaVersion",
        "generatedAt",
        "environment",
        "status",
        "owner",
        "targetRelease",
        "repoCommit",
        "database",
        "frontendUrl",
        "backendBuild",
        "testWindow",
        "blockerLedger",
        "backlogLedger",
        "basicConfigCoverageLedger",
        "exportGapBreakdown",
        "smokeSuites",
        "persistenceScopes",
        "openIssues",
        "signoffs",
    ):
        if field not in data:
            fail(failures, f"missing top-level field: {field}")

    if data.get("environment") != "production":
        fail(failures, "environment must be production")
    if data.get("database") != "PostgreSQL":
        fail(failures, "database must remain PostgreSQL")
    if data.get("status") not in ALLOWED_TOP_STATUSES:
        fail(failures, f"invalid top-level status: {data.get('status')!r}")
    if strict_ready and data.get("status") != "READY":
        fail(failures, "top-level status must be READY for strict business smoke signoff readiness")
    check_artifact_reference("businessSmokeSignoff", "blockerLedger", text_value(data.get("blockerLedger")), failures)
    check_artifact_reference("businessSmokeSignoff", "backlogLedger", text_value(data.get("backlogLedger")), failures)
    check_artifact_reference("businessSmokeSignoff", "basicConfigCoverageLedger", text_value(data.get("basicConfigCoverageLedger")), failures)
    check_artifact_reference("businessSmokeSignoff", "exportGapBreakdown", text_value(data.get("exportGapBreakdown")), failures)

    suites = data.get("smokeSuites")
    if not isinstance(suites, list):
        fail(failures, "smokeSuites must be a list")
        return
    seen_suites: set[str] = set()
    for index, suite in enumerate(suites):
        suite_id = check_smoke_suite(index, suite, failures, strict_ready)
        if suite_id:
            if suite_id in seen_suites:
                fail(failures, f"duplicate smoke suite id: {suite_id}")
            seen_suites.add(suite_id)
    missing_suites = sorted(REQUIRED_SMOKE_SUITE_IDS - seen_suites)
    if missing_suites:
        fail(failures, "missing required smoke suites: " + ", ".join(missing_suites))
    extra_suites = sorted(seen_suites - REQUIRED_SMOKE_SUITE_IDS)
    if extra_suites:
        fail(failures, "unknown smoke suites: " + ", ".join(extra_suites))

    scopes = data.get("persistenceScopes")
    if not isinstance(scopes, list):
        fail(failures, "persistenceScopes must be a list")
        return
    seen_scopes: set[str] = set()
    for index, scope in enumerate(scopes):
        scope_id = check_persistence_scope(index, scope, failures, strict_ready)
        if scope_id:
            if scope_id in seen_scopes:
                fail(failures, f"duplicate persistence scope id: {scope_id}")
            seen_scopes.add(scope_id)
    missing_scopes = sorted(REQUIRED_PERSISTENCE_SCOPE_IDS - seen_scopes)
    if missing_scopes:
        fail(failures, "missing required persistence scopes: " + ", ".join(missing_scopes))
    extra_scopes = sorted(seen_scopes - REQUIRED_PERSISTENCE_SCOPE_IDS)
    if extra_scopes:
        fail(failures, "unknown persistence scopes: " + ", ".join(extra_scopes))

    signoffs = data.get("signoffs")
    if not isinstance(signoffs, list):
        fail(failures, "signoffs must be a list")
        return
    seen_roles: set[str] = set()
    for index, signoff in enumerate(signoffs):
        role = check_signoff(index, signoff, failures, strict_ready)
        if role:
            if role in seen_roles:
                fail(failures, f"duplicate signoff role: {role}")
            seen_roles.add(role)
    missing_roles = sorted(REQUIRED_SIGNOFF_ROLES - seen_roles)
    if missing_roles:
        fail(failures, "missing required signoff roles: " + ", ".join(missing_roles))
    extra_roles = sorted(seen_roles - REQUIRED_SIGNOFF_ROLES)
    if extra_roles:
        fail(failures, "unknown signoff roles: " + ", ".join(extra_roles))

    check_open_issues(data, failures, strict_ready)
    check_blocker_ledger_gate(data, blocker_ids, failures, strict_ready)
    check_backlog_ledger_gate(data, backlog_ids, failures, strict_ready)
    check_basic_config_coverage_gate(data, incomplete_basic_config_area_ids, failures, strict_ready)
    check_export_gap_gate(data, unresolved_export_target_ids, failures, strict_ready)
    if strict_ready:
        for field in ("generatedAt", "targetRelease", "repoCommit", "frontendUrl", "backendBuild", "testWindow"):
            if is_placeholder(data.get(field)):
                fail(failures, f"{field} must be non-placeholder for strict business smoke signoff readiness")
        check_production_ready_reference("businessSmokeSignoff", "frontendUrl", data.get("frontendUrl"), failures)
        check_no_placeholders(data, "businessSmokeSignoff", failures)

    check_secret_hygiene(data, "businessSmokeSignoff", failures)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate ADP production business smoke signoff evidence.")
    parser.add_argument("--signoff", type=Path, required=True)
    parser.add_argument("--blocker-ledger", type=Path, default=DEFAULT_BLOCKER_LEDGER)
    parser.add_argument("--backlog-ledger", type=Path, default=DEFAULT_BACKLOG_LEDGER)
    parser.add_argument("--basic-config-coverage", type=Path, default=DEFAULT_BASIC_CONFIG_COVERAGE)
    parser.add_argument("--export-gap-breakdown", type=Path, default=DEFAULT_EXPORT_GAP_BREAKDOWN)
    parser.add_argument("--strict-ready", action="store_true")
    args = parser.parse_args()

    failures: list[str] = []
    try:
        data = json.loads(args.signoff.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"FAIL: business smoke signoff file not found: {args.signoff}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as error:
        print(f"FAIL: invalid JSON in business smoke signoff file: {error}", file=sys.stderr)
        return 1

    blocker_ledger = load_json(args.blocker_ledger, "production module blocker ledger", failures)
    blocker_ids = blocker_case_ids(blocker_ledger, failures) if blocker_ledger is not None else set()
    backlog_ledger = load_json(args.backlog_ledger, "production module backlog ledger", failures)
    backlog_ids = backlog_item_ids(backlog_ledger, failures) if backlog_ledger is not None else set()
    basic_config_coverage = load_json(args.basic_config_coverage, "basic config coverage ledger", failures)
    incomplete_basic_config_area_ids = (
        collect_incomplete_basic_config_area_ids(basic_config_coverage, failures)
        if basic_config_coverage is not None
        else set()
    )
    export_gap_breakdown = load_json(args.export_gap_breakdown, "production export gap breakdown", failures)
    unresolved_exports = (
        unresolved_export_target_ids(export_gap_breakdown, failures)
        if export_gap_breakdown is not None
        else set()
    )
    check_document(
        data,
        failures,
        args.strict_ready,
        blocker_ids,
        backlog_ids,
        incomplete_basic_config_area_ids,
        unresolved_exports,
    )
    if failures:
        print(f"Business smoke signoff validation failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Business smoke signoff validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
