DO $$
DECLARE
  updated_rows INTEGER;
BEGIN
  UPDATE public.custom_theme_wallpaper
  SET title_name = '飞天生物制造执行系统',
      copyright_information = '河南飞天生物科技股份有限公司',
      login_logo_image_path = '/bap/static/adp-custom/branding/feitian-logo-login.png',
      login_background_image_path = '/supplant-static/img/login_bg_1f42d52.jpg',
      login_title_image_path = '/bap/static/adp-custom/branding/feitian-logo-title.png',
      left_image_path = '/bap/static/adp-custom/branding/feitian-logo-login.png',
      left_collapsed_image_path = '/bap/static/adp-custom/branding/feitian-logo-login.png',
      enabled = 1,
      valid = TRUE,
      modifier = 'system',
      modify_time = CURRENT_TIMESTAMP
  WHERE id = 1001;

  GET DIAGNOSTICS updated_rows = ROW_COUNT;
  IF updated_rows <> 1 THEN
    RAISE EXCEPTION 'Expected custom_theme_wallpaper id 1001 before applying Feitian branding';
  END IF;
END
$$;
