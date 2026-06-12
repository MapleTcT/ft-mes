-- Compatibility for newer base module packages. The running configuration
-- service maps view metadata with mobile_version, while older recovered
-- PostgreSQL schemas did not contain this column.
ALTER TABLE public.ec_view
  ADD COLUMN IF NOT EXISTS mobile_version integer;

ALTER TABLE public.ec_view
  ADD COLUMN IF NOT EXISTS move_flag integer;

ALTER TABLE public.runtime_view
  ADD COLUMN IF NOT EXISTS mobile_version integer;

ALTER TABLE public.runtime_view
  ADD COLUMN IF NOT EXISTS move_flag boolean;
