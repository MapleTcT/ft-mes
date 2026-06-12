-- Restore core sysbase model/property metadata needed by standalone MES modules.
--
-- The module import packages reference platform-owned entities such as
-- department, staff, position, and company. Some rebuilt PostgreSQL test
-- databases contain the base tables and entities but miss the matching
-- ec_model/runtime_model and ec_property/runtime_property rows. Without these
-- rows BAP association resolution fails at runtime, for example on
-- base_department_id used by DocManage document lists.

WITH model_seed (
  code, runtime_version, jpa_name, table_name, data_type, entity_code,
  model_name, value_zh_cn, name, ec_entity_class, runtime_entity_class,
  is_mne_code
) AS (
  VALUES
    ('sysbase_1.0_company_base_company', 1, 'Company', 'BASE_COMPANY', 1, 'sysbase_1.0_company',
     'Company', '公司', 'foundation.login.company',
     'com.supcon.supfusion.base.entities.Company', 'com.supcon.orchid.foundation.entities.Company', false),
    ('sysbase_1.0_department_base_department', 2, 'Department', 'BASE_DEPARTMENT', 2, 'sysbase_1.0_department',
     'Department', '部门', 'foundation.workbench.filedownloadinfo.dept',
     'com.supcon.supfusion.base.entities.Department', 'com.supcon.orchid.foundation.entities.Department', true),
    ('sysbase_1.0_position_base_position', 2, 'Position', 'BASE_POSITION', 2, 'sysbase_1.0_position',
     'Position', '岗位', 'foundation.ec.entity.position',
     'com.supcon.supfusion.base.entities.Position', 'com.supcon.orchid.foundation.entities.Position', true),
    ('sysbase_1.0_staff_base_staff', 2, 'Staff', 'base_staff', 1, 'sysbase_1.0_staff',
     'Staff', '人员', 'foundation.ec.entity.staff',
     'com.supcon.supfusion.base.entities.Staff', 'com.supcon.orchid.foundation.entities.Staff', true)
)
INSERT INTO ec_model (
  code, ec_env, version, valid, is_error_sql, is_config_special,
  special_auth_isandrel, is_mne_code, is_control, is_cache, entity_class,
  is_extra_col, enable_data_audit, enable_operation_audit, enable_sync,
  table_name, inherent_common_flag, ec_version, jpa_name, module_code,
  is_extends, type, data_type, is_main, entity_code, model_name,
  value_zh_cn, name
)
SELECT
  code, 'product', 0, 1, 0, 0,
  0, CASE WHEN is_mne_code THEN 1 ELSE 0 END, 0, 1, ec_entity_class,
  0, 0, 0, 0,
  table_name, 1, '1.0', jpa_name, 'sysbase_1.0',
  0, 2, data_type, 1, entity_code, model_name,
  value_zh_cn, name
FROM model_seed
ON CONFLICT (code) DO NOTHING;

WITH model_seed (
  code, runtime_version, jpa_name, table_name, data_type, entity_code,
  model_name, value_zh_cn, name, ec_entity_class, runtime_entity_class,
  is_mne_code
) AS (
  VALUES
    ('sysbase_1.0_company_base_company', 1, 'Company', 'BASE_COMPANY', 1, 'sysbase_1.0_company',
     'Company', '公司', 'foundation.login.company',
     'com.supcon.supfusion.base.entities.Company', 'com.supcon.orchid.foundation.entities.Company', false),
    ('sysbase_1.0_department_base_department', 2, 'Department', 'BASE_DEPARTMENT', 2, 'sysbase_1.0_department',
     'Department', '部门', 'foundation.workbench.filedownloadinfo.dept',
     'com.supcon.supfusion.base.entities.Department', 'com.supcon.orchid.foundation.entities.Department', true),
    ('sysbase_1.0_position_base_position', 2, 'Position', 'BASE_POSITION', 2, 'sysbase_1.0_position',
     'Position', '岗位', 'foundation.ec.entity.position',
     'com.supcon.supfusion.base.entities.Position', 'com.supcon.orchid.foundation.entities.Position', true),
    ('sysbase_1.0_staff_base_staff', 2, 'Staff', 'base_staff', 1, 'sysbase_1.0_staff',
     'Staff', '人员', 'foundation.ec.entity.staff',
     'com.supcon.supfusion.base.entities.Staff', 'com.supcon.orchid.foundation.entities.Staff', true)
)
INSERT INTO runtime_model (
  code, ec_env, version, valid, is_error_sql, is_config_special,
  special_auth_isandrel, is_mne_code, is_control, is_cache, entity_class,
  is_extra_col, enable_data_audit, enable_operation_audit, enable_sync,
  table_name, inherent_common_flag, ec_version, jpa_name, module_code,
  is_extends, type, data_type, is_main, entity_code, model_name,
  value_zh_cn, name
)
SELECT
  code, 'product', runtime_version, true, 0, false,
  false, is_mne_code, false, true, runtime_entity_class,
  false, false, false, false,
  table_name, true, '1.0', jpa_name, 'sysbase_1.0',
  false, 2, data_type, true, entity_code, model_name,
  value_zh_cn, name
