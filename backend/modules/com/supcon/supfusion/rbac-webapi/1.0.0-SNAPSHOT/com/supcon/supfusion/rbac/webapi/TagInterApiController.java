package com.supcon.supfusion.rbac.webapi;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.dao.field.TagField;
import com.supcon.supfusion.rbac.dao.po.TagPO;
import com.supcon.supfusion.rbac.service.ITagService;
import com.supcon.supfusion.rbac.urlscan.annotation.MenuOperateCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.stream.Collectors;

/**
 * <p>
 * 标签表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-10
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1" + HttpConstants.URL_SPLITER + "tag")
@Validated
@Api(tags = "标签相关接口")
public class TagInterApiController extends BaseController {

    @Autowired
    private ITagService tagService;

    /**
     * @description: 查询标签列表
     * @param: tagName
     * @param: size
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<java.lang.String>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("queryRole")
    @GetMapping("/findTagsName")
    @ApiOperation(value = "查询标签列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name="tagName",value="标签名",required=false,paramType="query"),
            @ApiImplicitParam(name="size",value="返回个数",required=false,paramType="query"),
    })
    public ListResult<String> findTag(@RequestParam(required = false,value = "tagName") String tagName,@RequestParam(required = false,value = "size") Integer size){
        QueryWrapper<TagPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(TagField.name);
        if (!ObjectUtils.isEmpty(tagName)){
            queryWrapper.like(TagField.name,tagName);
        }
        queryWrapper.groupBy(TagField.name);
        Page<TagPO> page = new Page<>(1,ObjectUtils.isEmpty(size) ? 10 : size);
        Page<TagPO> pageResult = tagService.page(page, queryWrapper);
        return new ListResult<>(pageResult.getRecords().stream().map(TagPO::getName).collect(Collectors.toList()));
    }
}

