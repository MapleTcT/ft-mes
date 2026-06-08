package com.supcon.supfusion.auditlog.service.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.auditlog.common.constant.AuditLogConstants;
import com.supcon.supfusion.auditlog.common.exception.AuditLogErrorEnum;
import com.supcon.supfusion.auditlog.common.exception.AuditLogException;
import com.supcon.supfusion.auditlog.common.util.*;
import com.supcon.supfusion.auditlog.dao.mapper.AuditlogModelMapper;
import com.supcon.supfusion.auditlog.dao.mapper.EcPropertyMapper;
import com.supcon.supfusion.auditlog.dao.po.model.AuditlogModelPO;
import com.supcon.supfusion.auditlog.dao.po.model.EcPropertyPO;
import com.supcon.supfusion.auditlog.manager.FileServerApiServiceAdapter;
import com.supcon.supfusion.auditlog.manager.I18nServiceAdapter;
import com.supcon.supfusion.auditlog.manager.SystemCodeServiceAdapter;
import com.supcon.supfusion.auditlog.service.DataAuditLogService;
import com.supcon.supfusion.auditlog.service.bo.*;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.ModelObjectInfo;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.po.AuditBusinessLogPO;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.po.AuditDataLogPO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author caokele
 */
@Slf4j
@Service
public class DataAuditLogServiceImpl implements DataAuditLogService {

    @Value("${nacos:127.0.0.1}")
    private String serverAddress;
    private String baseServiceProperties = "supfusion-baseApplications.properties";
    @Value("${spring.profiles.active:prod}")
    private String group;

    @Autowired
    AuditlogModelMapper auditlogModelMapper;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MessageResourceWrapper messageResourceWrapper;
    @Autowired
    private SystemCodeServiceAdapter systemCodeServiceAdapter;
    @Autowired
    private I18nServiceAdapter i18nServiceAdapter;
    @Autowired
    private FileServerApiServiceAdapter fileServerApiServiceAdapter;
    @Autowired
    private EcPropertyMapper ecPropertyMapper;

