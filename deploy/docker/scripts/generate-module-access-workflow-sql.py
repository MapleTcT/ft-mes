#!/usr/bin/env python3
"""Generate PostgreSQL menu, permission, and workflow SQL from a BAP module.

Recovered ADP packages keep access-control and JBPM definitions in
``META-INF/bap/module.xml``.  The Linux/PostgreSQL recovery path cannot rely on
the original Windows installer to import those definitions, so this generator
turns the vendor XML into deterministic, idempotent SQL migrations.

The generated SQL intentionally grants imported operations only to the
explicitly selected bootstrap role.  It never creates business data and never
drops existing module metadata.
"""

from __future__ import annotations

import argparse
import configparser
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, replace
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple


def text(element: Optional[ET.Element], tag: str, default: str = "") -> str:
    if element is None:
        return default
    child = element.find(tag)
    if child is None or child.text is None:
        return default
    return child.text.strip()


def bool_value(value: str, default: bool = False) -> bool:
    if not value:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y"}


def int_value(value: str, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def sql_string(value: Optional[str]) -> str:
    if value is None or value == "":
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def sql_bool(value: bool) -> str:
    return "true" if value else "false"


def sql_int_bool(value: bool) -> str:
    return "1" if value else "0"


@dataclass(frozen=True)
class Menu:
    code: str
    name: str
    valid: bool
    memo: str
    target: str
    url: str
    namespace: str
    action: str
    sort: float
    css_class: str
    system_default: bool
    module_code: str
    entity_code: str
    leaf: bool
    lay_no: int
    is_hide: bool
    absolute_hidden: bool
    parent_code: str


@dataclass(frozen=True)
class Operation:
    menu_code: str
    code: str
    name: str
    url: str
    namespace: str
    action: str
    target: str
    memo: str
    sort: float
    icon_cls: str
    menu_operate_type: str
    deployment_id: Optional[int]
    msg_assembled: Optional[int]
    flow_key: str
    flow_version: str
    flow_name: str
    power_flag: bool
    ignore_permission: bool
    is_hidden: bool
    entity_code: str
    enable_group_restrict: bool
    enable_pos_restrict: bool
    enable_assign_pos: bool
    enable_assign_staff: bool
    enable_no_restrict: bool
    enable_other_restrict: bool
    enable_special_permission: bool
    is_or_relation: bool
    is_query: bool
    view_code: str
    valid: bool


@dataclass(frozen=True)
class App:
    code: str
    name: str
    app_type: int
    memory: int
    module_codes: Tuple[str, ...]
    menu_codes: Tuple[str, ...]


@dataclass(frozen=True)
class WorkflowTask:
    code: str
    name: str
    display_name: str
    task_type: int
    view_code: str
    open_mode: str
    ignore_permission: bool
    deal_set: int
    is_allow_proxy: bool
    show_in_simple_dealinfo: bool
    mobile_approve: bool
    recall_able: bool


@dataclass(frozen=True)
class WorkflowTransition:
    code: str
    name: str
    display_name: str
    from_node_code: str
    to_node_code: str
    transition_type: int


@dataclass(frozen=True)
class Workflow:
    process_key: str
    process_name: str
    name: str
    display_name: str
    description: str
    menu_code: str
    entity_code: str
    process_xml: str
    entry_url: str
    is_suspended: bool
    mobile_query: bool
    mobile_initiate: bool
    mobile_approve: bool
    allow_invalid: bool
    gradually_reject: bool
    recall_able: bool
    recall_remain_time: int
    main_view_code: str
    tasks: Tuple[WorkflowTask, ...]
    transitions: Tuple[WorkflowTransition, ...]


@dataclass(frozen=True)
class ModuleAccess:
    module_code: str
    acronym: str
    menus: Tuple[Menu, ...]
    operations: Tuple[Operation, ...]
    workflows: Tuple[Workflow, ...]
    app: Optional[App]


def load_i18n_properties(path: Path) -> Dict[str, str]:
    parser = configparser.ConfigParser(
        interpolation=None,
        strict=False,
        delimiters=("=", ":"),
        comment_prefixes=("#", "!"),
    )
    parser.optionxform = str
    content = path.read_text(encoding="utf-8-sig")
    parser.read_string("[properties]\n" + content)
    return dict(parser["properties"])


def localize_module(module: ModuleAccess, translations: Dict[str, str]) -> ModuleAccess:
    def resolve(value: str) -> str:
        return translations.get(value, value)

    localized_workflows = []
    for workflow in module.workflows:
        localized_workflows.append(
            replace(
                workflow,
                process_name=resolve(workflow.process_name),
                name=resolve(workflow.name),
                display_name=resolve(workflow.display_name),
                description=resolve(workflow.description),
                tasks=tuple(
                    replace(
                        task,
                        name=resolve(task.name),
                        display_name=resolve(task.display_name),
                    )
                    for task in workflow.tasks
                ),
                transitions=tuple(
                    replace(
                        transition,
                        name=resolve(transition.name),
                        display_name=resolve(transition.display_name),
                    )
                    for transition in workflow.transitions
                ),
            )
        )

    return replace(
        module,
        menus=tuple(replace(menu, name=resolve(menu.name)) for menu in module.menus),
        operations=tuple(
            replace(operation, name=resolve(operation.name)) for operation in module.operations
        ),
        workflows=tuple(localized_workflows),
        app=(replace(module.app, name=resolve(module.app.name)) if module.app else None),
    )


def parse_menu_node(element: ET.Element, parent_code: str = "") -> Menu:
    try:
        sort = float(text(element, "sort", "0"))
    except ValueError:
        sort = 0.0
    return Menu(
        code=text(element, "code"),
        name=text(element, "name"),
        valid=bool_value(text(element, "valid"), True),
        memo=text(element, "memo"),
        target=text(element, "target"),
        url=text(element, "url"),
        namespace=text(element, "namespace"),
        action=text(element, "action"),
        sort=sort,
        css_class=text(element, "cssClass"),
        system_default=bool_value(text(element, "systemDefault"), True),
        module_code=text(element, "moduleCode"),
        entity_code=text(element, "entityCode"),
        leaf=bool_value(text(element, "leaf")),
        lay_no=int_value(text(element, "layNo"), 1),
        is_hide=bool_value(text(element, "isHide")),
        absolute_hidden=bool_value(text(element, "absoluteHidden")),
        parent_code=parent_code,
    )


def parse_operation(menu_code: str, element: ET.Element) -> Operation:
    deployment_id = int_value(text(element, "deploymentId"), -1)
    msg_assembled = int_value(text(element, "msgAssembled"), -1)
    try:
        sort = float(text(element, "sort", "0"))
    except ValueError:
        sort = 0.0
    return Operation(
        menu_code=menu_code,
        code=text(element, "code"),
        name=text(element, "name"),
        url=text(element, "url"),
        namespace=text(element, "namespace"),
        action=text(element, "action"),
        target=text(element, "target"),
        memo=text(element, "memo"),
        sort=sort,
        icon_cls=text(element, "iconCls"),
        menu_operate_type=text(element, "menuOperateType"),
        deployment_id=None if deployment_id < 0 else deployment_id,
        msg_assembled=None if msg_assembled < 0 else msg_assembled,
        flow_key=text(element, "flowKey"),
        flow_version=text(element, "flowVersion"),
        flow_name=text(element, "flowName"),
        power_flag=bool_value(text(element, "powerFlag")),
        ignore_permission=bool_value(text(element, "ignorePermission")),
        is_hidden=bool_value(text(element, "isHidden")),
        entity_code=text(element, "entityCode"),
        enable_group_restrict=bool_value(text(element, "enableGroupRestrict")),
        enable_pos_restrict=bool_value(text(element, "enablePosRestrict")),
        enable_assign_pos=bool_value(text(element, "enableAssignPos")),
        enable_assign_staff=bool_value(text(element, "enableAssignStaff")),
        enable_no_restrict=bool_value(text(element, "enableNoRestrict")),
        enable_other_restrict=bool_value(text(element, "enableOtherRestrict")),
        enable_special_permission=bool_value(text(element, "enableSpecialPermission")),
        is_or_relation=bool_value(text(element, "isOrRelation")),
        is_query=bool_value(text(element, "isQuery")),
        view_code=text(element, "viewCode"),
        valid=bool_value(text(element, "valid"), True),
    )


def parse_process_xml(process_xml: str) -> Tuple[str, Tuple[WorkflowTask, ...], Tuple[WorkflowTransition, ...]]:
    root = ET.fromstring(process_xml.strip())
    namespace = ""
    if root.tag.startswith("{"):
        namespace = root.tag.split("}", 1)[0] + "}"

    description_node = root.find(f"{namespace}description")
    display_name = ""
    if description_node is not None:
        display_name = description_node.attrib.get("text", "")

    tasks: List[WorkflowTask] = []
    transitions: List[WorkflowTransition] = []
    node_types = {"start": 1, "task": 4, "end": 2, "end-cancel": 3}
    for child in root:
        local_tag = child.tag.split("}")[-1]
        if local_tag not in node_types:
            continue
        code = child.attrib.get("name", "")
        if not code:
            continue
        open_action = child.find(f"{namespace}open-action")
        view_code = child.attrib.get("viewCode", "")
        open_mode = ""
        if open_action is not None:
            view_code = open_action.attrib.get("viewCode", view_code)
            open_mode = open_action.attrib.get("target", "")
        tasks.append(
            WorkflowTask(
                code=code,
                name=child.attrib.get("internationalKey", "") or child.attrib.get("desc", "") or code,
                display_name=child.attrib.get("desc", "") or code,
                task_type=node_types[local_tag],
                view_code=view_code,
                open_mode=open_mode,
                ignore_permission=bool_value(child.attrib.get("ignorePermission", "")),
                deal_set=int_value(child.attrib.get("dealSet", "0")),
                is_allow_proxy=bool_value(child.attrib.get("isAllowProxy", "")),
                show_in_simple_dealinfo=bool_value(child.attrib.get("showInSimpleDealInfo", "")),
                mobile_approve=bool_value(child.attrib.get("mobileApprove", "")),
                recall_able=bool_value(child.attrib.get("recallAble", "")),
            )
        )
        for transition in child.findall(f"{namespace}transition"):
            transition_type = 1
            if bool_value(transition.attrib.get("reject", "")):
                transition_type = 2
            elif bool_value(transition.attrib.get("cancel", "")):
                transition_type = 3
            transitions.append(
                WorkflowTransition(
                    code=transition.attrib.get("encode", "") or transition.attrib.get("name", ""),
                    name=transition.attrib.get("internationalKey", "") or transition.attrib.get("desc", ""),
                    display_name=transition.attrib.get("desc", ""),
                    from_node_code=code,
                    to_node_code=transition.attrib.get("to", ""),
                    transition_type=transition_type,
                )
            )

    return display_name, tuple(tasks), tuple(transitions)


def parse_workflow(element: ET.Element) -> Workflow:
    process_xml = text(element, "processXml")
    display_name, tasks, transitions = parse_process_xml(process_xml)
    return Workflow(
        process_key=text(element, "processKey"),
        process_name=text(element, "processName"),
        name=text(element, "name"),
        display_name=display_name or text(element, "processName"),
        description=text(element, "description"),
        menu_code=text(element, "menuCode"),
        entity_code=text(element, "entityCode"),
        process_xml=process_xml,
        entry_url=text(element, "entryUrl"),
        is_suspended=bool_value(text(element, "isSuspended")),
        mobile_query=bool_value(text(element, "mobilequery")),
        mobile_initiate=bool_value(text(element, "mobileinitiate")),
        mobile_approve=bool_value(text(element, "mobileapprove")),
        allow_invalid=bool_value(text(element, "allowInvalid")),
        gradually_reject=bool_value(text(element, "graduallyReject")),
        recall_able=bool_value(text(element, "recallAble")),
        recall_remain_time=int_value(text(element, "recallRemainTime"), 3600),
        main_view_code=text(element, "mainViewViewCode"),
        tasks=tasks,
        transitions=transitions,
    )


def parse_app(path: Optional[Path], module_code: str) -> Optional[App]:
    if path is None:
        return None
    root = ET.parse(path).getroot()
    app_element = root.find("app")
    if app_element is None:
        return None
    modules = tuple(item.strip() for item in text(app_element, "modules").split(",") if item.strip())
    menus = tuple(item.strip() for item in text(app_element, "menus").split(",") if item.strip())
    return App(
        code=text(app_element, "code"),
        name=text(app_element, "name"),
        app_type=int_value(text(app_element, "appType")),
        memory=int_value(text(app_element, "memory")),
        module_codes=tuple(item for item in modules if item == module_code),
        menu_codes=tuple(item for item in menus if item.startswith(module_code + "_")),
    )


def parse_module(module_xml: Path, app_xml: Optional[Path]) -> ModuleAccess:
    root = ET.parse(module_xml).getroot()
    module_code = text(root, "code")
    acronym = text(root, "acronym") or module_code.split("_", 1)[0]
    if not module_code:
        raise ValueError(f"module code is missing in {module_xml}")

    menu_nodes: Dict[str, Menu] = {}
    operations: List[Operation] = []
    menu_infos = root.find("menuInfos")
    if menu_infos is not None:
        for menu_element in menu_infos.findall("menuInfo"):
            parent_element = menu_element.find("parent")
            parent_code = text(parent_element, "code")
            menu = parse_menu_node(menu_element, parent_code)
            if not menu.code:
                continue
            menu_nodes[menu.code] = menu
            while parent_element is not None:
                grand_parent = parent_element.find("parent")
                parsed_parent = parse_menu_node(parent_element, text(grand_parent, "code"))
                if (
                    parsed_parent.code
                    and parsed_parent.module_code == module_code
                    and parsed_parent.code not in menu_nodes
                ):
                    menu_nodes[parsed_parent.code] = parsed_parent
                parent_element = grand_parent
            operation_nodes = menu_element.find("menuOperates")
            if operation_nodes is not None:
                operations.extend(
                    parse_operation(menu.code, operation_element)
                    for operation_element in operation_nodes.findall("menuOperate")
                    if text(operation_element, "code")
                )

    # Vendor XMLs commonly contain duplicate query operations. Some differ only
    # because one copy carries the data-scope flags and another does not. The
    # runtime identifies both as the same operation, so merge those flags with
    # OR semantics while preserving genuine view/action variants.
    normalized_operations: Dict[Tuple[object, ...], Operation] = {}
    for operation in dict.fromkeys(operations):
        identity = (
            operation.menu_code,
            operation.code,
            operation.view_code,
            operation.action,
            operation.url,
            operation.power_flag,
        )
        current = normalized_operations.get(identity)
        if current is None:
            normalized_operations[identity] = operation
            continue
        normalized_operations[identity] = replace(
            current,
            ignore_permission=current.ignore_permission or operation.ignore_permission,
            is_hidden=current.is_hidden or operation.is_hidden,
            enable_group_restrict=current.enable_group_restrict or operation.enable_group_restrict,
            enable_pos_restrict=current.enable_pos_restrict or operation.enable_pos_restrict,
            enable_assign_pos=current.enable_assign_pos or operation.enable_assign_pos,
            enable_assign_staff=current.enable_assign_staff or operation.enable_assign_staff,
            enable_no_restrict=current.enable_no_restrict or operation.enable_no_restrict,
            enable_other_restrict=current.enable_other_restrict or operation.enable_other_restrict,
            enable_special_permission=(
                current.enable_special_permission or operation.enable_special_permission
            ),
            is_or_relation=current.is_or_relation or operation.is_or_relation,
            is_query=current.is_query or operation.is_query,
            valid=current.valid or operation.valid,
        )
    operations = list(normalized_operations.values())

    workflows: List[Workflow] = []
    entities = root.find("entities")
    if entities is not None:
        for entity in entities.findall("entity"):
            deployments = entity.find("deployments")
            if deployments is None:
                continue
            workflows.extend(parse_workflow(item) for item in deployments.findall("deployment"))

    result = ModuleAccess(
        module_code=module_code,
        acronym=acronym,
        menus=tuple(sorted(menu_nodes.values(), key=lambda item: (item.lay_no, item.code))),
        operations=tuple(
            sorted(
                operations,
                key=lambda item: (
                    item.menu_code,
                    item.code,
                    item.view_code,
                    item.url,
                    item.action,
                    item.power_flag,
                ),
            )
        ),
        workflows=tuple(sorted(workflows, key=lambda item: item.process_key)),
        app=parse_app(app_xml, module_code),
    )
    validate_module(result)
    return result


def validate_module(module: ModuleAccess) -> None:
    menu_codes = {menu.code for menu in module.menus}
    if not menu_codes:
        raise ValueError(f"{module.module_code} does not contain menu metadata")
    for operation in module.operations:
        if operation.menu_code not in menu_codes:
            raise ValueError(f"operation {operation.code} references unknown menu {operation.menu_code}")
    workflow_keys = [workflow.process_key for workflow in module.workflows]
    if len(workflow_keys) != len(set(workflow_keys)):
        raise ValueError(f"duplicate workflow process keys: {workflow_keys}")
    for workflow in module.workflows:
        if workflow.menu_code not in menu_codes:
            raise ValueError(f"workflow {workflow.process_key} references unknown menu {workflow.menu_code}")
        task_codes = {task.code for task in workflow.tasks}
        if not task_codes:
            raise ValueError(f"workflow {workflow.process_key} does not contain tasks")
        for transition in workflow.transitions:
            if transition.from_node_code not in task_codes or transition.to_node_code not in task_codes:
                raise ValueError(
                    f"workflow {workflow.process_key} transition {transition.code} references an unknown node"
                )


def values_sql(rows: Iterable[Sequence[str]], indent: str = "        ") -> str:
    rendered = [indent + "(" + ", ".join(row) + ")" for row in rows]
    return ",\n".join(rendered)


def generate_access_sql(
    module: ModuleAccess,
    id_base: int,
    admin_role_id: int,
    source_label: str,
    menu_app_code: Optional[str] = None,
    admin_user_id: int = 1,
    admin_company_id: int = 1000,
) -> str:
    resolved_menu_app_code = (menu_app_code or module.acronym).strip()
    if not resolved_menu_app_code:
        raise ValueError("menu application code cannot be empty")
    menu_base = id_base + 100_000
    operation_base = id_base + 200_000
    permission_base = id_base + 300_000
    user_permission_base = id_base + 350_000
    company_menu_ref_base = id_base + 360_000
    menu_ids = {menu.code: menu_base + index for index, menu in enumerate(module.menus, 1)}
    operation_ids = {operation: operation_base + index for index, operation in enumerate(module.operations, 1)}

    menu_rows = []
    for menu in module.menus:
        menu_rows.append(
            (
                str(menu_ids[menu.code]),
                sql_string(menu.code),
                sql_string(menu.name),
                sql_string(menu.parent_code),
                str(menu.lay_no),
                repr(menu.sort),
                sql_string(menu.url),
                sql_string(menu.namespace),
                sql_string(menu.action),
                sql_string(menu.target),
                sql_string(menu.css_class),
                sql_string(menu.module_code or module.module_code),
                sql_string(menu.entity_code),
                sql_bool(menu.leaf),
                sql_bool(menu.valid),
                sql_bool(menu.is_hide),
                sql_bool(menu.absolute_hidden),
                sql_bool(menu.system_default),
            )
        )

    operation_rows = []
    for operation in module.operations:
        operation_rows.append(
            (
                str(operation_ids[operation]),
                sql_string(operation.menu_code),
                sql_string(operation.code),
                sql_string(operation.name),
                sql_string(operation.url),
                sql_string(operation.namespace),
                sql_string(operation.action),
                sql_string(operation.target),
                sql_string(operation.memo),
                repr(operation.sort),
                sql_string(operation.icon_cls),
                sql_string(operation.menu_operate_type),
                "NULL" if operation.deployment_id is None else str(operation.deployment_id),
                "NULL" if operation.msg_assembled is None else str(operation.msg_assembled),
                sql_string(operation.flow_key),
                sql_string(operation.flow_version),
                sql_string(operation.flow_name),
                sql_bool(operation.power_flag),
                sql_bool(operation.ignore_permission),
                sql_bool(operation.is_hidden),
                sql_string(operation.entity_code),
                sql_bool(operation.enable_group_restrict),
                sql_bool(operation.enable_pos_restrict),
                sql_bool(operation.enable_assign_pos),
                sql_bool(operation.enable_assign_staff),
                sql_bool(operation.enable_no_restrict),
                sql_bool(operation.enable_other_restrict),
                sql_bool(operation.enable_special_permission),
                sql_bool(operation.is_or_relation),
                sql_bool(operation.is_query),
                sql_string(operation.view_code),
                sql_int_bool(operation.valid),
            )
        )

    app_sql = ""
    if module.app is not None:
        app = module.app
        app_sql = f"""
INSERT INTO public.supos_app(code, valid, name, app_type, memory, modules, menus, create_time)
SELECT {sql_string(app.code)}, 1, {sql_string(app.name)}, {app.app_type}, {app.memory}, '', '', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM public.supos_app WHERE code = {sql_string(app.code)});

DO $app$
DECLARE
    current_modules text;
    current_menus text;
    item text;
BEGIN
    SELECT coalesce(modules, ''), coalesce(menus, '')
      INTO current_modules, current_menus
      FROM public.supos_app
     WHERE code = {sql_string(app.code)}
     FOR UPDATE;

    FOREACH item IN ARRAY ARRAY[{', '.join(sql_string(item) for item in app.module_codes)}]::text[]
    LOOP
        IF NOT (item = ANY(string_to_array(current_modules, ','))) THEN
            current_modules := concat_ws(',', nullif(current_modules, ''), item);
        END IF;
    END LOOP;

    FOREACH item IN ARRAY ARRAY[{', '.join(sql_string(item) for item in app.menu_codes)}]::text[]
    LOOP
        IF NOT (item = ANY(string_to_array(current_menus, ','))) THEN
            current_menus := concat_ws(',', nullif(current_menus, ''), item);
        END IF;
    END LOOP;

    UPDATE public.supos_app
       SET valid = 1,
           name = coalesce(nullif(name, ''), {sql_string(app.name)}),
           app_type = coalesce(app_type, {app.app_type}),
           memory = coalesce(memory, {app.memory}),
           modules = current_modules,
           menus = current_menus,
           modify_time = CURRENT_TIMESTAMP
     WHERE code = {sql_string(app.code)};
END $app$;
"""

    return f"""-- Generated by deploy/docker/scripts/generate-module-access-workflow-sql.py
-- section: access
-- source: {source_label}
-- module_code: {module.module_code}
-- menu_app_code: {resolved_menu_app_code}
-- bootstrap_admin_user_id: {admin_user_id}
-- bootstrap_company_id: {admin_company_id}
-- menu_count: {len(module.menus)}
-- normalized_operation_count: {len(module.operations)}
-- Stable ID range owner: {id_base}..{id_base + 999_999}

BEGIN;

CREATE TEMP TABLE adp_module_menu_seed (
    id bigint PRIMARY KEY,
    code text NOT NULL,
    name text NOT NULL,
    parent_code text,
    lay_no integer NOT NULL,
    sort double precision,
    url text,
    namespace text,
    action_url text,
    target text,
    css_class text,
    module_code text NOT NULL,
    entity_code text,
    leaf boolean NOT NULL,
    valid boolean NOT NULL,
    is_hide boolean NOT NULL,
    absolute_hidden boolean NOT NULL,
    system_default boolean NOT NULL
) ON COMMIT DROP;

INSERT INTO adp_module_menu_seed VALUES
{values_sql(menu_rows)};

DO $guard$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM adp_module_menu_seed seed
          JOIN public.rbac_menuinfo current_row ON current_row.id = seed.id
         WHERE current_row.code IS DISTINCT FROM seed.code
           AND NOT EXISTS (
               SELECT 1 FROM public.rbac_menuinfo logical_match
                WHERE logical_match.code = seed.code
           )
    ) THEN
        RAISE EXCEPTION 'PATROL menu deterministic ID range collides with existing data';
    END IF;
END $guard$;

DO $menus$
DECLARE
    target_lay_no integer;
BEGIN
    FOR target_lay_no IN
        SELECT generate_series(1, coalesce(max(lay_no), 1)) FROM adp_module_menu_seed
    LOOP
        INSERT INTO public.rbac_menuinfo (
            id, version, create_time, modify_time, creator, modifier,
            create_staff_id, modify_staff_id, valid, cid, security_class,
            absolute_hidden, three_role, show_type, request_type, hidden_type,
            menu_type, is_hide, group_only, entity_code, module_code,
            system_default, css_class, sort, action_url, namespace, url, target,
            memo, name, code, app, enable, lay_no, lay_rec, parent_id,
            full_path, full_path_name, source, edited, type, no_restrict,
            status, route
        )
        SELECT seed.id, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
               'codex_module_recovery', 'codex_module_recovery', 1, 1,
               seed.valid, 1000, 'SYSTEM/BAP', seed.absolute_hidden, false,
               0, 0, 0, 0, seed.is_hide, false, nullif(seed.entity_code, ''),
               seed.module_code, seed.system_default, nullif(seed.css_class, ''),
               seed.sort, nullif(seed.action_url, ''), nullif(seed.namespace, ''),
               nullif(seed.url, ''),
               CASE WHEN seed.target <> '' THEN seed.target
                    WHEN NOT seed.leaf AND seed.lay_no > 1 THEN 'SELF'
                    ELSE NULL END,
               NULL, seed.name, seed.code, {sql_string(resolved_menu_app_code)}, true,
               seed.lay_no,
               CASE WHEN parent.id IS NULL OR parent.id = -1 THEN seed.id::text
                    ELSE concat_ws('-', parent.lay_rec, seed.id::text) END,
               coalesce(parent.id, -1),
               CASE WHEN parent.id IS NULL OR parent.id = -1 THEN seed.id::text
                    ELSE concat_ws('/', parent.full_path, seed.id::text) END,
               CASE WHEN parent.id IS NULL OR parent.id = -1 THEN seed.name
                    ELSE concat_ws('/', parent.full_path_name, seed.name) END,
               'ADP_RECOVERY', false, 0, false, 0, nullif(seed.url, '')
          FROM adp_module_menu_seed seed
          LEFT JOIN LATERAL (
              SELECT id, lay_rec, full_path, full_path_name
                FROM public.rbac_menuinfo
               WHERE code = seed.parent_code
                 AND coalesce(valid, true)
               ORDER BY id
               LIMIT 1
          ) parent ON seed.parent_code IS NOT NULL AND seed.parent_code <> ''
         WHERE seed.lay_no = target_lay_no
           AND NOT EXISTS (
               SELECT 1 FROM public.rbac_menuinfo current_row
                WHERE current_row.code = seed.code
           );
    END LOOP;
END $menus$;

UPDATE public.rbac_menuinfo current_row
   SET valid = seed.valid,
       enable = true,
       name = seed.name,
       module_code = seed.module_code,
       entity_code = nullif(seed.entity_code, ''),
       css_class = nullif(seed.css_class, ''),
       sort = seed.sort,
       action_url = nullif(seed.action_url, ''),
       namespace = nullif(seed.namespace, ''),
       url = nullif(seed.url, ''),
       route = nullif(seed.url, ''),
       is_hide = seed.is_hide,
       absolute_hidden = seed.absolute_hidden,
       system_default = seed.system_default,
       leaf = seed.leaf,
       app = {sql_string(resolved_menu_app_code)},
       modify_time = CURRENT_TIMESTAMP
 FROM adp_module_menu_seed seed
 WHERE current_row.code = seed.code;

DO $menu_paths$
DECLARE
    target_lay_no integer;
BEGIN
    FOR target_lay_no IN
        SELECT generate_series(1, coalesce(max(lay_no), 1)) FROM adp_module_menu_seed
    LOOP
        UPDATE public.rbac_menuinfo current_row
           SET lay_no = seed.lay_no,
               parent_id = coalesce(parent.id, -1),
               lay_rec = CASE
                   WHEN parent.id IS NULL OR parent.id = -1 THEN current_row.id::text
                   ELSE concat_ws('-', parent.lay_rec, current_row.id::text)
               END,
               full_path = CASE
                   WHEN parent.id IS NULL OR parent.id = -1 THEN current_row.id::text
                   ELSE concat_ws('/', parent.full_path, current_row.id::text)
               END,
               full_path_name = CASE
                   WHEN parent.id IS NULL OR parent.id = -1 THEN seed.name
                   ELSE concat_ws('/', parent.full_path_name, seed.name)
               END
          FROM adp_module_menu_seed seed
          LEFT JOIN LATERAL (
              SELECT id, lay_rec, full_path, full_path_name
                FROM public.rbac_menuinfo
               WHERE code = seed.parent_code
                 AND coalesce(valid, true)
               ORDER BY id
               LIMIT 1
          ) parent ON seed.parent_code IS NOT NULL AND seed.parent_code <> ''
         WHERE current_row.code = seed.code
           AND seed.lay_no = target_lay_no;
    END LOOP;
END $menu_paths$;

CREATE TEMP TABLE adp_module_operation_seed (
    id bigint PRIMARY KEY,
    menu_code text NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    url text,
    namespace text,
    action_url text,
    target text,
    memo text,
    sort double precision,
    icon_cls text,
    menuoperatetype text,
    deployment_id bigint,
    msg_assembled integer,
    flow_key text,
    flow_version text,
    flow_name text,
    power_flag boolean,
    ignore_permission boolean,
    is_hidden boolean,
    entity_code text,
    enable_grouprestrict boolean,
    enable_posrestrict boolean,
    enable_assignpos boolean,
    enable_assignstaff boolean,
    enable_norestrict boolean,
    enable_custompermission boolean,
    enable_datapermission boolean,
    is_orrelation boolean,
    is_query boolean,
    view_code text,
    valid integer
) ON COMMIT DROP;

INSERT INTO adp_module_operation_seed VALUES
{values_sql(operation_rows)};

DO $guard$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM adp_module_operation_seed seed
          JOIN public.rbac_menuoperate current_row ON current_row.id = seed.id
         WHERE current_row.code IS DISTINCT FROM seed.code
           AND NOT EXISTS (
               SELECT 1
                 FROM public.rbac_menuoperate logical_match
                 JOIN public.rbac_menuinfo menu ON menu.id = logical_match.menuinfo_id
                WHERE menu.code = seed.menu_code
                  AND logical_match.code = seed.code
                  AND coalesce(logical_match.view_code, '') = coalesce(seed.view_code, '')
                  AND coalesce(logical_match.action_url, '') = coalesce(seed.action_url, '')
                  AND coalesce(logical_match.url, '') = coalesce(seed.url, '')
                  AND coalesce(logical_match.power_flag, false) = coalesce(seed.power_flag, false)
           )
    ) THEN
        RAISE EXCEPTION 'PATROL operation deterministic ID range collides with existing data';
    END IF;
END $guard$;

INSERT INTO public.rbac_menuoperate (
    id, row_version, version, create_time, modify_time, creator, modifier,
    create_staff_id, modify_staff_id, valid, cid, is_allow_proxy, is_hidden,
    three_role, view_code, is_query, is_orrelation, enable_datapermission,
    enable_custompermission, for_flow_permission, enable_norestrict,
    enable_dealerpermission, enable_assignstaff, enable_assignpos,
    enable_posrestrict, enable_deptrict, enable_assigndept,
    enable_grouprestrict, entity_code, ignore_permission, power_flag,
    flow_version, flow_key, msg_assembled, deployment_id, menuoperatetype,
    menuinfo_id, icon_cls, module_code, sort, memo, target, action_url,
    namespace, url, name, code, app, default_operate, edited
)
SELECT seed.id, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       'codex_module_recovery', 'codex_module_recovery', 1, 1, seed.valid,
       1000, false, seed.is_hidden, false, nullif(seed.view_code, ''),
       seed.is_query, seed.is_orrelation, seed.enable_datapermission,
       seed.enable_custompermission, false, seed.enable_norestrict, false,
       seed.enable_assignstaff, seed.enable_assignpos, seed.enable_posrestrict,
       false, false, seed.enable_grouprestrict, nullif(seed.entity_code, ''),
       seed.ignore_permission, seed.power_flag, nullif(seed.flow_version, ''),
       nullif(seed.flow_key, ''), seed.msg_assembled, seed.deployment_id,
       nullif(seed.menuoperatetype, ''), menu.id, nullif(seed.icon_cls, ''),
       {sql_string(module.module_code)}, seed.sort, nullif(seed.memo, ''),
       nullif(seed.target, ''), nullif(seed.action_url, ''),
       nullif(seed.namespace, ''), nullif(seed.url, ''), seed.name, seed.code,
       {sql_string(resolved_menu_app_code)}, false, false
  FROM adp_module_operation_seed seed
  JOIN LATERAL (
      SELECT id FROM public.rbac_menuinfo
       WHERE code = seed.menu_code ORDER BY id LIMIT 1
  ) menu ON true
 WHERE NOT EXISTS (
     SELECT 1
       FROM public.rbac_menuoperate current_row
      WHERE current_row.menuinfo_id = menu.id
        AND current_row.code = seed.code
        AND coalesce(current_row.view_code, '') = coalesce(seed.view_code, '')
        AND coalesce(current_row.action_url, '') = coalesce(seed.action_url, '')
        AND coalesce(current_row.url, '') = coalesce(seed.url, '')
        AND coalesce(current_row.power_flag, false) = coalesce(seed.power_flag, false)
 );

WITH matched AS (
    SELECT DISTINCT ON (
               current_row.menuinfo_id, current_row.code,
               coalesce(current_row.view_code, ''),
               coalesce(current_row.action_url, ''),
               coalesce(current_row.url, ''),
               coalesce(current_row.power_flag, false)
           )
           current_row.id,
           seed.valid,
           seed.is_hidden,
           seed.is_query,
           seed.is_orrelation,
           seed.name,
           seed.ignore_permission,
           seed.enable_datapermission,
           seed.enable_custompermission,
           seed.enable_norestrict,
           seed.enable_assignstaff,
           seed.enable_assignpos,
           seed.enable_posrestrict,
           seed.enable_grouprestrict
      FROM adp_module_operation_seed seed
      JOIN public.rbac_menuinfo menu ON menu.code = seed.menu_code
      JOIN public.rbac_menuoperate current_row
        ON current_row.menuinfo_id = menu.id
       AND current_row.code = seed.code
       AND coalesce(current_row.view_code, '') = coalesce(seed.view_code, '')
       AND coalesce(current_row.action_url, '') = coalesce(seed.action_url, '')
       AND coalesce(current_row.url, '') = coalesce(seed.url, '')
       AND coalesce(current_row.power_flag, false) = coalesce(seed.power_flag, false)
     ORDER BY current_row.menuinfo_id, current_row.code,
              coalesce(current_row.view_code, ''),
              coalesce(current_row.action_url, ''),
              coalesce(current_row.url, ''),
              coalesce(current_row.power_flag, false), current_row.id
)
UPDATE public.rbac_menuoperate current_row
   SET valid = matched.valid,
       module_code = {sql_string(module.module_code)},
       app = {sql_string(resolved_menu_app_code)},
       is_hidden = matched.is_hidden,
       is_query = matched.is_query,
       is_orrelation = matched.is_orrelation,
       name = matched.name,
       ignore_permission = matched.ignore_permission,
       enable_datapermission = matched.enable_datapermission,
       enable_custompermission = matched.enable_custompermission,
       enable_norestrict = matched.enable_norestrict,
       enable_assignstaff = matched.enable_assignstaff,
       enable_assignpos = matched.enable_assignpos,
       enable_posrestrict = matched.enable_posrestrict,
       enable_grouprestrict = matched.enable_grouprestrict,
       modify_time = CURRENT_TIMESTAMP
  FROM matched
 WHERE current_row.id = matched.id;

WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY menuinfo_id, code, coalesce(view_code, ''),
                            coalesce(action_url, ''), coalesce(url, ''),
                            coalesce(power_flag, false)
               ORDER BY (
                   coalesce(enable_grouprestrict, false)::integer
                   + coalesce(enable_posrestrict, false)::integer
                   + coalesce(enable_assignpos, false)::integer
                   + coalesce(enable_assignstaff, false)::integer
                   + coalesce(enable_norestrict, false)::integer
                   + coalesce(enable_custompermission, false)::integer
                   + coalesce(enable_datapermission, false)::integer
               ) DESC,
               id
           ) AS duplicate_rank
      FROM public.rbac_menuoperate
     WHERE module_code = {sql_string(module.module_code)}
)
UPDATE public.rbac_menuoperate current_row
   SET valid = 0,
       modify_time = CURRENT_TIMESTAMP,
       modifier = 'codex_patrol_dedup'
  FROM ranked
 WHERE current_row.id = ranked.id
   AND ranked.duplicate_rank > 1;

WITH canonical_operations AS (
    SELECT DISTINCT ON (
               current_row.menuinfo_id, current_row.code,
               coalesce(current_row.view_code, ''),
               coalesce(current_row.action_url, ''),
               coalesce(current_row.url, ''),
               coalesce(current_row.power_flag, false)
           )
           current_row.id AS menuoperate_id,
           row_number() OVER (ORDER BY current_row.id) AS rn
      FROM public.rbac_menuoperate current_row
     WHERE current_row.module_code = {sql_string(module.module_code)}
       AND coalesce(current_row.valid, 0) = 1
     ORDER BY current_row.menuinfo_id, current_row.code,
              coalesce(current_row.view_code, ''),
              coalesce(current_row.action_url, ''),
              coalesce(current_row.url, ''),
              coalesce(current_row.power_flag, false), current_row.id
),
missing_permissions AS (
    SELECT canonical_operations.*
      FROM canonical_operations
     WHERE NOT EXISTS (
         SELECT 1 FROM public.rbac_rolepermission current_permission
          WHERE current_permission.role_id = {admin_role_id}
            AND current_permission.menuoperate_id = canonical_operations.menuoperate_id
     )
)
INSERT INTO public.rbac_rolepermission (
    id, cid, version, role_id, menuoperate_id, position_flag,
    department_flag, group_flag, assign_staff_flag, assign_pos_flag,
    assign_dept_flag, dealer_permission_flag, no_restrict_flag,
    assign_datapermission_flag, assign_custompermission_flag,
    create_time, modify_time, creator, modifier, create_staff_id,
    modify_staff_id
)
SELECT {permission_base} + missing_permissions.rn, {admin_company_id}, 0, {admin_role_id},
       missing_permissions.menuoperate_id, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'codex_module_recovery',
       'codex_module_recovery', {admin_user_id}, {admin_user_id}
  FROM missing_permissions
ON CONFLICT (role_id, menuoperate_id) WHERE role_id IS NOT NULL AND menuoperate_id IS NOT NULL
DO UPDATE SET no_restrict_flag = 1, modify_time = CURRENT_TIMESTAMP;

CREATE TEMP TABLE adp_module_user_permission_seed (
    id bigint PRIMARY KEY,
    menuoperate_id bigint NOT NULL,
    menuoperate_code text NOT NULL
) ON COMMIT DROP;

INSERT INTO adp_module_user_permission_seed
WITH canonical_operations AS (
    SELECT current_row.id AS menuoperate_id,
           current_row.code AS menuoperate_code,
           row_number() OVER (ORDER BY current_row.id) AS rn
      FROM public.rbac_menuoperate current_row
     WHERE current_row.module_code = {sql_string(module.module_code)}
       AND coalesce(current_row.valid, 0) = 1
)
SELECT {user_permission_base} + canonical_operations.rn,
       canonical_operations.menuoperate_id,
       canonical_operations.menuoperate_code
  FROM canonical_operations
 WHERE NOT EXISTS (
     SELECT 1
       FROM public.rbac_userpermission current_permission
      WHERE current_permission.user_id = {admin_user_id}
        AND current_permission.cid = {admin_company_id}
        AND current_permission.menuoperate_id = canonical_operations.menuoperate_id
 );

DO $user_permission_guard$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM adp_module_user_permission_seed seed
          JOIN public.rbac_userpermission current_permission
            ON current_permission.id = seed.id
         WHERE current_permission.user_id IS DISTINCT FROM {admin_user_id}
            OR current_permission.cid IS DISTINCT FROM {admin_company_id}
            OR current_permission.menuoperate_id IS DISTINCT FROM seed.menuoperate_id
    ) THEN
        RAISE EXCEPTION 'PATROL user-permission deterministic ID range collides with existing data';
    END IF;
END $user_permission_guard$;

INSERT INTO public.rbac_userpermission (
    id, version, user_id, deal_staff, cid, menuoperate_id, purview_type,
    position_flag, department_flag, group_flag, assign_staff_flag,
    assign_pos_flag, assign_dept_flag, dealer_permission_flag,
    no_restrict_flag, assign_datapermission_flag,
    assign_custompermission_flag, url_pattern, menuoperate_code,
    modify_time, create_time, modifier, creator, create_staff_id,
    modify_staff_id
)
SELECT seed.id, 0, {admin_user_id}, NULL, {admin_company_id},
       seed.menuoperate_id, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, NULL,
       seed.menuoperate_code, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       'codex_module_recovery', 'codex_module_recovery',
       {admin_user_id}, {admin_user_id}
  FROM adp_module_user_permission_seed seed;

UPDATE public.rbac_userpermission current_permission
   SET no_restrict_flag = 1,
       purview_type = 0,
       menuoperate_code = operation.code,
       modify_time = CURRENT_TIMESTAMP,
       modifier = 'codex_module_recovery'
  FROM public.rbac_menuoperate operation
 WHERE current_permission.user_id = {admin_user_id}
   AND current_permission.cid = {admin_company_id}
   AND current_permission.menuoperate_id = operation.id
   AND operation.module_code = {sql_string(module.module_code)}
   AND coalesce(operation.valid, 0) = 1;

CREATE TEMP TABLE adp_module_company_menu_seed (
    id bigint PRIMARY KEY,
    menuinfo_id bigint NOT NULL
) ON COMMIT DROP;

INSERT INTO adp_module_company_menu_seed
SELECT {company_menu_ref_base} + row_number() OVER (ORDER BY menu.id),
       menu.id
  FROM public.rbac_menuinfo menu
 WHERE menu.module_code = {sql_string(module.module_code)}
   AND coalesce(menu.valid, false)
   AND NOT EXISTS (
       SELECT 1
         FROM public.rbac_menuinfo_company_ref current_ref
        WHERE current_ref.menuinfo_id = menu.id
          AND current_ref.company_id IN ({admin_company_id}, -1)
   );

DO $company_menu_guard$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM adp_module_company_menu_seed seed
          JOIN public.rbac_menuinfo_company_ref current_ref
            ON current_ref.id = seed.id
         WHERE current_ref.menuinfo_id IS DISTINCT FROM seed.menuinfo_id
            OR current_ref.company_id NOT IN ({admin_company_id}, -1)
    ) THEN
        RAISE EXCEPTION 'PATROL company-menu deterministic ID range collides with existing data';
    END IF;
END $company_menu_guard$;

INSERT INTO public.rbac_menuinfo_company_ref (
    id, menuinfo_id, company_id, valid, creator, modifier,
    create_time, modify_time, create_staff_id, modify_staff_id, appid
)
SELECT seed.id, seed.menuinfo_id, -1, false,
       'codex_module_recovery', 'codex_module_recovery',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, {admin_user_id},
       {admin_user_id}, {sql_string(resolved_menu_app_code)}
  FROM adp_module_company_menu_seed seed;

UPDATE public.rbac_menuinfo_company_ref current_ref
   SET appid = {sql_string(resolved_menu_app_code)},
       modify_time = CURRENT_TIMESTAMP,
       modifier = 'codex_module_recovery'
  FROM public.rbac_menuinfo menu
 WHERE current_ref.menuinfo_id = menu.id
   AND current_ref.company_id IN ({admin_company_id}, -1)
   AND menu.module_code = {sql_string(module.module_code)};

{app_sql}
CREATE INDEX IF NOT EXISTS idx_rbac_menuinfo_patrol_module_code
    ON public.rbac_menuinfo(module_code, code);
CREATE INDEX IF NOT EXISTS idx_rbac_menuoperate_patrol_module_code
    ON public.rbac_menuoperate(module_code, menuinfo_id);

COMMIT;
"""


def generate_workflow_sql(module: ModuleAccess, id_base: int, admin_user_id: int, source_label: str) -> str:
    workflow_base = id_base + 400_000
    jbpm_base = id_base + 500_000
    workflow_ids = {
        workflow.process_key: workflow_base + (index * 1_000)
        for index, workflow in enumerate(module.workflows, 1)
    }
    jbpm_ids = {
        workflow.process_key: jbpm_base + (index * 1_000)
        for index, workflow in enumerate(module.workflows, 1)
    }

    deployment_rows = []
    task_rows = []
    transition_rows = []
    permission_rows = []
    for workflow in module.workflows:
        deployment_rows.append(
            (
                str(workflow_ids[workflow.process_key]),
                str(jbpm_ids[workflow.process_key]),
                sql_string(workflow.process_key),
                sql_string(workflow.process_name),
                sql_string(workflow.name),
                sql_string(workflow.display_name),
                sql_string(workflow.description),
                sql_string(workflow.menu_code),
                sql_string(workflow.entity_code),
                sql_string(workflow.process_xml),
                sql_string(workflow.entry_url),
                sql_int_bool(workflow.is_suspended),
                sql_int_bool(workflow.mobile_query),
                sql_int_bool(workflow.mobile_initiate),
                sql_int_bool(workflow.mobile_approve),
                sql_int_bool(workflow.allow_invalid),
                sql_int_bool(workflow.gradually_reject),
                sql_int_bool(workflow.recall_able),
                str(workflow.recall_remain_time),
                sql_string(workflow.main_view_code),
            )
        )
        for offset, task in enumerate(workflow.tasks, 101):
            task_rows.append(
                (
                    sql_string(workflow.process_key),
                    str(offset),
                    sql_string(task.code),
                    sql_string(task.name),
                    sql_string(task.display_name),
                    str(task.task_type),
                    sql_string(task.view_code),
                    sql_string(task.open_mode),
                    sql_int_bool(task.ignore_permission),
                    str(task.deal_set),
                    sql_int_bool(task.is_allow_proxy),
                    sql_int_bool(task.show_in_simple_dealinfo),
                    sql_int_bool(task.mobile_approve),
                    sql_int_bool(task.recall_able),
                )
            )
            if task.task_type == 4:
                permission_rows.append(
                    (
                        sql_string(workflow.process_key),
                        str(300 + offset),
                        sql_string(task.code),
                    )
                )
        for offset, transition in enumerate(workflow.transitions, 201):
            transition_rows.append(
                (
                    sql_string(workflow.process_key),
                    str(offset),
                    sql_string(transition.code),
                    sql_string(transition.name),
                    sql_string(transition.display_name),
                    sql_string(transition.from_node_code),
                    sql_string(transition.to_node_code),
                    str(transition.transition_type),
                )
            )

    return f"""-- Generated by deploy/docker/scripts/generate-module-access-workflow-sql.py
-- section: workflow
-- source: {source_label}
-- module_code: {module.module_code}
-- workflow_count: {len(module.workflows)}
-- Stable ID range owner: {id_base}..{id_base + 999_999}

BEGIN;

CREATE TEMP TABLE adp_module_workflow_seed (
    id bigint PRIMARY KEY,
    jbpm_id bigint NOT NULL UNIQUE,
    process_key text NOT NULL UNIQUE,
    process_name text NOT NULL,
    name text NOT NULL,
    display_name text NOT NULL,
    description text,
    menu_code text NOT NULL,
    entity_code text NOT NULL,
    process_xml text NOT NULL,
    entry_url text,
    is_suspended integer NOT NULL,
    mobile_query integer NOT NULL,
    mobile_initiate integer NOT NULL,
    mobile_approve integer NOT NULL,
    allow_invalid integer NOT NULL,
    gradually_reject integer NOT NULL,
    recall_able integer NOT NULL,
    recall_remain_time bigint NOT NULL,
    main_view_code text
) ON COMMIT DROP;

INSERT INTO adp_module_workflow_seed VALUES
{values_sql(deployment_rows)};

DO $deployments$
DECLARE
    seed adp_module_workflow_seed%ROWTYPE;
    current_id bigint;
    process_oid oid;
    temp_process_oid oid;
BEGIN
    FOR seed IN SELECT * FROM adp_module_workflow_seed ORDER BY process_key
    LOOP
        SELECT id INTO current_id
          FROM public.wf_deployment
         WHERE process_key = seed.process_key
         ORDER BY coalesce(is_current_version, 0) DESC,
                  coalesce(process_version, 0) DESC, id DESC
         LIMIT 1;

        IF NOT EXISTS (
            SELECT 1 FROM public.rbac_menuinfo WHERE code = seed.menu_code
        ) THEN
            RAISE EXCEPTION 'PATROL workflow % cannot resolve menu %', seed.process_key, seed.menu_code;
        END IF;

        IF current_id IS NULL THEN
            IF EXISTS (SELECT 1 FROM public.wf_deployment WHERE id = seed.id) THEN
                RAISE EXCEPTION 'PATROL workflow deterministic ID % collides with existing data', seed.id;
            END IF;
            process_oid := lo_from_bytea(0, convert_to(seed.process_xml, 'UTF8'));
            temp_process_oid := lo_from_bytea(0, convert_to(seed.process_xml, 'UTF8'));
            INSERT INTO public.wf_deployment (
                id, process_key, process_version, name, name_zh_cn, valid,
                is_current_version, version, create_time, modify_time,
                create_staff_id, modify_staff_id, signature_enable,
                publish_time, main_view_view_code, recall_remain_time,
                recall_able, gradually_reject, allow_invalid, mobileapprove,
                mobileinitiate, mobilequery, flow_edit_flag, temp_process_xml,
                entry_url, process_xml, publish_flag, entity_code, menu_code,
                menu_info_id, process_definition_id, is_suspended,
                deployment_id, process_name, description, cid,
                cross_company_flag, process_xml_text_backup,
                temp_process_xml_text_backup
            )
            SELECT seed.id, seed.process_key, 1, seed.name, seed.display_name,
                   1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0,
                   CURRENT_TIMESTAMP, nullif(seed.main_view_code, ''),
                   seed.recall_remain_time, seed.recall_able,
                   seed.gradually_reject, seed.allow_invalid,
                   seed.mobile_approve, seed.mobile_initiate,
                   seed.mobile_query, 0, temp_process_oid,
                   nullif(seed.entry_url, ''), process_oid, 1,
                   seed.entity_code, seed.menu_code, menu.id,
                   seed.process_key || '-1', seed.is_suspended,
                   seed.jbpm_id::text, seed.process_name,
                   nullif(seed.description, ''), 1000, 0,
                   seed.process_xml, seed.process_xml
              FROM public.rbac_menuinfo menu
             WHERE menu.code = seed.menu_code
             ORDER BY menu.id
             LIMIT 1;
            current_id := seed.id;
        ELSE
            UPDATE public.wf_deployment current_row
               SET valid = 1,
                   is_current_version = CASE WHEN current_row.id = current_id THEN 1 ELSE 0 END,
                   process_version = CASE WHEN current_row.id = current_id THEN 1 ELSE current_row.process_version END,
                   name = seed.name,
                   name_zh_cn = seed.display_name,
                   publish_flag = CASE WHEN current_row.id = current_id THEN 1 ELSE current_row.publish_flag END,
                   menu_code = seed.menu_code,
                   menu_info_id = (SELECT id FROM public.rbac_menuinfo WHERE code = seed.menu_code ORDER BY id LIMIT 1),
                   entity_code = seed.entity_code,
                   process_definition_id = CASE WHEN current_row.id = current_id THEN seed.process_key || '-1' ELSE current_row.process_definition_id END,
                   deployment_id = CASE WHEN current_row.id = current_id THEN seed.jbpm_id::text ELSE current_row.deployment_id END,
                   process_xml_text_backup = CASE WHEN current_row.id = current_id THEN seed.process_xml ELSE current_row.process_xml_text_backup END,
                   temp_process_xml_text_backup = CASE WHEN current_row.id = current_id THEN seed.process_xml ELSE current_row.temp_process_xml_text_backup END,
                   modify_time = CURRENT_TIMESTAMP
             WHERE current_row.process_key = seed.process_key;

            UPDATE public.wf_deployment
               SET process_xml = lo_from_bytea(0, convert_to(seed.process_xml, 'UTF8'))
             WHERE id = current_id AND process_xml IS NULL;
            UPDATE public.wf_deployment
               SET temp_process_xml = lo_from_bytea(0, convert_to(seed.process_xml, 'UTF8'))
             WHERE id = current_id AND temp_process_xml IS NULL;
        END IF;
    END LOOP;
END $deployments$;

CREATE TEMP TABLE adp_module_task_seed (
    process_key text NOT NULL,
    offset_id bigint NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    display_name text NOT NULL,
    task_type integer NOT NULL,
    view_code text,
    open_mode text,
    ignore_permission integer,
    deal_set integer,
    is_allow_proxy integer,
    show_in_simple_dealinfo integer,
    mobile_approve integer,
    recall_able integer
) ON COMMIT DROP;

INSERT INTO adp_module_task_seed VALUES
{values_sql(task_rows)};

INSERT INTO public.wf_task (
    id, version, create_time, modify_time, create_staff_id, modify_staff_id,
    valid, ignore_permission, web_signet_flag, deal_set, is_allow_proxy,
    show_in_simple_dealinfo, mobile_approve, recall_able, process_version,
    process_key, open_mode, view_code, type, deployment_id, code,
    name_zh_cn, name, cid
)
SELECT deployment.id + seed.offset_id, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       1, 1, 1, seed.ignore_permission, 0, seed.deal_set,
       seed.is_allow_proxy, seed.show_in_simple_dealinfo,
       seed.mobile_approve, seed.recall_able, 1, seed.process_key,
       nullif(seed.open_mode, ''), nullif(seed.view_code, ''), seed.task_type,
       deployment.id, seed.code, seed.display_name, seed.name, 1000
  FROM adp_module_task_seed seed
  JOIN public.wf_deployment deployment
    ON deployment.process_key = seed.process_key
   AND coalesce(deployment.is_current_version, 0) = 1
 WHERE NOT EXISTS (
     SELECT 1 FROM public.wf_task current_row
      WHERE current_row.deployment_id = deployment.id
        AND current_row.code = seed.code
 );

CREATE TEMP TABLE adp_module_transition_seed (
    process_key text NOT NULL,
    offset_id bigint NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    display_name text NOT NULL,
    from_node_code text NOT NULL,
    to_node_code text NOT NULL,
    transition_type integer NOT NULL
) ON COMMIT DROP;

INSERT INTO adp_module_transition_seed VALUES
{values_sql(transition_rows)};

INSERT INTO public.wf_transition (
    id, version, create_time, modify_time, create_staff_id, modify_staff_id,
    valid, default_staff, required_staff, select_staff, deployment_id,
    to_node_code, from_node_code, type, code, name_zh_cn, name
)
SELECT deployment.id + seed.offset_id, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       1, 1, 1, 0, 0, '0', deployment.id, seed.to_node_code,
       seed.from_node_code, seed.transition_type, seed.code,
       seed.display_name, seed.name
  FROM adp_module_transition_seed seed
  JOIN public.wf_deployment deployment
    ON deployment.process_key = seed.process_key
   AND coalesce(deployment.is_current_version, 0) = 1
 WHERE NOT EXISTS (
     SELECT 1 FROM public.wf_transition current_row
      WHERE current_row.deployment_id = deployment.id
        AND current_row.code = seed.code
 );

CREATE TEMP TABLE adp_module_flow_permission_seed (
    process_key text NOT NULL,
    offset_id bigint NOT NULL,
    activity_code text NOT NULL
) ON COMMIT DROP;

INSERT INTO adp_module_flow_permission_seed VALUES
{values_sql(permission_rows)};

INSERT INTO public.rbac_flow_permission (
    id, version, create_time, modify_time, create_staff_id, modify_staff_id,
    cid, entity_code, purview_distribution, purview_state, memo,
    unlimited_power, group_power_flag, assign_staff_flag, assign_pos_flag,
    position_power_flag, flow_permission_type, type_id, activity_code,
    flow_version, flow_key, flow_name, flow_name_display
)
SELECT deployment.id + seed.offset_id, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       1, 1, 1000, deployment.entity_code, 3, 1, '', true, false, false,
       false, false, 'USER', {admin_user_id}, seed.activity_code, '1',
       seed.process_key, deployment.name, deployment.name_zh_cn
  FROM adp_module_flow_permission_seed seed
  JOIN public.wf_deployment deployment
    ON deployment.process_key = seed.process_key
   AND coalesce(deployment.is_current_version, 0) = 1
 WHERE NOT EXISTS (
     SELECT 1 FROM public.rbac_flow_permission current_row
      WHERE current_row.flow_key = seed.process_key
        AND current_row.activity_code = seed.activity_code
        AND current_row.flow_permission_type = 'USER'
        AND current_row.type_id = {admin_user_id}
 );

DO $jbpm$
DECLARE
    seed adp_module_workflow_seed%ROWTYPE;
BEGIN
    FOR seed IN SELECT * FROM adp_module_workflow_seed ORDER BY process_key
    LOOP
        IF EXISTS (
            SELECT 1 FROM public.jbpm4_deployment current_row
             WHERE current_row.dbid_ = seed.jbpm_id
               AND NOT EXISTS (
                   SELECT 1 FROM public.jbpm4_deployprop prop
                    WHERE prop.deployment_ = seed.jbpm_id
                      AND prop.key_ = 'pdkey'
                      AND prop.stringval_ = seed.process_key
               )
        ) THEN
            RAISE EXCEPTION 'PATROL JBPM deterministic ID % collides with existing data', seed.jbpm_id;
        END IF;

        INSERT INTO public.jbpm4_deployment(dbid_, name_, timestamp_, state_)
        VALUES (seed.jbpm_id, '', 0, CASE WHEN seed.is_suspended = 1 THEN 'suspended' ELSE 'active' END)
        ON CONFLICT (dbid_) DO UPDATE SET state_ = EXCLUDED.state_;

        INSERT INTO public.jbpm4_deployprop(dbid_, deployment_, objname_, key_, stringval_, longval_)
        VALUES
            (seed.jbpm_id + 1, seed.jbpm_id, seed.process_key, 'langid', 'jpdl-4.4', NULL),
            (seed.jbpm_id + 2, seed.jbpm_id, seed.process_key, 'pdid', seed.process_key || '-1', NULL),
            (seed.jbpm_id + 3, seed.jbpm_id, seed.process_key, 'pdkey', seed.process_key, NULL),
            (seed.jbpm_id + 4, seed.jbpm_id, seed.process_key, 'pdversion', NULL, 1)
        ON CONFLICT (dbid_) DO UPDATE SET
            deployment_ = EXCLUDED.deployment_, objname_ = EXCLUDED.objname_,
            key_ = EXCLUDED.key_, stringval_ = EXCLUDED.stringval_,
            longval_ = EXCLUDED.longval_;

        IF NOT EXISTS (SELECT 1 FROM public.jbpm4_lob WHERE dbid_ = seed.jbpm_id + 10) THEN
            INSERT INTO public.jbpm4_lob(dbid_, dbversion_, blob_value_, deployment_, name_)
            VALUES (
                seed.jbpm_id + 10, 0,
                lo_from_bytea(0, convert_to(seed.process_xml, 'UTF8')),
                seed.jbpm_id, seed.process_key || '.jpdl.xml'
            );
        END IF;
    END LOOP;
END $jbpm$;

CREATE INDEX IF NOT EXISTS idx_wf_task_patrol_deployment_code
    ON public.wf_task(deployment_id, code);
CREATE INDEX IF NOT EXISTS idx_wf_transition_patrol_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);
CREATE INDEX IF NOT EXISTS idx_rbac_flow_permission_patrol_activity
    ON public.rbac_flow_permission(flow_key, activity_code, purview_state);

COMMIT;
"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--module-xml", type=Path, required=True)
    parser.add_argument("--app-xml", type=Path)
    parser.add_argument("--i18n-properties", type=Path)
    parser.add_argument("--section", choices=("access", "workflow"), required=True)
    parser.add_argument("--id-base", type=int, required=True)
    parser.add_argument("--admin-role-id", type=int, default=1)
    parser.add_argument("--admin-user-id", type=int, default=1)
    parser.add_argument("--admin-company-id", type=int, default=1000)
    parser.add_argument(
        "--menu-app-code",
        help=(
            "frontend host application code for imported menus and operations; "
            "defaults to the module acronym"
        ),
    )
    parser.add_argument("--source-label", default="recovered module package")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    module_xml = args.module_xml.expanduser().resolve()
    app_xml = args.app_xml.expanduser().resolve() if args.app_xml else None
    i18n_properties = (
        args.i18n_properties.expanduser().resolve() if args.i18n_properties else None
    )
    if not module_xml.is_file():
        print(f"module XML does not exist: {module_xml}", file=sys.stderr)
        return 2
    if app_xml is not None and not app_xml.is_file():
        print(f"app XML does not exist: {app_xml}", file=sys.stderr)
        return 2
    if i18n_properties is not None and not i18n_properties.is_file():
        print(f"i18n properties do not exist: {i18n_properties}", file=sys.stderr)
        return 2
    try:
        module = parse_module(module_xml, app_xml)
        if i18n_properties is not None:
            module = localize_module(module, load_i18n_properties(i18n_properties))
    except (ET.ParseError, ValueError, OSError, UnicodeError, configparser.Error) as exc:
        print(f"cannot parse module metadata: {exc}", file=sys.stderr)
        return 3
    if args.section == "workflow" and not module.workflows:
        print(f"module {module.module_code} has no workflow definitions", file=sys.stderr)
        return 4
    if args.section == "access":
        output = generate_access_sql(
            module,
            args.id_base,
            args.admin_role_id,
            args.source_label,
            args.menu_app_code,
            args.admin_user_id,
            args.admin_company_id,
        )
    else:
        output = generate_workflow_sql(
            module, args.id_base, args.admin_user_id, args.source_label
        )
    sys.stdout.write(output.rstrip() + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
