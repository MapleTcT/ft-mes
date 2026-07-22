#!/bin/sh
set -eu

ACTION=${1:-provision}

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${BPI_DATABASE_NAME:?BPI_DATABASE_NAME is required}"
: "${BPI_RETENTION_ARCHIVER_DATABASE_PASSWORD:?BPI_RETENTION_ARCHIVER_DATABASE_PASSWORD is required}"

case "$ACTION" in
    provision)
        psql --set ON_ERROR_STOP=1 \
            --username "$POSTGRES_USER" \
            --dbname "$POSTGRES_DB" \
            --set bpi_database="$BPI_DATABASE_NAME" <<'SQL'
\getenv archiver_password BPI_RETENTION_ARCHIVER_DATABASE_PASSWORD
SELECT format(
           'CREATE ROLE bpi_retention_archiver LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD %L',
           :'archiver_password')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_retention_archiver')
\gexec
SELECT format(
           'ALTER ROLE bpi_retention_archiver LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD %L',
           :'archiver_password')
\gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM bpi_retention_archiver', :'bpi_database')
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO bpi_retention_archiver', :'bpi_database')
\gexec
SQL
        psql --set ON_ERROR_STOP=1 \
            --username "$POSTGRES_USER" \
            --dbname "$BPI_DATABASE_NAME" <<'SQL'
REVOKE ALL ON SCHEMA public FROM bpi_retention_archiver;
DO $provision$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'bpi') THEN
        REVOKE ALL ON SCHEMA bpi FROM bpi_retention_archiver;
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA bpi FROM bpi_retention_archiver;
        REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA bpi FROM bpi_retention_archiver;
        REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA bpi FROM bpi_retention_archiver;
    END IF;
END
$provision$;
SQL
        ;;
    grant)
        psql --set ON_ERROR_STOP=1 \
            --username "$POSTGRES_USER" \
            --dbname "$BPI_DATABASE_NAME" <<'SQL'
DO $grant_check$
BEGIN
    IF to_regclass('bpi.bpi_dataset_definitions') IS NULL
       OR to_regclass('bpi.bpi_dataset_snapshots') IS NULL
       OR to_regclass('bpi.bpi_dataset_materializations') IS NULL
       OR to_regclass('bpi.bpi_dataset_catalog_publications') IS NULL
       OR to_regclass('bpi.bpi_dataset_retention_archives') IS NULL
       OR to_regclass('bpi.bpi_audit_events') IS NULL THEN
        RAISE EXCEPTION 'BPI retention archiver tables are incomplete';
    END IF;
END
$grant_check$;
GRANT USAGE ON SCHEMA bpi TO bpi_retention_archiver;
GRANT SELECT ON bpi.bpi_dataset_definitions TO bpi_retention_archiver;
GRANT SELECT ON bpi.bpi_dataset_snapshots TO bpi_retention_archiver;
GRANT SELECT ON bpi.bpi_dataset_materializations TO bpi_retention_archiver;
GRANT SELECT ON bpi.bpi_dataset_catalog_publications TO bpi_retention_archiver;
GRANT SELECT, UPDATE ON bpi.bpi_dataset_retention_archives TO bpi_retention_archiver;
GRANT INSERT ON bpi.bpi_audit_events TO bpi_retention_archiver;
SQL
        ;;
    verify)
        psql --set ON_ERROR_STOP=1 \
            --username "$POSTGRES_USER" \
            --dbname "$BPI_DATABASE_NAME" <<'SQL'
