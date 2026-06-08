package com.supcon.supfusion.printer.api.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ParamConditionDTO {

    /**
     * 表单ID（页面id）
     */
    private String id;

    /**
     * 透传条件
     */
    private String params;
}