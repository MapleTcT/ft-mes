#!/usr/bin/env python3
"""Regression tests for product/runtime metadata parity generation."""

from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch


SCRIPT_PATH = Path(__file__).with_name("generate-business-view-runtime-sql.py")
SPEC = importlib.util.spec_from_file_location("business_view_runtime_generator", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load {SCRIPT_PATH}")
GENERATOR = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = GENERATOR
SPEC.loader.exec_module(GENERATOR)


def sample_model():
    return SimpleNamespace(
        code="PATROL_1.0.0_sample_Sample",
        ec_env="product",
        is_error_sql=0,
        proj_flag=False,
        is_config_special=False,
        special_auth_isandrel=False,
        is_mne_code=False,
        is_control=False,
        is_cache=False,
        entity_class="com.example.Sample",
        is_extra_col=False,
        enable_data_audit=False,
        enable_operation_audit=False,
        enable_sync=False,
        table_name="mp_samples",
        inherent_common_flag=False,
        ec_version="6.0.4.0",
        jpa_name="sample",
        module_code="PATROL_1.0.0",
        extends_model_code="",
        is_extends=False,
        model_type=0,
        data_type=0,
        is_main=True,
        entity_code="PATROL_1.0.0_sample",
        description="sample",
        model_name="Sample",
        value_zh_cn="Sample",
        name="Sample",
    )


def sample_property():
    values = {
        "code": "PATROL_1.0.0_sample_Sample_name",
        "ec_env": "product",
        "entity_code": "PATROL_1.0.0_sample",
        "module_code": "PATROL_1.0.0",
        "fetch_mode": "",
        "associated_property_code": "",
        "column_name": "name",
        "pic_height": "",
        "pic_width": "",
        "default_value": "",
        "model_code": "PATROL_1.0.0_sample_Sample",
        "description": "",
        "field_type": "TEXT",
        "fmt": "",
        "prop_type": "STRING",
        "display_name": "Name",
        "name": "name",
        "org_column_name": "",
        "sort": 1,
        "associated_type": 0,
        "decimal_num": 0,
        "max_length": 255,
        "max_pic_num": 0,
        "show_width": 120,
    }
    for name in (
        "only_leaf", "is_group_object", "proj_custom_in_use", "proj_flag", "is_hidden",
        "is_engine", "is_custom", "is_mne_whole_like_query", "senior_system_code",
        "no_analyzer", "is_main_associated", "is_used_for_search", "stretch",
        "is_bussiness_key", "is_control", "is_used_mne_code", "is_sensitive",
        "is_main_display", "is_used_for_list", "is_pk", "is_ignore_audit",
        "is_inherent", "is_unique", "multable", "is_index", "is_support_sup_and_sub",
        "is_pic_support_multi_select", "is_tree_system_code",
    ):
        values[name] = False
    values["nullable"] = True
    return SimpleNamespace(**values)


def sample_view():
    return SimpleNamespace(
        code="PATROL_1.0.0_sample_sampleList",
        ec_env="product",
        view_type="LIST",
        show_type="SINGLE",
        title="Sample list",
        display_name="Sample list",
        name="sampleList",
        url="/msService/PATROL/sample/sample/sampleList",
        module_code="PATROL_1.0.0",
        entity_code="PATROL_1.0.0_sample",
        ass_model_code="PATROL_1.0.0_sample_Sample",
        has_attachment=False,
        only_for_query=False,
        main_view=True,
        main_ref=False,
        mobile=False,
        mobile_enable_flag=False,
        move_flag=False,
        config_root=None,
    )


def view_variant(**changes):
    values = vars(sample_view()).copy()
    values.update(changes)
    return SimpleNamespace(**values)


class ProductRuntimeParityTest(unittest.TestCase):
    def test_model_and_property_emit_product_rows(self):
        model_sql = GENERATOR.ec_model_sql(sample_model())
        property_sql = GENERATOR.ec_property_sql(sample_property())

        self.assertIn("INSERT INTO public.ec_model", model_sql)
        self.assertIn("valid = 1", model_sql)
        self.assertIn("INSERT INTO public.ec_property", property_sql)
        self.assertIn("nullable", property_sql)
        self.assertIn("valid = 1", property_sql)

    def test_view_and_layout_emit_product_rows(self):
        view = sample_view()
        view_sql = GENERATOR.ec_view_sql(view)
        layout_sql = GENERATOR.ec_extra_view_sql(view, {view.code: view})

        self.assertIn("INSERT INTO public.ec_view", view_sql)
        self.assertIn(view.url, view_sql)
        self.assertIn(view.ass_model_code, view_sql)
        self.assertIn("INSERT INTO public.ec_extra_view", layout_sql)
        self.assertIn('"pageType":"LIST"', layout_sql)

    def test_list_action_routes_match_platform_contract(self):
        action_sql = GENERATOR.action_view_sql(sample_view())

        self.assertIn("/PATROL/sample/sample/sampleList'", action_sql)
        self.assertIn("/PATROL/sample/sample/sampleList-query'", action_sql)
        self.assertIn("/PATROL/sample/sample/sampleList-getRequireData'", action_sql)
        self.assertIn("/PATROL/sample/sample/sampleList-pending'", action_sql)
        self.assertEqual(4, action_sql.count("INSERT INTO public.action_view"))

    def test_nested_associated_property_code_is_preserved(self):
        model = ET.fromstring("<model><moduleCode>PATROL_1.0.0</moduleCode></model>")
        property_element = ET.fromstring(
            "<property>"
            "<code>PATROL_1.0.0_sample_Child_parent</code>"
            "<name>parent</name>"
            "<associatedType>2</associatedType>"
            "<associatedProperty><code>PATROL_1.0.0_sample_Parent_id</code></associatedProperty>"
            "</property>"
        )

        prop = GENERATOR.parse_property(
            model, property_element, "PATROL_1.0.0_sample_Child"
        )

        self.assertIsNotNone(prop)
        self.assertEqual("PATROL_1.0.0_sample_Parent_id", prop.associated_property_code)
        self.assertEqual(2, prop.associated_type)

    def test_sql_data_grid_code_is_inferred_from_packaged_sql_code(self):
        view_code = "PATROL_1.0.0_patrolRoute_workGroupList"
        sql_element = ET.fromstring(
            "<sql>"
            "<sql>SELECT * FROM MP_WORK_GROUPS</sql>"
            "<type>6</type>"
            f"<viewCode>{view_code}</viewCode>"
            f"<code>{view_code}_dg1575506219664_6</code>"
            "</sql>"
        )

        sql_def = GENERATOR.parse_sql_def(Path("PATROL/module.xml"), sql_element)

        self.assertIsNotNone(sql_def)
        self.assertEqual(
            "PATROL_1.0.0_patrolRoute_workGroupListdg1575506219664",
            sql_def.data_grid_code,
        )

    def test_reference_tree_and_layout_action_routes_are_type_specific(self):
        reference = view_variant(view_type="REFERENCE", name="sampleRef")
        reference_sql = GENERATOR.action_view_sql(reference)
        tree = view_variant(view_type="TREE", name="sampleTree")
        tree_sql = GENERATOR.action_view_sql(tree)
        layout = view_variant(show_type="LAYOUT2", name="sampleLayout")
        layout_sql = GENERATOR.action_view_sql(layout)

        self.assertIn("sampleList-query'", reference_sql)
        self.assertNotIn("getRequireData", reference_sql)
        for suffix in ("Data", "FullData", "Drag", "Sort"):
            self.assertIn(f"sampleList{suffix}'", tree_sql)
        self.assertEqual(5, tree_sql.count("INSERT INTO public.action_view"))
        self.assertEqual(1, layout_sql.count("INSERT INTO public.action_view"))

    def test_tree_runtime_preserves_title_field_and_root_configuration(self):
        config_root = ET.fromstring(
            "<config><layout>"
            "<layoutCode>sample_tree_layout</layoutCode>"
            "<pageConfig>"
            "<rootName>sample.root</rootName>"
            "<treeModelCode>name</treeModelCode>"
            "<dragOpen>true</dragOpen>"
            "<canMemory>true</canMemory>"
            "<canDrag>false</canDrag>"
            "</pageConfig>"
            "</layout></config>"
        )
        tree = view_variant(
            code="PATROL_1.0.0_sample_sampleTree",
            view_type="TREE",
            config_root=config_root,
        )

        payload = GENERATOR.tree_json(tree, views={tree.code: tree})

        self.assertEqual("sample_tree_layout", payload["layoutCode"])
        self.assertEqual("sample.root", payload["rootName"])
        self.assertEqual("name", payload["treeModelCode"])
        self.assertIs(payload["dragOpen"], True)
        self.assertIs(payload["canMemory"], True)
        self.assertIs(payload["canDrag"], False)

    def test_permission_button_includes_platform_power_code(self):
        operation_code = "inputStanList_add_add_PATROL_1.0.0_inputStandard_inputStanList"
        button = ET.fromstring(
            "<list-item>"
            "<id>add</id>"
            "<showname>新增</showname>"
            "<operatetype>ADD</operatetype>"
            "<ispermission>true</ispermission>"
            "<isconfirm>false</isconfirm>"
            "<iscallback>true</iscallback>"
            "<iscustomfunc>false</iscustomfunc>"
            "<useInMore>false</useInMore>"
            f"<buttonoperationcode>{operation_code}</buttonoperationcode>"
            "</list-item>"
        )

        payload = GENERATOR.button_payload(button)

        self.assertIsNotNone(payload)
        self.assertEqual(
            "__pc__=aW5wdXRTdGFuTGlzdF9hZGRfYWRkX1BBVFJPTF8xLjAuMF9pbnB1dFN0YW5kYXJkX2lucHV0U3Rhbkxpc3R8",
            payload["pc"],
        )
        self.assertIs(payload["isconfirm"], False)
        self.assertIs(payload["iscallback"], True)
        self.assertIs(payload["iscustomfunc"], False)
        self.assertIs(payload["useInMore"], False)
        self.assertIs(payload["USEINMORE"], False)

    def test_unrestricted_button_does_not_receive_power_code(self):
        button = ET.fromstring(
            "<list-item>"
            "<id>refresh</id>"
            "<showname>刷新</showname>"
            "<operatetype>CUSTOM</operatetype>"
            "<ispermission>false</ispermission>"
            "<buttonoperationcode>sample_refresh</buttonoperationcode>"
            "</list-item>"
        )

        payload = GENERATOR.button_payload(button)

        self.assertIsNotNone(payload)
        self.assertNotIn("pc", payload)

    def test_truncated_button_function_name_is_repaired(self):
        button = ET.fromstring(
            "<list-item>"
            "<id>bulkSubmit</id>"
            "<showname>批量提交</showname>"
            "<operatetype>CUSTOM</operatetype>"
            "<funcname>onclick='nction bulkSubmitManu()'</funcname>"
            "<buttonoperationcode>qcs_bulk_submit</buttonoperationcode>"
            "</list-item>"
        )

        payload = GENERATOR.button_payload(button)

        self.assertEqual("bulkSubmitManu()", payload["onclick"])
        self.assertEqual("onclick='bulkSubmitManu()'", payload["funcname"])

    def test_qcs_packaged_list_supplements_missing_source_button(self):
        config_root = ET.fromstring(
            "<config><layout><sections><list><list-item>"
            "<regionType>BUTTON</regionType>"
            "<id>delete</id>"
            "<showname>删除</showname>"
            "<operatetype>DELETE</operatetype>"
            "<buttonoperationcode>qcs_delete</buttonoperationcode>"
            "</list-item></list></sections></layout></config>"
        )
        view = view_variant(
            code="QCS_5.0.0.0_inspect_manuInspectList",
            config_root=config_root,
        )
        payload = {
            "components": [
                {
                    "type": "layoutDatagrid",
                    "buttons": [{"id": "open", "buttonoperationcode": "qcs_open"}],
                }
            ]
        }

        GENERATOR.supplement_packaged_datagrid_buttons(
            view,
            payload,
            {view.code: view},
        )

        self.assertEqual(
            ["open", "delete"],
            [button["id"] for button in payload["components"][0]["buttons"]],
        )

    def test_qcs_other_inspection_restores_complete_runtime_actions(self):
        view_code = "QCS_5.0.0.0_inspect_otherInspectList"
        edit_code = "QCS_5.0.0.0_inspect_otherInspectEdit"
        view = view_variant(code=view_code)
        edit_view = view_variant(
            code=edit_code,
            title="其他检验申请",
            name="otherInspectEdit",
            open_type="frame",
            url="/msService/QCS/inspect/inspect/otherInspectEdit",
        )
        payload = {
            "components": [
                {
                    "type": "layoutDatagrid",
                    "buttons": [
                        {
                            "id": "open",
                            "showname": "打开",
                            "buttonoperationcode": "legacy_open",
                        },
                        {
                            "id": "delete",
                            "showname": "删除",
                            "buttonoperationcode": "legacy_delete",
                        },
                    ],
                }
            ]
        }

        GENERATOR.apply_qcs_secondary_list_actions(
            view,
            payload,
            {view_code: view, edit_code: edit_view},
        )

        buttons = payload["components"][0]["buttons"]
        self.assertEqual(
            ["manualAdd", "open", "close", "bulkSubmit", "delete"],
            [button["id"] for button in buttons],
        )
        self.assertEqual(edit_code, buttons[0]["viewselect"]["code"])
        self.assertEqual("CUSTOM", buttons[0]["operatetype"])
        self.assertFalse(buttons[0]["iscallback"])
        self.assertEqual(
            "QCS_5.0.0.0_inspect_Inspect", buttons[0]["modelCode"]
        )
        self.assertIn("/msService/QCS/inspect/inspect/otherInspectEdit", buttons[0]["funcbody"])
        self.assertIn(
            "QCS_5.0.0.0_inspect_otherInspectList_inspect_sdg",
            buttons[1]["funcbody"],
        )
        self.assertEqual("批量提交", buttons[3]["showname"])
        self.assertEqual("删除", buttons[4]["showname"])

    def test_qcs_quality_inspection_does_not_call_other_inspection_grid(self):
        view_code = "QCS_5.0.0.0_inspect_qualityInspectList"
        edit_code = "QCS_5.0.0.0_inspect_qualityInspectEdit"
        view = view_variant(code=view_code)
        edit_view = view_variant(
            code=edit_code,
            title="质量巡检申请",
            name="qualityInspectEdit",
            open_type="frame",
            url="/msService/QCS/inspect/inspect/qualityInspectEdit",
        )
        payload = {
            "components": [
                {
                    "type": "layoutDatagrid",
                    "buttons": [
                        {"id": "open", "showname": "默认操作"},
                        {"id": "close", "showname": "默认操作"},
                        {"id": "delete", "showname": "默认操作"},
                    ],
                }
            ]
        }

        GENERATOR.apply_qcs_secondary_list_actions(
            view,
            payload,
            {view_code: view, edit_code: edit_view},
        )

        buttons = payload["components"][0]["buttons"]
        self.assertEqual(
            ["新增申请", "打开", "关闭", "批量提交", "删除"],
            [button["showname"] for button in buttons],
        )
        for button in buttons[1:4]:
            self.assertNotIn("otherInspectList", button.get("funcbody", ""))
        self.assertIn(
            "QCS_5.0.0.0_inspect_qualityInspectList_inspect_sdg",
            buttons[1]["funcbody"],
        )

    def test_qcs_secondary_report_delete_label_is_not_default_operation(self):
        view = view_variant(
            code="QCS_5.0.0.0_inspectReport_quaInspReportList"
        )
        payload = {
            "components": [
                {
                    "type": "layoutDatagrid",
                    "buttons": [{"id": "delete", "showname": "默认操作"}],
                }
            ]
        }

        GENERATOR.apply_qcs_secondary_list_actions(view, payload, {view.code: view})

        button = payload["components"][0]["buttons"][0]
        self.assertEqual("删除", button["showname"])
        self.assertEqual("删除", button["namekey"])

    def test_qcs_table_number_fallback_replaces_namekey_and_display_name(self):
        view = view_variant(
            code="QCS_5.0.0.0_unQlfDeal_otherUnQlfDealList"
        )
        payload = {
            "components": [
                {
                    "namekey": "ec.common.tableNo",
                    "element": {
                        "displayName": "ec.common.tableNo",
                        "title": "ec.list.taskDescription",
                    },
                }
            ]
        }

        GENERATOR.apply_view_runtime_overrides(view, payload)

        self.assertEqual("单据编号", payload["components"][0]["namekey"])
        self.assertEqual(
            "单据编号", payload["components"][0]["element"]["displayName"]
        )
        self.assertEqual(
            "待办说明", payload["components"][0]["element"]["title"]
        )

    def test_nested_layout_button_uses_parent_view_permission_code(self):
        operation_code = "sampleList_add_add_PATROL_1.0.0_sample_sampleList"
        parent_code = "PATROL_1.0.0_sample_sampleLayout"
        config_root = ET.fromstring(
            "<config><layout><sections><list><list-item>"
            "<regionType>BUTTON</regionType>"
            "<id>add</id>"
            "<showname>新增</showname>"
            "<operatetype>ADD</operatetype>"
            "<ispermission>true</ispermission>"
            "<modelCode>PATROL_1.0.0_sample_Sample</modelCode>"
            f"<buttonoperationcode>{operation_code}</buttonoperationcode>"
            "</list-item></list></sections></layout></config>"
        )
        view = view_variant(config_root=config_root)

        grid = GENERATOR.data_grid_json(view, parent_code, {view.code: view})
        button = grid["buttons"][0]
        expected_operation_code = f"{parent_code}_{operation_code}"

        self.assertEqual(operation_code, button["buttonoperationcode"])
        self.assertEqual(operation_code, button["CODE"])
        self.assertEqual("PATROL_1.0.0_sample_Sample", button["modelCode"])
        self.assertEqual(
            GENERATOR.button_power_code(expected_operation_code),
            button["pc"],
        )

    def test_custom_property_placeholder_is_preserved_for_runtime_expansion(self):
        field = GENERATOR.FieldDef(
            key="PATROL_custom_placeholder",
            namekey="PATROL_custom_placeholder",
            show_type="TEXTFIELD",
            show_format="TEXT",
            width=120,
            hidden=False,
            column_type="TEXT",
            model_code="PATROL_1.0.0_sample_Sample",
            fill=None,
            custom_section=True,
            custom_model_code="PATROL_1.0.0_sample_Sample",
            property_lay_rec="sample",
        )

        payload = GENERATOR.field_json(field)

        self.assertTrue(payload["customSection"])
        self.assertEqual("PATROL_1.0.0_sample_Sample", payload["customModelCode"])
        self.assertEqual("sample", payload["propertyLayRec"])

    def test_data_grid_omits_unmapped_custom_property_placeholder(self):
        normal_field = GENERATOR.FieldDef(
            key="name",
            namekey="Name",
            show_type="TEXTFIELD",
            show_format="TEXT",
            width=120,
            hidden=False,
            column_type="TEXT",
            model_code="PATROL_1.0.0_sample_Sample",
            fill=None,
        )
        custom_field = GENERATOR.FieldDef(
            key="PATROL_custom_placeholder",
            namekey="PATROL_custom_placeholder",
            show_type="TEXTFIELD",
            show_format="TEXT",
            width=120,
            hidden=False,
            column_type="TEXT",
            model_code="PATROL_1.0.0_sample_Sample",
            fill=None,
            custom_section=True,
            custom_model_code="PATROL_1.0.0_sample_Sample",
            property_lay_rec="sample",
        )

        with patch.object(GENERATOR, "extract_fields", return_value=[normal_field, custom_field]):
            payload = GENERATOR.data_grid_json(sample_view())

        self.assertEqual(["name"], [field["key"] for field in payload["fields"]])
        self.assertNotIn("customSection", str(payload))

    def test_data_grid_preserves_list_double_click_and_initialization_contract(self):
        config = ET.fromstring(
            "<config><layout><sections><list><list-item>"
            "<regionType>LISTPT</regionType>"
            "<listProperty>"
            "<isdbcustom>true</isdbcustom>"
            "<dbcustomtextarea>function openDetail(event,row){return row.id;}</dbcustomtextarea>"
            "<dbcustomtextarea_es5>function openDetail(event,row){return row.id;}</dbcustomtextarea_es5>"
            "<ptPageInit>function initGrid(){return true;}</ptPageInit>"
            "<isFirstLoad>false</isFirstLoad>"
            "<isCheckBox>true</isCheckBox>"
            "</listProperty>"
            "</list-item></list></sections></layout></config>"
        )
        view = view_variant(config_root=config)

        payload = GENERATOR.data_grid_json(view)

        self.assertIs(payload["isdbcustom"], True)
        self.assertIs(payload["isFirstLoad"], False)
        self.assertIs(payload["isCheckBox"], True)
        self.assertIn("openDetail", payload["dbcustomtextarea"])
        self.assertIn("initGrid", payload["ptPageInit"])

    def test_process_execution_list_overrides_inert_vendor_double_click(self):
        view = view_variant(
            code="WOM_1.0.0_produceTask_processExeLogList",
            url="/msService/WOM/produceTask/processExelog/processExeLogList",
        )
        packaged_payload = {
            "pageType": "LIST",
            "components": [
                {
                    "type": "layoutDatagrid",
                    "DataGridCode": view.code,
                    "isdbcustom": True,
                    "dbcustomtextarea_es5": "function processExeLogListDB(event,row){}",
                    "buttons": [
                        {
                            "id": "outptConsumption",
                            "showname": "产耗查看",
                            "buttonoperationcode": (
                                "processExeLogList_outptConsumption_add_"
                                "WOM_1.0.0_produceTask_processExeLogList"
                            ),
                            "isPublished": False,
                            "isHide": False,
                        },
                        {
                            "id": "matConsumEntry",
                            "showname": "产耗录入",
                            "buttonoperationcode": (
                                "processExeLogList_matConsumEntry_add_"
                                "WOM_1.0.0_produceTask_processExeLogList"
                            ),
                            "isPublished": False,
                            "isHide": True,
                        },
                    ],
                }
            ],
        }

        payload = json.loads(
            GENERATOR.view_json(view, {view.code: view}, {view.code: packaged_payload})
        )
        grid = payload["components"][0]

        self.assertIs(grid["isdbcustom"], True)
        self.assertIn("processExecutionId", grid["dbcustomtextarea_es5"])
        self.assertIn("encodeURIComponent(row.id)", grid["dbcustomtextarea"])
        self.assertNotIn("console.info", grid["dbcustomtextarea_es5"])
        visible_buttons = [
            button
            for button in grid["buttons"]
            if button.get("isPublished") and not button.get("isHide")
        ]
        self.assertEqual(
            ["viewDetail", "outptConsumption", "manualStatistics"],
            [button["id"] for button in visible_buttons],
        )
        self.assertEqual(
            ["查看详情", "产耗查看", "工艺统计"],
            [button["showname"] for button in visible_buttons],
        )
        self.assertTrue(all(button["ispermission"] is False for button in visible_buttons))
        button_by_id = {button["id"]: button for button in grid["buttons"]}
        self.assertIn(
            "/ProcessAnalysis/processAnalysis/processExecution/detail",
            button_by_id["viewDetail"]["funcbody_es5"],
        )
        self.assertIn(
            "/WOM/produceTask/processExelog/matConsuEntryProView",
            button_by_id["outptConsumption"]["funcbody_es5"],
        )
        self.assertIn(
            "/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess",
            button_by_id["manualStatistics"]["funcbody_es5"],
        )
        self.assertIn("needParamAna", button_by_id["manualStatistics"]["funcbody_es5"])
        self.assertIn(
            'runStateId !== "WOM_runState/finished"',
            button_by_id["manualStatistics"]["funcbody_es5"],
        )
        self.assertTrue(button_by_id["matConsumEntry"]["isHide"])
        self.assertFalse(button_by_id["matConsumEntry"]["isPublished"])

    def test_wom_execution_record_lists_restore_safe_actions_and_double_click(self):
        cases = {
            GENERATOR.TASK_EXECUTION_VIEW_CODE: {
                "url": "/msService/WOM/produceTask/prodTaskExelog/makeTaskExecuList",
                "button_ids": [
                    "outptConsumption",
                    "analysisView",
                    "batchReportPreview",
                    "manualStatistics",
                ],
                "button_names": ["产耗查看", "工艺查看", "批次追溯", "工艺统计"],
                "double_click_route": "matConsumEntryView",
                "detail_route": "matConsumEntryView",
                "source_buttons": [
                    {
                        "id": "manualStatistics",
                        "showname": "工艺统计",
                        "isPublished": True,
                        "isHide": False,
                        "ispermission": True,
                    },
                    {
                        "id": "analysisView",
                        "showname": "工艺查看",
                        "isPublished": True,
                        "isHide": False,
                        "ispermission": True,
                    },
                    {
                        "id": "batchReportPreview",
                        "showname": "批次报告",
                        "isPublished": True,
                        "isHide": False,
                        "ispermission": True,
                    },
                ],
            },
            GENERATOR.ACTIVITY_EXECUTION_VIEW_CODE: {
                "url": "/msService/WOM/produceTask/actiExelog/activeExeLogList",
                "button_ids": ["viewDetail", "manualStatistics"],
                "button_names": ["查看详情", "工艺统计"],
                "double_click_route": "matConsuEntryActView",
                "detail_route": "checkRecord",
                "source_buttons": [
                    {
                        "id": "manualStatistics",
                        "showname": "工艺统计",
                        "isPublished": True,
                        "isHide": False,
                        "ispermission": True,
                    },
                ],
            },
            GENERATOR.CHECK_RECORD_VIEW_CODE: {
                "url": "/msService/WOM/produceTask/checkRecord/checkRecordList",
                "button_ids": ["viewDetail"],
                "button_names": ["查看详情"],
                "double_click_route": "actiExelog/checkRecord",
                "detail_route": "actiExelog/checkRecord",
            },
            GENERATOR.MATERIAL_CONSUMPTION_RECORD_VIEW_CODE: {
                "url": "/msService/WOM/produceTask/matConsumRecod/matConsumRecodList",
                "button_ids": ["viewDetail"],
                "button_names": ["查看详情"],
                "double_click_route": "matConsumeRecordView",
                "detail_route": "matConsumeRecordView",
            },
            GENERATOR.MATERIAL_OUTPUT_RECORD_VIEW_CODE: {
                "url": "/msService/WOM/produceTask/matOutptRecord/matOutptRecordList",
                "button_ids": ["viewDetail"],
                "button_names": ["查看详情"],
                "double_click_route": "matOutputRecordView",
                "detail_route": "matOutputRecordView",
            },
        }

        for code, expected in cases.items():
            with self.subTest(view_code=code):
                view = view_variant(code=code, url=expected["url"])
                packaged_payload = {
                    "pageType": "LIST",
                    "components": [
                        {
                            "type": "layoutDatagrid",
                            "DataGridCode": code,
                            "buttons": [
                                *expected.get("source_buttons", []),
                                {
                                    "id": "vendorHidden",
                                    "showname": "旧包隐藏动作",
                                    "buttonoperationcode": f"{code}_vendorHidden",
                                    "isPublished": False,
                                    "isHide": True,
                                }
                            ],
                        }
                    ],
                }

                payload = json.loads(
                    GENERATOR.view_json(
                        view,
                        {view.code: view},
                        {view.code: packaged_payload},
                    )
                )
                grid = payload["components"][0]
                visible_buttons = [
                    button
                    for button in grid["buttons"]
                    if button.get("isPublished") and not button.get("isHide")
                ]

                self.assertEqual(
                    expected["button_ids"],
                    [button["id"] for button in visible_buttons],
                )
                self.assertEqual(
                    expected["button_names"],
                    [button["showname"] for button in visible_buttons],
                )
                self.assertIs(grid["isdbcustom"], True)
                self.assertIn(
                    expected["double_click_route"],
                    grid["dbcustomtextarea_es5"],
                )
                self.assertIn(
                    "encodeURIComponent",
                    grid["dbcustomtextarea_es5"],
                )
                self.assertIn(
                    expected["detail_route"],
                    visible_buttons[0]["funcbody_es5"],
                )
                self.assertTrue(grid["buttons"][-1]["isHide"])
                self.assertFalse(grid["buttons"][-1]["isPublished"])

        task_buttons = GENERATOR.WOM_RECORD_ACTION_BUTTONS[
            GENERATOR.TASK_EXECUTION_VIEW_CODE
        ]
        self.assertFalse(task_buttons[0]["ispermission"])
        self.assertFalse(task_buttons[1]["ispermission"])
        self.assertFalse(task_buttons[2]["ispermission"])
        self.assertEqual(
            GENERATOR.button_power_code(
                "WOM_1.0.0_produceTask_makeTaskExecuList_self"
            ),
            task_buttons[0]["pc"],
        )
        self.assertIn(
            "/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut",
            task_buttons[2]["funcbody_es5"],
        )
        self.assertIn("productNo", task_buttons[2]["funcbody_es5"])
        activity_buttons = GENERATOR.WOM_RECORD_ACTION_BUTTONS[
            GENERATOR.ACTIVITY_EXECUTION_VIEW_CODE
        ]
        self.assertFalse(activity_buttons[0]["ispermission"])
        self.assertTrue(activity_buttons[1]["ispermission"])
        self.assertEqual(
            "cell_1600247412569_8869",
            activity_buttons[1]["cellCode"],
        )
        self.assertEqual(
            GENERATOR.button_power_code(
                "activeExeLogList_manualStatistics_add_"
                "WOM_1.0.0_produceTask_activeExeLogList"
            ),
            activity_buttons[1]["pc"],
        )
        self.assertIn(
            "/ProcessAnalysis/paramStatRec/paramStatRec/manualStatActive",
            activity_buttons[1]["funcbody_es5"],
        )
        self.assertNotIn(
            "/ProcessAnalysis/paramDetail/paramDetail/analysisiActive",
            activity_buttons[1]["funcbody_es5"],
        )
        for detail_view_code in (
            "WOM_1.0.0_produceTask_matConsumEntryView",
            "WOM_1.0.0_produceTask_matConsuEntryActView",
            "WOM_1.0.0_produceTask_matOutputActView",
            "WOM_1.0.0_produceTask_checkRecord",
            "WOM_1.0.0_produceTask_matConsumeRecordView",
            "WOM_1.0.0_produceTask_matOutputRecordView",
        ):
            self.assertIn(detail_view_code, GENERATOR.TARGET_VIEW_CODES)

    def test_incompatible_wom_action_views_are_regenerated_from_module_xml(self):
        packaged_payload = {
            "pageType": "EDIT",
            "legacyRuntimeMarker": "must-not-survive",
            "components": [{"type": "legacy-layout"}],
        }

        for view_code in GENERATOR.COMPAT_REGENERATED_ACTION_VIEW_CODES:
            with self.subTest(view_code=view_code):
                view = view_variant(
                    code=view_code,
                    view_type="VIEW",
                    show_type="SINGLE",
                    title="Compatible detail",
                )
                payload = json.loads(
                    GENERATOR.view_json(
                        view,
                        {view.code: view},
                        {view.code: packaged_payload},
                    )
                )

                self.assertNotIn("legacyRuntimeMarker", payload)
                self.assertEqual("EDIT", payload["pageType"])
                self.assertTrue(payload["isMain"])
                self.assertEqual("layout", payload["components"][0]["type"])

    def test_runtime_extra_view_oid_upsert_reuses_unchanged_large_object(self):
        view = view_variant()

        migration = GENERATOR.runtime_extra_view_sql(view, {view.code: view})

        self.assertIn(
            "SELECT view_json INTO runtime_extra_view_existing_oid",
            migration,
        )
        self.assertIn(
            "convert_from(lo_get(runtime_extra_view_existing_oid), 'UTF8') = "
            "runtime_extra_view_payload",
            migration,
        )
        self.assertIn(
            "runtime_extra_view_target_oid := runtime_extra_view_existing_oid",
            migration,
        )
        self.assertIn(
            "runtime_extra_view_target_oid := lo_from_bytea",
            migration,
        )
        self.assertIn(
            "runtime_extra_view_target_oid, false)",
            migration,
        )

    def test_make_task_runtime_migration_restores_business_tabs_and_grid_columns(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "210-wom-make-task-edit-full-runtime.sql"
        ).read_text(encoding="utf-8")

        for tab_name in ("工序活动", "用料汇总", "检验清单"):
            self.assertIn(f'"name":"{tab_name}"', migration)
        for model_code in (
            "WOM_1.0.0_produceTask_TaskProcess",
            "WOM_1.0.0_produceTask_TaskActive",
            "WOM_1.0.0_produceTask_TaskMaterial",
            "WOM_1.0.0_produceTask_TaskQuality",
        ):
            self.assertIn(f'"modelCode":"{model_code}"', migration)
        for field_key in (
            '"key":"taskProcessId.name"',
            '"key":"materialId.code"',
            '"key":"qualityStdId.name"',
        ):
            self.assertIn(field_key, migration)
        self.assertIn("restoreMakeTaskBusinessTabs", migration)
        self.assertIn(r'ReactAPI.Layout.showTab(\"tabs-3\")', migration)
        self.assertNotIn("只显示常规信息页签", migration)

    def test_factory_edit_runtime_migration_restores_create_form(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "212-hierarchicalmod-factory-edit-runtime.sql"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "HierarchicalMod_1.0.0_factoryModel_factoryEdit",
            GENERATOR.TARGET_VIEW_CODES,
        )
        self.assertEqual(migration.count('"pageType":"EDIT"'), 2)
        for field_key in (
            '"key":"factoryModel.code"',
            '"key":"factoryModel.name"',
            '"key":"factoryModel.nodeTypeId.name"',
        ):
            self.assertIn(field_key, migration)
        self.assertIn("HierarchicalMod_1.0.0_factoryNodeType_nodeTypeRef", migration)
        self.assertIn("getParamsInRequestUrl().parentId", migration)
        self.assertIn('"namekey":"装置","name":"装置"', migration)
        self.assertNotIn(
            '"namekey":"HierarchicalMod.tabname.randon1618564480544.flag"',
            migration,
        )
        self.assertIn("INSERT INTO public.runtime_extra_view", migration)
        self.assertIn("INSERT INTO public.ec_extra_view", migration)

    def test_factory_tree_runtime_migration_restores_node_title_mapping(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "215-hierarchicalmod-factory-tree-runtime.sql"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "HierarchicalMod_1.0.0_factoryModel_factoryTreeList",
            GENERATOR.TARGET_VIEW_CODES,
        )
        self.assertIn(
            "-- HierarchicalMod_1.0.0_factoryModel_factoryTree ",
            migration,
        )
        self.assertIn(
            "-- HierarchicalMod_1.0.0_factoryModel_factoryTreeList ",
            migration,
        )
        self.assertGreaterEqual(migration.count('"treeModelCode":"name"'), 4)
        self.assertGreaterEqual(
            migration.count(
                '"DataGridCode":"HierarchicalMod_1.0.0_factoryModel_factoryListPart"'
            ),
            2,
        )
        self.assertIn('"showname":"新增"', migration)
        self.assertIn('"id":"addNode"', migration)
        self.assertIn(
            '"buttonoperationcode":"factoryListPart_addNode_add_'
            'HierarchicalMod_1.0.0_factoryModel_factoryListPart"',
            migration,
        )
        self.assertIn(
            '"modelCode":"HierarchicalMod_1.0.0_factoryModel_FactoryModel"',
            migration,
        )
        expected_permission_code = (
            "HierarchicalMod_1.0.0_factoryModel_factoryTreeList_"
            "factoryListPart_addNode_add_"
            "HierarchicalMod_1.0.0_factoryModel_factoryListPart"
        )
        self.assertIn(
            f'"pc":"{GENERATOR.button_power_code(expected_permission_code)}"',
            migration,
        )
        self.assertNotIn(
            f'"pc":"{GENERATOR.button_power_code("factoryListPart_addNode_add_HierarchicalMod_1.0.0_factoryModel_factoryListPart")}"',
            migration,
        )
        self.assertIn(
            '"rootName":"HierarchicalMod.HierarchicalMod_100_factoryModel_factoryTree.'
            'randon1573030638717.flag"',
            migration,
        )

    def test_factory_node_type_reference_runtime_migration_restores_picker(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "213-hierarchicalmod-factory-node-type-ref-runtime.sql"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "HierarchicalMod_1.0.0_factoryNodeType_nodeTypeRef",
            GENERATOR.TARGET_VIEW_CODES,
        )
        self.assertEqual(migration.count('"pageType":"LIST"'), 2)
        for field_key in (
            '"key":"code"',
            '"key":"name"',
        ):
            self.assertIn(field_key, migration)
        self.assertIn("INSERT INTO public.runtime_extra_view", migration)
        self.assertIn("INSERT INTO public.ec_extra_view", migration)

    def test_qcs_incoming_reference_runtime_migration_restores_optional_pickers(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "246-qcs-incoming-inspect-reference-runtime.sql"
        ).read_text(encoding="utf-8")

        expected_views = (
            "BaseSet_1.0.0_cooperateClass_cmcClassTreeRef",
            "BaseSet_1.0.0_cooperate_cmcPartRef",
            "BaseSet_1.0.0_cooperate_cmcLayoutRef",
            "LIMSBasic_1.0.0_pickSite_pickSiteTreeRef",
            "LIMSBasic_1.0.0_pickSite_pickSiteRefPart",
            "LIMSBasic_1.0.0_pickSite_pickSiteRefLayout",
        )
        for view_code in expected_views:
            self.assertIn(f"-- {view_code} ", migration)

        for target_view_code in (
            "BaseSet_1.0.0_cooperate_cmcLayoutRef",
            "LIMSBasic_1.0.0_pickSite_pickSiteRefLayout",
        ):
            self.assertIn(target_view_code, GENERATOR.TARGET_VIEW_CODES)

        # Each view emits one insert path for a new LOB and one for an existing LOB.
        self.assertEqual(12, migration.count("INSERT INTO public.runtime_extra_view"))
        self.assertEqual(6, migration.count("INSERT INTO public.ec_extra_view"))
        self.assertNotIn("Oracle", migration)

    def test_factory_node_type_customer_condition_registration_is_idempotent(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "216-hierarchicalmod-factory-node-type-customer-condition.sql"
        ).read_text(encoding="utf-8")

        view_code = "HierarchicalMod_1.0.0_factoryNodeType_nodeTypeRef"
        self.assertEqual(4, migration.count(view_code))
        self.assertIn("INSERT INTO public.runtime_customer_condition", migration)
        self.assertIn("INSERT INTO public.ec_customer_condition", migration)
        self.assertEqual(2, migration.count("ON CONFLICT (code) DO UPDATE SET"))
        self.assertNotIn("runtime_extra_view", migration)
        self.assertNotIn("Oracle", migration)

    def test_rm_formula_menu_action_runtime_migration_restores_original_toolbars(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "241-rm-formula-menu-actions-runtime.sql"
        ).read_text(encoding="utf-8")

        expected_views = (
            "RM_1.0.0_formulaType_formualTypeTreeList",
            "RM_1.0.0_formulaType_formualTypeEdit",
            "RM_1.0.0_processType_processTypeList",
            "RM_1.0.0_processType_processTypeEdit",
            "RM_1.0.0_formulaBOM_formulaBomList",
            "RM_1.0.0_formulaBOM_formulaBomEdit",
            "RM_1.0.0_formula_commonFormulaList",
            "RM_1.0.0_formula_easyFormulaList",
            "RM_1.0.0_formula_craftFormulaList",
            "RM_1.0.0_formula_copyFormulaEdit",
            "RM_1.0.0_formula_qualityDepartEdit",
            "RM_1.0.0_formula_departEasyEdit",
            "RM_1.0.0_formula_arrSuitlineEdit",
            "RM_1.0.0_formula_easyArrSuitlineEdit",
        )
        for view_code in expected_views:
            self.assertIn(view_code, GENERATOR.TARGET_VIEW_CODES)
            self.assertIn(f"-- {view_code} ", migration)

        for button_name in (
            "新增",
            "修改",
            "删除",
            "复制",
            "启用",
            "停用",
            "设置默认",
            "设置检验部门",
            "适用产线",
        ):
            self.assertIn(f'"showname":"{button_name}"', migration)

        self.assertIn(
            '"buttonoperationcode":"formulaBomList_add_add_'
            'RM_1.0.0_formulaBOM_formulaBomList"',
            migration,
        )
        for editor_field in (
            '"key":"formulaType.code"',
            '"key":"processType.code"',
            '"key":"formulaBomMain.bomCode"',
        ):
            self.assertIn(editor_field, migration)
        self.assertNotIn("-- RM_1.0.0_formula_batchFormulaList ", migration)
        self.assertNotIn('"id":"rmFormulaWebEditor"', migration)

    def test_make_task_edit_restores_business_tabs_after_form_data_is_ready(self):
        view = view_variant(code="WOM_1.0.0_produceTask_makeTaskEdit")
        packaged_payload = {
            "pageType": "EDIT",
            "events": [
                {
                    "name": "onload",
                    "function": """let formData = ReactAPI.getFormData();
if (!formData.id || !formData.formulaId || !formData.planNum) {
    // 只显示常规信息页签
    ReactAPI.Layout.hideTab('tabs-3');
}
// 如果配方已经同步过
ReactAPI.Layout.hideTab('tabs-5');""",
                },
                {
                    "name": "onload_es5",
                    "function": """var formData = ReactAPI.getFormData();
if (!formData.id || !formData.formulaId || !formData.planNum) {
    // 只显示常规信息页签
    ReactAPI.Layout.hideTab('tabs-3');
}
// 如果配方已经同步过
ReactAPI.Layout.hideTab('tabs-5');""",
                },
            ],
        }

        payload = json.loads(
            GENERATOR.view_json(view, {view.code: view}, {view.code: packaged_payload})
        )

        for event in payload["events"]:
            self.assertIn("restoreMakeTaskBusinessTabs", event["function"])
            self.assertIn('ReactAPI.Layout.showTab("tabs-3")', event["function"])
            self.assertIn("formData.planNum != null", event["function"])
            self.assertIn("if (attempt < 50)", event["function"])
            self.assertNotIn("只显示常规信息页签", event["function"])
            self.assertIn("// 如果配方已经同步过", event["function"])
            self.assertLess(
                event["function"].index("restoreMakeTaskBusinessTabs"),
                event["function"].index("ReactAPI.Layout.hideTab"),
            )

    def test_packaged_view_json_preserves_complex_layout_and_normalizes_runtime_values(self):
        operation_code = "workGroupList_add_add_PATROL_1.0.0_patrolRoute_workGroupList"
        packaged_payload = {
            "pageType": "EDIT",
            "components": [
                {
                    "type": "layoutSection",
                    "components": [
                        {"key": "name", "nullable": "false"},
                        {"key": "tenantField", "customSection": "true"},
                    ],
                    "buttons": [
                        {
                            "buttonoperationcode": operation_code,
                            "showname": "新增",
                            "ispermission": "true",
                            "useInMore": "false",
                            "operatetype": "ADD",
                        }
                    ],
                }
            ],
        }

        payload = json.loads(
            GENERATOR.view_json(
                sample_view(),
                {sample_view().code: sample_view()},
                {sample_view().code: packaged_payload},
            )
        )

        self.assertEqual("EDIT", payload["pageType"])
        self.assertEqual("layoutSection", payload["components"][0]["type"])
        self.assertEqual(["name"], [item["key"] for item in payload["components"][0]["components"]])
        self.assertIs(payload["components"][0]["components"][0]["nullable"], False)
        button = payload["components"][0]["buttons"][0]
        self.assertEqual(operation_code, button["CODE"])
        self.assertEqual(GENERATOR.button_power_code(operation_code), button["pc"])
        self.assertEqual("新增", button["NAME"])
        self.assertEqual("cui-btn-add", button["ICONCLS"])

    def test_packaged_view_json_preserves_parent_qualified_button_permission(self):
        operation_code = "factoryListPart_addNode_add_sample"
        parent_operation_code = "sample_parent_" + operation_code
        packaged_permission = GENERATOR.button_power_code(parent_operation_code)
        packaged_payload = {
            "pageType": "LIST",
            "components": [
                {
                    "type": "layoutDatagrid",
                    "buttons": [
                        {
                            "buttonoperationcode": operation_code,
                            "showname": "新增",
                            "ispermission": True,
                            "modelCode": "sample_model",
                            "pc": packaged_permission,
                        }
                    ],
                }
            ],
        }

        payload = json.loads(
            GENERATOR.view_json(
                sample_view(),
                {sample_view().code: sample_view()},
                {sample_view().code: packaged_payload},
            )
        )
        button = payload["components"][0]["buttons"][0]

        self.assertEqual(operation_code, button["CODE"])
        self.assertEqual("sample_model", button["modelCode"])
        self.assertEqual(packaged_permission, button["pc"])
        self.assertIs(button["USEINMORE"], False)

    def test_packaged_view_loader_prefers_highest_module_version(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            modules_root = Path(temp_dir)
            for version, page_type in (("6.0.3.0", "LIST"), ("6.0.4.0", "EDIT")):
                metadata_path = modules_root / f"PATROL_{version}" / "META-INF" / "init" / "metadata.json"
                metadata_path.parent.mkdir(parents=True)
                metadata_path.write_text(
                    json.dumps(
                        [
                            {
                                "tableName": "runtime_extra_view",
                                "metadata": [
                                    {
                                        "CODE": sample_view().code,
                                        "VERSION": "1",
                                        "VIEW_JSON": json.dumps({"pageType": page_type}),
                                    }
                                ],
                            }
                        ]
                    ),
                    encoding="utf-8",
                )

            payloads = GENERATOR.load_packaged_view_json(modules_root)

        self.assertEqual("EDIT", payloads[sample_view().code]["pageType"])

    def test_packaged_data_grid_metadata_emits_runtime_and_product_rows(self):
        spec = next(
            item for item in GENERATOR.PACKAGED_UI_TABLE_SPECS
            if item.runtime_table == "runtime_data_grid"
        )
        row = {
            "CODE": "PATROL_1.0.0_sample_sampleListdg1",
            "VERSION": 1,
            "MODIFY_TIME": 1619618559316,
            "VALID": 1,
            "MODULE_CODE": "PATROL_1.0.0",
            "ENTITY_CODE": "PATROL_1.0.0_sample",
            "DATA_GRID_JSON": json.dumps(
                {
                    "type": "layoutDatagrid",
                    "fields": [
                        {"key": "name", "nullable": "false"},
                        {"key": "tenant", "customSection": "true"},
                    ],
                }
            ),
            "DATA_GRID_TYPE": 0,
            "VIEW_CODE": sample_view().code,
            "NAME": "dg1",
        }

        runtime_sql = GENERATOR.packaged_ui_row_sql(spec, row, runtime=True)
        product_sql = GENERATOR.packaged_ui_row_sql(spec, row, runtime=False)

        self.assertIn("INSERT INTO public.runtime_data_grid", runtime_sql)
        self.assertIn("lo_from_bytea(0, convert_to(", runtime_sql)
        self.assertIn("to_timestamp(1619618559316 / 1000.0)", runtime_sql)
        self.assertIn("INSERT INTO public.ec_data_grid", product_sql)
        self.assertNotIn("lo_from_bytea", product_sql)
        self.assertNotIn("customSection", runtime_sql)
        self.assertIn('"nullable":false', runtime_sql)

    def test_packaged_ui_generator_filters_requested_module(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            modules_root = Path(temp_dir)
            metadata_path = modules_root / "PATROL_6.0.4.0" / "META-INF" / "init" / "metadata.json"
            metadata_path.parent.mkdir(parents=True)
            metadata_path.write_text(
                json.dumps(
                    [
                        {
                            "tableName": "runtime_data_grid",
                            "metadata": [
                                {
                                    "CODE": "PATROL_1.0.0_sample_sampleListdg1",
                                    "MODULE_CODE": "PATROL_1.0.0",
                                    "DATA_GRID_JSON": "{}",
                                },
                                {
                                    "CODE": "EAM_1.0.0_sample_sampleListdg1",
                                    "MODULE_CODE": "EAM_1.0.0",
                                    "DATA_GRID_JSON": "{}",
                                },
                            ],
                        }
                    ]
                ),
                encoding="utf-8",
            )

            sql = GENERATOR.generate_packaged_ui_runtime_sql(modules_root, ("PATROL_1.0.0",))

        self.assertIn("-- runtime_data_grid: 1 packaged rows", sql)
        self.assertIn("PATROL_1.0.0_sample_sampleListdg1", sql)
        self.assertNotIn("EAM_1.0.0_sample_sampleListdg1", sql)

    def test_full_generator_wires_product_and_runtime_layers(self):
        source = Path(GENERATOR.__file__).read_text(encoding="utf-8")

        self.assertIn("lines.append(ec_model_sql(model))", source)
        self.assertIn("lines.append(ec_property_sql(prop))", source)
        self.assertIn("lines.append(ec_view_sql(view))", source)
        self.assertIn("lines.append(action_view_sql(view))", source)
        self.assertIn("lines.append(ec_extra_view_sql(view, views, packaged_view_json))", source)

    def test_wts_basic_settings_runtime_migration_covers_all_action_views(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "230-wts-basic-settings-action-runtime-json.sql"
        ).read_text(encoding="utf-8")

        self.assertIn("-- target_view_count: 46", migration)
        self.assertIn("-- emitted_view_count: 46", migration)
        self.assertEqual(
            92,
            migration.count("INSERT INTO public.runtime_extra_view"),
        )
        self.assertEqual(
            46,
            migration.count("INSERT INTO public.ec_extra_view"),
        )
        for view_code in (
            "WTS_1.0.0_workRegion_workRegionList",
            "WTS_1.0.0_qualifiSet_qualifyList",
            "WTS_1.0.0_hazidLib_hazidLibList",
            "WTS_1.0.0_riskSafeMeasures_riskSafeyLisit",
            "WTS_1.0.0_gasAnalysis_gasAnalysisList",
            "WTS_1.0.0_checkCriteriaLib_checkCriteriaList",
            "WTS_1.0.0_hourLimit_hourLimitList",
            "WTS_1.0.0_wfGasAnalysis_wfGasAnalysisList",
            "WTS_1.0.0_approveConfig_approveConfigList",
            "WTS_1.0.0_wfPathFunction_wfPathFuncList",
            "WTS_1.0.0_customizedConfig_listFilterList",
        ):
            self.assertIn(view_code, migration)
        for edit_view_code in (
            "WTS_1.0.0_workRegion_workRegionEdit",
            "WTS_1.0.0_hourLimit_hourLimitEdit",
            "WTS_1.0.0_hazidLib_hazidLibEdit",
        ):
            self.assertIn(edit_view_code, migration)
        self.assertIn('"showname":"新增"', migration)
        self.assertIn(
            "hazidLibList_add_add_WTS_1.0.0_hazidLib_hazidLibList",
            migration,
        )
        self.assertIn(
            "hazidLibList_modify_modify_WTS_1.0.0_hazidLib_hazidLibList",
            migration,
        )

    def test_qualify_runtime_migration_restores_module_action_views(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "235-qualify-action-runtime-json.sql"
        ).read_text(encoding="utf-8")

        self.assertIn("-- target_view_count: 41", migration)
        self.assertIn("-- requested_module_codes: Qualify_1.0.0", migration)
        self.assertIn("-- emitted_view_count: 41", migration)
        self.assertEqual(82, migration.count("INSERT INTO public.runtime_extra_view"))
        self.assertEqual(41, migration.count("INSERT INTO public.ec_extra_view"))
        for view_code in (
            "Qualify_1.0.0_reminderSet_reminderSetLayout",
            "Qualify_1.0.0_reminderSet_reminderSetList",
            "Qualify_1.0.0_certificateType_cerTypeLayOut",
            "Qualify_1.0.0_certificateType_cerTypeList",
            "Qualify_1.0.0_certificate_certifcateLayOut",
            "Qualify_1.0.0_certificate_certificateList",
            "Qualify_1.0.0_companyCertificateImport_companyCertImpList",
            "Qualify_1.0.0_staffCertificate_certStaffList",
            "Qualify_1.0.0_companyCertificate_certCompanyList",
            "Qualify_1.0.0_reminderPend_reminderPendList",
            "Qualify_1.0.0_staffCertificateImport_staffCertImpList",
        ):
            self.assertIn(view_code, migration)
        for operation_code in (
            "certificateList_addCertificate_add_Qualify_1.0.0_certificate_certificateList",
            "certificateList_delCertificate_del_Qualify_1.0.0_certificate_certificateList",
            "cerTypeList_add_add_Qualify_1.0.0_certificateType_cerTypeList",
            "reminderSetList_customAdd_add_Qualify_1.0.0_reminderSet_reminderSetList",
            "certStaffList_staffCertSet_add_Qualify_1.0.0_staffCertificate_certStaffList",
            "certCompanyList_companyCertSet_add_Qualify_1.0.0_companyCertificate_certCompanyList",
            "reminderPendList_proccess_add_Qualify_1.0.0_reminderPend_reminderPendList",
        ):
            self.assertIn(operation_code, migration)

    def test_qualify_custom_assets_are_served_before_generic_fallbacks(self):
        docker_root = SCRIPT_PATH.parent.parent
        static_root = docker_root / "assets" / "module-static" / "Qualify"
        for relative_path in (
            "certificate/certificate/certificateList/eventJs/customEvent.js",
            "certificate/certificate/certifcateLayOut/i18n-value.js",
            "certificateType/certificateType/cerTypeLayOut/body.js",
            "certificateType/certificateType/cerTypeList/eventJs/customEvent.js",
            "reminderSet/reminderSet/reminderSetList/eventJs/customEvent.js",
            "staffCertificate/certStaff/certStaffList/eventJs/customEvent.js",
            "companyCertificate/certCompany/certCompanyList/eventJs/customEvent.js",
            "reminderPend/reminderPend/reminderPendList/eventJs/customEvent.js",
        ):
            self.assertTrue((static_root / relative_path).is_file(), relative_path)

        nginx_config = (docker_root / "nginx" / "adp.conf").read_text(encoding="utf-8")
        qualify_location = "location ^~ /greenDill/static/Qualify/"
        generic_js_location = r"location ~* \.(?:js|css)$"
        self.assertIn(qualify_location, nginx_config)
        self.assertLess(
            nginx_config.index(qualify_location),
            nginx_config.index(generic_js_location),
        )
        self.assertIn(
            "alias /usr/share/nginx/module-static/Qualify/;",
            nginx_config,
        )
        self.assertIn(
            "location = /greenDill/static/scripts/treeList.js",
            nginx_config,
        )
        self.assertIn(
            """sub_filter 'else console.error("请将参数放在数组里!")' 'else resolve()';""",
            nginx_config,
        )

        qualify_i18n_files = sorted(static_root.rglob("i18n-value.js"))
        self.assertEqual(11, len(qualify_i18n_files))
        for i18n_file in qualify_i18n_files:
            i18n = i18n_file.read_text(encoding="utf-8")
            for resource in (
                'window.InternationalResource["Button.text.save"] = "保存";',
                'window.InternationalResource["Button.text.cancel"] = "取消";',
                'window.InternationalResource["SupDatagrid.button.delete"] = "确定删除吗？";',
                'window.InternationalResource["ec.common.confirm"] = "确定";',
                'window.InternationalResource["Qualify.viewtitle.randon1569401770550"] = "资质分类";',
                'window.InternationalResource["Qualify.viewtitle.randon1569416513467"] = "资质";',
            ):
                self.assertIn(resource, i18n, str(i18n_file))

    def test_qualify_root_category_migration_preserves_original_product_roots(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "236-qualify-certificate-type-roots.sql"
        ).read_text(encoding="utf-8")

        self.assertIn("(1000, 0, true, 1000", migration)
        self.assertIn("'人员资质', 'staffCert'", migration)
        self.assertIn("(1001, 0, true, 1000", migration)
        self.assertIn("'企业资质', 'companyCert'", migration)
        self.assertIn("ON CONFLICT (id) DO UPDATE", migration)
        self.assertIn("IF root_count <> 2", migration)
        self.assertNotIn("DELETE FROM", migration)

    def test_qualify_certificate_type_mne_code_migration_syncs_all_aliases(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "237-qualify-certificate-type-mne-code-compat.sql"
        ).read_text(encoding="utf-8")

        for column_name in (
            "certificate_type",
            "certificate_type_id",
            "certificatetype_id",
            "cer_type",
            "mne_code",
            "mnecode",
        ):
            self.assertIn(column_name, migration)
        self.assertIn(
            "CREATE OR REPLACE FUNCTION public.sync_qlf_certificate_types_mc_aliases()",
            migration,
        )
        self.assertIn(
            "CREATE TRIGGER trg_sync_qlf_certificate_types_mc_aliases",
            migration,
        )
        self.assertIn(
            "idx_qlf_certificate_types_mc_certificate_type",
            migration,
        )
        self.assertIn("compatibility_column_count <> 6", migration)
        self.assertNotIn("DELETE FROM", migration)

    def test_rm_type_mne_code_migration_supports_formula_and_process_type_saves(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "242-rm-type-mne-code-tables.sql"
        ).read_text(encoding="utf-8")

        for table_name, relation_column in (
            ("rm_formula_types_mc", "formula_type"),
            ("rm_process_types_mc", "process_type"),
        ):
            self.assertIn(table_name, migration)
            self.assertIn(relation_column, migration)
            self.assertIn(f"idx_{table_name}_{relation_column}", migration)
            self.assertIn(f"idx_{table_name}_mne_code", migration)
        self.assertIn("'mne_code', 'text'", migration)
        self.assertIn("missing_columns IS NOT NULL", migration)
        self.assertNotIn("DROP TABLE", migration)
        self.assertNotIn("DELETE FROM", migration)

    def test_rm_formula_type_root_bootstraps_first_user_managed_category(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "243-rm-formula-type-root.sql"
        ).read_text(encoding="utf-8")

        self.assertIn("'默认配方分类'", migration)
        self.assertIn("'defaultFormulaType'", migration)
        self.assertIn("parent_id", migration)
        self.assertIn("-1", migration)
        self.assertIn("ON CONFLICT (id) DO UPDATE", migration)
        self.assertIn("root_count <> 1", migration)
        self.assertNotIn("DELETE FROM", migration)

    def test_rm_formula_type_tree_uses_legacy_lob_compatible_oa_column(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "244-rm-formula-type-tree-lob-compat.sql"
        ).read_text(encoding="utf-8")

        self.assertIn("ALTER TABLE public.rm_formula_types", migration)
        self.assertIn("ALTER COLUMN oa TYPE text", migration)
        self.assertIn("ELSE oa::text", migration)
        self.assertIn("oa_type IS DISTINCT FROM 'text'", migration)
        self.assertNotIn("DROP TABLE", migration)
        self.assertNotIn("DELETE FROM", migration)

    def test_rm_formula_quality_remark_uses_legacy_lob_compatible_oid_column(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "245-rm-formula-quality-remark-lob-compat.sql"
        ).read_text(encoding="utf-8")

        self.assertIn("ALTER TABLE public.rm_formula_qualities", migration)
        self.assertIn("ALTER COLUMN remark TYPE oid", migration)
        self.assertIn(
            "lo_from_bytea(0, convert_to(remark::text, 'UTF8'))",
            migration,
        )
        self.assertIn("pg_largeobject_metadata", migration)
        self.assertIn("invalid_lob_count <> 0", migration)
        self.assertNotIn("DROP TABLE", migration)
        self.assertNotIn("DELETE FROM", migration)

    def test_common_i18n_fallback_covers_rm_formula_action_feedback(self):
        i18n_asset = (
            SCRIPT_PATH.parent.parent
            / "assets"
            / "module-static"
            / "compat"
            / "i18n-value.js"
        ).read_text(encoding="utf-8")

        for key, value in (
            ("RM.custom.randon1573717880503", "请选择一条记录进行操作！"),
            ("RM.custom.stateNeedEnable", "配方必须处于启用状态才可添加适用产线！"),
            ("RM.custom.notEffectiveFormulas", "配方必须处于生效状态才可设置检验部门！"),
            ("foundation.language.enable.success", "启用成功"),
            ("foundation.language.disable.success", "停用成功"),
            ("ec.list.taskDescription", "待办说明"),
            (
                "RM.formula.Formula.productId,BaseSet.material.Material.code",
                "产品编码",
            ),
        ):
            self.assertIn(
                f'window.InternationalResource["{key}"] = "{value}";',
                i18n_asset,
            )

    def test_qualify_system_config_restores_packaged_default_level_setting(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "238-qualify-system-config.sql"
        ).read_text(encoding="utf-8")

        self.assertIn("'Qualify', 'Qualify', 2, 'false', 'false'", migration)
        self.assertIn("'setDefaultLevel'", migration)
        self.assertIn("'dt/Qualify/Qualify'", migration)
        self.assertIn("configured_value IS DISTINCT FROM 'false'", migration)
        self.assertIn("ON CONFLICT (app_code, code) DO UPDATE", migration)
        self.assertNotIn("DELETE FROM", migration)

    def test_foundation_runtime_supplies_qualify_default_level_fallback(self):
        patch_source = (
            SCRIPT_PATH.parent.parent
            / "patches"
            / "qualify-config-defaults"
            / "src"
            / "com"
            / "supcon"
            / "orchid"
            / "Qualify"
            / "services"
            / "impl"
            / "QualifyConfigureUtil.java"
        ).read_text(encoding="utf-8")
        self.assertIn(
            '@Value("${Qualify/Qualify.setDefaultLevel:false}")',
            patch_source,
        )
        self.assertIn(
            "return setDefaultLevel != null ? setDefaultLevel : Boolean.FALSE;",
            patch_source,
        )

        prepare_script = (
            SCRIPT_PATH.parent.parent
            / "scripts"
            / "prepare-runtime-patches.sh"
        ).read_text(encoding="utf-8")
        self.assertIn(
            "patches/qualify-config-defaults/build.sh",
            prepare_script,
        )
        self.assertIn(
            'foundation_jar="$runtime_dir/module-Server/FoundationMs/manual/FoundationMs-1.0.0.jar"',
            prepare_script,
        )

        build_script = (
            SCRIPT_PATH.parent.parent
            / "patches"
            / "qualify-config-defaults"
            / "build.sh"
        ).read_text(encoding="utf-8")
        self.assertIn(
            'jar_path="$patch_root/qualify-config-defaults.jar"',
            build_script,
        )
        self.assertIn('jar xf "$foundation_jar" BOOT-INF/lib', build_script)

        manifest_script = (
            SCRIPT_PATH.parent.parent.parent.parent
            / "scripts"
            / "generate-runtime-patch-manifest.py"
        ).read_text(encoding="utf-8")
        self.assertIn(
            '"deploy/docker/patches/qualify-config-defaults/qualify-config-defaults.jar"',
            manifest_script,
        )

    def test_wts_basic_settings_lob_migration_covers_all_entities(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "231-wts-basic-settings-action-lob-compat.sql"
        ).read_text(encoding="utf-8")

        for entity_name in (
            "approveConfig",
            "checkCriteriaLib",
            "customizedConfig",
            "gasAnalysis",
            "hazidLib",
            "hourLimit",
            "qualifiSet",
            "riskSafeMeasures",
            "wfGasAnalysis",
            "wfPathFunction",
            "workRegion",
        ):
            self.assertIn(f"'WTS_1.0.0_{entity_name}_%'", migration)
        self.assertIn("lo_from_bytea(0, convert_to(item.view_json, 'UTF8'))", migration)
        self.assertIn("invalid_count", migration)

    def test_wts_hazard_permission_migration_splits_add_and_modify(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "232-wts-hazid-action-permissions.sql"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "hazidLibList_add_modify_WTS_1.0.0_hazidLib_hazidLibList",
            migration,
        )
        self.assertIn(
            "hazidLibList_add_add_WTS_1.0.0_hazidLib_hazidLibList",
            migration,
        )
        self.assertIn(
            "hazidLibList_modify_modify_WTS_1.0.0_hazidLib_hazidLibList",
            migration,
        )
        self.assertIn("role_id = 1", migration)
        self.assertIn("RAISE EXCEPTION", migration)

    def test_wts_hazard_i18n_migration_repairs_runtime_labels(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "233-wts-hazid-i18n-labels.sql"
        ).read_text(encoding="utf-8")

        self.assertIn("ec.property.dafult", migration)
        self.assertIn("'默认'", migration)
        self.assertIn('"namekey":"新增"', migration)
        self.assertIn('"displayName":"默认"', migration)
        self.assertIn("UPDATE public.runtime_extra_view", migration)
        self.assertIn("UPDATE public.ec_extra_view", migration)

    def test_wts_risk_safe_measures_migration_repairs_script_entrypoints(self):
        migration = (
            SCRIPT_PATH.parent.parent
            / "postgres"
            / "init"
            / "234-wts-risk-safe-measures-script-entrypoints.sql"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "WTS_1.0.0_riskSafeMeasures_riskSafeyEdit",
            migration,
        )
        self.assertIn(
            "WTS_1.0.0_riskSafeMeasures_riskSafeyView",
            migration,
        )
        self.assertIn(r'typeof onLoad === \"function\"', migration)
        self.assertIn("pageOnsave()", migration)
        self.assertIn(r'typeof onLoad_InitParam === \"function\"', migration)
        self.assertIn(r'typeof onSave === \"function\"', migration)
        self.assertIn("UPDATE public.runtime_extra_view", migration)
        self.assertIn("UPDATE public.ec_extra_view", migration)
        self.assertIn("decoded.payload::jsonb", migration)
        self.assertIn("runtime_valid_count <> 2", migration)
        self.assertIn("product_valid_count <> 2", migration)

    def test_cli_output_strips_trailing_whitespace_from_embedded_source(self):
        output = GENERATOR.strip_generated_line_trailing_whitespace(
            "SELECT 'function run() {   \n\treturn true;\t\n \treturn false;\n}';   \n\n"
        )

        self.assertEqual(
            "SELECT 'function run() {\n\treturn true;\n    return false;\n}';\n",
            output,
        )
        self.assertFalse(any(line.endswith((" ", "\t")) for line in output.splitlines()))


if __name__ == "__main__":
    unittest.main()
