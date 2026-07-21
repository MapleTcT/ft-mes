\set ON_ERROR_STOP on

BEGIN;

SELECT 1 / CASE WHEN EXISTS (
    SELECT 1
      FROM bpi.bpi_topology_versions
     WHERE tenant_id = :'tenant_id'
       AND created_by = :'marker'
    UNION ALL
    SELECT 1
      FROM bpi.bpi_rule_versions
     WHERE tenant_id = :'tenant_id'
       AND created_by = :'marker'
    UNION ALL
    SELECT 1
      FROM bpi.bpi_batch_candidates
     WHERE tenant_id = :'tenant_id'
       AND order_id = :'order_id'
) THEN 0 ELSE 1 END AS clean_marker_guard;

INSERT INTO bpi.bpi_topology_versions
    (id, tenant_id, topology_code, version, state, checksum, definition,
     plant_id, line_id, revision, created_by, updated_by)
VALUES (
    md5(:'marker' || ':topology')::uuid,
    :'tenant_id',
    :'topology_code',
    :'topology_version',
    'PUBLISHED',
    repeat('t', 64),
    jsonb_build_object(
        'localityGroup', 'FEED',
        'nodes', jsonb_build_array(
            jsonb_build_object('code', :'line_id', 'type', 'LINE'),
            jsonb_build_object('code', :'device_id', 'type', 'DEVICE')
        ),
        'bindings', jsonb_build_array(
            jsonb_build_object(
                'signal', 'feed.flow',
                'productId', :'product_id',
                'deviceId', :'device_id',
                'propertyId', 'flow',
                'expectedUnit', 'm3/h',
                'calibrationVersion', 'E2E-1'
            )
        )
    ),
    :'plant_id',
    :'line_id',
    1,
    :'marker',
    :'marker'
);

INSERT INTO bpi.bpi_rule_versions
    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition,
     revision, plant_id, line_id, created_by, updated_by)
VALUES (
    md5(:'marker' || ':rule')::uuid,
    :'tenant_id',
    :'rule_code',
    :'rule_version',
    md5(:'marker' || ':topology')::uuid,
    'PUBLISHED',
    repeat('r', 64),
    jsonb_build_object(
        'boundaryType', 'START',
        'quorumMinimum', 1,
        'minimumConfidence', 1.0,
        'maxCompositePenalty', 0.0,
        'timing', jsonb_build_object(
            'allowedLatenessSeconds', 30,
            'watermarkDelaySeconds', 30,
            'evaluationTimeoutSeconds', 300
        ),
        'conditions', jsonb_build_array(
            jsonb_build_object(
                'signal', 'feed.flow',
                'operator', 'GREATER_THAN',
                'threshold', 2,
                'holdSeconds', 0,
                'maxSilenceSeconds', 120,
                'classification', 'QUORUM',
                'weight', 100
            )
        )
    ),
    1,
    :'plant_id',
    :'line_id',
    :'marker',
    :'marker'
);

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
SELECT md5(:'marker' || ':flag:commands')::uuid,
       :'tenant_id',
       'LINE',
       :'line_id',
       'bpi.commands',
       true,
       1,
       :'marker'
 WHERE NOT EXISTS (
        SELECT 1
          FROM bpi.bpi_feature_flags existing
         WHERE existing.tenant_id = :'tenant_id'
           AND existing.scope_type = 'LINE'
           AND existing.scope_key = :'line_id'
           AND existing.flag_key = 'bpi.commands'
       );

SELECT json_build_object(
    'status', 'SEEDED_PUBLISHED_FIXTURE',
    'fixturePurpose', 'CONTROLLED_INTEGRATED_ROLLBACK_ONLY',
    'marker', :'marker',
    'tenantId', :'tenant_id',
    'plantId', :'plant_id',
    'lineId', :'line_id',
    'orderId', :'order_id',
    'topologyId', md5(:'marker' || ':topology')::uuid,
    'topology', :'topology_code' || '@' || :'topology_version',
    'topologyState', 'PUBLISHED',
    'ruleId', md5(:'marker' || ':rule')::uuid,
    'rule', :'rule_code' || '@' || :'rule_version',
    'ruleState', 'PUBLISHED'
);

COMMIT;
