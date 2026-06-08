package com.supcon.supfusion.scheduler.server.service.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum TaskErrorEnum implements ErrorDefinition {

    SYSTEM_ERROR(101006000, "调度系统有错误！", "taskScheduler.system_error"),
    ADD_TASK_FAILURE(101006001, "添加调度任务失败！", "taskScheduler.add_task_failure"),
    UPDATE_TASK_FAILURE(101006002, "更新调度任务失败！", "taskScheduler.update_task_failure"),
    DELETE_TASK_FAILURE(101006003, "删除调度任务失败！", "taskScheduler.delete_task_failure"),
    UPDATE_TRIGGER_FAILURE(101006004, "更新触发器失败！", "taskScheduler.update_trigger_failure"),
    PARAMETER_ERROR(101006005, "参数错误", "taskScheduler.parameter_error"),
    CRON_ERROR(101006006, "Cron 表达式错误", "taskScheduler.cron_error"),
    TASK_NO_EXIST(101006007, "调度任务不存在！", "taskScheduler.task_no_exist"),
    TASK_HAS_EXIST(101006008, "调度任务已经存在！", "taskScheduler.task_has_exist"),
    PAUSE_TASK_FAILURE(101006009, "暂停任务失败！", "taskScheduler.pause_task_failure"),
    ACTIVATE_TASK_FAILURE(101006009, "暂停任务失败！", "taskScheduler.activate_task_failure"),
    JOBCODE_EXIST(1010060010, "编码已经存在！", "taskScheduler.jobCode_exist"),
    ADD_I18N_ERROR(1010060011, "添加国际化信息错误！", "taskScheduler.add.i18n.error");

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;

    TaskErrorEnum(Integer code, String defaultMessage, String key) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.key = key;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return defaultMessage;
    }

    @Override
    public String getInfo() {
        return key;
    }
    @Override
    public String getSimpleMessage() {
        return defaultMessage;
    }
}
