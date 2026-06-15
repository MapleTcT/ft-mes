#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_JSON = ROOT / "metadata/oracle-migration-audit.json"
OUTPUT_MD = ROOT / "docs/oracle-migration-backlog.md"

SKIP_PREFIXES = (
    ".git/",
    "metadata/",
    "docs/oracle-migration-backlog.md",
)

TEXT_SUFFIXES = {
    "",
    ".conf",
    ".ftl",
    ".java",
    ".js",
    ".json",
    ".md",
    ".properties",
    ".py",
    ".sql",
    ".txt",
    ".xml",
    ".yml",
    ".yaml",
}

PATTERNS = {
    "oracle-keyword": re.compile(r"\boracle\b", re.IGNORECASE),
    "ojdbc": re.compile(r"\bojdbc\d*\b|com\.oracle\.jdbc|com\.oracle\.database\.jdbc", re.IGNORECASE),
    "rownum": re.compile(r"\brownum\b", re.IGNORECASE),
    "sysdate": re.compile(r"\bsysdate\b|\bsystimestamp\b", re.IGNORECASE),
    "from-dual": re.compile(r"\bfrom\s+dual\b", re.IGNORECASE),
    "nvl": re.compile(r"\bnvl\s*\(", re.IGNORECASE),
    "decode-sql": re.compile(r"\bdecode\s*\(", re.IGNORECASE),
}


def git_files() -> list[str]:
    result = subprocess.run(
        ["git", "ls-files"],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    )
    return [line for line in result.stdout.splitlines() if line]


def iter_text_files() -> Iterable[Path]:
    for relative in git_files():
        if relative.startswith(SKIP_PREFIXES):
            continue
        path = ROOT / relative
        if not path.is_file():
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        yield path


def read_text(path: Path) -> str | None:
    for encoding in ("utf-8", "gb18030", "latin-1"):
        try:
            return path.read_text(encoding=encoding)
        except UnicodeDecodeError:
            continue
    return None


