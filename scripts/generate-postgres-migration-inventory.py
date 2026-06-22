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
OUTPUT_WATCH_MD = ROOT / "docs/postgres-migration-watch-rationale.md"

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
WATCH_RATIONALES = {
    "delete": "Allowed only for scoped cleanup statements with an explicit WHERE clause; never for full-table cleanup.",
    "drop-view": "Allowed only with IF EXISTS when recreating compatibility views idempotently.",
    "drop-function": "Allowed only with IF EXISTS when replacing compatibility helper functions idempotently.",
    "drop-trigger": "Allowed only with IF EXISTS when replacing trigger definitions idempotently.",
    "drop-operator": "Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently.",
    "drop-aggregate": "Allowed only with IF EXISTS when replacing PostgreSQL compatibility aggregates idempotently.",
}
IDEMPOTENCY_SIGNAL_PATTERNS = {
    "if-exists": re.compile(r"\bif\s+exists\b", re.IGNORECASE),
    "if-not-exists": re.compile(r"\bif\s+not\s+exists\b", re.IGNORECASE),
    "on-conflict": re.compile(r"\bon\s+conflict\b", re.IGNORECASE),
    "do-block": re.compile(r"\bdo\s+\$\$", re.IGNORECASE),
    "to-regclass": re.compile(r"\bto_regclass\s*\(", re.IGNORECASE),
    "create-or-replace": re.compile(r"\bcreate\s+or\s+replace\b", re.IGNORECASE),
    "where-not-exists": re.compile(r"\bwhere\s+not\s+exists\b", re.IGNORECASE),
}
STRUCTURAL_GUARD_PATTERNS = {
    "create-table": re.compile(r"\bcreate\s+table\b", re.IGNORECASE),
    "create-index": re.compile(r"\bcreate\s+(?:unique\s+)?index\b", re.IGNORECASE),
    "alter-add-column": re.compile(
        r"\balter\s+table\b(?:(?!;).)*\badd\s+column\b",
        re.IGNORECASE | re.DOTALL,
    ),
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


def statement_from_line(text_lines: list[str], start_line: int) -> str:
    statement_lines: list[str] = []
    for line in text_lines[start_line - 1 :]:
        statement_lines.append(line)
        if ";" in line:
            break
        if len(statement_lines) >= 80:
            break
    return "\n".join(statement_lines)


def context_before(text_lines: list[str], start_line: int, line_count: int = 12) -> str:
    start = max(0, start_line - line_count - 1)
    return "\n".join(text_lines[start : start_line - 1])


def assess_structural_guard(pattern: str, statement: str, context: str) -> dict[str, object]:
    normalized = re.sub(r"\s+", " ", statement).strip().lower()
    normalized_context = re.sub(r"\s+", " ", context).strip().lower()

    if pattern == "create-table":
        guarded = (
            "create table if not exists" in normalized
            or re.search(r"\bif\s+to_regclass\s*\([^)]*\)\s+is\s+null\b", normalized_context)
            or re.search(r"\bif\s+rel_kind\s+is\s+null\b", normalized_context)
            or re.search(r"\bif\s+not\s+exists\s*\(", normalized_context)
        )
        return {
            "safe": bool(guarded),
            "safety": "guarded-create-table" if guarded else "unguarded-create-table",
            "issue": "" if guarded else "CREATE TABLE must use IF NOT EXISTS or an explicit catalog guard.",
        }

    if pattern == "create-index":
        guarded = "if not exists" in normalized
        return {
            "safe": guarded,
            "safety": "guarded-create-index" if guarded else "unguarded-create-index",
            "issue": "" if guarded else "CREATE INDEX must use IF NOT EXISTS.",
        }

    if pattern == "alter-add-column":
        guarded = (
            "add column if not exists" in normalized
            or re.search(r"\bif\s+not\s+exists\s*\(", normalized_context)
        )
        return {
            "safe": bool(guarded),
            "safety": "guarded-add-column" if guarded else "unguarded-add-column",
            "issue": "" if guarded else "ALTER TABLE ADD COLUMN must use IF NOT EXISTS or an explicit catalog guard.",
        }

    return {
        "safe": False,
        "safety": "unknown-structural-pattern",
        "issue": f"Unknown structural pattern: {pattern}.",
    }


def assess_watch_statement(pattern: str, statement: str) -> dict[str, object]:
    normalized = re.sub(r"\s+", " ", statement).strip().lower()
    if pattern.startswith("drop-"):
        safe = " if exists " in f" {normalized} "
        return {
            "safe": safe,
            "safety": "guarded-if-exists" if safe else "missing-if-exists",
            "issue": "" if safe else "DROP watch statements must include IF EXISTS.",
            "rationale": WATCH_RATIONALES[pattern],
        }
    if pattern == "delete":
        safe = " where " in f" {normalized} "
        return {
            "safe": safe,
            "safety": "scoped-delete" if safe else "missing-where",
            "issue": "" if safe else "DELETE watch statements must include a WHERE clause.",
            "rationale": WATCH_RATIONALES[pattern],
        }
    return {
        "safe": False,
        "safety": "unknown-watch-pattern",
        "issue": f"Unknown watch pattern: {pattern}.",
        "rationale": "No rationale registered for this watch pattern.",
    }


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
    unguarded_structural: list[dict[str, object]] = []
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
                statement = statement_from_line(text_lines, line_number)
                assessment = assess_watch_statement(name, statement)
                watch.append(
                    {
                        "line": line_number,
                        "pattern": name,
                        "excerpt": compact(line),
                        "statement": compact(statement),
                        "safe": assessment["safe"],
                        "safety": assessment["safety"],
                        "issue": assessment["issue"],
                        "rationale": assessment["rationale"],
                    }
                )
        for name, regex in STRUCTURAL_GUARD_PATTERNS.items():
            if regex.search(line):
                statement = statement_from_line(text_lines, line_number)
                context = context_before(text_lines, line_number)
                assessment = assess_structural_guard(name, statement, context)
                if not assessment["safe"]:
                    unguarded_structural.append(
                        {
                            "line": line_number,
                            "pattern": name,
                            "excerpt": compact(line),
                            "statement": compact(statement),
                            "safety": assessment["safety"],
                            "issue": assessment["issue"],
                        }
                    )
    idempotency_signals = {
        name: len(regex.findall(text))
        for name, regex in IDEMPOTENCY_SIGNAL_PATTERNS.items()
    }
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
        "idempotencySignals": idempotency_signals,
        "highRiskStatements": high_risk,
        "watchStatements": watch,
        "unguardedStructuralStatements": unguarded_structural,
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
    unguarded_structural_count = sum(
        len(item["unguardedStructuralStatements"]) for item in migrations
    )
    watch_count = sum(len(item["watchStatements"]) for item in migrations)
    watch_safety_issue_count = sum(
        1
        for item in migrations
        for statement in item["watchStatements"]
        if not statement.get("safe")
    )
    statement_totals: Counter[str] = Counter()
    idempotency_signal_totals: Counter[str] = Counter()
    tag_totals: Counter[str] = Counter()
    for item in migrations:
        statement_totals.update(item["statementCounts"])  # type: ignore[arg-type]
        idempotency_signal_totals.update(item["idempotencySignals"])  # type: ignore[arg-type]
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
        "unguardedStructuralStatementCount": unguarded_structural_count,
        "watchStatementCount": watch_count,
        "watchSafetyIssueCount": watch_safety_issue_count,
        "statementTotals": dict(sorted(statement_totals.items())),
        "idempotencySignalTotals": dict(sorted(idempotency_signal_totals.items())),
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
    migration_rows = [["No.", "File", "Tags", "Lines", "DDL/DML Summary", "Idempotency", "Watch"]]
    for item in inventory["migrations"]:  # type: ignore[index]
        counts = item["statementCounts"]
        summary = ", ".join(
            f"{name}:{count}"
            for name, count in counts.items()
            if count
        ) or "-"
        watch_count = len(item["watchStatements"])
        risk_count = len(item["highRiskStatements"])
        unguarded_count = len(item["unguardedStructuralStatements"])
        idempotency_count = sum(int(count) for count in item["idempotencySignals"].values())
        watch = []
        if risk_count:
            watch.append(f"high-risk:{risk_count}")
        if unguarded_count:
            watch.append(f"unguarded-structural:{unguarded_count}")
        if watch_count:
            watch.append(f"watch:{watch_count}")
        migration_rows.append(
            [
                f"{item['number']:03d}" if item["number"] is not None else "?",
                str(item["name"]),
                ", ".join(item["tags"]),
                str(item["lineCount"]),
                summary,
                str(idempotency_count),
                ", ".join(watch) or "-",
            ]
        )

    tag_rows = [["Tag", "Count"]]
    for tag, count in inventory["tagTotals"].items():  # type: ignore[union-attr]
        tag_rows.append([str(tag), str(count)])

    statement_rows = [["Statement", "Count"]]
    for name, count in inventory["statementTotals"].items():  # type: ignore[union-attr]
        statement_rows.append([str(name), str(count)])

    idempotency_rows = [["Signal", "Count"]]
    for name, count in inventory["idempotencySignalTotals"].items():  # type: ignore[union-attr]
        idempotency_rows.append([str(name), str(count)])

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
            f"- 未保护结构语句：`{inventory['unguardedStructuralStatementCount']}`。",
            f"- 需关注语句：`{inventory['watchStatementCount']}`。",
            f"- 需关注语句安全问题：`{inventory['watchSafetyIssueCount']}`。",
            "- 机器可读清单：`metadata/postgres-migration-inventory.json`。",
            "- 需关注语句说明：`docs/postgres-migration-watch-rationale.md`。",
            "",
            "## 标签统计",
            "",
            table(tag_rows),
            "",
            "## 语句统计",
            "",
            table(statement_rows),
            "",
            "## 幂等信号统计",
            "",
            table(idempotency_rows),
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
            "- `CREATE TABLE`、`CREATE INDEX`、`ALTER TABLE ... ADD COLUMN` 必须使用 `IF NOT EXISTS`，或位于显式 catalog guard 中；未保护结构语句会导致 `make postgres-migration-check` 失败。",
            "- `DROP DATABASE`、`DROP SCHEMA`、`DROP TABLE`、`TRUNCATE` 属于高风险语句，会导致 `make postgres-migration-check` 失败。",
            "- `DROP VIEW`、`DROP FUNCTION`、`DROP TRIGGER`、`DROP OPERATOR`、`DROP AGGREGATE`、`DELETE FROM` 会进入 watch 清单；drop-watch 必须带 `IF EXISTS`，delete-watch 必须带 `WHERE`，说明见 `docs/postgres-migration-watch-rationale.md`。",
            "",
        ]
    )


