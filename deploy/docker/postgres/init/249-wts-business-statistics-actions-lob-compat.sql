-- Keep restored WTS business/statistics layouts compatible with the legacy
-- Hibernate @Lob mapping. runtime_extra_view stores real OIDs; ec_extra_view
-- keeps a text column whose value must still point to a PostgreSQL large object.

BEGIN;

DO $do$
DECLARE
    item record;
    payload_oid oid;
    invalid_count bigint;
    target_codes text[] := ARRAY[
        'WTS_1.0.0_workTicket_soilWork',
        'WTS_1.0.0_workTicket_limitSpaceWork',
        'WTS_1.0.0_workTicket_liftWork',
        'WTS_1.0.0_workTicket_electricityWork',
        'WTS_1.0.0_workTicket_heightWork',
        'WTS_1.0.0_workTicket_firework',
        'WTS_1.0.0_workTicket_breakWork',
        'WTS_1.0.0_workTicket_blockWork',
        'WTS_1.0.0_workTicket_workList',
        'WTS_1.0.0_blindPlateAccount_plateAccountList',
        'WTS_1.0.0_workTicket_workTicket',
        'WTS_1.0.0_workTicket_soilWorkEdit',
        'WTS_1.0.0_workTicket_limitSpaceWorkEdit',
        'WTS_1.0.0_workTicket_electricityEdit',
        'WTS_1.0.0_workTicket_heightWorkEdit',
        'WTS_1.0.0_workTicket_breakWorkEdit',
        'WTS_1.0.0_workTicket_soilWorkView',
        'WTS_1.0.0_workTicket_limitSpaceView',
        'WTS_1.0.0_workTicket_liftWorkView',
        'WTS_1.0.0_workTicket_electricityView',
        'WTS_1.0.0_workTicket_heightWorkView',
        'WTS_1.0.0_workTicket_fireworkView',
        'WTS_1.0.0_workTicket_breakWorkView',
        'WTS_1.0.0_workTicket_blockWorkView'
    ];
BEGIN
    FOR item IN
        SELECT ctid, view_json
        FROM public.ec_extra_view
        WHERE code = ANY (target_codes)
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
    WHERE target.code = ANY (target_codes)
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
            'WTS business/statistics VIEW_JSON conversion incomplete: % invalid values',
            invalid_count;
    END IF;
END $do$;

COMMIT;