DO $verify$
DECLARE
    unexpected record;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles
         WHERE rolname = 'bpi_retention_archiver'
           AND rolcanlogin AND NOT rolsuper AND NOT rolinherit
           AND NOT rolcreaterole AND NOT rolcreatedb
           AND NOT rolreplication AND NOT rolbypassrls
    ) THEN
        RAISE EXCEPTION 'bpi_retention_archiver role attributes violate least privilege';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_auth_members membership
        JOIN pg_roles member ON member.oid = membership.member
        WHERE member.rolname = 'bpi_retention_archiver'
    ) THEN
        RAISE EXCEPTION 'bpi_retention_archiver must not assume another role';
    END IF;
    IF NOT has_database_privilege('bpi_retention_archiver', current_database(), 'CONNECT')
       OR has_database_privilege('bpi_retention_archiver', current_database(), 'CREATE') THEN
        RAISE EXCEPTION 'bpi_retention_archiver database privileges are invalid';
    END IF;
    IF NOT has_schema_privilege('bpi_retention_archiver', 'bpi', 'USAGE')
       OR has_schema_privilege('bpi_retention_archiver', 'bpi', 'CREATE') THEN
        RAISE EXCEPTION 'bpi_retention_archiver schema privileges are invalid';
    END IF;

    FOR unexpected IN
        WITH privileges(privilege_type) AS (
            VALUES ('SELECT'), ('INSERT'), ('UPDATE'), ('DELETE'),
                   ('TRUNCATE'), ('REFERENCES'), ('TRIGGER')
        ), expected(table_name, privilege_type) AS (
            VALUES
                ('bpi_dataset_definitions', 'SELECT'),
                ('bpi_dataset_snapshots', 'SELECT'),
                ('bpi_dataset_materializations', 'SELECT'),
                ('bpi_dataset_catalog_publications', 'SELECT'),
                ('bpi_dataset_retention_archives', 'SELECT'),
                ('bpi_dataset_retention_archives', 'UPDATE'),
                ('bpi_audit_events', 'INSERT')
        )
        SELECT tables.table_name, privileges.privilege_type
          FROM information_schema.tables tables CROSS JOIN privileges
         WHERE tables.table_schema = 'bpi'
           AND has_table_privilege(
                   'bpi_retention_archiver',
                   format('%I.%I', tables.table_schema, tables.table_name),
                   privileges.privilege_type)
           AND NOT EXISTS (
               SELECT 1 FROM expected
                WHERE expected.table_name = tables.table_name
                  AND expected.privilege_type = privileges.privilege_type)
    LOOP
        RAISE EXCEPTION 'unexpected bpi_retention_archiver privilege: %.%',
            unexpected.table_name, unexpected.privilege_type;
    END LOOP;

    IF NOT has_table_privilege('bpi_retention_archiver', 'bpi.bpi_dataset_definitions', 'SELECT')
       OR NOT has_table_privilege('bpi_retention_archiver', 'bpi.bpi_dataset_snapshots', 'SELECT')
       OR NOT has_table_privilege('bpi_retention_archiver', 'bpi.bpi_dataset_materializations', 'SELECT')
       OR NOT has_table_privilege('bpi_retention_archiver', 'bpi.bpi_dataset_catalog_publications', 'SELECT')
       OR NOT has_table_privilege('bpi_retention_archiver', 'bpi.bpi_dataset_retention_archives', 'SELECT')
       OR NOT has_table_privilege('bpi_retention_archiver', 'bpi.bpi_dataset_retention_archives', 'UPDATE')
       OR NOT has_table_privilege('bpi_retention_archiver', 'bpi.bpi_audit_events', 'INSERT') THEN
        RAISE EXCEPTION 'required bpi_retention_archiver privileges are incomplete';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_proc function
        JOIN pg_namespace namespace ON namespace.oid = function.pronamespace
        WHERE namespace.nspname = 'bpi'
          AND has_function_privilege('bpi_retention_archiver', function.oid, 'EXECUTE')
    ) THEN
        RAISE EXCEPTION 'bpi_retention_archiver must not execute BPI functions';
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.sequences sequence
        WHERE sequence.sequence_schema = 'bpi'
          AND has_sequence_privilege(
              'bpi_retention_archiver',
              format('%I.%I', sequence.sequence_schema, sequence.sequence_name),
              'USAGE,SELECT,UPDATE')
    ) THEN
        RAISE EXCEPTION 'bpi_retention_archiver must not access BPI sequences';
    END IF;
END
$verify$;
SQL
        ;;
    *)
        printf 'ERROR: expected provision, grant or verify, received: %s\n' "$ACTION" >&2
        exit 1
        ;;
esac

printf 'BPI retention archiver database role: PASS (%s)\n' "$ACTION"
