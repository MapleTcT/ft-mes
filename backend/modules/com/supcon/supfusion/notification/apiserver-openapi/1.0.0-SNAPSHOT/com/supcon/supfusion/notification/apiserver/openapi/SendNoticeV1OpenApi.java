package com.supcon.supfusion.notification.apiserver.openapi;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerError;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerExecption;
import com.supcon.supfusion.notification.apiserver.openapi.vo.SendNoticeV1RequestVO;
import com.supcon.supfusion.notification.apiserver.openapi.vo.SendNoticeV1ResponseVO;
import com.supcon.supfusion.notification.apiserver.service.NotificationService;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithMessageV1BO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 兼容老通知中心发送接口
 *
 * @param
 * @return
 */
@OpenApi(path = HttpConstants.URL_SPLITER + "api")
@Api(tags = {"外部消息发送V1接口", "open-api"})
public class SendNoticeV1OpenApi extends BaseController {
    @Resource(name = "apiserverNotificationServiceImpl")
    private NotificationService notificationService;

    /**
     * 消息发送V1接口
     *
     * @param sendNoticeV1RequestVO
     * @return
     */

    @PostMapping(value = {"/notification/sendNotice", "/openapi/notification/v1/message", "/open-api/notification-apiserver/v2/notice"})
    @ResponseBody
    @ApiOperation("消息发送V1接口")
    public SendNoticeV1ResponseVO send(@RequestBody @Valid @ApiParam(name = "消息发送内容", value = "传入json格式", required = true) SendNoticeV1RequestVO sendNoticeV1RequestVO) {
        SendWithMessageV1BO sendWithMessageV1BO = new SendWithMessageV1BO();
        sendWithMessageV1BO.setBsmodCode(sendNoticeV1RequestVO.getSender());
        sendWithMessageV1BO.setBsmodName(sendNoticeV1RequestVO.getSource());
        List<String> userIds = new ArrayList<>();
        sendNoticeV1RequestVO.getReceivers().stream().forEach(userId -> userIds.add(userId));
        sendWithMessageV1BO.setUserIds(userIds);
        if (StringUtils.hasText(sendNoticeV1RequestVO.getType())) {
            List<String> protocols = new ArrayList<>();
            protocols.add(sendNoticeV1RequestVO.getType());
            sendWithMessageV1BO.setProtocols(protocols);
            if (sendNoticeV1RequestVO.getContent() != null) {
                sendWithMessageV1BO.setContent(sendNoticeV1RequestVO.getContent().toJSONString());
            }
        } else if (sendNoticeV1RequestVO.getMultipleType() != null && sendNoticeV1RequestVO.getMultipleType().size() > 0) {
            List<String> protocols = new ArrayList<>();
            sendNoticeV1RequestVO.getMultipleType().stream().forEach(multipleType -> {
                if (multipleType != null && multipleType.size() > 0) {
                    protocols.add(multipleType.get(0));
                }
            });
            JSONObject content = new JSONObject();
            content.put("subject", sendNoticeV1RequestVO.getTitle());
            content.put("text", sendNoticeV1RequestVO.getText());
            sendWithMessageV1BO.setContent(content.toJSONString());
            sendWithMessageV1BO.setProtocols(protocols);
        }
        if (sendWithMessageV1BO.getProtocols() == null || sendWithMessageV1BO.getProtocols().size() == 0) {
            throw new BizHttpStatusException(NotificationApiServerError.ERROR_DUPLICATE_PROTOCOL, 400);
        }

        return new SendNoticeV1ResponseVO(notificationService.sendWithMessageV1(sendWithMessageV1BO));
    }
}
