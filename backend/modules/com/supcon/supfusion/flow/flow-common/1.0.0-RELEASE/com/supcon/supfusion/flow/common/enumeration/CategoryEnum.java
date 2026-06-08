/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年9月2日 下午2:37:37
 */
public enum CategoryEnum {
    /**
     * 任务
     */
    TASK("task", "activity_name"),
    /**
     * 流程
     */
    PROCESS("process", "process_key");
    
    private final String name;
    /**
     * 数据库列名,进行分组
     */
    private final String column;
    
    private CategoryEnum(String name, String column) {
        this.name = name;
        this.column = column;
    }

    public String getName() {
        return name;
    }
    
    public String getColumn() {
        return column;
    }

    public static CategoryEnum getCategory(String name) {
        for (CategoryEnum c : CategoryEnum.values()) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return TASK;
    }
}   
