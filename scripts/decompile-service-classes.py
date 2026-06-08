#!/usr/bin/env python3
import json
import shutil
import subprocess
import zipfile
from pathlib import Path

REPO_ROOT = Path.cwd()
ADP_ROOT = REPO_ROOT.parent
BASE_SERVER = ADP_ROOT / "bap-server" / "base-Server"
OUT_ROOT = REPO_ROOT / "backend" / "decompiled-services"
TMP_ROOT = REPO_ROOT / "metadata" / "tmp" / "decompile"
METADATA_ROOT = REPO_ROOT / "metadata"
CFR_JAR = REPO_ROOT / "metadata" / "tools" / "cfr-0.152.jar"

WEBSOCKET_INCLUDE_PREFIXES = (
    "com/supcon/supfusion/",
)


def service_jars():
    for service_dir in sorted(path for path in BASE_SERVER.iterdir() if path.is_dir()):
        jars = sorted(service_dir.glob("*.jar"))
        if jars:
            yield service_dir.name, jars[0]


def add_to_zip(out_zip, source_path, archive_name):
    out_zip.write(source_path, archive_name)


def build_service_class_jar(service, jar_path):
    service_tmp = TMP_ROOT / service
    if service_tmp.exists():
        shutil.rmtree(service_tmp)
    class_root = service_tmp / "classes"
    resource_root = OUT_ROOT / service / "src" / "main" / "resources"
    class_root.mkdir(parents=True, exist_ok=True)
    resource_root.mkdir(parents=True, exist_ok=True)

    class_count = 0
    resource_count = 0
    with zipfile.ZipFile(jar_path) as archive:
        names = archive.namelist()
        boot_mode = any(name.startswith("BOOT-INF/classes/") for name in names)
        for name in names:
            if name.endswith("/"):
                continue
            rel_name = None
            if boot_mode and name.startswith("BOOT-INF/classes/"):
                rel_name = name.removeprefix("BOOT-INF/classes/")
            elif not boot_mode and name.endswith(".class") and name.startswith(WEBSOCKET_INCLUDE_PREFIXES):
                rel_name = name
            elif not boot_mode and not name.endswith(".class") and name.startswith(("application", "ws-config", "META-INF/")):
                rel_name = name

            if not rel_name:
                continue
            if rel_name.endswith(".class"):
                target = class_root / rel_name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(name))
                class_count += 1
            elif boot_mode:
                target = resource_root / rel_name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(name))
                resource_count += 1

    classes_jar = service_tmp / f"{service}-classes.jar"
    with zipfile.ZipFile(classes_jar, "w", zipfile.ZIP_DEFLATED) as out_zip:
        for class_file in class_root.rglob("*.class"):
            add_to_zip(out_zip, class_file, class_file.relative_to(class_root).as_posix())
    return classes_jar, class_count, resource_count


def decompile_service(service, jar_path):
    classes_jar, class_count, resource_count = build_service_class_jar(service, jar_path)
    out_dir = OUT_ROOT / service / "src" / "main" / "java"
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    if class_count == 0:
        return {
            "service": service,
            "jar": jar_path.relative_to(ADP_ROOT).as_posix(),
            "classCount": 0,
            "resourceCount": resource_count,
            "javaFiles": 0,
            "status": "no-classes",
        }
    cmd = [
        "java",
        "-jar",
        str(CFR_JAR),
        str(classes_jar),
        "--outputdir",
        str(out_dir),
        "--silent",
        "true",
        "--caseinsensitivefs",
        "true",
    ]
    subprocess.run(cmd, check=True)
    java_count = sum(1 for _ in out_dir.rglob("*.java"))
    return {
        "service": service,
        "jar": jar_path.relative_to(ADP_ROOT).as_posix(),
        "classCount": class_count,
        "resourceCount": resource_count,
        "javaFiles": java_count,
        "status": "ok",
    }


def main():
    if not CFR_JAR.exists():
        raise SystemExit(f"Missing CFR jar: {CFR_JAR}")
    OUT_ROOT.mkdir(parents=True, exist_ok=True)
    TMP_ROOT.mkdir(parents=True, exist_ok=True)
    METADATA_ROOT.mkdir(parents=True, exist_ok=True)

    results = []
    for service, jar_path in service_jars():
        results.append(decompile_service(service, jar_path))
    summary = {
        "decompiler": "CFR 0.152",
        "serviceCount": len(results),
        "totalClasses": sum(item["classCount"] for item in results),
        "totalJavaFiles": sum(item["javaFiles"] for item in results),
        "services": results,
    }
    (METADATA_ROOT / "backend-decompile-summary.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
