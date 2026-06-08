package com.supcon.supfusion.printer.service.bo;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityDataResultBO {

    /**
     * 对象实例模型名称
     */
    private String modelCode;

    /**
     * 属性编码
     */
    private String propertyCode;

    /**
     * 属性名称
     */
    private String propertyName;

    /**
     * 属性类型
     */
    private String propertyType;

    /**
     * 属性值
     */
    private List<String> propertyValue;
}
