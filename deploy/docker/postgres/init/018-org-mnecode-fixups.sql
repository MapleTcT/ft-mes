CREATE TABLE IF NOT EXISTS public.org_mnecode (
    id BIGINT PRIMARY KEY,
    row_version BIGINT NOT NULL DEFAULT 0,
    language VARCHAR(510),
    mne_code VARCHAR(510),
    org_id BIGINT,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT,
    tenant_id VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS public.org_department_mnecode (
    id BIGINT PRIMARY KEY,
    row_version BIGINT NOT NULL DEFAULT 0,
    language VARCHAR(510),
    mne_code VARCHAR(510) NOT NULL,
    dept_id BIGINT NOT NULL,
    dept_name VARCHAR(510) NOT NULL,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.org_position_mnecode (
    id BIGINT PRIMARY KEY,
    row_version BIGINT NOT NULL DEFAULT 0,
    language VARCHAR(510),
    mne_code VARCHAR(510) NOT NULL,
    position_id BIGINT NOT NULL,
    position_name VARCHAR(510) NOT NULL,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.org_person_mnecode (
    id BIGINT PRIMARY KEY,
    row_version BIGINT NOT NULL DEFAULT 0,
    language VARCHAR(510),
    mne_code VARCHAR(510) NOT NULL,
    person_id BIGINT NOT NULL,
    person_name VARCHAR(510) NOT NULL,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

DROP VIEW IF EXISTS public.base_staff_mnecode;
DROP VIEW IF EXISTS public.base_department_mnecode;
DROP VIEW IF EXISTS public.base_position_mnecode;
DROP VIEW IF EXISTS public.base_custom_group_mnecode;

CREATE OR REPLACE VIEW public.base_staff_mnecode AS
SELECT
    id,
    row_version AS version,
    person_id AS staff,
    mne_code
FROM public.org_person_mnecode;

CREATE OR REPLACE VIEW public.base_department_mnecode AS
SELECT
    id,
    row_version AS version,
    dept_id AS department,
    mne_code
FROM public.org_department_mnecode;

CREATE OR REPLACE VIEW public.base_position_mnecode AS
SELECT
    id,
    row_version AS version,
    position_id AS position,
    mne_code
FROM public.org_position_mnecode;

CREATE OR REPLACE VIEW public.base_custom_group_mnecode AS
SELECT
    m.id,
    m.row_version AS version,
    m.org_id AS custom_group,
    m.mne_code
FROM public.org_mnecode m
WHERE EXISTS (
    SELECT 1
    FROM information_schema.tables t
    WHERE t.table_schema = 'public'
      AND t.table_name = 'org_group'
);
