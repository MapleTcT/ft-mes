\set ON_ERROR_STOP on

\echo 'Verifying PATROL 6.0.4.0 PostgreSQL schema and runtime metadata'

DO $$
DECLARE
    expected_tables text[] := ARRAY[
        'mp_candidate_values',
        'mp_create_task_plans',
        'mp_create_tasks',
        'mp_except_periods',
        'mp_exception_times',
        'mp_exception_works',
        'mp_exemption_items',
        'mp_input_standards',
        'mp_input_standards_di',
        'mp_input_standards_mc',
        'mp_patrol_monits',
        'mp_patrol_monits_di',
        'mp_patrol_plans',
        'mp_patrol_plans_di',
        'mp_patrol_task_areas',
        'mp_plan_roles',
        'mp_plan_staffs',
        'mp_potrol_tasks',
        'mp_potrol_tasks_di',
        'mp_potrol_tasks_sv',
        'mp_public_item_trees',
        'mp_public_item_trees_di',
        'mp_public_items',
        'mp_staff_searches',
        'mp_task_completes',
        'mp_task_details',
        'mp_task_staffs',
        'mp_work_areas',
        'mp_work_groups',
        'mp_work_groups_di',
        'mp_work_groups_mc',
        'mp_work_items',
        'team_team_detail_heads',
        'team_team_detail_stafs',
        'team_team_details',
        'team_teams',
        'team_teams_di'
    ];
    missing_tables text[];
    non_base_tables text[];
BEGIN
    SELECT array_agg(expected_name ORDER BY expected_name)
      INTO missing_tables
      FROM unnest(expected_tables) AS expected(expected_name)
      LEFT JOIN information_schema.tables current_table
        ON current_table.table_schema = 'public'
       AND current_table.table_name = expected.expected_name
     WHERE current_table.table_name IS NULL;

    IF missing_tables IS NOT NULL THEN
        RAISE EXCEPTION 'Missing PATROL tables: %', missing_tables;
    END IF;

    SELECT array_agg(expected_name ORDER BY expected_name)
      INTO non_base_tables
      FROM unnest(expected_tables) AS expected(expected_name)
      JOIN information_schema.tables current_table
        ON current_table.table_schema = 'public'
       AND current_table.table_name = expected.expected_name
     WHERE current_table.table_type <> 'BASE TABLE';

    IF non_base_tables IS NOT NULL THEN
        RAISE EXCEPTION 'PATROL entities must be base tables, found: %', non_base_tables;
    END IF;
END $$;

DO $$
DECLARE
    check_row record;
    actual_count bigint;
BEGIN
    FOR check_row IN
        SELECT * FROM (VALUES
            ('runtime_field', 'module_code = ''PATROL_1.0.0''', 1368),
            ('ec_field', 'module_code = ''PATROL_1.0.0''', 1368),
            ('runtime_data_grid', 'module_code = ''PATROL_1.0.0''', 23),
            ('ec_data_grid', 'module_code = ''PATROL_1.0.0''', 23),
            ('runtime_button', 'module_code = ''PATROL_1.0.0''', 75),
            ('ec_button', 'module_code = ''PATROL_1.0.0''', 75),
            ('runtime_event', 'module_code = ''PATROL_1.0.0''', 699),
            ('ec_event', 'module_code = ''PATROL_1.0.0''', 699),
            ('runtime_extra_query_json', 'view_code LIKE ''PATROL_1.0.0_%''', 74),
            ('ec_extra_query_json', 'view_code LIKE ''PATROL_1.0.0_%''', 74),
            ('runtime_fast_query_json', 'view_code LIKE ''PATROL_1.0.0_%''', 32),
            ('ec_fast_query_json', 'view_code LIKE ''PATROL_1.0.0_%''', 32),
            ('runtime_adv_query_json', 'view_code LIKE ''PATROL_1.0.0_%''', 31),
            ('ec_adv_query_json', 'view_code LIKE ''PATROL_1.0.0_%''', 31),
            ('runtime_validate', 'module_code = ''PATROL_1.0.0''', 2),
            ('ec_validate', 'module_code = ''PATROL_1.0.0''', 2)
        ) AS expected(table_name, predicate, expected_count)
    LOOP
        EXECUTE format('SELECT count(*) FROM public.%I WHERE %s', check_row.table_name, check_row.predicate)
           INTO actual_count;
        IF actual_count <> check_row.expected_count THEN
            RAISE EXCEPTION '% expected %, got %', check_row.table_name, check_row.expected_count, actual_count;
        END IF;
    END LOOP;
