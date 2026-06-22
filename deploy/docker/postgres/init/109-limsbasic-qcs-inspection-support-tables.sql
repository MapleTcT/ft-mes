-- LIMSBasic support tables required by QCS inspection creation.
--
-- WOM createManuInspect calls QCS createInspect. QCS validates the material
-- against quality-standard relations and loads standard-version components
-- before it can persist qcs_inspects/qcs_inspect_stds. The recovered
-- PostgreSQL runtime had the QCS tables but missed these LIMSBasic support
-- tables, so real WOM -> QCS persistence acceptance was blocked by missing
-- relations instead of business validation.

DO $$
DECLARE
    table_name text;
    column_def record;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'limsba_analy_prods',
        'limsba_analy_prod_stds',
        'limsba_std_ver_coms',
        'limsba_mat_test_plan_coms'
    ] LOOP
        EXECUTE format('CREATE TABLE IF NOT EXISTS public.%I (id bigint)', table_name);

        FOR column_def IN
            SELECT *
            FROM (
                VALUES
                    ('id', 'bigint'),
                    ('version', 'integer DEFAULT 0'),
                    ('create_staff_id', 'bigint'),
                    ('create_time', 'timestamp without time zone DEFAULT CURRENT_TIMESTAMP'),
                    ('modify_staff_id', 'bigint'),
                    ('modify_time', 'timestamp without time zone'),
                    ('delete_staff_id', 'bigint'),
                    ('delete_time', 'timestamp without time zone'),
                    ('valid', 'boolean DEFAULT true'),
                    ('cid', 'bigint'),
                    ('sort', 'integer'),
                    ('table_info_id', 'bigint')
            ) AS columns(name, definition)
        LOOP
            EXECUTE format(
                'ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS %I %s',
                table_name,
                column_def.name,
                column_def.definition
            );
        END LOOP;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conrelid = format('public.%I', table_name)::regclass
              AND contype = 'p'
        ) THEN
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I PRIMARY KEY (id)', table_name, table_name || '_pkey');
        END IF;
    END LOOP;
END $$;

ALTER TABLE public.limsba_analy_prods
    ADD COLUMN IF NOT EXISTS code character varying(200),
    ADD COLUMN IF NOT EXISTS default_std_id bigint,
    ADD COLUMN IF NOT EXISTS memo_field text,
    ADD COLUMN IF NOT EXISTS product_id bigint,
    ADD COLUMN IF NOT EXISTS as_id bigint,
    ADD COLUMN IF NOT EXISTS available_std text,
    ADD COLUMN IF NOT EXISTS bigintparama integer,
    ADD COLUMN IF NOT EXISTS bigintparamb integer,
    ADD COLUMN IF NOT EXISTS charparama text,
    ADD COLUMN IF NOT EXISTS charparamb text,
    ADD COLUMN IF NOT EXISTS dateparama timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamb timestamp without time zone,
    ADD COLUMN IF NOT EXISTS numberparama numeric(38, 6),
    ADD COLUMN IF NOT EXISTS numberparamb numeric(38, 6),
    ADD COLUMN IF NOT EXISTS objparama bigint,
    ADD COLUMN IF NOT EXISTS objparamb bigint,
    ADD COLUMN IF NOT EXISTS scparama text,
    ADD COLUMN IF NOT EXISTS scparamb text;

ALTER TABLE public.limsba_analy_prod_stds
    ADD COLUMN IF NOT EXISTS code character varying(200),
    ADD COLUMN IF NOT EXISTS memo_field text,
    ADD COLUMN IF NOT EXISTS product_id bigint,
    ADD COLUMN IF NOT EXISTS std_id bigint,
    ADD COLUMN IF NOT EXISTS as_id bigint,
    ADD COLUMN IF NOT EXISTS as_product_id bigint,
    ADD COLUMN IF NOT EXISTS available_std boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS bigintparama integer,
    ADD COLUMN IF NOT EXISTS bigintparamb integer,
    ADD COLUMN IF NOT EXISTS charparama text,
    ADD COLUMN IF NOT EXISTS charparamb text,
    ADD COLUMN IF NOT EXISTS dateparama timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamb timestamp without time zone,
    ADD COLUMN IF NOT EXISTS numberparama numeric(38, 6),
    ADD COLUMN IF NOT EXISTS numberparamb numeric(38, 6),
    ADD COLUMN IF NOT EXISTS objparama bigint,
    ADD COLUMN IF NOT EXISTS objparamb bigint,
    ADD COLUMN IF NOT EXISTS scparama text,
    ADD COLUMN IF NOT EXISTS scparamb text;