FROM model_seed
ON CONFLICT (code) DO NOTHING;

WITH property_seed (
  code, entity_code, model_code, column_name, type, field_type, format,
  display_name, name, max_length, nullable, is_pk, is_bussiness_key,
  is_used_for_list, is_main_display, is_used_mne_code, associated_type,
  is_inherent
) AS (
  VALUES
    ('base_company_id', 'sysbase_1.0_company', 'sysbase_1.0_company_base_company', 'ID', 'LONG', 'TEXTFIELD', 'TEXT', 'ID', 'id', 19, false, true, false, false, false, false, NULL::integer, true),
    ('base_company_code', 'sysbase_1.0_company', 'sysbase_1.0_company_base_company', 'CODE', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.company.code', 'code', 80, true, false, true, true, false, false, NULL::integer, false),
    ('base_company_name', 'sysbase_1.0_company', 'sysbase_1.0_company_base_company', 'NAME', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.company.name', 'name', 80, true, false, false, true, true, true, NULL::integer, false),

    ('base_department_id', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'ID', 'LONG', 'TEXTFIELD', 'TEXT', 'ID', 'id', 19, false, true, false, false, false, false, NULL::integer, true),
    ('base_department_code', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'CODE', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.department.code', 'code', 80, false, false, true, true, false, false, NULL::integer, false),
    ('base_department_name', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'NAME', 'TEXT', 'TEXTFIELD', 'TEXT', 'part.fzwdy.xm.mainPositionId.department.name', 'name', 80, false, false, false, true, true, true, 1, false),
    ('base_department_description', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'DESCRIPTION', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.department.description', 'description', 255, true, false, false, true, false, false, NULL::integer, false),
    ('base_department_layRec', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'LAY_REC', 'TEXT', 'TEXTFIELD', 'TEXT', 'layRec', 'layRec', 255, true, false, false, false, false, false, NULL::integer, false),
    ('base_department_parentId', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'PARENT_ID', 'INTEGER', 'TEXTFIELD', 'TEXT', 'foundation.ec.department.superiorDep', 'parentId', 19, true, false, false, false, false, false, NULL::integer, false),
    ('base_department_leaf', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'LEAF', 'BOOLEAN', 'CHECKBOX', 'CHECKBOX', 'foundation.systemCode.attribute', 'leaf', 0, true, false, false, false, false, false, NULL::integer, false),

    ('base_position_id', 'sysbase_1.0_position', 'sysbase_1.0_position_base_position', 'ID', 'LONG', 'TEXTFIELD', 'TEXT', 'ID', 'id', 19, false, true, false, false, false, false, NULL::integer, true),
    ('base_position_code', 'sysbase_1.0_position', 'sysbase_1.0_position_base_position', 'CODE', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.position.code', 'code', 80, false, false, true, true, false, false, NULL::integer, false),
    ('base_position_name', 'sysbase_1.0_position', 'sysbase_1.0_position_base_position', 'NAME', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.position.name', 'name', 80, false, false, false, true, true, true, 1, false),

    ('base_staff_id', 'sysbase_1.0_staff', 'sysbase_1.0_staff_base_staff', 'ID', 'LONG', 'TEXTFIELD', 'TEXT', 'ID', 'id', 19, false, true, false, false, false, false, NULL::integer, true),
    ('base_staff_code', 'sysbase_1.0_staff', 'sysbase_1.0_staff_base_staff', 'CODE', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.ec.entity.staff.code', 'code', 80, true, false, true, true, false, false, NULL::integer, false),
    ('base_staff_name', 'sysbase_1.0_staff', 'sysbase_1.0_staff_base_staff', 'NAME', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.staff.dimissionStaff_xls.staffName', 'name', 80, false, false, false, true, true, true, 1, false)
)
INSERT INTO ec_property (
  code, ec_env, version, valid, entity_code, module_code, sort, is_hidden,
  fetch_mode, associated_property_code, associated_type, is_custom,
  is_mne_whole_like_query, column_name, senior_system_code, no_analyzer,
  is_main_associated, is_used_for_search, stretch, is_bussiness_key,
  is_control, is_used_mne_code, is_sensitive, is_main_display,
  is_used_for_list, model_code, is_pk, is_inherent, is_ignore_audit,
  is_unique, multable, max_length, nullable, is_index, field_type,
  format, type, display_name, name
)
SELECT
  code, 'product', 0, 1, entity_code, 'sysbase_1.0', NULL, 0,
  'SELECT', NULL, associated_type, 0,
  0, column_name, 0, 0,
  0, 0, 0, CASE WHEN is_bussiness_key THEN 1 ELSE 0 END,
  0, CASE WHEN is_used_mne_code THEN 1 ELSE 0 END, 0,
  CASE WHEN is_main_display THEN 1 ELSE 0 END,
  CASE WHEN is_used_for_list THEN 1 ELSE 0 END,
  model_code, CASE WHEN is_pk THEN 1 ELSE 0 END,
  CASE WHEN is_inherent THEN 1 ELSE 0 END,
  0, 0, 0, max_length, CASE WHEN nullable THEN 1 ELSE 0 END,
  0, field_type, format, type, display_name, name
FROM property_seed
ON CONFLICT (code) DO NOTHING;

WITH property_seed (
  code, entity_code, model_code, column_name, type, field_type, format,
  display_name, name, max_length, nullable, is_pk, is_bussiness_key,
  is_used_for_list, is_main_display, is_used_mne_code, associated_type,
  is_inherent
) AS (
  VALUES
    ('base_company_id', 'sysbase_1.0_company', 'sysbase_1.0_company_base_company', 'ID', 'LONG', 'TEXTFIELD', 'TEXT', 'ID', 'id', 19, false, true, false, false, false, false, NULL::integer, true),
    ('base_company_code', 'sysbase_1.0_company', 'sysbase_1.0_company_base_company', 'CODE', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.company.code', 'code', 80, true, false, true, true, false, false, NULL::integer, false),
    ('base_company_name', 'sysbase_1.0_company', 'sysbase_1.0_company_base_company', 'NAME', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.company.name', 'name', 80, true, false, false, true, true, true, NULL::integer, false),

    ('base_department_id', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'ID', 'LONG', 'TEXTFIELD', 'TEXT', 'ID', 'id', 19, false, true, false, false, false, false, NULL::integer, true),
    ('base_department_code', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'CODE', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.department.code', 'code', 80, false, false, true, true, false, false, NULL::integer, false),
    ('base_department_name', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'NAME', 'TEXT', 'TEXTFIELD', 'TEXT', 'part.fzwdy.xm.mainPositionId.department.name', 'name', 80, false, false, false, true, true, true, 1, false),
    ('base_department_description', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'DESCRIPTION', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.department.description', 'description', 255, true, false, false, true, false, false, NULL::integer, false),
    ('base_department_layRec', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'LAY_REC', 'TEXT', 'TEXTFIELD', 'TEXT', 'layRec', 'layRec', 255, true, false, false, false, false, false, NULL::integer, false),
    ('base_department_parentId', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'PARENT_ID', 'INTEGER', 'TEXTFIELD', 'TEXT', 'foundation.ec.department.superiorDep', 'parentId', 19, true, false, false, false, false, false, NULL::integer, false),
    ('base_department_leaf', 'sysbase_1.0_department', 'sysbase_1.0_department_base_department', 'LEAF', 'BOOLEAN', 'CHECKBOX', 'CHECKBOX', 'foundation.systemCode.attribute', 'leaf', 0, true, false, false, false, false, false, NULL::integer, false),

    ('base_position_id', 'sysbase_1.0_position', 'sysbase_1.0_position_base_position', 'ID', 'LONG', 'TEXTFIELD', 'TEXT', 'ID', 'id', 19, false, true, false, false, false, false, NULL::integer, true),
    ('base_position_code', 'sysbase_1.0_position', 'sysbase_1.0_position_base_position', 'CODE', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.position.code', 'code', 80, false, false, true, true, false, false, NULL::integer, false),
    ('base_position_name', 'sysbase_1.0_position', 'sysbase_1.0_position_base_position', 'NAME', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.position.name', 'name', 80, false, false, false, true, true, true, 1, false),

    ('base_staff_id', 'sysbase_1.0_staff', 'sysbase_1.0_staff_base_staff', 'ID', 'LONG', 'TEXTFIELD', 'TEXT', 'ID', 'id', 19, false, true, false, false, false, false, NULL::integer, true),
    ('base_staff_code', 'sysbase_1.0_staff', 'sysbase_1.0_staff_base_staff', 'CODE', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.ec.entity.staff.code', 'code', 80, true, false, true, true, false, false, NULL::integer, false),
    ('base_staff_name', 'sysbase_1.0_staff', 'sysbase_1.0_staff_base_staff', 'NAME', 'TEXT', 'TEXTFIELD', 'TEXT', 'foundation.staff.dimissionStaff_xls.staffName', 'name', 80, false, false, false, true, true, true, 1, false)
)
INSERT INTO runtime_property (
  code, ec_env, version, valid, entity_code, module_code, sort, is_hidden,
  fetch_mode, associated_property_code, associated_type, is_custom,
  is_mne_whole_like_query, column_name, senior_system_code, no_analyzer,
  is_main_associated, is_used_for_search, stretch, is_bussiness_key,
  is_control, is_used_mne_code, is_sensitive, is_main_display,
  is_used_for_list, model_code, is_pk, is_inherent, is_ignore_audit,
  is_unique, multable, max_length, nullable, is_index, field_type,
  format, type, display_name, name
)
SELECT
  code, 'product', 0, true, entity_code, 'sysbase_1.0', NULL, false,
  'SELECT', NULL, associated_type, false,
  false, column_name, false, false,
  false, false, false, is_bussiness_key,
  false, is_used_mne_code, false, is_main_display,
  is_used_for_list, model_code, is_pk, is_inherent,
  false, false, false, max_length, nullable,
  false, field_type, format, type, display_name, name
FROM property_seed
ON CONFLICT (code) DO NOTHING;
