#!/usr/bin/env python3
"""Patch WTS runtime compatibility gaps in recovered Linux deployments.

This handles two package-migration issues seen in the recovered WTS module:

* desktop low-code HTML pages may miss vendors.sesgis.js, which prevents the
  Webpack list/edit entry from executing because chunk 7 never loads.
* selected WTS work-ticket LIST views may have runtime_extra_view.view_json as
  NULL after PostgreSQL import. The script rebuilds small LIST layouts from
  ec_view/ec_field metadata and writes both runtime_extra_view and ec_extra_view.
* WTS work-permit list post-processing reads the LOB-backed payload column with
  raw SQL. PostgreSQL returns the OID value there, so the event class must decode
  the large object before parsing the payload JSON.
"""

from __future__ import annotations

import argparse
import base64
import io
import json
import os
import re
import shutil
import subprocess
import sys
import time
import zipfile
import tempfile
from pathlib import Path
from typing import Any, Dict, Iterable, List, Tuple
import xml.etree.ElementTree as ET


INNER_WTS_SERVICE_JAR = "BOOT-INF/lib/com.supcon.greendill.WTS.service-6.1.8.2.jar"
WTS_UPLOAD_LIST_CONTROLLER_CLASS = "com/supcon/orchid/WTS/controllers/WTSFileDownloadController.class"
WTS_WORK_TICKET_CONTROLLER_CLASS = "com/supcon/orchid/WTS/controllers/WTSWorkTicketController.class"
WORK_TICKET_2_PERMIT_SERVICE_CLASS = (
    "com/supcon/orchid/WTS/superzy/basic/services/WorkTicket2PermitService.class"
)
WORK_TICKET_EVENT_CLASS = "com/supcon/orchid/WTS/superzy/basic/events/WorkTicketEvent.class"
AUTO_NODE_EVENT_CLASS = "com/supcon/orchid/WTS/superzy/basic/events/AutoNodeEvent.class"
WORK_PERMIT_VIEW_EVENT_CLASS = "com/supcon/orchid/WTS/superzy/view/events/WorkPermitViewEvent.class"
STAT_ICON_SOURCE = "custom/WTS/_static_jobStatistics/assets/images/comp_icon.png"
STAT_ICON_ALIAS = "static/WTS/assets/images/comp_icon.png"
SCRIPT_MARKER = "greenDill/static/scripts/vendors.sesgis.js"
ECHARTS_RE = re.compile(
    r'(<script[^>]+src="/greenDill/static/scripts/vendors\.echarts\.js\?v=([^"]+)"[^>]*></script>)'
)

UPLOAD_LIST_OLD_BLOCK = '''\t\t\tMap isShadow = viewServiceFoundation.findShadowCode(viewCode);
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

UPLOAD_LIST_NEW_BLOCK = '''    \t\ttry {
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
\t\t\t\tif(null != isShadow.get("TYPE")){
\t\t\t\t\tviewType = (String)isShadow.get("TYPE");
\t\t\t\t\tif (ViewType.EDIT.equals(getViewType(viewType)) || ViewType.VIEW.equals(getViewType(viewType))
\t\t\t\t\t\t\t|| ViewType.EXTRA.equals(getViewType(viewType))) {
\t\t\t\t\t\tmapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
\t\t\t\t\t\tMap resultMap = mapper.readValue(resultStr, Map.class);
\t\t\t\t\t\tattPermission(resultMap, attPermissionMap);
\t\t\t\t\t}
\t\t\t\t}
    \t\t} catch (Exception ex) {
    \t\t\tlogger.warn("Skip attachment permission parsing for viewCode {}: {}", viewCode, ex.toString());
    \t\t}
'''

WORK_TICKET_CONTROLLER_PROCESS_METHOD_VALIDATION_OLD = '''\t\tWTSWorkTicket workTicket = editEntity.getWorkTicket();
\t\tString operateType = editEntity.getOperateType();
\t\tMap<String, Object> responseMap = null;
        boolean isSuccess = true;
        String exceptionDescription = null;'''

WORK_TICKET_CONTROLLER_PROCESS_METHOD_VALIDATION_NEW = '''\t\tWTSWorkTicket workTicket = editEntity.getWorkTicket();
\t\tString operateType = editEntity.getOperateType();
        if ("submit".equals(operateType)
                && editEntity.getViewCode() != null
                && editEntity.getViewCode().contains("fireworkDeal")
                && workTicket != null
                && workTicket.getJobStatus() != null
                && "WTS_jobStatus/execution".equals(workTicket.getJobStatus().getId())
                && (workTicket.getProcessMethod() == null || workTicket.getProcessMethod().getId() == null)) {
            Map<String, Object> validationMap = new HashMap<>();
            validationMap.put("success", false);
            validationMap.put("code", 400);
            validationMap.put("message", "请选择作业票处理方式");
            validationMap.put("errorMsg", "请选择作业票处理方式");
            validationMap.put("dealSuccessFlag", false);
            return validationMap;
        }
\t\tMap<String, Object> responseMap = null;
        boolean isSuccess = true;
        String exceptionDescription = null;'''

WORK_TICKET_2_PERMIT_OLD_IMPORT = '''import com.supcon.orchid.WTS.daos.WTSWorkPermitDao;

import com.supcon.orchid.WTS.entities.WTSWorkPermit;'''

WORK_TICKET_2_PERMIT_NEW_IMPORT = '''import com.supcon.orchid.WTS.daos.WTSWorkPermitDao;

