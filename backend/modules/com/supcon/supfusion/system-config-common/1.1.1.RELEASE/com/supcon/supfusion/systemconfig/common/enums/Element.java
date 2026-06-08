package com.supcon.supfusion.systemconfig.common.enums;

/**
 * @author lifangyuan
 */

public enum Element {
    /**
     * 自定义控件
     */
    CUSTOM("custom", -1),
    /**
     * 输入框
     */
    INPUT("input", 0),
    /**
     * 控件checkbox
     */
    CHECKBOX("checkbox", 1),
    /**
     * 控件radio
     */
    RADIO("radio", 2),
    /**
     * 下拉框
     */
    SELECT("select", 3),
    /**
     * 时间框控件
     */
    TIME("time", 4),


    LONG_TEXT("longText", 6);

    private Integer type;
    private String name;

    Element(String name, Integer type) {
        this.name = name;
        this.type = type;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
