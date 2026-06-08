/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.supcon.greendill.base.controller;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdpCommonController {
    private static final String SUPPLANT_VERSION = "SUPPLANT_VERSION";
    private static final String ADP_VERSION = "ADP_VERSION";

    @GetMapping(value={"/baseService/platform/info"})
    public Map<String, Object> getPlatformInfo() {
        String supplantVersion = System.getenv(SUPPLANT_VERSION);
        String adpVersion = System.getenv(ADP_VERSION);
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("supplantVersion", StringUtils.isNotEmpty((CharSequence)supplantVersion) ? supplantVersion : null);
        resultMap.put("adpVersion", StringUtils.isNotEmpty((CharSequence)adpVersion) ? adpVersion : null);
        return resultMap;
    }
}

