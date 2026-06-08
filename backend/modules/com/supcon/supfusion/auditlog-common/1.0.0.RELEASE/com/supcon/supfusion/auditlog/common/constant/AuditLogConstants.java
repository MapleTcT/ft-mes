package com.supcon.supfusion.auditlog.common.constant;

/**
 * 审计日志常量类
 * @author caokele
 */
public interface AuditLogConstants {
    /**
     * 服务名
     */
    String SERVER_NAME = "auditlog";

    String SYSTEM_OPERATE_TYPE = "sys_operate_type";

    /**
     * 匹配首尾为字符的字符串
     */
    String LIKE_REGEX = "^.*%s.*$";
    //String LIKE_REGEX = "^.*$";

    String AUDIT_LOG_FAILURE = "auditlog.failure";

    String DISPLAY_FAILURE = "(%s)";

    /**
     * 字段名称
     */
    String FILED_TRACE_ID = "traceId";
    String FILED_OPERATE_TIME = "operateTime";
    String FIELD_NAME = "name";
    String FIELD_ID = "id";
    String FIELD_CODE = "code";
    String FIELD_COMPANY_ID = "companyId";
    String FIELD_MODULE_NAME = "moduleName";
    String FIELD_OPERATE_USER_NAME = "operateUserName";
    String FIELD_OPERATE_TIME = "operateTime";
    String FIELD_MODEL_OBJECTS_ENTITY_NAME = "modelObjects.entityName";
    String FIELD_MODEL_OBJECTS_MODEL_NAME = "modelObjects.modelName";
    String FIELD_MODEL_OBJECTS_MODEL_OBJ_CODE = "modelObjects.modelObjCode";
    String FIELD_MODEL_OBJECTS_MODEL_OBJ_NAME = "modelObjects.modelObjName";
    String FIELD_OPERATE_TYPE = "operateType";
    String FIELD_IP_ADDRESS = "ipAddress";
    String FIELD_DESCRIPTION = "description";
    String FIELD_EXCEPTION_DESCRIPTION = "exceptionDescription";
    String FIELD_MODEL_FORM_CODE = "model.code";
    String FIELD_MODEL_CODE = "modelCode";
    String FIELD_MODEL_NAME = "model.name";
    String FIELD_MODEL_ID = "model.id";
    String FIELD_MODEL_CID = "model.cid";
    String FILED_FILE_NAME = "fileName";
    String FILED_FILE_URL = "fileUrl";

    /**
     * 操作类型
     */
    String OPERATE_IMPORT = "import";

    /**
     * 错误消息
     */
    public static final String PAGE_CURRENT_ERROR = "分页页码不可以小于１!";
    public static final String PAGE_PAGESIZE_ERROR = "每页条数不可以小于１!";
    public static final String PAGE_PAGESIZE_MAX_ERROR = "每页条数不可以大于500!";

    //-------------------------Excel-----------------
    public static final String AUDIT_LOG_DATA_SHEETNAME = "审计日志信息";
    public static final String AUDIT_LOG_DATA_MODEL_SHEETNAME = "审计日志数据模型";
    public static final String EXCEL_IMPORT_SUCESS = "导入成功";
}
