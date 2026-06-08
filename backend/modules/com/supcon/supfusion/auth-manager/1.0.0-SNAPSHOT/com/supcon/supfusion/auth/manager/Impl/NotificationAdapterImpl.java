package com.supcon.supfusion.auth.manager.Impl;

import com.supcon.supfusion.auth.common.exception.AuthIdentityConfigException;
import com.supcon.supfusion.auth.common.exception.UserErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.manager.NotificationAdapter;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.api.NoticeProtocolConfigApi;
import com.supcon.supfusion.notification.admin.api.dto.NoticeProtocolConfigDTO;
import com.supcon.supfusion.notification.apiserver.api.SendNoticeV2InternalApi;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessageRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.naming.AuthenticationException;

/**
 * @Author kk.C
 * @Date 2021/3/2 13:44
 */
@Slf4j
@Service
public class NotificationAdapterImpl implements NotificationAdapter {

    @Autowired
    private SendNoticeV2InternalApi sendNoticeV2InternalApi;
    @Autowired
    NoticeProtocolConfigApi noticeProtocolConfigApi;

    /**
     * @Author kk.C
     * @Description 发送验证码至指定邮箱
     * @Date 2021/3/2 13:45
     * @Param [sendWithMessageRequestDTO]
     * @return void
     **/
    @Override
    public void sendMessage(SendWithMessageRequestDTO sendWithMessageRequestDTO) {


        Result<NoticeProtocolConfigDTO> email = noticeProtocolConfigApi.protocolconfig("email");
        if (email!=null && email.getData()!=null && !StringUtils.isEmpty( email.getData().getConfigValue())) {
        }else {
          throw   new UserException(UserErrorEnum.EMAIL_CONFIG_NOT_SET);
        }
        sendNoticeV2InternalApi.message(sendWithMessageRequestDTO);
    }
}
