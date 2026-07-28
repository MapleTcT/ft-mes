-- Restore the factory hierarchy required by the legacy tree editor.
--
-- The recovered PostgreSQL dataset can contain valid factory-model rows with
-- both parent columns set to NULL. The legacy tree endpoint always asks for
-- parent_id = -1 first, so that dataset renders only a synthetic "---" node.
-- In that state the toolbar is visible, but Add/Modify/Delete has no real tree
-- selection and cannot open the edit flow.
--
-- Keep an existing top-level root when one is already configured. Otherwise
-- reuse the recovered default factory root, creating it only when the baseline
-- has neither. Reparent only true orphans in the same company; established
-- hierarchy rows are left unchanged.

DO $$
DECLARE
    factory_root_id bigint;
    factory_company_id bigint;
    factory_root_name text;
    default_root_id constant bigint := 7000000000001000;
BEGIN
    SELECT model.id, model.cid, model.name
      INTO factory_root_id, factory_company_id, factory_root_name
      FROM public.hm_factory_models model
     WHERE COALESCE(model.valid, true) IS TRUE
       AND (
            model.parent_id = -1
            OR model.parentid = -1
            OR model.code = 'codex_default_oee_root'
       )
     ORDER BY
        CASE WHEN model.code = 'codex_default_oee_root' THEN 0 ELSE 1 END,
        model.id
     LIMIT 1;

    IF factory_root_id IS NULL THEN
        IF EXISTS (
            SELECT 1
              FROM public.hm_factory_models
             WHERE id = default_root_id
        ) THEN
            RAISE EXCEPTION
                'Factory tree root is missing and reserved id % is already occupied',
                default_root_id;
        END IF;

        SELECT id
          INTO factory_company_id
          FROM public.base_company
         WHERE valid = 1
         ORDER BY id
         LIMIT 1;

        factory_company_id := COALESCE(factory_company_id, 1000);
        factory_root_id := default_root_id;
        factory_root_name := '默认工厂';

        INSERT INTO public.hm_factory_models (
            id,
            version,
            create_time,
            valid,
            cid,
            code,
            name,
            parent_id,
            parentid,
            lay_no,
            lay_rec,
            leaf,
            sort,
            full_path_name,
            working_type
        )
        VALUES (
            factory_root_id,
            0,
            CURRENT_TIMESTAMP,
            true,
            factory_company_id,
            'codex_default_oee_root',
            factory_root_name,
            -1,
            -1,
            1,
            '-' || factory_root_id || '-',
            1,
            0,
            factory_root_name,
            'HierarchicalMod_workingType/notOccupied'
        );
    END IF;

    UPDATE public.hm_factory_models
       SET parent_id = -1,
           parentid = -1,
           lay_no = 1,
           lay_rec = '-' || factory_root_id || '-',
           sort = COALESCE(sort, 0),
           full_path_name = COALESCE(NULLIF(full_path_name, ''), name),
           modify_time = COALESCE(modify_time, CURRENT_TIMESTAMP)
     WHERE id = factory_root_id;

    UPDATE public.hm_factory_models model
       SET parent_id = factory_root_id,
           parentid = factory_root_id,
           lay_no = 2,
           lay_rec = '-' || factory_root_id || '-' || model.id || '-',
           leaf = COALESCE(model.leaf, 1),
           full_path_name = CONCAT_WS(
               '/',
               NULLIF(factory_root_name, ''),
               NULLIF(model.name, '')
           ),
           modify_time = COALESCE(model.modify_time, CURRENT_TIMESTAMP)
     WHERE model.id <> factory_root_id
       AND model.cid = factory_company_id
       AND COALESCE(model.valid, true) IS TRUE
       AND model.parent_id IS NULL
       AND model.parentid IS NULL;

    UPDATE public.hm_factory_models root_model
       SET leaf = CASE
            WHEN EXISTS (
                SELECT 1
                  FROM public.hm_factory_models child
                 WHERE child.parent_id = factory_root_id
                   AND COALESCE(child.valid, true) IS TRUE
            ) THEN 0
            ELSE 1
       END,
           modify_time = COALESCE(root_model.modify_time, CURRENT_TIMESTAMP)
     WHERE root_model.id = factory_root_id;
END $$;

CREATE INDEX IF NOT EXISTS idx_hm_factory_models_tree_parent
    ON public.hm_factory_models (parent_id, valid, sort, id);
