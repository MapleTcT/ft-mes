package com.supcon.supfusion.rbac.webapi;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.module.registry.api.ModuleRegistryApi;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.MenuErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuException;
import com.supcon.supfusion.rbac.common.exception.MenuOperateException;
import com.supcon.supfusion.rbac.dao.field.MenuInfoCompanyRefField;
import com.supcon.supfusion.rbac.dao.field.MenuInfoField;
import com.supcon.supfusion.rbac.dao.po.MenuInfoCompanyRefPO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.dao.po.MenuSuposPO;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import com.supcon.supfusion.rbac.service.IAppRefService;
import com.supcon.supfusion.rbac.service.IMenuInfoCompanyRefService;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import com.supcon.supfusion.rbac.service.impl.MenuInfoServiceImpl;
import com.supcon.supfusion.rbac.urlscan.annotation.MenuOperateCode;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.AdminButtonVO;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoBaseVO;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoEnableStatusVO;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoIcon;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoRuntimeVO;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoSuposVO;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoVO;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuSortVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "菜单相关接口")
public class MenuInfoInterApiController extends BaseController {

    @Autowired
    IMenuInfoService iMenuInfoService;
    @Autowired
    IMenuInfoCompanyRefService menuInfoCompanyRefService;
    @Autowired
    II18nAdapter i18nAdapterService;
    @Qualifier("rbacStringRedisTemplate")
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private IAppRefService appRefService;
    @Value("${spring.application.name}")
    private String app;

    private static Map<String,String> MODULE_NAME = new HashMap<>();
    @Autowired
    private ModuleRegistryApi moduleRegistryApi;
    @Autowired
    private MenuInfoServiceImpl menuInfoServiceImpl;
    private static List<String> MODULE_CODE=new ArrayList<>();

    /**
     * 创建菜单数据
     *
     * @param menuInfoVO
     */
    @MenuOperateCode(value = "addMenu")
    @PostMapping(value = {"/menu", "/menu/ref"})
    @ApiOperation(value = "新增菜单")
    Result<MenuInfoVO> addMenu(@Validated @RequestBody MenuInfoVO menuInfoVO) {
        log.info("GET:/menu,/menu/ref==params:menuInfoVO={}***********************************",menuInfoVO);
        //新增修改时先更新下国际化资源
        i18nAdapterService.refreshI18n();
        String code = menuInfoVO.getCode();
        //修改前端的失误
        if ("current".equals(menuInfoVO.getTarget())) {
            menuInfoVO.setTarget("SELF");
        }
        boolean matches = Pattern.matches("^[A-Za-z0-9_]([A-Za-z0-9_.]*[A-Za-z0-9_]){0,500}?$", code);
        if (!matches) {
            throw new MenuOperateException(MenuErrorEnum.CODE_UNQUALIFIED);
        }
        MenuInfoPO menuInfoPO = new MenuInfoPO();
        BeanUtils.copyProperties(menuInfoVO, menuInfoPO);
        menuInfoPO.setCid(UserContext.getUserContext().getCompanyId());
        //新增的菜单ROUTE字段后台直接填，把URL的?,=,#号换成/，url参数不同的前端会认为是同一个url，不进行跳转
        if (!ObjectUtils.isEmpty(menuInfoPO.getUrl())){
            menuInfoPO.setRoute(menuInfoPO.getUrl().replaceAll("\\?","/").replaceAll("=","/").replaceAll("#", "/"));
        }
        if (ObjectUtils.isEmpty(menuInfoPO.getApp())) {
            if (!ObjectUtils.isEmpty(menuInfoPO.getModuleCode())) {
                menuInfoPO.setApp(menuInfoPO.getModuleCode().split("_")[0]);
            } else {
                menuInfoPO.setApp(app);
            }
        }
        if (ObjectUtils.isEmpty(menuInfoPO.getModuleCode())) {
            menuInfoPO.setModuleCode(app);
        }
        if (ObjectUtils.isEmpty(menuInfoVO.getCompanyIds())) {
        	menuInfoPO.setCompanyIds(Collections.singletonList(menuInfoVO.getCid()));
        }
        // 适用范围包含所有公司,则直接跳过此判断
//        if (!menuInfoVO.getCompanyIds().contains(-1L) && !menuInfoVO.getCompanyIds().contains(menuInfoPO.getCid())) {
//            throw new MenuException(MenuErrorEnum.THE_CREATION_COMPANY_MUST_IN_COMPANYS);
//        }
        // 处理status字段
        if (ObjectUtils.isEmpty(menuInfoVO.getStatus())) {
            menuInfoPO.setStatus(1);
        } else {
        	menuInfoPO.setStatus(menuInfoVO.getStatus());
        }
        iMenuInfoService.addMenu(menuInfoPO);
        MenuInfoVO menuInfoVO1 = new MenuInfoVO();
        BeanUtils.copyProperties(menuInfoPO, menuInfoVO1);
        menuInfoVO1.setModuleName(iMenuInfoService.getModuleName(menuInfoVO1.getModuleCode()));
//        log.info("GET:/menu,/menu/ref==response:menuInfoVO1={}======================================================================",menuInfoVO1);
        return Result.data(menuInfoVO1);
    }

