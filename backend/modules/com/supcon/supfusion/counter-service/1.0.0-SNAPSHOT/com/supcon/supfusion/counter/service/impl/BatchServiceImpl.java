package com.supcon.supfusion.counter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.counter.dao.entities.CounterBatchPO;
import com.supcon.supfusion.counter.dao.entities.CounterBatchParamPO;
import com.supcon.supfusion.counter.dao.mappers.CounterBatchMapper;
import com.supcon.supfusion.counter.dao.mappers.CounterBatchParamMapper;
import com.supcon.supfusion.counter.service.BatchService;
import com.supcon.supfusion.counter.service.bo.AllocRuleParamBO;
import com.supcon.supfusion.counter.service.bo.BatchBO;
import com.supcon.supfusion.counter.service.bo.RuleBO;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class BatchServiceImpl implements BatchService {
    @Autowired
    private CounterBatchMapper counterBatchMapper;

    @Autowired
    private CounterBatchParamMapper counterBatchParamMapper;

    @Override
    public BatchBO find(Long batchId) {
        CounterBatchPO counterBatchPO = counterBatchMapper.selectById(batchId);
        if (counterBatchPO != null) {
            BatchBO batchBO = new BatchBO();
            batchBO.setId(counterBatchPO.getId());
            batchBO.setRuleId(counterBatchPO.getRuleId());
            batchBO.setApplyCount(counterBatchPO.getApplyCount());
            return batchBO;
        }
        return null;
    }

    @Override
    @Transactional
    public long save(long ruleId, int applyCount, List<AllocRuleParamBO> params) {
        CounterBatchPO counterBatchPO = new CounterBatchPO();
        long id = IDGenerator.newInstance().generate().longValue();
        counterBatchPO.setId(id);
        counterBatchPO.setRuleId(ruleId);
        counterBatchPO.setApplyCount(applyCount);
        counterBatchMapper.insert(counterBatchPO);
        if (params != null && params.size() > 0) {
            params.forEach(v->{
                CounterBatchParamPO counterBatchParamPO = new CounterBatchParamPO();
                long paramId = IDGenerator.newInstance().generate().longValue();
                counterBatchParamPO.setId(paramId);
                counterBatchParamPO.setBatchId(id);
                counterBatchParamPO.setRuleFieldId(v.getRuleFieldId());
                counterBatchParamPO.setRuleFieldValue(v.getRuleFieldValue());
                counterBatchParamMapper.insert(counterBatchParamPO);
            });
        }
        return id;
    }
}
