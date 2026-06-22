#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/project-goal-acceptance.json"
DOC_PATH = ROOT / "docs/project-goal-acceptance.md"
PLATFORM_VALIDATION_PATH = ROOT / "metadata/platform-validation-smoke.json"
PRODUCTION_MATRIX_PATH = ROOT / "metadata/production-module-test-cases.json"
PRODUCTION_BLOCKERS_PATH = ROOT / "metadata/production-module-blockers.json"
PRODUCTION_BACKLOG_PATH = ROOT / "metadata/production-module-backlog.json"
BUSINESS_DEPENDENCY_CONTRACTS_PATH = ROOT / "metadata/business-dependency-contracts.json"
PRODUCTION_EXPORT_READINESS_PATH = ROOT / "metadata/production-export-readiness-smoke.json"
PRODUCTION_MIGRATION_READINESS_PATH = ROOT / "metadata/production-migration-readiness.json"
PRODUCTION_CUTOVER_GATE_PATH = ROOT / "metadata/production-cutover-gate.json"
PRODUCTION_REHEARSAL_PLAN_PATH = ROOT / "metadata/production-rehearsal-plan.json"
PERSISTENCE_ACCEPTANCE_PATH = ROOT / "metadata/persistence-acceptance.json"
BASIC_CONFIG_COVERAGE_PATH = ROOT / "metadata/basic-config-coverage.json"
BASIC_CONFIG_ACTION_MATRIX_PATH = ROOT / "metadata/basic-config-action-matrix.json"
ENTITY_MODEL_PERSISTENCE_PATH = ROOT / "metadata/entity-model-config-crud-persistence-acceptance.json"
TEST_ENVIRONMENT_SMOKE_PATH = ROOT / "metadata/test-environment-smoke.json"
POSTGRES_RUNTIME_SMOKE_PATH = ROOT / "metadata/postgres-runtime-smoke.json"
NACOS_CONFIG_SMOKE_PATH = ROOT / "metadata/nacos-config-drift-smoke.json"
KEYCLOAK_JWT_SMOKE_PATH = ROOT / "metadata/keycloak-jwt-runtime-smoke.json"
MINIO_RUNTIME_SMOKE_PATH = ROOT / "metadata/minio-runtime-smoke.json"
RUNTIME_PATCH_MANIFEST_PATH = ROOT / "metadata/runtime-patch-manifest.json"
WOM_TOOLBAR_ROW_SMOKE_PATH = ROOT / "metadata/wom-toolbar-row-smoke.json"
WOM_TOOLBAR_ACTION_COVERAGE_PATH = ROOT / "metadata/wom-toolbar-action-coverage.json"
CI_REQUIRED_FILE_INVENTORY_PATH = ROOT / "metadata/ci-required-file-inventory.json"
ORACLE_REPLACEMENT_STATUS_PATH = ROOT / "metadata/oracle-replacement-status.json"
ORACLE_MIGRATION_AUDIT_PATH = ROOT / "metadata/oracle-migration-audit.json"
POSTGRES_MIGRATION_INVENTORY_PATH = ROOT / "metadata/postgres-migration-inventory.json"

G020_REQUIRED_TRACK_ARTIFACTS = {
    "database": {
        "deploy/database/production-migration/README.md",
        "deploy/database/production-migration/scripts/run-source-inventory.sh",
        "deploy/database/production-migration/scripts/run-target-preflight.sh",
        "deploy/database/production-migration/scripts/compare-row-counts.py",
        "deploy/database/production-migration/scripts/compare-checksums.py",
        "deploy/database/production-migration/source-checksums.example.tsv",
        "deploy/database/production-migration/target-checksums.example.tsv",
    },
    "rollback": {
        "deploy/rollback/production-migration/README.md",
        "deploy/rollback/production-migration/rollback-evidence.example.json",
        "deploy/rollback/production-migration/scripts/validate-rollback-evidence.py",
    },
    "license": {
        "deploy/license/production-migration/README.md",
        "deploy/license/production-migration/license-decision.example.json",
        "deploy/license/production-migration/scripts/validate-license-decision.py",
    },
    "minio": {
        "deploy/minio/production-migration/README.md",
        "deploy/minio/production-migration/minio-migration-evidence.example.json",
        "deploy/minio/production-migration/scripts/validate-minio-migration-evidence.py",
        "deploy/minio/production-migration/scripts/run-bucket-inventory.sh",
        "deploy/minio/production-migration/scripts/compare-bucket-inventory.py",
        "deploy/docker/scripts/adp-minio-runtime-smoke.js",
        "metadata/minio-runtime-smoke.json",
    },
    "keycloak": {
        "deploy/keycloak/production-migration/README.md",
        "deploy/keycloak/production-migration/keycloak-migration-evidence.example.json",
        "deploy/keycloak/production-migration/scripts/export-realm-inventory.sh",
        "deploy/keycloak/production-migration/scripts/compare-realm-inventory.py",
        "deploy/keycloak/production-migration/scripts/validate-keycloak-migration-evidence.py",
        "deploy/docker/scripts/adp-keycloak-jwt-runtime-smoke.js",
        "metadata/keycloak-jwt-runtime-smoke.json",
    },
    "nacos-runtime": {
        "docs/production-migration/nacos-runtime-patch-runbook.md",
        "deploy/nacos/production-migration/README.md",
        "deploy/nacos/production-migration/nacos-runtime-patch-evidence.example.json",
        "deploy/nacos/production-migration/scripts/validate-nacos-runtime-patch-evidence.py",
        "deploy/docker/scripts/adp-nacos-config-drift-smoke.js",
        "metadata/nacos-config-drift-smoke.json",
        "metadata/runtime-patch-manifest.json",
        "docs/production-migration/runtime-patch-manifest.md",
    },
    "network-tls": {
        "deploy/network/production-migration/README.md",
        "deploy/network/production-migration/network-tls-plan.example.json",
        "deploy/network/production-migration/scripts/validate-network-tls-plan.py",
        "deploy/docker/scripts/adp-test-environment-smoke.js",
        "metadata/test-environment-smoke.json",
    },
    "security": {
        "deploy/security/production-migration/README.md",
        "deploy/security/production-migration/security-hardening-plan.example.json",
        "deploy/security/production-migration/scripts/validate-security-hardening-plan.py",
    },
    "business-smoke": {
        "deploy/business-smoke/production-migration/README.md",
        "deploy/business-smoke/production-migration/business-smoke-signoff.example.json",
        "deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py",
        "metadata/production-module-blockers.json",
        "metadata/production-module-backlog.json",
        "metadata/business-dependency-readiness-smoke.json",
        "metadata/production-export-readiness-smoke.json",
    },
}

REQUIRED_ITEM_IDS = {
    "G-001",
    "G-002",
    "G-003",
    "G-004",
    "G-005",
    "G-006",
    "G-007",
    "G-008",
    "G-009",
    "G-010",
    "G-011",
    "G-012",
    "G-013",
    "G-014",
    "G-015",
    "G-016",
    "G-017",
    "G-018",
    "G-019",
    "G-020",
}

