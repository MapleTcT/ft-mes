package com.supcon.supfusion.counter.service.impl;

import com.supcon.supfusion.counter.api.CounterService;
import com.supcon.supfusion.counter.api.dto.*;
import com.supcon.supfusion.counter.service.CodeService;
import com.supcon.supfusion.counter.service.RuleService;
import com.supcon.supfusion.counter.service.bo.AllocCodeBO;
import com.supcon.supfusion.counter.service.bo.AllocRuleParamBO;
import com.supcon.supfusion.counter.service.bo.RuleBO;
import com.supcon.supfusion.counter.service.bo.RuleFieldBO;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@ServiceApiService
public class CounterServiceImpl implements CounterService {

    @Autowired
    private CounterServiceBridge counterServiceBridge;

    @Autowired
    private RuleService ruleService;

    @Override
    public Result<RuleDTO> findById(Long ruleId) {
        RuleBO ruleBO = counterServiceBridge.findById(ruleId);
        return new Result<>(ruleBO.transferToDTO(ruleBO));
    }

    @Override
    public Result<Long> doCreate(CreateRuleDTO createRuleDTO) {
        RuleBO bo = new RuleBO();
        bo.setRuleName(createRuleDTO.getRuleName());
        bo.setRuleFields(transferToBatchBO(createRuleDTO));
        Long ruleId = ruleService.add(bo);
        return new Result<>(ruleId);
    }

    @Override
    public Result<AllocCodeDTO> allocate(AllocRuleDTO allocRule) {
        List<AllocRuleParamDTO> params = allocRule.getParams();
        List<AllocRuleParamBO> paramBOS = transferToBatchBO(params);
        AllocCodeBO allocCodeBO = counterServiceBridge.allocate(allocRule.getRuleId(), allocRule.getApplyCount(), paramBOS);
        AllocCodeDTO allocCodeDTO = new AllocCodeDTO();
        allocCodeDTO.setBatchId(allocCodeBO.getBatchId());
        allocCodeDTO.setCodes(allocCodeBO.getCodes());
        return new Result<>(allocCodeDTO);
    }

    public List<AllocRuleParamBO> transferToBatchBO(List<AllocRuleParamDTO> params) {
        List<AllocRuleParamBO> paramBOS = new ArrayList<>();
        params.forEach(v -> {
            AllocRuleParamBO allocRuleParamBO = new AllocRuleParamBO();
            allocRuleParamBO.setRuleFieldId(v.getRuleFieldId());
            allocRuleParamBO.setRuleFieldValue(v.getRuleFieldValue());
            paramBOS.add(allocRuleParamBO);
        });
        return paramBOS;
    }

    public List<RuleFieldBO> transferToBatchBO(CreateRuleDTO createRuleDTO) {
        List<RuleFieldDTO> ruleFieldVOS = createRuleDTO.getRuleFields();
        List<RuleFieldBO> ruleFieldBOS = new ArrayList<>();
        ruleFieldVOS.forEach(v -> {
            ruleFieldBOS.add(transferToBO(v));
        });
        return ruleFieldBOS;
    }

    public RuleFieldBO transferToBO(RuleFieldDTO ruleFieldDTO) {
        RuleFieldBO ruleFieldBO = new RuleFieldBO();
        ruleFieldBO.setId(ruleFieldDTO.getId());
        ruleFieldBO.setAutoDateRuleType(ruleFieldDTO.getAutoDateRuleType());
        ruleFieldBO.setAutoLength(ruleFieldDTO.getAutoLength());
        ruleFieldBO.setAutoType(ruleFieldDTO.getAutoType());
        ruleFieldBO.setDateFormatter(ruleFieldDTO.getDateFormatter());
        ruleFieldBO.setFieldOrder(ruleFieldDTO.getOrder());
        ruleFieldBO.setFieldType(ruleFieldDTO.getFieldType());
        ruleFieldBO.setFieldValue(ruleFieldDTO.getFieldValue());
        ruleFieldBO.setTheCase(ruleFieldDTO.getTheCase());
        return ruleFieldBO;
    }

    @Override
    public void cancel(Long batchId) {
        counterServiceBridge.cancel(batchId);
    }
}
