package com.supcon.supfusion.printer.api.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityConditionDTO {

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