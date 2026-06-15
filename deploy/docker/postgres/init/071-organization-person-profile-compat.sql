-- Organization person profile fields added by later vendor schema revisions.
-- The recovered base org_person table can be created from an older baseline,
-- while orgmanagement 1.0.0.RELEASE mappers always select these columns.

ALTER TABLE public.org_person ADD COLUMN IF NOT EXISTS entry_date VARCHAR(64);
ALTER TABLE public.org_person ADD COLUMN IF NOT EXISTS title VARCHAR(64);
ALTER TABLE public.org_person ADD COLUMN IF NOT EXISTS qualification VARCHAR(2048);
ALTER TABLE public.org_person ADD COLUMN IF NOT EXISTS education VARCHAR(64);
ALTER TABLE public.org_person ADD COLUMN IF NOT EXISTS major VARCHAR(256);
ALTER TABLE public.org_person ADD COLUMN IF NOT EXISTS id_number VARCHAR(256);
ALTER TABLE public.org_person ADD COLUMN IF NOT EXISTS sign_pic_url VARCHAR(2048);

CREATE INDEX IF NOT EXISTS id_number_index
  ON public.org_person(id_number, valid);

CREATE INDEX IF NOT EXISTS code_index
  ON public.org_person(code, valid);

CREATE INDEX IF NOT EXISTS position_id_index
  ON public.org_person_position(position_id);
