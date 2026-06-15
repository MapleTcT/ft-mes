-- Target PostgreSQL schema inventory.
-- Usage:
--   psql "$TARGET_DSN" -v ON_ERROR_STOP=1 -f target-schema-inventory.sql

select
  current_database() as database_name,
  current_schema() as current_schema,
  version() as postgres_version;

select
  table_schema,
  table_name,
  table_type
from information_schema.tables
where table_schema not in ('information_schema', 'pg_catalog')
order by table_schema, table_name;

select
  table_schema,
  table_name,
  column_name,
  ordinal_position,
  data_type,
  udt_name,
  is_nullable,
  column_default
from information_schema.columns
where table_schema not in ('information_schema', 'pg_catalog')
order by table_schema, table_name, ordinal_position;

select
  schemaname as table_schema,
  tablename as table_name,
  indexname,
  indexdef
from pg_indexes
where schemaname not in ('pg_catalog', 'information_schema')
order by schemaname, tablename, indexname;
