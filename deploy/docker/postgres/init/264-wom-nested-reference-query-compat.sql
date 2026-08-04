-- PostgreSQL compatibility for nested WOM reference dialogs.
--
-- The packaged V6 metadata still points several read-only references at a
-- retired preparation entity and at pre-refactor output-detail columns. Keep
-- the current PostgreSQL tables authoritative and expose only the legacy read
-- contract needed by those views.

DO $migration$
DECLARE
    relation_kind "char";
BEGIN
    SELECT c.relkind
      INTO relation_kind
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public'
       AND c.relname = 'wom_prepre_materials';

    IF relation_kind IS NULL OR relation_kind = 'v' THEN
        EXECUTE $view$
            CREATE OR REPLACE VIEW public.wom_prepre_materials AS
            SELECT
                id,
                version,
                create_staff_id,
                create_time,
                modify_staff_id,
                modify_time,
                delete_staff_id,
                delete_time,
                valid,
                cid,
                status,
                table_no,
                table_info_id,
                done_num_sum,
                need_num,
                order_state,
                pre_pare_date,
                pre_pare_need_part_id,
                prepare_staff AS pre_pare_staff,
                product_id,
                remark,
                return_num,
                need_end_time AS need_date
            FROM public.wom_pre_pra_orders
        $view$;
    ELSE
        RAISE NOTICE 'wom_prepre_materials is a physical relation; compatibility view was not installed';
    END IF;
END
$migration$;

-- The packaged output reference was generated with fields from PutinDetail
-- while its FROM clause targets OutputDetail. Map the legacy aliases to the
-- closest persisted OutputDetail values without changing the entity schema.
UPDATE public.runtime_sql
SET query_sql = $sql$
SELECT
    "outputDetail".ID AS "id",
    "outputDetail".VERSION AS "version",
    "outputDetail".CID AS "cid",
    "outputDetail".MATERIAL_BATCH_NUM AS "materialBatchNum",
    "outputDetail".OUTPUT_NUM AS "putinNum",
    "outputDetail".REPORT_NUM AS "useNum",
    "outputDetail".PUTIN_TIME AS "putinTime",
    "outputDetail".PUTIN_END_TIME AS "putinEndTime",
    "outputDetail".TASK_TYPE AS "taskType",
    GREATEST(
        COALESCE("outputDetail".OUTPUT_NUM, "outputDetail".REPORT_NUM, 0)
        - COALESCE("outputDetail".REMAIN_NUM, 0),
        0
    ) AS "availableNum",
    "outputDetail".REMAIN_NUM AS "remainNum",
    "outputDetail".REMAIN_NUM AS "remainNumRac",
    "materialId".CODE AS "materialId.code",
    "materialId".NAME AS "materialId.name",
    "storeId".CODE AS "storeId.code",
    "storeId".NAME AS "storeId.name",
    "wareId".CODE AS "wareId.code",
    "wareId".NAME AS "wareId.name",
    "headId.taskId".ID AS "headId.taskId.id",
    "headId.taskId".PRODUCE_BATCH_NUM AS "headId.taskId.produceBatchNum",
    "materialId".ID AS "materialId.id",
    "storeId".ID AS "storeId.id",
    "wareId".ID AS "wareId.id",
    "outputDetail".VALID AS "valid"
FROM WOM_OUTPUT_DETAILS "outputDetail"
LEFT OUTER JOIN BASESET_MATERIALS "materialId"
    ON "materialId".ID = "outputDetail".PRODUCT
LEFT OUTER JOIN BASESET_STORE_SETS "storeId"
    ON "storeId".ID = "outputDetail".STORE_ID
LEFT OUTER JOIN BASESET_WAREHOUSES "wareId"
    ON "wareId".ID = "outputDetail".WARE_ID
LEFT OUTER JOIN WOM_PROC_REPORTS "headId"
    ON "headId".ID = "outputDetail".HEAD_ID
   AND "headId".VALID = 1
LEFT OUTER JOIN WOM_PRODUCE_TASKS "headId.taskId"
    ON "headId.taskId".ID = "headId".TASK_ID
$sql$
WHERE code = 'WOM_1.0.0_procReport_outputRef_7';

UPDATE public.ec_sql
SET query_sql = (
    SELECT query_sql
      FROM public.runtime_sql
     WHERE code = 'WOM_1.0.0_procReport_outputRef_7'
)
WHERE code = 'WOM_1.0.0_procReport_outputRef_7';

-- Both references are valid without a parent selection. A supplied parent
-- value still applies the original narrowing condition.
UPDATE public.runtime_customer_condition
SET condition_sql = $condition$
if (customCondition && customCondition.warehouseId) {
    return "(WAREHOUSE = \${warehouseId,Long})";
}
return '1=1';
$condition$
WHERE code = 'BaseSet_1.0.0_warehouse_storeSetRefTree';

UPDATE public.ec_customer_condition
SET condition_sql = (
    SELECT condition_sql
      FROM public.runtime_customer_condition
     WHERE code = 'BaseSet_1.0.0_warehouse_storeSetRefTree'
)
WHERE code = 'BaseSet_1.0.0_warehouse_storeSetRefTree';

UPDATE public.runtime_customer_condition
SET condition_sql = $condition$
String sqlPart = '''( "produceTask".id IN (
    SELECT id
      FROM WOM_PRODUCE_TASKS
     WHERE IS_PREPARED = 0
       AND PRE_PARE_STATE = 'WOM_prePareNeedState/waitPrePare'
       AND TASK_RUN_STATE IN (
           'WOM_runState/waitForRun',
           'WOM_runState/runing',
           'WOM_runState/pausing',
           'WOM_runState/paused',
           'WOM_runState/resuming'
       )''';
if (customCondition && customCondition.factoryId) {
    sqlPart += ''' AND LINE_ID IN (
        SELECT id
          FROM HM_FACTORY_MODELS
         WHERE PARENT_ID = ${factoryId,Long}
           AND NODE_TYPE_ID = 1003
           AND VALID = 1
    )''';
}
sqlPart += "))";
return sqlPart;
$condition$
WHERE code = 'WOM_1.0.0_produceTask_taskRefForPrNeed';

UPDATE public.ec_customer_condition
SET condition_sql = (
    SELECT condition_sql
      FROM public.runtime_customer_condition
     WHERE code = 'WOM_1.0.0_produceTask_taskRefForPrNeed'
)
WHERE code = 'WOM_1.0.0_produceTask_taskRefForPrNeed';
