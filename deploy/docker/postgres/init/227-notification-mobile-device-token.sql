-- The consolidated msgmanagement service includes notification-mobile and
-- queries this table for every mobile notification. The recovered PostgreSQL
-- bootstrap omitted the table even though the exact DDL is bundled in
-- mobile-dao/META-INF/postgresql/mobile_1.sql.

CREATE TABLE IF NOT EXISTS public.mobile_device_token (
    id BIGINT NOT NULL,
    client_type INTEGER NOT NULL,
    login_status INTEGER NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    device_token VARCHAR(255),
    valid SMALLINT DEFAULT 1,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id BIGINT NOT NULL,
    modify_staff_id BIGINT,
    tenant_id VARCHAR(64),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_mobile_device_token_user_login
    ON public.mobile_device_token (user_name, login_status)
    WHERE valid = 1;
