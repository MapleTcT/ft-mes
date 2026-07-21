#!/bin/sh
set -eu

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
repo_root="$(CDPATH= cd -- "$docker_dir/../.." && pwd)"
adp_root="$(CDPATH= cd -- "$repo_root/.." && pwd)"
runtime_dir="${1:-$repo_root/runtime/bap-server}"
python_bin="${PYTHON:-python3}"

source_file="${QCS_INSPECT_REPORT_SOURCE:-$adp_root/mes-modules-source-repo/modules/lims/QCS_6.1.3.5/service/src/main/java/com/supcon/orchid/QCS/services/impl/QCSInspectReportServiceImpl.java}"
limsbasic_source_file="${LIMSBASIC_WOM_SOURCE_RESPONSE_SOURCE:-$docker_dir/patches/limsbasic-wom-source-response/src/com/supcon/orchid/LIMSBasic/utils/ServiceClientUtils.java}"
lims_jar="${LIMS_BOOT_JAR:-$runtime_dir/module-Server/LIMS/manual/LIMS-1.0.0.jar}"

if [ ! -f "$lims_jar" ]; then
  echo "skip LIMS service patches; boot jar not found: $lims_jar" >&2
  exit 0
fi

if [ ! -f "$limsbasic_source_file" ]; then
  echo "LIMSBasic WOM response compatibility source not found: $limsbasic_source_file" >&2
  exit 1
fi

build_dir="$(mktemp -d "${TMPDIR:-/tmp}/adp-qcs-service-patch.XXXXXX")"
trap 'rm -rf "$build_dir"' EXIT

classes_dir="$build_dir/classes"
mkdir -p "$classes_dir"

has_qcs_nested="$($python_bin - "$lims_jar" "$build_dir" <<'PY'
from pathlib import Path
from zipfile import ZipFile
import sys

outer = Path(sys.argv[1])
build = Path(sys.argv[2]).resolve()
has_qcs = False
with ZipFile(outer, "r") as zf:
    for info in zf.infolist():
        name = info.filename
        if name.startswith("BOOT-INF/lib/com.supcon.greendill.QCS.service-") and name.endswith(".jar"):
            has_qcs = True
        if not (name.startswith("BOOT-INF/lib/") or name.startswith("BOOT-INF/classes/")):
            continue
        target = (build / name).resolve()
        if build not in target.parents and target != build:
            raise SystemExit(f"unsafe boot jar entry: {name}")
        if info.is_dir():
            target.mkdir(parents=True, exist_ok=True)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(zf.read(info))
print("true" if has_qcs else "false")
PY
)"

qcs_source_enabled=false
if [ -f "$source_file" ] && [ "$has_qcs_nested" = true ]; then
  qcs_source_enabled=true
else
  echo "skip optional LIMS QCS inspect-report source patch for: $lims_jar" >&2
fi

