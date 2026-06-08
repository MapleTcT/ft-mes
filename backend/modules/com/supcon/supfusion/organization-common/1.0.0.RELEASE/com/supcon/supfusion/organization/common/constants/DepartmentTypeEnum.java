package com.supcon.supfusion.organization.common.constants;

public enum DepartmentTypeEnum {
    DEPARTMENT_NORMAL(0, "普通部门"),
    DEPARTMENT_EMERGENCY(1, "应急部门");
    /**
     * 部门类型码
     */
    private Integer type;
    /**
     * 部门类型名称
     */
    private String typeName;

    DepartmentTypeEnum(Integer type, String typeName) {
        this.type = type;
        this.typeName = typeName;
    }

    public Integer getType() {
        return type;
    }

    public String getTypeName() {

        return typeName;
    }
}
