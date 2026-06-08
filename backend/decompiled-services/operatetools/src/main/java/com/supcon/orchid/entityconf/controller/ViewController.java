/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.ProjectFlagHolder
 *  com.supcon.orchid.entityconf.services.ViewService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.supcon.orchid.entityconf.controller;

import com.supcon.orchid.ProjectFlagHolder;
import com.supcon.orchid.entityconf.services.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ViewController {
    @Autowired
    private ViewService viewService;

    @PostMapping(value={"/servicemanager/msModule/view/publish"})
    public String viewPublish(@RequestParam(value="viewCode") String viewCode, @RequestParam(value="isProj", required=false) Boolean isProj) throws Exception {
        if (null != isProj && isProj.booleanValue()) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        this.viewService.viewPublish(viewCode);
        if (null != isProj && isProj.booleanValue()) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        this.viewService.refreshPermission(viewCode);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "success";
    }
}

