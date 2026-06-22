-- PostgreSQL compatibility for legacy QCS unqualified treatment creation.
--
-- QCSUnQlfDealServiceImpl.createUnQlfDealForOtherModule filters reports with
-- unQlfDealFlag = 0. PostgreSQL boolean NULL does not match that predicate,
-- so migrated/generated reports with a NULL flag cannot create a treatment
-- document even when no treatment exists yet. Keep the legacy false-by-default
-- behavior explicit.

DO $$
BEGIN
  IF to_regclass('public.qcs_inspect_reports') IS NOT NULL
     AND EXISTS (
       SELECT 1
       FROM information_schema.columns
       WHERE table_schema = 'public'
         AND table_name = 'qcs_inspect_reports'
         AND column_name = 'un_qlf_deal_flag'
     ) THEN
    UPDATE public.qcs_inspect_reports
    SET un_qlf_deal_flag = false
    WHERE un_qlf_deal_flag IS NULL;

    ALTER TABLE public.qcs_inspect_reports
      ALTER COLUMN un_qlf_deal_flag SET DEFAULT false;
  END IF;
END $$;
