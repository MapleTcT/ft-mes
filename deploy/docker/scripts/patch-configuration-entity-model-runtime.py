#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import shutil
import tempfile
import time
import zipfile
from pathlib import Path


OPEN_API_JAR = "BOOT-INF/lib/configuration-services-open-api-1.0.0-SNAPSHOT.jar"
BASE_JAR = "BOOT-INF/lib/configuration-services-base-1.0.0-SNAPSHOT.jar"
SERVICE_JAR = "BOOT-INF/lib/configuration-services-service-1.0.0-SNAPSHOT.jar"

PATCH_TARGETS = {
    OPEN_API_JAR: [
        "com/supcon/supfusion/configuration/services/openapi/utils/DtoUtils.class",
    ],
    BASE_JAR: [
        "com/supcon/supfusion/base/services/impl/MenuInfoServiceImpl.class",
    ],
    SERVICE_JAR: [
        "com/supcon/supfusion/configuration/services/service/impl/EntityServiceImpl.class",
        "com/supcon/supfusion/configuration/services/service/impl/ModelServiceImpl.class",
        "com/supcon/supfusion/configuration/services/service/impl/ModelServiceImpl$1.class",
        "com/supcon/supfusion/configuration/services/service/impl/ModelServiceImpl$LetterComparator.class",
    ],
}


def clone_info(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    cloned = zipfile.ZipInfo(info.filename, date_time=info.date_time)
    cloned.compress_type = info.compress_type
    cloned.comment = info.comment
    cloned.extra = info.extra
    cloned.internal_attr = info.internal_attr
    cloned.external_attr = info.external_attr
    cloned.create_system = info.create_system
    return cloned


def read_zip(path_or_bytes: Path | bytes) -> dict[str, tuple[zipfile.ZipInfo, bytes]]:
    if isinstance(path_or_bytes, Path):
        source = path_or_bytes
    else:
        source = io.BytesIO(path_or_bytes)
    entries: dict[str, tuple[zipfile.ZipInfo, bytes]] = {}
    with zipfile.ZipFile(source, "r") as zf:
        for info in zf.infolist():
            entries[info.filename] = (info, zf.read(info.filename))
    return entries


def write_zip(entries: dict[str, tuple[zipfile.ZipInfo, bytes]]) -> bytes:
    out = io.BytesIO()
    with zipfile.ZipFile(out, "w") as zf:
        for name, (info, data) in entries.items():
            zf.writestr(clone_info(info), data)
    return out.getvalue()


def patch_inner_jar(inner_bytes: bytes, class_bytes: dict[str, bytes], target_classes: list[str]) -> bytes:
    entries = read_zip(inner_bytes)
    missing = [name for name in target_classes if name not in class_bytes]
    if missing:
        raise SystemExit(f"missing compiled patch classes: {', '.join(missing)}")

    for class_name in target_classes:
        if class_name in entries:
            old_info, _old_data = entries[class_name]
            entries[class_name] = (old_info, class_bytes[class_name])
        else:
            info = zipfile.ZipInfo(class_name)
            info.compress_type = zipfile.ZIP_DEFLATED
            entries[class_name] = (info, class_bytes[class_name])
    return write_zip(entries)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--runtime-root", required=True, type=Path)
    parser.add_argument(
        "--classes-jar",
        type=Path,
        default=Path(__file__).resolve().parents[1]
        / "patches"
        / "configuration-entity-model-compat"
        / "configuration-entity-model-compat.jar",
    )
    args = parser.parse_args()

    boot_jar = args.runtime_root / "base-Server" / "configuration" / "supfusion-configuration.jar"
    if not boot_jar.is_file():
        raise SystemExit(f"configuration boot jar not found: {boot_jar}")
    if not args.classes_jar.is_file():
        raise SystemExit(f"compiled patch classes jar not found: {args.classes_jar}")

    class_entries = read_zip(args.classes_jar)
    class_bytes = {name: data for name, (_info, data) in class_entries.items() if name.endswith(".class")}
    outer_entries = read_zip(boot_jar)

    missing_nested = [name for name in PATCH_TARGETS if name not in outer_entries]
    if missing_nested:
        raise SystemExit(f"missing nested configuration jars: {', '.join(missing_nested)}")

    for nested_name, class_names in PATCH_TARGETS.items():
        info, inner_bytes = outer_entries[nested_name]
        patched_inner = patch_inner_jar(inner_bytes, class_bytes, class_names)
        outer_entries[nested_name] = (info, patched_inner)

    stamp = time.strftime("%Y%m%d%H%M%S")
    backup = boot_jar.with_name(f"{boot_jar.name}.bak-entity-model-compat-{stamp}")
    shutil.copy2(boot_jar, backup)

    with tempfile.TemporaryDirectory() as temp_dir:
        patched_boot = Path(temp_dir) / boot_jar.name
        patched_boot.write_bytes(write_zip(outer_entries))
        shutil.move(str(patched_boot), boot_jar)

    with zipfile.ZipFile(boot_jar, "r") as zf:
        for nested_name in PATCH_TARGETS:
            info = zf.getinfo(nested_name)
            if info.compress_type != zipfile.ZIP_STORED:
                raise SystemExit(f"nested boot jar must stay STORED: {nested_name}")
            nested_bytes = zf.read(nested_name)
            nested_entries = read_zip(nested_bytes)
            for class_name in PATCH_TARGETS[nested_name]:
                if class_name not in nested_entries:
                    raise SystemExit(f"patched class missing from {nested_name}: {class_name}")

    print(f"patched {boot_jar}")
    print(f"backup {backup}")


if __name__ == "__main__":
    main()
