#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_MODULES_DIR = ROOT / "backend/source-modules"
AGGREGATOR_POM = SOURCE_MODULES_DIR / "pom.xml"
MAVEN_NS = "http://maven.apache.org/POM/4.0.0"
ET.register_namespace("", MAVEN_NS)


def q(name: str) -> str:
    return f"{{{MAVEN_NS}}}{name}"


def normalize_module_name(value: str) -> str:
    name = value.strip()
    if not re.fullmatch(r"[a-z][a-z0-9-]*", name):
        raise SystemExit("Module name must match [a-z][a-z0-9-]*, for example platform-auth.")
    return name


def default_package(module_name: str) -> str:
    return "com.mapletct.ftmes." + module_name.replace("-", ".")


def package_to_path(package: str) -> Path:
    if not re.fullmatch(r"[a-zA-Z_][\w]*(\.[a-zA-Z_][\w]*)*", package):
        raise SystemExit(f"Invalid Java package: {package}")
    return Path(*package.split("."))


def indent(element: ET.Element, level: int = 0) -> None:
    child_indent = "\n" + "    " * (level + 1)
    own_indent = "\n" + "    " * level
    if len(element):
        if not element.text or not element.text.strip():
            element.text = child_indent
        for child in element:
            indent(child, level + 1)
        if not element[-1].tail or not element[-1].tail.strip():
            element[-1].tail = own_indent
    if level and (not element.tail or not element.tail.strip()):
        element.tail = own_indent


def read_modules() -> tuple[ET.ElementTree, ET.Element, ET.Element]:
    tree = ET.parse(AGGREGATOR_POM)
    root = tree.getroot()
    modules = root.find(q("modules"))
    if modules is None:
        modules = ET.SubElement(root, q("modules"))
    return tree, root, modules


def write_aggregator(module_name: str, dry_run: bool) -> bool:
    tree, root, modules = read_modules()
    existing = [node.text for node in modules.findall(q("module")) if node.text]
    if module_name in existing:
        return False
    new_node = ET.Element(q("module"))
    new_node.text = module_name
    module_nodes = modules.findall(q("module"))
    insert_at = len(modules)
    for index, node in enumerate(module_nodes):
        if (node.text or "") > module_name:
            insert_at = list(modules).index(node)
            break
    modules.insert(insert_at, new_node)
    indent(root)
    if not dry_run:
        tree.write(AGGREGATOR_POM, encoding="UTF-8", xml_declaration=True)
    return True


def module_pom(module_name: str, package: str) -> str:
    title = module_name.replace("-", " ").title()
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.mapletct.ftmes</groupId>
        <artifactId>ft-mes-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../../../pom.xml</relativePath>
    </parent>

    <artifactId>{module_name}</artifactId>
    <packaging>jar</packaging>

    <name>FT MES {title}</name>
    <description>Buildable backend source module promoted from recovered ADP/MES code.</description>

    <properties>
        <module.basePackage>{package}</module.basePackage>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
"""


def write_module(module_name: str, package: str, dry_run: bool) -> None:
    module_dir = SOURCE_MODULES_DIR / module_name
    if module_dir.exists():
        raise SystemExit(f"Module already exists: {module_dir.relative_to(ROOT)}")
    java_dir = module_dir / "src/main/java" / package_to_path(package)
    resource_dir = module_dir / "src/main/resources"
    test_dir = module_dir / "src/test/java" / package_to_path(package)
    paths = [
        module_dir / "pom.xml",
        java_dir / ".gitkeep",
        resource_dir / ".gitkeep",
        test_dir / ".gitkeep",
    ]
    if dry_run:
        for path in paths:
            print(f"would create {path.relative_to(ROOT)}")
        print(f"would update {AGGREGATOR_POM.relative_to(ROOT)}")
        return
    java_dir.mkdir(parents=True, exist_ok=True)
    resource_dir.mkdir(parents=True, exist_ok=True)
    test_dir.mkdir(parents=True, exist_ok=True)
    (module_dir / "pom.xml").write_text(module_pom(module_name, package), encoding="utf-8")
    for keep in (java_dir / ".gitkeep", resource_dir / ".gitkeep", test_dir / ".gitkeep"):
        keep.write_text("", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Create a buildable backend source module.")
    parser.add_argument("module", help="Module directory and artifactId, e.g. platform-auth.")
    parser.add_argument("--package", help="Base Java package. Defaults to com.mapletct.ftmes.<module>.")
    parser.add_argument("--dry-run", action="store_true", help="Print intended changes without writing files.")
    args = parser.parse_args()

    module_name = normalize_module_name(args.module)
    package = args.package or default_package(module_name)
    package_to_path(package)

    write_module(module_name, package, args.dry_run)
    changed = write_aggregator(module_name, args.dry_run)
    if args.dry_run:
        return 0
    print(f"Created backend/source-modules/{module_name}")
    print("Updated backend/source-modules/pom.xml" if changed else "Aggregator already contained module.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
