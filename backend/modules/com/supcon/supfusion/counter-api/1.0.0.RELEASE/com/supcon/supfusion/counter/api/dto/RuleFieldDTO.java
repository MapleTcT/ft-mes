package com.supcon.supfusion.counter.api.dto;

import com.supcon.supfusion.counter.common.constants.AutoDateRuleType;
import com.supcon.supfusion.counter.common.constants.AutoType;
import com.supcon.supfusion.counter.common.constants.FieldType;
import com.supcon.supfusion.counter.common.constants.TheCase;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RuleFieldDTO {
    private Long id;

    private FieldType fieldType;

    private TheCase theCase;

    private String fieldValue;

    private String dateFormatter;

    private Integer autoLength;

    private AutoType autoType;

    private AutoDateRuleType autoDateRuleType;

    private Integer order;
}
