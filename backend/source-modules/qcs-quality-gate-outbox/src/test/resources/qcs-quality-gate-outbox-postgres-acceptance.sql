\set ON_ERROR_STOP on

INSERT INTO public.baseset_materials (id, code) VALUES (101, 'SUGAR-FG-001');
INSERT INTO public.rm_formulas (id, formual_code, formula_edtion)
VALUES (201, 'EVAP', '1.0');

INSERT INTO public.wom_bpi_production_context_bindings
    (wom_cid, wom_line_id, tenant_id, plant_id, line_id, enabled)
VALUES
    (1000, 7001, '1000', 'PLANT-01', 'LINE-S07-01', TRUE);

INSERT INTO public.wom_produce_tasks
    (id, valid, cid, line_id, table_no, produce_batch_num, product_id,
     formula_id, task_run_state, act_start_time, modify_time)
VALUES
    (3001, TRUE, 1000, 7001, 'ADP_E2E_QCS_OUTBOX_ORDER',
     'ADP_E2E_QCS_OUTBOX_BATCH', 101, 201, 'finished', now() - interval '1 hour', now());

INSERT INTO public.qcs_inspects (id, valid, source_id, source_type, batch_code)
VALUES (4001, TRUE, 3001, 'QCS_sourceType/womComplete', 'ADP_E2E_QCS_OUTBOX_BATCH');
INSERT INTO public.qcs_inspect_reports
    (id, version, create_time, modify_time, valid, status, inspect_id, check_result)
VALUES (5001, 1, now() - interval '10 minutes', now(), TRUE, 88, 4001, NULL);
INSERT INTO public.qcs_report_coms
    (id, valid, report_id, procedure_no, std_ver_com, check_result)
VALUES
    (6001, TRUE, 5001, 'POL', 8001, '合格'),
    (6002, TRUE, 5001, 'MOISTURE', 8002, 'LIMSBasic_standardGrade/Qualified');

UPDATE public.qcs_inspect_reports
   SET status = 99, version = 2, check_result = '合格', modify_time = now()
 WHERE id = 5001;

DO $$
DECLARE
    v_row public.qcs_bpi_quality_gate_outbox%ROWTYPE;
BEGIN
    SELECT * INTO STRICT v_row
      FROM public.qcs_bpi_quality_gate_outbox
     WHERE qcs_report_id = 5001 AND qcs_report_version = 2;
    IF v_row.publication_state <> 'READY'
       OR v_row.tenant_id <> '1000'
       OR v_row.plant_id <> 'PLANT-01'
       OR v_row.line_id <> 'LINE-S07-01'
       OR v_row.source_order_id <> 'ADP_E2E_QCS_OUTBOX_ORDER'
       OR v_row.quality_gate_id <> 'qcs-inspect:4001'
       OR v_row.quality_gate_revision <> 2
       OR jsonb_array_length(v_row.inspections) <> 2
       OR v_row.inspections #>> '{0,disposition}' <> 'ACCEPTED'
       OR v_row.inspections #>> '{1,disposition}' <> 'ACCEPTED' THEN
        RAISE EXCEPTION 'accepted report snapshot mismatch: %', row_to_json(v_row);
    END IF;
END;
$$;

SELECT public.qcs_bpi_enqueue_quality_gate(5001);
DO $$
BEGIN
    IF (SELECT count(*) FROM public.qcs_bpi_quality_gate_outbox
         WHERE qcs_report_id = 5001 AND qcs_report_version = 2) <> 1 THEN
        RAISE EXCEPTION 'same report revision must remain idempotent';
    END IF;
END;
$$;

INSERT INTO public.qcs_inspects (id, valid, source_id, source_type, batch_code)
VALUES (4004, TRUE, 3001, 'QCS_sourceType/womComplete', 'FINAL-FIRST');
INSERT INTO public.qcs_inspect_reports
    (id, version, create_time, modify_time, valid, status, inspect_id, check_result)
VALUES (5004, 1, now(), now(), TRUE, 88, 4004, NULL);
INSERT INTO public.qcs_report_coms
    (id, valid, report_id, procedure_no, std_ver_com, check_result)
VALUES (6004, TRUE, 5004, 'POL', 8004, NULL);

BEGIN;
UPDATE public.qcs_inspect_reports
   SET status = 99, version = 2, check_result = '合格', modify_time = now()
 WHERE id = 5004;
UPDATE public.qcs_report_coms SET check_result = '合格' WHERE id = 6004;
COMMIT;

DO $$
BEGIN
    IF (SELECT publication_state FROM public.qcs_bpi_quality_gate_outbox
         WHERE qcs_report_id = 5004 AND qcs_report_version = 2) <> 'READY' THEN
        RAISE EXCEPTION 'deferred capture must see component writes after report finalization';
    END IF;
END;
$$;

UPDATE public.qcs_report_coms SET check_result = '不合格' WHERE id = 6002;
UPDATE public.qcs_inspect_reports
   SET version = 3, check_result = '不合格', modify_time = now()
 WHERE id = 5001;

DO $$
DECLARE
    v_row public.qcs_bpi_quality_gate_outbox%ROWTYPE;
