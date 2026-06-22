#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
import sys
import zipfile
from pathlib import Path
from tempfile import TemporaryDirectory


ROOT = Path(__file__).resolve().parents[1]
PRECHECK = ROOT / "scripts/precheck-module-intake.py"


CLEAN_POM = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.mapletct.ftmes</groupId>
  <artifactId>clean-intake-fixture</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <dependencies>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
    </dependency>
  </dependencies>
</project>
"""


ORACLE_POM = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.mapletct.ftmes</groupId>
  <artifactId>oracle-intake-fixture</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <dependencies>
    <dependency>
      <groupId>com.oracle.jdbc</groupId>
      <artifactId>ojdbc7</artifactId>
      <version>12.1.0.2</version>
    </dependency>
  </dependencies>
</project>
"""


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def run_precheck(path: Path, report: Path, report_only: bool = False) -> subprocess.CompletedProcess[str]:
    command = [
        sys.executable,
        str(PRECHECK.relative_to(ROOT)),
        str(path),
        "--report",
        str(report),
    ]
    if report_only:
        command.append("--report-only")
    return subprocess.run(command, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def read_report(path: Path, failures: list[str]) -> dict[str, object]:
    try:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing report: {path}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid report JSON {path}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"report must be a JSON object: {path}")
        return {}
    return data


def summary_error_count(report: dict[str, object]) -> int:
    summary = report.get("summary")
    if not isinstance(summary, dict):
        return -1
    value = summary.get("errorCount")
    return value if isinstance(value, int) else -1


def finding_patterns(report: dict[str, object]) -> set[str]:
    findings = report.get("findings")
    if not isinstance(findings, list):
        return set()
    patterns: set[str] = set()
    for finding in findings:
        if isinstance(finding, dict) and isinstance(finding.get("pattern"), str):
            patterns.add(str(finding["pattern"]))
    return patterns


def check_scan_coverage(report: dict[str, object], label: str, failures: list[str]) -> None:
    summary = report.get("summary")
    policy = report.get("scanPolicy")
    coverage = report.get("scanCoverage")
    if not isinstance(summary, dict):
        fail(failures, f"{label} report must include summary object")
        return
    if not isinstance(policy, dict):
        fail(failures, f"{label} report must include scanPolicy object")
        return
    if not isinstance(coverage, dict):
        fail(failures, f"{label} report must include scanCoverage object")
        return

    required_summary_numbers = (
        "zipArchiveCount",
        "unsupportedArchiveCount",
        "nestedFileCount",
        "maxArchiveDepthSeen",
        "textCandidateCount",
        "textScannedCount",
        "textSkippedLargeCount",
        "textSkippedUnavailableCount",
        "textScannedBytes",
    )
    for key in required_summary_numbers:
        value = summary.get(key)
        if not isinstance(value, int) or value < 0:
            fail(failures, f"{label} summary.{key} must be a non-negative integer")

    if policy.get("maxTextBytes") != 2 * 1024 * 1024:
        fail(failures, f"{label} scanPolicy.maxTextBytes must match the intake scanner limit")
    if policy.get("maxArchiveDepth") != 2:
        fail(failures, f"{label} scanPolicy.maxArchiveDepth must match the intake scanner limit")
    if ".7z" not in set(policy.get("unsupportedArchiveSuffixes") or []):
        fail(failures, f"{label} scanPolicy must disclose unsupported archive suffixes")
    for key in ("textCoverageComplete", "hasUnsupportedArchives", "hasOversizedNestedArchives", "nestedArchivePathsPreserved"):
        if not isinstance(coverage.get(key), bool):
            fail(failures, f"{label} scanCoverage.{key} must be boolean")


def make_clean_fixture(root: Path) -> Path:
    fixture = root / "clean-module"
    (fixture / "src/main/java/com/example").mkdir(parents=True)
    (fixture / "pom.xml").write_text(CLEAN_POM, encoding="utf-8")
    (fixture / "src/main/java/com/example/Clean.java").write_text(
        "package com.example;\npublic final class Clean {}\n",
        encoding="utf-8",
    )
    return fixture


