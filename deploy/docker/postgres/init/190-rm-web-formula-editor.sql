-- Sustainable Web formula editing and auditable Batch/DCS delivery boundary.
--
-- The legacy RM editor depends on an IE ActiveX control and localhost:4433.
-- These tables preserve optimistic revisions and every delivery attempt while
-- the existing RM_FORMULAS / RM_FORMULA_PROCESSES / RM_PROCESS_ACTIVES remain
-- the business source of truth.

CREATE SEQUENCE IF NOT EXISTS public.rm_web_formula_id_seq
    AS bigint START WITH 9100000000000000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS public.rm_web_formula_process_id_seq
    AS bigint START WITH 9200000000000000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS public.rm_web_process_active_id_seq
    AS bigint START WITH 9300000000000000 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS public.rm_formula_editor_revisions (
    id bigserial PRIMARY KEY,
    tenant_id varchar(64) NOT NULL DEFAULT 'default',
    formula_id bigint NOT NULL,
    request_id varchar(80) NOT NULL,
    request_hash char(64) NOT NULL,
    revision_no integer NOT NULL,
    formula_version integer NOT NULL,
    payload jsonb NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rm_formula_editor_revision_request UNIQUE (tenant_id, request_id),
    CONSTRAINT uq_rm_formula_editor_revision_no UNIQUE (formula_id, revision_no)
);

CREATE INDEX IF NOT EXISTS idx_rm_formula_editor_revision_formula
    ON public.rm_formula_editor_revisions(formula_id, revision_no DESC);

CREATE TABLE IF NOT EXISTS public.rm_formula_deliveries (
    id bigserial PRIMARY KEY,
    tenant_id varchar(64) NOT NULL DEFAULT 'default',
    request_id varchar(80) NOT NULL,
    formula_id bigint NOT NULL,
    revision_id bigint NOT NULL,
    endpoint text NOT NULL DEFAULT '',
    payload jsonb NOT NULL,
    state varchar(32) NOT NULL DEFAULT 'PENDING',
    attempts integer NOT NULL DEFAULT 0,
    http_status integer,
    response_body text,
    error_message text,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at timestamp without time zone,
    CONSTRAINT uq_rm_formula_delivery_request UNIQUE (tenant_id, request_id),
    CONSTRAINT ck_rm_formula_delivery_state CHECK (
        state IN ('PENDING', 'FAILED', 'ACKNOWLEDGED', 'CONFIG_REQUIRED')
    )
);

