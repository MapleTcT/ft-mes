#!/usr/bin/env python3
"""Verify that a packaged BPI service contains the exact release migrations."""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
import zipfile
from pathlib import Path


MIGRATION_PATTERN = re.compile(r"^V([1-9][0-9]*)__.+\.sql$")
JAR_PREFIXES = (
    "BOOT-INF/classes/db/migration/",
    "db/migration/",
)


class VerificationError(RuntimeError):
    pass


def _migration_version(name: str) -> int:
    match = MIGRATION_PATTERN.fullmatch(name)
    if match is None:
        raise VerificationError(f"invalid migration filename: {name}")
    return int(match.group(1))


def _read_source_migrations(directory: Path) -> dict[str, bytes]:
    if not directory.is_dir():
        raise VerificationError(f"migration directory is missing: {directory}")

    migrations: dict[str, bytes] = {}
    versions: dict[int, str] = {}
    for path in sorted(directory.glob("V*.sql")):
        version = _migration_version(path.name)
        if version in versions:
            raise VerificationError(
                f"duplicate migration version V{version}: {versions[version]}, {path.name}"
            )
        versions[version] = path.name
        migrations[path.name] = path.read_bytes()

    if not migrations:
        raise VerificationError(f"no migrations found in {directory}")
    return migrations


def _read_jar_migrations(jar: Path) -> dict[str, bytes]:
    if not jar.is_file():
        raise VerificationError(f"BPI service JAR is missing: {jar}")

    migrations: dict[str, bytes] = {}
    try:
        with zipfile.ZipFile(jar) as archive:
            for entry in archive.namelist():
                for prefix in JAR_PREFIXES:
                    if not entry.startswith(prefix):
                        continue
                    name = entry[len(prefix) :]
                    if "/" in name or not name.endswith(".sql"):
                        break
                    _migration_version(name)
                    if name in migrations:
                        raise VerificationError(
                            f"duplicate packaged migration path for {name}"
                        )
                    migrations[name] = archive.read(entry)
                    break
    except zipfile.BadZipFile as error:
        raise VerificationError(f"invalid BPI service JAR: {jar}") from error

    if not migrations:
        raise VerificationError(f"no packaged migrations found in {jar}")
    return migrations


def _set_digest(migrations: dict[str, bytes]) -> str:
    digest = hashlib.sha256()
    for name in sorted(migrations, key=lambda item: (_migration_version(item), item)):
        digest.update(name.encode("utf-8"))
        digest.update(b"\0")
        digest.update(hashlib.sha256(migrations[name]).digest())
    return digest.hexdigest()


def verify(jar: Path, migrations_directory: Path, expected_version: int) -> str:
    source = _read_source_migrations(migrations_directory)
    packaged = _read_jar_migrations(jar)

    highest_source = max(_migration_version(name) for name in source)
    highest_packaged = max(_migration_version(name) for name in packaged)
    if highest_source != expected_version:
        raise VerificationError(
            f"source migration head is V{highest_source}, expected V{expected_version}"
        )
    if highest_packaged != expected_version:
        raise VerificationError(
            f"packaged migration head is V{highest_packaged}, expected V{expected_version}"
        )

    source_names = set(source)
    packaged_names = set(packaged)
    if source_names != packaged_names:
        missing = sorted(source_names - packaged_names)
        extra = sorted(packaged_names - source_names)
        raise VerificationError(
            f"packaged migration set differs; missing={missing}, extra={extra}"
        )

    changed = [
        name
        for name in sorted(source_names, key=lambda item: (_migration_version(item), item))
        if hashlib.sha256(source[name]).digest()
        != hashlib.sha256(packaged[name]).digest()
    ]
    if changed:
        raise VerificationError(
            "packaged migration checksum differs from source: " + ", ".join(changed)
        )

    return _set_digest(source)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", required=True, type=Path)
    parser.add_argument("--migrations-dir", required=True, type=Path)
    parser.add_argument("--expected-version", required=True, type=int)
    parser.add_argument("--digest-only", action="store_true")
    args = parser.parse_args()

    if args.expected_version <= 0:
        print("ERROR: expected version must be a positive integer", file=sys.stderr)
        return 1

    try:
        digest = verify(args.jar, args.migrations_dir, args.expected_version)
    except VerificationError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    if args.digest_only:
        print(digest)
    else:
        print(
            "BPI release migration artifact: PASS "
            f"(V{args.expected_version}, sha256={digest})"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
