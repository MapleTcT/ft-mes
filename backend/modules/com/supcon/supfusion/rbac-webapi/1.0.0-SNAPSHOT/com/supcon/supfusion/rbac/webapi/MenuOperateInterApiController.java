package com.supcon.supfusion.rbac.webapi;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.MenuErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuOperateErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuOperateException;
import com.supcon.supfusion.rbac.dao.field.MenuOperateCodeUrlRefField;
import com.supcon.supfusion.rbac.dao.po.MenuOperateCodeUrlRefPO;
import com.supcon.supfusion.rbac.dao.po.MenuOperatePO;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import com.supcon.supfusion.rbac.service.IMenuOperateCodeUrlRefService;
import com.supcon.supfusion.rbac.service.IMenuOperateService;
import com.supcon.supfusion.rbac.webapi.vo.menuOperate.MenuOperateVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * 操作表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1" + HttpConstants.URL_SPLITER)
@Validated
@Api(tags = "菜单操作相关接口")
public class MenuOperateInterApiController extends BaseController {
    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private IMenuOperateCodeUrlRefService menuOperateCodeUrlRefService;
    @Autowired
    II18nAdapter i18nAdapterService;
    @Autowired
    private IMenuInfoService menuInfoService;

    /**
     * 获取翻页结构的操作列表
     *
     * @param menuinfoId 菜单id
     * @param keyword    查询关键字（编码、名称）
     * @param current    翻页页数
     * @param pageSize   每页所含元素据数
     * @return
     */
    @GetMapping("/menuOperatePage")
    @ApiOperation(value = "分页查询菜单操作")
    @ApiImplicitParams({
            @ApiImplicitParam(name="menuinfoId",value="菜单ID",required=false,paramType="query"),
            @ApiImplicitParam(name="keyword",value="关键字，模糊查询操作名和操作编码",required=false,paramType="query"),
            @ApiImplicitParam(name="current",value="当前页",required=true,paramType="query"),
            @ApiImplicitParam(name="pageSize",value="每页个数",required=true,paramType="query"),
    })
    public PageResult<MenuOperateVO> menuOperatePage(@RequestParam(required = false) Long menuinfoId, @RequestParam(required = false) String keyword, @RequestParam(required = true) Integer current, @RequestParam(required = true) Integer pageSize) {
        log.info("GET:/menuOperatePage=params:menuinfoId={},keyword={}***********************************",menuinfoId,keyword);
        PageResult<MenuOperatePO> menuOperatePOResult = menuOperateService.getMenuOperates(menuinfoId, keyword, current, pageSize);
        PageResult<MenuOperateVO> result = new PageResult<MenuOperateVO>();
        result.setPagination(menuOperatePOResult.getPagination());
        List<MenuOperateVO> menuOperateVOS = JSONArray.parseArray(JSON.toJSONString(menuOperatePOResult.getList()), MenuOperateVO.class);
        menuOperateVOS.forEach(menuOperateVO -> {
            menuOperateVO.setIsDefault(menuOperateVO.getDefaultOperate());
            setMenuOperate(menuOperateVO);
        });
        result.setList(menuOperateVOS);
        log.info("GET:/menuOperatePage=response={}***********************************",result);
        return result;
    }

    /**
     * 获取单个操作数据
     *
     * @param code 操作编码
     * @return
     */
    @GetMapping("/menuOperate")
    @ApiOperation(value = "根据编码查询菜单操作")
    @ApiImplicitParams({
            @ApiImplicitParam(name="code",value="菜单编码",required=true,paramType="query"),
    })
    public Result<MenuOperateVO> menuOperate(@RequestParam(required = true) String code) {
        log.info("GET:/menuOperate===params:={}***********************************",code);
        MenuOperateVO menuOperateVO = new MenuOperateVO();
        MenuOperatePO menuOperatePO = menuOperateService.getOne(code);
        BeanUtils.copyProperties(menuOperatePO, menuOperateVO);
        setMenuOperate(menuOperateVO);
        log.info("GET:/menuOperate===response={}***********************************",menuOperateVO);
        return Result.data(menuOperateVO);
    }

    /**
     * 删除操作
     *
     * @param codes 操作编码字符串（,号分割）
     * @return
     */
    @DeleteMapping("/menuOperates/{codes:.+}")
    @ApiOperation(value = "批量删除菜单操作")
    @ApiImplicitParams({
            @ApiImplicitParam(name="codes",value="菜单编码，逗号分隔",required=true,paramType="path"),
    })
    public Result<Boolean> deleteMenuOperates(@NotBlank(message = Constants.MENU_OPERATE_CODE_IS_NOT_EMPTY) @PathVariable String codes) {
        log.info("DELETE:/menuOperates/{codes}===codes={***********************************",codes);
        menuOperateService.deleteMenuOperates(codes);
        return Result.success();
    }