def compact(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()[:220]


def classify(relative: str, pattern: str, line: str) -> tuple[str, str]:
    lower = relative.lower()
    line_lower = line.lower()

    if relative in {
        "deploy/docker/.env.oracle-legacy.example",
        "pom.xml",
    }:
        return "allowed-legacy-contract", "Oracle is explicit legacy compatibility, not the default runtime path."
    if relative == "Makefile":
        return "tooling-or-audit-code", "Tooling may mention Oracle to generate or check migration audit outputs."
    if lower in {"agents.md", "readme.md"} or lower.startswith((".github/", "docs/")) or lower.endswith("/readme.md"):
        return "documentation-or-workflow", "Documentation/template reference; keep wording aligned with PostgreSQL-first policy."
    if lower.startswith("frontend/") and pattern == "rownum":
        return "frontend-row-index-noise", "Frontend rowNum variable naming is not Oracle SQL ROWNUM."
    if lower.startswith("scripts/"):
        return "tooling-or-audit-code", "Tooling may mention Oracle to detect or convert legacy references."
    if lower.startswith("deploy/docker/scripts/"):
        return "postgres-conversion-tooling", "Runtime conversion script; Oracle references should convert away from Oracle defaults."
    if lower.startswith("deploy/docker/patches/"):
        return "runtime-patch-backlog", "Runtime patch still contains Oracle branch logic that should be retired after source promotion."
    if lower.startswith("deploy/nacos-config/"):
        return "runtime-config-backlog", "Source Nacos config still carries Oracle fallback; rendered Docker config must override to PostgreSQL."
    if "/meta-inf/oracle/" in lower or "/mappers/oracle/" in lower or "/mapper/oracle/" in lower:
        return "legacy-oracle-sql-resource", "Recovered Oracle SQL/mapper resource; keep as reference until PostgreSQL module migration is complete."
    if lower.endswith("/pom.xml") and ("ojdbc" in line_lower or "oracle.jdbc" in line_lower):
        return "legacy-ojdbc-dependency", "Recovered module POM declares Oracle JDBC and needs module-level replacement."
    if lower.startswith("backend/modules/"):
        return "recovered-source-backlog", "Recovered source contains Oracle-specific branch or keyword; verify during module promotion."
    if lower.startswith("backend/decompiled-services/"):
        return "decompiled-runtime-backlog", "Decompiled runtime config/source contains Oracle-specific branch or keyword."
    if lower.startswith("deploy/docker/postgres/init/"):
        return "postgres-compat-reference", "PostgreSQL compatibility SQL may mention Oracle as source context."
    return "unclassified-oracle-reference", "Review and classify this reference."


def scan() -> dict[str, object]:
    findings: list[dict[str, object]] = []
    for path in iter_text_files():
        text = read_text(path)
        if text is None:
            continue
        relative = str(path.relative_to(ROOT))
        for line_number, line in enumerate(text.splitlines(), start=1):
            for name, regex in PATTERNS.items():
                if name == "decode-sql" and path.suffix.lower() not in {".sql", ".xml"}:
                    continue
                if not regex.search(line):
                    continue
                category, note = classify(relative, name, line)
                findings.append(
                    {
                        "file": relative,
                        "line": line_number,
                        "pattern": name,
                        "category": category,
                        "note": note,
                        "excerpt": compact(line),
                    }
                )
                break

    category_counts = Counter(str(item["category"]) for item in findings)
    pattern_counts = Counter(str(item["pattern"]) for item in findings)
    file_counts = Counter(str(item["file"]) for item in findings)
    return {
        "schemaVersion": 1,
        "findingCount": len(findings),
        "categoryCounts": dict(sorted(category_counts.items())),
        "patternCounts": dict(sorted(pattern_counts.items())),
        "topFiles": [
            {"file": file, "findingCount": count}
            for file, count in file_counts.most_common(30)
        ],
        "findings": findings,
    }


def table(rows: list[list[str]]) -> str:
    if not rows:
        return ""
    lines = [
        "| " + " | ".join(rows[0]) + " |",
        "| " + " | ".join(["---"] * len(rows[0])) + " |",
    ]
    lines.extend("| " + " | ".join(row) + " |" for row in rows[1:])
    return "\n".join(lines)


def render_markdown(report: dict[str, object]) -> str:
    category_rows = [["Category", "Count", "Meaning"]]
    notes: dict[str, str] = {}
    for finding in report["findings"]:  # type: ignore[index]
        notes.setdefault(str(finding["category"]), str(finding["note"]))
    for category, count in report["categoryCounts"].items():  # type: ignore[union-attr]
        category_rows.append([str(category), str(count), notes.get(str(category), "")])

    file_rows = [["File", "Findings"]]
    for item in report["topFiles"]:  # type: ignore[index]
        file_rows.append([str(item["file"]), str(item["findingCount"])])

    backlog_rows = [["File", "Line", "Category", "Pattern", "Excerpt"]]
    backlog_categories = {
        "legacy-oracle-sql-resource",
        "legacy-ojdbc-dependency",
        "runtime-config-backlog",
        "runtime-patch-backlog",
        "recovered-source-backlog",
        "decompiled-runtime-backlog",
        "unclassified-oracle-reference",
    }
    for finding in report["findings"]:  # type: ignore[index]
        if str(finding["category"]) not in backlog_categories:
            continue
        backlog_rows.append(
            [
                str(finding["file"]),
                str(finding["line"]),
                str(finding["category"]),
                str(finding["pattern"]),
                str(finding["excerpt"]).replace("|", "\\|"),
            ]
        )
        if len(backlog_rows) >= 81:
            break

    return "\n".join(
        [
            "# Oracle 迁移 Backlog",
            "",
            "本文件由 `scripts/generate-oracle-migration-audit.py` 生成，用来跟踪仓库内仍然出现的 Oracle/ojdbc/Oracle 方言引用。",
            "",
            "注意：本报告不是说所有引用都要立刻删除。`documentation-or-workflow`、`tooling-or-audit-code`、`allowed-legacy-contract` 属于可解释引用；`*-backlog` 和 `legacy-*` 是后续模块迁移时要逐步消化的项。",
            "",
            "## 摘要",
            "",
            f"- 总引用数：`{report['findingCount']}`。",
            "- 默认运行路径仍以 PostgreSQL 为准；Oracle 只能作为显式 legacy 路径。",
            "- 机器可读报告：`metadata/oracle-migration-audit.json`。",
            "",
            "## 分类统计",
            "",
            table(category_rows),
            "",
            "## 高频文件",
            "",
            table(file_rows),
            "",
            "## 优先 Backlog 样例",
            "",
            table(backlog_rows),
            "",
            "## 处理规则",
            "",
            "- `legacy-oracle-sql-resource`：保留作原厂参考，迁移模块时必须补 PostgreSQL 主路径。",
            "- `legacy-ojdbc-dependency`：模块提升到 `backend/source-modules` 时移除直接 ojdbc 依赖。",
            "- `runtime-config-backlog`：Nacos 源配置里的 Oracle 默认值要继续由渲染脚本转成 PostgreSQL，并逐步改源配置。",
            "- `runtime-patch-backlog`：当前 runtime patch 为了兼容老包可存在，源码提升后应删除 Oracle 分支。",
            "- `recovered-source-backlog` / `decompiled-runtime-backlog`：后端专项线程按模块确认是否是真 SQL、枚举、配置还是误报。",
            "",
        ]
    )


def write_outputs(report: dict[str, object]) -> None:
    OUTPUT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    OUTPUT_MD.write_text(render_markdown(report), encoding="utf-8")


def check_outputs(report: dict[str, object]) -> int:
    expected_json = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    expected_md = render_markdown(report)
    stale = []
    if not OUTPUT_JSON.exists() or OUTPUT_JSON.read_text(encoding="utf-8") != expected_json:
        stale.append(str(OUTPUT_JSON.relative_to(ROOT)))
    if not OUTPUT_MD.exists() or OUTPUT_MD.read_text(encoding="utf-8") != expected_md:
        stale.append(str(OUTPUT_MD.relative_to(ROOT)))
    if stale:
        print("Oracle migration audit outputs are stale:", ", ".join(stale), file=sys.stderr)
        print("Run: make oracle-audit", file=sys.stderr)
        return 1
    print("Oracle migration audit outputs are current.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate Oracle-to-PostgreSQL migration backlog.")
    parser.add_argument("--check", action="store_true", help="Check generated outputs without writing.")
    args = parser.parse_args()

    report = scan()
    if args.check:
        return check_outputs(report)
    write_outputs(report)
    print(f"Wrote {OUTPUT_JSON.relative_to(ROOT)}")
    print(f"Wrote {OUTPUT_MD.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