ALLOWED_STATUSES = {"READY", "PARTIAL", "BLOCKED", "FAIL", "NOT_STARTED"}
ALLOWED_OVERALL_STATUSES = {"IN_PROGRESS_NOT_COMPLETE", "COMPLETE"}
REQUIRED_ITEM_KEYS = [
    "id",
    "title",
    "status",
    "scope",
    "artifacts",
    "currentEvidence",
    "blockingIssues",
    "nextActions",
]


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def read_json(failures: list[str]) -> dict[str, Any]:
    try:
        with REPORT_PATH.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing goal acceptance report: {REPORT_PATH.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {REPORT_PATH.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, "goal acceptance report must be a JSON object")
        return {}
    return data


def read_json_file(path: Path, failures: list[str], label: str) -> dict[str, Any]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing {label}: {path.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"{label} must be a JSON object")
        return {}
    return data


def check_doc(failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"required document missing: {DOC_PATH.relative_to(ROOT)}")
        return
    text = DOC_PATH.read_text(encoding="utf-8")
    required_fragments = [
        "IN_PROGRESS_NOT_COMPLETE",
        "| ID | 目标项 | 状态 | 当前证据 | 缺口 |",
        "生产模块完整功能",
        "Oracle 替换为 PostgreSQL 默认路径",
        "PostgreSQL 缺口进入幂等 SQL/backlog",
        "生产迁移前置项",
        "make project-goal-acceptance-check",
        "跨账本一致性",
    ]
    for fragment in required_fragments:
        if fragment not in text:
            fail(failures, f"goal acceptance document missing required text: {fragment}")


def check_artifact(item_id: str, artifact: Any, failures: list[str]) -> None:
    if not isinstance(artifact, str) or not artifact.strip():
        fail(failures, f"{item_id}.artifacts must contain non-empty relative paths")
        return
    path = Path(artifact)
    if path.is_absolute() or ".." in path.parts:
        fail(failures, f"{item_id} artifact must stay inside the repository: {artifact}")
        return
    if not (ROOT / path).exists():
        fail(failures, f"{item_id} artifact does not exist: {artifact}")


def check_item(index: int, item: Any, failures: list[str]) -> str | None:
    if not isinstance(item, dict):
        fail(failures, f"items[{index}] must be an object")
        return None

    for key in REQUIRED_ITEM_KEYS:
        if key not in item:
            fail(failures, f"items[{index}] missing required key: {key}")

    item_id = str(item.get("id", "")).strip()
    status = item.get("status")
    if status not in ALLOWED_STATUSES:
        fail(failures, f"{item_id or f'items[{index}]'} has invalid status: {status!r}")
        return None

    for key in ("title", "scope"):
        if not str(item.get(key, "")).strip():
            fail(failures, f"{item_id} must include non-empty {key}")

    for key in ("artifacts", "currentEvidence", "blockingIssues", "nextActions"):
        if not isinstance(item.get(key), list):
            fail(failures, f"{item_id}.{key} must be a list")

    artifacts = as_list(item.get("artifacts"))
    for artifact in artifacts:
        check_artifact(item_id, artifact, failures)

    if status == "READY":
        if not artifacts:
            fail(failures, f"{item_id} READY must include at least one artifact")
        if not as_list(item.get("currentEvidence")):
            fail(failures, f"{item_id} READY must include currentEvidence")
        if as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} READY must not include blockingIssues")
    else:
        if not as_list(item.get("blockingIssues")):
            fail(failures, f"{item_id} {status} must include blockingIssues")
        if not as_list(item.get("nextActions")):
            fail(failures, f"{item_id} {status} must include nextActions")

    return status


