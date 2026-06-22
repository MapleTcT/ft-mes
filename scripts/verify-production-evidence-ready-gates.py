#!/usr/bin/env python3
from __future__ import annotations

import json
import importlib.util
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
EXPECTED_REJECTION_MARKERS = ("template/example/sample", "template/example")
EXPECTED_TEST_ENVIRONMENT_REJECTION = "test-environment evidence"
REAL_PRODUCTION_EVIDENCE_URL = "https://prod-evidence.invalid/adp/negative-test/evidence.json"
REAL_PRODUCTION_REPO_ARTIFACT = "docs/production-migration/README.md"


def load_json(relative_path: str) -> dict[str, Any]:
    path = ROOT / relative_path
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"{relative_path} must contain a JSON object")
    return data


def write_temp_json(temp_dir: Path, name: str, data: dict[str, Any]) -> Path:
    path = temp_dir / name
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def load_validator_module(relative_path: str) -> Any:
    path = ROOT / relative_path
    module_name = relative_path.replace("/", "_").replace("-", "_").replace(".", "_")
    spec = importlib.util.spec_from_file_location(module_name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load validator module: {relative_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def run_negative_case(label: str, validator: str, option: str, payload_path: Path, failures: list[str]) -> None:
    command = [
        sys.executable,
        str(ROOT / validator),
        option,
        str(payload_path),
        "--strict-ready",
    ]
    result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
    output = result.stdout + result.stderr
    if result.returncode == 0:
        failures.append(f"{label}: strict-ready accepted template evidence unexpectedly")
        return
    if not any(marker in output for marker in EXPECTED_REJECTION_MARKERS):
        failures.append(f"{label}: strict-ready failed, but not because template evidence was rejected")
        return
    print(f"PASS: {label} rejects template evidence in strict-ready mode")


def run_negative_case_expecting(
    label: str,
    validator: str,
    option: str,
    payload_path: Path,
    expected_marker: str,
    failures: list[str],
) -> None:
    command = [
        sys.executable,
        str(ROOT / validator),
        option,
        str(payload_path),
        "--strict-ready",
    ]
    result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
    output = result.stdout + result.stderr
    if result.returncode == 0:
        failures.append(f"{label}: strict-ready accepted incomplete evidence unexpectedly")
        return
    if expected_marker not in output:
        failures.append(f"{label}: strict-ready failed, but not because {expected_marker!r} was enforced")
        return
    print(f"PASS: {label} rejects incomplete evidence in strict-ready mode")


def run_business_smoke_negative_case_expecting(
    label: str,
    payload_path: Path,
    blocker_path: Path,
    backlog_path: Path,
    basic_config_path: Path,
    expected_marker: str,
    failures: list[str],
    export_gap_path: Path | None = None,
) -> None:
    command = [
        sys.executable,
        str(ROOT / "deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py"),
        "--signoff",
        str(payload_path),
        "--blocker-ledger",
        str(blocker_path),
        "--backlog-ledger",
        str(backlog_path),
        "--basic-config-coverage",
        str(basic_config_path),
    ]
    if export_gap_path is not None:
        command.extend(["--export-gap-breakdown", str(export_gap_path)])
    command.append("--strict-ready")
    result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
    output = result.stdout + result.stderr
    if result.returncode == 0:
        failures.append(f"{label}: strict-ready accepted incomplete business smoke signoff unexpectedly")
        return
    if expected_marker not in output:
        failures.append(f"{label}: strict-ready failed, but not because {expected_marker!r} was enforced")
        return
    print(f"PASS: {label} rejects incomplete business smoke signoff in strict-ready mode")


def run_inprocess_negative_case(label: str, validator: str, payload: dict[str, Any], failures: list[str]) -> None:
    module = load_validator_module(validator)
    validator_failures: list[str] = []
    if hasattr(module, "fail"):
        module.fail = lambda local_failures, message: local_failures.append(message)
    module.check_report(payload, validator_failures)
    output = "\n".join(validator_failures)
    if not validator_failures:
        failures.append(f"{label}: validator accepted template/example READY evidence unexpectedly")
        return
    if not any(marker in output for marker in EXPECTED_REJECTION_MARKERS):
        failures.append(f"{label}: validator failed, but not because template/example evidence was rejected")
        return
    print(f"PASS: {label} rejects template evidence before READY")


def run_inprocess_negative_case_expecting(
    label: str,
    validator: str,
    payload: dict[str, Any],
    expected_marker: str,
    failures: list[str],
) -> None:
    module = load_validator_module(validator)
    validator_failures: list[str] = []
    if hasattr(module, "fail"):
        module.fail = lambda local_failures, message: local_failures.append(message)
    module.check_report(payload, validator_failures)
    output = "\n".join(validator_failures)
    if not validator_failures:
        failures.append(f"{label}: validator accepted invalid READY evidence unexpectedly")
        return
    if expected_marker not in output:
        failures.append(f"{label}: validator failed, but not because {expected_marker!r} was enforced")
        return
    print(f"PASS: {label} rejects {expected_marker} before READY")


def fake_ready_rollback() -> dict[str, Any]:
    evidence = "deploy/rollback/production-migration/rollback-evidence.example.json"
    data = load_json(evidence)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        environment="production",
        cutoverWindow="negative-test-window",
        rollbackDecisionOwner="negative-test-owner",
        status="READY",
    )
    for component in data["components"]:
        component.update(
            status="READY",
            owner="negative-test-owner",
            backupEvidence=evidence,
            rehearsalEvidence=evidence,
            blockingIssues=[],
            nextActions=[],
        )
    return data


def fake_ready_database_migration() -> dict[str, Any]:
    evidence = "deploy/database/production-migration/migration-evidence.example.json"
    data = load_json(evidence)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        status="READY",
        environment="production-rehearsal-negative-test",
        owner="negative-test-owner",
        repoCommit="abcdef0123456789",
        migrationWindow="2026-06-20T00:00:00+08:00/2026-06-20T01:00:00+08:00",
    )
    data["sourceSnapshot"].update(
        type="postgresql",
        capturedAt="2026-06-20T00:00:00+08:00",
        evidence=evidence,
    )
    data["targetSnapshot"].update(
        capturedAt="2026-06-20T00:00:00+08:00",
        evidence=evidence,
    )
    for phase in data["phases"]:
        phase.update(
            status="READY",
            evidenceRefs=[evidence],
            blockingIssues=[],
            nextActions=[],
        )
        if phase["id"] in {"source-inventory", "target-preflight"}:
            phase["summary"] = {"totalTables": 1, "ok": 1, "errors": 0}
        elif phase["id"] == "row-count-comparison":
            phase["summary"] = {"totalTables": 1, "match": 1, "mismatch": 0, "errors": 0}
        elif phase["id"] == "checksum-comparison":
            phase["summary"] = {"totalTables": 1, "match": 1, "mismatch": 0, "missing": 0, "errors": 0}
        elif phase["id"] == "postgres-runtime-smoke":
            phase["summary"] = {"status": "PASS", "checks": 1, "passed": 1}
        elif phase["id"] == "rollback-link":
            phase["summary"] = {"status": "READY"}
    return data


