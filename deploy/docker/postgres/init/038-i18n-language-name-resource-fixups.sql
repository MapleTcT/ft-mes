-- The recovered i18n service translates language display names as resource keys
-- when rendering the language selector. Seed those name keys to avoid cache-miss
-- ERROR logs while keeping the existing i18n.language_tips_* keys intact.

INSERT INTO public.supfusion_i18n_resource (
  id, i18n_key, i18n_value, langu_code, module_code, module_version_code, valid,
  tenant_id, creator, create_time, create_staff_id, modifier, modify_time, modify_staff_id
)
SELECT *
FROM (
  VALUES
    (6579000000000101::bigint, '中文（简体）', '中文（简体）', 'zh_CN', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000102::bigint, '中文（简体）', '中文（簡體）', 'zh_HK', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000103::bigint, '中文（简体）', 'Chinese (Simplified)', 'en_US', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000111::bigint, '中文（香港）', '中文（香港）', 'zh_CN', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000112::bigint, '中文（香港）', '中文（香港）', 'zh_HK', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000113::bigint, '中文（香港）', 'Chinese (Hong Kong)', 'en_US', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000121::bigint, '英文（美国）', '英文（美国）', 'zh_CN', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000122::bigint, '英文（美国）', '英文（美國）', 'zh_HK', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000123::bigint, '英文（美国）', 'English (United States)', 'en_US', 'i18n', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint)
) AS seed (
  id, i18n_key, i18n_value, langu_code, module_code, module_version_code, valid,
  tenant_id, creator, create_time, create_staff_id, modifier, modify_time, modify_staff_id
)
WHERE NOT EXISTS (
  SELECT 1
  FROM public.supfusion_i18n_resource existing
  WHERE existing.i18n_key = seed.i18n_key
    AND existing.langu_code = seed.langu_code
    AND existing.tenant_id = seed.tenant_id
);
