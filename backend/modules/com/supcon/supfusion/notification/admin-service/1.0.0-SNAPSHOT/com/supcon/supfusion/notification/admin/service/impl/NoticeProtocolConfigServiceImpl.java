package com.supcon.supfusion.notification.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolConfigService;
import com.supcon.supfusion.notification.admin.service.bo.NoticeDingtalkConfig;
import com.supcon.supfusion.notification.admin.service.bo.NoticeEmailConfig;
import com.supcon.supfusion.notification.admin.service.bo.NoticeStationLetterConfig;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolConfig;
import com.supcon.supfusion.notification.admin.dao.mappers.protocolconfig.NoticeProtocolConfigMapper;
import com.supcon.supfusion.notification.admin.service.bo.NoticeSuplinkConfig;
import org.springframework.stereotype.Service;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:05
 */
@Service("adminNoticeProtocolConfigServiceImpl")
public class NoticeProtocolConfigServiceImpl extends NoticeBaseServiceImpl<NoticeProtocolConfigMapper, NoticeProtocolConfig> implements NoticeProtocolConfigService {
    @Override
    public void emailConfig(NoticeEmailConfig emailConfig) {
        this.updateOrSaveConfig(1000L, "email", emailConfig);
    }

    @Override
    public void stationLetterConfig(NoticeStationLetterConfig stationLetterConfig) {
        this.updateOrSaveConfig(1001L, "stationLetter", stationLetterConfig);
    }

    @Override
    public void dingTalkConfig(NoticeDingtalkConfig dingtalkConfig) {
        this.updateOrSaveConfig(1002L, "dingtalk", dingtalkConfig);
    }

    @Override
    public void suplinkConfig(NoticeSuplinkConfig suplinkConfig) {
        this.updateOrSaveConfig(1003L, "suplink", suplinkConfig);
    }

    @Override
    public void protocolConfig(Long protocolConfigId, String protocol, String configValue) {
        NoticeProtocolConfig protocolConfig = new NoticeProtocolConfig();
        protocolConfig.setConfigValue(configValue);
        protocolConfig.setId(protocolConfigId);
        protocolConfig.setProtocol(protocol);
        super.saveOrUpdate(protocolConfig, new QueryWrapper<NoticeProtocolConfig>().eq(NoticeProtocolConfig.getIdFieldName(), protocolConfigId));
    }

    @Override
    public NoticeProtocolConfig addProtocolConfig(NoticeProtocolConfig protocolConfig) {
        try {
            if (protocolConfig.getId() == null) {
                protocolConfig.setId(IDGenerator.newInstance().generate().longValue());
            }
            super.save(protocolConfig);
        } catch (Exception e) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_ADD_PROTOCOL_CONFIG);
        }
        return protocolConfig;
    }

    /***
     * 更新通知方式配置项
     * @param configId 通知方式配置项ID
     * @param protocolCode 通知方式ID
     * @param configValue 通知方式配置项的值
     */
    private void updateOrSaveConfig(Long configId, String protocolCode, Object configValue) {
        NoticeProtocolConfig protocolConfig = new NoticeProtocolConfig();
        protocolConfig.setConfigValue(JSON.toJSONString(configValue));
        protocolConfig.setId(configId);
        protocolConfig.setProtocol(protocolCode);

        NoticeProtocolConfig record_config = this.getOne(new QueryWrapper<NoticeProtocolConfig>()
                .eq(NoticeProtocolConfig.getIdFieldName(), configId)
                .or().eq(NoticeProtocolConfig.getProtocolFieldName(), protocolCode));
        if (record_config == null) {
            super.save(protocolConfig);
        } else {
            protocolConfig.setId(record_config.getId());
            super.updateEntity(protocolConfig);
        }
    }

}
