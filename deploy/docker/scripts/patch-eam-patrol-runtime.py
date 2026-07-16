#!/usr/bin/env python3
"""Add PATROL 6.0.4.0 to an existing EamMs Spring Boot runtime jar.

The recovered EamMs runtime already contains environment-specific PostgreSQL
patches. Rebuilding it from an older launcher can silently lose those fixes, so
this tool performs a guarded, additive merge against the exact running jar.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import time
import zipfile
from pathlib import Path
from typing import Iterable


BOOTSTRAP_PATH = "BOOT-INF/classes/bootstrap.properties"
PATROL_ALIAS = "PATROL"
PATROL_MODULE_CODE = "PATROL_1.0.0"
REQUIRED_ARTIFACT_PREFIXES = (
    "com.supcon.greendill.PATROL.core-",
    "com.supcon.greendill.PATROL.api-",
    "com.supcon.greendill.PATROL.service-",
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def indexed_properties(lines: Iterable[str], prefix: str) -> dict[int, str]:
    values: dict[int, str] = {}
    marker = prefix + "["
    for line in lines:
        if not line.startswith(marker) or "]=" not in line:
            continue
        index_text, value = line[len(marker) :].split("]=", 1)
        try:
            values[int(index_text)] = value
        except ValueError:
            continue
    return values


def next_index(values: dict[int, str]) -> int:
    return max(values, default=-1) + 1


def append_indexed_value(lines: list[str], prefix: str, value: str) -> int:
    current = indexed_properties(lines, prefix)
    for index, current_value in current.items():
        if current_value == value:
            return index
    index = next_index(current)
    lines.append(f"{prefix}[{index}]={value}")
    return index


def patch_bootstrap(original: bytes, version_stamp: str) -> bytes:
    text = original.decode("utf-8")
    lines = text.splitlines()

    append_indexed_value(
        lines, "supfusion.cloud.registry.instanceAliases", PATROL_ALIAS
    )
    append_indexed_value(lines, "service.moduleCodes", PATROL_MODULE_CODE)

    locale_prefix = "supfusion.cloud.i18n.locale-modules"
    locale_index = append_indexed_value(lines, locale_prefix, PATROL_ALIAS)
    version_prefix = "supfusion.cloud.i18n.module-versions"
    locale_versions = indexed_properties(lines, version_prefix)
    expected_version = PATROL_ALIAS + version_stamp
    if locale_versions.get(locale_index) != expected_version:
        lines.append(f"{version_prefix}[{locale_index}]={expected_version}")

    append_indexed_value(lines, "supfusion.cloud.i18n.modules", PATROL_ALIAS)
    patched = "\n".join(lines) + "\n"
    return patched.encode("utf-8")


def clone_zip_info(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    cloned = zipfile.ZipInfo(info.filename, date_time=info.date_time)
    cloned.comment = info.comment
    cloned.extra = info.extra
    cloned.internal_attr = info.internal_attr
    cloned.external_attr = info.external_attr
    cloned.create_system = info.create_system
    cloned.compress_type = info.compress_type
    cloned.flag_bits = info.flag_bits
    return cloned


def add_nested_jar(
    output: zipfile.ZipFile, source_path: Path, nested_name: str
) -> None:
    info = zipfile.ZipInfo(nested_name, date_time=time.localtime()[:6])
    info.compress_type = zipfile.ZIP_STORED
    info.create_system = 3
    info.external_attr = 0o100644 << 16
    with source_path.open("rb") as source, output.open(info, "w") as target:
        shutil.copyfileobj(source, target, length=1024 * 1024)


def validate_patrol_artifacts(paths: list[Path]) -> dict[str, Path]:
    if len(paths) != len(REQUIRED_ARTIFACT_PREFIXES):
        raise ValueError("exactly three PATROL core/api/service jars are required")

    resolved: dict[str, Path] = {}
    for prefix in REQUIRED_ARTIFACT_PREFIXES:
        matches = [path for path in paths if path.name.startswith(prefix)]
        if len(matches) != 1:
            raise ValueError(f"expected one artifact matching {prefix}, got {matches}")
        path = matches[0]
        if not path.is_file() or not zipfile.is_zipfile(path):
            raise ValueError(f"invalid PATROL jar: {path}")
        resolved[prefix] = path
    return resolved


def verify_output(
    input_entries: set[str], output_path: Path, nested_names: list[str]
) -> None:
    with zipfile.ZipFile(output_path, "r") as output:
        output_entries = [info.filename for info in output.infolist()]
        if len(output_entries) != len(set(output_entries)):
            raise ValueError("output jar contains duplicate ZIP entries")
        if output.testzip() is not None:
            raise ValueError("output jar failed ZIP CRC validation")
        expected_entries = input_entries | set(nested_names)
        if set(output_entries) != expected_entries:
            added = sorted(set(output_entries) - expected_entries)
            missing = sorted(expected_entries - set(output_entries))
            raise ValueError(f"unexpected output entries; added={added}, missing={missing}")
        for nested_name in nested_names:
            if output.getinfo(nested_name).compress_type != zipfile.ZIP_STORED:
                raise ValueError(f"Spring Boot nested jar must be stored: {nested_name}")

        bootstrap = output.read(BOOTSTRAP_PATH).decode("utf-8").splitlines()
        assertions = (
            (
                "supfusion.cloud.registry.instanceAliases",
                PATROL_ALIAS,
            ),
            ("service.moduleCodes", PATROL_MODULE_CODE),
            ("supfusion.cloud.i18n.locale-modules", PATROL_ALIAS),
            ("supfusion.cloud.i18n.modules", PATROL_ALIAS),
        )
        for prefix, expected in assertions:
            if expected not in indexed_properties(bootstrap, prefix).values():
                raise ValueError(f"missing bootstrap registration {prefix}={expected}")


def build_output(
    input_path: Path,
    output_path: Path,
    artifacts: dict[str, Path],
    version_stamp: str,
) -> tuple[list[str], int]:
    if output_path.exists():
        raise ValueError(f"refusing to overwrite existing output: {output_path}")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary = output_path.with_name(output_path.name + ".tmp")
    if temporary.exists():
        temporary.unlink()

    nested_names = [f"BOOT-INF/lib/{path.name}" for path in artifacts.values()]
    try:
        with zipfile.ZipFile(input_path, "r") as source:
            source_infos = source.infolist()
            input_entries = [info.filename for info in source_infos]
            if len(input_entries) != len(set(input_entries)):
                raise ValueError("input jar contains duplicate ZIP entries")
            if BOOTSTRAP_PATH not in input_entries:
                raise ValueError(f"input jar is missing {BOOTSTRAP_PATH}")
            collisions = sorted(set(input_entries) & set(nested_names))
            if collisions:
                raise ValueError(f"PATROL artifacts already exist in input jar: {collisions}")

            patched_bootstrap = patch_bootstrap(
                source.read(BOOTSTRAP_PATH), version_stamp
            )
            with zipfile.ZipFile(temporary, "w", allowZip64=True) as output:
                output.comment = source.comment
                for info in source_infos:
                    cloned = clone_zip_info(info)
                    if info.filename == BOOTSTRAP_PATH:
                        output.writestr(cloned, patched_bootstrap)
                        continue
                    with source.open(info, "r") as current, output.open(
                        cloned, "w"
                    ) as target:
                        shutil.copyfileobj(current, target, length=1024 * 1024)

                for path in artifacts.values():
                    add_nested_jar(output, path, f"BOOT-INF/lib/{path.name}")

        verify_output(set(input_entries), temporary, nested_names)
        temporary.replace(output_path)
        return nested_names, len(input_entries)
    except Exception:
        if temporary.exists():
            temporary.unlink()
        raise


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-jar", required=True, type=Path)
    parser.add_argument("--output-jar", required=True, type=Path)
    parser.add_argument("--patrol-jar", action="append", required=True, type=Path)
    parser.add_argument("--expected-input-sha256", required=True)
    parser.add_argument("--source-commit", default="unknown")
    parser.add_argument("--report", type=Path)
    parser.add_argument(
        "--version-stamp",
        default=time.strftime("%Y%m%d%H%M"),
        help="suffix used to invalidate the PATROL i18n cache",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.input_jar.is_file() or not zipfile.is_zipfile(args.input_jar):
        print(f"invalid input jar: {args.input_jar}", file=sys.stderr)
        return 2
    if args.input_jar.resolve() == args.output_jar.resolve():
        print("input and output jar must be different paths", file=sys.stderr)
        return 2

    input_hash = sha256(args.input_jar)
    if input_hash.lower() != args.expected_input_sha256.lower():
        print(
            f"input SHA-256 mismatch: expected {args.expected_input_sha256}, got {input_hash}",
            file=sys.stderr,
        )
        return 3

    try:
        artifacts = validate_patrol_artifacts(args.patrol_jar)
        nested_names, input_entry_count = build_output(
            args.input_jar,
            args.output_jar,
            artifacts,
            args.version_stamp,
        )
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"PATROL runtime patch failed: {error}", file=sys.stderr)
        return 4

    report = {
        "inputJar": str(args.input_jar),
        "inputSha256": input_hash,
        "outputJar": str(args.output_jar),
        "outputSha256": sha256(args.output_jar),
        "sourceCommit": args.source_commit,
        "moduleCode": PATROL_MODULE_CODE,
        "serviceAlias": PATROL_ALIAS,
        "versionStamp": args.version_stamp,
        "inputEntryCount": input_entry_count,
        "addedNestedJars": nested_names,
        "artifactSha256": {
            path.name: sha256(path) for path in artifacts.values()
        },
        "zipIntegrity": "PASS",
        "status": "PASS",
    }
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