BEGIN
    SELECT * INTO STRICT v_row
      FROM public.qcs_bpi_quality_gate_outbox
     WHERE qcs_report_id = 5001 AND qcs_report_version = 3;
    IF v_row.publication_state <> 'READY'
       OR v_row.inspections #>> '{1,disposition}' <> 'REJECTED' THEN
        RAISE EXCEPTION 'rejected report snapshot mismatch: %', row_to_json(v_row);
    END IF;
END;
$$;

INSERT INTO public.qcs_inspects (id, valid, source_id, source_type, batch_code)
VALUES (4002, TRUE, 9999, 'QCS_sourceType/womComplete', 'UNMAPPED');
INSERT INTO public.qcs_inspect_reports
    (id, version, create_time, modify_time, valid, status, inspect_id, check_result)
VALUES (5002, 1, now(), now(), TRUE, 88, 4002, NULL);
INSERT INTO public.qcs_report_coms
    (id, valid, report_id, procedure_no, std_ver_com, check_result)
VALUES (6003, TRUE, 5002, 'POL', 8003, '合格');
UPDATE public.qcs_inspect_reports SET status = 99, version = 2, check_result = '合格' WHERE id = 5002;

DO $$
BEGIN
    IF (SELECT publication_state FROM public.qcs_bpi_quality_gate_outbox
         WHERE qcs_report_id = 5002 AND qcs_report_version = 2) <> 'BLOCKED_MAPPING' THEN
        RAISE EXCEPTION 'unmapped QCS report must fail closed';
    END IF;
END;
$$;

INSERT INTO public.qcs_inspects (id, valid, source_id, source_type, batch_code)
VALUES (4005, TRUE, 3001, 'QCS_sourceType/manuComplete', 'WRONG-SOURCE-TYPE');
INSERT INTO public.qcs_inspect_reports
    (id, version, create_time, modify_time, valid, status, inspect_id, check_result)
VALUES (5005, 1, now(), now(), TRUE, 88, 4005, NULL);
INSERT INTO public.qcs_report_coms
    (id, valid, report_id, procedure_no, std_ver_com, check_result)
VALUES (6005, TRUE, 5005, 'POL', 8005, '合格');
UPDATE public.qcs_inspect_reports SET status = 99, version = 2, check_result = '合格' WHERE id = 5005;

DO $$
BEGIN
    IF (SELECT publication_state FROM public.qcs_bpi_quality_gate_outbox
         WHERE qcs_report_id = 5005 AND qcs_report_version = 2) <> 'BLOCKED_MAPPING' THEN
        RAISE EXCEPTION 'non-WOM QCS source type must fail closed';
    END IF;
END;
$$;

INSERT INTO public.qcs_inspects (id, valid, source_id, source_type, batch_code)
VALUES (4003, TRUE, 3001, 'QCS_sourceType/womComplete', 'NO-COMPONENT');
INSERT INTO public.qcs_inspect_reports
    (id, version, create_time, modify_time, valid, status, inspect_id, check_result)
VALUES (5003, 1, now(), now(), TRUE, 99, 4003, '合格');

DO $$
BEGIN
    IF (SELECT publication_state FROM public.qcs_bpi_quality_gate_outbox
         WHERE qcs_report_id = 5003 AND qcs_report_version = 1) <> 'BLOCKED_DATA' THEN
        RAISE EXCEPTION 'QCS report without components must fail closed';
    END IF;
END;
$$;

BEGIN;
UPDATE public.qcs_report_coms SET check_result = '合格' WHERE id = 6002;
UPDATE public.qcs_inspect_reports
   SET version = 4, check_result = '合格', modify_time = now()
 WHERE id = 5001;
ROLLBACK;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.qcs_bpi_quality_gate_outbox
                WHERE qcs_report_id = 5001 AND qcs_report_version = 4) THEN
        RAISE EXCEPTION 'rolled-back QCS report must not leave an outbox row';
    END IF;
END;
$$;

BEGIN;
INSERT INTO public.qcs_inspect_reports
    (id, version, create_time, modify_time, valid, status, inspect_id, check_result)
VALUES (5006, 1, now(), now(), TRUE, 99, 4001, '合格');
INSERT INTO public.qcs_report_coms
    (id, valid, report_id, procedure_no, std_ver_com, check_result)
VALUES (6006, TRUE, 5006, 'POL-RECHECK', 8006, '合格');
COMMIT;

DO $$
BEGIN
    IF (SELECT publication_state FROM public.qcs_bpi_quality_gate_outbox
         WHERE qcs_report_id = 5006 AND qcs_report_version = 1) <> 'BLOCKED_DATA' THEN
        RAISE EXCEPTION 'multiple final reports for one inspection must fail closed';
    END IF;
    IF (SELECT count(*) FROM public.qcs_bpi_quality_gate_outbox
         WHERE publication_state = 'READY') <> 3 THEN
        RAISE EXCEPTION 'expected exactly three publishable snapshots';
    END IF;
END;
$$;

SELECT 'QCS quality-gate PostgreSQL outbox acceptance: PASS' AS result;
