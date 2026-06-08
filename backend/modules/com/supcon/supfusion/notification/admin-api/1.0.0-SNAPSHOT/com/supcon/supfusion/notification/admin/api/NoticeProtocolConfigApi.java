package com.supcon.supfusion.notification.admin.api;


import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.api.dto.NoticeProtocolConfigDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
/**
 * 模板新增接口
 *
 * @param
 * @return
 */
@FeignClient(name = "notification-admin", contextId = "NoticeProtocolConfigApi")
@Api(tags = {"协议配置项接口", "internal-api"})
public interface NoticeProtocolConfigApi {

    @ApiOperation(value = "根据协议ID获取配置")
    @GetMapping(HttpConstants.URL_SERVICEAPI + "/notification-admin/v1/notice/protocolconfig/protocolconfig")
    public Result<NoticeProtocolConfigDTO> protocolconfig(@ApiParam(value = "站内信协议配置", required = true) @RequestParam String protocolId);

    @ApiOperation(value = "验证邮箱配置是否有效", notes = "邮件仅发给发送者邮箱")
    @GetMapping(HttpConstants.URL_SERVICEAPI + "/notification-admin/v1/notice/valid/emailconfig")
    public Result<Boolean> mailValid(@ApiParam(value = "接收邮箱地址", required = true) @RequestParam String username,
                                     @ApiParam(value = "发送者邮箱密码", required = true) @RequestParam String password,
                                     @ApiParam(value = "邮件服务器地址") @RequestParam(required = false) String host,
                                     @ApiParam(value = "邮件服务器端口") @RequestParam(required = false) String port,
                                     @ApiParam(value = "是否启用smtp的ssl协议") @RequestParam(required = false) Boolean enableSSL,
                                     @ApiParam(value = "邮件协议类型") @RequestParam(required = false) String emailProtocol);
}
