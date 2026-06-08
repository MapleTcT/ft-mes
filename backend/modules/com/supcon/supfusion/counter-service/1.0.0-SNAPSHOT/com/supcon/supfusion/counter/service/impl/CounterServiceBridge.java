package com.supcon.supfusion.counter.service.impl;

import com.supcon.supfusion.counter.common.constants.AutoDateRuleType;
import com.supcon.supfusion.counter.common.constants.AutoType;
import com.supcon.supfusion.counter.common.constants.FieldType;
import com.supcon.supfusion.counter.dao.entities.CounterRuleSequencePO;
import com.supcon.supfusion.counter.dao.mappers.CounterRuleSequenceMapper;
import com.supcon.supfusion.counter.service.BatchService;
import com.supcon.supfusion.counter.service.CodeService;
import com.supcon.supfusion.counter.service.RuleService;
import com.supcon.supfusion.counter.service.bo.*;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.StyledEditorKit;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.SimpleFormatter;

@Service
public class CounterServiceBridge {
    @Autowired
    private RuleService ruleService;

    @Autowired
    private CodeService codeService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private CounterRuleSequenceMapper counterRuleSequenceMapper;

    public RuleBO findById(long ruleId) {
        return ruleService.find(ruleId);
    }

    public void cancel(long batchId) {
        BatchBO batchBo = batchService.find(batchId);
        if (batchBo != null) {
            long ruleId = batchBo.getRuleId();
            int applyCount = batchBo.getApplyCount();
            codeService.cancel(ruleId, batchId, applyCount);
        }
    }

//    public AllocCodeBO allocate2(long ruleId, int applyCount, List<AllocRuleParamBO> params) {
//        //RuleBO ruleBO = new RuleBO();
//        long batchId = batchService.save(ruleId, applyCount, params);
//        List<RuleBO> ruleBOS = ruleService.find(ruleId);
//        List<String> codes = new ArrayList<>();
//        if (ruleBOS != null && ruleBOS.size() == 1) {
//
//            RuleBO ruleBO = ruleBOS.get(0);
//            String reference = codeService.getReference(ruleBO, params);
//            CounterRuleSequencePO counterRuleSequencePO = codeService.findRuleSequence(ruleId, reference);
//            long beginSequence = 0L;
//            if (counterRuleSequencePO == null && ruleBO.getRuleFields() != null) {
//                int i = ruleBO.getRuleFields().size() - 1;
//                RuleFieldBO lastRule = ruleBO.getRuleFields().get(i);
//                String fieldValue = codeService.getFieldValue(lastRule).toString();
//                if (!fieldValue.equals("")) {
//                    beginSequence = Long.valueOf(fieldValue);
//                }
//                //  新增一条sequence数据
//                long seqNo = beginSequence + applyCount;
//                CounterRuleSequencePO newCounterRuleSequencePO = new CounterRuleSequencePO();
//                long id = IDGenerator.newInstance().generate().longValue();
//                newCounterRuleSequencePO.setId(id);
//                newCounterRuleSequencePO.setRuleId(ruleId);
//                newCounterRuleSequencePO.setRuleFieldId(lastRule.getId());
//                newCounterRuleSequencePO.setSeqReference(reference);
//                newCounterRuleSequencePO.setLastBatchId(batchId);
//                newCounterRuleSequencePO.setSeqNo(seqNo);
//                codeService.insertSequence(newCounterRuleSequencePO);
//            } else {
//                beginSequence = counterRuleSequencePO.getSeqNo();
//                long seqNo = beginSequence + applyCount;
//                counterRuleSequencePO.setSeqNo(seqNo);
//                counterRuleSequencePO.setLastBatchId(batchId);
//                codeService.updateSequence(counterRuleSequencePO);
//            }
//            codes = codeService.generate(ruleBO, applyCount, beginSequence, params);
//        }
//        AllocCodeBO allocCodeBO = new AllocCodeBO();
//        allocCodeBO.setBatchId(batchId);
//        allocCodeBO.setCodes(codes);
//        return allocCodeBO;
//    }

