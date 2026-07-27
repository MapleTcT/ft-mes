-- Restore the first-use path for WOM formula-scoped production-line references.
--
-- The recovered PostgreSQL dataset can contain manufacturing orders with a
-- line_id while hm_fac_node_types is empty and rm_line_formulas has no matching
-- row. The legacy factoryLineRef condition then filters every row out:
--   NODE_TYPE_ID -> HM_FAC_NODE_TYPES.CODE = '004'
--   LINE_ID      -> RM_LINE_FORMULAS.FORMULA_ID
--
-- Bootstrap the standard production-line node type, infer only relationships
-- already evidenced by WOM orders, and keep future selections synchronized.
-- A formula with no configured line mapping may see all valid production-line
-- nodes once; after the order is saved the trigger records the selected mapping.

CREATE SEQUENCE IF NOT EXISTS public.rm_line_formulas_id_seq
    AS bigint
    START WITH 9100000000000000;

SELECT setval(
    'public.rm_line_formulas_id_seq',
    GREATEST(
        COALESCE((SELECT max(id) FROM public.rm_line_formulas), 0),
        9099999999999999
    ),
    true
);

ALTER TABLE public.rm_line_formulas
    ALTER COLUMN id SET DEFAULT nextval('public.rm_line_formulas_id_seq');

DO $$
DECLARE
    line_node_type_id bigint;
    default_company_id bigint;
BEGIN
    SELECT id
      INTO line_node_type_id
      FROM public.hm_fac_node_types
     WHERE code = '004'
       AND COALESCE(valid, true) IS TRUE
     ORDER BY id
     LIMIT 1;

    IF line_node_type_id IS NULL THEN
        line_node_type_id := nextval('public.rm_line_formulas_id_seq');
        SELECT id
          INTO default_company_id
          FROM public.base_company
         WHERE valid = 1
         ORDER BY id
         LIMIT 1;

        INSERT INTO public.hm_fac_node_types (
            id,
            version,
            create_time,
            valid,
            cid,
            code,
            name,
            table_no
        )
        VALUES (
            line_node_type_id,
            0,
            CURRENT_TIMESTAMP,
            true,
            COALESCE(default_company_id, 1000),
            '004',
            '生产线',
            'ADP_COMPAT_FACTORY_NODE_TYPE_004'
        );
    END IF;

    UPDATE public.hm_factory_models factory_model
       SET node_type_id = line_node_type_id,
           modify_time = COALESCE(factory_model.modify_time, CURRENT_TIMESTAMP)
     WHERE factory_model.node_type_id IS NULL
       AND factory_model.id IN (
            SELECT task.line_id
              FROM public.wom_produce_tasks task
             WHERE task.line_id IS NOT NULL
            UNION
            SELECT line_formula.line_id
              FROM public.rm_line_formulas line_formula
             WHERE line_formula.line_id IS NOT NULL
               AND COALESCE(line_formula.valid, true) IS TRUE
       );
END $$;

INSERT INTO public.rm_line_formulas (
    version,
    create_time,
    valid,
    cid,
    formula_id,
    line_id,
    table_info_id
)
SELECT
    0,
    CURRENT_TIMESTAMP,
    true,
    min(task.cid),
    task.formula_id,
    task.line_id,
    min(task.table_info_id)
  FROM public.wom_produce_tasks task
 WHERE task.formula_id IS NOT NULL
   AND task.line_id IS NOT NULL
   AND COALESCE(task.valid, true) IS TRUE
 GROUP BY task.formula_id, task.line_id
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION public.adp_sync_wom_formula_line_mapping()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    line_node_type_id bigint;
BEGIN
    IF NEW.formula_id IS NULL
       OR NEW.line_id IS NULL
       OR COALESCE(NEW.valid, true) IS NOT TRUE THEN
        RETURN NEW;
    END IF;

    SELECT id
      INTO line_node_type_id
      FROM public.hm_fac_node_types
     WHERE code = '004'
       AND COALESCE(valid, true) IS TRUE
     ORDER BY id
     LIMIT 1;

    IF line_node_type_id IS NOT NULL THEN
        UPDATE public.hm_factory_models
           SET node_type_id = line_node_type_id,
               modify_time = COALESCE(modify_time, CURRENT_TIMESTAMP)
         WHERE id = NEW.line_id
           AND node_type_id IS NULL;
    END IF;

    INSERT INTO public.rm_line_formulas (
        version,
        create_time,
        valid,
        cid,
        formula_id,
        line_id,
        table_info_id
    )
    VALUES (
        0,
        CURRENT_TIMESTAMP,
        true,
        NEW.cid,
        NEW.formula_id,
        NEW.line_id,
        NEW.table_info_id
    )
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS tr_adp_sync_wom_formula_line_mapping
    ON public.wom_produce_tasks;

