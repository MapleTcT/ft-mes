package com.supcon.supfusion.counter.webapi.controller;

import com.supcon.supfusion.counter.common.exception.CounterErrorEnum;
import com.supcon.supfusion.counter.service.RuleService;
import com.supcon.supfusion.counter.service.bo.RuleBO;
import com.supcon.supfusion.counter.webapi.vo.CreateRuleVO;
import com.supcon.supfusion.counter.webapi.vo.ModifyRuleVO;
import com.supcon.supfusion.counter.webapi.vo.RuleVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "编码生成器API")
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "counter" + HttpConstants.URL_SPLITER + "v1")
@Slf4j
public class RuleController {

    @Autowired
    private RuleService ruleService;

    @ApiOperation(value = "新增编码规则")
    @PostMapping("/rule")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Long> doCreate(@RequestBody CreateRuleVO createRuleVO) {
        RuleBO bo = new RuleBO();
        bo.setRuleName(createRuleVO.getRuleName());
        bo.setRuleFields(createRuleVO.transferToBatchBO(createRuleVO));
        Long ruleId = ruleService.add(bo);
        return new Result<>(ruleId);
    }

    @ApiOperation(value = "修改编码规则")
    @PutMapping("/rule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void doModify(@RequestBody ModifyRuleVO rule) {
        RuleBO bo = new RuleBO();
        bo.setRuleId(rule.getRuleId());
        bo.setRuleName(rule.getRuleName());
        bo.setRuleFields(rule.transferToBatchBO(rule));
        ruleService.modify(bo);
    }

    @ApiOperation(value = "删除编码规则")
    @DeleteMapping("/rule/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void doDelete(@PathVariable Long ruleId) {
        ruleService.delete(ruleId);
    }

    @ApiOperation(value = "查询编码规则")
    @GetMapping("/rules")
    @ResponseStatus(HttpStatus.OK)
    public Result<RuleVO> getRules(@ApiParam(value = "规则id") @RequestParam(value = "ruleId", required = false) Long ruleId) {
        RuleBO ruleBOS = ruleService.find(ruleId);
        RuleVO ruleVO = new RuleVO();
        ruleVO.setRuleId(ruleBOS.getRuleId());
        ruleVO.setRuleName(ruleBOS.getRuleName());
        ruleVO.setRuleFields(ruleVO.transferToBatchVO(ruleBOS.getRuleFields()));
        return new Result<>(ruleVO);
    }

}
