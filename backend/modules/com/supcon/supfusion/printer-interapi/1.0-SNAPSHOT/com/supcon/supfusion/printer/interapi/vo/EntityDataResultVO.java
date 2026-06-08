package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityDataResultVO extends VO {

    /**
     * 对象实例模型名称
     */
    private String modelCode;

    private String modelName;

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
