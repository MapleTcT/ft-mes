package com.supcon.supfusion.notification.apiserver.service.rpc;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.apiserver.api.SendNoticeV2InternalApi;
import com.supcon.supfusion.notification.apiserver.api.dto.RangeDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessageContentDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessageRequestDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessgaeResponseDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithTopicRequestDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithTopicResponseDTO;
import com.supcon.supfusion.notification.apiserver.common.bean.RangeBO;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerError;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerExecption;
import com.supcon.supfusion.notification.apiserver.service.NotificationService;
import com.supcon.supfusion.notification.apiserver.service.bo.MessageBO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithMessageBO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithTopicBO;
import com.supcon.supfusion.notification.common.bean.RangeType;
import com.supcon.supfusion.notification.common.util.BeanCopyUtil;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ServiceApiService
public class SendNoticeV2InternalApiImpl implements SendNoticeV2InternalApi {
    @Resource(name = "apiserverNotificationServiceImpl")
    private NotificationService notificationService;

    @Override
    public Result<SendWithTopicResponseDTO> topic(SendWithTopicRequestDTO sendWithTopicRequestDTO) {
        SendWithTopicBO sendWithTopicBO = BeanCopyUtil.copyBeanProperties(sendWithTopicRequestDTO, SendWithTopicBO::new);
        List<RangeDTO> receivers = sendWithTopicRequestDTO.getReceivers();
        if (receivers != null && receivers.size() > 0) {
            sendWithTopicBO.setReceivers(rangeBOS(receivers));
        }
        return new Result(HttpStatus.SC_OK,"",new SendWithTopicResponseDTO(notificationService.sendWithTopic(sendWithTopicBO)));
    }

    @Override
    public Result<SendWithMessgaeResponseDTO> message(SendWithMessageRequestDTO sendWithMessageRequestDTO) {
        SendWithMessageBO sendWithMessageBO = BeanCopyUtil.copyBeanProperties(sendWithMessageRequestDTO, SendWithMessageBO::new);
        if (sendWithMessageRequestDTO.getContents() != null && sendWithMessageRequestDTO.getContents().size() > 0) {
            List<MessageBO> messageBOS = new ArrayList<>();
            for (SendWithMessageContentDTO sendWithMessageContentDTO : sendWithMessageRequestDTO.getContents()) {
                MessageBO messageBO = BeanCopyUtil.copyBeanProperties(sendWithMessageContentDTO, MessageBO::new);
                messageBOS.add(messageBO);
            }
            sendWithMessageBO.setContents(messageBOS);
        }

        Collection<RangeDTO> receivers = sendWithMessageRequestDTO.getReceivers();
        if (receivers != null && receivers.size() > 0) {
            sendWithMessageBO.setReceivers(rangeBOS(receivers));
        }
        return new Result(new SendWithMessgaeResponseDTO(notificationService.sendWithMessage(sendWithMessageBO)));
    }

    public static List<RangeBO> rangeBOS(Collection<RangeDTO> receivers) {
        List<RangeBO> rangeBOS = new ArrayList<>();
        for (RangeDTO rangeDTO : receivers) {
            if (RangeType.STAFF == rangeDTO.getRangeType() ||
                    RangeType.DEPARTMENT == rangeDTO.getRangeType() ||
                    RangeType.ROLE == rangeDTO.getRangeType() ||
                    RangeType.POSITION == rangeDTO.getRangeType()) {
                if (rangeDTO.getCodes() == null || rangeDTO.getCodes().size() == 0) {
                    throw new NotificationApiServerExecption(NotificationApiServerError.ERROR_RECEIVER_ID_CANNOT_NULL);
                } else {
                    RangeBO rangeBO = new RangeBO();
                    List<String> codes = new ArrayList();
                    rangeBO.setCodes(codes);
                    rangeBO.setRangeType(rangeDTO.getRangeType());
                    for (String code : rangeDTO.getCodes()) {
                        codes.add(code);
                    }
                    rangeBOS.add(rangeBO);
                }
            } else {
                throw new NotificationApiServerExecption(NotificationApiServerError.ERROR_RECEIVER_TYPE_NOT_SUPPORTED);
            }
        }
        return rangeBOS;
    }
}
