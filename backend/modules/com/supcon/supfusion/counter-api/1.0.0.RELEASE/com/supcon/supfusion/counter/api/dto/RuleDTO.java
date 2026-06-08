package com.supcon.supfusion.counter.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class RuleDTO {
    private Long ruleId;

    private String ruleName;

    private List<RuleFieldDTO> ruleFields;
}
