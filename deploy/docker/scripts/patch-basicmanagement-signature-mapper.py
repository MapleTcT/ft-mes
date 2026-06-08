#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import shutil
import tempfile
import zipfile
from pathlib import Path


DOCKER_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = DOCKER_DIR.parents[1]
SIGNATURE_DAO_JAR = "BOOT-INF/lib/signature-dao-1.0.0-SNAPSHOT.jar"
SOURCE_XML = "mappers/postgresql/SignatureLog.xml"
TARGET_XMLS = (
    "BOOT-INF/classes/mappers/postgresql/SignatureLog.xml",
    "BOOT-INF/classes/mappers/mariadb/SignatureLog.xml",
    "BOOT-INF/classes/mappers/mysql/SignatureLog.xml",
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
    service_dir = runtime_root / "base-Server" / "basicmanagement"
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


def read_signature_xml(app_jar: zipfile.ZipFile) -> bytes:
    try:
        nested_bytes = app_jar.read(SIGNATURE_DAO_JAR)
    except KeyError as exc:
        raise FileNotFoundError(f"missing nested jar: {SIGNATURE_DAO_JAR}") from exc

    with tempfile.TemporaryDirectory(prefix="signature-dao-") as tmp:
        nested_path = Path(tmp) / "signature-dao.jar"
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

    with tempfile.TemporaryDirectory(prefix="signature-mapper-") as tmp:
        output = Path(tmp) / jar_path.name
        with zipfile.ZipFile(jar_path, "r") as zin:
            xml_bytes = read_signature_xml(zin)
            wrote_target = False
            with zipfile.ZipFile(output, "w") as zout:
                for info in zin.infolist():
                    if info.filename in TARGET_XMLS:
                        zout.writestr(zip_info_copy(info), xml_bytes)
                        wrote_target = True
                    else:
                        zout.writestr(zip_info_copy(info), zin.read(info.filename))
                existing = set(zin.namelist())
                for target_xml in TARGET_XMLS:
                    if target_xml not in existing:
                        zout.writestr(target_xml, xml_bytes)
                        wrote_target = True
                if not wrote_target:
                    raise FileNotFoundError("no signature mapper target was written")

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
        description="Expose signature PostgreSQL mapper XML from nested jar to BOOT-INF/classes for legacy classpath scanning."
    )
    parser.add_argument("--runtime-root", type=Path, default=runtime_root_default())
    parser.add_argument("--backup-suffix", default=".pre-signature-mapper.bak")
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()

    jar_path = service_jar(args.runtime_root.resolve())
    if args.restore:
        restored = restore_service_jar(jar_path, args.backup_suffix)
        print(f"{'restored' if restored else 'missing backup for'} basicmanagement: {jar_path}")
        return

    patch_service_jar(jar_path, args.backup_suffix)
    print(f"patched basicmanagement signature mapper: {jar_path}")


if __name__ == "__main__":
    main()
