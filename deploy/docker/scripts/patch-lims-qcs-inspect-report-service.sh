#!/bin/sh
set -eu

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
repo_root="$(CDPATH= cd -- "$docker_dir/../.." && pwd)"
adp_root="$(CDPATH= cd -- "$repo_root/.." && pwd)"
runtime_dir="${1:-$repo_root/runtime/bap-server}"
python_bin="${PYTHON:-python3}"

source_file="${QCS_INSPECT_REPORT_SOURCE:-$adp_root/mes-modules-source-repo/modules/lims/QCS_6.1.3.5/service/src/main/java/com/supcon/orchid/QCS/services/impl/QCSInspectReportServiceImpl.java}"
lims_jar="${LIMS_BOOT_JAR:-$runtime_dir/module-Server/LIMS/manual/LIMS-1.0.0.jar}"

if [ ! -f "$source_file" ]; then
  echo "skip LIMS QCS inspect-report service patch; source not found: $source_file" >&2
  exit 0
fi

if [ ! -f "$lims_jar" ]; then
  echo "skip LIMS QCS inspect-report service patch; boot jar not found: $lims_jar" >&2
  exit 0
fi

build_dir="$(mktemp -d "${TMPDIR:-/tmp}/adp-qcs-service-patch.XXXXXX")"
trap 'rm -rf "$build_dir"' EXIT

classes_dir="$build_dir/classes"
mkdir -p "$classes_dir"
(
  cd "$build_dir"
  jar xf "$lims_jar" BOOT-INF/lib BOOT-INF/classes
)

javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$build_dir/BOOT-INF/classes:$build_dir/BOOT-INF/lib/*" \
  -d "$classes_dir" \
  "$source_file"

"$python_bin" - "$lims_jar" "$classes_dir" <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZipInfo
import os
import shutil
import sys
import tempfile
import time

outer = Path(sys.argv[1])
class_root = Path(sys.argv[2])
inner_name = "BOOT-INF/lib/com.supcon.greendill.QCS.service-6.1.3.5.jar"
replace_names = [
    "com/supcon/orchid/QCS/services/impl/QCSInspectReportServiceImpl.class",
    "com/supcon/orchid/QCS/services/impl/QCSInspectReportServiceImpl$1.class",
]

stamp = time.strftime("%Y%m%d%H%M%S")
backup = outer.with_name(outer.name + f".bak-qcs-inspect-report-service-{stamp}")
shutil.copy2(outer, backup)

with ZipFile(outer, "r") as zf:
    outer_entries = [(info, zf.read(info.filename)) for info in zf.infolist()]
    inner_bytes = dict((info.filename, data) for info, data in outer_entries)[inner_name]

with tempfile.TemporaryDirectory() as temp_dir:
    temp_dir = Path(temp_dir)
    inner_in = temp_dir / "inner.jar"
    inner_out = temp_dir / "inner.new.jar"
    outer_out = temp_dir / "outer.new.jar"
    inner_in.write_bytes(inner_bytes)

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

    new_inner = inner_out.read_bytes()
    with ZipFile(outer_out, "w") as zout:
        for info, data in outer_entries:
            if info.filename == inner_name:
                data = new_inner
            zout.writestr(info, data)

    shutil.move(str(outer_out), outer)

with ZipFile(outer, "r") as zf:
    info = zf.getinfo(inner_name)
    if info.compress_type != 0:
        raise SystemExit(f"nested boot jar must stay STORED; got compress_type={info.compress_type}")
    inner_bytes = zf.read(inner_name)

with tempfile.NamedTemporaryFile(delete=False) as temp_file:
    temp_file.write(inner_bytes)
    temp_path = temp_file.name
try:
    with ZipFile(temp_path, "r") as zf:
        for name in replace_names:
            matches = [info for info in zf.infolist() if info.filename == name]
            if len(matches) != 1:
                raise SystemExit(f"expected one {name} in nested QCS jar, found {len(matches)}")
finally:
    os.unlink(temp_path)

print(f"patched {outer}")
print(f"backup {backup}")
PY
