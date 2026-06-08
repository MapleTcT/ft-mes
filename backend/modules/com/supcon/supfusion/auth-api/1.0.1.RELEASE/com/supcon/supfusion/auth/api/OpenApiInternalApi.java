package com.supcon.supfusion.auth.api;

import com.supcon.supfusion.auth.api.dto.OpenApiVersionDTO;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "auth", contextId = "AuthOpenApiInternalApi")
public interface OpenApiInternalApi {
    /**
     * 消息状态上送接口
     *
     * @param
     * @return
     */
    @GetMapping(value = HttpConstants.URL_SERVICEAPI + "/auth/v2/openapi/version")
    @ResponseBody
    OpenApiVersionDTO getVersion();

}
