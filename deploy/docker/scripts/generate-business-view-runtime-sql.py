#!/usr/bin/env python3
"""Generate PostgreSQL runtime view metadata for recovered MES modules.

The Windows-origin module packages carry page definitions in META-INF/bap/module.xml.
During the Linux/PostgreSQL recovery path those rows can exist without generated
runtime_extra_view.view_json, which makes /baseService/view/layoutJson fail at
page open time. This script creates a repeatable compatibility SQL patch from
the source module XMLs.
"""

from __future__ import annotations

import argparse
import base64
import copy
import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple


TARGET_VIEW_CODES: Sequence[str] = (
    "BaseSet_1.0.0_cooperate_cmcLayout",
    "BaseSet_1.0.0_material_materialLayout",
    "BaseSet_1.0.0_unit_unitGroupList",
    "ChartReportMap_1.0.0_scatterChartSet_scatterChartList",
    "DataSet_1.0.0_categoryMgt_categoryList",
    "HierarchicalMod_1.0.0_factoryModel_factoryTreeList",
    "HierarchicalMod_1.0.0_factoryModel_factoryEdit",
    "HierarchicalMod_1.0.0_factoryNodeType_nodeTypeRef",
    "TagManagement_1.0.0_tagInfo_tagList",
    "TagManagement_1.0.0_dataConvert_dataConvertList",
    "TeamInfo_1.0.0_team_teamList",
    "TeamInfo_1.0.0_team_teamLayout",
    "TeamInfo_1.0.0_team_teamEdit",
    "TeamInfo_1.0.0_team_teamView",
    "TeamInfo_1.0.0_schedulePlan_schedulePlanList",
    "TeamInfo_1.0.0_schedulePlan_schedulePlanEdit",
    "TeamInfo_1.0.0_schedulePlan_schedulePlanView",
    "TeamInfo_1.0.0_schedual_schedualDeptLayout",
    "Qualify_1.0.0_certificate_certifcateLayOut",
    "DocManage_1.0.0_document_documentList",
    "LIMSBasic_1.0.0_analySample_analySampleList",
    "LIMSBasic_1.0.0_sampleType_sampleTypeList",
    "LIMSDC_5.1.0.1_analysisFile_analysisFileRef",
    "LIMSDC_5.1.0.1_analysisFile_analysisFileRef2",
    "LIMSDC_5.1.0.1_collectionType_collectionTypeRef",
    "LIMSMaterial_5.1.0.1_mATInfo_matInfoList",
    "LIMSRetain_5.0.4.1_retention_retPlanList",
    "LIMSSample_5.0.0.0_sample_collectListLayout",
    "LIMSSTDS_5.1.0.1_aqPrepRecord_aqPrepRecordList",
    "LIMSSteady_6.0.4.1_envCondition_envConditionList",
    "QCS_5.0.0.0_inspect_manuInspectList",
    "QCS_5.0.0.0_inspectReport_manuInspReportEdit",
    "WOM_1.0.0_produceTask_makeTaskList",
    "WOM_1.0.0_produceTask_prepareMakeTaskList",
    "WOM_1.0.0_produceTask_makeTaskEdit",
    "WOM_1.0.0_produceTask_makeTaskSubmitView",
    "WOM_1.0.0_produceTask_makeTaskView",
    "WOM_1.0.0_produceTask_makeTaskBatchView",
    "WOM_1.0.0_produceTask_easyTaskOperateView",
    "WOM_1.0.0_produceTask_processExeLogList",
    "WOM_1.0.0_procReport_outPutCommonTaskEdit",
    "WOM_1.0.0_batchMaterial_baRetireMentPDAList",
    "WOM_1.0.0_rejectMaterilal_batchRejectEdit",
    "WOM_1.0.0_rejectMaterilal_materiaRejectEdit",
    "WOM_1.0.0_rejectMaterilal_materiaEditableEdit",
    "WOM_1.0.0_rejectMaterilal_prePareRejectEdit",
    "RM_1.0.0_formula_batchFormulaList",
    "craftGraph_1.0_basicInfo_basicInfoList",
    "craftGraph_1.0_operationButton_buttonConfList",
    "EAM_1.0.0_baseInfo_baseInfoLayout",
    "OverEquipEffect_6.1.6.1_runRecord_runningRecordLayout",
    "TOOL_6.0.0.0_toolInfo_toolLayout",
    "Special_0.0.1_semFinishCheck_finishCheckList",
    "WTS_1.0.0_workPermit_workPermitList",
    "workAppointment_6.1.6.1_workPlan_workPlanList",
)

PROCESS_EXECUTION_DETAIL_DOUBLE_CLICK = """function processExeLogListDB(event, row) {
    if (!row || !row.id) {
        ReactAPI.showMessage("w", "请选择一条工序执行记录");
        return false;
    }
    var target = "/msService/ProcessAnalysis/processAnalysis/processExecution/detail"
        + "?processExecutionId=" + encodeURIComponent(row.id);
    window.open(target, "_blank");
    return true;
}"""

DATAGRID_RUNTIME_OVERRIDES: Dict[str, Dict[str, Any]] = {
    "HierarchicalMod_1.0.0_factoryModel_factoryListPart": {
        "DataGridCode": "HierarchicalMod_1.0.0_factoryModel_factoryListPart",
    },
    "HierarchicalMod_1.0.0_factoryModel_factoryTreeList": {
        "DataGridCode": "HierarchicalMod_1.0.0_factoryModel_factoryListPart",
    },
    "WOM_1.0.0_produceTask_processExeLogList": {
        "isdbcustom": True,
        "dbcustomtextarea": PROCESS_EXECUTION_DETAIL_DOUBLE_CLICK,
        "dbcustomtextarea_es5": PROCESS_EXECUTION_DETAIL_DOUBLE_CLICK,
    },
}

MAKE_TASK_BUSINESS_TAB_RESTORE = """
(function restoreMakeTaskBusinessTabs(attempt) {
    setTimeout(function () {
        var formData = ReactAPI.getFormData();
        if (formData && formData.id && formData.formulaId && formData.planNum != null) {
            ReactAPI.Layout.showTab("tabs-3");
            ReactAPI.Layout.showTab("tabs-4");
            if (!formData.formulaId.setProcess
                    || formData.formulaId.setProcess.id != "RM_formulaType/commonFormula") {
                ReactAPI.Layout.showTab("tabs-5");
            }
            if (attempt < 50) {
                restoreMakeTaskBusinessTabs(attempt + 1);
            }
            return;
        }
        if (attempt < 50) {
            restoreMakeTaskBusinessTabs(attempt + 1);
            return;
        }
        ReactAPI.Layout.hideTab("tabs-3");
        ReactAPI.Layout.hideTab("tabs-4");
        ReactAPI.Layout.hideTab("tabs-5");
    }, attempt == 0 ? 0 : 100);
})(0);
""".strip()

MAKE_TASK_VENDOR_ONLOAD_TAIL_MARKER = "// 如果配方已经同步过"

VIEW_ONLOAD_APPENDS: Dict[str, str] = {
    "WOM_1.0.0_produceTask_makeTaskEdit": MAKE_TASK_BUSINESS_TAB_RESTORE,
}

VIEW_NAMEKEY_FALLBACKS: Dict[str, Dict[str, str]] = {
    "HierarchicalMod_1.0.0_factoryModel_factoryEdit": {
        "HierarchicalMod.tabname.randon1618564480544.flag": "装置",
    },
}

@dataclass(frozen=True)
class FieldDef:
    key: str
    namekey: str
    show_type: str
    show_format: str
    width: int
    hidden: bool
    column_type: str
    model_code: str
    fill: Optional[Dict[str, str]]
    custom_section: bool = False
    custom_model_code: str = ""
    property_lay_rec: str = ""


@dataclass
class PropertyDef:
    code: str
    ec_env: str
    name: str
    display_name: str
    prop_type: str
    fmt: str
    field_type: str
    column_name: str
    entity_code: str
    module_code: str
    model_code: str
    description: str
    default_value: str
    associated_property_code: str
    fetch_mode: str
    sort: Optional[int]
    max_length: Optional[int]
    decimal_num: Optional[int]
    associated_type: Optional[int]
    show_width: Optional[int]
    max_pic_num: Optional[int]
    only_leaf: bool
    is_group_object: bool
    proj_custom_in_use: bool
    proj_flag: bool
    is_hidden: bool
    is_engine: bool
    is_custom: bool
    is_mne_whole_like_query: bool
    senior_system_code: bool
    no_analyzer: bool
    is_main_associated: bool
    is_used_for_search: bool
    stretch: bool
    is_bussiness_key: bool
    is_control: bool
    is_used_mne_code: bool
    is_sensitive: bool
    is_main_display: bool
    is_used_for_list: bool
    is_pk: bool
    is_ignore_audit: bool
    is_inherent: bool
    is_unique: bool
    multable: bool
    nullable: bool
    is_index: bool
    is_support_sup_and_sub: bool
    is_pic_support_multi_select: bool
    pic_height: str
    pic_width: str
    org_column_name: str
    is_tree_system_code: bool


@dataclass
class ModelDef:
    code: str
    source: Path
    version_key: Tuple[int, ...]
    ec_env: str
    name: str
    value_zh_cn: str
    entity_class: str
    model_name: str
    jpa_name: str
    ec_version: str
    description: str
    module_code: str
    table_name: str
    extends_model_code: str
    model_type: Optional[int]
    data_type: Optional[int]
    is_main: bool
    is_error_sql: Optional[int]
    proj_flag: bool
    is_config_special: bool
    special_auth_isandrel: bool
    is_mne_code: bool
    is_control: bool
    is_cache: bool
    is_extra_col: bool
    enable_data_audit: bool
    enable_operation_audit: bool
    enable_sync: bool
    inherent_common_flag: bool
    is_extends: bool
    entity_code: str
    properties: List[PropertyDef]


@dataclass
class EntityDef:
    code: str
    source: Path
    version_key: Tuple[int, ...]
    ec_env: str
    name: str
    value_zh_cn: str
    entity_name: str
    workflow_enabled: bool
    pay_close_attention: bool
    description: str
    prefix: str
    group_enabled: bool
    cross_company_flag: bool
    is_base: bool
    enable_audit: bool
    is_inherented_base: bool
    mobile: bool
    enable_acl_restrict: bool
    enable_rest: bool
    enable_ws: bool
    entity_type: str
    module_code: str
    enable_fields_permission_conf: bool
    proj_flag: bool
    is_control: bool
    inherent_common_flag: bool
    list_view_data_type: Optional[int]
    reff_view_data_type: Optional[int]


@dataclass
class ModuleDef:
    code: str
    source: Path
    version_key: Tuple[int, ...]
    ec_env: str
    name: str
    value_zh_cn: str
    acronym: str
    category: str
    artifact: str
    project_version: str
    initial_version: str
    description: str
    module_type: str
    deploy_order: str
    is_proto: bool
    main_module: bool
    is_hide: bool
    is_read_only: bool
    proj_flag: bool
    is_new_generate: bool
    is_inherented_base: bool
    is_cluster: Optional[int]


@dataclass
class CustomerConditionDef:
    code: str
    source: Optional[Path]
    ec_env: str
    condition_sql: str
    json_condition: str
    dataclassific_code: str
    datagrid_code: str
    view_code: str
    module_code: str
    entity_code: str
    proj_flag: bool


@dataclass
class SqlDef:
    code: str
    source: Path
    version_key: Tuple[int, ...]
    ec_env: str
    data_grid_code: str
    view_code: str
    sql_type: Optional[int]
    query_sql: str
    proj_flag: bool


@dataclass
class ViewDef:
    code: str
    source: Path
    version_key: Tuple[int, ...]
    ec_env: str
    name: str
    title: str
    display_name: str
    view_type: str
    show_type: str
    url: str
    open_type: str
    module_code: str
    ass_model_code: str
    entity_code: str
    has_attachment: bool
    only_for_query: bool
    main_view: bool
    main_ref: bool
    mobile: bool
    mobile_enable_flag: bool
    move_flag: bool
    config_root: Optional[ET.Element]


def direct_text(element: Optional[ET.Element], tag: str, default: str = "") -> str:
    if element is None:
        return default
    child = element.find(tag)
    if child is None or child.text is None:
        return default
    return child.text.strip()


def bool_text(value: str, default: bool = False) -> bool:
    if value is None or value == "":
        return default
    return value.strip().lower() in {"1", "true", "t", "yes", "y"}


def int_text(value: str, default: int = 120) -> int:
    try:
        return int(float(value))
    except (TypeError, ValueError):
        return default


def int_or_none(value: str) -> Optional[int]:
    value = (value or "").strip()
    if not value:
        return None
    try:
        return int(float(value))
    except ValueError:
        return None


def xml_value(element: ET.Element) -> Any:
    children = list(element)
    if not children:
        return (element.text or "").strip()
    if element.tag == "list":
        return [xml_value(child) for child in children if child.tag == "list-item"]

    result: Dict[str, Any] = {}
    for child in children:
        value = xml_value(child)
        if child.tag in result:
            existing = result[child.tag]
            if not isinstance(existing, list):
                result[child.tag] = [existing]
            result[child.tag].append(value)
        else:
            result[child.tag] = value
    return result


def xml_children_dict(element: Optional[ET.Element]) -> Dict[str, Any]:
    if element is None:
        return {}
    value = xml_value(element)
    return value if isinstance(value, dict) else {}


def list_items(element: Optional[ET.Element]) -> List[ET.Element]:
    if element is None:
        return []
    if element.tag == "list":
        return [child for child in list(element) if child.tag == "list-item"]
    child_list = element.find("list")
    if child_list is None:
        return []
    return [child for child in list(child_list) if child.tag == "list-item"]


def first_text(element: Optional[ET.Element], *tags: str, default: str = "") -> str:
    for tag in tags:
        value = direct_text(element, tag)
        if value:
            return value
    return default


def unwrap_xml_list(value: Any) -> Any:
    if isinstance(value, dict) and set(value.keys()) == {"list"} and isinstance(value.get("list"), list):
        return value["list"]
    return value


VIEW_REFERENCE_KEYS: Sequence[str] = (
    "viewview",
    "linkView",
    "allowviewcode",
    "referenceview",
    "viewselect",
    "allowmultviewselect",
)


BOOLEAN_XML_KEYS = {
    "acceptrevisions",
    "allowview",
    "autoresize",
    "batchoperation",
    "casesensitive",
    "complex",
    "containlower",
    "convertpdfonline",
    "couple",
    "crosscol",
    "defaultvaluehaschanged",
    "downloaddoc",
    "getrevisions",
    "hiderevision",
    "iscontrol",
    "iscallback",
    "isconfirm",
    "iscreatenew",
    "iscustom",
    "iscustomfunc",
    "isgroup",
    "ishide",
    "ishidden",
    "islink",
    "ismultable",
    "isnocopy",
    "isofficesign",
    "isofficehandsign",
    "ispermission",
    "isrefselect",
    "isreadonly",
    "isrevision",
    "ispublished",
    "issignatureconfig",
    "istitle",
    "mnecode",
    "mneenable",
    "multable",
    "multallowview",
    "nullable",
    "officeprint",
    "officenotnull",
    "openemptydoc",
    "openpending",
    "precisionhaschanged",
    "readonly",
    "savetemplate",
    "showformathaschanged",
    "showrevision",
    "showtypehaschanged",
    "useinmore",
}


BUTTON_COPY_KEYS: Sequence[str] = (
    "id",
    "showname",
    "name",
    "namekey",
    "buttonstyle",
    "operatetype",
    "operateType",
    "isHide",
    "ispermission",
    "isPublished",
    "buttonoperationcode",
    "funcname",
    "funcbody",
    "funcbody_es5",
    "callbackbody",
    "callbackbody_es5",
    "callbackname",
    "viewselect",
    "iscallback",
    "iscustomfunc",
    "useInMore",
    "isconfirm",
    "isSignatureConfig",
    "batchOperation",
    "buttonAlign",
    "cellCode",
    "ecEnv",
    "modelCode",
    "regionType",
)


BUTTON_MEANINGFUL_KEYS: Sequence[str] = (
    "id",
    "showname",
    "namekey",
    "buttonoperationcode",
    "funcbody",
    "funcname",
    "viewselect",
)


def normalize_xml_scalar_types(value: Any, key: str = "") -> Any:
    if isinstance(value, dict):
        for child_key, child_value in list(value.items()):
            value[child_key] = normalize_xml_scalar_types(child_value, child_key)
        return value
    if isinstance(value, list):
        return [normalize_xml_scalar_types(item, key) for item in value]
    if isinstance(value, str) and key.lower() in BOOLEAN_XML_KEYS:
        lowered = value.strip().lower()
        if lowered in {"true", "false"}:
            return lowered == "true"
    return value


def sanitize_runtime_strings(value: Any) -> Any:
    if isinstance(value, dict):
        for child_key, child_value in list(value.items()):
            value[child_key] = sanitize_runtime_strings(child_value)
        return value
    if isinstance(value, list):
        return [sanitize_runtime_strings(item) for item in value]
    if isinstance(value, str):
        return value.replace("`", "'")
    return value


def is_unmapped_custom_placeholder(value: Any) -> bool:
    if not isinstance(value, dict):
        return False
    custom_section = value.get("customSection")
    if not bool_text(str(custom_section or "false")):
        return False
    return not str(value.get("propertyCode") or value.get("fullPropertyCode") or "").strip()


def scrub_unmapped_custom_placeholders(value: Any) -> Any:
    """Remove tenant-only custom columns when their mapping rows are absent.

    The module package contains visual placeholders for project custom fields,
    while the concrete base_cp_* mapping rows belong to a tenant database and
    are not shipped with the base module. Keeping those unresolved placeholders
    makes the legacy layout service dereference a missing mapping.
    """
    if isinstance(value, list):
        return [
            scrub_unmapped_custom_placeholders(item)
            for item in value
            if not is_unmapped_custom_placeholder(item)
        ]
    if isinstance(value, dict):
        result: Dict[str, Any] = {}
        for key, child in value.items():
            if is_unmapped_custom_placeholder(child):
                continue
            result[key] = scrub_unmapped_custom_placeholders(child)
        return result
    return value


def normalize_packaged_runtime_payload(value: Any) -> Any:
    """Normalize authoritative packaged JSON for the recovered runtime."""
    value = scrub_unmapped_custom_placeholders(copy.deepcopy(value))
    normalize_xml_scalar_types(value)

    def visit(node: Any) -> None:
        if isinstance(node, list):
            for child in node:
                visit(child)
            return
        if not isinstance(node, dict):
            return

        operation_code = str(node.get("buttonoperationcode") or "").strip()
        if operation_code:
            node["CODE"] = operation_code
            if bool_text(str(node.get("ispermission") or "false")):
                # Nested packaged views already carry the parent-qualified
                # permission token expected by the static page's listButtons
                # contract. Recomputing it from the child operation code keeps
                # the button visible in some contexts but detaches its action.
                node.setdefault("pc", button_power_code(operation_code))
            show_name = str(node.get("showname") or node.get("name") or "").strip()
            if show_name:
                node["NAME"] = show_name
            node["ICONCLS"] = "cui-btn-" + default_button_style(node)
            node["USEINMORE"] = bool_text(
                str(node.get("useInMore") if node.get("useInMore") is not None else node.get("USEINMORE") or "false")
            )
            node.setdefault("SEPARATENUM", "0")

        for child in node.values():
            visit(child)

    visit(value)
    sanitize_runtime_strings(value)
    return value


def reference_mne_type(url: str) -> str:
    if url.startswith("/organization/#/reference?type="):
        return url.rsplit("=", 1)[-1][:1].upper() + url.rsplit("=", 1)[-1][1:]
    return "other"


def view_reference_payload(ref_code: str, views: Dict[str, "ViewDef"], element: Dict[str, Any]) -> Any:
    ref_view = views.get(ref_code)
    if ref_view is None:
        return ref_code
    cross_company = element.get("iscrosscompany")
    if cross_company in (None, ""):
        cross_company = element.get("isgroup")
    if cross_company in (None, ""):
        cross_company = "false"
    return {
        "title": ref_view.title,
        "code": ref_view.code,
        "name": ref_view.name,
        "openType": ref_view.open_type,
        "url": ref_view.url,
        "iscrosscompany": str(cross_company).lower(),
        "mneType": reference_mne_type(ref_view.url),
    }


def expand_element_view_references(element: Dict[str, Any], views: Dict[str, "ViewDef"]) -> None:
    for key in VIEW_REFERENCE_KEYS:
        ref_value = element.get(key)
        if isinstance(ref_value, str) and ref_value:
            element[key] = view_reference_payload(ref_value, views, element)
            if key == "referenceview" and not element.get("mainDisplayName"):
                field_key = str(element.get("key") or element.get("name") or "")
                if "." in field_key:
                    element["mainDisplayName"] = field_key.rsplit(".", 1)[-1]


def canonical_button_onclick(funcname: str) -> str:
    value = (funcname or "").strip()
    if not value:
        return ""
    match = re.search(r"onclick\s*=\s*['\"]([^'\"]+)['\"]", value)
    if match:
        return match.group(1).strip()
    if value.startswith("onclick="):
        return value.split("=", 1)[-1].strip().strip("'\"")
    return value


def button_power_code(operation_code: str) -> str:
    """Mirror OrchidUtils/BAPUrlBase64 for runtime button permission checks."""
    encoded = base64.urlsafe_b64encode((operation_code + "|").encode("utf-8")).decode("ascii")
    return "__pc__=" + encoded.replace("=", "_")


