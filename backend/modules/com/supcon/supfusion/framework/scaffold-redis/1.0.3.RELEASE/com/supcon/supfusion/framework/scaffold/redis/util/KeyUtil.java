package com.supcon.supfusion.framework.scaffold.redis.util;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import org.springframework.util.StringUtils;

/**
 * @author chenlizhong
 * @date 2020/5/18下午1:46
 * @description
 */
public class KeyUtil {
    /**
     * tenantID名称环境变量名称
     */
    private static final String TENANT_ID_ENV_NAME = "SUPOS_SUPOS_APP_TENANT_ID";
    /**
     * 获取当前租户的key = T[#{tenantId}]
     *
     * @return
     */
    public static String getCurrTenantKey() {
        String tid = RpcContext.getContext().getTenantId();
        if (StringUtils.isEmpty(tid)) {
            String envTenantId = System.getenv(TENANT_ID_ENV_NAME);
            return StringUtils.isEmpty(envTenantId) ? "unknown" : envTenantId;
        } else {
            // tenant前缀定义
            tid = tid.replaceAll(":", "-");
            return tid;
        }
    }

}