compile_java() {
  classpath="$build_dir/BOOT-INF/classes:$build_dir/BOOT-INF/lib/*"
  if [ -n "${JAVAC:-}" ]; then
    "$JAVAC" -encoding UTF-8 -source 8 -target 8 -cp "$classpath" -d "$classes_dir" "$@"
    return
  fi
  if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -source 8 -target 8 -cp "$classpath" -d "$classes_dir" "$@"
    return
  fi
  if ! command -v docker >/dev/null 2>&1; then
    echo "javac is unavailable and Docker fallback cannot be used" >&2
    exit 1
  fi
  java8_jdk_image="${ADP_JAVA8_JDK_IMAGE:-m.daocloud.io/docker.io/eclipse-temurin:8-jdk}"
  if ! docker image inspect "$java8_jdk_image" >/dev/null 2>&1; then
    echo "Java 8 JDK image is not available locally: $java8_jdk_image" >&2
    exit 1
  fi
  for java_source in "$@"; do
    case "$java_source" in
      "$adp_root"/*) ;;
      *)
        echo "Docker javac fallback only accepts sources under $adp_root: $java_source" >&2
        exit 1
        ;;
    esac
  done
  docker run --rm \
    --user "$(id -u):$(id -g)" \
    -v "$build_dir:$build_dir" \
    -v "$adp_root:$adp_root:ro" \
    -w "$repo_root" \
    "$java8_jdk_image" \
    javac -encoding UTF-8 -source 8 -target 8 \
      -cp "$classpath" -d "$classes_dir" "$@"
}

if [ "$qcs_source_enabled" = true ]; then
  compile_java "$source_file" "$limsbasic_source_file"
else
  compile_java "$limsbasic_source_file"
fi

"$python_bin" - "$lims_jar" "$classes_dir" "$qcs_source_enabled" <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZipInfo
import os
import shutil
import sys
import tempfile
import time

outer = Path(sys.argv[1])
class_root = Path(sys.argv[2])
qcs_source_enabled = sys.argv[3] == "true"
stamp = time.strftime("%Y%m%d%H%M%S")
backup = outer.with_name(outer.name + f".bak-lims-service-compat-{stamp}")
shutil.copy2(outer, backup)

with ZipFile(outer, "r") as zf:
    outer_entries = [(info, zf.read(info.filename)) for info in zf.infolist()]
    outer_data = dict((info.filename, data) for info, data in outer_entries)

limsbasic_jars = sorted(
    name
    for name in outer_data
    if name.startswith("BOOT-INF/lib/com.supcon.greendill.LIMSBasic.service-")
    and name.endswith(".jar")
)
if len(limsbasic_jars) != 1:
    raise SystemExit(f"expected one nested LIMSBasic service jar, found {limsbasic_jars}")

replacements = {
    limsbasic_jars[0]: [
        "com/supcon/orchid/LIMSBasic/utils/ServiceClientUtils.class",
    ],
}
if qcs_source_enabled:
    qcs_jars = sorted(
        name
        for name in outer_data
        if name.startswith("BOOT-INF/lib/com.supcon.greendill.QCS.service-")
        and name.endswith(".jar")
    )
    if len(qcs_jars) != 1:
        raise SystemExit(f"expected one nested QCS service jar, found {qcs_jars}")
    replacements[qcs_jars[0]] = [
        "com/supcon/orchid/QCS/services/impl/QCSInspectReportServiceImpl.class",
        "com/supcon/orchid/QCS/services/impl/QCSInspectReportServiceImpl$1.class",
    ]

with tempfile.TemporaryDirectory() as temp_dir:
    temp_dir = Path(temp_dir)
    outer_out = temp_dir / "outer.new.jar"
    patched_inner = {}
    for index, (inner_name, replace_names) in enumerate(replacements.items()):
        inner_in = temp_dir / f"inner-{index}.jar"
        inner_out = temp_dir / f"inner-{index}.new.jar"
        inner_in.write_bytes(outer_data[inner_name])
        with ZipFile(inner_in, "r") as zin, ZipFile(inner_out, "w") as zout:
            seen = set()
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename in replace_names:
                    data = (class_root / info.filename).read_bytes()
                zout.writestr(info, data)
                seen.add(info.filename)
            for name in replace_names:
                if name not in seen:
                    zi = ZipInfo(name)
                    zi.compress_type = 8
                    zout.writestr(zi, (class_root / name).read_bytes())
        patched_inner[inner_name] = inner_out.read_bytes()

    with ZipFile(outer_out, "w") as zout:
        for info, data in outer_entries:
            if info.filename in patched_inner:
                data = patched_inner[info.filename]
            zout.writestr(info, data)

    shutil.move(str(outer_out), outer)

with ZipFile(outer, "r") as zf:
    for inner_name, replace_names in replacements.items():
        info = zf.getinfo(inner_name)
        if info.compress_type != 0:
            raise SystemExit(
                f"nested boot jar {inner_name} must stay STORED; got compress_type={info.compress_type}"
            )
        inner_bytes = zf.read(inner_name)
        with tempfile.NamedTemporaryFile(delete=False) as temp_file:
            temp_file.write(inner_bytes)
            temp_path = temp_file.name
        try:
            with ZipFile(temp_path, "r") as inner_zip:
                for name in replace_names:
                    matches = [item for item in inner_zip.infolist() if item.filename == name]
                    if len(matches) != 1:
                        raise SystemExit(
                            f"expected one {name} in {inner_name}, found {len(matches)}"
                        )
        finally:
            os.unlink(temp_path)

print(f"patched {outer}")
print(f"backup {backup}")
PY
