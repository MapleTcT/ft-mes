-- PostgreSQL compatibility for legacy import/native SQL that binds textual
-- boolean values such as 'FALSE' into boolean columns.
--
-- The recovered ADP services sometimes build INSERT statements with varchar
-- parameters for boolean fields. PostgreSQL can explicitly cast these values,
-- but assignment into a boolean column needs an assignment cast.

DO $$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT *
        FROM (
            VALUES
                ('character varying'::regtype),
                ('text'::regtype),
                ('bpchar'::regtype)
        ) AS casts(source_type)
    LOOP
        IF EXISTS (
            SELECT 1
            FROM pg_catalog.pg_cast
            WHERE castsource = item.source_type
              AND casttarget = 'boolean'::regtype
        ) THEN
            UPDATE pg_catalog.pg_cast
               SET castcontext = 'a'
             WHERE castsource = item.source_type
               AND casttarget = 'boolean'::regtype
               AND castcontext <> 'a';
        ELSE
            EXECUTE format(
                'CREATE CAST (%s AS boolean) WITH INOUT AS ASSIGNMENT',
                item.source_type::text
            );
        END IF;
    END LOOP;
END $$;
