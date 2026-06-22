#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_JSON = ROOT / "metadata/oracle-replacement-status.json"
OUTPUT_MD = ROOT / "docs/oracle-replacement-status.md"

INPUTS = {
    "contentInventory": ROOT / "metadata/current-content-inventory.json",
    "backendDependencyInventory": ROOT / "metadata/backend-module-dependency-inventory.json",
    "oracleMigrationAudit": ROOT / "metadata/oracle-migration-audit.json",
    "postgresMigrationInventory": ROOT / "metadata/postgres-migration-inventory.json",
    "sourceModulePom": ROOT / "backend/source-modules/pom.xml",
    "sourceModuleVerifier": ROOT / "scripts/verify-source-modules.py",
    "composeFile": ROOT / "deploy/docker/docker-compose.yml",
    "envExample": ROOT / "deploy/docker/.env.example",
    "oracleLegacyEnvExample": ROOT / "deploy/docker/.env.oracle-legacy.example",
    "nacosConfigDir": ROOT / "deploy/nacos-config",
    "nacosRenderedDir": ROOT / "deploy/docker/nacos-rendered",
}

RUNTIME_ORACLE_PATTERN = re.compile(
    r"\b(?:oracle|ojdbc|jdbc:oracle|orcl)\b|SUPOS_SYSTEM_DB_PORT:1521|[:=]1521\b",
    re.IGNORECASE,
)


