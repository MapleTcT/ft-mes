-- PATROL 6.0.4.0 PostgreSQL technical tables.
--
-- module.xml describes the 27 business models, while the compiled JPA model
-- also requires deal-info, mnemonic-code and supervision tables. Create those
-- first so the runtime metadata generator does not claim MP_POTROL_TASKS_SV as
-- a permissive share view.

CREATE OR REPLACE FUNCTION public.adp_patrol_ensure_deal_info_table(target_table text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    column_def record;
BEGIN
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS public.%I (id bigint PRIMARY KEY)',
        target_table
    );

    FOR column_def IN
        SELECT *
        FROM (VALUES
            ('version', 'integer DEFAULT 0'),
            ('activity_name', 'varchar(255)'),
            ('assign_staff', 'text'),
            ('assign_staff_id', 'varchar(255)'),
            ('cid', 'bigint'),
            ('comments', 'text'),
            ('create_time', 'timestamp without time zone'),
            ('dealinfo_type', 'varchar(255)'),
            ('entity_code', 'varchar(255)'),
            ('instance_id', 'varchar(255)'),
            ('outcome', 'varchar(255)'),
            ('outcome_des', 'varchar(255)'),
            ('outcome_des_zh_cn', 'varchar(255)'),
            ('pending_create_time', 'timestamp without time zone'),
            ('process_key', 'varchar(255)'),
            ('process_version', 'integer'),
            ('proxy_staff', 'varchar(255)'),
            ('proxy_staff_ids', 'varchar(255)'),
            ('signature', 'varchar(400)'),
            ('task_description', 'varchar(255)'),
            ('task_description_zh_cn', 'varchar(255)'),
            ('user_id', 'bigint'),
            ('recalled_flag', 'boolean'),
            ('sort', 'integer'),
            ('table_info_id', 'bigint'),
            ('user_agent', 'varchar(255)'),
            ('main_obj', 'bigint'),
            ('staff', 'bigint')
        ) AS columns(column_name, column_type)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS %I %s',
            target_table,
            column_def.column_name,
            column_def.column_type
        );
    END LOOP;
END;
$$;

SELECT public.adp_patrol_ensure_deal_info_table('mp_input_standards_di');
SELECT public.adp_patrol_ensure_deal_info_table('mp_patrol_monits_di');
SELECT public.adp_patrol_ensure_deal_info_table('mp_patrol_plans_di');
SELECT public.adp_patrol_ensure_deal_info_table('mp_potrol_tasks_di');
SELECT public.adp_patrol_ensure_deal_info_table('mp_public_item_trees_di');
SELECT public.adp_patrol_ensure_deal_info_table('mp_work_groups_di');
SELECT public.adp_patrol_ensure_deal_info_table('team_teams_di');

DROP FUNCTION IF EXISTS public.adp_patrol_ensure_deal_info_table(text);

CREATE TABLE IF NOT EXISTS public.mp_input_standards_mc (
    id bigint PRIMARY KEY,
    version integer DEFAULT 0,
    mne_code varchar(255),
    input_standard_id bigint,
    input_standard bigint
);

ALTER TABLE public.mp_input_standards_mc
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mne_code varchar(255),
    ADD COLUMN IF NOT EXISTS input_standard_id bigint,
    ADD COLUMN IF NOT EXISTS input_standard bigint;

CREATE TABLE IF NOT EXISTS public.mp_work_groups_mc (
    id bigint PRIMARY KEY,
    version integer DEFAULT 0,
    mne_code varchar(255),
    work_group_id bigint,
    work_group bigint
);

ALTER TABLE public.mp_work_groups_mc
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mne_code varchar(255),
    ADD COLUMN IF NOT EXISTS work_group_id bigint,
    ADD COLUMN IF NOT EXISTS work_group bigint;

CREATE TABLE IF NOT EXISTS public.mp_potrol_tasks_sv (
    id bigint PRIMARY KEY,
    version integer DEFAULT 0,
    create_staff_id bigint,
    create_time timestamp without time zone,
    delete_staff_id bigint,
    delete_time timestamp without time zone,
    modify_staff_id bigint,
    modify_time timestamp without time zone,
    table_info_id bigint,
    valid integer DEFAULT 1,
    main_obj bigint,
    staff bigint
);