END $$;

DO $$
DECLARE
    actual_count bigint;
BEGIN
    SELECT count(*) INTO actual_count
      FROM public.runtime_module
     WHERE code = 'PATROL_1.0.0';
    IF actual_count <> 1 THEN
        RAISE EXCEPTION 'runtime_module expected 1, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.runtime_entity
     WHERE module_code = 'PATROL_1.0.0';
    IF actual_count <> 7 THEN
        RAISE EXCEPTION 'runtime_entity expected 7, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.runtime_model
     WHERE module_code = 'PATROL_1.0.0';
    IF actual_count <> 27 THEN
        RAISE EXCEPTION 'runtime_model expected 27, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.ec_model
     WHERE module_code = 'PATROL_1.0.0'
       AND coalesce(valid, 0) = 1;
    IF actual_count <> 27 THEN
        RAISE EXCEPTION 'ec_model expected 27, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.ec_property
     WHERE module_code = 'PATROL_1.0.0'
       AND coalesce(valid, 0) = 1;
    IF actual_count <> 480 THEN
        RAISE EXCEPTION 'ec_property expected 480, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.runtime_property
     WHERE module_code = 'PATROL_1.0.0'
       AND associated_property_code IS NOT NULL;
    IF actual_count <> 103 THEN
        RAISE EXCEPTION 'runtime_property associations expected 103, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.ec_property
     WHERE module_code = 'PATROL_1.0.0'
       AND coalesce(valid, 0) = 1
       AND associated_property_code IS NOT NULL;
    IF actual_count <> 103 THEN
        RAISE EXCEPTION 'ec_property associations expected 103, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.runtime_view
     WHERE module_code = 'PATROL_1.0.0';
    IF actual_count <> 74 THEN
        RAISE EXCEPTION 'runtime_view expected 74, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.ec_view
     WHERE module_code = 'PATROL_1.0.0'
       AND coalesce(valid, 0) = 1;
    IF actual_count <> 74 THEN
        RAISE EXCEPTION 'ec_view expected 74, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.ec_extra_view
     WHERE view_code LIKE 'PATROL_1.0.0_%';
    IF actual_count <> 74 THEN
        RAISE EXCEPTION 'ec_extra_view expected 74, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.ec_extra_view extra_view
      CROSS JOIN LATERAL jsonb_path_query(
          extra_view.view_json::jsonb,
          '$.**.buttons[*] ? (@.ispermission == true)'
      ) permission_button
     WHERE extra_view.view_code LIKE 'PATROL_1.0.0_%'
       AND coalesce(permission_button->>'pc', '') LIKE '__pc__=%';
    IF actual_count <> 76 THEN
        RAISE EXCEPTION 'PATROL permission buttons with power codes expected 76, got %', actual_count;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.ec_extra_view extra_view
          CROSS JOIN LATERAL jsonb_path_query(
              extra_view.view_json::jsonb,
              '$.**.buttons[*] ? (@.ispermission == true)'
          ) permission_button
         WHERE extra_view.view_code LIKE 'PATROL_1.0.0_%'
           AND coalesce(permission_button->>'pc', '') NOT LIKE '__pc__=%'
    ) THEN
        RAISE EXCEPTION 'PATROL contains permission buttons without a runtime power code';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.ec_extra_view
         WHERE code = 'PATROL_1.0.0_patrolRoute_workGroupList'
           AND view_json::jsonb->>'pageType' = 'EDIT'
           AND jsonb_path_exists(view_json::jsonb, '$.** ? (@.type == "layoutSection")')
    ) THEN
        RAISE EXCEPTION 'PATROL route editor must retain the packaged EDIT/layoutSection runtime layout';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.ec_extra_view extra_view
          CROSS JOIN LATERAL jsonb_path_query(
              extra_view.view_json::jsonb,
              '$.** ? (@.customSection == true && !exists(@.propertyCode) && !exists(@.fullPropertyCode))'
          ) placeholder
         WHERE extra_view.view_code LIKE 'PATROL_1.0.0_%'
    ) THEN
        RAISE EXCEPTION 'PATROL contains unresolved tenant custom-property placeholders';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.ec_view
         WHERE code = 'PATROL_1.0.0_inputStandard_inputStanList'
           AND url = '/msService/PATROL/inputStandard/inputStandard/inputStanList'
           AND ass_model_code = 'PATROL_1.0.0_inputStandard_InputStandard'
    ) THEN
        RAISE EXCEPTION 'PATROL input standard EC URL/model mapping is missing';
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.action_view
     WHERE view_code LIKE 'PATROL_1.0.0_%';
    IF actual_count <> 166 THEN
        RAISE EXCEPTION 'PATROL action routes expected 166, got %', actual_count;
    END IF;

    WITH expected(action_url) AS (
        VALUES
            ('/PATROL/inputStandard/inputStandard/inputStanList'),
            ('/PATROL/inputStandard/inputStandard/inputStanList-query'),
            ('/PATROL/inputStandard/inputStandard/inputStanList-getRequireData'),
            ('/PATROL/inputStandard/inputStandard/inputStanList-pending')
    )
    SELECT count(*) INTO actual_count
      FROM expected
      JOIN public.action_view action
        ON action.action_url = expected.action_url
       AND action.view_code = 'PATROL_1.0.0_inputStandard_inputStanList';
    IF actual_count <> 4 THEN
        RAISE EXCEPTION 'PATROL input standard action routes expected 4, got %', actual_count;
    END IF;

    WITH expected(action_url, view_code) AS (
        VALUES
            ('/PATROL/patrolTask/potrolTask/potrolTaskList-pending', 'PATROL_1.0.0_patrolTask_potrolTaskList'),
            ('/PATROL/patrolTask/potrolTask/tempTaskList-pending', 'PATROL_1.0.0_patrolTask_tempTaskList')
    )
    SELECT count(*) INTO actual_count
      FROM expected
      JOIN public.action_view action
        ON action.action_url = expected.action_url
       AND action.view_code = expected.view_code;
    IF actual_count <> 2 THEN
        RAISE EXCEPTION 'PATROL pending action routes expected 2, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.runtime_customer_condition
     WHERE module_code = 'PATROL_1.0.0';
    IF actual_count <> 79 THEN
        RAISE EXCEPTION 'runtime_customer_condition expected 79, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.runtime_sql
     WHERE view_code IN (
        SELECT code
          FROM public.runtime_view
         WHERE module_code = 'PATROL_1.0.0'
     );
    IF actual_count <> 209 THEN
        RAISE EXCEPTION 'runtime_sql expected 209, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.runtime_sql
     WHERE view_code LIKE 'PATROL_1.0.0_%'
       AND data_grid_code IS NOT NULL
       AND data_grid_code <> '';
    IF actual_count <> 131 THEN
        RAISE EXCEPTION 'runtime_sql grid bindings expected 131, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.ec_sql
     WHERE view_code LIKE 'PATROL_1.0.0_%'
       AND data_grid_code IS NOT NULL
       AND data_grid_code <> '';
    IF actual_count <> 131 THEN
        RAISE EXCEPTION 'ec_sql grid bindings expected 131, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.runtime_sql
     WHERE data_grid_code = 'PATROL_1.0.0_patrolRoute_workGroupListdg1575506219664'
       AND type IN (3, 4, 6);
    IF actual_count <> 3 THEN
        RAISE EXCEPTION 'PATROL route-list SQL grid bindings expected 3, got %', actual_count;
    END IF;
