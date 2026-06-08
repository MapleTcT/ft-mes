package com.supcon.supfusion.counter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.counter.common.constants.AutoDateRuleType;
import com.supcon.supfusion.counter.common.constants.AutoType;
import com.supcon.supfusion.counter.common.constants.FieldType;
import com.supcon.supfusion.counter.common.constants.TheCase;
import com.supcon.supfusion.counter.common.service.RedissonService;
import com.supcon.supfusion.counter.dao.entities.CounterRuleSequencePO;
import com.supcon.supfusion.counter.dao.mappers.CounterRuleSequenceMapper;
import com.supcon.supfusion.counter.service.CodeService;
import com.supcon.supfusion.counter.service.bo.AllocRuleParamBO;
import com.supcon.supfusion.counter.service.bo.CodeParameterBO;
import com.supcon.supfusion.counter.service.bo.RuleBO;
import com.supcon.supfusion.counter.service.bo.RuleFieldBO;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class CodeServiceImpl implements CodeService {
    @Autowired
    private CounterRuleSequenceMapper counterRuleSequenceMapper;

    @Autowired
    private RedissonService redissonService;

    @Override
    public long getAndIncMaxSequence(long ruleId, long seqFieldId, String seqRef, int applyCount) {
        return 0;
    }

    @Override
    public List<String> generate(RuleBO ruleBO, int applyCount, long beginSequeue, List<AllocRuleParamBO> params) {
        RLock lock = redissonService.getRLock(String.valueOf(ruleBO.getRuleId()));
        lock.lock();
        List<String> codes = getCustomeCode(ruleBO, applyCount, beginSequeue, params);
        lock.unlock();
        return codes;
    }

    @Override
    public void cancel(long ruleId, long lastBatchId, int applyCount) {
        QueryWrapper<CounterRuleSequencePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("rule_id", ruleId);
        queryWrapper.eq("last_batch_id", lastBatchId);
        CounterRuleSequencePO counterRuleSequencePO = counterRuleSequenceMapper.selectOne(queryWrapper);
        if (counterRuleSequencePO != null) {
            long backTo = counterRuleSequencePO.getSeqNo() - applyCount;
            CounterRuleSequencePO counterRuleSequence = new CounterRuleSequencePO();
            counterRuleSequence.setId(counterRuleSequencePO.getId());
            counterRuleSequence.setSeqNo(backTo);
            updateSequence(counterRuleSequence);
        }
    }

    @Override
    public String getReference(RuleBO ruleBO, List<AllocRuleParamBO> params) {
        int lastIndex = ruleBO.getRuleFields().size() - 1;
        RuleFieldBO ruleFieldBO = ruleBO.getRuleFields().get(lastIndex);
        AutoType autoType = ruleFieldBO.getAutoType();
        AutoDateRuleType autoDateRuleType = ruleFieldBO.getAutoDateRuleType();
        StringBuilder fieldValue = new StringBuilder();
        switch (autoType) {
            case DATE:
                String dateFormatter = "yyyyMMdd";
                switch (autoDateRuleType) {
                   /* case DAILY:
                        dateFormatter = "yyyyMMdd";
                        break;*/
                    case MONTHLY:
                        dateFormatter = "yyyyMM";
                        break;
                    case YEARLY:
                        dateFormatter = "yyyy";
                        break;
                }
                Date date = new Date();
                SimpleDateFormat format = new SimpleDateFormat(dateFormatter);
                fieldValue.append(format.format(date));
                break;
            case CODE:
                Map<Long, String> map = new HashMap<>();
                params.forEach(v -> {
                    map.put(v.getRuleFieldId(), v.getRuleFieldValue());
                });
                List<RuleFieldBO> ruleFieldBOS = ruleBO.getRuleFields();

                if (params != null) {
                    for (int i = 0; i < ruleFieldBOS.size() - 1; i++) {
                        RuleFieldBO ruleFieldBO1 = ruleFieldBOS.get(i);
                        if (map.get(ruleFieldBO1.getId()) != null) {
                            ruleFieldBO1.setFieldValue(map.get(ruleFieldBO1.getId()));
                        }
                        fieldValue = getFieldValue(ruleFieldBO1);
                        fieldValue.append(fieldValue);
                    }
                }

        }
        return fieldValue.toString();
    }

    @Override
    public StringBuilder getFieldValue(RuleFieldBO ruleFieldBO) {
        FieldType fieldType = ruleFieldBO.getFieldType();
        TheCase theCase = ruleFieldBO.getTheCase();
        String dateFormatter = ruleFieldBO.getDateFormatter();
        String result = "";
        switch (fieldType) {
            case DATE:
                Date date = new Date();
                SimpleDateFormat format = new SimpleDateFormat(dateFormatter);
                result = format.format(date);
                break;
            case AUTO:
                StringBuilder buffer = new StringBuilder("0");
                int length = ruleFieldBO.getAutoLength();
                for (int i = 1; i < length; i++) {
                    buffer.append("0");
                }
                result = buffer.toString();
                break;
            case CUSTOM:
            case PROPERTY:
            case INHERENT:
            case SEPARATOR:
                result = ruleFieldBO.getFieldValue();
                break;
        }
        switch (theCase) {
            case LOWER:
                result = result.toLowerCase();
                break;
            case UPPER:
                result = result.toUpperCase();
                break;
        }
        return new StringBuilder(result);
    }

    @Override
    public CounterRuleSequencePO findRuleSequence(Long ruleId, String reference) {
        QueryWrapper<CounterRuleSequencePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("rule_id", ruleId);
        queryWrapper.eq("seq_reference", reference);
        CounterRuleSequencePO counterRuleSequencePO = counterRuleSequenceMapper.selectOne(queryWrapper);
        return counterRuleSequencePO;
    }

    @Override
    public int insertSequence(CounterRuleSequencePO counterRuleSequencePO) {
        return counterRuleSequenceMapper.insert(counterRuleSequencePO);
    }

    @Override
    public int updateSequence(CounterRuleSequencePO counterRuleSequencePO) {
        return counterRuleSequenceMapper.updateById(counterRuleSequencePO);
    }

    /**
     * @param ruleBO
     * @param params
     * @param applyCount
     * @param beginSequeue
     * @return 生成codes
     */
    @Override
    public List<String> getCustomeCode(RuleBO ruleBO, int applyCount, long beginSequeue, List<AllocRuleParamBO> params) {
        StringBuilder codeHead = new StringBuilder();
        Map<Long, String> map = new HashMap<>();
        params.forEach(v -> {
            map.put(v.getRuleFieldId(), v.getRuleFieldValue());
        });
        List<RuleFieldBO> ruleFieldBOS = ruleBO.getRuleFields();
        int lastRuleIndex = ruleFieldBOS.size() - 1;
        RuleFieldBO ruleFieldBO = ruleFieldBOS.get(lastRuleIndex);
        List<String> codes = new ArrayList<>();
        if (params != null) {
            for (int i = 0; i < ruleFieldBOS.size() - 1; i++) {
                RuleFieldBO ruleFieldBO1 = ruleFieldBOS.get(i);
                if (map.get(ruleFieldBO1.getId()) != null) {
                    ruleFieldBO1.setFieldValue(map.get(ruleFieldBO1.getId()));
                }
                codeHead = getFieldValue(ruleFieldBO1);
                codeHead.append(codeHead);
            }
        }

        String codeH = codeHead.toString();
        //序号位数
        int length = ruleFieldBO.getAutoLength();
        //计算数值
        for (int i = 0; i < applyCount; i++) {
            String code = complementStr(length, beginSequeue + i);
            codes.add(code);
        }
        return codes;
    }

    /**
     * @return java.util.List<java.lang.String>
     * @Author kk.C
     * @Description 生成编码数组
     * @Date 2020/10/16 15:29
     * @Param [codeParameterBO, applyCount, beginSequence, ruleId]
     **/
    @Override
    public List<String> generateNumberCode(CodeParameterBO codeParameterBO, int applyCount, Long beginSequence, Long ruleId) {
        RLock lock = redissonService.getRLock(String.valueOf(ruleId));
        lock.lock();
        List<String> codes = new ArrayList<>();
        String number = codeParameterBO.getNumberCode();
        List<String> numbers = getNumbers(codeParameterBO.getNumberCode(), applyCount, beginSequence);
        for (int i = 0; i < applyCount; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String code : codeParameterBO.getCodeList()) {
                if (number != null && number.equals(code)) {
                    stringBuilder.append(numbers.get(i));
                } else {
                    stringBuilder.append(code);
                }
            }
            codes.add(stringBuilder.toString());
        }
        lock.unlock();
        return codes;
    }

    /**
     * @Author kk.C
     * @Description 生成序号数组
     * @Date 2020/10/16 15:54
     * @Param [numberCode, applyCount, beginSequence]
     * @return java.util.List<java.lang.String>
     **/
    private List<String> getNumbers(String numberCode, int applyCount, Long beginSequence) {
        List<String> numbers = new ArrayList<>();
        int numberCodeSize = numberCode.length();
        for (int i = 0; i < applyCount; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            int seqSize = numberCodeSize - (beginSequence.toString().length());
            for (int n = 0; n <= seqSize; n++) {
                if (n == seqSize) {
                    stringBuilder.append(beginSequence.toString());
                    beginSequence++;
                } else {
                    stringBuilder.append("0");
                }
            }
            numbers.add(stringBuilder.toString());
        }
        return numbers;
    }

    public String complementStr(int length, long number) {
        String str = String.valueOf(number);
        if (str.length() > length) {
            number = 0L;
        }
        StringBuilder builder = new StringBuilder();
        int len = length - str.length();
        for (int i = 0; i < len; i++) {
            builder.append("0");
        }
        builder.append(str);
        return builder.toString();
    }
}
