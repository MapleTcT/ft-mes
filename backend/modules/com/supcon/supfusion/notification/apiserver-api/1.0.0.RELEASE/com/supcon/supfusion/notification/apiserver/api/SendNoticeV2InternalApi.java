package com.supcon.supfusion.notification.apiserver.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessageRequestDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessgaeResponseDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithTopicRequestDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithTopicResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;

/**
 * 通知中消息发送、消息状态上送接口
 *
 * @param
 * @return
 */
@FeignClient(name = "notification-apiserver", contextId = "SendNoticeV2InternalApi")
@Api(tags = {"内部通知中消息发送", "internal-api"})
public interface SendNoticeV2InternalApi {
    /**
     * 根据主题发送消息
     *
     * @param sendWithTopicRequestDTO
     * @return
     */
    @PostMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-apiserver/v1/message/topic")
    @ResponseBody
    @ApiOperation("消息发送V2接口--主题消息")
    Result<SendWithTopicResponseDTO> topic(@RequestBody @Valid @ApiParam(name = "消息实体", value = "传入json格式", required = true) SendWithTopicRequestDTO sendWithTopicRequestDTO);


    /**
     * 消息直发
     *
     * @param sendWithMessageRequestDTO
     * @return
     */
    @PostMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-apiserver/v1/message")
    @ResponseBody
    @ApiOperation("消息发送V2接口--消息直发")
    Result<SendWithMessgaeResponseDTO> message(@RequestBody @Valid @ApiParam(name = "消息实体", value = "传入json格式", required = true) SendWithMessageRequestDTO sendWithMessageRequestDTO);
}
