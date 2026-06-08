/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.foundation.utils.StringUtil
 *  com.supcon.supfusion.framework.cloud.common.context.RpcContext
 *  com.supcon.supfusion.rbac.api.IMenuInfoApiService
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.core.annotation.Order
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.stereotype.Component
 */
package com.supcon.greendill.config;

import com.supcon.greendill.base.util.JSONHelper;
import com.supcon.orchid.foundation.utils.StringUtil;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(value=1)
public class MenuDataInitialize
implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(MenuDataInitialize.class);
    @Autowired
    private IMenuInfoApiService menuInfoApiService;
    @Value(value="${base.initsupos:true}")
    private Boolean isSupos;
    @Value(value="${supfusion.environment.is-saas:'false'}")
    private String isSaas;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void run(String ... args) throws Exception {
        JSONHelper jsonHelper = new JSONHelper();
        try {
            String tenantId = System.getenv("SUPOS_SUPOS_APP_TENANT_ID");
            if (StringUtil.isEmpty((Object)tenantId)) {
                tenantId = "dt";
            }
            log.info("tenantId is " + tenantId);
            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setTenantId(tenantId);
            String resourceSupplant = (String)this.redisTemplate.opsForValue().get((Object)"integration.supplant.enabled");
            log.info("integration.supplant.enabled  value is " + resourceSupplant);
            this.redisTemplate.opsForValue().set((Object)"integration.supplant.enabled", (Object)"true");
            log.info("redis add integration.supplant.enabled true ");
            if (this.isSupos.booleanValue()) {
                log.info("begin to initialize module menus ");
                String json = "";
                if (new Boolean(this.isSaas).booleanValue()) {
                    log.info("saas environment ");
                    json = jsonHelper.ResolveJsonFileToString("initdata_saas.json");
                } else {
                    log.info("non saas environment ");
                    json = jsonHelper.ResolveJsonFileToString("initdata_bap.json");
                }
                this.menuInfoApiService.initModuleMenu(json);
            }
        }
        catch (Exception e) {
            log.info("fail to initialize module menus ", (Throwable)e);
        }
    }
}