def git_head() -> str:
    result = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    )
    return result.stdout.strip()


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def strip_namespace(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def child(element: ET.Element, name: str) -> ET.Element | None:
    for item in list(element):
        if strip_namespace(item.tag) == name:
            return item
    return None


def source_module_count() -> int:
    root = ET.parse(INPUTS["sourceModulePom"]).getroot()
    modules = child(root, "modules")
    if modules is None:
        return 0
    return sum(1 for item in list(modules) if strip_namespace(item.tag) == "module")


def direct_children(element: ET.Element | None, name: str) -> list[ET.Element]:
    if element is None:
        return []
    return [item for item in list(element) if strip_namespace(item.tag) == name]


def direct_text(element: ET.Element | None, name: str) -> str:
    if element is None:
        return ""
    item = child(element, name)
    return (item.text or "").strip() if item is not None else ""


def dependency_keys(parent: ET.Element | None) -> list[str]:
    dependencies = child(parent, "dependencies") if parent is not None else None
    keys = []
    for dependency in direct_children(dependencies, "dependency"):
        keys.append(f"{direct_text(dependency, 'groupId')}:{direct_text(dependency, 'artifactId')}".lower())
    return keys


def is_oracle_dependency(key: str) -> bool:
    return "oracle" in key or ":ojdbc" in key


def parent_pom_oracle_policy() -> dict[str, Any]:
    root = ET.parse(ROOT / "pom.xml").getroot()
    default_dependency_management = child(root, "dependencyManagement")
    default_oracle_dependencies = [
        key for key in dependency_keys(default_dependency_management) if is_oracle_dependency(key)
    ]

    oracle_legacy = None
    profiles = child(root, "profiles")
    for profile in direct_children(profiles, "profile"):
        if direct_text(profile, "id") == "oracle-legacy":
            oracle_legacy = profile
            break

    legacy_dependency_management = child(oracle_legacy, "dependencyManagement") if oracle_legacy is not None else None
    legacy_oracle_dependencies = [
        key for key in dependency_keys(legacy_dependency_management) if is_oracle_dependency(key)
    ]
    return {
        "defaultOracleDependencyManagementCount": len(default_oracle_dependencies),
        "defaultOracleDependencies": default_oracle_dependencies,
        "oracleLegacyProfilePresent": oracle_legacy is not None,
        "oracleLegacyDependencyManagementCount": len(legacy_oracle_dependencies),
        "oracleLegacyDependencies": legacy_oracle_dependencies,
    }


def run_postgres_mapping_audit() -> tuple[dict[str, Any], int]:
    result = subprocess.run(
        [
            sys.executable,
            "deploy/docker/scripts/audit-postgres-mappings.py",
            "backend/modules",
            "deploy/docker/postgres/init",
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    try:
        summary = json.loads(result.stdout)
    except json.JSONDecodeError:
        summary = {
            "findingCount": None,
            "errorCount": None,
            "warningCount": None,
            "stdout": result.stdout,
            "stderr": result.stderr,
        }
    return summary, result.returncode


def compact_command_output(stdout: str, stderr: str) -> str:
    text = " ".join((stdout + "\n" + stderr).split())
    return text[:300] if text else "no output"


def run_source_module_oracle_policy() -> tuple[dict[str, Any], int]:
    result = subprocess.run(
        [sys.executable, "scripts/verify-source-modules.py"],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return {
        "returnCode": result.returncode,
        "output": compact_command_output(result.stdout, result.stderr),
    }, result.returncode


def compose_defaults(content: dict[str, Any]) -> dict[str, Any]:
    compose = INPUTS["composeFile"].read_text(encoding="utf-8")
    env_example = INPUTS["envExample"].read_text(encoding="utf-8")
    legacy_env = INPUTS["oracleLegacyEnvExample"].read_text(encoding="utf-8")
    return {
        "contentInventoryDefault": content["deployment"].get("databaseDefault"),
        "composeDefaultsPostgres": "${SUPOS_SYSTEM_DB_TYPE:-postgresql}" in compose,
        "composeImplicitOracleDefault": "${SUPOS_SYSTEM_DB_TYPE:-oracle}" in compose,
        "envExamplePostgres": "SUPOS_SYSTEM_DB_TYPE=postgresql" in env_example,
        "envExamplePostgresHost": "SUPOS_SYSTEM_DB_HOST=postgres" in env_example,
        "oracleLegacyTemplatePresent": "SUPOS_SYSTEM_DB_TYPE=oracle" in legacy_env,
    }


def active_config_line(raw_line: str) -> str:
    line = raw_line.strip()
    if not line or line.startswith("#") or line.startswith("!"):
        return ""
    return line


def runtime_oracle_defaults() -> dict[str, Any]:
    roots = [INPUTS["nacosConfigDir"], INPUTS["nacosRenderedDir"]]
    findings: list[dict[str, Any]] = []
    scanned_files = 0
    scanned_lines = 0
    for root in roots:
        if not root.exists():
            continue
        for path in sorted(root.glob("*.properties")):
            scanned_files += 1
            area = "rendered" if "nacos-rendered" in path.parts else "source-template"
            for line_number, raw_line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), start=1):
                line = active_config_line(raw_line)
                if not line:
                    continue
                scanned_lines += 1
                if RUNTIME_ORACLE_PATTERN.search(line):
                    findings.append(
                        {
                            "area": area,
                            "path": str(path.relative_to(ROOT)),
                            "line": line_number,
                            "text": line[:300],
                        }
                    )
    source_findings = [item for item in findings if item["area"] == "source-template"]
    rendered_findings = [item for item in findings if item["area"] == "rendered"]
    return {
        "scannedFiles": scanned_files,
        "scannedActiveLines": scanned_lines,
        "activeOracleLineCount": len(findings),
        "sourceTemplateActiveOracleLineCount": len(source_findings),
        "renderedActiveOracleLineCount": len(rendered_findings),
        "findings": findings,
    }


def status_item(
    item_id: str,
    title: str,
    status: str,
    evidence: str,
    next_action: str,
    blocking: bool = False,
) -> dict[str, Any]:
    return {
        "id": item_id,
        "title": title,
        "status": status,
        "evidence": evidence,
        "nextAction": next_action,
        "blocking": blocking,
    }


def build_status() -> dict[str, Any]:
    content = load_json(INPUTS["contentInventory"])
    backend_deps = load_json(INPUTS["backendDependencyInventory"])
    oracle_audit = load_json(INPUTS["oracleMigrationAudit"])
    postgres_migrations = load_json(INPUTS["postgresMigrationInventory"])
    mapping_audit, mapping_returncode = run_postgres_mapping_audit()
    source_module_policy, source_module_policy_returncode = run_source_module_oracle_policy()
    defaults = compose_defaults(content)
    runtime_oracle_scan = runtime_oracle_defaults()
    promoted_source_modules = source_module_count()
    parent_oracle_policy = parent_pom_oracle_policy()
    current_head = git_head()
    oracle_audit_category_counts = oracle_audit.get("categoryCounts", {})
    oracle_audit_finding_count = int(oracle_audit.get("findingCount") or 0)
    oracle_audit_unclassified_count = int(
        oracle_audit_category_counts.get("unclassified-oracle-reference") or 0
    )
    oracle_audit_category_total = sum(int(value) for value in oracle_audit_category_counts.values())
    oracle_audit_current_and_classified = (
        bool(oracle_audit.get("generatedAt"))
        and oracle_audit.get("repoCommit") == current_head
        and oracle_audit_unclassified_count == 0
        and oracle_audit_category_total == oracle_audit_finding_count
    )

    source_counts = {
        "backendSourceJars": content["backend"]["sourceJarCount"],
        "backendJavaFiles": content["backend"]["javaFileCount"],
        "backendXmlFiles": content["backend"]["xmlFileCount"],
        "decompiledServices": content["backend"]["decompiledServiceCount"],
        "frontendSourceMaps": content["frontend"]["sourceMapCount"],
        "frontendRecoveredFiles": content["frontend"]["recoveredFileCount"],
        "composeServices": content["deployment"]["composeServiceCount"],
        "businessRuntimeServices": content["deployment"]["composeServiceKindCounts"].get("business-runtime", 0),
    }

    postgres_default_ok = (
        defaults["contentInventoryDefault"] == "postgresql"
        and defaults["composeDefaultsPostgres"]
        and defaults["envExamplePostgres"]
        and defaults["envExamplePostgresHost"]
        and not defaults["composeImplicitOracleDefault"]
    )
    postgres_migration_ok = (
        postgres_migrations["highRiskStatementCount"] == 0
        and not postgres_migrations["missingNumbers"]
        and not postgres_migrations["duplicateNumbers"]
        and not postgres_migrations["invalidNames"]
    )
    mapping_errors = mapping_audit.get("errorCount")
    mapper_audit_ok = mapping_returncode == 0 and mapping_errors == 0
    source_module_oracle_policy_ok = source_module_policy_returncode == 0
    dependency_parse_ok = backend_deps["parseErrorCount"] == 0
    parent_pom_oracle_policy_ok = (
        parent_oracle_policy["defaultOracleDependencyManagementCount"] == 0
        and parent_oracle_policy["oracleLegacyProfilePresent"]
        and parent_oracle_policy["oracleLegacyDependencyManagementCount"] > 0
    )
    runtime_config_defaults_ok = runtime_oracle_scan["activeOracleLineCount"] == 0

    checks = [
        status_item(
            "runtime-default-postgresql",
            "Docker 测试环境默认 PostgreSQL",
            "pass" if postgres_default_ok else "fail",
            (
                f"content inventory default={defaults['contentInventoryDefault']}, "
                f"compose postgres default={defaults['composeDefaultsPostgres']}, "
                f"env example postgres={defaults['envExamplePostgres']}"
            ),
            "保持 `.env.example` 和 Compose 默认值指向 PostgreSQL。",
            blocking=not postgres_default_ok,
        ),
        status_item(
            "parent-pom-oracle-legacy-profile",
            "父 POM 默认依赖管理不提供 Oracle JDBC",
            "pass" if parent_pom_oracle_policy_ok else "fail",
            (
                f"defaultOracleDeps={parent_oracle_policy['defaultOracleDependencyManagementCount']}, "
                f"legacyProfile={parent_oracle_policy['oracleLegacyProfilePresent']}, "
                f"legacyOracleDeps={parent_oracle_policy['oracleLegacyDependencyManagementCount']}"
            ),
            "Oracle JDBC 只能放在 `oracle-legacy` profile；默认父 POM 只管理 PostgreSQL/JDK 基线。",
            blocking=not parent_pom_oracle_policy_ok,
        ),
        status_item(
            "runtime-config-no-oracle-defaults",
            "Nacos 默认运行配置不含 active Oracle-like fallback",
            "pass" if runtime_config_defaults_ok else "fail",
            (
                f"activeOracle={runtime_oracle_scan['activeOracleLineCount']}, "
                f"source={runtime_oracle_scan['sourceTemplateActiveOracleLineCount']}, "
                f"rendered={runtime_oracle_scan['renderedActiveOracleLineCount']}, "
                f"files={runtime_oracle_scan['scannedFiles']}"
            ),
            "Nacos source templates and rendered configs must default to PostgreSQL; Oracle-like defaults can only remain in comments, backlog, or explicit legacy templates.",
            blocking=not runtime_config_defaults_ok,
        ),
        status_item(
            "oracle-legacy-only",
            "Oracle 只作为 legacy 路径保留",
            "watch" if oracle_audit["findingCount"] else "pass",
            f"Oracle migration backlog has {oracle_audit['findingCount']} tracked references.",
            "逐模块清理 backlog；删除引用前必须保留 PostgreSQL 替代证据。",
        ),
        status_item(
            "oracle-audit-current-and-classified",
            "Oracle migration audit 可追溯且无未分类引用",
            "pass" if oracle_audit_current_and_classified else "fail",
            (
                f"generatedAt={oracle_audit.get('generatedAt')}, "
                f"repoCommit={oracle_audit.get('repoCommit')}, "
                f"unclassified={oracle_audit_unclassified_count}, "
                f"findingCount={oracle_audit_finding_count}, "
                f"categoryTotal={oracle_audit_category_total}"
            ),
            "先运行 `make oracle-audit`；新增 Oracle 引用必须分类到 backlog、legacy、tooling 或文档路径。",
            blocking=not oracle_audit_current_and_classified,
        ),
        status_item(
            "backend-direct-oracle-deps",
            "恢复 POM 直接 Oracle/JDBC 依赖已入账",
            "gap" if backend_deps["oracleDependencyCount"] else "pass",
            (
                f"{backend_deps['moduleCount']} recovered modules, "
                f"{backend_deps['oracleDependencyCount']} direct Oracle dependencies, "
                f"{backend_deps['jdbcDependencyCount']} JDBC dependencies."
            ),
            "模块提升时优先处理直接 Oracle JDBC 依赖，默认路径只保留 PostgreSQL。",
        ),
        status_item(
            "mapper-postgres-audit",
            "默认路径 Mapper/SQL 无阻断方言",
            "pass" if mapper_audit_ok else "fail",
            (
                f"errors={mapping_audit.get('errorCount')}, "
                f"warnings={mapping_audit.get('warningCount')}, "
                f"findings={mapping_audit.get('findingCount')}"
            ),
            "任何 error 级方言必须先迁移；warning 级 `to_char` 保留人工确认记录。",
            blocking=not mapper_audit_ok,
        ),
        status_item(
            "postgres-migration-governance",
            "PostgreSQL 初始化脚本可审计",
            "pass" if postgres_migration_ok else "fail",
            (
                f"{postgres_migrations['migrationCount']} scripts, "
                f"range={postgres_migrations['firstNumber']:03d}-{postgres_migrations['lastNumber']:03d}, "
                f"highRisk={postgres_migrations['highRiskStatementCount']}, "
                f"watch={postgres_migrations['watchStatementCount']}"
            ),
            "新增 SQL 只能追加编号并保持幂等；watch 语句在 PR 中解释。",
            blocking=not postgres_migration_ok,
        ),
        status_item(
            "recovered-source-inventory",
            "当前恢复内容有机器库存",
            "pass",
            (
                f"{source_counts['backendSourceJars']} source jars, "
                f"{source_counts['frontendRecoveredFiles']} frontend files, "
                f"{source_counts['composeServices']} compose services."
            ),
            "新增包、服务或 source map 后运行 `make inventory`。",
        ),
        status_item(
            "source-module-promotion",
            "恢复源码已开始提升为可编译模块",
            "gap" if promoted_source_modules == 0 else "watch",
            f"`backend/source-modules` currently declares {promoted_source_modules} buildable modules.",
            "按 auth/rbac/organization/configuration/workflow 顺序提升高频维护模块。",
        ),
        status_item(
            "source-module-oracle-policy",
            "已提升源码模块不含 Oracle 默认依赖",
            "pass" if source_module_oracle_policy_ok else "fail",
            source_module_policy["output"],
            "修复 `backend/source-modules` 中的 Oracle JDBC、Oracle 默认配置、Oracle dialect 或 mapper/oracle 资源后重新运行 `make source-module-check`。",
            blocking=not source_module_oracle_policy_ok,
        ),
        status_item(
            "backend-table-audit",
            "后端落表业务排查已拆为专门工作流",
            "planned",
            "`docs/backend-table-audit-handoff.md` and issue template exist; detailed table maps remain future work.",
            "专门线程输出页面/API/服务/Mapper/表/字段映射，避免混进平台工程化任务。",
        ),
    ]

    blocking_issue_count = sum(1 for item in checks if item["blocking"])
    gap_count = sum(1 for item in checks if item["status"] == "gap")
    watch_count = sum(1 for item in checks if item["status"] == "watch")
    planned_count = sum(1 for item in checks if item["status"] == "planned")
    return {
        "schemaVersion": 1,
        "generatedAt": now_iso(),
        "repoCommit": current_head,
        "generatedFrom": [str(path.relative_to(ROOT)) for path in INPUTS.values()],
        "summary": {
            "blockingIssueCount": blocking_issue_count,
            "gapCount": gap_count,
            "watchCount": watch_count,
            "plannedCount": planned_count,
            "sourceModuleCount": promoted_source_modules,
            "sourceModuleOraclePolicyPass": source_module_oracle_policy_ok,
            "oracleBacklogReferenceCount": oracle_audit["findingCount"],
            "directOracleDependencyCount": backend_deps["oracleDependencyCount"],
            "postgresMigrationCount": postgres_migrations["migrationCount"],
            "postgresMigrationHighRiskCount": postgres_migrations["highRiskStatementCount"],
            "postgresMapperAuditErrorCount": mapping_audit.get("errorCount"),
            "postgresMapperAuditWarningCount": mapping_audit.get("warningCount"),
            "runtimeConfigActiveOracleLineCount": runtime_oracle_scan["activeOracleLineCount"],
            "oracleAuditUnclassifiedCount": oracle_audit_unclassified_count,
            "oracleAuditRepoCommitMatchesHead": oracle_audit.get("repoCommit") == current_head,
        },
        "sourceCounts": source_counts,
        "composeDefaults": defaults,
        "runtimeConfigOracleScan": runtime_oracle_scan,
        "parentPomOraclePolicy": parent_oracle_policy,
        "sourceModuleOraclePolicy": source_module_policy,
        "postgresMapperAudit": mapping_audit,
        "oracleMigrationAudit": {
            "generatedAt": oracle_audit.get("generatedAt"),
            "repoCommit": oracle_audit.get("repoCommit"),
            "findingCount": oracle_audit_finding_count,
            "categoryTotal": oracle_audit_category_total,
            "unclassifiedReferenceCount": oracle_audit_unclassified_count,
        },
        "oracleBacklogCategoryCounts": oracle_audit["categoryCounts"],
        "backendDirectOracleModules": backend_deps["oracleModules"],
        "backendDirectOracleDependencies": backend_deps.get("oracleDependencies", []),
        "postgresMigrationWatchCount": postgres_migrations["watchStatementCount"],
        "checks": checks,
    }


def table(rows: list[list[str]]) -> str:
    if not rows:
        return ""
    output = [
        "| " + " | ".join(rows[0]) + " |",
        "| " + " | ".join(["---"] * len(rows[0])) + " |",
    ]
    output.extend("| " + " | ".join(row) + " |" for row in rows[1:])
    return "\n".join(output)


def render_markdown(status: dict[str, Any]) -> str:
    summary = status["summary"]
    check_rows = [["ID", "Status", "Blocking", "Evidence", "Next Action"]]
    for item in status["checks"]:
        check_rows.append(
            [
                item["id"],
                item["status"],
                "yes" if item["blocking"] else "no",
                item["evidence"],
                item["nextAction"],
            ]
        )

    source_rows = [["Area", "Count"]]
    for key, value in status["sourceCounts"].items():
        source_rows.append([key, str(value)])

    category_rows = [["Category", "References"]]
    for key, value in sorted(status["oracleBacklogCategoryCounts"].items()):
        category_rows.append([key, str(value)])

    oracle_rows = [["Module", "Oracle deps", "Path"]]
    for item in status["backendDirectOracleModules"]:
        oracle_rows.append([item["key"], str(item["oracleDependencyCount"]), item["path"]])

    oracle_dependency_rows = [["Module", "Dependency", "Action", "Verification"]]
    for item in status["backendDirectOracleDependencies"]:
        oracle_dependency_rows.append(
            [
                item["from"],
                item["dependency"],
                item["migrationAction"],
                item["verification"],
            ]
        )

    mapper = status["postgresMapperAudit"]
    mapper_top_rows = [["File", "Findings"]]
    for item in mapper.get("topFiles", [])[:10]:
        mapper_top_rows.append([item["file"], str(item["findingCount"])])

    runtime_oracle_scan = status["runtimeConfigOracleScan"]
    runtime_oracle_rows = [["Area", "Path", "Line", "Text"]]
    for item in runtime_oracle_scan.get("findings", [])[:20]:
        runtime_oracle_rows.append(
            [
                item["area"],
                item["path"],
                str(item["line"]),
                item["text"].replace("|", "\\|"),
            ]
        )

    return "\n".join(
        [
            "# Oracle 替换状态总账",
            "",
            "本文件由 `scripts/generate-oracle-replacement-status.py` 生成，用于把 Oracle 退场相关证据聚合到一个可审计状态页。",
            "",
            "## 摘要",
            "",
            f"- Generated At：`{status.get('generatedAt')}`。",
            f"- Repo Commit：`{status.get('repoCommit')}`。",
            f"- CI 阻断问题：`{summary['blockingIssueCount']}`。",
            f"- 迁移缺口：`{summary['gapCount']}`。",
            f"- 关注项：`{summary['watchCount']}`。",
            f"- 计划项：`{summary['plannedCount']}`。",
            f"- 已提升源码模块：`{summary['sourceModuleCount']}`。",
            f"- 源码模块 Oracle 禁入：`{'pass' if summary['sourceModuleOraclePolicyPass'] else 'fail'}`。",
            f"- Oracle backlog 引用：`{summary['oracleBacklogReferenceCount']}`。",
            f"- 直接 Oracle 依赖：`{summary['directOracleDependencyCount']}`。",
            f"- PostgreSQL migration 脚本：`{summary['postgresMigrationCount']}`。",
            f"- PostgreSQL mapper audit：`{summary['postgresMapperAuditErrorCount']}` error / `{summary['postgresMapperAuditWarningCount']}` warning。",
            f"- 运行配置 active Oracle-like 默认行：`{summary['runtimeConfigActiveOracleLineCount']}`。",
            f"- Oracle audit 未分类引用：`{summary['oracleAuditUnclassifiedCount']}`。",
            f"- Oracle audit commit matches HEAD：`{summary['oracleAuditRepoCommitMatchesHead']}`。",
            "- 机器可读清单：`metadata/oracle-replacement-status.json`。",
            "",
            "## 状态矩阵",
            "",
            table(check_rows),
            "",
            "## 恢复资产计数",
            "",
            table(source_rows),
            "",
            "## Oracle Backlog 分类",
            "",
            table(category_rows),
            "",
            "## 直接 Oracle 依赖模块",
            "",
            table(oracle_rows) if len(oracle_rows) > 1 else "当前没有恢复 POM 直接声明 Oracle JDBC。",
            "",
            "## 直接 Oracle 依赖退场动作",
            "",
            table(oracle_dependency_rows) if len(oracle_dependency_rows) > 1 else "当前没有直接 Oracle JDBC 退场动作。",
            "",
            "## PostgreSQL Mapper Audit Top Files",
            "",
            table(mapper_top_rows) if len(mapper_top_rows) > 1 else "当前没有 mapper/sql 方言发现。",
            "",
            "## 运行配置 Oracle 默认扫描",
            "",
            (
                table(runtime_oracle_rows)
                if len(runtime_oracle_rows) > 1
                else f"当前 Nacos source/rendered 配置 active 行没有 Oracle-like fallback；扫描 `{runtime_oracle_scan['scannedFiles']}` 个文件、`{runtime_oracle_scan['scannedActiveLines']}` 行 active 配置。"
            ),
            "",
            "## 使用规则",
            "",
            "- 这个总账不是替代各专项报告，而是把专项报告的当前结论串起来。",
            "- `blocking=yes` 的项目会导致 `make oracle-replacement-check` 失败或应阻断合并。",
            "- `gap` 表示长期目标尚未完成，但不一定阻断当前仓库治理提交。",
            "- Oracle 退场前必须同时满足依赖、配置、SQL、migration、smoke 证据。",
            "",
        ]
    )


def write_outputs(status: dict[str, Any]) -> None:
    OUTPUT_JSON.write_text(json.dumps(status, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    OUTPUT_MD.write_text(render_markdown(status), encoding="utf-8")


def check_outputs(status: dict[str, Any]) -> int:
    failures: list[str] = []
    existing_json: dict[str, Any] = {}
    if OUTPUT_JSON.exists():
        try:
            loaded = json.loads(OUTPUT_JSON.read_text(encoding="utf-8"))
            if isinstance(loaded, dict):
                existing_json = loaded
        except json.JSONDecodeError as error:
            failures.append(f"{OUTPUT_JSON.relative_to(ROOT)} is invalid JSON: {error}")
    stable_status = json.loads(json.dumps(status))
    stable_status["generatedAt"] = existing_json.get("generatedAt")
    expected_json = json.dumps(stable_status, ensure_ascii=False, indent=2) + "\n"
    expected_md = render_markdown(stable_status)
    if status["summary"]["blockingIssueCount"]:
        failures.append(f"blocking Oracle replacement issues: {status['summary']['blockingIssueCount']}")
    if not OUTPUT_JSON.exists() or OUTPUT_JSON.read_text(encoding="utf-8") != expected_json:
        failures.append(str(OUTPUT_JSON.relative_to(ROOT)) + " is stale")
    if not OUTPUT_MD.exists() or OUTPUT_MD.read_text(encoding="utf-8") != expected_md:
        failures.append(str(OUTPUT_MD.relative_to(ROOT)) + " is stale")
    if failures:
        for failure in failures:
            print("FAIL:", failure, file=sys.stderr)
        print("Run: make oracle-replacement-status", file=sys.stderr)
        return 1
    print("Oracle replacement status is current.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate Oracle replacement status report.")
    parser.add_argument("--check", action="store_true", help="Check generated outputs without writing.")
    args = parser.parse_args()

    status = build_status()
    if args.check:
        return check_outputs(status)
    write_outputs(status)
    print(f"Wrote {OUTPUT_JSON.relative_to(ROOT)}")
    print(f"Wrote {OUTPUT_MD.relative_to(ROOT)}")
    if status["summary"]["blockingIssueCount"]:
        print(f"WARN: blocking issues: {status['summary']['blockingIssueCount']}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
