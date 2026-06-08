/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.flow.common.po.DiagramPO;

/**
 * @author: zhuangmh
 * @date: 2020年5月21日 下午2:21:18
 */
public class DiagramUpdateWrapper {
    
    private DiagramUpdateWrapper() {
        throw new IllegalStateException("DiagramUpdateWrapper is utility class, do not instantiate");
    }
    
    /**
     * 更新条件为appId, processKey, tenantId
     * @param appId
     * @param processKey 流程编码
     * @param tenantId 租户ID
     * @return LambdaQueryWrapper
     */
    public static LambdaQueryWrapper<DiagramPO> buildEnableWrapper(String appId, String processKey, String tenantId) {
        LambdaQueryWrapper<DiagramPO> queryWrapper = Wrappers.<DiagramPO>lambdaQuery()
                .eq(DiagramPO::getProcessKey, processKey)
                .and(i -> i.eq(DiagramPO::getAppId, appId));
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(DiagramPO::getTenantId, tenantId));
        }
        return queryWrapper;
    }

}