    @Override
    public PageResult<DataAuditLogBO> queryDataLogs(Integer current, Integer pageSize, DataAuditLogQueryBO dataAuditLogQuery) {
        // 拼接条件
        Query query = buildAuditLogQuery(current, pageSize, dataAuditLogQuery);
        // 总数
        long count = mongoTemplate.count(query, AuditBusinessLogPO.class, AuditBusinessLogPO.COLLECTION_NAME);
        if (count == 0) {
            return new PageResult<>(Collections.emptyList(), count, pageSize, current);
        }
        // 数据审计日志列表
        LinkedList<DataAuditLogBO> dataAuditLogs = new LinkedList<>();
        // 审计业务日志列表
        List<AuditBusinessLogPO> auditBusinessLogs = mongoTemplate.find(query, AuditBusinessLogPO.class, AuditBusinessLogPO.COLLECTION_NAME);
        for (AuditBusinessLogPO auditBusinessLog : auditBusinessLogs) {
            DataAuditLogBO dataAuditLogBO = new DataAuditLogBO();
            // Long转String
            dataAuditLogBO.setTraceId(auditBusinessLog.getTraceId().toString());
            dataAuditLogBO.setModuleName(messageResourceWrapper.getMessageNotBlank(auditBusinessLog.getModuleName()));
            dataAuditLogBO.setOperateUserName(auditBusinessLog.getOperateUserName());
            dataAuditLogBO.setOperateTime(DateTimeExtraUtils.formatUTC0(auditBusinessLog.getOperateTime()));
            SystemCodeResultDTO operateType = systemCodeServiceAdapter.getSystemCode(AuditLogConstants.SYSTEM_OPERATE_TYPE, auditBusinessLog.getOperateType());
            dataAuditLogBO.setOperateType(operateType);
            dataAuditLogBO.setIpAddress(auditBusinessLog.getIpAddress());
            dataAuditLogBO.setSuccess(auditBusinessLog.getSuccess());
            dataAuditLogBO.setExceptionDescription(auditBusinessLog.getExceptionDescription());
            dataAuditLogBO.setFileName(auditBusinessLog.getFileName());
            dataAuditLogBO.setFileUrl(auditBusinessLog.getFileUrl());

            // 导入操作 不展示子列表
            if (StringUtils.equalsIgnoreCase(AuditLogConstants.OPERATE_IMPORT, operateType.getCode())){
                List<ModelObjectInfo> modelObjects = auditBusinessLog.getModelObjects();
                if (!CollectionUtils.isEmpty(modelObjects)){
                    ModelObjectInfo modelObjectInfo = modelObjects.get(0);
                    // 模型名称
                    String modelName = modelObjectInfo.getModelName();
                    // 表单名称
                    String entityName = modelObjectInfo.getEntityName();
                    dataAuditLogBO.setDescription(getDescription(auditBusinessLog.getDescription(), operateType.getName(), modelName, auditBusinessLog.getSuccess()));
                    dataAuditLogBO.setFormName(messageResourceWrapper.getMessageNotBlank(entityName));
                }

                dataAuditLogs.add(dataAuditLogBO);
                continue;
            }

            List<ModelObjectInfo> modelObjects = auditBusinessLog.getModelObjects();
            // 没有数据记录
            if (modelObjects == null || modelObjects.isEmpty()) {
                dataAuditLogBO.setDescription(getDescription(auditBusinessLog.getDescription(), operateType.getName(), null, auditBusinessLog.getSuccess()));
                dataAuditLogs.add(dataAuditLogBO);
                continue;
            }
            // 只有一条数据记录
            if (modelObjects.size() == 1) {
                ModelObjectInfo modelObject = modelObjects.get(0);
                dataAuditLogBO.setDescription(getDescription(auditBusinessLog.getDescription(), operateType.getName(), modelObject.getModelName(), auditBusinessLog.getSuccess()));
                // 实体名称
                dataAuditLogBO.setFormName(messageResourceWrapper.getMessageNotBlank(modelObject.getEntityName()));
                // 被操作对象名称为模型名称
                dataAuditLogBO.setModelObjName(messageResourceWrapper.getMessageNotBlank(modelObject.getModelName()));
                // 业务主键
                dataAuditLogBO.setModelObjCode(modelObject.getModelObjPk() != null ? modelObject.getModelObjCode() : modelObject.getModelObjPk());

                dataAuditLogs.add(dataAuditLogBO);
                continue;
            }

            // 有多条数据记录
            LinkedList<DataAuditLogBO> dataAuditLogChildren = new LinkedList<>();
            // 记录子模型编码
            Set<String> modelCodes = new HashSet<>(modelObjects.size());
            for (ModelObjectInfo modelObject : modelObjects) {
                String modelCode = modelObject.getModelCode();
                // 不记录主模型编码
                if (!Objects.equals(modelCode, auditBusinessLog.getMainModelCode())){
                    modelCodes.add(modelCode);
                }
                // 如果是主模型
                if (dataAuditLogBO.getModelObjName() == null && auditBusinessLog.getMainModelCode() != null && Objects.equals(modelCode, auditBusinessLog.getMainModelCode())) {
                    dataAuditLogBO.setDescription(getDescription(auditBusinessLog.getDescription(), operateType.getName(), modelObject.getModelName(), auditBusinessLog.getSuccess()));
                    // 实体名称
                    dataAuditLogBO.setFormName(messageResourceWrapper.getMessageNotBlank(modelObject.getEntityName()));
                    // 被操作对象名称为模型名称
                    dataAuditLogBO.setModelObjName(messageResourceWrapper.getMessageNotBlank(modelObject.getModelName()));
                    // code > id主键
                    dataAuditLogBO.setModelObjCode(modelObject.getModelObjCode() != null ? modelObject.getModelObjCode() : modelObject.getModelObjPk());
                }
            }

            // 完善子模型的信息
            for (String modelCode : modelCodes) {
                Query modelQuery = Query.query(Criteria.where(AuditLogConstants.FILED_TRACE_ID).is(auditBusinessLog.getTraceId()))
                        .with(Sort.by(Sort.Direction.ASC, AuditLogConstants.FILED_OPERATE_TIME));
                List<AuditDataLogPO> auditDataLogs = mongoTemplate.find(modelQuery, AuditDataLogPO.class, modelCode);

                // 避免空指针
                if (!CollectionUtils.isEmpty(auditDataLogs)) {
                    // 记录已存在的主键，确保同一种模型对象只展示一次
                    Set<String> existPk = new HashSet<>(auditDataLogs.size());
                    for (AuditDataLogPO auditDataLog : auditDataLogs) {
                        Map<String, Object> modelMap = auditDataLog.getModel();
                        String pk = getPk(modelMap);
                        if (existPk.contains(pk)) {
                            continue;
                        }
                        DataAuditLogBO dataAuditLogChild = new DataAuditLogBO();
                        // Long转String
                        dataAuditLogChild.setTraceId(auditBusinessLog.getTraceId().toString());
                        dataAuditLogChild.setModuleName(messageResourceWrapper.getMessageNotBlank(auditBusinessLog.getModuleName()));
                        dataAuditLogChild.setOperateUserName(auditBusinessLog.getOperateUserName());
                        dataAuditLogChild.setOperateTime(DateTimeExtraUtils.formatUTC0(auditDataLog.getOperateTime()));
                        SystemCodeResultDTO childOperateType = systemCodeServiceAdapter.getSystemCode(AuditLogConstants.SYSTEM_OPERATE_TYPE, auditDataLog.getOperateType());
                        dataAuditLogChild.setOperateType(childOperateType);
                        dataAuditLogChild.setIpAddress(auditBusinessLog.getIpAddress());
                        // code > id主键
                        dataAuditLogChild.setModelObjCode(getCode(modelMap) != null ? getCode(modelMap) : getPk(modelMap));
                        // 被操作对象名称为模型名称
                        dataAuditLogChild.setModelObjName(messageResourceWrapper.getMessageNotBlank(auditDataLog.getModelName()));
                        // 操作描述
                        dataAuditLogChild.setDescription(getDescription(auditBusinessLog.getDescription(), childOperateType.getName(), auditDataLog.getModelName(), auditBusinessLog.getSuccess()));

                        // 实体名称
                        dataAuditLogChild.setFormName(messageResourceWrapper.getMessageNotBlank(auditDataLog.getEntityName()));

                        dataAuditLogChildren.add(dataAuditLogChild);
                        existPk.add(pk);
                    }
                    dataAuditLogBO.setChildren(dataAuditLogChildren);
                }
            }
            dataAuditLogs.add(dataAuditLogBO);
        }
        return new PageResult<>(dataAuditLogs, count, pageSize, current);
    }

