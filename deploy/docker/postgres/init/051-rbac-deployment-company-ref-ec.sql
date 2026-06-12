-- Workflow import writes company references through a legacy native SQL path:
-- insert into RBAC_DEPLOYMENT_COMPANY_REF_EC(ID, DEPLOYMENT_ID, CID, FLOW_KEY).
CREATE TABLE IF NOT EXISTS public.rbac_deployment_company_ref_ec (
  id bigint NOT NULL,
  deployment_id bigint,
  cid bigint,
  flow_key varchar(510),
  CONSTRAINT rbac_deployment_company_ref_ec_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_rbac_deploy_company_ref_ec_deployment
  ON public.rbac_deployment_company_ref_ec(deployment_id);

CREATE INDEX IF NOT EXISTS idx_rbac_deploy_company_ref_ec_flow_company
  ON public.rbac_deployment_company_ref_ec(flow_key, cid);
