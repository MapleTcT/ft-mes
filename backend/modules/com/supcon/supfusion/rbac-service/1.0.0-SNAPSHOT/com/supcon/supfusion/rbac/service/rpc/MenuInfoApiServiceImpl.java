package com.supcon.supfusion.rbac.service.rpc;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import com.supcon.supfusion.rbac.api.dto.*;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.MenuErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuException;
import com.supcon.supfusion.rbac.common.exception.MenuOperateErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuOperateException;
import com.supcon.supfusion.rbac.dao.AppRefMapper;
import com.supcon.supfusion.rbac.dao.MenuInfoMapper;
import com.supcon.supfusion.rbac.dao.field.AppCompanyRefField;
import com.supcon.supfusion.rbac.dao.field.AppRefField;
import com.supcon.supfusion.rbac.dao.field.MenuAppDesignerRelField;
import com.supcon.supfusion.rbac.dao.field.MenuInfoField;
import com.supcon.supfusion.rbac.dao.field.MenuOperateCodeUrlRefField;
import com.supcon.supfusion.rbac.dao.field.MenuOperateField;
import com.supcon.supfusion.rbac.dao.field.MenuTempField;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import com.supcon.supfusion.rbac.service.*;
import com.supcon.supfusion.rbac.service.config.LaunchInitialize;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@ServiceApiService
@Transactional
@Slf4j
public class MenuInfoApiServiceImpl implements IMenuInfoApiService {

    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private IMenuInfoCompanyRefService menuInfoCompanyRefService;
    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private IMenuOperateCodeUrlRefService menuOperateCodeUrlRefService;
    @Autowired
    private IMenuInfoMneCodeService menuInfoMneCodeService;
    @Autowired
    private IMenuTempService menuTempService;
    @Autowired
    II18nAdapter i18nAdapterService;
    @Autowired
    MessageResourceService messageResourceService;
    @Value("${spring.application.name}")
    private String moduleName = "rbac";
    @Autowired
    private IAppCompanyRefService appCompanyRefService;
    @Autowired
    private IAppRefService appRefService;
    @Autowired
    private MenuInfoMapper menuInfoMapper;
    @Autowired
    private AppRefMapper appRefMapper;
    @Autowired
    private IMenuAppDesignerRelService menuAppDesignerRelService;
    @Autowired
    private LaunchInitialize launchInitialize;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Override
    public void saveBachUrlByJson(String json) {
        menuInfoService.saveBachUrlByJson(json);
    }

