/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年6月3日 上午9:48:39
 */
public enum TaskStatusEnum {
    /**
     * 进行中
     */
    ACTIVED(88),
    /**
     * 暂停
     */
    SUSPENDED(77);
    
    private final int status;
    
    private TaskStatusEnum(final int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
    
}
