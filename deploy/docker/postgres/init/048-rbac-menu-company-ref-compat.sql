-- RBAC menu-company references are used by the configuration module import
-- path. Newer RBAC code maps COMPANY_NAME and APPID, while some recovered
-- PostgreSQL bases only had MENUINFO_ID and COMPANY_ID.
ALTER TABLE public.rbac_menuinfo_company_ref
  ADD COLUMN IF NOT EXISTS company_name varchar(510);

ALTER TABLE public.rbac_menuinfo_company_ref
  ADD COLUMN IF NOT EXISTS appid varchar(200);

UPDATE public.rbac_menuinfo_company_ref mcr
SET appid = mi.app
FROM public.rbac_menuinfo mi
WHERE mcr.menuinfo_id = mi.id
  AND coalesce(mi.valid, true) = true
  AND mcr.appid IS NULL;

CREATE INDEX IF NOT EXISTS idx_rbac_menuinfo_company_ref_appid
  ON public.rbac_menuinfo_company_ref(appid);