def render_watch_markdown(inventory: dict[str, object]) -> str:
    watch_rows = [["File", "Line", "Pattern", "Safety", "Rationale", "Statement"]]
    for item in inventory["migrations"]:  # type: ignore[index]
        for statement in item["watchStatements"]:
            watch_rows.append(
                [
                    str(item["name"]),
                    str(statement["line"]),
                    str(statement["pattern"]),
                    str(statement["safety"]),
                    str(statement["rationale"]),
                    str(statement["statement"]).replace("|", "\\|"),
                ]
            )

    return "\n".join(
        [
            "# PostgreSQL Watch 语句说明",
            "",
            "本文件由 `scripts/generate-postgres-migration-inventory.py` 生成，用于解释 PostgreSQL init SQL 中允许但需要关注的 watch 语句。",
            "",
            "## 口径",
            "",
            "- `DROP DATABASE`、`DROP SCHEMA`、`DROP TABLE`、`TRUNCATE` 仍是阻断级高风险语句。",
            "- `DROP VIEW`、`DROP FUNCTION`、`DROP TRIGGER`、`DROP OPERATOR`、`DROP AGGREGATE` 只允许带 `IF EXISTS`，用于幂等重建兼容对象。",
            "- `DELETE FROM` 只允许带 `WHERE`，用于有范围的测试环境兼容数据清理或 pending 清理。",
            "- 这里的 PASS 只证明语句具备最低保护；业务影响仍需要在对应 SQL、验收报告或 PR 中解释。",
            "",
            "## 摘要",
            "",
            f"- Watch 语句：`{inventory['watchStatementCount']}`。",
            f"- Watch 安全问题：`{inventory['watchSafetyIssueCount']}`。",
            "",
            "## 语句清单",
            "",
            table(watch_rows),
            "",
        ]
    )


