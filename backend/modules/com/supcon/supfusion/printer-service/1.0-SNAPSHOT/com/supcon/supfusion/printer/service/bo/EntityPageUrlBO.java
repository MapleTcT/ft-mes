package com.supcon.supfusion.printer.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityPageUrlBO {

    /**
     * 实体名
     */
    private String name;

    /**
     * 实体iframe url
     */
    private String entityUrl;

    /**
     * 来源
     */
    private Integer source;

    /**
     * 是否启用
     */
    private Boolean valid = true;
}
