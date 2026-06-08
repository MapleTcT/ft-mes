/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cloud.openfeign.FeignClient
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestParam
 */
package com.supcon.supos.suposgateway.feign.client;

import com.supcon.supos.suposgateway.feign.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value="flow-service", path="/service-api/flow-service")
public interface WorkFlowClient {
    @GetMapping(value={"/verificationProcessOwner"})
    public Result<Boolean> verificationProcessOwner(@RequestParam(value="pendingId") Long var1, @RequestParam(value="userId") Long var2);
}

