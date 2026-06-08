package com.supcon.supfusion.printer.service.bo;

import lombok.*;


@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ParamConditionBO {

    /**
     * 表单ID（页面id）
     */
    private String id;

    /**
     * 透传条件
     */
    private String params;
    
    private String viewCode;
}