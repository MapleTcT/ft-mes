#!/usr/bin/env python3
from __future__ import annotations

import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

REQUIRED_PATHS = [
    "AGENTS.md",
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
    "docs/project-goal-acceptance.md",
    "docs/sustainable-development.md",
    "docs/oracle-to-postgres-transition.md",
    "docs/runtime-validation-scope.md",
    "docs/backend-table-audit-handoff.md",
    "docs/module-intake-latest-basic-modules.md",
    "docs/functional-persistence-acceptance.md",
    "docs/frontend-functional-test-report.md",
    "docs/backend-table-audit/persistence-acceptance.md",
    "docs/backend-table-audit/business-production.md",
    "docs/production-module-functional-test-cases.md",
    "docs/production-module-backlog.md",
    "docs/business-module-intake-requirements.md",
    "docs/current-content-inventory.md",
    "docs/backend-module-dependency-inventory.md",
    "docs/oracle-migration-backlog.md",
    "docs/oracle-replacement-status.md",
    "docs/postgres-migration-index.md",
    "docs/production-migration-readiness.md",
    "docs/production-migration/README.md",
    "docs/production-migration/rehearsal-plan.md",
    "docs/production-migration/rollback-runbook.md",
    "docs/production-migration/license-strategy.md",
    "docs/production-migration/minio-migration-runbook.md",
    "docs/production-migration/keycloak-production-runbook.md",
    "docs/production-migration/nacos-runtime-patch-runbook.md",
    "docs/production-migration/runtime-patch-manifest.md",
    "docs/production-migration/network-tls-checklist.md",
    "docs/production-migration/security-hardening-checklist.md",
    "docs/production-migration/business-smoke-signoff-template.md",
    "metadata/current-content-inventory.json",
    "metadata/project-goal-acceptance.json",
    "metadata/backend-module-dependency-inventory.json",
    "metadata/module-intake-latest-basic-modules.json",
    "metadata/oracle-migration-audit.json",
    "metadata/oracle-replacement-status.json",
    "metadata/postgres-migration-inventory.json",
    "metadata/persistence-acceptance.json",
    "metadata/platform-validation-smoke.json",
    "metadata/production-module-test-cases.json",
    "metadata/production-module-blockers.json",
    "metadata/production-module-backlog.json",
    "metadata/business-module-intake-requirements.json",
    "metadata/business-dependency-package-scan.json",
    "metadata/production-export-readiness-smoke.json",
    "metadata/production-migration-readiness.json",
    "metadata/production-cutover-gate.json",
    "metadata/production-rehearsal-plan.json",
    "metadata/runtime-patch-manifest.json",
    ".github/workflows/verify.yml",
    "scripts/create-backend-source-module.py",
    "scripts/precheck-module-intake.py",
    "scripts/verify-module-intake-candidate-report.py",
    "scripts/verify-module-intake-precheck.py",
    "scripts/verify-source-modules.py",
    "scripts/generate-backend-dependency-inventory.py",
    "scripts/generate-oracle-replacement-status.py",
    "scripts/generate-postgres-migration-inventory.py",
    "scripts/verify-project-goal-acceptance.py",
    "scripts/verify-persistence-acceptance.py",
    "scripts/verify-platform-validation-smoke.py",
    "scripts/verify-production-module-test-cases.py",
    "scripts/verify-production-module-blockers.py",
    "scripts/verify-production-module-backlog.py",
    "scripts/verify-business-module-intake-requirements.py",
    "scripts/scan-business-dependency-packages.py",
    "scripts/verify-business-dependency-package-scan.py",
    "scripts/verify-production-export-readiness.py",
    "scripts/verify-production-migration-readiness.py",
    "scripts/verify-production-cutover-gate.py",
    "scripts/refresh-production-source-evidence.py",
    "scripts/generate-production-rehearsal-plan.py",
    "scripts/generate-runtime-patch-manifest.py",
    "deploy/database/production-migration/README.md",
    "deploy/database/production-migration/.env.example",
    "deploy/database/production-migration/table-list.example.txt",
    "deploy/database/production-migration/source-checksums.example.tsv",
    "deploy/database/production-migration/target-checksums.example.tsv",
    "deploy/database/production-migration/scripts/run-target-preflight.sh",
    "deploy/database/production-migration/scripts/run-source-inventory.sh",
    "deploy/database/production-migration/scripts/compare-row-counts.py",
    "deploy/database/production-migration/scripts/compare-checksums.py",
    "deploy/database/production-migration/sql/target-schema-inventory.sql",
    "deploy/database/production-migration/sql/target-row-counts-template.sql",
    "deploy/database/production-migration/sql/target-checksum-template.sql",
    "deploy/minio/production-migration/README.md",
    "deploy/minio/production-migration/.env.example",
    "deploy/minio/production-migration/bucket-list.example.txt",
    "deploy/minio/production-migration/scripts/run-bucket-inventory.sh",
    "deploy/minio/production-migration/scripts/normalize-mc-ls-json.py",
    "deploy/minio/production-migration/scripts/compare-bucket-inventory.py",
    "deploy/keycloak/production-migration/README.md",
    "deploy/keycloak/production-migration/.env.example",
    "deploy/keycloak/production-migration/scripts/export-realm-inventory.sh",
    "deploy/keycloak/production-migration/scripts/normalize-realm-inventory.py",
    "deploy/keycloak/production-migration/scripts/compare-realm-inventory.py",
    "deploy/rollback/production-migration/README.md",
    "deploy/rollback/production-migration/rollback-evidence.example.json",
    "deploy/rollback/production-migration/scripts/validate-rollback-evidence.py",
    "deploy/license/production-migration/README.md",
    "deploy/license/production-migration/license-decision.example.json",
    "deploy/license/production-migration/scripts/validate-license-decision.py",
    "deploy/network/production-migration/README.md",
    "deploy/network/production-migration/network-tls-plan.example.json",
    "deploy/network/production-migration/scripts/validate-network-tls-plan.py",
    "deploy/security/production-migration/README.md",
    "deploy/security/production-migration/security-hardening-plan.example.json",
    "deploy/security/production-migration/scripts/validate-security-hardening-plan.py",
    "deploy/business-smoke/production-migration/README.md",
    "deploy/business-smoke/production-migration/business-smoke-signoff.example.json",
    "deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py",
    "deploy/docker/scripts/adp-platform-validation-smoke.js",
    "deploy/docker/scripts/adp-production-action-discovery.js",
    "deploy/docker/scripts/adp-production-export-readiness-smoke.js",
    "deploy/docker/scripts/patch-eam-reactapi-ready.py",
]

