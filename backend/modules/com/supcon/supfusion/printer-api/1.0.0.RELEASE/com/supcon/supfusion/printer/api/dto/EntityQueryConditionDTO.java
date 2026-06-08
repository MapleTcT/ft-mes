package com.supcon.supfusion.printer.api.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityQueryConditionDTO {

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

    private ParamConditionDTO condition;

    /**
     * 需要的数据
     */
    private List<EntityConditionDTO> resultData;




}
