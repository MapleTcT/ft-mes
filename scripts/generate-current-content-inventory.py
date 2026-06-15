#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
METADATA_DIR = ROOT / "metadata"
OUTPUT_JSON = METADATA_DIR / "current-content-inventory.json"
OUTPUT_MD = ROOT / "docs/current-content-inventory.md"

INFRA_SERVICES = {
    "postgres",
    "redis",
    "mongo",
    "zookeeper",
    "kafka",
    "nacos",
    "nacos-config",
    "keycloak",
    "minio",
    "nginx",
    "test-license-seed",
}


def read_json(relative: str) -> Any:
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def docker_services() -> list[dict[str, str]]:
    compose = (ROOT / "deploy/docker/docker-compose.yml").read_text(encoding="utf-8")
    services: list[dict[str, str]] = []
    in_services = False
    current: dict[str, str] | None = None
    for line in compose.splitlines():
        if line == "services:":
            in_services = True
            continue
        if in_services and line and not line.startswith(" "):
            break
        if not in_services:
            continue
        match = re.match(r"^  ([A-Za-z0-9_-]+):\s*$", line)
        if match:
            current = {"service": match.group(1), "jarPath": "", "kind": ""}
            services.append(current)
            continue
        if current and "/opt/adp/bap-server/" in line and ".jar" in line:
            jar_match = re.search(r"/opt/adp/bap-server/([^\"'\s,]+?\.jar)", line)
            if jar_match:
                current["jarPath"] = "bap-server/" + jar_match.group(1)
    for item in services:
        item["kind"] = classify_service(item["service"], item["jarPath"])
    return services


def classify_service(name: str, jar_path: str) -> str:
    if name in INFRA_SERVICES:
        return "infrastructure"
    if "/module-Server/" in jar_path:
        return "business-runtime"
    if "/base-Server/" in jar_path:
        return "platform-runtime"
    return "support-runtime"


