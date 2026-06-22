-- Complete RM formula workflow deal-info table for PostgreSQL.
--
-- Source evidence:
-- - RMFormulaDealInfo extends AbstractDealInfoEntity and maps to RM_FORMULAS_DI.
-- - RM batch formula sync persists RMFormulaDealInfo through Hibernate after
--   workflow start; recovered PostgreSQL schema only had RM local columns, so
--   inserts failed on inherited deal-info columns such as activity_name.

DO $$
DECLARE
    column_def record;
BEGIN
    FOR column_def IN
        SELECT *
        FROM (
            VALUES
                ('activity_name', 'varchar(510)'),
                ('assign_staff', 'varchar(4000)'),
                ('assign_staff_id', 'varchar(255)'),
                ('cid', 'bigint'),
                ('comments', 'varchar(4000)'),
                ('dealinfo_type', 'varchar(255)'),
                ('entity_code', 'varchar(510)'),
                ('instance_id', 'varchar(510)'),
                ('outcome', 'varchar(510)'),
                ('outcome_des', 'varchar(2000)'),
                ('outcome_des_zh_cn', 'varchar(2000)'),
                ('pending_create_time', 'timestamp without time zone'),
                ('process_key', 'varchar(510)'),
                ('process_version', 'integer'),
                ('proxy_staff', 'varchar(255)'),
                ('proxy_staff_ids', 'varchar(255)'),
                ('signature', 'varchar(400)'),
                ('task_description', 'varchar(2000)'),
                ('task_description_zh_cn', 'varchar(2000)'),
                ('user_id', 'bigint')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.rm_formulas_di ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

ALTER TABLE public.rm_formulas_di
    ALTER COLUMN assign_staff TYPE varchar(4000),
    ALTER COLUMN comments TYPE varchar(4000),
    ALTER COLUMN signature TYPE varchar(400);

CREATE INDEX IF NOT EXISTS idx_rm_formulas_di_activity_name
    ON public.rm_formulas_di(activity_name);

CREATE INDEX IF NOT EXISTS idx_rm_formulas_di_process_key
    ON public.rm_formulas_di(process_key);

CREATE INDEX IF NOT EXISTS idx_rm_formulas_di_user_id
    ON public.rm_formulas_di(user_id);
