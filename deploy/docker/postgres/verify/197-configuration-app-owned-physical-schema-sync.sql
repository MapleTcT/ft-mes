DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname IN (
            'tr_adp_ec_model_physical_table_sync',
            'tr_adp_ec_property_physical_column_sync'
        )
          AND NOT tgisinternal
    ) THEN
        RAISE EXCEPTION 'legacy configuration physical-schema triggers are still enabled';
    END IF;
END;
$$;