CREATE INDEX IF NOT EXISTS idx_rm_formula_delivery_formula
    ON public.rm_formula_deliveries(formula_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_rm_formula_delivery_state
    ON public.rm_formula_deliveries(state, updated_at);

CREATE TABLE IF NOT EXISTS public.rm_formula_delivery_attempts (
    id bigserial PRIMARY KEY,
    delivery_id bigint NOT NULL,
    attempt_no integer NOT NULL,
    state varchar(32) NOT NULL,
    http_status integer,
    response_body text,
    error_message text,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rm_formula_delivery_attempt UNIQUE (delivery_id, attempt_no)
);

CREATE INDEX IF NOT EXISTS idx_rm_formula_delivery_attempt_delivery
    ON public.rm_formula_delivery_attempts(delivery_id, attempt_no);

CREATE OR REPLACE FUNCTION public.adp_rm_formula_web_editor_button(target jsonb)
RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    output jsonb;
    entry record;
    buttons jsonb;
    button jsonb := jsonb_build_object(
        'id', 'rmFormulaWebEditor',
        'showname', 'Web编辑',
        'namekey', 'Web编辑',
        'buttonstyle', 'edit',
        'operatetype', 'CUSTOM',
        'operateType', 'CUSTOM',
        'isHide', false,
        'ispermission', false,
        'isPublished', false,
        'buttonoperationcode', 'RM_1.0.0_formula_batchFormulaList_webEditor',
        'funcname', 'onclick=''rmFormulaWebEditor(event)''',
        'funcbody', 'function rmFormulaWebEditor(event) { window.open(''/msService/RM/formula/editor'', ''_blank''); }',
        'funcbody_es5', 'function rmFormulaWebEditor(event) { window.open(''/msService/RM/formula/editor'', ''_blank''); }',
        'iscallback', 'false',
        'iscustomfunc', 'false',
        'useInMore', 'false',
        'isconfirm', false,
        'cellCode', 'cell_rm_formula_web_editor',
        'regionType', 'BUTTON',
        'name', 'Web编辑',
        'onclick', 'rmFormulaWebEditor(event)',
        'ONCLICK', 'rmFormulaWebEditor(event)',
        'CODE', 'RM_1.0.0_formula_batchFormulaList_webEditor',
        'NAME', 'Web编辑',
        'ICONCLS', 'cui-btn-edit',
        'USEINMORE', 'false',
        'SEPARATENUM', '0'
    );
BEGIN
    IF target IS NULL THEN
        RETURN target;
    END IF;
    IF jsonb_typeof(target) = 'array' THEN
        SELECT COALESCE(jsonb_agg(public.adp_rm_formula_web_editor_button(value) ORDER BY ordinality), '[]'::jsonb)
          INTO output
          FROM jsonb_array_elements(target) WITH ORDINALITY AS array_item(value, ordinality);
        RETURN output;
    END IF;
    IF jsonb_typeof(target) <> 'object' THEN
        RETURN target;
    END IF;

    output := '{}'::jsonb;
    FOR entry IN SELECT key, value FROM jsonb_each(target)
    LOOP
        output := output || jsonb_build_object(
            entry.key,
            public.adp_rm_formula_web_editor_button(entry.value)
        );
    END LOOP;

    IF output->>'DataGridCode' = 'RM_1.0.0_formula_batchFormulaList'
       OR output->>'idPrefix' = 'compat_RM_1.0.0_formula_batchFormulaList' THEN
        buttons := CASE
            WHEN jsonb_typeof(output->'buttons') = 'array' THEN output->'buttons'
            ELSE '[]'::jsonb
        END;
        IF NOT EXISTS (
            SELECT 1 FROM jsonb_array_elements(buttons) AS existing
             WHERE existing->>'id' = 'rmFormulaWebEditor'
                OR existing->>'buttonoperationcode' = 'RM_1.0.0_formula_batchFormulaList_webEditor'
        ) THEN
            buttons := buttons || jsonb_build_array(button);
        END IF;
        output := jsonb_set(output, '{buttons}', buttons, true);
    END IF;
    RETURN output;
END $$;

DO $do$
DECLARE
    is_oid boolean;
    target record;
    patched_payload jsonb;
    replacement_oid oid;
BEGIN
    SELECT udt_name = 'oid' INTO is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_extra_view'
       AND column_name = 'view_json';

    IF COALESCE(is_oid, false) THEN
        FOR target IN
            SELECT ctid AS row_id,
                   view_json AS old_oid,
                   convert_from(lo_get(view_json), 'UTF8')::jsonb AS payload
              FROM public.runtime_extra_view
             WHERE (code = 'RM_1.0.0_formula_batchFormulaList'
                    OR view_code = 'RM_1.0.0_formula_batchFormulaList')
               AND convert_from(lo_get(view_json), 'UTF8') ~ '^\s*[\{\[]'
        LOOP
            patched_payload := public.adp_rm_formula_web_editor_button(target.payload);
            IF patched_payload IS DISTINCT FROM target.payload THEN
                replacement_oid := lo_from_bytea(0, convert_to(patched_payload::text, 'UTF8'));
                UPDATE public.runtime_extra_view
                   SET view_json = replacement_oid
                 WHERE ctid = target.row_id;
                PERFORM lo_unlink(target.old_oid);
            END IF;
        END LOOP;
    ELSE
        UPDATE public.runtime_extra_view
           SET view_json = public.adp_rm_formula_web_editor_button(view_json::text::jsonb)::text
         WHERE (code = 'RM_1.0.0_formula_batchFormulaList'
                OR view_code = 'RM_1.0.0_formula_batchFormulaList')
           AND view_json::text ~ '^\s*[\{\[]'
           AND public.adp_rm_formula_web_editor_button(view_json::text::jsonb)
               IS DISTINCT FROM view_json::text::jsonb;
    END IF;
END $do$;

DROP FUNCTION IF EXISTS public.adp_rm_formula_web_editor_button(jsonb);
