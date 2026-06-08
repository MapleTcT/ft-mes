/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cache.annotation.Cacheable
 *  org.springframework.stereotype.Repository
 */
package com.supcon.supos.suposgateway.repository;

import com.alibaba.fastjson.JSON;
import com.supcon.supos.suposgateway.feign.client.IAMClient;
import com.supcon.supos.suposgateway.feign.dto.AccountDTO;
import com.supcon.supos.suposgateway.feign.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class SecretKeyDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecretKeyDao.class);
    @Autowired
    private IAMClient iamClient;

    @Cacheable(value={"secretKeyCache"})
    public String get(String tenantId, String accessKey) {
        Result<AccountDTO> result = this.iamClient.findByAccessKey(tenantId, accessKey);
        if (result == null || result.getData() == null) {
            LOGGER.warn("get account info by accessKey using iamClient, response: {}", (Object)JSON.toJSONString(result));
            return null;
        }
        return result.getData().getSk();
    }
}

