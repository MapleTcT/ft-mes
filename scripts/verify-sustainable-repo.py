#!/usr/bin/env python3
from __future__ import annotations

import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

REQUIRED_PATHS = [
    "pom.xml",
    "backend/pom.xml",
    "backend/source-modules/pom.xml",
    "deploy/pom.xml",
    "deploy/docker/docker-compose.yml",
    "deploy/docker/.env.example",
    "deploy/docker/.env.oracle-legacy.example",
    "docs/project-objectives.md",
    "docs/sustainable-development.md",
    "docs/oracle-to-postgres-transition.md",
    "docs/backend-table-audit-handoff.md",
    "docs/current-content-inventory.md",
    "docs/oracle-migration-backlog.md",
    "metadata/current-content-inventory.json",
    "metadata/oracle-migration-audit.json",
    ".github/workflows/verify.yml",
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


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


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
    pom = (ROOT / "pom.xml").read_text(encoding="utf-8")
    for module in ("<module>backend</module>", "<module>deploy</module>"):
        if module not in pom:
            fail(f"root pom missing module declaration: {module}", failures)
    source_modules = (ROOT / "backend/source-modules/pom.xml").read_text(encoding="utf-8")
    if "<packaging>pom</packaging>" not in source_modules:
        fail("backend/source-modules/pom.xml must remain an aggregator pom", failures)


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
