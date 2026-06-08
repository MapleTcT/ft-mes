#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path


DOCKER_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = DOCKER_DIR.parents[1]
SOURCE_FILE = (
    DOCKER_DIR
    / "patches"
    / "orchid-postgres-datasource"
    / "src"
    / "com"
    / "supcon"
    / "orchid"
    / "container"
    / "orm"
    / "config"
    / "DataSourceConfig.java"
)
CLASS_FILE = (
    DOCKER_DIR
    / "patches"
    / "orchid-postgres-datasource"
    / "classes"
    / "com"
    / "supcon"
    / "orchid"
    / "container"
    / "orm"
    / "config"
    / "DataSourceConfig.class"
)
TARGET_CLASS = "com/supcon/orchid/container/orm/config/DataSourceConfig.class"
ORM_JAR_PATTERN = re.compile(r"BOOT-INF/lib/com\.supcon\.greendill\.container\.orm-[^/]+\.jar$")
DEFAULT_SERVICES = ("baseService", "operatetools")


def find_one(repo: Path, pattern: str, preferred: str | None = None) -> Path:
    matches = sorted(repo.glob(pattern))
    if preferred:
        preferred_matches = [path for path in matches if preferred in path.name or preferred in str(path)]
        if preferred_matches:
            return preferred_matches[-1]
    if not matches:
        raise FileNotFoundError(f"missing dependency: {pattern}")
    return matches[-1]


def runtime_root_default() -> Path:
    candidate = PROJECT_ROOT / "runtime" / "bap-server"
    if candidate.exists():
        return candidate
    sibling = PROJECT_ROOT.parent / "bap-server"
    if sibling.exists():
        return sibling
    return candidate


def dependency_classpath(runtime_root: Path) -> str:
    repo = runtime_root / "assembly" / "repository" / "maven"
    jars = [
        find_one(repo, "com/supcon/greendill/container/com.supcon.greendill.container.orm/6.1.1.00/*.jar"),
        find_one(repo, "com/alibaba/druid/*/*.jar", "1.1.22"),
        find_one(repo, "org/slf4j/slf4j-api/*/*.jar", "1.7.30"),
        find_one(repo, "org/springframework/spring-beans/*/*.jar", "5.1.7.RELEASE"),
        find_one(repo, "org/springframework/spring-context/*/*.jar", "5.1.7.RELEASE"),
        find_one(repo, "org/springframework/spring-core/*/*.jar", "5.1.7.RELEASE"),
        find_one(repo, "org/springframework/spring-jcl/*/*.jar", "5.1.7.RELEASE"),
        find_one(repo, "org/springframework/boot/spring-boot/*/*.jar", "2.1.5.RELEASE"),
    ]
    return os.pathsep.join(str(path) for path in jars)


def build_class(runtime_root: Path, force: bool) -> Path:
    if CLASS_FILE.exists() and not force:
        return CLASS_FILE
    if not SOURCE_FILE.exists():
        raise FileNotFoundError(f"patch source not found: {SOURCE_FILE}")

    classes_dir = CLASS_FILE.parents[6]
    classes_dir.mkdir(parents=True, exist_ok=True)
    cmd = [
        "javac",
        "-encoding",
        "UTF-8",
        "-source",
        "1.8",
        "-target",
        "1.8",
        "-cp",
        dependency_classpath(runtime_root),
        "-d",
        str(classes_dir),
        str(SOURCE_FILE),
    ]
    subprocess.run(cmd, check=True)
    if not CLASS_FILE.exists():
        raise FileNotFoundError(f"compiled class not found: {CLASS_FILE}")
    return CLASS_FILE


def service_jar(runtime_root: Path, service: str) -> Path:
    service_dir = runtime_root / "base-Server" / service
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


def replace_class_in_nested_jar(nested_bytes: bytes, class_bytes: bytes) -> tuple[bytes, bool]:
    replaced = False
    with tempfile.TemporaryDirectory(prefix="orchid-orm-") as tmp:
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


def patch_service_jar(jar_path: Path, class_file: Path, backup_suffix: str) -> bool:
    class_bytes = class_file.read_bytes()
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        shutil.copy2(jar_path, backup)

    patched_nested = False
    with tempfile.TemporaryDirectory(prefix="orchid-service-") as tmp:
        output = Path(tmp) / jar_path.name
        with zipfile.ZipFile(jar_path, "r") as zin, zipfile.ZipFile(output, "w") as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                if ORM_JAR_PATTERN.match(info.filename):
                    data, replaced = replace_class_in_nested_jar(data, class_bytes)
                    patched_nested = patched_nested or replaced
                zout.writestr(zip_info_copy(info), data)

        if not patched_nested:
            raise FileNotFoundError(f"{jar_path} does not contain {TARGET_CLASS}")

        mode = jar_path.stat().st_mode
        shutil.move(str(output), jar_path)
        os.chmod(jar_path, mode)

    return True


def restore_service_jar(jar_path: Path, backup_suffix: str) -> bool:
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        return False
    shutil.copy2(backup, jar_path)
    return True


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Patch legacy Orchid ORM DataSourceConfig to support PostgreSQL in selected service jars."
    )
    parser.add_argument("--runtime-root", type=Path, default=runtime_root_default())
    parser.add_argument("--services", nargs="+", default=list(DEFAULT_SERVICES))
    parser.add_argument("--class-file", type=Path, default=CLASS_FILE)
    parser.add_argument("--backup-suffix", default=".pre-orchid-postgres.bak")
    parser.add_argument("--force-build", action="store_true")
    parser.add_argument("--build-only", action="store_true")
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()

    runtime_root = args.runtime_root.resolve()
    class_file = args.class_file.resolve()

    if not args.restore:
        if class_file == CLASS_FILE.resolve():
            class_file = build_class(runtime_root, args.force_build)
        elif not class_file.exists():
            raise FileNotFoundError(f"compiled class not found: {class_file}")
        print(f"using patch class: {class_file}")

    if args.build_only:
        return

    for service in args.services:
        jar_path = service_jar(runtime_root, service)
        if args.restore:
            restored = restore_service_jar(jar_path, args.backup_suffix)
            print(f"{'restored' if restored else 'missing backup for'} {service}: {jar_path}")
            continue
        patch_service_jar(jar_path, class_file, args.backup_suffix)
        print(f"patched {service}: {jar_path}")


if __name__ == "__main__":
    main()
