#!/usr/bin/env python3
"""Patch PATROL 6.0.4.0 generated SQL for PostgreSQL-compatible quoting."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


UTILITY_RELATIVE_PATH = Path(
    "service/src/main/java/com/supcon/orchid/PATROL/util/PATROLSqlUtils.java"
)
SERVICE_IMPL_RELATIVE_PATH = Path(
    "service/src/main/java/com/supcon/orchid/PATROL/services/impl"
)
PATROL_PLAN_SERVICE_RELATIVE_PATH = (
    SERVICE_IMPL_RELATIVE_PATH / "PATROLPatrolPlanServiceImpl.java"
)
PATROL_TASK_DETAIL_SERVICE_RELATIVE_PATH = (
    SERVICE_IMPL_RELATIVE_PATH / "PATROLTaskDetailServiceImpl.java"
)
PATROL_TASK_DETAIL_CUSTOM_RELATIVE_PATH = Path(
    "service/src/main/custom/com/supcon/orchid/PATROL/services/impl/"
    "PATROLTaskDetailServiceImpl"
)
PATROL_REPORT_CUSTOM_RELATIVE_PATH = Path(
    "service/src/main/custom/com/supcon/orchid/PATROL/services/impl/"
    "PatrolReportServiceImpl.java"
)
PATROL_GATHER_DATA_CONSUMER_RELATIVE_PATH = Path(
    "service/src/main/java/com/supcon/orchid/PATROL/mobileInterface/consumer/"
    "PATROLCreateCalcGatherDataService.java"
)
UTILITY_METHOD_MARKER = b"public static String normalizeIdentifierQuotes"
UTILITY_IMPORT = b"import com.supcon.orchid.db.DbUtils;"
UTILITY_IMPORT_ANCHOR = b"import com.supcon.orchid.foundation.entities.Company;"
UTILITY_CLASS_ANCHOR = b"public class PATROLSqlUtils extends BaseServiceImpl {"
QUALIFIED_HELPER = (
    b"com.supcon.orchid.PATROL.util.PATROLSqlUtils.normalizeIdentifierQuotes"
)
REPLACEMENTS = (
    (
        b"buffer.toString().replace('\"', '`')",
        QUALIFIED_HELPER + b"(buffer.toString())",
    ),
    (
        b"realSql.toString().replace('\"', '`')",
        QUALIFIED_HELPER + b"(realSql.toString())",
    ),
    (
        b"treesql.replace('\"', '`')",
        QUALIFIED_HELPER + b"(treesql)",
    ),
    # PostgreSQL folds unquoted result aliases to lower case, while the generated
    # association-field merger reads these keys as upper case.
    (b' AS OID";', b' AS \\"OID\\"";'),
    (b' AS ID1";', b' AS \\"ID1\\"";'),
    (b' AS ID2";', b' AS \\"ID2\\"";'),
    (b' AS VAL";', b' AS \\"VAL\\"";'),
    (b' AS REALVAL";', b' AS \\"REALVAL\\"";'),
    # Once aliases are quoted, PostgreSQL requires the ORDER BY reference to
    # use the same case-sensitive identifier.
    (
        b'? "ID1 ASC" : "OID ASC";',
        b'? "\\"ID1\\" ASC" : "\\"OID\\" ASC";',
    ),
    (
        b'? "ID2 ASC" : "ID1 ASC";',
        b'? "\\"ID2\\" ASC" : "\\"ID1\\" ASC";',
    ),
)

PLAN_ID_ASSIGNMENT = b"potrolTask.setPatrolPlanId(potrolPlan.getId());//"
PLAN_ASSOCIATION_ASSIGNMENT = b"potrolTask.setPatrolPlan(potrolPlan);"
TASK_DETAIL_COLUMNS = b"EAM_ID,TASK_AREA_ID,SORT) "
PATCHED_TASK_DETAIL_COLUMNS = (
    b"EAM_ID,TASK_AREA_ID,SORT,VALID,VERSION,TASK_DETAIL_STATE) "
)
TASK_DETAIL_VALUES_PREFIX = b'+ " values('
TASK_DETAIL_VALUES_SUFFIX = b')";'
TASK_DETAIL_DEFAULT_MARKER = b'ps.setString(30, "PATROL_taskDetailState/pending")'
TASK_DETAIL_BATCH_ANCHOR = b"                    ps.addBatch();"
HIDDEN_DANGER_SIGNATURE = b"public Map<String, Object> createHiddenDanger("
HIDDEN_DANGER_NEXT_METHOD = b"    private Map<String, Object> generateDetailMap("
HIDDEN_DANGER_COMPATIBILITY_MARKER = b"PATROL_COMPATIBILITY_PENDING"
HIDDEN_DANGER_METHOD = """    @Override
    public Map<String, Object> createHiddenDanger(String ids, Map<String, String> headerMap, Boolean isCheckFault) {
        if (headerMap == null) {
            headerMap = new HashMap<>();
        }
        Map<String, Object> resultMap = new HashMap<>();
        boolean faultModuleAvailable = moduleCheckService.checkModelIsUpload(FAULT_MODULE_CODE)
                && moduleCheckService.checkModelIsPublish(FAULT_MODULE_CODE);

        if (StringUtil.isBlank(ids)) {
            throw new BAPException(InternationalResource.get(KEY_TASKDETAIL_EMPTY, getCurrentLanguage()));
        }
        String[] idArr = ids.split(",");
        List<Map<String, Object>> detailList = new ArrayList<>();
        List<Map<String, Object>> existingResultList = new ArrayList<>();
        List<PATROLTaskDetail> taskDetails = new ArrayList<>();
        for (String idText : idArr) {
            Long id;
            try {
                id = Long.parseLong(idText.trim());
            } catch (Exception e) {
                log.error("Invalid PATROL task detail id: " + idText, e);
                throw new BAPException(InternationalResource.get(KEY_PARAMS_ERROR, getCurrentLanguage()), e);
            }
            PATROLTaskDetail patrolTaskDetail = getTaskDetail(id);
            if (patrolTaskDetail == null) {
                continue;
            }
            if (patrolTaskDetail.getFaultId() != null) {
                Map<String, Object> existingResult = new HashMap<>();
                existingResult.put("taskDetailId", patrolTaskDetail.getId());
                existingResult.put("tableNo", patrolTaskDetail.getFaultTableNo());
                existingResult.put("riskId", patrolTaskDetail.getFaultId());
                existingResult.put("reused", Boolean.TRUE);
                existingResultList.add(existingResult);
                continue;
            }
            taskDetails.add(patrolTaskDetail);
        }

        if (Boolean.TRUE.equals(isCheckFault)) {
            checkFaultExist(taskDetails);
        }
        for (PATROLTaskDetail patrolTaskDetail : taskDetails) {
            detailList.add(generateDetailMap(patrolTaskDetail));
        }

        Map<String, Object> resultInfo = new HashMap<>();
        if (!detailList.isEmpty()) {
            log.info("Creating PATROL hidden danger records, count=" + detailList.size());
            try {
                if (faultModuleAvailable) {
                    resultInfo = riskHandleWFClient.saveRiskHandleWF(detailList, headerMap);
                } else {
                    resultInfo = saveRiskHandleCompatibility(taskDetails, detailList);
                }
            } catch (Exception e) {
                log.error("Failed to create PATROL hidden danger records", e);
                throw new BAPException(InternationalResource.get(KEY_CREATE_FAIL, getCurrentLanguage()) + e.getMessage());
            }
        }

        List<Map<String, Object>> resultList = resultInfo == null
                ? null
                : (List<Map<String, Object>>) resultInfo.get("data");
        if (resultList != null) {
            for (Map<String, Object> faultInfo : resultList) {
                try {
                    Long taskDetailId = Long.valueOf(faultInfo.get("taskDetailId").toString());
                    PATROLTaskDetail patrolTaskDetail = getTaskDetail(taskDetailId);
                    patrolTaskDetail.setIsFault(true);
                    patrolTaskDetail.setFaultTableNo((String) faultInfo.get("tableNo"));
                    patrolTaskDetail.setFaultId(Long.valueOf(faultInfo.get("riskId").toString()));
                    taskDetailDao.merge(patrolTaskDetail);
                    taskDetailDao.flush();
                } catch (Exception e) {
                    log.error("Created hidden danger but failed to link PATROL task detail", e);
                    throw new BAPException(InternationalResource.get(KEY_TASKDETAIL_UPDATE_FAIL, getCurrentLanguage()));
                }
            }
        }

        if (resultInfo != null) {
            resultMap.putAll(resultInfo);
        }
        List<Map<String, Object>> responseData = new ArrayList<>(existingResultList);
        if (resultList != null) {
            responseData.addAll(resultList);
        }
        resultMap.put("data", responseData);
        resultMap.put("createdCount", resultList == null ? 0 : resultList.size());
        resultMap.put("reusedCount", existingResultList.size());
        return resultMap;
    }

    private Map<String, Object> saveRiskHandleCompatibility(
            List<PATROLTaskDetail> taskDetails, List<Map<String, Object>> detailList) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Date createdAt = new Date();
        String insertSql = "INSERT INTO SES_HRM_RISKHANDLES "
                + "(ID,VALID,STATUS,VERSION,SORT,CID,CREATE_TIME,MODIFY_TIME,CREATE_STAFF_ID,MODIFY_STAFF_ID,"
                + "FIND_TIME,FINDER,TABLE_NO,EAM_INFO,RISK_TYPE,RISK_SOURCE,RISK_MODE,RISK_CONTENT) "
                + "VALUES (:id,1,1,0,0,:cid,:createdAt,:createdAt,:staffId,:staffId,"
                + ":findTime,:finder,:tableNo,:eamInfo,:riskType,:riskSource,:riskMode,:riskContent)";

        for (int index = 0; index < taskDetails.size(); index++) {
            PATROLTaskDetail taskDetail = taskDetails.get(index);
            Map<String, Object> detailInfo = detailList.get(index);
            PATROLPotrolTask patrolTask = taskDetail.getPatrolTask() == null
                    ? null
                    : potrolTaskDao.get(taskDetail.getPatrolTask().getId());
            Long finderId = resolveHiddenDangerFinder(taskDetail, patrolTask);
            Date findTime = resolveHiddenDangerFindTime(taskDetail, patrolTask);
            Long riskId = SnowFlakeIdWorker.getInstance().nextId();
            String tableNo = "PATROL-RISK-" + riskId;

            Object equipmentInfo = detailInfo.get("equipmentInfo");
            Long equipmentId = equipmentInfo == null
                    ? null
                    : Long.valueOf(equipmentInfo.toString());
            Object riskTypeValue = detailInfo.get("riskType");
            String riskType = riskTypeValue == null ? null : riskTypeValue.toString();
            Object riskSourceValue = detailInfo.get("riskResource");
            String riskSource = riskSourceValue == null ? null : riskSourceValue.toString();
            Object riskContentValue = detailInfo.get("riskRemark");
            String riskContent = riskContentValue == null ? null : riskContentValue.toString();

            NativeQuery insertQuery = taskDetailDao.createNativeQuery(insertSql);
            insertQuery
                    .setParameter("id", riskId)
                    .setParameter("cid", getCurrentCompanyId())
                    .setParameter("createdAt", createdAt)
                    .setParameter("staffId", finderId)
                    .setParameter("findTime", findTime)
                    .setParameter("finder", finderId)
                    .setParameter("tableNo", tableNo)
                    .setParameter("eamInfo", equipmentId, org.hibernate.type.LongType.INSTANCE)
                    .setParameter("riskType", riskType, org.hibernate.type.StringType.INSTANCE)
                    .setParameter("riskSource", riskSource, org.hibernate.type.StringType.INSTANCE)
                    .setParameter(
                            "riskMode",
                            "PATROL_COMPATIBILITY_PENDING",
                            org.hibernate.type.StringType.INSTANCE)
                    .setParameter("riskContent", riskContent, org.hibernate.type.StringType.INSTANCE)
                    .executeUpdate();

            Map<String, Object> faultInfo = new HashMap<>();
            faultInfo.put("taskDetailId", taskDetail.getId());
            faultInfo.put("tableNo", tableNo);
            faultInfo.put("riskId", riskId);
            resultList.add(faultInfo);
        }

        Map<String, Object> resultInfo = new HashMap<>();
        resultInfo.put("data", resultList);
        resultInfo.put("compatibilityMode", Boolean.TRUE);
        resultInfo.put("compatibilityStatus", "PATROL_COMPATIBILITY_PENDING");
        log.warn("SESHRM is unavailable; created auditable EAM pending risk records");
        return resultInfo;
    }

    private Long resolveHiddenDangerFinder(PATROLTaskDetail taskDetail, PATROLPotrolTask patrolTask) {
        if (taskDetail.getCompleteUser() != null) {
            return taskDetail.getCompleteUser().getId();
        }
        if (patrolTask != null && patrolTask.getCompleteStaff() != null) {
            return patrolTask.getCompleteStaff().getId();
        }
        Staff currentStaff = (Staff) getCurrentStaff();
        if (currentStaff != null) {
            return currentStaff.getId();
        }
        throw new BAPException("PATROL hidden danger finder is missing");
    }

    private Date resolveHiddenDangerFindTime(PATROLTaskDetail taskDetail, PATROLPotrolTask patrolTask) {
        if (taskDetail.getCompleteDate() != null) {
            return taskDetail.getCompleteDate();
        }
        if (patrolTask != null && patrolTask.getActualEndTime() != null) {
            return patrolTask.getActualEndTime();
        }
        return new Date();
    }