ALTER TABLE public.limsba_std_ver_coms
    ADD COLUMN IF NOT EXISTS std_id bigint,
    ADD COLUMN IF NOT EXISTS std_ver_id bigint,
    ADD COLUMN IF NOT EXISTS std_ver_test_id bigint,
    ADD COLUMN IF NOT EXISTS test_id bigint,
    ADD COLUMN IF NOT EXISTS unit_name text,
    ADD COLUMN IF NOT EXISTS valuen integer,
    ADD COLUMN IF NOT EXISTS carry_formula text,
    ADD COLUMN IF NOT EXISTS carry_rule text,
    ADD COLUMN IF NOT EXISTS carry_space text,
    ADD COLUMN IF NOT EXISTS carry_type character varying,
    ADD COLUMN IF NOT EXISTS code character varying(200),
    ADD COLUMN IF NOT EXISTS com_id bigint,
    ADD COLUMN IF NOT EXISTS default_value text,
    ADD COLUMN IF NOT EXISTS digit_type character varying,
    ADD COLUMN IF NOT EXISTS is_report boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS memo_field text,
    ADD COLUMN IF NOT EXISTS name_eng text,
    ADD COLUMN IF NOT EXISTS parallel_times integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS ref_value text,
    ADD COLUMN IF NOT EXISTS report_name text,
    ADD COLUMN IF NOT EXISTS report_sort integer,
    ADD COLUMN IF NOT EXISTS sampling_plan character varying,
    ADD COLUMN IF NOT EXISTS bigintparama integer,
    ADD COLUMN IF NOT EXISTS bigintparamb integer,
    ADD COLUMN IF NOT EXISTS charparama text,
    ADD COLUMN IF NOT EXISTS charparamb text,
    ADD COLUMN IF NOT EXISTS dateparama timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamb timestamp without time zone,
    ADD COLUMN IF NOT EXISTS numberparama numeric(38, 6),
    ADD COLUMN IF NOT EXISTS numberparamb numeric(38, 6),
    ADD COLUMN IF NOT EXISTS objparama bigint,
    ADD COLUMN IF NOT EXISTS objparamb bigint,
    ADD COLUMN IF NOT EXISTS scparama text,
    ADD COLUMN IF NOT EXISTS scparamb text;

ALTER TABLE public.limsba_mat_test_plan_coms
    ADD COLUMN IF NOT EXISTS count_num integer,
    ADD COLUMN IF NOT EXISTS last_test_date timestamp without time zone,
    ADD COLUMN IF NOT EXISTS mat_test_plan_id bigint,
    ADD COLUMN IF NOT EXISTS remark text,
    ADD COLUMN IF NOT EXISTS test_com character varying(256),
    ADD COLUMN IF NOT EXISTS test_frequency bigint;

CREATE INDEX IF NOT EXISTS idx_limsba_analy_prods_code
    ON public.limsba_analy_prods(code);
CREATE INDEX IF NOT EXISTS idx_limsba_analy_prods_product_id
    ON public.limsba_analy_prods(product_id);
CREATE INDEX IF NOT EXISTS idx_limsba_analy_prods_as_id
    ON public.limsba_analy_prods(as_id);
CREATE INDEX IF NOT EXISTS idx_limsba_analy_prods_valid
    ON public.limsba_analy_prods(valid);

CREATE INDEX IF NOT EXISTS idx_limsba_analy_prod_stds_product_id
    ON public.limsba_analy_prod_stds(product_id);
CREATE INDEX IF NOT EXISTS idx_limsba_analy_prod_stds_std_id
    ON public.limsba_analy_prod_stds(std_id);
CREATE INDEX IF NOT EXISTS idx_limsba_analy_prod_stds_as_id
    ON public.limsba_analy_prod_stds(as_id);
CREATE INDEX IF NOT EXISTS idx_limsba_analy_prod_stds_valid
    ON public.limsba_analy_prod_stds(valid);

CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_coms_code
    ON public.limsba_std_ver_coms(code);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_coms_std_ver_id
    ON public.limsba_std_ver_coms(std_ver_id);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_coms_std_id
    ON public.limsba_std_ver_coms(std_id);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_coms_test_id
    ON public.limsba_std_ver_coms(test_id);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_coms_valid
    ON public.limsba_std_ver_coms(valid);

CREATE INDEX IF NOT EXISTS idx_limsba_mat_test_plan_coms_plan_id
    ON public.limsba_mat_test_plan_coms(mat_test_plan_id);
CREATE INDEX IF NOT EXISTS idx_limsba_mat_test_plan_coms_test_com
    ON public.limsba_mat_test_plan_coms(test_com);
CREATE INDEX IF NOT EXISTS idx_limsba_mat_test_plan_coms_valid
    ON public.limsba_mat_test_plan_coms(valid);
