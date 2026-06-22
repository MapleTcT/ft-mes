#!/usr/bin/env python3
"""Patch WorkAppointment upload-list attachment permission parsing.

The recovered WorkAppointment controller assumes findShadowCode values are
Strings.  PostgreSQL returns numeric shadow flags as Long/Integer, which makes
`/workAppointment/baseService/workbench/upload-list` fail before it can return
an empty attachment list.  This patches the controller class inside the WAPS
Spring Boot jar with the same tolerant logic used for the WTS recovery path.
"""

from __future__ import annotations

import argparse
import io
import shutil
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path


INNER_SERVICE_JAR = "BOOT-INF/lib/com.supcon.greendill.workAppointment.service-6.1.8.2.jar"
CONTROLLER_CLASS = "com/supcon/orchid/workAppointment/controllers/WorkAppointmentFileDownloadController.class"

OLD_BLOCK = '''\t\t\tMap isShadow = viewServiceFoundation.findShadowCode(viewCode);
\t\t\tString attViewCode = viewCode;
\t\t\tif(null != isShadow.get("IS_SHADOW") && (Integer.parseInt(isShadow.get("IS_SHADOW").toString()) == 1 || Boolean.valueOf(isShadow.get("IS_SHADOW").toString()))){
\t\t\t\tif(null != isShadow.get("SHADOW_VIEW_CODE")){
\t\t\t\t\tattViewCode = (String) isShadow.get("SHADOW_VIEW_CODE");
\t\t\t\t}
\t\t\t}
\t\t\tString resultStr = viewServiceFoundation.findViewJsonByXML(attViewCode);
\t\t\tString viewType = null;
\t\t\tif(null != isShadow.get("TYPE")){
\t\t\t\tviewType = (String)isShadow.get("TYPE");
\t\t\t\tif (ViewType.EDIT.equals(getViewType(viewType)) || ViewType.VIEW.equals(getViewType(viewType))
\t\t\t\t\t\t|| ViewType.EXTRA.equals(getViewType(viewType))) {
\t\t\t\t\tmapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
\t\t\t\t\tMap resultMap = mapper.readValue(resultStr, Map.class);
\t\t\t\t\tattPermission(resultMap, attPermissionMap);
\t\t\t\t}
\t\t\t}
'''

NEW_BLOCK = '''    \t\ttry {
\t\t\t\tMap isShadow = viewServiceFoundation.findShadowCode(viewCode);
\t\t\t\tString attViewCode = viewCode;
\t\t\t\tObject isShadowValue = isShadow == null ? null : isShadow.get("IS_SHADOW");
\t\t\t\tboolean shadow = false;
\t\t\t\tif (isShadowValue instanceof Number) {
\t\t\t\t\tshadow = ((Number) isShadowValue).intValue() == 1;
\t\t\t\t} else if (isShadowValue != null) {
\t\t\t\t\tString shadowText = isShadowValue.toString();
\t\t\t\t\tshadow = "1".equals(shadowText) || Boolean.valueOf(shadowText);
\t\t\t\t}
\t\t\t\tif(shadow){
\t\t\t\t\tif(null != isShadow.get("SHADOW_VIEW_CODE")){
\t\t\t\t\t\tattViewCode = (String) isShadow.get("SHADOW_VIEW_CODE");
\t\t\t\t\t}
\t\t\t\t}
\t\t\t\tString resultStr = viewServiceFoundation.findViewJsonByXML(attViewCode);
\t\t\t\tString viewType = null;
\t\t\t\tif(isShadow != null && null != isShadow.get("TYPE")){
\t\t\t\t\tviewType = String.valueOf(isShadow.get("TYPE"));
\t\t\t\t\tif (ViewType.EDIT.equals(getViewType(viewType)) || ViewType.VIEW.equals(getViewType(viewType))
\t\t\t\t\t\t\t|| ViewType.EXTRA.equals(getViewType(viewType))) {
\t\t\t\t\t\tmapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
\t\t\t\t\t\tMap resultMap = mapper.readValue(resultStr, Map.class);
\t\t\t\t\t\tattPermission(resultMap, attPermissionMap);
\t\t\t\t\t}
\t\t\t\t}
    \t\t} catch (Exception ex) {
    \t\t\tlogger.warn("Skip WorkAppointment attachment permission parsing for viewCode {}: {}", viewCode, ex.toString());
    \t\t}
'''


