DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        REVOKE UPDATE, DELETE ON
            bpi.bpi_point_catalog_snapshots,
            bpi.bpi_point_catalog_entries
        FROM bpi_service;
    END IF;
END
$$;
