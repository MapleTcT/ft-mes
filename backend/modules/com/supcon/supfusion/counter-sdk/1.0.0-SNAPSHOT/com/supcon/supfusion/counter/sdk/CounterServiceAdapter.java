package com.supcon.supfusion.counter.sdk;

import com.supcon.supfusion.counter.api.CounterService;
import com.supcon.supfusion.counter.api.dto.AllocCodeDTO;
import com.supcon.supfusion.counter.api.dto.AllocRuleDTO;
import com.supcon.supfusion.counter.api.dto.AllocRuleParamDTO;
import com.supcon.supfusion.counter.api.dto.RuleDTO;
import com.supcon.supfusion.counter.sdk.bo.AllocCodeBO;
import com.supcon.supfusion.counter.sdk.bo.RuleBO;
import com.supcon.supfusion.counter.sdk.bo.RuleParamBO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
public class CounterServiceAdapter {

    @Autowired
    private CounterService counterService;

    public CounterServiceAdapter() {
    }

    public RuleBO find(Long ruleId){
        Result<RuleDTO> result = counterService.findById(ruleId);
        RuleDTO ruleDTO = result.getData();
        if (ruleDTO != null) {
            RuleBO ruleBO = new RuleBO();
            ruleBO.setRuleId(ruleDTO.getRuleId());
            ruleBO.setRuleName(ruleDTO.getRuleName());
            ruleBO.setRuleFields(ruleBO.transferToBatchBO(ruleDTO.getRuleFields()));
            if (ruleDTO.getRuleFields() == null || ruleDTO.getRuleFields().size() == 0) {
                ruleBO.setHasSequenceField(false);
            }
            return ruleBO;
        }
        return null;
    }

    public AllocCodeBO allocateWithSequeue(RuleBO rule, int applyCount, List<RuleParamBO> params){
        AllocRuleDTO allocRuleDTO = new AllocRuleDTO();
        allocRuleDTO.setRuleId(rule.getRuleId());
        allocRuleDTO.setApplyCount(applyCount);
        List<AllocRuleParamDTO> dtoList = new ArrayList<>();
        params.forEach(v->{
            AllocRuleParamDTO allocRuleParamDTO =  new AllocRuleParamDTO();
            allocRuleParamDTO.setRuleFieldId(v.getRuleFieldId());
            allocRuleParamDTO.setRuleFieldValue(v.getRuleFieldValue());
            dtoList.add(allocRuleParamDTO);
        });
        allocRuleDTO.setParams(dtoList);
        Result<AllocCodeDTO> result = counterService.allocate(allocRuleDTO);
        AllocCodeDTO allocCodeDTO = result.getData();
        AllocCodeBO allocCodeBO = new AllocCodeBO();
        if(allocCodeDTO != null){
            allocCodeBO.setBatchId(allocCodeDTO.getBatchId());
            allocCodeBO.setCodes(allocCodeDTO.getCodes());
            return allocCodeBO;
        }
        return null;
    }

    public void rollbackWithSequeue(long batchId){
        counterService.cancel(batchId);
    }

    public String allocateNoSequeue(RuleBO rule, List<RuleParamBO> params){
        return "";
    }

}