END $$;

DO $$
DECLARE
    actual_count bigint;
BEGIN
    SELECT count(*) INTO actual_count
      FROM public.sys_entity
     WHERE code LIKE 'PATROL\_%' ESCAPE '\'
       AND coalesce(valid, 0) = 1;
    IF actual_count <> 19 THEN
        RAISE EXCEPTION 'PATROL system-code entities expected 19, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.sys_code
     WHERE entity_code LIKE 'PATROL\_%' ESCAPE '\'
       AND coalesce(valid, 0) = 1;
    IF actual_count <> 58 THEN
        RAISE EXCEPTION 'PATROL system-code values expected 58, got %', actual_count;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.sys_code
         WHERE entity_code = 'PATROL_valueType'
           AND code = 'char'
           AND display_name = '字符'
           AND coalesce(valid, 0) = 1
    ) OR NOT EXISTS (
        SELECT 1
          FROM public.sys_code
         WHERE entity_code = 'PATROL_editType'
           AND code = 'input'
           AND display_name = '录入'
           AND coalesce(valid, 0) = 1
    ) OR NOT EXISTS (
        SELECT 1
          FROM public.sys_code
         WHERE entity_code = 'PATROL_routeType'
           AND code = 'ses'
           AND display_name = '安环巡检'
           AND coalesce(valid, 0) = 1
    ) THEN
        RAISE EXCEPTION 'PATROL critical system-code values are missing';
    END IF;
