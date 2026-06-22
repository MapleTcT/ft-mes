#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import subprocess
import sys
import tarfile
import zipfile
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]

DEFAULT_ROOTS = [
    "/Users/zhangchu/Documents/MES包",
    "/Users/zhangchu/Downloads/ADP/bap-server/base-Server",
    "/Users/zhangchu/Downloads/ADP/bap-server/config",
    "/Users/zhangchu/Downloads/ADP/Temp/static/custom",
]

TEXT_SUFFIXES = {
    ".java",
    ".js",
    ".ts",
    ".xml",
    ".json",
    ".properties",
    ".yml",
    ".yaml",
    ".sql",
    ".txt",
    ".md",
    ".html",
    ".jsp",
}

ARCHIVE_SUFFIXES = {
    ".zip",
    ".jar",
    ".war",
    ".ear",
    ".tar",
    ".tgz",
    ".gz",
    ".7z",
}

SKIP_DIRS = {
    ".git",
    "node_modules",
    "target",
    "dist",
    "build",
    ".gradle",
    "__pycache__",
}

MAX_TEXT_BYTES = 2 * 1024 * 1024
MAX_HITS_PER_DEPENDENCY = 120
MAX_NESTED_ARCHIVE_BYTES = 20 * 1024 * 1024
MAX_ARCHIVE_HASH_BYTES = 0
MAX_ARCHIVE_TEXT_ENTRIES = int(os.environ.get("ADP_BUSINESS_PACKAGE_SCAN_MAX_ARCHIVE_TEXT_ENTRIES", "100000"))
MAX_TAR_SCAN_BYTES = 50 * 1024 * 1024
MAX_ARCHIVE_ENTRIES = int(os.environ.get("ADP_BUSINESS_PACKAGE_SCAN_MAX_ARCHIVE_ENTRIES", "2000000"))

FIRST_PARTY_ARCHIVE_HINTS = {
    "supcon",
    "orchid",
    "supplant",
    "wom",
    "qcs",
    "rm",
    "lims",
    "baseset",
    "sparemanage",
    "eam",
    "wts",
    "waps",
    "material",
    "processanalysis",
    "traceability",
}

VENDOR_ARCHIVE_HINTS = {
    "jdk",
    "jre",
    "missioncontrol",
    "org/eclipse",
    "org.apache",
    "org/apache",
    "cn/hutool",
    "com/fasterxml",
    "com/google",
    "springframework",
    "javax/",
    "net/sf",
}

DEPENDENCIES = {
    "material-service": {
        "title": "Material warehouse/inventory service",
        "requiredFor": ["PROD-019", "PROD-021", "PROD-022"],
        "targetTerms": [
            "checkProdResult",
            "generateProduceOutSing",
            "produceOutSingle",
            "material/foreign/foreign",
            "produceOutSingle/produceOutSing",
        ],
        "moduleTerms": ["serviceName=material", "service-name=material", "com/supcon/orchid/material"],
        "knownAdjacentTerms": [
            "LIMSMaterial",
            "BaseSet/material",
            "SpareManage",
            "WOMPutInMaterial",
            "WOMOutputMaterial",
            "WOMRejectMaterial",
            "WOMRemainMaterial",
        ],
        "callerOnlyHints": ["WOMQCSServiceImpl", "WOMPutInMaterialServiceImpl", "WOMMatConsumRecodServiceImpl"],
        "archiveContentPathHints": [
            "material",
            "wms",
            "warehouse",
            "inventory",
            "stock",
            "produceout",
            "produce_out",
            "outsingle",
            "out_single",
            "womqcsservice",
            "womputinmaterial",
            "wommatconsumrecod",
        ],
        "implementationExcludeHints": [
            "LIMSMaterial",
            "BaseSet",
            "SpareManage",
            "WOM_",
            "wom",
            "supPlant-WOM",
            "WOMQCSServiceImpl",
            "WOMPutInMaterialServiceImpl",
            "WOMMatConsumRecodServiceImpl",
            "QCS",
        ],
    },
    "process-analysis": {
        "title": "ProcessAnalysis traceability service",
        "requiredFor": ["PROD-020"],
        "targetTerms": [
            "ProcessAnalysis",
            "isProdprocessView",
            "processBatchViewOut",
            "analysisiTask",
            "manualStatActive",
            "manualStatProcess",
            "paActiExeLog",
            "paPrExeLog",
        ],
        "moduleTerms": [
            "serviceName=ProcessAnalysis",
            "service-name=ProcessAnalysis",
            "com/supcon/orchid/ProcessAnalysis",
            "processAnalysis_",
        ],
        "knownAdjacentTerms": [
            "prodprocessView",
            "makeTaskGraphList",
            "processExeLogList",
            "activeExeLogList",
        ],
        "callerOnlyHints": ["customEvent.js", "WOM_"],
        "archiveContentPathHints": [
            "processanalysis",
            "processanalysis_",
            "trace",
            "traceability",
            "analysisparam",
            "paramstatrec",
            "paramdetail",
            "paacti",
            "papr",
            "exelogsecond",
            "prodprocess",
            "maketasklist",
            "maketaskgraphlist",
            "processexelog",
            "activeexelog",
            "maketaskexeculist",
        ],
        "implementationExcludeHints": [
            "WOM_",
            "wom",
            "supPlant-WOM",
            "customEvent.js",
            "produceTask",
        ],
    },
}


