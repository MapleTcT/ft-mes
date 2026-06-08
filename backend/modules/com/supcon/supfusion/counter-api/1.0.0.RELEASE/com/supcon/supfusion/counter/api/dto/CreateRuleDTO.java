package com.supcon.supfusion.counter.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class CreateRuleDTO {
    private String ruleName;

    private List<RuleFieldDTO> ruleFields;

}
