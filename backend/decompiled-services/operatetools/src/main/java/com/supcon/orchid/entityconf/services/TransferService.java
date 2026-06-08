/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cloud.openfeign.FeignClient
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestMethod
 */
package com.supcon.orchid.entityconf.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="servicemanager")
public interface TransferService {
    @RequestMapping(value={"/actuator/shutdown"}, method={RequestMethod.POST})
    public String stop();

    @RequestMapping(value={"/actuator/env/PID"}, method={RequestMethod.GET})
    public String getPid();
}