REQUIRED_TEXT = {
    "AGENTS.md": [
        "本项目当前阶段以功能验收优先",
        "必须先验证真实前端行为",
        "真实落到 PostgreSQL",
        "不能只凭代码推断",
        "不能跳到写文档结束",
        "可交接、可复验、可继续推进",
    ],
    "docs/functional-persistence-acceptance.md": [
        "本次项目固定指令",
        "任务不是继续补治理层，也不是只跑静态检查",
        "真实前端页面或等效 E2E",
        "唯一 marker",
        "metadata/persistence-acceptance.json",
        "不能因为启动失败就跳到写文档结束",
        "业务动作是否真实落库",
    ],
    "README.md": [
        "项目工作指令",
        "功能验收与落库验收规则",
        "不能只凭源码、静态检查或 `make ci` 判断功能完成",
    ],
}

ALLOWED_BINARY_FILES = {
    "deploy/docker/patches/kafka-jaas-noop/kafka-jaas-noop.jar",
    "deploy/docker/patches/notification-dynamic-templates/notification-dynamic-templates.jar",
    "deploy/docker/patches/rm-config-defaults/rm-config-defaults.jar",
    "deploy/docker/patches/scaffold-dbp-postgresql-line/scaffold-dbp-postgresql-line.jar",
    "deploy/docker/patches/scaffold-dbp-postgresql-url/scaffold-dbp-postgresql-url.jar",
    "deploy/docker/patches/scdog-test-bypass/lib/libSCDog.so",
    "deploy/docker/patches/wom-config-default/wom-config-default.jar",
}

