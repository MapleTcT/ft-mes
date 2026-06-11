#!/usr/bin/env python3
from __future__ import annotations

import argparse
import shutil
import struct
import tempfile
import zipfile
from pathlib import Path


DOCKER_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = DOCKER_DIR.parents[1]
PATCH_ROOT = DOCKER_DIR / "patches" / "operatetools-standalone-app-list"
CLASSES_DIR = PATCH_ROOT / "classes"
TARGET_CLASS = "BOOT-INF/classes/com/supcon/orchid/entityconf/services/imps/AppServiceImpl.class"
PATCHED_CLASS_FILE = CLASSES_DIR / "com/supcon/orchid/entityconf/services/imps/AppServiceImpl.class"
ERROR_DESCRIPTOR = "(Ljava/lang/String;Ljava/lang/Throwable;)V"


def runtime_root_default() -> Path:
    candidate = PROJECT_ROOT / "runtime" / "bap-server"
    if candidate.exists():
        return candidate
    sibling = PROJECT_ROOT.parent / "bap-server"
    if sibling.exists():
        return sibling
    return candidate


def service_jar(runtime_root: Path) -> Path:
    service_dir = runtime_root / "base-Server" / "operatetools"
    jars = sorted(path for path in service_dir.glob("*.jar") if ".bak" not in path.name)
    if len(jars) != 1:
        raise FileNotFoundError(f"expected one jar in {service_dir}, found {len(jars)}")
    return jars[0]


def zip_info_copy(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    copied = zipfile.ZipInfo(info.filename, info.date_time)
    copied.comment = info.comment
    copied.extra = info.extra
    copied.internal_attr = info.internal_attr
    copied.external_attr = info.external_attr
    copied.create_system = info.create_system
    copied.compress_type = info.compress_type
    return copied


def read_u2(data: bytes | bytearray, offset: int) -> int:
    return struct.unpack_from(">H", data, offset)[0]


def write_u2(data: bytearray, offset: int, value: int) -> None:
    struct.pack_into(">H", data, offset, value)


def parse_constant_pool(data: bytes) -> list[dict[str, object] | None]:
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("not a Java class file")

    count = read_u2(data, 8)
    entries: list[dict[str, object] | None] = [None] * count
    offset = 10
    index = 1
    while index < count:
        tag = data[offset]
        start = offset
        offset += 1

        if tag == 1:
            length = read_u2(data, offset)
            value_offset = offset + 2
            value = data[value_offset : value_offset + length].decode("utf-8")
            entries[index] = {
                "tag": tag,
                "start": start,
                "value": value,
            }
            offset = value_offset + length
        elif tag in {3, 4}:
            entries[index] = {"tag": tag, "start": start}
            offset += 4
        elif tag in {5, 6}:
            entries[index] = {"tag": tag, "start": start}
            offset += 8
            index += 1
        elif tag in {7, 8, 16, 19, 20}:
            entries[index] = {"tag": tag, "start": start}
            offset += 2
        elif tag in {9, 10, 11, 18}:
            entries[index] = {"tag": tag, "start": start}
            offset += 4
        elif tag == 12:
            entries[index] = {
                "tag": tag,
                "start": start,
                "name_offset": offset,
                "descriptor_offset": offset + 2,
                "name_index": read_u2(data, offset),
                "descriptor_index": read_u2(data, offset + 2),
            }
            offset += 4
        elif tag == 15:
            entries[index] = {"tag": tag, "start": start}
            offset += 3
        else:
            raise ValueError(f"unsupported constant pool tag {tag} at index {index}")

        index += 1

    return entries


def utf8_value(entries: list[dict[str, object] | None], index: int) -> str | None:
    entry = entries[index]
    if not entry or entry.get("tag") != 1:
        return None
    return str(entry["value"])


def patch_class_bytes(raw: bytes) -> tuple[bytes, int]:
    data = bytearray(raw)
    entries = parse_constant_pool(raw)
    debug_indexes = [
        index
        for index, entry in enumerate(entries)
        if entry and entry.get("tag") == 1 and entry.get("value") == "debug"
    ]
    if not debug_indexes:
        raise ValueError("class has no debug logger method name in the constant pool")

    patched = 0
    for entry in entries:
        if not entry or entry.get("tag") != 12:
            continue
        name = utf8_value(entries, int(entry["name_index"]))
        descriptor = utf8_value(entries, int(entry["descriptor_index"]))
        if name == "error" and descriptor == ERROR_DESCRIPTOR:
            write_u2(data, int(entry["name_offset"]), debug_indexes[0])
            patched += 1

    if patched != 1:
        raise ValueError(f"expected to patch one Logger.error(String, Throwable), patched {patched}")

    return bytes(data), patched


def build_class(jar_path: Path, force: bool) -> Path:
    if PATCHED_CLASS_FILE.exists() and not force:
        return PATCHED_CLASS_FILE

    with zipfile.ZipFile(jar_path, "r") as app_jar:
        original = app_jar.read(TARGET_CLASS)
    patched, _count = patch_class_bytes(original)

    PATCHED_CLASS_FILE.parent.mkdir(parents=True, exist_ok=True)
    PATCHED_CLASS_FILE.write_bytes(patched)
    return PATCHED_CLASS_FILE


def patch_service_jar(jar_path: Path, class_file: Path, backup_suffix: str) -> None:
    class_bytes = class_file.read_bytes()
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        shutil.copy2(jar_path, backup)

    patched = False
    with tempfile.TemporaryDirectory(prefix="operatetools-app-list-") as tmp:
        output = Path(tmp) / jar_path.name
        with zipfile.ZipFile(jar_path, "r") as zin, zipfile.ZipFile(output, "w") as zout:
            for info in zin.infolist():
                data = class_bytes if info.filename == TARGET_CLASS else zin.read(info.filename)
                if info.filename == TARGET_CLASS:
                    patched = True
                zout.writestr(zip_info_copy(info), data)

        if not patched:
            raise FileNotFoundError(f"{jar_path} does not contain {TARGET_CLASS}")

        mode = jar_path.stat().st_mode
        shutil.move(str(output), jar_path)
        jar_path.chmod(mode)


def restore_service_jar(jar_path: Path, backup_suffix: str) -> bool:
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        return False
    shutil.copy2(backup, jar_path)
    return True


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Patch operatetools AppServiceImpl so missing installer service does not emit "
            "ERROR stack traces in the standalone Docker test profile."
        )
    )
    parser.add_argument("--runtime-root", type=Path, default=runtime_root_default())
    parser.add_argument("--backup-suffix", default=".pre-standalone-app-list.bak")
    parser.add_argument("--force-build", action="store_true")
    parser.add_argument("--build-only", action="store_true")
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()

    jar_path = service_jar(args.runtime_root.resolve())
    if args.restore:
        restored = restore_service_jar(jar_path, args.backup_suffix)
        print(f"{'restored' if restored else 'missing backup for'} operatetools: {jar_path}")
        return

    class_file = build_class(jar_path, args.force_build)
    print(f"using patch class: {class_file}")
    if args.build_only:
        return

    patch_service_jar(jar_path, class_file, args.backup_suffix)
    print(f"patched operatetools standalone app list: {jar_path}")


if __name__ == "__main__":
    main()