CREATE TRIGGER tr_adp_sync_wom_formula_line_mapping
AFTER INSERT OR UPDATE OF formula_id, line_id, valid
ON public.wom_produce_tasks
FOR EACH ROW
EXECUTE FUNCTION public.adp_sync_wom_formula_line_mapping();

WITH patched_conditions(view_code, condition_sql) AS (
    VALUES
        (
            'HierarchicalMod_1.0.0_factoryModel_factoryLineRef',
            $condition$
def formulaIdValue = null;
try {
    formulaIdValue = customCondition?.formulaId;
} catch (Throwable ignored) {
    formulaIdValue = null;
}
if (formulaIdValue) {
    return "( \"factoryModel\".VALID = 1 and \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where CODE like '004') and (not exists(select 1 from rm_line_formulas where FORMULA_ID = \${formulaId, Long} and VALID = 1) or \"factoryModel\".ID in(select LINE_ID from rm_line_formulas where FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where CODE like '004'))";
}
$condition$
        ),
        (
            'HierarchicalMod_1.0.0_factoryModel_factoryLineRef2',
            $condition$
def formulaIdValue = null;
try {
    formulaIdValue = customCondition?.formulaId;
} catch (Throwable ignored) {
    formulaIdValue = null;
}
if (formulaIdValue) {
    return "( \"factoryModel\".VALID = 1 and \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where CODE like '004') and (not exists(select 1 from rm_line_formulas where FORMULA_ID = \${formulaId, Long} and VALID = 1) or \"factoryModel\".ID in(select LINE_ID from rm_line_formulas where FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where CODE like '004'))";
}
$condition$
        ),
        (
            'HierarchicalMod_1.0.0_factoryModel_factoryLineRef3',
            $condition$
def formulaIdValue = null;
try {
    formulaIdValue = customCondition?.formulaId;
} catch (Throwable ignored) {
    formulaIdValue = null;
}
if (formulaIdValue) {
    return "( \"factoryModel\".VALID = 1 and \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where CODE like '004') and (not exists(select 1 from rm_line_formulas where FORMULA_ID = \${formulaId, Long} and VALID = 1) or \"factoryModel\".ID in(select LINE_ID from rm_line_formulas where FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where CODE like '004'))";
}
$condition$
        ),
        (
            'HierarchicalMod_1.0.0_factoryModel_factoryLineRef4',
            $condition$
def lineIdValue = null;
try {
    lineIdValue = customCondition?.lineId;
} catch (Throwable ignored) {
    lineIdValue = null;
}
if (lineIdValue) {
    return "( \"factoryModel\".VALID = 1 and \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where CODE like '004') and \"factoryModel\".ID not in(select ID from HM_FACTORY_MODELS where id = \${lineId, Long}))";
} else {
    return "( \"factoryModel\".VALID = 1 and \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where CODE like '004'))";
}
$condition$
        )
)
UPDATE public.runtime_customer_condition target
   SET condition_sql = patched_conditions.condition_sql
  FROM patched_conditions
 WHERE target.view_code = patched_conditions.view_code
   AND target.code = patched_conditions.view_code
   AND target.condition_sql IS DISTINCT FROM patched_conditions.condition_sql;

WITH patched_conditions(view_code, condition_sql) AS (
    SELECT view_code, condition_sql
      FROM public.runtime_customer_condition
     WHERE view_code IN (
        'HierarchicalMod_1.0.0_factoryModel_factoryLineRef',
        'HierarchicalMod_1.0.0_factoryModel_factoryLineRef2',
        'HierarchicalMod_1.0.0_factoryModel_factoryLineRef3',
        'HierarchicalMod_1.0.0_factoryModel_factoryLineRef4'
     )
)
UPDATE public.ec_customer_condition target
   SET condition_sql = patched_conditions.condition_sql
  FROM patched_conditions
 WHERE target.view_code = patched_conditions.view_code
   AND target.code = patched_conditions.view_code
   AND target.condition_sql IS DISTINCT FROM patched_conditions.condition_sql;
