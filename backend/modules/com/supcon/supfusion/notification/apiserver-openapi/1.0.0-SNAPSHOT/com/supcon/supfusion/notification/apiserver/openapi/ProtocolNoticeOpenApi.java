package com.supcon.supfusion.notification.apiserver.openapi;

import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.apiserver.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.apiserver.openapi.vo.AckRequestVO;
import com.supcon.supfusion.notification.apiserver.openapi.vo.AckResponseVO;
import com.supcon.supfusion.notification.apiserver.service.NotificationService;
import com.supcon.supfusion.notification.apiserver.service.bo.AckBO;
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

/**
 * 通知中消息发送、消息状态上送接口
 *
 * @param
 * @return
 */
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "notification-apiserver" + HttpConstants.URL_SPLITER + "v2")
@Api(tags = {"通知中消息发送、消息状态上送接口", "open-api"})
public class ProtocolNoticeOpenApi extends BaseController {
    @Resource(name = "apiserverNotificationServiceImpl")
    private NotificationService notificationService;

    /**
     * 消息状态上送接口
     *
     * @param ackRequestVO
     * @return
     */
    @PostMapping(value = "/notice/status")
    @ResponseBody
    @ApiOperation("消息状态上送接口")
    public Result<AckResponseVO> noticeStatus(@RequestBody @Valid @ApiParam(name = "消息状态ACK", value = "传入json格式", required = true) AckRequestVO ackRequestVO) {
        List<AckBO> ackBOS = new ArrayList<>();
        ackRequestVO.getAcks().forEach(ackVO -> {
            AckBO ackBO = BeanCopyUtil.copyBeanProperties(ackVO, AckBO::new);
            ackBOS.add(ackBO);
        });
        notificationService.ack(ackBOS);
        return new Result<>();
    }
}
