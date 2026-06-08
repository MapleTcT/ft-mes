package com.supcon.supfusion.license.api;


import com.supcon.supfusion.license.api.dto.LicenseInfoDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@FeignClient(name = "supplant-license",contextId = "licenseApiService")
public interface LicenseApiService {

    String API_PREFIX = "/service-api/license";

    /**
     * app服务启动时，向nacos注册moduleCode和软件狗key相关信息
     */
    @PostMapping(API_PREFIX + "/v1/registerLicenseInfo")
    @ResponseBody
    Result registerLicenseInfo(@Valid @RequestBody LicenseInfoDTO licenseInfoDTO);

    /**
     * 根据licenseKey从狗中获取值
     */
    @PostMapping(API_PREFIX + "/v1/lincenseInfo")
    @ResponseBody
    Integer getLicenseInfoByLicenseKey(@Valid @RequestBody LicenseInfoDTO licenseInfoDTO);

    /**
     * 根据licenseKey从已注册的授权信息中获取值
     */
    @PostMapping(API_PREFIX + "/v1/lincenseInfoFromRegistry")
    @ResponseBody
    Integer getLicenseInfoFromRegistry(@Valid @RequestBody LicenseInfoDTO licenseInfoDTO);
}
