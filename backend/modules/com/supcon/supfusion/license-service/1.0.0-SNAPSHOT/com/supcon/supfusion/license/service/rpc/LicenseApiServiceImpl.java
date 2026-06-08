package com.supcon.supfusion.license.service.rpc;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.license.api.LicenseApiService;
import com.supcon.supfusion.license.api.dto.LicenseInfoDTO;
import com.supcon.supfusion.license.common.utils.security.Base64Util;
import com.supcon.supfusion.license.service.LicenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@ServiceApiService
@Slf4j
public class LicenseApiServiceImpl extends BaseController implements LicenseApiService {

    @Autowired
    private LicenseService licenseService;

    @SuppressWarnings("unchecked")
    @Override
    public Result registerLicenseInfo(LicenseInfoDTO licenseInfoDTO) {
        licenseService.registerLicenseInfo(licenseInfoDTO);
        return new Result();
    }

    @Override
    public Integer getLicenseInfoByLicenseKey(LicenseInfoDTO licenseInfoDTO) {
        return licenseService.getValueFromSCDog(licenseInfoDTO.getLicenseKey());
    }

    @Override
    public Integer getLicenseInfoFromRegistry(LicenseInfoDTO licenseInfoDTO) {
        return licenseService.getLicenseInfoByLicenseKeyFromRegistry(licenseInfoDTO.getLicenseKey());
    }
}
