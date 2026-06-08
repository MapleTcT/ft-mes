package com.supcon.supfusion.auth.service.rpc;

import com.supcon.supfusion.auth.api.IpBlackWhiteApiService;
import com.supcon.supfusion.auth.api.dto.IpBlackWhiteDTO;
import com.supcon.supfusion.auth.service.IpBlackWhiteService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;

/**
 * @author caokele
 */
@ServiceApiService
public class IpBlackWhiteApiServiceImpl extends BaseController implements IpBlackWhiteApiService {
    @Autowired
    private IpBlackWhiteService ipBlackWhiteService;

    @SuppressWarnings("unchecked")
    @Override
    public Result<Boolean> verifyIp(@Valid IpBlackWhiteDTO ipBlackWhiteDTO) {
        boolean isLegal = ipBlackWhiteService.verifyIp(ipBlackWhiteDTO.getIp(), ipBlackWhiteDTO.getCompanyId());
        return Result.custom()
                .data(isLegal)
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }
}
