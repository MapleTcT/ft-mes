/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年6月13日 下午4:45:32
 */
public enum OperationTypeEnum {
    /**
     * 驳回
     */
    REJECT,
    /**
     * 撤回
     */
    REVOKE,
    /**
     * 重新指派
     */
    APPOINT,
    /**
     * 未知操作
     */
    UNKNOW;
    
    /**
     * @param name
     * @return
     */
    public static OperationTypeEnum getOperationByName(String name) {
        for (OperationTypeEnum operation : OperationTypeEnum.values()) {
            if (operation.name().equals(name)) {
                return operation;
            }
        }
        return UNKNOW;
    }
    
    @Override
    public String toString() {
        return this.name();
    }
}
