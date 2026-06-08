package com.supcon.supfusion.counter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.counter.common.exception.CounterErrorEnum;
import com.supcon.supfusion.counter.common.exception.CounterException;
import com.supcon.supfusion.counter.dao.entities.CounterRuleFieldPO;
import com.supcon.supfusion.counter.dao.entities.CounterRulePO;
import com.supcon.supfusion.counter.dao.mappers.CounterRuleFieldMapper;
import com.supcon.supfusion.counter.dao.mappers.CounterRuleMapper;
import com.supcon.supfusion.counter.service.RuleService;
import com.supcon.supfusion.counter.service.bo.RuleBO;
import com.supcon.supfusion.counter.service.bo.RuleFieldBO;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RuleServiceImpl implements RuleService {

    @Autowired
    private CounterRuleMapper counterRuleMapper;

    @Autowired
    private CounterRuleFieldMapper counterRuleFieldMapper;

    @Override
    @Transactional
    public Long add(RuleBO rule) {
        CounterRulePO counterRulePO = new CounterRulePO();
        long id = IDGenerator.newInstance().generate().longValue();
        counterRulePO.setId(id);
        counterRulePO.setRuleName(rule.getRuleName());
        List<RuleFieldBO> ruleFieldBOS = rule.getRuleFields();
        List<CounterRuleFieldPO> counterRuleFieldPOS = rule.transferToBatchPO(ruleFieldBOS);
        counterRuleMapper.insert(counterRulePO);
        if (counterRulePO.getId() != null) {
            counterRuleFieldPOS.forEach(v -> {
                v.setRuleId(counterRulePO.getId());
                long ruleFieldId = IDGenerator.newInstance().generate().longValue();
                v.setId(ruleFieldId);
                counterRuleFieldMapper.insert(v);
            });
        }
        return counterRulePO.getId();
    }

    @Override
    @Transactional
    public void modify(RuleBO rule) {
        CounterRulePO counterRule = counterRuleMapper.selectById(rule.getRuleId());
        if (counterRule != null) {
            counterRule.setRuleName(rule.getRuleName());
            counterRuleMapper.updateById(counterRule);
            Map<String, Object> condition = new HashMap<>();
            condition.put("rule_id", counterRule.getId());
            counterRuleFieldMapper.deleteByMap(condition);
            List<RuleFieldBO> ruleFieldBOS = rule.getRuleFields();
            List<CounterRuleFieldPO> counterRuleFieldPOS = rule.transferToBatchPO(ruleFieldBOS);
            counterRuleFieldPOS.forEach(v -> {
                v.setRuleId(rule.getRuleId());
                long ruleFieldId = IDGenerator.newInstance().generate().longValue();
                v.setId(ruleFieldId);
                counterRuleFieldMapper.insert(v);
            });
        }
    }

    @Override
    @Transactional
    public void delete(Long ruleId) {
        CounterRulePO counterRule = counterRuleMapper.selectById(ruleId);
        if (counterRule != null) {
            counterRule.setValid(true);
            counterRuleMapper.updateById(counterRule);
        }
        QueryWrapper<CounterRuleFieldPO> queryWrapper = new QueryWrapper();
        queryWrapper.eq("rule_id", ruleId);
        CounterRuleFieldPO counterRuleFieldPO = new CounterRuleFieldPO();
        counterRuleFieldPO.setValid(true);
        counterRuleFieldMapper.update(counterRuleFieldPO, queryWrapper);
       /* List<CounterRuleFieldPO> counterRuleFieldPOS = counterRuleFieldMapper.selectList(queryWrapper);
        if (counterRuleFieldPOS != null && counterRuleFieldPOS.size() >0) {
            counterRuleFieldPOS.forEach(v ->{
                QueryWrapper<CounterRuleFieldPO> vWapper = new QueryWrapper();
                v.setValid(false);
                vWapper.eq("id",v.getId());
                counterRuleFieldMapper.update(v,vWapper);
            });
        }*/
    }

    @Override
    public RuleBO find(Long ruleId) {
        QueryWrapper<CounterRulePO> queryWrapper = new QueryWrapper();
        if (ruleId != null) {
            queryWrapper.eq("id", ruleId);
        }
        queryWrapper.eq("valid", 0);
        CounterRulePO counterRulePO = counterRuleMapper.selectOne(queryWrapper);
        if (!Optional.ofNullable(counterRulePO).isPresent()) {
            throw new CounterException(CounterErrorEnum.RULE_NOT_FOUND);
        }
        RuleBO ruleBO = new RuleBO();
        ruleBO.setRuleId(ruleId);
        ruleBO.setRuleName(counterRulePO.getRuleName());
        QueryWrapper<CounterRuleFieldPO> counterRuleFieldPOQueryWrapper = new QueryWrapper();
        counterRuleFieldPOQueryWrapper.eq("rule_id", ruleId);
        counterRuleFieldPOQueryWrapper.eq("valid", 0);
        counterRuleFieldPOQueryWrapper.orderByAsc("field_order");
        List<CounterRuleFieldPO> counterRuleFieldPOS = counterRuleFieldMapper.selectList(counterRuleFieldPOQueryWrapper);
        if (counterRuleFieldPOS != null && counterRuleFieldPOS.size() > 0) {
            ruleBO.setRuleFields(ruleBO.transferToBatchBO(counterRuleFieldPOS));
        }
        return ruleBO;
    }

}
