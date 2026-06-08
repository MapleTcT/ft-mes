/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSONObject
 *  org.springframework.cloud.openfeign.FeignClient
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestParam
 */
package com.supcon.supos.suposgateway.feign.client;

import com.alibaba.fastjson.JSONObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value="auth", path="/service-api/auth/v1", contextId="userAdmin")
public interface UserAdminClient {
    @DeleteMapping(value={"/online-user"})
    public String removeOnlineUserByTicket(@RequestParam(value="ticket") String var1, @RequestHeader(value="X-Tenant-Id") String var2);

    @PostMapping(value={"/ip-black-white/verify"})
    public String verifyIp(JSONObject var1, @RequestHeader(value="X-Tenant-Id") String var2);
}

