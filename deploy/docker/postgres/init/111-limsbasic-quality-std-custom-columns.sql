-- PostgreSQL compatibility columns for RM/LIMSBasic quality standard entity.
--
-- Source evidence:
-- - WOMProduceTask.qualityStdId maps to RM RMRmQualityStd.
-- - RMRmQualityStd.TABLE_NAME = limsba_quality_stds and the entity declares
--   BIGINTPARAMC/D/E, CHARPARAMC/D/E, DATEPARAMC/D, and NUMBERPARAMC.
-- - Without these columns, WOM createManuInspect fails while loading
--   WOMProduceTask with PostgreSQL SQLGrammarException.

ALTER TABLE public.limsba_quality_stds
    ADD COLUMN IF NOT EXISTS bigintparamc integer,
    ADD COLUMN IF NOT EXISTS bigintparamd integer,
    ADD COLUMN IF NOT EXISTS bigintparame integer,
    ADD COLUMN IF NOT EXISTS charparamc text,
    ADD COLUMN IF NOT EXISTS charparamd text,
    ADD COLUMN IF NOT EXISTS charparame text,
    ADD COLUMN IF NOT EXISTS dateparamc timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamd timestamp without time zone,
    ADD COLUMN IF NOT EXISTS numberparamc numeric(38, 6);
