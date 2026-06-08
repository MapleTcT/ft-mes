/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cloud.openfeign.FeignClient
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestParam
 */
package com.supcon.supos.suposgateway.feign.client;

import com.supcon.supos.suposgateway.feign.dto.AccountDTO;
import com.supcon.supos.suposgateway.feign.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value="supos-iam", path="/service-api/supos-iam/v1")
public interface IAMClient {
    @GetMapping(value={"/account"})
    public Result<AccountDTO> findByAccessKey(@RequestHeader(value="X-Tenant-Id") String var1, @RequestParam(value="accessKey") String var2);
}

