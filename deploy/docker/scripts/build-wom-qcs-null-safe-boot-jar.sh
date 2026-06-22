#!/bin/sh
set -eu

usage() {
  cat >&2 <<'USAGE'
Usage:
  build-wom-qcs-null-safe-boot-jar.sh --input-wom-jar PATH --output-wom-jar PATH

Builds a patched WOMMs boot jar by replacing WOMQCSServiceImpl inside the
nested com.supcon.greendill.WOM.service jar. Do not install this class as an
external runtime patch jar; it depends on nested Spring Boot classes that must
remain in the same class loader.

Optional:
  ADP_WOM_QCS_SOURCE_FILE=/path/to/WOMQCSServiceImpl.java
USAGE
}

input_wom_jar=""
output_wom_jar=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --input-wom-jar)
      input_wom_jar="${2:-}"
      shift 2
      ;;
    --output-wom-jar)
      output_wom_jar="${2:-}"
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

if [ -z "$input_wom_jar" ] || [ -z "$output_wom_jar" ]; then
  usage
  exit 2
fi

if [ ! -f "$input_wom_jar" ]; then
  echo "input WOM jar not found: $input_wom_jar" >&2
  exit 1
fi

for command_name in javac jar unzip zip python3; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "missing required command: $command_name" >&2
    exit 1
  fi
done

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
repo_root="$(CDPATH= cd -- "$docker_dir/../.." && pwd)"
source_root="$(CDPATH= cd -- "$repo_root/.." && pwd)/mes-modules-source-repo"
default_src_file="$source_root/modules/wom/WOM_6.1.3.1/service/src/main/java/com/supcon/orchid/WOM/services/impl/WOMQCSServiceImpl.java"
src_file="${ADP_WOM_QCS_SOURCE_FILE:-$default_src_file}"

if [ ! -f "$src_file" ]; then
  echo "patch source not found: $src_file" >&2
  echo "set ADP_WOM_QCS_SOURCE_FILE to the patched WOMQCSServiceImpl.java" >&2
  exit 1
fi

abs_path() {
  python3 -c 'import os, sys; print(os.path.abspath(sys.argv[1]))' "$1"
}

input_wom_jar="$(abs_path "$input_wom_jar")"
output_wom_jar="$(abs_path "$output_wom_jar")"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/adp-wom-qcs-null-safe.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT INT TERM

outer_dir="$tmp_dir/outer"
classes_dir="$tmp_dir/classes"
mkdir -p "$outer_dir" "$classes_dir" "$(dirname "$output_wom_jar")"

unzip -q "$input_wom_jar" 'BOOT-INF/lib/*.jar' -d "$outer_dir"

wom_service_jar="$(
  find "$outer_dir/BOOT-INF/lib" -maxdepth 1 -name 'com.supcon.greendill.WOM.service-*.jar' | sort | head -1
)"
if [ -z "$wom_service_jar" ]; then
  echo "nested WOM service jar not found in $input_wom_jar" >&2
  exit 1
fi

classpath="$(
  find "$outer_dir/BOOT-INF/lib" -maxdepth 1 -name '*.jar' | sort | tr '\n' ':'
)"

javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$classpath" \
  -d "$classes_dir" \
  "$src_file"

cp "$wom_service_jar" "$tmp_dir/wom-service.jar"
jar uf "$tmp_dir/wom-service.jar" -C "$classes_dir" com/supcon/orchid/WOM/services/impl
cp "$tmp_dir/wom-service.jar" "$wom_service_jar"
cp "$input_wom_jar" "$output_wom_jar"

(
  cd "$outer_dir"
  zip -0 -q -u "$output_wom_jar" "BOOT-INF/lib/$(basename "$wom_service_jar")"
)

echo "built patched WOM jar: $output_wom_jar"
