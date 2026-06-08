package com.supcon.supfusion.notification.apiserver.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.notification.apiserver.api.dto.OpenApiVersionDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 通知中消息发送、消息状态上送接口
 *
 * @param
 * @return
 */
@FeignClient(name = "notification-apiserver", contextId = "OpenApiInternalApi")
@Api(tags = {"OpenApi版本管理", "internal-api"})
public interface OpenApiInternalApi {

    /**
     * 消息状态上送接口
     *
     * @param
     * @return
     */
    @GetMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-apiserver/v2/openapi/version")
    @ResponseBody
    @ApiOperation("OpenApi版本查询接口")
    OpenApiVersionDTO getVersion();
}
