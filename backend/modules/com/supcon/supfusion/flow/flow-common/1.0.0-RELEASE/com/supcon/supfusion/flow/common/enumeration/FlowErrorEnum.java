/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年5月20日 上午9:20:54
 */
public enum FlowErrorEnum implements ErrorDefinition {
    /**
     * ##############################################################################################
     * 流程组态异常 从100108001开始
     * ##############################################################################################
     */
    DIAGRAM_NAME_DUPLICATE_ERROR(100108001, "流程名称已存在"),
    
    DIAGRAM_NOT_EXIST_ERROR(100108002, "流程组态不存在"),
    
    DIAGRAM_STATUS_NOT_ALLOW_PUBLISH_ERROR(100108003, "流程状态异常, 不允许发布"),
    
    DIAGRAM_STRUCTURE_ERROR(100108004, "流程结构有误"),
    
    DIAGRAM_CHANGED_NOT_ALLOW_PUBLISH_ERROR(100108005, "流程结构已变更, 请升版后再发布"),
    
    DIAGRAM_ENABLE_DUPLICATE_ERROR(100108006, "流程已启用"),
    
    DIAGRAM_UPGRADE_ERROR(100108007, "流程升版异常"),
    
    DIAGRAM_IMPORT_ERROR(100108008, "导入失败"),
    
    DIAGRAM_CROSS_APP_IMPORT_ERROR(100108009, "无法跨app导入"),
    
    FLOW_CHART_PARAMETER_ERROR(100108010, "请求参数缺少流程编号或流程实例ID"),
    /**
     * ##############################################################################################
     * 流程发布异常 从100108101开始
     * ##############################################################################################
     */
    ONLY_ONE_START_NODE_IS_ALLOWED_ERROR(100108101, "请设置一个开始节点"),
    
    END_NODE_NOT_EXIST(100108102, "结束节点未设置"),
    
    USERTASK_NAME_NOT_EMPTY_ERROR(100108103, "用户任务名称不能为空"),

    USERTASK_PAGE_NOT_EMPTY_ERROR(100108104, "用户任务关联页面不能为空"),
    // TODO 带参数--当前环节名称
    MULTIPLE_SEQUENCE_NAME_NOT_EMPTY_ERROR(100108105, "人工环节分支名称不能为空"),
    
    MULTIPLE_SEQUENCE_CONDITION_NOT_SET_ERROR(100108106, "人工环节分支条件未设置"),
    
    SERVICETASK_NAME_NOT_EMPTY_ERROR(100108107, "自动环节名称不能为空"),
    
    SERVICETASK_EXECUTOR_NOT_SET_ERROR(100108108, "自动环节执行对象未设置"),
    
    TIMER_VARIABLE_NOT_SET_ERROR(100108109, "定时器变量未设置"),
    
    SIGNEL_SOURCE_NOT_SET_ERROR(100108110, "信号源对象未设置"),
    
    PROCESS_STARTUP_USERTASK_NOT_EXIST(100108111, "人工启动环节不存在"),
    
    PROCESS_NOT_PUBLISHED_ERROR(100108112, "流程未发布"),
    
    PROCESS_NOT_ALLOWED_SAVE_ERROR(100108113, "该流程不允许保存,检查第一个环节是否为发起者"),
    
    PROCESS_NO_PERMISSION_START(100108114, "当前用户没有权限发起流程"),
    
    START_NODE_PAGE_NOT_SET_ERROR(100108115, "启动节点关联页面未设置"),

    USERTASK_NOT_EXIST(100108116, "当前人工节点不存在"),
    
    /**
     * ##############################################################################################
     * 运行期异常 从100108201开始
     * ##############################################################################################
     */
    PROCESS_STARTUP_FAIL(100108212, "流程启动异常"),
    
    TASK_ASSIGNEE_NOT_EMPTY_ERROR(100108213, "待办任务执行者不能为空"),
    
    TASK_NO_CHECK_PERMISSION_ERROR(100108214, "无权限查看"),
    
    TASK_COMPLETE_FAIL(100108215, "待办提交异常"),
    
    TASK_STATUS_NOT_ALLOW_COMPLETE_ERROR(100108216, "流程已暂停, 无法提交"),
    
    TASK_NOT_ALLOWED_SELF_ENTRUST_ERROR(100108217, "不能委托给自己"),
    
    TASK_DUPLICATE_ENTRUST_ERROR(100108218, "不能重复委托"),
    
