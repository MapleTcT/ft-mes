package com.supcon.supfusion.counter.webapi.vo;

import com.supcon.supfusion.counter.service.bo.RuleFieldBO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class CreateRuleVO {
    private String ruleName;

    private List<RuleFieldVO> ruleFields;

    public List<RuleFieldBO> transferToBatchBO(CreateRuleVO createRuleVO) {
        List<RuleFieldVO> ruleFieldVOS = createRuleVO.getRuleFields();
        List<RuleFieldBO> ruleFieldBOS = new ArrayList<>();
        ruleFieldVOS.forEach(v -> {
            ruleFieldBOS.add(transferToBO(v));
        });
        return ruleFieldBOS;
    }

    public RuleFieldBO transferToBO(RuleFieldVO ruleFieldVO) {
        RuleFieldBO ruleFieldBO = new RuleFieldBO();
        ruleFieldBO.setId(ruleFieldVO.getId());
        ruleFieldBO.setAutoDateRuleType(ruleFieldVO.getAutoDateRule());
        ruleFieldBO.setAutoLength(ruleFieldVO.getAutoLength());
        ruleFieldBO.setAutoType(ruleFieldVO.getAutoType());
        ruleFieldBO.setDateFormatter(ruleFieldVO.getDateFormatter());
        ruleFieldBO.setFieldOrder(ruleFieldVO.getOrder());
        ruleFieldBO.setFieldType(ruleFieldVO.getFieldType());
        ruleFieldBO.setFieldValue(ruleFieldVO.getFieldValue());
        ruleFieldBO.setTheCase(ruleFieldVO.getThecase());
        return ruleFieldBO;
    }
}
