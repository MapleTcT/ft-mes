#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "metadata/bpi-shell-menu-gate-acceptance.json"
DOC = ROOT / "docs/testing/bpi-shell-menu-gate-acceptance.md"
PERSISTENCE = ROOT / "metadata/persistence-acceptance.json"
EXPECTED_STATUS = "PASS_TARGET_NATIVE_SHELL_GOVERNED"
EXPECTED_COMMIT = "df6fdb0e5ddb929626dd0ea3c81b170afbaa62a4"
EXPECTED_MARKER = "ADP_E2E_BPI_SHELL_20260720_050100_df6fdb0e"
EXPECTED_ITEM_IDS = {
    "artifact-provenance",
    "pre-deploy-backup",
    "initial-hidden",
    "direct-recovery-route",
    "ui-enable-write",
    "native-menu-visible",
    "native-iframe-navigation",
    "ui-disable-write",
    "legacy-menu-preservation",
    "ui-inherit-write",
    "postgres-pre-cleanup",
    "marker-cleanup",
    "nginx-fallback",
    "adapter-restore",
    "durable-test-config",
    "desktop-layout",
    "mobile-layout",
    "runtime-isolation",
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


def read(path: str, failures: list[str]) -> str:
    target = ROOT / path
    try:
        return target.read_text(encoding="utf-8")
    except FileNotFoundError:
        failures.append(f"missing {path}")
        return ""


def main() -> int:
    failures: list[str] = []
    report = load_json(REPORT, failures)
    if not report:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1

    require(report.get("schemaVersion") == 1, "schemaVersion must be 1", failures)
    require(report.get("status") == EXPECTED_STATUS, f"status must be {EXPECTED_STATUS}", failures)
    require(report.get("repoCommit") == EXPECTED_COMMIT, "repoCommit must identify deployed code", failures)
    require(report.get("marker") == EXPECTED_MARKER, "target marker changed", failures)

    environment = report.get("environment", {})
    require(environment.get("host") == "10.11.100.17", "target host must be 10.11.100.17", failures)
    require(environment.get("composeProject") == "adp-mes-newbase", "only the accepted ADP Compose project may be claimed", failures)
    database = environment.get("database", {})
    require(database == {"engine": "PostgreSQL", "version": "15.18", "flywayVersion": 21}, "database evidence must be PostgreSQL 15.18/Flyway V21", failures)
    streaming = environment.get("streaming", {})
    require(streaming.get("jobId") == "1e981b842f4693e49f3c3def0fb98cb6", "Flink job ID changed", failures)
    require(streaming.get("state") == "RUNNING", "Flink job must be RUNNING", failures)
    require(streaming.get("runningTasks") == streaming.get("totalTasks") == 36, "Flink job must remain 36/36", failures)

    deployment = report.get("deployment", {})
    backup = deployment.get("backup", {})
    require(backup.get("databaseDumpBytes") == 139076, "pre-deploy dump size changed", failures)
    require(backup.get("sha256") == "fe4df8543cee09a83df2546869e23686bae4b72228e083b299e29ba949ba1c08", "pre-deploy dump hash changed", failures)
    require(backup.get("restoreListValidated") is True, "pre-deploy dump restore list must be validated", failures)
    for component in ("service", "adapter"):
        details = deployment.get(component, {})
        require(details.get("ociRevision") == EXPECTED_COMMIT, f"{component} OCI revision must equal the source commit", failures)
        require(str(details.get("imageId", "")).startswith("sha256:"), f"{component} image ID is required", failures)
        require(details.get("health") == "healthy", f"{component} must be healthy", failures)
    require(deployment.get("databaseMigrationApplied") is False, "this acceptance must not claim a database migration", failures)
    require(deployment.get("streamingRestarted") is False, "this acceptance must not claim a streaming restart", failures)

    actions = report.get("browserActions", [])
    require([item.get("action") for item in actions] == ["ENABLE", "DISABLE", "INHERIT"], "browser actions must be ENABLE, DISABLE, INHERIT", failures)
    require([item.get("ifMatch") for item in actions] == [0, 1, 2], "browser actions must use revisions 0, 1, 2", failures)
    require(all(item.get("responseStatus") == 200 for item in actions), "all three browser writes must return 200", failures)
    require(all(item.get("idempotencyKeyPresent") is True for item in actions), "all writes need an idempotency key", failures)
    require(all(item.get("idempotencyKeyValueRecorded") is False for item in actions), "idempotency key values must not be committed", failures)
    require(all(EXPECTED_MARKER in item.get("payload", {}).get("reason", "") for item in actions), "all marker writes must carry the exact marker", failures)

    browser = report.get("browser", {})
    authentication = browser.get("authentication", {})
    require(authentication.get("performedThroughLoginPage") is True, "acceptance must use the real login page", failures)
    require(authentication.get("loginStatus") == 200, "real login must return 200", failures)
    require(authentication.get("credentialValueRecorded") is False, "credential values must not be recorded", failures)
    require(authentication.get("unauthenticatedMenuStatus") == 401, "unauthenticated native menu access must fail with 401", failures)
    require("userPortal" in authentication.get("existingIssue", "") and "401" in authentication.get("existingIssue", ""), "the existing portal 401 must remain explicit", failures)

    initial = browser.get("initialHidden", {})
    require([initial.get("menuCount"), initial.get("bpiMenuCount"), initial.get("gateHeader")] == [28, 0, "HIDDEN_DISABLED"], "initial hidden state must remain 28/0/HIDDEN_DISABLED", failures)
    recovery = browser.get("directRecovery", {})
    require(recovery.get("route") == "/bpi/#/featureFlags" and recovery.get("editable") is True, "direct recovery route must stay available and editable", failures)
    require(recovery.get("bpiUiEnforcement") == "ENFORCED", "bpi.ui must be ENFORCED", failures)
    enabled = browser.get("enabled", {})
    final = browser.get("final", {})
    for label, state in (("enabled", enabled), ("final", final)):
        require([state.get("menuCount"), state.get("bpiMenuCount"), state.get("gateHeader")] == [29, 1, "VISIBLE_INJECTED"], f"{label} native menu must be 29/1/VISIBLE_INJECTED", failures)
        require(state.get("iframeUrl") == "/bpi/#/overview?&workFlowMenuCode=BPI_1.0.0_console&openType=page", f"{label} iframe URL changed", failures)
        require(state.get("iframeHeading") == "实时生产态势", f"{label} iframe heading changed", failures)
    for label in ("disabled", "inherited"):
        state = browser.get(label, {})
        require([state.get("menuCount"), state.get("bpiMenuCount"), state.get("gateHeader")] == [28, 0, "HIDDEN_DISABLED"], f"{label} state must be 28/0/HIDDEN_DISABLED", failures)
    errors = browser.get("bpiActionErrors", {})
    require(all(errors.get(key) == 0 for key in ("consoleErrors", "pageErrors", "requestFailures", "httpErrors")), "BPI action phases must have zero browser errors", failures)
    desktop = browser.get("desktop", {})
    require(desktop.get("documentScrollWidth") == desktop.get("documentClientWidth") == 1440, "desktop must not overflow horizontally", failures)
    require(desktop.get("documentScrollHeight") == desktop.get("documentClientHeight") == 900, "desktop must not overflow vertically", failures)
    mobile = browser.get("mobile", {})
    require(mobile.get("documentScrollWidth") == mobile.get("documentClientWidth") == 390, "mobile document must not overflow", failures)
    require(mobile.get("bodyScrollWidth") == mobile.get("bodyClientWidth") == 390, "mobile body must not overflow", failures)

    fallback = report.get("fallback", {})
    require(fallback.get("menuStatus") == 200, "gateway fallback must return 200", failures)
    require([fallback.get("menuCount"), fallback.get("bpiMenuCount")] == [28, 0], "gateway fallback must return only 28 base menus", failures)
    require(fallback.get("gateHeaderPresent") is False, "gateway fallback must not forge a BPI gate header", failures)
    require(fallback.get("legacyBaseMenusUnchanged") is True, "gateway fallback must preserve base menus", failures)
    require(fallback.get("adapterRestoredHealthy") is True, "adapter must be restored after fallback rehearsal", failures)

    persistence = report.get("persistence", {})
    require(set(persistence.get("tables", [])) == {"bpi.bpi_feature_flags", "bpi.bpi_audit_events", "bpi.bpi_api_idempotency"}, "persistence table inventory changed", failures)
    pre = persistence.get("preCleanup", {})
    require([pre.get("featureFlagRows"), pre.get("auditRows"), pre.get("idempotencyRows")] == [1, 3, 3], "pre-cleanup counts must be 1/3/3", failures)
    cleanup = persistence.get("cleanup", {})
    require([cleanup.get("remainingFeatureFlagRows"), cleanup.get("remainingAuditRows"), cleanup.get("remainingIdempotencyRows")] == [0, 0, 0], "marker residue must be 0/0/0", failures)
    final_config = persistence.get("finalConfiguration", {})
    require(final_config.get("scopeType") == "LINE" and final_config.get("scopeKey") == "LINE-S07-01", "final config must be the accepted LINE scope", failures)
    require(final_config.get("flagKey") == "bpi.ui" and final_config.get("enabled") is True and final_config.get("active") is True, "final bpi.ui config must be active and enabled", failures)
    require(final_config.get("revision") == 1, "final bpi.ui config must be revision 1", failures)
    require([final_config.get("auditRows"), final_config.get("idempotencyRows"), final_config.get("globalBaselineRows")] == [1, 1, 6], "final governance rows must be 1/1 with six global baselines", failures)
    require(final_config.get("preExistingRuleManagementOverridePreserved") is True, "pre-existing rule-management override must survive", failures)

    boundary = report.get("productionWriteBoundary", {})
    require(all(boundary.get(key) == 0 for key in ("womWrites", "qcsWrites", "wmsWrites", "plcWrites", "dcsWrites")), "menu acceptance must not write production systems", failures)
    summary = report.get("summary", {})
    require(summary == {"checks": 18, "pass": 18, "fail": 0, "blocked": 0}, "acceptance summary must be 18/18 with no failure or block", failures)
    items = report.get("items", [])
    require({item.get("id") for item in items} == EXPECTED_ITEM_IDS, "acceptance item inventory changed", failures)
    require(all(item.get("status") == "PASS" for item in items), "all acceptance items must pass", failures)

    for screenshot in report.get("screenshots", []):
        artifact = str(screenshot.get("artifact", ""))
        path = ROOT / artifact
        require(path.is_file(), f"missing screenshot {artifact}", failures)
        if path.is_file():
            require(sha256(path) == screenshot.get("sha256"), f"SHA-256 mismatch for {artifact}", failures)

    controller = read("backend/source-modules/batch-intelligence-adapter/src/main/java/com/mapletct/ftmes/bpiadapter/BpiShellMenuController.java", failures)
    nginx = read("deploy/docker/nginx/adp.conf", failures)
    compose = read("deploy/docker/docker-compose.yml", failures)
    ui = read("frontend/apps/bpi/src/main.ts", failures)
    for phrase in ("/bpi-shell/menus/currentUser", "X-BPI-UI-Gate", "VISIBLE_INJECTED", "HIDDEN_DISABLED"):
        require(phrase in controller, f"menu controller missing {phrase}", failures)
    require("location = /inter-api/rbac/v1/menus/currentUser" in nginx, "Nginx exact native-menu route is missing", failures)
    require("error_page 404 500 502 503 504 = @adp_gateway_current_user_menu" in nginx, "Nginx gateway fallback is missing", failures)
    require("BPI_ADAPTER_SHELL_MENU_ENABLED" in compose, "Compose shell-menu switch is missing", failures)
    require("旧 MES 菜单由 Java 8 adapter" in ui, "feature-flag page must describe the active Java 8 enforcement point", failures)

    doc = read("docs/testing/bpi-shell-menu-gate-acceptance.md", failures)
    for phrase in (EXPECTED_STATUS, EXPECTED_MARKER, "1/3/3", "0/0/0", "userPortal", "LINE 层 INHERIT"):
        require(phrase in doc, f"acceptance document missing {phrase}", failures)
    ledger = load_json(PERSISTENCE, failures)
    ledger_text = json.dumps(ledger, ensure_ascii=False)
    require(EXPECTED_MARKER in ledger_text, "persistence ledger must reference the shell marker", failures)
    require("metadata/bpi-shell-menu-gate-acceptance.json" in ledger_text, "persistence ledger must reference shell machine evidence", failures)

    serialized = json.dumps(report, ensure_ascii=False).lower()
    for secret in ("ft123456789", "123456", '"password"', '"token"', '"cookievalue"', '"authorization"'):
        require(secret not in serialized, f"acceptance report must not contain secret marker {secret}", failures)

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1
    print("BPI native shell menu gate acceptance: PASS (target browser/API/PostgreSQL/fallback 18/18)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
