-- Runtime TaskServiceImpl.createPendings inserts MEMO into wfm_task_pending.
-- Older recovered PostgreSQL DDL missed the column, which blocks workflow
-- start during business actions such as WOM prepare-need generation.

ALTER TABLE public.wfm_task_pending
  ADD COLUMN IF NOT EXISTS memo varchar(1024) DEFAULT NULL;
