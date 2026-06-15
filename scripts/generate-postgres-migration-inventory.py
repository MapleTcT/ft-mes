#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MIGRATION_DIR = ROOT / "deploy/docker/postgres/init"
OUTPUT_JSON = ROOT / "metadata/postgres-migration-inventory.json"
OUTPUT_MD = ROOT / "docs/postgres-migration-index.md"

FILENAME_RE = re.compile(r"^(?P<number>\d{3})-(?P<slug>[a-z0-9][a-z0-9-]*)\.sql$")
HIGH_RISK_PATTERNS = {
    "drop-database": re.compile(r"\bdrop\s+database\b", re.IGNORECASE),
    "drop-schema": re.compile(r"\bdrop\s+schema\b", re.IGNORECASE),
    "drop-table": re.compile(r"\bdrop\s+table\b", re.IGNORECASE),
    "truncate": re.compile(r"\btruncate\s+(?:table\s+)?", re.IGNORECASE),
}
WATCH_PATTERNS = {
    "delete": re.compile(r"\bdelete\s+from\b", re.IGNORECASE),
    "drop-view": re.compile(r"\bdrop\s+view\b", re.IGNORECASE),
    "drop-function": re.compile(r"\bdrop\s+function\b", re.IGNORECASE),
    "drop-trigger": re.compile(r"\bdrop\s+trigger\b", re.IGNORECASE),
    "drop-operator": re.compile(r"\bdrop\s+operator\b", re.IGNORECASE),
    "drop-aggregate": re.compile(r"\bdrop\s+aggregate\b", re.IGNORECASE),
}
STATEMENT_PATTERNS = {
    "create-table": re.compile(r"\bcreate\s+table\b", re.IGNORECASE),
    "create-view": re.compile(r"\bcreate\s+(?:or\s+replace\s+)?view\b", re.IGNORECASE),
    "create-function": re.compile(r"\bcreate\s+(?:or\s+replace\s+)?function\b", re.IGNORECASE),
    "create-index": re.compile(r"\bcreate\s+(?:unique\s+)?index\b", re.IGNORECASE),
    "alter-table": re.compile(r"\balter\s+table\b", re.IGNORECASE),
    "insert": re.compile(r"\binsert\s+into\b", re.IGNORECASE),
    "update": re.compile(r"\bupdate\s+", re.IGNORECASE),
}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def lines(path: Path) -> list[str]:
    return path.read_text(encoding="utf-8").splitlines()


