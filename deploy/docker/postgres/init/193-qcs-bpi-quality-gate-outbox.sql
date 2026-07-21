-- Transactional QCS quality-gate outbox for BPI.
-- A final QCS report and its required component results are frozen in the same
-- PostgreSQL transaction. Publication remains disabled until the sidecar and
-- BPI Phase 2 scope allowlists are explicitly enabled.

CREATE TABLE IF NOT EXISTS public.qcs_bpi_quality_gate_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(256),
    idempotency_key VARCHAR(256),
    topic VARCHAR(128) NOT NULL DEFAULT 'qcs.batch.quality-gate.v1',
    qcs_report_id BIGINT NOT NULL,
    qcs_report_version INTEGER NOT NULL,
    qcs_inspect_id BIGINT,
    wom_task_id BIGINT,
    wom_cid BIGINT,
    wom_line_id BIGINT,
    tenant_id VARCHAR(64),
    plant_id VARCHAR(64),
    line_id VARCHAR(64),
    source_order_id VARCHAR(255),
    source_batch_code VARCHAR(255),
    quality_gate_id VARCHAR(256),
    quality_gate_revision BIGINT,
    observed_at TIMESTAMP WITH TIME ZONE,
    inspections JSONB NOT NULL DEFAULT '[]'::jsonb,
    publication_state VARCHAR(32) NOT NULL,
    block_reason VARCHAR(1000),
    resolved_batch_id UUID,
    payload_sha256 CHAR(64),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claimed_by VARCHAR(128),
    last_error VARCHAR(2000),
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_qcs_bpi_gate_report_revision UNIQUE (qcs_report_id, qcs_report_version),
    CONSTRAINT uk_qcs_bpi_gate_event UNIQUE (event_id),
    CONSTRAINT ck_qcs_bpi_gate_report_revision CHECK (qcs_report_version > 0),
    CONSTRAINT ck_qcs_bpi_gate_revision CHECK (
        quality_gate_revision IS NULL OR quality_gate_revision > 0
    ),
    CONSTRAINT ck_qcs_bpi_gate_state CHECK (publication_state IN (
        'READY', 'SENDING', 'RETRY', 'SENT', 'DEAD',
        'BLOCKED_MAPPING', 'BLOCKED_STATE', 'BLOCKED_DATA'
    )),
    CONSTRAINT ck_qcs_bpi_gate_attempt CHECK (attempt_count >= 0),
    CONSTRAINT ck_qcs_bpi_gate_ready_data CHECK (
        publication_state NOT IN ('READY', 'SENDING', 'RETRY', 'SENT', 'DEAD')
        OR (
            event_id IS NOT NULL
            AND idempotency_key IS NOT NULL
            AND qcs_inspect_id IS NOT NULL
            AND wom_task_id IS NOT NULL
            AND tenant_id IS NOT NULL
            AND plant_id IS NOT NULL
            AND line_id IS NOT NULL
            AND source_order_id IS NOT NULL
            AND quality_gate_id IS NOT NULL
            AND quality_gate_revision > 0
            AND observed_at IS NOT NULL
            AND jsonb_typeof(inspections) = 'array'
            AND jsonb_array_length(inspections) > 0
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_qcs_bpi_gate_dispatch
    ON public.qcs_bpi_quality_gate_outbox (publication_state, next_attempt_at, id);
CREATE INDEX IF NOT EXISTS idx_qcs_bpi_gate_source
    ON public.qcs_bpi_quality_gate_outbox (qcs_report_id, qcs_report_version DESC);
CREATE INDEX IF NOT EXISTS idx_qcs_bpi_gate_scope_order
    ON public.qcs_bpi_quality_gate_outbox
       (tenant_id, plant_id, line_id, source_order_id, id DESC);

CREATE OR REPLACE FUNCTION public.qcs_bpi_result_disposition(source_result TEXT)
RETURNS VARCHAR(16)
LANGUAGE sql
IMMUTABLE
RETURNS NULL ON NULL INPUT
AS $$
    SELECT CASE
        WHEN lower(btrim(source_result)) IN ('合格', 'qualified', 'accepted')
          OR lower(btrim(source_result)) LIKE '%/qualified'
            THEN 'ACCEPTED'
        WHEN lower(btrim(source_result)) IN ('不合格', 'unqualified', 'rejected')
          OR lower(btrim(source_result)) LIKE '%/unqualified'
            THEN 'REJECTED'
        ELSE NULL
    END;
$$;

CREATE OR REPLACE FUNCTION public.qcs_bpi_enqueue_quality_gate(target_report_id BIGINT)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_report public.qcs_inspect_reports%ROWTYPE;
    v_inspect public.qcs_inspects%ROWTYPE;
    v_task public.wom_produce_tasks%ROWTYPE;
    v_binding public.wom_bpi_production_context_bindings%ROWTYPE;
    v_report_disposition VARCHAR(16);
    v_inspections JSONB := '[]'::jsonb;
    v_component_count INTEGER := 0;
    v_unknown_count INTEGER := 0;
    v_rejected_count INTEGER := 0;
    v_state VARCHAR(32);
    v_reason VARCHAR(1000);
    v_event_id VARCHAR(256);
    v_idempotency_key VARCHAR(256);
    v_gate_id VARCHAR(256);
    v_observed_at TIMESTAMP WITH TIME ZONE;
    v_outbox_id BIGINT;
BEGIN
    SELECT report.*
      INTO v_report
      FROM public.qcs_inspect_reports report
     WHERE report.id = target_report_id
     FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'QCS report % does not exist', target_report_id;
    END IF;

    v_observed_at := COALESCE(
        v_report.effect_time AT TIME ZONE current_setting('TIMEZONE'),
        v_report.modify_time AT TIME ZONE current_setting('TIMEZONE'),
        v_report.create_time AT TIME ZONE current_setting('TIMEZONE'),
        CURRENT_TIMESTAMP
    );
    v_report_disposition := public.qcs_bpi_result_disposition(v_report.check_result);

    IF v_report.valid IS FALSE OR v_report.status IS DISTINCT FROM 99 THEN
        v_state := 'BLOCKED_STATE';
        v_reason := 'QCS report must be valid and effective (status=99)';
    ELSIF v_report.version IS NULL OR v_report.version <= 0 THEN
        v_state := 'BLOCKED_DATA';
        v_reason := 'QCS report version must be positive';
    ELSIF v_report_disposition IS NULL THEN
        v_state := 'BLOCKED_DATA';
        v_reason := 'QCS report result is not a recognized final disposition';
    END IF;

    SELECT inspect.*
      INTO v_inspect
      FROM public.qcs_inspects inspect
     WHERE inspect.id = v_report.inspect_id
       AND inspect.valid IS DISTINCT FROM FALSE;

    IF NOT FOUND AND v_state IS NULL THEN
        v_state := 'BLOCKED_MAPPING';
        v_reason := 'QCS report does not reference a valid inspection request';
    ELSIF lower(btrim(COALESCE(v_inspect.source_type, ''))) <> 'qcs_sourcetype/womcomplete'
       AND v_state IS NULL THEN
        v_state := 'BLOCKED_MAPPING';
        v_reason := 'Only QCS_sourceType/womComplete resolves directly to a WOM production task';
    END IF;

    IF v_inspect.id IS NOT NULL THEN
        IF (
            SELECT count(*)
              FROM public.qcs_inspect_reports sibling
             WHERE sibling.inspect_id = v_inspect.id
               AND sibling.valid IS DISTINCT FROM FALSE
               AND sibling.status = 99
        ) > 1 AND v_state IS NULL THEN
            v_state := 'BLOCKED_DATA';
            v_reason := 'Multiple final QCS reports reference the same inspection request';
        END IF;

        SELECT task.*
          INTO v_task
          FROM public.wom_produce_tasks task
         WHERE task.id = v_inspect.source_id
           AND task.valid IS DISTINCT FROM FALSE;

        IF NOT FOUND AND v_state IS NULL THEN
            v_state := 'BLOCKED_MAPPING';
            v_reason := 'QCS inspection does not resolve to a valid WOM production task';
        END IF;
    END IF;

    IF v_task.id IS NOT NULL THEN
        SELECT binding.*
          INTO v_binding
          FROM public.wom_bpi_production_context_bindings binding
         WHERE binding.wom_cid = v_task.cid
           AND binding.wom_line_id = v_task.line_id
           AND binding.enabled = TRUE;

        IF NOT FOUND AND v_state IS NULL THEN
            v_state := 'BLOCKED_MAPPING';
            v_reason := 'No enabled BPI scope binding for the QCS source WOM task';
        END IF;
    END IF;

    IF v_task.id IS NOT NULL
       AND NULLIF(btrim(COALESCE(v_task.table_no, '')), '') IS NULL
       AND v_state IS NULL THEN
        v_state := 'BLOCKED_DATA';
        v_reason := 'QCS source WOM task requires a stable table_no/order identifier';
    ELSIF v_task.id IS NOT NULL
       AND length(btrim(COALESCE(v_task.table_no, ''))) > 128
       AND v_state IS NULL THEN
        v_state := 'BLOCKED_DATA';
        v_reason := 'QCS source WOM task table_no exceeds the BPI order_id limit of 128 characters';
    END IF;

    SELECT
        COALESCE(jsonb_agg(
            jsonb_build_object(
                'inspectionCode', COALESCE(
                    NULLIF(btrim(component.procedure_no), ''),
                    CASE WHEN component.std_ver_com IS NOT NULL
                        THEN 'STD-' || component.std_ver_com::text END,
                    'REPORT-COM-' || component.id::text
                ),
                'inspectionRecordId', component.id::text,
                'required', TRUE,
                'disposition', public.qcs_bpi_result_disposition(component.check_result),
                'finalResult', public.qcs_bpi_result_disposition(component.check_result) IS NOT NULL,
                'observedAtMs', floor(extract(epoch FROM v_observed_at) * 1000)::BIGINT,
                'sourceResult', component.check_result
            ) ORDER BY component.id
        ), '[]'::jsonb),
        count(*)::INTEGER,
        count(*) FILTER (
            WHERE public.qcs_bpi_result_disposition(component.check_result) IS NULL
        )::INTEGER,
        count(*) FILTER (
            WHERE public.qcs_bpi_result_disposition(component.check_result) = 'REJECTED'
        )::INTEGER
      INTO v_inspections, v_component_count, v_unknown_count, v_rejected_count
      FROM public.qcs_report_coms component
     WHERE component.report_id = v_report.id
       AND component.valid IS DISTINCT FROM FALSE;

    IF v_component_count = 0 AND v_state IS NULL THEN
        v_state := 'BLOCKED_DATA';
        v_reason := 'QCS report has no required inspection component snapshot';
    ELSIF v_unknown_count > 0 AND v_state IS NULL THEN
        v_state := 'BLOCKED_DATA';
        v_reason := 'QCS report contains a component without a recognized final disposition';
    ELSIF v_report_disposition = 'ACCEPTED' AND v_rejected_count > 0 AND v_state IS NULL THEN
        v_state := 'BLOCKED_DATA';
        v_reason := 'Accepted QCS report contains a rejected required component';
    ELSIF v_report_disposition = 'REJECTED' AND v_rejected_count = 0 AND v_state IS NULL THEN
        v_state := 'BLOCKED_DATA';
        v_reason := 'Rejected QCS report has no rejected required component';
    END IF;

    IF v_state IS NULL THEN
        v_state := 'READY';
        v_gate_id := 'qcs-inspect:' || v_inspect.id::text;
        v_event_id := 'qcs-gate:' || v_binding.tenant_id || ':' || v_inspect.id::text
            || ':' || v_report.version::text;
        v_idempotency_key := v_event_id;
    ELSIF v_inspect.id IS NOT NULL THEN
        v_gate_id := 'qcs-inspect:' || v_inspect.id::text;
    END IF;

    INSERT INTO public.qcs_bpi_quality_gate_outbox (
        event_id, idempotency_key, qcs_report_id, qcs_report_version,
        qcs_inspect_id, wom_task_id, wom_cid, wom_line_id,
        tenant_id, plant_id, line_id, source_order_id, source_batch_code,
        quality_gate_id, quality_gate_revision, observed_at, inspections,
        publication_state, block_reason, attempt_count, next_attempt_at,
        claimed_at, claimed_by, last_error, resolved_batch_id, payload_sha256,
        sent_at, updated_at
    ) VALUES (
        v_event_id, v_idempotency_key, v_report.id, v_report.version,
        v_inspect.id, v_task.id, v_task.cid, v_task.line_id,
        v_binding.tenant_id, v_binding.plant_id, v_binding.line_id,
        NULLIF(btrim(COALESCE(v_task.table_no, '')), ''), v_inspect.batch_code,
        v_gate_id, v_report.version, v_observed_at, v_inspections,
        v_state, v_reason, 0, CURRENT_TIMESTAMP,
        NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP
    )
    ON CONFLICT (qcs_report_id, qcs_report_version)
    DO UPDATE SET
        event_id = EXCLUDED.event_id,
        idempotency_key = EXCLUDED.idempotency_key,
        qcs_inspect_id = EXCLUDED.qcs_inspect_id,
        wom_task_id = EXCLUDED.wom_task_id,
        wom_cid = EXCLUDED.wom_cid,
        wom_line_id = EXCLUDED.wom_line_id,
        tenant_id = EXCLUDED.tenant_id,
        plant_id = EXCLUDED.plant_id,
        line_id = EXCLUDED.line_id,
        source_order_id = EXCLUDED.source_order_id,
        source_batch_code = EXCLUDED.source_batch_code,
        quality_gate_id = EXCLUDED.quality_gate_id,
        quality_gate_revision = EXCLUDED.quality_gate_revision,
        observed_at = EXCLUDED.observed_at,
        inspections = EXCLUDED.inspections,
        publication_state = EXCLUDED.publication_state,
        block_reason = EXCLUDED.block_reason,
        attempt_count = 0,
        next_attempt_at = CURRENT_TIMESTAMP,
        claimed_at = NULL,
        claimed_by = NULL,
        last_error = NULL,
        resolved_batch_id = NULL,
        payload_sha256 = NULL,
        sent_at = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE public.qcs_bpi_quality_gate_outbox.publication_state IN (
        'BLOCKED_MAPPING', 'BLOCKED_STATE', 'BLOCKED_DATA'
    )
    RETURNING id INTO v_outbox_id;

    IF v_outbox_id IS NULL THEN
        SELECT outbox.id
          INTO v_outbox_id
          FROM public.qcs_bpi_quality_gate_outbox outbox
         WHERE outbox.qcs_report_id = v_report.id
           AND outbox.qcs_report_version = v_report.version;
    END IF;

    RETURN v_outbox_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.qcs_bpi_capture_quality_gate()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status = 99 AND NEW.valid IS DISTINCT FROM FALSE THEN
        IF TG_OP = 'INSERT'
           OR OLD.status IS DISTINCT FROM NEW.status
           OR OLD.valid IS DISTINCT FROM NEW.valid
           OR OLD.version IS DISTINCT FROM NEW.version
           OR OLD.check_result IS DISTINCT FROM NEW.check_result THEN
            PERFORM public.qcs_bpi_enqueue_quality_gate(NEW.id);
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_qcs_bpi_quality_gate ON public.qcs_inspect_reports;
CREATE CONSTRAINT TRIGGER trg_qcs_bpi_quality_gate
AFTER INSERT OR UPDATE
ON public.qcs_inspect_reports
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION public.qcs_bpi_capture_quality_gate();

CREATE OR REPLACE FUNCTION public.qcs_bpi_recapture_quality_gate_component()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_report_id BIGINT;
BEGIN
    v_report_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.report_id ELSE NEW.report_id END;
    IF EXISTS (
        SELECT 1
          FROM public.qcs_inspect_reports report
         WHERE report.id = v_report_id
           AND report.status = 99
           AND report.valid IS DISTINCT FROM FALSE
    ) THEN
        PERFORM public.qcs_bpi_enqueue_quality_gate(v_report_id);
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS trg_qcs_bpi_quality_gate_component ON public.qcs_report_coms;
CREATE CONSTRAINT TRIGGER trg_qcs_bpi_quality_gate_component
AFTER INSERT OR UPDATE OR DELETE
ON public.qcs_report_coms
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION public.qcs_bpi_recapture_quality_gate_component();

COMMENT ON TABLE public.qcs_bpi_quality_gate_outbox IS
    'Immutable final QCS report snapshots awaiting canonical BPI quality-gate publication.';
COMMENT ON FUNCTION public.qcs_bpi_enqueue_quality_gate(BIGINT) IS
    'Idempotently captures one immutable QCS report revision; ambiguous multi-report inspections fail closed and only blocked snapshots may recover in place.';
COMMENT ON FUNCTION public.qcs_bpi_recapture_quality_gate_component() IS
    'Recaptures a final report after all component writes in the source transaction are visible.';