    @Override
    public ListResult<DataModelBO> queryDataModels(Long traceId, DataModelQueryBO dataModelQueryBO) {
        Query auditBusinessQuery = Query.query(Criteria.where(AuditLogConstants.FIELD_COMPANY_ID).is(UserContext.getUserContext().getCompanyId()))
                .addCriteria(Criteria.where(AuditLogConstants.FILED_TRACE_ID).is(traceId));
        //Query auditBusinessQuery = new Query();
        //auditBusinessQuery.addCriteria(Criteria.where(AuditLogConstants.FILED_TRACE_ID).is(traceId));
        AuditBusinessLogPO auditBusinessLog = mongoTemplate.findOne(auditBusinessQuery, AuditBusinessLogPO.class, AuditBusinessLogPO.COLLECTION_NAME);
        if (auditBusinessLog == null || auditBusinessLog.getModelObjects() == null || auditBusinessLog.getModelObjects().isEmpty()) {
            return new ListResult<>(Collections.emptyList());
        }
        Set<String> modelCodes = auditBusinessLog.getModelObjects().stream().map(ModelObjectInfo::getModelCode).collect(Collectors.toSet());
        // 拼接条件
        Query query = buildDataModelQuery(traceId, dataModelQueryBO);
        List<DataModelBO> dataModels = new LinkedList<>();
        for (String modelCode : modelCodes) {
            // 审计数据日志
            List<AuditDataLogPO> auditDataLogs = mongoTemplate.find(query, AuditDataLogPO.class, modelCode);
            // 过滤重复模型对象
            Set<String> set = new HashSet<>();
            for (AuditDataLogPO auditDataLog : auditDataLogs) {
                DataModelBO dataModelBO = new DataModelBO();
                BeanUtils.copyProperties(auditDataLog, dataModelBO);
                Map<String, Object> model = auditDataLog.getModel();
                // 业务主键
                String pk = getPk(model);
                if (set.contains(pk)){
                    continue;
                }
                set.add(pk);
                // Long转String
                dataModelBO.setTraceId(auditBusinessLog.getTraceId().toString());
                dataModelBO.setOperateTime(DateTimeExtraUtils.formatUTC0(auditDataLog.getOperateTime()));
                dataModelBO.setEntityName(messageResourceWrapper.getMessageNotBlank(auditDataLog.getEntityName()));
                dataModelBO.setOperateType(systemCodeServiceAdapter.getSystemCode(AuditLogConstants.SYSTEM_OPERATE_TYPE, auditDataLog.getOperateType()));

                if (!CollectionUtils.isEmpty(model)) {
                    dataModelBO.setModelObjName(messageResourceWrapper.getMessageNotBlank(auditDataLog.getModelName()));
                    // code > id
                    dataModelBO.setModelObjCode(getCode(model) != null ? getCode(model) : pk);
                }

                //if (model != null && !model.isEmpty()) {
                //    String modelObjName = (String) model.get(AuditLogConstants.FIELD_NAME);
                //    if (modelObjName == null) {
                //        Long id = (Long) model.get(AuditLogConstants.FIELD_ID);
                //        if (id != null) {
                //            modelObjName = id.toString();
                //        }
                //    }
                //    dataModelBO.setModelObjName(modelObjName);
                //    dataModelBO.setModelObjCode((String) model.get(AuditLogConstants.FIELD_CODE));
                //}

                dataModels.add(dataModelBO);
            }
        }
        return new ListResult<>(dataModels);
    }

    @Override
    public ResponseEntity<byte[]> queryDataLogRes(Long traceId) {
        Query query = Query.query(Criteria.where(AuditLogConstants.FIELD_COMPANY_ID).is(UserContext.getUserContext().getCompanyId()));
        // traceId
        Criteria criteria = Criteria.where(AuditLogConstants.FILED_TRACE_ID).is(traceId);
        query.addCriteria(criteria);
        // 文件
        Criteria fileCriteria = Criteria.where(AuditLogConstants.FILED_FILE_NAME).ne(null);
        query.addCriteria(fileCriteria);
        AuditBusinessLogPO auditBusinessLogPO = mongoTemplate.findOne(query, AuditBusinessLogPO.class, AuditBusinessLogPO.COLLECTION_NAME);
        if (auditBusinessLogPO == null) {
            return null;
        }
        String fileUrl = auditBusinessLogPO.getFileUrl();
        if (StringUtils.isEmpty(fileUrl)){
            // 文件路径不存在
            throw new AuditLogException(AuditLogErrorEnum.AUDIT_LOG_FILE_NOT_FOUND);
        }
        ResponseEntity<byte[]> res = fileServerApiServiceAdapter.downloadFile(auditBusinessLogPO.getFileUrl());
        return res;
    }

    @Override
    public void exportAuditLogExcelData(List<Long> traceIds, Boolean all, HttpServletResponse response) {
        if (!all && CollectionUtils.isEmpty(traceIds)) {
            throw new AuditLogException(AuditLogErrorEnum.EXCEL_EXPORT_IDS_EMPTY);
        }
        exportExcelData(traceIds, all, response);
    }

    @Override
    public void exportAuditLogExcelModel(Boolean all, Long traceId, List<String> modelCodes, HttpServletResponse response) {
        if (!all && CollectionUtils.isEmpty(modelCodes)) {
            throw new AuditLogException(AuditLogErrorEnum.EXCEL_EXPORT_IDS_EMPTY);
        }

        exportExcelDataModel(traceId, modelCodes, all, response);
    }

