/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.util;

/**
 * @author: zhuangmh
 * @date: 2020年5月20日 上午11:11:29
 */
public class Constants {
    
    private Constants() {
        throw new IllegalStateException("Constants is utility class, do not instantiate");
    }
    
    /**
     * ##############################################################################################
     * Openapi请求参数
     * ##############################################################################################
     */
    public static final String PARAMETER_USER_ID = "userId";
    public static final String PARAMETER_USER_NAME = "username";
    
    /**
     * ##############################################################################################
     * BPMN XML 属性键
     * ##############################################################################################
     */
    public static final String ID = "id"; // 
    public static final String NAME = "name"; // 名称
    public static final String VALUE = "value"; // 
    public static final String START_PERMISSION = "permission"; // 启动权限
    public static final String MOBILE_START = "startOnMobile"; // 移动端启动
    public static final String PROCESS_START = "startUser"; // 流程启动者
    public static final String START_INITIATOR = "${starter}"; // 发起者变量
    public static final String POSITION_RESTRICT = "posRestrict"; // 岗位限制
    public static final String GROUP_RESTRICT = "groupRestrict"; // 组限制
    public static final String POSITION = "position"; // 指定岗位限制
    public static final String PERSON = "person"; // 指定人员限制
    public static final String UNRESTRICT = "unRestrict"; // 无限制
    public static final String TAG_ASSIGNEERULE = "assigneeRule"; // 指派者规则
    public static final String TAG_OODM = "oodm"; // oodm标签
    public static final String SEQUENCE_ORDER = "order"; // 分支顺序
    public static final String REJECT_SEQUENCE = "rejectToSubmitter"; // 输出线类型
    public static final String ENABLE_REVOCATION = "enableRevocation"; // 撤回
    public static final String ASSIGN = "reassign"; // 重新指派
    public static final String PAGE_URL = "url"; // 关联页面url
    public static final String READONLY = "readonly"; // 页面是否只读
    public static final String ENABLE_COMMENT = "enableComment"; // 是否开启备注 
    public static final String ENABLE_DELETE = "enableDelete"; // 是否可删除
    public static final String ENABLE_SHOWLOG = "showlog"; // 是否展示流程日志
    public static final String ENABLE_ADDINSTANCE = "addInstance"; // 是否开启加签 
    public static final String CANDIDATE_USERS = "candidateUsers"; // 候选人变量
    public static final String CANDIDATE_GROUPS = "candidateGroups"; // 候选人变量
    public static final String PROCESS_INITIATOR = "startUser"; // 流程启动者变量
    public static final String PROCESS_INITIATOR_NAME = "startUserName"; // 流程启动者变量
    public static final String FLOWABLE_NAMESPACE_DEF = "http://flowable.org/bpmn"; // bpmn xml属性定义的命名空间
    public static final String OUTPUT_CONDITION_VARIABLE = "flowableAudit"; // 输出分支条件变量
    public static final String CANDIDATE = "candidate"; // 用户类型是candidate
    public static final String NOTIFICATION_KEY = "notificationType"; // 通知变量
    public static final String MULTIPLE_INSTANCE = "multipleInstance"; // 会签任务标识
    public static final String AUTOTASK_ASYNC = "async"; // 异步任务
    // OODM变量设置
    public static final String OODM_TEMPLATE_NAMESPACE = "templateNamespace";
    public static final String OODM_TEMPLATE_NAME = "templateName";
    public static final String OODM_INSTANCE_NAME = "instanceName";
    public static final String OODM_SERVICE_NAMESPACE = "serviceNamespace";
    public static final String OODM_SERVICE_NAME = "serviceName";
    public static final String ACTIVITY_TYPE_USERTASK = "userTask";
    public static final String ACTIVITY_TYPE_PARALLELGATEWAY = "parallelGateway";
    /**
     * ##############################################################################################
     * 组织架构常量设置
     * ##############################################################################################
     */
    public static final String GROUP_ROLE = "role";
    public static final String GROUP_ORGANIZATION = "organization";
    public static final String GROUP_DEPARTMENT = "Department";
    public static final String GROUP_POSITION = "Position";
    public static final String PERSON_PREFIX = "person_";
    public static final String ROLE_PREFIX = "role_";
    public static final String POS_PREFIX = "pos_";
    public static final String DEPT_PREFIX = "dept_";
    public static final int ORG_TYPE_DEPT = 0; // 部门类型
    public static final int ORG_TYPE_POS = 1; // 岗位类型
    public static final int ORG_LEVEL_DIRECT = 0; // 直属领导
    public static final int ORG_LEVEL_SUPERIOR = 1; // 隔级领导
    
