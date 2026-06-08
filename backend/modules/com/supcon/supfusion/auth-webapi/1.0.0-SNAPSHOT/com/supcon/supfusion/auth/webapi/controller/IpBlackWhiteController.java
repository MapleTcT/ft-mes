package com.supcon.supfusion.auth.webapi.controller;

import com.supcon.supfusion.auth.common.utils.IpUtil;
import com.supcon.supfusion.auth.service.IpBlackWhiteService;
import com.supcon.supfusion.auth.service.bo.IpBlackWhiteBO;
import com.supcon.supfusion.auth.service.bo.IpBlackWhiteQueryBO;
import com.supcon.supfusion.auth.webapi.vo.IpBlackWhiteOperateTipVO;
import com.supcon.supfusion.auth.webapi.vo.IpBlackWhiteResponseVO;
import com.supcon.supfusion.auth.webapi.vo.IpBlackWhiteVO;
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
@Api(value = "IP黑白名单相关文档", tags = "IP黑白名单")
@Validated
public class IpBlackWhiteController extends BaseController {
    @Autowired
    private IpBlackWhiteService ipBlackWhiteService;

    @SuppressWarnings("unchecked")
    @PostMapping("/v1/ip-black-white")
    @ApiOperation(value = "新增IP黑白名单V1接口", httpMethod = "POST")
    public Result<IpBlackWhiteResponseVO> createIpBlackWhite(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) IpBlackWhiteVO vo) {
        IpBlackWhiteBO ipBlackWhiteBO = new IpBlackWhiteBO();
        BeanUtils.copyProperties(vo, ipBlackWhiteBO);
        ipBlackWhiteBO.setCurrentIp(IpUtil.getIpAddr(getRequest()));
        ipBlackWhiteBO = ipBlackWhiteService.createIpBlackWhite(ipBlackWhiteBO, vo.getAddCurrentIp());
        IpBlackWhiteResponseVO result = new IpBlackWhiteResponseVO();
        BeanUtils.copyProperties(ipBlackWhiteBO, result);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(result)
                .build();
    }

    @SuppressWarnings("unchecked")
    @DeleteMapping("/v1/ip-black-white")
    @ApiOperation(value = "删除IP黑白名单V1接口", httpMethod = "DELETE")
    public Result removeIpBlackWhiteList(@ApiParam(name = "ids", value = "主键id，支持传多个，半角逗号分隔", required = true) @RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        String currentIp = IpUtil.getIpAddr(getRequest());
        ipBlackWhiteService.removeIpBlackWhiteList(idList, currentIp);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }


    @GetMapping(value = "/v1/ip-black-white/list")
    @ResponseBody
    @ApiOperation(value = "查询IP黑白名单列表V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ip", value = "访问IP，支持通配符", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = false, dataType = "String", paramType = "query"),
    })
    public PageResult<IpBlackWhiteResponseVO> queryIpBlackWhiteList(@RequestParam(required = false) String ip,
                                                                    @RequestParam(defaultValue = "1", required = false) Integer current,
                                                                    @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        IpBlackWhiteQueryBO queryParams = new IpBlackWhiteQueryBO();
        queryParams.setIp(ip);
        PageResult<IpBlackWhiteBO> result = ipBlackWhiteService.queryIpBlackWhiteList(queryParams, new Pagination(0, pageSize, current));
        List<IpBlackWhiteResponseVO> ipBlackWhiteResponseList = result.getList().stream().map(entity -> {
            IpBlackWhiteResponseVO ipBlackWhiteResponseVO = new IpBlackWhiteResponseVO();
            BeanUtils.copyProperties(entity, ipBlackWhiteResponseVO);
            return ipBlackWhiteResponseVO;
        }).collect(Collectors.toList());
        return new PageResult<>(ipBlackWhiteResponseList, result.getPagination().getTotal(), result.getPagination().getPageSize(), result.getPagination().getCurrent());
    }

    @SuppressWarnings("unchecked")
    @PostMapping(value = "/v1/ip-black-white/check/tip")
    @ApiOperation(value = "检查IP黑白名单操作是否需要提示V1接口", httpMethod = "POST")
    public Result<Boolean> checkOperateNeedTip(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) IpBlackWhiteOperateTipVO ipBlackWhiteOperateTipVO) {
        IpBlackWhiteBO ipBlackWhiteBO = new IpBlackWhiteBO();
        BeanUtils.copyProperties(ipBlackWhiteOperateTipVO, ipBlackWhiteBO);
        ipBlackWhiteBO.setCurrentIp(IpUtil.getIpAddr(getRequest()));
        boolean haveMeaning = ipBlackWhiteService.checkOperateNeedTip(ipBlackWhiteBO, ipBlackWhiteOperateTipVO.getOperateType());
        return Result.custom()
                .data(haveMeaning)
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }
}
