-- Seed WorkAppointment list button i18n resources restored by
-- 148-runtime-visible-business-buttons.sql. Without these rows the legacy
-- React runtime renders the i18n keys as visible button text.

INSERT INTO public.supfusion_i18n_resource (
  id, i18n_key, i18n_value, langu_code, module_code, module_version_code, valid,
  tenant_id, creator, create_time, create_staff_id, modifier, modify_time, modify_staff_id
)
SELECT *
FROM (
  VALUES
    (6579000000000301::bigint, 'workAppointment.buttonPropertyshowName.workPlan.add', '新增', 'zh_CN', 'workAppointment', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000302::bigint, 'workAppointment.buttonPropertyshowName.workPlan.add', '新增', 'zh_HK', 'workAppointment', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000303::bigint, 'workAppointment.buttonPropertyshowName.workPlan.add', 'Add', 'en_US', 'workAppointment', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000311::bigint, 'workAppointment.buttonPropertyshowName.workPlan.delete', '删除', 'zh_CN', 'workAppointment', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000312::bigint, 'workAppointment.buttonPropertyshowName.workPlan.delete', '刪除', 'zh_HK', 'workAppointment', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000313::bigint, 'workAppointment.buttonPropertyshowName.workPlan.delete', 'Delete', 'en_US', 'workAppointment', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint)
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
