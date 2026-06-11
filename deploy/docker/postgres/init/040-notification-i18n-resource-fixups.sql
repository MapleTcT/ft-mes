-- Seed notification app display-name resources used by notification-admin.
-- Without these rows the page still renders, but the service emits ERROR logs
-- every time it translates the optional notification integrations.

INSERT INTO public.supfusion_i18n_resource (
  id, i18n_key, i18n_value, langu_code, module_code, module_version_code, valid,
  tenant_id, creator, create_time, create_staff_id, modifier, modify_time, modify_staff_id
)
SELECT *
FROM (
  VALUES
    (6579000000000201::bigint, 'notificationWechat.app_show_name', '微信通知', 'zh_CN', 'notificationWechat', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000202::bigint, 'notificationWechat.app_show_name', '微信通知', 'zh_HK', 'notificationWechat', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000203::bigint, 'notificationWechat.app_show_name', 'WeChat Notification', 'en_US', 'notificationWechat', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000211::bigint, 'notificationSMS.jin_cang_app_show_name', '金仓短信', 'zh_CN', 'notificationSMS', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000212::bigint, 'notificationSMS.jin_cang_app_show_name', '金倉短信', 'zh_HK', 'notificationSMS', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000213::bigint, 'notificationSMS.jin_cang_app_show_name', 'Jincang SMS', 'en_US', 'notificationSMS', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000221::bigint, 'notificationDingtalk.app_show_name', '钉钉通知', 'zh_CN', 'notificationDingtalk', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000222::bigint, 'notificationDingtalk.app_show_name', '釘釘通知', 'zh_HK', 'notificationDingtalk', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint),
    (6579000000000223::bigint, 'notificationDingtalk.app_show_name', 'DingTalk Notification', 'en_US', 'notificationDingtalk', NULL, '1', 'dt', 'system', CURRENT_TIMESTAMP, 1::bigint, NULL, CURRENT_TIMESTAMP, NULL::bigint)
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
