package com.supcon.supfusion.printer.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityModelBO {

    /**
     * 属性名称
     */
    private String propertyName;

    /**
     * 属性编码
     */
    private String propertyCode;

    /**
     * 是否包含下级属性
     */
    private Boolean isLeaf;

    private String columnName;
}