def fake_ready_license() -> dict[str, Any]:
    evidence = "deploy/license/production-migration/license-decision.example.json"
    data = load_json(evidence)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        status="READY",
        productionMode="FORMAL_LICENSE",
        decisionOwner="negative-test-owner",
    )
    for decision in data["decisions"]:
        decision.update(
            status="READY",
            owner="negative-test-owner",
            decision="approved for negative test",
            evidence=evidence,
            blockingIssues=[],
            nextActions=[],
        )
        if decision["id"] == "emergency-bypass":
            decision.update(
                allowed=False,
                approver="negative-test-owner",
                maxDuration="0h",
                auditEvidence=evidence,
            )
    return data


def replace_tbd(value: Any) -> None:
    if isinstance(value, dict):
        for key, nested in value.items():
            if isinstance(nested, str) and nested == "TBD":
                value[key] = "filled"
            else:
                replace_tbd(nested)
    elif isinstance(value, list):
        for nested in value:
            replace_tbd(nested)


def fake_ready_network() -> dict[str, Any]:
    evidence = "deploy/network/production-migration/network-tls-plan.example.json"
    data = load_json(evidence)
    replace_tbd(data)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        status="READY",
        publicBaseUrl="https://adp.example.invalid",
    )
    for domain in data["domains"]:
        domain.update(status="READY", evidence=evidence, blockingIssues=[], nextActions=[])
    for certificate in data["tlsCertificates"]:
        certificate.update(
            status="READY",
            domains=["adp.example.invalid"],
            issuer="negative-test-ca",
            storage="negative-test-secret-store",
            renewalMethod="managed",
            expiryMonitoring="negative-test-monitor",
            autoRenewalEvidence=evidence,
            blockingIssues=[],
            nextActions=[],
        )
    for collection in ("reverseProxies", "exposedServices", "firewallRules"):
        for item in data[collection]:
            item.update(status="READY", evidence=evidence, blockingIssues=[], nextActions=[])
    for check in data["validationChecks"]:
        check.update(status="READY", commandOrEvidence=evidence, blockingIssues=[], nextActions=[])
    return data


