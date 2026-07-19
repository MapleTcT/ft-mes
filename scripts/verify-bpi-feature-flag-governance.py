#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "metadata/bpi-feature-flag-governance-acceptance.json"
DOC = ROOT / "docs/testing/bpi-feature-flag-governance-acceptance.md"
PERSISTENCE = ROOT / "metadata/persistence-acceptance.json"
EXPECTED_STATUS = "PASS_TARGET_GOVERNED_CLEANED"
EXPECTED_COMMIT = "0cf61838e31623c29fadbc1dbca6b44854716079"
EXPECTED_MARKER = "ADP_E2E_BPI_FLAGS_20260720_034527_0cf61838"
EXPECTED_FLAGS = {
    "bpi.ui",
    "bpi.commands",
    "bpi.rule-management",
    "bpi.shadow-only",
    "bpi.auto-confirm",
    "bpi.wms-link",
}


def load_json(path: Path, failures: list[str]) -> dict:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        failures.append(f"missing {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        failures.append(f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(value, dict):
        failures.append(f"{path.relative_to(ROOT)} must contain a JSON object")
        return {}
    return value


def require(condition: bool, message: str, failures: list[str]) -> None:
    if not condition:
        failures.append(message)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def main() -> int:
    failures: list[str] = []
    report = load_json(REPORT, failures)
    if not report:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1

    require(report.get("schemaVersion") == 1, "schemaVersion must be 1", failures)
    require(report.get("status") == EXPECTED_STATUS, f"status must be {EXPECTED_STATUS}", failures)
    require(report.get("repoCommit") == EXPECTED_COMMIT, "repoCommit must identify the deployed code", failures)
    require(report.get("marker") == EXPECTED_MARKER, "marker must remain the accepted target marker", failures)

    environment = report.get("environment", {})
    require(environment.get("host") == "10.11.100.17", "target host must be 10.11.100.17", failures)
    database = environment.get("database", {})
    require(database.get("engine") == "PostgreSQL", "database engine must remain PostgreSQL", failures)
    require(database.get("flywayVersion") == 21, "target acceptance must prove Flyway V21", failures)

    deployment = report.get("deployment", {})
    require(deployment.get("ociRevision") == EXPECTED_COMMIT, "OCI revision must equal the exact Git commit", failures)
    require(deployment.get("serviceHealth") == "UP", "service health must be UP", failures)
    require(deployment.get("adapterHealth") == "UP", "adapter health must be UP", failures)
    require(bool(deployment.get("backup", {}).get("sha256")), "pre-V21 backup hash is required", failures)

    feature_flags = report.get("featureFlags", {})
    listed_flags = set(feature_flags.get("editable", [])) | set(feature_flags.get("lockedOrReadOnly", []))
    require(feature_flags.get("count") == 6, "feature flag count must be 6", failures)
    require(listed_flags == EXPECTED_FLAGS, "feature flag inventory must contain the governed six flags", failures)
    require(set(feature_flags.get("enforced", [])) == {"bpi.commands", "bpi.rule-management"}, "both editable flags must be enforced", failures)
    require(feature_flags.get("pendingShellIntegration") == ["bpi.ui"], "bpi.ui must remain pending shell integration", failures)
    require(feature_flags.get("phaseLocks") == {"bpi.shadow-only": True, "bpi.auto-confirm": False, "bpi.wms-link": False}, "Phase 1 locks changed", failures)

    actions = report.get("browserActions", [])
    require(len(actions) == 2, "exactly two browser write actions are required", failures)
    if len(actions) == 2:
        require([action.get("action") for action in actions] == ["SET_FALSE", "INHERIT"], "browser actions must be SET_FALSE then INHERIT", failures)
        require(all(action.get("responseStatus") == 200 for action in actions), "both browser writes must return 200", failures)
        require([action.get("ifMatch") for action in actions] == [0, 1], "browser writes must use revisions 0 then 1", failures)
        require(len({action.get("idempotencyKey") for action in actions}) == 2, "browser writes need unique idempotency keys", failures)

    persistence = report.get("persistence", {})
    before = persistence.get("beforeCleanup", {})
    after = persistence.get("afterCleanup", {})
    require([before.get("featureFlagRows"), before.get("auditRows"), before.get("idempotencyRows")] == [1, 2, 2], "pre-cleanup persistence counts must be 1/2/2", failures)
    require([after.get("featureFlagRows"), after.get("auditRows"), after.get("idempotencyRows")] == [0, 0, 0], "marker residue must be 0/0/0", failures)
    require(after.get("globalBaselineRows") == 6, "six global seed flags must survive cleanup", failures)
    require(after.get("preExistingRuleManagementOverridePreserved") is True, "pre-existing rule-management override must survive cleanup", failures)

    browser = report.get("browser", {})
    auth = browser.get("authentication", {})
    require(auth.get("performedThroughLoginPage") is True, "acceptance must use the real login page", failures)
    require(auth.get("loginStatus") == 200 and auth.get("ticketCookiePresent") is True, "real login must return 200 with a ticket cookie", failures)
    require(auth.get("credentialValueRecorded") is False, "credential values must not be recorded", failures)
    require(auth.get("expectedNavigationAborts") == 2, "expected legacy navigation abort count must remain explicit", failures)
    for key in ("unexpectedConsoleErrors", "unexpectedPageErrors", "unexpectedHttpErrors", "unexpectedRequestFailures"):
        require(auth.get(key) == 0, f"authentication {key} must be zero", failures)
    for viewport_name in ("finalDesktop", "finalMobile"):
        viewport = browser.get(viewport_name, {})
        require(viewport.get("flagRows") == 6, f"{viewport_name} must render six flags", failures)
        require(viewport.get("navItems") == 8, f"{viewport_name} must render eight navigation items", failures)
        for key in ("consoleErrors", "pageErrors", "requestFailures", "httpErrors"):
            require(viewport.get(key) == 0, f"{viewport_name} {key} must be zero", failures)
    mobile = browser.get("finalMobile", {})
    require(mobile.get("documentScrollWidth") == mobile.get("documentClientWidth") == 390, "mobile document must not overflow", failures)
    require(mobile.get("bodyScrollWidth") == mobile.get("bodyClientWidth") == 390, "mobile body must not overflow", failures)

    boundary = report.get("productionWriteBoundary", {})
    require(all(boundary.get(key) == 0 for key in ("womWrites", "qcsWrites", "wmsWrites", "plcDcsWrites")), "Phase 1 must not write production systems", failures)
    summary = report.get("summary", {})
    require(summary.get("assertions") == summary.get("pass") == 20, "acceptance summary must remain 20/20", failures)
    require(summary.get("fail") == 0 and summary.get("markerResidueAfterCleanup") == 0, "acceptance must have no failures or marker residue", failures)

    for evidence in report.get("evidence", []):
        artifact = str(evidence.get("artifact", ""))
        if not artifact.startswith("metadata/"):
            continue
        path = ROOT / artifact
        require(path.is_file(), f"missing committed evidence {artifact}", failures)
        if path.is_file():
            require(sha256(path) == evidence.get("sha256"), f"SHA-256 mismatch for {artifact}", failures)

    try:
        doc_text = DOC.read_text(encoding="utf-8")
    except FileNotFoundError:
        failures.append(f"missing {DOC.relative_to(ROOT)}")
        doc_text = ""
    for phrase in (EXPECTED_STATUS, EXPECTED_MARKER, "Flyway V21", "0/0/0", "PENDING_SHELL_INTEGRATION"):
        require(phrase in doc_text, f"acceptance document missing {phrase}", failures)

    ledger = load_json(PERSISTENCE, failures)
    ledger_text = json.dumps(ledger, ensure_ascii=False)
    require(EXPECTED_MARKER in ledger_text, "persistence ledger must reference the accepted marker", failures)
    require("metadata/bpi-feature-flag-governance-acceptance.json" in ledger_text, "persistence ledger must reference the machine evidence", failures)

    serialized = json.dumps(report, ensure_ascii=False).lower()
    for secret_key in ('"password"', '"token"', '"cookievalue"'):
        require(secret_key not in serialized, f"acceptance report must not contain {secret_key}", failures)

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1
    print("BPI feature-flag governance acceptance: PASS (target browser/API/PostgreSQL 20/20, cleanup 0/0/0)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
