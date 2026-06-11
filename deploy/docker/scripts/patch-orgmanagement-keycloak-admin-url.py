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
AUTH_MANAGER_JAR = "BOOT-INF/lib/auth-manager-1.0.0-SNAPSHOT.jar"
KEYCLOAK_ADMIN_CLASS = "com/supcon/supfusion/auth/manager/KeycliandAdminClient.class"
OLD_URL = b"http://127.0.0.1:8010/auth"
NEW_URL = b"http://keycloak:8080/auth"


def runtime_root_default() -> Path:
    candidate = PROJECT_ROOT / "runtime" / "bap-server"
    if candidate.exists():
        return candidate
    sibling = PROJECT_ROOT.parent / "bap-server"
    if sibling.exists():
        return sibling
    return candidate


def service_jar(runtime_root: Path) -> Path:
    service_dir = runtime_root / "base-Server" / "orgmanagement"
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


def replace_utf8_constant(class_bytes: bytes) -> tuple[bytes, int]:
    old_entry = len(OLD_URL).to_bytes(2, "big") + OLD_URL
    new_entry = len(NEW_URL).to_bytes(2, "big") + NEW_URL
    if old_entry in class_bytes:
        return class_bytes.replace(old_entry, new_entry, 1), 1
    if new_entry in class_bytes:
        return class_bytes, 0
    raise ValueError("expected Keycloak admin URL constant not found")


def replace_class_in_nested_jar(nested_bytes: bytes) -> tuple[bytes, int]:
    with tempfile.TemporaryDirectory(prefix="auth-manager-") as tmp:
        nested_in = Path(tmp) / "in.jar"
        nested_out = Path(tmp) / "out.jar"
        nested_in.write_bytes(nested_bytes)
        changes = 0
        with zipfile.ZipFile(nested_in, "r") as zin, zipfile.ZipFile(nested_out, "w") as zout:
            if KEYCLOAK_ADMIN_CLASS not in zin.namelist():
                raise FileNotFoundError(f"missing class: {KEYCLOAK_ADMIN_CLASS}")
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename == KEYCLOAK_ADMIN_CLASS:
                    data, changes = replace_utf8_constant(data)
                zout.writestr(zip_info_copy(info), data)
        return nested_out.read_bytes(), changes


def patch_service_jar(jar_path: Path, backup_suffix: str) -> int:
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        shutil.copy2(jar_path, backup)

    changes = 0
    patched = False
    with tempfile.TemporaryDirectory(prefix="orgmanagement-keycloak-") as tmp:
        output = Path(tmp) / jar_path.name
        with zipfile.ZipFile(jar_path, "r") as zin, zipfile.ZipFile(output, "w") as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename == AUTH_MANAGER_JAR:
                    data, changes = replace_class_in_nested_jar(data)
                    patched = True
                zout.writestr(zip_info_copy(info), data)

        if not patched:
            raise FileNotFoundError(f"{jar_path} does not contain {AUTH_MANAGER_JAR}")

        mode = jar_path.stat().st_mode
        shutil.move(str(output), jar_path)
        os.chmod(jar_path, mode)
    return changes


def restore_service_jar(jar_path: Path, backup_suffix: str) -> bool:
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        return False
    shutil.copy2(backup, jar_path)
    return True


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Patch orgmanagement Keycloak admin URL for the Docker test profile."
    )
    parser.add_argument("--runtime-root", type=Path, default=runtime_root_default())
    parser.add_argument("--backup-suffix", default=".pre-keycloak-admin-url.bak")
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()

    jar_path = service_jar(args.runtime_root.resolve())
    if args.restore:
        restored = restore_service_jar(jar_path, args.backup_suffix)
        print(f"{'restored' if restored else 'missing backup for'} orgmanagement: {jar_path}")
        return

    changes = patch_service_jar(jar_path, args.backup_suffix)
    state = "patched" if changes else "already patched"
    print(f"{state} orgmanagement Keycloak admin URL: {jar_path}")


if __name__ == "__main__":
    main()
