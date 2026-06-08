package com.supcon.supfusion.notification.apiserver.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.apiserver.api.dto.AckRequestDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.AckResponseDTO;
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
@FeignClient(name = "notification-apiserver", contextId = "ProtocolNoticeInternalApi")
@Api(tags = {"消息状态上送接口", "internal-api"})
public interface ProtocolNoticeInternalApi {

    /**
     * 消息状态上送接口
     *
     * @param ackRequestDTO
     * @return
     */
    @PostMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-apiserver/v1/notice/status")
    @ResponseBody
    @ApiOperation("消息状态上送接口")
    Result<AckResponseDTO> noticeStatus(@RequestBody @Valid @ApiParam(name = "消息状态ACK", value = "传入json格式", required = true) AckRequestDTO ackRequestDTO);
}
