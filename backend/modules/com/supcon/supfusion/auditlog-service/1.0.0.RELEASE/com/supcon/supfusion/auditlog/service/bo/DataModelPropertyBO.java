package com.supcon.supfusion.auditlog.service.bo;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * 数据模型属性
 * @author caokele
 */
@Getter
@Setter
@ToString
public class DataModelPropertyBO {
    /**
     * 属性名
     */
    private String propertyName;

    /**
     * 当前值
     */
    private String currentValue;

    /**
     * 历史值
     */
    private String historyValue;
}
