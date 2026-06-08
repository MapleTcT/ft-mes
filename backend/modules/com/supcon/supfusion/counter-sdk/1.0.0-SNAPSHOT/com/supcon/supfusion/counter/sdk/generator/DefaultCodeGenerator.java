package com.supcon.supfusion.counter.sdk.generator;

import com.supcon.supfusion.counter.sdk.CodeGenerator;
import com.supcon.supfusion.counter.sdk.CounterServiceAdapter;
import com.supcon.supfusion.counter.sdk.ParamSupplier;
import com.supcon.supfusion.counter.sdk.bo.AllocCodeBO;
import com.supcon.supfusion.counter.sdk.bo.CodeResult;
import com.supcon.supfusion.counter.sdk.bo.RuleBO;
import com.supcon.supfusion.counter.sdk.bo.RuleParamBO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Setter
@Getter
@Component
public class DefaultCodeGenerator implements CodeGenerator {

    @Autowired
    private CounterServiceAdapter counterServiceAdapter;

    public DefaultCodeGenerator() {
    }

    @Override
    public CodeResult allocate(long ruleId, int applyCount, ParamSupplier supplier) {
        RuleBO ruleBO = counterServiceAdapter.find(ruleId);
        CodeResult codeResult = new CodeResult();
        if (ruleBO != null) {
            if(ruleBO.isHasSequenceField()){
                AllocCodeBO allocCodeBO = counterServiceAdapter.allocateWithSequeue(ruleBO,applyCount,fillAndGet(ruleBO));
                codeResult.setBatchId(allocCodeBO.getBatchId());
                codeResult.setWithSequence(true);
                Set<String> set = new HashSet<>();
                allocCodeBO.getCodes().forEach(v->{
                    set.add(v);
                });
                codeResult.setCodes(set);
            }else {
                String codeStr = counterServiceAdapter.allocateNoSequeue(ruleBO,fillAndGet(ruleBO));
                codeResult.setWithSequence(false);
                codeResult.setBatchId(null);
                Set<String> set = new HashSet();
                set.add(codeStr);
                codeResult.setCodes(set);
            }
        }
        return codeResult;
    }

    @Override
    public void rollback(long batchId) {
        counterServiceAdapter.rollbackWithSequeue(batchId);
    }

    public String allocCodeHasNoSequeue(RuleBO ruleBO, List<RuleParamBO> paramBOList) {

        return "";
    }

    @Override
    public List<RuleParamBO> fillAndGet(RuleBO ruleBO) {
        List<RuleParamBO> ruleParamBOS = new ArrayList<>();
        if(ruleBO != null && ruleBO.getRuleFields() !=null && ruleBO.getRuleFields().size() > 0) {
            ruleBO.getRuleFields().forEach(v->{
                RuleParamBO ruleParamBO = new RuleParamBO();
                ruleParamBO.setRuleFieldId(v.getId());
                ruleParamBO.setRuleFieldValue(v.getFieldValue());
                ruleParamBOS.add(ruleParamBO);
            });
        }
        return ruleParamBOS;
    }
}
