#!/usr/bin/env python3
"""Patch foundation ExtraView LOB mapping inside Spring Boot service jars.

The recovered Windows packages map ExtraView.config/fullConfig/viewJson as
Hibernate LOB fields. PostgreSQL stores those as OID large objects by default,
which makes business list queries fail in this Linux test deployment. This
patcher replaces ExtraView.class inside nested foundation.core jars with a
PostgreSQL text-mapped build.
"""

from __future__ import annotations

import argparse
import io
import shutil
import sys
import time
import zipfile
from pathlib import Path
from typing import Tuple


CLASS_ENTRY = "com/supcon/orchid/ec/entities/ExtraView.class"


def copy_zip_info(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    copied = zipfile.ZipInfo(info.filename, info.date_time)
    copied.comment = info.comment
    copied.extra = info.extra
    copied.internal_attr = info.internal_attr
    copied.external_attr = info.external_attr
    copied.create_system = info.create_system
    copied.compress_type = info.compress_type
    return copied


def patch_nested_jar(data: bytes, class_bytes: bytes) -> Tuple[bytes, bool]:
    changed = False
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(data), "r") as source:
        with zipfile.ZipFile(output, "w") as target:
            for info in source.infolist():
                content = source.read(info.filename)
                if info.filename == CLASS_ENTRY:
                    content = class_bytes
                    changed = True
                target.writestr(copy_zip_info(info), content)
    return output.getvalue(), changed


def patch_service_jar(jar_path: Path, class_bytes: bytes, backup_suffix: str) -> bool:
    jar_data = jar_path.read_bytes()
    changed = False
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(jar_data), "r") as source:
        with zipfile.ZipFile(output, "w") as target:
            for info in source.infolist():
                content = source.read(info.filename)
                should_patch_nested = (
                    info.filename.startswith("BOOT-INF/lib/")
                    and "foundation.core" in info.filename
                    and info.filename.endswith(".jar")
                )
                if info.filename == CLASS_ENTRY:
                    content = class_bytes
                    changed = True
                elif should_patch_nested:
                    content, nested_changed = patch_nested_jar(content, class_bytes)
                    changed = changed or nested_changed
                target.writestr(copy_zip_info(info), content)

    if not changed:
        return False

    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        shutil.copy2(jar_path, backup)
    temp_path = jar_path.with_name(jar_path.name + ".tmp-extra-view-lob")
    temp_path.write_bytes(output.getvalue())
    shutil.copystat(jar_path, temp_path)
    temp_path.replace(jar_path)
    return True


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--class-file", required=True, type=Path)
    parser.add_argument("jars", nargs="+", type=Path)
    parser.add_argument("--backup-suffix", default=f".bak-extra-view-lob-{int(time.time())}")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    class_file = args.class_file.expanduser().resolve()
    class_bytes = class_file.read_bytes()
    patched = 0
    skipped = 0
    for jar in args.jars:
        jar_path = jar.expanduser().resolve()
        if not jar_path.exists():
            print(f"MISS {jar_path}")
            skipped += 1
            continue
        try:
            if patch_service_jar(jar_path, class_bytes, args.backup_suffix):
                print(f"PATCHED {jar_path}")
                patched += 1
            else:
                print(f"SKIP {jar_path}")
                skipped += 1
        except zipfile.BadZipFile as error:
            print(f"BADZIP {jar_path}: {error}", file=sys.stderr)
            skipped += 1
    print(f"SUMMARY patched={patched} skipped={skipped}")
    return 0 if patched else 1


if __name__ == "__main__":
    raise SystemExit(main())
