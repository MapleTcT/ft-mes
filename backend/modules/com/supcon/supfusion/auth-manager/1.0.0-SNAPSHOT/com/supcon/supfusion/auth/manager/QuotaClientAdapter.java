package com.supcon.supfusion.auth.manager;

import java.util.Map;

public interface QuotaClientAdapter {

    Integer getTenantFeatureQuota(String tenant, String key);

    Integer getTenantProductQuota(String tenant, String key);

    Integer getFeatureQuota(String key);

    Integer getProductQuota(String key);

    boolean reportTenantUsedQuota(String tenant, Map<String, Object> quotas);

    boolean reportSystemUsedQuota(Map<String, Object> quotas);
}
