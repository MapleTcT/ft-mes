-- Compatibility for newer property metadata used by 6.1.2+ module packages.
ALTER TABLE public.ec_property
  ADD COLUMN IF NOT EXISTS is_support_sup_and_sub integer,
  ADD COLUMN IF NOT EXISTS is_pic_support_multi_select integer,
  ADD COLUMN IF NOT EXISTS max_pic_num integer,
  ADD COLUMN IF NOT EXISTS org_column_name varchar(510),
  ADD COLUMN IF NOT EXISTS is_tree_system_code integer,
  ADD COLUMN IF NOT EXISTS show_width integer;

ALTER TABLE public.runtime_property
  ADD COLUMN IF NOT EXISTS is_support_sup_and_sub boolean,
  ADD COLUMN IF NOT EXISTS is_pic_support_multi_select boolean,
  ADD COLUMN IF NOT EXISTS max_pic_num integer,
  ADD COLUMN IF NOT EXISTS org_column_name varchar(510),
  ADD COLUMN IF NOT EXISTS is_tree_system_code boolean,
  ADD COLUMN IF NOT EXISTS show_width integer;
