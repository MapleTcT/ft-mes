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
