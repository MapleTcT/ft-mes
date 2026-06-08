/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.entityconf.entities.ModuleTree
 *  com.supcon.orchid.entityconf.services.ModuleService
 *  com.supcon.orchid.foundation.services.ModuleInfoService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.supcon.orchid.entityconf.controller;

import com.supcon.orchid.entityconf.entities.ModuleTree;
import com.supcon.orchid.entityconf.services.ModuleService;
import com.supcon.orchid.foundation.services.ModuleInfoService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModuleController {
    @Autowired
    private ModuleInfoService moduleInfoService;
    @Autowired
    private ModuleService moduleService;

    @GetMapping(value={"/servicemanager/msModule/module/updatemd5"})
    public void viewPublish(@RequestParam(value="modulecode") String moduleCode) {
        this.moduleInfoService.updateModuleMD5(moduleCode);
    }

    @GetMapping(value={"/servicemanager/msModule/module/modules"})
    public List<ModuleTree> modules() {
        return this.moduleService.getModules();
    }
}

