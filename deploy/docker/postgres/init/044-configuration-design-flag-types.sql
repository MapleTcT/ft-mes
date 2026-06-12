-- Module XML import writes design-time metadata flags as MySQL-style 0/1
-- values through native JDBC batch inserts. Keep EC design tables integer-based
-- while runtime_* tables remain PostgreSQL booleans for page rendering.

DO $$
DECLARE
  item RECORD;
  current_default text;
  default_sql text;
BEGIN
  FOR item IN
    SELECT * FROM (VALUES
      ('ec_adv_query_condition', 'valid'),
      ('ec_adv_query_condition_item', 'valid'),
      ('ec_adv_query_json', 'proj_flag'),
      ('ec_backup_data_grid', 'ex'),
      ('ec_backup_data_grid', 'valid'),
      ('ec_backup_view', 'valid'),
      ('ec_button', 'is_callback'),
      ('ec_button', 'is_confirm'),
      ('ec_button', 'is_custom_func'),
      ('ec_button', 'is_hide'),
      ('ec_button', 'is_permission'),
      ('ec_button', 'is_published'),
      ('ec_button', 'is_signature_config'),
      ('ec_button', 'is_use_more'),
      ('ec_button', 'proj_flag'),
      ('ec_button', 'signature_enabled'),
      ('ec_button', 'valid'),
      ('ec_customer_condition', 'proj_flag'),
      ('ec_customer_condition', 'valid'),
      ('ec_data_classific', 'is_default'),
      ('ec_data_classific', 'proj_flag'),
      ('ec_data_classific', 'valid'),
      ('ec_data_grid', 'ex'),
      ('ec_data_grid', 'is_permission'),
      ('ec_data_grid', 'proj_flag'),
      ('ec_data_grid', 'valid'),
      ('ec_data_group', 'is_multiple'),
      ('ec_data_group', 'proj_flag'),
      ('ec_data_group', 'valid'),
      ('ec_echarts', 'is_first_load'),
      ('ec_echarts', 'is_show_legend'),
      ('ec_echarts', 'is_show_magic_type'),
      ('ec_echarts', 'proj_flag'),
      ('ec_echarts', 'valid'),
      ('ec_echarts_model', 'is_custom_conditions'),
      ('ec_echarts_model', 'proj_flag'),
      ('ec_echarts_model', 'valid'),
      ('ec_entity', 'cross_company_flag'),
      ('ec_entity', 'enable_acl_restrict'),
      ('ec_entity', 'enable_audit'),
      ('ec_entity', 'enable_fields_permission_conf'),
      ('ec_entity', 'enable_rest'),
      ('ec_entity', 'enable_ws'),
      ('ec_entity', 'group_enabled'),
      ('ec_entity', 'inherent_common_flag'),
      ('ec_entity', 'is_base'),
      ('ec_entity', 'is_control'),
      ('ec_entity', 'is_inherented_base'),
      ('ec_entity', 'mobile'),
      ('ec_entity', 'pay_close_attention'),
      ('ec_entity', 'proj_flag'),
      ('ec_entity', 'valid'),
      ('ec_entity', 'workflow_enabled'),
      ('ec_event', 'proj_flag'),
      ('ec_event', 'valid'),
      ('ec_extra_query_json', 'proj_flag'),
      ('ec_extra_view', 'proj_flag'),
      ('ec_fast_query_json', 'proj_flag'),
      ('ec_field', 'is_hidden'),
      ('ec_field', 'proj_flag'),
      ('ec_field', 'valid'),
      ('ec_import_template', 'proj_flag'),
      ('ec_layout', 'valid'),
      ('ec_model', 'enable_data_audit'),
      ('ec_model', 'enable_operation_audit'),
      ('ec_model', 'enable_sync'),
      ('ec_model', 'inherent_common_flag'),
      ('ec_model', 'is_cache'),
      ('ec_model', 'is_config_special'),
      ('ec_model', 'is_control'),
      ('ec_model', 'is_extends'),
      ('ec_model', 'is_extra_col'),
      ('ec_model', 'is_main'),
      ('ec_model', 'is_mne_code'),
      ('ec_model', 'proj_flag'),
      ('ec_model', 'special_auth_isandrel'),
      ('ec_model', 'valid'),
      ('ec_module', 'is_hide'),
      ('ec_module', 'is_inherented_base'),
      ('ec_module', 'is_new_generate'),
      ('ec_module', 'is_proto'),
      ('ec_module', 'is_read_only'),
      ('ec_module', 'main_module'),
      ('ec_module', 'proj_flag'),
      ('ec_module', 'valid'),
      ('ec_module_reference', 'valid'),
      ('ec_module_relation', 'proj_flag'),
      ('ec_module_relation', 'valid'),
      ('ec_msmodule', 'is_hide'),
      ('ec_msmodule', 'is_inherented_base'),
      ('ec_msmodule', 'is_new_generate'),
      ('ec_msmodule', 'is_read_only'),
      ('ec_msmodule', 'proj_flag'),
      ('ec_msmodule', 'valid'),
      ('ec_msmodule_ipadress', 'valid'),
      ('ec_msmodule_relation', 'valid'),
      ('ec_other_restrict', 'hand_writing_flag'),
      ('ec_other_restrict', 'valid'),
      ('ec_portlet', 'iframe_flag'),
      ('ec_portlet', 'is_default'),
      ('ec_portlet', 'is_hidden'),
      ('ec_portlet', 'power_flag'),
      ('ec_print_template', 'extra_param'),
      ('ec_print_template', 'proj_flag'),
      ('ec_print_template', 'template_enabled'),
      ('ec_print_template', 'valid'),
      ('ec_property', 'is_bussiness_key'),
      ('ec_property', 'is_control'),
      ('ec_property', 'is_custom'),
      ('ec_property', 'is_engine'),
      ('ec_property', 'is_group_object'),
      ('ec_property', 'is_hidden'),
      ('ec_property', 'is_ignore_audit'),
      ('ec_property', 'is_index'),
      ('ec_property', 'is_inherent'),
      ('ec_property', 'is_main_associated'),
      ('ec_property', 'is_main_display'),
      ('ec_property', 'is_mne_whole_like_query'),
      ('ec_property', 'is_pk'),
      ('ec_property', 'is_sensitive'),
      ('ec_property', 'is_unique'),
      ('ec_property', 'is_used_for_list'),
      ('ec_property', 'is_used_for_search'),
      ('ec_property', 'is_used_mne_code'),
      ('ec_property', 'multable'),
      ('ec_property', 'no_analyzer'),
      ('ec_property', 'nullable'),
      ('ec_property', 'only_leaf'),
      ('ec_property', 'proj_custom_in_use'),
      ('ec_property', 'proj_flag'),
      ('ec_property', 'senior_system_code'),
      ('ec_property', 'stretch'),
      ('ec_property', 'valid'),
      ('ec_special_permission', 'is_tree'),
      ('ec_special_permission', 'valid'),
      ('ec_special_permission_rshow', 'is_assigned'),
      ('ec_special_permission_rshow', 'valid'),
      ('ec_special_permission_ushow', 'is_assigned'),
      ('ec_special_permission_ushow', 'valid'),
      ('ec_sql', 'proj_flag'),
      ('ec_table_info', 'valid'),
      ('ec_validate', 'proj_flag'),
      ('ec_validate', 'valid'),
      ('ec_view', 'attachment_flag'),
      ('ec_view', 'close_page_after_save'),
      ('ec_view', 'control_print'),
      ('ec_view', 'custom_flag'),
      ('ec_view', 'deal_info_show'),
      ('ec_view', 'enable_simple_deal_info'),
      ('ec_view', 'has_attachment'),
      ('ec_view', 'has_custom_section'),
      ('ec_view', 'import_flag'),
      ('ec_view', 'include_children'),
      ('ec_view', 'is_audit'),
      ('ec_view', 'is_batch_control_print'),
      ('ec_view', 'is_control'),
      ('ec_view', 'is_hand_sign'),
      ('ec_view', 'is_permission'),
      ('ec_view', 'is_print'),
      ('ec_view', 'is_reference'),
      ('ec_view', 'is_shadow'),
      ('ec_view', 'is_sign'),
      ('ec_view', 'main_ref'),
      ('ec_view', 'main_view'),
      ('ec_view', 'mobile'),
      ('ec_view', 'mobile_enable_flag'),
      ('ec_view', 'only_for_query'),
      ('ec_view', 'proj_enabled'),
      ('ec_view', 'proj_flag'),
      ('ec_view', 'retrial_flag'),
      ('ec_view', 'used_for_tree'),
      ('ec_view', 'used_for_work_flow'),
      ('ec_view', 'valid')
    ) AS columns(table_name, column_name)
  LOOP
    SELECT column_default
    INTO current_default
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = item.table_name
      AND column_name = item.column_name
      AND data_type = 'boolean';

    IF FOUND THEN
      IF current_default ILIKE '%true%' THEN
        default_sql := '1';
      ELSIF current_default ILIKE '%false%' THEN
        default_sql := '0';
      ELSIF item.column_name = 'valid' THEN
        default_sql := '1';
      ELSE
        default_sql := NULL;
      END IF;

      EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I DROP DEFAULT', item.table_name, item.column_name);
      EXECUTE format(
        'ALTER TABLE public.%I ALTER COLUMN %I TYPE integer USING CASE WHEN %I THEN 1 ELSE 0 END',
        item.table_name,
        item.column_name,
        item.column_name
      );

      IF default_sql IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I SET DEFAULT %s', item.table_name, item.column_name, default_sql);
      END IF;
    END IF;
  END LOOP;
END $$;
