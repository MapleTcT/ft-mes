package com.supcon.supfusion.counter.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class AllocRuleDTO {
    private Long ruleId;

    private Integer applyCount;

    private List<AllocRuleParamDTO> params;

}
