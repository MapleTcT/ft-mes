-- Compatibility for newer SQL model metadata. Vendor 1.2.8 adds dm_sql to
-- ec_sql_model and the 2022 configuration service maps it through Hibernate.
ALTER TABLE public.ec_sql_model
  ADD COLUMN IF NOT EXISTS dm_sql text;
