package com.supcon.supfusion.notification.apiserver.service.rpc;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.apiserver.api.ProtocolNoticeInternalApi;
import com.supcon.supfusion.notification.apiserver.api.dto.AckRequestDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.AckResponseDTO;
import com.supcon.supfusion.notification.apiserver.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.apiserver.service.NotificationService;
import com.supcon.supfusion.notification.apiserver.service.bo.AckBO;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@ServiceApiService
public class ProtocolNoticeInternalApiImpl extends BaseController implements ProtocolNoticeInternalApi {
    @Resource(name = "apiserverNotificationServiceImpl")
    private NotificationService notificationService;

    @Override
    public Result<AckResponseDTO> noticeStatus(@Valid AckRequestDTO ackRequestDTO) {
        AckBO ackBO = BeanCopyUtil.copyBeanProperties(ackRequestDTO, AckBO::new);
        List<AckBO> ackBOList = new ArrayList<>();
        ackBOList.add(ackBO);
        notificationService.ack(ackBOList);
        return new Result<>();
    }
}
