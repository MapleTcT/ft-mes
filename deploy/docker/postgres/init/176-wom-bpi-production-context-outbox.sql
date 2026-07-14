-- WOM production context bridge for BPI.
-- The trigger and WOM write participate in the same PostgreSQL transaction.
-- No state or line mapping is seeded: production semantics must be confirmed per site.

CREATE TABLE IF NOT EXISTS public.wom_bpi_production_context_bindings (
    id BIGSERIAL PRIMARY KEY,
    wom_cid BIGINT NOT NULL,
    wom_line_id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    plant_id VARCHAR(64) NOT NULL,
    line_id VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wom_bpi_context_binding UNIQUE (wom_cid, wom_line_id),
    CONSTRAINT uk_wom_bpi_context_scope UNIQUE (tenant_id, plant_id, line_id),
    CONSTRAINT ck_wom_bpi_context_binding_values CHECK (
        btrim(tenant_id) <> '' AND btrim(plant_id) <> '' AND btrim(line_id) <> ''
    )
);

CREATE TABLE IF NOT EXISTS public.wom_bpi_task_state_mappings (
    wom_state_code VARCHAR(128) PRIMARY KEY,
    active BOOLEAN NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wom_bpi_task_state_code CHECK (btrim(wom_state_code) <> '')
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wom_bpi_task_state_normalized
    ON public.wom_bpi_task_state_mappings (lower(btrim(wom_state_code)));

CREATE TABLE IF NOT EXISTS public.wom_bpi_context_revisions (
    tenant_id VARCHAR(64) NOT NULL,
    plant_id VARCHAR(64) NOT NULL,
    line_id VARCHAR(64) NOT NULL,
    last_revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, plant_id, line_id),
    CONSTRAINT ck_wom_bpi_context_revision_positive CHECK (last_revision >= 0)
);