    public AllocCodeBO allocate(long ruleId, int applyCount, List<AllocRuleParamBO> params) {
        Map<Long, String> paramsMap = new HashMap<>();
        List<String> codes = new ArrayList<>();
        for (AllocRuleParamBO param : params) {
            paramsMap.put(param.getRuleFieldId(), param.getRuleFieldValue());
        }
        long batchId = batchService.save(ruleId, applyCount, params);
        RuleBO ruleBO = ruleService.find(ruleId);
        List<RuleFieldBO> ruleFields = ruleBO.getRuleFields();
        CodeParameterBO codeParameterBO = new CodeParameterBO();
        AutoType autoType = null;
        if (ruleFields != null) {
            for (RuleFieldBO ruleField : ruleFields) {
                if (FieldType.AUTO == ruleField.getFieldType()) {
                    autoType = ruleField.getAutoType();
                    if (AutoType.DATE == autoType) {
                        //将系统时间装配到参照值
                        setReferenceForDate(codeParameterBO, ruleField);
                    }
                }
            }
            for (RuleFieldBO ruleField : ruleFields) {
                //按照规则配置的插入顺序，根据每个规则生成对应的编码
                checkFieldType(ruleField, codeParameterBO, autoType, paramsMap);
            }
            //获得当前参照值
            String reference = codeParameterBO.getReference().toString();
            CounterRuleSequencePO counterRuleSequencePO = codeService.findRuleSequence(ruleId, codeParameterBO.getReference().toString());
            Long beginSequence = 0L;
            if (counterRuleSequencePO == null && ruleBO.getRuleFields() != null) {
                //  新增一条sequence数据
                int i = ruleBO.getRuleFields().size() - 1;
                RuleFieldBO lastRule = ruleBO.getRuleFields().get(i);
                CounterRuleSequencePO newCounterRuleSequencePO = new CounterRuleSequencePO();
                long id = IDGenerator.newInstance().generate().longValue();
                newCounterRuleSequencePO.setId(id);
                newCounterRuleSequencePO.setRuleId(ruleId);
                newCounterRuleSequencePO.setRuleFieldId(lastRule.getId());
                newCounterRuleSequencePO.setSeqReference(reference);
                newCounterRuleSequencePO.setLastBatchId(batchId);
                newCounterRuleSequencePO.setSeqNo((long)applyCount);
                counterRuleSequenceMapper.insert(newCounterRuleSequencePO);
            } else {
                beginSequence = counterRuleSequencePO.getSeqNo();
                counterRuleSequencePO.setSeqNo(counterRuleSequencePO.getSeqNo() + applyCount);
                counterRuleSequencePO.setLastBatchId(batchId);
                counterRuleSequenceMapper.updateById(counterRuleSequencePO);
            }
            codes = codeService.generateNumberCode(codeParameterBO, applyCount, beginSequence, ruleBO.getRuleId());
        }
        AllocCodeBO allocCodeBO = new AllocCodeBO();
        allocCodeBO.setBatchId(batchId);
        allocCodeBO.setCodes(codes);
        return allocCodeBO;
    }

    private void setReferenceForDate(CodeParameterBO codeParameterBO, RuleFieldBO ruleField) {
        AutoDateRuleType autoDateRuleType = ruleField.getAutoDateRuleType();
        Calendar cal = Calendar.getInstance();
        String reference = null;
        switch (autoDateRuleType) {
            case YEARLY:
                reference = cal.get(Calendar.YEAR) + "";
                break;
            case MONTHLY:
                reference = cal.get(Calendar.MONTH) + 1 + "";
                break;
            case DAILY:
                reference = cal.get(Calendar.DATE) + "";
        }
        if (codeParameterBO.getReference() == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(reference);
            codeParameterBO.setReference(stringBuilder);
        } else {
            codeParameterBO.getReference().append(reference);
        }
    }

