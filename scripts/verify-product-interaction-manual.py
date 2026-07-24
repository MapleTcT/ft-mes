#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANUAL_PATH = ROOT / "docs/product/ft-mes-bpi-product-interaction-manual.md"
FRONTEND_PATH = ROOT / "frontend/apps/bpi/src/main.ts"
OPENAPI_PATH = ROOT / "contracts/bpi-api/openapi.json"
MIGRATION_PATH = ROOT / "services/bpi-service/app/src/main/resources/db/migration"
README_PATH = ROOT / "README.md"
DESIGN_INDEX_PATH = ROOT / "docs/designs/README.md"

MANUAL_RELATIVE = "docs/product/ft-mes-bpi-product-interaction-manual.md"
REQUIRED_FRAGMENTS = {
    "https://github.com/MapleTcT/ft-mes",
    "https://github.com/MapleTcT/iot",
    "/Users/zhangchu/Documents/ADP/adp-source-repo",
    "/Users/zhangchu/Documents/ADP/adp-bpi-live-operations",
    "/Users/zhangchu/Documents/ADP/iot",
    "/home/v6/adp-mes-docker-newbase-20260611-181921/deploy/docker",
    "/home/v6/adp-bpi-stream-v15/deploy/bpi-streaming",
    "http://10.11.100.17:18080/",
    "http://100.99.133.43:18080/",
    "http://100.99.133.43:18081/",
    "IN_PROGRESS_NOT_COMPLETE",
    "当前产品可定义为“工程化集成测试系统”，不能定义为“生产 READY”",
}


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def read_text(path: Path, failures: list[str], label: str) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        fail(failures, f"missing {label}: {path.relative_to(ROOT)}")
        return ""


def frontend_views(source: str, failures: list[str]) -> set[str]:
    match = re.search(r"const VIEWS:[^=]+=\s*\[([^\]]+)\]", source)
    if not match:
        fail(failures, "could not parse frontend VIEWS")
        return set()
    return set(re.findall(r"'([A-Za-z][A-Za-z0-9]*)'", match.group(1)))


def documented_views(manual: str) -> set[str]:
    return set(re.findall(r"/bpi/#/([A-Za-z][A-Za-z0-9]*)", manual))


def inline_code_spans(manual: str) -> list[str]:
    return re.findall(r"(?<!`)`([^`\n]+)`(?!`)", manual)


def check_api_references(manual: str, failures: list[str]) -> int:
    try:
        openapi = json.loads(OPENAPI_PATH.read_text(encoding="utf-8"))
    except FileNotFoundError:
        fail(failures, f"missing OpenAPI contract: {OPENAPI_PATH.relative_to(ROOT)}")
        return 0
    except json.JSONDecodeError as error:
        fail(failures, f"invalid OpenAPI JSON: {error}")
        return 0

    paths = openapi.get("paths")
    if not isinstance(paths, dict):
        fail(failures, "OpenAPI paths must be an object")
        return 0

    references: set[tuple[str, str]] = set()
    pattern = re.compile(r"^(GET|POST|GET\|POST) (/bpi-api/[A-Za-z0-9_{}./-]+)$")
    for span in inline_code_spans(manual):
        match = pattern.match(span)
        if not match:
            continue
        methods = match.group(1).split("|")
        service_path = match.group(2).replace("/bpi-api", "/bpi/v1", 1)
        for method in methods:
            references.add((method.lower(), service_path))

    if not references:
        fail(failures, "manual contains no documented BPI API methods")
        return 0

    for method, service_path in sorted(references):
        path_item = paths.get(service_path)
        if not isinstance(path_item, dict):
            fail(failures, f"manual API path is absent from OpenAPI: {service_path}")
            continue
        if method not in path_item:
            fail(failures, f"manual API method is absent from OpenAPI: {method.upper()} {service_path}")
    return len(references)


def check_local_links(manual: str, failures: list[str]) -> int:
    links = re.findall(r"\[[^\]]*\]\(([^)]+)\)", manual)
    local_links = [
        link
        for link in links
        if not link.startswith(("http://", "https://", "#", "mailto:"))
    ]
    for link in local_links:
        path_text = link.split("#", 1)[0].strip("<>")
        target = (MANUAL_PATH.parent / path_text).resolve()
        if not target.exists():
            fail(failures, f"manual local link does not exist: {link}")
    return len(local_links)


def check_table_references(manual: str, failures: list[str]) -> int:
    table_names = set(re.findall(r"bpi\.(bpi_[a-z0-9_]+)", manual))
    migration_text = "\n".join(
        path.read_text(encoding="utf-8")
        for path in sorted(MIGRATION_PATH.glob("*.sql"))
    )
    for table_name in sorted(table_names):
        if f"bpi.{table_name}" not in migration_text:
            fail(failures, f"manual table is absent from Flyway migrations: bpi.{table_name}")
    return len(table_names)


def main() -> int:
    failures: list[str] = []
    manual = read_text(MANUAL_PATH, failures, "product interaction manual")
    frontend = read_text(FRONTEND_PATH, failures, "BPI frontend entry")
    readme = read_text(README_PATH, failures, "README")
    design_index = read_text(DESIGN_INDEX_PATH, failures, "design index")
    if failures:
        return 1

    for fragment in sorted(REQUIRED_FRAGMENTS):
        if fragment not in manual:
            fail(failures, f"manual missing required fragment: {fragment}")

    code_views = frontend_views(frontend, failures)
    manual_views = documented_views(manual)
    if code_views != manual_views:
        fail(
            failures,
            "manual routes differ from frontend VIEWS: "
            f"missing={sorted(code_views - manual_views)}, extra={sorted(manual_views - code_views)}",
        )

    api_reference_count = check_api_references(manual, failures)
    local_link_count = check_local_links(manual, failures)
    table_reference_count = check_table_references(manual, failures)

    if MANUAL_RELATIVE not in readme:
        fail(failures, f"README must link {MANUAL_RELATIVE}")
    if "../product/ft-mes-bpi-product-interaction-manual.md" not in design_index:
        fail(failures, "design index must link the current product interaction manual")

    if failures:
        print(f"Product interaction manual verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1

    print(
        "Product interaction manual verification passed "
        f"(routes={len(code_views)}, apiMethods={api_reference_count}, "
        f"tables={table_reference_count}, localLinks={local_link_count})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
