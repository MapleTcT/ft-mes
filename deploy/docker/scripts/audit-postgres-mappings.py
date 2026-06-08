#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import json
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


DEFAULT_EXTENSIONS = {".xml", ".sql"}
IGNORED_PARTS = {
    ".git",
    "__pycache__",
    "node_modules",
    "target",
    "dist",
    "metadata",
}
REFERENCE_DB_PARTS = {"oracle", "mysql", "mariadb", "sqlserver"}
POSTGRES_PARTS = {"postgresql", "postgres"}


@dataclass(frozen=True)
class PatternSpec:
    name: str
    regex: re.Pattern[str]
    severity: str = "error"


PATTERNS = [
    PatternSpec("oracle-rownum", re.compile(r"\brownum\b", re.IGNORECASE)),
    PatternSpec("oracle-sysdate", re.compile(r"\bsysdate\b", re.IGNORECASE)),
    PatternSpec("oracle-systimestamp", re.compile(r"\bsystimestamp\b", re.IGNORECASE)),
    PatternSpec("oracle-to-date", re.compile(r"\bto_date\s*\(", re.IGNORECASE)),
    PatternSpec("oracle-to-char", re.compile(r"\bto_char\s*\(", re.IGNORECASE)),
    PatternSpec("oracle-nvl", re.compile(r"\bnvl\s*\(", re.IGNORECASE)),
    PatternSpec("oracle-decode", re.compile(r"\bdecode\s*\(", re.IGNORECASE)),
    PatternSpec("oracle-dual", re.compile(r"\bfrom\s+dual\b|\bdual\b", re.IGNORECASE)),
    PatternSpec("oracle-varchar2", re.compile(r"\bvarchar2\s*\(", re.IGNORECASE)),
    PatternSpec("oracle-number", re.compile(r"\bnumber\s*\(", re.IGNORECASE)),
    PatternSpec("mysql-ifnull", re.compile(r"\bifnull\s*\(", re.IGNORECASE)),
    PatternSpec("mysql-date-format", re.compile(r"\bdate_format\s*\(", re.IGNORECASE)),
    PatternSpec("mysql-str-to-date", re.compile(r"\bstr_to_date\s*\(", re.IGNORECASE)),
    PatternSpec("mysql-group-concat", re.compile(r"\bgroup_concat\s*\(", re.IGNORECASE)),
    PatternSpec("mysql-find-in-set", re.compile(r"\bfind_in_set\s*\(", re.IGNORECASE)),
    PatternSpec("mysql-on-duplicate", re.compile(r"\bon\s+duplicate\s+key\s+update\b", re.IGNORECASE)),
    PatternSpec("mysql-insert-ignore", re.compile(r"\binsert\s+ignore\b", re.IGNORECASE)),
    PatternSpec("mysql-zero-date", re.compile(r"0000-00-00(?:\s+00:00:00)?", re.IGNORECASE)),
    PatternSpec("mysql-backtick", re.compile(r"`[^`]+`")),
    PatternSpec("mysql-alter-modify", re.compile(r"\balter\s+table\b[^;]*\bmodify\b", re.IGNORECASE | re.DOTALL)),
    PatternSpec("mysql-engine", re.compile(r"\bengine\s*=", re.IGNORECASE)),
    PatternSpec("sqlserver-top", re.compile(r"\bselect\s+top\s+\d+", re.IGNORECASE)),
    PatternSpec("sqlserver-isnull", re.compile(r"\bisnull\s*\(", re.IGNORECASE)),
]


def should_skip_path(path: Path, include_reference: bool) -> bool:
    parts = set(path.parts)
    if parts & IGNORED_PARTS:
        return True
    if not include_reference and parts & REFERENCE_DB_PARTS:
        return True
    return False


def iter_source_files(root: Path, include_reference: bool, include_java: bool) -> Iterable[Path]:
    extensions = DEFAULT_EXTENSIONS | ({".java"} if include_java else set())
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        if path.suffix.lower() not in extensions:
            continue
        if should_skip_path(path, include_reference):
            continue
        yield path


def path_scope(path: Path) -> str:
    parts = set(path.parts)
    if parts & POSTGRES_PARTS:
        return "postgresql"
    if parts & REFERENCE_DB_PARTS:
        return "reference"
    return "runtime-default"


def decode(data: bytes) -> str | None:
    for encoding in ("utf-8", "gb18030", "latin-1"):
        try:
            return data.decode(encoding)
        except UnicodeDecodeError:
            continue
    return None


def compact_line(line: str) -> str:
    return re.sub(r"\s+", " ", line).strip()[:240]


def scan_text(name: str, text: str, max_hits_per_file: int) -> list[dict[str, object]]:
    findings: list[dict[str, object]] = []
    lines = text.splitlines()
    for spec in PATTERNS:
        hit_count = 0
        for index, line in enumerate(lines, start=1):
            if spec.regex.search(line):
                findings.append(
                    {
                        "file": name,
                        "line": index,
                        "pattern": spec.name,
                        "severity": spec.severity,
                        "excerpt": compact_line(line),
                    }
                )
                hit_count += 1
                if hit_count >= max_hits_per_file:
                    break
    return findings


