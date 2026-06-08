/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.annotation.RequestMapping
 */
package com.supcon.greendill.base.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ResourceServiceController {
    @RequestMapping(value={"/baseService/resource/sourceAuthority"})
    public String sourceAuthority() {
        return "sourceAuthority.html";
    }

    @RequestMapping(value={"/baseService/resource/sourceDetail"})
    public String sourceDetail() {
        return "sourceDetail.html";
    }
}