def make_oracle_pom_fixture(root: Path) -> Path:
    fixture = root / "oracle-pom-module"
    fixture.mkdir()
    (fixture / "pom.xml").write_text(ORACLE_POM, encoding="utf-8")
    return fixture


def make_oracle_resource_fixture(root: Path) -> Path:
    fixture = root / "oracle-resource-module"
    resource_dir = fixture / "src/main/resources/mapper/oracle"
    resource_dir.mkdir(parents=True)
    (fixture / "pom.xml").write_text(CLEAN_POM, encoding="utf-8")
    (resource_dir / "legacy.xml").write_text(
        "<mapper><select id=\"legacy\">select 1 from dual</select></mapper>\n",
        encoding="utf-8",
    )
    return fixture


def make_nested_oracle_archive_fixture(root: Path) -> Path:
    inner_jar = root / "oracle-inner.jar"
    with zipfile.ZipFile(inner_jar, "w") as archive:
        archive.writestr("META-INF/maven/com.example/oracle-nested/pom.xml", ORACLE_POM)
        archive.writestr("mapper/oracle/legacy.xml", "<mapper><select id=\"legacy\">select 1 from dual</select></mapper>\n")

    outer_zip = root / "nested-business-package.zip"
    with zipfile.ZipFile(outer_zip, "w") as archive:
        archive.write(inner_jar, "BOOT-INF/lib/oracle-inner.jar")
    return outer_zip


def make_unsupported_archive_fixture(root: Path) -> Path:
    fixture = root / "opaque-business-package.7z"
    fixture.write_bytes(b"not-a-real-7z-but-opaque-to-the-intake-scanner")
    return fixture


