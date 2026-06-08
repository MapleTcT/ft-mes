package com.supcon.supfusion.counter.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class AllocRuleParamDTO {
    private Long ruleFieldId;

    private String ruleFieldValue;
}
