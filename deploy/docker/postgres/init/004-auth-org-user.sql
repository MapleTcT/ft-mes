-- Compatibility DDL for PostgreSQL deployments where the auth user table is
-- not created before organization/RBAC initialization queries run.

ALTER TABLE org_person ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT NULL;
ALTER TABLE org_person ADD COLUMN IF NOT EXISTS user_name VARCHAR(256) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS auth_user (
    id BIGINT PRIMARY KEY,
    user_name VARCHAR(256) NOT NULL,
    company_id BIGINT DEFAULT NULL,
    current_company_id BIGINT DEFAULT NULL,
    password VARCHAR(256) NOT NULL,
    person_id BIGINT DEFAULT NULL,
    person_name VARCHAR(50) DEFAULT NULL,
    person_code VARCHAR(50) DEFAULT NULL,
    has_lock SMALLINT DEFAULT 0,
    valid SMALLINT DEFAULT 1,
    user_type SMALLINT DEFAULT 0,
    ldap_user_name VARCHAR(256) DEFAULT NULL,
    user_directory_id BIGINT DEFAULT NULL,
    lock_reason SMALLINT DEFAULT NULL,
    time_zone VARCHAR(64) DEFAULT NULL,
    description VARCHAR(512) DEFAULT NULL,
    login_first SMALLINT NOT NULL DEFAULT 1,
    face_url VARCHAR(256) DEFAULT NULL,
    third_identity VARCHAR(256) DEFAULT NULL,
    third_source VARCHAR(256) DEFAULT NULL,
    creator VARCHAR(32) NOT NULL DEFAULT 'system',
    modifier VARCHAR(32) DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT DEFAULT NULL,
    modify_staff_id BIGINT DEFAULT NULL,
    tenant_id VARCHAR(64) DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_ldap_user_name ON auth_user(ldap_user_name);
CREATE INDEX IF NOT EXISTS idx_user_username_company ON auth_user(user_name, company_id);
CREATE INDEX IF NOT EXISTS idx_user_staff ON auth_user(person_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_name ON auth_user(user_name);

INSERT INTO auth_user(id, user_name, company_id, current_company_id, password, user_type, person_id, person_name, person_code)
VALUES (1, 'admin', 1000, 1000, '$2a$10$QEd181jr.RNYME6hz/.xpONiMe3uGkI5sI8fjH5DQWwgwBKEs0/Cy', 1, 1, '默认人员', 'default')
ON CONFLICT (id) DO NOTHING;

UPDATE org_person
SET user_id = 1,
    user_name = 'admin'
WHERE id = 1
  AND (user_id IS NULL OR user_name IS NULL);

CREATE OR REPLACE VIEW base_userinfo (
    id,
    version,
    name,
    password,
    staff_id,
    locked,
    valid,
    timezone,
    create_staff_id,
    modify_staff_id,
    delete_staff_id,
    create_time,
    modify_time,
    delete_time,
    uuid,
    user_type,
    u_password,
    token_sn,
    three_role,
    state,
    pims_safe_access_id,
    passwordmodify_time,
    login_time,
    login_num,
    lock_time,
    language,
    is_allow_remote_access,
    remote_access_modified_flag
) AS
SELECT
    id,
    0 AS version,
    user_name AS name,
    password,
    person_id AS staff_id,
    has_lock AS locked,
    valid,
    time_zone AS timezone,
    NULL::BIGINT AS create_staff_id,
    NULL::BIGINT AS modify_staff_id,
    NULL::BIGINT AS delete_staff_id,
    create_time,
    modify_time,
    NULL::TIMESTAMP AS delete_time,
    user_name AS uuid,
    1 AS user_type,
    NULL::VARCHAR AS u_password,
    NULL::VARCHAR AS token_sn,
    CASE WHEN user_type > 0 THEN 1 ELSE 0 END AS three_role,
    0 AS state,
    0 AS pims_safe_access_id,
    NULL::TIMESTAMP AS passwordmodify_time,
    NULL::TIMESTAMP AS login_time,
    NULL::INTEGER AS login_num,
    NULL::TIMESTAMP AS lock_time,
    'zh_CN' AS language,
    0 AS is_allow_remote_access,
    0 AS remote_access_modified_flag
FROM auth_user;
