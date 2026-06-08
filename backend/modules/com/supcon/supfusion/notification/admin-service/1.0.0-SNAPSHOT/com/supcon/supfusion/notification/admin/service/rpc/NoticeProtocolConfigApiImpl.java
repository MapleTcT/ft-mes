package com.supcon.supfusion.notification.admin.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.admin.api.NoticeProtocolConfigApi;
import com.supcon.supfusion.notification.admin.api.dto.NoticeProtocolConfigDTO;
import com.supcon.supfusion.notification.admin.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolConfig;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolConfigService;
import com.supcon.supfusion.notification.email.bootstrap.INoticeEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

@Slf4j
@ServiceApiService
public class NoticeProtocolConfigApiImpl extends BaseController implements NoticeProtocolConfigApi {

    @Resource(name = "adminNoticeProtocolConfigServiceImpl")
    private NoticeProtocolConfigService protocolConfigService;

    @Autowired
    private INoticeEmailService emailEngine;
    @Override
    public Result<NoticeProtocolConfigDTO> protocolconfig(String protocolId) {
        QueryWrapper<NoticeProtocolConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(NoticeProtocolConfig.getProtocolFieldName(), protocolId);
        NoticeProtocolConfig protocolConfig = protocolConfigService.getOne(queryWrapper);
        if (protocolConfig == null) {
            return new Result();
        }

        NoticeProtocolConfigDTO noticeProtocolConfigVO = BeanCopyUtil.copyBeanProperties(protocolConfig, NoticeProtocolConfigDTO::new);
        return new Result<>(noticeProtocolConfigVO);
    }

    @Override
    public Result<Boolean> mailValid(String username, String password, String host, String port, Boolean enableSSL, String emailProtocol) {
        Boolean result = emailEngine.validEmailconfig(username, password, host, port, enableSSL, emailProtocol);
        return new Result<>(result);
    }
}