def check_summary(data: dict[str, Any], failures: list[str]) -> None:
    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return

    items = as_list(data.get("items"))
    statuses = [item.get("status") for item in items if isinstance(item, dict)]
    expected = {
        "totalItems": len(items),
        "ready": statuses.count("READY"),
        "partial": statuses.count("PARTIAL"),
        "blocked": statuses.count("BLOCKED"),
        "fail": statuses.count("FAIL"),
        "notStarted": statuses.count("NOT_STARTED"),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def artifact_set(item: dict[str, Any]) -> set[str]:
    return {str(artifact) for artifact in as_list(item.get("artifacts"))}


def check_item_has_artifacts(item_id: str, items_by_id: dict[str, dict[str, Any]], artifacts: set[str], failures: list[str]) -> None:
    item = items_by_id.get(item_id)
    if not item:
        return
    missing = sorted(artifacts - artifact_set(item))
    if missing:
        fail(failures, f"{item_id} must reference artifacts: " + ", ".join(missing))


def is_git_tracked(relative_path: str) -> bool:
    result = subprocess.run(
        ["git", "ls-files", "--error-unmatch", relative_path],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    return result.returncode == 0


def result_by_id(report: dict[str, Any]) -> dict[str, dict[str, Any]]:
    results: dict[str, dict[str, Any]] = {}
    for result in as_list(report.get("results")):
        if isinstance(result, dict) and result.get("id"):
            results[str(result["id"])] = result
    return results


def summary_int(summary: dict[str, Any], key: str) -> int:
    value = summary.get(key, 0)
    return value if isinstance(value, int) else 0


def summary_status(report: dict[str, Any]) -> str | None:
    summary = report.get("summary")
    if isinstance(summary, dict):
        status = summary.get("status")
        return status if isinstance(status, str) else None
    return None


def item_evidence_text(item: dict[str, Any]) -> str:
    return "\n".join(str(value) for value in as_list(item.get("currentEvidence")))


def source_audit_target_export_match_count(report: dict[str, Any]) -> int:
    count = 0
    for item in as_list(report.get("items")):
        if not isinstance(item, dict):
            continue
        source_audit = item.get("sourceAudit")
        if not isinstance(source_audit, dict):
            continue
        count += len(as_list(source_audit.get("targetExportMatches")))
    return count


def check_export_evidence_text(
    item_id: str,
    item: dict[str, Any],
    export_readiness: dict[str, Any],
    failures: list[str],
) -> None:
    if "metadata/production-export-readiness-smoke.json" not in artifact_set(item):
        return
    summary = export_readiness.get("summary")
    target = export_readiness.get("target")
    if not isinstance(summary, dict) or not isinstance(target, dict):
        fail(failures, "production export readiness report must include summary and target objects")
        return

    base_url = target.get("baseUrl")
    browser_base_url = target.get("browserBaseUrl") or base_url
    if browser_base_url == base_url:
        target_base_fragment = f"API base 和 browser base 均为 {base_url}"
    else:
        target_base_fragment = f"API base={base_url}, browser base={browser_base_url}"

    target_export_matches = source_audit_target_export_match_count(export_readiness)
    required_fragments = [
        target_base_fragment,
        f"pagePass={summary.get('pagePass')}",
        f"visibleExportActions={summary.get('visibleExportActions')}",
        f"runtimeExportActions={summary.get('runtimeExportActions')}",
        f"nonEmptyDownloads={summary.get('nonEmptyDownloads')}",
        f"verifiedDataExports={summary.get('verifiedDataExports')}",
        f"targetExportMatches={target_export_matches}",
    ]
    evidence_text = item_evidence_text(item)
    for fragment in required_fragments:
        if fragment not in evidence_text:
            fail(failures, f"{item_id} currentEvidence must include current production export readiness value: {fragment}")


def check_wom_toolbar_evidence_text(
    item_id: str,
    item: dict[str, Any],
    row_smoke: dict[str, Any],
    action_coverage: dict[str, Any],
    failures: list[str],
) -> None:
    marker = str(row_smoke.get("marker") or "").strip()
    task_id = str(row_smoke.get("taskId") or "").strip()
    generated_at = str(row_smoke.get("generatedAt") or "").strip()
    status = str(row_smoke.get("status") or "").strip()
    if not marker or not task_id or not generated_at:
        fail(failures, "wom-toolbar-row-smoke must include generatedAt, marker and taskId")
        return
    if status != "PASS_WITH_KNOWN_BLOCKERS":
        fail(failures, "wom-toolbar-row-smoke must remain PASS_WITH_KNOWN_BLOCKERS while dependency buttons are guarded")
    if action_coverage:
        runtime_evidence = action_coverage.get("runtimeButtonEvidence")
        if not isinstance(runtime_evidence, dict):
            fail(failures, "wom-toolbar-action-coverage runtimeButtonEvidence must be an object")
        else:
            expected = {
                "latestToolbarRowSmokeGeneratedAt": generated_at,
                "latestToolbarRowSmokeMarker": marker,
                "latestToolbarRowSmokeTaskId": task_id,
            }
            for key, value in expected.items():
                if runtime_evidence.get(key) != value:
                    fail(failures, f"wom-toolbar-action-coverage {key} must match latest row smoke")

    evidence_text = item_evidence_text(item)
    required_fragments = [
        marker,
        task_id,
        generated_at,
        "metadata/wom-toolbar-row-smoke.json",
        "PASS_WITH_KNOWN_BLOCKERS",
    ]
    for fragment in required_fragments:
        if fragment not in evidence_text:
            fail(failures, f"{item_id} currentEvidence must include latest WOM toolbar row smoke value: {fragment}")


def oracle_status_by_id(oracle_status: dict[str, Any]) -> dict[str, str]:
    result: dict[str, str] = {}
    for item in as_list(oracle_status.get("checks")):
        if isinstance(item, dict) and item.get("id"):
            result[str(item["id"])] = str(item.get("status", ""))
    return result


def check_oracle_evidence_text(
    item: dict[str, Any],
    oracle_status: dict[str, Any],
    oracle_audit: dict[str, Any],
    failures: list[str],
) -> None:
    status_summary = oracle_status.get("summary")
    if not isinstance(status_summary, dict):
        fail(failures, "Oracle replacement status summary must be an object")
        return

    runtime_scan = oracle_status.get("runtimeConfigOracleScan")
    if not isinstance(runtime_scan, dict):
        fail(failures, "Oracle replacement status runtimeConfigOracleScan must be an object")
        return

    audit_categories = oracle_audit.get("categoryCounts")
    if not isinstance(audit_categories, dict):
        fail(failures, "Oracle migration audit categoryCounts must be an object")
        return

    finding_count = int(oracle_audit.get("findingCount") or 0)
    unclassified_count = int(audit_categories.get("unclassified-oracle-reference") or 0)
    classified_count = finding_count - unclassified_count
    required_fragments = [
        (
            "metadata/oracle-replacement-status.json summary: "
            f"blockingIssueCount={status_summary.get('blockingIssueCount')}, "
            f"runtimeConfigActiveOracleLineCount={status_summary.get('runtimeConfigActiveOracleLineCount')}, "
            f"directOracleDependencyCount={status_summary.get('directOracleDependencyCount')}, "
            f"oracleBacklogReferenceCount={status_summary.get('oracleBacklogReferenceCount')}, "
            f"postgresMigrationHighRiskCount={status_summary.get('postgresMigrationHighRiskCount')}, "
            f"postgresMapperAuditErrorCount={status_summary.get('postgresMapperAuditErrorCount')}"
        ),
        (
            "metadata/oracle-migration-audit.json currently has "
            f"{classified_count} classified references, {unclassified_count} unclassified references"
        ),
        (
            f"{runtime_scan.get('scannedFiles')} config files and "
            f"{runtime_scan.get('scannedActiveLines')} active config lines scanned, "
            f"active Oracle-like default lines={runtime_scan.get('activeOracleLineCount')}"
        ),
    ]
    evidence_text = item_evidence_text(item)
    for fragment in required_fragments:
        if fragment not in evidence_text:
            fail(failures, f"G-003 currentEvidence must include current Oracle ledger value: {fragment}")


def check_oracle_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    item = items_by_id.get("G-003")
    oracle_status = read_json_file(ORACLE_REPLACEMENT_STATUS_PATH, failures, "Oracle replacement status")
    oracle_audit = read_json_file(ORACLE_MIGRATION_AUDIT_PATH, failures, "Oracle migration audit")
    if not item:
        return

    check_item_has_artifacts(
        "G-003",
        items_by_id,
        {
            "docs/oracle-replacement-status.md",
            "metadata/oracle-replacement-status.json",
            "scripts/generate-oracle-replacement-status.py",
            "docs/oracle-migration-backlog.md",
            "metadata/oracle-migration-audit.json",
            "scripts/generate-oracle-migration-audit.py",
            "deploy/docker/.env.oracle-legacy.example",
        },
        failures,
    )
    if not oracle_status or not oracle_audit:
        return

    status_summary = oracle_status.get("summary")
    if not isinstance(status_summary, dict):
        fail(failures, "Oracle replacement status summary must be an object")
        return

    status_checks = oracle_status_by_id(oracle_status)
    required_pass_checks = {
        "runtime-default-postgresql",
        "parent-pom-oracle-legacy-profile",
        "runtime-config-no-oracle-defaults",
        "mapper-postgres-audit",
        "postgres-migration-governance",
        "source-module-oracle-policy",
    }
    for check_id in sorted(required_pass_checks):
        if status_checks.get(check_id) != "pass":
            fail(failures, f"G-003 requires Oracle replacement check {check_id}=pass")

    if summary_int(status_summary, "blockingIssueCount") != 0:
        fail(failures, "G-003 requires Oracle replacement status blockingIssueCount=0")
    if summary_int(status_summary, "runtimeConfigActiveOracleLineCount") != 0:
        fail(failures, "G-003 requires runtimeConfigActiveOracleLineCount=0")
    if summary_int(status_summary, "postgresMigrationHighRiskCount") != 0:
        fail(failures, "G-003 requires postgresMigrationHighRiskCount=0")
    if summary_int(status_summary, "postgresMapperAuditErrorCount") != 0:
        fail(failures, "G-003 requires postgresMapperAuditErrorCount=0")
    if status_summary.get("sourceModuleOraclePolicyPass") is not True:
        fail(failures, "G-003 requires sourceModuleOraclePolicyPass=true")

    compose_defaults = oracle_status.get("composeDefaults")
    if not isinstance(compose_defaults, dict):
        fail(failures, "Oracle replacement status composeDefaults must be an object")
    else:
        expected_defaults = {
            "contentInventoryDefault": "postgresql",
            "composeDefaultsPostgres": True,
            "composeImplicitOracleDefault": False,
            "envExamplePostgres": True,
            "envExamplePostgresHost": True,
            "oracleLegacyTemplatePresent": True,
        }
        for key, expected in expected_defaults.items():
            if compose_defaults.get(key) != expected:
                fail(failures, f"G-003 requires composeDefaults.{key}={expected!r}")

    parent_policy = oracle_status.get("parentPomOraclePolicy")
    if not isinstance(parent_policy, dict):
        fail(failures, "Oracle replacement status parentPomOraclePolicy must be an object")
    else:
        if parent_policy.get("defaultOracleDependencyManagementCount") != 0:
            fail(failures, "G-003 requires parent POM default Oracle dependency management count=0")
        if parent_policy.get("oracleLegacyProfilePresent") is not True:
            fail(failures, "G-003 requires parent POM oracle-legacy profile")
        if int(parent_policy.get("oracleLegacyDependencyManagementCount") or 0) <= 0:
            fail(failures, "G-003 requires Oracle JDBC to remain isolated in oracle-legacy profile")

    audit_categories = oracle_audit.get("categoryCounts")
    if not isinstance(audit_categories, dict):
        fail(failures, "Oracle migration audit categoryCounts must be an object")
    else:
        unclassified_count = int(audit_categories.get("unclassified-oracle-reference") or 0)
        if unclassified_count != 0:
            fail(failures, "G-003 requires oracle migration audit unclassified references=0")
        if oracle_audit.get("findingCount") != status_summary.get("oracleBacklogReferenceCount"):
            fail(failures, "G-003 requires Oracle audit findingCount to match replacement status backlog count")

    if item.get("status") == "READY" and (
        summary_int(status_summary, "directOracleDependencyCount") > 0
        or summary_int(status_summary, "oracleBacklogReferenceCount") > 0
    ):
        fail(failures, "G-003 cannot be READY while Oracle backlog or direct Oracle dependencies remain")

    check_oracle_evidence_text(item, oracle_status, oracle_audit, failures)


def persistence_coverage_key(item: dict[str, Any]) -> tuple[str, str, str, str]:
    return (
        str(item.get("status", "")).strip(),
        str(item.get("module", "")).strip(),
        str(item.get("operation", "")).strip(),
        str(item.get("api", "")).strip(),
    )


def check_postgres_gap_evidence_text(
    item: dict[str, Any],
    persistence: dict[str, Any],
    production_backlog: dict[str, Any],
    postgres_inventory: dict[str, Any],
    failures: list[str],
) -> None:
    persistence_summary = persistence.get("summary")
    backlog_summary = production_backlog.get("summary")
    if not isinstance(persistence_summary, dict):
        fail(failures, "persistence acceptance summary must be an object for G-019")
        return
    if not isinstance(backlog_summary, dict):
        fail(failures, "production module backlog summary must be an object for G-019")
        return

    required_fragments = [
        (
            "metadata/persistence-acceptance.json currently has "
            f"{persistence_summary.get('fail')} FAIL and {persistence_summary.get('blocked')} BLOCKED rows"
        ),
        (
            "metadata/production-module-backlog.json covers those unresolved persistence rows with "
            f"{backlog_summary.get('totalItems')} backlog items, "
            f"{backlog_summary.get('failBacklog')} FAIL_BACKLOG and {backlog_summary.get('blocked')} BLOCKED"
        ),
        (
            "metadata/postgres-migration-inventory.json currently has "
            f"{postgres_inventory.get('migrationCount')} scripts, "
            f"highRiskStatementCount={postgres_inventory.get('highRiskStatementCount')}, "
            f"watchStatementCount={postgres_inventory.get('watchStatementCount')}, "
            f"watchSafetyIssueCount={postgres_inventory.get('watchSafetyIssueCount')}"
        ),
    ]
    evidence_text = item_evidence_text(item)
    for fragment in required_fragments:
        if fragment not in evidence_text:
            fail(failures, f"G-019 currentEvidence must include current PostgreSQL gap ledger value: {fragment}")


def check_postgres_gap_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    item = items_by_id.get("G-019")
    persistence = read_json_file(PERSISTENCE_ACCEPTANCE_PATH, failures, "persistence acceptance report")
    production_backlog = read_json_file(PRODUCTION_BACKLOG_PATH, failures, "production module backlog report")
    postgres_inventory = read_json_file(POSTGRES_MIGRATION_INVENTORY_PATH, failures, "PostgreSQL migration inventory")
    if not item:
        return

    check_item_has_artifacts(
        "G-019",
        items_by_id,
        {
            "docs/oracle-migration-backlog.md",
            "metadata/oracle-migration-audit.json",
            "docs/postgres-migration-index.md",
            "docs/postgres-migration-watch-rationale.md",
            "metadata/postgres-migration-inventory.json",
            "docs/backend-table-audit/persistence-acceptance.md",
            "metadata/persistence-acceptance.json",
            "scripts/verify-persistence-acceptance.py",
            "docs/production-module-backlog.md",
            "metadata/production-module-backlog.json",
            "scripts/verify-production-module-backlog.py",
        },
        failures,
    )
    if not persistence or not production_backlog or not postgres_inventory:
        return

    if postgres_inventory.get("highRiskStatementCount") != 0:
        fail(failures, "G-019 requires PostgreSQL migration highRiskStatementCount=0")
    if postgres_inventory.get("watchSafetyIssueCount") != 0:
        fail(failures, "G-019 requires PostgreSQL migration watchSafetyIssueCount=0")
    if postgres_inventory.get("missingNumbers"):
        fail(failures, "G-019 requires PostgreSQL migration missingNumbers to be empty")
    if postgres_inventory.get("duplicateNumbers"):
        fail(failures, "G-019 requires PostgreSQL migration duplicateNumbers to be empty")
    if postgres_inventory.get("invalidNames"):
        fail(failures, "G-019 requires PostgreSQL migration invalidNames to be empty")

    covered_persistence: set[tuple[str, str, str, str]] = set()
    for backlog_item in as_list(production_backlog.get("items")):
        if not isinstance(backlog_item, dict):
            continue
        for coverage_item in as_list(backlog_item.get("persistenceCoverage")):
            if isinstance(coverage_item, dict):
                covered_persistence.add(persistence_coverage_key(coverage_item))

    expected_persistence = {
        persistence_coverage_key(persistence_item)
        for persistence_item in as_list(persistence.get("items"))
        if isinstance(persistence_item, dict) and persistence_item.get("status") in {"FAIL", "BLOCKED"}
    }
    missing_persistence = sorted(expected_persistence - covered_persistence)
    if missing_persistence:
        fail(
            failures,
            "G-019 requires every persistence FAIL/BLOCKED row to be covered by production backlog: "
            + json.dumps(missing_persistence, ensure_ascii=False),
        )

    backlog_summary = production_backlog.get("summary")
    if isinstance(backlog_summary, dict) and summary_int(backlog_summary, "postgresCompatibilityGap"):
        for backlog_item in as_list(production_backlog.get("items")):
            if not isinstance(backlog_item, dict) or backlog_item.get("isPostgresCompatibilityGap") is not True:
                continue
            text = json.dumps(backlog_item, ensure_ascii=False).lower()
            if "sql" not in text and "postgres" not in text:
                fail(
                    failures,
                    f"G-019 PostgreSQL compatibility backlog {backlog_item.get('id')} must reference SQL/PostgreSQL handling",
                )

    check_postgres_gap_evidence_text(item, persistence, production_backlog, postgres_inventory, failures)


def check_sustainable_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    item = items_by_id.get("G-001")
    if not item:
        return

    check_item_has_artifacts(
        "G-001",
        items_by_id,
        {
            ".gitignore",
            "Makefile",
            ".github/workflows/verify.yml",
            "docs/sustainable-development.md",
            "scripts/verify-sustainable-repo.py",
        },
        failures,
    )
    if CI_REQUIRED_FILE_INVENTORY_PATH.exists():
        check_item_has_artifacts(
            "G-001",
            items_by_id,
            {
                "scripts/generate-ci-required-file-inventory.py",
                "metadata/ci-required-file-inventory.json",
            },
            failures,
        )

    inventory = read_json_file(CI_REQUIRED_FILE_INVENTORY_PATH, failures, "CI required file inventory")
    if not inventory:
        return
    inventory_summary = inventory.get("summary")
    if not isinstance(inventory_summary, dict):
        fail(failures, "CI required file inventory summary must be an object")
        return

    missing_references = summary_int(inventory_summary, "missingReferences")
    total_files = summary_int(inventory_summary, "totalFiles")
    tracked = summary_int(inventory_summary, "tracked")
    untracked = summary_int(inventory_summary, "untracked")
    if item.get("status") == "READY" and missing_references != 0:
        fail(failures, "G-001 READY requires CI required file inventory to have zero missingReferences")
    if item.get("status") == "READY" and untracked != 0:
        fail(failures, "G-001 READY requires CI required file inventory to have zero untracked required files")
    if item.get("status") == "READY":
        expected_inventory_fragments = [
            f"metadata/ci-required-file-inventory.json currently records {total_files}",
            f"{tracked} tracked",
            f"{untracked} untracked",
            f"{missing_references} missing references",
        ]
        evidence_text = " ".join(str(value) for value in as_list(item.get("currentEvidence")))
        for fragment in expected_inventory_fragments:
            if fragment not in evidence_text:
                fail(failures, f"G-001 currentEvidence must include current CI inventory fragment: {fragment}")
        if DOC_PATH.exists():
            doc_text = DOC_PATH.read_text(encoding="utf-8")
            expected_doc_fragments = [
                f"`metadata/ci-required-file-inventory.json` 当前记录 {total_files} 个",
                f"{tracked} tracked",
                f"{untracked} untracked",
                f"{missing_references} missing references",
            ]
            for fragment in expected_doc_fragments:
                if fragment not in doc_text:
                    fail(failures, f"G-001 document evidence must include current CI inventory fragment: {fragment}")
        required_tracked = {
            ".gitignore",
            "Makefile",
            ".github/workflows/verify.yml",
            "docs/sustainable-development.md",
            "scripts/verify-sustainable-repo.py",
            "scripts/generate-ci-required-file-inventory.py",
            "metadata/ci-required-file-inventory.json",
        }
        for relative_path in sorted(required_tracked):
            if not is_git_tracked(relative_path):
                fail(failures, f"G-001 READY requires tracked artifact: {relative_path}")


def check_platform_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    platform = read_json_file(PLATFORM_VALIDATION_PATH, failures, "platform validation smoke report")
    if not platform:
        return

    if platform.get("ok") is not True or platform.get("failed") != 0 or platform.get("skipped") != 0:
        for item_id in ("G-006", "G-009", "G-010", "G-011"):
            item = items_by_id.get(item_id)
            if item and item.get("status") == "READY":
                fail(failures, f"{item_id} cannot be READY while platform validation smoke is not fully passing")

    results = result_by_id(platform)
    required_by_goal = {
        "G-006": {"platform-api", "home-todo", "organization", "rbac-authority", "menu-pages"},
        "G-009": {"rbac-authority"},
        "G-010": {"menu-pages"},
        "G-011": {"home-todo"},
    }
    for item_id, required_results in required_by_goal.items():
        item = items_by_id.get(item_id)
        if not item or item.get("status") != "READY":
            continue
        check_item_has_artifacts(item_id, items_by_id, {"metadata/platform-validation-smoke.json"}, failures)
        missing = sorted(required_results - set(results))
        if missing:
            fail(failures, f"{item_id} READY requires platform smoke results: " + ", ".join(missing))
        for result_id in sorted(required_results & set(results)):
            result = results[result_id]
            if result.get("status") != "passed" or result.get("ok") is not True:
                fail(failures, f"{item_id} READY requires {result_id} platform smoke to pass")


def check_production_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    matrix = read_json_file(PRODUCTION_MATRIX_PATH, failures, "production module test matrix")
    blockers = read_json_file(PRODUCTION_BLOCKERS_PATH, failures, "production module blocker report")
    backlog = read_json_file(PRODUCTION_BACKLOG_PATH, failures, "production module backlog report")
    dependency_contracts = read_json_file(BUSINESS_DEPENDENCY_CONTRACTS_PATH, failures, "business dependency contracts")
    export_readiness = read_json_file(PRODUCTION_EXPORT_READINESS_PATH, failures, "production export readiness report")
    persistence = read_json_file(PERSISTENCE_ACCEPTANCE_PATH, failures, "persistence acceptance report")
    row_smoke = read_json_file(WOM_TOOLBAR_ROW_SMOKE_PATH, failures, "WOM toolbar row smoke report")
    action_coverage = read_json_file(WOM_TOOLBAR_ACTION_COVERAGE_PATH, failures, "WOM toolbar action coverage report")
    if not matrix:
        return

    matrix_summary = matrix.get("summary")
    if not isinstance(matrix_summary, dict):
        fail(failures, "production module test matrix summary must be an object")
        return

    blocked = summary_int(matrix_summary, "blocked")
    not_run = summary_int(matrix_summary, "notRun")
    failed = summary_int(matrix_summary, "fail")
    total = summary_int(matrix_summary, "totalCases")
    passed = summary_int(matrix_summary, "pass")
    backlog_summary = backlog.get("summary") if backlog else {}
    backlog_items = summary_int(backlog_summary, "totalItems") if isinstance(backlog_summary, dict) else 0

    if backlog:
        if not isinstance(backlog_summary, dict):
            fail(failures, "production module backlog summary must be an object")
        if backlog.get("overallStatus") == "OPEN" and backlog_items <= 0:
            fail(failures, "production module backlog marked OPEN must contain unresolved items")
        if backlog.get("overallStatus") != "OPEN" and backlog_items > 0:
            fail(failures, "production module backlog with unresolved items must remain OPEN")

    g013 = items_by_id.get("G-013")
    if g013:
        check_item_has_artifacts(
            "G-013",
            items_by_id,
            {
                "metadata/production-module-test-cases.json",
                "metadata/production-module-blockers.json",
                "docs/production-module-backlog.md",
                "metadata/production-module-backlog.json",
                "scripts/verify-production-module-backlog.py",
                "metadata/business-dependency-readiness-smoke.json",
                "docs/business-dependency-contracts.md",
                "metadata/business-dependency-contracts.json",
                "scripts/verify-business-dependency-contracts.py",
                "metadata/business-dependency-package-scan.json",
                "metadata/production-export-readiness-smoke.json",
                "docs/wom-toolbar-action-coverage.md",
                "metadata/wom-toolbar-action-coverage.json",
                "metadata/wom-toolbar-row-smoke.json",
                "scripts/verify-wom-toolbar-action-coverage.py",
            },
            failures,
        )
        if failed > 0 and g013.get("status") != "FAIL":
            fail(failures, "G-013 must be FAIL while the production matrix has failed cases")
        if failed == 0 and (blocked > 0 or not_run > 0) and g013.get("status") != "BLOCKED":
            fail(failures, "G-013 must remain BLOCKED while the production matrix has BLOCKED or NOT_RUN cases")
        if g013.get("status") == "READY" and (passed != total or blocked or not_run or failed):
            fail(failures, "G-013 READY requires every production test case to PASS")
        if g013.get("status") == "READY" and backlog_items > 0:
            fail(failures, "G-013 READY requires zero unresolved production module backlog items")
        if export_readiness:
            check_export_evidence_text("G-013", g013, export_readiness, failures)
        if row_smoke:
            check_wom_toolbar_evidence_text("G-013", g013, row_smoke, action_coverage, failures)

    if blockers:
        blocker_summary = blockers.get("summary")
        if not isinstance(blocker_summary, dict):
            fail(failures, "production module blocker summary must be an object")
        elif blocker_summary.get("blockedCases") != blocked:
            fail(
                failures,
                "production blocker report blockedCases must match production matrix blocked count "
                f"({blocker_summary.get('blockedCases')!r} != {blocked})",
            )

    g018 = items_by_id.get("G-018")
    if g018:
        check_item_has_artifacts(
            "G-018",
            items_by_id,
            {
                "metadata/production-module-test-cases.json",
                "metadata/production-module-blockers.json",
                "docs/production-module-backlog.md",
                "metadata/production-module-backlog.json",
                "scripts/verify-production-module-backlog.py",
                "metadata/business-dependency-readiness-smoke.json",
                "docs/business-dependency-contracts.md",
                "metadata/business-dependency-contracts.json",
                "scripts/verify-business-dependency-contracts.py",
                "metadata/production-export-readiness-smoke.json",
                "docs/wom-toolbar-action-coverage.md",
                "metadata/wom-toolbar-action-coverage.json",
                "metadata/wom-toolbar-row-smoke.json",
                "scripts/verify-wom-toolbar-action-coverage.py",
            },
            failures,
        )
        if (blocked > 0 or not_run > 0 or failed > 0) and g018.get("status") == "READY":
            fail(failures, "G-018 cannot be READY while production module cases are not all PASS")
        if backlog_items > 0 and g018.get("status") == "READY":
            fail(failures, "G-018 cannot be READY while production module backlog has unresolved items")
        if export_readiness:
            check_export_evidence_text("G-018", g018, export_readiness, failures)
        if row_smoke:
            check_wom_toolbar_evidence_text("G-018", g018, row_smoke, action_coverage, failures)

    if dependency_contracts:
        if dependency_contracts.get("database") != "PostgreSQL":
            fail(failures, "business dependency contracts database must remain PostgreSQL")
        if dependency_contracts.get("module") != "production":
            fail(failures, "business dependency contracts module must be production")
        if dependency_contracts.get("overallStatus") != "BLOCKED" and (blocked > 0 or backlog_items > 0):
            fail(failures, "business dependency contracts must remain BLOCKED while production cases/backlog are unresolved")
        contract_ids = {
            str(item.get("id"))
            for item in as_list(dependency_contracts.get("dependencies"))
            if isinstance(item, dict) and item.get("id")
        }
        missing_contracts = sorted({"material-service", "process-analysis"} - contract_ids)
        if missing_contracts:
            fail(failures, "business dependency contracts missing dependencies: " + ", ".join(missing_contracts))

    persistence_summary = persistence.get("summary") if persistence else {}
    if isinstance(persistence_summary, dict):
        persistence_failed = summary_int(persistence_summary, "fail")
        persistence_blocked = summary_int(persistence_summary, "blocked")
        if g018 and g018.get("status") == "READY" and (persistence_failed > 0 or persistence_blocked > 0):
            fail(failures, "G-018 READY requires persistence acceptance to have zero FAIL and zero BLOCKED")


def check_basic_config_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    g012 = items_by_id.get("G-012")
    coverage = read_json_file(BASIC_CONFIG_COVERAGE_PATH, failures, "basic config coverage report")
    action_matrix = read_json_file(BASIC_CONFIG_ACTION_MATRIX_PATH, failures, "basic config action matrix")
    if not g012:
        return

    check_item_has_artifacts(
        "G-012",
        items_by_id,
        {
            "docs/basic-config-coverage.md",
            "metadata/basic-config-coverage.json",
            "scripts/verify-basic-config-coverage.py",
            "docs/basic-config-action-matrix.md",
            "metadata/basic-config-action-matrix.json",
            "scripts/verify-basic-config-action-matrix.py",
            "deploy/docker/scripts/adp-systemcode-persistence-acceptance.js",
            "deploy/docker/scripts/adp-systemconfig-persistence-acceptance.js",
            "deploy/docker/scripts/adp-custom-property-persistence-acceptance.js",
            "metadata/custom-property-persistence-acceptance.json",
            "deploy/docker/postgres/init/169-custom-property-project-property-compat.sql",
            "deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js",
            "metadata/entity-model-config-crud-persistence-acceptance.json",
            "metadata/persistence-acceptance.json",
            "metadata/systemconfig-persistence-acceptance.json",
            "docs/backend-table-audit/persistence-acceptance.md",
        },
        failures,
    )
    entity_model = read_json_file(ENTITY_MODEL_PERSISTENCE_PATH, failures, "entity/model CRUD persistence acceptance report")
    if entity_model:
        if entity_model.get("status") != "PASS":
            fail(failures, "G-012 entity/model CRUD persistence acceptance must PASS")
        marker = str(entity_model.get("marker", ""))
        g012_evidence_text = "\n".join(str(item) for item in as_list(g012.get("currentEvidence")))
        if not marker or marker not in g012_evidence_text:
            fail(failures, "G-012 currentEvidence must include the entity/model CRUD marker")
        if "metadata/entity-model-config-crud-persistence-acceptance.json" not in g012_evidence_text:
            fail(failures, "G-012 currentEvidence must cite entity/model CRUD persistence report")
        stale_gap_text = "\n".join(
            str(item)
            for item in as_list(g012.get("blockingIssues")) + as_list(g012.get("nextActions"))
        )
        for stale_fragment in (
            "完整实体/模型新增/编辑/删除",
            "完整实体/模型配置页面执行 marker",
        ):
            if stale_fragment in stale_gap_text:
                fail(failures, f"G-012 must not keep stale entity/model CRUD gap after PASS evidence: {stale_fragment}")
    if action_matrix:
        if action_matrix.get("database") != "PostgreSQL":
            fail(failures, "G-012 action matrix database must remain PostgreSQL")
        if action_matrix.get("goalId") != "G-012":
            fail(failures, "G-012 action matrix goalId must be G-012")
        if action_matrix.get("overallStatus") != "PASS" and g012.get("status") == "READY":
            fail(failures, "G-012 READY requires basic config action matrix overallStatus=PASS")
        actions = as_list(action_matrix.get("actions"))
        action_ids = {
            str(action.get("id"))
            for action in actions
            if isinstance(action, dict) and action.get("id")
        }
        required_action_ids = {
            "systemcode-dictionary-crud",
            "systemconfig-app-catalog-crud",
            "builtin-identity-directory",
            "builtin-print-auth",
            "builtin-ak-sk",
            "builtin-password-policy",
            "builtin-qcs-runtime-config",
            "builtin-rm-runtime-config",
            "builtin-baseset-runtime-config",
            "custom-property-model-mapping-toggle",
            "entity-model-create",
            "entity-model-edit",
            "entity-model-delete",
            "entity-model-postgres-physical-table-autocreate",
            "nacos-production-export-diff",
            "keycloak-production-realm-migration",
        }
        missing_action_ids = sorted(required_action_ids - action_ids)
        if missing_action_ids:
            fail(failures, "G-012 action matrix missing actions: " + ", ".join(missing_action_ids))
        non_pass_action_ids = [
            str(action.get("id"))
            for action in actions
            if isinstance(action, dict) and action.get("status") != "PASS"
        ]
        if g012.get("status") == "READY" and non_pass_action_ids:
            fail(failures, "G-012 READY requires every basic config action to PASS: " + ", ".join(non_pass_action_ids))
    if not coverage:
        return

    areas = as_list(coverage.get("areas"))
    area_statuses = [
        area.get("status")
        for area in areas
        if isinstance(area, dict)
    ]
    incomplete = [
        str(area.get("id"))
        for area in areas
        if isinstance(area, dict) and area.get("status") not in {"PASS", "NOT_APPLICABLE"}
    ]
    if g012.get("status") == "READY" and coverage.get("overallStatus") != "PASS":
        fail(failures, "G-012 READY requires basic config coverage overallStatus=PASS")
    if g012.get("status") == "READY" and incomplete:
        fail(failures, "G-012 READY requires every basic config area to PASS or NOT_APPLICABLE: " + ", ".join(incomplete))
    if incomplete and g012.get("status") not in {"PARTIAL", "BLOCKED", "FAIL"}:
        fail(failures, "G-012 must remain non-READY while basic config areas are incomplete")

    required_area_ids = {
        "systemcode-dictionary",
        "systemconfig-app-catalog-value",
        "systemconfig-built-in-catalogs",
        "configuration-entity-runtime",
        "configuration-physical-model-table",
        "nacos-keycloak-production-config",
    }
    seen_area_ids = {
        str(area.get("id"))
        for area in areas
        if isinstance(area, dict) and area.get("id")
    }
    missing_area_ids = sorted(required_area_ids - seen_area_ids)
    if missing_area_ids:
        fail(failures, "G-012 basic config coverage missing areas: " + ", ".join(missing_area_ids))

    if "PASS" not in area_statuses:
        fail(failures, "G-012 basic config coverage must include at least one PASS area with real persistence evidence")


def check_migration_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    readiness = read_json_file(PRODUCTION_MIGRATION_READINESS_PATH, failures, "production migration readiness report")
    cutover = read_json_file(PRODUCTION_CUTOVER_GATE_PATH, failures, "production cutover gate report")
    rehearsal = read_json_file(PRODUCTION_REHEARSAL_PLAN_PATH, failures, "production rehearsal plan")
    backlog = read_json_file(PRODUCTION_BACKLOG_PATH, failures, "production module backlog report")
    g020 = items_by_id.get("G-020")
    if not g020:
        return

    check_item_has_artifacts(
        "G-020",
        items_by_id,
        {
            "metadata/production-migration-readiness.json",
            "metadata/production-cutover-gate.json",
            "metadata/production-rehearsal-plan.json",
            "docs/production-migration/rehearsal-plan.md",
            "scripts/generate-production-rehearsal-plan.py",
            "metadata/production-module-blockers.json",
            "metadata/production-module-backlog.json",
            "metadata/test-environment-smoke.json",
            "metadata/postgres-runtime-smoke.json",
            "metadata/minio-runtime-smoke.json",
            "scripts/verify-production-evidence-ready-gates.py",
        },
        failures,
    )
    g020_artifacts = artifact_set(g020)
    for track_id, required_artifacts in sorted(G020_REQUIRED_TRACK_ARTIFACTS.items()):
        missing = sorted(required_artifacts - g020_artifacts)
        if missing:
            fail(failures, f"G-020 must reference {track_id} production migration artifacts: " + ", ".join(missing))

    readiness_status = readiness.get("status") if readiness else None
    cutover_status = cutover.get("status") if cutover else None
    if g020.get("status") == "READY" and readiness_status != "READY_FOR_PRODUCTION_MIGRATION":
        fail(failures, "G-020 READY requires production migration readiness to be READY_FOR_PRODUCTION_MIGRATION")
    if g020.get("status") == "READY" and cutover_status != "READY_FOR_PRODUCTION_CUTOVER":
        fail(failures, "G-020 READY requires production cutover gate to be READY_FOR_PRODUCTION_CUTOVER")

    readiness_summary = readiness.get("summary") if readiness else {}
    cutover_summary = cutover.get("summary") if cutover else {}
    rehearsal_summary = rehearsal.get("summary") if rehearsal else {}
    backlog_summary = backlog.get("summary") if backlog else {}
    readiness_blocked = summary_int(readiness_summary, "blocked") if isinstance(readiness_summary, dict) else 0
    cutover_blocked = summary_int(cutover_summary, "blocked") if isinstance(cutover_summary, dict) else 0
    rehearsal_blocked = summary_int(rehearsal_summary, "blocked") if isinstance(rehearsal_summary, dict) else 0
    production_blockers = summary_int(cutover_summary, "productionBlockers") if isinstance(cutover_summary, dict) else 0
    production_backlog = summary_int(cutover_summary, "productionBacklogItems") if isinstance(cutover_summary, dict) else 0
    backlog_items = summary_int(backlog_summary, "totalItems") if isinstance(backlog_summary, dict) else 0
    if isinstance(cutover_summary, dict) and cutover_summary.get("productionBacklogItems") != backlog_items:
        fail(
            failures,
            "production cutover gate productionBacklogItems must match production backlog summary "
            f"({cutover_summary.get('productionBacklogItems')!r} != {backlog_items})",
        )
    if (
        readiness_blocked > 0
        or cutover_blocked > 0
        or rehearsal_blocked > 0
        or production_blockers > 0
        or production_backlog > 0
        or backlog_items > 0
    ) and g020.get("status") not in {"BLOCKED", "FAIL"}:
        fail(failures, "G-020 must remain BLOCKED/FAIL while migration, cutover, production blockers, or production backlog remain open")


def check_runtime_report_for_goal(
    item_id: str,
    items_by_id: dict[str, dict[str, Any]],
    report_path: Path,
    report_label: str,
    failures: list[str],
) -> dict[str, Any]:
    item = items_by_id.get(item_id)
    report = read_json_file(report_path, failures, report_label)
    if not item:
        return report

    relative_report_path = str(report_path.relative_to(ROOT))
    check_item_has_artifacts(item_id, items_by_id, {relative_report_path, "scripts/verify-runtime-smoke-reports.py"}, failures)
    status = item.get("status")
    if status in {"READY", "PARTIAL"} and summary_status(report) != "PASS":
        fail(failures, f"{item_id} {status} requires {relative_report_path} summary.status=PASS")
    return report


def check_runtime_patch_manifest_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    item = items_by_id.get("G-017")
    manifest = read_json_file(RUNTIME_PATCH_MANIFEST_PATH, failures, "runtime patch manifest")
    if not item:
        return

    check_item_has_artifacts(
        "G-017",
        items_by_id,
        {
            "metadata/runtime-patch-manifest.json",
            "docs/production-migration/runtime-patch-manifest.md",
            "deploy/docker/scripts/prepare-runtime-patches.sh",
        },
        failures,
    )
    if not manifest:
        return

    summary = manifest.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "runtime patch manifest summary must be an object")
        return

    by_category = summary.get("byCategory")
    if not isinstance(by_category, dict):
        fail(failures, "runtime patch manifest summary.byCategory must be an object")
        return

    required_categories = {
        "nacos-config-template",
        "postgres-init-sql",
        "runtime-binary-patch",
        "runtime-script",
    }
    missing_categories = sorted(category for category in required_categories if int(by_category.get(category) or 0) <= 0)
    if missing_categories:
        fail(failures, "runtime patch manifest missing non-empty categories: " + ", ".join(missing_categories))

    if int(summary.get("totalFiles") or 0) <= 0:
        fail(failures, "runtime patch manifest must include at least one file")
    production_use = manifest.get("productionUse")
    if not isinstance(production_use, dict):
        fail(failures, "runtime patch manifest must include productionUse guardrails")


def check_runtime_alignment(items_by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    nacos = check_runtime_report_for_goal(
        "G-014",
        items_by_id,
        NACOS_CONFIG_SMOKE_PATH,
        "Nacos config drift smoke report",
        failures,
    )
    keycloak = check_runtime_report_for_goal(
        "G-015",
        items_by_id,
        KEYCLOAK_JWT_SMOKE_PATH,
        "Keycloak/JWT runtime smoke report",
        failures,
    )
    postgres = check_runtime_report_for_goal(
        "G-016",
        items_by_id,
        POSTGRES_RUNTIME_SMOKE_PATH,
        "PostgreSQL runtime smoke report",
        failures,
    )

    g014 = items_by_id.get("G-014")
    nacos_summary = nacos.get("summary") if isinstance(nacos, dict) else {}
    if g014 and isinstance(nacos_summary, dict):
        if summary_int(nacos_summary, "criticalFail") != 0 or summary_int(nacos_summary, "oracleResidueFiles") != 0:
            fail(failures, "G-014 requires Nacos drift smoke to have zero criticalFail and oracleResidueFiles")
        if summary_int(nacos_summary, "healthyServices") != summary_int(nacos_summary, "expectedServices"):
            fail(failures, "G-014 requires Nacos drift smoke healthyServices to equal expectedServices")

    g015 = items_by_id.get("G-015")
    keycloak_summary = keycloak.get("summary") if isinstance(keycloak, dict) else {}
    if g015 and isinstance(keycloak_summary, dict):
        if keycloak_summary.get("keycloakPublicKeySha256Prefix") != keycloak_summary.get("nacosJwtSha256Prefix"):
            fail(failures, "G-015 requires Keycloak public key hash to match Nacos JWT hash")
        if summary_int(keycloak_summary, "nacosHealthyKeycloakHosts") <= 0:
            fail(failures, "G-015 requires at least one healthy Keycloak Nacos host")

    g016 = items_by_id.get("G-016")
    postgres_summary = postgres.get("summary") if isinstance(postgres, dict) else {}
    if g016 and isinstance(postgres_summary, dict):
        for expected_key, present_key in (
            ("expectedTables", "presentExpectedTables"),
            ("expectedColumns", "presentExpectedColumns"),
            ("expectedIndexes", "presentExpectedIndexes"),
        ):
            if postgres_summary.get(expected_key) != postgres_summary.get(present_key):
                fail(failures, f"G-016 requires PostgreSQL runtime {present_key} to match {expected_key}")

    g020 = items_by_id.get("G-020")
    if g020:
        for report_path, report_label in (
            (TEST_ENVIRONMENT_SMOKE_PATH, "test environment smoke report"),
            (POSTGRES_RUNTIME_SMOKE_PATH, "PostgreSQL runtime smoke report"),
            (NACOS_CONFIG_SMOKE_PATH, "Nacos config drift smoke report"),
            (KEYCLOAK_JWT_SMOKE_PATH, "Keycloak/JWT runtime smoke report"),
            (MINIO_RUNTIME_SMOKE_PATH, "MinIO runtime smoke report"),
        ):
            relative_report_path = str(report_path.relative_to(ROOT))
            check_item_has_artifacts("G-020", items_by_id, {relative_report_path, "scripts/verify-runtime-smoke-reports.py"}, failures)
            report = read_json_file(report_path, failures, report_label)
            if g020.get("status") in {"READY", "PARTIAL"} and summary_status(report) != "PASS":
                fail(failures, f"G-020 {g020.get('status')} requires {relative_report_path} summary.status=PASS")

    check_runtime_patch_manifest_alignment(items_by_id, failures)


def check_cross_ledger_alignment(data: dict[str, Any], failures: list[str]) -> None:
    items_by_id = {
        str(item.get("id")): item
        for item in as_list(data.get("items"))
        if isinstance(item, dict) and item.get("id")
    }
    check_oracle_alignment(items_by_id, failures)
    check_sustainable_alignment(items_by_id, failures)
    check_platform_alignment(items_by_id, failures)
    check_basic_config_alignment(items_by_id, failures)
    check_production_alignment(items_by_id, failures)
    check_postgres_gap_alignment(items_by_id, failures)
    check_runtime_alignment(items_by_id, failures)
    check_migration_alignment(items_by_id, failures)


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in (
        "schemaVersion",
        "generatedAt",
        "generatedFromCommit",
        "overallStatus",
        "databaseDefault",
        "summary",
        "items",
    ):
        if key not in data:
            fail(failures, f"report missing top-level key: {key}")

    if data.get("databaseDefault") != "PostgreSQL":
        fail(failures, "goal acceptance databaseDefault must remain PostgreSQL")

    if data.get("overallStatus") not in ALLOWED_OVERALL_STATUSES:
        fail(failures, f"invalid overallStatus: {data.get('overallStatus')!r}")

    commit = data.get("generatedFromCommit")
    if not isinstance(commit, str) or len(commit.strip()) < 7:
        fail(failures, "generatedFromCommit must identify the code baseline")

    items = data.get("items")
    if not isinstance(items, list):
        fail(failures, "items must be a list")
        return

    seen_ids: set[str] = set()
    statuses: list[str] = []
    for index, item in enumerate(items):
        status = check_item(index, item, failures)
        if not isinstance(item, dict):
            continue
        item_id = str(item.get("id", "")).strip()
        if item_id in seen_ids:
            fail(failures, f"duplicate item id: {item_id}")
        seen_ids.add(item_id)
        if status:
            statuses.append(status)

    missing = sorted(REQUIRED_ITEM_IDS - seen_ids)
    if missing:
        fail(failures, "goal acceptance report missing required items: " + ", ".join(missing))

    extra = sorted(seen_ids - REQUIRED_ITEM_IDS)
    if extra:
        fail(failures, "goal acceptance report contains unknown items: " + ", ".join(extra))

    check_summary(data, failures)

    if data.get("overallStatus") == "COMPLETE" and any(status != "READY" for status in statuses):
        fail(failures, "overall COMPLETE requires every goal item to be READY")

    if data.get("overallStatus") == "IN_PROGRESS_NOT_COMPLETE" and all(status == "READY" for status in statuses):
        fail(failures, "overall status should be COMPLETE when every goal item is READY")

    check_cross_ledger_alignment(data, failures)


def main() -> int:
    failures: list[str] = []
    check_doc(failures)
    data = read_json(failures)
    if data:
        check_report(data, failures)
    if failures:
        print(f"Project goal acceptance verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Project goal acceptance verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
