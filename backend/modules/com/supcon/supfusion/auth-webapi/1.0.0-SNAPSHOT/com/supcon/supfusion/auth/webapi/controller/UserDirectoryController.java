package com.supcon.supfusion.auth.webapi.controller;

import com.supcon.supfusion.auth.manager.SystemCodeServiceAdapter;
import com.supcon.supfusion.auth.service.UserDirectoryService;
import com.supcon.supfusion.auth.service.bo.UserDirectoryBO;
import com.supcon.supfusion.auth.service.bo.UserDirectoryQueryBO;
import com.supcon.supfusion.auth.webapi.vo.UserDirectoryConnectVO;
import com.supcon.supfusion.auth.webapi.vo.UserDirectoryQueryVO;
import com.supcon.supfusion.auth.webapi.vo.UserDirectoryResponseVO;
import com.supcon.supfusion.auth.webapi.vo.UserDirectoryVO;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author caokele
 */
@Slf4j
@RestController
@InternalApi(path = "/inter-api/auth")
@Api(value = "用户目录相关文档", tags = "用户目录")
@Validated
public class UserDirectoryController extends BaseController {
    @Autowired
    private UserDirectoryService userDirectoryService;
    @Autowired
    private SystemCodeServiceAdapter systemCodeServiceAdapter;

