/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cloud.openfeign.FeignClient
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestParam
 */
package com.supcon.supos.suposgateway.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="auth", contextId="branch-office")
public interface BranchOfficeApiClient {
    public static final String API_PREFIX = "/service-api/auth";

    @GetMapping(value={"/service-api/auth/v1/branch-office/authorize/url"})
    public String authorizeUrl(@RequestParam(value="origin_url") String var1, @RequestParam(value="host_url") String var2);
}

