#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANUAL = ROOT / "docs/product/ft-mes-lean-standard-product-manual.md"
RATIONALIZATION = ROOT / "docs/product/ft-mes-capability-rationalization.md"
MATRIX = ROOT / "metadata/product-capability-rationalization.json"
README = ROOT / "README.md"
OBJECTIVES = ROOT / "docs/project-objectives.md"

EXPECTED_COUNTS = {
    "KEEP_STANDARD": 18,
    "MERGE_INTO_STANDARD": 6,
    "INDUSTRY_PACK": 8,
    "INTERNAL_CONSOLE": 5,
    "HIDE_UNTIL_READY": 6,
    "RETIRE_AFTER_OBSERVATION": 2,
}

REQUIRED_ITEM_FIELDS = {
    "id",
    "name",
    "sourceModules",
    "decision",
    "targetEdition",
    "targetPlacement",
    "standardDefault",
    "currentEvidence",
    "releaseGate",
}

REQUIRED_PRIMARY_MENUS = {
    "工作台",
    "生产执行",
    "质量管理",
    "物料库存",
    "批次追溯",
    "智能批次",
    "主数据",
    "系统管理",
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


def main() -> int:
    failures: list[str] = []
    manual = read_text(MANUAL, failures)
    rationalization = read_text(RATIONALIZATION, failures)
    readme = read_text(README, failures)
    objectives = read_text(OBJECTIVES, failures)

    try:
        matrix = json.loads(read_text(MATRIX, failures))
    except json.JSONDecodeError as error:
        fail(failures, f"invalid capability matrix JSON: {error}")
        matrix = {}

    if failures and not matrix:
        return 1

    repo_commit = matrix.get("repoCommit")
    if not isinstance(repo_commit, str) or re.fullmatch(r"[0-9a-f]{40}", repo_commit) is None:
        fail(failures, "repoCommit must be a full lowercase Git SHA")

    items = matrix.get("items")
    if not isinstance(items, list):
        fail(failures, "items must be an array")
        items = []

    ids: set[str] = set()
    counts: Counter[str] = Counter()
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            fail(failures, f"item {index} must be an object")
            continue
        missing = sorted(REQUIRED_ITEM_FIELDS - item.keys())
        if missing:
            fail(failures, f"item {index} missing fields: {', '.join(missing)}")
        item_id = item.get("id")
        if not isinstance(item_id, str) or not item_id:
            fail(failures, f"item {index} has an invalid id")
        elif item_id in ids:
            fail(failures, f"duplicate capability id: {item_id}")
        else:
            ids.add(item_id)
        decision = item.get("decision")
        if decision not in EXPECTED_COUNTS:
            fail(failures, f"item {item_id or index} has unsupported decision: {decision}")
        else:
            counts[decision] += 1
        if not isinstance(item.get("sourceModules"), list) or not item.get("sourceModules"):
            fail(failures, f"item {item_id or index} must declare sourceModules")
        if not isinstance(item.get("standardDefault"), bool):
            fail(failures, f"item {item_id or index} standardDefault must be boolean")
        for field in ("name", "targetEdition", "targetPlacement", "currentEvidence", "releaseGate"):
            if not isinstance(item.get(field), str) or not item[field].strip():
                fail(failures, f"item {item_id or index} has empty {field}")

    if counts != Counter(EXPECTED_COUNTS):
        fail(failures, f"decision counts differ: actual={dict(counts)}, expected={EXPECTED_COUNTS}")

    summary = matrix.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
    else:
        if summary.get("totalCapabilities") != len(items):
            fail(failures, "summary.totalCapabilities differs from items length")
        for decision, expected in EXPECTED_COUNTS.items():
            if summary.get(decision) != expected:
                fail(failures, f"summary.{decision} must equal {expected}")

    missing_menus = sorted(menu for menu in REQUIRED_PRIMARY_MENUS if menu not in manual)
    if missing_menus:
        fail(failures, f"lean manual is missing primary menus: {', '.join(missing_menus)}")

    required_references = {
        "README.md": (
            readme,
            "docs/product/ft-mes-lean-standard-product-manual.md",
            "docs/product/ft-mes-capability-rationalization.md",
        ),
        "docs/project-objectives.md": (
            objectives,
            "product/ft-mes-lean-standard-product-manual.md",
            "product/ft-mes-capability-rationalization.md",
            "metadata/product-capability-rationalization.json",
        ),
    }
    for label, (source, *fragments) in required_references.items():
        for fragment in fragments:
            if fragment not in source:
                fail(failures, f"{label} must reference {fragment}")

    link_count = check_local_links(MANUAL, manual, failures)
    link_count += check_local_links(RATIONALIZATION, rationalization, failures)

    if failures:
        print(
            f"Product capability rationalization verification failed: {len(failures)} issue(s).",
            file=sys.stderr,
        )
        return 1

    print(
        "Product capability rationalization verification passed "
        f"(capabilities={len(items)}, decisions={dict(counts)}, localLinks={link_count})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
