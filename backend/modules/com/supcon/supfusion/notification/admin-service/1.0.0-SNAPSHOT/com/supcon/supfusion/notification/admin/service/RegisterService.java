package com.supcon.supfusion.notification.admin.service;


import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.service.bo.ProtocolConfigBO;

public interface RegisterService {
    /**
     * 协议注册
     *
     * @param protocolConfigBO
     * @return
     */
    Long register(ProtocolConfigBO protocolConfigBO) throws NotificationAdminExecption;

    /**
     * 协议反注册
     *
     * @param appName
     * @param venderName
     * @return
     */
    void unregister(String appName, String venderName);
}