    @Override
    public ListResult<DataModelPropertyBO> queryImportDataModelProperty(Long traceId, String modelCode, String code) {
        List<DataModelPropertyBO> modelPropertyBOS = new ArrayList<>();
        // 导入导出数据审计日志
        // 判断是否存在公司cid字段，存在，则添加过滤条件
        Query query = Query.query(Criteria.where(AuditLogConstants.FIELD_MODEL_CID).is(UserContext.getUserContext().getCompanyId()));
        //Query query = Query.query(Criteria.where(AuditLogConstants.FILED_TRACE_ID).lte(traceId));
        query.addCriteria(Criteria.where(AuditLogConstants.FILED_TRACE_ID).lte(traceId));
        Pattern patternCode = Pattern.compile(String.format(AuditLogConstants.LIKE_REGEX, code), Pattern.CASE_INSENSITIVE);
        Criteria criteriaCode = Criteria.where(AuditLogConstants.FIELD_MODEL_FORM_CODE).regex(patternCode);
        // id
        if (NumUtils.isInteger(code)){
            Criteria criteriaId = Criteria.where(AuditLogConstants.FIELD_MODEL_ID).is(Long.valueOf(code));
            query.addCriteria(new Criteria().orOperator(criteriaCode, criteriaId));
        }else {
            query.addCriteria(criteriaCode);
        }

        query.limit(2);

        List<AuditDataLogPO> auditDataLogPOS = mongoTemplate.find(query, AuditDataLogPO.class, modelCode);
        if (CollectionUtils.isEmpty(auditDataLogPOS)) {
            log.info("审计数据日志不存在!");
            throw new AuditLogException(AuditLogErrorEnum.AUDIT_DATA_LOG_NOT_EXISTS);
        }

        if (auditDataLogPOS.size() == 1) {
            // 新增
            AuditDataLogPO auditDataLogPO = auditDataLogPOS.get(0);
            Map<String, Object> model = auditDataLogPO.getModel();
            if (model == null){
                log.info("审计模型查找为空!");
                throw new AuditLogException(AuditLogErrorEnum.AUDIT_DATA_MODEL_LOG_FIELD);
            }
            Map<String, String> modelMap = getEcPropertyPOS(modelCode, model.keySet());

            for (String key : model.keySet()) {
                DataModelPropertyBO dataModelPropertyBO = new DataModelPropertyBO();

                // 属性名称
                String displayName = modelMap.get(key);
                if (StringUtils.isEmpty(displayName) || org.springframework.util.ObjectUtils.isEmpty(model.get(key))){
                    continue;
                }
                dataModelPropertyBO.setPropertyName(displayName);
                // 按类型 todo
                dataModelPropertyBO.setCurrentValue(model.get(key) != null ? model.get(key).toString() : "");
                dataModelPropertyBO.setHistoryValue("");

                modelPropertyBOS.add(dataModelPropertyBO);
            }
        } else if (auditDataLogPOS.size() == 2) {
            // 修改
            // 当前值
            AuditDataLogPO auditDataLogPOCur = auditDataLogPOS.get(0);
            // 历史值
            AuditDataLogPO auditDataLogPOHis = auditDataLogPOS.get(1);
            Map<String, Object> modelCur = auditDataLogPOCur.getModel();
            Map<String, Object> modelHis = auditDataLogPOHis.getModel();

            Set<String> keySets = new HashSet<>();
            Set<String> curSet = modelCur.keySet();
            Set<String> hisSet = modelHis.keySet();
            keySets.addAll(curSet);
            keySets.addAll(hisSet);

            Map<String, String> modelMap = getEcPropertyPOS(modelCode, keySets);
            for (String key : keySets) {
                DataModelPropertyBO dataModelPropertyBO = new DataModelPropertyBO();
                // 属性名称
                String displayName = modelMap.get(key);
                if (StringUtils.isEmpty(displayName)
                        || (org.springframework.util.ObjectUtils.isEmpty(modelCur.get(key)) && org.springframework.util.ObjectUtils.isEmpty(modelHis.get(key)))){
                    continue;
                }
                if (ObjectUtils.notEqual(modelCur.get(key), modelHis.get(key))) {
                    dataModelPropertyBO.setPropertyName(displayName);
                    dataModelPropertyBO.setCurrentValue(modelCur.get(key) != null ? modelCur.get(key).toString() : "");
                    dataModelPropertyBO.setHistoryValue(modelHis.get(key) != null ? modelHis.get(key).toString() : "");

                    modelPropertyBOS.add(dataModelPropertyBO);
                }
            }
        }

        return new ListResult<>(modelPropertyBOS);
    }

    /**
     * 获取i18n的key、value
     * @param modelCode
     * @param model
     * @return
     */
    private Map<String, String> getEcPropertyPOS(String modelCode, Set<String> model) {
        // 拼接ecProperty的code
        List<String> propertyCodeList = model.stream().map(key -> modelCode + "_" + key).collect(Collectors.toList());
        QueryWrapper<EcPropertyPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("code", propertyCodeList);
        List<EcPropertyPO> ecPropertyPOList = ecPropertyMapper.selectList(queryWrapper);

        Map<String, String> modelMap = new HashMap();
        // i18n查询
        for (EcPropertyPO ecPropertyPO : ecPropertyPOList) {
            // 国际化key
            String displayName = ecPropertyPO.getDisplayName();
            String codeKey = ecPropertyPO.getCode();
            if (StringUtils.isEmpty(codeKey)){
                continue;
            }
            String key = codeKey.split("_")[4];
            String messageVal = messageResourceWrapper.getMessageNotBlank(displayName);
             //String messageVal = i18nServiceAdapter.searchValue(displayName);
            modelMap.put(key, messageVal);
        }
        return modelMap;
    }