def compact(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()[:220]


def classify_slug(slug: str) -> list[str]:
    tags: list[str] = []
    rules = {
        "platform": ("platform", "runtime", "ui", "page", "menu", "theme", "portal", "i18n"),
        "auth-rbac-org": ("auth", "rbac", "organization", "org", "role", "permission", "user"),
        "workflow": ("workflow", "wfm", "flow", "scheduler", "quartz", "pending"),
        "configuration": ("configuration", "ec", "entity", "view", "model", "property"),
        "notification": ("notification", "notice", "stationletter", "sms", "dingtalk"),
        "business": ("business", "foundation", "wts", "craftgraph", "lims", "qcs", "sysbase"),
        "compatibility": ("compat", "fixup", "shim", "boolean", "cast"),
    }
    for tag, needles in rules.items():
        if any(needle in slug for needle in needles):
            tags.append(tag)
    return tags or ["general"]


def scan_file(path: Path) -> dict[str, object]:
    match = FILENAME_RE.match(path.name)
    text_lines = lines(path)
    text = "\n".join(text_lines)
    slug = match.group("slug") if match else path.stem
    statement_counts = {
        name: len(regex.findall(text))
        for name, regex in STATEMENT_PATTERNS.items()
    }
    high_risk: list[dict[str, object]] = []
    watch: list[dict[str, object]] = []
    for line_number, line in enumerate(text_lines, start=1):
        for name, regex in HIGH_RISK_PATTERNS.items():
            if regex.search(line):
                high_risk.append(
                    {
                        "line": line_number,
                        "pattern": name,
                        "excerpt": compact(line),
                    }
                )
        for name, regex in WATCH_PATTERNS.items():
            if regex.search(line):
                watch.append(
                    {
                        "line": line_number,
                        "pattern": name,
                        "excerpt": compact(line),
                    }
                )
    return {
        "file": str(path.relative_to(ROOT)),
        "name": path.name,
        "number": int(match.group("number")) if match else None,
        "slug": slug,
        "validName": match is not None,
        "tags": classify_slug(slug),
        "lineCount": len(text_lines),
        "sha256": sha256(path),
        "statementCounts": statement_counts,
        "highRiskStatements": high_risk,
        "watchStatements": watch,
    }


def build_inventory() -> dict[str, object]:
    files = sorted(MIGRATION_DIR.glob("*.sql"))
    migrations = [scan_file(path) for path in files]
    numbers = [item["number"] for item in migrations if item["number"] is not None]
    duplicate_numbers = sorted({number for number in numbers if numbers.count(number) > 1})
    expected = list(range(min(numbers), max(numbers) + 1)) if numbers else []
    missing_numbers = [number for number in expected if number not in numbers]
    invalid_names = [item["name"] for item in migrations if not item["validName"]]
    high_risk_count = sum(len(item["highRiskStatements"]) for item in migrations)
    watch_count = sum(len(item["watchStatements"]) for item in migrations)
    statement_totals: Counter[str] = Counter()
    tag_totals: Counter[str] = Counter()
    for item in migrations:
        statement_totals.update(item["statementCounts"])  # type: ignore[arg-type]
        tag_totals.update(item["tags"])  # type: ignore[arg-type]
    return {
        "schemaVersion": 1,
        "migrationDirectory": str(MIGRATION_DIR.relative_to(ROOT)),
        "migrationCount": len(migrations),
        "firstNumber": min(numbers) if numbers else None,
        "lastNumber": max(numbers) if numbers else None,
        "missingNumbers": missing_numbers,
        "duplicateNumbers": duplicate_numbers,
        "invalidNames": invalid_names,
        "highRiskStatementCount": high_risk_count,
        "watchStatementCount": watch_count,
        "statementTotals": dict(sorted(statement_totals.items())),
        "tagTotals": dict(sorted(tag_totals.items())),
        "migrations": migrations,
    }


def table(rows: list[list[str]]) -> str:
    if not rows:
        return ""
    output = [
        "| " + " | ".join(rows[0]) + " |",
        "| " + " | ".join(["---"] * len(rows[0])) + " |",
    ]
    output.extend("| " + " | ".join(row) + " |" for row in rows[1:])
    return "\n".join(output)


def render_markdown(inventory: dict[str, object]) -> str:
    migration_rows = [["No.", "File", "Tags", "Lines", "DDL/DML Summary", "Watch"]]
    for item in inventory["migrations"]:  # type: ignore[index]
        counts = item["statementCounts"]
        summary = ", ".join(
            f"{name}:{count}"
            for name, count in counts.items()
            if count
        ) or "-"
        watch_count = len(item["watchStatements"])
        risk_count = len(item["highRiskStatements"])
        watch = []
        if risk_count:
            watch.append(f"high-risk:{risk_count}")
        if watch_count:
            watch.append(f"watch:{watch_count}")
        migration_rows.append(
            [
                f"{item['number']:03d}" if item["number"] is not None else "?",
                str(item["name"]),
                ", ".join(item["tags"]),
                str(item["lineCount"]),
                summary,
                ", ".join(watch) or "-",
            ]
        )

    tag_rows = [["Tag", "Count"]]
    for tag, count in inventory["tagTotals"].items():  # type: ignore[union-attr]
        tag_rows.append([str(tag), str(count)])

    statement_rows = [["Statement", "Count"]]
    for name, count in inventory["statementTotals"].items():  # type: ignore[union-attr]
        statement_rows.append([str(name), str(count)])

    return "\n".join(
        [
            "# PostgreSQL 迁移脚本索引",
            "",
            "本文件由 `scripts/generate-postgres-migration-inventory.py` 生成，用于跟踪 Docker 测试环境的 PostgreSQL 初始化和兼容 SQL。",
            "",
            "## 摘要",
            "",
            f"- 目录：`{inventory['migrationDirectory']}`。",
            f"- 脚本数量：`{inventory['migrationCount']}`。",
            f"- 编号范围：`{inventory['firstNumber']:03d}` 到 `{inventory['lastNumber']:03d}`。",
            f"- 缺失编号：`{inventory['missingNumbers']}`。",
            f"- 重复编号：`{inventory['duplicateNumbers']}`。",
            f"- 高风险语句：`{inventory['highRiskStatementCount']}`。",
            f"- 需关注语句：`{inventory['watchStatementCount']}`。",
            "- 机器可读清单：`metadata/postgres-migration-inventory.json`。",
            "",
            "## 标签统计",
            "",
            table(tag_rows),
            "",
            "## 语句统计",
            "",
            table(statement_rows),
            "",
            "## 脚本清单",
            "",
            table(migration_rows),
            "",
            "## 规则",
            "",
            "- 文件名必须是 `NNN-lowercase-slug.sql`。",
            "- 编号必须连续、唯一，新增脚本追加到末尾。",
            "- 默认脚本必须可重复执行，优先使用 `IF EXISTS` / `IF NOT EXISTS` / `ON CONFLICT` / `DO $$` 保护。",
            "- `DROP DATABASE`、`DROP SCHEMA`、`DROP TABLE`、`TRUNCATE` 属于高风险语句，会导致 `make postgres-migration-check` 失败。",
            "- `DROP VIEW`、`DROP FUNCTION`、`DROP TRIGGER`、`DROP OPERATOR`、`DELETE FROM` 会进入 watch 清单，必须在 PR 中解释原因。",
            "",
        ]
    )


def write_outputs(inventory: dict[str, object]) -> None:
    OUTPUT_JSON.write_text(json.dumps(inventory, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    OUTPUT_MD.write_text(render_markdown(inventory), encoding="utf-8")


def check_invariants(inventory: dict[str, object]) -> list[str]:
    failures: list[str] = []
    if inventory["invalidNames"]:
        failures.append(f"invalid migration names: {inventory['invalidNames']}")
    if inventory["missingNumbers"]:
        failures.append(f"missing migration numbers: {inventory['missingNumbers']}")
    if inventory["duplicateNumbers"]:
        failures.append(f"duplicate migration numbers: {inventory['duplicateNumbers']}")
    if inventory["highRiskStatementCount"]:
        failures.append(f"high-risk statements found: {inventory['highRiskStatementCount']}")
    return failures


def check_outputs(inventory: dict[str, object]) -> int:
    failures = check_invariants(inventory)
    expected_json = json.dumps(inventory, ensure_ascii=False, indent=2) + "\n"
    expected_md = render_markdown(inventory)
    if not OUTPUT_JSON.exists() or OUTPUT_JSON.read_text(encoding="utf-8") != expected_json:
        failures.append(str(OUTPUT_JSON.relative_to(ROOT)) + " is stale")
    if not OUTPUT_MD.exists() or OUTPUT_MD.read_text(encoding="utf-8") != expected_md:
        failures.append(str(OUTPUT_MD.relative_to(ROOT)) + " is stale")
    if failures:
        for failure in failures:
            print("FAIL:", failure, file=sys.stderr)
        print("Run: make postgres-migration-index", file=sys.stderr)
        return 1
    print("PostgreSQL migration inventory is current.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate PostgreSQL migration inventory.")
    parser.add_argument("--check", action="store_true", help="Check invariants and generated outputs without writing.")
    args = parser.parse_args()

    inventory = build_inventory()
    if args.check:
        return check_outputs(inventory)
    failures = check_invariants(inventory)
    if failures:
        for failure in failures:
            print("WARN:", failure, file=sys.stderr)
    write_outputs(inventory)
    print(f"Wrote {OUTPUT_JSON.relative_to(ROOT)}")
    print(f"Wrote {OUTPUT_MD.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
