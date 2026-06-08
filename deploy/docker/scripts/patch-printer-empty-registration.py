#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path


DOCKER_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = DOCKER_DIR.parents[1]
PATCH_ROOT = DOCKER_DIR / "patches" / "printer-empty-registration"
SOURCE_FILE = (
    PATCH_ROOT
    / "src"
    / "com"
    / "supcon"
    / "supfusion"
    / "printer"
    / "service"
    / "impl"
    / "PrinterAppDataServiceImpl.java"
)
CLASSES_DIR = PATCH_ROOT / "classes"
CLASS_FILE = CLASSES_DIR / "com/supcon/supfusion/printer/service/impl/PrinterAppDataServiceImpl.class"
TARGET_CLASS = "com/supcon/supfusion/printer/service/impl/PrinterAppDataServiceImpl.class"
PRINTER_SERVICE_JAR = "BOOT-INF/lib/printer-service-1.0-SNAPSHOT.jar"


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


def extract_nested_libs(jar_path: Path, target_dir: Path) -> list[Path]:
    libs: list[Path] = []
    with zipfile.ZipFile(jar_path, "r") as app_jar:
        for info in app_jar.infolist():
            if not info.filename.startswith("BOOT-INF/lib/") or not info.filename.endswith(".jar"):
                continue
            out = target_dir / Path(info.filename).name
            out.write_bytes(app_jar.read(info.filename))
            libs.append(out)
    if not any(path.name == Path(PRINTER_SERVICE_JAR).name for path in libs):
        raise FileNotFoundError(f"{jar_path} does not contain {PRINTER_SERVICE_JAR}")
    return libs


def build_class(jar_path: Path, force: bool) -> Path:
    if CLASS_FILE.exists() and not force:
        return CLASS_FILE
    if not SOURCE_FILE.exists():
        raise FileNotFoundError(f"patch source not found: {SOURCE_FILE}")

    if CLASSES_DIR.exists():
        shutil.rmtree(CLASSES_DIR)
    CLASSES_DIR.mkdir(parents=True)

    with tempfile.TemporaryDirectory(prefix="printer-libs-") as tmp:
        libs = extract_nested_libs(jar_path, Path(tmp))
        cmd = [
            "javac",
            "-encoding",
            "UTF-8",
            "-source",
            "1.8",
            "-target",
            "1.8",
            "-cp",
            os.pathsep.join(str(path) for path in libs),
            "-d",
            str(CLASSES_DIR),
            str(SOURCE_FILE),
        ]
        subprocess.run(cmd, check=True)

    if not CLASS_FILE.exists():
        raise FileNotFoundError(f"compiled class not found: {CLASS_FILE}")
    return CLASS_FILE


def replace_class_in_nested_jar(nested_bytes: bytes, class_bytes: bytes) -> tuple[bytes, bool]:
    replaced = False
    with tempfile.TemporaryDirectory(prefix="printer-nested-") as tmp:
        nested_in = Path(tmp) / "in.jar"
        nested_out = Path(tmp) / "out.jar"
        nested_in.write_bytes(nested_bytes)
        with zipfile.ZipFile(nested_in, "r") as zin, zipfile.ZipFile(nested_out, "w") as zout:
            for info in zin.infolist():
                data = class_bytes if info.filename == TARGET_CLASS else zin.read(info.filename)
                if info.filename == TARGET_CLASS:
                    replaced = True
                zout.writestr(zip_info_copy(info), data)
        return nested_out.read_bytes(), replaced


def patch_service_jar(jar_path: Path, class_file: Path, backup_suffix: str) -> None:
    class_bytes = class_file.read_bytes()
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        shutil.copy2(jar_path, backup)

    patched = False
    with tempfile.TemporaryDirectory(prefix="printer-service-") as tmp:
        output = Path(tmp) / jar_path.name
        with zipfile.ZipFile(jar_path, "r") as zin, zipfile.ZipFile(output, "w") as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename == PRINTER_SERVICE_JAR:
                    data, nested_patched = replace_class_in_nested_jar(data, class_bytes)
                    patched = patched or nested_patched
                zout.writestr(zip_info_copy(info), data)

        if not patched:
            raise FileNotFoundError(f"{jar_path} does not contain {TARGET_CLASS}")

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
        description="Patch printer app data service to return an empty list when no printer register exists."
    )
    parser.add_argument("--runtime-root", type=Path, default=runtime_root_default())
    parser.add_argument("--backup-suffix", default=".pre-printer-empty-registration.bak")
    parser.add_argument("--force-build", action="store_true")
    parser.add_argument("--build-only", action="store_true")
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()

    jar_path = service_jar(args.runtime_root.resolve())

    if args.restore:
        restored = restore_service_jar(jar_path, args.backup_suffix)
        print(f"{'restored' if restored else 'missing backup for'} basicmanagement: {jar_path}")
        return

    class_file = build_class(jar_path, args.force_build)
    print(f"using patch class: {class_file}")
    if args.build_only:
        return

    patch_service_jar(jar_path, class_file, args.backup_suffix)
    print(f"patched basicmanagement: {jar_path}")


if __name__ == "__main__":
    main()