    /**
     * ##############################################################################################
     * 通用常量设置
     * ##############################################################################################
     */
    public static final int REJECT = 1; // 驳回
    public static final int ENABLED = 1; // 开启
    public static final int DISABLED = 0; // 关闭
    public static final int VALID = 1; // 正在使用
    public static final int INVALID = 0; // 软删标志
    public static final int DEFAULT_PAGE = 1; // 默认从第一页开始
    public static final int DEFAULT_PAGE_SIZE = 10; // 默认10条
    public static final int MAX_PAGE_SIZE = 500; // 最大条数
    public static final String SPLIT_COMMA = ","; // 逗号分隔符
    public static final String SPLIT_JINHAO = "#"; // #分隔符
    public static final String PERCENTAGE = "%"; // 百分比
    public static final String UNDERLINE = "_"; // 下划线
    public static final int SUCCESS_CODE = 200; // 
    public static final int SYSTEM_ERROR_CODE = 500; // 
    /**
     * ##############################################################################################
     * 数据库字段设置
     * ##############################################################################################
     */
    public static final String COL_APPID = "appId"; // 
    public static final String COL_VALID = "valid"; // 删除标志位
    public static final String COL_DIAGRAM_CODE = "process_key"; // 流程编号
    public static final String COL_VERSION = "process_version"; // 流程版本号
    public static final String COL_DIAGRAM_NAME = "process_name"; // 流程名称
    public static final String COL_COMPANY_ID = "cid"; // 公司ID
    public static final String COL_DIAGRAM_STATUS = "process_status"; // 流程状态
    public static final String COL_USER_ID = "user_id"; // 用户ID
    public static final String COL_TASK_ID = "task_id"; // 待办ID
    public static final String COL_INSTANCE_ID = "instance_id"; // 任务实例ID
    public static final String COL_PROCESS_ID = "process_id"; // 流程ID
    public static final String COL_INITIATOR_ID = "initiator_id"; // 流程发起者ID
    
    /**
     * ##############################################################################################
     * 编码设置
     * ##############################################################################################
     */
    public static final String ENCODE_UTF8 = "utf-8";
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    
    /**
     * ##############################################################################################
     * 运行时变量
     * ##############################################################################################
     */
    public static final String CODE = "code"; // 
    public static final String MESSAGE = "message"; // 
    public static final String RESULT = "result"; // 
    public static final String TASK_ID = "taskId"; // 待办ID
    public static final String FORM_NO = "formNo"; // 单据编号
    public static final String FLOW_DATA = "flowData"; // 工作流数据
    public static final String AUDIT_DATA = "auditData"; // 审批数据
    public static final String FORM_DATA = "data"; // 表单数据(老数据兼容)
    public static final String PROCESS_NAME = "processName"; // 流程名称
    public static final String SKIP_EXPRESSION_ENABLED_VARIABLE = "_ACTIVITI_SKIP_EXPRESSION_ENABLED"; // 是否开启跳过人工任务
    public static final String AUDIT_PATTERN = "flowableAudit\\s*\\=\\=\\s*((\\-?)\\d+)"; // 审批分支正则
    public static final String AUDIT_VARIABLE = "flowableAudit"; // 审批变量
    public static final String PROCESS_INITIATOR_VARIABLE = "${startUser}"; // 流程发起者变量
    public static final String CANDIDATE_USER_PREFIX = "user_";
    public static final String APP_FOLDER_PATH = "/tmp/flow/";
    public static final String APP_EXPORT_ZIP_PATH = APP_FOLDER_PATH + "flow_%s.zip"; // 导出ZIP文件路径, %s为taskId
    public static final String APP_EXPORT_FILE_PATH = APP_FOLDER_PATH + "flow_%s.data"; // 导出文件路径, %s为taskId
    public static final String APP_BACKUP_PATH = APP_FOLDER_PATH + "flow_%s_backup.data"; // 备份文件路径, %s为taskId
    public static final String STATISTICS_CATEGORY_TASK = "task"; // 统计类型-待办
    public static final String STATISTICS_CATEGORY_PROCESS = "process"; // 统计类型-流程
    public static final String DISABLE_TASK_EVENT = "disableTaskEvent"; // 关闭任务创建和删除事件
    /**
     * ##############################################################################################
     * 其他变量
     * ##############################################################################################
     */
    public static final String DATA = "data";
    public static final String PAGE_CURRENT = "current";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PERMISSION_ALL = "all";
    public static final String APP_ID = "appId";
    
    /**
     * ##############################################################################################
     * 通知模板变量
     * ##############################################################################################
     */
    public static final String TASK_RECEIVE_PARAMS_TITLE = "title"; // 
    public static final String TASK_RECEIVE_PARAMS_CONTENT = "content"; // 
    public static final String TASK_RECEIVE_PARAMS_EXTENDCONTENT = "extendcontent"; // 
    public static final String TASK_RECEIVE_PARAMS_CREATOR = "creator"; // 
    public static final String TASK_RECEIVE_PARAMS_CREATIONTIME = "creationTime"; //
    public static final String TASK_RECEIVE_PARAMS_URL = "url"; //
    public static final String TASK_RECEIVE_TOPIC = "bap_pending"; // 待办通知默认主题
    public static final String TASK_URGE_TOPIC = "bap_reminding"; // 催办通知默认主题
    /**
     * ##############################################################################################
     * 中间件变量
     * ##############################################################################################
     */
    public static final String KFK_TOPIC_TENANT_EVENT = "supOS_tenant";
}