def fake_ready_security() -> dict[str, Any]:
    evidence = "deploy/security/production-migration/security-hardening-plan.example.json"
    data = load_json(evidence)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        status="READY",
        productionSecretManager="negative-test-vault",
        breakGlassOwner="negative-test-owner",
        reviewDate="2026-06-20",
    )
    for control in data["controls"]:
        control.update(status="READY", evidence=evidence, blockingIssues=[], nextActions=[])
    return data


def fake_ready_business_smoke() -> dict[str, Any]:
    evidence = "deploy/business-smoke/production-migration/business-smoke-signoff.example.json"
    data = load_json(evidence)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        status="READY",
        targetRelease="negative-test-release",
        repoCommit="abcdef0123456789",
        frontendUrl="https://adp.example.invalid",
        backendBuild="negative-test-build",
        testWindow="2026-06-20T00:00:00+08:00/2026-06-20T01:00:00+08:00",
    )
    for suite in data["smokeSuites"]:
        suite.update(status="PASS", evidence=evidence, blockingIssues=[], nextActions=[])
    for scope in data["persistenceScopes"]:
        scope.update(status="PASS", evidence=evidence, blockingIssues=[], nextActions=[])
    for issue in data["openIssues"]:
        issue["riskAcceptance"] = {
            "decision": "ACCEPTED",
            "acceptedBy": "negative-test-owner",
            "signedAt": "2026-06-20T00:00:00+08:00",
            "evidence": "https://evidence.invalid/adp/prod-business-risk/negative-test.json",
        }
    for signoff in data["signoffs"]:
        signoff.update(
            decision="APPROVED",
            name="negative-test-owner",
            signedAt="2026-06-20T00:00:00+08:00",
            evidence=evidence,
        )
    return data


def fake_ready_business_smoke_with_real_evidence_refs() -> dict[str, Any]:
    data = fake_ready_business_smoke()
    real_evidence = "https://evidence.invalid/adp/prod-business-smoke/negative-test.json"
    for suite in data["smokeSuites"]:
        suite["evidence"] = real_evidence
    for scope in data["persistenceScopes"]:
        scope["evidence"] = real_evidence
    for signoff in data["signoffs"]:
        signoff["evidence"] = real_evidence
    return data


def fake_ready_business_smoke_with_test_environment_evidence() -> dict[str, Any]:
    data = fake_ready_business_smoke_with_real_evidence_refs()
    data["frontendUrl"] = "http://100.99.133.43:18080"
    data["smokeSuites"][0]["evidence"] = "http://222.88.185.146:18080/prod-business-smoke.json"
    data["persistenceScopes"][0]["evidence"] = "http://10.11.100.17/prod-persistence-evidence.json"
    data["signoffs"][0]["evidence"] = "http://100.99.133.43:18080/business-signoff.json"
    data["openIssues"][0]["riskAcceptance"]["evidence"] = "http://222.88.185.146:18080/risk-acceptance.json"
    return data


def fake_ready_business_smoke_with_plain_risk_acceptance() -> dict[str, Any]:
    data = fake_ready_business_smoke_with_real_evidence_refs()
    for issue in data["openIssues"]:
        issue["riskAcceptance"] = "signed negative-test risk acceptance"
    return data


def fake_blocker_ledger_for_business_smoke(payload: dict[str, Any]) -> dict[str, Any]:
    blocker_ids = sorted(
        {
            case_id
            for issue in payload["openIssues"]
            for case_id in issue.get("blockerCaseIds", [])
            if isinstance(case_id, str) and case_id.strip()
        }
    )
    return {
        "database": "PostgreSQL",
        "summary": {"blockers": len(blocker_ids), "blockedCases": len(blocker_ids)},
        "blockers": [{"caseId": case_id, "status": "BLOCKED"} for case_id in blocker_ids],
    }


def fake_backlog_ledger_for_business_smoke(payload: dict[str, Any]) -> dict[str, Any]:
    backlog_ids = sorted(
        {
            item_id
            for issue in payload["openIssues"]
            for item_id in issue.get("backlogItemIds", [])
            if isinstance(item_id, str) and item_id.strip()
        }
    )
    return {
        "database": "PostgreSQL",
        "summary": {"totalItems": len(backlog_ids)},
        "items": [{"id": item_id, "status": "BLOCKED"} for item_id in backlog_ids],
    }


