#!/bin/sh
set -eu

usage() {
  cat >&2 <<'USAGE'
Usage:
  build-foundation-simulated-login-serverport-patch.sh --input-boot-jar PATH --output-boot-jar PATH

Builds a patched Spring Boot jar by replacing SimulatedLoginService inside the
nested com.supcon.greendill.foundation.services jar. Do not install this class
as an external runtime patch jar; Spring Boot nested dependencies must remain in
the same class loader for this legacy service.

The legacy --input-lims-jar and --output-lims-jar aliases remain supported.
USAGE
}

input_boot_jar=""
output_boot_jar=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --input-boot-jar|--input-lims-jar)
      input_boot_jar="${2:-}"
      shift 2
      ;;
    --output-boot-jar|--output-lims-jar)
      output_boot_jar="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

if [ -z "$input_boot_jar" ] || [ -z "$output_boot_jar" ]; then
  usage
  exit 2
fi

if [ ! -f "$input_boot_jar" ]; then
  echo "input Spring Boot jar not found: $input_boot_jar" >&2
  exit 1
fi

for command_name in javac jar unzip zip python3; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "missing required command: $command_name" >&2
    exit 1
  fi
done

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
patch_root="$docker_dir/patches/foundation-simulated-login-serverport"
src_file="$patch_root/src/com/supcon/orchid/foundation/internal/services/SimulatedLoginService.java"

if [ ! -f "$src_file" ]; then
  echo "patch source not found: $src_file" >&2
  exit 1
fi

abs_path() {
  python3 -c 'import os, sys; print(os.path.abspath(sys.argv[1]))' "$1"
}

input_boot_jar="$(abs_path "$input_boot_jar")"
output_boot_jar="$(abs_path "$output_boot_jar")"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/adp-foundation-simlogin.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT INT TERM

outer_dir="$tmp_dir/outer"
classes_dir="$tmp_dir/classes"
mkdir -p "$outer_dir" "$classes_dir" "$(dirname "$output_boot_jar")"

unzip -q "$input_boot_jar" 'BOOT-INF/lib/*.jar' -d "$outer_dir"

foundation_services_jar="$(
  find "$outer_dir/BOOT-INF/lib" -maxdepth 1 -name 'com.supcon.greendill.foundation.services-*.jar' | sort | head -1
)"
if [ -z "$foundation_services_jar" ]; then
  echo "nested foundation services jar not found in $input_boot_jar" >&2
  exit 1
fi

classpath="$(
  find "$outer_dir/BOOT-INF/lib" -maxdepth 1 -name '*.jar' | sort | tr '\n' ':'
)"

javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$classpath" \
  -d "$classes_dir" \
  "$src_file"

cp "$foundation_services_jar" "$tmp_dir/foundation-services.jar"
jar uf "$tmp_dir/foundation-services.jar" -C "$classes_dir" com/supcon/orchid/foundation/internal/services/SimulatedLoginService.class
cp "$tmp_dir/foundation-services.jar" "$foundation_services_jar"
cp "$input_boot_jar" "$output_boot_jar"

(
  cd "$outer_dir"
  zip -0 -q -u "$output_boot_jar" "BOOT-INF/lib/$(basename "$foundation_services_jar")"
)

python3 - "$output_boot_jar" "BOOT-INF/lib/$(basename "$foundation_services_jar")" <<'PY'
import io
import sys
import zipfile

outer_path, nested_name = sys.argv[1:]
class_name = "com/supcon/orchid/foundation/internal/services/SimulatedLoginService.class"

with zipfile.ZipFile(outer_path) as outer:
    names = [entry.filename for entry in outer.infolist()]
    if len(names) != len(set(names)):
        raise SystemExit("patched boot jar contains duplicate ZIP entries")
    if outer.testzip() is not None:
        raise SystemExit("patched boot jar failed CRC validation")
    nested_info = outer.getinfo(nested_name)
    if nested_info.compress_type != zipfile.ZIP_STORED:
        raise SystemExit("Spring Boot nested foundation jar is not ZIP_STORED")
    nested_bytes = outer.read(nested_name)

with zipfile.ZipFile(io.BytesIO(nested_bytes)) as nested:
    names = [entry.filename for entry in nested.infolist()]
    if len(names) != len(set(names)):
        raise SystemExit("patched foundation jar contains duplicate ZIP entries")
    if nested.testzip() is not None:
        raise SystemExit("patched foundation jar failed CRC validation")
    if names.count(class_name) != 1:
        raise SystemExit("patched SimulatedLoginService class is missing or duplicated")
PY

echo "built patched Spring Boot jar: $output_boot_jar"
