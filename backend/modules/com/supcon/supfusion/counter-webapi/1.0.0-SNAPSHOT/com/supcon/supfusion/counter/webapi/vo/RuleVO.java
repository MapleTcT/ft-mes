package com.supcon.supfusion.counter.webapi.vo;

import com.supcon.supfusion.counter.service.bo.RuleBO;
import com.supcon.supfusion.counter.service.bo.RuleFieldBO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class RuleVO {
    private Long ruleId;

    private String ruleName;

    private List<RuleFieldVO> ruleFields;

    public RuleFieldVO transferToVO(RuleFieldBO ruleFieldBO) {
        RuleFieldVO ruleFieldVO = new RuleFieldVO();
        ruleFieldVO.setId(ruleFieldBO.getId());
        ruleFieldVO.setFieldType(ruleFieldBO.getFieldType());
        ruleFieldVO.setThecase(ruleFieldBO.getTheCase());
        ruleFieldVO.setFieldValue(ruleFieldBO.getFieldValue());
        ruleFieldVO.setDateFormatter(ruleFieldBO.getDateFormatter());
        ruleFieldVO.setAutoType(ruleFieldBO.getAutoType());
        ruleFieldVO.setAutoLength(ruleFieldBO.getAutoLength());
        ruleFieldVO.setOrder(ruleFieldBO.getFieldOrder());
        ruleFieldVO.setAutoDateRule(ruleFieldBO.getAutoDateRuleType());
        return ruleFieldVO;
    }

    public List<RuleFieldVO> transferToBatchVO(List<RuleFieldBO> ruleFieldBOS){
        List<RuleFieldVO> ruleFieldVOS = new ArrayList<>();
        ruleFieldBOS.forEach(v->{
            ruleFieldVOS.add(transferToVO(v));
        });
        return ruleFieldVOS;
    }
}
