\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE bpi_point_catalog_pagination_targets ON COMMIT DROP AS
SELECT id
  FROM bpi.bpi_point_catalog_snapshots
 WHERE tenant_id = :'tenant_id'
   AND source_revision LIKE :'marker' || '_CATALOG_%';

DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = :'tenant_id'
   AND object_type = 'POINT_CATALOG_SNAPSHOT'
   AND object_id IN (SELECT id FROM bpi_point_catalog_pagination_targets);

DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = :'tenant_id'
   AND idempotency_key LIKE :'marker' || '_IMPORT_%';

DELETE FROM bpi.bpi_point_catalog_entries
 WHERE tenant_id = :'tenant_id'
   AND snapshot_id IN (SELECT id FROM bpi_point_catalog_pagination_targets);

DELETE FROM bpi.bpi_point_catalog_snapshots
 WHERE tenant_id = :'tenant_id'
   AND id IN (SELECT id FROM bpi_point_catalog_pagination_targets);

SELECT count(*) = 0 AS cleanup_ok
  FROM bpi.bpi_point_catalog_snapshots
 WHERE tenant_id = :'tenant_id'
   AND source_revision LIKE :'marker' || '_CATALOG_%'
\gset

\if :cleanup_ok
\else
    \warn 'point catalog pagination cleanup left marker snapshots'
    \quit 1
\endif

COMMIT;

SELECT jsonb_build_object(
    'marker', :'marker',
    'remainingSnapshots', (
        SELECT count(*)
          FROM bpi.bpi_point_catalog_snapshots
         WHERE tenant_id = :'tenant_id'
           AND source_revision LIKE :'marker' || '_CATALOG_%'
    ),
    'remainingEntries', (
        SELECT count(*)
          FROM bpi.bpi_point_catalog_entries entry
          JOIN bpi.bpi_point_catalog_snapshots snapshot ON snapshot.id = entry.snapshot_id
         WHERE snapshot.tenant_id = :'tenant_id'
           AND snapshot.source_revision LIKE :'marker' || '_CATALOG_%'
    ),
    'remainingIdempotency', (
        SELECT count(*)
          FROM bpi.bpi_api_idempotency
         WHERE tenant_id = :'tenant_id'
           AND idempotency_key LIKE :'marker' || '_IMPORT_%'
    ),
    'remainingAudit', (
        SELECT count(*)
          FROM bpi.bpi_audit_events
         WHERE tenant_id = :'tenant_id'
           AND reason LIKE :'marker' || '%'
    )
) AS cleanup_result;