    @Override
    @Transactional
    public String saveBachUrl(List<MenuInfoJsonDTO> menuInfoJsonDTOS) {
        if (ObjectUtils.isEmpty(menuInfoJsonDTOS)) {
            return null;
        }
       /* //寻找老数据，回滚需要
        List<String> codes = menuInfoJsonDTOS.stream().map(MenuInfoJsonDTO::getCode).collect(Collectors.toList());
        List<MenuInfoPO> old_menu_data = menuInfoService.list(new QueryWrapper<MenuInfoPO>().in(MenuInfoField.code, codes));
        List<MenuOperatePO> old_operate_data = new ArrayList<>();
        List<MenuOperateCodeUrlRefPO> old_operate_url_data = new ArrayList<>();
        if (!ObjectUtils.isEmpty(old_menu_data)) {
            List<Long> menuIds = old_menu_data.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
            old_operate_data = menuOperateService.list(new QueryWrapper<MenuOperatePO>().in(MenuOperateField.menuinfoId, menuIds));
            if (!ObjectUtils.isEmpty(old_operate_data)) {
                List<String> operateCodes = old_operate_data.stream().map(MenuOperatePO::getCode).collect(Collectors.toList());
                old_operate_url_data = menuOperateCodeUrlRefService.list(new QueryWrapper<MenuOperateCodeUrlRefPO>().in(MenuOperateCodeUrlRefField.menuoperateCode, operateCodes));
            }
        }
        //保存老数据,用于回滚
        String uuid = createTemp(old_menu_data, old_operate_data, old_operate_url_data, menuInfoJsonDTOS);*/

        //新增修改时先更新下国际化资源
        i18nAdapterService.refreshI18n();


        List<MenuInfoPO> menuInfoPOS = menuInfoJsonDTOS.stream().map(menuInfoJsonDTO -> {
            //新增的菜单ROUTE字段后台直接填，把URL的?,=,#号换成/，url参数不同的前端会认为是同一个url，不进行跳转
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getUrl()) && ObjectUtils.isEmpty(menuInfoJsonDTO.getRoute())) {
                menuInfoJsonDTO.setRoute(menuInfoJsonDTO.getUrl());
            }
            if (menuInfoJsonDTO.getCode().equals(Constants.MENU_LIST)) {
                return null;
            }
            MenuInfoPO menuInfoPO = new MenuInfoPO();
            BeanUtils.copyProperties(menuInfoJsonDTO, menuInfoPO);
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getCode())) {
                MenuInfoPO dbMenuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", menuInfoJsonDTO.getCode()));
                //如果该菜单在页面上修改过，就不进行更新，直接返回数据库里的数据
                if (!ObjectUtils.isEmpty(dbMenuInfoPO) && !ObjectUtils.isEmpty(dbMenuInfoPO.getEdited()) && dbMenuInfoPO.getEdited()) {
                    return dbMenuInfoPO;
                }
                if (!ObjectUtils.isEmpty(dbMenuInfoPO)) {
                    menuInfoPO.setId(dbMenuInfoPO.getId());
                } else {
                    menuInfoPO.setId(IDGenerator.newInstance().generate().longValue());
                }
            } else {
                throw new MenuException(MenuErrorEnum.CODE_CAN_NOT_EMPTY);
            }
            MenuInfoPO parentMenu = null;
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getParentCode())) {
                parentMenu = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", menuInfoJsonDTO.getParentCode()));
            } else {
                parentMenu = new MenuInfoPO();
                parentMenu.setId(-1L);
            }
            if (ObjectUtils.isEmpty(parentMenu)) {
                parentMenu = getParentMenuInfo(menuInfoJsonDTOS, menuInfoJsonDTO.getParentCode());
                if (ObjectUtils.isEmpty(parentMenu)) {
                    throw new MenuException(MenuErrorEnum.PARENT_CON_NOT_FIND);
                }
            }
            menuInfoPO.setParentId(parentMenu.getId());

            /**
             *将app字段的值赋给appId, app字段赋值为模块编码
             *目的是解决菜单表 APP字段 技术公司存储的是模块编码，我们存储的是appid 所产生的不兼容问题
             */
            if (!StringUtils.isEmpty(menuInfoPO.getApp())) {
                menuInfoPO.setAppId(menuInfoPO.getApp());
                menuInfoPO.setApp(null == menuInfoPO.getModuleCode() ? "rbac" : menuInfoPO.getModuleCode());
            } else {
                menuInfoPO.setApp(null == menuInfoPO.getModuleCode() ? "rbac" : menuInfoPO.getModuleCode());
            }
            setFullPath(menuInfoPO);
            //设置助记码
            menuInfoMneCodeService.createMenuInfoMneCodeI18NKey(menuInfoPO.getName(), menuInfoPO.getId());
            menuInfoService.saveOrUpdate(menuInfoPO);
            return menuInfoPO;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        //设置排序
        setSort(menuInfoPOS);
        log.info("batchSaveMenuInfo=========menuInfoPOS======>{}", menuInfoPOS);
        menuInfoService.saveOrUpdateBatch(menuInfoPOS);
        for (int i = 0; i < menuInfoJsonDTOS.size(); i++) {
            saveBachOperate(menuInfoJsonDTOS.get(i).getMenuOperates(), menuInfoPOS.get(i).getId());
        }
        //创建默认操作
        if (!ObjectUtils.isEmpty(menuInfoPOS)) {
            menuInfoService.generateDefaultOperate(menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList()));
        }
        //创建公司关联
        saveMenuInfoCompanyRef(menuInfoPOS);
        //创建菜单 app关联
        saveMenuAppRef(menuInfoPOS);
        saveAppCompanyRef(menuInfoPOS);
        //菜单、操作、URL初始化后 将所有需要匹配的URL加入redis
        menuOperateCodeUrlRefService.updateUrl(null);
        // 保存app设计器资源与菜单联系 rbac_menu_app_designer
        saveMenuAppDesigner(menuInfoJsonDTOS);
        /**
         *  兼容升级功能,对再次安装减少的菜单进行处理
         */
        /*
        // 获取当前安装的菜单id
        List<Long> currentVersionMenuIds = menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
        // 获取appid
        Set<String> appIds = menuInfoPOS.stream().map(MenuInfoPO::getAppId).collect(Collectors.toSet());
        List<MenuInfoPO> menuInfoPOList = new ArrayList<>();
        for (String appId : appIds) {
            QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(MenuInfoField.appId, appId);
            menuInfoPOList = menuInfoService.list(queryWrapper);
            log.info("appId is.......{}", appId);
        }
        // 上个版本的menuIds
        List<Long> lastVersionMenuIds = menuInfoPOList.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
        List<Long> reduceMenuIds = CollectionUtils.subtract(lastVersionMenuIds, currentVersionMenuIds).stream().collect(Collectors.toList());
        */


        // 获取原始安装的菜单id

        return null;
    }

    private void saveMenuAppDesigner(List<MenuInfoJsonDTO> menuInfoJsonDTOS) {
        /**
         * 维护app设计器中,oodm数据与菜单的映射关系
         * 无法区分哪些是设计器菜单,因此只过滤掉installer数据
         */
        List<MenuAppDesignerRelPO> list = menuInfoJsonDTOS.stream().filter(menuInfoDTO -> menuInfoDTO.getStatus() == 1 && null != menuInfoDTO.getSource() && menuInfoDTO.getSource().equals("app")).map(menuInfoDTO -> {
            menuInfoDTO.setAppId(menuInfoDTO.getApp());

            QueryWrapper<MenuAppDesignerRelPO> appDesignerWrapper = new QueryWrapper<>();
            appDesignerWrapper.eq("code", menuInfoDTO.getCode());
            appDesignerWrapper.eq("appid", menuInfoDTO.getAppId());
            MenuAppDesignerRelPO dbAppDesignerRelPO = menuAppDesignerRelService.getOne(appDesignerWrapper);
//
            MenuAppDesignerRelPO menuAppDesignerRelPO = new MenuAppDesignerRelPO();
            if (null == dbAppDesignerRelPO) {
                menuAppDesignerRelPO.setCode(menuInfoDTO.getCode());
                menuAppDesignerRelPO.setAppId(menuInfoDTO.getAppId());
                menuAppDesignerRelPO.setParentCode(menuInfoDTO.getParentCode());
                return menuAppDesignerRelPO;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        menuAppDesignerRelService.saveBatch(list);
    }

    // 菜单为app类型的插入关联关系
    private void saveMenuAppRef(List<MenuInfoPO> menuInfoPOS) {
        List<MenuInfoPO> filterMenus = menuInfoPOS.stream().filter(menuInfoPO -> !ObjectUtils.isEmpty(menuInfoPO.getStatus()) && menuInfoPO.getStatus() == 1).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(filterMenus)) {
            return;
        }
        List<AppRefPO> AppRefPOS = menuInfoPOS.stream().filter(menuInfoPO -> menuInfoPO.getStatus() == 1).map(menuInfoPO -> {
            QueryWrapper<AppRefPO> getAppRefPOByAppidWapper = new QueryWrapper<>();
            getAppRefPOByAppidWapper.eq(AppRefField.appId, menuInfoPO.getAppId());
            getAppRefPOByAppidWapper.eq(AppRefField.menuId, menuInfoPO.getId());
            AppRefPO dbAppRefPO = appRefService.getOne(getAppRefPOByAppidWapper);

            AppRefPO appRefPO = new AppRefPO();
            if (null == dbAppRefPO && null != menuInfoPO.getId() && null != menuInfoPO.getAppId()) {
                appRefPO.setMenuId(menuInfoPO.getId());
                appRefPO.setAppId(menuInfoPO.getAppId());
                return appRefPO;
            } else {
                return null;
            }

        }).filter(Objects::nonNull).collect(Collectors.toList());
        appRefService.saveBatch(AppRefPOS);
    }

    // 菜单为app类型的插入关联关系 menuType为4表示app，flag为1表示本地安装的APP
    private void saveAppCompanyRef(List<MenuInfoPO> menuInfoPOS) {
        List<AppCompanyRefPO> appCompanyRefPOS = menuInfoPOS.stream().filter(menuInfoPO -> !ObjectUtils.isEmpty(menuInfoPO.getMenuType()) && menuInfoPO.getMenuType() == 4 && !ObjectUtils.isEmpty(menuInfoPO.getStatus()) && menuInfoPO.getStatus() == 1 && null != menuInfoPO.getFlag() && menuInfoPO.getFlag() == 1).map(menuInfoPO -> {
            QueryWrapper<AppCompanyRefPO> getAppCompanyRefPOByAppid = new QueryWrapper<>();
            getAppCompanyRefPOByAppid.eq(AppCompanyRefField.appId, menuInfoPO.getAppId());
            getAppCompanyRefPOByAppid.eq(AppCompanyRefField.cid, -1L);
            AppCompanyRefPO dbAppCompanyRefPO = appCompanyRefService.getOne(getAppCompanyRefPOByAppid);
            AppCompanyRefPO appCompanyRefPO = new AppCompanyRefPO();
            if (null == dbAppCompanyRefPO) {
                appCompanyRefPO.setCid(-1L);
                appCompanyRefPO.setAppId(menuInfoPO.getAppId());
                return appCompanyRefPO;
            } else {
                return null;
            }

        }).filter(Objects::nonNull).collect(Collectors.toList());
        appCompanyRefService.saveBatch(appCompanyRefPOS);
    }

    private void setSort(List<MenuInfoPO> menuInfoPOS) {
        if (ObjectUtils.isEmpty(menuInfoPOS)) {
            return;
        }
        Map<Long, List<MenuInfoPO>> map = menuInfoPOS.stream().collect(Collectors.groupingBy(MenuInfoPO::getParentId));
        List<MenuInfoPO> parentMenuInfos = menuInfoService.list(new QueryWrapper<MenuInfoPO>().in(MenuInfoField.parentId, map.keySet()));
        map.forEach((parentId, list) -> {
            //把该父菜单下的子菜单查出来一起进行排序，序号为0或为null的才进行重新赋值
            List<MenuInfoPO> children = parentMenuInfos.stream().filter(menuInfoPO -> menuInfoPO.getParentId().equals(parentId)).collect(Collectors.toList());
            list.addAll(children);
            //把序号为0的设置为null，免得排序的时候一直在最前面
            list.forEach(menuInfoPO -> {
                if (!ObjectUtils.isEmpty(menuInfoPO.getSort()) && 0 == menuInfoPO.getSort()) {
                    menuInfoPO.setSort(null);
                }
            });
            //对列表进行排序，null的排在最后
            list.sort(Comparator.comparing(MenuInfoPO::getSort, Comparator.nullsLast(Double::compareTo)));
            for (int i = 0; i < list.size(); i++) {
                MenuInfoPO menuInfoPO = list.get(i);
                if (ObjectUtils.isEmpty(menuInfoPO.getSort()) || menuInfoPO.getSort() == 0) {
                    if (i == 0) {
                        menuInfoPO.setSort(i + 1.0);
                    } else {
                        menuInfoPO.setSort(list.get(i - 1).getSort() + 1.0);
                    }
                }
            }
        });
    }

    private String createTemp(List<MenuInfoPO> menuInfoPOS, List<MenuOperatePO> menuOperatePOS, List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS, List<MenuInfoJsonDTO> newData) {
        String uuid = UUID.randomUUID().toString();
        //构造DTO结构
        List<MenuInfoJsonDTO> menuInfoJsonDTOS = JSON.parseArray(JSON.toJSONString(menuInfoPOS), MenuInfoJsonDTO.class);
        List<MenuOperateJsonDTO> menuOperateJsonDTOS = JSON.parseArray(JSON.toJSONString(menuOperatePOS), MenuOperateJsonDTO.class);
        List<MenuOperateUrlJsonDTO> menuOperateUrlJsonDTOS = JSON.parseArray(JSON.toJSONString(menuOperateCodeUrlRefPOS), MenuOperateUrlJsonDTO.class);
        menuInfoJsonDTOS.forEach(menuInfoJsonDTO -> {
            List<MenuOperateJsonDTO> childOperates = menuOperateJsonDTOS.stream().filter(menuOperateJsonDTO -> menuOperateJsonDTO.getMenuinfoId().equals(menuInfoJsonDTO.getId())).collect(Collectors.toList());
            menuInfoJsonDTO.setMenuOperates(childOperates);
        });
        menuOperateJsonDTOS.forEach(menuOperateJsonDTO -> {
            List<MenuOperateUrlJsonDTO> urlJsonDTOS = menuOperateUrlJsonDTOS.stream().filter(menuOperateUrlJsonDTO -> menuOperateUrlJsonDTO.getMenuoperateCode().equals(menuOperateJsonDTO.getCode())).collect(Collectors.toList());
            menuOperateJsonDTO.setUrls(urlJsonDTOS);
        });
        MenuTempPO menuTempPO = new MenuTempPO();
        menuTempPO.setNewData(JSON.toJSONString(newData));
        menuTempPO.setOldData(JSON.toJSONString(menuInfoJsonDTOS));
        menuTempPO.setUuid(uuid);
        menuTempService.save(menuTempPO);
        return uuid;
    }

    private void saveMenuInfoCompanyRef(List<MenuInfoPO> menuInfoPOS) {
        List<MenuInfoCompanyRefPO> menuInfoCompanyRefPOS = menuInfoPOS.stream().map(menuInfoPO -> {
            log.info("saveMenuInfoCompanyRef----getCompanyId={}", menuInfoPO.getCid());
            MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
            // 本地安装，lcdp 等app
            if (null != menuInfoPO.getFlag() && menuInfoPO.getFlag() == 1) {
                int count = menuInfoCompanyRefService.count(new QueryWrapper<MenuInfoCompanyRefPO>().eq("COMPANY_ID", -1L).eq("MENUINFO_ID", menuInfoPO.getId()));
                if (count == 0) {
                    menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
                    menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
                    menuInfoCompanyRefPO.setCompanyId(-1L);
                    menuInfoCompanyRefPO.setAppId(menuInfoPO.getAppId());
                }
                return menuInfoCompanyRefPO;
            } else {
                int count = menuInfoCompanyRefService.count(new QueryWrapper<MenuInfoCompanyRefPO>().eq("COMPANY_ID", menuInfoPO.getCid()).eq("MENUINFO_ID", menuInfoPO.getId()));
                if (count == 0) {
                    menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
                    menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
                    menuInfoCompanyRefPO.setCompanyId(menuInfoPO.getCid());
                    menuInfoCompanyRefPO.setAppId(menuInfoPO.getAppId());
                }
                return menuInfoCompanyRefPO;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        menuInfoCompanyRefService.saveBatch(menuInfoCompanyRefPOS);
    }

    @Transactional
    private void saveBachOperate(List<MenuOperateJsonDTO> menuOperateJsonDTOS, Long menuInfoId) {
        if (!ObjectUtils.isEmpty(menuOperateJsonDTOS)) {
            Map<String, String> operI18nResourceMap = new HashMap<>();
            List<MenuOperatePO> menuOperatePOS = menuOperateJsonDTOS.stream().map(menuOperateJsonDTO -> {
                MenuOperatePO menuOperatePO = new MenuOperatePO();
                BeanUtils.copyProperties(menuOperateJsonDTO, menuOperatePO);
                if (!ObjectUtils.isEmpty(menuOperateJsonDTO.getCode())) {
                    if (null == menuOperateJsonDTO.getFlowVersion()) {
                        menuOperatePO.setFlowVersion("0");
                    }
                    // bap operation存在脏数据 此处特殊处理
                    // 根据code查出的操作数据>1直接跳过，=1则更新
                    List<MenuOperatePO> menuOperatePOList = menuOperateService.list(new QueryWrapper<MenuOperatePO>().eq("CODE", menuOperatePO.getCode()));
                    if (menuOperatePOList.size() > 1) {
                        log.info("get operatetion by code==========menuOperatePO.getCode={}====menuOperatePOList==={}", menuOperatePO.getCode(), menuOperatePOList);
                        return new MenuOperatePO();
                    } else if (menuOperatePOList.size() == 1) {
                        menuOperatePO.setId(menuOperatePOList.get(0).getId());
                    }
                } else {
                    throw new MenuOperateException(MenuOperateErrorEnum.OPERATE_CODE_EMPTY);
                }
                menuOperatePO.setMenuinfoId(menuInfoId);

                // 国际化处理
                String name = menuOperatePO.getName();
                if (!StringUtils.isEmpty(name) && name.startsWith("I18N(") && name.endsWith(")")) {
                    menuOperatePO.setName(name.substring(name.indexOf("(") + 1, name.indexOf(")")));
                } else {
                    menuOperatePO.setNameDisplay(menuOperatePO.getName());
                    /**
                     * 操作国际化处理
                     */
                    final String menuI18nKey = seti18nKeyForOper(menuOperatePO);
                    // 国际化资源key， value
                    operI18nResourceMap.put(menuI18nKey, menuOperatePO.getNameDisplay());
                }
                return menuOperatePO;

            }).collect(Collectors.toList());
            log.info("saveBatchOperate=================>{}", menuOperatePOS);
            if (!CollectionUtils.isEmpty(menuOperatePOS)) {
                menuOperateService.saveOrUpdateBatch(menuOperatePOS);
            }
            // 批量新增国际化资源
            String language = RpcContext.getContext().getLanguage().toString();
            log.info("batchSaveMenu getLanguage from RpcContext:language==={}", language);
            if (null != operI18nResourceMap && operI18nResourceMap.size() > 0) {
                messageResourceService.messageResourceAddOrUpdateList(operI18nResourceMap, moduleName, language);
            }
            for (int i = 0; i < menuOperateJsonDTOS.size(); i++) {
                saveBatchUrl(menuOperateJsonDTOS.get(i).getUrls(), menuOperatePOS.get(i).getId());
            }
        }
    }

    private String seti18nKeyForOper(MenuOperatePO menuOperatePO) {
        /**
         * 为菜单创建国际化的key
         * todo
         */
        //获取本地模块名
        String menuI18nKey = getMenuI18nKey(moduleName);
        menuOperatePO.setName(menuI18nKey);
        return menuI18nKey;
    }

    private void saveBatchUrl(List<MenuOperateUrlJsonDTO> menuOperateCodeUrlRefDTOS, Long menuOperateId) {
        if (!ObjectUtils.isEmpty(menuOperateCodeUrlRefDTOS)) {
            List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS = menuOperateCodeUrlRefDTOS.stream().map(menuOperateCodeUrlRefDTO -> {
                MenuOperateCodeUrlRefPO menuOperateCodeUrlRefPO = new MenuOperateCodeUrlRefPO();
                BeanUtils.copyProperties(menuOperateCodeUrlRefDTO, menuOperateCodeUrlRefPO);
                if (ObjectUtils.isEmpty(menuOperateCodeUrlRefPO.getUrl())) {
                    throw new MenuOperateException(MenuOperateErrorEnum.URL_CONT_NOT_EMPTY);
                }
                if (ObjectUtils.isEmpty(menuOperateCodeUrlRefPO.getMethodType())) {
                    throw new MenuOperateException(MenuOperateErrorEnum.METHOD_TYPE_CONT_NOT_EMPTY);
                }
                menuOperateCodeUrlRefPO.setUrl(menuInfoService.filterPathParams(menuOperateCodeUrlRefPO.getUrl()));
                MenuOperateCodeUrlRefPO codeUrlRefServiceOne = menuOperateCodeUrlRefService.getOne(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq("URL", menuOperateCodeUrlRefPO.getUrl()).eq("MENUOPERATE_CODE", menuOperateCodeUrlRefPO.getMenuoperateCode()).eq("METHOD_TYPE", menuOperateCodeUrlRefPO.getMethodType()));
                menuOperateCodeUrlRefPO.setRegMatch(menuOperateCodeUrlRefPO.getUrl().contains("/[^/^]"));
                if (!ObjectUtils.isEmpty(codeUrlRefServiceOne)) {
                    if (!ObjectUtils.isEmpty(codeUrlRefServiceOne)) {
                        menuOperateCodeUrlRefPO.setId(codeUrlRefServiceOne.getId());
                    }
                }
                return menuOperateCodeUrlRefPO;
            }).collect(Collectors.toList());
            menuOperateCodeUrlRefService.saveOrUpdateBatch(menuOperateCodeUrlRefPOS);
        }
    }

    private MenuInfoPO getParentMenuInfo(List<MenuInfoJsonDTO> menuInfoJsonDTOS, String parentCode) {
        Optional<MenuInfoJsonDTO> first = menuInfoJsonDTOS.stream().filter(menuInfoJsonDTO -> menuInfoJsonDTO.getCode().equals(parentCode)).findFirst();
        if (first.isPresent()) {
            MenuInfoJsonDTO menuInfoJsonDTO = first.get();
            MenuInfoPO menuInfoPO = new MenuInfoPO();
            BeanUtils.copyProperties(menuInfoJsonDTO, menuInfoPO);
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getCode())) {
                MenuInfoPO dbMenuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", menuInfoJsonDTO.getCode()));
                if (!ObjectUtils.isEmpty(dbMenuInfoPO)) {
                    menuInfoPO.setId(dbMenuInfoPO.getId());
                } else {
                    menuInfoPO.setId(IDGenerator.newInstance().generate().longValue());
                }
            } else {
                throw new MenuException(MenuErrorEnum.CODE_CAN_NOT_EMPTY);
            }
            if (!ObjectUtils.isEmpty(menuInfoPO.getEdited()) && menuInfoPO.getEdited()) {
                return null;
            }
            MenuInfoPO parentMenu;
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getParentCode()) && menuInfoPO.getId() != -1L) {
                parentMenu = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", menuInfoJsonDTO.getParentCode()));
            } else {
                parentMenu = new MenuInfoPO();
                parentMenu.setId(-1L);
            }
            if (ObjectUtils.isEmpty(parentMenu)) {
                parentMenu = getParentMenuInfo(menuInfoJsonDTOS, menuInfoJsonDTO.getParentCode());
                if (ObjectUtils.isEmpty(parentMenu)) {
                    log.info("menuInfoJsonDTO.getParentCode()================224=================> {}", menuInfoJsonDTO.getParentCode());
                    throw new MenuException(MenuErrorEnum.PARENT_CON_NOT_FIND);
                }
            }
            menuInfoPO.setParentId(parentMenu.getId());
            //设置助记码
            menuInfoMneCodeService.createMenuInfoMneCodeI18NKey(menuInfoPO.getName(), menuInfoPO.getId());
            setFullPath(menuInfoPO);
            menuInfoService.saveMenuInfo(menuInfoPO);
            return menuInfoPO;
        }
        return null;
    }

    @Override
    public MenuInfoDTO getMenuInfoByCode(String code) {

        MenuInfoPO menuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", code));
        MenuInfoDTO menuInfoDTO = new MenuInfoDTO();
        if (!ObjectUtils.isEmpty(menuInfoPO)) {
            BeanUtils.copyProperties(menuInfoPO, menuInfoDTO);
            return menuInfoDTO;
        }
        return null;
    }

    @Override
    public MenuInfoDTO getMenuInfoById(Long id) {

        MenuInfoPO menuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("ID", id));
        MenuInfoDTO menuInfoDTO = new MenuInfoDTO();
        if (!ObjectUtils.isEmpty(menuInfoPO)) {
            BeanUtils.copyProperties(menuInfoPO, menuInfoDTO);
            return menuInfoDTO;
        }
        return null;
    }

    @Override
    public MenuInfoDTO save(MenuInfoDTO menuInfoDTO) {

        MenuInfoPO menuInfoPO = new MenuInfoPO();
        BeanUtils.copyProperties(menuInfoDTO, menuInfoPO);
        if (ObjectUtils.isEmpty(menuInfoPO.getCode())) {
            throw new MenuException(MenuErrorEnum.CODE_CAN_NOT_EMPTY);
        }
        MenuInfoPO infoPOS = null;
        if (!ObjectUtils.isEmpty(menuInfoDTO.getCode())) {
            infoPOS = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", menuInfoPO.getCode()));
        } else if (!ObjectUtils.isEmpty(menuInfoDTO.getId())) {
            infoPOS = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("ID", menuInfoPO.getId()));
        }
        if (!ObjectUtils.isEmpty(infoPOS)) {
            menuInfoPO.setId(infoPOS.getId());
        } else {
            menuInfoPO.setId(IDGenerator.newInstance().generate().longValue());
        }
        //设置助记码
        menuInfoMneCodeService.createMenuInfoMneCodeI18NKey(menuInfoPO.getName(), menuInfoPO.getId());
        setFullPath(menuInfoPO);
        menuInfoService.saveMenuInfo(menuInfoPO);
        if (ObjectUtils.isEmpty(infoPOS) && menuInfoPO.getId() != -1) {
            //创建默认操作
            MenuOperatePO menuOperatePO = new MenuOperatePO();
            menuOperatePO.setCode(menuInfoPO.getCode() + "_default");
            menuOperatePO.setDefaultOperate(true);
            menuOperatePO.setMenuinfoId(menuInfoPO.getId());
            menuOperatePO.setUrl(menuInfoPO.getUrl());
            menuOperatePO.setCid(menuInfoPO.getCid());
            menuOperatePO.setEnableNorestrict(true);
            menuOperatePO.setName("rbac.MENU_OPERATE_DEFAULT_OPERATE");
            menuOperatePO.setVersion(0);
            menuOperateService.save(menuOperatePO);
        }
        MenuInfoDTO menuInfoDTO1 = new MenuInfoDTO();
        BeanUtils.copyProperties(menuInfoPO, menuInfoDTO1);
        return menuInfoDTO1;
    }

    private void setFullPath(MenuInfoPO menuInfoPO) {
        if (!ObjectUtils.isEmpty(menuInfoPO.getId()) && menuInfoPO.getId() != -1) {
            MenuInfoPO parentMenuInfo = new MenuInfoPO();
            if (!ObjectUtils.isEmpty(menuInfoPO.getParentId())) {
                parentMenuInfo = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("ID", menuInfoPO.getParentId()));
            }
            String parentFullPath = !ObjectUtils.isEmpty(parentMenuInfo.getFullPath()) ? parentMenuInfo.getFullPath() + HttpConstants.URL_SPLITER : "";
            String parentLayRec = !ObjectUtils.isEmpty(parentMenuInfo.getLayRec()) ? parentMenuInfo.getLayRec() + Constants.LAY_REC_SPLIT : "";
            String parentFullPathName = !ObjectUtils.isEmpty(parentMenuInfo.getFullPathName()) ? parentMenuInfo.getFullPathName() + HttpConstants.URL_SPLITER : "";
            //不拼接 -1 菜单的ID
            if (menuInfoPO.getParentId().equals(Constants.MENU_LIST_ID)) {
                menuInfoPO.setFullPath(String.valueOf(menuInfoPO.getId()));
                menuInfoPO.setLayRec(String.valueOf(menuInfoPO.getId()));
                menuInfoPO.setFullPathName(menuInfoPO.getName());
            } else {
                menuInfoPO.setFullPath(parentFullPath + menuInfoPO.getId());
                menuInfoPO.setLayRec(parentLayRec + menuInfoPO.getId());
                menuInfoPO.setFullPathName(parentFullPathName + menuInfoPO.getName());
            }
            menuInfoPO.setLayNo(!ObjectUtils.isEmpty(parentMenuInfo.getLayNo()) ? parentMenuInfo.getLayNo() + 1 : 1);
            String code = menuInfoPO.getModuleCode();
            String app = menuInfoPO.getApp();
            if (!ObjectUtils.isEmpty(code) && ObjectUtils.isEmpty(app)) {
                menuInfoPO.setApp(code.split("_")[0]);
            }
        }
    }

    @Override
    public List<MenuInfoDTO> getMenuInfoAsc() {
        List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().orderBy(true, true, "SORT"));

        return menuInfoPOS.stream().map(menuInfoPO -> {
            MenuInfoDTO menuInfoDTO = new MenuInfoDTO();
            BeanUtils.copyProperties(menuInfoPO, menuInfoDTO);
            return menuInfoDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MenuInfoDTO> getMenuInfoByModuleCode(String moduleCode) {
        List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().like("MODULE_CODE", moduleCode));
        if (!ObjectUtils.isEmpty(menuInfoPOS)) {
            return menuInfoPOS.stream().map(menuInfoPO -> {
                MenuInfoDTO menuInfoDTO = new MenuInfoDTO();
                BeanUtils.copyProperties(menuInfoPO, menuInfoDTO);
                return menuInfoDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<MenuInfoDTO> getMenuInfoByEntityCode(String entityCode) {
        List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().eq("ENTITY_CODE", entityCode));
        if (!ObjectUtils.isEmpty(menuInfoPOS)) {
            return menuInfoPOS.stream().map(menuInfoPO -> {
                MenuInfoDTO menuInfoDTO = new MenuInfoDTO();
                BeanUtils.copyProperties(menuInfoPO, menuInfoDTO);
                return menuInfoDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void deleteMenuInfoByIds(List<Long> ids) {
        List<MenuInfoPO> menuInfoPOS = menuInfoService.listByIds(ids);
        menuInfoService.cascadeDeleteMenuById(menuInfoPOS);
    }

    @Override
    public void updateMenuInfoByEntityCode(String entityCode) {
        menuInfoService.updateMenuInfoByEntityCode(entityCode);
    }

    @Override
    public void deleteMenuInfoByEntityCode(String entityCode) {
        menuInfoService.removeMenuInfo(new QueryWrapper<MenuInfoPO>().eq("ENTITY_CODE", entityCode));
    }

    @Override
    public Result<Boolean> deleteCompanyRef(Long cid) {
        menuInfoCompanyRefService.remove(new QueryWrapper<MenuInfoCompanyRefPO>().eq("COMPANY_ID", cid));
        menuInfoService.removeMenuInfo(new QueryWrapper<MenuInfoPO>().eq("CID", cid));
        return new Result<>(true);
    }

    /**
     * @description: 单个保存菜单, 转换parentCode2ParentId
     * @param: OperateMenuDTO
     * @author: lcs
     * @date: 2020/9/11
     */
    @Override
    @Transactional
    public Result<MenuInfoJsonDTO> saveMenu(OperateMenuDTO operateMenuDTO) {
        log.info("save menu, menu={}", operateMenuDTO.toString());
        // 校验code规则
        boolean matches = Pattern.matches("^[A-Za-z0-9_]([A-Za-z0-9_.]*[A-Za-z0-9_]){0,50}?$", operateMenuDTO.getCode());
        if (!matches) {
            throw new MenuOperateException(MenuErrorEnum.CODE_UNQUALIFIED);
        }

        MenuInfoJsonDTO menuInfoJsonDTO = new MenuInfoJsonDTO();
        ArrayList<MenuInfoJsonDTO> menuInfoJsonDTOS = new ArrayList<>();
        //获取国际化key 并赋值给name
        String menuI18nKey = seti18nKeyForMenu(operateMenuDTO);

        BeanUtils.copyProperties(operateMenuDTO, menuInfoJsonDTO);
        menuInfoJsonDTOS.add(menuInfoJsonDTO);
        log.debug("converting menu DTO to InfoJsonDTOS, dtos={}", menuInfoJsonDTOS.toString());

        /**
         * 新增单个国际化
         *   @PostMapping({"/resource/update/one"})
         */
        String nameDisplay = operateMenuDTO.getNameDisplay();
        if (StringUtils.isEmpty(nameDisplay)) {
            throw new MenuOperateException(MenuErrorEnum.CODE_UNQUALIFIED);
        }
        addOrUpdateMenuI18n(menuI18nKey, nameDisplay, moduleName);
        saveBachUrl(menuInfoJsonDTOS);
        return new Result<>(menuInfoJsonDTO);
    }


    /**
     * @description: 批量保存菜单, 转换parentCode2ParentId
     * @param: List<OperateMenuDTO>
     * @author: lcs
     * @date: 2020/9/24
     */
    @Override
    @Transactional
    public void batchSaveMenu(List<OperateMenuDTO> operateMenuDTOList) {

        /**
         *  批量新增国际化资源 @PostMapping({"/resource/update/list"})
         */
        Map<String, String> i18nResourceMap = new HashMap<>();

        List<MenuInfoJsonDTO> menuInfoJsonDTOS = operateMenuDTOList.stream().map(operateMenuDTO -> {
            log.info("service-api ,batchSaveMenu===operateMenuDTO=={}", operateMenuDTO.toString());
            // 校验code规则
            boolean matches = Pattern.matches("^[A-Za-z0-9_]([A-Za-z0-9_.]*[A-Za-z0-9_]){0,50}?$", operateMenuDTO.getCode());
            if (!matches) {
                throw new MenuOperateException(MenuErrorEnum.CODE_UNQUALIFIED);
            }
            operateMenuDTO.setModuleCode(null == operateMenuDTO.getModuleCode() ? "rbac" : operateMenuDTO.getModuleCode());
            // 如果name已进行国际化key格式，则不再进行国际化
            String nameDisplay = operateMenuDTO.getNameDisplay();
            if (!StringUtils.isEmpty(nameDisplay) && nameDisplay.startsWith("I18N(") && nameDisplay.endsWith(")")) {
                operateMenuDTO.setName(nameDisplay.substring(nameDisplay.indexOf("(") + 1, nameDisplay.indexOf(")")));
                operateMenuDTO.setNameDisplay(null);
            }else {
                // 获取国际化key
                String menuI18nKey = seti18nKeyForMenu(operateMenuDTO);
                if (!StringUtils.isEmpty(operateMenuDTO.getNameDisplay())) {
                    // 国际化资源key， value
                    i18nResourceMap.put(menuI18nKey, operateMenuDTO.getNameDisplay());
                }
            }

            MenuInfoJsonDTO menuInfoJsonDTO = new MenuInfoJsonDTO();
            BeanUtils.copyProperties(operateMenuDTO, menuInfoJsonDTO);
            return menuInfoJsonDTO;

        }).collect(Collectors.toList());

        // 批量新增国际化资源
        String language = RpcContext.getContext().getLanguage().toString();
        log.info("batchSaveMenu getLanguage from RpcContext:language==={}", language);
        if (null != i18nResourceMap && i18nResourceMap.size() > 0) {
            messageResourceService.messageResourceAddOrUpdateList(i18nResourceMap, moduleName, language);
        }
        saveBachUrl(menuInfoJsonDTOS);

    }


    private String seti18nKeyForMenu(OperateMenuDTO operateMenuDTO) {
        /**
         * 为菜单创建国际化的key
         * todo
         */
        //获取本地模块名
//        String moduleName = ApplicationContextUtil.getContext().getEnvironment().getProperty("spring.application.name");
        String menuI18nKey = getMenuI18nKey(moduleName);
        operateMenuDTO.setName(menuI18nKey);
        return menuI18nKey;
    }


    /**
     * 为菜单创建国际化的key
     */
    public String getMenuI18nKey(String moduleName) {
        return messageResourceService.initI18nKey(moduleName);
    }

    /**
     * 新增单个国际化
     *
     * @PostMapping({"/resource/update/one"})
     */
    public void addOrUpdateMenuI18n(String menuI18nKey, String nameDis, String moduleName) {
        String language = RpcContext.getContext().getLanguage().toString();
        log.info("saveMenu getLanguage from RpcContext:language==={}", language);
        HashMap<String, Object> i18nMap = new HashMap<>();
        HashMap<String, String> multilingual = new HashMap<>();
//        multilingual.put("en_US", "");
//        multilingual.put("zh_HK", "");
        multilingual.put(language, nameDis);

        i18nMap.put("i18n_key", menuI18nKey);
        i18nMap.put("moduleCode", moduleName);
        i18nMap.put("i18n_value", multilingual);
        messageResourceService.messageResourceAddOrUpdateOne(i18nMap);
    }


    /**
     * 删除国际化资源
     */
//    public void deleteIi8nSource(String[] keys) {
//        Result result = messageResourceService.messageResourceDeleteKeys(keys);
//        log.info("delete i18n resource {}", result.toString());
//    }

    /**
     * 菜单修改
     */

    @Override
    @Transactional
    public Result<OperateMenuDTO> updateMenu(OperateMenuDTO operateMenuDTO) {
        log.info("updateMenu=====================operateMenuDTO={}", operateMenuDTO);
        MenuInfoPO menuInfoPO = new MenuInfoPO();
        // 修改国际化的值
        MenuInfoPO menuPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", operateMenuDTO.getCode()));
        if (null == menuPO) {
            log.info("update menu but menu not exits============================> {}", menuPO);
        }
        if (null != operateMenuDTO.getNameDisplay()) {
            addOrUpdateMenuI18n(menuPO.getName(), operateMenuDTO.getNameDisplay(), moduleName);
        }
        // 刷新国际化资源
        i18nAdapterService.refreshI18n();

        //lay_no字段处理
        MenuInfoPO menuParentPO = null;
        if (null != operateMenuDTO.getParentCode()) {
            menuParentPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", operateMenuDTO.getParentCode()));
            if (null == menuParentPO) {
                log.debug("menuInfoParentPO not exists,code = :{}", operateMenuDTO.getParentCode());
                throw new MenuOperateException(MenuErrorEnum.MENUCODE_NOT_FOUND);
            }
            operateMenuDTO.setLayNo(menuParentPO.getLayNo() + 1);
            menuInfoPO.setId(menuPO.getId());
            BeanUtils.copyProperties(operateMenuDTO, menuInfoPO);
            menuInfoPO.setParentId(menuParentPO.getId());
        } else {
            menuInfoPO.setId(menuPO.getId());
            BeanUtils.copyProperties(operateMenuDTO, menuInfoPO);
        }

        //修改接口 cid赋值给 companyIds
        ArrayList<Long> companyIds = new ArrayList<>();
        companyIds.add(operateMenuDTO.getCid());
        menuInfoPO.setCompanyIds(companyIds);
        //app 字段处理
        if (!StringUtils.isEmpty(menuInfoPO.getApp())) {
            menuInfoPO.setAppId(menuInfoPO.getApp());
            menuInfoPO.setApp("rbac");
        } else {
            menuInfoPO.setApp("rbac");
        }

        menuInfoService.updateMenuInfo(menuInfoPO);
        // 维护设计器菜单列表
        MenuAppDesignerRelPO menuAppDesignerRelPO = new MenuAppDesignerRelPO();
        menuAppDesignerRelPO.setAppId(menuInfoPO.getAppId());
        menuAppDesignerRelPO.setCode(menuInfoPO.getCode());
        menuAppDesignerRelPO.setParentCode(menuInfoPO.getParentCode());
        menuAppDesignerRelService.updateAppDesinerRel(menuAppDesignerRelPO);
        BeanUtils.copyProperties(menuInfoPO, operateMenuDTO);
        return new Result<>(operateMenuDTO);
    }


    private void getMenuTreeByRootCode(String code, List<Long> idList, Map<Long, MenuInfoPO> menuObjectHashMap) {
        MenuInfoPO rootMenuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", code));
        if (ObjectUtils.isEmpty(rootMenuInfoPO)) {
            log.info("deleteMenuInfoByCodes but code not exits============================> {}", code);
        }

        List<MenuInfoPO> menuInfoPOS = menuInfoService.list();
        Map<Long, MenuInfoPO> menuInfoPOMap = menuInfoPOS.stream().collect(Collectors.toMap(MenuInfoPO::getId, menuInfoPO -> menuInfoPO));

        menuObjectHashMap.put(rootMenuInfoPO.getId(), rootMenuInfoPO);
//        menuInfoPOMap.values().stream().forEach(menuInfoPO -> {
//            if(menuInfoPO.getParentId().equals(rootMenuInfoPO.getId())){
//                menuObjectHashMap.put(menuInfoPO.getId(),menuInfoPO);
//            }
//        });
        menuInfoPOS.stream().forEach(menuInfoPO -> {
            if (menuInfoPO.getParentId().equals(rootMenuInfoPO.getId())) {
                menuObjectHashMap.put(menuInfoPO.getId(), menuInfoPO);
            }
        });

    }

    /**
     * 设计器app删除时菜单处理：
     * 1. 根据根节点获取整个菜单树 menuIdPOList
     * 2. 菜单树menuIdPOList与原有菜单列表 originalMenuIdList对比
     * 2.1 menuIdPOList比 originalMenuIdList多的菜单，表示菜单配置新增了菜单 addMenuThanParamIds
     * 2.1.1 根据 addMenuThanParamIds找出其关联直系父节点addIds
     * 2.1.2 addIds移除新增的菜单，剩余菜单置为folder
     * 2.2 menuIdPOList比 originalMenuIdList少的菜单，表示菜单配置中将此app的菜单移至其他app中
     * 2.2.1 reduceMenuThanParamIds所有菜单置为folder
     * 3. menuIdPOList移除不能删除的菜单，其余菜单删除,菜单关联关系删除
     */
    @Override
    public void deleteMenuInfoByCode(String code) {
        long a1 = System.currentTimeMillis();
        log.info("delete menu starting...=========code==>{}", code);
        if (code == null) {
            throw new MenuException(MenuErrorEnum.CODE_CAN_NOT_EMPTY);
        }
        menuInfoService.deleteMenuInfoByCode(code);
        long a2 = System.currentTimeMillis();
        menuAppDesignerRelService.deleteAppDesignerByCode(code);
        long a3 = System.currentTimeMillis();
        log.info("deleteAppDesignerByCode===========it cost=a3-a2======================================={}",a3-a2);
        log.info("deleteAppDesignerByCode===========total======================================={}",a3-a1);
    }


    @Override
    public void deleteMenuInfoByApps(String apps, String source) {
        if (StringUtils.isBlank(apps)) {
            throw new MenuException(MenuErrorEnum.MENU_DELETE_DATA_IS_NOT_EMPTY);
        }
        String[] codeArray = apps.split(",");
        List<String> appIdList = Arrays.asList(codeArray);
        menuInfoService.batchDeleteMenusByApp(appIdList, source);
    }

    @Override
    public void deletePhysics(List<Long> ids) {
        if (!ObjectUtils.isEmpty(ids)) {
            List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().in("id", ids));
            //级联删操作权限
            menuInfoService.cascadeDeleteMenuById(menuInfoPOS);
            //物理删菜单
            menuInfoService.deletePhysics(ids);
        }

    }

    @Override
    public void updateMenuInfoById(UpdateMenuInfoIdDTO updateMenuInfoIdDTO) {
        UpdateWrapper<MenuInfoPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ID", updateMenuInfoIdDTO.getOldId());
        updateWrapper.set("valid", 0);
        updateWrapper.set("ID", updateMenuInfoIdDTO.getNewId());
        menuInfoService.updateMenuInfo(null, updateWrapper);
    }

    @Override
    public void rollBack(String uuid) {
        MenuTempPO menuTempPO = menuTempService.getOne(new QueryWrapper<MenuTempPO>().eq(MenuTempField.uuid, uuid));
        String newData = menuTempPO.getNewData();
        String oldData = menuTempPO.getOldData();
        List<MenuInfoJsonDTO> new_menuInfoJsonDTOS = JSON.parseArray(newData, MenuInfoJsonDTO.class);
        List<MenuInfoJsonDTO> old_menuInfoJsonDTOS = JSON.parseArray(oldData, MenuInfoJsonDTO.class);
        //新老数据比对，找出新增的菜单数据
        List<MenuInfoJsonDTO> new_menu = new_menuInfoJsonDTOS.stream().filter(menuInfoJsonDTO -> {
            Optional<MenuInfoJsonDTO> optional = old_menuInfoJsonDTOS.stream().filter(menuInfoJsonDTO1 -> menuInfoJsonDTO1.getCode().equals(menuInfoJsonDTO.getCode())).findFirst();
            return !optional.isPresent();
        }).collect(Collectors.toList());
        //级联删除新菜单的数据
        List<String> new_menu_codes = new_menu.stream().map(MenuInfoJsonDTO::getCode).collect(Collectors.toList());
        //查询新菜单
        if (!ObjectUtils.isEmpty(new_menu_codes)) {
            List<MenuInfoPO> new_menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().in(MenuInfoField.code, new_menu_codes));
            menuInfoService.cascadeDeleteMenuById(new_menuInfoPOS);
        }
        //old数据覆盖现有数据
        //收集老操作
        List<MenuOperateJsonDTO> old_menuOperates_dto = new ArrayList<>();
        //收集老操作URL
        List<MenuOperateUrlJsonDTO> old_menuOperate_urls_dto = new ArrayList<>();
        //覆盖菜单
        List<MenuInfoPO> old_menuInfoPOS = old_menuInfoJsonDTOS.stream().map(menuInfoJsonDTO -> {
            MenuInfoPO menuInfoPO = new MenuInfoPO();
            BeanUtils.copyProperties(menuInfoJsonDTO, menuInfoPO);
            old_menuOperates_dto.addAll(menuInfoJsonDTO.getMenuOperates());
            return menuInfoPO;
        }).collect(Collectors.toList());
        menuInfoService.saveBatchMenuInfo(old_menuInfoPOS);
        //覆盖操作
        //收集老操作
        List<MenuOperatePO> old_menuOperatePOS = old_menuOperates_dto.stream().map(menuOperateJsonDTO -> {
            MenuOperatePO menuOperatePO = new MenuOperatePO();
            BeanUtils.copyProperties(menuOperateJsonDTO, menuOperatePO);
            old_menuOperate_urls_dto.addAll(menuOperateJsonDTO.getUrls());
            return menuOperatePO;
        }).collect(Collectors.toList());
        menuOperateService.saveOrUpdateBatch(old_menuOperatePOS);
        //覆盖老URL
        List<MenuOperateCodeUrlRefPO> old_menuOperateCodeUrlRefPOS = old_menuOperate_urls_dto.stream().map(menuOperateUrlJsonDTO -> {
            MenuOperateCodeUrlRefPO menuOperateCodeUrlRefPO = new MenuOperateCodeUrlRefPO();
            BeanUtils.copyProperties(menuOperateUrlJsonDTO, menuOperateCodeUrlRefPO);
            return menuOperateCodeUrlRefPO;
        }).collect(Collectors.toList());
        menuOperateCodeUrlRefService.saveOrUpdateBatch(old_menuOperateCodeUrlRefPOS);
    }

    @Override
    public void removeTemp(String uuid) {
        menuTempService.remove(new QueryWrapper<MenuTempPO>().eq(MenuTempField.uuid, uuid));
    }

    @Override
    public List<MenuInfoDTO> findPermissionMenu(Long userId) {
        List<MenuInfoPO> menus = menuInfoService.findUserMenu(userId, 0, null);
        if (!ObjectUtils.isEmpty(menus)) {
            return menus.stream().map(menu -> {
                MenuInfoDTO menuInfoDTO = new MenuInfoDTO();
                BeanUtils.copyProperties(menu, menuInfoDTO);
                return menuInfoDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }


    /**
     * 初始化组件菜单
     */
    @Override
    public void initModuleMenu(String json) {
        log.info("beginning to initialize module menus from json ");
        try {
            launchInitialize.initPermission(json);
            log.info("ending to initialize module menus from json ");
        } catch (Exception e) {
            log.error("initialize module menus error", e);
        }

    }


}
