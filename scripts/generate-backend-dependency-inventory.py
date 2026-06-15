#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MODULE_ROOT = ROOT / "backend/modules"
OUTPUT_JSON = ROOT / "metadata/backend-module-dependency-inventory.json"
OUTPUT_MD = ROOT / "docs/backend-module-dependency-inventory.md"

POM_GLOB = "*/**/META-INF/maven/*/*/pom.xml"

LAYER_SUFFIXES = [
    ("resources", ("-resources", "-resource")),
    ("common", ("-common", "-base")),
    ("api", ("-api", "-openapi", "-open-api", "-client-api", "-project-api")),
    ("dao", ("-dao", "-repository")),
    ("manager", ("-manager",)),
    ("service", ("-service", "-server", "-services")),
    ("webapi", ("-webapi", "-web-api", "-controller")),
    ("bootstrap", ("-bootstrap", "-starter")),
    ("upgrade", ("-upgrade",)),
    ("parent", ("-parent",)),
]

PRIORITY_FAMILY_PATTERNS = [
    ("platform-auth", re.compile(r"^(auth|iam|rbac|organization)")),
    ("platform-config", re.compile(r"^(configuration|system-config|systemcode|module)")),
    ("workflow", re.compile(r"^(flow|workflow|cloud-task-scheduler|task-scheduler)")),
    ("platform-io", re.compile(r"^(file|printer|notification|auditlog|custom|license|i18n|portal|theme)")),
    ("quality", re.compile(r"(lims|qcs|qualify)", re.IGNORECASE)),
]

JDBC_ARTIFACTS = {
    "ojdbc6",
    "ojdbc7",
    "ojdbc8",
    "ojdbc10",
    "oracle-jdbc",
    "postgresql",
    "mysql-connector-java",
    "mssql-jdbc",
    "dm-jdbc",
    "kingbase8",
}


