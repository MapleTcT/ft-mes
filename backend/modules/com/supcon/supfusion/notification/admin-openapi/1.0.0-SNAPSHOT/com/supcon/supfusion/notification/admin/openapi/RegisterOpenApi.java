package com.supcon.supfusion.notification.admin.openapi;

import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.admin.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.admin.openapi.vo.ProtocolConfigVO;
import com.supcon.supfusion.notification.admin.openapi.vo.RegisterResponseVO;
import com.supcon.supfusion.notification.admin.openapi.vo.UnRegisterResponseVO;
import com.supcon.supfusion.notification.admin.service.RegisterService;
import com.supcon.supfusion.notification.admin.service.bo.ProtocolConfigBO;
import com.supcon.supfusion.notification.admin.service.bo.ProtocolTemplateBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

/**
 * 通知中心协议注册器
 *
 * @param
 * @return
 */
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v2")
@Api(tags = {"外部协议注册接口", "open-api"})
public class RegisterOpenApi extends BaseController {

    @Resource(name = "adminRegisterServiceImpl")
    private RegisterService registerService;

    /**
     * 协议注册
     *
     * @param protocolConfigVO
     * @return
     */
    @PostMapping(value = "/register")
    @ResponseBody
    @ApiOperation("协议注册")
    public Result<RegisterResponseVO> register(@RequestBody @Valid @ApiParam(name = "协议配置参数", value = "传入json格式", required = true) ProtocolConfigVO protocolConfigVO) {
        ProtocolConfigBO protocolConfigBO = BeanCopyUtil.copyBeanProperties(protocolConfigVO, ProtocolConfigBO::new, (source, target) -> {
            if (source.getProtocolContentType() != null) {
                target.setProtocolContentType(source.getProtocolContentType().ordinal());
            }
        });
        protocolConfigBO.setTemplates(BeanCopyUtil.copyListProperties(protocolConfigVO.getTemplates(), ProtocolTemplateBO::new));
        Long id = registerService.register(protocolConfigBO);

        RegisterResponseVO registerResponseVO = new RegisterResponseVO();
        registerResponseVO.setId(id.toString());
        return new Result<>(registerResponseVO);
    }

    /**
     * 协议反注册
     *
     * @param appName
     * @param venderName
     * @return
     */
    @DeleteMapping(value = "/unregister")
    @ResponseBody
    @ApiOperation("协议反注册")
    public Result<UnRegisterResponseVO> unregister(@RequestParam("appName") @NotEmpty(message = "appName不能为空") @ApiParam(value = "appName", required = true) String appName,
                                                   @RequestParam("venderName") @NotEmpty(message = "venderName不能为空") @ApiParam(value = "venderName", required = true) String venderName) {
        registerService.unregister(appName, venderName);
        return new Result<>();
    }
}
