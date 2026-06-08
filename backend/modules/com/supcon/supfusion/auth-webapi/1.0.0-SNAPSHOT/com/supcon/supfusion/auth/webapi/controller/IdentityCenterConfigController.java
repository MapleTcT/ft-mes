package com.supcon.supfusion.auth.webapi.controller;

import com.supcon.supfusion.auth.common.utils.BijectionUtils;
import com.supcon.supfusion.auth.service.IdentityCenterConfigService;
import com.supcon.supfusion.auth.service.bo.IdentityCenterConfigBO;
import com.supcon.supfusion.auth.webapi.vo.IdentityCenterConfigRespVo;
import com.supcon.supfusion.auth.webapi.vo.SaveIdentityCenterConfigVo;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * auth 配置中心
 * </p>
 *
 * @author 王海峰
 * @date 2021-05-08
 */
@RestController
@Api(value = "第三方auth配置中心", tags = {"第三方auth配置中心"})
@InternalApi(path = "/inter-api/auth/v1/identityProviders")
public class IdentityCenterConfigController {

    @Autowired
    private IdentityCenterConfigService identityCenterConfigService;

    @ApiOperation(value = "获取auth配置列表", notes = "")
    @GetMapping("")
    public ListResult<IdentityCenterConfigRespVo> identityCenterConfig() throws Exception {

        List<IdentityCenterConfigBO> identityCenterConfig =
                identityCenterConfigService.
                        listIdentityCenterConfig(null);

        return new ListResult(BijectionUtils.applys(identityCenterConfig,
                IdentityCenterConfigRespVo::new));
    }


    @ApiOperation(value = "keyword 获取auth配置列表", notes = "")
    @GetMapping("keyword")
    public ListResult<IdentityCenterConfigRespVo> identityCenterConfig(
            @RequestParam String keyword)
            throws Exception {

        List<IdentityCenterConfigBO> identityCenterConfig =
                identityCenterConfigService.listIdentityCenterConfig(keyword);

        return new ListResult(BijectionUtils.applys(identityCenterConfig,
                IdentityCenterConfigRespVo::new));
    }

    @ApiOperation(value = "按主鍵查寻", notes = "根据id查询identityCenterConfig")
    @GetMapping("/{id}")
    public Result<IdentityCenterConfigRespVo> identityCenterConfigGet(@PathVariable(name = "id") Long id) throws Exception {

        IdentityCenterConfigRespVo config = BijectionUtils.apply(identityCenterConfigService.getIdentityCenterConfigById(id), IdentityCenterConfigRespVo::new);

        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(config)
                .build();

    }

    @ApiOperation(value = "添加identityCenterConfig", notes = "新增一条identityCenterConfig")
    @PostMapping("add")
    public Result identityCenterConfigAdd(@RequestBody
                                          @Valid SaveIdentityCenterConfigVo identityCenterConfig
    ) throws Exception {

        Integer flag = identityCenterConfigService.addIdentityCenterConfig(BijectionUtils.apply(identityCenterConfig, IdentityCenterConfigBO::new));
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(flag == 1).build();
    }

    @ApiOperation(value = "修改identityCenterConfig", notes = "根据id修改identity_center_config")
    @PostMapping("{id}/update")
    public Result identityCenterConfigUpdate(@PathVariable long id,
                                             @Valid @RequestBody SaveIdentityCenterConfigVo identityCenterConfig) throws Exception {

        IdentityCenterConfigBO bo = BijectionUtils.apply(identityCenterConfig, IdentityCenterConfigBO::new);
        bo.setId(id);
        Integer flag = identityCenterConfigService.updateIdentityCenterConfig(bo);
        Result Result = new Result();
        Result.data(flag);
        return Result;
    }


    @ApiOperation(value = "更新auth启用状态", notes = "根据id修改identity_center_config")
    @PostMapping("{id}/{status}")
    public Result identityCenterConfigUpdate(@PathVariable long id, @PathVariable boolean status) throws Exception {

        IdentityCenterConfigBO target = new IdentityCenterConfigBO();

        target.setId(id);
        target.setEnable(status);
        Integer flag = identityCenterConfigService.updateIdentityCenterConfigStatus(target);


        return Result.success(BizErrorEnum.SYSTEM_OK.getInfo());
    }

    @ApiOperation(value = "删除identityCenterConfig", notes = "根据id物理删除identityCenterConfig")
    @DeleteMapping("/{ids}")
    public Result identityCenterConfigDelete(@Valid
                                             @Size(min = 1)
                                             @NotNull
                                             @PathVariable(name = "ids") Long[] id) throws Exception {

        Integer flag = identityCenterConfigService.deleteIdentityCenterConfigByIds(id);
        return Result.custom().code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(true).build();
    }

    @ApiOperation("获取当前认证配置")
    @GetMapping("/current")
    public Result current(@RequestHeader(name = "Referer", required = false) String host) {

        Map<String, List<IdentityCenterConfigBO>> maps
                = identityCenterConfigService.getCurrentAuthConfig();
        return Result.custom().code(0).data(maps).build();

    }


}