def copy_zip_info(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    copied = zipfile.ZipInfo(info.filename, info.date_time)
    copied.compress_type = info.compress_type
    copied.comment = info.comment
    copied.extra = info.extra
    copied.internal_attr = info.internal_attr
    copied.external_attr = info.external_attr
    return copied


def extract_boot_libs(jar_path: Path, lib_dir: Path) -> None:
    with zipfile.ZipFile(jar_path, "r") as outer:
        for name in outer.namelist():
            if name.startswith("BOOT-INF/lib/") and name.endswith(".jar"):
                (lib_dir / Path(name).name).write_bytes(outer.read(name))


def compile_controller(jar_path: Path, source_path: Path) -> bytes:
    source_text = source_path.read_text(encoding="utf-8")
    if NEW_BLOCK in source_text:
        patched_source = source_text
    elif OLD_BLOCK in source_text:
        patched_source = source_text.replace(OLD_BLOCK, NEW_BLOCK, 1)
    else:
        raise RuntimeError("target WorkAppointment upload-list block was not found")

    javac = shutil.which("javac")
    if not javac:
        raise RuntimeError("javac is required")

    with tempfile.TemporaryDirectory(prefix="adp-waps-upload-list-") as temp_name:
        temp_dir = Path(temp_name)
        lib_dir = temp_dir / "lib"
        source_dir = temp_dir / "src/com/supcon/orchid/workAppointment/controllers"
        classes_dir = temp_dir / "classes"
        lib_dir.mkdir(parents=True)
        source_dir.mkdir(parents=True)
        classes_dir.mkdir(parents=True)

        patched_source_path = source_dir / "WorkAppointmentFileDownloadController.java"
        patched_source_path.write_text(patched_source, encoding="utf-8")
        extract_boot_libs(jar_path, lib_dir)
        classpath = ":".join(str(path) for path in sorted(lib_dir.glob("*.jar")))
        subprocess.run(
            [
                javac,
                "-encoding",
                "UTF-8",
                "-parameters",
                "-g",
                "-cp",
                classpath,
                "-d",
                str(classes_dir),
                str(patched_source_path),
            ],
            check=True,
        )
        class_path = classes_dir / CONTROLLER_CLASS
        if not class_path.exists():
            raise RuntimeError(f"compiled class was not created: {class_path}")
        return class_path.read_bytes()


def patch_inner_service_jar(data: bytes, controller_class: bytes) -> bytes:
    output = io.BytesIO()
    replaced = False
    with zipfile.ZipFile(io.BytesIO(data), "r") as source:
        with zipfile.ZipFile(output, "w") as target:
            for info in source.infolist():
                content = controller_class if info.filename == CONTROLLER_CLASS else source.read(info.filename)
                if info.filename == CONTROLLER_CLASS:
                    replaced = True
                target.writestr(copy_zip_info(info), content)
    if not replaced:
        raise RuntimeError(f"missing controller class in service jar: {CONTROLLER_CLASS}")
    return output.getvalue()


def patch_waps_jar(jar_path: Path, source_path: Path, controller_class_path: Path | None = None) -> dict:
    if controller_class_path:
        controller_class = controller_class_path.read_bytes()
    else:
        controller_class = compile_controller(jar_path, source_path)
    original = jar_path.read_bytes()
    output = io.BytesIO()
    replaced_inner = False
    with zipfile.ZipFile(io.BytesIO(original), "r") as source:
        if INNER_SERVICE_JAR not in source.namelist():
            raise RuntimeError(f"missing nested service jar: {INNER_SERVICE_JAR}")
        with zipfile.ZipFile(output, "w") as target:
            for info in source.infolist():
                content = source.read(info.filename)
                if info.filename == INNER_SERVICE_JAR:
                    content = patch_inner_service_jar(content, controller_class)
                    replaced_inner = True
                target.writestr(copy_zip_info(info), content)
    if not replaced_inner:
        raise RuntimeError(f"missing nested service jar: {INNER_SERVICE_JAR}")

    patched = output.getvalue()
    if patched == original:
        return {"changed": False, "jar": str(jar_path)}

    backup = jar_path.with_name(jar_path.name + f".bak-workappointment-upload-list-{int(time.time())}")
    shutil.copy2(jar_path, backup)
    temp_path = jar_path.with_name(jar_path.name + ".tmp-workappointment-upload-list")
    temp_path.write_bytes(patched)
    shutil.copystat(jar_path, temp_path)
    temp_path.replace(jar_path)
    return {"changed": True, "jar": str(jar_path), "backup": str(backup)}


def parse_args() -> argparse.Namespace:
    script_path = Path(__file__).resolve()
    root = next((parent for parent in script_path.parents if parent.name == "adp-source-repo"), Path.cwd())
    default_source = (
        root.parent
        / "mes-modules-source-repo/modules/wts/workAppointment_6.1.8.2/service/src/main/java/com/supcon/orchid/workAppointment/controllers/WorkAppointmentFileDownloadController.java"
    )
    default_jar = root.parent / "mes-modules-source-repo/deploy/staging/ms-services-runtime-20260613/WAPS/manual/WAPS-1.0.0.jar"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--waps-jar", type=Path, default=default_jar)
    parser.add_argument("--source", type=Path, default=default_source)
    parser.add_argument(
        "--controller-class",
        type=Path,
        help="precompiled WorkAppointmentFileDownloadController.class for hosts without javac",
    )
    parser.add_argument(
        "--write-controller-class",
        type=Path,
        help="compile the patched controller class locally and write it without modifying the jar",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    waps_jar = args.waps_jar.expanduser().resolve()
    source = args.source.expanduser().resolve()
    if args.write_controller_class:
        output_path = args.write_controller_class.expanduser().resolve()
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_bytes(compile_controller(waps_jar, source))
        print({"controllerClass": str(output_path)})
        return 0

    controller_class = args.controller_class.expanduser().resolve() if args.controller_class else None
    result = patch_waps_jar(waps_jar, source, controller_class)
    print(result)
    return 0


if __name__ == "__main__":
    sys.exit(main())
