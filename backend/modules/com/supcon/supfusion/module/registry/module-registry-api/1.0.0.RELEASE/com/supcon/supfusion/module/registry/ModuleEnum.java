/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry;

/**
 * @author: zhuangmh
 * @date: 2020年7月2日 下午6:13:46
 */
public enum ModuleEnum {
    /**
     * 工作流模块
     */
    WORKFLOW("workflow"),
    /**
     * 实体配置模块
     */
    APP_CONFIG("appConfig"),
    /**
     * 系统配置模块
     */
    SYSTEM_CONFIG("systemConfig"),
    /**
     * 系统编码编码
     */
    SYSTEM_CODE("systemCode"),
    /**
     * 通知中心Admin模块
     */
    NOTIFICATION_ADMIN("notificationAdmin"),
    /**
     * 通知中心ApiServer模块
     */
    NOTIFICATION_APISERVER("notificationApiServer"),
    /**
     * 通知中心Engine模块
     */
    NOTIFICATION_ENGINE("notificationEngine"),
    /**
     * 通知中心Stationletter模块
     */
    NOTIFICATION_STATION_LETTER("notificationStationletter"),
    /**
     * 通知中心Email模块
     */
    NOTIFICATION_EMAIL("notificationEmail"),
    /**
     * 通知中心Dingtalk模块
     */
    NOTIFICATION_DINGTALK("notificationDingtalk"),
    /**
     * 通知中心Wechat模块
     */
    NOTIFICATION_WECHAT("notificationWechat"),
    /**
     * 组织模块
     */
    ORGANIZATION("organization"),
    /**
     * 用户管理
     */
    USER_MANAGEMENT("userManagement"),
    /**
     * 认证模块
     */
    AUTHENTICATION("authentication"),
    /**
     * 权限
     */
    AUTHORIZATION("rbac"),
    /**
     *
     */
    COMPOSE_MANAGE("composeManage"),
    /**
     * app管理
     */
    APP_MANAGER("appManager"),
    /**
     * 国际化模块
     */
    I18N("i18n"),
    /**
     * 模块中心
     */
    MODULE_REGISTRY("reg"),
    /**
     * 主题
     */
    THEME("theme"),
    /**
     * 系统默认
     */
    DEFAULT("sys"),
    /**
     * 电子签名
     */
    SIGNATURE("signature"),
    /**
     * 门户
     */
    PORTAL("portal"),
    /**
     * 附件服务
     */
    FILE_SERVER("fileServer"),
    /**
     * 调度服务
     */
    TASK_SCHEDULER("taskScheduler"),
    /**
     * 编码生成器服务
     */
    COUNTER("counter"),

    /**
     * 系统基础模块
     */
    SYSBASE("sysbase"),

    /**
     * 打印模块
     */
    PRINT_MANAGE("printer"),

    /**
     * 审计日志模块
     */
    AUDIT_LOG("auditlog");

    private final String moduleId;

    private ModuleEnum(final String moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleId() {
        return moduleId;
    }

}
