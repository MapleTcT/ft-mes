/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.stereotype.Component
 *  org.springframework.util.ObjectUtils
 */
package com.supcon.supos.suposgateway.repository;

import com.google.common.collect.Lists;
import com.supcon.supos.suposgateway.enums.Constants;
import com.supcon.supos.suposgateway.feign.dto.LicenseInfoDTO;
import com.supcon.supos.suposgateway.task.LicenseServiceTask;
import com.supcon.supos.suposgateway.utils.Base64Util;
import com.supcon.supos.suposgateway.utils.CollectionUtil;
import com.supcon.supos.suposgateway.utils.date.DateHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
@ConditionalOnProperty(name={"license.enabled"}, havingValue="false")
public class LicenseDao {
    private static final Logger log = LoggerFactory.getLogger(LicenseDao.class);
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private LicenseServiceTask licenseService;
    private static final String SPLIT = "/";
    ConcurrentHashMap<String, LicenseInfoDTO> licenseMap = LicenseServiceTask.licenseMap;

    public void getLicenseFromRedis() {
        List<String> moduleCodeFromProperties = this.licenseService.getModuleCodeFromProperties();
        Map entries = null;
        try {
            entries = this.redisTemplate.opsForHash().entries((Object)"LICENSE:INFO");
        }
        catch (Exception e) {
            log.error("redis\u53d1\u751f\u9519\u8bef ========", (Throwable)e);
            this.setTrial(moduleCodeFromProperties);
        }
        if (ObjectUtils.isEmpty((Object)entries)) {
            log.info("\u6388\u6743redis\u6570\u636e\u88ab\u6e05\u7a7a =====");
            this.setTrial(moduleCodeFromProperties);
        } else {
            ArrayList moduleCodeRedisList = Lists.newArrayList();
            for (Map.Entry redisEntry : entries.entrySet()) {
                String[] keySplit = ((String)redisEntry.getKey()).split(SPLIT);
                String moduleCodeRedis = Base64Util.decode(keySplit[0]);
                String[] split = ((String)redisEntry.getValue()).split(SPLIT);
                String value = Base64Util.decode(split[0]);
                String time = Base64Util.decode(split[1]);
                LicenseInfoDTO licenseInfoDTO = new LicenseInfoDTO();
                licenseInfoDTO.setModuleCode(moduleCodeRedis);
                licenseInfoDTO.setValue(Integer.valueOf(value));
                licenseInfoDTO.setTime(time);
                this.licenseMap.put(moduleCodeRedis, licenseInfoDTO);
                moduleCodeRedisList.add(moduleCodeRedis);
            }
            List<String> same = CollectionUtil.getSame(moduleCodeFromProperties, moduleCodeRedisList);
            List different = (List)CollectionUtil.getDifferent(moduleCodeFromProperties, same);
            this.setTrial(different);
        }
    }

    private void setTrial(List<String> moduleCodeFromProperties) {
        for (String moduleCode : moduleCodeFromProperties) {
            boolean isExpire;
            LicenseInfoDTO licenseInfoDTO = this.licenseMap.get(moduleCode);
            if (ObjectUtils.isEmpty((Object)licenseInfoDTO)) {
                LicenseInfoDTO licenseInfoDTO1 = new LicenseInfoDTO();
                licenseInfoDTO1.setModuleCode(moduleCode);
                licenseInfoDTO1.setTime(String.valueOf(DateHelper.time()));
                licenseInfoDTO1.setValue(-2);
                this.licenseMap.put(moduleCode, licenseInfoDTO1);
                continue;
            }
            if (!licenseInfoDTO.getValue().equals(0) && !licenseInfoDTO.getValue().equals(-2)) continue;
            if (moduleCode.equalsIgnoreCase("supPlant-Dev")) {
                isExpire = DateHelper.time() - Long.parseLong(licenseInfoDTO.getTime()) > Constants.ecEntityTime;
            } else {
                boolean bl = isExpire = DateHelper.time() - Long.parseLong(licenseInfoDTO.getTime()) > Constants.moduleTime;
            }
            if (isExpire) {
                licenseInfoDTO.setValue(-1);
            } else {
                licenseInfoDTO.setValue(-2);
            }
            this.licenseMap.put(moduleCode, licenseInfoDTO);
        }
    }
}

