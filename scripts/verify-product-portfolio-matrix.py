#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MATRIX = ROOT / "metadata/product-portfolio-matrix.json"
PORTFOLIO = ROOT / "docs/product/ft-mes-modular-product-portfolio.md"
ADR = ROOT / "docs/decisions/0001-modular-product-line-architecture.md"
MANUAL = ROOT / "docs/product/ft-mes-lean-standard-product-manual.md"
RATIONALIZATION = ROOT / "docs/product/ft-mes-capability-rationalization.md"
README = ROOT / "README.md"
OBJECTIVES = ROOT / "docs/project-objectives.md"

FOUNDATION_IDS = {
    "ft-platform-foundation",
    "ft-manufacturing-foundation",
}
ALLOWED_PRODUCT_TYPES = {"FOUNDATION", "DOMAIN_PRODUCT"}
ALLOWED_MATURITIES = {
    "M0_ASSET_ONLY",
    "M1_NAVIGABLE",
    "M2_TRANSACTIONAL",
    "M3_SCENARIO_COMPLETE",
    "M4_PRODUCTION_READY",
}
ALLOWED_RELEASE_STATUSES = {
    "SHARED_FOUNDATION",
    "STANDARD_CANDIDATE",
    "CONTROLLED_PILOT",
    "INCLUDED_COMPONENT",
    "HIDDEN",
    "BLOCKED",
}
REQUIRED_PRODUCT_FIELDS = {
    "id",
    "name",
    "type",
    "productResponsibility",
    "hardDependencies",
    "integrationDependencies",
    "externalIntegrations",
    "dataSourceDependencies",
    "includedCapabilities",
    "deployables",
    "databaseSchemas",
    "menuGroups",
    "currentMaturity",
    "maturityNote",
    "targetMaturity",
    "releaseStatus",
}


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_text(path: Path, failures: list[str]) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        fail(failures, f"missing file: {path.relative_to(ROOT)}")
        return ""


