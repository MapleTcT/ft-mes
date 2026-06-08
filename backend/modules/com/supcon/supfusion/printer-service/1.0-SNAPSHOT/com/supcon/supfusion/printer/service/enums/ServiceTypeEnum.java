package com.supcon.supfusion.printer.service.enums;

public enum  ServiceTypeEnum {
    APP(1),

    PAGE(2),

    MODEL(3),

    PROPERTY(4),

    DATA(5);

    private final Integer code;

    ServiceTypeEnum(Integer code){
        this.code = code;
    }

    public Integer getCode(){
        return code;
    }
}