ALTER TABLE public.mp_potrol_tasks_sv
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS create_staff_id bigint,
    ADD COLUMN IF NOT EXISTS create_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS delete_staff_id bigint,
    ADD COLUMN IF NOT EXISTS delete_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS modify_staff_id bigint,
    ADD COLUMN IF NOT EXISTS modify_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS table_info_id bigint,
    ADD COLUMN IF NOT EXISTS valid integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS main_obj bigint,
    ADD COLUMN IF NOT EXISTS staff bigint;

CREATE OR REPLACE FUNCTION public.adp_patrol_sync_input_standard_reference()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.input_standard_id IS DISTINCT FROM OLD.input_standard_id THEN
        NEW.input_standard := NEW.input_standard_id;
    ELSIF TG_OP = 'UPDATE' AND NEW.input_standard IS DISTINCT FROM OLD.input_standard THEN
        NEW.input_standard_id := NEW.input_standard;
    ELSE
        NEW.input_standard_id := COALESCE(NEW.input_standard_id, NEW.input_standard);
        NEW.input_standard := COALESCE(NEW.input_standard, NEW.input_standard_id);
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_patrol_sync_input_standard_reference
    ON public.mp_input_standards_mc;
CREATE TRIGGER trg_patrol_sync_input_standard_reference
BEFORE INSERT OR UPDATE ON public.mp_input_standards_mc
FOR EACH ROW EXECUTE FUNCTION public.adp_patrol_sync_input_standard_reference();

CREATE OR REPLACE FUNCTION public.adp_patrol_sync_work_group_reference()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.work_group_id IS DISTINCT FROM OLD.work_group_id THEN
        NEW.work_group := NEW.work_group_id;
    ELSIF TG_OP = 'UPDATE' AND NEW.work_group IS DISTINCT FROM OLD.work_group THEN
        NEW.work_group_id := NEW.work_group;
    ELSE
        NEW.work_group_id := COALESCE(NEW.work_group_id, NEW.work_group);
        NEW.work_group := COALESCE(NEW.work_group, NEW.work_group_id);
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_patrol_sync_work_group_reference
    ON public.mp_work_groups_mc;
CREATE TRIGGER trg_patrol_sync_work_group_reference
BEFORE INSERT OR UPDATE ON public.mp_work_groups_mc
FOR EACH ROW EXECUTE FUNCTION public.adp_patrol_sync_work_group_reference();

CREATE INDEX IF NOT EXISTS idx_mp_input_standards_mc_standard
    ON public.mp_input_standards_mc(input_standard_id);
CREATE INDEX IF NOT EXISTS idx_mp_work_groups_mc_group
    ON public.mp_work_groups_mc(work_group_id);
CREATE INDEX IF NOT EXISTS idx_mp_potrol_tasks_sv_main_obj
    ON public.mp_potrol_tasks_sv(main_obj);
CREATE INDEX IF NOT EXISTS idx_mp_potrol_tasks_sv_staff
    ON public.mp_potrol_tasks_sv(staff);

CREATE INDEX IF NOT EXISTS idx_mp_input_standards_di_main_obj
    ON public.mp_input_standards_di(main_obj);
CREATE INDEX IF NOT EXISTS idx_mp_patrol_monits_di_main_obj
    ON public.mp_patrol_monits_di(main_obj);
CREATE INDEX IF NOT EXISTS idx_mp_patrol_plans_di_main_obj
    ON public.mp_patrol_plans_di(main_obj);
CREATE INDEX IF NOT EXISTS idx_mp_potrol_tasks_di_main_obj
    ON public.mp_potrol_tasks_di(main_obj);
CREATE INDEX IF NOT EXISTS idx_mp_public_item_trees_di_main_obj
    ON public.mp_public_item_trees_di(main_obj);
CREATE INDEX IF NOT EXISTS idx_mp_work_groups_di_main_obj
    ON public.mp_work_groups_di(main_obj);
