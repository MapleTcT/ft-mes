#!/usr/bin/env python3
from __future__ import annotations

import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

REQUIRED_PATHS = [
    "pom.xml",
    "package.json",
    "package-lock.json",
    "backend/pom.xml",
    "backend/source-modules/pom.xml",
    "deploy/pom.xml",
    "deploy/docker/docker-compose.yml",
    "deploy/docker/.env.example",
    "deploy/docker/.env.oracle-legacy.example",
    "docs/project-objectives.md",
    "docs/sustainable-development.md",
    "docs/oracle-to-postgres-transition.md",
    "docs/runtime-validation-scope.md",
    "docs/backend-table-audit-handoff.md",
    "docs/current-content-inventory.md",
    "docs/backend-module-dependency-inventory.md",
    "docs/oracle-migration-backlog.md",
    "docs/oracle-replacement-status.md",
    "docs/postgres-migration-index.md",
    "metadata/current-content-inventory.json",
    "metadata/backend-module-dependency-inventory.json",
    "metadata/oracle-migration-audit.json",
    "metadata/oracle-replacement-status.json",
    "metadata/postgres-migration-inventory.json",
    ".github/workflows/verify.yml",
    "scripts/create-backend-source-module.py",
    "scripts/precheck-module-intake.py",
    "scripts/verify-source-modules.py",
    "scripts/generate-backend-dependency-inventory.py",
    "scripts/generate-oracle-replacement-status.py",
    "scripts/generate-postgres-migration-inventory.py",
    "deploy/docker/scripts/adp-platform-validation-smoke.js",
    "deploy/docker/scripts/patch-eam-reactapi-ready.py",
]

ALLOWED_BINARY_FILES = {
    "deploy/docker/patches/kafka-jaas-noop/kafka-jaas-noop.jar",
    "deploy/docker/patches/notification-dynamic-templates/notification-dynamic-templates.jar",
    "deploy/docker/patches/scdog-test-bypass/lib/libSCDog.so",
}

BINARY_SUFFIXES = {
    ".jar",
    ".war",
    ".ear",
    ".exe",
    ".dll",
    ".so",
    ".dylib",
    ".zip",
    ".7z",
    ".rar",
}


MAVEN_NS = "http://maven.apache.org/POM/4.0.0"


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def q(name: str) -> str:
    return f"{{{MAVEN_NS}}}{name}"


def direct_child(element: ET.Element, name: str) -> ET.Element | None:
    return element.find(q(name))


def direct_children(element: ET.Element, name: str) -> list[ET.Element]:
    return list(element.findall(q(name)))


def text(element: ET.Element | None, name: str) -> str:
    if element is None:
        return ""
    child = direct_child(element, name)
    return (child.text or "").strip() if child is not None else ""


def dependency_keys(parent: ET.Element | None) -> list[str]:
    if parent is None:
        return []
    dependencies = direct_child(parent, "dependencies")
    if dependencies is None:
        return []
    keys = []
    for dependency in direct_children(dependencies, "dependency"):
        keys.append(f"{text(dependency, 'groupId')}:{text(dependency, 'artifactId')}".lower())
    return keys


def is_oracle_dependency(key: str) -> bool:
    return "oracle" in key or ":ojdbc" in key


def git_ls_files() -> list[str]:
    result = subprocess.run(
        ["git", "ls-files"],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    )
    return [line for line in result.stdout.splitlines() if line]


def check_required_paths(failures: list[str]) -> None:
    for relative in REQUIRED_PATHS:
        if not (ROOT / relative).exists():
            fail(f"required path missing: {relative}", failures)


def check_maven_structure(failures: list[str]) -> None:
    pom_path = ROOT / "pom.xml"
    pom = pom_path.read_text(encoding="utf-8")
    for module in ("<module>backend</module>", "<module>deploy</module>"):
        if module not in pom:
            fail(f"root pom missing module declaration: {module}", failures)
    source_modules = (ROOT / "backend/source-modules/pom.xml").read_text(encoding="utf-8")
    if "<packaging>pom</packaging>" not in source_modules:
        fail("backend/source-modules/pom.xml must remain an aggregator pom", failures)

    root = ET.parse(pom_path).getroot()
    default_dependency_management = direct_child(root, "dependencyManagement")
    default_oracle_dependencies = [
        key for key in dependency_keys(default_dependency_management) if is_oracle_dependency(key)
    ]
    if default_oracle_dependencies:
        fail(
            "root default dependencyManagement must not manage Oracle JDBC: "
            + ", ".join(default_oracle_dependencies),
            failures,
        )

    profiles = direct_child(root, "profiles")
    oracle_legacy = None
    if profiles is not None:
        for profile in direct_children(profiles, "profile"):
            if text(profile, "id") == "oracle-legacy":
                oracle_legacy = profile
                break
    if oracle_legacy is None:
        fail("root pom must keep an explicit oracle-legacy profile for old-database comparisons", failures)
        return
    legacy_dependency_management = direct_child(oracle_legacy, "dependencyManagement")
    legacy_oracle_dependencies = [
        key for key in dependency_keys(legacy_dependency_management) if is_oracle_dependency(key)
    ]
    if not legacy_oracle_dependencies:
        fail("oracle-legacy profile must be the only place that manages Oracle JDBC", failures)


def check_postgres_defaults(failures: list[str]) -> None:
    compose = (ROOT / "deploy/docker/docker-compose.yml").read_text(encoding="utf-8")
    env_example = (ROOT / "deploy/docker/.env.example").read_text(encoding="utf-8")
    if "${SUPOS_SYSTEM_DB_TYPE:-postgresql}" not in compose:
        fail("docker-compose.yml must default SUPOS_SYSTEM_DB_TYPE to postgresql", failures)
    if "${SUPOS_SYSTEM_DB_TYPE:-oracle}" in compose:
        fail("docker-compose.yml still has an implicit Oracle default", failures)
    if "SUPOS_SYSTEM_DB_TYPE=postgresql" not in env_example:
        fail(".env.example must default SUPOS_SYSTEM_DB_TYPE=postgresql", failures)
    if "SUPOS_SYSTEM_DB_HOST=postgres" not in env_example:
        fail(".env.example must point the system database host to postgres", failures)


def check_binary_policy(failures: list[str]) -> None:
    for relative in git_ls_files():
        path = Path(relative)
        if path.suffix.lower() not in BINARY_SUFFIXES:
            continue
        if relative in ALLOWED_BINARY_FILES:
            continue
        fail(f"tracked runtime binary is not allowed: {relative}", failures)


def check_large_files(failures: list[str]) -> None:
    for relative in git_ls_files():
        path = ROOT / relative
        if path.is_file() and path.stat().st_size > 50 * 1024 * 1024:
            fail(f"tracked file exceeds 50 MiB: {relative}", failures)


def main() -> int:
    failures: list[str] = []
    check_required_paths(failures)
    check_maven_structure(failures)
    check_postgres_defaults(failures)
    check_binary_policy(failures)
    check_large_files(failures)
    if failures:
        print(f"Sustainable repo verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Sustainable repo verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
