package com.supcon.supfusion.notification.admin.common.execption;

/**
 * 通知中心管理服务异常定义
 *
 * <ol>
 * <li>错误码格式：[100]+[101]+[000]</li>
 * </ol>
 */
public enum NotificationAdminError implements NotificationAdminDefinition {
    ERROR_DUPLICATE_PROTOCOL(100101001, "DUPLICATE_PROTOCOL", "notificationAdmin.error_duplicate_protocol"),
    ERROR_DUPLICATE_PROTOCOL_TEMPLATE(100101002, "DUPLICATE_PROTOCOL_TEMPLATE_CODE", "notificationAdmin.duplicate_protocol_template_code"),
    ERROR_ADD_PROTOCOL_FAIL(100101003, "ADD_PROTOCOL_FAIL", "notificationAdmin.error_add_protocol_fail"),
    ERROR_DUPLICATE_TOPIC(100101004, "消息主题新增失败", "notificationAdmin.error_duplicate_top"),
    ERROR_DUPLICATE_TOPIC_CODE(100101005, "消息主题编码重复", "notificationAdmin.error_duplicate_top_code"),
    ERROR_DUPLICATE_TOPIC_NAME(10010106, "消息主题名称重复", "notificationAdmin.error_duplicate_top_name"),
    ERROR_DUPLICATE_TEMPLATE(100101007, "消息模板新增失败", "notificationAdmin.error_duplicate_template"),
    ERROR_DUPLICATE_TEMPLATE_CODE(100101008, "消息模板编码重复", "notificationAdmin.error_duplicate_template_code"),
    ERROR_ADD_PROTOCOL_CONFIG(100101009, "协议配置新增失败", "notificationAdmin.error_add_protocol_config"),
    ERROR_DELETE_TEMPLATE(100101010, "%s", ""),
    ERROR_TOPIC_NOT_EXIST(100101011, "TOPIC_NOT_EXIST", "notificationAdmin.topic_not_exist"),
    ERROR_TOPIC_HAS_NO_TEMPLATE(100101012, "TOPIC_HAS_NO_TEMPLATE", "notificationAdmin.topic_has_no_template"),
    ERROR_PROTOCOL_DONT_EXIST(100101013, "协议不存在", "notificationAdmin.protocol_dont_exist"),
    ERROR_DUPLICATE_PROTOCOL_TEMPLATE_NAME(100101014, "模板名称冲突", "notificationAdmin.error_duplicate_protocol_template_name"),
    ERROR_PROTOCOL_TEMPLATE_DONT_EXIST(100101015, "模板不存在", "notificationAdmin.protocol_template_dont_exist"),
    ERROR_ID_CAN_ONLY_BE_NUMBER(100101016, "id只能为数字", "notificationAdmin.id_can_only_be_number"),
    ERROR_PROTOCOL_TEMPLATE_CANNOT_BE_GREATER_THAN_10(100101017, "模板数量不能大于10", "notificationAdmin.the_number_of_templates_cannot_be_greater_than_10"),
    ERROR_SYSTEM_DATA_CATNOT_MODIFY(100101018, "系统数据不可修改", "notificationAdmin.system_data_cannot_modify"),
    ERROR_SYSTEM_DATA_CATNOT_DELETE(100101019, "系统数据不可删除", "notificationAdmin.system_data_cannot_delete"),
    ERROR_TOPIC_TREE_DELETE(100101020, "%s或下级存在主题不可删除", "notificationAdmin.topic_tree_delete"),
    ERROR_TOPIC_TREE_DONOT_EXIST(100101021, "主题类型不存在", "notificationAdmin.topic_tree_donot_exist");

    private Integer code;
    private String message;
    private String info;

    NotificationAdminError(Integer code, String message, String info) {
        this.code = code;
        this.message = message;
        this.info = info;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 国际化信息
     *
     * @return
     */
    @Override
    public String getInfo() {
        return info;
    }
}
