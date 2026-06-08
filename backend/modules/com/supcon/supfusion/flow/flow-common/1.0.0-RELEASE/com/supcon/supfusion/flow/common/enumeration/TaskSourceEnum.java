/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年6月3日 上午9:56:10
 */
public enum TaskSourceEnum {
    
    SUPOS("supOS"),
    
    BAP("bap");
    
    private final String sourceName;
    
    private TaskSourceEnum(final String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceName() {
        return sourceName;
    }
    
}
