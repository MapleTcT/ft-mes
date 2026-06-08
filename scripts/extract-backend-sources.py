#!/usr/bin/env python3
import json
import os
import re
import shutil
import subprocess
import zipfile
from pathlib import Path

REPO_ROOT = Path.cwd()
ADP_ROOT = REPO_ROOT.parent
MAVEN_REPO = ADP_ROOT / "bap-server" / "assembly" / "repository" / "maven"
OUT_ROOT = REPO_ROOT / "backend" / "modules"
METADATA_ROOT = REPO_ROOT / "metadata"

INCLUDE_PREFIXES = (
    "com/supcon/",
    "com/supcon.greendill/",
)


def safe_name(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", value).strip("_") or "unknown"


def artifact_coordinates(source_jar: Path):
    rel_parts = source_jar.relative_to(MAVEN_REPO).parts
    if len(rel_parts) < 4:
        return None
    version = rel_parts[-2]
    file_name = rel_parts[-1]
    artifact = rel_parts[-3]
    group = ".".join(rel_parts[:-3])
    if not file_name.endswith("-sources.jar"):
        return None
    return group, artifact, version


def should_extract(source_jar: Path) -> bool:
    rel = source_jar.relative_to(MAVEN_REPO).as_posix()
    return rel.startswith(INCLUDE_PREFIXES)


def count_files(root: Path):
    counts = {"java": 0, "xml": 0, "properties": 0, "other": 0}
    for file_path in root.rglob("*"):
        if not file_path.is_file():
            continue
        suffix = file_path.suffix.lower().lstrip(".")
        if suffix in counts:
            counts[suffix] += 1
        else:
            counts["other"] += 1
    return counts


def extract_source_jar(source_jar: Path):
    coords = artifact_coordinates(source_jar)
    if coords is None:
        return None
    group, artifact, version = coords
    target_dir = OUT_ROOT / group.replace(".", "/") / artifact / version
    if target_dir.exists():
        shutil.rmtree(target_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(source_jar) as archive:
        archive.extractall(target_dir)
    counts = count_files(target_dir)
    return {
        "group": group,
        "artifact": artifact,
        "version": version,
        "sourceJar": source_jar.relative_to(REPO_ROOT).as_posix() if source_jar.is_relative_to(REPO_ROOT) else source_jar.relative_to(ADP_ROOT).as_posix(),
        "target": target_dir.relative_to(REPO_ROOT).as_posix(),
        "files": counts,
    }


def main():
    OUT_ROOT.mkdir(parents=True, exist_ok=True)
    METADATA_ROOT.mkdir(parents=True, exist_ok=True)

    source_jars = sorted(path for path in MAVEN_REPO.rglob("*-sources.jar") if should_extract(path))
    extracted = []
    for source_jar in source_jars:
        result = extract_source_jar(source_jar)
        if result:
            extracted.append(result)

    summary = {
        "mavenRepository": MAVEN_REPO.relative_to(ADP_ROOT).as_posix(),
        "extractedSourceJars": len(extracted),
        "totalJavaFiles": sum(item["files"]["java"] for item in extracted),
        "totalXmlFiles": sum(item["files"]["xml"] for item in extracted),
        "modules": extracted,
    }
    (METADATA_ROOT / "backend-source-summary.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
