package com.supcon.supfusion.counter.webapi.vo;

import com.supcon.supfusion.counter.common.constants.AutoDateRuleType;
import com.supcon.supfusion.counter.common.constants.AutoType;
import com.supcon.supfusion.counter.common.constants.FieldType;
import com.supcon.supfusion.counter.common.constants.TheCase;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RuleFieldVO {
    private Long id;

    private FieldType fieldType;

    private TheCase thecase;

    private String fieldValue;

    private String dateFormatter;

    private Integer autoLength;

    private AutoType autoType;

    private AutoDateRuleType autoDateRule;

    private Integer order;

}
