-- Null-safe runtime conditions for HierarchicalMod factory line references.
--
-- SQL 175 restored the missing rm_line_formulas table used by formula-scoped
-- WOM production-line references. A remaining legacy runtime condition still
-- accessed customCondition.formulaId/customCondition.lineId without checking
-- whether customCondition itself was present. Generic line-reference callers can
-- post without customCondition, which raises:
--   Cannot get property 'formulaId' on null object
--
-- Keep the original filter semantics: when formulaId/lineId is provided, apply
-- the existing scoped filter; otherwise fall back to all valid line nodes.

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
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')) and \"factoryModel\".ID in(select LINE_ID from rm_line_formulas WHERE (FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')))";
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
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')) and \"factoryModel\".ID in(select LINE_ID from rm_line_formulas WHERE (FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')))";
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
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')) and \"factoryModel\".ID in(select LINE_ID from rm_line_formulas WHERE (FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')))";
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
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')) and \"factoryModel\".ID not in(select ID from HM_FACTORY_MODELS where id = \${lineId, Long}))";
} else {
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')))";
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
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')) and \"factoryModel\".ID in(select LINE_ID from rm_line_formulas WHERE (FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')))";
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
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')) and \"factoryModel\".ID in(select LINE_ID from rm_line_formulas WHERE (FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')))";
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
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')) and \"factoryModel\".ID in(select LINE_ID from rm_line_formulas WHERE (FORMULA_ID = \${formulaId, Long} and VALID = 1)))";
} else {
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')))";
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
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')) and \"factoryModel\".ID not in(select ID from HM_FACTORY_MODELS where id = \${lineId, Long}))";
} else {
    return "( \"factoryModel\".VALID = 1 and  \"factoryModel\".NODE_TYPE_ID in(select ID from HM_FAC_NODE_TYPES where ( CODE  like  '004')))";
}
$condition$
        )
)
UPDATE public.ec_customer_condition target
   SET condition_sql = patched_conditions.condition_sql
  FROM patched_conditions
 WHERE target.view_code = patched_conditions.view_code
   AND target.code = patched_conditions.view_code
   AND target.condition_sql IS DISTINCT FROM patched_conditions.condition_sql;