CREATE TABLE IF NOT EXISTS public.wom_bpi_production_context_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(256),
    topic VARCHAR(128) NOT NULL DEFAULT 'mes.production.context.v1',
    wom_task_id BIGINT NOT NULL,
    wom_cid BIGINT,
    wom_line_id BIGINT,
    tenant_id VARCHAR(64),
    plant_id VARCHAR(64),
    line_id VARCHAR(64),
    order_id VARCHAR(255),
    task_id VARCHAR(64) NOT NULL,
    material_code VARCHAR(200),
    recipe_version VARCHAR(512),
    batch_id VARCHAR(255),
    source_state VARCHAR(255),
    context_revision BIGINT,
    active BOOLEAN,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    publication_state VARCHAR(32) NOT NULL,
    block_reason VARCHAR(1000),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claimed_by VARCHAR(128),
    last_error VARCHAR(2000),
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wom_bpi_context_event UNIQUE (event_id),
    CONSTRAINT ck_wom_bpi_context_outbox_state CHECK (publication_state IN (
        'READY', 'SENDING', 'RETRY', 'SENT', 'DEAD',
        'BLOCKED_MAPPING', 'BLOCKED_STATE', 'BLOCKED_DATA'
    )),
    CONSTRAINT ck_wom_bpi_context_outbox_attempt CHECK (attempt_count >= 0),
    CONSTRAINT ck_wom_bpi_context_ready_data CHECK (
        publication_state NOT IN ('READY', 'SENDING', 'RETRY', 'SENT', 'DEAD')
        OR (
            event_id IS NOT NULL
            AND tenant_id IS NOT NULL
            AND plant_id IS NOT NULL
            AND line_id IS NOT NULL
            AND context_revision > 0
            AND active IS NOT NULL
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_wom_bpi_context_outbox_dispatch
    ON public.wom_bpi_production_context_outbox (publication_state, next_attempt_at, id);
CREATE INDEX IF NOT EXISTS idx_wom_bpi_context_outbox_task
    ON public.wom_bpi_production_context_outbox (wom_task_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_wom_bpi_context_outbox_scope
    ON public.wom_bpi_production_context_outbox (tenant_id, plant_id, line_id, context_revision DESC);

CREATE OR REPLACE FUNCTION public.wom_bpi_capture_production_context()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_binding public.wom_bpi_production_context_bindings%ROWTYPE;
    v_state public.wom_bpi_task_state_mappings%ROWTYPE;
    v_material_code VARCHAR(200);
    v_recipe_version VARCHAR(512);
    v_source_state VARCHAR(255);
    v_active BOOLEAN;
    v_revision BIGINT;
    v_event_id VARCHAR(256);
    v_publication_state VARCHAR(32);
    v_block_reason VARCHAR(1000);
    v_effective_from TIMESTAMP WITH TIME ZONE := clock_timestamp();
BEGIN
    v_source_state := lower(btrim(COALESCE(NEW.task_run_state, '')));

    SELECT binding.*
      INTO v_binding
      FROM public.wom_bpi_production_context_bindings binding
     WHERE binding.wom_cid = NEW.cid
       AND binding.wom_line_id = NEW.line_id
       AND binding.enabled = TRUE;

    IF NOT FOUND THEN
        v_publication_state := 'BLOCKED_MAPPING';
        v_block_reason := 'No enabled BPI scope binding for WOM cid=' || COALESCE(NEW.cid::text, 'null')
            || ', line_id=' || COALESCE(NEW.line_id::text, 'null');
    ELSE
        IF NEW.valid IS FALSE THEN
            v_active := FALSE;
        ELSE
            SELECT mapping.*
              INTO v_state
              FROM public.wom_bpi_task_state_mappings mapping
             WHERE lower(btrim(mapping.wom_state_code)) = v_source_state
               AND mapping.enabled = TRUE;

            IF NOT FOUND THEN
                v_publication_state := 'BLOCKED_STATE';
                v_block_reason := 'No enabled BPI state mapping for WOM task_run_state=' ||
                    COALESCE(NULLIF(v_source_state, ''), 'null');
            ELSE
                v_active := v_state.active;
            END IF;
        END IF;
    END IF;

    IF v_binding.id IS NOT NULL THEN
        SELECT NULLIF(btrim(material.code), '')
          INTO v_material_code
          FROM public.baseset_materials material
         WHERE material.id = NEW.product_id;

        SELECT NULLIF(btrim(concat_ws(':',
                   NULLIF(btrim(formula.formual_code), ''),
                   NULLIF(btrim(formula.formula_edtion), '')
               )), '')
          INTO v_recipe_version
          FROM public.rm_formulas formula
         WHERE formula.id = NEW.formula_id;
    END IF;

    IF v_publication_state IS NULL AND v_active IS TRUE AND (
        NULLIF(btrim(COALESCE(NEW.table_no, '')), '') IS NULL
        AND NULLIF(btrim(COALESCE(NEW.produce_batch_num, '')), '') IS NULL
    ) THEN
        v_publication_state := 'BLOCKED_DATA';
        v_block_reason := 'Active context requires WOM table_no or produce_batch_num';
    END IF;

    IF v_publication_state IS NULL AND v_active IS TRUE AND v_material_code IS NULL THEN
        v_publication_state := 'BLOCKED_DATA';
        v_block_reason := 'Active context requires a resolvable material code';
    END IF;

    IF v_publication_state IS NULL AND v_active IS TRUE AND v_recipe_version IS NULL THEN
        v_publication_state := 'BLOCKED_DATA';
        v_block_reason := 'Active context requires a resolvable recipe code/version';
    END IF;

    IF v_publication_state IS NULL THEN
        INSERT INTO public.wom_bpi_context_revisions (
            tenant_id, plant_id, line_id, last_revision, updated_at
        ) VALUES (
            v_binding.tenant_id, v_binding.plant_id, v_binding.line_id, 1, CURRENT_TIMESTAMP
        )
        ON CONFLICT (tenant_id, plant_id, line_id)
        DO UPDATE SET
            last_revision = public.wom_bpi_context_revisions.last_revision + 1,
            updated_at = CURRENT_TIMESTAMP
        RETURNING last_revision INTO v_revision;

        v_event_id := 'wom-context:' || v_binding.tenant_id || ':' || v_binding.plant_id || ':'
            || v_binding.line_id || ':' || v_revision::text;
        v_publication_state := 'READY';
    END IF;

    INSERT INTO public.wom_bpi_production_context_outbox (
        event_id, wom_task_id, wom_cid, wom_line_id,
        tenant_id, plant_id, line_id, order_id, task_id,
        material_code, recipe_version, batch_id, source_state,
        context_revision, active, effective_from, publication_state, block_reason
    ) VALUES (
        v_event_id, NEW.id, NEW.cid, NEW.line_id,
        v_binding.tenant_id, v_binding.plant_id, v_binding.line_id,
        NULLIF(btrim(COALESCE(NEW.table_no, '')), ''), NEW.id::text,
        v_material_code, v_recipe_version,
        NULLIF(btrim(COALESCE(NEW.produce_batch_num, '')), ''),
        CASE WHEN NEW.valid IS FALSE THEN 'invalidated' ELSE NULLIF(v_source_state, '') END,
        v_revision, v_active, v_effective_from, v_publication_state, v_block_reason
    );

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_wom_bpi_production_context ON public.wom_produce_tasks;
CREATE TRIGGER trg_wom_bpi_production_context
AFTER INSERT OR UPDATE OF
    valid, cid, line_id, table_no, produce_batch_num, product_id, formula_id,
    task_run_state, act_start_time, act_end_time, modify_time
ON public.wom_produce_tasks
FOR EACH ROW
EXECUTE FUNCTION public.wom_bpi_capture_production_context();

COMMENT ON TABLE public.wom_bpi_production_context_bindings IS
    'Explicit WOM cid/line to BPI tenant/plant/line bindings; disabled by default.';
COMMENT ON TABLE public.wom_bpi_task_state_mappings IS
    'Explicit WOM task state semantics for BPI production context; no implicit guesses.';
COMMENT ON TABLE public.wom_bpi_production_context_outbox IS
    'Transactional outbox snapshots for mes.production.context.v1; BLOCKED rows are operator-visible and never published.';
