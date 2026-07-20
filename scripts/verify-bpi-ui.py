#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "frontend/apps/bpi"
ACCEPTANCE = ROOT / "metadata/bpi-ui-acceptance.json"
QUALITY_INVENTORY_ACCEPTANCE = ROOT / "metadata/bpi-quality-inventory-ui-acceptance.json"
REQUIRED = [
    "README.md",
    "package.json",
    "package-lock.json",
    "tsconfig.json",
    "vite.config.ts",
    "index.html",
    "src/api.ts",
    "src/main.ts",
    "src/styles.css",
    "src/types.ts",
    "tests/bpi-console.e2e.cjs",
]


def main() -> int:
    failures: list[str] = []
    for relative in REQUIRED:
        if not (UI / relative).is_file():
            failures.append(f"missing BPI UI file: frontend/apps/bpi/{relative}")
    if not ACCEPTANCE.is_file():
        failures.append("missing BPI UI acceptance evidence: metadata/bpi-ui-acceptance.json")
    if not QUALITY_INVENTORY_ACCEPTANCE.is_file():
        failures.append("missing BPI quality/inventory UI acceptance evidence")

    if not failures:
        package = json.loads((UI / "package.json").read_text(encoding="utf-8"))
        if package.get("devDependencies", {}).get("vite") != "6.4.3":
            failures.append("BPI UI must remain on the audited Vite 6.4.3 patch baseline")
        scripts = package.get("scripts", {})
        for name in ("build", "test:e2e"):
            if name not in scripts:
                failures.append(f"BPI UI package is missing {name!r} script")

        api = (UI / "src/api.ts").read_text(encoding="utf-8")
        for required in ("const API_ROOT = '/bpi-api'", "localStorage.getItem('ticket')", "Idempotency-Key", "If-Match", "rejectCandidate", "suspendBatch", "resumeBatch", "batchRelease", "featureFlags", "changeFeatureFlag", "simulateRule", "publishRule", "topologies", "createTopologyDraft", "validateTopology", "publishTopology", "createRuleDraft", "currentPointCatalog", "options?.cursor", "options?.search", "listPointCalibrations", "submitPointCalibration", "approvePointCalibration", "rejectPointCalibration", "revokePointCalibration", "shadowRuns", "createShadowRun", "reviewShadowRunBatch", "startShadowRun", "completeShadowRun", "approveShadowRun", "rejectShadowRun", "cancelShadowRun"):
            if required not in api:
                failures.append(f"BPI UI API client is missing {required!r}")
        forbidden = ("BPI_INTERNAL_JWT_SECRET", "http://bpi-service", "https://bpi-service")
        for path in (UI / "src").rglob("*"):
            if path.is_file() and path.suffix in {".ts", ".css", ".html"}:
                text = path.read_text(encoding="utf-8")
                for marker in forbidden:
                    if marker in text:
                        failures.append(f"{path.relative_to(ROOT)} exposes forbidden marker {marker!r}")

        e2e = (UI / "tests/bpi-console.e2e.cjs").read_text(encoding="utf-8")
        for required in ("console", "pageerror", "requestfailed", "/tmp/bpi-console-desktop.png", "/tmp/bpi-console-candidate-rejected.png", "/tmp/bpi-console-batch-lifecycle.png", "/tmp/bpi-console-end-boundary.png", "/tmp/bpi-console-feature-flags.png", "/tmp/bpi-console-point-catalog-pagination.png", "/tmp/bpi-console-point-calibration-governance.png", "/tmp/bpi-console-rule-published.png", "/tmp/bpi-console-rule-application-applied.png", "/tmp/bpi-console-rule-publication-blocked.png", "/tmp/bpi-console-shadow-run-approved.png", "/tmp/bpi-console-batch-quality-inventory.png", "/tmp/bpi-console-batch-quality-inventory-mobile.png", "getBatchRelease", "WMS_LOCATION_LOCKED", "ADP-E2E-RELEASE-TRACE-503", "CLOSED_RAW", "END_BOUNDARY_CONFIRMED", "bpi.wms-link", "overrideRevision", "sourceCalibrationStatus", "calibrationEvidenceId", "REVOKED", "point catalog incrementally loads a pinned snapshot", "property.0204", "wrongSearch.status, 422", "rule-runtime-readiness", "DEGRADED", "runtimeReadinessStatus", "UNRESOLVED_CRITICAL_DATA_QUALITY", "boundaryAgreement", "externalWrites", "document.documentElement.scrollWidth"):
            if required not in e2e:
                failures.append(f"BPI UI E2E is missing {required!r} evidence")

        acceptance = json.loads(ACCEPTANCE.read_text(encoding="utf-8"))
        if acceptance.get("scope") != "deterministic BPI simulator browser acceptance":
            failures.append("BPI UI acceptance must declare its deterministic simulator scope")
        summary = acceptance.get("summary", {})
        browser_tests = summary.get("browserTests", 0)
        if browser_tests < 17 or summary.get("pass") != browser_tests or summary.get("fail") != 0:
            failures.append("BPI UI acceptance must record at least seventeen browser tests with every test passing")
        item_ids = {item.get("id") for item in acceptance.get("items", [])}
        if "desktop-topology-rule-productization" not in item_ids:
            failures.append("BPI UI acceptance must cover topology and rule productization")
        if "desktop-point-catalog-readiness" not in item_ids:
            failures.append("BPI UI acceptance must cover point-catalog readiness")
        if "desktop-point-catalog-pagination" not in item_ids:
            failures.append("BPI UI acceptance must cover stable point-catalog pagination")
        if "desktop-point-calibration-governance" not in item_ids:
            failures.append("BPI UI acceptance must cover independent point-calibration governance")
        if "desktop-point-readiness-publication-blocker" not in item_ids:
            failures.append("BPI UI acceptance must cover point-readiness publication blocking")
        if "desktop-shadow-run-acceptance" not in item_ids:
            failures.append("BPI UI acceptance must cover the shadow-run approval workbench")
        if "desktop-feature-flag-governance" not in item_ids:
            failures.append("BPI UI acceptance must cover scoped feature-flag governance")
        if "desktop-batch-quality-inventory" not in item_ids:
            failures.append("BPI UI acceptance must cover batch quality and WMS business states")
        if "mobile-batch-release-partial-failure" not in item_ids:
            failures.append("BPI UI acceptance must cover local release failure and mobile recovery")
        rule_item = next(
            (item for item in acceptance.get("items", [])
             if item.get("id") == "desktop-rule-replay-and-publication"),
            {},
        )
        operations = " ".join(rule_item.get("operations", []))
        for required in ("APPLIED", "DEGRADED", "READY"):
            if required not in operations:
                failures.append(f"BPI UI rule acceptance is missing independent {required} evidence")
        if any(summary.get(key) != 0 for key in ("consoleErrors", "pageErrors", "requestFailures")):
            failures.append("BPI UI acceptance contains browser errors")
        limitations = " ".join(acceptance.get("limitations", []))
        for required in ("simulator", "PostgreSQL", "MES shell"):
            if required not in limitations:
                failures.append(f"BPI UI acceptance limitations are missing {required!r}")

        quality_acceptance = json.loads(QUALITY_INVENTORY_ACCEPTANCE.read_text(encoding="utf-8"))
        if quality_acceptance.get("status") != "PASS_DETERMINISTIC_BROWSER_NOT_TARGET_ACTIVATED":
            failures.append("BPI quality/inventory UI acceptance must preserve its target-not-activated boundary")
        quality_summary = quality_acceptance.get("summary", {})
        if quality_summary.get("stateCasesPass") != 6 or quality_summary.get("browserFail") != 0:
            failures.append("BPI quality/inventory UI acceptance must record all six states passing")
        if quality_summary.get("unexpectedConsoleErrors") != 0:
            failures.append("BPI quality/inventory UI acceptance contains unexpected browser errors")
        quality_limitations = " ".join(quality_acceptance.get("limitations", []))
        for required in ("simulator", "QCS", "WMS", "target"):
            if required not in quality_limitations:
                failures.append(f"BPI quality/inventory UI limitations are missing {required!r}")

    if failures:
        print("\n".join(f"BPI UI error: {item}" for item in failures), file=sys.stderr)
        return 1
    print("BPI UI source, auth boundary, command headers, and browser acceptance contract verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
