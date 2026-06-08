/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年8月25日 上午9:58:28
 */
public enum AppUpgradeStatus {
    /**
     * 正在升级
     */
    UPGRADING("3"),
    /**
     * 升级成功
     */
    UPGRADE_SUCCESS("4"),
    /**
     * 升级失败
     */
    UPGRADE_FAIL("5");
    
    
    private final String status;
    
    private AppUpgradeStatus(final String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
    
}
