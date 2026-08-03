-- Restore the editable detail model used by the WOM create-batching-order dialog.
-- The original package maps WOM_1.0.0_batchMaterialNeed_MakeBatOrdPart to this
-- table, but the recovered PostgreSQL baseline did not include it.

CREATE TABLE IF NOT EXISTS public.wom_make_bat_ord_parts (id int8 PRIMARY KEY);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS version int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS create_staff_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS create_time timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS modify_staff_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS modify_time timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS delete_staff_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS delete_time timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS valid boolean;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS cid int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS sort int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS create_department_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS create_position_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS deployment_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS effect_staff_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS effect_time timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS effective_state int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS oa boolean;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS group_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS owner_department_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS owner_position_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS owner_staff_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS position_lay_rec varchar(255);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS process_key varchar(255);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS process_version int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS status int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS table_no varchar(255);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS table_info_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS extra_col text;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS parent_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS lay_rec varchar(1000);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS lay_no int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS leaf int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS full_path_name varchar(2000);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS batch_num numeric(38,6);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS batch_site varchar(255);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS batch_type varchar(255);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS bigintparama int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS bigintparamb int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS bigintparamc int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS bigintparamd int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS bigintparame int4;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS charparama varchar(2000);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS charparamb varchar(2000);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS charparamc varchar(2000);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS charparamd varchar(2000);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS charparame varchar(2000);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS dateparama timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS dateparamb timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS dateparamc timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS dateparamd timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS make_staff int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS material_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS max_quantity numeric(38,6);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS min_quantity numeric(38,6);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS need_date timestamp;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS need_id int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS numberparama numeric(38,6);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS numberparamb numeric(38,6);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS numberparamc numeric(38,6);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS objparama int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS objparamb int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS order_depart_ment int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS order_done_staff int8;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS remark text;
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS scparama varchar(2000);
ALTER TABLE public.wom_make_bat_ord_parts ADD COLUMN IF NOT EXISTS scparamb varchar(2000);

CREATE INDEX IF NOT EXISTS idx_wom_make_bat_ord_parts_valid
    ON public.wom_make_bat_ord_parts (valid);
CREATE INDEX IF NOT EXISTS idx_wom_make_bat_ord_parts_table_info_id
    ON public.wom_make_bat_ord_parts (table_info_id);
CREATE INDEX IF NOT EXISTS idx_wom_make_bat_ord_parts_cid
    ON public.wom_make_bat_ord_parts (cid);

DO $$
BEGIN
  IF to_regclass('public.wom_make_bat_ord_parts') IS NOT NULL
     AND (to_regclass('public.wom_make_bat_ord_parts_sv') IS NULL OR EXISTS (
       SELECT 1
       FROM pg_class c
       JOIN pg_namespace n ON n.oid = c.relnamespace
       WHERE n.nspname = 'public'
         AND c.relname = 'wom_make_bat_ord_parts_sv'
         AND c.relkind = 'v'
     )) THEN
    EXECUTE $view$
CREATE OR REPLACE VIEW public.wom_make_bat_ord_parts_sv AS
SELECT table_info_id, owner_staff_id AS staff
FROM public.wom_make_bat_ord_parts
WHERE table_info_id IS NOT NULL AND owner_staff_id IS NOT NULL
$view$;
  END IF;
END $$;
