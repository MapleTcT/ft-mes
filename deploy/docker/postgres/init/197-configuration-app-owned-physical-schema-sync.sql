-- Physical model and property DDL is owned by the patched configuration service.
-- Retire the earlier database-trigger fallback to avoid duplicate create/rename actions.
DROP TRIGGER IF EXISTS tr_adp_ec_model_physical_table_sync ON public.ec_model;
DROP TRIGGER IF EXISTS tr_adp_ec_property_physical_column_sync ON public.ec_property;
