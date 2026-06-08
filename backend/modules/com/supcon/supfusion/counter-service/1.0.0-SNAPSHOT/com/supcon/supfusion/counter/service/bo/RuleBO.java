package com.supcon.supfusion.counter.service.bo;

import com.supcon.supfusion.counter.api.dto.RuleDTO;
import com.supcon.supfusion.counter.api.dto.RuleFieldDTO;
import com.supcon.supfusion.counter.dao.entities.CounterRuleFieldPO;
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

    public CounterRuleFieldPO transferToPO (RuleFieldBO bo) {
        CounterRuleFieldPO counterRuleFieldPO = new CounterRuleFieldPO();
        counterRuleFieldPO.setId(bo.getId());
        counterRuleFieldPO.setFieldType(bo.getFieldType());
        counterRuleFieldPO.setThecase(bo.getTheCase());
        counterRuleFieldPO.setFieldValue(bo.getFieldValue());
        counterRuleFieldPO.setDateFormatter(bo.getDateFormatter());
        counterRuleFieldPO.setAutoLength(bo.getAutoLength());
        counterRuleFieldPO.setAutoType(bo.getAutoType());
        counterRuleFieldPO.setAutoDateRule(bo.getAutoDateRuleType());
        counterRuleFieldPO.setFieldOrder(bo.getFieldOrder());
        return counterRuleFieldPO;
    }

    public List<CounterRuleFieldPO> transferToBatchPO(List<RuleFieldBO> bos) {
        List<CounterRuleFieldPO> counterRuleFieldPOS = new ArrayList<>();
        bos.forEach(v ->{
            CounterRuleFieldPO counterRuleFieldPO = transferToPO(v);
            counterRuleFieldPOS.add(counterRuleFieldPO);
        });
        return counterRuleFieldPOS;
    }


    public RuleFieldBO transferToBO(CounterRuleFieldPO counterRuleFieldPO){
        RuleFieldBO ruleFieldBO = new RuleFieldBO();
        ruleFieldBO.setId(counterRuleFieldPO.getId());
        ruleFieldBO.setFieldType(counterRuleFieldPO.getFieldType());
        ruleFieldBO.setTheCase(counterRuleFieldPO.getThecase());
        ruleFieldBO.setFieldValue(counterRuleFieldPO.getFieldValue());
        ruleFieldBO.setDateFormatter(counterRuleFieldPO.getDateFormatter());
        ruleFieldBO.setAutoLength(counterRuleFieldPO.getAutoLength());
        ruleFieldBO.setAutoType(counterRuleFieldPO.getAutoType());
        ruleFieldBO.setAutoDateRuleType(counterRuleFieldPO.getAutoDateRule());
        ruleFieldBO.setFieldOrder(counterRuleFieldPO.getFieldOrder());
        return ruleFieldBO;
    }

    public List<RuleFieldBO> transferToBatchBO(List<CounterRuleFieldPO> bos) {
        List<RuleFieldBO> ruleFieldBOS = new ArrayList<>();
        bos.forEach(v ->{
            RuleFieldBO ruleFieldBO = transferToBO(v);
            ruleFieldBOS.add(ruleFieldBO);
        });
        return ruleFieldBOS;
    }

    public RuleDTO transferToDTO(RuleBO ruleBO){
        RuleDTO ruleDTO = new RuleDTO();
        ruleDTO.setRuleId(ruleBO.getRuleId());
        ruleDTO.setRuleName(ruleBO.getRuleName());
        ruleDTO.setRuleFields(transferToBatchFieldDTO(ruleBO.getRuleFields()));
        return ruleDTO;
    }

    public List<RuleFieldDTO> transferToBatchFieldDTO(List<RuleFieldBO> ruleFields){
        List<RuleFieldDTO> ruleFieldDTOS = new ArrayList<>();
        if (ruleFields != null) {
            ruleFields.forEach(v->{
                ruleFieldDTOS.add(transferToFieldDTO(v));
            });
        }
        return ruleFieldDTOS;
    }

    public RuleFieldDTO transferToFieldDTO(RuleFieldBO ruleFieldBO){
        RuleFieldDTO ruleFieldDTO = new RuleFieldDTO();
        ruleFieldDTO.setId(ruleFieldBO.getId());
        ruleFieldDTO.setFieldType(ruleFieldBO.getFieldType());
        ruleFieldDTO.setFieldValue(ruleFieldBO.getFieldValue());
        ruleFieldDTO.setDateFormatter(ruleFieldBO.getDateFormatter());
        ruleFieldDTO.setAutoLength(ruleFieldBO.getAutoLength());
        ruleFieldDTO.setAutoDateRuleType(ruleFieldBO.getAutoDateRuleType());
        ruleFieldDTO.setAutoType(ruleFieldBO.getAutoType());
        ruleFieldDTO.setTheCase(ruleFieldBO.getTheCase());
        ruleFieldDTO.setOrder(ruleFieldBO.getFieldOrder());
        return ruleFieldDTO;
    }

}
