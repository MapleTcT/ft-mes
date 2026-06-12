-- Module import records which companies can use a module.
CREATE TABLE IF NOT EXISTS public.module_company_ref (
  id bigint NOT NULL,
  module_code varchar(510) NOT NULL,
  company_id bigint,
  CONSTRAINT module_company_ref_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_module_company_ref_module_code
  ON public.module_company_ref(module_code);

CREATE INDEX IF NOT EXISTS idx_module_company_ref_company_id
  ON public.module_company_ref(company_id);
