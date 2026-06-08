package com.supcon.supfusion.notification.apiserver.service.rpc;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.apiserver.api.OpenApiInternalApi;
import com.supcon.supfusion.notification.apiserver.api.dto.OpenApiVersionDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@ConditionalOnMissingBean(OpenApiInternalApi.class)
@ServiceApiService("notificationOpenApi")
public class OpenApiInternalApiImpl extends BaseController implements OpenApiInternalApi {

    @Override
    public OpenApiVersionDTO getVersion() {
        OpenApiVersionDTO openApiVersionDTO = new OpenApiVersionDTO();
        openApiVersionDTO.setVersion("1.0.0");
        return openApiVersionDTO;
    }
}
