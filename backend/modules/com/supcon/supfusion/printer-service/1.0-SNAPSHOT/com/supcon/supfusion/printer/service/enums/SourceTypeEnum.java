package com.supcon.supfusion.printer.service.enums;

public enum  SourceTypeEnum {

    SUPOS(1),

    SUPPLANT(2);

    private final Integer code;

    SourceTypeEnum(Integer code){
        this.code = code;
    }

    public Integer getCode(){
        return code;
    }
}
