-- OEE's legacy factory-tree service groups every valid node by lay_no and
-- throws when any active row has a null level. Normalize recovered orphan test
-- nodes without changing established parent relationships.

UPDATE public.hm_factory_models
SET lay_no = 1,
    lay_rec = COALESCE(NULLIF(lay_rec, ''), '-' || id || '-'),
    leaf = COALESCE(leaf, 1),
    modify_time = COALESCE(modify_time, CURRENT_TIMESTAMP)
WHERE COALESCE(valid, true) IS TRUE
  AND lay_no IS NULL;
