/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.ec.entities.Model
 *  com.supcon.orchid.entityconf.services.BAPGenerateService
 *  com.supcon.orchid.entityconf.services.ModelService
 *  com.supcon.orchid.utils.StringUtils
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.supcon.orchid.entityconf.controller;

import com.supcon.orchid.ec.entities.Model;
import com.supcon.orchid.entityconf.services.BAPGenerateService;
import com.supcon.orchid.entityconf.services.ModelService;
import com.supcon.orchid.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImportTemplateController {
    @Autowired
    private ModelService modelService;
    @Autowired
    private BAPGenerateService generateService;

    @GetMapping(value={"/servicemanager/msModule/import-template/publish"})
    public String publish(@RequestParam(value="modelCode") String modelCode) {
        Model model = null;
        if (!StringUtils.isEmpty((String)modelCode) && (model = this.modelService.getModel(modelCode)) != null) {
            this.generateService.generateImportTemplateXMLInside(model.getModuleCode());
        }
        return "success";
    }
}

