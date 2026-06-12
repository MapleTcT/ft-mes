-- Restore the vendor 1.1.5 schema delta for PostgreSQL module imports.
-- The Hibernate Script entity maps "code" as script content, while the
-- original base table only has "script_code" as the identifier.
ALTER TABLE IF EXISTS public.sc_script
  ADD COLUMN IF NOT EXISTS code text;
