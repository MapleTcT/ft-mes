/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年6月8日 下午6:32:46
 */
public enum EntrustStatus {
    /**
     * 进行中
     */
    RUNNING(1),
    /**
     * 已取消
     */
    CANCELLED(2),
    /**
     * 已完成
     */
    COMPLETED(3);
    
    private final int status;
    
    private EntrustStatus(final int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
    
}