ALLOWED_GENERATED_FILES_WITH_SOURCE = {
    "deploy/docker/patches/orgmanagement-standalone-auth-tasks/classes/com/supcon/supfusion/auth/service/task/AuthOnlionLoginTask.class": (
        "deploy/docker/patches/orgmanagement-standalone-auth-tasks/src/com/supcon/supfusion/auth/service/task/AuthOnlionLoginTask.java"
    ),
    "deploy/docker/patches/signature-log-service-fallback/classes/com/supcon/supfusion/signature/services/service/impl/SignatureLogServiceImpl.class": (
        "deploy/docker/patches/signature-log-service-fallback/src/com/supcon/supfusion/signature/services/service/impl/SignatureLogServiceImpl.java"
    ),
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

REQUIRED_GITIGNORE_PATTERNS = {
    "**/.DS_Store",
    "__pycache__/",
    "*.pyc",
    "/metadata/tmp/",
    "/metadata/*.log",
    "/metadata/*.cache",
    "/test-environment-smoke.json",
    "*.class",
    "*.jar",
    "*.war",
    "*.ear",
    "*.exe",
    "*.dll",
    "*.so",
    "*.dylib",
    "/logs/",
    "/data/",
    "/dist/",
    "**/target/",
    "/deploy/docker/patches/*/build/",
    "/node_modules/",
    "/deploy/docker/.env",
    "/deploy/docker/nacos-rendered/",
}

TRACKED_GENERATED_MARKERS = (
    "/__pycache__/",
    "/node_modules/",
    "/target/",
    "/dist/",
)

TRACKED_GENERATED_SUFFIXES = {
    ".pyc",
    ".class",
    ".log",
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


def check_required_text(failures: list[str]) -> None:
    for relative, snippets in REQUIRED_TEXT.items():
        path = ROOT / relative
        if not path.exists():
            continue
        content = path.read_text(encoding="utf-8")
        for snippet in snippets:
            if snippet not in content:
                fail(f"{relative} missing required instruction text: {snippet}", failures)


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


def check_gitignore_policy(failures: list[str]) -> None:
    gitignore = ROOT / ".gitignore"
    if not gitignore.exists():
        fail(".gitignore is required to keep generated artifacts out of the sustainable repo", failures)
        return

    patterns = {
        line.strip()
        for line in gitignore.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }
    missing = sorted(REQUIRED_GITIGNORE_PATTERNS - patterns)
    if missing:
        fail(".gitignore missing required generated-artifact patterns: " + ", ".join(missing), failures)


def check_tracked_generated_artifacts(failures: list[str]) -> None:
    for generated, source in ALLOWED_GENERATED_FILES_WITH_SOURCE.items():
        if not (ROOT / generated).exists():
            fail(f"allowed runtime patch generated file is missing: {generated}", failures)
        if not (ROOT / source).exists():
            fail(f"allowed runtime patch generated file must keep adjacent source: {generated} -> {source}", failures)

    for relative in git_ls_files():
        if relative in ALLOWED_GENERATED_FILES_WITH_SOURCE:
            continue
        normalized = f"/{relative}"
        suffix = Path(relative).suffix.lower()
        if suffix in TRACKED_GENERATED_SUFFIXES:
            fail(f"tracked generated artifact is not allowed: {relative}", failures)
            continue
        if any(marker in normalized for marker in TRACKED_GENERATED_MARKERS):
            fail(f"tracked generated directory artifact is not allowed: {relative}", failures)


def main() -> int:
    failures: list[str] = []
    check_required_paths(failures)
    check_required_text(failures)
    check_maven_structure(failures)
    check_postgres_defaults(failures)
    check_gitignore_policy(failures)
    check_tracked_generated_artifacts(failures)
    check_binary_policy(failures)
    check_large_files(failures)
    if failures:
        print(f"Sustainable repo verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Sustainable repo verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
