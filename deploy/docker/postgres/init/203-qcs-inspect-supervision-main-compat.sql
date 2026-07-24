-- QCS inspection supervision compatibility for PostgreSQL runtime.
--
-- Recovered QCS metadata exposes supervision-style fields on the inspection
-- detail path. The PostgreSQL table restored from legacy view metadata did not
-- include those columns, so /QCS/inspect/inspect/data/{id} failed with:
--   ERROR: column this_.main_obj does not exist

ALTER TABLE public.qcs_inspects
  ADD COLUMN IF NOT EXISTS main_obj int8,
  ADD COLUMN IF NOT EXISTS staff int8,
  ADD COLUMN IF NOT EXISTS recalled_flag boolean,
  ADD COLUMN IF NOT EXISTS user_agent varchar(2000);

UPDATE public.qcs_inspects
   SET main_obj = COALESCE(main_obj, id),
       staff = COALESCE(staff, owner_staff_id, create_staff_id, apply_staff_id),
       recalled_flag = COALESCE(recalled_flag, false)
 WHERE main_obj IS NULL
    OR staff IS NULL
    OR recalled_flag IS NULL;

CREATE INDEX IF NOT EXISTS idx_qcs_inspects_main_obj
    ON public.qcs_inspects (main_obj);

CREATE INDEX IF NOT EXISTS idx_qcs_inspects_staff
    ON public.qcs_inspects (staff);

CREATE OR REPLACE VIEW public.qcs_inspects_sv AS
SELECT
  table_info_id,
  COALESCE(staff, owner_staff_id, create_staff_id, apply_staff_id) AS staff,
  id,
  COALESCE(main_obj, id) AS main_obj,
  valid,
  version,
  create_staff_id,
  create_time,
  modify_staff_id,
  modify_time,
  delete_staff_id,
  delete_time,
  cid,
  sort,
  recalled_flag,
  user_agent
FROM public.qcs_inspects
WHERE table_info_id IS NOT NULL
  AND COALESCE(staff, owner_staff_id, create_staff_id, apply_staff_id) IS NOT NULL;