    TASK_STATUS_NOT_ALLOW_ENTRUST_ERROR(100108219, "暂停的待办任务不能被委托"), 
    
    TASK_ENTRUST_OWNER_NOT_ALLOW_ERROR(100108220, "对方已经有该待办操作权限"),
    
    USER_NOT_BIND_STAFF_ERROR(100108221, "登录用户未绑定人员"),
    
    PROCESS_STATUS_NOT_ALLOW_REVOKE_ERROR(100108222, "流程已结束"),
    
    TASK_STATUS_NOT_ALLOW_JOIN_ERROR(100108223, "暂停的流程不允许加签"),
    
    TASK_JOIN_NOT_ENABLE_ERROR(100108224, "未开启加签配置"),
    
    PROCESS_SUSPENDED_ERROR(100108225, "流程已暂停"),
    
    PROCESS_ACTIVED_ERROR(100108226, "流程已恢复"),
    
    PROCESS_NOT_EXIST_ERROR(100108227, "流程实例不存在"),
    
    GROUP_MEMBER_NOT_EMPTY_ERROR(100108228, "%s成员不能为空"),
    
    LEADER_NOT_EXIST_ERROR(100108229, "领导不存在"),
    
    USER_NOT_EXIST_ERROR(100108230, "用户不存在"),
    
    TASK_PERMISSION_DENY_ERROR(100108231, "无权限委托"), 
    
    TASK_REVOKE_FAIL(100108232, "并行无法撤回"),
    
    OODM_EXECUTE_FAIL(100108233, "%s"),
    
    TIME_FORMAT_ERROR(100108234, "时间格式有误"),
    
    TASK_JOIN_DUPLICATE(100108235, "被邀请人已经有该待办权限"),
    
    STATUS_CHANGED_REVOKE_FAIL(100108236, "流程状态已变更无法撤回"),
    
    TASK_NOT_EXIST_ERROR(100108237, "待办任务不存在"),
    
    COMPLETED_TASK_NOT_EXIST_ERROR(100108238, "已办不存在"),
    
    TASK_REVOKE_DENY_ERROR(100108239, "无权限撤回"), 
    
    TASK_NO_SUBMIT_PERMISSION_ERROR(100108240, "无权限操作"),
    
    RUNTIME_UPGRADE_FAIL(100108241, "运行期数据升级失败"),
    
    SEQUENCE_NOT_EXIST(1001082242, "流程分支不存在"),

    LAST_TASK_CANNOT_REVOKE(100108243, "结尾环节无法撤回"),

    LOOP_TASK_NOT_REVOKE(100108244, "会签无法撤回"),

    MANDATARY_USER_NOT_EXIST_ERROR(100108245, "受托人不存在"),

    AUDIT_BRANCH_REQUIRED(1001082246, "请指定分支再提交"),
    /**
     * ##############################################################################################
     * App异常 从100108401开始
     * ##############################################################################################
     */
    APP_EXPORTING_ERROR(100108401, "流程正在导出"),
    
    APP_IMPORTING_ERROR(100108402, "流程正在导入"),
    
    APP_UPGRADING_ERROR(100108403, "流程正在升级"),
    
    PARAMETER_APPID_ERROR(100108404, "缺少参数appId"),
    
    /**
     * ##############################################################################################
     * 参数校验 从100108501开始
     * ##############################################################################################
     */
    PROCESS_STATUS_NOT_EXIST_FAIL(100108501, "流程状态码不存在"),
    
    USERNAME_NOT_EMPTY(100108502, "参数username不能为空"),
    
    PAGE_MAXIMUM_LIMIT_ERROR(100108503, "pageSize超过最大值500"),
    
    TASK_ID_NOT_EMPTY(100108504, "参数taskId不能为空"),
    
    MANDATARY_NOT_EMPTY(100108505, "参数mandatary不能为空"),
    
    PROCESS_ID_NOT_EMPTY(100108506, "参数processId不能为空"),
    
    PROCESS_KEY_NOT_EMPTY(100108507, "参数processKey不能为空");
    
    /**
     * 异常码
     */
    private int code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;
    
    FlowErrorEnum(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * @see com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition#getCode()
     */
    @Override
    public Integer getCode() {
        return this.code;
    }

    /**
     * @see com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition#getMessage()
     */
    @Override
    public String getMessage() {
        return this.defaultMessage;
    }

    /**
     * @see com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition#getInfo()
     */
    @Override
    public String getInfo() {
        return null;
    }

}
