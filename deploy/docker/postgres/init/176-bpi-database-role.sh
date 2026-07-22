#!/bin/sh
set -eu

: "${BPI_DATABASE_PASSWORD:?BPI_DATABASE_PASSWORD is required}"
: "${BPI_MIGRATOR_PASSWORD:?BPI_MIGRATOR_PASSWORD is required}"
: "${BPI_MATERIALIZER_DATABASE_PASSWORD:?BPI_MATERIALIZER_DATABASE_PASSWORD is required}"
: "${BPI_CATALOG_PUBLISHER_DATABASE_PASSWORD:?BPI_CATALOG_PUBLISHER_DATABASE_PASSWORD is required}"
: "${BPI_RETENTION_ARCHIVER_DATABASE_PASSWORD:?BPI_RETENTION_ARCHIVER_DATABASE_PASSWORD is required}"

BPI_DATABASE_NAME="${BPI_DATABASE_NAME:-ft_mes_bpi}"

psql --set ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set bpi_database="$BPI_DATABASE_NAME" \
  --set service_password="$BPI_DATABASE_PASSWORD" \
  --set migrator_password="$BPI_MIGRATOR_PASSWORD" \
  --set materializer_password="$BPI_MATERIALIZER_DATABASE_PASSWORD" \
  --set catalog_publisher_password="$BPI_CATALOG_PUBLISHER_DATABASE_PASSWORD" \
  --set retention_archiver_password="$BPI_RETENTION_ARCHIVER_DATABASE_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE bpi_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT PASSWORD %L', :'migrator_password')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_migrator')
\gexec
SELECT format('ALTER ROLE bpi_migrator PASSWORD %L', :'migrator_password')
\gexec

SELECT format('CREATE ROLE bpi_service LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT PASSWORD %L', :'service_password')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service')
\gexec
SELECT format('ALTER ROLE bpi_service PASSWORD %L', :'service_password')
\gexec

SELECT format('CREATE ROLE bpi_materializer LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT PASSWORD %L', :'materializer_password')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_materializer')
\gexec
SELECT format('ALTER ROLE bpi_materializer PASSWORD %L', :'materializer_password')
\gexec

SELECT format('CREATE ROLE bpi_catalog_publisher LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD %L', :'catalog_publisher_password')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_catalog_publisher')
\gexec
SELECT format('ALTER ROLE bpi_catalog_publisher LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD %L', :'catalog_publisher_password')
\gexec

SELECT format('CREATE ROLE bpi_retention_archiver LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD %L', :'retention_archiver_password')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_retention_archiver')
\gexec
SELECT format('ALTER ROLE bpi_retention_archiver LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD %L', :'retention_archiver_password')
\gexec

SELECT format('CREATE DATABASE %I OWNER bpi_migrator', :'bpi_database')
 WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'bpi_database')
\gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'bpi_database')
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO bpi_migrator, bpi_service, bpi_materializer, bpi_catalog_publisher, bpi_retention_archiver', :'bpi_database')
\gexec
SQL

psql --set ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$BPI_DATABASE_NAME" <<'SQL'
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO bpi_migrator, bpi_service, bpi_materializer, bpi_catalog_publisher, bpi_retention_archiver;
SQL
