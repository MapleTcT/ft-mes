#!/bin/sh
set -eu

usage() {
  cat >&2 <<'USAGE'
Usage:
  build-rm-import-transaction-patch.sh --input-rm-jar PATH --output-rm-jar PATH

Builds a patched RMMs boot jar by replacing RMFormulaServiceImpl inside the
nested com.supcon.greendill.RM.service jar. Do not install this class as an
external runtime patch jar; the boot parent classloader cannot see the nested
RM api jar when loading service classes from an external patch.

The patch changes the RM formula Excel import workflow from SUPPORTS to
REQUIRED transaction propagation so PostgreSQL JDBC batch inserts are committed.

Optional:
  ADP_RM_FORMULA_SOURCE_FILE=/path/to/RMFormulaServiceImpl.java
USAGE
}

input_rm_jar=""
output_rm_jar=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --input-rm-jar)
      input_rm_jar="${2:-}"
      shift 2
      ;;
    --output-rm-jar)
      output_rm_jar="${2:-}"
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

for command_name in javac jar unzip python3; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "missing required command: $command_name" >&2
    exit 1
  fi
done

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
repo_root="$(CDPATH= cd -- "$docker_dir/../.." && pwd)"
adp_root="$(CDPATH= cd -- "$repo_root/.." && pwd)"
source_root="$adp_root/mes-modules-source-repo"

default_src_file="$source_root/modules/rm/RM_6.1.2.3/service/src/main/java/com/supcon/orchid/RM/services/impl/RMFormulaServiceImpl.java"
src_file="${ADP_RM_FORMULA_SOURCE_FILE:-$default_src_file}"

if [ -z "$input_rm_jar" ]; then
  for candidate in \
    "$adp_root/runtime/bap-server/module-Server/RMMs/manual/RMMs-1.0.0.jar" \
    "$source_root/deploy/staging/ms-services-runtime-20260613/RMMs/manual/RMMs-1.0.0.jar" \
    "$source_root/deploy/build/ms-services-20260613/RMMs/target/RMMs-1.0.0.jar"
  do
    if [ -f "$candidate" ]; then
      input_rm_jar="$candidate"
      break
    fi
  done
fi

if [ -z "$input_rm_jar" ] || [ ! -f "$input_rm_jar" ] || [ -z "$output_rm_jar" ]; then
  echo "input RMMs jar not found. Pass --input-rm-jar PATH." >&2
  echo "output RMMs jar is required. Pass --output-rm-jar PATH." >&2
  exit 1
fi

if [ ! -f "$src_file" ]; then
  echo "RMFormulaServiceImpl source not found: $src_file" >&2
  echo "set ADP_RM_FORMULA_SOURCE_FILE to the recovered source file" >&2
  exit 1
fi

abs_path() {
  python3 -c 'import os, sys; print(os.path.abspath(sys.argv[1]))' "$1"
}

input_rm_jar="$(abs_path "$input_rm_jar")"
src_file="$(abs_path "$src_file")"
output_rm_jar="$(abs_path "$output_rm_jar")"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/adp-rm-import-transaction.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT INT TERM

outer_dir="$tmp_dir/outer"
classes_dir="$tmp_dir/classes"
patched_src="$tmp_dir/src/com/supcon/orchid/RM/services/impl/RMFormulaServiceImpl.java"
mkdir -p "$outer_dir" "$classes_dir" "$(dirname "$patched_src")" "$(dirname "$output_rm_jar")"

unzip -q "$input_rm_jar" 'BOOT-INF/lib/*.jar' -d "$outer_dir"
rm_service_jar="$(
  find "$outer_dir/BOOT-INF/lib" -maxdepth 1 -name 'com.supcon.greendill.RM.service-*.jar' | sort | head -1
)"
if [ -z "$rm_service_jar" ]; then
  echo "nested RM service jar not found in $input_rm_jar" >&2
  exit 1
fi

python3 - "$src_file" "$patched_src" <<'PY'
from pathlib import Path
import sys

src = Path(sys.argv[1])
dst = Path(sys.argv[2])
lines = src.read_text(encoding="utf-8").splitlines(keepends=True)
changed = 0

for index, line in enumerate(lines):
    if line.strip() != "@Transactional(propagation = Propagation.SUPPORTS)":
        continue
    lookahead = "".join(lines[index + 1:index + 4])
    if "public Map<Object, Long> importBatchFormulaWorkFlow" in lookahead:
        indent = line[: len(line) - len(line.lstrip())]
        lines[index] = f"{indent}@Transactional(propagation = Propagation.REQUIRED)\n"
        changed += 1

if changed != 2:
    raise SystemExit(f"expected to patch 2 importBatchFormulaWorkFlow annotations, patched {changed}")

dst.write_text("".join(lines), encoding="utf-8")
PY

classpath="$(
  find "$outer_dir/BOOT-INF/lib" -maxdepth 1 -name '*.jar' | sort | tr '\n' ':'
)"

javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$classpath" \
  -d "$classes_dir" \
  "$patched_src"

cp "$rm_service_jar" "$tmp_dir/rm-service.jar"
jar uf "$tmp_dir/rm-service.jar" -C "$classes_dir" com/supcon/orchid/RM/services/impl
cp "$tmp_dir/rm-service.jar" "$rm_service_jar"
cp "$input_rm_jar" "$output_rm_jar"

(
  cd "$outer_dir"
  zip -0 -q -u "$output_rm_jar" "BOOT-INF/lib/$(basename "$rm_service_jar")"
)

echo "built patched RMMs jar: $output_rm_jar"
