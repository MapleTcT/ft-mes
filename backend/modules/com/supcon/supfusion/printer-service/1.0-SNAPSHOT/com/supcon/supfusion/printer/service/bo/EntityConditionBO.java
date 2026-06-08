package com.supcon.supfusion.printer.service.bo;

import lombok.*;


@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityConditionBO {

    /**
     * 对象实例模型名称
     */
    private String modelCode;

    /**
     * 属性名称
     */
    private String propertyCode;

    private String tableName;

    private String columnName;
}