def main() -> int:
    failures: list[str] = []
    with TemporaryDirectory(prefix="adp-module-intake-") as temp_dir_name:
        temp_dir = Path(temp_dir_name)

        clean = make_clean_fixture(temp_dir)
        clean_report = temp_dir / "clean.json"
        clean_result = run_precheck(clean, clean_report)
        clean_data = read_report(clean_report, failures)
        check_scan_coverage(clean_data, "clean fixture", failures)
        if clean_result.returncode != 0:
            fail(failures, "clean fixture must pass module intake precheck")
        if summary_error_count(clean_data) != 0:
            fail(failures, "clean fixture report must have errorCount=0")
        if clean_data.get("scanCoverage", {}).get("hasUnsupportedArchives") is True:  # type: ignore[union-attr]
            fail(failures, "clean fixture must not report unsupported archives")

        oracle_pom = make_oracle_pom_fixture(temp_dir)
        oracle_pom_report = temp_dir / "oracle-pom.json"
        oracle_pom_result = run_precheck(oracle_pom, oracle_pom_report)
        oracle_pom_data = read_report(oracle_pom_report, failures)
        check_scan_coverage(oracle_pom_data, "Oracle POM fixture", failures)
        if oracle_pom_result.returncode == 0:
            fail(failures, "Oracle POM fixture must fail module intake precheck")
        if summary_error_count(oracle_pom_data) <= 0:
            fail(failures, "Oracle POM fixture report must have blocking errors")
        if "oracle-maven-dependency" not in finding_patterns(oracle_pom_data):
            fail(failures, "Oracle POM fixture must report oracle-maven-dependency")

        oracle_resource = make_oracle_resource_fixture(temp_dir)
        oracle_resource_report = temp_dir / "oracle-resource.json"
        oracle_resource_result = run_precheck(oracle_resource, oracle_resource_report)
        oracle_resource_data = read_report(oracle_resource_report, failures)
        check_scan_coverage(oracle_resource_data, "Oracle resource fixture", failures)
        if oracle_resource_result.returncode == 0:
            fail(failures, "Oracle resource fixture must fail module intake precheck")
        if "mapper/oracle" not in finding_patterns(oracle_resource_data):
            fail(failures, "Oracle resource fixture must report mapper/oracle path")

        nested_archive = make_nested_oracle_archive_fixture(temp_dir)
        nested_archive_report = temp_dir / "nested-oracle-archive.json"
        nested_archive_result = run_precheck(nested_archive, nested_archive_report)
        nested_archive_data = read_report(nested_archive_report, failures)
        check_scan_coverage(nested_archive_data, "nested Oracle archive fixture", failures)
        if nested_archive_result.returncode == 0:
            fail(failures, "Nested Oracle archive fixture must fail module intake precheck")
        nested_patterns = finding_patterns(nested_archive_data)
        if "oracle-maven-dependency" not in nested_patterns:
            fail(failures, "Nested Oracle archive fixture must report oracle-maven-dependency")
        if "mapper/oracle" not in nested_patterns:
            fail(failures, "Nested Oracle archive fixture must report mapper/oracle path")
        nested_findings = nested_archive_data.get("findings")
        if not isinstance(nested_findings, list) or not any("!" in str(item.get("file", "")) for item in nested_findings if isinstance(item, dict)):
            fail(failures, "Nested Oracle archive fixture findings must preserve outer!inner archive paths")
        nested_summary = nested_archive_data.get("summary")
        nested_coverage = nested_archive_data.get("scanCoverage")
        if not isinstance(nested_summary, dict) or int(nested_summary.get("nestedFileCount") or 0) <= 0:
            fail(failures, "Nested Oracle archive fixture must report nestedFileCount > 0")
        if not isinstance(nested_coverage, dict) or nested_coverage.get("nestedArchivePathsPreserved") is not True:
            fail(failures, "Nested Oracle archive fixture must report nestedArchivePathsPreserved=true")

        unsupported_archive = make_unsupported_archive_fixture(temp_dir)
        unsupported_archive_report = temp_dir / "unsupported-archive.json"
        unsupported_archive_result = run_precheck(unsupported_archive, unsupported_archive_report)
        unsupported_archive_data = read_report(unsupported_archive_report, failures)
        check_scan_coverage(unsupported_archive_data, "unsupported archive fixture", failures)
        if unsupported_archive_result.returncode == 0:
            fail(failures, "Unsupported archive fixture must fail module intake precheck")
        if "7z" not in finding_patterns(unsupported_archive_data):
            fail(failures, "Unsupported archive fixture must report 7z unsupported archive pattern")
        if unsupported_archive_data.get("scanCoverage", {}).get("hasUnsupportedArchives") is not True:  # type: ignore[union-attr]
            fail(failures, "Unsupported archive fixture must report hasUnsupportedArchives=true")
        unsupported_report_only = temp_dir / "unsupported-archive-report-only.json"
        unsupported_report_only_result = run_precheck(unsupported_archive, unsupported_report_only, report_only=True)
        unsupported_report_only_data = read_report(unsupported_report_only, failures)
        check_scan_coverage(unsupported_report_only_data, "unsupported archive report-only fixture", failures)
        if unsupported_report_only_result.returncode != 0:
            fail(failures, "unsupported archive report-only mode must exit 0")
        if summary_error_count(unsupported_report_only_data) <= 0:
            fail(failures, "unsupported archive report-only mode must still record blocking findings")

        report_only_report = temp_dir / "report-only.json"
        report_only_result = run_precheck(oracle_pom, report_only_report, report_only=True)
        report_only_data = read_report(report_only_report, failures)
        check_scan_coverage(report_only_data, "Oracle POM report-only fixture", failures)
        if report_only_result.returncode != 0:
            fail(failures, "report-only mode must exit 0 even for blocking findings")
        if summary_error_count(report_only_data) <= 0:
            fail(failures, "report-only mode must still record blocking findings")

    if failures:
        print(f"Module intake precheck regression failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Module intake precheck regression passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
