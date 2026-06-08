package com.supcon.supfusion.rbac.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
public enum MenuInfoTypeEnum implements IEnum<Integer> {
    MENU(0,"menu"),
    PORTLET(1,"portlet");

    MenuInfoTypeEnum(Integer value,String description){
        this.value = value;
        this.description = description;
    }

    private final Integer value;
    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }
}
