-- Keep the recovered WTS design-time action layouts compatible with the
-- PostgreSQL @Lob mapping after migration 230 restores their JSON payloads.
--
-- runtime_extra_view stores real OIDs. The legacy ec_extra_view table keeps a
-- text column, but Hibernate still treats VIEW_JSON as a CLOB and therefore
-- expects that text to contain a PostgreSQL large-object reference.

BEGIN;

DO $do$
DECLARE
    item record;
    payload_oid oid;
    invalid_count bigint;
BEGIN
    FOR item IN
        SELECT ctid, view_json
        FROM public.ec_extra_view
        WHERE code LIKE ANY (
            ARRAY[
                'WTS_1.0.0_approveConfig_%',
                'WTS_1.0.0_checkCriteriaLib_%',
                'WTS_1.0.0_customizedConfig_%',
                'WTS_1.0.0_gasAnalysis_%',
                'WTS_1.0.0_hazidLib_%',
                'WTS_1.0.0_hourLimit_%',
                'WTS_1.0.0_qualifiSet_%',
                'WTS_1.0.0_riskSafeMeasures_%',
                'WTS_1.0.0_wfGasAnalysis_%',
                'WTS_1.0.0_wfPathFunction_%',
                'WTS_1.0.0_workRegion_%'
            ]
        )
          AND view_json IS NOT NULL
    LOOP
        IF item.view_json ~ '^[0-9]+$'
           AND EXISTS (
               SELECT 1
               FROM pg_largeobject_metadata
               WHERE oid = item.view_json::oid
           ) THEN
            CONTINUE;
        END IF;

        payload_oid := lo_from_bytea(0, convert_to(item.view_json, 'UTF8'));
        UPDATE public.ec_extra_view
        SET view_json = payload_oid::text
        WHERE ctid = item.ctid;
    END LOOP;

    SELECT count(*)
    INTO invalid_count
    FROM public.ec_extra_view target
    WHERE target.code LIKE ANY (
        ARRAY[
            'WTS_1.0.0_approveConfig_%',
            'WTS_1.0.0_checkCriteriaLib_%',
            'WTS_1.0.0_customizedConfig_%',
            'WTS_1.0.0_gasAnalysis_%',
            'WTS_1.0.0_hazidLib_%',
            'WTS_1.0.0_hourLimit_%',
            'WTS_1.0.0_qualifiSet_%',
            'WTS_1.0.0_riskSafeMeasures_%',
            'WTS_1.0.0_wfGasAnalysis_%',
            'WTS_1.0.0_wfPathFunction_%',
            'WTS_1.0.0_workRegion_%'
        ]
    )
      AND target.view_json IS NOT NULL
      AND NOT (
          target.view_json ~ '^[0-9]+$'
          AND EXISTS (
              SELECT 1
              FROM pg_largeobject_metadata
              WHERE oid = target.view_json::oid
          )
      );

    IF invalid_count > 0 THEN
        RAISE EXCEPTION
            'WTS basic-settings VIEW_JSON conversion incomplete: % invalid values',
            invalid_count;
    END IF;
END $do$;

COMMIT;
