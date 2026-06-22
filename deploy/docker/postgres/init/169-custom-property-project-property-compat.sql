-- PostgreSQL compatibility for custom-property model management.
--
-- The recovered custom-property service maps PropertyProject to
-- project_property and updates it together with runtime_property. The vendor
-- runtime metadata currently stores the same rows in runtime_property only, so
-- expose a simple updatable compatibility view for the legacy table name.

CREATE OR REPLACE VIEW public.project_property AS
SELECT *
FROM public.runtime_property;
