package com.supcon.supfusion.notification.admin.service.rpc;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.admin.api.RegisterInternalApi;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolConfigDTO;
import com.supcon.supfusion.notification.admin.api.dto.RegisterResponseDTO;
import com.supcon.supfusion.notification.admin.api.dto.UnRegisterResponseDTO;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.admin.service.RegisterService;
import com.supcon.supfusion.notification.admin.service.bo.ProtocolConfigBO;
import com.supcon.supfusion.notification.admin.service.bo.ProtocolTemplateBO;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

@ServiceApiService
public class RegisterInternalApiImpl extends BaseController implements RegisterInternalApi {
    @Resource(name = "adminRegisterServiceImpl")
    private RegisterService registerService;

    @Override
    public Result<RegisterResponseDTO> register(ProtocolConfigDTO protocolConfigDTO) throws NotificationAdminExecption {
        ProtocolConfigBO protocolConfigBO = BeanCopyUtil.copyBeanProperties(protocolConfigDTO, ProtocolConfigBO::new);
        protocolConfigBO.setTemplates(BeanCopyUtil.copyListProperties(protocolConfigDTO.getTemplates(), ProtocolTemplateBO::new));
        Long id = registerService.register(protocolConfigBO);

        RegisterResponseDTO registerResponseDTO = new RegisterResponseDTO();
        registerResponseDTO.setId(id.toString());
        return new Result<>(registerResponseDTO);
    }

    @Override
    public Result<UnRegisterResponseDTO> unregister(String appName, String venderName) {
        registerService.unregister(appName, venderName);
        return new Result<>();
    }
}