def fake_backlog_ledger_for_business_smoke_with_extra_items(
    payload: dict[str, Any],
    extra_item_ids: set[str],
) -> dict[str, Any]:
    ledger = fake_backlog_ledger_for_business_smoke(payload)
    item_ids = {item["id"] for item in ledger["items"]}
    for item_id in sorted(extra_item_ids - item_ids):
        ledger["items"].append({"id": item_id, "status": "BLOCKED"})
    ledger["items"].sort(key=lambda item: item["id"])
    ledger["summary"]["totalItems"] = len(ledger["items"])
    return ledger


def fake_export_gap_for_business_smoke(target_ids: set[str]) -> dict[str, Any]:
    return {
        "database": "PostgreSQL",
        "caseId": "PROD-023",
        "summary": {
            "status": "BLOCKED",
            "targets": len(target_ids),
            "verifiedDataExports": 0,
        },
        "items": [
            {
                "id": target_id,
                "status": "BLOCKED",
                "verifiedDataExport": False,
            }
            for target_id in sorted(target_ids)
        ],
    }


def fake_complete_basic_config_coverage() -> dict[str, Any]:
    return {
        "database": "PostgreSQL",
        "goalId": "G-012",
        "overallStatus": "PASS",
        "areas": [],
    }


def fake_ready_nacos_runtime_patch() -> dict[str, Any]:
    evidence = "deploy/nacos/production-migration/nacos-runtime-patch-evidence.example.json"
    data = load_json(evidence)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        status="READY",
        owner="negative-test-owner",
        repoCommit="abcdef0123456789",
        publishWindow="negative-test-window",
    )
    for phase in data["phases"]:
        phase.update(
            status="READY",
            owner="negative-test-owner",
            evidenceRefs=[evidence],
            blockingIssues=[],
            nextActions=[],
        )
        phase_id = phase["id"]
        if phase_id == "rendered-config-baseline":
            phase["summary"] = {"renderedDataIds": 1, "errors": 0}
        elif phase_id == "production-nacos-export":
            phase["summary"] = {"exportedDataIds": 1, "exportedServices": 1, "errors": 0}
        elif phase_id == "nacos-diff-review":
            phase["summary"] = {
                "reviewedDataIds": 1,
                "criticalDrift": 0,
                "oracleDefaults": 0,
                "secretLeaks": 0,
                "approvedBy": "negative-test-owner",
            }
        elif phase_id == "runtime-patch-package":
            phase["summary"] = {
                "packageRef": "negative-test-package.tar.gz",
                "manifestEntryCount": 1,
                "sha256": "0" * 64,
                "signatureEvidence": evidence,
            }
        elif phase_id == "publish-window":
            phase["summary"] = {
                "windowApproved": True,
                "restartSequenceApproved": True,
                "rollbackWindowApproved": True,
            }
        elif phase_id in {"post-publish-nacos-smoke", "post-publish-platform-smoke"}:
            phase["summary"] = {
                "status": "PASS",
                "checks": 1,
                "passed": 1,
                "oracleResiduals": 0,
                "criticalServicesHealthy": 1,
            }
        elif phase_id == "rollback-link":
            phase["summary"] = {"status": "READY", "rollbackEvidence": evidence}
    data["openIssues"] = []
    return data


def fake_ready_minio_migration() -> dict[str, Any]:
    evidence = "deploy/minio/production-migration/minio-migration-evidence.example.json"
    data = load_json(evidence)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        status="READY",
        owner="negative-test-owner",
        repoCommit="abcdef0123456789",
        migrationWindow="2026-06-20T00:00:00+08:00/2026-06-20T01:00:00+08:00",
        sourceEndpointRef="negative-test-source-endpoint",
        targetEndpointRef="negative-test-target-endpoint",
    )
    for phase in data["phases"]:
        phase.update(
            status="READY",
            owner="negative-test-owner",
            evidenceRefs=[evidence],
            blockingIssues=[],
            nextActions=[],
        )
        phase_id = phase["id"]
        if phase_id in {"source-bucket-inventory", "target-bucket-inventory"}:
            phase["summary"] = {"buckets": 1, "objects": 1, "bytes": 1, "errors": 0}
        elif phase_id == "object-inventory-comparison":
            phase["summary"] = {"totalObjects": 1, "match": 1, "missing": 0, "mismatch": 0, "errors": 0}
        elif phase_id == "migration-dry-run":
            phase["summary"] = {"status": "PASS", "commands": 1, "failedCommands": 0, "resumable": True}
        elif phase_id == "sample-download-verification":
            phase["summary"] = {"status": "PASS", "samples": 1, "passed": 1, "failed": 0}
        elif phase_id == "post-migration-runtime-smoke":
            phase["summary"] = {"status": "PASS", "checks": 1, "passed": 1, "buckets": 1, "objects": 1}
        elif phase_id == "rollback-link":
            phase["summary"] = {"status": "READY", "rollbackEvidence": evidence, "sourceReadFallback": True}
    data["openIssues"] = []
    return data