def strip_namespace(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def child(element: ET.Element, name: str) -> ET.Element | None:
    for item in list(element):
        if strip_namespace(item.tag) == name:
            return item
    return None


def children(element: ET.Element, name: str) -> list[ET.Element]:
    return [item for item in list(element) if strip_namespace(item.tag) == name]


def text(element: ET.Element | None) -> str | None:
    if element is None or element.text is None:
        return None
    value = element.text.strip()
    return value or None


def direct_text(element: ET.Element, name: str) -> str | None:
    return text(child(element, name))


def gav_key(group_id: str | None, artifact_id: str | None) -> str:
    return f"{group_id or '?'}:{artifact_id or '?'}"


def classify_layer(artifact_id: str) -> str:
    lowered = artifact_id.lower()
    for layer, suffixes in LAYER_SUFFIXES:
        if any(lowered.endswith(suffix) for suffix in suffixes):
            return layer
    return "other"


def family_name(artifact_id: str) -> str:
    lowered = artifact_id.lower()
    for _, suffixes in LAYER_SUFFIXES:
        for suffix in sorted(suffixes, key=len, reverse=True):
            if lowered.endswith(suffix):
                return artifact_id[: -len(suffix)]
    return artifact_id


def priority_bucket(family: str) -> str:
    normalized = family.lower()
    for bucket, pattern in PRIORITY_FAMILY_PATTERNS:
        if pattern.search(normalized):
            return bucket
    return "other"


def is_jdbc_dependency(group_id: str | None, artifact_id: str | None) -> bool:
    group = (group_id or "").lower()
    artifact = (artifact_id or "").lower()
    return artifact in JDBC_ARTIFACTS or "jdbc" in group or "jdbc" in artifact


def is_oracle_dependency(group_id: str | None, artifact_id: str | None) -> bool:
    group = (group_id or "").lower()
    artifact = (artifact_id or "").lower()
    return "oracle" in group or "oracle" in artifact or artifact.startswith("ojdbc")


def parse_dependency(dep: ET.Element, module_key: str, internal_keys: set[str]) -> dict[str, Any]:
    group_id = direct_text(dep, "groupId")
    artifact_id = direct_text(dep, "artifactId")
    version = direct_text(dep, "version")
    scope = direct_text(dep, "scope")
    dep_key = gav_key(group_id, artifact_id)
    return {
        "from": module_key,
        "groupId": group_id,
        "artifactId": artifact_id,
        "version": version,
        "scope": scope,
        "key": dep_key,
        "internal": dep_key in internal_keys,
        "jdbc": is_jdbc_dependency(group_id, artifact_id),
        "oracle": is_oracle_dependency(group_id, artifact_id),
    }


def oracle_dependency_action(dep: dict[str, Any], module: dict[str, Any]) -> dict[str, Any]:
    layer = module["layer"]
    if layer == "common":
        action = (
            "提升为 source module 时删除直接 Oracle JDBC；common/DTO 模块默认不应打开厂商连接，"
            "需要数据库连接时由 DAO/service 层通过父 POM 管理 PostgreSQL JDBC。"
        )
        verification = "提升模块后运行 `make source-module-check`、`make source-module-test`，并用流程/待办 smoke 覆盖调用链。"
    elif layer == "upgrade":
        action = (
            "把 Oracle driver 从默认 POM 移到显式 legacy profile 或独立迁移工具；"
            "默认 PostgreSQL 目标库写入使用标准 `java.sql` API，避免 `oracle.sql.*`。"
        )
        verification = "提升模块后运行 `make source-module-check`、`make source-module-test`，并用登录/currentuser 或升级任务 dry-run 验证。"
    else:
        action = (
            "删除默认直接 Oracle JDBC；只有明确需要回连旧库对比时，才放入 legacy profile 或默认构建外的工具模块。"
        )
        verification = "提升模块后运行 `make source-module-check`、`make source-module-test` 和相关页面/API smoke。"

    return {
        "from": dep["from"],
        "fromPath": module["path"],
        "fromFamily": module["family"],
        "fromLayer": module["layer"],
        "dependency": dep["key"],
        "version": dep["version"],
        "scope": dep["scope"],
        "replacement": "org.postgresql:postgresql only where the promoted module really opens JDBC connections",
        "migrationAction": action,
        "verification": verification,
    }


def parse_pom(path: Path) -> dict[str, Any]:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as exc:
        return {
            "path": str(path.relative_to(ROOT)),
            "parseError": str(exc),
        }

    parent = child(root, "parent")
    group_id = direct_text(root, "groupId") or direct_text(parent, "groupId")
    artifact_id = direct_text(root, "artifactId")
    version = direct_text(root, "version") or direct_text(parent, "version")
    packaging = direct_text(root, "packaging") or "jar"
    parent_info = None
    if parent is not None:
        parent_info = {
            "groupId": direct_text(parent, "groupId"),
            "artifactId": direct_text(parent, "artifactId"),
            "version": direct_text(parent, "version"),
            "relativePath": direct_text(parent, "relativePath"),
        }
    dependencies = child(root, "dependencies")
    dependency_elements = children(dependencies, "dependency") if dependencies is not None else []
    artifact = artifact_id or path.parent.name
    family = family_name(artifact)
    return {
        "path": str(path.relative_to(ROOT)),
        "parseError": None,
        "groupId": group_id,
        "artifactId": artifact_id,
        "version": version,
        "packaging": packaging,
        "key": gav_key(group_id, artifact_id),
        "parent": parent_info,
        "layer": classify_layer(artifact),
        "family": family,
        "priorityBucket": priority_bucket(family),
        "dependencyElements": dependency_elements,
    }


def build_inventory() -> dict[str, Any]:
    pom_paths = sorted(MODULE_ROOT.glob(POM_GLOB))
    parsed = [parse_pom(path) for path in pom_paths]
    modules = [item for item in parsed if not item["parseError"]]
    parse_errors = [item for item in parsed if item["parseError"]]
    internal_keys = {item["key"] for item in modules}

    dependencies: list[dict[str, Any]] = []
    oracle_dependencies: list[dict[str, Any]] = []
    for item in modules:
        for dep in item.pop("dependencyElements"):
            dependency = parse_dependency(dep, item["key"], internal_keys)
            dependencies.append(dependency)
            if dependency["oracle"]:
                oracle_dependencies.append(oracle_dependency_action(dependency, item))

    for item in modules:
        own_deps = [dep for dep in dependencies if dep["from"] == item["key"]]
        item["dependencyCount"] = len(own_deps)
        item["internalDependencyCount"] = sum(1 for dep in own_deps if dep["internal"])
        item["externalDependencyCount"] = sum(1 for dep in own_deps if not dep["internal"])
        item["oracleDependencyCount"] = sum(1 for dep in own_deps if dep["oracle"])
        item["jdbcDependencyCount"] = sum(1 for dep in own_deps if dep["jdbc"])

    key_counts = Counter(item["key"] for item in modules)
    duplicate_modules = [
        {
            "key": key,
            "count": count,
            "versions": sorted({item["version"] or "" for item in modules if item["key"] == key}),
            "paths": sorted(item["path"] for item in modules if item["key"] == key),
        }
        for key, count in sorted(key_counts.items())
        if count > 1
    ]

    incoming = Counter(dep["key"] for dep in dependencies if dep["internal"])
    external = Counter(dep["key"] for dep in dependencies if not dep["internal"])
    layer_counts = Counter(item["layer"] for item in modules)
    family_counts = Counter(item["family"] for item in modules)
    priority_counts = Counter(item["priorityBucket"] for item in modules)
    oracle_modules = [
        {
            "key": item["key"],
            "path": item["path"],
            "oracleDependencyCount": item["oracleDependencyCount"],
        }
        for item in modules
        if item["oracleDependencyCount"]
    ]
    jdbc_dependencies = [
        {
            "from": dep["from"],
            "key": dep["key"],
            "version": dep["version"],
            "scope": dep["scope"],
            "oracle": dep["oracle"],
        }
        for dep in dependencies
        if dep["jdbc"]
    ]
    return {
        "schemaVersion": 1,
        "sourceRoot": str(MODULE_ROOT.relative_to(ROOT)),
        "pomCount": len(pom_paths),
        "moduleCount": len(modules),
        "parseErrorCount": len(parse_errors),
        "dependencyCount": len(dependencies),
        "internalDependencyCount": sum(1 for dep in dependencies if dep["internal"]),
        "externalDependencyCount": sum(1 for dep in dependencies if not dep["internal"]),
        "oracleDependencyCount": sum(1 for dep in dependencies if dep["oracle"]),
        "jdbcDependencyCount": sum(1 for dep in dependencies if dep["jdbc"]),
        "layerCounts": dict(sorted(layer_counts.items())),
        "priorityBucketCounts": dict(sorted(priority_counts.items())),
        "topFamilies": [
            {"family": name, "count": count}
            for name, count in family_counts.most_common(40)
        ],
        "topInternalDependencies": [
            {"key": key, "incomingCount": count}
            for key, count in incoming.most_common(40)
        ],
        "topExternalDependencies": [
            {"key": key, "count": count}
            for key, count in external.most_common(60)
        ],
        "duplicateModules": duplicate_modules,
        "parseErrors": parse_errors,
        "oracleModules": sorted(oracle_modules, key=lambda item: (item["key"], item["path"])),
        "oracleDependencies": sorted(
            oracle_dependencies,
            key=lambda item: (item["from"], item["dependency"], item["fromPath"]),
        ),
        "jdbcDependencies": sorted(jdbc_dependencies, key=lambda item: (item["from"], item["key"])),
        "modules": sorted(modules, key=lambda item: (item["family"], item["layer"], item["key"], item["path"])),
        "dependencies": sorted(dependencies, key=lambda item: (item["from"], item["internal"], item["key"])),
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


def render_top(rows: list[dict[str, Any]], key_field: str, count_field: str, limit: int) -> str:
    table_rows = [["Name", "Count"]]
    for item in rows[:limit]:
        table_rows.append([str(item[key_field]), str(item[count_field])])
    return table(table_rows)


def render_markdown(inventory: dict[str, Any]) -> str:
    layer_rows = [["Layer", "Modules"]]
    for name, count in inventory["layerCounts"].items():
        layer_rows.append([str(name), str(count)])

    priority_rows = [["Bucket", "Modules"]]
    for name, count in inventory["priorityBucketCounts"].items():
        priority_rows.append([str(name), str(count)])

    oracle_rows = [["Module", "Oracle deps", "Path"]]
    for item in inventory["oracleModules"]:
        oracle_rows.append([item["key"], str(item["oracleDependencyCount"]), item["path"]])

    oracle_dependency_rows = [["Module", "Dependency", "Replacement", "Migration Action", "Verification"]]
    for item in inventory["oracleDependencies"]:
        oracle_dependency_rows.append(
            [
                item["from"],
                item["dependency"],
                item["replacement"],
                item["migrationAction"],
                item["verification"],
            ]
        )

    duplicate_rows = [["Module", "Count", "Versions"]]
    for item in inventory["duplicateModules"][:30]:
        duplicate_rows.append([item["key"], str(item["count"]), ", ".join(item["versions"])])

    module_rows = [["Module", "Family", "Layer", "Deps", "Internal", "External", "Oracle", "Path"]]
    for item in inventory["modules"]:
        module_rows.append(
            [
                item["key"],
                item["family"],
                item["layer"],
                str(item["dependencyCount"]),
                str(item["internalDependencyCount"]),
                str(item["externalDependencyCount"]),
                str(item["oracleDependencyCount"]),
                item["path"],
            ]
        )

    lines = [
        "# 后端恢复模块依赖库存",
        "",
        "本文件由 `scripts/generate-backend-dependency-inventory.py` 生成，用于把恢复源码中的 Maven POM 变成可审计的模块依赖清单。",
        "",
        "## 摘要",
        "",
        f"- 源目录：`{inventory['sourceRoot']}`。",
        f"- POM 数量：`{inventory['pomCount']}`。",
        f"- 可解析模块：`{inventory['moduleCount']}`。",
        f"- 解析失败：`{inventory['parseErrorCount']}`。",
        f"- 依赖边：`{inventory['dependencyCount']}`。",
        f"- 内部依赖边：`{inventory['internalDependencyCount']}`。",
        f"- 外部依赖边：`{inventory['externalDependencyCount']}`。",
        f"- JDBC 依赖：`{inventory['jdbcDependencyCount']}`。",
        f"- Oracle 依赖：`{inventory['oracleDependencyCount']}`。",
        f"- 重复模块坐标：`{len(inventory['duplicateModules'])}`。",
        "- 机器可读清单：`metadata/backend-module-dependency-inventory.json`。",
        "",
        "## 模块层级统计",
        "",
        table(layer_rows),
        "",
        "## 提升优先域统计",
        "",
        table(priority_rows),
        "",
        "## Top 内部依赖",
        "",
        render_top(inventory["topInternalDependencies"], "key", "incomingCount", 20),
        "",
        "## Top 外部依赖",
        "",
        render_top(inventory["topExternalDependencies"], "key", "count", 30),
        "",
        "## Oracle/JDBC 风险",
        "",
        table(oracle_rows) if len(oracle_rows) > 1 else "当前恢复 POM 没有直接 Oracle JDBC 依赖。",
        "",
        "## 直接 Oracle 依赖退场动作",
        "",
        table(oracle_dependency_rows) if len(oracle_dependency_rows) > 1 else "当前没有直接 Oracle JDBC 退场动作。",
        "",
        "## 重复模块坐标",
        "",
        table(duplicate_rows) if len(duplicate_rows) > 1 else "当前没有重复模块坐标。",
        "",
        "## 模块清单",
        "",
        table(module_rows),
        "",
        "## 使用规则",
        "",
        "- `backend/modules` 是恢复源码参考区，不直接纳入 Maven reactor。",
        "- 提升模块前先查看本清单中的 family、layer、内部依赖和 Oracle/JDBC 风险。",
        "- 新模块复制到 `backend/source-modules/<module>` 后，使用根父 POM 重新声明最小依赖。",
        "- Oracle JDBC 只能保留在 `oracle-legacy` profile、默认构建外的迁移工具或迁移说明中，不能进入默认 PostgreSQL 路径。",
        "- 修改恢复 POM、提升模块或新增来源包后，运行 `make backend-dependency-inventory`。",
        "",
    ]
    return "\n".join(lines)


def write_outputs(inventory: dict[str, Any]) -> None:
    OUTPUT_JSON.write_text(json.dumps(inventory, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    OUTPUT_MD.write_text(render_markdown(inventory), encoding="utf-8")


def invariant_failures(inventory: dict[str, Any]) -> list[str]:
    failures: list[str] = []
    if inventory["parseErrorCount"]:
        failures.append(f"unparseable recovered module poms: {inventory['parseErrorCount']}")
    return failures


def check_outputs(inventory: dict[str, Any]) -> int:
    failures = invariant_failures(inventory)
    expected_json = json.dumps(inventory, ensure_ascii=False, indent=2) + "\n"
    expected_md = render_markdown(inventory)
    if not OUTPUT_JSON.exists() or OUTPUT_JSON.read_text(encoding="utf-8") != expected_json:
        failures.append(str(OUTPUT_JSON.relative_to(ROOT)) + " is stale")
    if not OUTPUT_MD.exists() or OUTPUT_MD.read_text(encoding="utf-8") != expected_md:
        failures.append(str(OUTPUT_MD.relative_to(ROOT)) + " is stale")
    if failures:
        for failure in failures:
            print("FAIL:", failure, file=sys.stderr)
        print("Run: make backend-dependency-inventory", file=sys.stderr)
        return 1
    print("Backend dependency inventory is current.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate backend module dependency inventory.")
    parser.add_argument("--check", action="store_true", help="Check generated outputs without writing.")
    args = parser.parse_args()

    inventory = build_inventory()
    if args.check:
        return check_outputs(inventory)
    failures = invariant_failures(inventory)
    if failures:
        for failure in failures:
            print("WARN:", failure, file=sys.stderr)
    write_outputs(inventory)
    print(f"Wrote {OUTPUT_JSON.relative_to(ROOT)}")
    print(f"Wrote {OUTPUT_MD.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
