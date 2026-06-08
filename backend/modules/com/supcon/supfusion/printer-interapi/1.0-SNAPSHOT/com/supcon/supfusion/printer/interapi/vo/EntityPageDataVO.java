package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityPageDataVO extends VO {

    /**
     * 实体服务来源
     */
    private String source;

    /**
     * 模块编码
     */
    private String appCode;

    /**
     *
     */
    private String entityCode;
}
