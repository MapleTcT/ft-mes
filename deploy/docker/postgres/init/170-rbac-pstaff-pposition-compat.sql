-- PostgreSQL compatibility for legacy configuration delete paths.
--
-- EntityServiceImpl still cleans permission detail objects through BASE_* names
-- that were Oracle views over the RBAC tables. Some ADP packages ship without
-- the four RBAC detail tables below, so create the canonical RBAC tables first
-- and expose simple updatable BASE_* views for the legacy native SQL.

CREATE TABLE IF NOT EXISTS public.rbac_rolepstaff (
  id BIGINT PRIMARY KEY,
  version INTEGER NOT NULL DEFAULT 0,
  staff_id BIGINT,
  rolepermission_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_rbac_rolepstaff_rolepermission_id
  ON public.rbac_rolepstaff(rolepermission_id);

CREATE TABLE IF NOT EXISTS public.rbac_rolepposition (
  id BIGINT PRIMARY KEY,
  version INTEGER NOT NULL DEFAULT 0,
  include_lower BOOLEAN NOT NULL DEFAULT false,
  position_id BIGINT,
  rolepermission_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_rbac_rolepposition_rolepermission_id
  ON public.rbac_rolepposition(rolepermission_id);

CREATE TABLE IF NOT EXISTS public.rbac_userpstaff (
  id BIGINT PRIMARY KEY,
  version INTEGER NOT NULL DEFAULT 0,
  staff_id BIGINT,
  userpermission_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_rbac_userpstaff_userpermission_id
  ON public.rbac_userpstaff(userpermission_id);

CREATE TABLE IF NOT EXISTS public.rbac_userpposition (
  id BIGINT PRIMARY KEY,
  version INTEGER NOT NULL DEFAULT 0,
  include_lower BOOLEAN NOT NULL DEFAULT false,
  position_id BIGINT,
  userpermission_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_rbac_userpposition_userpermission_id
  ON public.rbac_userpposition(userpermission_id);

CREATE OR REPLACE VIEW public.base_rolepstaff (
  id,
  version,
  staff_id,
  rolepermission_id
) AS
SELECT
  id,
  version,
  staff_id,
  rolepermission_id
FROM public.rbac_rolepstaff;

CREATE OR REPLACE VIEW public.base_rolepposition (
  id,
  version,
  include_lower,
  position_id,
  rolepermission_id
) AS
SELECT
  id,
  version,
  include_lower,
  position_id,
  rolepermission_id
FROM public.rbac_rolepposition;

CREATE OR REPLACE VIEW public.base_userpstaff (
  id,
  version,
  staff_id,
  userpermission_id
) AS
SELECT
  id,
  version,
  staff_id,
  userpermission_id
FROM public.rbac_userpstaff;

CREATE OR REPLACE VIEW public.base_userpposition (
  id,
  version,
  include_lower,
  position_id,
  userpermission_id
) AS
SELECT
  id,
  version,
  include_lower,
  position_id,
  userpermission_id
FROM public.rbac_userpposition;
