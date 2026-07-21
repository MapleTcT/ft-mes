-- Re-enable the fallback only when rolling back the application-owned PostgreSQL sync patch.
DO $$
BEGIN
    IF to_regprocedure('public.adp_ec_model_physical_table_sync_trigger()') IS NOT NULL
       AND to_regclass('public.ec_model') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_trigger
           WHERE tgname = 'tr_adp_ec_model_physical_table_sync'
             AND tgrelid = 'public.ec_model'::regclass
             AND NOT tgisinternal
       ) THEN
        CREATE TRIGGER tr_adp_ec_model_physical_table_sync
        AFTER INSERT OR UPDATE OF table_name, entity_code, is_main, data_type, is_extra_col, is_mne_code, valid, delete_time
        ON public.ec_model
        FOR EACH ROW EXECUTE FUNCTION public.adp_ec_model_physical_table_sync_trigger();
    END IF;

    IF to_regprocedure('public.adp_ec_property_physical_column_sync_trigger()') IS NOT NULL
       AND to_regclass('public.ec_property') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_trigger
           WHERE tgname = 'tr_adp_ec_property_physical_column_sync'
             AND tgrelid = 'public.ec_property'::regclass
             AND NOT tgisinternal
       ) THEN
        CREATE TRIGGER tr_adp_ec_property_physical_column_sync
        AFTER INSERT OR UPDATE OF model_code, column_name, type, max_length, decimal_num, is_index, valid, delete_time
        ON public.ec_property
        FOR EACH ROW EXECUTE FUNCTION public.adp_ec_property_physical_column_sync_trigger();
    END IF;
END;
$$;