END $$;

DO $$
DECLARE
    incompatible_columns text[];
BEGIN
    SELECT array_agg(table_name || '.' || column_name || '=' || data_type ORDER BY table_name, column_name)
      INTO incompatible_columns
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND (
            (table_name IN ('mp_public_item_trees', 'mp_work_groups')
             AND column_name = 'oa'
             AND data_type <> 'text')
         OR (table_name IN ('mp_public_item_trees', 'mp_work_groups')
             AND column_name = 'sort'
             AND data_type <> 'bigint')
       );

    IF incompatible_columns IS NOT NULL THEN
        RAISE EXCEPTION 'PATROL incompatible column types: %', incompatible_columns;
    END IF;
END $$;

DO $$
DECLARE
    default_value text;
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.mp_potrol_tasks
         WHERE patrol_plan IS DISTINCT FROM patrol_plan_id
           AND (patrol_plan IS NOT NULL OR patrol_plan_id IS NOT NULL)
    ) THEN
        RAISE EXCEPTION 'PATROL task plan association columns are inconsistent';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.mp_task_details detail
          JOIN public.mp_potrol_tasks task ON task.id = detail.patrol_task
         WHERE (
                   task.task_state IS NULL
                   OR task.task_state = 'PATROL_taskState/notIssued'
               )
           AND (
                   detail.valid IS NULL
                   OR detail.version IS NULL
                   OR detail.task_detail_state IS NULL
               )
    ) THEN
        RAISE EXCEPTION 'PATROL pending task details contain null lifecycle fields';
    END IF;

    SELECT column_default
      INTO default_value
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'mp_task_details'
       AND column_name = 'valid';
    IF coalesce(default_value, '') NOT IN ('true', 'true::boolean') THEN
        RAISE EXCEPTION 'mp_task_details.valid default is %, expected true', default_value;
    END IF;

    SELECT column_default
      INTO default_value
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'mp_task_details'
       AND column_name = 'version';
    IF coalesce(default_value, '') NOT IN ('0', '0::integer') THEN
        RAISE EXCEPTION 'mp_task_details.version default is %, expected 0', default_value;
    END IF;

    SELECT column_default
      INTO default_value
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'mp_task_details'
       AND column_name = 'task_detail_state';
    IF coalesce(default_value, '') NOT LIKE '''PATROL_taskDetailState/pending''%' THEN
        RAISE EXCEPTION
            'mp_task_details.task_detail_state default is %, expected pending',
            default_value;
    END IF;
END $$;

DO $$
DECLARE
    actual_count bigint;
BEGIN
    SELECT count(*) INTO actual_count
      FROM public.mp_input_standards
     WHERE code IN ('M-RJY-1-001', 'M-RJY-1-002', 'M-RJY-1-003');
    IF actual_count <> 3 THEN
        RAISE EXCEPTION 'PATROL input standards expected 3, got %', actual_count;
    END IF;

    WITH expected(value_name) AS (
        VALUES ('是'), ('否'), ('YES'), ('NO')
    )
    SELECT count(*) INTO actual_count
      FROM expected
      JOIN public.mp_input_standards standards
        ON standards.code = 'M-RJY-1-002'
      JOIN public.mp_candidate_values candidate
        ON candidate.input_standard_id = standards.id
       AND candidate.value_name = expected.value_name;
    IF actual_count <> 4 THEN
        RAISE EXCEPTION 'PATROL candidate values expected 4, got %', actual_count;
    END IF;

    WITH expected(standard_code, mne_code) AS (
        VALUES
            ('M-RJY-1-001', 'character'),
            ('M-RJY-1-001', 'character input'),
            ('M-RJY-1-001', 'm-rjy-1-00'),
            ('M-RJY-1-001', 'm-rjy-1-001'),
            ('M-RJY-1-001', 'zflr'),
            ('M-RJY-1-001', 'zifuluru'),
            ('M-RJY-1-001', '字符录入'),
            ('M-RJY-1-002', 'yes or no'),
            ('M-RJY-1-002', 'm-rjy-1-00'),
            ('M-RJY-1-002', 'm-rjy-1-002'),
            ('M-RJY-1-002', 'sf'),
            ('M-RJY-1-002', 'shifou'),
            ('M-RJY-1-002', 'shipi'),
            ('M-RJY-1-002', 'sp'),
            ('M-RJY-1-002', '是否'),
            ('M-RJY-1-003', 'digital'),
            ('M-RJY-1-003', 'digital input'),
            ('M-RJY-1-003', 'm-rjy-1-00'),
            ('M-RJY-1-003', 'm-rjy-1-003'),
            ('M-RJY-1-003', 'shuziluru'),
            ('M-RJY-1-003', 'shuoziluru'),
            ('M-RJY-1-003', 'szlr'),
            ('M-RJY-1-003', '数字录入')
    )
    SELECT count(*) INTO actual_count
      FROM expected
      JOIN public.mp_input_standards standards
        ON standards.code = expected.standard_code
      JOIN public.mp_input_standards_mc mnemonic
        ON COALESCE(mnemonic.input_standard_id, mnemonic.input_standard) = standards.id
       AND mnemonic.mne_code = expected.mne_code;
    IF actual_count <> 23 THEN
        RAISE EXCEPTION 'PATROL mnemonic values expected 23, got %', actual_count;
    END IF;
END $$;

DO $$
DECLARE
    actual_count bigint;
    missing_app_menus text[];
BEGIN
    SELECT count(*) INTO actual_count
      FROM public.rbac_menuinfo
     WHERE module_code = 'PATROL_1.0.0'
       AND app = 'EAM'
       AND coalesce(valid, false)
       AND coalesce(enable, false);
    IF actual_count <> 24 THEN
        RAISE EXCEPTION 'PATROL enabled EAM menus expected 24, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.rbac_menuoperate
     WHERE module_code = 'PATROL_1.0.0'
       AND app = 'EAM'
       AND coalesce(valid, 0) = 1;
    IF actual_count <> 102 THEN
        RAISE EXCEPTION 'PATROL normalized EAM operations expected 102, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.rbac_rolepermission permission
      JOIN public.rbac_menuoperate operation
        ON operation.id = permission.menuoperate_id
     WHERE permission.role_id = 1
       AND operation.module_code = 'PATROL_1.0.0'
       AND coalesce(operation.valid, 0) = 1;
    IF actual_count <> 102 THEN
        RAISE EXCEPTION 'PATROL admin operation permissions expected 102, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.rbac_userpermission permission
      JOIN public.rbac_menuoperate operation
        ON operation.id = permission.menuoperate_id
     WHERE permission.user_id = 1
       AND permission.cid = 1000
       AND operation.module_code = 'PATROL_1.0.0'
       AND operation.app = 'EAM'
       AND coalesce(operation.valid, 0) = 1
       AND coalesce(permission.no_restrict_flag, 0) = 1;
    IF actual_count <> 102 THEN
        RAISE EXCEPTION 'PATROL admin direct-user permissions expected 102, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.rbac_menuinfo menu
     WHERE menu.module_code = 'PATROL_1.0.0'
       AND coalesce(menu.valid, false)
       AND EXISTS (
           SELECT 1
             FROM public.rbac_menuinfo_company_ref company_ref
            WHERE company_ref.menuinfo_id = menu.id
              AND company_ref.company_id IN (1000, -1)
              AND company_ref.appid = 'EAM'
       );
    IF actual_count <> 24 THEN
        RAISE EXCEPTION 'PATROL EAM company-menu references expected 24, got %', actual_count;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.rbac_menuinfo menu
         WHERE menu.module_code = 'PATROL_1.0.0'
           AND coalesce(menu.valid, false)
           AND (
               menu.parent_id IS NULL
               OR menu.lay_rec IS NULL
               OR menu.full_path IS NULL
               OR menu.full_path_name IS NULL
           )
    ) THEN
        RAISE EXCEPTION 'PATROL menu hierarchy contains unresolved paths';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.supos_app
         WHERE code = 'eamms'
           AND 'PATROL_1.0.0' = ANY(string_to_array(coalesce(modules, ''), ','))
    ) THEN
        RAISE EXCEPTION 'eamms application does not contain PATROL_1.0.0';
    END IF;

    WITH expected(menu_code) AS (
        VALUES
            ('PATROL_1.0.0_patrolMonit_patrolMonitMap'),
            ('PATROL_1.0.0_patrolTask_errorAnalysisMenu'),
            ('PATROL_1.0.0_patrolTask_stabilityRAMenu'),
            ('PATROL_1.0.0_patrolTask_taskFinishStatistics'),
            ('PATROL_1.0.0_patrolTask_unusualSummary'),
            ('PATROL_1.0.0_patrolTask_taskOverviewList'),
            ('PATROL_1.0.0_patrolTask_abnormalSummary'),
            ('PATROL_1.0.0_patrolTask_potrolResultSummary'),
            ('PATROL_1.0.0_patrolTask_enteringResultList'),
            ('PATROL_1.0.0_patrolTask_batchChangeList'),
            ('PATROL_1.0.0_patrolTask_tempTaskList'),
            ('PATROL_1.0.0_patrolTask_potrolTaskList'),
            ('PATROL_1.0.0_patrolPlan_createTaskList'),
            ('PATROL_1.0.0_patrolPlan_potrolPlanList'),
            ('PATROL_1.0.0_patrolRoute_workRouteLayout'),
            ('PATROL_1.0.0_patrolRoute_workGroupList'),
            ('PATROL_1.0.0_inputStandard_inputStanList'),
            ('PATROL_1.0.0_publicItem_itemLayout')
    )
    SELECT array_agg(expected.menu_code ORDER BY expected.menu_code)
      INTO missing_app_menus
      FROM expected
     WHERE NOT EXISTS (
         SELECT 1
           FROM public.supos_app app
          WHERE app.code = 'eamms'
            AND expected.menu_code = ANY(string_to_array(coalesce(app.menus, ''), ','))
     );
    IF missing_app_menus IS NOT NULL THEN
        RAISE EXCEPTION 'eamms application is missing PATROL menus: %', missing_app_menus;
    END IF;
END $$;

DO $$
DECLARE
    actual_count bigint;
BEGIN
    SELECT count(*) INTO actual_count
      FROM public.wf_deployment deployment
      JOIN public.rbac_menuinfo menu ON menu.id = deployment.menu_info_id
     WHERE deployment.process_key IN ('potrolTaskWF', 'tempTaskWF')
       AND coalesce(deployment.valid, 0) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
       AND coalesce(deployment.publish_flag, 0) = 1
       AND menu.code = deployment.menu_code
       AND deployment.process_definition_id = deployment.process_key || '-1'
       AND deployment.process_xml IS NOT NULL
       AND deployment.temp_process_xml IS NOT NULL
       AND convert_from(lo_get(deployment.process_xml), 'UTF8') = deployment.process_xml_text_backup
       AND convert_from(lo_get(deployment.temp_process_xml), 'UTF8') = deployment.temp_process_xml_text_backup;
    IF actual_count <> 2 THEN
        RAISE EXCEPTION 'PATROL published workflow deployments expected 2, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.wf_task task
      JOIN public.wf_deployment deployment ON deployment.id = task.deployment_id
     WHERE deployment.process_key IN ('potrolTaskWF', 'tempTaskWF')
       AND coalesce(deployment.is_current_version, 0) = 1
       AND coalesce(task.valid, 0) = 1;
    IF actual_count <> 10 THEN
        RAISE EXCEPTION 'PATROL workflow tasks expected 10, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.wf_transition transition_row
      JOIN public.wf_deployment deployment ON deployment.id = transition_row.deployment_id
     WHERE deployment.process_key IN ('potrolTaskWF', 'tempTaskWF')
       AND coalesce(deployment.is_current_version, 0) = 1
       AND coalesce(transition_row.valid, 0) = 1;
    IF actual_count <> 10 THEN
        RAISE EXCEPTION 'PATROL workflow transitions expected 10, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.rbac_flow_permission
     WHERE flow_key IN ('potrolTaskWF', 'tempTaskWF')
       AND flow_permission_type = 'USER'
       AND type_id = 1
       AND coalesce(purview_state, 0) = 1
       AND coalesce(unlimited_power, false);
    IF actual_count <> 4 THEN
        RAISE EXCEPTION 'PATROL workflow admin permissions expected 4, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.jbpm4_deployment deployment
     WHERE deployment.state_ = 'active'
       AND EXISTS (
           SELECT 1
             FROM public.jbpm4_deployprop property
            WHERE property.deployment_ = deployment.dbid_
              AND property.key_ = 'pdkey'
              AND property.stringval_ IN ('potrolTaskWF', 'tempTaskWF')
       );
    IF actual_count <> 2 THEN
        RAISE EXCEPTION 'PATROL active JBPM deployments expected 2, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.jbpm4_deployprop
     WHERE objname_ IN ('potrolTaskWF', 'tempTaskWF');
    IF actual_count <> 8 THEN
        RAISE EXCEPTION 'PATROL JBPM deployment properties expected 8, got %', actual_count;
    END IF;

    SELECT count(*) INTO actual_count
      FROM public.jbpm4_lob lob
      JOIN public.jbpm4_deployprop property
        ON property.deployment_ = lob.deployment_
       AND property.key_ = 'pdkey'
     WHERE property.stringval_ IN ('potrolTaskWF', 'tempTaskWF')
       AND lob.blob_value_ IS NOT NULL
       AND position(property.stringval_ IN convert_from(lo_get(lob.blob_value_), 'UTF8')) > 0;
    IF actual_count <> 2 THEN
        RAISE EXCEPTION 'PATROL JBPM process XML LOBs expected 2, got %', actual_count;
    END IF;
END $$;

BEGIN;

INSERT INTO public.mp_input_standards_mc(id, mne_code, input_standard_id)
VALUES (9223372036854700001, 'ADP_PATROL_SYNC_ID', 123456);
INSERT INTO public.mp_input_standards_mc(id, mne_code, input_standard)
VALUES (9223372036854700002, 'ADP_PATROL_SYNC_LEGACY', 654321);
INSERT INTO public.mp_work_groups_mc(id, mne_code, work_group_id)
VALUES (9223372036854700003, 'ADP_PATROL_SYNC_ID', 111111);
INSERT INTO public.mp_work_groups_mc(id, mne_code, work_group)
VALUES (9223372036854700004, 'ADP_PATROL_SYNC_LEGACY', 222222);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.mp_input_standards_mc
         WHERE id = 9223372036854700001
           AND input_standard_id = 123456
           AND input_standard = 123456
    ) OR NOT EXISTS (
        SELECT 1 FROM public.mp_input_standards_mc
         WHERE id = 9223372036854700002
           AND input_standard_id = 654321
           AND input_standard = 654321
    ) THEN
        RAISE EXCEPTION 'PATROL input-standard reference trigger did not synchronize both columns';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.mp_work_groups_mc
         WHERE id = 9223372036854700003
           AND work_group_id = 111111
           AND work_group = 111111
    ) OR NOT EXISTS (
        SELECT 1 FROM public.mp_work_groups_mc
         WHERE id = 9223372036854700004
           AND work_group_id = 222222
           AND work_group = 222222
    ) THEN
        RAISE EXCEPTION 'PATROL work-group reference trigger did not synchronize both columns';
    END IF;
END $$;

ROLLBACK;

SELECT
    37 AS entity_tables,
    1 AS modules,
    7 AS entities,
    27 AS models,
    74 AS views,
    79 AS conditions,
    209 AS sql_definitions,
    1368 AS fields,
    23 AS data_grids,
    75 AS buttons,
    699 AS events,
    19 AS system_code_entities,
    58 AS system_code_values,
    24 AS menus,
    102 AS operations,
    2 AS workflows,
    'PASS' AS status;