def default_button_style(button: Dict[str, Any]) -> str:
    style = str(button.get("buttonstyle") or "").strip()
    if style:
        return style
    operate_type = str(button.get("operatetype") or button.get("operateType") or "").upper()
    return {
        "ADD": "add",
        "MODIFY": "modify",
        "UPDATE": "modify",
        "DELETE": "del",
        "DEL": "del",
        "VIEW": "view",
    }.get(operate_type, "add")


def button_payload(button_item: ET.Element, views: Optional[Dict[str, "ViewDef"]] = None) -> Optional[Dict[str, Any]]:
    raw = {key: direct_text(button_item, key) for key in BUTTON_COPY_KEYS}
    if not any(raw.get(key) for key in BUTTON_MEANINGFUL_KEYS):
        return None
    if bool_text(raw.get("isHide", "")):
        return None

    result: Dict[str, Any] = {}
    for key, value in raw.items():
        if value != "":
            result[key] = value

    show_name = str(result.get("showname") or result.get("name") or result.get("namekey") or "").strip()
    original_namekey = str(result.get("namekey") or "").strip()
    if original_namekey and show_name and original_namekey != show_name:
        result["i18nKey"] = original_namekey
    if show_name:
        result["name"] = show_name
        result["showname"] = show_name
        result["namekey"] = show_name

    if "operateType" in result and "operatetype" not in result:
        result["operatetype"] = result["operateType"]
    if "operatetype" in result:
        result["operatetype"] = str(result["operatetype"]).upper()
        result["operateType"] = result["operatetype"]

    button_style = default_button_style(result)
    result["buttonstyle"] = button_style
    operation_code = str(result.get("buttonoperationcode") or result.get("id") or "").strip()
    onclick = canonical_button_onclick(str(result.get("funcname") or ""))
    if onclick:
        result["onclick"] = onclick
        result["ONCLICK"] = onclick
    if operation_code:
        result["CODE"] = operation_code
        if bool_text(str(result.get("ispermission", "false"))):
            result["pc"] = button_power_code(operation_code)
    if show_name:
        result["NAME"] = show_name
    result["ICONCLS"] = "cui-btn-" + button_style
    result["USEINMORE"] = str(result.get("useInMore", "false")).lower()
    result["SEPARATENUM"] = "0"

    normalize_xml_scalar_types(result)
    viewselect = result.get("viewselect")
    if isinstance(viewselect, str) and viewselect:
        result["viewselect"] = view_reference_payload(viewselect, views or {}, result)
    return result


def extract_buttons(view: "ViewDef", views: Optional[Dict[str, "ViewDef"]] = None) -> List[Dict[str, Any]]:
    buttons: List[Dict[str, Any]] = []
    seen = set()
    for item in iter_region_items(view, "BUTTON"):
        payload = button_payload(item, views)
        if payload is None:
            continue
        key = (
            payload.get("buttonoperationcode")
            or payload.get("id")
            or payload.get("funcname")
            or payload.get("showname")
            or json.dumps(payload, sort_keys=True, ensure_ascii=False)
        )
        if key in seen:
            continue
        seen.add(key)
        buttons.append(payload)
    return buttons


def null_if_blank(value: str) -> Optional[str]:
    value = (value or "").strip()
    return value or None


def sql_str(value: Optional[str]) -> str:
    if value is None or value == "":
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def sql_bool(value: bool) -> str:
    return "true" if value else "false"


def sql_bool_int(value: bool) -> str:
    return "1" if value else "0"


def sql_int(value: Optional[int]) -> str:
    return "NULL" if value is None else str(value)


def module_version_key(path: Path) -> Tuple[int, ...]:
    best: Tuple[int, ...] = ()
    for part in path.parts:
        match = re.search(r"_([0-9]+(?:\.[0-9]+)+)", part)
        if not match:
            continue
        version = tuple(int(piece) for piece in match.group(1).split("."))
        if version > best:
            best = version
    return best


def load_packaged_view_json(modules_root: Path) -> Dict[str, Dict[str, Any]]:
    """Load authoritative runtime_extra_view JSON exported with module packages."""
    selected: Dict[str, Tuple[Tuple[int, ...], int, str, Dict[str, Any]]] = {}
    for metadata_path in sorted(modules_root.rglob("metadata.json")):
        if "target" in metadata_path.parts:
            continue
        try:
            blocks = json.loads(metadata_path.read_text(encoding="utf-8"))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
            print(f"-- WARNING: skip invalid metadata JSON {metadata_path}: {error}", file=sys.stderr)
            continue
        if not isinstance(blocks, list):
            continue
        for block in blocks:
            if not isinstance(block, dict) or str(block.get("tableName") or "").lower() != "runtime_extra_view":
                continue
            rows = block.get("metadata")
            if not isinstance(rows, list):
                continue
            for row in rows:
                if not isinstance(row, dict):
                    continue
                code = str(row.get("CODE") or row.get("code") or row.get("VIEW_CODE") or row.get("viewCode") or "").strip()
                raw_payload = row.get("VIEW_JSON") if "VIEW_JSON" in row else row.get("viewJson")
                if not code or raw_payload in (None, ""):
                    continue
                try:
                    payload = json.loads(raw_payload) if isinstance(raw_payload, str) else copy.deepcopy(raw_payload)
                except json.JSONDecodeError as error:
                    print(
                        f"-- WARNING: skip invalid runtime view JSON {code} from {metadata_path}: {error}",
                        file=sys.stderr,
                    )
                    continue
                if not isinstance(payload, dict):
                    continue
                try:
                    row_version = int(row.get("VERSION") or row.get("version") or 0)
                except (TypeError, ValueError):
                    row_version = 0
                candidate = (module_version_key(metadata_path), row_version, str(metadata_path), payload)
                existing = selected.get(code)
                if existing is None or candidate[:3] > existing[:3]:
                    selected[code] = candidate
    return {code: candidate[3] for code, candidate in selected.items()}


@dataclass(frozen=True)
class PackagedUiTableSpec:
    runtime_table: str
    product_table: str
    columns: Tuple[str, ...]
    boolean_columns: Tuple[str, ...] = ()
    integer_columns: Tuple[str, ...] = ()
    lob_columns: Tuple[str, ...] = ()
    json_columns: Tuple[str, ...] = ()


PACKAGED_UI_TABLE_SPECS: Tuple[PackagedUiTableSpec, ...] = (
    PackagedUiTableSpec(
        runtime_table="runtime_field",
        product_table="ec_field",
        columns=(
            "code", "ec_env", "version", "modify_time", "valid", "entity_code", "module_code",
            "layer_type", "proj_flag", "column_type", "full_property_code", "region_type",
            "advqueryjson_code", "fastqueryjson_code", "datagrid_code", "config", "cell_code", "none",
            "is_hidden", "lay_rec", "show_format", "show_type", "view_code", "property_code",
            "display_name", "name", "field_key",
        ),
        boolean_columns=("valid", "proj_flag", "is_hidden"),
        integer_columns=("version",),
        lob_columns=("config",),
    ),
    PackagedUiTableSpec(
        runtime_table="runtime_data_grid",
        product_table="ec_data_grid",
        columns=(
            "code", "ec_env", "version", "modify_time", "valid", "entity_code", "module_code",
            "data_grid_json", "proj_flag", "operate_name", "permission_code", "is_permission",
            "data_grid_type", "full_config", "data_grid_name", "ex", "orgproperty_code",
            "targetmodel_code", "config", "view_code", "name",
        ),
        boolean_columns=("valid", "proj_flag", "is_permission", "ex"),
        integer_columns=("version", "data_grid_type"),
        lob_columns=("data_grid_json", "full_config", "config"),
        json_columns=("data_grid_json",),
    ),
    PackagedUiTableSpec(
        runtime_table="runtime_button",
        product_table="ec_button",
        columns=(
            "code", "ec_env", "version", "modify_time", "valid", "entity_code", "module_code",
            "button_operation_code", "release_felid", "is_signature_config", "power_type", "role_id",
            "position_id", "signer_id", "signature_type", "signature_enabled", "proj_flag", "is_published",
            "button_align", "permission_code", "config", "script_code", "region_type", "datagrid_code",
            "view_code", "cell_code", "display_name", "operate_url", "is_hide", "is_custom_func",
            "is_callback", "is_permission", "is_use_more", "button_style", "confirm_content", "is_confirm",
            "viewselect_code", "operate_type", "name", "signature_describle",
        ),
        boolean_columns=(
            "valid", "is_signature_config", "signature_enabled", "proj_flag", "is_published", "is_hide",
            "is_custom_func", "is_callback", "is_permission", "is_use_more", "is_confirm",
        ),
        integer_columns=("version",),
        lob_columns=("config",),
    ),
    PackagedUiTableSpec(
        runtime_table="runtime_event",
        product_table="ec_event",
        columns=(
            "code", "ec_env", "version", "modify_time", "valid", "entity_code", "module_code",
            "event_function_es5", "proj_flag", "section_code", "tab_code", "layout_code", "button_code",
            "field_code", "event_function", "name",
        ),
        boolean_columns=("valid", "proj_flag"),
        integer_columns=("version",),
        lob_columns=("event_function_es5", "event_function"),
    ),
    PackagedUiTableSpec(
        runtime_table="runtime_extra_query_json",
        product_table="ec_extra_query_json",
        columns=("code", "ec_env", "version", "proj_flag", "query_config", "view_code"),
        boolean_columns=("proj_flag",),
        integer_columns=("version",),
        lob_columns=("query_config",),
        json_columns=("query_config",),
    ),
    PackagedUiTableSpec(
        runtime_table="runtime_fast_query_json",
        product_table="ec_fast_query_json",
        columns=(
            "code", "ec_env", "version", "targetmodel_code", "proj_flag", "layout_name", "query_config",
            "view_code",
        ),
        boolean_columns=("proj_flag",),
        integer_columns=("version",),
        lob_columns=("query_config",),
        json_columns=("query_config",),
    ),
    PackagedUiTableSpec(
        runtime_table="runtime_adv_query_json",
        product_table="ec_adv_query_json",
        columns=(
            "code", "ec_env", "version", "targetmodel_code", "proj_flag", "name", "layout_name",
            "query_config", "view_code",
        ),
        boolean_columns=("proj_flag",),
        integer_columns=("version",),
        lob_columns=("query_config",),
        json_columns=("query_config",),
    ),
    PackagedUiTableSpec(
        runtime_table="runtime_validate",
        product_table="ec_validate",
        columns=(
            "code", "ec_env", "version", "modify_time", "valid", "entity_code", "module_code",
            "proj_flag", "field_code", "params", "type",
        ),
        boolean_columns=("valid", "proj_flag"),
        integer_columns=("version",),
        lob_columns=("params",),
    ),
)


def load_packaged_ui_rows(modules_root: Path) -> Dict[str, List[Dict[str, Any]]]:
    """Load authoritative executable page metadata from the newest package export."""
    table_names = {spec.runtime_table for spec in PACKAGED_UI_TABLE_SPECS}
    selected: Dict[Tuple[str, str], Tuple[Tuple[int, ...], int, str, Dict[str, Any]]] = {}
    for metadata_path in sorted(modules_root.rglob("metadata.json")):
        if "target" in metadata_path.parts:
            continue
        try:
            blocks = json.loads(metadata_path.read_text(encoding="utf-8"))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
            print(f"-- WARNING: skip invalid metadata JSON {metadata_path}: {error}", file=sys.stderr)
            continue
        if not isinstance(blocks, list):
            continue
        for block in blocks:
            if not isinstance(block, dict):
                continue
            table_name = str(block.get("tableName") or "").lower()
            if table_name not in table_names or not isinstance(block.get("metadata"), list):
                continue
            for row in block["metadata"]:
                if not isinstance(row, dict):
                    continue
                code = str(row.get("CODE") or row.get("code") or "").strip()
                if not code:
                    continue
                try:
                    row_version = int(row.get("VERSION") or row.get("version") or 0)
                except (TypeError, ValueError):
                    row_version = 0
                candidate = (module_version_key(metadata_path), row_version, str(metadata_path), copy.deepcopy(row))
                key = (table_name, code)
                existing = selected.get(key)
                if existing is None or candidate[:3] > existing[:3]:
                    selected[key] = candidate

    result: Dict[str, List[Dict[str, Any]]] = {table_name: [] for table_name in table_names}
    for (table_name, _code), candidate in sorted(selected.items()):
        result[table_name].append(candidate[3])
    return result


def packaged_row_matches_modules(row: Dict[str, Any], module_codes: Sequence[str]) -> bool:
    if not module_codes:
        return False
    direct_module = str(row.get("MODULE_CODE") or row.get("module_code") or "").strip()
    if direct_module in module_codes:
        return True
    identifiers = [
        str(row.get(key) or "").strip()
        for key in ("CODE", "code", "VIEW_CODE", "view_code", "ENTITY_CODE", "entity_code")
    ]
    return any(identifier == module_code or identifier.startswith(module_code + "_")
               for module_code in module_codes for identifier in identifiers if identifier)


