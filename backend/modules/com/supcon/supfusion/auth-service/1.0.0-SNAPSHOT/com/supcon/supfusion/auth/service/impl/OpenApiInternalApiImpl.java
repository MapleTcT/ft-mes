package com.supcon.supfusion.auth.service.impl;

import com.supcon.supfusion.auth.api.OpenApiInternalApi;
import com.supcon.supfusion.auth.api.dto.OpenApiVersionDTO;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import org.springframework.stereotype.Service;

@Service
public class OpenApiInternalApiImpl extends BaseController implements OpenApiInternalApi {
    @Override
    public OpenApiVersionDTO getVersion() {
        OpenApiVersionDTO openApiVersionDTO = new OpenApiVersionDTO();
        openApiVersionDTO.setVersion("1.0.0");
        return openApiVersionDTO;
    }

}
