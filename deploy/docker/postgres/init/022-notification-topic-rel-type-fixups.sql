DO $$
BEGIN
  IF to_regclass('public.notice_topic_tmpl_rel') IS NULL THEN
    RETURN;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'notice_topic_tmpl_rel'
      AND column_name IN ('notice_topic_id', 'notice_tmpl_id', 'notice_protocol_id')
      AND data_type <> 'bigint'
  ) THEN
    IF EXISTS (
      SELECT 1
      FROM public.notice_topic_tmpl_rel
      WHERE notice_topic_id::text !~ '^[0-9]+$'
         OR notice_tmpl_id::text !~ '^[0-9]+$'
         OR notice_protocol_id::text !~ '^[0-9]+$'
    ) THEN
      RAISE EXCEPTION 'notice_topic_tmpl_rel contains non-numeric relation ids';
    END IF;

    ALTER TABLE public.notice_topic_tmpl_rel
      DROP CONSTRAINT IF EXISTS udx_notice_topic_tmpl_rel;
    DROP INDEX IF EXISTS public.udx_notice_topic_tmpl_rel;

    ALTER TABLE public.notice_topic_tmpl_rel
      ALTER COLUMN notice_topic_id TYPE bigint USING notice_topic_id::text::bigint,
      ALTER COLUMN notice_tmpl_id TYPE bigint USING notice_tmpl_id::text::bigint,
      ALTER COLUMN notice_protocol_id TYPE bigint USING notice_protocol_id::text::bigint;
  END IF;

  ALTER TABLE public.notice_topic_tmpl_rel
    ALTER COLUMN notice_topic_id SET NOT NULL,
    ALTER COLUMN notice_tmpl_id SET NOT NULL,
    ALTER COLUMN notice_protocol_id SET NOT NULL;

  CREATE UNIQUE INDEX IF NOT EXISTS udx_notice_topic_tmpl_rel
    ON public.notice_topic_tmpl_rel (notice_topic_id, notice_tmpl_id, notice_protocol_id);
END $$;
