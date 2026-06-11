CREATE TABLE IF NOT EXISTS public.auth_passwd_rules (
  id BIGINT PRIMARY KEY,
  min_length INTEGER NOT NULL,
  max_length INTEGER NOT NULL,
  rule_type INTEGER,
  contain_letter_case BOOLEAN,
  contain_numbers BOOLEAN,
  contain_special_char BOOLEAN,
  regular_expression VARCHAR(100),
  hint VARCHAR(100),
  find_pwd_switch BOOLEAN DEFAULT TRUE,
  encrypted BOOLEAN DEFAULT FALSE,
  retry_limit INTEGER DEFAULT -1,
  lock_time INTEGER DEFAULT 15,
  creator VARCHAR(32) NOT NULL DEFAULT 'system',
  modifier VARCHAR(32),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id BIGINT,
  modify_staff_id BIGINT,
  valid BOOLEAN DEFAULT TRUE
);

INSERT INTO public.auth_passwd_rules (
  id,
  min_length,
  max_length,
  rule_type,
  contain_letter_case,
  contain_numbers,
  contain_special_char,
  find_pwd_switch,
  encrypted,
  retry_limit,
  lock_time,
  valid
)
VALUES (1, 8, 32, 0, TRUE, TRUE, TRUE, TRUE, FALSE, -1, 15, TRUE)
ON CONFLICT (id) DO UPDATE
SET min_length = EXCLUDED.min_length,
    max_length = EXCLUDED.max_length,
    rule_type = EXCLUDED.rule_type,
    contain_letter_case = EXCLUDED.contain_letter_case,
    contain_numbers = EXCLUDED.contain_numbers,
    contain_special_char = EXCLUDED.contain_special_char,
    find_pwd_switch = EXCLUDED.find_pwd_switch,
    encrypted = EXCLUDED.encrypted,
    retry_limit = EXCLUDED.retry_limit,
    lock_time = EXCLUDED.lock_time,
    valid = EXCLUDED.valid,
    modify_time = CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS public.custom_theme_wallpaper (
  id BIGINT PRIMARY KEY,
  title_name VARCHAR(500),
  copyright_information VARCHAR(4000),
  default_home_page VARCHAR(4000),
  login_logo_image_path VARCHAR(500),
  login_background_image_path VARCHAR(500),
  login_title_image_path VARCHAR(500),
  left_image_path VARCHAR(500),
  right_image_path VARCHAR(500),
  left_collapsed_image_path VARCHAR(500),
  enabled INTEGER DEFAULT 0,
  valid BOOLEAN DEFAULT TRUE,
  creator VARCHAR(200) DEFAULT 'system',
  modifier VARCHAR(200),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_staff_id BIGINT,
  modify_staff_id BIGINT
);

INSERT INTO public.custom_theme_wallpaper (
  id,
  title_name,
  copyright_information,
  default_home_page,
  login_logo_image_path,
  login_background_image_path,
  login_title_image_path,
  left_image_path,
  right_image_path,
  left_collapsed_image_path,
  enabled,
  valid,
  creator
)
VALUES (
  1001,
  'supOS工业操作系统',
  NULL,
  NULL,
  '/supplant-static/img/login_logo_dc338cd.png',
  '/supplant-static/img/login_bg_1f42d52.jpg',
  '/supplant-static/img/login_title_c1e3bef.png',
  '/supplant-static/img/topleft_logo_d6f999c.png',
  '/supplant-static/img/right_logo_ec966fd.png',
  '/supplant-static/img/top_logo_collapsed_4ff313d.png',
  1,
  TRUE,
  'system'
)
ON CONFLICT (id) DO UPDATE
SET title_name = EXCLUDED.title_name,
    copyright_information = EXCLUDED.copyright_information,
    default_home_page = EXCLUDED.default_home_page,
    login_logo_image_path = EXCLUDED.login_logo_image_path,
    login_background_image_path = EXCLUDED.login_background_image_path,
    login_title_image_path = EXCLUDED.login_title_image_path,
    left_image_path = EXCLUDED.left_image_path,
    right_image_path = EXCLUDED.right_image_path,
    left_collapsed_image_path = EXCLUDED.left_collapsed_image_path,
    enabled = EXCLUDED.enabled,
    valid = EXCLUDED.valid,
    modify_time = CURRENT_TIMESTAMP;
