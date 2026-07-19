#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODULE = ROOT / "backend/source-modules/batch-intelligence-adapter"
REQUIRED = [
    "Dockerfile",
    "README.md",
    "pom.xml",
    "src/main/resources/application.yml",
    "src/main/java/com/mapletct/ftmes/bpiadapter/BpiClaimsMapper.java",
    "src/main/java/com/mapletct/ftmes/bpiadapter/BpiProxyController.java",
    "src/main/java/com/mapletct/ftmes/bpiadapter/BpiRoutePolicy.java",
    "src/main/java/com/mapletct/ftmes/bpiadapter/InternalJwtIssuer.java",
    "src/main/java/com/mapletct/ftmes/bpiadapter/SecurityConfiguration.java",
    "src/test/java/com/mapletct/ftmes/bpiadapter/BpiProxyControllerTest.java",
]


def main() -> int:
    failures: list[str] = []
    for relative in REQUIRED:
        if not (MODULE / relative).is_file():
            failures.append(f"missing adapter file: backend/source-modules/batch-intelligence-adapter/{relative}")

    evidence_path = ROOT / "metadata/bpi-adapter-acceptance.json"
    if not evidence_path.is_file():
        failures.append("missing metadata/bpi-adapter-acceptance.json")

    if not failures:
        pom = (MODULE / "pom.xml").read_text(encoding="utf-8")
        for forbidden in ("spring-boot-starter-jdbc", "postgresql", "ojdbc"):
            if forbidden in pom:
                failures.append(f"stateless BPI adapter must not declare {forbidden!r}")

        dockerfile = (MODULE / "Dockerfile").read_text(encoding="utf-8")
        child_build = "mvn -q -f backend/source-modules/batch-intelligence-adapter/pom.xml"
        if child_build not in dockerfile:
            failures.append("BPI adapter image must build from the copied child POM")
        if "mvn -q -f backend/source-modules/pom.xml" in dockerfile:
            failures.append("BPI adapter image must not invoke the uncopied source-modules reactor")

        proxy = (MODULE / "src/main/java/com/mapletct/ftmes/bpiadapter/BpiProxyController.java").read_text(encoding="utf-8")
        for required in ("/bpi-api", "/bpi/v1", "Idempotency-Key", "If-Match", "setBearerAuth", "65_536"):
            if required not in proxy:
                failures.append(f"BPI adapter proxy is missing {required!r}")
        for forbidden in ('getHeader("X-Tenant-Id")', 'getHeader("X-Plant-Id")', 'getHeader("X-Line-Id")'):
            if forbidden in proxy:
                failures.append(f"BPI adapter trusts forbidden browser scope header {forbidden!r}")

        route_policy = (MODULE / "src/main/java/com/mapletct/ftmes/bpiadapter/BpiRoutePolicy.java").read_text(encoding="utf-8")
        for required in (
            "confirm",
            "reject",
            "suspend",
            "resume",
            "point-calibrations",
            "approve",
            "revoke",
            "topologies",
            "rules",
            "rule-simulations",
            "compare",
            "simulate",
            "submit-approval",
            "reject-approval",
            "publish",
            "retire",
            "shadow-runs",
            "feature-flags",
            "batch-reviews",
            "complete",
            "cancel",
        ):
            if required not in route_policy:
                failures.append(f"BPI adapter route policy is missing approved command {required!r}")

        properties = (MODULE / "src/main/java/com/mapletct/ftmes/bpiadapter/BpiAdapterProperties.java").read_text(encoding="utf-8")
        for required in ("at least 32 UTF-8 bytes", "Duration.ofMinutes(15)", "parseRoleRules", "parseSubjectScopeRules", "roleMappings.isEmpty()", "subjectScopes.isEmpty()"):
            if required not in properties:
                failures.append(f"BPI adapter properties are missing {required!r}")

        security = (MODULE / "src/main/java/com/mapletct/ftmes/bpiadapter/SecurityConfiguration.java").read_text(encoding="utf-8")
        application = (MODULE / "src/main/java/com/mapletct/ftmes/bpiadapter/BpiAdapterApplication.java").read_text(encoding="utf-8")
        if "SessionCreationPolicy.STATELESS" not in security:
            failures.append("BPI adapter security must remain stateless")
        if "UserDetailsServiceAutoConfiguration.class" not in application:
            failures.append("BPI adapter must exclude the default generated user/password")

        compose = (ROOT / "deploy/docker/docker-compose.yml").read_text(encoding="utf-8")
        nginx = (ROOT / "deploy/docker/nginx/adp.conf").read_text(encoding="utf-8")
        for required in (
            "bpi-adapter:",
            "BPI_ADAPTER_KEYCLOAK_JWK_SET_URI",
            "BPI_ADAPTER_KEYCLOAK_ISSUER",
            "BPI_ADAPTER_LEGACY_TICKET_ENABLED",
            "BPI_ADAPTER_LEGACY_GATEWAY_BASE_URL",
            "BPI_ADAPTER_ROLE_RULES",
            "systemRole=BPI_ADMIN|BPI_OPERATOR",
            "BPI_ADAPTER_SUBJECT_SCOPE_RULES",
        ):
            if required not in compose:
                failures.append(f"BPI adapter Compose wiring is missing {required!r}")
        for required in ("location ^~ /bpi-api/", "location ^~ /bpi/", "bpi_adapter_upstream"):
            if required not in nginx:
                failures.append(f"BPI adapter Nginx wiring is missing {required!r}")

        evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
        summary = evidence.get("summary", {})
        if summary.get("tests") != 21 or summary.get("pass") != 21:
            failures.append("BPI adapter acceptance must record twenty-one passing tests")
        if summary.get("runtimeSmokeChecks") != 2 or summary.get("runtimeSmokePass") != 2:
            failures.append("BPI adapter acceptance must record two passing runtime smoke checks")
        limitations = " ".join(evidence.get("limitations", []))
        for required in ("Keycloak", "PostgreSQL", "HS256"):
            if required not in limitations:
                failures.append(f"BPI adapter acceptance limitations are missing {required!r}")

    if failures:
        print("\n".join(f"BPI adapter error: {failure}" for failure in failures), file=sys.stderr)
        return 1
    print("BPI Java 8 adapter auth, scope, proxy, deployment, and acceptance boundaries verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
