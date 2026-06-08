package com.supcon.supfusion.auth.webapi.controller;

import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.bo.OnlineUserBO;
import com.supcon.supfusion.auth.service.bo.OnlineUserQueryBO;
import com.supcon.supfusion.auth.webapi.vo.OnlineUserResponseVO;
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
@Api(value = "在线用户相关文档", tags = "在线用户")
@Validated
public class OnlineUserController extends BaseController {
    @Autowired
    private OnlineUserService onlineUserService;

    @GetMapping(value = "/v1/online-user")
    @ResponseBody
    @ApiOperation(value = "查询用户目录列表V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "用户名，支持模糊搜索", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "startLoginTime", value = "登陆时间-开始", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "endLoginTime", value = "登陆时间-结束", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = false, dataType = "String", paramType = "query"),
    })
    public PageResult<OnlineUserResponseVO> queryOnlineUsers(@RequestParam(required = false) String username,
                                                             @RequestParam(required = false) String startLoginTime,
                                                             @RequestParam(required = false) String endLoginTime,
                                                             @RequestParam(defaultValue = "1", required = false) Integer current,
                                                             @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        OnlineUserQueryBO queryParams = new OnlineUserQueryBO();
        queryParams.setUserName(username);
        queryParams.setStartLoginTime(startLoginTime);
        queryParams.setEndLoginTime(endLoginTime);
        PageResult<OnlineUserBO> result = onlineUserService.queryOnlineUsers(queryParams, new Pagination(0, pageSize, current));
        List<OnlineUserResponseVO> onlineUserResponseVOList = ((List<OnlineUserBO>) result.getList()).stream().map(entity -> {
            OnlineUserResponseVO onlineUserResponseVO = new OnlineUserResponseVO();
            BeanUtils.copyProperties(entity, onlineUserResponseVO);
            return onlineUserResponseVO;
        }).collect(Collectors.toList());
        return new PageResult<>(onlineUserResponseVOList, result.getPagination().getTotal(), result.getPagination().getPageSize(), result.getPagination().getCurrent());
    }


    @SuppressWarnings("unchecked")
    @DeleteMapping("/v1/online-user")
    @ApiOperation(value = "注销在线用户V1接口", httpMethod = "DELETE")
    public Result logoutOnLineUsers(@ApiParam(name = "ids", value = "主键id，支持传多个，半角逗号分隔", required = true) @RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        onlineUserService.logoutOnlineUsers(idList);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }
}