def fake_ready_keycloak_migration() -> dict[str, Any]:
    evidence = "deploy/keycloak/production-migration/keycloak-migration-evidence.example.json"
    data = load_json(evidence)
    data.update(
        generatedAt="2026-06-20T00:00:00+08:00",
        status="READY",
        owner="negative-test-owner",
        repoCommit="abcdef0123456789",
        migrationWindow="2026-06-20T00:00:00+08:00/2026-06-20T01:00:00+08:00",
        sourceRealmRef="negative-test-source-realm",
        targetRealmRef="negative-test-target-realm",
    )
    for phase in data["phases"]:
        phase.update(
            status="READY",
            owner="negative-test-owner",
            evidenceRefs=[evidence],
            blockingIssues=[],
            nextActions=[],
        )
        phase_id = phase["id"]
        if phase_id in {"source-realm-inventory", "target-realm-inventory"}:
            phase["summary"] = {
                "realm": "dt",
                "realmEnabled": True,
                "clients": 2,
                "roles": 1,
                "clientScopes": 1,
                "components": 0,
                "users": 1,
                "errors": 0,
            }
        elif phase_id == "realm-inventory-comparison":
            phase["summary"] = {
                "totalChecks": 1,
                "match": 1,
                "differences": 0,
                "unresolvedDifferences": 0,
                "jwtKeyRotationApproved": False,
            }
        elif phase_id == "database-backup-restore-rehearsal":
            phase["summary"] = {
                "status": "PASS",
                "backupEvidence": evidence,
                "restoreChecks": 1,
                "passed": 1,
                "failed": 0,
            }
        elif phase_id == "secret-rotation":
            phase["summary"] = {
                "status": "PASS",
                "adminAccountsRotated": 1,
                "clientsRotated": 1,
                "leakedSecrets": 0,
                "secretManagerRef": evidence,
            }
        elif phase_id == "jwt-nacos-sync":
            phase["summary"] = {
                "status": "PASS",
                "keycloakPublicKeySha256Prefix": "abc123",
                "nacosJwtSha256Prefix": "abc123",
                "nacosDataIdsUpdated": 1,
                "gatewayRestarted": True,
            }
        elif phase_id == "post-migration-auth-smoke":
            phase["summary"] = {
                "status": "PASS",
                "checks": 5,
                "passed": 5,
                "login": True,
                "currentUser": True,
                "menu": True,
                "rbacAuthority": True,
                "organizationSmoke": True,
            }
        elif phase_id == "rollback-link":
            phase["summary"] = {
                "status": "READY",
                "rollbackEvidence": evidence,
                "databaseRestoreEvidence": evidence,
            }
    data["openIssues"] = []
    return data


def fake_ready_migration_readiness() -> dict[str, Any]:
    data = load_json("metadata/production-migration-readiness.json")
    tracks = data["tracks"]
    data.update(status="READY_FOR_PRODUCTION_MIGRATION")
    data["summary"] = {
        "totalTracks": len(tracks),
        "ready": len(tracks),
        "planned": 0,
        "blocked": 0,
        "notStarted": 0,
    }
    for track in tracks:
        track.update(status="READY", blockingIssues=[])
        evidence = track.setdefault("currentEvidence", [])
        if not any(isinstance(value, str) and "example" in value.lower() for value in evidence):
            evidence.append("deploy/database/production-migration/table-list.example.txt")
    return data


