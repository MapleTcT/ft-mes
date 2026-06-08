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
     * 获取当前租户的key = T[#{tenantId}]
     *
     * @return
     */
    public static String getCurrTenantKey() {
        String tid = RpcContext.getContext().getTenantId();
        if (StringUtils.isEmpty(tid)) {
            // 没租户信息-怎么处理？,暂时定义为-1
            return "unknown";
        } else {
            // tenant前缀定义
            tid = tid.replaceAll(":", "-");
            return tid;
        }

    }

}
