-- Provide a minimal batch-control external system configuration for RM batch
-- formula sync acceptance on PostgreSQL test deployments.
--
-- RM's batch sync endpoint resolves baseset_other_systems.code by the
-- request origin and then uses the same code as the workflow deployment key.
-- Recovered test databases may contain the RM formula workflow deployment
-- but no external-system row, which makes /RM/formula/formula/batch/sync fail
-- with a NullPointerException before any business validation can run.

INSERT INTO public.baseset_other_systems (
    id,
    version,
    create_time,
    modify_time,
    valid,
    cid,
    status,
    code,
    system_name,
    post_url,
    url,
    username,
    password,
    system_type,
    remark
)
SELECT
    6579588704999001,
    0,
    now(),
    now(),
    true,
    1000,
    99,
    'formulaEnableFlw',
    'ADP E2E RM batch sync test system',
    '127.0.0.1:60000',
    '127.0.0.1',
    'adp-e2e',
    'adp-e2e',
    NULL,
    'Seeded for PostgreSQL RM batch formula sync acceptance; not a production external batch server.'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.baseset_other_systems
    WHERE code = 'formulaEnableFlw'
);

UPDATE public.baseset_other_systems
SET
    valid = true,
    cid = COALESCE(cid, 1000),
    post_url = COALESCE(NULLIF(post_url, ''), '127.0.0.1:60000'),
    url = COALESCE(NULLIF(url, ''), '127.0.0.1'),
    username = COALESCE(NULLIF(username, ''), 'adp-e2e'),
    password = COALESCE(NULLIF(password, ''), 'adp-e2e'),
    system_type = NULL,
    modify_time = now()
WHERE code = 'formulaEnableFlw';

UPDATE public.wf_deployment deployment
SET is_current_version = 1
WHERE deployment.process_key = 'formulaEnableFlw'
  AND COALESCE(deployment.valid, 1) = 1
  AND deployment.id = (
      SELECT latest.id
      FROM public.wf_deployment latest
      WHERE latest.process_key = 'formulaEnableFlw'
        AND COALESCE(latest.valid, 1) = 1
      ORDER BY latest.process_version DESC NULLS LAST, latest.id DESC
      LIMIT 1
  );