    /**
     * 查询菜单树数据
     *
     * @return
     */
    @MenuOperateCode(value = {"queryMenu", "queryMenuConfigure"})
    @GetMapping(value = {"/menus"})
    @ApiOperation(value = "菜单列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "enableStatus", value = "启停状态筛选参数", required = false, paramType = "query"),
            @ApiImplicitParam(name = "restrict", value = "是否查询无限制菜单", required = false, paramType = "query"),
    })
    ListResult<MenuInfoVO> queryMenus(@RequestParam(value = "enableStatus", required = false,defaultValue = "true") boolean enableStatus,@RequestParam(value = "restrict",required = false) String restrict) {
    	log.info("GET:/menus=params:enableStatus={},restrict={}***********************************",enableStatus,restrict);
        MODULE_CODE=getSystemModule();
        List<MenuInfoPO> menuInfoPOList = iMenuInfoService.queryMenus(enableStatus, !ObjectUtils.isEmpty(restrict));
        List<MenuInfoVO> menuInfoVOList = JSONArray.parseArray(JSON.toJSONString(menuInfoPOList), MenuInfoVO.class);
        setMenuInfoModuleName(menuInfoVOList,null);
//        log.info("GET:/menus=response:menuInfoVOList={}***********************************",menuInfoVOList);
        return new ListResult<>(menuInfoVOList);
    }

    /**
     * 查询菜单树数据
     *
     * @return
     */
    @GetMapping(value = {"/menus/ref"})
    @ApiOperation(value = "菜单列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "enableStatus", value = "启停状态筛选参数", required = false, paramType = "query"),
            @ApiImplicitParam(name = "restrict", value = "是否查询无限制菜单", required = false, paramType = "query"),
    })
    ListResult<MenuInfoVO> queryMenusRef(@RequestParam(value = "enableStatus", required = false,defaultValue = "true") boolean enableStatus,@RequestParam(value = "restrict",required = false) String restrict) {
        log.info("GET:/menus/ref=params:enableStatus={},restrict={}***********************************",enableStatus,restrict);
        List<MenuInfoPO> menuInfoPOList = iMenuInfoService.queryMenusRef(enableStatus, !ObjectUtils.isEmpty(restrict));
        List<MenuInfoVO> menuInfoVOList = JSONArray.parseArray(JSON.toJSONString(menuInfoPOList), MenuInfoVO.class);
        // 用户分配权限页面,菜单数据去除版本信息,待办中心
        menuInfoVOList.removeIf(menuInfoVO -> "versionInfo".equals(menuInfoVO.getCode()) || "todoCenter".equals(menuInfoVO.getCode()));
//        log.info("GET:/menus=response:menuInfoVOList={}***********************************",menuInfoVOList);
        return new ListResult<>(menuInfoVOList);
    }



    /**
     * 查询菜单配置数据,,仅限查询运行期菜单
     *
     * @return
     */
    @MenuOperateCode(value = "queryResourcesRuntime")
    @GetMapping(value = "/resources/runtime")
    @ApiOperation(value = "查询菜单配置数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "enableStatus", value = "启停状态筛选参数", required = false, paramType = "query"),
            @ApiImplicitParam(name = "restrict", value = "是否查询无限制菜单", required = false, paramType = "query"),
    })
    ListResult<MenuInfoVO> queryMenuConfigure(@RequestParam(value = "enableStatus", required = false,defaultValue = "true") boolean enableStatus,@RequestParam(value = "restrict",required = false) String restrict) {
        log.info("GET:/resources/runtime=params:enableStatus={},restrict={}***********************************",enableStatus,restrict);
        List<MenuInfoPO> menuInfoPOList = iMenuInfoService.queryMenuConfigure(enableStatus, !ObjectUtils.isEmpty(restrict));
        List<MenuInfoPO> personalInfoList = new ArrayList<>();

        long e1 = System.currentTimeMillis();
        List<MenuInfoVO> menuInfoVOList = convertToMenuInfoVO(menuInfoPOList, personalInfoList);
        long e2 = System.currentTimeMillis();
        log.info("MenuInfoInterApiController.queryMenuConfigure=====convertToMenuInfoVO==e2-e1===========================",e2-e1);

        long e3 = System.currentTimeMillis();
        // 批量设置模块名
        setMenuInfoModuleName(menuInfoVOList,null);
        long e4 = System.currentTimeMillis();
        log.info("MenuInfoInterApiController.queryMenuConfigure=====setMenuInfoModuleName==e4-e3===========================",e4-e3);

        if (!CollectionUtils.isEmpty(personalInfoList)) {
            menuInfoVOList.addAll(convertToPersonalInfo(personalInfoList));
        }
//        log.info("GET:/resources/runtime=response:menuInfoVOList={}***********************************",menuInfoVOList);
        return new ListResult<>(menuInfoVOList);
    }

    @GetMapping(value = {"/menus/findByKeyword","/menus/findByKeyword/ref"})
    @ApiOperation(value = "模糊查询和指定ID查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "模糊查询,菜单名和菜单CODE", required = false, paramType = "query"),
            @ApiImplicitParam(name = "id", value = "菜单ID", required = false, paramType = "query"),
    })
    ListResult<MenuInfoVO> findByKeyword(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "id", required = false) Long id, @RequestParam(value = "enable", required = false, defaultValue = "true") String enable) {
        log.info("====================== /inter-api/rbac/v1/menus/findByKeyword beginning ======================");
        log.info("==> keyword={}, id={}, enable={}", keyword, id, enable);

        List<MenuInfoPO> menuInfoPOS = iMenuInfoService.queryMenuList(keyword, id, enable);
        List<MenuInfoVO> menuInfoVOS = JSONArray.parseArray(JSON.toJSONString(menuInfoPOS), MenuInfoVO.class);
        //  set modulename batch
        setMenuInfoModuleName(menuInfoVOS,null);

        log.info("====================== /inter-api/rbac/v1/menus/findByKeyword ending =========================");
        return new ListResult<>(menuInfoVOS);
    }

    /**
     * 菜单配置模糊查询,,仅限查询运行期菜单
     * @param keyword
     * @param id
     * @param enable
     * @return
     */
    @GetMapping(value = "/resources/runtime/findByKeyword")
    @ApiOperation(value = "模糊查询和指定ID查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "模糊查询,菜单名和菜单CODE", required = false, paramType = "query"),
            @ApiImplicitParam(name = "id", value = "菜单ID", required = false, paramType = "query"),
    })
    ListResult<MenuInfoVO> findResourcesByKeyword(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "id", required = false) Long id,@RequestParam(value = "enable",required = false) String enable) {
        log.info("GET:/resources/runtime/findByKeyword=params:keyword={},id={}***********************************",keyword,id);
        List<MenuInfoPO> menuInfoPOList = iMenuInfoService.queryMenuConfigureList(keyword, id, enable);
        List<MenuInfoPO> personalInfoList = new ArrayList<>();
        List<MenuInfoVO> menuInfoVOList = convertToMenuInfoVO(menuInfoPOList, personalInfoList);
        if (!CollectionUtils.isEmpty(personalInfoList)) {
            menuInfoVOList.addAll(convertToPersonalInfo(personalInfoList));
        }
//        log.info("GET:/resources/runtime/findByKeyword=response:menuInfoVOList={}***********************************",menuInfoVOList);
        return new ListResult<>(menuInfoVOList);
    }

    private List<MenuInfoVO> convertToMenuInfoVO(List<MenuInfoPO> menuInfoPOList, List<MenuInfoPO> personalInfoList) {
        List<MenuInfoVO> menuInfoVOList = new ArrayList<>();
        if (!ObjectUtils.isEmpty(menuInfoPOList)) {
            for (MenuInfoPO menuInfoPO : menuInfoPOList) {
                if ("personalInfo".equals(menuInfoPO.getCode())) {
                    personalInfoList.add(menuInfoPO);
                    continue;
                }
                MenuInfoVO menuInfoVO = new MenuInfoVO();
                menuInfoVO.setCode(menuInfoPO.getCode());
                menuInfoVO.setId(menuInfoPO.getId());
                menuInfoVO.setName(menuInfoPO.getName());
                menuInfoVO.setNameDisplay(menuInfoPO.getNameDisplay());
                menuInfoVO.setParentId(menuInfoPO.getParentId());
                menuInfoVO.setShowType(menuInfoPO.getShowType());
                menuInfoVO.setSource(menuInfoPO.getSource());
                menuInfoVO.setTarget(menuInfoPO.getTarget());
                menuInfoVO.setType(menuInfoPO.getType());
                menuInfoVO.setUrl(menuInfoPO.getUrl());
                menuInfoVO.setApp(menuInfoPO.getApp());
                menuInfoVO.setCid(menuInfoPO.getCid());
                menuInfoVO.setCompanyIds(menuInfoPO.getCompanyIds());
                menuInfoVO.setCssClass(menuInfoPO.getCssClass());
                menuInfoVO.setMemo(menuInfoPO.getMemo());
                menuInfoVO.setMenuType(menuInfoPO.getMenuType());
                menuInfoVO.setModuleCode(menuInfoPO.getModuleCode());
                menuInfoVO.setChildren(convertToMenuInfoVO(menuInfoPO.getChildren(), personalInfoList));
                menuInfoVO.setCompany_readOnly(menuInfoPO.getCompany_readOnly());
                menuInfoVOList.add(menuInfoVO);
            }

        }
        return menuInfoVOList;
    }

    private List<MenuInfoVO> convertToPersonalInfo(List<MenuInfoPO> personalInfoList){
        List<MenuInfoVO> personalInfoVOList = new ArrayList<>();
        // 最后单独处理个人信息管理以及子菜单,放到列表的最后位置
        if (!CollectionUtils.isEmpty(personalInfoList)) {
            for (MenuInfoPO menuInfoPO : personalInfoList) {
                MenuInfoVO menuInfoVO = new MenuInfoVO();
                menuInfoVO.setCode(menuInfoPO.getCode());
                menuInfoVO.setId(menuInfoPO.getId());
                menuInfoVO.setName(menuInfoPO.getName());
                menuInfoVO.setNameDisplay(menuInfoPO.getNameDisplay());
                menuInfoVO.setParentId(menuInfoPO.getParentId());
                menuInfoVO.setShowType(menuInfoPO.getShowType());
                menuInfoVO.setSource(menuInfoPO.getSource());
                menuInfoVO.setTarget(menuInfoPO.getTarget());
                menuInfoVO.setType(menuInfoPO.getType());
                menuInfoVO.setUrl(menuInfoPO.getUrl());
                menuInfoVO.setApp(menuInfoPO.getApp());
                menuInfoVO.setCid(menuInfoPO.getCid());
                menuInfoVO.setCompanyIds(menuInfoPO.getCompanyIds());
                menuInfoVO.setCssClass(menuInfoPO.getCssClass());
                menuInfoVO.setMemo(menuInfoPO.getMemo());
                menuInfoVO.setMenuType(menuInfoPO.getMenuType());
                menuInfoVO.setModuleCode(menuInfoPO.getModuleCode());
                menuInfoVO.setChildren(convertToPersonalInfo(menuInfoPO.getChildren()));
                setMenuInfoI18n(menuInfoVO);
                setMenuInfoModuleName(menuInfoVO);
                personalInfoVOList.add(menuInfoVO);
            }
        }
        return personalInfoVOList;
    }

    /**
     * 查询菜单树数据
     *
     * @return
     */
    @MenuOperateCode("queryMenu")
    @GetMapping(value = "/menuTree")
    @ApiOperation(value = "菜单树查询")
    Result<MenuInfoVO> queryMenuTree() {
        MenuInfoPO menuInfoPO = iMenuInfoService.queryMenuTree();
        MenuInfoVO menuInfoVO = JSON.parseObject(JSON.toJSONString(menuInfoPO), MenuInfoVO.class);
        setMenuInfoI18n(menuInfoVO);
//        log.info("GET:/menuTree====response:menuInfoVO={}***********************************",menuInfoVO);
        return new Result<>(menuInfoVO);
    }

    /**
     * 修改菜单基本信息
     *
     * @param menuInfoVO
     */
    @MenuOperateCode("editMenu")
    @PutMapping(value = {"/menu","/menu/ref"})
    @ApiOperation(value = "菜单修改")
    Result<MenuInfoVO> updateMenu(@Validated @RequestBody MenuInfoVO menuInfoVO) {
//        log.info("PUT:/menu,/menu/ref==params:menuInfoVO={}***********************************",menuInfoVO);
        //新增修改时先更新下国际化资源
        i18nAdapterService.refreshI18n();
        MenuInfoPO menuInfoPO = new MenuInfoPO();
        BeanUtils.copyProperties(menuInfoVO, menuInfoPO);
        if (ObjectUtils.isEmpty(menuInfoVO.getCid()) || !menuInfoVO.getCid().equals(UserContext.getUserContext().getCompanyId())) {
            throw new MenuException(MenuErrorEnum.NO_AUTHORITY_EDIT);
        }
        //新增的菜单ROUTE字段后台直接填，把URL的?、=号换成/，url参数不同的前端会认为是同一个url，不进行跳转
        if (!ObjectUtils.isEmpty(menuInfoPO.getUrl())){
            menuInfoPO.setRoute(menuInfoPO.getUrl().replaceAll("\\?","/").replaceAll("=","/").replaceAll("#", "/"));
        }
        //修改过的菜单打上标识
        menuInfoPO.setEdited(true);
        iMenuInfoService.updateMenu(menuInfoPO);
        if (!ObjectUtils.isEmpty(menuInfoVO.getCompanyIds()) && (ObjectUtils.isEmpty(menuInfoVO.getCompany_readOnly()) || !menuInfoVO.getCompany_readOnly())){
            //先删除菜单的公司关联，再新增
            menuInfoCompanyRefService.remove(new QueryWrapper<MenuInfoCompanyRefPO>().eq(MenuInfoCompanyRefField.menuinfoId, menuInfoPO.getId()));
            List<MenuInfoCompanyRefPO> menuInfoCompanyRefPOS = menuInfoVO.getCompanyIds().stream().map(id -> {
                MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
                menuInfoCompanyRefPO.setCompanyId(id);
                menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
                return menuInfoCompanyRefPO;
            }).collect(Collectors.toList());
            menuInfoCompanyRefService.saveOrUpdateBatch(menuInfoCompanyRefPOS);
        }
        MenuInfoVO menuInfoVO1 = new MenuInfoVO();
        BeanUtils.copyProperties(menuInfoPO, menuInfoVO1);
//        log.info("PUT:/menu,/menu/ref==response:menuInfoVO1={}***********************************",menuInfoVO1);
        return Result.data(menuInfoVO1);
    }

    /**
     * 批量删除菜单数据
     *
     * @param codes
     */
    @MenuOperateCode("deleteMenu")
    @DeleteMapping(value = "/menus/{codes:.+}")
    @ApiOperation(value = "菜单批量删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "codes", value = "菜单编码，逗号分隔", required = true, paramType = "path"),
    })
    void batchDeleteMenus(@NotBlank(message = Constants.MENU_CODE_IS_NOT_EMPTY) @PathVariable("codes") String codes) {
        log.info("DELETE:/menus/{codes}==params={}***********************************",codes);
        if (codes == null) {
            throw new MenuException(MenuErrorEnum.MENU_DELETE_DATA_IS_NOT_EMPTY);
        }
        String[] codeArray = codes.split(",");
        List<String> codeList = Arrays.asList(codeArray);
        iMenuInfoService.batchDeleteMenus(codeList);
    }

    /**
     * 修改菜单顺序
     *
     * @param menuSortVO
     */
    @MenuOperateCode("editMenu")
    @PutMapping(value = "/menu/sort")
    @ApiOperation(value = "菜单排序修改")
    void modifyMenuSort(@RequestBody MenuSortVO menuSortVO) {
        log.info("PUT:/menu/sort===params:menuSortVO={}***********************************",menuSortVO);
        JSONObject jsonObject = (JSONObject) JSONObject.toJSON(menuSortVO);
        iMenuInfoService.modifyMenuSort(jsonObject);
    }

    /**
     * 启停用
     *
     * @param menuInfo
     * @Author yy
     */
    @MenuOperateCode("enableMenu")
    @PutMapping(value = "/menu/modifyEnableStatus")
    @ApiOperation(value = "启停用菜单")
    void modifyMenuEnableStatus(@RequestBody MenuInfoEnableStatusVO menuInfo) {
        log.info("PUT:/menu/modifyEnableStatus===params:menuInfo={}***********************************",menuInfo);
        MenuInfoPO menuInfoPO = new MenuInfoPO();
        BeanUtils.copyProperties(menuInfo, menuInfoPO);
        iMenuInfoService.modifyMenuEnableStatus(menuInfoPO);
    }

    /**
     * @description: 查询当前登陆人菜单
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoVO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @GetMapping("/menus/currentUser")
    @ApiOperation(value = "查询当前登陆人菜单")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuCode", value = "菜单编码", required = false, paramType = "path"),
    })
    public ListResult<MenuInfoVO> findUserMenus(@RequestParam(value = "menuCode",required = false) String menuCode) {
        log.info("GET:/menus/currentUser===menuCode={}***********************************",menuCode);
        UserContext userContext = UserContext.getUserContext();
        Long userId = userContext.getUserId();
        List<MenuInfoPO> userMenus = iMenuInfoService.findUserMenu(userId, null ,null);
        sortMenuList(userMenus);
        List<MenuInfoVO> menuInfoVOS = JSONArray.parseArray(JSON.toJSONString(userMenus), MenuInfoVO.class);
        for (MenuInfoVO mo : menuInfoVOS) {
            setMenuInfoI18n(mo);
        }
        if (!ObjectUtils.isEmpty(menuCode)){
            menuInfoVOS = findAssignMenu(menuInfoVOS,menuCode);
        }
        log.info("GET:/menus/currentUser===menuInfoVOS={}***********************************",menuInfoVOS);
        return new ListResult<>(menuInfoVOS);
    }

    private void sortMenuList(List<MenuInfoPO> menuInfoPOS){
        menuInfoPOS.sort(Comparator.comparing(MenuInfoPO::getSort,Comparator.nullsLast(Comparator.nullsLast(Double::compareTo))));
        menuInfoPOS.forEach(menuInfoPO -> {
            if (!ObjectUtils.isEmpty(menuInfoPO.getChildren())){
                sortMenuList(menuInfoPO.getChildren());
            }
        });
    }

    private List<MenuInfoVO> findAssignMenu(List<MenuInfoVO> menuInfoVOS,String menuCode){
        List<MenuInfoVO> result = new ArrayList<>();
        for (MenuInfoVO menuInfoVO : menuInfoVOS) {
            if (menuCode.equals(menuInfoVO.getCode())){
                result =  Collections.singletonList(menuInfoVO);
                break;
            }
            if (!ObjectUtils.isEmpty(menuInfoVO.getChildren())){
                result = findAssignMenu(menuInfoVO.getChildren(),menuCode);
                if (!ObjectUtils.isEmpty(result)){
                    break;
                }
            }
        }
        return result;
    }

    /**
     * @description: 查询当前登陆人菜单
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoVO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @GetMapping("/menus/currentUser/supos")
    @ApiOperation(value = "查询当前登陆人菜单")
    public ListResult<MenuInfoSuposVO> findUserMenusSupos(@RequestParam(value = "status",required = false) Integer status) {
        long begin = System.currentTimeMillis();
        log.info("====================== /inter-api/rbac/v1/menus/currentUser/supos beginning ======================");
        log.info("==> trace id={}, status={}", RpcContext.getContext().getTraceId(), status);

        List<MenuSuposPO> userMenus = iMenuInfoService.findUserPermissionMenusTree(UserContext.getUserContext().getUserId(), status);

        long b = System.currentTimeMillis();
        List<MenuInfoSuposVO> menuInfoSuposVOS = toSuposMenusVO(userMenus);
        long e = System.currentTimeMillis();
        log.info("==> 菜单从平铺转换输出, {}ms, size={}", e - b, menuInfoSuposVOS.size());

        long end = System.currentTimeMillis();
        log.info("====================== /inter-api/rbac/v1/menus/currentUser/supos ending ========================={}ms", end - begin);
        return new ListResult<>(menuInfoSuposVOS);
    }

    @GetMapping("/menus/currentUser/supos/admin_button")
    @ApiOperation(value = "查询当前登陆人是否有组态期按钮权限")
    public Result<AdminButtonVO> adminButton() {
        UserContext userContext = UserContext.getUserContext();
        Long userId = userContext.getUserId();
        boolean isAdmin = iMenuInfoService.isAdmin(userId);
        AdminButtonVO adminButtonVO = new AdminButtonVO();
        adminButtonVO.setShow(isAdmin);
        return new Result<>(adminButtonVO);
    }

    private List<MenuInfoSuposVO> toSuposMenusVO(List<MenuSuposPO> menus) {
        final List<MenuInfoSuposVO> vos = new LinkedList<>();

        final List<MenuInfoSuposVO> personalInfoMenuVOList = new LinkedList<>();
        Optional.ofNullable(menus).ifPresent(pos -> pos.forEach(po -> {
            if ("personalInfo".equals(po.getCode())) {
                MenuInfoSuposVO menuInfoSuposVO = new MenuInfoSuposVO();
                menuInfoSuposVO.setRoute(po.getRoute());
                menuInfoSuposVO.setCode(po.getCode());
                menuInfoSuposVO.setName(po.getName());
                menuInfoSuposVO.setType(ObjectUtils.isEmpty(po.getShowType()) ? 0 : po.getShowType());
                menuInfoSuposVO.setUrl(po.getUrl());
                if (StringUtils.isBlank(po.getRoute())) {
                    menuInfoSuposVO.setRoute(po.getUrl());
                }
                menuInfoSuposVO.setDisplayName(po.getNameDisplay());
                menuInfoSuposVO.setId(po.getId());
                menuInfoSuposVO.setIndex(po.getSort());
                menuInfoSuposVO.setNewTab("blank".equalsIgnoreCase(po.getTarget()));
                MenuInfoIcon menuInfoIcon = new MenuInfoIcon();
                menuInfoIcon.setType("icon");
                menuInfoIcon.setValue(po.getCssClass());
                menuInfoSuposVO.setIcon(menuInfoIcon);
                menuInfoSuposVO.setChildren(toSuposMenusVO(po.getChildren()));

                personalInfoMenuVOList.add(menuInfoSuposVO);
            } else {
                MenuInfoSuposVO menuInfoSuposVO = new MenuInfoSuposVO();
                menuInfoSuposVO.setRoute(po.getRoute());
                menuInfoSuposVO.setCode(po.getCode());
                menuInfoSuposVO.setName(po.getName());
                menuInfoSuposVO.setType(ObjectUtils.isEmpty(po.getShowType()) ? 0 : po.getShowType());
                menuInfoSuposVO.setUrl(po.getUrl());
                if (StringUtils.isNotBlank(po.getRoute())) {
                    if (po.getRoute().indexOf("?") != -1) {
                        menuInfoSuposVO.setRoute(po.getRoute().split("\\?")[0]);
                    } else {
                        menuInfoSuposVO.setRoute(po.getRoute());
                    }
                } else {
                    if (StringUtils.isNotBlank(po.getUrl())) {
                        if (po.getUrl().indexOf("?") != -1) {
                            menuInfoSuposVO.setRoute(po.getUrl().split("\\?")[0]);
                        } else {
                            menuInfoSuposVO.setRoute(po.getUrl());
                        }
                    }
                }
                menuInfoSuposVO.setDisplayName(po.getNameDisplay());
                menuInfoSuposVO.setId(po.getId());
                menuInfoSuposVO.setIndex(po.getSort());
                menuInfoSuposVO.setNewTab("blank".equalsIgnoreCase(po.getTarget()));
                MenuInfoIcon menuInfoIcon = new MenuInfoIcon();
                menuInfoIcon.setType("icon");
                menuInfoIcon.setValue(po.getCssClass());
                if (null != po.getMenuType() && po.getMenuType().equals(0)) {
                    menuInfoIcon.setValue(StringUtils.isEmpty(po.getCssClass()) ? "icon_app_nav" : po.getCssClass());
                }
                menuInfoSuposVO.setIcon(menuInfoIcon);
                menuInfoSuposVO.setChildren(toSuposMenusVO(po.getChildren()));

                vos.add(menuInfoSuposVO);
            }
        }));

        // 将personalInfo的菜单放在最后面
        vos.addAll(personalInfoMenuVOList);

        return vos;
    }

    private List<MenuInfoSuposVO> menuInfoToMenuInfoSuposVO(List<MenuInfoPO> menuInfoPOS) {
        List<MenuInfoSuposVO> menuInfoSuposVOList = new LinkedList<>();

        List<MenuInfoSuposVO> personalInfoMenusVOList = new LinkedList<>();
        Optional.ofNullable(menuInfoPOS).ifPresent(pos -> pos.forEach(po -> {
            if ("personalInfo".equals(po.getCode())) {
                MenuInfoSuposVO menuInfoSuposVO = new MenuInfoSuposVO();
                menuInfoSuposVO.setRoute(po.getRoute());
                menuInfoSuposVO.setCode(po.getCode());
                menuInfoSuposVO.setName(po.getName());
                menuInfoSuposVO.setType(ObjectUtils.isEmpty(po.getShowType()) ? 0 : po.getShowType());
                menuInfoSuposVO.setUrl(po.getUrl());
                if (StringUtils.isBlank(po.getRoute())) {
                    menuInfoSuposVO.setRoute(po.getUrl());
                }
                menuInfoSuposVO.setDisplayName(po.getNameDisplay());
                menuInfoSuposVO.setId(po.getId());
                menuInfoSuposVO.setIndex(po.getSort());
                menuInfoSuposVO.setNewTab("blank".equalsIgnoreCase(po.getTarget()));
                MenuInfoIcon menuInfoIcon = new MenuInfoIcon();
                menuInfoIcon.setType("icon");
                menuInfoIcon.setValue(po.getCssClass());
                menuInfoSuposVO.setIcon(menuInfoIcon);
                menuInfoSuposVO.setChildren(menuInfoToMenuInfoSuposVO(po.getChildren()));

                personalInfoMenusVOList.add(menuInfoSuposVO);
            } else {
                MenuInfoSuposVO menuInfoSuposVO = new MenuInfoSuposVO();
                menuInfoSuposVO.setRoute(po.getRoute());
                menuInfoSuposVO.setCode(po.getCode());
                menuInfoSuposVO.setName(po.getName());
                menuInfoSuposVO.setType(ObjectUtils.isEmpty(po.getShowType()) ? 0 : po.getShowType());
                menuInfoSuposVO.setUrl(po.getUrl());
                if (StringUtils.isNotBlank(po.getRoute())) {
                    if (po.getRoute().indexOf("?") != -1) {
                        menuInfoSuposVO.setRoute(po.getRoute().split("\\?")[0]);
                    } else {
                        menuInfoSuposVO.setRoute(po.getRoute());
                    }
                } else {
                    if (StringUtils.isNotBlank(po.getUrl())) {
                        if (po.getUrl().indexOf("?") != -1) {
                            menuInfoSuposVO.setRoute(po.getUrl().split("\\?")[0]);
                        } else {
                            menuInfoSuposVO.setRoute(po.getUrl());
                        }
                    }
                }
                menuInfoSuposVO.setDisplayName(po.getNameDisplay());
                menuInfoSuposVO.setId(po.getId());
                menuInfoSuposVO.setIndex(po.getSort());
                menuInfoSuposVO.setNewTab("blank".equalsIgnoreCase(po.getTarget()));
                MenuInfoIcon menuInfoIcon = new MenuInfoIcon();
                menuInfoIcon.setType("icon");
                menuInfoIcon.setValue(po.getCssClass());
                if (null != po.getMenuType() && po.getMenuType().equals(0)) {
                    menuInfoIcon.setValue(StringUtils.isEmpty(po.getCssClass()) ? "icon_app_nav" : po.getCssClass());
                }
                menuInfoSuposVO.setIcon(menuInfoIcon);
                menuInfoSuposVO.setChildren(menuInfoToMenuInfoSuposVO(po.getChildren()));

                menuInfoSuposVOList.add(menuInfoSuposVO);
            }
        }));

        // 将personalInfo的菜单放在最后面
        menuInfoSuposVOList.addAll(personalInfoMenusVOList);

        return menuInfoSuposVOList;
    }

    /**
     * @description: 菜单联想查询
     * @param: keyword
     * @param: size
     * @param: restrict
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoVO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("queryMenu")
    @GetMapping(value = {"/menus/associate", "/menus/associate/ref"})
    @ApiOperation(value = "菜单联想查询")
    public ListResult<MenuInfoVO> fuzzySearch(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "restrict", required = false) String restrict, @RequestParam(value = "enable", required = false) String enable) {
        log.info("GET:/menus/associate,/menus/associate/ref====params:keyword={},size={},restrict={},enable={}***********************************",keyword,size,restrict,enable);
        Page<MenuInfoPO> menus = iMenuInfoService.findMenusByKeyword(keyword, size, restrict, enable);
        List<MenuInfoVO> collect = menus.getRecords().stream().map(menuInfoPO -> {
            MenuInfoVO menuInfoVO = new MenuInfoVO();
            BeanUtils.copyProperties(menuInfoPO, menuInfoVO);
            setMenuInfoI18n(menuInfoVO);
            setMenuInfoModuleName(menuInfoVO);
            return menuInfoVO;
        }).collect(Collectors.toList());
//        log.info("GET:/menus/associate,/menus/associate/ref====response={}***********************************",collect);
        return new ListResult<>(collect);
    }

    /**
     * 删除菜单数据
     *
     * @param code
     */
    @DeleteMapping(value = "/menuInfo/deleteByCode")
    @ApiOperation(value = "菜单删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "code", value = "菜单code", required = true),
            @ApiImplicitParam(name = "forceDeleteMenuOperate", value = "是否强制删除菜单的所有操作，默认false ", required = false),
    })
    void deleteById(@RequestParam("code") String code, @RequestParam(value = "forceDeleteMenuOperate",required = false) boolean forceDeleteMenuOperate) {
        log.info("DELETE:/menuInfo/deleteByCode===code={},forceDeleteMenuOperate={}***********************************",code,forceDeleteMenuOperate);
        iMenuInfoService.deleteByCode(code,forceDeleteMenuOperate);
    }

    /**
     * 菜单配置页面联想查询,仅限查询运行期菜单
     * @param keyword
     * @param size
     * @param restrict
     * @param enable
     * @return
     */
    @MenuOperateCode("queryResourcesRuntime")
    @GetMapping(value = "/resources/runtime/associate")
    @ApiOperation(value = "菜单配置联想查询")
    public ListResult<MenuInfoVO> associateSearch(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "restrict", required = false) String restrict, @RequestParam(value = "enable", required = false) String enable) {
       log.info("GET:/resources/runtime/associate===params:keyword={},size={},restrict={},enable={}***********************************",keyword,size,restrict,enable);
        Page<MenuInfoPO> menus = iMenuInfoService.findMenuConfigureByKeyword(keyword, size, restrict, enable);
        List<MenuInfoVO> collect = menus.getRecords().stream().map(menuInfoPO -> {
            MenuInfoVO menuInfoVO = new MenuInfoVO();
            BeanUtils.copyProperties(menuInfoPO, menuInfoVO);
            setMenuInfoI18n(menuInfoVO);
            return menuInfoVO;
        }).collect(Collectors.toList());
//        log.info("GET:/resources/runtime/associate===response={}***********************************",collect);
        return new ListResult<>(collect);
    }

    /**
     * 菜单名转国际化
     *
     * @param menuInfoVO
     */
    private void setMenuInfoI18n(MenuInfoVO menuInfoVO) {
        Locale locale = LocaleContextHolder.getLocale();
        menuInfoVO.setNameDisplay(i18nAdapterService.getRemoteMessage(menuInfoVO.getName(), null, locale));
        List<MenuInfoVO> menuInfoVOS = menuInfoVO.getChildren();
        if (null != menuInfoVOS && !menuInfoVOS.isEmpty()) {
            for (MenuInfoVO mo : menuInfoVOS) {
                setMenuInfoI18n(mo);
            }
        }
    }

    /**
     * PO菜单名转国际化,解决数据库获取PO转VO的时Children对象还是PO问题
     *
     * @param menuInfoPO
     */
    private void setMenuInfoPOI18n(MenuInfoPO menuInfoPO) {
        Locale locale = LocaleContextHolder.getLocale();
        if (!ObjectUtils.isEmpty(i18nAdapterService.getRemoteMessage(menuInfoPO.getName(), null, locale))) {
            menuInfoPO.setNameDisplay(i18nAdapterService.getRemoteMessage(menuInfoPO.getName(), null, locale));
        }
        List<MenuInfoPO> menuInfoPOS = menuInfoPO.getChildren();
        if (null != menuInfoPOS && !menuInfoPOS.isEmpty()) {
            for (MenuInfoPO mo : menuInfoPOS) {
                setMenuInfoPOI18n(mo);
            }
        }
    }

    private void setParentInfo(MenuInfoPO menuInfoPO) {
        if (Objects.isNull(menuInfoPO)) {
            return;
        } else {
            Long parentId = menuInfoPO.getParentId();
            log.debug("parentId -> {}", parentId);
            MenuInfoPO menuInfo = iMenuInfoService.getById(parentId);
            if (Objects.nonNull(menuInfo)) {
                menuInfoPO.setParentCode(menuInfo.getCode());
            }
            List<MenuInfoPO> menuInfoPOList = menuInfoPO.getChildren();
            if (null != menuInfoPOList && !menuInfoPOList.isEmpty()) {
                for (MenuInfoPO mo : menuInfoPOList) {
                    setParentInfo(mo);
                }
            }
        }
    }

    /**
     * @description: 设置模块名
     * @param: menuInfo
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    private void setMenuInfoModuleName(List<MenuInfoVO> menuInfos, Map<String,String> module_name) {
        if (ObjectUtils.isEmpty(module_name)){
            module_name = new HashMap<>();
            Collection<ModuleDTO> moduleDTOS = iMenuInfoService.queryModules();
            if (ObjectUtils.isEmpty(moduleDTOS)) {
                return;
            }
            for (ModuleDTO moduleDTO : moduleDTOS) {
                module_name.put(moduleDTO.getModuleId(), moduleDTO.getModuleName());
            }
        }
        for (MenuInfoVO menuInfoVO : menuInfos) {
            menuInfoVO.setModuleName(module_name.get(menuInfoVO.getApp()));
            //业务模块适用范围只读
            if(!MODULE_CODE.contains(menuInfoVO.getApp())) {
            	menuInfoVO.setCompany_readOnly(true);
            }

            List<MenuInfoVO> children = menuInfoVO.getChildren();
            if (!ObjectUtils.isEmpty(children)) {
                setMenuInfoModuleName(children,module_name);
            }
        }
    }

    /**
     * 单个设置模块名
     * @param menuInfo
     */
    private void setMenuInfoModuleName(MenuInfoVO menuInfo){
        if (ObjectUtils.isEmpty(MODULE_NAME)){
            Collection<ModuleDTO> moduleDTOS = iMenuInfoService.queryModules();
            if (!ObjectUtils.isEmpty(moduleDTOS)){
                moduleDTOS.forEach(moduleDTO -> {
                    MODULE_NAME.put(moduleDTO.getModuleId(),moduleDTO.getModuleName());
                });
            }
        }
        menuInfo.setModuleName(MODULE_NAME.get(menuInfo.getApp()));
        List<MenuInfoVO> children = menuInfo.getChildren();
        if (!ObjectUtils.isEmpty(children)){
            children.forEach(this::setMenuInfoModuleName);
        }
    }


    @PostMapping("/menus/check")
    @ApiOperation(value = "通过URL校验菜单是否存在")
    public Result<JSONObject> checkMenu(@RequestBody MenuInfoBaseVO menuInfoBaseVO) {
        JSONObject jsonObject = iMenuInfoService.checkMenu(menuInfoBaseVO.getUrl());
        return new Result<>(jsonObject);
    }

    @GetMapping(value = "/menus/runtime")
    @ApiOperation(value = "获取运行期菜单")
    ListResult<MenuInfoRuntimeVO> queryRuntimeMenus(@RequestParam(value = "appId", required = false) String appId) {
        log.info("GET:/menus/runtime=====appId={}***********************************",appId);
        List<MenuInfoPO> menuInfoPOList = iMenuInfoService.queryRuntimeMenus(appId);
//        log.info("GET:/menus/runtime=====response={}***********************************",menuInfoMenuInfoRuntimeVO(menuInfoPOList));
        return new ListResult<>(menuInfoMenuInfoRuntimeVO(menuInfoPOList));
    }

    private List<MenuInfoRuntimeVO> menuInfoMenuInfoRuntimeVO(List<MenuInfoPO> menuInfoPOS) {
        if (!ObjectUtils.isEmpty(menuInfoPOS)) {
            List<MenuInfoRuntimeVO> menuInfoRuntimeVOList = new ArrayList<>();
            for (MenuInfoPO menuInfoPO : menuInfoPOS) {
                if ("personalInfo".equals(menuInfoPO.getCode())) {
                    continue;
                }
                MenuInfoRuntimeVO menuInfoRuntimeVO = new MenuInfoRuntimeVO();
                menuInfoRuntimeVO.setName(menuInfoPO.getNameDisplay());
                menuInfoRuntimeVO.setResourceId(String.valueOf(menuInfoPO.getId()));
                menuInfoRuntimeVO.setResourceCode(menuInfoPO.getCode());
                menuInfoRuntimeVO.setResourceOrder(menuInfoPO.getSort());
                menuInfoRuntimeVO.setParentId(String.valueOf(menuInfoPO.getParentId()));
                menuInfoRuntimeVO.setParentCode(menuInfoPO.getParentCode());
                menuInfoRuntimeVO.setResourceType(StringUtils.isEmpty(menuInfoPO.getSource()) ? "app" : menuInfoPO.getSource());
                menuInfoRuntimeVO.setResourceFunctionType(getType(menuInfoPO));
                menuInfoRuntimeVO.setUrl(menuInfoPO.getUrl());
                menuInfoRuntimeVO.setChildResources(menuInfoMenuInfoRuntimeVO(menuInfoPO.getChildren()));
                menuInfoRuntimeVOList.add(menuInfoRuntimeVO);
            }
            return menuInfoRuntimeVOList;
        }
        return new ArrayList<>();
    }

    /**
     * 临时转换获取菜单类型
     * @param menuInfoPO
     * @return
     */
    private String getType(MenuInfoPO menuInfoPO) {
        String type = null;
        if (Objects.nonNull(menuInfoPO.getMenuType()) && menuInfoPO.getMenuType().equals(4)) {
            type = "app";
        } else if (Objects.nonNull(menuInfoPO.getMenuType()) && menuInfoPO.getMenuType().equals(0)) {
            type = "folder";
        } else if (Objects.nonNull(menuInfoPO.getMenuType()) && menuInfoPO.getMenuType().equals(1)) {
            type = "page";
        }
        if (StringUtils.isEmpty(type)) {
            if (StringUtils.isBlank(menuInfoPO.getUrl())) {
                type = "folder";
            } else {
                type = "page";
            }
        }
        return type;
    }

    /**
     * 获取业务模块
     *
     * @return
     */
    private List<String> getSystemModule() {
        Collection<ModuleDTO> moduleDtos  = moduleRegistryApi.queryModules();
        List<String> modules = new LinkedList<String>();
        for (ModuleDTO moduleDto : moduleDtos) {
        	if("SYSTEM".equals(moduleDto.getModuleType())) {
        		modules.add(moduleDto.getModuleId());
        	}
        }
        return modules;
    }

    @GetMapping(value = {"/app_menus"})
    @ApiOperation(value = "APP菜单列表查询")
    ListResult<MenuInfoVO> app_menus(@RequestParam(value = "filter_sys",required = false) Boolean filter_sys) {
        List<MenuInfoPO> menuInfoPOList = iMenuInfoService.queryMenus(true, true);
        List<MenuInfoVO> menuInfoVOList = JSONArray.parseArray(JSON.toJSONString(menuInfoPOList), MenuInfoVO.class);
        if (filter_sys != null && !filter_sys) {
            return new ListResult<>(menuInfoVOList.stream().collect(Collectors.toList()));
        }
        List sys_menus = Arrays.asList(MenuInfoField.sys_menus);
        return new ListResult<>(menuInfoVOList.stream().filter(menuInfoVO ->
                !sys_menus.contains(menuInfoVO.getApp())
        ).collect(Collectors.toList()));
    }
}

