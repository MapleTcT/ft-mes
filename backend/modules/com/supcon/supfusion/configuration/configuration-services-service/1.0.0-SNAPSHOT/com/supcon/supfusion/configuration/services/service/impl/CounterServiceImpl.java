package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.dao.CounterRuleFieldImpl;
import com.supcon.supfusion.configuration.services.dao.CounterRuleImpl;
import com.supcon.supfusion.configuration.services.entity.CounterRule;
import com.supcon.supfusion.configuration.services.entity.CounterRuleField;
import com.supcon.supfusion.configuration.services.entity.CounterRuleInfo;
import com.supcon.supfusion.configuration.services.service.CounterService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * @Author kk.C
 * @Description: 编码生成器实现类
 * @Date 2020/10/27 10:13
 */
@Slf4j
@ServiceApiService()
@Transactional
public class CounterServiceImpl implements CounterService {

    @Autowired
    private CounterRuleImpl counterRuleDao;
    @Autowired
    private CounterRuleFieldImpl counterRuleFieldDao;

    @Override
    public Long add(CounterRule rule) {
        long id = IDGenerator.newInstance().generate().longValue();
        rule.setId(id);
        counterRuleDao.save(rule);
        rule.getRuleFields().forEach(v -> {
            v.setRuleId(id);
            long ruleFieldId = IDGenerator.newInstance().generate().longValue();
            v.setId(ruleFieldId);
            if (StringUtils.isEmpty(v.getFieldValue()) || "".equals(v.getFieldValue())) {
                v.setFieldValue("unknown");
            }
            counterRuleFieldDao.save(v);
        });
        return id;
    }

    @Override
    public void modify(CounterRule rule) {
        CounterRule findRule = counterRuleDao.load(rule.getId());
        if (Optional.ofNullable(findRule).isPresent()) {
            counterRuleDao.createNativeQuery("update counter_rule set rule_name=? where id=?",rule.getRuleName(),rule.getId()).addSynchronizedEntityClass(CounterRule.class).executeUpdate();
            String sql = "delete from counter_rule_field where rule_id=?";
            counterRuleFieldDao.createNativeQuery(sql, rule.getId()).addSynchronizedEntityClass(CounterRuleField.class).executeUpdate();
            rule.getRuleFields().forEach(v -> {
                v.setRuleId(findRule.getId());
                long ruleFieldId = IDGenerator.newInstance().generate().longValue();
                v.setId(ruleFieldId);
                if (StringUtils.isEmpty(v.getFieldValue()) || "".equals(v.getFieldValue())) {
                    v.setFieldValue("unknown");
                }
                counterRuleFieldDao.save(v);
            });
        }
    }

    @Override
    public void delete(Long ruleId) {
        CounterRule findRule = counterRuleDao.load(ruleId);
        if (Optional.ofNullable(findRule).isPresent()) {
            counterRuleDao.delete(ruleId);
            String sql = "delete from counter_rule_field where rule_id=?";
            counterRuleFieldDao.createNativeQuery(sql, ruleId).addSynchronizedEntityClass(CounterRuleField.class).executeUpdate();
        }
    }

    @Override
    public CounterRule find(Long ruleId) {
        CounterRule findRule = counterRuleDao.load(ruleId);
        if (!Optional.ofNullable(findRule).isPresent()) {
            return null;
        }
//        String hql = "From CounterRuleField where ruleId=?0";
//        List<CounterRuleField> counterRuleFields = counterRuleFieldDao.findByHql(hql, ruleId);
//        ruleInfo.setRuleFields(counterRuleFields);
        return findRule;
    }

    @Override
    public CounterRule findByName(String ruleName) {
        List<CounterRule> findRule = counterRuleDao.findByHql("From CounterRule where ruleName=?0",ruleName);
        if(ObjectUtils.isEmpty(findRule)){
            return null;
        }
//        String hql = "From CounterRuleField where ruleId=?0";
//        List<CounterRuleField> counterRuleFields = counterRuleFieldDao.findByHql(hql, ruleInfo.getId());
//        ruleInfo.setRuleFields(counterRuleFields);
        return findRule.get(0);
    }
}
