-- PostgreSQL compatibility table for WOM_1.0.0_prepareMaterialNeed_PrePareNeedDealInfo.
-- Source evidence: WOM_6.1.3.4/service/src/main/resources/META-INF/init/init.xml.
-- The prepare-need flow save path may persist deal-info rows while creating a
-- WOM_PRE_PARE_NEEDS header from generatePrepareNeed.

CREATE TABLE IF NOT EXISTS public.wom_pre_pare_needs_di (
    id bigint
);

DO $$
DECLARE
    column_def record;
BEGIN
    FOR column_def IN
        SELECT *
        FROM (
            VALUES
                ('id', 'bigint'),
                ('version', 'integer DEFAULT 0'),
                ('activityname', 'character varying'),
                ('assignstaff', 'character varying'),
                ('assignstaffid', 'character varying'),
                ('cid', 'bigint'),
                ('comments', 'character varying'),
                ('createtime', 'timestamp without time zone'),
                ('dealinfo_type', 'character varying'),
                ('entitycode', 'character varying'),
                ('instanceid', 'character varying'),
                ('outcome', 'character varying'),
                ('outcomedes', 'character varying'),
                ('outcome_des_zh_cn', 'character varying'),
                ('pendingcreatetime', 'timestamp without time zone'),
                ('processkey', 'character varying'),
                ('processversion', 'integer'),
                ('proxystaff', 'character varying'),
                ('proxystaffids', 'character varying'),
                ('signature', 'character varying'),
                ('taskdescription', 'character varying'),
                ('task_description_zh_cn', 'character varying'),
                ('userid', 'bigint'),
                ('recalledflag', 'boolean'),
                ('sort', 'integer'),
                ('tableinfoid', 'bigint'),
                ('useragent', 'character varying'),
                ('main_obj', 'bigint'),
                ('staff', 'bigint'),
                ('activity_name', 'character varying'),
                ('assign_staff', 'character varying'),
                ('assign_staff_id', 'character varying'),
                ('create_time', 'timestamp without time zone'),
                ('entity_code', 'character varying'),
                ('instance_id', 'character varying'),
                ('outcome_des', 'character varying'),
                ('pending_create_time', 'timestamp without time zone'),
                ('process_key', 'character varying'),
                ('process_version', 'integer'),
                ('proxy_staff', 'character varying'),
                ('proxy_staff_ids', 'character varying'),
                ('task_description', 'character varying'),
                ('user_id', 'bigint'),
                ('recalled_flag', 'boolean'),
                ('table_info_id', 'bigint'),
                ('user_agent', 'character varying')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_pre_pare_needs_di ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.wom_pre_pare_needs_di'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_pre_pare_needs_di
            ADD CONSTRAINT wom_pre_pare_needs_di_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_wom_pre_pare_needs_di_table_info
    ON public.wom_pre_pare_needs_di(table_info_id);

CREATE INDEX IF NOT EXISTS idx_wom_pre_pare_needs_di_tableinfoid
    ON public.wom_pre_pare_needs_di(tableinfoid);

CREATE INDEX IF NOT EXISTS idx_wom_pre_pare_needs_di_main_obj
    ON public.wom_pre_pare_needs_di(main_obj);

CREATE INDEX IF NOT EXISTS idx_wom_pre_pare_needs_di_staff
    ON public.wom_pre_pare_needs_di(staff);