"""
HIDDEN_DANGER_OLD_CONTEXT = """        PATROLPotrolTask patrolTask = potrolTaskDao.get(patrolTaskDetail.getPatrolTask().getId());
        //发现人
        //任务明细的提交人(完成人)
        Long findUserId = null;
        if (patrolTaskDetail.getCompleteUser() != null) {
            findUserId = patrolTaskDetail.getCompleteUser().getId();
        } else {
            findUserId = patrolTask.getCompleteStaff().getId();
        }
        detailMap.put("finder", patrolTaskDetail.getCompleteUser().getId());
        //发现时间
        //任务明细的提交时间
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date findTime = null;
        if (patrolTaskDetail.getCompleteDate() != null) {
            findTime = patrolTaskDetail.getCompleteDate();
        } else {
            findTime = patrolTask.getActualEndTime();
        }
        detailMap.put("findTime", format.format(findTime));
"""
HIDDEN_DANGER_NEW_CONTEXT = """        PATROLPotrolTask patrolTask = patrolTaskDetail.getPatrolTask() == null
                ? null
                : potrolTaskDao.get(patrolTaskDetail.getPatrolTask().getId());
        Long findUserId = resolveHiddenDangerFinder(patrolTaskDetail, patrolTask);
        detailMap.put("finder", findUserId);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date findTime = resolveHiddenDangerFindTime(patrolTaskDetail, patrolTask);
        detailMap.put("findTime", format.format(findTime));
