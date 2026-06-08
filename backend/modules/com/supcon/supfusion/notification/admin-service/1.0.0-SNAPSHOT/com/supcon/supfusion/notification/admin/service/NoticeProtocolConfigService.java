package com.supcon.supfusion.notification.admin.service;

import com.supcon.supfusion.notification.admin.service.bo.NoticeDingtalkConfig;
import com.supcon.supfusion.notification.admin.service.bo.NoticeEmailConfig;
import com.supcon.supfusion.notification.admin.service.bo.NoticeStationLetterConfig;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolConfig;
import com.supcon.supfusion.notification.admin.service.bo.NoticeSuplinkConfig;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:26
 */
public interface NoticeProtocolConfigService extends NoticeBaseService<NoticeProtocolConfig>{

    public NoticeProtocolConfig addProtocolConfig(NoticeProtocolConfig protocolConfig);
    /**
     * 邮件配置
     * @param emailConfig
     */
    public void emailConfig(NoticeEmailConfig emailConfig);

    /***
     * 站内信配置
     * @param stationLetterConfig
     */
    public void stationLetterConfig(NoticeStationLetterConfig stationLetterConfig);

    /***
     * 钉钉服务配置
     * @param dingtalkConfig
     */
    public void dingTalkConfig(NoticeDingtalkConfig dingtalkConfig);

    /**
     * suplink服务配置
     * @param suplinkConfig
     */
    public void suplinkConfig(NoticeSuplinkConfig suplinkConfig);
    /***
     * 扩展协议配置
     * @param protocolConfigId
     * @param protocol
     * @param configValue
     */
    public void protocolConfig(Long protocolConfigId, String protocol, String configValue);
}
