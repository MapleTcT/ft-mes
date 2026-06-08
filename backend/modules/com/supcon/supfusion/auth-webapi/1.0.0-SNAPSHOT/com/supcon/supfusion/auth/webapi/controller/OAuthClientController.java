package com.supcon.supfusion.auth.webapi.controller;

import com.supcon.supfusion.auth.manager.SystemCodeServiceAdapter;
import com.supcon.supfusion.auth.service.OAuthClientService;
import com.supcon.supfusion.auth.service.bo.OAuthClientBO;
import com.supcon.supfusion.auth.service.bo.OAuthClientQueryBO;
import com.supcon.supfusion.auth.webapi.vo.OAuthClientResponseVO;
import com.supcon.supfusion.auth.webapi.vo.OAuthClientVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author caokele
 */
@Slf4j
@RestController
@InternalApi(path = "/inter-api/auth")
@Api(value = "认证客户端相关文档", tags = "认证客户端")
@Validated
public class OAuthClientController extends BaseController {
    @Autowired
    private OAuthClientService oAuthClientService;
    @Autowired
    private SystemCodeServiceAdapter systemCodeServiceAdapter;

    @SuppressWarnings("unchecked")
    @GetMapping("/v1/oauth-client/{id}")
    @ApiOperation(value = "获取认证客户端V1接口", httpMethod = "GET")
    public Result<OAuthClientResponseVO> queryOAuthClient(@ApiParam(name = "id", value = "主键id", required = true) @PathVariable Long id) {
        OAuthClientBO oAuthClientBO = oAuthClientService.queryOAuthClient(id);
        OAuthClientResponseVO result = new OAuthClientResponseVO();
        BeanUtils.copyProperties(oAuthClientBO, result);
        result.setAuthMethod(systemCodeServiceAdapter.getSystemCodeByCode(oAuthClientBO.getAuthMethod()));
        result.setGrantType(systemCodeServiceAdapter.getSystemCodeByCode(oAuthClientBO.getGrantType()));
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(result)
                .build();
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/v1/oauth-client")
    @ApiOperation(value = "新增认证客户端V1接口", httpMethod = "POST")
    public Result<OAuthClientResponseVO> createOAuthClient(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) OAuthClientVO vo) {
        OAuthClientBO oAuthClientBO = new OAuthClientBO();
        BeanUtils.copyProperties(vo, oAuthClientBO);
        oAuthClientBO = oAuthClientService.createOAuthClient(oAuthClientBO);
        OAuthClientResponseVO result = new OAuthClientResponseVO();
        BeanUtils.copyProperties(oAuthClientBO, result);
        result.setAuthMethod(systemCodeServiceAdapter.getSystemCodeByCode(oAuthClientBO.getAuthMethod()));
        result.setGrantType(systemCodeServiceAdapter.getSystemCodeByCode(oAuthClientBO.getGrantType()));
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(result)
                .build();
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/v1/oauth-client/{id}")
    @ApiOperation(value = "更新认证客户端V1接口", httpMethod = "PUT")
    public Result<OAuthClientResponseVO> updateOAuthClient(@ApiParam(name = "id", value = "主键id", required = true) @PathVariable Long id,
                                                           @Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) OAuthClientVO vo) {
        OAuthClientBO oAuthClientBO = new OAuthClientBO();
        BeanUtils.copyProperties(vo, oAuthClientBO);
        oAuthClientBO.setId(id);
        oAuthClientBO = oAuthClientService.updateOAuthClient(oAuthClientBO);
        OAuthClientResponseVO result = new OAuthClientResponseVO();
        BeanUtils.copyProperties(oAuthClientBO, result);
        result.setAuthMethod(systemCodeServiceAdapter.getSystemCodeByCode(oAuthClientBO.getAuthMethod()));
        result.setGrantType(systemCodeServiceAdapter.getSystemCodeByCode(oAuthClientBO.getGrantType()));
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(result)
                .build();
    }

    @DeleteMapping("/v1/oauth-client")
    @ApiOperation(value = "删除认证客户端V1接口", httpMethod = "DELETE")
    public Result removeOAuthClients(@ApiParam(name = "ids", value = "主键id，支持传多个，半角逗号分隔", required = true) @RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        oAuthClientService.removeOAuthClients(idList);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @PutMapping("/v1/oauth-client/{id}/enable/{enabled}")
    @ApiOperation(value = "启用/禁用认证客户端V1接口", httpMethod = "PUT")
    public Result enableOAuthClient(@ApiParam(name = "id", value = "主键id", required = true) @PathVariable Long id,
                                    @ApiParam(name = "enabled", value = "是否启用", required = true) @PathVariable Boolean enabled) {
        oAuthClientService.enableOAuthClient(id, enabled);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @SuppressWarnings("unchecked")
    @GetMapping(value = "/v1/oauth-clients")
    @ResponseBody
    @ApiOperation(value = "查询认证客户端列表V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "认证客户端名称", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = false, dataType = "String", paramType = "query"),
    })
    public PageResult<OAuthClientResponseVO> queryOAuthClients(@RequestParam(required = false) String name,
                                                               @RequestParam(defaultValue = "1", required = false) Integer current,
                                                               @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        OAuthClientQueryBO queryParams = new OAuthClientQueryBO();
        queryParams.setName(name);
        PageResult<OAuthClientBO> result = oAuthClientService.queryOAuthClients(queryParams, new Pagination(0, pageSize, current));
        List<OAuthClientResponseVO> oAuthClientResponseList = result.getList().stream().map(entity -> {
            OAuthClientResponseVO oAuthClientResponseVO = new OAuthClientResponseVO();
            BeanUtils.copyProperties(entity, oAuthClientResponseVO);
            oAuthClientResponseVO.setAuthMethod(systemCodeServiceAdapter.getSystemCodeByCode(entity.getAuthMethod()));
            oAuthClientResponseVO.setGrantType(systemCodeServiceAdapter.getSystemCodeByCode(entity.getGrantType()));
            return oAuthClientResponseVO;
        }).collect(Collectors.toList());
        return new PageResult<>(oAuthClientResponseList, result.getPagination().getTotal(), result.getPagination().getPageSize(), result.getPagination().getCurrent());
    }
}
