package com.supcon.supfusion.rbac.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;


import static com.supcon.supfusion.rbac.api.Constants.Constants.API_PREFIX;

@Validated
@FeignClient(name = "rbac",contextId = "appCompanyRef")
public interface IAppCompanyRefApiService {

    @DeleteMapping(API_PREFIX + "/app/{appId}/companies")
    @ResponseBody
    void deleteAppCompanyRef(@PathVariable("appId") String appId);
}