def is_loadable_jar_entry(entry: str) -> bool:
    path = Path(entry)
    parts = path.parts
    lower = entry.lower()
    if path.suffix.lower() not in {".xml", ".sql"}:
        return False
    if lower.startswith("meta-inf/maven/"):
        return False
    if lower.startswith("meta-inf/postgresql/"):
        return True
    if "postgresql" in parts or "postgres" in parts:
        return True
    if path.suffix.lower() == ".xml" and parts and parts[0] in {"mappers", "mapper"}:
        return True
    return False


def scan_file(path: Path, root: Path, max_hits_per_file: int) -> tuple[list[dict[str, object]], list[str]]:
    data = path.read_bytes()
    text = decode(data)
    if text is None:
        return [], [str(path.relative_to(root))]
    relative = str(path.relative_to(root))
    findings = scan_text(relative, text, max_hits_per_file)
    for item in findings:
        item["scope"] = path_scope(path)
    return findings, []


def scan_jar(jar: Path, max_hits_per_file: int) -> tuple[list[dict[str, object]], list[str]]:
    findings: list[dict[str, object]] = []
    undecodable: list[str] = []
    with zipfile.ZipFile(jar, "r") as outer:
        for nested_name in outer.namelist():
            if not (nested_name.startswith("BOOT-INF/lib/") and nested_name.endswith(".jar")):
                continue
            try:
                nested_data = outer.read(nested_name)
                with zipfile.ZipFile(io.BytesIO(nested_data), "r") as nested:
                    for entry in nested.namelist():
                        if not is_loadable_jar_entry(entry):
                            continue
                        parts = set(Path(entry).parts)
                        if parts & REFERENCE_DB_PARTS:
                            continue
                        text = decode(nested.read(entry))
                        display = f"{jar.name}!{nested_name}!{entry}"
                        if text is None:
                            undecodable.append(display)
                            continue
                        entry_findings = scan_text(display, text, max_hits_per_file)
                        for item in entry_findings:
                            item["scope"] = "jar-postgresql" if parts & POSTGRES_PARTS else "jar-runtime-default"
                        findings.extend(entry_findings)
            except zipfile.BadZipFile:
                continue
    return findings, undecodable


def summarize(findings: list[dict[str, object]], undecodable: list[str]) -> dict[str, object]:
    by_pattern: dict[str, int] = {}
    by_scope: dict[str, int] = {}
    by_file: dict[str, int] = {}
    for item in findings:
        by_pattern[str(item["pattern"])] = by_pattern.get(str(item["pattern"]), 0) + 1
        by_scope[str(item["scope"])] = by_scope.get(str(item["scope"]), 0) + 1
        by_file[str(item["file"])] = by_file.get(str(item["file"]), 0) + 1
    return {
        "findingCount": len(findings),
        "undecodableCount": len(undecodable),
        "byPattern": dict(sorted(by_pattern.items())),
        "byScope": dict(sorted(by_scope.items())),
        "topFiles": [
            {"file": file, "findingCount": count}
            for file, count in sorted(by_file.items(), key=lambda item: (-item[1], item[0]))[:50]
        ],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Audit ADP mapper and SQL files for non-PostgreSQL syntax.")
    parser.add_argument("paths", nargs="+", type=Path, help="Directories or Spring Boot JAR files to audit.")
    parser.add_argument("--include-reference", action="store_true", help="Also scan oracle/mysql/mariadb/sqlserver reference folders.")
    parser.add_argument("--include-java", action="store_true", help="Also scan recovered Java sources. This is intentionally noisy.")
    parser.add_argument("--max-hits-per-file", type=int, default=5)
    parser.add_argument("--report", type=Path, help="Optional JSON report path.")
    args = parser.parse_args()

    findings: list[dict[str, object]] = []
    undecodable: list[str] = []
    for input_path in args.paths:
        path = input_path.resolve()
        if path.is_dir():
            for source in iter_source_files(path, args.include_reference, args.include_java):
                file_findings, file_undecodable = scan_file(source, path, args.max_hits_per_file)
                findings.extend(file_findings)
                undecodable.extend(file_undecodable)
        elif path.is_file() and path.suffix.lower() == ".jar":
            jar_findings, jar_undecodable = scan_jar(path, args.max_hits_per_file)
            findings.extend(jar_findings)
            undecodable.extend(jar_undecodable)
        else:
            raise SystemExit(f"Unsupported path: {input_path}")

    output = {
        "summary": summarize(findings, undecodable),
        "findings": findings,
        "undecodable": undecodable[:100],
    }
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(output["summary"], ensure_ascii=False, indent=2))
    if findings:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