def fake_ready_migration_readiness_with_real_evidence_refs() -> dict[str, Any]:
    data = load_json("metadata/production-migration-readiness.json")
    tracks = data["tracks"]
    data.update(status="READY_FOR_PRODUCTION_MIGRATION")
    data["summary"] = {
        "totalTracks": len(tracks),
        "ready": len(tracks),
        "planned": 0,
        "blocked": 0,
        "notStarted": 0,
    }
    for track in tracks:
        track_id = str(track.get("id", "track"))
        current_evidence = [f"{REAL_PRODUCTION_EVIDENCE_URL}#{track_id}"]
        if track_id == "business-smoke-signoff":
            current_evidence.extend(
                [
                    "metadata/production-module-blockers.json",
                    "metadata/production-module-backlog.json",
                ]
            )
        track.update(
            status="READY",
            currentEvidence=current_evidence,
            artifacts=[REAL_PRODUCTION_REPO_ARTIFACT],
            blockingIssues=[],
            nextActions=[],
        )
    return data


def fake_ready_migration_readiness_with_test_environment_evidence() -> dict[str, Any]:
    data = fake_ready_migration_readiness_with_real_evidence_refs()
    data["tracks"][0]["currentEvidence"].append("http://100.99.133.43:18080/prod-readiness-evidence.json")
    return data


def fake_ready_cutover_gate() -> dict[str, Any]:
    data = load_json("metadata/production-cutover-gate.json")
    gates = data["gates"]
    data.update(status="READY_FOR_PRODUCTION_CUTOVER")
    data["summary"] = {
        "totalGates": len(gates),
        "ready": len(gates),
        "planned": 0,
        "blocked": 0,
        "notStarted": 0,
        "productionBlockers": 0,
        "productionBacklogItems": 0,
    }
    data["releaseRule"] = "READY negative test with intentionally fake template evidence."
    for gate in gates:
        gate.update(status="READY", blockingReason="")
        evidence = gate.setdefault("currentEvidence", [])
        if not any(isinstance(value, str) and "example" in value.lower() for value in evidence):
            evidence.append("deploy/database/production-migration/table-list.example.txt")
    return data


def fake_ready_cutover_gate_with_real_evidence_refs() -> dict[str, Any]:
    data = load_json("metadata/production-cutover-gate.json")
    gates = data["gates"]
    data.update(status="READY_FOR_PRODUCTION_CUTOVER")
    data["summary"] = {
        "totalGates": len(gates),
        "ready": len(gates),
        "planned": 0,
        "blocked": 0,
        "notStarted": 0,
        "productionBlockers": 0,
        "productionBacklogItems": 0,
    }
    data["releaseRule"] = "READY negative test with intentionally fake production evidence."
    for gate in gates:
        gate_id = str(gate.get("id", "gate"))
        gate.update(
            status="READY",
            blockingReason="",
            currentEvidence=[f"{REAL_PRODUCTION_REPO_ARTIFACT}#{gate_id}"],
        )
    return data


def fake_ready_cutover_gate_with_test_environment_evidence() -> dict[str, Any]:
    data = fake_ready_cutover_gate_with_real_evidence_refs()
    data["gates"][0]["currentEvidence"].append("http://100.99.133.43:18080/prod-cutover-evidence.json")
    return data


def fake_ready_business_dependency_readiness() -> dict[str, Any]:
    data = load_json("metadata/business-dependency-readiness-smoke.json")
    items = data["items"]
    data["summary"] = {
        "status": "READY",
        "dependencies": len(items),
        "ready": len(items),
        "actionRequired": 0,
        "blocked": 0,
    }
    for item in items:
        item.update(status="READY", issues=[], nextAction="negative-test ready fixture")
        for service in item.get("services", []):
            service.update(fetched=True, hostCount=1, healthyHostCount=1, ports=[18000], instanceIds=["negative-test"])
        for endpoint in item.get("endpoints", []):
            endpoint.update(status=200, body="{}", tenantServiceMissing=False)
        if item.get("id") == "material-service":
            item["database"]["materialLikeTableCount"] = max(1, int(item["database"].get("materialLikeTableCount") or 0))
            if not item["database"].get("materialLikeTables"):
                item["database"]["materialLikeTables"] = ["wom_output_materials"]
        if item.get("id") == "process-analysis":
            item["database"].update(
                processAnalysisTableCount=1,
                processAnalysisTables=["process_analysis_marker"],
                processAnalysisRuntimeViewCount=1,
                processAnalysisMenuCount=1,
            )
    return data


def fake_ready_business_dependency_with_endpoint_404() -> dict[str, Any]:
    data = fake_ready_business_dependency_readiness()
    data["items"][0]["endpoints"][0]["status"] = 404
    return data


