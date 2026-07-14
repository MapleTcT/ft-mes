INSERT INTO public.baseset_materials (id, code) VALUES (501, 'MAT-ADP-E2E');
INSERT INTO public.rm_formulas (id, formual_code, formula_edtion)
VALUES (601, 'FORMULA-ADP-E2E', 'V3');

INSERT INTO public.wom_produce_tasks (
    id, valid, cid, line_id, table_no, produce_batch_num,
    product_id, formula_id, task_run_state, modify_time
) VALUES (
    701, true, 1000, 77, 'MO-ADP-E2E-CONTEXT', 'BATCH-ADP-E2E-CONTEXT',
    501, 601, 'WOM_runState/runing', CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM public.wom_bpi_production_context_outbox
         WHERE wom_task_id = 701
           AND publication_state = 'BLOCKED_MAPPING'
    ) THEN
        RAISE EXCEPTION 'missing fail-closed BLOCKED_MAPPING snapshot';
    END IF;
END;
$$;

INSERT INTO public.wom_bpi_production_context_bindings (
    wom_cid, wom_line_id, tenant_id, plant_id, line_id, enabled
) VALUES (1000, 77, '1000', 'PLANT-ADP-E2E', 'LINE-ADP-E2E', true);

INSERT INTO public.wom_bpi_task_state_mappings (
    wom_state_code, active, enabled, description
) VALUES
    ('wom_runstate/runing', true, true, 'PostgreSQL acceptance active state'),
    ('wom_runstate/finished', false, true, 'PostgreSQL acceptance inactive state');

UPDATE public.wom_produce_tasks
   SET modify_time = clock_timestamp()
 WHERE id = 701;

DO $$
DECLARE
    v_row public.wom_bpi_production_context_outbox%ROWTYPE;
BEGIN
    SELECT *
      INTO v_row
      FROM public.wom_bpi_production_context_outbox
     WHERE wom_task_id = 701
       AND publication_state = 'READY'
     ORDER BY id DESC
     LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'missing READY production context';
    END IF;
    IF v_row.event_id <> 'wom-context:1000:PLANT-ADP-E2E:LINE-ADP-E2E:' || v_row.context_revision::text
       OR v_row.context_revision < 1000000000000
       OR v_row.active IS DISTINCT FROM TRUE
       OR v_row.order_id <> 'MO-ADP-E2E-CONTEXT'
       OR v_row.batch_id <> 'BATCH-ADP-E2E-CONTEXT'
       OR v_row.material_code <> 'MAT-ADP-E2E'
       OR v_row.recipe_version <> 'FORMULA-ADP-E2E:V3' THEN
        RAISE EXCEPTION 'READY snapshot fields are incomplete: %', row_to_json(v_row);
    END IF;
END;
$$;

UPDATE public.wom_produce_tasks
   SET task_run_state = 'WOM_runState/finished',
       modify_time = clock_timestamp()
 WHERE id = 701;

DO $$
DECLARE
    v_row public.wom_bpi_production_context_outbox%ROWTYPE;
    v_previous_revision BIGINT;
BEGIN
    SELECT context_revision
      INTO v_previous_revision
      FROM public.wom_bpi_production_context_outbox
     WHERE wom_task_id = 701
       AND publication_state = 'READY'
       AND active IS TRUE
     ORDER BY id DESC
     LIMIT 1;

    SELECT *
      INTO v_row
      FROM public.wom_bpi_production_context_outbox
     WHERE wom_task_id = 701
       AND publication_state = 'READY'
     ORDER BY id DESC
     LIMIT 1;

    IF v_row.context_revision <= v_previous_revision
       OR v_row.active IS DISTINCT FROM FALSE THEN
        RAISE EXCEPTION 'inactive context did not advance line revision: %', row_to_json(v_row);
    END IF;
END;
$$;

CREATE TEMP TABLE context_outbox_count_before AS
SELECT count(*) AS total
  FROM public.wom_bpi_production_context_outbox
 WHERE wom_task_id = 701;

BEGIN;
UPDATE public.wom_produce_tasks
   SET table_no = 'MO-ADP-E2E-ROLLED-BACK',
       modify_time = clock_timestamp()
 WHERE id = 701;
ROLLBACK;

DO $$
DECLARE
    v_before BIGINT;
    v_after BIGINT;
BEGIN
    SELECT total INTO v_before FROM context_outbox_count_before;
    SELECT count(*) INTO v_after
      FROM public.wom_bpi_production_context_outbox
     WHERE wom_task_id = 701;

    IF v_before <> v_after OR EXISTS (
        SELECT 1
          FROM public.wom_bpi_production_context_outbox
         WHERE order_id = 'MO-ADP-E2E-ROLLED-BACK'
    ) THEN
        RAISE EXCEPTION 'WOM rollback left an outbox snapshot';
    END IF;
END;
$$;

SELECT publication_state, count(*)
  FROM public.wom_bpi_production_context_outbox
 GROUP BY publication_state
 ORDER BY publication_state;
