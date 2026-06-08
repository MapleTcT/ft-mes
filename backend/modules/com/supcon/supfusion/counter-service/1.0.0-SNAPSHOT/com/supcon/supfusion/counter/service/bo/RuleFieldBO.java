package com.supcon.supfusion.counter.service.bo;

import com.supcon.supfusion.counter.common.constants.AutoDateRuleType;
import com.supcon.supfusion.counter.common.constants.AutoType;
import com.supcon.supfusion.counter.common.constants.FieldType;
import com.supcon.supfusion.counter.common.constants.TheCase;
import com.supcon.supfusion.counter.dao.entities.CounterRuleFieldPO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class RuleFieldBO {
    private Long id;

    private FieldType fieldType;

    private TheCase theCase;

    private String fieldValue;

    private String dateFormatter;

    private Integer autoLength;

    private AutoType autoType;

    private AutoDateRuleType autoDateRuleType;

    private Integer fieldOrder;
}
