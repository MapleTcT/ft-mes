package com.supcon.supfusion.printer.service.bo;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityQueryConditionBO {

    /**
     * 数据来源
     */
    private String source;


    /**
     * app或模块编码
     */
    private String appCode;

    /**
     * 查询条件
     */

    private ParamConditionBO condition;

    /**
     * 需要的数据
     */
    private List<EntityConditionBO> resultData;

}
