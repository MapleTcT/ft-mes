/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年5月19日 下午1:57:01
 */
public enum DiagramStatusEnum {
    /**
     * 新建
     */
    CREATION(1), 
    /**
     * 已发布
     */
    PUBLISHED(2),
    /**
     * 修改待发布
     */
    DRAFT(3),
    /**
     * 导入状态 -- 从外部导入的流程需要先升版
     */
    OUTER(4),
    /**
     * 作废
     */
    DISABLE(5);
    
    private final int status;
    
    private DiagramStatusEnum(int status) {
        this.status = status;
    }
    
    public int getStatus() {
        return status;
    }
    
    public static DiagramStatusEnum getByStatus(int status) {
        for (DiagramStatusEnum statusEnum : DiagramStatusEnum.values()) {
            if (statusEnum.getStatus() == status) {
                return statusEnum;
            }
        }
        return null;
    }
}
