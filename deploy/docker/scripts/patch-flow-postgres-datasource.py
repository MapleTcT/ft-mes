#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import shutil
import tempfile
import zipfile
from pathlib import Path

DOCKER_DIR = Path(__file__).resolve().parents[1]
PATCH_ROOT = DOCKER_DIR / "patches" / "flow-postgres-datasource"
CLASS_ROOT = PATCH_ROOT / "classes"
CLASS_FILE = CLASS_ROOT / "com" / "supcon" / "supfusion" / "flow" / "taskcenter" / "component" / "DataSourceBuilder.class"
SOURCE_FILE = PATCH_ROOT / "src" / "com" / "supcon" / "supfusion" / "flow" / "taskcenter" / "component" / "DataSourceBuilder.java"
FLOW_DIR = "base-Server/flow"
TARGET_CLASS = "com/supcon/supfusion/flow/taskcenter/component/DataSourceBuilder.class"
TARGET_CLASSES_BY_NESTED_JAR = {
    "BOOT-INF/lib/task-center-1.0.0-RELEASE.jar": (
        "com/supcon/supfusion/flow/taskcenter/component/DataSourceBuilder.class",
        "com/supcon/supfusion/flow/taskcenter/component/DataSourceBuilder$1.class",
        "com/supcon/supfusion/flow/taskcenter/listener/AllTenantInitialCommandLineRunner.class",
        "com/supcon/supfusion/flow/taskcenter/listener/TenantProcessEngineInitialListener.class",
        "com/supcon/supfusion/flow/taskcenter/listener/TenantProcessEngineInitialListener$1.class",
    ),
    "BOOT-INF/lib/flow-engine-1.0.0-RELEASE.jar": (
        "com/supcon/supfusion/flow/engine/server/config/MultiSchemaProcessEngineInit.class",
        "com/supcon/supfusion/flow/engine/server/config/MultiSchemaProcessEngineInit$1.class",
        "com/supcon/supfusion/flow/engine/server/register/TenantRegister.class",
        "com/supcon/supfusion/flow/engine/server/register/TenantRegister$1.class",
    ),
}


def service_jar(runtime_root: Path) -> Path:
    service_dir = runtime_root / FLOW_DIR
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


def class_bytes_by_nested_jar() -> dict[str, dict[str, bytes]]:
    replacements: dict[str, dict[str, bytes]] = {}
    for nested_jar, targets in TARGET_CLASSES_BY_NESTED_JAR.items():
        target_bytes: dict[str, bytes] = {}
        for target in targets:
            path = CLASS_ROOT / target
            if path.exists():
                target_bytes[target] = path.read_bytes()
        if target_bytes:
            replacements[nested_jar] = target_bytes
    return replacements


def replace_class_in_nested_jar(nested_bytes: bytes, replacements: dict[str, bytes]) -> tuple[bytes, bool]:
    with tempfile.TemporaryDirectory() as tmp:
        nested_in = Path(tmp) / "in.jar"
        nested_out = Path(tmp) / "out.jar"
        nested_in.write_bytes(nested_bytes)
        patched = False
        with zipfile.ZipFile(nested_in, "r") as zin, zipfile.ZipFile(nested_out, "w") as zout:
            for info in zin.infolist():
                data = replacements.get(info.filename, zin.read(info.filename))
                if info.filename in replacements:
                    patched = True
                zout.writestr(zip_info_copy(info), data)
        return nested_out.read_bytes(), patched


def patch_service_jar(jar_path: Path, backup_suffix: str) -> None:
    if not CLASS_FILE.exists():
        raise FileNotFoundError(
            f"compiled class not found: {CLASS_FILE}; compile {SOURCE_FILE} before patching"
        )
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        shutil.copy2(jar_path, backup)
    replacements_by_nested_jar = class_bytes_by_nested_jar()
    if TARGET_CLASS not in replacements_by_nested_jar.get("BOOT-INF/lib/task-center-1.0.0-RELEASE.jar", {}):
        raise FileNotFoundError(f"compiled class not found: {CLASS_FILE}")
    patched_nested_jars: set[str] = set()
    with tempfile.TemporaryDirectory() as tmp:
        output = Path(tmp) / jar_path.name
        with zipfile.ZipFile(jar_path, "r") as zin, zipfile.ZipFile(output, "w") as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                replacements = replacements_by_nested_jar.get(info.filename)
                if replacements:
                    data, nested_patched = replace_class_in_nested_jar(data, replacements)
                    if nested_patched:
                        patched_nested_jars.add(info.filename)
                zout.writestr(zip_info_copy(info), data)
        missing_nested = sorted(set(TARGET_CLASSES_BY_NESTED_JAR) - patched_nested_jars)
        if missing_nested:
            raise FileNotFoundError(f"{jar_path} was not patched for nested jars: {', '.join(missing_nested)}")
        mode = jar_path.stat().st_mode
        shutil.move(str(output), jar_path)
        os.chmod(jar_path, mode)


def restore_service_jar(jar_path: Path, backup_suffix: str) -> bool:
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        return False
    shutil.copy2(backup, jar_path)
    return True


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Patch flow runtime classes to support PostgreSQL datasource and process-engine initialization."
    )
    parser.add_argument(
        "--runtime-root",
        type=Path,
        default=(DOCKER_DIR / "../../runtime/bap-server"),
        help="Path to runtime bap-server directory.",
    )
    parser.add_argument("--restore", action="store_true")
    parser.add_argument("--backup-suffix", default=".bak-flow-postgres-datasource")
    args = parser.parse_args()

    jar_path = service_jar(args.runtime_root.resolve())
    if args.restore:
        restored = restore_service_jar(jar_path, args.backup_suffix)
        print(f"{'restored' if restored else 'missing backup for'} flow: {jar_path}")
        return
    patch_service_jar(jar_path, args.backup_suffix)
    print(f"patched flow PostgreSQL runtime support: {jar_path}")


if __name__ == "__main__":
    main()
