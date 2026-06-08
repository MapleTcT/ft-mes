/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年6月3日 上午9:48:39
 */
public enum ProcessStatusEnum {
    /**
     * 进行中
     */
    ACTIVED(88),
    /**
     * 暂停
     */
    SUSPENDED(77),
    /**
     * 作废
     */
    CANCELLED(99),
    /**
     * 已完成
     */
    COMPLETED(66);
    
    private final int status;
    
    private ProcessStatusEnum(final int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
    
    public static ProcessStatusEnum getStatusEnum(int status) {
        for (ProcessStatusEnum s : ProcessStatusEnum.values()) {
            if (s.getStatus() == status) {
                return s;
            }
        }
        return null;
    }
}