    /**
     * 保存操作
     *
     * @param menuOperate 操作对象
     * @return
     */
    @PostMapping(value = {"/menuOperate","/menuOperate/ref"})
    @ApiOperation(value = "新增菜单操作")
    public Result<Boolean> newMenuOperate(@RequestBody MenuOperateVO menuOperate) {
        log.info("POST:/menuOperate,/menuOperate/ref===params:menuOperate={}***********************************",menuOperate);
        i18nAdapterService.refreshI18n();
        String code = menuOperate.getCode();
        boolean matches = Pattern.matches("^[A-Za-z0-9_]([A-Za-z0-9_.]*[A-Za-z0-9_]){0,500}?$", code);
        if (!matches){
            throw new MenuOperateException(MenuErrorEnum.CODE_UNQUALIFIED);
        }
        MenuOperatePO menuOperatePO = new MenuOperatePO();
        BeanUtils.copyProperties(menuOperate, menuOperatePO);
        //新增时添加cid
        if(null!=menuOperate.getCid()) {
            menuOperate.setCid(UserContext.getUserContext().getCompanyId());
        }
        if(null!=menuOperate.getUrls()) {
            List<MenuOperateCodeUrlRefPO> list = menuOperate.getUrls().stream().map(s -> {
                MenuOperateCodeUrlRefPO menuOperateCodeUrlRefPO = new MenuOperateCodeUrlRefPO();
                BeanUtils.copyProperties(s, menuOperateCodeUrlRefPO);
                menuOperateCodeUrlRefPO.setIsCustom(true);
                menuOperateCodeUrlRefPO.setMenuoperateCode(menuOperate.getCode());
                menuOperateCodeUrlRefPO.setUrl(menuInfoService.filterPathParams(menuOperateCodeUrlRefPO.getUrl()));
//                MenuOperateCodeUrlRefPO one = menuOperateCodeUrlRefService.getOne(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq(MenuOperateCodeUrlRefField.menuoperateCode, menuOperateCodeUrlRefPO.getMenuoperateCode()).eq(MenuOperateCodeUrlRefField.methodType, menuOperateCodeUrlRefPO.getMethodType()).eq(MenuOperateCodeUrlRefField.url, menuOperateCodeUrlRefPO.getUrl()));
//                if (!ObjectUtils.isEmpty(one)){
//                    throw new MenuOperateException(MenuOperateErrorEnum.SAME_URL);
//                }
                menuOperateCodeUrlRefPO.setRegMatch(menuOperateCodeUrlRefPO.getUrl().contains("[^/^]+"));
                return menuOperateCodeUrlRefPO;
            }).collect(Collectors.toList());
            menuOperatePO.setUrls(list);
        }
        log.info("POST:/menuOperate,/menuOperate/ref===menuOperateService.save=menuOperatePO=={}***********************************",menuOperatePO);
        menuOperateService.save(menuOperatePO);
        return Result.success();
    }

    /**
     * 修改操作
     *
     * @param menuOperate 操作对象
     * @return
     */
    @PutMapping(value = {"/menuOperate"})
    @ApiOperation(value = "修改菜单操作")
    public Result<Boolean> updateMenuOperate(@Validated @RequestBody MenuOperateVO menuOperate) {
        log.info("PUT:/menuOperate===params:menuOperate={}***********************************",menuOperate);
        i18nAdapterService.refreshI18n();
        MenuOperatePO menuOperatePO = new MenuOperatePO();
        BeanUtils.copyProperties(menuOperate, menuOperatePO);
        menuOperatePO.setEdited(true);
        //清空操作URL
        menuOperateCodeUrlRefService.remove(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq(MenuOperateCodeUrlRefField.menuoperateCode,menuOperate.getCode()));
        if(null!=menuOperate.getUrls()) {
            List<MenuOperateCodeUrlRefPO> list = menuOperate.getUrls().stream().map(s -> {
                MenuOperateCodeUrlRefPO menuOperateCodeUrlRefPO = new MenuOperateCodeUrlRefPO();
                BeanUtils.copyProperties(s, menuOperateCodeUrlRefPO);
                menuOperateCodeUrlRefPO.setIsCustom(true);
                menuOperateCodeUrlRefPO.setMenuoperateCode(menuOperate.getCode());
                menuOperateCodeUrlRefPO.setUrl(menuInfoService.filterPathParams(menuOperateCodeUrlRefPO.getUrl()));
                if (menuOperate.getUrls().stream().filter(s::equals).count() > 1){
                    throw new MenuOperateException(MenuOperateErrorEnum.SAME_URL);
                }
                menuOperateCodeUrlRefPO.setRegMatch(menuOperateCodeUrlRefPO.getUrl().contains("[^/^]+"));
                return menuOperateCodeUrlRefPO;
            }).collect(Collectors.toList());
            menuOperatePO.setUrls(list);
        }
        log.info("PUT:/menuOperate===menuOperateService.save(menuOperatePO)={}***********************************",menuOperatePO);
        menuOperateService.save(menuOperatePO);
        return Result.success();
    }

    private void setMenuOperate(MenuOperateVO menuOperateVO){
        Locale locale = LocaleContextHolder.getLocale();
        String name = i18nAdapterService.getRemoteMessage(menuOperateVO.getName(),null,locale);
        if (!ObjectUtils.isEmpty(name)){
            menuOperateVO.setNameDisplay(name);
        }else{
            name = i18nAdapterService.MessageResourceGetByKeyOneLanguage(menuOperateVO.getName(),locale.toString());
            if (!ObjectUtils.isEmpty(name)){
                menuOperateVO.setNameDisplay(name);
            }
        }

        if (!ObjectUtils.isEmpty(menuOperateVO.getFullPathName())){
            String fullPathName = menuOperateVO.getFullPathName();
            String[] strings = fullPathName.split("/");
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                String remoteMessage = i18nAdapterService.getRemoteMessage(string, null, locale);
                if (ObjectUtils.isEmpty(remoteMessage)){
                    remoteMessage = i18nAdapterService.MessageResourceGetByKeyOneLanguage(string,locale.toString());
                }
                if (sb.length() > 0){
                    sb.append("/");
                }
                sb.append(remoteMessage);
            }
            menuOperateVO.setFullPathName(sb.toString());
        }
    }
}