def read_json(path: Path, failures: list[str]) -> dict[str, Any]:
    source = read_text(path, failures)
    if not source:
        return {}
    try:
        value = json.loads(source)
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {path.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(value, dict):
        fail(failures, f"{path.relative_to(ROOT)} root must be an object")
        return {}
    return value


def require_string(item: dict[str, Any], field: str, label: str, failures: list[str]) -> None:
    value = item.get(field)
    if not isinstance(value, str) or not value.strip():
        fail(failures, f"{label} has an invalid {field}")


def require_string_list(
    item: dict[str, Any], field: str, label: str, failures: list[str], *, allow_empty: bool = True
) -> list[str]:
    value = item.get(field)
    if not isinstance(value, list) or any(not isinstance(entry, str) or not entry for entry in value):
        fail(failures, f"{label}.{field} must be an array of non-empty strings")
        return []
    if not allow_empty and not value:
        fail(failures, f"{label}.{field} must not be empty")
    if len(value) != len(set(value)):
        fail(failures, f"{label}.{field} contains duplicates")
    return value


def check_local_links(path: Path, source: str, failures: list[str]) -> int:
    count = 0
    for link in re.findall(r"\[[^\]]*\]\(([^)]+)\)", source):
        if link.startswith(("http://", "https://", "#", "mailto:")):
            continue
        target_text = link.split("#", 1)[0].strip("<>")
        if not target_text:
            continue
        count += 1
        if not (path.parent / target_text).resolve().exists():
            fail(failures, f"broken local link in {path.relative_to(ROOT)}: {link}")
    return count


def check_hard_dependency_cycles(
    dependencies: dict[str, list[str]], failures: list[str]
) -> None:
    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(product_id: str, path: list[str]) -> None:
        if product_id in visiting:
            cycle_start = path.index(product_id)
            cycle = path[cycle_start:] + [product_id]
            fail(failures, f"hard dependency cycle: {' -> '.join(cycle)}")
            return
        if product_id in visited:
            return
        visiting.add(product_id)
        path.append(product_id)
        for dependency in dependencies.get(product_id, []):
            visit(dependency, path)
        path.pop()
        visiting.remove(product_id)
        visited.add(product_id)

    for product_id in dependencies:
        visit(product_id, [])


def transitive_hard_dependencies(product_id: str, dependencies: dict[str, list[str]]) -> set[str]:
    closure: set[str] = set()
    stack = list(dependencies.get(product_id, []))
    while stack:
        dependency = stack.pop()
        if dependency in closure:
            continue
        closure.add(dependency)
        stack.extend(dependencies.get(dependency, []))
    return closure


def main() -> int:
    failures: list[str] = []
    matrix = read_json(MATRIX, failures)
    portfolio = read_text(PORTFOLIO, failures)
    adr = read_text(ADR, failures)
    manual = read_text(MANUAL, failures)
    rationalization = read_text(RATIONALIZATION, failures)
    readme = read_text(README, failures)
    objectives = read_text(OBJECTIVES, failures)

    repo_commit = matrix.get("repoCommit")
    if not isinstance(repo_commit, str) or re.fullmatch(r"[0-9a-f]{40}", repo_commit) is None:
        fail(failures, "repoCommit must be a full lowercase Git SHA")

    maturity_levels = matrix.get("maturityLevels")
    if not isinstance(maturity_levels, list):
        fail(failures, "maturityLevels must be an array")
        maturity_levels = []
    maturity_ids = [level.get("id") for level in maturity_levels if isinstance(level, dict)]
    if set(maturity_ids) != ALLOWED_MATURITIES or len(maturity_ids) != len(ALLOWED_MATURITIES):
        fail(failures, "maturityLevels must define every supported maturity exactly once")
    ranks = sorted(level.get("rank") for level in maturity_levels if isinstance(level, dict))
    if ranks != list(range(5)):
        fail(failures, "maturityLevels ranks must be contiguous from 0 through 4")

    deployment_units = matrix.get("targetDeploymentUnits")
    if not isinstance(deployment_units, list):
        fail(failures, "targetDeploymentUnits must be an array")
        deployment_units = []
    deployment_ids: set[str] = set()
    owned_schemas: set[str] = set()
    for index, unit in enumerate(deployment_units):
        if not isinstance(unit, dict):
            fail(failures, f"targetDeploymentUnits[{index}] must be an object")
            continue
        unit_id = unit.get("id")
        if not isinstance(unit_id, str) or not unit_id:
            fail(failures, f"targetDeploymentUnits[{index}] has an invalid id")
            continue
        if unit_id in deployment_ids:
            fail(failures, f"duplicate deployment unit id: {unit_id}")
        deployment_ids.add(unit_id)
        schemas = require_string_list(unit, "ownedSchemas", f"deployment unit {unit_id}", failures)
        owned_schemas.update(schemas)

    products = matrix.get("products")
    if not isinstance(products, list):
        fail(failures, "products must be an array")
        products = []
    product_ids: set[str] = set()
    product_by_id: dict[str, dict[str, Any]] = {}
    maturity_counts: Counter[str] = Counter()
    type_counts: Counter[str] = Counter()
    hard_dependencies: dict[str, list[str]] = {}

    for index, product in enumerate(products):
        if not isinstance(product, dict):
            fail(failures, f"products[{index}] must be an object")
            continue
        missing = sorted(REQUIRED_PRODUCT_FIELDS - product.keys())
        if missing:
            fail(failures, f"products[{index}] missing fields: {', '.join(missing)}")
        product_id = product.get("id")
        label = f"product {product_id or index}"
        if not isinstance(product_id, str) or not product_id:
            fail(failures, f"products[{index}] has an invalid id")
            continue
        if product_id in product_ids:
            fail(failures, f"duplicate product id: {product_id}")
        product_ids.add(product_id)
        product_by_id[product_id] = product
        for field in ("name", "productResponsibility", "maturityNote"):
            require_string(product, field, label, failures)
        product_type = product.get("type")
        if product_type not in ALLOWED_PRODUCT_TYPES:
            fail(failures, f"{label} has unsupported type: {product_type}")
        else:
            type_counts[product_type] += 1
        current_maturity = product.get("currentMaturity")
        target_maturity = product.get("targetMaturity")
        if current_maturity not in ALLOWED_MATURITIES:
            fail(failures, f"{label} has unsupported currentMaturity: {current_maturity}")
        else:
            maturity_counts[current_maturity] += 1
        if target_maturity not in ALLOWED_MATURITIES:
            fail(failures, f"{label} has unsupported targetMaturity: {target_maturity}")
        if product.get("releaseStatus") not in ALLOWED_RELEASE_STATUSES:
            fail(failures, f"{label} has unsupported releaseStatus: {product.get('releaseStatus')}")
        hard_dependencies[product_id] = require_string_list(
            product, "hardDependencies", label, failures
        )
        require_string_list(product, "integrationDependencies", label, failures)
        require_string_list(product, "externalIntegrations", label, failures)
        require_string_list(product, "includedCapabilities", label, failures, allow_empty=False)
        deployables = require_string_list(product, "deployables", label, failures, allow_empty=False)
        schemas = require_string_list(product, "databaseSchemas", label, failures, allow_empty=False)
        require_string_list(product, "menuGroups", label, failures, allow_empty=False)
        if any(unit not in deployment_ids for unit in deployables):
            fail(failures, f"{label} references an unknown deployment unit")
        if any(schema not in owned_schemas for schema in schemas):
            fail(failures, f"{label} references a schema not owned by a deployment unit")
        data_sources = product.get("dataSourceDependencies")
        if not isinstance(data_sources, list):
            fail(failures, f"{label}.dataSourceDependencies must be an array")

    if FOUNDATION_IDS - product_ids:
        fail(failures, f"missing foundation products: {sorted(FOUNDATION_IDS - product_ids)}")

    for product_id, product in product_by_id.items():
        dependencies = hard_dependencies.get(product_id, [])
        integrations = product.get("integrationDependencies", [])
        for dependency in dependencies:
            if dependency == product_id:
                fail(failures, f"product {product_id} depends on itself")
            elif dependency not in product_ids:
                fail(failures, f"product {product_id} has unknown hard dependency: {dependency}")
        for dependency in integrations:
            if dependency == product_id:
                fail(failures, f"product {product_id} integrates with itself")
            elif dependency not in product_ids:
                fail(failures, f"product {product_id} has unknown integration dependency: {dependency}")
        if product.get("type") == "DOMAIN_PRODUCT" and not FOUNDATION_IDS.issubset(
            set(dependencies)
        ):
            fail(failures, f"domain product {product_id} must explicitly depend on both foundations")

    check_hard_dependency_cycles(hard_dependencies, failures)

    bundles = matrix.get("commercialBundles")
    if not isinstance(bundles, list):
        fail(failures, "commercialBundles must be an array")
        bundles = []
    bundle_ids: set[str] = set()
    bundle_products: dict[str, set[str]] = {}
    for index, bundle in enumerate(bundles):
        if not isinstance(bundle, dict):
            fail(failures, f"commercialBundles[{index}] must be an object")
            continue
        bundle_id = bundle.get("id")
        label = f"bundle {bundle_id or index}"
        if not isinstance(bundle_id, str) or not bundle_id:
            fail(failures, f"commercialBundles[{index}] has an invalid id")
            continue
        if bundle_id in bundle_ids:
            fail(failures, f"duplicate bundle id: {bundle_id}")
        bundle_ids.add(bundle_id)
        require_string(bundle, "name", label, failures)
        require_string(bundle, "runtimeWeight", label, failures)
        require_string(bundle, "releaseStatus", label, failures)
        required = require_string_list(bundle, "requiredProducts", label, failures, allow_empty=False)
        optional = require_string_list(bundle, "optionalProducts", label, failures)
        require_string_list(bundle, "targetScenarios", label, failures, allow_empty=False)
        references = set(required) | set(optional)
        if references - product_ids:
            fail(failures, f"{label} references unknown products: {sorted(references - product_ids)}")
        required_set = set(required)
        bundle_products[bundle_id] = required_set
        for product_id in required:
            missing_dependencies = transitive_hard_dependencies(product_id, hard_dependencies) - required_set
            if missing_dependencies:
                fail(
                    failures,
                    f"{label} omits hard dependencies for {product_id}: {sorted(missing_dependencies)}",
                )

    solutions = matrix.get("industrySolutions")
    if not isinstance(solutions, list):
        fail(failures, "industrySolutions must be an array")
        solutions = []
    solution_ids: set[str] = set()
    for index, solution in enumerate(solutions):
        if not isinstance(solution, dict):
            fail(failures, f"industrySolutions[{index}] must be an object")
            continue
        solution_id = solution.get("id")
        label = f"industry solution {solution_id or index}"
        if not isinstance(solution_id, str) or not solution_id:
            fail(failures, f"industrySolutions[{index}] has an invalid id")
            continue
        if solution_id in solution_ids:
            fail(failures, f"duplicate industry solution id: {solution_id}")
        solution_ids.add(solution_id)
        require_string(solution, "name", label, failures)
        base_bundle = solution.get("baseBundle")
        if base_bundle not in bundle_ids:
            fail(failures, f"{label} references unknown base bundle: {base_bundle}")
        additional = require_string_list(solution, "additionalProducts", label, failures)
        excluded = require_string_list(solution, "excludedByDefault", label, failures)
        require_string_list(solution, "industryPacks", label, failures, allow_empty=False)
        if (set(additional) | set(excluded)) - product_ids:
            fail(failures, f"{label} references unknown products")
        assembled = set(bundle_products.get(base_bundle, set())) | set(additional)
        if assembled & set(excluded):
            fail(failures, f"{label} excludes a product included in its assembled solution")
        for product_id in additional:
            missing_dependencies = transitive_hard_dependencies(product_id, hard_dependencies) - assembled
            if missing_dependencies:
                fail(
                    failures,
                    f"{label} omits hard dependencies for {product_id}: {sorted(missing_dependencies)}",
                )

    summary = matrix.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
    else:
        expected_summary = {
            "productUnits": len(products),
            "foundationUnits": type_counts["FOUNDATION"],
            "domainProducts": type_counts["DOMAIN_PRODUCT"],
            "commercialBundles": len(bundles),
            "industrySolutions": len(solutions),
        }
        for field, expected in expected_summary.items():
            if summary.get(field) != expected:
                fail(failures, f"summary.{field} must equal {expected}")
        summary_maturity = summary.get("currentMaturity")
        if not isinstance(summary_maturity, dict):
            fail(failures, "summary.currentMaturity must be an object")
        else:
            if set(summary_maturity) != ALLOWED_MATURITIES:
                fail(failures, "summary.currentMaturity must contain every maturity level")
            for maturity_id in ALLOWED_MATURITIES:
                if summary_maturity.get(maturity_id) != maturity_counts[maturity_id]:
                    fail(
                        failures,
                        f"summary.currentMaturity.{maturity_id} must equal "
                        f"{maturity_counts[maturity_id]}",
                    )

    required_references = {
        "README.md": (readme, "docs/product/ft-mes-modular-product-portfolio.md"),
        "docs/project-objectives.md": (
            objectives,
            "product/ft-mes-modular-product-portfolio.md",
            "metadata/product-portfolio-matrix.json",
        ),
        "lean product manual": (manual, "ft-mes-modular-product-portfolio.md"),
        "capability rationalization": (
            rationalization,
            "ft-mes-modular-product-portfolio.md",
        ),
    }
    for label, (source, *fragments) in required_references.items():
        for fragment in fragments:
            if fragment not in source:
                fail(failures, f"{label} must reference {fragment}")

    link_count = check_local_links(PORTFOLIO, portfolio, failures)
    link_count += check_local_links(ADR, adr, failures)

    if failures:
        print(f"Product portfolio verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1

    print(
        "Product portfolio verification passed "
        f"(products={len(products)}, bundles={len(bundles)}, "
        f"solutions={len(solutions)}, hardDependencyCycles=0, localLinks={link_count})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