import com.supcon.orchid.WTS.entities.WTSWorkPermit;
import com.supcon.orchid.WTS.entities.WTSWorkTicket;
import com.supcon.orchid.WTS.superzy.basic.util.Dbs;'''

WORK_TICKET_2_PERMIT_OLD_METHOD = '''    public WTSWorkPermit getPermitByWorkTicketId(Long id){
        return  workPermitDao.findEntityByCriteria(Restrictions.like("payload", "%"+id.toString()+"%"));

    }'''

WORK_TICKET_2_PERMIT_NEW_METHOD = '''    public WTSWorkPermit getPermitByWorkTicketId(Long id){
        if (id == null) {
            return null;
        }
        WTSWorkPermit permit = workPermitDao.findEntityByCriteria(Restrictions.like("payload", "%"+id.toString()+"%"));
        if (permit != null) {
            return permit;
        }
        WTSWorkTicket workTicket;
        try {
            workTicket = Dbs.load(WTSWorkTicket.class, id);
        } catch (Exception ignored) {
            return null;
        }
        if (workTicket == null) {
            return null;
        }
        if (workTicket.getWorkPermit() != null) {
            return workTicket.getWorkPermit();
        }
        String ticketNo = workTicket.getTicketNo();
        if (ticketNo != null && ticketNo.trim().length() > 0) {
            return workPermitDao.findEntityByCriteria(
                    Restrictions.eq("ticketNo", ticketNo),
                    Restrictions.eq("valid", true)
            );
        }
        return null;

    }'''

WORK_TICKET_EVENT_OLD_BLOCK = '''            if (null != workTicket.getDeploymentId() && null != workTicket.getTableInfoId() && workFlowService.isNextNodeRequired(workTicket.getTableInfoId(), workTicket.getDeploymentId(), "end")) {
                //外部业务状态回填
                WTSWorkPermit permit = workTicket2PermitService.getPermitByWorkTicketId(workTicket.getId());
                if (null != permit.getBusinessKey()) {
                    workTicket.setStatus(99);
                    operateExternalDataService.updateExternalTable(permit, workTicket);
                }

            }'''

WORK_TICKET_EVENT_NEW_BLOCK = '''            if (null != workTicket.getDeploymentId() && null != workTicket.getTableInfoId() && workFlowService.isNextNodeRequired(workTicket.getTableInfoId(), workTicket.getDeploymentId(), "end")) {
                //外部业务状态回填。许可单可能来自 ADP 原始业务，也可能只是 WTS 内部合并票据。
                workTicket.setStatus(99);
                WTSWorkPermit permit = workTicket2PermitService.getPermitByWorkTicketId(workTicket.getId());
                if (permit != null && null != permit.getBusinessKey()) {
                    operateExternalDataService.updateExternalTable(permit, workTicket);
                }

            }'''

WORK_TICKET_EVENT_PROCESS_METHOD_OLD_BLOCK = '''            //只需要在作业执行中这个状态 选完 处理方法之后去设置
            if (workTicket.getJobStatus().getId().equals(WorkStatusEnum.EXECUTION.getValue())) {
                // 进入封票封票 待验收状态
                if (workTicket.getProcessMethod().getId().equals(ProcessMethodSc.CLOSE)) {
                    workTicket.setJobStatus(new SystemCode(WorkStatusEnum.READY_CLOSE.getValue()));
                }
                // 进入终止  终止中
                if (workTicket.getProcessMethod().getId().equals(ProcessMethodSc.STOP)) {
                    workTicket.setJobStatus(new SystemCode(WorkStatusEnum.STOPPING.getValue()));
                }
                //延期
                if (workTicket.getProcessMethod().getId().equals(ProcessMethodSc.DELAY)) {
                    workTicket.setJobStatus(new SystemCode(WorkStatusEnum.DELAYING.getValue()));
                }

            }'''

WORK_TICKET_EVENT_PROCESS_METHOD_NEW_BLOCK = '''            //只需要在作业执行中这个状态 选完 处理方法之后去设置
            if (workTicket.getJobStatus() != null && WorkStatusEnum.EXECUTION.getValue().equals(workTicket.getJobStatus().getId())) {
                if (workTicket.getProcessMethod() == null || workTicket.getProcessMethod().getId() == null) {
                    throw new BAPException("请选择作业票处理方式");
                }
                String processMethodId = workTicket.getProcessMethod().getId();
                // 进入封票封票 待验收状态
                if (ProcessMethodSc.CLOSE.equals(processMethodId)) {
                    workTicket.setJobStatus(new SystemCode(WorkStatusEnum.READY_CLOSE.getValue()));
                }
                // 进入终止  终止中
                if (ProcessMethodSc.STOP.equals(processMethodId)) {
                    workTicket.setJobStatus(new SystemCode(WorkStatusEnum.STOPPING.getValue()));
                }
                //延期
                if (ProcessMethodSc.DELAY.equals(processMethodId)) {
                    workTicket.setJobStatus(new SystemCode(WorkStatusEnum.DELAYING.getValue()));
                }

            }'''

WORK_TICKET_EVENT_END_PUSH_OLD = (
    "                plsEventClient.receiveEvent(buildDataService.buildData2EventRecordReceive(workTicket, WorkEventKeys.END));"
)
WORK_TICKET_EVENT_END_PUSH_NEW = (
    '                log.info("Skip PLS END event push in ADP PostgreSQL compatibility runtime.");'
)
AUTO_NODE_EVENT_OLD_BLOCK = "        if (systemConfig.getStartAllRecord()) {"
AUTO_NODE_EVENT_NEW_BLOCK = "        if (Boolean.TRUE.equals(systemConfig.getStartAllRecord())) {"
AUTO_NODE_EVENT_START_PUSH_OLD = (
    "            plsEventClient.receiveEvent(buildDataService.buildData2EventRecordReceive(workTicket, WorkEventKeys.START));"
)
AUTO_NODE_EVENT_START_PUSH_NEW = (
    '            log.info("Skip PLS START event push in ADP PostgreSQL compatibility runtime.");'
)

WORK_PERMIT_VIEW_EVENT_OLD_PAYLOAD_BLOCK = '''            String payload = Dbs.uniqueResult(
                    "SELECT PAYLOAD FROM "+WTSWorkPermit.TABLE_NAME+" WHERE ID=? ",
                    String.class,
                    permit.getId()
            );
