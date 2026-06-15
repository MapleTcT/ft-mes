#!/usr/bin/env python3
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_MODULES_DIR = ROOT / "backend/source-modules"
AGGREGATOR_POM = SOURCE_MODULES_DIR / "pom.xml"
MAVEN_NS = "http://maven.apache.org/POM/4.0.0"


def q(name: str) -> str:
    return f"{{{MAVEN_NS}}}{name}"


def text(parent: ET.Element | None, name: str) -> str:
    if parent is None:
        return ""
    child = parent.find(q(name))
    return (child.text or "").strip() if child is not None else ""


def parse(path: Path) -> ET.Element:
    try:
        return ET.parse(path).getroot()
    except ET.ParseError as exc:
        raise SystemExit(f"Invalid XML in {path.relative_to(ROOT)}: {exc}") from exc


def aggregator_modules() -> list[str]:
    root = parse(AGGREGATOR_POM)
    modules = root.find(q("modules"))
    if modules is None:
        return []
    return [text(node, ".") or (node.text or "").strip() for node in modules.findall(q("module"))]


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def check_module_pom(module_name: str, failures: list[str]) -> None:
    module_dir = SOURCE_MODULES_DIR / module_name
    pom = module_dir / "pom.xml"
    if not module_dir.is_dir():
        fail(failures, f"aggregator references missing module directory: {module_name}")
        return
    if not pom.exists():
        fail(failures, f"module missing pom.xml: {module_name}")
        return

    root = parse(pom)
    parent = root.find(q("parent"))
    if text(parent, "groupId") != "com.mapletct.ftmes":
        fail(failures, f"{module_name} parent groupId must be com.mapletct.ftmes")
    if text(parent, "artifactId") != "ft-mes-parent":
        fail(failures, f"{module_name} must inherit ft-mes-parent")
    if text(parent, "relativePath") != "../../../pom.xml":
        fail(failures, f"{module_name} parent relativePath must be ../../../pom.xml")
    if text(root, "artifactId") != module_name:
        fail(failures, f"{module_name} artifactId must match its directory name")

    contents = pom.read_text(encoding="utf-8").lower()
    if "ojdbc" in contents or "com.oracle.jdbc" in contents or "com.oracle.database.jdbc" in contents:
        fail(failures, f"{module_name} must not declare a direct Oracle JDBC dependency")
    if "db_type:oracle" in contents or "db-type:oracle" in contents or "db-type>oracle" in contents:
        fail(failures, f"{module_name} must not default database type to Oracle")


def check_unlisted_modules(modules: list[str], failures: list[str]) -> None:
    listed = set(modules)
    for child in SOURCE_MODULES_DIR.iterdir():
        if not child.is_dir():
            continue
        if child.name.startswith(".") or child.name.startswith("_"):
            continue
        if (child / "pom.xml").exists() and child.name not in listed:
            fail(failures, f"module directory has pom.xml but is not listed in aggregator: {child.name}")


def main() -> int:
    failures: list[str] = []
    modules = aggregator_modules()
    duplicates = sorted({name for name in modules if modules.count(name) > 1})
    for name in duplicates:
        fail(failures, f"duplicate module in backend/source-modules/pom.xml: {name}")
    if modules != sorted(modules):
        fail(failures, "backend/source-modules/pom.xml modules must be sorted alphabetically")
    for module_name in modules:
        if not module_name:
            fail(failures, "empty module entry in backend/source-modules/pom.xml")
            continue
        check_module_pom(module_name, failures)
    check_unlisted_modules(modules, failures)
    if failures:
        print(f"Source module verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print(f"Source module verification passed. Modules: {len(modules)}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