    @SuppressWarnings("unchecked")
    @PostMapping("/v1/user-directory")
    @ApiOperation(value = "新增用户目录V1接口", httpMethod = "POST")
    public Result<UserDirectoryResponseVO> createUserDirectory(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) UserDirectoryVO vo) {
        UserDirectoryBO userDirectoryBO = new UserDirectoryBO();
        BeanUtils.copyProperties(vo, userDirectoryBO);
        userDirectoryBO = userDirectoryService.createUserDirectory(userDirectoryBO);
        UserDirectoryResponseVO result = new UserDirectoryResponseVO();
        BeanUtils.copyProperties(userDirectoryBO, result);
        result.setDirectoryType(systemCodeServiceAdapter.getSystemCodeByCode(userDirectoryBO.getDirectoryType()));
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(result)
                .build();
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/v1/user-directory/{id}")
    @ApiOperation(value = "更新用户目录V1接口", httpMethod = "PUT")
    public Result<UserDirectoryResponseVO> updateUserDirectory(@ApiParam(name = "id", value = "主键id", required = true) @PathVariable Long id,
                                                               @Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) UserDirectoryVO vo) {
        UserDirectoryBO userDirectoryBO = new UserDirectoryBO();
        BeanUtils.copyProperties(vo, userDirectoryBO);
        userDirectoryBO.setId(id);
        userDirectoryBO = userDirectoryService.updateUserDirectory(userDirectoryBO);
        UserDirectoryResponseVO result = new UserDirectoryResponseVO();
        BeanUtils.copyProperties(userDirectoryBO, result);
        result.setDirectoryType(systemCodeServiceAdapter.getSystemCodeByCode(userDirectoryBO.getDirectoryType()));
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(result)
                .build();
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/v1/user-directory/{id}")
    @ApiOperation(value = "获取用户目录V1接口", httpMethod = "GET")
    public Result<UserDirectoryResponseVO> queryUserDirectory(@ApiParam(name = "id", value = "主键id", required = true) @PathVariable Long id) {
        UserDirectoryBO userDirectoryBO = userDirectoryService.queryUserDirectory(id);
        UserDirectoryResponseVO result = new UserDirectoryResponseVO();
        BeanUtils.copyProperties(userDirectoryBO, result);
        result.setDirectoryType(systemCodeServiceAdapter.getSystemCodeByCode(userDirectoryBO.getDirectoryType()));
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .data(result)
                .build();
    }

    @SuppressWarnings("unchecked")
    @DeleteMapping("/v1/user-directory")
    @ApiOperation(value = "删除用户目录V1接口", httpMethod = "DELETE")
    public Result removeUserDirectories(@ApiParam(name = "ids", value = "主键id，支持传多个，半角逗号分隔", required = true) @RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        userDirectoryService.removeUserDirectories(idList);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @PutMapping("/v1/user-directory/{id}/enable/{enabled}")
    @ApiOperation(value = "启用/禁用用户目录V1接口", httpMethod = "PUT")
    public Result enableUserDirectory(@ApiParam(name = "id", value = "主键id", required = true) @PathVariable Long id,
                                      @ApiParam(name = "enabled", value = "是否启用", required = true) @PathVariable Boolean enabled) {
        userDirectoryService.enableUserDirectory(id, enabled);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @PutMapping("/v1/user-directory/{id}/sort/{direction}")
    @ApiOperation(value = "更新用户目录排序V1接口", httpMethod = "PUT")
    public Result sortUserDirectory(@ApiParam(name = "id", value = "主键id", required = true) @PathVariable Long id,
                                    @ApiParam(name = "direction", value = "排序方向 0:向上 1:向下", required = true) @PathVariable Integer direction) {
        userDirectoryService.sortUserDirectory(id, direction);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @PostMapping(value = "/v1/user-directories")
    @ResponseBody
    @ApiOperation(value = "查询用户目录列表V1接口", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = false, dataType = "String", paramType = "query"),
    })
    public PageResult<UserDirectoryResponseVO> queryUserDirectories(@RequestBody @ApiParam(name = "body", value = "json格式", required = true) UserDirectoryQueryVO userDirectoryQueryVO,
                                                                    @RequestParam(defaultValue = "1", required = false) Integer current,
                                                                    @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        UserDirectoryQueryBO queryParams = new UserDirectoryQueryBO();
        BeanUtils.copyProperties(userDirectoryQueryVO, queryParams);
        PageResult<UserDirectoryBO> result = userDirectoryService.queryUserDirectories(queryParams, new Pagination(0, pageSize, current));
        List<UserDirectoryResponseVO> userDirectoryVOList = result.getList().stream().map(entity -> {
            UserDirectoryResponseVO userDirectoryResponseVO = new UserDirectoryResponseVO();
            BeanUtils.copyProperties(entity, userDirectoryResponseVO);
            userDirectoryResponseVO.setDirectoryType(systemCodeServiceAdapter.getSystemCodeByCode(entity.getDirectoryType()));
            return userDirectoryResponseVO;
        }).collect(Collectors.toList());
        return new PageResult<>(userDirectoryVOList, result.getPagination().getTotal(), result.getPagination().getPageSize(), result.getPagination().getCurrent());
    }

    @SuppressWarnings("unchecked")
    @GetMapping(value = "/v1/user-directory/field/values")
    @ResponseBody
    @ApiOperation(value = "查询筛选字段V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fieldName", value = "字段名称", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "fieldValue", value = "字段值，除了系统编码之外支持模糊搜索", required = false, dataType = "String", paramType = "query"),
    })
    public Result<Set<String>> queryUserDirectoryFieldValues(@RequestParam(required = true) String fieldName,
                                                             @RequestParam(required = false) String fieldValue) {
        Set<String> values = userDirectoryService.queryUserDirectoryFieldValues(fieldName, fieldValue);
        return Result.custom()
                .data(values)
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @PostMapping("/v1/user-directory/connect")
    @ApiOperation(value = "测试用户目录连接V1接口", httpMethod = "POST")
    public Result connectUserDirectory(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) UserDirectoryConnectVO vo) {
        UserDirectoryBO userDirectoryBO = new UserDirectoryBO();
        BeanUtils.copyProperties(vo, userDirectoryBO);
        userDirectoryService.connectUserDirectory(userDirectoryBO);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @GetMapping("/v1/user-directory/export")
    @ApiOperation(value = "暂不可用!!!导出用户目录V1接口", httpMethod = "GET")
    public Result exportUserDirectories(@ApiParam(name = "ids", value = "主键id，支持传多个，半角逗号分隔", required = true) @RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        userDirectoryService.exportUserDirectories(idList);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

}
