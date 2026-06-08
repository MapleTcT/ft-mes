package com.supcon.supfusion.counter.service;

import com.supcon.supfusion.counter.dao.entities.CounterBatchPO;
import com.supcon.supfusion.counter.service.bo.AllocRuleParamBO;
import com.supcon.supfusion.counter.service.bo.BatchBO;
import com.supcon.supfusion.counter.service.bo.RuleBO;

import java.util.List;

public interface BatchService {
    BatchBO find(Long batchId);

    long save(long ruleId, int applyCount,List<AllocRuleParamBO> params);
}
