package com.supcon.supfusion.auth.manager.Impl;

import com.supcon.supfusion.auth.manager.QuotaClientAdapter;
import com.supos.license.QuotaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class QuotaClientAdapterImpl implements QuotaClientAdapter {

    @Autowired(required=false)
    private QuotaClient quotaClient;

    @Override
    public Integer getTenantFeatureQuota(String tenant, String key) {
        return (Integer) quotaClient.getTenantFeatureQuota(tenant, key);
    }

    @Override
    public Integer getTenantProductQuota(String tenant, String key) {
        return (Integer) quotaClient.getTenantProductQuota(tenant, key);
    }

    @Override
    public Integer getFeatureQuota(String key) {
        return (Integer) quotaClient.getFeatureQuota(key);
    }

    @Override
    public Integer getProductQuota(String key) {
        return (Integer) quotaClient.getProductQuota(key);
    }

    @Override
    public boolean reportTenantUsedQuota(String tenant, Map<String, Object> quotas) {
        return quotaClient.reportTenantUsedQuota(tenant, quotas);
    }

    @Override
    public boolean reportSystemUsedQuota(Map<String, Object> quotas) {
        return quotaClient.reportSystemUsedQuota(quotas);
    }


}
