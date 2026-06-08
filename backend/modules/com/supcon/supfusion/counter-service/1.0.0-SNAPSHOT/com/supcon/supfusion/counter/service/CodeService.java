package com.supcon.supfusion.counter.service;

import com.supcon.supfusion.counter.dao.entities.CounterRuleSequencePO;
import com.supcon.supfusion.counter.service.bo.AllocRuleParamBO;
import com.supcon.supfusion.counter.service.bo.CodeParameterBO;
import com.supcon.supfusion.counter.service.bo.RuleBO;
import com.supcon.supfusion.counter.service.bo.RuleFieldBO;

import java.util.List;

public interface CodeService {
    long getAndIncMaxSequence(long ruleId,long seqFieldId,String seqRef,int applyCount);

    List<String> generate(RuleBO ruleBO,int applyCount,long beginSequeue,List<AllocRuleParamBO> params);

    void cancel(long ruleId,long lastBatchId,int applyCount);

    String getReference(RuleBO ruleBO,List<AllocRuleParamBO> params);

    StringBuilder getFieldValue(RuleFieldBO ruleFieldBO);

    CounterRuleSequencePO findRuleSequence(Long ruleId,String reference);

    int insertSequence(CounterRuleSequencePO counterRuleSequencePO);

    int updateSequence(CounterRuleSequencePO counterRuleSequencePO);

    List<String> getCustomeCode(RuleBO ruleBO, int applyCount, long beginSequeue, List<AllocRuleParamBO> params);

    List<String> generateNumberCode(CodeParameterBO codeParameterBO, int applyCount, Long beginSequence, Long ruleId);
}
