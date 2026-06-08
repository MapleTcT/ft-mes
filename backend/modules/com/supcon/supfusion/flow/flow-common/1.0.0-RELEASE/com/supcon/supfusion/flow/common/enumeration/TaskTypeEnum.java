/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年6月3日 上午9:56:10
 */
public enum TaskTypeEnum {
    /**
     * 编辑状态待办
     */
    EDIT(4),
    /**
     * 常规待办
     */
    NORMAL(0),
    /**
     * 委托待办
     */
    DELEGATE(2);
    
    private final int type;
    
    private TaskTypeEnum(final int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
    
}
