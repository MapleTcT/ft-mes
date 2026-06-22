-- Runtime compatibility for WOM QCS reject backfill on PostgreSQL.
-- Legacy WOM code expects ins_after_manu to be a non-null Boolean when QCS
-- calls /WOM/quality/quality/syncRejectReport during unqualified-deal effect.

UPDATE public.wom_produce_tasks
SET ins_after_manu = false
WHERE ins_after_manu IS NULL;

ALTER TABLE public.wom_produce_tasks
  ALTER COLUMN ins_after_manu SET DEFAULT false;
