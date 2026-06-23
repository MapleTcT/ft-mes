#!/usr/bin/env python3
import argparse
import shutil
import tempfile
import time
import zipfile
from pathlib import Path


TARGET_CLASS = "com/supcon/orchid/ec/cache/BaseServiceCacheService.class"
INNER_JAR_PREFIX = "BOOT-INF/lib/com.supcon.greendill.foundation.services-"
INNER_JAR_SUFFIX = ".jar"


def default_paths() -> tuple[Path, Path]:
    script_dir = Path(__file__).resolve().parent
    docker_dir = script_dir.parent
    repo_root = docker_dir.parent.parent
    adp_root = repo_root.parent
    service_jar = adp_root / "runtime" / "bap-server" / "base-Server" / "baseService" / "supfusion-baseservice.jar"
    class_file = (
        docker_dir
        / "patches"
        / "base-service-layout-json-postgres"
        / "build"
        / "classes"
        / TARGET_CLASS
    )
    return service_jar, class_file


def find_inner_services_jar(outer: zipfile.ZipFile) -> str:
    candidates = [
        name
        for name in outer.namelist()
        if name.startswith(INNER_JAR_PREFIX) and name.endswith(INNER_JAR_SUFFIX)
    ]
    if not candidates:
        raise RuntimeError("foundation services jar not found inside baseService jar")
    return sorted(candidates)[-1]


def rewrite_zip_bytes(zip_bytes: bytes, replacement_class: bytes) -> bytes:
    source = tempfile.NamedTemporaryFile(delete=False)
    target = tempfile.NamedTemporaryFile(delete=False)
    try:
        source.write(zip_bytes)
        source.close()
        target.close()
        source_path = Path(source.name)
        target_path = Path(target.name)
        replaced = False
        with zipfile.ZipFile(source_path, "r") as zin, zipfile.ZipFile(target_path, "w") as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                if item.filename == TARGET_CLASS:
                    data = replacement_class
                    replaced = True
                zout.writestr(item, data)
        if not replaced:
            raise RuntimeError(f"{TARGET_CLASS} not found inside foundation services jar")
        return target_path.read_bytes()
    finally:
        Path(source.name).unlink(missing_ok=True)
        Path(target.name).unlink(missing_ok=True)


def patch_outer_jar(service_jar: Path, class_file: Path, backup: bool) -> Path | None:
    replacement_class = class_file.read_bytes()
    backup_path = None
    if backup:
        backup_path = service_jar.with_name(
            f"{service_jar.name}.bak-layout-json-postgres-{time.strftime('%Y%m%d%H%M%S')}"
        )
        shutil.copy2(service_jar, backup_path)

    tmp_path = service_jar.with_suffix(service_jar.suffix + ".tmp-layout-json-postgres")
    try:
        replaced_outer_class = False
        with zipfile.ZipFile(service_jar, "r") as outer:
            inner_name = find_inner_services_jar(outer)
            patched_inner = rewrite_zip_bytes(outer.read(inner_name), replacement_class)
            with zipfile.ZipFile(tmp_path, "w") as patched_outer:
                for item in outer.infolist():
                    if item.filename == inner_name:
                        data = patched_inner
                    elif item.filename.endswith(TARGET_CLASS):
                        data = replacement_class
                        replaced_outer_class = True
                    else:
                        data = outer.read(item.filename)
                    patched_outer.writestr(item, data)
        if not replaced_outer_class:
            raise RuntimeError(f"{TARGET_CLASS} not found in outer baseService jar")
        shutil.move(str(tmp_path), service_jar)
    finally:
        tmp_path.unlink(missing_ok=True)
    return backup_path


def main() -> None:
    default_service_jar, default_class_file = default_paths()
    parser = argparse.ArgumentParser(description="Patch baseService layoutJson PostgreSQL OID runtime compatibility.")
    parser.add_argument("--service-jar", type=Path, default=default_service_jar)
    parser.add_argument("--class-file", type=Path, default=default_class_file)
    parser.add_argument("--no-backup", action="store_true")
    args = parser.parse_args()

    if not args.service_jar.is_file():
        raise SystemExit(f"service jar not found: {args.service_jar}")
    if not args.class_file.is_file():
        raise SystemExit(f"class file not found, run build-base-service-layout-json-postgres-patch.sh first: {args.class_file}")

    backup_path = patch_outer_jar(args.service_jar, args.class_file, backup=not args.no_backup)
    print(f"patched {args.service_jar}")
    if backup_path:
        print(f"backup {backup_path}")


if __name__ == "__main__":
    main()
