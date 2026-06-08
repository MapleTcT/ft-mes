package com.supcon.supfusion.auth.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * 分厂单点认证接口
 * @author caokele
 */
@FeignClient(name = "auth", contextId = "branch-office")
public interface BranchOfficeApiService {
    String API_PREFIX = "/service-api/auth";

    /**
     * 分厂单点认证url
     */
    @GetMapping(API_PREFIX + "/v1/branch-office/authorize/url")
    String authorizeUrl(@RequestParam("origin_url") String originUrl, @RequestParam("host_url") String hostUrl);
}