@dataclass(frozen=True)
class Hit:
    dependency: str
    term: str
    match_type: str
    source: str
    entry: str | None
    context: str


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def get_repo_commit() -> str:
    result = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
    )
    return result.stdout.strip() if result.returncode == 0 else "UNKNOWN"


def parse_roots(value: str | None) -> list[Path]:
    raw = value if value is not None else os.environ.get("ADP_BUSINESS_PACKAGE_SCAN_ROOTS", os.pathsep.join(DEFAULT_ROOTS))
    return [Path(part).expanduser() for part in raw.split(os.pathsep) if part.strip()]


def is_archive(path: str | Path) -> bool:
    lower = str(path).lower()
    return lower.endswith((".zip", ".jar", ".war", ".ear", ".tar", ".tgz", ".tar.gz", ".7z"))


def is_text_candidate(path: str | Path) -> bool:
    return Path(str(path).lower()).suffix in TEXT_SUFFIXES


def should_skip(path: Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def decode_text(data: bytes) -> str:
    return data[:MAX_TEXT_BYTES].decode("utf-8", errors="ignore")


def context_for(text: str, term: str, lower_text: str | None = None) -> str:
    lower = lower_text if lower_text is not None else text.lower()
    index = lower.find(term.lower())
    if index < 0:
        return ""
    start = max(0, index - 120)
    end = min(len(text), index + len(term) + 120)
    return " ".join(text[start:end].split())


def add_hit(hits: list[Hit], dependency: str, term: str, match_type: str, source: Path | str, entry: str | None, context: str) -> None:
    if len([hit for hit in hits if hit.dependency == dependency]) >= MAX_HITS_PER_DEPENDENCY:
        return
    hits.append(
        Hit(
            dependency=dependency,
            term=term,
            match_type=match_type,
            source=str(source),
            entry=entry,
            context=context[:300],
        )
    )


def scan_text(text: str, source: Path | str, entry: str | None, hits: list[Hit]) -> None:
    source_text = f"{source} {entry or ''}"
    source_lower = source_text.lower()
    text_lower = text.lower()
    for dependency, config in DEPENDENCIES.items():
        for group_name in ("targetTerms", "moduleTerms", "knownAdjacentTerms", "callerOnlyHints"):
            for term in config[group_name]:
                term_lower = term.lower()
                if term_lower in source_lower:
                    add_hit(hits, dependency, term, f"path:{group_name}", source, entry, source_text)
                if term_lower in text_lower:
                    add_hit(hits, dependency, term, f"content:{group_name}", source, entry, context_for(text, term, text_lower))


def should_scan_archive_text(source: Path | str, entry: str | None) -> bool:
    haystack = f"{source} {entry or ''}".lower()
    return any(
        hint.lower() in haystack
        for config in DEPENDENCIES.values()
        for hint in config["archiveContentPathHints"]
    )


def should_scan_nested_archive(source: Path | str, entry: str | None) -> bool:
    haystack = f"{source} {entry or ''}".lower()
    if any(hint in haystack for hint in VENDOR_ARCHIVE_HINTS) and not any(
        hint in haystack for hint in FIRST_PARTY_ARCHIVE_HINTS
    ):
        return False
    return any(hint in haystack for hint in FIRST_PARTY_ARCHIVE_HINTS) or should_scan_archive_text(source, entry)


def scan_plain_file(path: Path, hits: list[Hit], counters: dict[str, int]) -> None:
    counters["textFilesScanned"] += 1
    try:
        data = path.read_bytes()[:MAX_TEXT_BYTES]
    except OSError:
        counters["readErrors"] += 1
        return
    scan_text(decode_text(data), path, None, hits)


def scan_zip_archive(
    archive: zipfile.ZipFile,
    source: Path | str,
    hits: list[Hit],
    counters: dict[str, int],
    depth: int,
) -> None:
    members = archive.infolist()
    if counters["archiveEntriesScanned"] >= MAX_ARCHIVE_ENTRIES:
        counters["skippedArchiveEntries"] += len(members)
        return
    for index, member in enumerate(members):
        if counters["archiveEntriesScanned"] >= MAX_ARCHIVE_ENTRIES:
            counters["skippedArchiveEntries"] += len(members) - index
            return
        if member.is_dir():
            continue
        counters["archiveEntriesScanned"] += 1
        entry = member.filename
        scan_text("", source, entry, hits)
        if is_text_candidate(entry) and member.file_size <= MAX_TEXT_BYTES:
            if not should_scan_archive_text(source, entry):
                counters["archiveTextEntriesSkippedByPath"] += 1
                continue
            if counters["archiveTextEntriesScanned"] >= MAX_ARCHIVE_TEXT_ENTRIES:
                counters["skippedArchiveTextEntries"] += 1
                continue
            try:
                scan_text(decode_text(archive.read(member)), source, entry, hits)
                counters["archiveTextEntriesScanned"] += 1
            except (OSError, RuntimeError, zipfile.BadZipFile):
                counters["readErrors"] += 1
        if depth > 0 and is_archive(entry) and member.file_size > MAX_NESTED_ARCHIVE_BYTES:
            counters["skippedLargeNestedArchives"] += 1
            continue
        if depth > 0 and is_archive(entry):
            if not should_scan_nested_archive(source, entry):
                counters["nestedArchivesSkippedByPath"] += 1
                continue
            try:
                nested = archive.read(member)
            except (OSError, RuntimeError, zipfile.BadZipFile):
                counters["readErrors"] += 1
                continue
            if zipfile.is_zipfile(io.BytesIO(nested)):
                counters["nestedArchivesScanned"] += 1
                scan_zip_bytes(nested, f"{source}!{entry}", hits, counters, depth - 1)


def scan_zip_bytes(data: bytes, source: Path | str, hits: list[Hit], counters: dict[str, int], depth: int) -> None:
    try:
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            scan_zip_archive(archive, source, hits, counters, depth)
    except zipfile.BadZipFile:
        counters["unsupportedArchives"] += 1


def scan_zip_file(path: Path, hits: list[Hit], counters: dict[str, int], depth: int) -> None:
    counters["archivesScanned"] += 1
    try:
        with zipfile.ZipFile(path) as archive:
            scan_zip_archive(archive, path, hits, counters, depth)
    except OSError:
        counters["readErrors"] += 1
    except zipfile.BadZipFile:
        counters["unsupportedArchives"] += 1


def scan_tar_file(path: Path, hits: list[Hit], counters: dict[str, int]) -> None:
    counters["archivesScanned"] += 1
    try:
        with tarfile.open(path) as archive:
            for member in archive.getmembers():
                if not member.isfile():
                    continue
                scan_text("", path, member.name, hits)
                if is_text_candidate(member.name) and member.size <= MAX_TEXT_BYTES:
                    if not should_scan_archive_text(path, member.name):
                        counters["archiveTextEntriesSkippedByPath"] += 1
                        continue
                    extracted = archive.extractfile(member)
                    if extracted is None:
                        continue
                    if counters["archiveTextEntriesScanned"] >= MAX_ARCHIVE_TEXT_ENTRIES:
                        counters["skippedArchiveTextEntries"] += 1
                        continue
                    scan_text(decode_text(extracted.read(MAX_TEXT_BYTES)), path, member.name, hits)
                    counters["archiveTextEntriesScanned"] += 1
    except (tarfile.TarError, OSError):
        counters["unsupportedArchives"] += 1


def scan_7z_listing(path: Path, hits: list[Hit], counters: dict[str, int]) -> None:
    counters["archivesScanned"] += 1
    binary = next((candidate for candidate in ("7zz", "7z") if shutil_which(candidate)), None)
    if not binary:
        counters["unsupportedArchives"] += 1
        return
    result = subprocess.run(
        [binary, "l", "-ba", str(path)],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
    )
    if result.returncode != 0:
        counters["unsupportedArchives"] += 1
        return
    for line in result.stdout.splitlines():
        scan_text("", path, line, hits)
    counters["archiveListingsScanned"] += 1


def shutil_which(binary: str) -> str | None:
    for directory in os.environ.get("PATH", "").split(os.pathsep):
        candidate = Path(directory) / binary
        if candidate.exists() and os.access(candidate, os.X_OK):
            return str(candidate)
    return None


def iter_files(roots: Iterable[Path]) -> Iterable[Path]:
    for root in roots:
        if not root.exists():
            continue
        if root.is_file():
            yield root
            continue
        for path in root.rglob("*"):
            if should_skip(path):
                continue
            if path.is_file():
                yield path


def classify_hits(hits: list[Hit]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for dependency, config in DEPENDENCIES.items():
        dependency_hits = [hit for hit in hits if hit.dependency == dependency]
        target_hits = [hit for hit in dependency_hits if "targetTerms" in hit.match_type]
        module_hits = [hit for hit in dependency_hits if "moduleTerms" in hit.match_type]
        adjacent_hits = [hit for hit in dependency_hits if "knownAdjacentTerms" in hit.match_type]
        caller_hits = [hit for hit in dependency_hits if "callerOnlyHints" in hit.match_type]
        exclude_hints = [hint.lower() for hint in config["implementationExcludeHints"]]

        implementation_candidates = []
        for hit in target_hits + module_hits:
            haystack = f"{hit.source} {hit.entry or ''} {hit.context}".lower()
            if any(exclude in haystack for exclude in exclude_hints):
                continue
            implementation_candidates.append(hit)

        status = "CANDIDATE_FOUND" if implementation_candidates else "BLOCKED_NO_IMPLEMENTATION_CANDIDATE"
        result[dependency] = {
            "id": dependency,
            "title": config["title"],
            "status": status,
            "requiredFor": config["requiredFor"],
            "searchCriteria": {
                "targetTerms": config["targetTerms"],
                "moduleTerms": config["moduleTerms"],
                "knownAdjacentTerms": config["knownAdjacentTerms"],
                "callerOnlyHints": config["callerOnlyHints"],
                "archiveContentPathHints": config["archiveContentPathHints"],
                "implementationExcludeHints": config["implementationExcludeHints"],
            },
            "targetHitCount": len(target_hits),
            "moduleHitCount": len(module_hits),
            "knownAdjacentHitCount": len(adjacent_hits),
            "callerOnlyHitCount": len(caller_hits),
            "implementationCandidateCount": len(implementation_candidates),
            "implementationCandidates": [hit.__dict__ for hit in implementation_candidates[:20]],
            "targetHits": [hit.__dict__ for hit in target_hits[:40]],
            "moduleHits": [hit.__dict__ for hit in module_hits[:40]],
            "knownAdjacentHits": [hit.__dict__ for hit in adjacent_hits[:40]],
            "callerOnlyHits": [hit.__dict__ for hit in caller_hits[:40]],
            "nextAction": (
                "Inspect and deploy the implementation candidate, then rerun runtime dependency smoke and marker acceptance."
                if implementation_candidates
                else "Provide a package containing the missing service implementation, then rerun this scan and runtime dependency smoke."
            ),
        }
    return result


def build_report(roots: list[Path], nested_depth: int) -> dict[str, Any]:
    hits: list[Hit] = []
    counters = {
        "rootsRequested": len(roots),
        "rootsExisting": sum(1 for root in roots if root.exists()),
        "filesVisited": 0,
        "textFilesScanned": 0,
        "archivesScanned": 0,
        "nestedArchivesScanned": 0,
        "nestedArchivesSkippedByPath": 0,
        "skippedLargeNestedArchives": 0,
        "archiveEntriesScanned": 0,
        "skippedArchiveEntries": 0,
        "archiveTextEntriesScanned": 0,
        "skippedArchiveTextEntries": 0,
        "archiveListingsScanned": 0,
        "archiveTextEntriesSkippedByPath": 0,
        "skippedArchiveHashes": 0,
        "unsupportedArchives": 0,
        "skippedLargeArchives": 0,
        "readErrors": 0,
    }
    scanned_archives = []

    for path in iter_files(roots):
        counters["filesVisited"] += 1
        lower = str(path).lower()
        try:
            size = path.stat().st_size
        except OSError:
            counters["readErrors"] += 1
            continue
        if is_archive(path):
            sha256 = None
            if size <= MAX_ARCHIVE_HASH_BYTES:
                sha256 = sha256_file(path)
            else:
                counters["skippedArchiveHashes"] += 1
            archive_info = {
                "path": str(path),
                "sizeBytes": size,
                "sha256": sha256,
            }
            scanned_archives.append(archive_info)
            scan_text("", path, None, hits)
            if lower.endswith((".zip", ".jar", ".war", ".ear")):
                scan_zip_file(path, hits, counters, nested_depth)
            elif lower.endswith((".tar", ".tgz", ".tar.gz")):
                if size > MAX_TAR_SCAN_BYTES:
                    counters["skippedLargeArchives"] += 1
                else:
                    scan_tar_file(path, hits, counters)
            elif lower.endswith(".7z"):
                scan_7z_listing(path, hits, counters)
            else:
                counters["unsupportedArchives"] += 1
        elif is_text_candidate(path):
            scan_plain_file(path, hits, counters)

    dependencies = classify_hits(hits)
    status = (
        "CANDIDATE_FOUND"
        if any(item["implementationCandidateCount"] > 0 for item in dependencies.values())
        else "BLOCKED_NO_IMPLEMENTATION_CANDIDATE"
    )
    unscanned_reasons = {
        key: value
        for key, value in counters.items()
        if key
        in {
            "skippedArchiveEntries",
            "skippedArchiveTextEntries",
            "skippedLargeNestedArchives",
            "skippedLargeArchives",
            "unsupportedArchives",
            "readErrors",
        }
        and value
    }
    coverage_status = "FULL_BOUNDED" if not unscanned_reasons else "PARTIAL_BOUNDED"

    return {
        "schemaVersion": 1,
        "generatedAt": now_iso(),
        "repoCommit": get_repo_commit(),
        "database": "PostgreSQL",
        "scanRoots": [{"path": str(root), "exists": root.exists()} for root in roots],
        "coverage": {
            "status": coverage_status,
            "nestedDepth": nested_depth,
            "textSuffixes": sorted(TEXT_SUFFIXES),
            "archiveSuffixes": sorted(ARCHIVE_SUFFIXES),
            "firstPartyArchiveHints": sorted(FIRST_PARTY_ARCHIVE_HINTS),
            "vendorArchiveHints": sorted(VENDOR_ARCHIVE_HINTS),
            "limits": {
                "maxTextBytes": MAX_TEXT_BYTES,
                "maxArchiveEntries": MAX_ARCHIVE_ENTRIES,
                "maxArchiveTextEntries": MAX_ARCHIVE_TEXT_ENTRIES,
                "maxNestedArchiveBytes": MAX_NESTED_ARCHIVE_BYTES,
                "maxTarScanBytes": MAX_TAR_SCAN_BYTES,
                "maxArchiveHashBytes": MAX_ARCHIVE_HASH_BYTES,
            },
            "unscannedReasons": unscanned_reasons,
        },
        "summary": {
            "status": status,
            **counters,
            "dependencies": len(dependencies),
            "blockedDependencies": sum(
                1 for item in dependencies.values() if item["status"] == "BLOCKED_NO_IMPLEMENTATION_CANDIDATE"
            ),
            "candidateDependencies": sum(1 for item in dependencies.values() if item["status"] == "CANDIDATE_FOUND"),
        },
        "scannedArchives": scanned_archives[:200],
        "dependencies": list(dependencies.values()),
        "evidence": {
            "method": "Read-only local package/source scan over configured roots; no archives are extracted into the repository.",
            "limitations": [
                "Encrypted or unsupported archives are recorded as unsupportedArchives.",
                "Default nested-depth is 1 so vendor packages nested one level inside delivery archives are included.",
                f"Archive entry path scanning is capped at {MAX_ARCHIVE_ENTRIES} entries.",
                f"Archive SHA-256 is only computed for files up to {MAX_ARCHIVE_HASH_BYTES} bytes.",
                "Archive entry paths are scanned broadly; archive text content is read only when the entry/source path matches dependency-specific archiveContentPathHints.",
                f"Archive text content reads that match archiveContentPathHints are capped at {MAX_ARCHIVE_TEXT_ENTRIES} entries.",
                "Nested archives are expanded only when their path matches firstPartyArchiveHints or dependency-specific archiveContentPathHints; vendor archive paths are counted but not expanded.",
                f"Tar archives larger than {MAX_TAR_SCAN_BYTES} bytes are recorded but not expanded by default.",
                f"Nested archives larger than {MAX_NESTED_ARCHIVE_BYTES} bytes are listed but not expanded by default.",
                "A candidate hit is not acceptance; runtime Nacos/API/PostgreSQL marker verification is still required.",
            ],
        },
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Scan ADP/MES package roots for missing business dependency implementations.")
    parser.add_argument("--roots", help=f"os.pathsep-separated roots. Default: {os.pathsep.join(DEFAULT_ROOTS)}")
    parser.add_argument("--output", default="metadata/business-dependency-package-scan.json")
    parser.add_argument("--nested-depth", type=int, default=1)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    roots = parse_roots(args.roots)
    report = build_report(roots, max(0, args.nested_depth))
    output = Path(args.output)
    if not output.is_absolute():
        output = ROOT / output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report["summary"], ensure_ascii=False, indent=2))
    print(f"Business dependency package scan report: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