def metadata_boolean(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return value != 0
    return str(value or "").strip().lower() in {"1", "true", "yes", "y"}


def normalize_packaged_json_text(value: Any) -> Any:
    if value in (None, ""):
        return value
    try:
        payload = json.loads(value) if isinstance(value, str) else copy.deepcopy(value)
    except json.JSONDecodeError:
        return value
    return json.dumps(
        normalize_packaged_runtime_payload(payload),
        ensure_ascii=False,
        separators=(",", ":"),
    )


def packaged_metadata_value_sql(
    column: str,
    value: Any,
    spec: PackagedUiTableSpec,
    runtime: bool,
) -> str:
    if value is None:
        return "NULL"
    if column in spec.json_columns:
        value = normalize_packaged_json_text(value)
    if column == "modify_time":
        if isinstance(value, (int, float)) or str(value).isdigit():
            return f"to_timestamp({int(value)} / 1000.0)"
        return sql_str(str(value))
    if column in spec.boolean_columns:
        boolean_value = metadata_boolean(value)
        return sql_bool(boolean_value) if runtime else sql_bool_int(boolean_value)
    if column in spec.integer_columns:
        try:
            return str(int(value))
        except (TypeError, ValueError):
            return "NULL"
    literal = sql_str(str(value))
    if runtime and column in spec.lob_columns:
        return f"lo_from_bytea(0, convert_to({literal}, 'UTF8'))"
    return literal


def packaged_ui_row_sql(spec: PackagedUiTableSpec, row: Dict[str, Any], runtime: bool) -> str:
    source = {str(key).lower(): value for key, value in row.items()}
    source.setdefault("ec_env", "product")
    source.setdefault("valid", 1)
    source.setdefault("proj_flag", 0)
    columns = [column for column in spec.columns if column in source]
    table_name = spec.runtime_table if runtime else spec.product_table
    values = [packaged_metadata_value_sql(column, source[column], spec, runtime) for column in columns]
    updates = [column for column in columns if column != "code"]
    return (
        f"INSERT INTO public.{table_name} ({', '.join(columns)})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        + ",\n".join(f"    {column} = EXCLUDED.{column}" for column in updates)
        + ";"
    )


def generate_packaged_ui_runtime_sql(modules_root: Path, module_codes: Sequence[str]) -> str:
    rows_by_table = load_packaged_ui_rows(modules_root)
    lines = [
        "-- Generated by deploy/docker/scripts/generate-business-view-runtime-sql.py --packaged-ui-runtime-only",
        "-- Restores executable field/grid/button/event/query metadata into runtime and product layers.",
        f"-- modules_root: {modules_root}",
        f"-- requested_module_codes: {','.join(module_codes)}",
        "",
    ]
    for spec in PACKAGED_UI_TABLE_SPECS:
        rows = [
            row for row in rows_by_table.get(spec.runtime_table, [])
            if packaged_row_matches_modules(row, module_codes)
        ]
        lines.append(f"-- {spec.runtime_table}: {len(rows)} packaged rows")
        for row in rows:
            lines.append(packaged_ui_row_sql(spec, row, runtime=True))
            lines.append(packaged_ui_row_sql(spec, row, runtime=False))
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def default_modules_root() -> Path:
    script_path = Path(__file__).resolve()
    adp_repo = script_path.parents[3]
    return adp_repo.parent / "mes-modules-source-repo" / "modules"


def config_root(view_element: ET.Element) -> Optional[ET.Element]:
    wrapper = view_element.find("extraView/config")
    if wrapper is None:
        return None
    children = list(wrapper)
    if children:
        return children[0]
    return wrapper


def derive_entity_code(ass_model_code: str) -> str:
    if not ass_model_code:
        return ""
    parts = ass_model_code.split("_")
    if len(parts) <= 1:
        return ass_model_code
    return "_".join(parts[:-1])


def should_replace(existing: ViewDef, candidate: ViewDef) -> bool:
    if candidate.version_key != existing.version_key:
        return candidate.version_key > existing.version_key
    existing_size = len(ET.tostring(existing.config_root, encoding="unicode")) if existing.config_root is not None else 0
    candidate_size = len(ET.tostring(candidate.config_root, encoding="unicode")) if candidate.config_root is not None else 0
    if candidate_size != existing_size:
        return candidate_size > existing_size
    return str(candidate.source) > str(existing.source)


def parse_view(source: Path, view_element: ET.Element) -> Optional[ViewDef]:
    code = direct_text(view_element, "code")
    view_type = direct_text(view_element, "type")
    url = direct_text(view_element, "url")
    if not code or not view_type:
        return None

    ass_model_code = direct_text(view_element.find("assModel"), "code") or direct_text(view_element, "assModelCode")
    entity_code = direct_text(view_element, "entityCode") or derive_entity_code(ass_model_code)
    module_code = direct_text(view_element, "moduleCode")
    if not module_code and code:
        module_code = "_".join(code.split("_")[:2]) if "_" in code else code

    return ViewDef(
        code=code,
        source=source,
        version_key=module_version_key(source),
        ec_env=direct_text(view_element, "ecEnv", "product") or "product",
        name=direct_text(view_element, "name"),
        title=direct_text(view_element, "title") or direct_text(view_element, "displayName") or code,
        display_name=direct_text(view_element, "displayName") or direct_text(view_element, "title") or code,
        view_type=view_type,
        show_type=direct_text(view_element, "showType", "SINGLE") or "SINGLE",
        url=url,
        open_type=direct_text(view_element, "openType"),
        module_code=module_code,
        ass_model_code=ass_model_code,
        entity_code=entity_code,
        has_attachment=bool_text(direct_text(view_element, "hasAttachment")),
        only_for_query=bool_text(direct_text(view_element, "onlyForQuery")),
        main_view=bool_text(direct_text(view_element, "mainView")),
        main_ref=bool_text(direct_text(view_element, "mainRef")),
        mobile=bool_text(direct_text(view_element, "mobile")),
        mobile_enable_flag=bool_text(direct_text(view_element, "mobileEnableFlag")),
        move_flag=bool_text(direct_text(view_element, "moveFlag")),
        config_root=config_root(view_element),
    )


def parse_property(model: ET.Element, property_element: ET.Element, model_code: str) -> Optional[PropertyDef]:
    code = direct_text(property_element, "code")
    name = direct_text(property_element, "name")
    if not code or not name:
        return None

    return PropertyDef(
        code=code,
        ec_env=direct_text(property_element, "ecEnv", "product") or "product",
        name=name,
        display_name=direct_text(property_element, "displayName") or name,
        prop_type=direct_text(property_element, "type"),
        fmt=direct_text(property_element, "format"),
        field_type=direct_text(property_element, "fieldType"),
        column_name=direct_text(property_element, "columnName"),
        entity_code=direct_text(property_element, "entityCode") or derive_entity_code(model_code),
        module_code=direct_text(property_element, "moduleCode") or direct_text(model, "moduleCode"),
        model_code=model_code,
        description=direct_text(property_element, "description"),
        default_value=direct_text(property_element, "defaultValue"),
        associated_property_code=(
            direct_text(property_element.find("associatedProperty"), "code")
            or direct_text(property_element, "associatedProperty")
        ),
        fetch_mode=direct_text(property_element, "fetchMode"),
        sort=int_or_none(direct_text(property_element, "sort")),
        max_length=int_or_none(direct_text(property_element, "maxLength")),
        decimal_num=int_or_none(direct_text(property_element, "decimalNum")),
        associated_type=int_or_none(direct_text(property_element, "associatedType")),
        show_width=int_or_none(direct_text(property_element, "showWidth")),
        max_pic_num=int_or_none(direct_text(property_element, "maxPicNum")),
        only_leaf=bool_text(direct_text(property_element, "onlyLeaf")),
        is_group_object=bool_text(direct_text(property_element, "isGroupObject")),
        proj_custom_in_use=bool_text(direct_text(property_element, "projCustomInUse")),
        proj_flag=bool_text(direct_text(property_element, "projFlag")),
        is_hidden=bool_text(direct_text(property_element, "isHidden")),
        is_engine=bool_text(direct_text(property_element, "isEngine")),
        is_custom=bool_text(direct_text(property_element, "isCustom")),
        is_mne_whole_like_query=bool_text(direct_text(property_element, "isMneWholeLikeQuery")),
        senior_system_code=bool_text(direct_text(property_element, "seniorSystemCode")),
        no_analyzer=bool_text(direct_text(property_element, "noAnalyzer")),
        is_main_associated=bool_text(direct_text(property_element, "isMainAssociated")),
        is_used_for_search=bool_text(direct_text(property_element, "isUsedForSearch")),
        stretch=bool_text(direct_text(property_element, "stretch")),
        is_bussiness_key=bool_text(direct_text(property_element, "isBussinessKey")),
        is_control=bool_text(direct_text(property_element, "isControl")),
        is_used_mne_code=bool_text(direct_text(property_element, "isUsedMneCode")),
        is_sensitive=bool_text(direct_text(property_element, "sensitive")),
        is_main_display=bool_text(direct_text(property_element, "isMainDisplay")),
        is_used_for_list=bool_text(direct_text(property_element, "isUsedForList")),
        is_pk=bool_text(direct_text(property_element, "isPk")),
        is_ignore_audit=bool_text(direct_text(property_element, "isIgnoreAudit")),
        is_inherent=bool_text(direct_text(property_element, "isInherent")),
        is_unique=bool_text(direct_text(property_element, "isUnique")),
        multable=bool_text(direct_text(property_element, "multable")),
        nullable=bool_text(direct_text(property_element, "nullable"), default=True),
        is_index=bool_text(direct_text(property_element, "isIndex")),
        is_support_sup_and_sub=bool_text(direct_text(property_element, "isSupportSupAndSub")),
        is_pic_support_multi_select=bool_text(direct_text(property_element, "isPicSupportMultiSelect")),
        pic_height=direct_text(property_element, "picHeight"),
        pic_width=direct_text(property_element, "picWidth"),
        org_column_name=direct_text(property_element, "orgColumnName"),
        is_tree_system_code=bool_text(direct_text(property_element, "isTreeSystemCode")),
    )


def parse_model(source: Path, model_element: ET.Element) -> Optional[ModelDef]:
    code = direct_text(model_element, "code")
    if not code:
        return None

    properties: List[PropertyDef] = []
    properties_element = model_element.find("properties")
    if properties_element is not None:
        for property_element in properties_element.findall("property"):
            prop = parse_property(model_element, property_element, code)
            if prop is not None:
                properties.append(prop)

    return ModelDef(
        code=code,
        source=source,
        version_key=module_version_key(source),
        ec_env=direct_text(model_element, "ecEnv", "product") or "product",
        name=direct_text(model_element, "name"),
        value_zh_cn=direct_text(model_element, "zhCnValue"),
        entity_class=direct_text(model_element, "entityClass"),
        model_name=direct_text(model_element, "modelName"),
        jpa_name=direct_text(model_element, "jpaName"),
        ec_version=direct_text(model_element, "ecVersion"),
        description=direct_text(model_element, "description"),
        module_code=direct_text(model_element, "moduleCode"),
        table_name=direct_text(model_element, "tableName"),
        extends_model_code=direct_text(model_element, "extendsModelCode"),
        model_type=int_or_none(direct_text(model_element, "type")),
        data_type=int_or_none(direct_text(model_element, "dataType")),
        is_main=bool_text(direct_text(model_element, "isMain")),
        is_error_sql=int_or_none(direct_text(model_element, "isErrorSql")),
        proj_flag=bool_text(direct_text(model_element, "projFlag")),
        is_config_special=bool_text(direct_text(model_element, "isConfigSpecial")),
        special_auth_isandrel=bool_text(direct_text(model_element, "specialAuthIsAndRel")),
        is_mne_code=bool_text(direct_text(model_element, "isMneCode")),
        is_control=bool_text(direct_text(model_element, "isControl")),
        is_cache=bool_text(direct_text(model_element, "isCache")),
        is_extra_col=bool_text(direct_text(model_element, "isExtraCol")),
        enable_data_audit=bool_text(direct_text(model_element, "enableDataAudit")),
        enable_operation_audit=bool_text(direct_text(model_element, "enableOperationAudit")),
        enable_sync=bool_text(direct_text(model_element, "enableSync")),
        inherent_common_flag=bool_text(direct_text(model_element, "inherentCommonFlag")),
        is_extends=bool_text(direct_text(model_element, "isExtends")),
        entity_code=direct_text(model_element, "entityCode") or derive_entity_code(code),
        properties=properties,
    )


def derive_module_code(code: str) -> str:
    pieces = code.split("_")
    if len(pieces) >= 2:
        return "_".join(pieces[:2])
    return code


def parse_entity(source: Path, entity_element: ET.Element) -> Optional[EntityDef]:
    code = direct_text(entity_element, "code")
    if not code:
        return None

    return EntityDef(
        code=code,
        source=source,
        version_key=module_version_key(source),
        ec_env=direct_text(entity_element, "ecEnv", "product") or "product",
        name=direct_text(entity_element, "name"),
        value_zh_cn=direct_text(entity_element, "zhCnValue"),
        entity_name=direct_text(entity_element, "entityName") or code.rsplit("_", 1)[-1],
        workflow_enabled=bool_text(direct_text(entity_element, "workflowEnabled")),
        pay_close_attention=bool_text(direct_text(entity_element, "payCloseAttention")),
        description=direct_text(entity_element, "description"),
        prefix=direct_text(entity_element, "prefix") or code.rsplit("_", 1)[-1],
        group_enabled=bool_text(direct_text(entity_element, "groupEnabled")),
        cross_company_flag=bool_text(direct_text(entity_element, "crossCompanyFlag")),
        is_base=bool_text(direct_text(entity_element, "isBase")),
        enable_audit=bool_text(direct_text(entity_element, "enableAudit")),
        is_inherented_base=bool_text(direct_text(entity_element, "isInherentedBase")),
        mobile=bool_text(direct_text(entity_element, "mobile")),
        enable_acl_restrict=bool_text(direct_text(entity_element, "enableAclRestrict")),
        enable_rest=bool_text(direct_text(entity_element, "enableRest")),
        enable_ws=bool_text(direct_text(entity_element, "enableWs")),
        entity_type=direct_text(entity_element, "entityType"),
        module_code=direct_text(entity_element, "moduleCode") or derive_module_code(code),
        enable_fields_permission_conf=bool_text(direct_text(entity_element, "enableFieldsPermissionConf")),
        proj_flag=bool_text(direct_text(entity_element, "projFlag")),
        is_control=bool_text(direct_text(entity_element, "isControl")),
        inherent_common_flag=bool_text(direct_text(entity_element, "inherentCommonFlag")),
        list_view_data_type=int_or_none(direct_text(entity_element, "listViewDataType")) or 0,
        reff_view_data_type=int_or_none(direct_text(entity_element, "reffViewDataType")) or 0,
    )


def parse_module(source: Path, root: ET.Element) -> Optional[ModuleDef]:
    code = direct_text(root, "code")
    if not code:
        return None
    return ModuleDef(
        code=code,
        source=source,
        version_key=module_version_key(source),
        ec_env=direct_text(root, "ecEnv", "product") or "product",
        name=direct_text(root, "name"),
        value_zh_cn=direct_text(root, "zhCnValue"),
        acronym=direct_text(root, "acronym"),
        category=direct_text(root, "category"),
        artifact=direct_text(root, "artifact"),
        project_version=direct_text(root, "projectVersion"),
        initial_version=direct_text(root, "orgVersion"),
        description=direct_text(root, "description"),
        module_type=direct_text(root, "type", "Mis") or "Mis",
        deploy_order=direct_text(root, "deployOrder"),
        is_proto=bool_text(direct_text(root, "isProto")),
        main_module=bool_text(direct_text(root, "mainModule")),
        is_hide=bool_text(direct_text(root, "isHide")),
        is_read_only=bool_text(direct_text(root, "isReadOnly")),
        proj_flag=bool_text(direct_text(root, "projFlag")),
        is_new_generate=bool_text(direct_text(root, "isNewGenerate")),
        is_inherented_base=bool_text(direct_text(root, "isInherentedBase")),
        is_cluster=int_or_none(direct_text(root, "isCluster")) or 0,
    )


def parse_customer_condition(source: Path, condition_element: ET.Element) -> Optional[CustomerConditionDef]:
    code = direct_text(condition_element, "code")
    view_code = direct_text(condition_element.find("view"), "code")
    datagrid_code = direct_text(condition_element.find("dataGrid"), "code")
    dataclassific_code = direct_text(condition_element.find("dataClassific"), "code")
    if not code:
        code = view_code or datagrid_code or dataclassific_code
    if not code:
        return None
    return CustomerConditionDef(
        code=code,
        source=source,
        ec_env=direct_text(condition_element, "ecEnv", "product") or "product",
        condition_sql=direct_text(condition_element, "sql"),
        json_condition=direct_text(condition_element, "jsonCondition"),
        dataclassific_code=dataclassific_code,
        datagrid_code=datagrid_code,
        view_code=view_code,
        module_code=direct_text(condition_element, "moduleCode"),
        entity_code=direct_text(condition_element, "entityCode"),
        proj_flag=bool_text(direct_text(condition_element, "projFlag")),
    )


def infer_sql_data_grid_code(code: str, view_code: str, sql_type: Optional[int]) -> str:
    if not code or not view_code or sql_type is None:
        return ""
    prefix = f"{view_code}_"
    suffix = f"_{sql_type}"
    if not code.startswith(prefix) or not code.endswith(suffix):
        return ""
    data_grid_suffix = code[len(prefix) : -len(suffix)]
    if not re.fullmatch(r"dg\d+", data_grid_suffix):
        return ""
    return f"{view_code}{data_grid_suffix}"


def parse_sql_def(source: Path, sql_element: ET.Element) -> Optional[SqlDef]:
    query_sql = direct_text(sql_element, "sql")
    sql_type = int_or_none(direct_text(sql_element, "type"))
    view_code = direct_text(sql_element, "viewCode")
    data_grid_code = direct_text(sql_element, "dataGridCode")
    code = direct_text(sql_element, "code")
    if not code:
        target_code = view_code or data_grid_code
        code = f"{target_code}_{sql_type}" if target_code and sql_type is not None else ""
    if not code or sql_type is None:
        return None
    if not data_grid_code:
        data_grid_code = infer_sql_data_grid_code(code, view_code, sql_type)
    return SqlDef(
        code=code,
        source=source,
        version_key=module_version_key(source),
        ec_env=direct_text(sql_element, "ecEnv", "product") or "product",
        data_grid_code=data_grid_code,
        view_code=view_code,
        sql_type=sql_type,
        query_sql=query_sql,
        proj_flag=bool_text(direct_text(sql_element, "projFlag")),
    )


def synthetic_customer_condition(view: ViewDef) -> CustomerConditionDef:
    return CustomerConditionDef(
        code=view.code,
        source=None,
        ec_env=view.ec_env,
        condition_sql="",
        json_condition="",
        dataclassific_code="",
        datagrid_code="",
        view_code=view.code,
        module_code=view.module_code,
        entity_code=view.entity_code,
        proj_flag=False,
    )


def load_views(modules_root: Path) -> Dict[str, ViewDef]:
    views: Dict[str, ViewDef] = {}
    for module_xml in sorted(modules_root.rglob("module.xml")):
        if "target" in module_xml.parts:
            continue
        try:
            root = ET.parse(module_xml).getroot()
        except ET.ParseError as error:
            print(f"-- WARNING: skip invalid XML {module_xml}: {error}", file=sys.stderr)
            continue
        for view_element in root.iter("view"):
            view = parse_view(module_xml, view_element)
            if view is None:
                continue
            existing = views.get(view.code)
            if existing is None or should_replace(existing, view):
                views[view.code] = view
    return views


def load_customer_conditions(modules_root: Path) -> Dict[str, CustomerConditionDef]:
    conditions: Dict[str, CustomerConditionDef] = {}
    for module_xml in sorted(modules_root.rglob("module.xml")):
        if "target" in module_xml.parts:
            continue
        try:
            root = ET.parse(module_xml).getroot()
        except ET.ParseError as error:
            print(f"-- WARNING: skip invalid XML {module_xml}: {error}", file=sys.stderr)
            continue
        for condition_element in root.findall("./customerConditions/customerCondition"):
            condition = parse_customer_condition(module_xml, condition_element)
            if condition is not None:
                conditions[condition.code] = condition
    return conditions


def load_sql_defs(modules_root: Path) -> Dict[str, SqlDef]:
    sql_defs: Dict[str, SqlDef] = {}
    for module_xml in sorted(modules_root.rglob("module.xml")):
        if "target" in module_xml.parts:
            continue
        try:
            root = ET.parse(module_xml).getroot()
        except ET.ParseError as error:
            print(f"-- WARNING: skip invalid XML {module_xml}: {error}", file=sys.stderr)
            continue
        for sql_element in root.findall(".//sqls/sql"):
            sql_def = parse_sql_def(module_xml, sql_element)
            if sql_def is None:
                continue
            existing = sql_defs.get(sql_def.code)
            if existing is None or sql_def.version_key > existing.version_key:
                sql_defs[sql_def.code] = sql_def
    return sql_defs


def load_modules(modules_root: Path) -> Dict[str, ModuleDef]:
    modules: Dict[str, ModuleDef] = {}
    for module_xml in sorted(modules_root.rglob("module.xml")):
        if "target" in module_xml.parts:
            continue
        try:
            root = ET.parse(module_xml).getroot()
        except ET.ParseError as error:
            print(f"-- WARNING: skip invalid XML {module_xml}: {error}", file=sys.stderr)
            continue
        module = parse_module(module_xml, root)
        if module is None:
            continue
        existing = modules.get(module.code)
        if existing is None or module.version_key > existing.version_key:
            modules[module.code] = module
    return modules


def load_entities(modules_root: Path) -> Dict[str, EntityDef]:
    entities: Dict[str, EntityDef] = {}
    for module_xml in sorted(modules_root.rglob("module.xml")):
        if "target" in module_xml.parts:
            continue
        try:
            root = ET.parse(module_xml).getroot()
        except ET.ParseError as error:
            print(f"-- WARNING: skip invalid XML {module_xml}: {error}", file=sys.stderr)
            continue
        for entity_element in root.iter("entity"):
            entity = parse_entity(module_xml, entity_element)
            if entity is None:
                continue
            existing = entities.get(entity.code)
            if existing is None or entity.version_key > existing.version_key:
                entities[entity.code] = entity
    return entities


def load_models(modules_root: Path) -> Dict[str, ModelDef]:
    models: Dict[str, ModelDef] = {}
    for module_xml in sorted(modules_root.rglob("module.xml")):
        if "target" in module_xml.parts:
            continue
        try:
            root = ET.parse(module_xml).getroot()
        except ET.ParseError as error:
            print(f"-- WARNING: skip invalid XML {module_xml}: {error}", file=sys.stderr)
            continue
        for model_element in root.iter("model"):
            model = parse_model(module_xml, model_element)
            if model is None:
                continue
            existing = models.get(model.code)
            if existing is None or model.version_key > existing.version_key:
                models[model.code] = model
    return models


def layout_element(view: ViewDef) -> Optional[ET.Element]:
    if view.config_root is None:
        return None
    return view.config_root.find("layout")


def extract_layout2_dependencies(view: ViewDef) -> List[str]:
    layout = layout_element(view)
    if layout is None:
        return []
    dependencies: List[str] = []
    west = layout.find("west")
    center = layout.find("center")
    for parent, tag in ((west, "treeView"), (center, "vcode"), (center, "treeView")):
        code = direct_text(parent, tag)
        if code and code not in dependencies:
            dependencies.append(code)
    return dependencies


def resolve_required_views(views: Dict[str, ViewDef], targets: Sequence[str]) -> List[ViewDef]:
    ordered: List[str] = []
    seen = set()

    def visit(code: str) -> None:
        if code in seen:
            return
        seen.add(code)
        view = views.get(code)
        if view is None:
            print(f"-- WARNING: missing view definition for {code}", file=sys.stderr)
            return
        if view.show_type == "LAYOUT2":
            for dependency in extract_layout2_dependencies(view):
                visit(dependency)
        ordered.append(code)

    for target in targets:
        visit(target)
    return [views[code] for code in ordered if code in views]


def iter_region_items(view: ViewDef, region_type: str) -> Iterable[ET.Element]:
    layout = layout_element(view)
    if layout is None:
        return []
    matches: List[ET.Element] = []
    for item in layout.iter("list-item"):
        if direct_text(item, "regionType") == region_type:
            matches.append(item)
    return matches


def extract_fill(item: ET.Element) -> Optional[Dict[str, str]]:
    fill_element = item.find("fill")
    if fill_element is None:
        return None
    fill: Dict[str, str] = {}
    for source, target in (("fillName", "fillName"), ("fillType", "fillType"), ("fillContent", "fillContent")):
        value = direct_text(fill_element, source)
        if value:
            fill[target] = value
    return fill or None


def extract_fields(view: ViewDef) -> List[FieldDef]:
    fields: List[FieldDef] = []
    seen: Dict[str, int] = {}
    for region_type in ("LISTPT", "DATAGRID", "EDIT"):
        for item in iter_region_items(view, region_type):
            element = item.find("element")
            field_source = element if element is not None else item
            key = direct_text(field_source, "key") or direct_text(field_source, "name") or direct_text(item, "name") or direct_text(item, "layRec")
            property_code = direct_text(field_source, "propertyCode") or direct_text(item, "propertyCode")
            if not key and not property_code:
                continue
            key = key or property_code.rsplit("_", 1)[-1] or "id"
            none_marker = direct_text(field_source, "none").lower() or direct_text(item, "none").lower()
            field = FieldDef(
                key=key,
                namekey=direct_text(field_source, "namekey") or direct_text(field_source, "displayName") or key,
                show_type=direct_text(field_source, "showType", "TEXTFIELD") or "TEXTFIELD",
                show_format=direct_text(field_source, "showFormat", "TEXT") or "TEXT",
                width=int_text(direct_text(field_source, "width") or direct_text(item, "width"), 120),
                hidden=bool_text(direct_text(field_source, "isHidden") or direct_text(item, "isHidden")) or none_marker == "hide",
                column_type=direct_text(field_source, "columnType", "TEXT") or "TEXT",
                model_code=direct_text(field_source, "modelCode") or direct_text(field_source, "modelcode") or direct_text(item, "modelCode") or direct_text(item, "modelcode"),
                fill=extract_fill(field_source) or extract_fill(item),
                custom_section=bool_text(direct_text(field_source, "customSection") or direct_text(item, "customSection")),
                custom_model_code=direct_text(field_source, "customModelCode") or direct_text(item, "customModelCode"),
                property_lay_rec=direct_text(field_source, "propertyLayRec") or direct_text(item, "propertyLayRec"),
            )
            if key in seen:
                existing_index = seen[key]
                existing = fields[existing_index]
                if existing.show_type == "LABEL" and field.show_type != "LABEL":
                    fields[existing_index] = field
                continue
            seen[key] = len(fields)
            fields.append(field)
            if len(fields) >= 80:
                break
        if fields:
            return fields

    fallback_key = "name"
    return [
        FieldDef(
            key=fallback_key,
            namekey=view.display_name or view.title or view.code,
            show_type="TEXTFIELD",
            show_format="TEXT",
            width=160,
            hidden=False,
            column_type="TEXT",
            model_code=view.ass_model_code,
            fill=None,
        )
    ]


def model_code_for(view: ViewDef, fields: Sequence[FieldDef]) -> str:
    if view.ass_model_code:
        return view.ass_model_code
    for field in fields:
        if field.model_code:
            return field.model_code
    return view.entity_code or view.code


def field_json(field: FieldDef) -> Dict[str, object]:
    show_type = "TEXTFIELD" if field.show_type == "LABEL" else field.show_type
    result: Dict[str, object] = {
        "key": field.key,
        "namekey": field.namekey,
        "showType": show_type,
        "showFormat": field.show_format,
        "width": field.width,
        "isHidden": field.hidden,
        "columnType": field.column_type,
    }
    if field.fill:
        result["fill"] = field.fill
    if field.custom_section:
        result["customSection"] = True
        result["customModelCode"] = field.custom_model_code
        result["propertyLayRec"] = field.property_lay_rec
    return result


def extract_list_runtime_properties(view: ViewDef) -> Dict[str, object]:
    list_property: Optional[ET.Element] = None
    for item in iter_region_items(view, "LISTPT"):
        candidate = item.find("listProperty")
        if candidate is not None:
            list_property = candidate
            break
    if list_property is None:
        return {}

    result: Dict[str, object] = {}
    for key in (
        "isdbcustom",
        "isFirstLoad",
        "isCheckBox",
        "selectFirstRow",
        "isTreeView",
        "treePaging",
        "isExportExcel",
    ):
        value = direct_text(list_property, key)
        if value:
            result[key] = bool_text(value)
    for key in (
        "dbcustomtextarea",
        "dbcustomtextarea_es5",
        "ptPageInit",
        "ptPageInit_es5",
        "renderOver",
        "renderOver_es5",
    ):
        value = direct_text(list_property, key)
        if value:
            result[key] = value
    return result


def apply_datagrid_runtime_overrides(view: ViewDef, payload: Any) -> None:
    overrides = DATAGRID_RUNTIME_OVERRIDES.get(view.code)
    if not overrides:
        return
    expected_grid_code = first_datagrid_code(view) or view.code
    candidates: List[Dict[str, Any]] = []

    def visit(node: Any) -> None:
        if isinstance(node, list):
            for child in node:
                visit(child)
            return
        if not isinstance(node, dict):
            return
        if node.get("type") == "layoutDatagrid":
            candidates.append(node)
        for child in node.values():
            visit(child)

    visit(payload)
    target = next(
        (
            candidate
            for candidate in candidates
            if candidate.get("DataGridCode") == expected_grid_code
            or candidate.get("code") == expected_grid_code
        ),
        candidates[0] if candidates else None,
    )
    if target is not None:
        target.update(copy.deepcopy(overrides))


def apply_view_runtime_overrides(view: ViewDef, payload: Any) -> None:
    namekey_fallbacks = VIEW_NAMEKEY_FALLBACKS.get(view.code, {})
    if namekey_fallbacks:
        def replace_missing_namekey(node: Any) -> None:
            if isinstance(node, list):
                for child in node:
                    replace_missing_namekey(child)
                return
            if not isinstance(node, dict):
                return
            namekey = node.get("namekey")
            if namekey in namekey_fallbacks:
                node["namekey"] = namekey_fallbacks[namekey]
            for child in node.values():
                replace_missing_namekey(child)

        replace_missing_namekey(payload)

    onload_append = VIEW_ONLOAD_APPENDS.get(view.code)
    if not onload_append or not isinstance(payload, dict):
        return
    for event in payload.get("events") or []:
        if not isinstance(event, dict) or event.get("name") not in {"onload", "onload_es5"}:
            continue
        current = str(event.get("function") or "").rstrip()
        if (
            view.code == "WOM_1.0.0_produceTask_makeTaskEdit"
            and current.lstrip().startswith(
                (
                    "var formData = ReactAPI.getFormData();",
                    "let formData = ReactAPI.getFormData();",
                )
            )
            and MAKE_TASK_VENDOR_ONLOAD_TAIL_MARKER in current
        ):
            # The recovered package hides all business tabs before asynchronous
            # form data is available. Keep the remaining vendor initialization,
            # while the guarded poll above owns tab visibility.
            current = current[current.index(MAKE_TASK_VENDOR_ONLOAD_TAIL_MARKER) :]
        if onload_append not in current:
            event["function"] = f"{onload_append}\n{current}" if current else onload_append


def data_grid_json(
    view: ViewDef, parent_code: Optional[str] = None, views: Optional[Dict[str, ViewDef]] = None
) -> Dict[str, object]:
    # Project custom-property columns are placeholders whose concrete fields come
    # from base_cp_model_mapping/base_cp_view_mapping. Recovered base packages do
    # not carry those tenant mappings; emitting the placeholders makes the legacy
    # layout service dereference a missing mapping and fail the whole page. Keep
    # the real module fields and let tenant-specific columns be restored only when
    # their mapping rows are available.
    fields = [field for field in extract_fields(view) if not field.custom_section]
    if not fields:
        fields = [
            FieldDef(
                key="name",
                namekey=view.display_name or view.title or view.code,
                show_type="TEXTFIELD",
                show_format="TEXT",
                width=160,
                hidden=False,
                column_type="TEXT",
                model_code=view.ass_model_code,
                fill=None,
            )
        ]
    visible_fields = [field for field in fields if not field.hidden]
    main_display = "name"
    if not any(field.key == "name" for field in visible_fields):
        main_display = visible_fields[0].key if visible_fields else fields[0].key
    datagrid_code = first_datagrid_code(view) or view.code
    buttons = extract_buttons(view, views)
    if parent_code:
        operation_prefix = parent_code + "_"
        for button in buttons:
            operation_code = str(button.get("buttonoperationcode") or "").strip()
            if not operation_code or not bool(button.get("ispermission")):
                continue
            permission_operation_code = operation_code
            if not operation_code.startswith(operation_prefix):
                permission_operation_code = operation_prefix + operation_code
            button["pc"] = button_power_code(permission_operation_code)
    grid: Dict[str, object] = {
        "type": "layoutDatagrid",
        "layoutmethod": "container",
        "ratio_h": 100,
        "DataGridCode": datagrid_code,
        "modelCode": model_code_for(view, fields),
        "hasFastQuery": True,
        "mainDisplayName": main_display,
        "idPrefix": "compat_" + (parent_code or view.code),
        "listPT": False,
        "buttons": buttons,
        "fields": [field_json(field) for field in fields],
    }
    grid.update(extract_list_runtime_properties(view))
    if view.url:
        prefix = view.url.rsplit("/", 1)[0]
        grid["downloadXls"] = prefix + "/downloadXls"
        grid["importMainXls"] = prefix + "/importMainXls"
    return grid


def first_datagrid_code(view: ViewDef) -> str:
    for item in iter_region_items(view, "DATAGRID"):
        code = direct_text(item, "datagridCode") or direct_text(item, "dataGridCode")
        if code:
            return code
    return ""


def runtime_page_type(view: ViewDef) -> str:
    if view.view_type in {"EDIT", "VIEW"} and view.show_type == "SINGLE":
        return view.view_type
    return "LIST"


def section_cells(section: ET.Element) -> List[ET.Element]:
    return list_items(section.find("cells"))


def section_region_type(section: ET.Element) -> str:
    return direct_text(section, "regionType").upper()


def action_cell_json(cell: ET.Element, views: Optional[Dict[str, ViewDef]] = None) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    for child in list(cell):
        if child.tag == "element":
            result["element"] = xml_children_dict(child)
        elif child.tag == "cells":
            continue
        else:
            result[child.tag] = xml_value(child)

    if "element" not in result:
        result["element"] = xml_children_dict(cell)
    for list_key in ("validate",):
        if list_key in result:
            result[list_key] = unwrap_xml_list(result[list_key])
    result["colspan"] = int_text(str(result.get("colspan") or "1"), 1)
    result["rowspan"] = int_text(str(result.get("rowspan") or "1"), 1)
    result["firstTd"] = int_text(str(result.get("firstTd") or "0"), 0)

    element = result.get("element")
    if not isinstance(element, dict):
        element = {}
        result["element"] = element
    if not element.get("key"):
        cell_code = str(result.get("cellCode") or "blank").replace(".", "_")
        element.update(
            {
                "key": f"compat.blank.{cell_code}",
                "namekey": "",
                "name": "",
                "showType": "LABEL",
                "showFormat": "TEXT",
                "columnType": "TEXT",
                "nullable": "true",
            }
        )
        result.setdefault("name", "")
    normalize_xml_scalar_types(result)
    element = result.get("element") if isinstance(result.get("element"), dict) else {}
    if str(element.get("columnType", "")).upper() == "SYSTEMCODE" and not isinstance(element.get("fill"), dict):
        element["fill"] = {}
    expand_element_view_references(element, views or {})
    return result


def action_sections(container: ET.Element) -> List[ET.Element]:
    sections: List[ET.Element] = []
    for item in container.iter("list-item"):
        region_type = section_region_type(item)
        if region_type in {"EDIT", "DATAGRID"} and item.find("cells") is not None:
            sections.append(item)
    return sections


def action_tab_sources(layout: ET.Element) -> List[ET.Element]:
    tab_items = list_items(layout.find("tabs"))
    return tab_items or [layout]


def data_grid_name(view: ViewDef, datagrid_code: str) -> str:
    if datagrid_code.startswith(view.code):
        suffix = datagrid_code[len(view.code) :]
        if suffix:
            return suffix
    return datagrid_code


def action_data_grid_json(
    view: ViewDef, section: ET.Element, marker: Dict[str, Any], views: Optional[Dict[str, ViewDef]] = None
) -> Dict[str, Any]:
    element = marker.get("element") if isinstance(marker.get("element"), dict) else {}
    marker_code = str(element.get("code") or direct_text(section, "datagridCode") or direct_text(section, "dataGridCode"))
    datagrid_code = direct_text(section, "datagridCode") or direct_text(section, "dataGridCode") or marker_code
    section_config = xml_children_dict(section.find("pageConfig"))
    is_readonly = direct_text(section, "isreadonly") or direct_text(section, "isreadonlyBak")
    return {
        "DataGridCode": datagrid_code,
        "code": datagrid_code,
        "name": data_grid_name(view, datagrid_code),
        "datagridName": direct_text(section, "datagridName") or datagrid_code,
        "isEditable": view.view_type != "VIEW" and not bool_text(is_readonly),
        "ptRealTimeLoad": bool_text(direct_text(section, "ptRealTimeLoad")),
        "ptPageInit": section_config.get("ptPageInit", ""),
        "renderOver": section_config.get("renderOver", ""),
        "config": {
            "code": marker_code,
            "DataGridCode": datagrid_code,
        },
        "buttons": extract_buttons(view, views),
        "elements": [],
    }


def action_section_colwidth(section: ET.Element) -> str:
    colwidth = first_text(section, "colwidth", "colWidth", "columnWidth", "columnwidth")
    if colwidth:
        return colwidth
    col_num = int_text(first_text(section, "colNum", "columnNum"), 6)
    col_num = max(1, min(col_num, 12))
    each = round(100 / col_num, 4)
    return ",".join(str(each).rstrip("0").rstrip(".") for _ in range(col_num))


def action_section_json(section: ET.Element, cells: List[Dict[str, Any]], index: int) -> Dict[str, Any]:
    section_code = first_text(section, "sectionCode", "layoutCode", "code", default=f"compat_section_{index + 1}")
    section_name = first_text(section, "sectionName", "namekey", "name", default=section_code)
    return {
        "type": "layoutSection",
        "layoutmethod": "container",
        "layoutContent": "section",
        "sectionCode": section_code,
        "namekey": section_name,
        "cssstyle": direct_text(section, "cssstyle"),
        "colwidth": action_section_colwidth(section),
        "cells": cells,
    }


def action_datagrid_cell_json(
    view: ViewDef, section: ET.Element, marker: Dict[str, Any], views: Optional[Dict[str, ViewDef]] = None
) -> Dict[str, Any]:
    grid = action_data_grid_json(view, section, marker, views)
    data_grid_code = (
        grid.get("DataGridCode")
        or grid.get("config", {}).get("DataGridCode")
        or grid.get("datagridName")
        or grid.get("name")
    )
    grid.update(
        {
            "type": "layoutDatagrid",
            "layoutmethod": "container",
            "dataGridName": grid.get("datagridName") or data_grid_code,
            "DataGridCode": data_grid_code,
            "code": data_grid_code,
            "fields": grid.get("elements", []),
            "modelCode": model_code_for(view, extract_fields(view)),
            "idPrefix": "compat_" + view.code,
            "listPT": False,
            "isCheckBox": False,
            "isFirstLoad": True,
        }
    )
    return {
        "type": "layout",
        "layoutmethod": "container",
        "colspan": 1,
        "rowspan": 1,
        "firstTd": 1,
        "components": [grid],
    }


def action_tab_layout_json(
    view: ViewDef, tab_source: ET.Element, index: int, views: Optional[Dict[str, ViewDef]] = None
) -> Optional[Dict[str, Any]]:
    components: List[Dict[str, Any]] = []

    for section_index, section in enumerate(action_sections(tab_source)):
        region_type = section_region_type(section)
        cells = section_cells(section)
        if region_type == "EDIT":
            field_cells = [action_cell_json(cell, views) for cell in cells]
            if field_cells:
                components.append(action_section_json(section, field_cells, section_index))
            continue

        if region_type == "DATAGRID" and cells:
            marker = action_cell_json(cells[0], views)
            components.append(
                action_section_json(
                    section,
                    [action_datagrid_cell_json(view, section, marker, views)],
                    section_index,
                )
            )

    if not components:
        return None

    tab_code = first_text(tab_source, "tabCode", default=f"compat_{view.code}_tab_{index + 1}")
    tab_name = first_text(tab_source, "tabName", "name", "namekey", default=view.display_name or view.title or tab_code)
    return {
        "type": "layout",
        "layoutmethod": "column",
        "tabCode": tab_code,
        "namekey": tab_name,
        "components": [
            {
                "type": "layout",
                "layoutmethod": "container",
                "layoutContent": "section",
                "components": components,
            }
        ],
    }


def action_view_json(view: ViewDef, views: Optional[Dict[str, ViewDef]] = None) -> Dict[str, Any]:
    layout = layout_element(view)
    tab_layouts: List[Dict[str, Any]] = []
    if layout is not None:
        for index, tab_source in enumerate(action_tab_sources(layout)):
            tab_layout = action_tab_layout_json(view, tab_source, index, views)
            if tab_layout:
                tab_layouts.append(tab_layout)

    if not tab_layouts:
        tab_layouts = [
            {
                "type": "layout",
                "layoutmethod": "column",
                "tabCode": f"compat_{view.code}_tab_1",
                "namekey": view.display_name or view.title or view.code,
                "components": [
                    {
                        "type": "layout",
                        "layoutmethod": "container",
                        "layoutContent": "section",
                        "components": [
                            {
                                "type": "layoutSection",
                                "layoutmethod": "container",
                                "layoutContent": "section",
                                "sectionCode": "compat_empty_section",
                                "namekey": "",
                                "colwidth": "16.6667,16.6667,16.6667,16.6667,16.6667,16.6667",
                                "cells": [],
                            }
                        ],
                    }
                ],
            }
        ]

    body_layout: Dict[str, Any]
    if len(tab_layouts) > 1:
        body_layout = {
            "type": "layout",
            "layoutmethod": "tab",
            "components": tab_layouts,
        }
    else:
        body_layout = tab_layouts[0]

    return {
        "pageType": runtime_page_type(view),
        "title": view.title,
        "url": view.url,
        "isMain": True,
        "hasAttachment": view.has_attachment,
        "onlyForQuery": view.only_for_query,
        "components": [
            {
                "type": "layout",
                "layoutmethod": "column",
                "events": [],
                "components": [body_layout],
            }
        ],
        "isFileView": True,
        "moveFlag": view.move_flag,
    }


def list_json(
    view: ViewDef, parent_code: Optional[str] = None, views: Optional[Dict[str, ViewDef]] = None
) -> Dict[str, object]:
    return {
        "pageType": runtime_page_type(view),
        "title": view.title,
        "url": view.url,
        "isMain": True,
        "hasAttachment": view.has_attachment,
        "onlyForQuery": view.only_for_query,
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
                    data_grid_json(view, parent_code, views),
                ],
            }
        ],
        "isFileView": view.code != "RM_1.0.0_formula_batchFormulaList",
        "moveFlag": view.move_flag,
    }


