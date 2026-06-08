/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.annotation.RequestMapping
 */
package com.supcon.orchid.entityconf.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageController {
    @RequestMapping(value={"/servicemanager/msModule/misPage"})
    public String page() {
        return "serviceconfig.html";
    }

    @RequestMapping(value={"/servicemanager/msModule/page"})
    public String pagePublish() {
        return "serviceconfig.html";
    }

    @RequestMapping(value={"/servicemanager/msModule/appPage"})
    public String appPage() {
        return "appConfig.html";
    }
}

