package com.supcon.supfusion.file.server.common.utils;

import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

@Slf4j
public class TenantUtil {

    public static String getTenantId() {
        return getCommonTenantId() + Constants.BUCKET;
    }

    public static String getCommonTenantId() {
        String tenantId = RpcContext.getContext().getTenantId();
        log.info("初始tenantId:{}", tenantId);
        if (ObjectUtils.isEmpty(tenantId)) {
            if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
                tenantId = Constants.DT_TENANT_ID;
            } else {
                tenantId = ObjectUtils.isEmpty(tenantId) ? Constants.DT_TENANT_ID : System.getenv("SUPOS_SUPOS_APP_TENANT_ID");
            }
        }
        log.info("最后tenantId:{}", tenantId);
        return tenantId;
    }
}