def tree_json(
    view: ViewDef,
    parent_code: Optional[str] = None,
    width: Optional[int] = None,
    views: Optional[Dict[str, ViewDef]] = None,
) -> Dict[str, object]:
    layout = layout_element(view)
    layout_code = direct_text(layout, "layoutCode") or "compat_" + view.code
    page_config = layout.find("pageConfig") if layout is not None else None
    result: Dict[str, object] = {
        "type": "layoutTree",
        "layoutmethod": "container",
        "layoutCode": layout_code,
        "title": view.title,
        "url": view.url,
        "treeViewCode": view.code,
        "hasAttachment": view.has_attachment,
        "buttons": extract_buttons(view, views),
        "rootName": direct_text(page_config, "rootName") or view.title,
        "treeModelCode": direct_text(page_config, "treeModelCode") or "name",
        "dragOpen": bool_text(direct_text(page_config, "dragOpen")),
        "canMemory": bool_text(direct_text(page_config, "canMemory")),
        "canDrag": bool_text(direct_text(page_config, "canDrag")),
        "events": [],
    }
    if width:
        result["fix_w"] = width
    if parent_code:
        result["parentViewCode"] = parent_code
    return result


def layout2_json(view: ViewDef, views: Dict[str, ViewDef]) -> Dict[str, object]:
    layout = layout_element(view)
    west = layout.find("west") if layout is not None else None
    center = layout.find("center") if layout is not None else None
    tree_code = direct_text(west, "treeView") or direct_text(center, "treeView")
    list_code = direct_text(center, "vcode")
    width = int_text(direct_text(west, "width"), 200)

    components: List[Dict[str, object]] = []
    tree_view = views.get(tree_code)
    if tree_view is not None:
        components.append(tree_json(tree_view, view.code, width, views))
    list_view = views.get(list_code)
    components.append(list_json(list_view or view, view.code, views))

    return {
        "title": view.title,
        "pageType": "LIST",
        "url": view.url,
        "hasAttachment": view.has_attachment,
        "onlyForQuery": view.only_for_query,
        "layoutType": "classic",
        "components": [
            {
                "type": "layout",
                "layoutmethod": "row",
                "components": components,
            }
        ],
        "moveFlag": view.move_flag,
    }


def view_json(
    view: ViewDef,
    views: Dict[str, ViewDef],
    packaged_view_json: Optional[Dict[str, Dict[str, Any]]] = None,
) -> str:
    packaged_payload = (packaged_view_json or {}).get(view.code)
    if packaged_payload is not None:
        payload = normalize_packaged_runtime_payload(packaged_payload)
    elif view.show_type == "LAYOUT2":
        payload = layout2_json(view, views)
    elif view.view_type == "TREE":
        payload = {
            "type": "layout",
            "layoutmethod": "column",
            "title": view.title,
            "components": [tree_json(view, views=views)],
        }
    elif runtime_page_type(view) in {"EDIT", "VIEW"}:
        payload = action_view_json(view, views)
    else:
        payload = list_json(view, views=views)
    apply_datagrid_runtime_overrides(view, payload)
    apply_view_runtime_overrides(view, payload)
    sanitize_runtime_strings(payload)
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":"))


def runtime_view_type(view: ViewDef) -> str:
    if view.view_type == "EXTRA" and view.url:
        return "LIST"
    return view.view_type


def resolve_required_models(models: Dict[str, ModelDef], views: Sequence[ViewDef]) -> List[ModelDef]:
    ordered_codes: List[str] = []
    seen = set()
    for view in views:
        candidate_codes = [view.ass_model_code]
        candidate_codes.extend(field.model_code for field in extract_fields(view) if field.model_code)
        for code in candidate_codes:
            if not code or code in seen:
                continue
            seen.add(code)
            if code in models:
                ordered_codes.append(code)
            else:
                print(f"-- WARNING: missing model definition for {code}", file=sys.stderr)
    return [models[code] for code in ordered_codes]


def resolve_required_entities(
    entities: Dict[str, EntityDef], views: Sequence[ViewDef], models: Sequence[ModelDef]
) -> List[EntityDef]:
    ordered_codes: List[str] = []
    seen = set()
    for code in [view.entity_code for view in views] + [model.entity_code for model in models]:
        if not code or code in seen:
            continue
        seen.add(code)
        if code in entities:
            ordered_codes.append(code)
        else:
            print(f"-- WARNING: missing entity definition for {code}", file=sys.stderr)
    return [entities[code] for code in ordered_codes]


def resolve_required_modules(
    modules: Dict[str, ModuleDef],
    views: Sequence[ViewDef],
    models: Sequence[ModelDef],
    entities: Sequence[EntityDef],
) -> List[ModuleDef]:
    ordered_codes: List[str] = []
    seen = set()
    candidates = [view.module_code for view in views]
    candidates.extend(model.module_code for model in models)
    candidates.extend(entity.module_code for entity in entities)
    for code in candidates:
        if not code or code in seen:
            continue
        seen.add(code)
        if code in modules:
            ordered_codes.append(code)
        else:
            print(f"-- WARNING: missing module definition for {code}", file=sys.stderr)
    return [modules[code] for code in ordered_codes]


def resolve_required_customer_conditions(
    conditions: Dict[str, CustomerConditionDef], views: Sequence[ViewDef]
) -> List[CustomerConditionDef]:
    by_view = {condition.view_code: condition for condition in conditions.values() if condition.view_code}
    required: List[CustomerConditionDef] = []
    seen = set()
    for view in views:
        condition = by_view.get(view.code) or conditions.get(view.code) or synthetic_customer_condition(view)
        if condition.code in seen:
            continue
        seen.add(condition.code)
        required.append(condition)
    return required


def resolve_required_sqls(sql_defs: Dict[str, SqlDef], views: Sequence[ViewDef]) -> List[SqlDef]:
    required_view_codes = {view.code for view in views}
    ordered: List[SqlDef] = []
    seen = set()
    for view in views:
        for sql_def in sorted(
            sql_defs.values(),
            key=lambda item: (item.view_code, item.data_grid_code, item.sql_type or 0, item.code),
        ):
            if sql_def.code in seen:
                continue
            if sql_def.view_code != view.code and sql_def.data_grid_code not in required_view_codes:
                continue
            seen.add(sql_def.code)
            ordered.append(sql_def)
    return ordered


def extract_sql_table_refs(sql_defs: Sequence[SqlDef]) -> List[str]:
    refs: List[str] = []
    seen = set()
    table_ref_pattern = re.compile(r"\b(?:FROM|JOIN)\s+([A-Za-z_][A-Za-z0-9_\.]*)", re.IGNORECASE)
    for sql_def in sql_defs:
        for match in table_ref_pattern.finditer(sql_def.query_sql or ""):
            table_name = match.group(1).split(".")[-1].lower()
            if table_name not in seen:
                seen.add(table_name)
                refs.append(table_name)
    return refs


def resolve_business_schema_models(
    all_models: Dict[str, ModelDef], required_models: Sequence[ModelDef], sql_defs: Sequence[SqlDef]
) -> List[ModelDef]:
    ordered_codes: List[str] = []
    seen = set()

    def add(model: ModelDef) -> None:
        if model.code in seen:
            return
        seen.add(model.code)
        ordered_codes.append(model.code)

    for model in required_models:
        add(model)

    models_by_table: Dict[str, ModelDef] = {}
    for model in all_models.values():
        table_name = (model.table_name or "").lower()
        if not table_name:
            continue
        existing = models_by_table.get(table_name)
        if existing is None or model.version_key > existing.version_key:
            models_by_table[table_name] = model

    for table_name in extract_sql_table_refs(sql_defs):
        model = models_by_table.get(table_name)
        if model is not None:
            add(model)

    return [all_models[code] for code in ordered_codes]


