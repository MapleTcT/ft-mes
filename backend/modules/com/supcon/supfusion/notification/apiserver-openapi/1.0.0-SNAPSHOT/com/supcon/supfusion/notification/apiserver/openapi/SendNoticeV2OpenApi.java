package com.supcon.supfusion.notification.apiserver.openapi;

import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.apiserver.common.bean.RangeBO;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerError;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerExecption;
import com.supcon.supfusion.notification.apiserver.openapi.vo.RangeVO;
import com.supcon.supfusion.notification.apiserver.openapi.vo.SendWithMessageRequestVO;
import com.supcon.supfusion.notification.apiserver.openapi.vo.SendWithMessgaeResponseVO;
import com.supcon.supfusion.notification.apiserver.openapi.vo.SendWithTopicRequestVO;
import com.supcon.supfusion.notification.apiserver.openapi.vo.SendWithTopicResponseVO;
import com.supcon.supfusion.notification.apiserver.service.NotificationService;
import com.supcon.supfusion.notification.apiserver.service.bo.MessageBO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithMessageBO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithTopicBO;
import com.supcon.supfusion.notification.common.bean.RangeType;
import com.supcon.supfusion.notification.common.util.BeanCopyUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;


@OpenApi(path = HttpConstants.URL_OPENAPI)
@Api(tags = {"外部消息发送V2接口", "open-api"})
public class SendNoticeV2OpenApi extends BaseController {
    @Resource(name = "apiserverNotificationServiceImpl")
    private NotificationService notificationService;

    /**
     * 消息发送V2接口
     *
     * @param sendWithTopicVO
     * @return
     */
    @PostMapping(value = {"/notification-apiserver/v2/message/topic", "/p/notification/v2/topic/messages"})
    @ResponseBody
    @ApiOperation("消息发送V2接口--主题消息")
    public SendWithTopicResponseVO sendWithTopic(@RequestBody @ApiParam(name = "消息发送内容", value = "传入json格式", required = true) @Valid SendWithTopicRequestVO sendWithTopicVO) {

        SendWithTopicBO sendWithTopicBO = BeanCopyUtil.copyBeanProperties(sendWithTopicVO, SendWithTopicBO::new);
        List<RangeVO> receivers = sendWithTopicVO.getReceivers();
        if (receivers != null && receivers.size() > 0) {
            sendWithTopicBO.setReceivers(rangeBOS(receivers));
        }
        return new SendWithTopicResponseVO(notificationService.sendWithTopic(sendWithTopicBO));
    }


    /**
     * 消息发送V2接口
     *
     * @param sendWithMessageRequestVO
     * @return
     */
    @PostMapping(value = {"/notification-apiserver/v2/message", "/p/notification/v2/messages"})
    @ResponseBody
    @ApiOperation("消息发送V2接口--消息直发")
    public SendWithMessgaeResponseVO sendWithMessage(@RequestBody @ApiParam(name = "消息发送内容", value = "传入json格式", required = true) @Valid SendWithMessageRequestVO sendWithMessageRequestVO) {

        SendWithMessageBO sendWithMessageBO = BeanCopyUtil.copyBeanProperties(sendWithMessageRequestVO, SendWithMessageBO::new);
        sendWithMessageBO.setContents(BeanCopyUtil.copyListProperties(sendWithMessageRequestVO.getContents(), MessageBO::new));
        List<RangeVO> receivers = sendWithMessageRequestVO.getReceivers();
        if (receivers != null && receivers.size() > 0) {
            sendWithMessageBO.setReceivers(rangeBOS(receivers));
        }
        return new SendWithMessgaeResponseVO(notificationService.sendWithMessage(sendWithMessageBO));
    }

    public static List<RangeBO> rangeBOS(List<RangeVO> receivers) {
        List<RangeBO> rangeBOS = new ArrayList<>();
        for (RangeVO rangeVO : receivers) {
            if (RangeType.STAFF == rangeVO.getRangeType() ||
                    RangeType.DEPARTMENT == rangeVO.getRangeType() ||
                    RangeType.ROLE == rangeVO.getRangeType() ||
                    RangeType.POSITION == rangeVO.getRangeType()) {
                if (rangeVO.getCodes() == null || rangeVO.getCodes().size() == 0) {
                    throw new NotificationApiServerExecption(NotificationApiServerError.ERROR_RECEIVER_ID_CANNOT_NULL);
                } else {
                    RangeBO rangeBO = new RangeBO();
                    List<String> codes = new ArrayList();
                    rangeBO.setCodes(codes);
                    rangeBO.setRangeType(rangeVO.getRangeType());
                    for (String code : rangeVO.getCodes()) {
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
