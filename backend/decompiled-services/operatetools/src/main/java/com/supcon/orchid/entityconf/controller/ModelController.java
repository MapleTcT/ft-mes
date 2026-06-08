/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.entityconf.entities.vo.ModelAuditLogReqVO
 *  com.supcon.orchid.entityconf.services.ModelService
 *  io.swagger.annotations.Api
 *  io.swagger.annotations.ApiOperation
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RestController
 */
package com.supcon.orchid.entityconf.controller;

import com.supcon.orchid.entityconf.entities.vo.ModelAuditLogReqVO;
import com.supcon.orchid.entityconf.services.ModelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Api(description="\u6a21\u578b\u63a5\u53e3")
@RestController
public class ModelController {
    @Autowired
    private ModelService modelService;

    @ApiOperation(value="\u6a21\u578b\u542f\u7528/\u4e0d\u542f\u7528\u5ba1\u8ba1\u65e5\u5fd7", httpMethod="PUT")
    @PostMapping(value={"/servicemanager/model/audit-log"})
    public void enableAuditLog(@RequestBody ModelAuditLogReqVO modelAuditLogReqVO) {
        List modelCodes = modelAuditLogReqVO.getModelCodes();
        Boolean enable = modelAuditLogReqVO.getEnable();
        if (modelCodes == null || enable == null || modelCodes.isEmpty()) {
            return;
        }
        this.modelService.enableAuditLog(modelCodes, enable);
    }
}

