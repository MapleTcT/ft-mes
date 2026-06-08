package com.supcon.supfusion.notification.apiserver.common.execption;

/**
 * 通知中心管理服务异常定义
 *
 * <ol>
 * <li>错误码格式：[100]+[102]+[000]</li>
 * </ol>
 */
public enum NotificationApiServerError implements NotificationApiServerDefinition {
    ERROR_DUPLICATE_PROTOCOL(100102001, "协议已存在","notificationApiServer.error_duplicate_protocol"),
    ERROR_NO_USER(100102002, "接收人都不存在","notificationApiServer.no_user"),
    ERROR_TIME_FORMAT(100102003, "时间格式错误","notificationApiServer.error_time_format"),
    ERROR_RECEIVER_ID_CANNOT_NULL(100102004, "receiver id不能为空","notificationApiServer.receiver_id_cannot_null"),
    ERROR_RECEIVER_ID_CAN_ONLY_BE_NUMBER(100102005, "receiver id只能为数字","notificationApiServer.receiver_id_can_only_be_number"),
    ERROR_RECEIVER_TYPE_NOT_SUPPORTED(100102006, "Receiver type除STAFF、POSITION、DEPARTMENT、ROLE以外暂不支持","notificationApiServer.receiver_type_not_supported"),
    ERROR_TOPIC_NOT_EXIST(100102007, "主题不存在","notificationApiServer.topic_not_exist"),
    ERROR_TOPIC_HAS_NO_TEMPLATE(100102008, "主题没有关联模板","notificationApiServer.topic_has_no_template"),
    ERROR_NO_RECEIVERS(100102009, "消息接受者不存在","notificationApiServer.no_receivers"),
    ERROR_FREEMARK_BUILD_FAIL(100102010, "模板解析出错","notificationApiServer.freemark_build_fail"),
    ERROR_TOPIC_EXCLUSIVE_PROTOCOL(100102011, "主题不包含该协议","notificationApiServer.topic_exclusive_protocol"),
    ERROR_PROTOCOL_DONT_EXIST(100102012, "协议不存在","notificationApiServer.protocol_dont_exist");


    private Integer code;
    private String message;
    private String info;


    NotificationApiServerError(Integer code, String message,String info) {
        this.code = code;
        this.message = message;
        this.info = info;
    }

    @Override
    public Integer getCode() {
        return code;
    }
    
    /**
     * 国际化信息
     *
     * @return
     */
    public String getInfo() {
    	return info;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
