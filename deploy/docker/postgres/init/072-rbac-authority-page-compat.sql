-- RBAC role/user authority pages use the newer menu-operate mapper from the
-- runtime package. The recovered PostgreSQL schema missed this newer workflow
-- column, causing GET /inter-api/rbac/v1/rolePermissions to fail when a menu
-- node is selected in the authority editor.

ALTER TABLE public.rbac_menuoperate
  ADD COLUMN IF NOT EXISTS current_flow_version varchar(510);

UPDATE public.rbac_menuoperate
SET current_flow_version = NULLIF(flow_version, '')
WHERE current_flow_version IS NULL
  AND flow_version IS NOT NULL
  AND btrim(flow_version) <> '';

-- The authority editor also asks FlowRefCompanyMapper for deployments visible
-- to the current company. Some recovered dumps only contain the EC import table
-- variant, while the runtime mapper is bound to RBAC_DEPLOYMENT_COMPANY_REF.
CREATE TABLE IF NOT EXISTS public.rbac_deployment_company_ref (
  id bigint NOT NULL,
  cid bigint,
  flow_key varchar(510),
  deployment_id bigint,
  CONSTRAINT rbac_deployment_company_ref_pkey PRIMARY KEY (id)
);

DO $$
BEGIN
  IF to_regclass('public.rbac_deployment_company_ref_ec') IS NOT NULL THEN
    INSERT INTO public.rbac_deployment_company_ref (id, cid, flow_key, deployment_id)
    SELECT id, cid, flow_key, deployment_id
    FROM public.rbac_deployment_company_ref_ec
    ON CONFLICT (id) DO UPDATE
    SET cid = EXCLUDED.cid,
        flow_key = EXCLUDED.flow_key,
        deployment_id = EXCLUDED.deployment_id;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rbac_deployment_company_ref_cid
  ON public.rbac_deployment_company_ref(cid);

CREATE INDEX IF NOT EXISTS idx_rbac_deployment_company_ref_flow_company
  ON public.rbac_deployment_company_ref(flow_key, cid);
