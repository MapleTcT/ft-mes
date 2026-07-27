#!/bin/sh
set -eu

usage() {
  cat >&2 <<'USAGE'
Usage:
  build-wom-core-production-boot-jar.sh --input-wom-jar PATH --output-wom-jar PATH

Builds a patched WOMMs boot jar for the manufacturing-order core flow:
  - keeps workflow end callbacks from clearing the active Hibernate session;
  - ignores soft-deleted tasks during production-batch uniqueness checks;
  - reports missing product/unit master data as a business error instead of NPE;
  - closes both the easy-report activity and its execution record consistently;
  - materializes missing put-in/output execution records before activity close.

Optional environment:
  ADP_WOM_PRODUCE_TASK_SERVICE_SOURCE_FILE=/path/to/WOMProduceTaskServiceImpl.java
  ADP_WOM_WAIT_PUT_SERVICE_SOURCE_FILE=/path/to/WOMWaitPutRecordServiceImpl.java

The source paths default to the recovered WOM 6.1.3.4 source repository next
to this repository. SHA-256 verification is optional for local/runtime
preparation. If any checksum below is supplied, all three are required:
  ADP_WOM_PRODUCE_TASK_SERVICE_SOURCE_SHA256=<expected sha256>
  ADP_WOM_WAIT_PUT_SERVICE_SOURCE_SHA256=<expected sha256>
  ADP_WOM_INPUT_JAR_SHA256=<expected sha256>

Set ADP_WOM_REQUIRE_CHECKSUMS=true to reject an unchecked production build.
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

script_dir="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
repo_root="$(CDPATH= cd -- "$script_dir/../../.." && pwd)"
default_source_root="$(dirname "$repo_root")/mes-modules-source-repo/modules/wom/WOM_6.1.3.4/service/src/main/java/com/supcon/orchid/WOM/services/impl"
produce_src="${ADP_WOM_PRODUCE_TASK_SERVICE_SOURCE_FILE:-$default_source_root/WOMProduceTaskServiceImpl.java}"
wait_put_src="${ADP_WOM_WAIT_PUT_SERVICE_SOURCE_FILE:-$default_source_root/WOMWaitPutRecordServiceImpl.java}"
produce_src_sha="${ADP_WOM_PRODUCE_TASK_SERVICE_SOURCE_SHA256:-}"
wait_put_src_sha="${ADP_WOM_WAIT_PUT_SERVICE_SOURCE_SHA256:-}"
input_wom_jar_sha="${ADP_WOM_INPUT_JAR_SHA256:-}"
require_checksums="${ADP_WOM_REQUIRE_CHECKSUMS:-false}"

if [ "$require_checksums" != "true" ] && [ "$require_checksums" != "false" ]; then
  echo "ADP_WOM_REQUIRE_CHECKSUMS must be true or false" >&2
  exit 2
fi

for src_file in "$produce_src" "$wait_put_src"; do
  if [ ! -f "$src_file" ]; then
    echo "patch source not found: $src_file" >&2
    exit 1
  fi
done

abs_path() {
  python3 -c 'import os, sys; print(os.path.abspath(sys.argv[1]))' "$1"
}

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

input_wom_jar="$(abs_path "$input_wom_jar")"
output_wom_jar="$(abs_path "$output_wom_jar")"
produce_src="$(abs_path "$produce_src")"
wait_put_src="$(abs_path "$wait_put_src")"

actual_input_wom_jar_sha="$(sha256_file "$input_wom_jar")"
actual_produce_src_sha="$(sha256_file "$produce_src")"
actual_wait_put_src_sha="$(sha256_file "$wait_put_src")"
checksum_values="${produce_src_sha}${wait_put_src_sha}${input_wom_jar_sha}"
if [ -n "$checksum_values" ]; then
  if [ -z "$produce_src_sha" ] || [ -z "$wait_put_src_sha" ] || [ -z "$input_wom_jar_sha" ]; then
    echo "all three expected SHA-256 values are required when checksum verification is enabled" >&2
    exit 2
  fi
  if [ "$actual_input_wom_jar_sha" != "$input_wom_jar_sha" ]; then
    echo "input WOM jar checksum mismatch: expected $input_wom_jar_sha, got $actual_input_wom_jar_sha" >&2
    exit 1
  fi
  if [ "$actual_produce_src_sha" != "$produce_src_sha" ]; then
    echo "WOMProduceTaskServiceImpl source checksum mismatch: expected $produce_src_sha, got $actual_produce_src_sha" >&2
    exit 1
  fi
  if [ "$actual_wait_put_src_sha" != "$wait_put_src_sha" ]; then
    echo "WOMWaitPutRecordServiceImpl source checksum mismatch: expected $wait_put_src_sha, got $actual_wait_put_src_sha" >&2
    exit 1
  fi
elif [ "$require_checksums" = "true" ]; then
  echo "ADP_WOM_REQUIRE_CHECKSUMS=true requires all three expected SHA-256 values" >&2
  exit 2
else
  echo "warning: building WOM patch without pinned checksums" >&2
  echo "  input WOM jar: $actual_input_wom_jar_sha" >&2
  echo "  WOMProduceTaskServiceImpl.java: $actual_produce_src_sha" >&2
  echo "  WOMWaitPutRecordServiceImpl.java: $actual_wait_put_src_sha" >&2
fi

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/adp-wom-core-production.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT INT TERM

outer_dir="$tmp_dir/outer"
classes_dir="$tmp_dir/classes"
patched_src_dir="$tmp_dir/src/com/supcon/orchid/WOM/services/impl"
patched_produce_src="$patched_src_dir/WOMProduceTaskServiceImpl.java"
patched_wait_put_src="$patched_src_dir/WOMWaitPutRecordServiceImpl.java"
mkdir -p "$outer_dir" "$classes_dir" "$patched_src_dir" "$(dirname "$output_wom_jar")"
cp "$produce_src" "$patched_produce_src"
cp "$wait_put_src" "$patched_wait_put_src"

python3 - "$patched_produce_src" "$patched_wait_put_src" <<'PY'
import re
import sys
from pathlib import Path

produce_path = Path(sys.argv[1])
wait_put_path = Path(sys.argv[2])
produce = produce_path.read_text(encoding="utf-8")
wait_put = wait_put_path.read_text(encoding="utf-8")

# The recovered 6.1.3.4 generator source declares optional extension beans that
# are absent from the delivered runtime jar and are never used outside destroy().
# Strip only those inert declarations so the patch compiles against the exact
# running package instead of introducing unavailable modules.
optional_types = (
    "WOMEleSignature",
    "WOMBatchingTask",
    "WOMBatchingMateri",
    "WOMEquipmentList",
    "WOMPrintDate",
    "WOMQrcodeIndex",
    "WOMPackageQrcode",
    "WOMQrcodeAccount",
)
optional_variables = (
    "eleSignatureDao", "eleSignatureService",
    "batchingTaskDao", "batchingTaskService",
    "batchingMateriDao", "batchingMateriService",
    "equipmentListDao", "equipmentListService",
    "printDateDao", "printDateService",
    "qrcodeIndexDao", "qrcodeIndexService",
    "packageQrcodeDao", "packageQrcodeService",
    "qrcodeAccountDao", "qrcodeAccountService",
)

def strip_optional_runtime_extensions(text):
    for type_name in optional_types:
        text = re.sub(
            rf'^import com\.supcon\.orchid\.WOM\.(?:entities|daos|services)\.{type_name}(?:Dao|Service)?;\n',
            '',
            text,
            flags=re.MULTILINE,
        )
        text = re.sub(
            rf'\t@Autowired\n\tprivate {type_name}(?:Dao|Service) \w+;\n',
            '',
            text,
        )
    for variable_name in optional_variables:
        text = re.sub(rf'^\t\t{variable_name} = null;\n', '', text, flags=re.MULTILINE)
    return text

produce = strip_optional_runtime_extensions(produce)
wait_put = strip_optional_runtime_extensions(wait_put)

sync_pattern = re.compile(
    r'\t@Override\n'
    r'\tpublic void syncEntity\(Long id, String type\) \{\n'
    r'[\s\S]*?'
    r'\t\}\n'
    r'\t@Override\n'
    r'\tpublic Date findLastDealInfo',
)
sync_replacement = '''\t@Override
\tpublic void syncEntity(Long id, String type) {
\t\t// The recovered callback had no sync side effect, but flush/clear detached
\t\t// the order in the middle of workflow completion and caused stale updates.
\t\tif (id == null) {
\t\t\tlog.warn("skip WOM produceTask syncEntity callback with null id, type={}", type);
\t\t}
\t}
\t@Override
\tpublic Date findLastDealInfo'''
produce, sync_count = sync_pattern.subn(sync_replacement, produce, count=1)
if sync_count != 1:
    raise SystemExit("failed to patch WOMProduceTaskServiceImpl.syncEntity")

unique_pattern = re.compile(
    r'produceTaskDao\.findByCriteria\(Restrictions\.ne\("id", produceTask\.getId\(\)\), '
    r'Restrictions\.eq\("produceBatchNum", produceTask\.getProduceBatchNum\(\)\), '
    r'Restrictions\.ne\("status", 0\)\)'
)
produce, update_unique_count = unique_pattern.subn(
    'produceTaskDao.findByCriteria(Restrictions.ne("id", produceTask.getId()), '
    'Restrictions.eq("produceBatchNum", produceTask.getProduceBatchNum()), '
    'Restrictions.eq("valid", true), Restrictions.ne("status", 0))',
    produce,
    count=1,
)
if update_unique_count != 1:
    raise SystemExit("failed to patch existing-task production-batch uniqueness check")

create_unique_pattern = re.compile(
    r'produceTaskDao\.findByCriteria\(Restrictions\.eq\("produceBatchNum", produceTask\.getProduceBatchNum\(\)\), '
    r'Restrictions\.ne\("status", 0\)\)'
)
produce, create_unique_count = create_unique_pattern.subn(
    'produceTaskDao.findByCriteria(Restrictions.eq("produceBatchNum", produceTask.getProduceBatchNum()), '
    'Restrictions.eq("valid", true), Restrictions.ne("status", 0))',
    produce,
    count=1,
)
if create_unique_count != 1:
    raise SystemExit("failed to patch new-task production-batch uniqueness check")

wait_put_save_block = '''\t\twomWaitPutRecord.setRecordType(new SystemCode("WOM_recordType/workOrder"));

\t\twaitPutRecordDao.save(womWaitPutRecord);'''
wait_put_save_replacement = '''\t\twomWaitPutRecord.setRecordType(new SystemCode("WOM_recordType/workOrder"));

\t\ttry {
\t\t\tlog.info("saving WOM work-order wait-put record for task {}", task.getId());
\t\t\twaitPutRecordDao.save(womWaitPutRecord);
\t\t\twaitPutRecordDao.flush();
\t\t\tlog.info("saved WOM work-order wait-put record {} for task {}", womWaitPutRecord.getId(), task.getId());
\t\t} catch (RuntimeException ex) {
\t\t\tlog.error("failed to persist WOM work-order wait-put record for task " + task.getId(), ex);
\t\t\tthrow ex;
\t\t}'''
if wait_put_save_block not in produce:
    raise SystemExit("failed to locate work-order wait-put persistence")
produce = produce.replace(wait_put_save_block, wait_put_save_replacement, 1)

task_state_merge_block = '''\t\t\tproduceTask.setTaskRunState(new SystemCode(WOM_RUNSTATE_WAIT_FOR_RUN));
\t\t\tproduceTask.setIsPrepared(false);
\t\t\tproduceTaskDao.merge(produceTask);'''
task_state_merge_replacement = '''\t\t\tproduceTask.setTaskRunState(new SystemCode(WOM_RUNSTATE_WAIT_FOR_RUN));
\t\t\tproduceTask.setIsPrepared(false);
\t\t\tif (produceTaskDao.getSessionFactory().getCurrentSession().contains(produceTask)) {
\t\t\t\tproduceTaskDao.flush();
\t\t\t} else {
\t\t\t\t// Workflow completion may detach the request object after changing its
\t\t\t\t// status/version. Reload the current row instead of merging stale state.
\t\t\t\tWOMProduceTask managedTask = getProduceTask(produceTask.getId());
\t\t\t\tmanagedTask.setStatus(produceTask.getStatus());
\t\t\t\tmanagedTask.setEffectiveState(produceTask.getEffectiveState());
\t\t\t\tmanagedTask.setEffectTime(produceTask.getEffectTime());
\t\t\t\tmanagedTask.setEffectStaff(produceTask.getEffectStaff());
\t\t\t\tmanagedTask.setEffectStaffId(produceTask.getEffectStaffId());
\t\t\t\tmanagedTask.setTaskRunState(new SystemCode(WOM_RUNSTATE_WAIT_FOR_RUN));
\t\t\t\tmanagedTask.setIsPrepared(false);
\t\t\t\tproduceTaskDao.flush();
\t\t\t\tlog.warn("reloaded detached WOM produceTask {} after workflow completion", produceTask.getId());
\t\t\t}'''
if task_state_merge_block not in produce:
    raise SystemExit("failed to locate workflow task-state merge")
produce = produce.replace(task_state_merge_block, task_state_merge_replacement, 1)

is_craft_line = '\t\t\tif(formulaId.getIsCraft()){'
is_craft_replacement = '\t\t\tif(Boolean.TRUE.equals(formulaId.getIsCraft())){'
if is_craft_line not in produce:
    raise SystemExit("failed to locate nullable isCraft workflow branch")
produce = produce.replace(is_craft_line, is_craft_replacement, 1)

material_line = (
    '\t\tBaseSetMaterial material = materials.get(0);\n'
    '\t\tlog.info("产品单位:" + material.getProduceUnit());'
)
material_replacement = '''\t\tif (materials.isEmpty()) {
\t\t\tthrow new BAPException("制造指令产品主数据不存在，请检查产品配置。");
\t\t}
\t\tBaseSetMaterial material = materials.get(0);
\t\tif (material.getProduceUnit() == null) {
\t\t\tthrow new BAPException("产品未配置生产单位，请先在物料主数据中维护生产单位。");
\t\t}
\t\tlog.info("产品单位:" + material.getProduceUnit());'''
if material_line not in wait_put:
    raise SystemExit("failed to locate product/unit lookup in WOMWaitPutRecordServiceImpl")
wait_put = wait_put.replace(material_line, material_replacement, 1)

unit_line = (
    '\t\tList<BaseSetUnit> units = processExelogDao.createNativeQuery(produceUnitSql, '
    'material.getProduceUnit().getId()).addEntity(BaseSetUnit.class).list();\n'
    '\t\tBaseSetUnit unit = units.get(0);'
)
unit_replacement = unit_line.split('\n')[0] + '''
\t\tif (units.isEmpty()) {
\t\t\tthrow new BAPException("产品生产单位不存在或已失效，请检查物料主数据。");
\t\t}
\t\tBaseSetUnit unit = units.get(0);'''
if unit_line not in wait_put:
    raise SystemExit("failed to locate unit entity lookup in WOMWaitPutRecordServiceImpl")
wait_put = wait_put.replace(unit_line, unit_replacement, 1)

easy_active_finish_block = '''\t\tDate now = new Date();
\t\t// 将活动修改为已完成
\t\ttaskActive.setIsFinish(true);
\t\t// 活动结束时间
\t\ttaskActive.setActEndTime(now);
\t\ttaskActiveDao.merge(taskActive);'''
easy_active_finish_replacement = '''\t\tDate now = new Date();
\t\t// Keep the activity row consistent with the finished state returned to the UI.
\t\ttaskActive.setIsFinish(true);
\t\ttaskActive.setRunState(new SystemCode("WOM_runState/finished"));
\t\ttaskActive.setActEndTime(now);
\t\ttaskActiveDao.merge(taskActive);'''
if easy_active_finish_block not in produce:
    raise SystemExit("failed to locate easy-report activity completion block")
produce = produce.replace(easy_active_finish_block, easy_active_finish_replacement, 1)

easy_execution_finish_block = '''\t\tfor(WOMActiExelog actiExelog : actiExelogs){
\t\t\t// 活动执行状态(已完成)
\t\t\tactiExelog.setRunState(new SystemCode("WOM_runState/finished"));'''
easy_execution_finish_replacement = '''\t\tfor(WOMActiExelog actiExelog : actiExelogs){
\t\t\tDate executionEndTime = actiExelog.getActEndTime();
\t\t\tif (executionEndTime == null) {
\t\t\t\texecutionEndTime = now;
\t\t\t\tactiExelog.setActEndTime(executionEndTime);
\t\t\t}
\t\t\tif (actiExelog.getActStartTime() != null) {
\t\t\t\tlong takeMinutes = (executionEndTime.getTime() - actiExelog.getActStartTime().getTime()) / (1000 * 60);
\t\t\t\tactiExelog.setActlongTime(BigDecimal.valueOf(takeMinutes));
\t\t\t}
\t\t\t// 活动执行状态(已完成)
\t\t\tactiExelog.setRunState(new SystemCode("WOM_runState/finished"));'''
if easy_execution_finish_block not in produce:
    raise SystemExit("failed to locate easy-report execution completion block")
produce = produce.replace(easy_execution_finish_block, easy_execution_finish_replacement, 1)

end_active_feedback_block = '''\t\tif (!(Boolean) feedBackResult.get("success")) {
\t\t\tresultMap.put("data", active.getRunState());
\t\t\treturn resultMap;
\t\t}
\t\t//结束活动回填活动活动执行记录'''
end_active_feedback_replacement = '''\t\tif (!(Boolean) feedBackResult.get("success")) {
\t\t\tresultMap.put("data", active.getRunState());
\t\t\treturn resultMap;
\t\t}
\t\t// Detail-row saves can bypass the parent report callback in recovered views.
\t\t// Rebuild every missing execution row for the current unfinished report;
\t\t// existing rows are retained so a partial callback cannot create duplicates.
\t\tSet<Long> existingPutinDetailIds = new HashSet<Long>();
\t\tSet<Long> existingOutputDetailIds = new HashSet<Long>();
\t\tfor (WOMActiExelog existingExelog : actiExelogs) {
\t\t\tif (existingExelog.getPutinDetailId() != null) {
\t\t\t\texistingPutinDetailIds.add(existingExelog.getPutinDetailId().getId());
\t\t\t}
\t\t\tif (existingExelog.getOutputDetailId() != null) {
\t\t\t\texistingOutputDetailIds.add(existingExelog.getOutputDetailId().getId());
\t\t\t}
\t\t}
\t\tList<WOMProcReport> activeProcReports = procReportDao.findByCriteria(
\t\t\t\tRestrictions.eq("taskActiveId", active),
\t\t\t\tRestrictions.eq("valid", true),
\t\t\t\tRestrictions.eq("isFinish", false));
\t\tfor (WOMProcReport activeProcReport : activeProcReports) {
\t\t\tif ("RM_activeType/putin".equals(activeType)
\t\t\t\t\t|| "RM_activeType/batchPutin".equals(activeType)
\t\t\t\t\t|| "RM_activeType/pipePutin".equals(activeType)
\t\t\t\t\t|| "RM_activeType/pipeBatchPutin".equals(activeType)) {
\t\t\t\tList<WOMPutinDetail> putinDetails = putinDetailDao.findByCriteria(
\t\t\t\t\t\tRestrictions.eq("headId", activeProcReport),
\t\t\t\t\t\tRestrictions.eq("valid", true));
\t\t\t\tfor (WOMPutinDetail putinDetail : putinDetails) {
\t\t\t\t\tif (existingPutinDetailIds.add(putinDetail.getId())) {
\t\t\t\t\t\tactiExelogs.add(procReportService.getExeLogs(
\t\t\t\t\t\t\t\tputinDetail, null, activeProcReport.getOperateType(),
\t\t\t\t\t\t\t\tactiveProcReport.getCheckActiveId()));
\t\t\t\t\t}
\t\t\t\t}
\t\t\t}
\t\t\tif ("RM_activeType/output".equals(activeType)
\t\t\t\t\t|| "RM_activeType/pipeOutput".equals(activeType)) {
\t\t\t\tList<WOMOutputDetail> outputDetails = outputDetailDao.findByCriteria(
\t\t\t\t\t\tRestrictions.eq("headId", activeProcReport),
\t\t\t\t\t\tRestrictions.eq("valid", true));
\t\t\t\tfor (WOMOutputDetail outputDetail : outputDetails) {
\t\t\t\t\tif (existingOutputDetailIds.add(outputDetail.getId())) {
\t\t\t\t\t\tactiExelogs.add(procReportService.getExeLogs(
\t\t\t\t\t\t\t\tnull, outputDetail, activeProcReport.getOperateType(),
\t\t\t\t\t\t\t\tactiveProcReport.getCheckActiveId()));
\t\t\t\t\t}
\t\t\t\t}
\t\t\t}
\t\t}
\t\t//结束活动回填活动活动执行记录'''
if end_active_feedback_block not in produce:
    raise SystemExit("failed to locate endActive feedback completion block")
produce = produce.replace(end_active_feedback_block, end_active_feedback_replacement, 1)

produce_path.write_text(produce, encoding="utf-8")
wait_put_path.write_text(wait_put, encoding="utf-8")
PY

unzip -q "$input_wom_jar" 'BOOT-INF/lib/*.jar' -d "$outer_dir"
wom_service_jar="$(find "$outer_dir/BOOT-INF/lib" -maxdepth 1 -name 'com.supcon.greendill.WOM.service-*.jar' | sort | head -1)"
if [ -z "$wom_service_jar" ]; then
  echo "nested WOM service jar not found in $input_wom_jar" >&2
  exit 1
fi

classpath="$(find "$outer_dir/BOOT-INF/lib" -maxdepth 1 -name '*.jar' | sort | tr '\n' ':')"
javac -encoding UTF-8 -source 8 -target 8 -parameters \
  -cp "$classpath" \
  -d "$classes_dir" \
  "$patched_produce_src" "$patched_wait_put_src"

cp "$wom_service_jar" "$tmp_dir/wom-service.jar"
jar uf "$tmp_dir/wom-service.jar" -C "$classes_dir" com/supcon/orchid/WOM/services/impl
cp "$tmp_dir/wom-service.jar" "$wom_service_jar"
cp "$input_wom_jar" "$output_wom_jar"
(
  cd "$outer_dir"
  zip -0 -q -u "$output_wom_jar" "BOOT-INF/lib/$(basename "$wom_service_jar")"
)

echo "built patched WOM core production jar: $output_wom_jar"
