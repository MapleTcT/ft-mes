package com.supcon.supfusion.counter.api;

import com.supcon.supfusion.counter.api.dto.AllocCodeDTO;
import com.supcon.supfusion.counter.api.dto.AllocRuleDTO;
import com.supcon.supfusion.counter.api.dto.CreateRuleDTO;
import com.supcon.supfusion.counter.api.dto.RuleDTO;
import com.supcon.supfusion.counter.common.config.CommonConfig;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@ResponseBody
@FeignClient(name = "counter", contextId = "CounterService")
@Api(tags = {"内部接口", "service-api"})
public interface CounterService {

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = CommonConfig.PATH + "/rule")
    @ApiOperation("按ID查询编码规则")
    Result<RuleDTO> findById(@ApiParam(value = "编码规则ID") @RequestParam(value = "ruleId") Long ruleId);

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(CommonConfig.PATH + "/rule")
    @ApiOperation("新增编码规则")
    Result<Long> doCreate(@ApiParam(name = "编码规则DTO类", value = "传入json格式", required = true) @RequestBody CreateRuleDTO createRuleDTO);

    @PostMapping(value = CommonConfig.PATH + "/batch")
    @ApiOperation("申请编码")
    @ResponseStatus(HttpStatus.OK)
    Result<AllocCodeDTO> allocate(@RequestBody @Valid @ApiParam(name = "申请编码入参", value = "传入json格式", required = true) AllocRuleDTO allocRuleDTO);

    @ResponseStatus(HttpStatus.CREATED)
    @DeleteMapping(value = CommonConfig.PATH + "/batch/{batchId}")
    @ApiOperation("取消申请")
    void cancel(@ApiParam(value = "申请批次ID") @PathVariable(value = "batchId") Long batchId);
}