    /**
     * @Author kk.C
     * @Description 根据规则的不同fieldType，生成不同编码
     * @Date 2020/10/16 15:56
     * @Param [ruleField, codeParameterBO, autoType, paramsMap]
     * @return com.supcon.supfusion.counter.service.bo.CodeParameterBO
     **/
    private CodeParameterBO checkFieldType(RuleFieldBO ruleField, CodeParameterBO codeParameterBO, AutoType autoType, Map<Long, String> paramsMap) {
        FieldType fieldType = ruleField.getFieldType();
        switch (fieldType) {
            case DATE:
                generateDate(ruleField, codeParameterBO, autoType);
                break;
            case AUTO:
                generateAuto(ruleField, codeParameterBO);
                break;
            case CUSTOM:
            case PROPERTY:
            case INHERENT:
                generateCustom(ruleField, codeParameterBO, autoType, paramsMap);
                break;
            case SEPARATOR:
                generateSeparator(ruleField, codeParameterBO, autoType);

        }
        return codeParameterBO;
    }

    private void generateSeparator(RuleFieldBO ruleField, CodeParameterBO codeParameterBO, AutoType autoType) {
        String code = ruleField.getFieldValue();
        setParamInCodeParameterBO(code, codeParameterBO, autoType);
    }

    /**
     * @Author kk.C
     * @Description 生成初始序号值，方便之后的自增和锁定编码组中的序号部分
     * @Date 2020/10/16 16:02
     * @Param [ruleField, codeParameterBO]
     * @return void
     **/
    private void generateAuto(RuleFieldBO ruleField, CodeParameterBO codeParameterBO) {
        int autoLength = ruleField.getAutoLength();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= autoLength; i++) {
            if (i == autoLength) {
                stringBuilder.append(1);
            } else {
                stringBuilder.append(0);
            }
        }
        codeParameterBO.setNumberCode(stringBuilder.toString());
        if (codeParameterBO.getCodeList() == null) {
            List<String> codeList = new ArrayList<>();
            codeList.add(stringBuilder.toString());
            codeParameterBO.setCodeList(codeList);
        } else {
            codeParameterBO.getCodeList().add(stringBuilder.toString());
        }
    }

    /**
     * @Author kk.C
     * @Description 将自定义的内容作为编码组的一员
     * @Date 2020/10/16 16:00
     * @Param [ruleField, codeParameterBO, autoType, paramsMap]
     * @return void
     **/
    private void generateCustom(RuleFieldBO ruleField, CodeParameterBO codeParameterBO, AutoType autoType, Map<Long, String> paramsMap) {
        String code = paramsMap.get(ruleField.getId());
        switch (ruleField.getTheCase()) {
            case LOWER:
                code = code.toLowerCase();
                break;
            case UPPER:
                code = code.toUpperCase();
        }
        setParamInCodeParameterBO(code, codeParameterBO, autoType);
    }

    /**
     * @Author kk.C
     * @Description 将系统时间作为编码一员
     * @Date 2020/10/16 15:57
     * @Param [ruleField, codeParameterBO, autoType]
     * @return void
     **/
    private void generateDate(RuleFieldBO ruleField, CodeParameterBO codeParameterBO, AutoType autoType) {
        SimpleDateFormat sdf = new SimpleDateFormat(ruleField.getDateFormatter());
        String code = sdf.format(new Date());
        setParamInCodeParameterBO(code, codeParameterBO, autoType);
    }

    /**
     * @Author kk.C
     * @Description 将各个编码成员插入BO类
     * @Date 2020/10/16 15:57
     * @Param [code, codeParameterBO, autoType]
     * @return void
     **/
    private void setParamInCodeParameterBO(String code, CodeParameterBO codeParameterBO, AutoType autoType) {
        if (codeParameterBO.getCodeList() == null) {
            List<String> codeList = new ArrayList<>();
            codeList.add(code);
            codeParameterBO.setCodeList(codeList);
        } else {
            codeParameterBO.getCodeList().add(code);
        }
        //当autotype为code时，将各个code成员按照order字段组合成当前参照值放入BO类
        if (AutoType.CODE == autoType) {
            if (codeParameterBO.getReference() == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(code);
                codeParameterBO.setReference(stringBuilder);
            } else {
                codeParameterBO.getReference().append(code);
            }

        }
    }
}