def emit_preamble() -> List[str]:
    return [
        "-- Generated by deploy/docker/scripts/generate-business-view-runtime-sql.py",
        "-- Restores runtime layout JSON and PostgreSQL compatibility views for recovered MES modules.",
        "",
        "CREATE OR REPLACE FUNCTION public.upper(bigint) RETURNS text",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT upper($1::text) $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.upper(integer) RETURNS text",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT upper($1::text) $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.upper(smallint) RETURNS text",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT upper($1::text) $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.upper(numeric) RETURNS text",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT upper($1::text) $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.upper(double precision) RETURNS text",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT upper($1::text) $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.upper(real) RETURNS text",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT upper($1::text) $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.upper(boolean) RETURNS text",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT upper($1::text) $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.eq_text_bigint(left_value text, right_value bigint) RETURNS boolean",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT left_value = right_value::text $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.eq_bigint_text(left_value bigint, right_value text) RETURNS boolean",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT left_value::text = right_value $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.eq_boolean_text(left_value boolean, right_value text) RETURNS boolean",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT left_value = CASE lower(right_value) WHEN '1' THEN true WHEN 't' THEN true WHEN 'true' THEN true WHEN 'yes' THEN true WHEN 'y' THEN true ELSE false END $$;",
        "",
        "CREATE OR REPLACE FUNCTION public.eq_text_boolean(left_value text, right_value boolean) RETURNS boolean",
        "LANGUAGE sql IMMUTABLE PARALLEL SAFE",
        "AS $$ SELECT CASE lower(left_value) WHEN '1' THEN true WHEN 't' THEN true WHEN 'true' THEN true WHEN 'yes' THEN true WHEN 'y' THEN true ELSE false END = right_value $$;",
        "",
        "DROP OPERATOR IF EXISTS public.= (text, bigint);",
        "CREATE OPERATOR public.= (LEFTARG = text, RIGHTARG = bigint, FUNCTION = public.eq_text_bigint);",
        "",
        "DROP OPERATOR IF EXISTS public.= (bigint, text);",
        "CREATE OPERATOR public.= (LEFTARG = bigint, RIGHTARG = text, FUNCTION = public.eq_bigint_text);",
        "",
        "DROP OPERATOR IF EXISTS public.= (boolean, text);",
        "CREATE OPERATOR public.= (LEFTARG = boolean, RIGHTARG = text, FUNCTION = public.eq_boolean_text);",
        "",
        "DROP OPERATOR IF EXISTS public.= (text, boolean);",
        "CREATE OPERATOR public.= (LEFTARG = text, RIGHTARG = boolean, FUNCTION = public.eq_text_boolean);",
        "",
        "CREATE TABLE IF NOT EXISTS public.article (",
        "    id varchar(64) PRIMARY KEY,",
        "    code varchar(255),",
        "    title varchar(255),",
        "    content text,",
        "    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP",
        ");",
        "CREATE INDEX IF NOT EXISTS idx_article_code ON public.article(code);",
        "",
        "ALTER TABLE public.runtime_entity",
        "    ADD COLUMN IF NOT EXISTS list_view_data_type integer DEFAULT 0,",
        "    ADD COLUMN IF NOT EXISTS reff_view_data_type integer DEFAULT 0;",
        "",
        "ALTER TABLE public.ec_entity",
        "    ADD COLUMN IF NOT EXISTS list_view_data_type integer DEFAULT 0,",
        "    ADD COLUMN IF NOT EXISTS reff_view_data_type integer DEFAULT 0;",
        "",
        "DO $$",
        "BEGIN",
        "  IF to_regclass('public.org_person') IS NOT NULL AND to_regclass('public.auth_user') IS NOT NULL THEN",
        "    EXECUTE $view$",
        "CREATE OR REPLACE VIEW public.base_staff AS",
        "SELECT",
        "    op.id,",
        "    op.row_version AS version,",
        "    op.code,",
        "    op.name,",
        "    op.gender AS sex,",
        "    op.phone AS mobile,",
        "    op.email,",
        "    op.status AS work_status,",
        "    op.classified_level AS security_class,",
        "    op.valid,",
        "    au.id AS user_id,",
        "    au.user_name,",
        "    op.main_position AS main_position_id,",
        "    op.create_staff_id,",
        "    op.modify_staff_id,",
        "    NULL::bigint AS delete_staff_id,",
        "    op.create_time,",
        "    op.modify_time,",
        "    NULL::timestamp without time zone AS delete_time,",
        "    op.description AS memo,",
        "    NULL::character varying(255) AS customer_field2,",
        "    NULL::character varying(255) AS customer_field1,",
        "    NULL::character varying(64) AS uuid,",
        "    NULL::double precision AS sort,",
        "    op.sys_flag,",
        "    op.grand_leader_id AS higher_leader_staff_id,",
        "    op.direct_leader_id AS leader_staff_id,",
        "    NULL::character varying(255) AS ygxs,",
        "    NULL::character varying(255) AS hukouxingzhi,",
        "    NULL::character varying(255) AS hukoudi,",
        "    NULL::character varying(255) AS dangandi,",
        "    NULL::character varying(255) AS degree,",
        "    NULL::character varying(255) AS educational,",
        "    NULL::character varying(255) AS profession,",
        "    NULL::timestamp without time zone AS biye_time,",
        "    NULL::character varying(255) AS school,",
        "    NULL::character varying(255) AS foreign_language,",
        "    NULL::character varying(255) AS compute_level,",
        "    NULL::double precision AS height,",
        "    NULL::character varying(255) AS virtual_mobile,",
        "    NULL::character varying(255) AS politics_info,",
        "    NULL::character varying(255) AS nation,",
        "    NULL::character varying(255) AS marriage,",
        "    NULL::character varying(255) AS id_card,",
        "    NULL::character varying(255) AS native_place,",
        "    NULL::timestamp without time zone AS birthday,",
        "    op.avatar_url,",
        "    NULL::character varying(512) AS sign_pic_url,",
        "    NULL::character varying(32) AS entry_date,",
        "    NULL::character varying(200) AS qualification,",
        "    NULL::bigint AS title",
        "FROM public.org_person op",
        "LEFT JOIN public.auth_user au ON op.id = au.person_id AND au.valid = 1",
        "$view$;",
        "  END IF;",
        "END $$;",
        "",
        "DO $$",
        "BEGIN",
        "  IF to_regclass('public.org_department') IS NOT NULL THEN",
        "    EXECUTE $view$",
        "CREATE OR REPLACE VIEW public.base_department AS",
        "SELECT",
        "    od.id,",
        "    od.row_version AS version,",
        "    od.code,",
        "    od.name,",
        "    od.description,",
        "    od.full_path AS full_path_name,",
        "    od.lay_no,",
        "    od.lay_rec,",
        "    od.sort,",
        "    od.company_id AS cid,",
        "    od.parent_id,",
        "    od.valid,",
        "    od.leaf,",
        "    od.create_staff_id,",
        "    od.modify_staff_id,",
        "    NULL::bigint AS delete_staff_id,",
        "    od.create_time,",
        "    od.modify_time,",
        "    NULL::timestamp without time zone AS delete_time,",
        "    NULL::bigint AS staff_id,",
        "    NULL::character varying(255) AS objparame,",
        "    NULL::character varying(255) AS objparamd,",
        "    NULL::character varying(255) AS objparamc,",
        "    NULL::character varying(255) AS objparamb,",
        "    NULL::character varying(255) AS objparama,",
        "    NULL::character varying(255) AS res_scparame,",
        "    NULL::character varying(255) AS res_scparamd,",
        "    NULL::character varying(255) AS res_scparamc,",
        "    NULL::character varying(255) AS res_scparamb,",
        "    NULL::character varying(255) AS res_scparama,",
        "    NULL::character varying(255) AS charparame,",
        "    NULL::character varying(255) AS charparamd,",
        "    NULL::character varying(255) AS charparamc,",
        "    NULL::character varying(255) AS charparamb,",
        "    NULL::character varying(255) AS charparama,",
        "    NULL::double precision AS numberparame,",
        "    NULL::double precision AS numberparamd,",
        "    NULL::double precision AS numberparamc,",
        "    NULL::double precision AS numberparamb,",
        "    NULL::double precision AS numberparama,",
        "    NULL::timestamp without time zone AS dateparame,",
        "    NULL::timestamp without time zone AS dateparamd,",
        "    NULL::timestamp without time zone AS dateparamc,",
        "    NULL::timestamp without time zone AS dateparamb,",
        "    NULL::timestamp without time zone AS dateparama,",
        "    NULL::integer AS integerparame,",
        "    NULL::integer AS integerparamd,",
        "    NULL::integer AS integerparamc,",
        "    NULL::integer AS integerparamb,",
        "    NULL::integer AS integerparama,",
        "    NULL::character varying(255) AS customer_field2,",
        "    NULL::character varying(255) AS customer_field1,",
        "    NULL::character varying(255) AS sc_nature,",
        "    NULL::character varying(64) AS uuid,",
        "    od.sys_flag AS is_virtual,",
        "    od.dept_type",
        "FROM public.org_department od",
        "$view$;",
        "  END IF;",
        "END $$;",
        "",
        "DO $$",
        "BEGIN",
        "  IF to_regclass('public.rbac_menuoperate') IS NOT NULL THEN",
        "    ALTER TABLE public.rbac_menuoperate",
        "      ADD COLUMN IF NOT EXISTS enable_assigndept boolean DEFAULT false,",
        "      ADD COLUMN IF NOT EXISTS enable_deptrict boolean DEFAULT false;",
        "    EXECUTE $view$",
        "CREATE OR REPLACE VIEW public.base_menuoperate AS",
        "SELECT",
        "    rbac_menuoperate.id,",
        "    rbac_menuoperate.row_version AS version,",
        "    rbac_menuoperate.create_staff_id,",
        "    rbac_menuoperate.modify_staff_id,",
        "    NULL::text AS delete_staff_id,",
        "    rbac_menuoperate.create_time,",
        "    rbac_menuoperate.modify_time,",
        "    rbac_menuoperate.delete_time,",
        "    CASE WHEN rbac_menuoperate.valid IS NULL THEN NULL WHEN lower(rbac_menuoperate.valid::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS valid,",
        "    rbac_menuoperate.cid,",
        "    CASE WHEN rbac_menuoperate.is_allow_proxy IS NULL THEN NULL WHEN lower(rbac_menuoperate.is_allow_proxy::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS is_allow_proxy,",
        "    CASE WHEN rbac_menuoperate.is_hidden IS NULL THEN NULL WHEN lower(rbac_menuoperate.is_hidden::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS is_hidden,",
        "    CASE WHEN rbac_menuoperate.three_role IS NULL THEN NULL WHEN lower(rbac_menuoperate.three_role::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS three_role,",
        "    rbac_menuoperate.view_code,",
        "    CASE WHEN rbac_menuoperate.is_query IS NULL THEN NULL WHEN lower(rbac_menuoperate.is_query::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS is_query,",
        "    CASE WHEN rbac_menuoperate.is_orrelation IS NULL THEN NULL WHEN lower(rbac_menuoperate.is_orrelation::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS is_orrelation,",
        "    CASE WHEN rbac_menuoperate.for_flow_permission IS NULL THEN NULL WHEN lower(rbac_menuoperate.for_flow_permission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS for_data_permission,",
        "    CASE WHEN rbac_menuoperate.enable_norestrict IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_norestrict::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_norestrict,",
        "    CASE WHEN rbac_menuoperate.enable_custompermission IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_custompermission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_otherrestrict,",
        "    CASE WHEN rbac_menuoperate.enable_datapermission IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_datapermission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_specialpermission,",
        "    CASE WHEN rbac_menuoperate.enable_dealerpermission IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_dealerpermission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_dealerpermission,",
        "    CASE WHEN rbac_menuoperate.enable_assignstaff IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_assignstaff::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_assignstaff,",
        "    CASE WHEN rbac_menuoperate.enable_assignpos IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_assignpos::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_assignpos,",
        "    CASE WHEN rbac_menuoperate.enable_posrestrict IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_posrestrict::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_posrestrict,",
        "    CASE WHEN rbac_menuoperate.enable_grouprestrict IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_grouprestrict::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_grouprestrict,",
        "    rbac_menuoperate.entity_code,",
        "    CASE WHEN rbac_menuoperate.ignore_permission IS NULL THEN NULL WHEN lower(rbac_menuoperate.ignore_permission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS ignore_permission,",
        "    CASE WHEN rbac_menuoperate.power_flag IS NULL THEN NULL WHEN lower(rbac_menuoperate.power_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS power_flag,",
        "    NULLIF(rbac_menuoperate.flow_version::text, '')::integer AS flow_version,",
        "    rbac_menuoperate.flow_key,",
        "    rbac_menuoperate.msg_assembled,",
        "    rbac_menuoperate.deployment_id,",
        "    rbac_menuoperate.menuoperatetype AS type,",
        "    rbac_menuoperate.menuinfo_id,",
        "    rbac_menuoperate.icon_cls,",
        "    rbac_menuoperate.module_code AS module,",
        "    rbac_menuoperate.sort,",
        "    rbac_menuoperate.memo,",
        "    rbac_menuoperate.target,",
        "    rbac_menuoperate.namespace,",
        "    rbac_menuoperate.url,",
        "    rbac_menuoperate.name_zh_cn,",
        "    rbac_menuoperate.name,",
        "    rbac_menuoperate.code,",
        "    rbac_menuoperate.action_url AS action,",
        "    0 AS st_flag,",
        "    NULL::text AS st_type,",
        "    NULL::text AS st_tablecode,",
        "    NULL::text AS st_showstyle,",
        "    NULL::text AS st_operatetype,",
        "    NULL::text AS st_isview,",
        "    NULL::text AS st_iss2flowoperate,",
        "    NULL::text AS st_ismainquery,",
        "    NULL::text AS st_isdefault,",
        "    NULL::text AS st_flowkey,",
        "    NULL::text AS st_digitalsignature,",
        "    NULL::text AS st_defaultdisplay,",
        "    NULL::text AS st_activityid,",
        "    NULL::text AS menuoperate_mainoperatecode,",
        "    NULL::text AS menuoperate_iscontainer,",
        "    NULL::text AS menuoperate_entryoperatecode,",
        "    rbac_menuoperate.menuoperatetype,",
        "    rbac_menuoperate.flow_name,",
        "    rbac_menuoperate.flow_name_display,",
        "    CASE WHEN rbac_menuoperate.enable_assigndept IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_assigndept::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_assigndept,",
        "    CASE WHEN rbac_menuoperate.enable_deptrict IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_deptrict::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_deptrict",
        "FROM public.rbac_menuoperate",
        "$view$;",
        "  END IF;",
        "END $$;",
        "",
        "DO $$",
        "BEGIN",
        "  IF to_regclass('public.rbac_role') IS NOT NULL THEN",
        "    EXECUTE $view$",
        "CREATE OR REPLACE VIEW public.base_role AS",
        "SELECT",
        "    id, valid, version, cid, leaf, full_path_name, parent_id, lay_no, lay_rec, uuid,",
        "    three_role_type, role_type, sort, description, name, code, create_staff_id,",
        "    modify_staff_id, NULL::bigint AS delete_staff_id, create_time, modify_time, delete_time",
        "FROM public.rbac_role",
        "$view$;",
        "  END IF;",
        "END $$;",
        "",
        "DO $$",
        "BEGIN",
        "  IF to_regclass('public.rbac_flow_permission') IS NOT NULL THEN",
        "    EXECUTE $view$",
        "CREATE OR REPLACE VIEW public.base_datapermission AS",
        "SELECT",
        "    id, version, create_staff_id, modify_staff_id, NULL::bigint AS delete_staff_id,",
        "    create_time, modify_time, delete_time, 1::smallint AS valid, entity_code, purview_distribution,",
        "    purview_state, memo,",
        "    CASE WHEN unlimited_power IS NULL THEN NULL WHEN lower(unlimited_power::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS unlimited_power,",
        "    CASE WHEN group_power_flag IS NULL THEN NULL WHEN lower(group_power_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS group_power_flag,",
        "    CASE WHEN assign_staff_flag IS NULL THEN NULL WHEN lower(assign_staff_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS assign_staff_flag,",
        "    CASE WHEN assign_pos_flag IS NULL THEN NULL WHEN lower(assign_pos_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS assign_pos_flag,",
        "    CASE WHEN position_power_flag IS NULL THEN NULL WHEN lower(position_power_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS position_power_flag,",
        "    flow_permission_type AS data_permission_type, type_id, activity_code,",
        "    NULLIF(flow_version::text, '')::integer AS flow_version, flow_key, cid",
        "FROM public.rbac_flow_permission",
        "$view$;",
        "  END IF;",
        "END $$;",
        "",
        "DO $$",
        "BEGIN",
        "  IF to_regclass('public.rbac_menuinfo') IS NOT NULL THEN",
        "    EXECUTE $view$",
        "CREATE OR REPLACE VIEW public.base_menuinfo AS",
        "SELECT",
        "    id, version, create_staff_id, modify_staff_id, NULL::bigint AS delete_staff_id,",
        "    create_time, modify_time, delete_time, CASE WHEN valid THEN 1 ELSE 0 END::smallint AS valid,",
        "    cid, security_class, absolute_hidden, three_role, show_type, request_type, hidden_type,",
        "    menu_type, is_hide, group_only, entity_code, entity_code AS ec_entity_code, module_code,",
        "    system_default, css_class, sort, namespace, url, target, memo, name, name_display AS name_zh_cn,",
        "    code, lay_no, lay_rec, parent_id, full_path_name, action_url AS action, leaf,",
        "    NULL::smallint AS st_flag, NULL::text AS st_digitalsignature, NULL::text AS st_intro,",
        "    NULL::text AS st_tabtypeid, NULL::text AS st_type, NULL::text AS remote_user_name_named,",
        "    NULL::text AS remote_password_named, NULL::bigint AS remote_id, NULL::text AS pims_menu_type,",
        "    NULL::text AS module, NULL::text AS icon_url, status, enable, edited",
        "FROM public.rbac_menuinfo",
        "$view$;",
        "  END IF;",
        "END $$;",
        "",
    ]


