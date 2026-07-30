#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import shutil
import tempfile
import zipfile
from pathlib import Path


DOCKER_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = DOCKER_DIR.parents[1] if len(DOCKER_DIR.parents) > 1 else Path.cwd()
ADMIN_DAO_JAR = "BOOT-INF/lib/admin-dao-1.0.0-SNAPSHOT.jar"
SOURCE_XML = "mappers/postgresql/NoticeProtocolMapper.xml"
TARGET_XMLS = (
    "BOOT-INF/classes/mappers/postgresql/NoticeProtocolMapper.xml",
)
TARGET_DIRECTORIES = (
    "BOOT-INF/classes/mappers/postgresql/",
)


def runtime_root_default() -> Path:
    candidate = PROJECT_ROOT / "runtime" / "bap-server"
    if candidate.exists():
        return candidate
    sibling = PROJECT_ROOT.parent / "bap-server"
    if sibling.exists():
        return sibling
    return candidate


def service_jar(runtime_root: Path) -> Path:
    service_dir = runtime_root / "base-Server" / "msgmanagement"
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


def read_mapper_xml(app_jar: zipfile.ZipFile) -> bytes:
    try:
        nested_bytes = app_jar.read(ADMIN_DAO_JAR)
    except KeyError as exc:
        raise FileNotFoundError(f"missing nested jar: {ADMIN_DAO_JAR}") from exc

    with tempfile.TemporaryDirectory(prefix="msgmanagement-admin-dao-") as tmp:
        nested_path = Path(tmp) / "admin-dao.jar"
        nested_path.write_bytes(nested_bytes)
        with zipfile.ZipFile(nested_path, "r") as nested:
            try:
                return nested.read(SOURCE_XML)
            except KeyError as exc:
                raise FileNotFoundError(f"missing mapper xml: {SOURCE_XML}") from exc


def patch_service_jar(jar_path: Path, backup_suffix: str) -> None:
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        shutil.copy2(jar_path, backup)

    with tempfile.TemporaryDirectory(prefix="msgmanagement-mapper-") as tmp:
        output = Path(tmp) / jar_path.name
        with zipfile.ZipFile(jar_path, "r") as zin:
            mapper_xml = read_mapper_xml(zin)
            with zipfile.ZipFile(output, "w") as zout:
                for info in zin.infolist():
                    if info.filename in TARGET_XMLS:
                        zout.writestr(zip_info_copy(info), mapper_xml)
                    else:
                        zout.writestr(zip_info_copy(info), zin.read(info.filename))
                existing = set(zin.namelist())
                for target_directory in TARGET_DIRECTORIES:
                    if target_directory not in existing:
                        directory = zipfile.ZipInfo(target_directory)
                        directory.external_attr = 0o40755 << 16
                        zout.writestr(directory, b"")
                for target_xml in TARGET_XMLS:
                    if target_xml not in existing:
                        zout.writestr(target_xml, mapper_xml)

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
        description=(
            "Expose the PostgreSQL NoticeProtocol mapper from the nested admin-dao jar "
            "to BOOT-INF/classes for msgmanagement's legacy classpath scanner."
        )
    )
    parser.add_argument("--runtime-root", type=Path, default=runtime_root_default())
    parser.add_argument("--backup-suffix", default=".pre-notice-protocol-mapper.bak")
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()

    jar_path = service_jar(args.runtime_root.resolve())
    if args.restore:
        restored = restore_service_jar(jar_path, args.backup_suffix)
        print(f"{'restored' if restored else 'missing backup for'} msgmanagement: {jar_path}")
        return

    patch_service_jar(jar_path, args.backup_suffix)
    print(f"patched msgmanagement notice protocol mapper: {jar_path}")


if __name__ == "__main__":
    main()
