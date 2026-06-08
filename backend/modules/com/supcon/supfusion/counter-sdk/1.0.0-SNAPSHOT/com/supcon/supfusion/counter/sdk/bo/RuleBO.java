package com.supcon.supfusion.counter.sdk.bo;

import com.supcon.supfusion.counter.api.dto.RuleFieldDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class RuleBO {
    private Long ruleId;
    private String ruleName;
    private List<RuleFieldBO> ruleFields;

    private boolean hasSequenceField;

    public List<RuleFieldBO> transferToBatchBO(List<RuleFieldDTO> ruleFields){
        List<RuleFieldBO> ruleFieldBOS = new ArrayList<>();
        ruleFields.forEach(v->{
            ruleFieldBOS.add(transferToBatchBO(v));
        });
        return ruleFieldBOS;
    }

    public RuleFieldBO transferToBatchBO(RuleFieldDTO ruleFieldDTO) {
        RuleFieldBO ruleFieldBO = new RuleFieldBO();
        ruleFieldBO.setId(ruleFieldDTO.getId());
        ruleFieldBO.setFieldType(ruleFieldDTO.getFieldType());
        ruleFieldBO.setTheCase(ruleFieldDTO.getTheCase());
        ruleFieldBO.setFieldValue(ruleFieldDTO.getFieldValue());
        ruleFieldBO.setDateFormatter(ruleFieldDTO.getDateFormatter());
        ruleFieldBO.setAutoLength(ruleFieldDTO.getAutoLength());
        ruleFieldBO.setAutoType(ruleFieldDTO.getAutoType());
        ruleFieldBO.setAutoDateRuleType(ruleFieldDTO.getAutoDateRuleType());
        ruleFieldBO.setFieldOrder(ruleFieldDTO.getOrder());
        return ruleFieldBO;
    }
}