def fake_ready_business_dependency_with_missing_process_tables() -> dict[str, Any]:
    data = fake_ready_business_dependency_readiness()
    for item in data["items"]:
        if item.get("id") == "process-analysis":
            item["database"]["processAnalysisTableCount"] = 0
            item["database"]["processAnalysisTables"] = []
    return data


def main() -> int:
    failures: list[str] = []
    cases = [
        (
            "database-migration",
            "deploy/database/production-migration/scripts/validate-migration-evidence.py",
            "--evidence",
            fake_ready_database_migration(),
        ),
        (
            "rollback",
            "deploy/rollback/production-migration/scripts/validate-rollback-evidence.py",
            "--evidence",
            fake_ready_rollback(),
        ),
        (
            "license",
            "deploy/license/production-migration/scripts/validate-license-decision.py",
            "--decision",
            fake_ready_license(),
        ),
        (
            "network-tls",
            "deploy/network/production-migration/scripts/validate-network-tls-plan.py",
            "--plan",
            fake_ready_network(),
        ),
        (
            "security-hardening",
            "deploy/security/production-migration/scripts/validate-security-hardening-plan.py",
            "--plan",
            fake_ready_security(),
        ),
        (
            "business-smoke-signoff",
            "deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py",
            "--signoff",
            fake_ready_business_smoke(),
        ),
        (
            "nacos-runtime-patch",
            "deploy/nacos/production-migration/scripts/validate-nacos-runtime-patch-evidence.py",
            "--evidence",
            fake_ready_nacos_runtime_patch(),
        ),
        (
            "minio-migration",
            "deploy/minio/production-migration/scripts/validate-minio-migration-evidence.py",
            "--evidence",
            fake_ready_minio_migration(),
        ),
        (
            "keycloak-migration",
            "deploy/keycloak/production-migration/scripts/validate-keycloak-migration-evidence.py",
            "--evidence",
            fake_ready_keycloak_migration(),
        ),
    ]
    ledger_cases = [
        (
            "production-migration-readiness",
            "scripts/verify-production-migration-readiness.py",
            fake_ready_migration_readiness(),
        ),
        (
            "production-cutover-gate",
            "scripts/verify-production-cutover-gate.py",
            fake_ready_cutover_gate(),
        ),
    ]
    test_environment_ledger_cases = [
        (
            "production-migration-readiness-test-environment",
            "scripts/verify-production-migration-readiness.py",
            fake_ready_migration_readiness_with_test_environment_evidence(),
        ),
        (
            "production-cutover-gate-test-environment",
            "scripts/verify-production-cutover-gate.py",
            fake_ready_cutover_gate_with_test_environment_evidence(),
        ),
    ]

    with tempfile.TemporaryDirectory(prefix="adp-production-ready-gate-") as temp:
        temp_dir = Path(temp)
        for label, validator, option, payload in cases:
            payload_path = write_temp_json(temp_dir, f"{label}.json", payload)
            run_negative_case(label, validator, option, payload_path, failures)
        basic_config_payload_path = write_temp_json(
            temp_dir,
            "business-smoke-basic-config-incomplete.json",
            fake_ready_business_smoke_with_real_evidence_refs(),
        )
        run_negative_case_expecting(
            "business-smoke-signoff-basic-config-coverage",
            "deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py",
            "--signoff",
            basic_config_payload_path,
            "basic config coverage",
            failures,
        )
        plain_risk_payload = fake_ready_business_smoke_with_plain_risk_acceptance()
        run_business_smoke_negative_case_expecting(
            "business-smoke-signoff-structured-risk-acceptance",
            write_temp_json(temp_dir, "business-smoke-plain-risk-acceptance.json", plain_risk_payload),
            write_temp_json(temp_dir, "business-smoke-risk-blockers.json", fake_blocker_ledger_for_business_smoke(plain_risk_payload)),
            write_temp_json(temp_dir, "business-smoke-risk-backlog.json", fake_backlog_ledger_for_business_smoke(plain_risk_payload)),
            write_temp_json(temp_dir, "business-smoke-risk-basic-config.json", fake_complete_basic_config_coverage()),
            "signed object",
            failures,
        )
        test_environment_business_smoke_payload = fake_ready_business_smoke_with_test_environment_evidence()
        test_environment_export_target_ids = {
            target_id
            for issue in test_environment_business_smoke_payload["openIssues"]
            for target_id in issue.get("exportTargetIds", [])
            if isinstance(target_id, str) and target_id.strip()
        }
        run_business_smoke_negative_case_expecting(
            "business-smoke-signoff-test-environment-evidence",
            write_temp_json(temp_dir, "business-smoke-test-environment-evidence.json", test_environment_business_smoke_payload),
            write_temp_json(temp_dir, "business-smoke-test-environment-blockers.json", fake_blocker_ledger_for_business_smoke(test_environment_business_smoke_payload)),
            write_temp_json(temp_dir, "business-smoke-test-environment-backlog.json", fake_backlog_ledger_for_business_smoke(test_environment_business_smoke_payload)),
            write_temp_json(temp_dir, "business-smoke-test-environment-basic-config.json", fake_complete_basic_config_coverage()),
            EXPECTED_TEST_ENVIRONMENT_REJECTION,
            failures,
            write_temp_json(temp_dir, "business-smoke-test-environment-export-gap.json", fake_export_gap_for_business_smoke(test_environment_export_target_ids)),
        )
        missing_qrcode_payload = fake_ready_business_smoke_with_real_evidence_refs()
        for issue in missing_qrcode_payload["openIssues"]:
            if isinstance(issue.get("backlogItemIds"), list):
                issue["backlogItemIds"] = [
                    item_id for item_id in issue["backlogItemIds"] if item_id != "PROD-ACTION-008"
                ]
        run_business_smoke_negative_case_expecting(
            "business-smoke-signoff-missing-qrcode-backlog-coverage",
            write_temp_json(temp_dir, "business-smoke-missing-qrcode-backlog.json", missing_qrcode_payload),
            write_temp_json(temp_dir, "business-smoke-missing-qrcode-blockers.json", fake_blocker_ledger_for_business_smoke(missing_qrcode_payload)),
            write_temp_json(
                temp_dir,
                "business-smoke-missing-qrcode-backlog-ledger.json",
                fake_backlog_ledger_for_business_smoke_with_extra_items(missing_qrcode_payload, {"PROD-ACTION-008"}),
            ),
            write_temp_json(temp_dir, "business-smoke-missing-qrcode-basic-config.json", fake_complete_basic_config_coverage()),
            "PROD-ACTION-008",
            failures,
        )
        missing_export_target_payload = fake_ready_business_smoke_with_real_evidence_refs()
        export_target_ids = {
            target_id
            for issue in missing_export_target_payload["openIssues"]
            for target_id in issue.get("exportTargetIds", [])
            if isinstance(target_id, str) and target_id.strip()
        }
        for issue in missing_export_target_payload["openIssues"]:
            if isinstance(issue.get("exportTargetIds"), list):
                issue["exportTargetIds"] = [
                    target_id
                    for target_id in issue["exportTargetIds"]
                    if target_id != "qcs-inspect-release"
                ]
        run_business_smoke_negative_case_expecting(
            "business-smoke-signoff-missing-export-target-coverage",
            write_temp_json(temp_dir, "business-smoke-missing-export-target.json", missing_export_target_payload),
            write_temp_json(temp_dir, "business-smoke-missing-export-target-blockers.json", fake_blocker_ledger_for_business_smoke(missing_export_target_payload)),
            write_temp_json(temp_dir, "business-smoke-missing-export-target-backlog.json", fake_backlog_ledger_for_business_smoke(missing_export_target_payload)),
            write_temp_json(temp_dir, "business-smoke-missing-export-target-basic-config.json", fake_complete_basic_config_coverage()),
            "qcs-inspect-release",
            failures,
            write_temp_json(temp_dir, "business-smoke-missing-export-target-gap.json", fake_export_gap_for_business_smoke(export_target_ids)),
        )
        for label, validator, payload in ledger_cases:
            run_inprocess_negative_case(label, validator, payload, failures)
        for label, validator, payload in test_environment_ledger_cases:
            run_inprocess_negative_case_expecting(
                label,
                validator,
                payload,
                EXPECTED_TEST_ENVIRONMENT_REJECTION,
                failures,
            )
        run_inprocess_negative_case_expecting(
            "business-dependency-readiness-endpoint-status",
            "scripts/verify-business-dependency-readiness.py",
            fake_ready_business_dependency_with_endpoint_404(),
            "HTTP 2xx",
            failures,
        )
        run_inprocess_negative_case_expecting(
            "business-dependency-readiness-process-tables",
            "scripts/verify-business-dependency-readiness.py",
            fake_ready_business_dependency_with_missing_process_tables(),
            "PostgreSQL process analysis tables",
            failures,
        )

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        print(f"Production evidence ready gate regression failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Production evidence ready gate regression passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
