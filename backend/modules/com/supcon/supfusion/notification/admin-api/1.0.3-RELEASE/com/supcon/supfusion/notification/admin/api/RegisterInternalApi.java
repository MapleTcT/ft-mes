package com.supcon.supfusion.notification.admin.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolConfigDTO;
import com.supcon.supfusion.notification.admin.api.dto.RegisterResponseDTO;
import com.supcon.supfusion.notification.admin.api.dto.UnRegisterResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

/**
 * 通知中心协议注册器
 *
 * @param
 * @return
 */
@FeignClient(name = "notification-admin", contextId = "RegisterInternalApi")
@Api(tags = {"内部协议注册接口", "service-api"})
public interface RegisterInternalApi {

    /**
     * 协议注册
     *
     * @param protocolConfig
     * @return
     */
    @PostMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-admin/v1/register")
    @ResponseBody
    @ApiOperation("协议注册")
    Result<RegisterResponseDTO> register(@RequestBody @Valid @ApiParam(name = "协议配置参数", value = "传入json格式", required = true) ProtocolConfigDTO protocolConfig);

    /**
     * 协议反注册
     *
     * @param appName
     * @param venderName
     * @return
     */
    @DeleteMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-admin/v1/unregister")
    @ResponseBody
    @ApiOperation("协议反注册")
    @Deprecated
    Result<UnRegisterResponseDTO> unregister(@RequestParam("appName") @NotEmpty(message = "appName不能为空") @ApiParam(value = "appName", required = true) String appName,
                                             @RequestParam("venderName") @NotEmpty(message = "venderName不能为空") @ApiParam(value = "venderName", required = true) String venderName);
}