"""
HIDDEN_DANGER_NORMAL_RANGE_OLD = (
    b"if (StringUtil.isNotBlank(patrolTaskDetail.getWorkItemId().getNormalRange())) {"
)
HIDDEN_DANGER_NORMAL_RANGE_NEW = (
    b"if (patrolTaskDetail.getWorkItemId() != null "
    b"&& StringUtil.isNotBlank(patrolTaskDetail.getWorkItemId().getNormalRange())) {"
)
HIDDEN_DANGER_WORK_ITEM_GUARD = b"        if (workItemIds.isEmpty()) {"
HIDDEN_DANGER_WORK_ITEM_ANCHOR = b"        // \xe8\x8e\xb7\xe5\x8f\x96\xe9\x9a\x90\xe6\x82\xa3\xe5\x8d\x95\xe6\x9c\xaa\xe7\x94\x9f\xe6\x95\x88\xe7\x9a\x84\xe5\xb7\xa1\xe6\xa3\x80\xe9\xa1\xb9id"
HIDDEN_DANGER_ALIAS_OLD = (
    b"select detail.WORK_ITEM_ID as WORKITEMID, max(riskhandle.id) as RISKHANDLEID, "
    b"max(riskhandle.table_no) as RISKHANDLETABLENO from MP_TASK_DETAILS detail "
)
HIDDEN_DANGER_ALIAS_NEW = (
    b"select detail.WORK_ITEM_ID as \\\"WORKITEMID\\\", max(riskhandle.id) as \\\"RISKHANDLEID\\\", "
    b"max(riskhandle.table_no) as \\\"RISKHANDLETABLENO\\\" from MP_TASK_DETAILS detail "
)
REPORT_PENDING_MARKER = b'"PATROL_COMPATIBILITY_PENDING".equals(riskMode)'
REPORT_PENDING_REPLACEMENTS = (
    (
        b'StringBuffer sql = new StringBuffer(" select count(1) total, hr.status, wfp.task_description taskDescription ")',
        b'StringBuffer sql = new StringBuffer(" select count(1) total, hr.status, hr.risk_mode riskMode, wfp.task_description taskDescription ")',
    ),
    (
        b'sql.append(" group by hr.status, wfp.task_description ");',
        b'sql.append(" group by hr.status, hr.risk_mode, wfp.task_description ");',
    ),
    (
        b'.addScalar("status", HibernateType.INTEGER)\n                    .addScalar("taskDescription", HibernateType.STRING)',
        b'.addScalar("status", HibernateType.INTEGER)\n                    .addScalar("riskMode", HibernateType.STRING)\n                    .addScalar("taskDescription", HibernateType.STRING)',
    ),
    (
        b'Integer total = (Integer) map.get("total");\n                    if (status==PatrolConstant.flowStatus.FLOW_VALID',
        b'Integer total = (Integer) map.get("total");\n                    String riskMode = (String) map.get("riskMode");\n                    if ("PATROL_COMPATIBILITY_PENDING".equals(riskMode)) {\n                        pending = pending.add(new BigDecimal(total));\n                        continue;\n                    }\n                    if (status==PatrolConstant.flowStatus.FLOW_VALID',
    ),
)
GATHER_DATA_REPLACEMENTS = (
    (
        b'import java.math.BigDecimal;',
        b'import java.math.BigDecimal;\nimport java.nio.charset.StandardCharsets;',
    ),
    (
        b'public void CreateCalcGatherData(Message<String> value)',
        b'public void CreateCalcGatherData(Message<?> value)',
    ),
    (
        b'String jsonString = value.getPayload();',
        b'Object payload = value.getPayload();\n'
        b'                String jsonString = null;\n'
        b'                if (payload instanceof byte[]) {\n'
        b'                    jsonString = new String((byte[]) payload, StandardCharsets.UTF_8);\n'
        b'                } else if (payload instanceof String) {\n'
        b'                    jsonString = (String) payload;\n'
        b'                } else if (payload != null) {\n'
        b'                    jsonString = JSON.toJSONString(payload);\n'
        b'                }',
    ),
    (
        b'PATROLKafkaMessageDTO<List<Map<String, Object>>> messageData = JSONObject.parseObject(jsonString,new TypeReference<PATROLKafkaMessageDTO<List<Map<String, Object>>>>(){});\n'
        b'                    List<Map<String, Object>> workItemTaskIds = messageData.getData();',
        b'PATROLKafkaMessageDTO<List<Map<String, Object>>> messageData = JSONObject.parseObject(jsonString,new TypeReference<PATROLKafkaMessageDTO<List<Map<String, Object>>>>(){});\n'
        b'                    if (messageData == null || ObjectUtils.isEmpty(messageData.getData())) {\n'
        b'                        logger.warn("Ignore empty PATROL gather-data message");\n'
        b'                        return;\n'
        b'                    }\n'
        b'                    List<Map<String, Object>> workItemTaskIds = messageData.getData();',
    ),
    (
        b"if (workItemTaskIds.size() > 0) {",
        b"if (!ObjectUtils.isEmpty(workItemTaskIds)) {",
    ),
    (
        b'new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")',
        b'new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")',
    ),
    (
        b'Long taskId = Long.parseLong(String.valueOf(map.get("taskId")));\n'
        b'                                JSONArray workItemIds = JSON.parseArray(String.valueOf(map.get("workItemIds")));\n'
        b'                                workItemIds.forEach(workItemId -> {',
        b'if (map == null || map.get("taskId") == null || map.get("workItemIds") == null) {\n'
        b'                                    logger.warn("Ignore PATROL gather-data message with missing task or work-item ids");\n'
        b'                                    return;\n'
        b'                                }\n'
        b'                                Long taskId;\n'
        b'                                try {\n'
        b'                                    taskId = Long.parseLong(String.valueOf(map.get("taskId")));\n'
        b'                                } catch (NumberFormatException error) {\n'
        b'                                    logger.warn("Ignore PATROL gather-data message with an invalid task id", error);\n'
        b'                                    return;\n'
        b'                                }\n'
        b'                                JSONArray workItemIds;\n'
        b'                                try {\n'
        b'                                    workItemIds = JSON.parseArray(String.valueOf(map.get("workItemIds")));\n'
        b'                                } catch (Exception error) {\n'
        b'                                    logger.warn("Ignore PATROL gather-data message with an invalid work-item list", error);\n'
        b'                                    return;\n'
        b'                                }\n'
        b'                                if (ObjectUtils.isEmpty(workItemIds)) {\n'
        b'                                    logger.warn("Ignore PATROL gather-data message with an empty work-item list");\n'
        b'                                    return;\n'
        b'                                }\n'
        b'                                workItemIds.forEach(workItemId -> {',
    ),
    (
        b'long aLong = Long.parseLong(String.valueOf(workItemId));\n'
        b'                                    dc.add(Restrictions.eq("workItemId.id", aLong));',
        b'long aLong;\n'
        b'                                    try {\n'
        b'                                        aLong = Long.parseLong(String.valueOf(workItemId));\n'
        b'                                    } catch (NumberFormatException error) {\n'
        b'                                        logger.warn("Ignore PATROL gather-data message with an invalid work-item id", error);\n'
        b'                                        return;\n'
        b'                                    }\n'
        b'                                    dc.add(Restrictions.eq("workItemId.id", aLong));',
    ),
    (
        b'PATROLTaskDetail taskDetail = taskDetails.get(0);\n'
        b'                                        //\xe6\x9f\xa5\xe8\xaf\xa2\xe5\xbc\x80\xe5\xa7\x8b\xe6\x97\xb6\xe9\x97\xb4  \xe5\xae\x8c\xe6\x88\x90\xe6\x97\xb6\xe9\x97\xb4 -30\xe7\xa7\x92\n'
        b'                                        Calendar calendar = Calendar.getInstance();\n'
        b'                                        calendar.setTime(taskDetail.getCompleteDate());',
        b'PATROLTaskDetail taskDetail = taskDetails.get(0);\n'
        b'                                        if (taskDetail.getCompleteDate() == null\n'
        b'                                                || StringUtils.isBlank(taskDetail.getItemNumber())) {\n'
        b'                                            logger.warn("Skip PATROL gather-data lookup without completion time or tag name, detailId="\n'
        b'                                                    + taskDetail.getId());\n'
        b'                                            return;\n'
        b'                                        }\n'
        b'                                        //\xe6\x9f\xa5\xe8\xaf\xa2\xe5\xbc\x80\xe5\xa7\x8b\xe6\x97\xb6\xe9\x97\xb4  \xe5\xae\x8c\xe6\x88\x90\xe6\x97\xb6\xe9\x97\xb4 -30\xe7\xa7\x92\n'
        b'                                        Calendar calendar = Calendar.getInstance();\n'
        b'                                        calendar.setTime(taskDetail.getCompleteDate());',
    ),
    (
        b'JSONObject response = JSON.parseObject(result);\n'
        b'                                            JSONObject data = response.getJSONObject("data");\n'
        b'                                            JSONArray values = data.getJSONArray("values");\n'
        b'                                            if (!ObjectUtils.isEmpty(values)) {\n'
        b'                                                List<Double> doubles = values.parallelStream().map(o -> ((JSONObject) o).getDouble("value")).collect(Collectors.toList());\n'
        b'                                                //\xe8\xae\xa1\xe7\xae\x97\xe4\xb8\xad\xe4\xbd\x8d\xe6\x95\xb0\n'
        b'                                                Double median = calcMedian(doubles);\n'
        b'                                                //\xe4\xbf\x9d\xe5\xad\x98\xe4\xb8\xad\xe4\xbd\x8d\xe6\x95\xb0\n'
        b'                                                taskDetail.setGatherData(new BigDecimal(median));\n'
        b'                                                //\xe4\xbf\x9d\xe5\xad\x98\xe5\xae\x9e\xe4\xbd\x93\n'
        b'                                                taskDetailService.mergeTaskDetail(taskDetail, null);\n'
        b'                                            }',
        b'JSONObject response;\n'
        b'                                            try {\n'
        b'                                                response = JSON.parseObject(result);\n'
        b'                                            } catch (Exception error) {\n'
        b'                                                logger.warn("Ignore malformed TagManagement history response", error);\n'
        b'                                                return;\n'
        b'                                            }\n'
        b'                                            JSONObject data = response == null ? null : response.getJSONObject("data");\n'
        b'                                            JSONArray values = data == null ? null : data.getJSONArray("values");\n'
        b'                                            if (!ObjectUtils.isEmpty(values)) {\n'
        b'                                                List<Double> doubles = values.parallelStream()\n'
        b'                                                        .filter(JSONObject.class::isInstance)\n'
        b'                                                        .map(o -> ((JSONObject) o).getDouble("value"))\n'
        b'                                                        .filter(Objects::nonNull)\n'
        b'                                                        .filter(sample -> !sample.isNaN() && !sample.isInfinite())\n'
        b'                                                        .collect(Collectors.toList());\n'
        b'                                                //\xe8\xae\xa1\xe7\xae\x97\xe4\xb8\xad\xe4\xbd\x8d\xe6\x95\xb0\n'
        b'                                                Double median = calcMedian(doubles);\n'
        b'                                                if (median != null) {\n'
        b'                                                    //\xe4\xbf\x9d\xe5\xad\x98\xe4\xb8\xad\xe4\xbd\x8d\xe6\x95\xb0\n'
        b'                                                    taskDetail.setGatherData(BigDecimal.valueOf(median));\n'
        b'                                                    //\xe4\xbf\x9d\xe5\xad\x98\xe5\xae\x9e\xe4\xbd\x93\n'
        b'                                                    taskDetailService.mergeTaskDetail(taskDetail, null);\n'
        b'                                                }\n'
        b'                                            }',
    ),
    (
        b'private Double calcMedian(List<Double> data) {\n'
        b'        //\xe6\x8e\x92\xe5\xba\x8f\n'
        b'        Collections.sort(data);\n'
        b'        //\xe5\x8f\x96\xe4\xb8\xad\xe4\xbd\x8d\n'
        b'        int size = data.size();\n'
        b'        //\xe5\xa6\x82\xe6\x9e\x9c\xe6\x98\xaf\xe5\x81\xb6\xe6\x95\xb0 \xe5\x8f\x96\xe4\xb8\xad\xe9\x97\xb4\xe4\xb8\xa4\xe4\xb8\xaa\xe6\x95\xb0\xe7\x9a\x84\xe5\xb9\xb3\xe5\x9d\x87\xe5\x80\xbc\n'
        b'        if (size % 2 == 0) {\n'
        b'            Double double1 = data.get(size / 2);\n'
        b'            Double double2 = data.get((size / 2) - 1);\n'
        b'            return (double1 + double2) / 2;\n'
        b'        } else {\n'
        b'            int index = (int) Math.ceil(size / 2.0);\n'
        b'            return data.get(index);\n'
        b'        }\n'
        b'    }',
        b'private Double calcMedian(List<Double> data) {\n'
        b'        if (ObjectUtils.isEmpty(data)) {\n'
        b'            return null;\n'
        b'        }\n'
        b'        Collections.sort(data);\n'
        b'        int size = data.size();\n'
        b'        if (size % 2 == 0) {\n'
        b'            return (data.get(size / 2) + data.get((size / 2) - 1)) / 2;\n'
        b'        }\n'
        b'        return data.get(size / 2);\n'
        b'    }',
    ),
)


def patch_utility(source: bytes) -> tuple[bytes, bool]:
    if UTILITY_CLASS_ANCHOR not in source:
        raise ValueError("PATROLSqlUtils class declaration was not found")

    changed = False
    newline = b"\r\n" if b"\r\n" in source else b"\n"
    if UTILITY_IMPORT not in source:
        if UTILITY_IMPORT_ANCHOR not in source:
            raise ValueError("PATROLSqlUtils import anchor was not found")
        source = source.replace(
            UTILITY_IMPORT_ANCHOR,
            UTILITY_IMPORT_ANCHOR + newline + UTILITY_IMPORT,
            1,
        )
        changed = True

    if UTILITY_METHOD_MARKER not in source:
        method = newline.join(
            (
                b"",
                b"    public static String normalizeIdentifierQuotes(String sql) {",
                b"        return normalizeIdentifierQuotes(sql, DbUtils.getDbName());",
                b"    }",
                b"",
                b"    static String normalizeIdentifierQuotes(String sql, String databaseName) {",
                b"        if (sql == null) {",
                b"            return null;",
                b"        }",
                b"        boolean mysql = databaseName != null",
                b"                && databaseName.toLowerCase(java.util.Locale.ROOT).contains(\"mysql\");",
                b"        return mysql ? sql.replace('\"', '`') : sql;",
                b"    }",
            )
        )
        source = source.replace(UTILITY_CLASS_ANCHOR, UTILITY_CLASS_ANCHOR + method, 1)
        changed = True

    return source, changed


def normalize_helper_line_indentation(source: bytes) -> tuple[bytes, int]:
    normalized_lines = []
    changed = 0
    for line in source.splitlines(keepends=True):
        if QUALIFIED_HELPER not in line:
            normalized_lines.append(line)
            continue
        content = line.lstrip(b" \t")
        prefix = line[: len(line) - len(content)]
        normalized_prefix = prefix
        while b" \t" in normalized_prefix:
            normalized_prefix = normalized_prefix.replace(b" \t", b"\t")
        if normalized_prefix != prefix:
            line = normalized_prefix + content
            changed += 1
        normalized_lines.append(line)
    return b"".join(normalized_lines), changed


def patch_service(source: bytes) -> tuple[bytes, int, int]:
    replacement_count = 0
    for original, replacement in REPLACEMENTS:
        count = source.count(original)
        if count:
            source = source.replace(original, replacement)
            replacement_count += count
    source, indentation_count = normalize_helper_line_indentation(source)
    return source, replacement_count, indentation_count


def patch_task_generation(source: bytes) -> tuple[bytes, int]:
    """Keep generated task relations and detail defaults persistence-complete."""

    changed = 0
    newline = b"\r\n" if b"\r\n" in source else b"\n"

    if PLAN_ASSOCIATION_ASSIGNMENT not in source:
        assignment_at = source.find(PLAN_ID_ASSIGNMENT)
        if assignment_at < 0:
            raise ValueError("PATROL task plan-id assignment was not found")
        line_start = source.rfind(newline, 0, assignment_at) + len(newline)
        line_end = source.find(newline, assignment_at)
        if line_end < 0:
            raise ValueError("PATROL task plan-id assignment line is incomplete")
        indentation = source[line_start:assignment_at]
        insertion = newline + indentation + PLAN_ASSOCIATION_ASSIGNMENT
        source = source[:line_end] + insertion + source[line_end:]
        changed += 1

    if PATCHED_TASK_DETAIL_COLUMNS not in source:
        if source.count(TASK_DETAIL_COLUMNS) != 1:
            raise ValueError("PATROL task-detail insert column list was not found uniquely")
        source = source.replace(
            TASK_DETAIL_COLUMNS, PATCHED_TASK_DETAIL_COLUMNS, 1
        )
        changed += 1

    if TASK_DETAIL_DEFAULT_MARKER not in source:
        column_at = source.find(PATCHED_TASK_DETAIL_COLUMNS)
        values_at = source.find(TASK_DETAIL_VALUES_PREFIX, column_at)
        values_end = source.find(TASK_DETAIL_VALUES_SUFFIX, values_at)
        if values_at < 0 or values_end < 0:
            raise ValueError("PATROL task-detail insert placeholders were not found")
        source = (
            source[:values_end]
            + b",?,?,?"
            + source[values_end:]
        )

        batch_at = source.find(TASK_DETAIL_BATCH_ANCHOR, values_end)
        if batch_at < 0:
            raise ValueError("PATROL task-detail batch anchor was not found")
        defaults = newline.join(
            (
                b"                    ps.setBoolean(28, true);// valid",
                b"                    ps.setInt(29, 0);// version",
                b'                    ps.setString(30, "PATROL_taskDetailState/pending");// pending',
                b"",
            )
        )
        source = source[:batch_at] + defaults + source[batch_at:]
        changed += 1

    return source, changed


def source_fragment(text: str, newline: bytes) -> bytes:
    return text.replace("\n", newline.decode("ascii")).encode("utf-8")


def patch_hidden_danger(source: bytes) -> tuple[bytes, int]:
    """Keep PATROL abnormal results traceable when SESH is not deployed."""

    changed = 0
    newline = b"\r\n" if b"\r\n" in source else b"\n"

    if HIDDEN_DANGER_COMPATIBILITY_MARKER not in source:
        signature_at = source.find(HIDDEN_DANGER_SIGNATURE)
        if signature_at < 0:
            raise ValueError("PATROL hidden-danger method was not found")
        method_start = source.rfind(b"    @Override", 0, signature_at)
        method_end = source.find(HIDDEN_DANGER_NEXT_METHOD, signature_at)
        if method_start < 0 or method_end < 0:
            raise ValueError("PATROL hidden-danger method boundaries were not found")
        replacement = source_fragment(HIDDEN_DANGER_METHOD, newline)
        source = source[:method_start] + replacement + source[method_end:]
        changed += 1

    old_context = source_fragment(HIDDEN_DANGER_OLD_CONTEXT, newline)
    new_context = source_fragment(HIDDEN_DANGER_NEW_CONTEXT, newline)
    if old_context in source:
        source = source.replace(old_context, new_context, 1)
        changed += 1
    elif new_context not in source:
        raise ValueError("PATROL hidden-danger finder/time block was not found")

    if HIDDEN_DANGER_NORMAL_RANGE_OLD in source:
        source = source.replace(
            HIDDEN_DANGER_NORMAL_RANGE_OLD,
            HIDDEN_DANGER_NORMAL_RANGE_NEW,
            1,
        )
        changed += 1
    elif HIDDEN_DANGER_NORMAL_RANGE_NEW not in source:
        raise ValueError("PATROL hidden-danger normal-range guard was not found")

    if HIDDEN_DANGER_WORK_ITEM_GUARD not in source:
        anchor_at = source.find(HIDDEN_DANGER_WORK_ITEM_ANCHOR)
        if anchor_at < 0:
            raise ValueError("PATROL hidden-danger work-item query anchor was not found")
        guard = source_fragment(
            """        if (workItemIds.isEmpty()) {
            return;
        }
