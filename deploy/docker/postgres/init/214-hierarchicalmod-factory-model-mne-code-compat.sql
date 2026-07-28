-- PostgreSQL compatibility for HierarchicalMod factory-model mnemonic codes.
--
-- The legacy BAP mnemonic-code service reads the model metadata column
-- factory_model, while different Hibernate naming strategies created
-- factorymodel_id and factory_model_id in restored environments. Keep the
-- aliases synchronized so both the legacy JDBC path and JPA path can coexist.

CREATE TABLE IF NOT EXISTS public.hm_factory_models_mc (
    id bigint
);

DO $$
DECLARE
    column_def record;
BEGIN
    FOR column_def IN
        SELECT *
        FROM (
            VALUES
                ('id', 'bigint'),
                ('version', 'integer DEFAULT 0'),
                ('mnecode', 'varchar(255)'),
                ('mne_code', 'varchar(255)'),
                ('factorymodel_id', 'bigint'),
                ('factory_model_id', 'bigint'),
                ('factory_model', 'bigint')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.hm_factory_models_mc ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conrelid = 'public.hm_factory_models_mc'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.hm_factory_models_mc
            ADD CONSTRAINT hm_factory_models_mc_pkey PRIMARY KEY (id);
    END IF;
END $$;

UPDATE public.hm_factory_models_mc
   SET factory_model = COALESCE(factory_model, factory_model_id, factorymodel_id),
       factory_model_id = COALESCE(factory_model_id, factory_model, factorymodel_id),
       factorymodel_id = COALESCE(factorymodel_id, factory_model, factory_model_id),
       mne_code = COALESCE(mne_code, mnecode),
       mnecode = COALESCE(mnecode, mne_code)
 WHERE factory_model IS NULL
    OR factory_model_id IS NULL
    OR factorymodel_id IS NULL
    OR mne_code IS NULL
    OR mnecode IS NULL;

CREATE OR REPLACE FUNCTION public.sync_hm_factory_models_mc_aliases()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    resolved_factory_model bigint;
    resolved_mne_code varchar(255);
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.factory_model IS DISTINCT FROM OLD.factory_model THEN
        resolved_factory_model := NEW.factory_model;
    ELSIF TG_OP = 'UPDATE' AND NEW.factory_model_id IS DISTINCT FROM OLD.factory_model_id THEN
        resolved_factory_model := NEW.factory_model_id;
    ELSIF TG_OP = 'UPDATE' AND NEW.factorymodel_id IS DISTINCT FROM OLD.factorymodel_id THEN
        resolved_factory_model := NEW.factorymodel_id;
    ELSE
        resolved_factory_model := COALESCE(
            NEW.factory_model,
            NEW.factory_model_id,
            NEW.factorymodel_id
        );
    END IF;
    NEW.factory_model := resolved_factory_model;
    NEW.factory_model_id := resolved_factory_model;
    NEW.factorymodel_id := resolved_factory_model;

    IF TG_OP = 'UPDATE' AND NEW.mne_code IS DISTINCT FROM OLD.mne_code THEN
        resolved_mne_code := NEW.mne_code;
    ELSIF TG_OP = 'UPDATE' AND NEW.mnecode IS DISTINCT FROM OLD.mnecode THEN
        resolved_mne_code := NEW.mnecode;
    ELSE
        resolved_mne_code := COALESCE(NEW.mne_code, NEW.mnecode);
    END IF;
    NEW.mne_code := resolved_mne_code;
    NEW.mnecode := resolved_mne_code;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_hm_factory_models_mc_aliases
    ON public.hm_factory_models_mc;

CREATE TRIGGER trg_sync_hm_factory_models_mc_aliases
BEFORE INSERT OR UPDATE
ON public.hm_factory_models_mc
FOR EACH ROW
EXECUTE FUNCTION public.sync_hm_factory_models_mc_aliases();

CREATE INDEX IF NOT EXISTS idx_hm_factory_models_mc_factory_model
    ON public.hm_factory_models_mc(factory_model);

CREATE INDEX IF NOT EXISTS idx_hm_factory_models_mc_factory_model_id
    ON public.hm_factory_models_mc(factory_model_id);

CREATE INDEX IF NOT EXISTS idx_hm_factory_models_mc_factorymodel_id
    ON public.hm_factory_models_mc(factorymodel_id);

CREATE INDEX IF NOT EXISTS idx_hm_factory_models_mc_mne_code
    ON public.hm_factory_models_mc(mne_code);