def source_module_groups(modules: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped: dict[str, dict[str, Any]] = {}
    for module in modules:
        group = module["group"]
        item = grouped.setdefault(
            group,
            {
                "group": group,
                "moduleCount": 0,
                "javaFiles": 0,
                "xmlFiles": 0,
                "artifacts": [],
            },
        )
        item["moduleCount"] += 1
        item["javaFiles"] += module.get("files", {}).get("java", 0)
        item["xmlFiles"] += module.get("files", {}).get("xml", 0)
        item["artifacts"].append(f"{module['artifact']}:{module['version']}")
    return sorted(grouped.values(), key=lambda x: (-x["moduleCount"], x["group"]))


def build_inventory() -> dict[str, Any]:
    backend_source = read_json("metadata/backend-source-summary.json")
    backend_services = read_json("metadata/backend-service-manifest.json")
    backend_decompile = read_json("metadata/backend-decompile-summary.json")
    frontend_summary = read_json("metadata/frontend-sourcemap-summary.json")
    frontend_files = read_json("metadata/frontend-recovered-files.json")
    compose_services = docker_services()

    apps = frontend_summary.get("apps", {})
    app_file_counts = Counter(item["app"] for item in frontend_files)
    compose_kind_counts = Counter(item["kind"] for item in compose_services)

    return {
        "schemaVersion": 1,
        "generatedFrom": [
            "metadata/backend-source-summary.json",
            "metadata/backend-service-manifest.json",
            "metadata/backend-decompile-summary.json",
            "metadata/frontend-sourcemap-summary.json",
            "metadata/frontend-recovered-files.json",
            "deploy/docker/docker-compose.yml",
        ],
        "backend": {
            "sourceJarCount": backend_source["extractedSourceJars"],
            "javaFileCount": backend_source["totalJavaFiles"],
            "xmlFileCount": backend_source["totalXmlFiles"],
            "sourceGroups": source_module_groups(backend_source["modules"]),
            "platformServiceCount": backend_services["serviceCount"],
            "platformServices": [
                {
                    "service": item["service"],
                    "startClass": item.get("startClass", ""),
                    "springBootVersion": item.get("springBootVersion", ""),
                    "jar": item.get("jar", ""),
                }
                for item in sorted(backend_services["services"], key=lambda x: x["service"])
            ],
            "decompiledServiceCount": backend_decompile["serviceCount"],
            "decompiledJavaFileCount": backend_decompile["totalJavaFiles"],
        },
        "frontend": {
            "sourceMapCount": frontend_summary["totalMapFiles"],
            "recoveredFileCount": frontend_summary["recoveredFiles"],
            "failedSourceMapCount": frontend_summary["failedMapFiles"],
            "apps": [
                {
                    "app": app,
                    "recoveredFileCount": app_file_counts.get(app, 0),
                    "sourceMapCount": data.get("mapFiles", 0) if isinstance(data, dict) else 0,
                }
                for app, data in sorted(apps.items())
            ],
        },
        "deployment": {
            "composeServiceCount": len(compose_services),
            "composeServiceKindCounts": dict(sorted(compose_kind_counts.items())),
            "composeServices": compose_services,
            "databaseDefault": "postgresql",
            "oracleMode": "legacy-template-only",
        },
    }


def render_table(rows: list[list[str]]) -> str:
    if not rows:
        return ""
    header = rows[0]
    separator = ["---"] * len(header)
    lines = [
        "| " + " | ".join(header) + " |",
        "| " + " | ".join(separator) + " |",
    ]
    lines.extend("| " + " | ".join(row) + " |" for row in rows[1:])
    return "\n".join(lines)


def render_markdown(inventory: dict[str, Any]) -> str:
    backend = inventory["backend"]
    frontend = inventory["frontend"]
    deployment = inventory["deployment"]

    source_group_rows = [["Group", "Modules", "Java", "XML"]]
    for group in backend["sourceGroups"][:30]:
        source_group_rows.append(
            [
                group["group"],
                str(group["moduleCount"]),
                str(group["javaFiles"]),
                str(group["xmlFiles"]),
            ]
        )

    platform_rows = [["Service", "Spring Boot", "Start Class"]]
    for service in backend["platformServices"]:
        platform_rows.append(
            [
                service["service"],
                service["springBootVersion"],
                service["startClass"],
            ]
        )

    frontend_rows = [["App", "Recovered Files", "Source Maps"]]
    for app in frontend["apps"]:
        frontend_rows.append([app["app"], str(app["recoveredFileCount"]), str(app["sourceMapCount"])])

    compose_rows = [["Service", "Kind", "Jar Path"]]
    for service in deployment["composeServices"]:
        compose_rows.append([service["service"], service["kind"], service["jarPath"] or "-"])

    return "\n".join(
        [
            "# 当前内容迁移清单",
            "",
            "本文件由 `scripts/generate-current-content-inventory.py` 生成，用于说明当前恢复内容已经迁移到可持续开发仓库中的哪些位置。",
            "",
            "## 总览",
            "",
            f"- 后端 sources.jar：`{backend['sourceJarCount']}` 个。",
            f"- 后端 Java 源码：`{backend['javaFileCount']}` 个文件。",
            f"- 后端 XML：`{backend['xmlFileCount']}` 个文件。",
            f"- 反编译服务 Java：`{backend['decompiledJavaFileCount']}` 个文件。",
            f"- 前端 source map：`{frontend['sourceMapCount']}` 个。",
            f"- 前端恢复源码：`{frontend['recoveredFileCount']}` 个文件。",
            f"- Docker Compose 服务：`{deployment['composeServiceCount']}` 个。",
            f"- 默认数据库：`{deployment['databaseDefault']}`。",
            f"- Oracle 模式：`{deployment['oracleMode']}`。",
            "",
            "## 后端源码分组",
            "",
            "下表按 Maven group 汇总恢复出来的 sources.jar。完整机器可读清单见 `metadata/current-content-inventory.json`。",
            "",
            render_table(source_group_rows),
            "",
            "## 平台运行服务",
            "",
            render_table(platform_rows),
            "",
            "## 前端应用",
            "",
            render_table(frontend_rows),
            "",
            "## Docker 编排服务",
            "",
            render_table(compose_rows),
            "",
            "## 使用方式",
            "",
            "- 新业务包进来后，先更新 runtime/部署编排，再运行 `make inventory` 刷新本清单。",
            "- 如果清单变化涉及后端表结构，继续补 `docs/backend-table-audit/` 下的落表报告。",
            "- 如果清单变化引入 Oracle 配置，必须进入 `oracle-legacy` 路径并补迁移 issue。",
            "",
        ]
    )


def write_outputs(inventory: dict[str, Any]) -> None:
    OUTPUT_JSON.write_text(json.dumps(inventory, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    OUTPUT_MD.write_text(render_markdown(inventory), encoding="utf-8")


def check_outputs(inventory: dict[str, Any]) -> int:
    expected_json = json.dumps(inventory, ensure_ascii=False, indent=2) + "\n"
    expected_md = render_markdown(inventory)
    failures = []
    if not OUTPUT_JSON.exists() or OUTPUT_JSON.read_text(encoding="utf-8") != expected_json:
        failures.append(str(OUTPUT_JSON.relative_to(ROOT)))
    if not OUTPUT_MD.exists() or OUTPUT_MD.read_text(encoding="utf-8") != expected_md:
        failures.append(str(OUTPUT_MD.relative_to(ROOT)))
    if failures:
        print("Inventory outputs are stale:", ", ".join(failures), file=sys.stderr)
        print("Run: make inventory", file=sys.stderr)
        return 1
    print("Inventory outputs are current.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate ADP/MES current content inventory.")
    parser.add_argument("--check", action="store_true", help="Check generated outputs without writing.")
    args = parser.parse_args()

    inventory = build_inventory()
    if args.check:
        return check_outputs(inventory)
    write_outputs(inventory)
    print(f"Wrote {OUTPUT_JSON.relative_to(ROOT)}")
    print(f"Wrote {OUTPUT_MD.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
