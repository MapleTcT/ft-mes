-- PostgreSQL compatibility for Qualify certificate-type mnemonic codes.
--
-- The legacy BAP mnemonic-code service uses the entity property name
-- certificate_type directly, while restored Hibernate schemas may contain
-- certificate_type_id, certificatetype_id, or cer_type. Keep every alias
-- synchronized so the JDBC mnemonic-code path and JPA entity path can coexist.

CREATE TABLE IF NOT EXISTS public.qlf_certificate_types_mc (
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
                ('certificatetype_id', 'bigint'),
                ('certificate_type_id', 'bigint'),
                ('certificate_type', 'bigint'),
                ('cer_type', 'bigint')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.qlf_certificate_types_mc ADD COLUMN IF NOT EXISTS %I %s',
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
         WHERE conrelid = 'public.qlf_certificate_types_mc'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.qlf_certificate_types_mc
            ADD CONSTRAINT qlf_certificate_types_mc_pkey PRIMARY KEY (id);
    END IF;
END $$;

UPDATE public.qlf_certificate_types_mc
   SET certificate_type = COALESCE(
           certificate_type,
           certificate_type_id,
           certificatetype_id,
           cer_type
       ),
       certificate_type_id = COALESCE(
           certificate_type_id,
           certificate_type,
           certificatetype_id,
           cer_type
       ),
       certificatetype_id = COALESCE(
           certificatetype_id,
           certificate_type,
           certificate_type_id,
           cer_type
       ),
       cer_type = COALESCE(
           cer_type,
           certificate_type,
           certificate_type_id,
           certificatetype_id
       ),
       mne_code = COALESCE(mne_code, mnecode),
       mnecode = COALESCE(mnecode, mne_code)
 WHERE certificate_type IS NULL
    OR certificate_type_id IS NULL
    OR certificatetype_id IS NULL
    OR cer_type IS NULL
    OR mne_code IS NULL
    OR mnecode IS NULL;

CREATE OR REPLACE FUNCTION public.sync_qlf_certificate_types_mc_aliases()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    resolved_certificate_type bigint;
    resolved_mne_code varchar(255);
BEGIN
    IF TG_OP = 'UPDATE'
       AND NEW.certificate_type IS DISTINCT FROM OLD.certificate_type THEN
        resolved_certificate_type := NEW.certificate_type;
    ELSIF TG_OP = 'UPDATE'
       AND NEW.certificate_type_id IS DISTINCT FROM OLD.certificate_type_id THEN
        resolved_certificate_type := NEW.certificate_type_id;
    ELSIF TG_OP = 'UPDATE'
       AND NEW.certificatetype_id IS DISTINCT FROM OLD.certificatetype_id THEN
        resolved_certificate_type := NEW.certificatetype_id;
    ELSIF TG_OP = 'UPDATE'
       AND NEW.cer_type IS DISTINCT FROM OLD.cer_type THEN
        resolved_certificate_type := NEW.cer_type;
    ELSE
        resolved_certificate_type := COALESCE(
            NEW.certificate_type,
            NEW.certificate_type_id,
            NEW.certificatetype_id,
            NEW.cer_type
        );
    END IF;

    NEW.certificate_type := resolved_certificate_type;
    NEW.certificate_type_id := resolved_certificate_type;
    NEW.certificatetype_id := resolved_certificate_type;
    NEW.cer_type := resolved_certificate_type;

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

DROP TRIGGER IF EXISTS trg_sync_qlf_certificate_types_mc_aliases
    ON public.qlf_certificate_types_mc;

CREATE TRIGGER trg_sync_qlf_certificate_types_mc_aliases
BEFORE INSERT OR UPDATE
ON public.qlf_certificate_types_mc
FOR EACH ROW
EXECUTE FUNCTION public.sync_qlf_certificate_types_mc_aliases();

CREATE INDEX IF NOT EXISTS idx_qlf_certificate_types_mc_certificate_type
    ON public.qlf_certificate_types_mc(certificate_type);

CREATE INDEX IF NOT EXISTS idx_qlf_certificate_types_mc_certificate_type_id
    ON public.qlf_certificate_types_mc(certificate_type_id);

CREATE INDEX IF NOT EXISTS idx_qlf_certificate_types_mc_certificatetype_id
    ON public.qlf_certificate_types_mc(certificatetype_id);

CREATE INDEX IF NOT EXISTS idx_qlf_certificate_types_mc_cer_type
    ON public.qlf_certificate_types_mc(cer_type);

CREATE INDEX IF NOT EXISTS idx_qlf_certificate_types_mc_mne_code
    ON public.qlf_certificate_types_mc(mne_code);

DO $$
DECLARE
    compatibility_column_count integer;
BEGIN
    SELECT count(*)
      INTO compatibility_column_count
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'qlf_certificate_types_mc'
       AND column_name IN (
           'certificate_type',
           'certificate_type_id',
           'certificatetype_id',
           'cer_type',
           'mne_code',
           'mnecode'
       );

    IF compatibility_column_count <> 6 THEN
        RAISE EXCEPTION
            'Qualify certificate-type mnemonic compatibility failed: expected 6 aliases, got %',
            compatibility_column_count;
    END IF;
END $$;
