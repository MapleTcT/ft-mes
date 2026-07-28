-- Register the recovered factory node-type reference in the EC customer-condition cache.
-- The reference is functional without these rows, but FoundationMs logs an ERROR on every query.

INSERT INTO public.runtime_customer_condition (
    code,
    ec_env,
    version,
    create_time,
    modify_time,
    valid,
    entity_code,
    module_code,
    proj_flag,
    condition_sql,
    json_condition,
    dataclassific_code,
    datagrid_code,
    view_code
)
VALUES (
    'HierarchicalMod_1.0.0_factoryNodeType_nodeTypeRef',
    'product',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    true,
    'HierarchicalMod_1.0.0_factoryNodeType',
    'HierarchicalMod_1.0.0',
    false,
    NULL,
    NULL,
    NULL,
    NULL,
    'HierarchicalMod_1.0.0_factoryNodeType_nodeTypeRef'
)
ON CONFLICT (code) DO UPDATE SET
    ec_env = COALESCE(NULLIF(EXCLUDED.ec_env, ''), public.runtime_customer_condition.ec_env),
    modify_time = CURRENT_TIMESTAMP,
    valid = true,
    entity_code = COALESCE(NULLIF(EXCLUDED.entity_code, ''), public.runtime_customer_condition.entity_code),
    module_code = COALESCE(NULLIF(EXCLUDED.module_code, ''), public.runtime_customer_condition.module_code),
    proj_flag = COALESCE(EXCLUDED.proj_flag, public.runtime_customer_condition.proj_flag),
    condition_sql = COALESCE(NULLIF(EXCLUDED.condition_sql, ''), public.runtime_customer_condition.condition_sql),
    json_condition = COALESCE(NULLIF(EXCLUDED.json_condition, ''), public.runtime_customer_condition.json_condition),
    dataclassific_code = COALESCE(NULLIF(EXCLUDED.dataclassific_code, ''), public.runtime_customer_condition.dataclassific_code),
    datagrid_code = COALESCE(NULLIF(EXCLUDED.datagrid_code, ''), public.runtime_customer_condition.datagrid_code),
    view_code = COALESCE(NULLIF(EXCLUDED.view_code, ''), public.runtime_customer_condition.view_code);

INSERT INTO public.ec_customer_condition (
    code,
    ec_env,
    version,
    create_time,
    modify_time,
    valid,
    entity_code,
    module_code,
    proj_flag,
    condition_sql,
    json_condition,
    dataclassific_code,
    datagrid_code,
    view_code
)
VALUES (
    'HierarchicalMod_1.0.0_factoryNodeType_nodeTypeRef',
    'product',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1,
    'HierarchicalMod_1.0.0_factoryNodeType',
    'HierarchicalMod_1.0.0',
    0,
    NULL,
    NULL,
    NULL,
    NULL,
    'HierarchicalMod_1.0.0_factoryNodeType_nodeTypeRef'
)
ON CONFLICT (code) DO UPDATE SET
    ec_env = COALESCE(NULLIF(EXCLUDED.ec_env, ''), public.ec_customer_condition.ec_env),
    modify_time = CURRENT_TIMESTAMP,
    valid = 1,
    entity_code = COALESCE(NULLIF(EXCLUDED.entity_code, ''), public.ec_customer_condition.entity_code),
    module_code = COALESCE(NULLIF(EXCLUDED.module_code, ''), public.ec_customer_condition.module_code),
    proj_flag = COALESCE(EXCLUDED.proj_flag, public.ec_customer_condition.proj_flag),
    condition_sql = COALESCE(NULLIF(EXCLUDED.condition_sql, ''), public.ec_customer_condition.condition_sql),
    json_condition = COALESCE(NULLIF(EXCLUDED.json_condition, ''), public.ec_customer_condition.json_condition),
    dataclassific_code = COALESCE(NULLIF(EXCLUDED.dataclassific_code, ''), public.ec_customer_condition.dataclassific_code),
    datagrid_code = COALESCE(NULLIF(EXCLUDED.datagrid_code, ''), public.ec_customer_condition.datagrid_code),
    view_code = COALESCE(NULLIF(EXCLUDED.view_code, ''), public.ec_customer_condition.view_code);