def runtime_module_sql(module: ModuleDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, is_proto, main_module, acronym, "
        "type, publish_time, category, is_hide, is_read_only, proj_flag, is_new_generate, "
        "is_inherented_base, deploy_order, description, initial_version, project_version, "
        "artifact, value_zh_cn, name, is_cluster"
    )
    values = [
        sql_str(module.code),
        sql_str(module.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "true",
        sql_bool(module.is_proto),
        sql_bool(module.main_module),
        sql_str(null_if_blank(module.acronym)),
        sql_str(null_if_blank(module.module_type)),
        "NULL",
        sql_str(null_if_blank(module.category)),
        sql_bool(module.is_hide),
        sql_bool(module.is_read_only),
        sql_bool(module.proj_flag),
        sql_bool(module.is_new_generate),
        sql_bool(module.is_inherented_base),
        sql_str(null_if_blank(module.deploy_order)),
        sql_str(null_if_blank(module.description)),
        sql_str(null_if_blank(module.initial_version)),
        sql_str(null_if_blank(module.project_version)),
        sql_str(null_if_blank(module.artifact)),
        sql_str(null_if_blank(module.value_zh_cn)),
        sql_str(null_if_blank(module.name)),
        sql_int(module.is_cluster),
    ]
    return (
        f"INSERT INTO public.runtime_module ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(public.runtime_module.ec_env, ''), EXCLUDED.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = true,\n"
        "    is_proto = COALESCE(public.runtime_module.is_proto, EXCLUDED.is_proto),\n"
        "    main_module = COALESCE(public.runtime_module.main_module, EXCLUDED.main_module),\n"
        "    acronym = COALESCE(NULLIF(public.runtime_module.acronym, ''), EXCLUDED.acronym),\n"
        "    type = COALESCE(NULLIF(public.runtime_module.type, ''), EXCLUDED.type),\n"
        "    category = COALESCE(NULLIF(public.runtime_module.category, ''), EXCLUDED.category),\n"
        "    is_hide = COALESCE(public.runtime_module.is_hide, EXCLUDED.is_hide),\n"
        "    is_read_only = COALESCE(public.runtime_module.is_read_only, EXCLUDED.is_read_only),\n"
        "    proj_flag = COALESCE(public.runtime_module.proj_flag, EXCLUDED.proj_flag),\n"
        "    is_new_generate = COALESCE(public.runtime_module.is_new_generate, EXCLUDED.is_new_generate),\n"
        "    is_inherented_base = COALESCE(public.runtime_module.is_inherented_base, EXCLUDED.is_inherented_base),\n"
        "    deploy_order = COALESCE(NULLIF(public.runtime_module.deploy_order, ''), EXCLUDED.deploy_order),\n"
        "    description = COALESCE(NULLIF(public.runtime_module.description, ''), EXCLUDED.description),\n"
        "    initial_version = COALESCE(NULLIF(public.runtime_module.initial_version, ''), EXCLUDED.initial_version),\n"
        "    project_version = COALESCE(NULLIF(public.runtime_module.project_version, ''), EXCLUDED.project_version),\n"
        "    artifact = COALESCE(NULLIF(public.runtime_module.artifact, ''), EXCLUDED.artifact),\n"
        "    value_zh_cn = COALESCE(NULLIF(public.runtime_module.value_zh_cn, ''), EXCLUDED.value_zh_cn),\n"
        "    name = COALESCE(NULLIF(public.runtime_module.name, ''), EXCLUDED.name),\n"
        "    is_cluster = COALESCE(public.runtime_module.is_cluster, EXCLUDED.is_cluster);"
    )


def ec_module_sql(module: ModuleDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, name, value_zh_cn, artifact, "
        "project_version, initial_version, description, deploy_order, is_inherented_base, "
        "is_new_generate, proj_flag, is_read_only, is_hide, category, publish_time, type, "
        "acronym, is_proto, main_module, is_cluster"
    )
    values = [
        sql_str(module.code),
        sql_str(module.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "1",
        sql_str(null_if_blank(module.name)),
        sql_str(null_if_blank(module.value_zh_cn)),
        sql_str(null_if_blank(module.artifact)),
        sql_str(null_if_blank(module.project_version)),
        sql_str(null_if_blank(module.initial_version)),
        sql_str(null_if_blank(module.description)),
        sql_str(null_if_blank(module.deploy_order)),
        sql_bool_int(module.is_inherented_base),
        sql_bool_int(module.is_new_generate),
        sql_bool_int(module.proj_flag),
        sql_bool_int(module.is_read_only),
        sql_bool_int(module.is_hide),
        sql_str(null_if_blank(module.category)),
        "NULL",
        sql_str(null_if_blank(module.module_type)),
        sql_str(null_if_blank(module.acronym)),
        sql_bool_int(module.is_proto),
        sql_bool_int(module.main_module),
        sql_int(module.is_cluster),
    ]
    return (
        f"INSERT INTO public.ec_module ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(public.ec_module.ec_env, ''), EXCLUDED.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = 1,\n"
        "    name = COALESCE(NULLIF(public.ec_module.name, ''), EXCLUDED.name),\n"
        "    value_zh_cn = COALESCE(NULLIF(public.ec_module.value_zh_cn, ''), EXCLUDED.value_zh_cn),\n"
        "    artifact = COALESCE(NULLIF(public.ec_module.artifact, ''), EXCLUDED.artifact),\n"
        "    project_version = COALESCE(NULLIF(public.ec_module.project_version, ''), EXCLUDED.project_version),\n"
        "    initial_version = COALESCE(NULLIF(public.ec_module.initial_version, ''), EXCLUDED.initial_version),\n"
        "    description = COALESCE(NULLIF(public.ec_module.description, ''), EXCLUDED.description),\n"
        "    deploy_order = COALESCE(NULLIF(public.ec_module.deploy_order, ''), EXCLUDED.deploy_order),\n"
        "    is_inherented_base = COALESCE(public.ec_module.is_inherented_base, EXCLUDED.is_inherented_base),\n"
        "    is_new_generate = COALESCE(public.ec_module.is_new_generate, EXCLUDED.is_new_generate),\n"
        "    proj_flag = COALESCE(public.ec_module.proj_flag, EXCLUDED.proj_flag),\n"
        "    is_read_only = COALESCE(public.ec_module.is_read_only, EXCLUDED.is_read_only),\n"
        "    is_hide = COALESCE(public.ec_module.is_hide, EXCLUDED.is_hide),\n"
        "    category = COALESCE(NULLIF(public.ec_module.category, ''), EXCLUDED.category),\n"
        "    type = COALESCE(NULLIF(public.ec_module.type, ''), EXCLUDED.type),\n"
        "    acronym = COALESCE(NULLIF(public.ec_module.acronym, ''), EXCLUDED.acronym),\n"
        "    is_proto = COALESCE(public.ec_module.is_proto, EXCLUDED.is_proto),\n"
        "    main_module = COALESCE(public.ec_module.main_module, EXCLUDED.main_module),\n"
        "    is_cluster = COALESCE(public.ec_module.is_cluster, EXCLUDED.is_cluster);"
    )


def runtime_customer_condition_sql(condition: CustomerConditionDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, entity_code, module_code, "
        "proj_flag, condition_sql, json_condition, dataclassific_code, datagrid_code, view_code"
    )
    values = [
        sql_str(condition.code),
        sql_str(condition.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "true",
        sql_str(null_if_blank(condition.entity_code)),
        sql_str(null_if_blank(condition.module_code)),
        sql_bool(condition.proj_flag),
        sql_str(null_if_blank(condition.condition_sql)),
        sql_str(null_if_blank(condition.json_condition)),
        sql_str(null_if_blank(condition.dataclassific_code)),
        sql_str(null_if_blank(condition.datagrid_code)),
        sql_str(null_if_blank(condition.view_code)),
    ]
    return (
        f"INSERT INTO public.runtime_customer_condition ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(EXCLUDED.ec_env, ''), public.runtime_customer_condition.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = true,\n"
        "    entity_code = COALESCE(NULLIF(EXCLUDED.entity_code, ''), public.runtime_customer_condition.entity_code),\n"
        "    module_code = COALESCE(NULLIF(EXCLUDED.module_code, ''), public.runtime_customer_condition.module_code),\n"
        "    proj_flag = COALESCE(EXCLUDED.proj_flag, public.runtime_customer_condition.proj_flag),\n"
        "    condition_sql = COALESCE(NULLIF(EXCLUDED.condition_sql, ''), public.runtime_customer_condition.condition_sql),\n"
        "    json_condition = COALESCE(NULLIF(EXCLUDED.json_condition, ''), public.runtime_customer_condition.json_condition),\n"
        "    dataclassific_code = COALESCE(NULLIF(EXCLUDED.dataclassific_code, ''), public.runtime_customer_condition.dataclassific_code),\n"
        "    datagrid_code = COALESCE(NULLIF(EXCLUDED.datagrid_code, ''), public.runtime_customer_condition.datagrid_code),\n"
        "    view_code = COALESCE(NULLIF(EXCLUDED.view_code, ''), public.runtime_customer_condition.view_code);"
    )


def ec_customer_condition_sql(condition: CustomerConditionDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, entity_code, module_code, "
        "proj_flag, condition_sql, json_condition, dataclassific_code, datagrid_code, view_code"
    )
    values = [
        sql_str(condition.code),
        sql_str(condition.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "1",
        sql_str(null_if_blank(condition.entity_code)),
        sql_str(null_if_blank(condition.module_code)),
        sql_bool_int(condition.proj_flag),
        sql_str(null_if_blank(condition.condition_sql)),
        sql_str(null_if_blank(condition.json_condition)),
        sql_str(null_if_blank(condition.dataclassific_code)),
        sql_str(null_if_blank(condition.datagrid_code)),
        sql_str(null_if_blank(condition.view_code)),
    ]
    return (
        f"INSERT INTO public.ec_customer_condition ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(EXCLUDED.ec_env, ''), public.ec_customer_condition.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = 1,\n"
        "    entity_code = COALESCE(NULLIF(EXCLUDED.entity_code, ''), public.ec_customer_condition.entity_code),\n"
        "    module_code = COALESCE(NULLIF(EXCLUDED.module_code, ''), public.ec_customer_condition.module_code),\n"
        "    proj_flag = COALESCE(EXCLUDED.proj_flag, public.ec_customer_condition.proj_flag),\n"
        "    condition_sql = COALESCE(NULLIF(EXCLUDED.condition_sql, ''), public.ec_customer_condition.condition_sql),\n"
        "    json_condition = COALESCE(NULLIF(EXCLUDED.json_condition, ''), public.ec_customer_condition.json_condition),\n"
        "    dataclassific_code = COALESCE(NULLIF(EXCLUDED.dataclassific_code, ''), public.ec_customer_condition.dataclassific_code),\n"
        "    datagrid_code = COALESCE(NULLIF(EXCLUDED.datagrid_code, ''), public.ec_customer_condition.datagrid_code),\n"
        "    view_code = COALESCE(NULLIF(EXCLUDED.view_code, ''), public.ec_customer_condition.view_code);"
    )


def runtime_sql_sql(sql_def: SqlDef) -> str:
    columns = "code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql"
    values = [
        sql_str(sql_def.code),
        sql_str(sql_def.ec_env),
        "0",
        sql_bool(sql_def.proj_flag),
        sql_str(null_if_blank(sql_def.data_grid_code)),
        sql_str(null_if_blank(sql_def.view_code)),
        sql_int(sql_def.sql_type),
        sql_str(null_if_blank(sql_def.query_sql)),
    ]
    return (
        f"INSERT INTO public.runtime_sql ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(EXCLUDED.ec_env, ''), public.runtime_sql.ec_env),\n"
        "    proj_flag = COALESCE(EXCLUDED.proj_flag, public.runtime_sql.proj_flag),\n"
        "    data_grid_code = COALESCE(NULLIF(EXCLUDED.data_grid_code, ''), public.runtime_sql.data_grid_code),\n"
        "    view_code = COALESCE(NULLIF(EXCLUDED.view_code, ''), public.runtime_sql.view_code),\n"
        "    type = COALESCE(EXCLUDED.type, public.runtime_sql.type),\n"
        "    query_sql = COALESCE(NULLIF(EXCLUDED.query_sql, ''), public.runtime_sql.query_sql);"
    )


def ec_sql_sql(sql_def: SqlDef) -> str:
    columns = "code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql"
    values = [
        sql_str(sql_def.code),
        sql_str(sql_def.ec_env),
        "0",
        sql_bool_int(sql_def.proj_flag),
        sql_str(null_if_blank(sql_def.data_grid_code)),
        sql_str(null_if_blank(sql_def.view_code)),
        sql_int(sql_def.sql_type),
        sql_str(null_if_blank(sql_def.query_sql)),
    ]
    return (
        f"INSERT INTO public.ec_sql ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(EXCLUDED.ec_env, ''), public.ec_sql.ec_env),\n"
        "    proj_flag = COALESCE(EXCLUDED.proj_flag, public.ec_sql.proj_flag),\n"
        "    data_grid_code = COALESCE(NULLIF(EXCLUDED.data_grid_code, ''), public.ec_sql.data_grid_code),\n"
        "    view_code = COALESCE(NULLIF(EXCLUDED.view_code, ''), public.ec_sql.view_code),\n"
        "    type = COALESCE(EXCLUDED.type, public.ec_sql.type),\n"
        "    query_sql = COALESCE(NULLIF(EXCLUDED.query_sql, ''), public.ec_sql.query_sql);"
    )


def runtime_entity_sql(entity: EntityDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, enable_fields_permission_conf, "
        "enable_ws, enable_rest, proj_flag, type, enable_audit, enable_acl_restrict, mobile, "
        "cross_company_flag, is_control, is_inherented_base, inherent_common_flag, is_base, "
        "module_code, pay_close_attention, group_enabled, prefix, description, workflow_enabled, "
        "entity_name, value_zh_cn, name, list_view_data_type, reff_view_data_type"
    )
    values = [
        sql_str(entity.code),
        sql_str(entity.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "true",
        sql_bool(entity.enable_fields_permission_conf),
        sql_bool(entity.enable_ws),
        sql_bool(entity.enable_rest),
        sql_bool(entity.proj_flag),
        sql_str(null_if_blank(entity.entity_type)),
        sql_bool(entity.enable_audit),
        sql_bool(entity.enable_acl_restrict),
        sql_bool(entity.mobile),
        sql_bool(entity.cross_company_flag),
        sql_bool(entity.is_control),
        sql_bool(entity.is_inherented_base),
        sql_bool(entity.inherent_common_flag),
        sql_bool(entity.is_base),
        sql_str(null_if_blank(entity.module_code)),
        sql_bool(entity.pay_close_attention),
        sql_bool(entity.group_enabled),
        sql_str(null_if_blank(entity.prefix)),
        sql_str(null_if_blank(entity.description)),
        sql_bool(entity.workflow_enabled),
        sql_str(null_if_blank(entity.entity_name)),
        sql_str(null_if_blank(entity.value_zh_cn)),
        sql_str(null_if_blank(entity.name)),
        sql_int(entity.list_view_data_type),
        sql_int(entity.reff_view_data_type),
    ]
    return (
        f"INSERT INTO public.runtime_entity ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(public.runtime_entity.ec_env, ''), EXCLUDED.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = true,\n"
        "    enable_fields_permission_conf = COALESCE(public.runtime_entity.enable_fields_permission_conf, EXCLUDED.enable_fields_permission_conf),\n"
        "    enable_ws = COALESCE(public.runtime_entity.enable_ws, EXCLUDED.enable_ws),\n"
        "    enable_rest = COALESCE(public.runtime_entity.enable_rest, EXCLUDED.enable_rest),\n"
        "    proj_flag = COALESCE(public.runtime_entity.proj_flag, EXCLUDED.proj_flag),\n"
        "    type = COALESCE(NULLIF(public.runtime_entity.type, ''), EXCLUDED.type),\n"
        "    enable_audit = COALESCE(public.runtime_entity.enable_audit, EXCLUDED.enable_audit),\n"
        "    enable_acl_restrict = COALESCE(public.runtime_entity.enable_acl_restrict, EXCLUDED.enable_acl_restrict),\n"
        "    mobile = COALESCE(public.runtime_entity.mobile, EXCLUDED.mobile),\n"
        "    cross_company_flag = COALESCE(public.runtime_entity.cross_company_flag, EXCLUDED.cross_company_flag),\n"
        "    is_control = COALESCE(public.runtime_entity.is_control, EXCLUDED.is_control),\n"
        "    is_inherented_base = COALESCE(public.runtime_entity.is_inherented_base, EXCLUDED.is_inherented_base),\n"
        "    inherent_common_flag = COALESCE(public.runtime_entity.inherent_common_flag, EXCLUDED.inherent_common_flag),\n"
        "    is_base = COALESCE(public.runtime_entity.is_base, EXCLUDED.is_base),\n"
        "    module_code = COALESCE(NULLIF(public.runtime_entity.module_code, ''), EXCLUDED.module_code),\n"
        "    pay_close_attention = COALESCE(public.runtime_entity.pay_close_attention, EXCLUDED.pay_close_attention),\n"
        "    group_enabled = COALESCE(public.runtime_entity.group_enabled, EXCLUDED.group_enabled),\n"
        "    prefix = COALESCE(NULLIF(public.runtime_entity.prefix, ''), EXCLUDED.prefix),\n"
        "    description = COALESCE(NULLIF(public.runtime_entity.description, ''), EXCLUDED.description),\n"
        "    workflow_enabled = COALESCE(public.runtime_entity.workflow_enabled, EXCLUDED.workflow_enabled),\n"
        "    entity_name = COALESCE(NULLIF(public.runtime_entity.entity_name, ''), EXCLUDED.entity_name),\n"
        "    value_zh_cn = COALESCE(NULLIF(public.runtime_entity.value_zh_cn, ''), EXCLUDED.value_zh_cn),\n"
        "    name = COALESCE(NULLIF(public.runtime_entity.name, ''), EXCLUDED.name),\n"
        "    list_view_data_type = COALESCE(public.runtime_entity.list_view_data_type, EXCLUDED.list_view_data_type),\n"
        "    reff_view_data_type = COALESCE(public.runtime_entity.reff_view_data_type, EXCLUDED.reff_view_data_type);"
    )


def ec_entity_sql(entity: EntityDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, enable_fields_permission_conf, "
        "enable_ws, enable_rest, proj_flag, type, enable_audit, enable_acl_restrict, mobile, "
        "cross_company_flag, is_control, is_inherented_base, inherent_common_flag, is_base, "
        "module_code, pay_close_attention, group_enabled, prefix, description, workflow_enabled, "
        "entity_name, value_zh_cn, name, list_view_data_type, reff_view_data_type"
    )
    values = [
        sql_str(entity.code),
        sql_str(entity.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "1",
        sql_bool_int(entity.enable_fields_permission_conf),
        sql_bool_int(entity.enable_ws),
        sql_bool_int(entity.enable_rest),
        sql_bool_int(entity.proj_flag),
        sql_str(null_if_blank(entity.entity_type)),
        sql_bool_int(entity.enable_audit),
        sql_bool_int(entity.enable_acl_restrict),
        sql_bool_int(entity.mobile),
        sql_bool_int(entity.cross_company_flag),
        sql_bool_int(entity.is_control),
        sql_bool_int(entity.is_inherented_base),
        sql_bool_int(entity.inherent_common_flag),
        sql_bool_int(entity.is_base),
        sql_str(null_if_blank(entity.module_code)),
        sql_bool_int(entity.pay_close_attention),
        sql_bool_int(entity.group_enabled),
        sql_str(null_if_blank(entity.prefix)),
        sql_str(null_if_blank(entity.description)),
        sql_bool_int(entity.workflow_enabled),
        sql_str(null_if_blank(entity.entity_name)),
        sql_str(null_if_blank(entity.value_zh_cn)),
        sql_str(null_if_blank(entity.name)),
        sql_int(entity.list_view_data_type),
        sql_int(entity.reff_view_data_type),
    ]
    return (
        f"INSERT INTO public.ec_entity ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(public.ec_entity.ec_env, ''), EXCLUDED.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = 1,\n"
        "    enable_fields_permission_conf = COALESCE(public.ec_entity.enable_fields_permission_conf, EXCLUDED.enable_fields_permission_conf),\n"
        "    enable_ws = COALESCE(public.ec_entity.enable_ws, EXCLUDED.enable_ws),\n"
        "    enable_rest = COALESCE(public.ec_entity.enable_rest, EXCLUDED.enable_rest),\n"
        "    proj_flag = COALESCE(public.ec_entity.proj_flag, EXCLUDED.proj_flag),\n"
        "    type = COALESCE(NULLIF(public.ec_entity.type, ''), EXCLUDED.type),\n"
        "    enable_audit = COALESCE(public.ec_entity.enable_audit, EXCLUDED.enable_audit),\n"
        "    enable_acl_restrict = COALESCE(public.ec_entity.enable_acl_restrict, EXCLUDED.enable_acl_restrict),\n"
        "    mobile = COALESCE(public.ec_entity.mobile, EXCLUDED.mobile),\n"
        "    cross_company_flag = COALESCE(public.ec_entity.cross_company_flag, EXCLUDED.cross_company_flag),\n"
        "    is_control = COALESCE(public.ec_entity.is_control, EXCLUDED.is_control),\n"
        "    is_inherented_base = COALESCE(public.ec_entity.is_inherented_base, EXCLUDED.is_inherented_base),\n"
        "    inherent_common_flag = COALESCE(public.ec_entity.inherent_common_flag, EXCLUDED.inherent_common_flag),\n"
        "    is_base = COALESCE(public.ec_entity.is_base, EXCLUDED.is_base),\n"
        "    module_code = COALESCE(NULLIF(public.ec_entity.module_code, ''), EXCLUDED.module_code),\n"
        "    pay_close_attention = COALESCE(public.ec_entity.pay_close_attention, EXCLUDED.pay_close_attention),\n"
        "    group_enabled = COALESCE(public.ec_entity.group_enabled, EXCLUDED.group_enabled),\n"
        "    prefix = COALESCE(NULLIF(public.ec_entity.prefix, ''), EXCLUDED.prefix),\n"
        "    description = COALESCE(NULLIF(public.ec_entity.description, ''), EXCLUDED.description),\n"
        "    workflow_enabled = COALESCE(public.ec_entity.workflow_enabled, EXCLUDED.workflow_enabled),\n"
        "    entity_name = COALESCE(NULLIF(public.ec_entity.entity_name, ''), EXCLUDED.entity_name),\n"
        "    value_zh_cn = COALESCE(NULLIF(public.ec_entity.value_zh_cn, ''), EXCLUDED.value_zh_cn),\n"
        "    name = COALESCE(NULLIF(public.ec_entity.name, ''), EXCLUDED.name),\n"
        "    list_view_data_type = COALESCE(public.ec_entity.list_view_data_type, EXCLUDED.list_view_data_type),\n"
        "    reff_view_data_type = COALESCE(public.ec_entity.reff_view_data_type, EXCLUDED.reff_view_data_type);"
    )


def runtime_model_sql(model: ModelDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, is_error_sql, proj_flag, "
        "is_config_special, special_auth_isandrel, is_mne_code, is_control, is_cache, "
        "entity_class, is_extra_col, enable_data_audit, enable_operation_audit, enable_sync, "
        "table_name, inherent_common_flag, ec_version, jpa_name, module_code, extends_model_code, "
        "is_extends, type, data_type, is_main, entity_code, description, model_name, value_zh_cn, name"
    )
    values = [
        sql_str(model.code),
        sql_str(model.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "true",
        sql_int(model.is_error_sql),
        sql_bool(model.proj_flag),
        sql_bool(model.is_config_special),
        sql_bool(model.special_auth_isandrel),
        sql_bool(model.is_mne_code),
        sql_bool(model.is_control),
        sql_bool(model.is_cache),
        sql_str(null_if_blank(model.entity_class)),
        sql_bool(model.is_extra_col),
        sql_bool(model.enable_data_audit),
        sql_bool(model.enable_operation_audit),
        sql_bool(model.enable_sync),
        sql_str(null_if_blank(model.table_name)),
        sql_bool(model.inherent_common_flag),
        sql_str(null_if_blank(model.ec_version)),
        sql_str(null_if_blank(model.jpa_name)),
        sql_str(null_if_blank(model.module_code)),
        sql_str(null_if_blank(model.extends_model_code)),
        sql_bool(model.is_extends),
        sql_int(model.model_type),
        sql_int(model.data_type),
        sql_bool(model.is_main),
        sql_str(null_if_blank(model.entity_code)),
        sql_str(null_if_blank(model.description)),
        sql_str(null_if_blank(model.model_name)),
        sql_str(null_if_blank(model.value_zh_cn)),
        sql_str(null_if_blank(model.name)),
    ]
    return (
        f"INSERT INTO public.runtime_model ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(EXCLUDED.ec_env, ''), public.runtime_model.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = true,\n"
        "    is_error_sql = COALESCE(EXCLUDED.is_error_sql, public.runtime_model.is_error_sql),\n"
        "    proj_flag = COALESCE(EXCLUDED.proj_flag, public.runtime_model.proj_flag),\n"
        "    is_config_special = COALESCE(EXCLUDED.is_config_special, public.runtime_model.is_config_special),\n"
        "    special_auth_isandrel = COALESCE(EXCLUDED.special_auth_isandrel, public.runtime_model.special_auth_isandrel),\n"
        "    is_mne_code = COALESCE(EXCLUDED.is_mne_code, public.runtime_model.is_mne_code),\n"
        "    is_control = COALESCE(EXCLUDED.is_control, public.runtime_model.is_control),\n"
        "    is_cache = COALESCE(EXCLUDED.is_cache, public.runtime_model.is_cache),\n"
        "    entity_class = COALESCE(NULLIF(EXCLUDED.entity_class, ''), public.runtime_model.entity_class),\n"
        "    is_extra_col = COALESCE(EXCLUDED.is_extra_col, public.runtime_model.is_extra_col),\n"
        "    enable_data_audit = COALESCE(EXCLUDED.enable_data_audit, public.runtime_model.enable_data_audit),\n"
        "    enable_operation_audit = COALESCE(EXCLUDED.enable_operation_audit, public.runtime_model.enable_operation_audit),\n"
        "    enable_sync = COALESCE(EXCLUDED.enable_sync, public.runtime_model.enable_sync),\n"
        "    table_name = COALESCE(NULLIF(EXCLUDED.table_name, ''), public.runtime_model.table_name),\n"
        "    inherent_common_flag = COALESCE(EXCLUDED.inherent_common_flag, public.runtime_model.inherent_common_flag),\n"
        "    ec_version = COALESCE(NULLIF(EXCLUDED.ec_version, ''), public.runtime_model.ec_version),\n"
        "    jpa_name = COALESCE(NULLIF(EXCLUDED.jpa_name, ''), public.runtime_model.jpa_name),\n"
        "    module_code = COALESCE(NULLIF(EXCLUDED.module_code, ''), public.runtime_model.module_code),\n"
        "    extends_model_code = COALESCE(NULLIF(EXCLUDED.extends_model_code, ''), public.runtime_model.extends_model_code),\n"
        "    is_extends = COALESCE(EXCLUDED.is_extends, public.runtime_model.is_extends),\n"
        "    type = COALESCE(EXCLUDED.type, public.runtime_model.type),\n"
        "    data_type = COALESCE(EXCLUDED.data_type, public.runtime_model.data_type),\n"
        "    is_main = COALESCE(EXCLUDED.is_main, public.runtime_model.is_main),\n"
        "    entity_code = COALESCE(NULLIF(EXCLUDED.entity_code, ''), public.runtime_model.entity_code),\n"
        "    description = COALESCE(NULLIF(EXCLUDED.description, ''), public.runtime_model.description),\n"
        "    model_name = COALESCE(NULLIF(EXCLUDED.model_name, ''), public.runtime_model.model_name),\n"
        "    value_zh_cn = COALESCE(NULLIF(EXCLUDED.value_zh_cn, ''), public.runtime_model.value_zh_cn),\n"
        "    name = COALESCE(NULLIF(EXCLUDED.name, ''), public.runtime_model.name);"
    )


def ec_model_sql(model: ModelDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, is_error_sql, proj_flag, "
        "is_config_special, special_auth_isandrel, is_mne_code, is_control, is_cache, "
        "entity_class, is_extra_col, enable_data_audit, enable_operation_audit, enable_sync, "
        "table_name, inherent_common_flag, ec_version, jpa_name, module_code, extends_model_code, "
        "is_extends, type, data_type, is_main, entity_code, description, model_name, value_zh_cn, name"
    )
    values = [
        sql_str(model.code),
        sql_str(model.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "1",
        sql_int(model.is_error_sql),
        sql_bool_int(model.proj_flag),
        sql_bool_int(model.is_config_special),
        sql_bool_int(model.special_auth_isandrel),
        sql_bool_int(model.is_mne_code),
        sql_bool_int(model.is_control),
        sql_bool_int(model.is_cache),
        sql_str(null_if_blank(model.entity_class)),
        sql_bool_int(model.is_extra_col),
        sql_bool_int(model.enable_data_audit),
        sql_bool_int(model.enable_operation_audit),
        sql_bool_int(model.enable_sync),
        sql_str(null_if_blank(model.table_name)),
        sql_bool_int(model.inherent_common_flag),
        sql_str(null_if_blank(model.ec_version)),
        sql_str(null_if_blank(model.jpa_name)),
        sql_str(null_if_blank(model.module_code)),
        sql_str(null_if_blank(model.extends_model_code)),
        sql_bool_int(model.is_extends),
        sql_int(model.model_type),
        sql_int(model.data_type),
        sql_bool_int(model.is_main),
        sql_str(null_if_blank(model.entity_code)),
        sql_str(null_if_blank(model.description)),
        sql_str(null_if_blank(model.model_name)),
        sql_str(null_if_blank(model.value_zh_cn)),
        sql_str(null_if_blank(model.name)),
    ]
    return (
        f"INSERT INTO public.ec_model ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = EXCLUDED.ec_env,\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = 1,\n"
        "    is_error_sql = EXCLUDED.is_error_sql,\n"
        "    proj_flag = EXCLUDED.proj_flag,\n"
        "    is_config_special = EXCLUDED.is_config_special,\n"
        "    special_auth_isandrel = EXCLUDED.special_auth_isandrel,\n"
        "    is_mne_code = EXCLUDED.is_mne_code,\n"
        "    is_control = EXCLUDED.is_control,\n"
        "    is_cache = EXCLUDED.is_cache,\n"
        "    entity_class = EXCLUDED.entity_class,\n"
        "    is_extra_col = EXCLUDED.is_extra_col,\n"
        "    enable_data_audit = EXCLUDED.enable_data_audit,\n"
        "    enable_operation_audit = EXCLUDED.enable_operation_audit,\n"
        "    enable_sync = EXCLUDED.enable_sync,\n"
        "    table_name = EXCLUDED.table_name,\n"
        "    inherent_common_flag = EXCLUDED.inherent_common_flag,\n"
        "    ec_version = EXCLUDED.ec_version,\n"
        "    jpa_name = EXCLUDED.jpa_name,\n"
        "    module_code = EXCLUDED.module_code,\n"
        "    extends_model_code = EXCLUDED.extends_model_code,\n"
        "    is_extends = EXCLUDED.is_extends,\n"
        "    type = EXCLUDED.type,\n"
        "    data_type = EXCLUDED.data_type,\n"
        "    is_main = EXCLUDED.is_main,\n"
        "    entity_code = EXCLUDED.entity_code,\n"
        "    description = EXCLUDED.description,\n"
        "    model_name = EXCLUDED.model_name,\n"
        "    value_zh_cn = EXCLUDED.value_zh_cn,\n"
        "    name = EXCLUDED.name;"
    )


def runtime_property_sql(prop: PropertyDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, entity_code, module_code, "
        "only_leaf, is_group_object, proj_custom_in_use, proj_flag, sort, is_hidden, is_engine, "
        "fetch_mode, associated_property_code, associated_type, is_custom, is_mne_whole_like_query, "
        "column_name, senior_system_code, no_analyzer, is_main_associated, is_used_for_search, "
        "stretch, pic_height, pic_width, is_bussiness_key, is_control, is_used_mne_code, "
        "default_value, is_sensitive, is_main_display, is_used_for_list, model_code, description, "
        "is_pk, is_ignore_audit, is_inherent, is_unique, multable, decimal_num, max_length, "
        "nullable, is_index, field_type, format, type, display_name, name, "
        "is_support_sup_and_sub, is_pic_support_multi_select, max_pic_num, org_column_name, "
        "is_tree_system_code, show_width"
    )
    values = [
        sql_str(prop.code),
        sql_str(prop.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "true",
        sql_str(null_if_blank(prop.entity_code)),
        sql_str(null_if_blank(prop.module_code)),
        sql_bool(prop.only_leaf),
        sql_bool(prop.is_group_object),
        sql_bool(prop.proj_custom_in_use),
        sql_bool(prop.proj_flag),
        sql_int(prop.sort),
        sql_bool(prop.is_hidden),
        sql_bool(prop.is_engine),
        sql_str(null_if_blank(prop.fetch_mode)),
        sql_str(null_if_blank(prop.associated_property_code)),
        sql_int(prop.associated_type),
        sql_bool(prop.is_custom),
        sql_bool(prop.is_mne_whole_like_query),
        sql_str(null_if_blank(prop.column_name)),
        sql_bool(prop.senior_system_code),
        sql_bool(prop.no_analyzer),
        sql_bool(prop.is_main_associated),
        sql_bool(prop.is_used_for_search),
        sql_bool(prop.stretch),
        sql_str(null_if_blank(prop.pic_height)),
        sql_str(null_if_blank(prop.pic_width)),
        sql_bool(prop.is_bussiness_key),
        sql_bool(prop.is_control),
        sql_bool(prop.is_used_mne_code),
        sql_str(null_if_blank(prop.default_value)),
        sql_bool(prop.is_sensitive),
        sql_bool(prop.is_main_display),
        sql_bool(prop.is_used_for_list),
        sql_str(prop.model_code),
        sql_str(null_if_blank(prop.description)),
        sql_bool(prop.is_pk),
        sql_bool(prop.is_ignore_audit),
        sql_bool(prop.is_inherent),
        sql_bool(prop.is_unique),
        sql_bool(prop.multable),
        sql_int(prop.decimal_num),
        sql_int(prop.max_length),
        sql_bool(prop.nullable),
        sql_bool(prop.is_index),
        sql_str(null_if_blank(prop.field_type)),
        sql_str(null_if_blank(prop.fmt)),
        sql_str(null_if_blank(prop.prop_type)),
        sql_str(null_if_blank(prop.display_name)),
        sql_str(null_if_blank(prop.name)),
        sql_bool(prop.is_support_sup_and_sub),
        sql_bool(prop.is_pic_support_multi_select),
        sql_int(prop.max_pic_num),
        sql_str(null_if_blank(prop.org_column_name)),
        sql_bool(prop.is_tree_system_code),
        sql_int(prop.show_width),
    ]
    return (
        f"INSERT INTO public.runtime_property ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(NULLIF(EXCLUDED.ec_env, ''), public.runtime_property.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = true,\n"
        "    entity_code = COALESCE(NULLIF(EXCLUDED.entity_code, ''), public.runtime_property.entity_code),\n"
        "    module_code = COALESCE(NULLIF(EXCLUDED.module_code, ''), public.runtime_property.module_code),\n"
        "    only_leaf = COALESCE(EXCLUDED.only_leaf, public.runtime_property.only_leaf),\n"
        "    is_group_object = COALESCE(EXCLUDED.is_group_object, public.runtime_property.is_group_object),\n"
        "    proj_custom_in_use = COALESCE(EXCLUDED.proj_custom_in_use, public.runtime_property.proj_custom_in_use),\n"
        "    proj_flag = COALESCE(EXCLUDED.proj_flag, public.runtime_property.proj_flag),\n"
        "    sort = COALESCE(EXCLUDED.sort, public.runtime_property.sort),\n"
        "    is_hidden = COALESCE(EXCLUDED.is_hidden, public.runtime_property.is_hidden),\n"
        "    is_engine = COALESCE(EXCLUDED.is_engine, public.runtime_property.is_engine),\n"
        "    fetch_mode = COALESCE(NULLIF(EXCLUDED.fetch_mode, ''), public.runtime_property.fetch_mode),\n"
        "    associated_property_code = COALESCE(NULLIF(EXCLUDED.associated_property_code, ''), public.runtime_property.associated_property_code),\n"
        "    associated_type = COALESCE(EXCLUDED.associated_type, public.runtime_property.associated_type),\n"
        "    is_custom = COALESCE(EXCLUDED.is_custom, public.runtime_property.is_custom),\n"
        "    is_mne_whole_like_query = COALESCE(EXCLUDED.is_mne_whole_like_query, public.runtime_property.is_mne_whole_like_query),\n"
        "    column_name = COALESCE(NULLIF(EXCLUDED.column_name, ''), public.runtime_property.column_name),\n"
        "    senior_system_code = COALESCE(EXCLUDED.senior_system_code, public.runtime_property.senior_system_code),\n"
        "    no_analyzer = COALESCE(EXCLUDED.no_analyzer, public.runtime_property.no_analyzer),\n"
        "    is_main_associated = COALESCE(EXCLUDED.is_main_associated, public.runtime_property.is_main_associated),\n"
        "    is_used_for_search = COALESCE(EXCLUDED.is_used_for_search, public.runtime_property.is_used_for_search),\n"
        "    stretch = COALESCE(EXCLUDED.stretch, public.runtime_property.stretch),\n"
        "    pic_height = COALESCE(NULLIF(EXCLUDED.pic_height, ''), public.runtime_property.pic_height),\n"
        "    pic_width = COALESCE(NULLIF(EXCLUDED.pic_width, ''), public.runtime_property.pic_width),\n"
        "    is_bussiness_key = COALESCE(EXCLUDED.is_bussiness_key, public.runtime_property.is_bussiness_key),\n"
        "    is_control = COALESCE(EXCLUDED.is_control, public.runtime_property.is_control),\n"
        "    is_used_mne_code = COALESCE(EXCLUDED.is_used_mne_code, public.runtime_property.is_used_mne_code),\n"
        "    default_value = COALESCE(NULLIF(EXCLUDED.default_value, ''), public.runtime_property.default_value),\n"
        "    is_sensitive = COALESCE(EXCLUDED.is_sensitive, public.runtime_property.is_sensitive),\n"
        "    is_main_display = COALESCE(EXCLUDED.is_main_display, public.runtime_property.is_main_display),\n"
        "    is_used_for_list = COALESCE(EXCLUDED.is_used_for_list, public.runtime_property.is_used_for_list),\n"
        "    model_code = COALESCE(NULLIF(EXCLUDED.model_code, ''), public.runtime_property.model_code),\n"
        "    description = COALESCE(NULLIF(EXCLUDED.description, ''), public.runtime_property.description),\n"
        "    is_pk = COALESCE(EXCLUDED.is_pk, public.runtime_property.is_pk),\n"
        "    is_ignore_audit = COALESCE(EXCLUDED.is_ignore_audit, public.runtime_property.is_ignore_audit),\n"
        "    is_inherent = COALESCE(EXCLUDED.is_inherent, public.runtime_property.is_inherent),\n"
        "    is_unique = COALESCE(EXCLUDED.is_unique, public.runtime_property.is_unique),\n"
        "    multable = COALESCE(EXCLUDED.multable, public.runtime_property.multable),\n"
        "    decimal_num = COALESCE(EXCLUDED.decimal_num, public.runtime_property.decimal_num),\n"
        "    max_length = COALESCE(EXCLUDED.max_length, public.runtime_property.max_length),\n"
        "    nullable = COALESCE(EXCLUDED.nullable, public.runtime_property.nullable),\n"
        "    is_index = COALESCE(EXCLUDED.is_index, public.runtime_property.is_index),\n"
        "    field_type = COALESCE(NULLIF(EXCLUDED.field_type, ''), public.runtime_property.field_type),\n"
        "    format = COALESCE(NULLIF(EXCLUDED.format, ''), public.runtime_property.format),\n"
        "    type = COALESCE(NULLIF(EXCLUDED.type, ''), public.runtime_property.type),\n"
        "    display_name = COALESCE(NULLIF(EXCLUDED.display_name, ''), public.runtime_property.display_name),\n"
        "    name = COALESCE(NULLIF(EXCLUDED.name, ''), public.runtime_property.name),\n"
        "    is_support_sup_and_sub = COALESCE(EXCLUDED.is_support_sup_and_sub, public.runtime_property.is_support_sup_and_sub),\n"
        "    is_pic_support_multi_select = COALESCE(EXCLUDED.is_pic_support_multi_select, public.runtime_property.is_pic_support_multi_select),\n"
        "    max_pic_num = COALESCE(EXCLUDED.max_pic_num, public.runtime_property.max_pic_num),\n"
        "    org_column_name = COALESCE(NULLIF(EXCLUDED.org_column_name, ''), public.runtime_property.org_column_name),\n"
        "    is_tree_system_code = COALESCE(EXCLUDED.is_tree_system_code, public.runtime_property.is_tree_system_code),\n"
        "    show_width = COALESCE(EXCLUDED.show_width, public.runtime_property.show_width);"
    )


def ec_property_sql(prop: PropertyDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, entity_code, module_code, "
        "only_leaf, is_group_object, proj_custom_in_use, proj_flag, sort, is_hidden, is_engine, "
        "fetch_mode, associated_property_code, associated_type, is_custom, is_mne_whole_like_query, "
        "column_name, senior_system_code, no_analyzer, is_main_associated, is_used_for_search, "
        "stretch, pic_height, pic_width, is_bussiness_key, is_control, is_used_mne_code, "
        "default_value, is_sensitive, is_main_display, is_used_for_list, model_code, description, "
        "is_pk, is_ignore_audit, is_inherent, is_unique, multable, decimal_num, max_length, "
        "nullable, is_index, field_type, format, type, display_name, name, "
        "is_support_sup_and_sub, is_pic_support_multi_select, max_pic_num, org_column_name, "
        "is_tree_system_code, show_width"
    )
    values = [
        sql_str(prop.code),
        sql_str(prop.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "1",
        sql_str(null_if_blank(prop.entity_code)),
        sql_str(null_if_blank(prop.module_code)),
        sql_bool_int(prop.only_leaf),
        sql_bool_int(prop.is_group_object),
        sql_bool_int(prop.proj_custom_in_use),
        sql_bool_int(prop.proj_flag),
        sql_int(prop.sort),
        sql_bool_int(prop.is_hidden),
        sql_bool_int(prop.is_engine),
        sql_str(null_if_blank(prop.fetch_mode)),
        sql_str(null_if_blank(prop.associated_property_code)),
        sql_int(prop.associated_type),
        sql_bool_int(prop.is_custom),
        sql_bool_int(prop.is_mne_whole_like_query),
        sql_str(null_if_blank(prop.column_name)),
        sql_bool_int(prop.senior_system_code),
        sql_bool_int(prop.no_analyzer),
        sql_bool_int(prop.is_main_associated),
        sql_bool_int(prop.is_used_for_search),
        sql_bool_int(prop.stretch),
        sql_str(null_if_blank(prop.pic_height)),
        sql_str(null_if_blank(prop.pic_width)),
        sql_bool_int(prop.is_bussiness_key),
        sql_bool_int(prop.is_control),
        sql_bool_int(prop.is_used_mne_code),
        sql_str(null_if_blank(prop.default_value)),
        sql_bool_int(prop.is_sensitive),
        sql_bool_int(prop.is_main_display),
        sql_bool_int(prop.is_used_for_list),
        sql_str(prop.model_code),
        sql_str(null_if_blank(prop.description)),
        sql_bool_int(prop.is_pk),
        sql_bool_int(prop.is_ignore_audit),
        sql_bool_int(prop.is_inherent),
        sql_bool_int(prop.is_unique),
        sql_bool_int(prop.multable),
        sql_int(prop.decimal_num),
        sql_int(prop.max_length),
        sql_bool_int(prop.nullable),
        sql_bool_int(prop.is_index),
        sql_str(null_if_blank(prop.field_type)),
        sql_str(null_if_blank(prop.fmt)),
        sql_str(null_if_blank(prop.prop_type)),
        sql_str(null_if_blank(prop.display_name)),
        sql_str(null_if_blank(prop.name)),
        sql_bool_int(prop.is_support_sup_and_sub),
        sql_bool_int(prop.is_pic_support_multi_select),
        sql_int(prop.max_pic_num),
        sql_str(null_if_blank(prop.org_column_name)),
        sql_bool_int(prop.is_tree_system_code),
        sql_int(prop.show_width),
    ]
    update_columns = (
        "ec_env", "entity_code", "module_code", "only_leaf", "is_group_object",
        "proj_custom_in_use", "proj_flag", "sort", "is_hidden", "is_engine", "fetch_mode",
        "associated_property_code", "associated_type", "is_custom", "is_mne_whole_like_query",
        "column_name", "senior_system_code", "no_analyzer", "is_main_associated",
        "is_used_for_search", "stretch", "pic_height", "pic_width", "is_bussiness_key",
        "is_control", "is_used_mne_code", "default_value", "is_sensitive", "is_main_display",
        "is_used_for_list", "model_code", "description", "is_pk", "is_ignore_audit",
        "is_inherent", "is_unique", "multable", "decimal_num", "max_length", "nullable",
        "is_index", "field_type", "format", "type", "display_name", "name",
        "is_support_sup_and_sub", "is_pic_support_multi_select", "max_pic_num",
        "org_column_name", "is_tree_system_code", "show_width",
    )
    updates = ",\n".join(f"    {column} = EXCLUDED.{column}" for column in update_columns)
    return (
        f"INSERT INTO public.ec_property ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        f"{updates},\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = 1;"
    )


def runtime_view_sql(view: ViewDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, type, show_type, title, "
        "display_name, name, url, module_code, entity_code, ass_model_code, has_attachment, "
        "only_for_query, main_view, main_ref, mobile, mobile_enable_flag, move_flag, extra_view, is_shadow"
    )
    values = [
        sql_str(view.code),
        sql_str(view.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "true",
        sql_str(runtime_view_type(view)),
        sql_str(view.show_type),
        sql_str(view.title),
        sql_str(view.display_name),
        sql_str(view.name),
        sql_str(view.url),
        sql_str(view.module_code),
        sql_str(view.entity_code),
        sql_str(view.ass_model_code),
        sql_bool(view.has_attachment),
        sql_bool(view.only_for_query),
        sql_bool(view.main_view),
        sql_bool(view.main_ref),
        sql_bool(view.mobile),
        sql_bool(view.mobile_enable_flag),
        sql_bool(view.move_flag),
        sql_str(view.code),
        "0",
    ]
    return (
        f"INSERT INTO public.runtime_view ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = COALESCE(EXCLUDED.ec_env, public.runtime_view.ec_env),\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = true,\n"
        "    type = COALESCE(EXCLUDED.type, public.runtime_view.type),\n"
        "    show_type = COALESCE(EXCLUDED.show_type, public.runtime_view.show_type),\n"
        "    title = COALESCE(EXCLUDED.title, public.runtime_view.title),\n"
        "    display_name = COALESCE(EXCLUDED.display_name, public.runtime_view.display_name),\n"
        "    name = COALESCE(EXCLUDED.name, public.runtime_view.name),\n"
        "    url = COALESCE(EXCLUDED.url, public.runtime_view.url),\n"
        "    module_code = COALESCE(EXCLUDED.module_code, public.runtime_view.module_code),\n"
        "    entity_code = COALESCE(EXCLUDED.entity_code, public.runtime_view.entity_code),\n"
        "    ass_model_code = COALESCE(EXCLUDED.ass_model_code, public.runtime_view.ass_model_code),\n"
        "    has_attachment = EXCLUDED.has_attachment,\n"
        "    only_for_query = EXCLUDED.only_for_query,\n"
        "    main_view = EXCLUDED.main_view,\n"
        "    main_ref = EXCLUDED.main_ref,\n"
        "    mobile = EXCLUDED.mobile,\n"
        "    mobile_enable_flag = EXCLUDED.mobile_enable_flag,\n"
        "    move_flag = EXCLUDED.move_flag,\n"
        "    extra_view = EXCLUDED.extra_view,\n"
        "    is_shadow = 0;"
    )


def ec_view_sql(view: ViewDef) -> str:
    columns = (
        "code, ec_env, version, create_time, modify_time, valid, type, show_type, title, "
        "display_name, name, url, module_code, entity_code, ass_model_code, has_attachment, "
        "only_for_query, main_view, main_ref, mobile, mobile_enable_flag, move_flag, extra_view, is_shadow"
    )
    values = [
        sql_str(view.code),
        sql_str(view.ec_env),
        "0",
        "CURRENT_TIMESTAMP",
        "CURRENT_TIMESTAMP",
        "1",
        sql_str(runtime_view_type(view)),
        sql_str(view.show_type),
        sql_str(view.title),
        sql_str(view.display_name),
        sql_str(view.name),
        sql_str(view.url),
        sql_str(view.module_code),
        sql_str(view.entity_code),
        sql_str(view.ass_model_code),
        sql_bool_int(view.has_attachment),
        sql_bool_int(view.only_for_query),
        sql_bool_int(view.main_view),
        sql_bool_int(view.main_ref),
        sql_bool_int(view.mobile),
        sql_bool_int(view.mobile_enable_flag),
        sql_bool_int(view.move_flag),
        sql_str(view.code),
        "0",
    ]
    return (
        f"INSERT INTO public.ec_view ({columns})\n"
        f"VALUES ({', '.join(values)})\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = EXCLUDED.ec_env,\n"
        "    modify_time = CURRENT_TIMESTAMP,\n"
        "    valid = 1,\n"
        "    type = EXCLUDED.type,\n"
        "    show_type = EXCLUDED.show_type,\n"
        "    title = EXCLUDED.title,\n"
        "    display_name = EXCLUDED.display_name,\n"
        "    name = EXCLUDED.name,\n"
        "    url = EXCLUDED.url,\n"
        "    module_code = EXCLUDED.module_code,\n"
        "    entity_code = EXCLUDED.entity_code,\n"
        "    ass_model_code = EXCLUDED.ass_model_code,\n"
        "    has_attachment = EXCLUDED.has_attachment,\n"
        "    only_for_query = EXCLUDED.only_for_query,\n"
        "    main_view = EXCLUDED.main_view,\n"
        "    main_ref = EXCLUDED.main_ref,\n"
        "    mobile = EXCLUDED.mobile,\n"
        "    mobile_enable_flag = EXCLUDED.mobile_enable_flag,\n"
        "    move_flag = EXCLUDED.move_flag,\n"
        "    extra_view = EXCLUDED.extra_view,\n"
        "    is_shadow = 0;"
    )


def action_view_routes(view: ViewDef) -> Tuple[str, ...]:
    """Return the framework routes that resolve an HTTP action to a view code."""
    url = (view.url or "").strip().rstrip("/")
    if not url:
        return ()
    if url.startswith("/msService/"):
        url = url[len("/msService"):]

    routes = [url]
    view_type = runtime_view_type(view).upper()
    show_type = (view.show_type or "").upper()
    if show_type != "LAYOUT2":
        if view_type == "LIST":
            routes.extend((f"{url}-query", f"{url}-getRequireData", f"{url}-pending"))
        elif view_type == "REFERENCE":
            routes.append(f"{url}-query")
        elif view_type in {"TREE", "REFTREE"}:
            routes.extend(f"{url}{suffix}" for suffix in ("Data", "FullData", "Drag", "Sort"))
    return tuple(dict.fromkeys(routes))


def action_view_sql(view: ViewDef) -> str:
    statements = []
    for action_url in action_view_routes(view):
        statements.append(
            "INSERT INTO public.action_view (action_url, view_name, view_code)\n"
            f"VALUES ({sql_str(action_url)}, {sql_str(view.name)}, {sql_str(view.code)})\n"
            "ON CONFLICT (action_url) DO UPDATE SET\n"
            "    view_name = EXCLUDED.view_name,\n"
            "    view_code = EXCLUDED.view_code;"
        )
    return "\n".join(statements)


def runtime_extra_view_sql(
    view: ViewDef,
    views: Dict[str, ViewDef],
    packaged_view_json: Optional[Dict[str, Dict[str, Any]]] = None,
) -> str:
    payload = view_json(view, views, packaged_view_json)
    code = sql_str(view.code)
    ec_env = sql_str(view.ec_env)
    payload_literal = sql_str(payload)
    return (
        "DO $$\n"
        "DECLARE runtime_extra_view_json_is_oid boolean;\n"
        f"DECLARE runtime_extra_view_payload text := {payload_literal};\n"
        "BEGIN\n"
        "    SELECT udt_name = 'oid' INTO runtime_extra_view_json_is_oid\n"
        "    FROM information_schema.columns\n"
        "    WHERE table_schema = 'public'\n"
        "      AND table_name = 'runtime_extra_view'\n"
        "      AND column_name = 'view_json';\n"
        "    IF COALESCE(runtime_extra_view_json_is_oid, false) THEN\n"
        "        INSERT INTO public.runtime_extra_view (code, ec_env, version, view_code, view_json, proj_flag)\n"
        f"        VALUES ({code}, {ec_env}, 0, {code}, lo_from_bytea(0, convert_to(runtime_extra_view_payload, 'UTF8')), false)\n"
        "        ON CONFLICT (code) DO UPDATE SET\n"
        "            ec_env = COALESCE(EXCLUDED.ec_env, public.runtime_extra_view.ec_env),\n"
        "            version = GREATEST(COALESCE(public.runtime_extra_view.version, 0), EXCLUDED.version),\n"
        "            view_code = EXCLUDED.view_code,\n"
        "            view_json = EXCLUDED.view_json,\n"
        "            proj_flag = COALESCE(public.runtime_extra_view.proj_flag, EXCLUDED.proj_flag);\n"
        "    ELSE\n"
        "        INSERT INTO public.runtime_extra_view (code, ec_env, version, view_code, view_json, proj_flag)\n"
        f"        VALUES ({code}, {ec_env}, 0, {code}, runtime_extra_view_payload, false)\n"
        "        ON CONFLICT (code) DO UPDATE SET\n"
        "            ec_env = COALESCE(EXCLUDED.ec_env, public.runtime_extra_view.ec_env),\n"
        "            version = GREATEST(COALESCE(public.runtime_extra_view.version, 0), EXCLUDED.version),\n"
        "            view_code = EXCLUDED.view_code,\n"
        "            view_json = EXCLUDED.view_json,\n"
        "            proj_flag = COALESCE(public.runtime_extra_view.proj_flag, EXCLUDED.proj_flag);\n"
        "    END IF;\n"
        "END $$;"
    )


def ec_extra_view_sql(
    view: ViewDef,
    views: Dict[str, ViewDef],
    packaged_view_json: Optional[Dict[str, Dict[str, Any]]] = None,
) -> str:
    payload = sql_str(view_json(view, views, packaged_view_json))
    code = sql_str(view.code)
    ec_env = sql_str(view.ec_env)
    return (
        "INSERT INTO public.ec_extra_view (code, ec_env, version, view_code, view_json, proj_flag)\n"
        f"VALUES ({code}, {ec_env}, 0, {code}, {payload}, 0)\n"
        "ON CONFLICT (code) DO UPDATE SET\n"
        "    ec_env = EXCLUDED.ec_env,\n"
        "    version = GREATEST(COALESCE(public.ec_extra_view.version, 0), EXCLUDED.version),\n"
        "    view_code = EXCLUDED.view_code,\n"
        "    view_json = EXCLUDED.view_json,\n"
        "    proj_flag = EXCLUDED.proj_flag;"
    )


COMMON_BUSINESS_COLUMNS: Sequence[Tuple[str, str]] = (
    ("version", "int4"),
    ("create_staff_id", "int8"),
    ("create_time", "timestamp"),
    ("modify_staff_id", "int8"),
    ("modify_time", "timestamp"),
    ("delete_staff_id", "int8"),
    ("delete_time", "timestamp"),
    ("valid", "boolean"),
    ("cid", "int8"),
    ("sort", "int4"),
    ("create_department_id", "int8"),
    ("create_position_id", "int8"),
    ("deployment_id", "int8"),
    ("effect_staff_id", "int8"),
    ("effect_time", "timestamp"),
    ("effective_state", "int4"),
    ("oa", "boolean"),
    ("group_id", "int8"),
    ("owner_department_id", "int8"),
    ("owner_position_id", "int8"),
    ("owner_staff_id", "int8"),
    ("position_lay_rec", "varchar(255)"),
    ("process_key", "varchar(255)"),
    ("process_version", "int4"),
    ("status", "int4"),
    ("table_no", "varchar(255)"),
    ("table_info_id", "int8"),
    ("extra_col", "text"),
    ("parent_id", "int8"),
    ("lay_rec", "varchar(1000)"),
    ("lay_no", "int4"),
    ("leaf", "int4"),
    ("full_path_name", "varchar(2000)"),
)


def pg_identifier(value: str) -> Optional[str]:
    value = (value or "").strip().lower()
    if not value or not re.fullmatch(r"[a-z_][a-z0-9_]*", value):
        return None
    return value


def pg_column_type(prop: PropertyDef) -> str:
    prop_type = (prop.prop_type or prop.field_type or "").strip().upper()
    if prop_type in {"LONG", "OBJECT"}:
        return "int8"
    if prop_type in {"INTEGER", "INT"}:
        return "int4"
    if prop_type in {"FLOAT", "DOUBLE", "DECIMAL", "NUMBER", "NUMERIC"}:
        scale = prop.decimal_num if prop.decimal_num is not None else 6
        return f"numeric(38,{scale})"
    if prop_type in {"DATETIME", "DATE", "TIME"}:
        return "timestamp"
    if prop_type == "BOOLEAN":
        return "boolean"
    if prop_type in {"LONGTEXT", "TEXTAREA", "CLOB"}:
        return "text"
    if prop.max_length and prop.max_length > 0:
        if prop.max_length > 4000:
            return "text"
        return f"varchar({prop.max_length})"
    if prop_type in {"SYSTEMCODE", "BAPCODE"}:
        return "varchar(255)"
    return "varchar(255)"


def business_schema_sql(models: Sequence[ModelDef]) -> List[str]:
    lines: List[str] = []
    table_model_codes: Dict[str, List[str]] = {}
    table_columns: Dict[str, Dict[str, str]] = {}
    for model in models:
        table_name = pg_identifier(model.table_name)
        if table_name is None:
            continue
        table_model_codes.setdefault(table_name, []).append(model.code)
        column_types = table_columns.setdefault(
            table_name, {name: col_type for name, col_type in COMMON_BUSINESS_COLUMNS}
        )
        for prop in model.properties:
            column_name = pg_identifier(prop.column_name)
            if column_name is None or column_name == "id":
                continue
            column_types.setdefault(column_name, pg_column_type(prop))

    for table_name in sorted(table_columns):
        model_codes = ", ".join(table_model_codes[table_name])
        lines.append(f"-- business table skeleton for {model_codes}")
        lines.append(f"CREATE TABLE IF NOT EXISTS public.{table_name} (id int8 PRIMARY KEY);")
        column_types = table_columns[table_name]
        for column_name, column_type in column_types.items():
            if column_name == "id":
                continue
            lines.append(f"ALTER TABLE public.{table_name} ADD COLUMN IF NOT EXISTS {column_name} {column_type};")
        for index_column in ("valid", "table_info_id", "cid"):
            if index_column in column_types:
                lines.append(
                    f"CREATE INDEX IF NOT EXISTS idx_{table_name}_{index_column} ON public.{table_name} ({index_column});"
                )
        lines.append("")
    return lines


def business_share_view_sql(models: Sequence[ModelDef]) -> List[str]:
    lines: List[str] = []
    seen = set()
    for model in models:
        table_name = pg_identifier(model.table_name)
        if table_name is None or table_name in seen:
            continue
        seen.add(table_name)
        view_name = f"{table_name}_sv"
        lines.extend(
            [
                f"-- permissive share-view compatibility for {view_name}",
                "DO $$",
                "BEGIN",
                f"  IF to_regclass('public.{table_name}') IS NOT NULL",
                f"     AND (to_regclass('public.{view_name}') IS NULL OR EXISTS (",
                "       SELECT 1",
                "       FROM pg_class c",
                "       JOIN pg_namespace n ON n.oid = c.relnamespace",
                f"       WHERE n.nspname = 'public' AND c.relname = '{view_name}' AND c.relkind = 'v'",
                "     )) THEN",
                "    EXECUTE $view$",
                f"CREATE OR REPLACE VIEW public.{view_name} AS",
                "SELECT table_info_id, owner_staff_id AS staff",
                f"FROM public.{table_name}",
                "WHERE table_info_id IS NOT NULL AND owner_staff_id IS NOT NULL",
                "$view$;",
                "  END IF;",
                "END $$;",
                "",
            ]
        )
    return lines


def merge_defs_by_code(primary: Sequence[Any], extras: Iterable[Any]) -> List[Any]:
    merged = list(primary)
    seen = {item.code for item in merged}
    for item in sorted(extras, key=lambda value: value.code):
        if item.code in seen:
            continue
        merged.append(item)
        seen.add(item.code)
    return merged


def module_target_views(
    views: Dict[str, ViewDef], targets: Sequence[str], module_codes: Sequence[str]
) -> Tuple[str, ...]:
    selected = list(targets)
    seen = set(selected)
    requested_modules = set(module_codes)
    for view in sorted(views.values(), key=lambda value: value.code):
        if view.module_code not in requested_modules or view.code in seen:
            continue
        selected.append(view.code)
        seen.add(view.code)
    return tuple(selected)


def generate_sql(
    modules_root: Path, targets: Sequence[str], module_codes: Sequence[str] = ()
) -> str:
    modules = load_modules(modules_root)
    views = load_views(modules_root)
    entities = load_entities(modules_root)
    models = load_models(modules_root)
    conditions = load_customer_conditions(modules_root)
    sql_defs = load_sql_defs(modules_root)
    packaged_view_json = load_packaged_view_json(modules_root)
    requested_modules = set(module_codes)
    effective_targets = module_target_views(views, targets, module_codes)
    required = resolve_required_views(views, effective_targets)
    required_models = resolve_required_models(models, required)
    required_models = merge_defs_by_code(
        required_models,
        (model for model in models.values() if model.module_code in requested_modules),
    )
    required_entities = resolve_required_entities(entities, required, required_models)
    required_entities = merge_defs_by_code(
        required_entities,
        (entity for entity in entities.values() if entity.module_code in requested_modules),
    )
    required_modules = resolve_required_modules(modules, required, required_models, required_entities)
    required_modules = merge_defs_by_code(
        required_modules,
        (module for module in modules.values() if module.code in requested_modules),
    )
    required_conditions = resolve_required_customer_conditions(conditions, required)
    required_conditions = merge_defs_by_code(
        required_conditions,
        (condition for condition in conditions.values() if condition.module_code in requested_modules),
    )
    required_sqls = resolve_required_sqls(sql_defs, required)
    required_sqls = merge_defs_by_code(
        required_sqls,
        (
            sql_def
            for sql_def in sql_defs.values()
            if any(
                (sql_def.code or "").startswith(module_code + "_")
                or (sql_def.view_code or "").startswith(module_code + "_")
                or (sql_def.data_grid_code or "").startswith(module_code + "_")
                for module_code in requested_modules
            )
        ),
    )
    schema_models = resolve_business_schema_models(models, required_models, required_sqls)
    lines = emit_preamble()
    lines.append(f"-- modules_root: {modules_root}")
    lines.append(f"-- target_view_count: {len(effective_targets)}")
    lines.append(f"-- requested_module_codes: {','.join(module_codes)}")
    lines.append(f"-- emitted_view_count: {len(required)}")
    lines.append(f"-- emitted_module_count: {len(required_modules)}")
    lines.append(f"-- emitted_entity_count: {len(required_entities)}")
    lines.append(f"-- emitted_model_count: {len(required_models)}")
    lines.append(f"-- emitted_customer_condition_count: {len(required_conditions)}")
    lines.append(f"-- emitted_sql_count: {len(required_sqls)}")
    lines.append(f"-- emitted_business_schema_model_count: {len(schema_models)}")
    lines.append(f"-- packaged_runtime_view_count: {len(packaged_view_json)}")
    lines.append("")
    lines.extend(business_schema_sql(schema_models))
    lines.extend(business_share_view_sql(schema_models))
    for module in required_modules:
        lines.append(f"-- {module.code} from {module.source}")
        lines.append(runtime_module_sql(module))
        lines.append(ec_module_sql(module))
        lines.append("")
    for entity in required_entities:
        lines.append(f"-- {entity.code} from {entity.source}")
        lines.append(runtime_entity_sql(entity))
        lines.append(ec_entity_sql(entity))
        lines.append("")
    for model in required_models:
        lines.append(f"-- {model.code} from {model.source}")
        lines.append(runtime_model_sql(model))
        lines.append(ec_model_sql(model))
        for prop in model.properties:
            lines.append(runtime_property_sql(prop))
            lines.append(ec_property_sql(prop))
        lines.append("")
    for condition in required_conditions:
        source = condition.source or Path("<synthetic>")
        lines.append(f"-- {condition.code} from {source}")
        lines.append(runtime_customer_condition_sql(condition))
        lines.append(ec_customer_condition_sql(condition))
        lines.append("")
    for sql_def in required_sqls:
        lines.append(f"-- {sql_def.code} from {sql_def.source}")
        lines.append(runtime_sql_sql(sql_def))
        lines.append(ec_sql_sql(sql_def))
        lines.append("")
    for view in required:
        lines.append(f"-- {view.code} from {view.source}")
        lines.append(runtime_view_sql(view))
        lines.append(ec_view_sql(view))
        lines.append(action_view_sql(view))
        lines.append(runtime_extra_view_sql(view, views, packaged_view_json))
        lines.append(ec_extra_view_sql(view, views, packaged_view_json))
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def generate_runtime_extra_view_sql(
    modules_root: Path, targets: Sequence[str], module_codes: Sequence[str] = ()
) -> str:
    views = load_views(modules_root)
    packaged_view_json = load_packaged_view_json(modules_root)
    effective_targets = module_target_views(views, targets, module_codes)
    required = resolve_required_views(views, effective_targets)
    lines = [
        "-- Generated by deploy/docker/scripts/generate-business-view-runtime-sql.py --runtime-extra-view-only",
        "-- Restores runtime_extra_view.view_json for recovered action views without touching business data.",
        f"-- modules_root: {modules_root}",
        f"-- target_view_count: {len(effective_targets)}",
        f"-- requested_module_codes: {','.join(module_codes)}",
        f"-- emitted_view_count: {len(required)}",
        f"-- packaged_runtime_view_count: {len(packaged_view_json)}",
        "",
    ]
    for view in required:
        lines.append(f"-- {view.code} from {view.source}")
        lines.append(runtime_extra_view_sql(view, views, packaged_view_json))
        lines.append(ec_extra_view_sql(view, views, packaged_view_json))
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def strip_generated_line_trailing_whitespace(output: str) -> str:
    """Keep embedded source readable without emitting patch-hostile line tails."""
    lines = []
    for raw_line in output.splitlines():
        line = raw_line.rstrip(" \t")
        indent = re.match(r"^[ \t]+", line)
        if indent and " " in indent.group(0) and "\t" in indent.group(0):
            line = indent.group(0).expandtabs(4) + line[indent.end() :]
        lines.append(line)
    return "\n".join(lines).rstrip("\n") + "\n"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--modules-root",
        type=Path,
        default=default_modules_root(),
        help="Path to mes-modules-source-repo/modules.",
    )
    parser.add_argument(
        "--target-view-code",
        action="append",
        dest="targets",
        help="Restrict generation to a target view code. Can be repeated.",
    )
    parser.add_argument(
        "--module-code",
        action="append",
        dest="module_codes",
        help="Generate complete runtime metadata and schema for a module code. Can be repeated.",
    )
    parser.add_argument(
        "--runtime-extra-view-only",
        action="store_true",
        help="Emit only runtime_extra_view.view_json upserts for the selected target view codes.",
    )
    parser.add_argument(
        "--packaged-ui-runtime-only",
        action="store_true",
        help="Emit packaged field/grid/button/event/query metadata for the selected module codes.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    modules_root = args.modules_root.expanduser().resolve()
    if not modules_root.exists():
        print(f"Modules root does not exist: {modules_root}", file=sys.stderr)
        return 2
    module_codes = tuple(args.module_codes or ())
    targets = tuple(args.targets or (() if module_codes else TARGET_VIEW_CODES))
    if args.runtime_extra_view_only and args.packaged_ui_runtime_only:
        print("Choose only one output mode.", file=sys.stderr)
        return 2
    if args.packaged_ui_runtime_only:
        if not module_codes:
            print("--packaged-ui-runtime-only requires at least one --module-code.", file=sys.stderr)
            return 2
        output = generate_packaged_ui_runtime_sql(modules_root, module_codes)
    elif args.runtime_extra_view_only:
        output = generate_runtime_extra_view_sql(modules_root, targets, module_codes)
    else:
        output = generate_sql(modules_root, targets, module_codes)
    sys.stdout.write(strip_generated_line_trailing_whitespace(output))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
