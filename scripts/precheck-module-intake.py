#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
import zipfile
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
MAX_TEXT_BYTES = 2 * 1024 * 1024

TEXT_SUFFIXES = {
    "",
    ".conf",
    ".ftl",
    ".java",
    ".json",
    ".properties",
    ".sql",
    ".txt",
    ".xml",
    ".yaml",
    ".yml",
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

ERROR_PATTERNS = {
    "oracle-jdbc-url": re.compile(r"jdbc:oracle:", re.IGNORECASE),
    "oracle-driver": re.compile(r"\boracle\.jdbc(?:\.driver)?\b|\bcom\.oracle\.database\.jdbc\b", re.IGNORECASE),
    "oracle-db-default": re.compile(
        r"\b(?:db|database)[_.-]?type\s*[:=]\s*oracle\b"
        r"|\bdbType\s*[:=]\s*oracle\b"
        r"|<\s*(?:db[-_]?type|dbType|database[-_]?type)\s*>\s*oracle\s*<"
        r"|SUPOS_SYSTEM_DB_TYPE\s*=\s*oracle\b",
        re.IGNORECASE,
    ),
    "oracle-hibernate-dialect": re.compile(r"org\.hibernate\.dialect\.Oracle", re.IGNORECASE),
}

WARNING_PATTERNS = {
    "from-dual": re.compile(r"\bfrom\s+dual\b", re.IGNORECASE),
    "rownum": re.compile(r"\brownum\b", re.IGNORECASE),
    "sysdate": re.compile(r"\bsysdate\b|\bsystimestamp\b", re.IGNORECASE),
    "nvl": re.compile(r"\bnvl\s*\(", re.IGNORECASE),
    "decode": re.compile(r"\bdecode\s*\(", re.IGNORECASE),
    "mysql-only": re.compile(r"`[^`]+`|\bifnull\s*\(|\bgroup_concat\s*\(", re.IGNORECASE),
}

ORACLE_RESOURCE_PATH_MARKERS = (
    "/meta-inf/oracle/",
    "/mapper/oracle/",
    "/mappers/oracle/",
)


@dataclass(frozen=True)
class CandidateFile:
    name: str
    size: int
    data: bytes | None


def compact(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()[:220]


def relative_display(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def iter_directory(path: Path) -> Iterable[CandidateFile]:
    for item in sorted(path.rglob("*")):
        if not item.is_file():
            continue
        rel = str(item.relative_to(path))
        size = item.stat().st_size
        data = None
        if size <= MAX_TEXT_BYTES and item.suffix.lower() in TEXT_SUFFIXES:
            data = item.read_bytes()
        yield CandidateFile(rel, size, data)


def iter_zip(path: Path) -> Iterable[CandidateFile]:
    with zipfile.ZipFile(path) as archive:
        for info in sorted(archive.infolist(), key=lambda value: value.filename):
            if info.is_dir():
                continue
            suffix = Path(info.filename).suffix.lower()
            data = None
            if info.file_size <= MAX_TEXT_BYTES and suffix in TEXT_SUFFIXES:
                data = archive.read(info)
            yield CandidateFile(info.filename, info.file_size, data)


def iter_candidate_files(path: Path) -> tuple[str, Iterable[CandidateFile]]:
    if path.is_dir():
        return "directory", iter_directory(path)
    if path.is_file() and zipfile.is_zipfile(path):
        return "archive", iter_zip(path)
    if path.is_file():
        size = path.stat().st_size
        data = path.read_bytes() if size <= MAX_TEXT_BYTES and path.suffix.lower() in TEXT_SUFFIXES else None
        return "file", [CandidateFile(path.name, size, data)]
    raise SystemExit(f"Input path does not exist: {path}")


def read_text(data: bytes) -> str | None:
    for encoding in ("utf-8", "gb18030", "latin-1"):
        try:
            return data.decode(encoding)
        except UnicodeDecodeError:
            continue
    return None


def strip_namespace(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def child_text(element: ET.Element, name: str) -> str | None:
    for item in list(element):
        if strip_namespace(item.tag) == name:
            value = item.text.strip() if item.text else ""
            return value or None
    return None


def is_oracle_dependency(group_id: str | None, artifact_id: str | None) -> bool:
    group = (group_id or "").lower()
    artifact = (artifact_id or "").lower()
    return "oracle" in group or "oracle" in artifact or artifact.startswith("ojdbc")


def pom_dependency_findings(candidate: CandidateFile, text_value: str) -> list[dict[str, object]]:
    findings: list[dict[str, object]] = []
    try:
        root = ET.fromstring(text_value)
    except ET.ParseError as exc:
        findings.append(
            {
                "file": candidate.name,
                "line": None,
                "severity": "warning",
                "category": "pom-parse",
                "pattern": "invalid-pom",
                "excerpt": str(exc),
            }
        )
        return findings

    for dependency in root.iter():
        if strip_namespace(dependency.tag) != "dependency":
            continue
        group_id = child_text(dependency, "groupId")
        artifact_id = child_text(dependency, "artifactId")
        if not is_oracle_dependency(group_id, artifact_id):
            continue
        findings.append(
            {
                "file": candidate.name,
                "line": None,
                "severity": "error",
                "category": "oracle-dependency",
                "pattern": "oracle-maven-dependency",
                "excerpt": f"{group_id or '?'}:{artifact_id or '?'}",
            }
        )
    return findings


def scan_text(candidate: CandidateFile, text_value: str) -> list[dict[str, object]]:
    findings: list[dict[str, object]] = []
    is_pom = candidate.name.lower().endswith("/pom.xml") or candidate.name.lower() == "pom.xml"
    if is_pom:
        findings.extend(pom_dependency_findings(candidate, text_value))

    for line_number, line in enumerate(text_value.splitlines(), start=1):
        patterns = ERROR_PATTERNS
        if is_pom:
            patterns = {
                key: pattern
                for key, pattern in ERROR_PATTERNS.items()
                if key not in {"oracle-driver"}
            }
        for name, pattern in patterns.items():
            if pattern.search(line):
                findings.append(
                    {
                        "file": candidate.name,
                        "line": line_number,
                        "severity": "error",
                        "category": "oracle-default-path",
                        "pattern": name,
                        "excerpt": compact(line),
                    }
                )
                break
        for name, pattern in WARNING_PATTERNS.items():
            if pattern.search(line):
                findings.append(
                    {
                        "file": candidate.name,
                        "line": line_number,
                        "severity": "warning",
                        "category": "sql-dialect",
                        "pattern": name,
                        "excerpt": compact(line),
                    }
                )
                break
    return findings


def scan(path: Path) -> dict[str, object]:
    input_kind, candidates = iter_candidate_files(path)
    findings: list[dict[str, object]] = []
    stats = Counter()

    for candidate in candidates:
        suffix = Path(candidate.name).suffix.lower()
        normalized = "/" + candidate.name.lower().replace("\\", "/")
        stats["fileCount"] += 1
        stats["byteCount"] += candidate.size
        if suffix in BINARY_SUFFIXES:
            stats["binaryCount"] += 1
            findings.append(
                {
                    "file": candidate.name,
                    "line": None,
                    "severity": "warning",
                    "category": "binary-artifact",
                    "pattern": suffix.lstrip(".") or "binary",
                    "excerpt": "Runtime/package binary should not be copied into source modules.",
                }
            )
        if candidate.name.lower().endswith("pom.xml"):
            stats["pomCount"] += 1
        if suffix == ".java":
            stats["javaFileCount"] += 1
        if suffix == ".sql":
            stats["sqlFileCount"] += 1
        if suffix in {".xml", ".sql"}:
            stats["mapperOrSqlFileCount"] += 1

        for marker in ORACLE_RESOURCE_PATH_MARKERS:
            if marker in normalized:
                findings.append(
                    {
                        "file": candidate.name,
                        "line": None,
                        "severity": "error",
                        "category": "oracle-resource-path",
                        "pattern": marker.strip("/"),
                        "excerpt": "Oracle SQL/mapper resource must stay outside default source-module paths.",
                    }
                )
                break

        if candidate.data is None:
            continue
        text_value = read_text(candidate.data)
        if text_value is None:
            stats["undecodableTextCount"] += 1
            continue
        findings.extend(scan_text(candidate, text_value))

    severity_counts = Counter(str(item["severity"]) for item in findings)
    category_counts = Counter(str(item["category"]) for item in findings)
    pattern_counts = Counter(str(item["pattern"]) for item in findings)
    return {
        "schemaVersion": 1,
        "input": relative_display(path),
        "inputKind": input_kind,
        "summary": {
            "fileCount": stats["fileCount"],
            "byteCount": stats["byteCount"],
            "pomCount": stats["pomCount"],
            "javaFileCount": stats["javaFileCount"],
            "sqlFileCount": stats["sqlFileCount"],
            "mapperOrSqlFileCount": stats["mapperOrSqlFileCount"],
            "binaryCount": stats["binaryCount"],
            "findingCount": len(findings),
            "errorCount": severity_counts["error"],
            "warningCount": severity_counts["warning"],
        },
        "categoryCounts": dict(sorted(category_counts.items())),
        "patternCounts": dict(sorted(pattern_counts.items())),
        "findings": findings,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Precheck an incoming ADP/MES module package before promoting it.")
    parser.add_argument("path", help="Directory, file, or zip/jar package to scan.")
    parser.add_argument("--report", help="Write JSON report to this path instead of stdout.")
    parser.add_argument("--report-only", action="store_true", help="Always exit 0, even when blocking findings exist.")
    args = parser.parse_args()

    report = scan(Path(args.path).expanduser().resolve())
    content = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.report:
        Path(args.report).write_text(content, encoding="utf-8")
    else:
        print(content, end="")

    error_count = int(report["summary"]["errorCount"])  # type: ignore[index]
    if error_count and not args.report_only:
        print(f"Module intake precheck failed: {error_count} blocking finding(s).", file=sys.stderr)
        return 1
    print(
        "Module intake precheck passed."
        if not error_count
        else f"Module intake precheck report-only: {error_count} blocking finding(s).",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