'''

WORK_PERMIT_VIEW_EVENT_NEW_PAYLOAD_BLOCK = '''            String payload = readPermitPayload(permit.getId());
'''

WORK_PERMIT_VIEW_EVENT_HELPER_INSERT_BEFORE = '''    @Transactional
    public void setPermitPage(Collection<WTSWorkPermit> permits){'''

WORK_PERMIT_VIEW_EVENT_HELPER_METHOD = '''    private String readPermitPayload(Long permitId) {
        if (permitId == null) {
            return null;
        }
        String payload = Dbs.uniqueResult(
                "SELECT PAYLOAD FROM "+WTSWorkPermit.TABLE_NAME+" WHERE ID=? ",
                String.class,
                permitId
        );
        if (!Strings.valid(payload)) {
            return payload;
        }
        String trimmed = payload.trim();
        if (trimmed.startsWith("{")) {
            return payload;
        }
        if (!trimmed.matches("\\\\d+")) {
            return null;
        }
        try {
            String decodedPayload = Dbs.uniqueResult(
                    "SELECT convert_from(lo_get(CAST(? AS oid)), 'UTF8')",
                    String.class,
                    Long.valueOf(trimmed)
            );
            if (Strings.valid(decodedPayload) && decodedPayload.trim().startsWith("{")) {
                return decodedPayload;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

'''


def copy_zip_info(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    copied = zipfile.ZipInfo(info.filename, info.date_time)
    copied.comment = info.comment
    copied.extra = info.extra
    copied.internal_attr = info.internal_attr
    copied.external_attr = info.external_attr
    copied.create_system = info.create_system
    copied.compress_type = info.compress_type
    return copied


def patch_inner_wts_jar(
    data: bytes,
    upload_list_controller_class: bytes | None = None,
    extra_classes: Dict[str, bytes] | None = None,
) -> Tuple[bytes, Dict[str, Any]]:
    patched_files: List[str] = []
    added_alias = False
    patched_upload_list_controller = False
    patched_extra_classes: List[str] = []
    extra_classes = extra_classes or {}
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(data), "r") as source:
        names = set(source.namelist())
        with zipfile.ZipFile(output, "w") as target:
            for info in source.infolist():
                content = source.read(info.filename)
                if upload_list_controller_class is not None and info.filename == WTS_UPLOAD_LIST_CONTROLLER_CLASS:
                    content = upload_list_controller_class
                    patched_upload_list_controller = True
                if info.filename in extra_classes:
                    content = extra_classes[info.filename]
                    patched_extra_classes.append(info.filename)
                if info.filename.endswith(".html"):
                    text = content.decode("utf-8", "ignore")
                    is_lowcode_desktop = (
                        "greenDill/static/scripts/vendors.echarts.js" in text
                        and SCRIPT_MARKER not in text
                        and any(
                            script in text
                            for script in (
                                "greenDill/static/scripts/list.js",
                                "greenDill/static/scripts/edit.js",
                                "greenDill/static/scripts/treeList.js",
                            )
                        )
                    )
                    if is_lowcode_desktop:
                        match = ECHARTS_RE.search(text)
                        if not match:
                            raise RuntimeError(f"cannot find vendors.echarts script in {info.filename}")
                        version = match.group(2)
                        insert = (
                            match.group(1)
                            + "\n"
                            + f'<script type="text/javascript" src="/greenDill/static/scripts/vendors.sesgis.js?v={version}"></script>'
                        )
                        text = ECHARTS_RE.sub(insert, text, count=1)
                        content = text.encode("utf-8")
                        patched_files.append(info.filename)
                target.writestr(copy_zip_info(info), content)

            if STAT_ICON_ALIAS not in names and STAT_ICON_SOURCE in names:
                alias_info = zipfile.ZipInfo(STAT_ICON_ALIAS, time.localtime()[:6])
                alias_info.compress_type = zipfile.ZIP_DEFLATED
                target.writestr(alias_info, source.read(STAT_ICON_SOURCE))
                added_alias = True

    return output.getvalue(), {
        "html_patched": patched_files,
        "added_static_alias": added_alias,
        "patched_upload_list_controller": patched_upload_list_controller,
        "patched_extra_classes": patched_extra_classes,
    }


def extract_boot_libs(jar_path: Path, lib_dir: Path) -> None:
    with zipfile.ZipFile(jar_path, "r") as outer:
        for name in outer.namelist():
            if name.startswith("BOOT-INF/lib/") and name.endswith(".jar"):
                (lib_dir / Path(name).name).write_bytes(outer.read(name))


def run_javac(javac: str, lib_dir: Path, classes_dir: Path, source_path: Path) -> None:
    proc = subprocess.run(
        [
            javac,
            "-parameters",
            "-encoding",
            "UTF-8",
            "-source",
            "8",
            "-target",
            "8",
            "-cp",
            str(lib_dir / "*"),
            "-d",
            str(classes_dir),
            str(source_path),
        ],
        text=True,
        capture_output=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())


def compile_java_class_patch(
    jar_path: Path,
    source_path: Path,
    class_entry: str,
    patched_source: str,
    temp_prefix: str,
) -> bytes:
    if not source_path.exists():
        raise RuntimeError(f"missing WTS source: {source_path}")
    javac = shutil.which("javac")
    if not javac:
        raise RuntimeError("javac is required to compile WTS runtime compatibility patches")

    with tempfile.TemporaryDirectory(prefix=temp_prefix) as temp_name:
        temp_dir = Path(temp_name)
        lib_dir = temp_dir / "lib"
        source_dir = temp_dir / "src" / Path(class_entry).parent
        classes_dir = temp_dir / "classes"
        lib_dir.mkdir(parents=True)
        source_dir.mkdir(parents=True)
        classes_dir.mkdir(parents=True)

        patched_source_path = source_dir / source_path.name
        patched_source_path.write_text(patched_source, encoding="utf-8")
        extract_boot_libs(jar_path, lib_dir)
        run_javac(javac, lib_dir, classes_dir, patched_source_path)

        class_path = classes_dir / class_entry
        if not class_path.exists():
            raise RuntimeError(f"compiled class was not created: {class_path}")
        return class_path.read_bytes()


def compile_upload_list_controller(jar_path: Path, source_path: Path) -> bytes:
    if not source_path.exists():
        raise RuntimeError(f"missing WTS upload-list controller source: {source_path}")
    javac = shutil.which("javac")
    if not javac:
        raise RuntimeError("javac is required to compile WTS upload-list controller patch")

    with tempfile.TemporaryDirectory(prefix="adp-wts-upload-list-") as temp_name:
        temp_dir = Path(temp_name)
        lib_dir = temp_dir / "lib"
        source_dir = temp_dir / "src/com/supcon/orchid/WTS/controllers"
        classes_dir = temp_dir / "classes"
        lib_dir.mkdir(parents=True)
        source_dir.mkdir(parents=True)
        classes_dir.mkdir(parents=True)

        source_text = source_path.read_text(encoding="utf-8")
        if UPLOAD_LIST_NEW_BLOCK in source_text:
            patched_source = source_text
        elif UPLOAD_LIST_OLD_BLOCK in source_text:
            patched_source = source_text.replace(UPLOAD_LIST_OLD_BLOCK, UPLOAD_LIST_NEW_BLOCK, 1)
        else:
            raise RuntimeError("target upload-list permission block was not found in WTSFileDownloadController.java")
        patched_source_path = source_dir / "WTSFileDownloadController.java"
        patched_source_path.write_text(patched_source, encoding="utf-8")

        extract_boot_libs(jar_path, lib_dir)
        run_javac(javac, lib_dir, classes_dir, patched_source_path)

        class_path = classes_dir / WTS_UPLOAD_LIST_CONTROLLER_CLASS
        if not class_path.exists():
            raise RuntimeError(f"compiled class was not created: {class_path}")
        return class_path.read_bytes()


def compile_work_ticket_controller(jar_path: Path, source_path: Path) -> bytes:
    source_text = source_path.read_text(encoding="utf-8")
    if WORK_TICKET_CONTROLLER_PROCESS_METHOD_VALIDATION_NEW in source_text:
        patched_source = source_text
    elif WORK_TICKET_CONTROLLER_PROCESS_METHOD_VALIDATION_OLD in source_text:
        patched_source = source_text.replace(
            WORK_TICKET_CONTROLLER_PROCESS_METHOD_VALIDATION_OLD,
            WORK_TICKET_CONTROLLER_PROCESS_METHOD_VALIDATION_NEW,
            1,
        )
    else:
        raise RuntimeError("target processMethod validation block was not found in WTSWorkTicketController.java")
    return compile_java_class_patch(
        jar_path,
        source_path,
        WTS_WORK_TICKET_CONTROLLER_CLASS,
        patched_source,
        "adp-wts-work-ticket-controller-",
    )


def compile_work_ticket_2_permit_service(jar_path: Path, source_path: Path) -> bytes:
    source_text = source_path.read_text(encoding="utf-8")
    patched_source = source_text
    if WORK_TICKET_2_PERMIT_NEW_IMPORT not in patched_source:
        if WORK_TICKET_2_PERMIT_OLD_IMPORT not in patched_source:
            raise RuntimeError("target imports were not found in WorkTicket2PermitService.java")
        patched_source = patched_source.replace(
            WORK_TICKET_2_PERMIT_OLD_IMPORT,
            WORK_TICKET_2_PERMIT_NEW_IMPORT,
            1,
        )
    if WORK_TICKET_2_PERMIT_NEW_METHOD not in patched_source:
        if WORK_TICKET_2_PERMIT_OLD_METHOD not in patched_source:
            raise RuntimeError("target method was not found in WorkTicket2PermitService.java")
        patched_source = patched_source.replace(
            WORK_TICKET_2_PERMIT_OLD_METHOD,
            WORK_TICKET_2_PERMIT_NEW_METHOD,
            1,
        )
    return compile_java_class_patch(
        jar_path,
        source_path,
        WORK_TICKET_2_PERMIT_SERVICE_CLASS,
        patched_source,
        "adp-wts-ticket-permit-",
    )


def compile_work_ticket_event(jar_path: Path, source_path: Path) -> bytes:
    source_text = source_path.read_text(encoding="utf-8")
    if WORK_TICKET_EVENT_NEW_BLOCK in source_text:
        patched_source = source_text
    elif WORK_TICKET_EVENT_OLD_BLOCK in source_text:
        patched_source = source_text.replace(WORK_TICKET_EVENT_OLD_BLOCK, WORK_TICKET_EVENT_NEW_BLOCK, 1)
    else:
        raise RuntimeError("target end-node block was not found in WorkTicketEvent.java")
    if WORK_TICKET_EVENT_PROCESS_METHOD_NEW_BLOCK not in patched_source:
        if WORK_TICKET_EVENT_PROCESS_METHOD_OLD_BLOCK not in patched_source:
            raise RuntimeError("target processMethod execution-state block was not found in WorkTicketEvent.java")
        patched_source = patched_source.replace(
            WORK_TICKET_EVENT_PROCESS_METHOD_OLD_BLOCK,
            WORK_TICKET_EVENT_PROCESS_METHOD_NEW_BLOCK,
            1,
        )
    if WORK_TICKET_EVENT_END_PUSH_NEW not in patched_source:
        if WORK_TICKET_EVENT_END_PUSH_OLD not in patched_source:
            raise RuntimeError("target PLS END event push line was not found in WorkTicketEvent.java")
        patched_source = patched_source.replace(
            WORK_TICKET_EVENT_END_PUSH_OLD,
            WORK_TICKET_EVENT_END_PUSH_NEW,
            1,
        )
    return compile_java_class_patch(
        jar_path,
        source_path,
        WORK_TICKET_EVENT_CLASS,
        patched_source,
        "adp-wts-ticket-event-",
    )


def compile_auto_node_event(jar_path: Path, source_path: Path) -> bytes:
    source_text = source_path.read_text(encoding="utf-8")
    if AUTO_NODE_EVENT_NEW_BLOCK in source_text:
        patched_source = source_text
    elif AUTO_NODE_EVENT_OLD_BLOCK in source_text:
        patched_source = source_text.replace(AUTO_NODE_EVENT_OLD_BLOCK, AUTO_NODE_EVENT_NEW_BLOCK, 1)
    else:
        raise RuntimeError("target start-all-record block was not found in AutoNodeEvent.java")
    if AUTO_NODE_EVENT_START_PUSH_NEW not in patched_source:
        if AUTO_NODE_EVENT_START_PUSH_OLD not in patched_source:
            raise RuntimeError("target PLS START event push line was not found in AutoNodeEvent.java")
        patched_source = patched_source.replace(
            AUTO_NODE_EVENT_START_PUSH_OLD,
            AUTO_NODE_EVENT_START_PUSH_NEW,
            1,
        )
    return compile_java_class_patch(
        jar_path,
        source_path,
        AUTO_NODE_EVENT_CLASS,
        patched_source,
        "adp-wts-auto-node-event-",
    )


def compile_work_permit_view_event(jar_path: Path, source_path: Path) -> bytes:
    source_text = source_path.read_text(encoding="utf-8")
    if "String payload = readPermitPayload(permit.getId());" in source_text:
        patched_source = source_text
    elif WORK_PERMIT_VIEW_EVENT_OLD_PAYLOAD_BLOCK in source_text:
        patched_source = source_text.replace(
            WORK_PERMIT_VIEW_EVENT_OLD_PAYLOAD_BLOCK,
            WORK_PERMIT_VIEW_EVENT_NEW_PAYLOAD_BLOCK,
            1,
        )
    else:
        raise RuntimeError("target payload read block was not found in WorkPermitViewEvent.java")
    if "private String readPermitPayload(Long permitId)" not in patched_source:
        if WORK_PERMIT_VIEW_EVENT_HELPER_INSERT_BEFORE not in patched_source:
            raise RuntimeError("target helper insertion point was not found in WorkPermitViewEvent.java")
        patched_source = patched_source.replace(
            WORK_PERMIT_VIEW_EVENT_HELPER_INSERT_BEFORE,
            WORK_PERMIT_VIEW_EVENT_HELPER_METHOD + WORK_PERMIT_VIEW_EVENT_HELPER_INSERT_BEFORE,
            1,
        )
    return compile_java_class_patch(
        jar_path,
        source_path,
        WORK_PERMIT_VIEW_EVENT_CLASS,
        patched_source,
        "adp-wts-work-permit-view-event-",
    )


def read_precompiled_class(precompiled_dir: Path, class_entry: str) -> bytes:
    class_path = precompiled_dir / class_entry
    if not class_path.exists():
        raise RuntimeError(f"missing precompiled WTS class: {class_path}")
    return class_path.read_bytes()


def patch_wts_jar(
    jar_path: Path,
    backup_suffix: str,
    upload_list_controller_class: bytes | None = None,
    extra_classes: Dict[str, bytes] | None = None,
) -> Dict[str, Any]:
    original = jar_path.read_bytes()
    changed = False
    output = io.BytesIO()
    stats: Dict[str, Any] = {}
    with zipfile.ZipFile(io.BytesIO(original), "r") as source:
        if INNER_WTS_SERVICE_JAR not in source.namelist():
            raise RuntimeError(f"missing nested WTS service jar: {INNER_WTS_SERVICE_JAR}")
        with zipfile.ZipFile(output, "w") as target:
            for info in source.infolist():
                content = source.read(info.filename)
                if info.filename == INNER_WTS_SERVICE_JAR:
                    content, stats = patch_inner_wts_jar(content, upload_list_controller_class, extra_classes)
                    changed = bool(
                        stats["html_patched"]
                        or stats["added_static_alias"]
                        or stats["patched_upload_list_controller"]
                        or stats["patched_extra_classes"]
                    )
                target.writestr(copy_zip_info(info), content)

    if changed:
        backup = jar_path.with_name(jar_path.name + backup_suffix)
        if not backup.exists():
            shutil.copy2(jar_path, backup)
        temp_path = jar_path.with_name(jar_path.name + ".tmp-wts-runtime-compat")
        temp_path.write_bytes(output.getvalue())
        shutil.copystat(jar_path, temp_path)
        temp_path.replace(jar_path)
        stats["backup"] = str(backup)
    stats["changed"] = changed
    return stats


def docker_psql_text(container: str, sql: str, check: bool = True) -> str:
    proc = subprocess.run(
        ["docker", "exec", "-i", container, "psql", "-U", "adp", "-d", "adp", "-At", "-c", sql],
        text=True,
        capture_output=True,
        check=False,
    )
    if check and proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())
    return proc.stdout


def docker_psql_exec(container: str, sql: str) -> None:
    proc = subprocess.run(
        ["docker", "exec", "-i", container, "psql", "-U", "adp", "-d", "adp", "-v", "ON_ERROR_STOP=1"],
        input=sql,
        text=True,
        capture_output=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())


def query_json(container: str, sql: str) -> List[Dict[str, Any]]:
    text = docker_psql_text(container, sql).strip()
    return json.loads(text or "[]")


def xml_text(root: ET.Element, tag: str) -> str | None:
    found = root.find(f".//{tag}")
    return found.text if found is not None else None


def parse_field_config(config: str | None) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    if not config:
        return result
    try:
        root = ET.fromstring(config)
    except ET.ParseError:
        return result

    width = xml_text(root, "width")
    if width and width.isdigit():
        result["width"] = int(width)

    is_hidden = xml_text(root, "isHidden")
    if is_hidden is not None:
        result["isHidden"] = is_hidden.lower() == "true"

    fill = root.find(".//fill")
    if fill is not None:
        fill_content = xml_text(fill, "fillContent") or ""
        if fill_content:
            result["fill"] = {
                "fillName": xml_text(fill, "fillName") or "system code",
                "fillType": xml_text(fill, "fillType") or "3",
                "fillContent": fill_content,
            }
    return result


FIELD_PRIORITY = {
    "workTicketNo": 0,
    "ticketNo": 1,
    "workType": 2,
    "content": 3,
    "applyDept.name": 4,
    "applyStaff.name": 5,
    "applyTime": 6,
    "startTime": 7,
    "endTime": 8,
    "contractor.name": 9,
    "workDept.name": 10,
}


def field_sort_key(field: Dict[str, Any]) -> Tuple[int, int, str]:
    key = field.get("field_key") or ""
    return (1 if field.get("is_hidden") else 0, FIELD_PRIORITY.get(key, 100), field.get("code") or "")


def model_code(entity_code: str) -> str:
    if entity_code.endswith("_workTicket"):
        return entity_code + "_WorkTicket"
    return entity_code


def build_layout(view: Dict[str, Any], fields: Iterable[Dict[str, Any]]) -> Dict[str, Any]:
    view_fields = []
    for field in sorted(fields, key=field_sort_key):
        key = field.get("field_key") or field.get("name") or field.get("display_name") or "unknown"
        config = parse_field_config(field.get("config"))
        item: Dict[str, Any] = {
            "key": key,
            "namekey": field.get("display_name") or field.get("name") or key,
            "showType": field.get("show_type") or "TEXTFIELD",
            "showFormat": field.get("show_format") or "TEXT",
            "width": config.get("width", 150 if key.endswith("Time") else 120),
            "isHidden": config.get("isHidden", bool(field.get("is_hidden"))),
            "columnType": field.get("column_type") or "TEXT",
        }
        if "fill" in config:
            item["fill"] = config["fill"]
        view_fields.append(item)

    main_display = "workTicketNo"
    if not any(field["key"] == main_display for field in view_fields) and view_fields:
        main_display = view_fields[0]["key"]
    base_url = re.sub(r"/[^/]+$", "", view.get("url") or "")

    return {
        "pageType": "LIST",
        "title": view.get("title") or view["code"],
        "url": view.get("url") or "",
        "isMain": True,
        "hasAttachment": bool(view.get("has_attachment")),
        "onlyForQuery": bool(view.get("only_for_query")),
        "components": [
            {
                "type": "layout",
                "layoutmethod": "column",
                "components": [
                    {
                        "type": "layoutSearchWidget",
                        "code": "query",
                        "layoutName": "layoutSearchWidget",
                        "layoutmethod": "container",
                        "fix_h": 150,
                        "fastProperty": [],
                        "advProperty": [],
                    },
                    {
                        "type": "layoutDatagrid",
                        "layoutmethod": "container",
                        "ratio_h": 100,
                        "modelCode": model_code(view["entity_code"]),
                        "hasFastQuery": True,
                        "mainDisplayName": main_display,
                        "idPrefix": "compat_" + view["code"],
                        "buttons": [],
                        "fields": view_fields,
                        "downloadXls": base_url + "/downloadXls" if base_url else "",
                        "importMainXls": base_url + "/importMainXls" if base_url else "",
                    },
                ],
            }
        ],
        "isFileView": True,
        "moveFlag": bool(view.get("move_flag")),
    }


def patch_wts_layouts(container: str) -> Dict[str, Any]:
    view_sql = """
select coalesce(json_agg(row_to_json(t)), '[]'::json)::text
from (
  select v.code, v.title, v.url, v.entity_code, v.has_attachment, v.only_for_query, v.move_flag
  from ec_view v
  join runtime_extra_view r on r.view_code = v.code
  where v.code like 'WTS_1.0.0_workTicket_%'
    and v.code not like '%__mobile__'
    and v.type = 'LIST'
    and r.view_json is null
    and exists (
      select 1 from ec_field f where f.view_code = v.code and f.code like '%_LISTPT_%'
    )
  order by v.code
) t;
"""
    field_sql = """
select coalesce(json_agg(row_to_json(t)), '[]'::json)::text
from (
  select f.view_code, f.code, f.field_key, f.display_name, f.name, f.show_type,
         f.show_format, f.column_type, f.is_hidden, f.config
  from ec_field f
  join ec_view v on v.code = f.view_code
  join runtime_extra_view r on r.view_code = v.code
  where v.code like 'WTS_1.0.0_workTicket_%'
    and v.code not like '%__mobile__'
    and v.type = 'LIST'
    and r.view_json is null
    and f.code like '%_LISTPT_%'
  order by f.view_code, f.code
) t;
"""
    views = query_json(container, view_sql)
    fields = query_json(container, field_sql)
    fields_by_view: Dict[str, List[Dict[str, Any]]] = {}
    for field in fields:
        fields_by_view.setdefault(field["view_code"], []).append(field)

    if not views:
        return {"updated": 0, "views": []}

    timestamp = time.strftime("%Y%m%d_%H%M%S")
    runtime_backup = f"runtime_extra_view_wts_layout_backup_{timestamp}"
    ec_backup = f"ec_extra_view_wts_layout_backup_{timestamp}"
    statements = [
        "BEGIN;",
        f"create table {runtime_backup} as select * from runtime_extra_view where view_code like 'WTS_1.0.0_workTicket_%';",
        f"create table {ec_backup} as select * from ec_extra_view where view_code like 'WTS_1.0.0_workTicket_%';",
    ]
    updated_views = []
    for view in views:
        layout = build_layout(view, fields_by_view.get(view["code"], []))
        raw = json.dumps(layout, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        encoded = base64.b64encode(raw).decode("ascii")
        code = view["code"].replace("'", "''")
        statements.append(
            "update runtime_extra_view "
            f"set view_json = lo_from_bytea(0, decode('{encoded}', 'base64')) "
            f"where view_code = '{code}' and view_json is null;"
        )
        statements.append(
            "update ec_extra_view "
            f"set view_json = convert_from(decode('{encoded}', 'base64'), 'UTF8') "
            f"where view_code = '{code}' and view_json is null;"
        )
        updated_views.append({"code": view["code"], "fields": len(layout["components"][0]["components"][1]["fields"])})
    statements.append("COMMIT;")
    docker_psql_exec(container, "\n".join(statements))
    return {"updated": len(updated_views), "runtime_backup": runtime_backup, "ec_backup": ec_backup, "views": updated_views}


def restart_container(name: str) -> None:
    subprocess.run(["docker", "restart", name], check=True)


def default_root() -> Path:
    return Path(__file__).resolve().parents[3]


def parse_args() -> argparse.Namespace:
    root = Path(os.environ.get("ADP_DEPLOY_ROOT", default_root())).expanduser()
    default_wts_upload_list_source = (
        root.parent
        / "mes-modules-source-repo/modules/wts/WTS_6.1.8.2/service/src/main/java/com/supcon/orchid/WTS/controllers/WTSFileDownloadController.java"
    )
    default_wts_work_ticket_controller_source = (
        root.parent
        / "mes-modules-source-repo/modules/wts/WTS_6.1.8.2/service/src/main/java/com/supcon/orchid/WTS/controllers/WTSWorkTicketController.java"
    )
    default_work_ticket_2_permit_source = (
        root.parent
        / "mes-modules-source-repo/modules/wts/WTS_6.1.8.2/service/src/main/custom/com/supcon/orchid/WTS/superzy/basic/services/WorkTicket2PermitService.java"
    )
    default_work_ticket_event_source = (
        root.parent
        / "mes-modules-source-repo/modules/wts/WTS_6.1.8.2/service/src/main/custom/com/supcon/orchid/WTS/superzy/basic/events/WorkTicketEvent.java"
    )
    default_auto_node_event_source = (
        root.parent
        / "mes-modules-source-repo/modules/wts/WTS_6.1.8.2/service/src/main/custom/com/supcon/orchid/WTS/superzy/basic/events/AutoNodeEvent.java"
    )
    default_work_permit_view_event_source = (
        root.parent
        / "mes-modules-source-repo/modules/wts/WTS_6.1.8.2/service/src/main/custom/com/supcon/orchid/WTS/superzy/view/events/WorkPermitViewEvent.java"
    )
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=root)
    parser.add_argument(
        "--wts-jar",
        type=Path,
        default=root / "runtime/bap-server/module-Server/WTSs/manual/WTSs-1.0.0.jar",
    )
    parser.add_argument("--postgres-container", default=os.environ.get("POSTGRES_CONTAINER", "adp-mes-newbase-postgres-1"))
    parser.add_argument("--wts-container", default=os.environ.get("WTS_CONTAINER", "adp-mes-newbase-WTSs-1"))
    parser.add_argument(
        "--baseservice-container",
        default=os.environ.get("BASESERVICE_CONTAINER", "adp-mes-newbase-baseService-1"),
    )
    parser.add_argument("--no-jar", action="store_true", help="skip WTS jar patch")
    parser.add_argument("--no-db", action="store_true", help="skip runtime_extra_view layout patch")
    parser.add_argument(
        "--wts-upload-list-source",
        type=Path,
        default=Path(os.environ.get("ADP_WTS_UPLOAD_LIST_SOURCE", default_wts_upload_list_source)),
        help="source WTSFileDownloadController.java used to compile the nested upload-list controller patch",
    )
    parser.add_argument(
        "--no-upload-list-controller",
        action="store_true",
        help="skip nested WTSFileDownloadController upload-list compatibility patch",
    )
    parser.add_argument(
        "--wts-work-ticket-controller-source",
        type=Path,
        default=Path(
            os.environ.get("ADP_WTS_WORK_TICKET_CONTROLLER_SOURCE", default_wts_work_ticket_controller_source)
        ),
        help="source WTSWorkTicketController.java used to compile WTS processMethod validation patch",
    )
    parser.add_argument(
        "--wts-work-ticket-2-permit-source",
        type=Path,
        default=Path(os.environ.get("ADP_WTS_WORK_TICKET_2_PERMIT_SOURCE", default_work_ticket_2_permit_source)),
        help="source WorkTicket2PermitService.java used to compile WTS permit lookup compatibility patch",
    )
    parser.add_argument(
        "--wts-work-ticket-event-source",
        type=Path,
        default=Path(os.environ.get("ADP_WTS_WORK_TICKET_EVENT_SOURCE", default_work_ticket_event_source)),
        help="source WorkTicketEvent.java used to compile WTS close-node null-safe compatibility patch",
    )
    parser.add_argument(
        "--wts-auto-node-event-source",
        type=Path,
        default=Path(os.environ.get("ADP_WTS_AUTO_NODE_EVENT_SOURCE", default_auto_node_event_source)),
        help="source AutoNodeEvent.java used to compile WTS null-safe runtime config compatibility patch",
    )
    parser.add_argument(
        "--wts-work-permit-view-event-source",
        type=Path,
        default=Path(os.environ.get("ADP_WTS_WORK_PERMIT_VIEW_EVENT_SOURCE", default_work_permit_view_event_source)),
        help="source WorkPermitViewEvent.java used to compile WTS payload OID compatibility patch",
    )
    parser.add_argument(
        "--no-work-ticket-flow",
        action="store_true",
        help="skip WTS work-ticket permit lookup and close-node compatibility patches",
    )
    parser.add_argument(
        "--precompiled-work-ticket-class-dir",
        type=Path,
        default=Path(os.environ["ADP_WTS_PRECOMPILED_WORK_TICKET_CLASS_DIR"])
        if os.environ.get("ADP_WTS_PRECOMPILED_WORK_TICKET_CLASS_DIR")
        else None,
        help="directory containing precompiled WTS work-ticket compatibility class files",
    )
    parser.add_argument("--restart", action="store_true", help="restart WTS and baseService containers after changes")
    parser.add_argument("--backup-suffix", default=f".bak-wts-runtime-compat-{int(time.time())}")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    result: Dict[str, Any] = {}
    try:
        if not args.no_jar:
            upload_list_controller_class = None
            if not args.no_upload_list_controller:
                upload_list_controller_class = compile_upload_list_controller(
                    args.wts_jar.expanduser().resolve(),
                    args.wts_upload_list_source.expanduser().resolve(),
                )
            extra_classes: Dict[str, bytes] = {}
            if not args.no_work_ticket_flow:
                if args.precompiled_work_ticket_class_dir:
                    precompiled_dir = args.precompiled_work_ticket_class_dir.expanduser().resolve()
                    extra_classes[WTS_WORK_TICKET_CONTROLLER_CLASS] = read_precompiled_class(
                        precompiled_dir,
                        WTS_WORK_TICKET_CONTROLLER_CLASS,
                    )
                    extra_classes[WORK_TICKET_2_PERMIT_SERVICE_CLASS] = read_precompiled_class(
                        precompiled_dir,
                        WORK_TICKET_2_PERMIT_SERVICE_CLASS,
                    )
                    extra_classes[WORK_TICKET_EVENT_CLASS] = read_precompiled_class(
                        precompiled_dir,
                        WORK_TICKET_EVENT_CLASS,
                    )
                    extra_classes[AUTO_NODE_EVENT_CLASS] = read_precompiled_class(
                        precompiled_dir,
                        AUTO_NODE_EVENT_CLASS,
                    )
                    extra_classes[WORK_PERMIT_VIEW_EVENT_CLASS] = read_precompiled_class(
                        precompiled_dir,
                        WORK_PERMIT_VIEW_EVENT_CLASS,
                    )
                else:
                    extra_classes[WTS_WORK_TICKET_CONTROLLER_CLASS] = compile_work_ticket_controller(
                        args.wts_jar.expanduser().resolve(),
                        args.wts_work_ticket_controller_source.expanduser().resolve(),
                    )
                    extra_classes[WORK_TICKET_2_PERMIT_SERVICE_CLASS] = compile_work_ticket_2_permit_service(
                        args.wts_jar.expanduser().resolve(),
                        args.wts_work_ticket_2_permit_source.expanduser().resolve(),
                    )
                    extra_classes[WORK_TICKET_EVENT_CLASS] = compile_work_ticket_event(
                        args.wts_jar.expanduser().resolve(),
                        args.wts_work_ticket_event_source.expanduser().resolve(),
                    )
                    extra_classes[AUTO_NODE_EVENT_CLASS] = compile_auto_node_event(
                        args.wts_jar.expanduser().resolve(),
                        args.wts_auto_node_event_source.expanduser().resolve(),
                    )
                    extra_classes[WORK_PERMIT_VIEW_EVENT_CLASS] = compile_work_permit_view_event(
                        args.wts_jar.expanduser().resolve(),
                        args.wts_work_permit_view_event_source.expanduser().resolve(),
                    )
            result["jar"] = patch_wts_jar(
                args.wts_jar.expanduser().resolve(),
                args.backup_suffix,
                upload_list_controller_class,
                extra_classes,
            )
        if not args.no_db:
            result["db"] = patch_wts_layouts(args.postgres_container)
        if args.restart:
            if result.get("jar", {}).get("changed"):
                restart_container(args.wts_container)
            if result.get("db", {}).get("updated"):
                restart_container(args.baseservice_container)
            result["restarted"] = {
                "wts": bool(result.get("jar", {}).get("changed")),
                "baseService": bool(result.get("db", {}).get("updated")),
            }
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