def write_outputs(inventory: dict[str, object]) -> None:
    OUTPUT_JSON.write_text(json.dumps(inventory, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    OUTPUT_MD.write_text(render_markdown(inventory), encoding="utf-8")
    OUTPUT_WATCH_MD.write_text(render_watch_markdown(inventory), encoding="utf-8")


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
    if inventory["unguardedStructuralStatementCount"]:
        failures.append(
            f"unguarded structural statements found: {inventory['unguardedStructuralStatementCount']}"
        )
        for item in inventory["migrations"]:
            for statement in item["unguardedStructuralStatements"]:
                failures.append(
                    f"{item['name']}:{statement['line']} {statement['pattern']} {statement['issue']}"
                )
    if inventory["watchSafetyIssueCount"]:
        failures.append(f"watch statement safety issues found: {inventory['watchSafetyIssueCount']}")
        for item in inventory["migrations"]:
            for statement in item["watchStatements"]:
                if not statement.get("safe"):
                    failures.append(
                        f"{item['name']}:{statement['line']} {statement['pattern']} {statement['issue']}"
                    )
    return failures


def check_outputs(inventory: dict[str, object]) -> int:
    failures = check_invariants(inventory)
    expected_json = json.dumps(inventory, ensure_ascii=False, indent=2) + "\n"
    expected_md = render_markdown(inventory)
    expected_watch_md = render_watch_markdown(inventory)
    if not OUTPUT_JSON.exists() or OUTPUT_JSON.read_text(encoding="utf-8") != expected_json:
        failures.append(str(OUTPUT_JSON.relative_to(ROOT)) + " is stale")
    if not OUTPUT_MD.exists() or OUTPUT_MD.read_text(encoding="utf-8") != expected_md:
        failures.append(str(OUTPUT_MD.relative_to(ROOT)) + " is stale")
    if not OUTPUT_WATCH_MD.exists() or OUTPUT_WATCH_MD.read_text(encoding="utf-8") != expected_watch_md:
        failures.append(str(OUTPUT_WATCH_MD.relative_to(ROOT)) + " is stale")
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
    print(f"Wrote {OUTPUT_WATCH_MD.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