    @Override
    public PageResult<DataModelBO> queryImportDataModels(Integer current, Integer pageSize, Long traceId, DataModelQueryBO dataModelQueryBO) {
        // debug时CompanyId为空
        Query auditBusinessQuery = Query.query(Criteria.where(AuditLogConstants.FIELD_COMPANY_ID).is(UserContext.getUserContext().getCompanyId()))
                .limit(1)
                .addCriteria(Criteria.where(AuditLogConstants.FILED_TRACE_ID).is(traceId));
        //Query auditBusinessQuery = new Query();
        //auditBusinessQuery.addCriteria(Criteria.where(AuditLogConstants.FILED_TRACE_ID).is(traceId));
        AuditBusinessLogPO auditBusinessLog = mongoTemplate.findOne(auditBusinessQuery, AuditBusinessLogPO.class, AuditBusinessLogPO.COLLECTION_NAME);
        if (auditBusinessLog == null || auditBusinessLog.getModelObjects() == null || auditBusinessLog.getModelObjects().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, pageSize, current);
        }
        // 模块编码
        String modelCode = auditBusinessLog.getModelObjects().get(0).getModelCode();
        Set<String> modelCodes = auditBusinessLog.getModelObjects().stream().map(ModelObjectInfo::getModelCode).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(modelCodes)){
            throw new AuditLogException(AuditLogErrorEnum.AUDIT_DATA_MODEL_LOG_FIELD);
        }
        // 拼接条件
        Query query = buildDataModelQuery(traceId, dataModelQueryBO);
        long count = mongoTemplate.count(query, AuditDataLogPO.class, modelCode);
        // 分页
        if (count == 0) {
            return new PageResult<>(Collections.emptyList(), count, pageSize, current);
        }
        query = buildImportDataLogQuery(query, current, pageSize);

        List<AuditDataLogPO> auditDataLogPOS = mongoTemplate.find(query, AuditDataLogPO.class, modelCode);
        List<DataModelBO> dataModels = new LinkedList<>();
        for (AuditDataLogPO auditDataLog : auditDataLogPOS) {
            DataModelBO dataModelBO = new DataModelBO();
            BeanUtils.copyProperties(auditDataLog, dataModelBO);
            // Long转String
            dataModelBO.setTraceId(auditBusinessLog.getTraceId().toString());
            dataModelBO.setOperateTime(DateTimeExtraUtils.formatUTC0(auditDataLog.getOperateTime()));
            dataModelBO.setEntityName(messageResourceWrapper.getMessageNotBlank(auditDataLog.getEntityName()));
            dataModelBO.setOperateType(systemCodeServiceAdapter.getSystemCode(AuditLogConstants.SYSTEM_OPERATE_TYPE, auditDataLog.getOperateType()));
            Map<String, Object> model = auditDataLog.getModel();

            if (!CollectionUtils.isEmpty(model)) {
                dataModelBO.setModelObjName(messageResourceWrapper.getMessageNotBlank(auditDataLog.getModelName()));
                // code > id
                dataModelBO.setModelObjCode(getCode(model) != null ? getCode(model) : getPk(model));
            }
            dataModels.add(dataModelBO);
        }
        return new PageResult<>(dataModels, count, pageSize, current);
    }

    private Query buildImportDataLogQuery(Query query, Integer current, Integer pageSize) {
        //query.addCriteria(Criteria.where(AuditLogConstants.FIELD_COMPANY_ID).is(UserContext.getUserContext().getCompanyId()))
        //        // 分页
        //        .with(PageRequest.of(current - 1, pageSize));
        query.with(PageRequest.of(current - 1, pageSize));
        return query;
    }

    @Override
    public String enableAuditLog(Map<String, String> modelCodes, Boolean enable) {
        // 根据modelCode获取moduleCode
        if (enable){
            SnowFlakeIdWorker snowFlakeIdWorker = SnowFlakeIdWorker.getInstance();
            // 插入auditlog_model表数据
            for (String modelCode : modelCodes.keySet()){
                long id = snowFlakeIdWorker.nextId();
                AuditlogModelPO auditlogModelPO = new AuditlogModelPO();
                auditlogModelPO.setId(id);
                auditlogModelPO.setModelCode(modelCode);
                auditlogModelPO.setModuleCode(modelCodes.get(modelCode));
                auditlogModelMapper.insert(auditlogModelPO);
            }
        }else {
            // 删除数据
            QueryWrapper<AuditlogModelPO> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.in("model_code", modelCodes.keySet());
            auditlogModelMapper.delete(deleteWrapper);
        }
        // 修改模块MD5，通知模块刷新本地缓存
        List<String> moduleCodeList = modelCodes.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
        updateModuleMD5(moduleCodeList);
        return "success";
    }

    /**
     * 修改模块MD5，通知模块刷新本地缓存
     * @param moduleCodes
     */
    private String updateModuleMD5(List<String> moduleCodes) {
        try {
            Properties props = new Properties();
            ConfigService configService = NacosFactory.createConfigService(serverAddress);
            String content = configService.getConfig(this.baseServiceProperties, this.group, 10000L);
            if (!StringUtils.isEmpty(content)) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
                props.load(inputStream);
            }

            moduleCodes.forEach((moduleCode) -> {
                Long currentTime = System.currentTimeMillis();
                String md5 = MD5.getInstance().getMD5String(this.serverAddress + moduleCode + currentTime);
                props.put("ec." + moduleCode, md5);
            });
            String newContent = PropertiesUtils.convertToString(props);
            configService.publishConfig(this.baseServiceProperties, this.group, newContent);
            return newContent;
        } catch (IOException | NacosException var5) {
            log.error(var5.getMessage(), var5);
            return null;
        }
    }

    /**
     * 导出审计日志模型
     *
     * @param traceId
     * @param modelCodes
     * @param all
     */
    private void exportExcelDataModel(Long traceId, List<String> modelCodes, Boolean all, HttpServletResponse response) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        //ExcelUtils.createExplainSheet(ExcelUtils.AUDITLOG_TEMPLATE_EXPLAIN, workbook);
        Sheet sheet = workbook.createSheet(AuditLogConstants.AUDIT_LOG_DATA_SHEETNAME);
        // 创建表标题栏
        ExcelUtils.createHeadComments(sheet, ExcelUtils.AUDITLOG_MODEL_IMPORT_TEMPLATE);

        // 查日志数据
        Query logQuery = new Query();
        // 过滤公司
        logQuery.addCriteria(Criteria.where(AuditLogConstants.FIELD_COMPANY_ID).is(UserContext.getUserContext().getCompanyId()));

        logQuery.addCriteria(Criteria.where("traceId").is(traceId));
        AuditBusinessLogPO auditBussLogPO = mongoTemplate.findOne(logQuery, AuditBusinessLogPO.class, AuditBusinessLogPO.COLLECTION_NAME);
        if (auditBussLogPO == null) {
            throw new AuditLogException(AuditLogErrorEnum.AUDIT_BUSSINESS_LOG_NOT_EXISTS);
        }
        List<ModelObjectInfo> modelInfoList = auditBussLogPO.getModelObjects();
        if (CollectionUtils.isEmpty(modelInfoList)) {
            throw new AuditLogException(AuditLogErrorEnum.AUDIT_DATA_LOG_NOT_EXISTS);
        }

        // 查部分模型
        if (!all) {
            modelInfoList = modelInfoList.stream().filter(model -> modelCodes.contains(model.getModelCode()))
                    .collect(Collectors.toList());
        }

        List<DataAuditLogModelBO> dataAuditLogModelBOList = new ArrayList<>();
        for (int i = 0; i < modelInfoList.size(); i++) {
            ModelObjectInfo modelObjectInfo = modelInfoList.get(i);
            DataAuditLogModelBO dataAuditLogModelBO = new DataAuditLogModelBO();
            // 表单名称
            dataAuditLogModelBO.setFormName(messageResourceWrapper.getMessageNotBlank(modelObjectInfo.getEntityName()));
            dataAuditLogModelBO.setModelObjName(messageResourceWrapper.getMessageNotBlank(modelObjectInfo.getModelName()));
            dataAuditLogModelBO.setModelObjCode(modelObjectInfo.getModelObjCode() != null ? modelObjectInfo.getModelObjCode() : modelObjectInfo.getModelObjPk());

            // 操作类型 系统编码
            SystemCodeResultDTO operateType = systemCodeServiceAdapter.getSystemCode(AuditLogConstants.SYSTEM_OPERATE_TYPE, auditBussLogPO.getOperateType());
            dataAuditLogModelBO.setOperateType(operateType);
            dataAuditLogModelBO.setDescription(auditBussLogPO.getDescription());

            dataAuditLogModelBOList.add(dataAuditLogModelBO);
        }
        // 表格插数据
        int rowIndex = 1;
        for (int i = 0; i < dataAuditLogModelBOList.size(); i++) {
            Row row = sheet.createRow(rowIndex);
            createModelCell(row, dataAuditLogModelBOList.get(i));
            rowIndex++;
        }

        try {
            ExcelUtils.createExportFile(workbook, ExcelUtils.AUDITLOG_MODEL_FILE, response);
            //log.info("创建日志导出文件路径：{}", filePath);
            return;
        } catch (IOException e) {
            log.info("创建日志导出文件异常：{}", e.getMessage());
            return;
        }
    }

    private void createModelCell(Row curRow, DataAuditLogModelBO audit) {
        curRow.createCell(0, CellType.STRING).setCellValue(audit.getFormName());
        curRow.createCell(1, CellType.STRING).setCellValue(audit.getModelObjName());
        curRow.createCell(2, CellType.STRING).setCellValue(audit.getModelObjCode());
        curRow.createCell(3, CellType.STRING).setCellValue(audit.getOperateType() != null ? audit.getOperateType().getDisplayName() : "");
        curRow.createCell(4, CellType.STRING).setCellValue(audit.getDescription());
    }

    /**
     * 导出审计日志数据
     *
     * @param traceIds
     * @param all
     */
    private void exportExcelData(List<Long> traceIds, Boolean all, HttpServletResponse response) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        // 创建说明页
        //ExcelUtils.createExplainSheet(ExcelUtils.AUDITLOG_TEMPLATE_EXPLAIN, workbook);
        Sheet sheet = workbook.createSheet(AuditLogConstants.AUDIT_LOG_DATA_MODEL_SHEETNAME);
        // 创建表标题栏
        ExcelUtils.createHeadComments(sheet, ExcelUtils.AUDITLOG_IMPORT_TEMPLATE);

        // 查日志数据
        Query logQuery = new Query();
        // 过滤公司
        logQuery.addCriteria(Criteria.where(AuditLogConstants.FIELD_COMPANY_ID).is(UserContext.getUserContext().getCompanyId()));

        // 查部分
        if (!all) {
            logQuery.addCriteria(Criteria.where("traceId").in(traceIds));
        }
        List<AuditBusinessLogPO> auditlLogs = mongoTemplate.find(logQuery, AuditBusinessLogPO.class, AuditBusinessLogPO.COLLECTION_NAME);

        if (CollectionUtils.isEmpty(auditlLogs)) {
            try {
                ExcelUtils.createExportFile(workbook, ExcelUtils.AUDITLOG_FILE, response);
                return;
            } catch (IOException e) {
                log.warn("创建日志导出文件异常：{}", e.getMessage());
                return;
            }
        }

        List<DataAuditLogBO> dataAuditLogBOList = new ArrayList<>();
        for (int i = 0; i < auditlLogs.size(); i++) {
            AuditBusinessLogPO auditBusinessLogPO = auditlLogs.get(i);
            List<ModelObjectInfo> modelObjects = auditBusinessLogPO.getModelObjects();

            // 导入、导出
            if (CollectionUtils.isEmpty(modelObjects)) {
                DataAuditLogBO dataAuditLogBO = new DataAuditLogBO();
                BeanUtils.copyProperties(auditBusinessLogPO, dataAuditLogBO);
                // 操作时间 时间戳转字符串，设置时间格式
                dataAuditLogBO.setOperateTime(DateTimeExtraUtils.timeToString(auditBusinessLogPO.getOperateTime()));
                dataAuditLogBO.setModuleName(messageResourceWrapper.getMessageNotBlank(auditBusinessLogPO.getModuleName()));
                SystemCodeResultDTO operateType = systemCodeServiceAdapter.getSystemCode(AuditLogConstants.SYSTEM_OPERATE_TYPE, auditBusinessLogPO.getOperateType());
                dataAuditLogBO.setOperateType(operateType);
                // 操作描述
                dataAuditLogBO.setDescription(getDescription(auditBusinessLogPO.getDescription(), operateType.getName(), null, auditBusinessLogPO.getSuccess()));
                dataAuditLogBOList.add(dataAuditLogBO);
                continue;
            }

            for (int j = 0; j < modelObjects.size(); j++) {
                DataAuditLogBO dataAuditLogBO = new DataAuditLogBO();
                BeanUtils.copyProperties(auditBusinessLogPO, dataAuditLogBO);
                // 操作时间 时间戳转字符串
                dataAuditLogBO.setOperateTime(DateTimeExtraUtils.timeToString(auditBusinessLogPO.getOperateTime()));
                dataAuditLogBO.setModuleName(messageResourceWrapper.getMessageNotBlank(auditBusinessLogPO.getModuleName()));
                SystemCodeResultDTO operateType = systemCodeServiceAdapter.getSystemCode(AuditLogConstants.SYSTEM_OPERATE_TYPE, auditBusinessLogPO.getOperateType());
                dataAuditLogBO.setOperateType(operateType);
                // 表单名称
                dataAuditLogBO.setFormName(messageResourceWrapper.getMessageNotBlank(modelObjects.get(j).getEntityName()));
                dataAuditLogBO.setModelObjCode(modelObjects.get(j).getModelObjCode() != null ? modelObjects.get(j).getModelObjCode() : modelObjects.get(j).getModelObjPk());
                dataAuditLogBO.setModelObjName(messageResourceWrapper.getMessageNotBlank(modelObjects.get(j).getModelName()));
                // 描述
                dataAuditLogBO.setDescription(getDescription(auditBusinessLogPO.getDescription(), operateType.getName(), modelObjects.get(j).getModelName(), auditBusinessLogPO.getSuccess()));

                dataAuditLogBOList.add(dataAuditLogBO);
            }
        }
        // 表格插数据
        int rowIndex = 1;
        for (int i = 0; i < dataAuditLogBOList.size(); i++) {
            Row row = sheet.createRow(rowIndex);
            createCell(row, dataAuditLogBOList.get(i));
            rowIndex++;
        }

        try {
            ExcelUtils.createExportFile(workbook, ExcelUtils.AUDITLOG_FILE, response);
            return;
        } catch (IOException e) {
            log.info("创建日志导出文件异常：{}", e.getMessage());
            return;
        }
    }

    private void createCell(Row curRow, DataAuditLogBO audit) {
        curRow.createCell(0, CellType.STRING).setCellValue(audit.getModuleName());
        curRow.createCell(1, CellType.STRING).setCellValue(audit.getFormName());
        curRow.createCell(2, CellType.STRING).setCellValue(audit.getOperateUserName());
        curRow.createCell(3, CellType.STRING).setCellValue(audit.getOperateTime());
        curRow.createCell(4, CellType.STRING).setCellValue(audit.getModelObjName());
        curRow.createCell(5, CellType.STRING).setCellValue(audit.getModelObjCode());
        // 操作类型
        curRow.createCell(6, CellType.STRING).setCellValue(audit.getOperateType() != null ? audit.getOperateType().getDisplayName() : "");
        curRow.createCell(7, CellType.STRING).setCellValue(audit.getIpAddress());
        curRow.createCell(8, CellType.STRING).setCellValue(audit.getDescription());
        curRow.createCell(9, CellType.STRING).setCellValue(audit.getExceptionDescription());
        curRow.createCell(10, CellType.STRING).setCellValue(audit.getFileName());
    }

    /**
     * 根据审计业务日志获取描述
     *
     * @param oldDesc         已存在的描述
     * @param operateTypeName 操作名称
     * @param modelName       模型名称
     * @param success         是否成功
     * @return 描述
     */
    private String getDescription(String oldDesc, String operateTypeName, String modelName, Boolean success) {
        String description = null;
        if (StringUtils.isNotBlank(oldDesc)) {
            description = messageResourceWrapper.getMessageNotBlank(oldDesc);
        } else {
            description = messageResourceWrapper.getMessageNotBlank(operateTypeName) + (modelName == null ? "" : messageResourceWrapper.getMessageNotBlank(modelName));
        }
        if (!success) {
            String failureMessage = messageResourceWrapper.getMessageNotBlank(AuditLogConstants.AUDIT_LOG_FAILURE);
            return description + String.format(AuditLogConstants.DISPLAY_FAILURE, failureMessage);
        }
        return description;
    }

    private Query buildAuditLogQuery(Integer current, Integer pageSize, DataAuditLogQueryBO dataAuditLogQuery) {
        // debug时获取的companyId为空
        Query query = new Query();
        if (null != UserContext.getUserContext() && null != UserContext.getUserContext().getCompanyId()) {
            query.addCriteria(Criteria.where(AuditLogConstants.FIELD_COMPANY_ID).is(UserContext.getUserContext().getCompanyId()));
        }
        // 分页
        query.with(PageRequest.of(current - 1, pageSize));
        // 所属模块名称
        String moduleName = dataAuditLogQuery.getModuleName();
        if (StringUtils.isNotBlank(moduleName)) {
            // todo 接口不可用
            List<String> moduleNameKeys = i18nServiceAdapter.searchKeys(moduleName);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_MODULE_NAME).in(moduleNameKeys);
            query.addCriteria(criteria);
        }
        // 操作用户名
        List<String> userNames = dataAuditLogQuery.getUserNames();
        if (userNames != null && !userNames.isEmpty()) {
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_OPERATE_USER_NAME).in(userNames);
            query.addCriteria(criteria);
        }
        // 操作开始时间
        String operateStartTime = dataAuditLogQuery.getOperateStartTime();
        // 操作结束时间
        String operateEndTime = dataAuditLogQuery.getOperateEndTime();
        // mongo查询同一字段多个约束需要用andOperator
        if (StringUtils.isNotBlank(operateStartTime) && StringUtils.isNotBlank(operateEndTime)) {
            long startTime = Long.parseLong(operateStartTime);
            long endTime = Long.parseLong(operateEndTime);

            Criteria startCriteria = Criteria.where(AuditLogConstants.FIELD_OPERATE_TIME).gte(startTime);
            Criteria endCriteria = Criteria.where(AuditLogConstants.FIELD_OPERATE_TIME).lte(endTime);

            query.addCriteria(new Criteria().andOperator(startCriteria, endCriteria));
        }else if (StringUtils.isNotBlank(operateEndTime)) {
            long endTime = Long.parseLong(operateEndTime);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_OPERATE_TIME).lte(endTime);
            query.addCriteria(criteria);
        }else if (StringUtils.isNotBlank(operateStartTime)){
            long startTime = Long.parseLong(operateStartTime);
            Criteria startCriteria = Criteria.where(AuditLogConstants.FIELD_OPERATE_TIME).gte(startTime);
            query.addCriteria(startCriteria);
        }
        // 表单名称
        String formName = dataAuditLogQuery.getFormName();
        if (StringUtils.isNotBlank(formName)) {
            List<String> entityNameKeys = i18nServiceAdapter.searchKeys(formName);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_MODEL_OBJECTS_ENTITY_NAME).in(entityNameKeys)
                    .orOperator(Criteria.where(AuditLogConstants.FIELD_MODEL_OBJECTS_MODEL_NAME).in(entityNameKeys));
            query.addCriteria(criteria);
        }
        // 被操作对象编码
        String modelObjCode = dataAuditLogQuery.getModelObjCode();
        if (StringUtils.isNotBlank(modelObjCode)) {
            Pattern pattern = Pattern.compile(String.format(AuditLogConstants.LIKE_REGEX, modelObjCode), Pattern.CASE_INSENSITIVE);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_MODEL_OBJECTS_MODEL_OBJ_CODE).regex(pattern);
            if (NumUtils.isInteger(modelObjCode)){
                Criteria criteriaId = Criteria.where(AuditLogConstants.FIELD_MODEL_ID).is(Long.valueOf(modelObjCode));
                query.addCriteria(new Criteria().orOperator(criteria, criteriaId));
            }else {
                query.addCriteria(criteria);
            }
        }
        // 被操作对象名称
        String modelObjName = dataAuditLogQuery.getModelObjName();
        if (StringUtils.isNotBlank(modelObjName)) {
            Pattern pattern = Pattern.compile(String.format(AuditLogConstants.LIKE_REGEX, modelObjName), Pattern.CASE_INSENSITIVE);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_MODEL_OBJECTS_MODEL_OBJ_NAME).regex(pattern);
            query.addCriteria(criteria);
        }
        // 操作类型
        List<String> operateTypes = dataAuditLogQuery.getOperateTypes();
        if (operateTypes != null && !operateTypes.isEmpty()) {
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_OPERATE_TYPE).in(operateTypes);
            query.addCriteria(criteria);
        }
        // IP地址
        String ipAddress = dataAuditLogQuery.getIpAddress();
        if (StringUtils.isNotBlank(ipAddress)) {
            Pattern pattern = Pattern.compile(String.format(AuditLogConstants.LIKE_REGEX, ipAddress), Pattern.CASE_INSENSITIVE);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_IP_ADDRESS).regex(pattern);
            query.addCriteria(criteria);
        }
        // 操作描述
        String operateDesc = dataAuditLogQuery.getOperateDesc();
        if (StringUtils.isNotBlank(operateDesc)) {
            Pattern pattern = Pattern.compile(String.format(AuditLogConstants.LIKE_REGEX, operateDesc), Pattern.CASE_INSENSITIVE);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_DESCRIPTION).regex(pattern);
            query.addCriteria(criteria);
        }
        // 操作异常描述
        String operateErrorDesc = dataAuditLogQuery.getOperateErrorDesc();
        if (StringUtils.isNotBlank(operateErrorDesc)) {
            Pattern pattern = Pattern.compile(String.format(AuditLogConstants.LIKE_REGEX, operateErrorDesc), Pattern.CASE_INSENSITIVE);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_EXCEPTION_DESCRIPTION).regex(pattern);
            query.addCriteria(criteria);
        }
        // 排序
        String sortKey = dataAuditLogQuery.getSortKey();
        Boolean desc = dataAuditLogQuery.getDesc();
        if (StringUtils.isNotEmpty(sortKey) && !org.springframework.util.ObjectUtils.isEmpty(desc)){
            query.with(Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, sortKey));
        }else {
            // 默认根据traceId降序排序
            query.with(Sort.by(Sort.Direction.DESC, AuditLogConstants.FILED_TRACE_ID));
        }

        return query;
    }

    private Query buildDataModelQuery(Long traceId, DataModelQueryBO dataModelQuery) {
        Query query = Query.query(Criteria.where(AuditLogConstants.FILED_TRACE_ID).is(traceId))
                // 根据操作时间升序排序
                .with(Sort.by(Sort.Direction.ASC, AuditLogConstants.FILED_OPERATE_TIME));
        // 被操作对象编码
        String modelObjCode = dataModelQuery.getModelObjCode();
        if (StringUtils.isNotBlank(modelObjCode)) {
            Pattern pattern = Pattern.compile(String.format(AuditLogConstants.LIKE_REGEX, modelObjCode), Pattern.CASE_INSENSITIVE);
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_MODEL_FORM_CODE).regex(pattern);
            if (NumUtils.isInteger(modelObjCode)){
                Criteria criteriaId = Criteria.where(AuditLogConstants.FIELD_MODEL_ID).is(Long.valueOf(modelObjCode));
                query.addCriteria(new Criteria().orOperator(criteria, criteriaId));
            }else {
                query.addCriteria(criteria);
            }
        }
        // 被操作对象名称
        String modelObjName = dataModelQuery.getModelObjName();
        if (StringUtils.isNotBlank(modelObjName)) {
            Pattern pattern = Pattern.compile(String.format(AuditLogConstants.LIKE_REGEX, modelObjName), Pattern.CASE_INSENSITIVE);
            //Criteria criteria = Criteria.where(AuditLogConstants.FIELD_MODEL_NAME).regex(pattern).orOperator(Criteria.where(AuditLogConstants.FIELD_MODEL_ID).regex(pattern));
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_MODEL_NAME).regex(pattern);
            query.addCriteria(criteria);
        }
        // 操作类型
        List<String> operateTypes = dataModelQuery.getOperateTypes();
        if (operateTypes != null && !operateTypes.isEmpty()) {
            Criteria criteria = Criteria.where(AuditLogConstants.FIELD_OPERATE_TYPE).in(operateTypes);
            query.addCriteria(criteria);
        }
        return query;
    }

    private String getPk(Map<String, Object> model) {
        String code = getCode(model);
        if (code != null) {
            return code;
        }
        Long id = (Long) model.get(AuditLogConstants.FIELD_ID);
        if (id != null) {
            return id.toString();
        }
        return null;
    }

    private String getCode(Map<String, Object> model) {
        return (String) model.get(AuditLogConstants.FIELD_CODE);
    }

    private String getName(Map<String, Object> model) {
        return (String) model.getOrDefault(AuditLogConstants.FIELD_NAME, getPk(model));
    }
}
