#!/bin/sh
set -eu

usage() {
  cat >&2 <<'USAGE'
Usage:
  build-wom-public-produce-created-disabled-boot-jar.sh --input-wom-jar PATH --output-wom-jar PATH

Builds a patched WOMMs boot jar by replacing WOMProduceTaskController inside the
nested com.supcon.greendill.WOM.service jar. The public produceTaskCreated
endpoint currently returns success without persisting a task; this patch makes
that endpoint fail explicitly until the product contract is restored.

Optional:
  ADP_WOM_PRODUCE_TASK_CONTROLLER_SOURCE_FILE=/path/to/WOMProduceTaskController.java
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
default_src_file="$source_root/modules/wom/WOM_6.1.3.4/service/src/main/java/com/supcon/orchid/WOM/controllers/WOMProduceTaskController.java"
src_file="${ADP_WOM_PRODUCE_TASK_CONTROLLER_SOURCE_FILE:-$default_src_file}"

if [ ! -f "$src_file" ]; then
  echo "patch source not found: $src_file" >&2
  echo "set ADP_WOM_PRODUCE_TASK_CONTROLLER_SOURCE_FILE to WOMProduceTaskController.java" >&2
  exit 1
fi

abs_path() {
  python3 -c 'import os, sys; print(os.path.abspath(sys.argv[1]))' "$1"
}

input_wom_jar="$(abs_path "$input_wom_jar")"
output_wom_jar="$(abs_path "$output_wom_jar")"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/adp-wom-public-produce-created-disabled.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT INT TERM

outer_dir="$tmp_dir/outer"
classes_dir="$tmp_dir/classes"
patched_src_dir="$tmp_dir/src"
patched_src_file="$patched_src_dir/com/supcon/orchid/WOM/controllers/WOMProduceTaskController.java"
mkdir -p "$outer_dir" "$classes_dir" "$(dirname "$patched_src_file")" "$(dirname "$output_wom_jar")"

cp "$src_file" "$patched_src_file"
python3 - "$patched_src_file" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
replacement = '''\t@PostMapping ("/public/WOM/produceTask/produceTask/produceTaskCreated")
\t@ResponseBody
\tpublic Result produceTaskCreated(@RequestBody String paramJson){
\t\tlog.warn("public produceTaskCreated is disabled because the recovered implementation returns success without persistence.");
\t\treturn Result.fail("public produceTaskCreated 已禁用：当前恢复版本不会创建制造指令单，请使用 /msService/WOM/produceTask/produceTask/produceTaskCreated2 或恢复经产品确认的落库实现。");
\t}
'''
pattern = re.compile(
    r'\t@PostMapping \("/public/WOM/produceTask/produceTask/produceTaskCreated"\)\n'
    r'\t@ResponseBody\n'
    r'\tpublic Result produceTaskCreated\(@RequestBody String paramJson\)\{\n'
    r'(?:\t\t.*\n)*?'
    r'\t\}\n',
    re.MULTILINE,
)
patched, count = pattern.subn(replacement, text, count=1)
if count != 1:
    raise SystemExit("failed to patch produceTaskCreated method")
replacements = {
    "@PathVariable long id": '@PathVariable("id") long id',
    "@PathVariable String viewName": '@PathVariable("viewName") String viewName',
    "@RequestParam String taskId": '@RequestParam(value = "taskId") String taskId',
    "@RequestParam String state": '@RequestParam(value = "state") String state',
    "@RequestParam String procReportId": '@RequestParam(value = "procReportId") String procReportId',
    "@RequestParam String formulaId": '@RequestParam(value = "formulaId") String formulaId',
    "@RequestParam String lineId": '@RequestParam(value = "lineId") String lineId',
    "@RequestParam Long activeId": '@RequestParam(value = "activeId") Long activeId',
    "@RequestParam String value": '@RequestParam(value = "value") String value',
    "@RequestParam String stand": '@RequestParam(value = "stand") String stand',
    "@RequestParam Long ajustActiveId": '@RequestParam(value = "ajustActiveId") Long ajustActiveId',
    "@RequestParam Long taskId": '@RequestParam(value = "taskId") Long taskId',
}
for source, target in replacements.items():
    patched = patched.replace(source, target)
patched = patched.replace(
    "public Map<String, Object> endActive( @RequestParam(value = \"activeId\") Long activeId,String exeSystemId)",
    "public Map<String, Object> endActive( @RequestParam(value = \"activeId\") Long activeId,"
    "@RequestParam(value = \"exeSystemId\", required = false) String exeSystemId)",
)
patched = patched.replace(
    "public Map<String, Object> endEasyActive( @PathVariable(\"id\") Long activeId,String exeSystemId)",
    "public Map<String, Object> endEasyActive( @PathVariable(\"id\") Long activeId,"
    "@RequestParam(value = \"exeSystemId\", required = false) String exeSystemId)",
)
path.write_text(patched, encoding="utf-8")
PY

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

javac -encoding UTF-8 -source 8 -target 8 -parameters \
  -cp "$classpath" \
  -d "$classes_dir" \
  "$patched_src_file"

cp "$wom_service_jar" "$tmp_dir/wom-service.jar"
jar uf "$tmp_dir/wom-service.jar" -C "$classes_dir" com/supcon/orchid/WOM/controllers
cp "$tmp_dir/wom-service.jar" "$wom_service_jar"
cp "$input_wom_jar" "$output_wom_jar"

(
  cd "$outer_dir"
  zip -0 -q -u "$output_wom_jar" "BOOT-INF/lib/$(basename "$wom_service_jar")"
)

echo "built patched WOM jar: $output_wom_jar"