""",
            newline,
        )
        source = source[:anchor_at] + guard + source[anchor_at:]
        changed += 1

    if HIDDEN_DANGER_ALIAS_OLD in source:
        source = source.replace(
            HIDDEN_DANGER_ALIAS_OLD,
            HIDDEN_DANGER_ALIAS_NEW,
            1,
        )
        changed += 1
    elif HIDDEN_DANGER_ALIAS_NEW not in source:
        raise ValueError("PATROL hidden-danger PostgreSQL aliases were not found")

    return source, changed


def patch_report_pending_handoff(source: bytes) -> tuple[bytes, int]:
    """Count degraded PATROL-to-EAM handoffs as pending, never as closed."""

    if REPORT_PENDING_MARKER in source:
        return source, 0

    changed = 0
    newline = b"\r\n" if b"\r\n" in source else b"\n"
    for original, replacement in REPORT_PENDING_REPLACEMENTS:
        original = original.replace(b"\n", newline)
        replacement = replacement.replace(b"\n", newline)
        if source.count(original) != 1:
            raise ValueError("PATROL pending-handoff report anchor was not found uniquely")
        source = source.replace(original, replacement, 1)
        changed += 1
    return source, changed


def patch_gather_data_consumer(source: bytes) -> tuple[bytes, int]:
    """Harden the Kafka-to-TagManagement gather-data calculation path."""

    changed = 0
    newline = b"\r\n" if b"\r\n" in source else b"\n"
    for original, replacement in GATHER_DATA_REPLACEMENTS:
        original = original.replace(b"\n", newline)
        replacement = replacement.replace(b"\n", newline)
        if replacement in source:
            continue
        if source.count(original) != 1:
            raise ValueError("PATROL gather-data consumer anchor was not found uniquely")
        source = source.replace(original, replacement, 1)
        changed += 1
    return source, changed


def patch_module(module_root: Path, check: bool, source_commit: str) -> dict[str, object]:
    utility_path = module_root / UTILITY_RELATIVE_PATH
    service_root = module_root / SERVICE_IMPL_RELATIVE_PATH
    if not utility_path.is_file() or not service_root.is_dir():
        raise ValueError(f"not a PATROL 6.0.4.0 source module: {module_root}")

    utility_before = utility_path.read_bytes()
    utility_after, utility_changed = patch_utility(utility_before)
    patched_files = []
    replacement_count = 0
    indentation_count = 0
    task_generation_fix_count = 0
    hidden_danger_fix_count = 0
    report_pending_fix_count = 0
    gather_data_fix_count = 0
    for path in sorted(service_root.glob("PATROL*ServiceImpl.java")):
        before = path.read_bytes()
        after, current_count, current_indentation_count = patch_service(before)
        current_task_generation_count = 0
        if path == module_root / PATROL_PLAN_SERVICE_RELATIVE_PATH:
            after, current_task_generation_count = patch_task_generation(after)
        current_hidden_danger_count = 0
        if path == module_root / PATROL_TASK_DETAIL_SERVICE_RELATIVE_PATH:
            after, current_hidden_danger_count = patch_hidden_danger(after)
        if current_count or current_indentation_count:
            patched_files.append(str(path.relative_to(module_root)))
            replacement_count += current_count
            indentation_count += current_indentation_count
        if current_task_generation_count:
            relative_path = str(path.relative_to(module_root))
            if relative_path not in patched_files:
                patched_files.append(relative_path)
            task_generation_fix_count += current_task_generation_count
        if current_hidden_danger_count:
            relative_path = str(path.relative_to(module_root))
            if relative_path not in patched_files:
                patched_files.append(relative_path)
            hidden_danger_fix_count += current_hidden_danger_count
        if not check and after != before:
            path.write_bytes(after)

    custom_task_detail_path = module_root / PATROL_TASK_DETAIL_CUSTOM_RELATIVE_PATH
    if not custom_task_detail_path.is_file():
        raise ValueError("PATROL custom task-detail service source was not found")
    custom_before = custom_task_detail_path.read_bytes()
    custom_after, custom_hidden_danger_count = patch_hidden_danger(custom_before)
    if custom_hidden_danger_count:
        relative_path = str(custom_task_detail_path.relative_to(module_root))
        if relative_path not in patched_files:
            patched_files.append(relative_path)
        hidden_danger_fix_count += custom_hidden_danger_count
    if not check and custom_after != custom_before:
        custom_task_detail_path.write_bytes(custom_after)

    custom_report_path = module_root / PATROL_REPORT_CUSTOM_RELATIVE_PATH
    if not custom_report_path.is_file():
        raise ValueError("PATROL custom report service source was not found")
    report_before = custom_report_path.read_bytes()
    report_after, report_pending_fix_count = patch_report_pending_handoff(report_before)
    if report_pending_fix_count:
        relative_path = str(custom_report_path.relative_to(module_root))
        if relative_path not in patched_files:
            patched_files.append(relative_path)
    if not check and report_after != report_before:
        custom_report_path.write_bytes(report_after)

    gather_data_path = module_root / PATROL_GATHER_DATA_CONSUMER_RELATIVE_PATH
    if not gather_data_path.is_file():
        raise ValueError("PATROL gather-data consumer source was not found")
    gather_data_before = gather_data_path.read_bytes()
    gather_data_after, gather_data_fix_count = patch_gather_data_consumer(gather_data_before)
    if gather_data_fix_count:
        relative_path = str(gather_data_path.relative_to(module_root))
        if relative_path not in patched_files:
            patched_files.append(relative_path)
    if not check and gather_data_after != gather_data_before:
        gather_data_path.write_bytes(gather_data_after)

    remaining = []
    for path in sorted(service_root.glob("PATROL*ServiceImpl.java")):
        source = path.read_bytes()
        if not check:
            source, _, _ = patch_service(source)
            if path == module_root / PATROL_PLAN_SERVICE_RELATIVE_PATH:
                source, _ = patch_task_generation(source)
            if path == module_root / PATROL_TASK_DETAIL_SERVICE_RELATIVE_PATH:
                source, _ = patch_hidden_danger(source)
        if b".replace('\"', '`')" in source:
            remaining.append(str(path.relative_to(module_root)))

    if remaining:
        raise ValueError(f"unhandled identifier quote conversions: {remaining}")
    if check and (
        utility_changed
        or replacement_count
        or indentation_count
        or task_generation_fix_count
        or hidden_danger_fix_count
        or report_pending_fix_count
        or gather_data_fix_count
    ):
        raise ValueError(
            f"source patch is required: utilityChanged={utility_changed}, "
            f"replacements={replacement_count}, indentation={indentation_count}, "
            f"taskGeneration={task_generation_fix_count}, "
            f"hiddenDanger={hidden_danger_fix_count}"
            f", reportPending={report_pending_fix_count}"
            f", gatherData={gather_data_fix_count}"
        )
    if not check and utility_changed:
        utility_path.write_bytes(utility_after)

    helper_references = sum(
        path.read_bytes().count(QUALIFIED_HELPER)
        for path in service_root.glob("PATROL*ServiceImpl.java")
    )
    return {
        "moduleRoot": str(module_root),
        "sourceCommit": source_commit,
        "mode": "check" if check else "apply",
        "utilityChanged": utility_changed,
        "replacementCount": replacement_count,
        "indentationFixCount": indentation_count,
        "taskGenerationFixCount": task_generation_fix_count,
        "hiddenDangerFixCount": hidden_danger_fix_count,
        "reportPendingFixCount": report_pending_fix_count,
        "gatherDataFixCount": gather_data_fix_count,
        "patchedFileCount": len(patched_files),
        "patchedFiles": patched_files,
        "helperReferenceCount": helper_references,
        "status": "PASS",
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--module-root", required=True, type=Path)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--source-commit", default="unknown")
    parser.add_argument("--report", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        report = patch_module(
            args.module_root.expanduser().resolve(), args.check, args.source_commit
        )
    except (OSError, ValueError) as error:
        print(f"PATROL source patch failed: {error}", file=sys.stderr)
        return 2

    payload = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(payload, encoding="utf-8")
    print(payload, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
