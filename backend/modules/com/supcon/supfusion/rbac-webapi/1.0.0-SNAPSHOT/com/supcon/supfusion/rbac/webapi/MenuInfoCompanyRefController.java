package com.supcon.supfusion.rbac.webapi;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.dao.po.MenuInfoCompanyRefPO;
import com.supcon.supfusion.rbac.service.IMenuInfoCompanyRefService;
import com.supcon.supfusion.rbac.webapi.vo.menuInfoCompany.MenuInfoCompanyRefQueryVO;
import com.supcon.supfusion.rbac.webapi.vo.menuInfoCompany.MenuInfoCompanySaveVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单公司关联表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-30
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "菜单公司关联相关接口")
public class MenuInfoCompanyRefController extends BaseController {

    @Autowired
    private IMenuInfoCompanyRefService menuInfoCompanyRefService;

    /**
     * @description: 保存菜单公司关联
     * @param: menuInfoCompanySaveVO
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @PostMapping("/menuInfoCompany")
    @ApiOperation(value = "保存菜单公司关联")
    public void save(@RequestBody MenuInfoCompanySaveVO menuInfoCompanySaveVO){
        log.info("GET:/menuInfoCompany==params:menuInfoCompanySaveVO={}***********************************",menuInfoCompanySaveVO);
        List<MenuInfoCompanyRefPO> collect = menuInfoCompanySaveVO.getCompanies().stream().map(company -> {
            MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
            menuInfoCompanyRefPO.setCompanyId(company.getCompanyId());
            menuInfoCompanyRefPO.setCompanyName(company.getCompanyName());
            menuInfoCompanyRefPO.setMenuinfoId(menuInfoCompanySaveVO.getMenuinfoId());
            return menuInfoCompanyRefPO;
        }).collect(Collectors.toList());
        menuInfoCompanyRefService.saveBatch(collect);
    }

    /**
     * @description: 批量删除菜单公司关联
     * @param: ids
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @DeleteMapping("/menuInfoCompany/{ids}")
    @ApiOperation(value = "批量删除菜单公司关联")
    @ApiImplicitParams({
            @ApiImplicitParam(name="ids",value="菜单公司ID，逗号分隔",required=true,paramType="path"),
    })
    public void delete(@PathVariable String ids){
        log.info("DELETE:/menuInfoCompany/{ids}=params:ids={}***********************************",ids);
        menuInfoCompanyRefService.removeByIds(Arrays.asList(ids.split(",")));
    }

    /**
     * @description: 分页查询菜单关联的公司
     * @param: menuInfoId
     * @param: keyword
     * @param: current
     * @param: pageSize
     * @return: com.supcon.supfusion.framework.cloud.common.result.PageResult<com.supcon.supfusion.rbac.webapi.vo.menuInfoCompany.MenuInfoCompanyRefQueryVO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @GetMapping("/menuInfoCompanies")
    @ApiOperation(value = "分页查询菜单关联的公司")
    @ApiImplicitParams({
            @ApiImplicitParam(name="menuInfoId",value="菜单ID",required=true,paramType="query"),
            @ApiImplicitParam(name="keyword",value="关键词,模糊查询公司名和菜单名",required=false,paramType="query"),
            @ApiImplicitParam(name="current",value="当前页数",required=true,paramType="query"),
            @ApiImplicitParam(name="pageSize",value="每页返回个数",required=true,paramType="query"),
    })
    public PageResult<MenuInfoCompanyRefQueryVO> findPage(@RequestParam(value = "menuInfoId") Long menuInfoId,@RequestParam(value = "keyword",required = false) String keyword,@RequestParam("current") Integer current,@RequestParam("pageSize") Integer pageSize){
        log.info("GET:/menuInfoCompanies=params:menuInfoId={},keyword={},current={},pageSize={}***********************************", menuInfoId, keyword, current, pageSize);
        IPage<MenuInfoCompanyRefPO> page = menuInfoCompanyRefService.findByPage(menuInfoId, keyword, current, pageSize);
        List<MenuInfoCompanyRefQueryVO> collect = page.getRecords().stream().map(menuInfoCompanyRefPO -> {
            MenuInfoCompanyRefQueryVO menuInfoCompanyRefQueryVO = new MenuInfoCompanyRefQueryVO();
            BeanUtils.copyProperties(menuInfoCompanyRefPO,menuInfoCompanyRefQueryVO);
            return menuInfoCompanyRefQueryVO;
        }).collect(Collectors.toList());
//        log.info("GET:/menuInfoCompanies=response:collect={}***********************************", collect);
        return new PageResult<>(collect,page.getTotal(),page.getSize(),page.getCurrent());
    }
}

