#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import subprocess
import sys
import xml.etree.ElementTree as ET
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
    "composeFile": ROOT / "deploy/docker/docker-compose.yml",
    "envExample": ROOT / "deploy/docker/.env.example",
    "oracleLegacyEnvExample": ROOT / "deploy/docker/.env.oracle-legacy.example",
}


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
    defaults = compose_defaults(content)
    promoted_source_modules = source_module_count()

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
    dependency_parse_ok = backend_deps["parseErrorCount"] == 0

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
            "oracle-legacy-only",
            "Oracle 只作为 legacy 路径保留",
            "watch" if oracle_audit["findingCount"] else "pass",
            f"Oracle migration backlog has {oracle_audit['findingCount']} tracked references.",
            "逐模块清理 backlog；删除引用前必须保留 PostgreSQL 替代证据。",
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
        "generatedFrom": [str(path.relative_to(ROOT)) for path in INPUTS.values()],
        "summary": {
            "blockingIssueCount": blocking_issue_count,
            "gapCount": gap_count,
            "watchCount": watch_count,
            "plannedCount": planned_count,
            "sourceModuleCount": promoted_source_modules,
            "oracleBacklogReferenceCount": oracle_audit["findingCount"],
            "directOracleDependencyCount": backend_deps["oracleDependencyCount"],
            "postgresMigrationCount": postgres_migrations["migrationCount"],
            "postgresMigrationHighRiskCount": postgres_migrations["highRiskStatementCount"],
            "postgresMapperAuditErrorCount": mapping_audit.get("errorCount"),
            "postgresMapperAuditWarningCount": mapping_audit.get("warningCount"),
        },
        "sourceCounts": source_counts,
        "composeDefaults": defaults,
        "postgresMapperAudit": mapping_audit,
        "oracleBacklogCategoryCounts": oracle_audit["categoryCounts"],
        "backendDirectOracleModules": backend_deps["oracleModules"],
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

    mapper = status["postgresMapperAudit"]
    mapper_top_rows = [["File", "Findings"]]
    for item in mapper.get("topFiles", [])[:10]:
        mapper_top_rows.append([item["file"], str(item["findingCount"])])

    return "\n".join(
        [
            "# Oracle 替换状态总账",
            "",
            "本文件由 `scripts/generate-oracle-replacement-status.py` 生成，用于把 Oracle 退场相关证据聚合到一个可审计状态页。",
            "",
            "## 摘要",
            "",
            f"- CI 阻断问题：`{summary['blockingIssueCount']}`。",
            f"- 迁移缺口：`{summary['gapCount']}`。",
            f"- 关注项：`{summary['watchCount']}`。",
            f"- 计划项：`{summary['plannedCount']}`。",
            f"- 已提升源码模块：`{summary['sourceModuleCount']}`。",
            f"- Oracle backlog 引用：`{summary['oracleBacklogReferenceCount']}`。",
            f"- 直接 Oracle 依赖：`{summary['directOracleDependencyCount']}`。",
            f"- PostgreSQL migration 脚本：`{summary['postgresMigrationCount']}`。",
            f"- PostgreSQL mapper audit：`{summary['postgresMapperAuditErrorCount']}` error / `{summary['postgresMapperAuditWarningCount']}` warning。",
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
            "## PostgreSQL Mapper Audit Top Files",
            "",
            table(mapper_top_rows) if len(mapper_top_rows) > 1 else "当前没有 mapper/sql 方言发现。",
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
    expected_json = json.dumps(status, ensure_ascii=False, indent=2) + "\n"
    expected_md = render_markdown(status)
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
