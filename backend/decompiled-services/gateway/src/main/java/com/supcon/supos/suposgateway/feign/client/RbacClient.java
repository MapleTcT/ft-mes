/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cloud.openfeign.FeignClient
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.ResponseBody
 */
package com.supcon.supos.suposgateway.feign.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(value="rbac", path="/service-api/rbac/v1")
public interface RbacClient {
    @GetMapping(value={"/permission/refreshRedisByUser"})
    @ResponseBody
    public Map<String, List<String>> refreshRedisByUser(@RequestParam(value="userId") Long var1, @RequestParam(value="cid") Long var2, @RequestParam(value="tenantId") String var3, @RequestParam(value="method") String var4, @RequestHeader(value="X-Tenant-Id") String var5);
}

