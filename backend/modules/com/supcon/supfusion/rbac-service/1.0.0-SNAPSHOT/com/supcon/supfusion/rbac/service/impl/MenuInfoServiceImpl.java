package com.supcon.supfusion.rbac.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.rbac.api.dto.MenuInfoJsonDTO;
import com.supcon.supfusion.rbac.api.dto.MenuOperateJsonDTO;
import com.supcon.supfusion.rbac.api.dto.MenuOperateUrlJsonDTO;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.MenuErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuException;
import com.supcon.supfusion.rbac.common.exception.MenuOperateErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuOperateException;
import com.supcon.supfusion.rbac.common.exception.PermissionErrorEnum;
import com.supcon.supfusion.rbac.common.exception.PermissionException;
import com.supcon.supfusion.rbac.dao.*;
import com.supcon.supfusion.rbac.dao.field.*;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.dao.query.MenuInfoQuery;
import com.supcon.supfusion.rbac.dao.query.FlowPermissionQuery;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import com.supcon.supfusion.rbac.manager.IModuleAdapter;
import com.supcon.supfusion.rbac.service.*;
import com.supcon.supfusion.rbac.service.asyncTask.SetMenuI18NNameTask;
import com.supcon.supfusion.rbac.service.asyncTask.SetSuposMenuI18NNameTask;
import com.supcon.supfusion.rbac.service.bo.MenuInfoJsonBO;
import com.supcon.supfusion.rbac.service.bo.MenuOperateJsonBO;
import com.supcon.supfusion.rbac.service.bo.MenuOperateUrlJsonBO;
import com.supcon.supfusion.rbac.service.config.WhiteListInitConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.supcon.supfusion.rbac.common.utils.StringUtils.listContainsElement;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@Service
public class MenuInfoServiceImpl extends ServiceImpl<MenuInfoMapper, MenuInfoPO> implements IMenuInfoService {

    private static final String MENUINFO_ID = "MENUINFO_ID";
    private static final String COMPANY_ID = "COMPANY_ID";
    private static final String PARENT = "parent";
    private static final String MENUOPERATES = "menuOperates";
    private static final Long DEFAULT_CID = 1000L;

    @Value("${spring.application.name}")
    String appName;

    @Autowired
    private MenuInfoMapper menuInfoMapper;
    @Autowired
    private MenuoperateMapper menuoperateMapper;
    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private IMenuOperateCodeUrlRefService menuOperateCodeUrlRefService;
    @Autowired
    private MenuOperateCodeUrlRefMapper menuOperateCodeUrlRefMapper;
    @Autowired
    private IMenuInfoCompanyRefService menuInfoCompanyRefService;
    @Autowired
    II18nAdapter i18nAdapterService;
    @Autowired
    private IModuleAdapter moduleAdapter;
    @Autowired
    private UserPermissionMapper userPermissionMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private IRolePPositionService rolePPositionService;
    @Autowired
    private IRolePStaffService rolePStaffService;
    @Autowired
    private IUserPPositionService userPPositionService;
    @Autowired
    private IUserPStaffService userPStaffService;
    @Autowired
    private IMenuInfoMneCodeService menuInfoMneCodeService;
    @Autowired
    private IMenuTempService menuTempService;
    @Autowired
    private IFlowPermissionService flowPermissionService;
    @Autowired
    private IRoleUserService roleUserService;
    @Autowired
    private IAppRefService appRefService;
    @Autowired
    private IAppCompanyRefService appCompanyRefService;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private IMenuInfoApiService menuInfoApiService;
    @Autowired
    private IMenuAppDesignerRelService menuAppDesignerRelService;

    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;
    @Autowired
    private AppRefMapper appRefMapper;

    @Transactional
    @Override
    public Boolean saveMenuInfo(MenuInfoPO menuInfoPO) {
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                menuInfoPO.getCode(),
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        return this.saveOrUpdate(menuInfoPO);
    }

    @Transactional
    @Override
    public Boolean saveBatchMenuInfo(List<MenuInfoPO> menuInfoPOS) {
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                "batch save",
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        return this.saveOrUpdateBatch(menuInfoPOS);
    }

    @Transactional
    @Override
    public Boolean updateMenuInfo(MenuInfoPO menuInfoPO, UpdateWrapper<MenuInfoPO> updateWrapper) {
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                menuInfoPO.getCode(),
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        if (ObjectUtils.isEmpty(updateWrapper)) {
            return this.updateById(menuInfoPO);
        }
        return this.update(menuInfoPO, updateWrapper);
    }

    @Override
    public Boolean updateBatchMenuInfoById(List<MenuInfoPO> menuInfoPOS) {
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                "batch save",
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        return this.updateBatchById(menuInfoPOS);
    }

    @Transactional
    @Override
    public Boolean removeMenuInfoByIds(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Transactional
    @Override
    public Boolean removeMenuInfo(QueryWrapper<MenuInfoPO> queryWrapper) {
        return this.remove(queryWrapper);
    }

    /**
     * @description: 查询菜单树
     * @return: com.supcon.supfusion.rbac.dao.po.MenuInfoPO
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public MenuInfoPO queryMenuTree() {
        MenuInfoPO menuInfoPO = getById(-1);
        menuInfoPO.setChildren(getChildren(menuInfoPO));
        return menuInfoPO;
    }

    /**
     * @description: 查询菜单及其父菜单，树结构
     * @param: enableStatus
     * @param: restrict
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public List<MenuInfoPO> queryMenus(boolean enableStatus, boolean restrict) {
        MenuInfoQuery mq = new MenuInfoQuery();
        mq.setId(Constants.MENU_LIST_ID);
        mq.setRefCompanyId(UserContext.getUserContext().getCompanyId());
        if (restrict) {
            mq.setNoRestrict(0);
        }
        if (!ObjectUtils.isEmpty(enableStatus)) {
            mq.setEnable(enableStatus ? true : false);
        }
        List<MenuInfoPO> list = menuInfoMapper.findMenus(mq);
        list = this.filter(list);
        List<MenuInfoPO> result = getMenuListByLayRec(list);
        result = setI18nForkJoin(result);
        setReadOnly(result);
        setCompanyIds(result);
        // 转成树形结构
        List<MenuInfoPO> menuInfoPOS = flatToMenuTree(result);
        return menuInfoPOS;
    }


    /**
     * @description: 查询菜单及其父菜单，树结构
     * @param: enableStatus
     * @param: restrict
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public List<MenuInfoPO> queryMenusRef(boolean enableStatus, boolean restrict) {
        MenuInfoQuery mq = new MenuInfoQuery();
        mq.setId(Constants.MENU_LIST_ID);
        mq.setRefCompanyId(UserContext.getUserContext().getCompanyId());
        if (restrict) {
            mq.setNoRestrict(0);
        }
        if (!ObjectUtils.isEmpty(enableStatus)) {
            mq.setEnable(enableStatus ? true : false);
        }
        List<MenuInfoPO> list = menuInfoMapper.findMenus(mq);
        list = this.filter(list);
        List<MenuInfoPO> menuList = getMenuListByLayRec(list);
        List<MenuInfoPO> menuInfoPOS = setI18nForkJoin(menuList);
        // 转成树形结构
        List<MenuInfoPO> result = flatToMenuTree(menuInfoPOS);
        return result;
    }

    @Override
    public List<MenuInfoPO> queryMenuConfigure(boolean enableStatus, boolean restrict) {
        long d1 = System.currentTimeMillis();
        MenuInfoQuery mq = new MenuInfoQuery();
        mq.setId(Constants.MENU_LIST_ID);
        mq.setRefCompanyId(UserContext.getUserContext().getCompanyId());
        mq.setStatus(1);
        if (restrict) {
            mq.setNoRestrict(0);
        }
        if (!ObjectUtils.isEmpty(enableStatus)) {
            mq.setEnable(enableStatus ? true : false);
        }
        List<MenuInfoPO> list = menuInfoMapper.findMenus(mq);
        long d2 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuConfigure===findMenus=d2-d1==========================================={}", d2 - d1);

        long d3 = System.currentTimeMillis();
        list = this.filter(list);
        long d4 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuConfigure===filter=d4-d3==========================================={}", d4 - d3);

        long d5 = System.currentTimeMillis();
        //补全树结构
        list = getMenuListByLayRec(list);
        long d6 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuConfigure===getMenuListByLayRec=d6-d5==========================================={}", d6 - d5);
        //树形转为平铺，引用不变
//        List<MenuInfoPO> flat = this.treeFlat(list, null);
        long d7 = System.currentTimeMillis();
        //forkjoin设置i18n
        list = setI18nForkJoin(list);
        long d8 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuConfigure===setI18nForkJoin=d8-d7==========================================={}", d8 - d7);

        long d9 = System.currentTimeMillis();
        //设置适用范围
        setCompanyIds(list);
        long d10 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuConfigure===setCompanyIds=d10-d9==========================================={}", d10 - d9);

        long d11 = System.currentTimeMillis();
        //设置适用范围是否可编辑
        setReadOnly(list);
        long d12 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuConfigure===setReadOnly=d10-d9==========================================={}", d12 - d11);

        long d13 = System.currentTimeMillis();
        //排序
        list.sort(Comparator.comparing(MenuInfoPO::getSort, Comparator.nullsLast(Comparator.nullsLast(Double::compareTo))));
        long d14 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuConfigure===setReadOnly=d10-d9==========================================={}", d14 - d13);

        long d15 = System.currentTimeMillis();
        //平铺转树形结构
        List<MenuInfoPO> menuInfoPOS = flatToMenuTree(list);
        long d16 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuConfigure===setReadOnly=d10-d9==========================================={}", d16 - d15);
        return menuInfoPOS;
    }

    private List<MenuInfoPO> setI18nForkJoin(List<MenuInfoPO> flat) {
        ForkJoinPool pool = new ForkJoinPool();
        SetMenuI18NNameTask forkJoinTask = new SetMenuI18NNameTask(0, flat.size() - 1, flat,i18nAdapterService);
        ForkJoinTask<List<MenuInfoPO>> result = pool.submit(forkJoinTask);
        try {
            return result.get();
        } catch (Exception e) {
            throw new PermissionException(PermissionErrorEnum.SET_MENU_I18N_ERROR);
        }
    }

    private List<MenuSuposPO> setI18nForkJoinForSuposMenu(List<MenuSuposPO> flat) {
        ForkJoinPool pool = new ForkJoinPool();
        SetSuposMenuI18NNameTask forkJoinTask = new SetSuposMenuI18NNameTask(0, flat.size() - 1, flat,i18nAdapterService);
        ForkJoinTask<List<MenuSuposPO>> result = pool.submit(forkJoinTask);
        try {
            return result.get();
        } catch (Exception e) {
            throw new PermissionException(PermissionErrorEnum.SET_MENU_I18N_ERROR);
        }
    }

    private List<MenuInfoPO> flatToMenuTree(List<MenuInfoPO> flat) {
        ArrayList<MenuInfoPO> menuInfoPOList = new ArrayList<>();
        // 构造map
        Map<Long, MenuInfoPO> menuMap = flat.stream().collect(Collectors.toMap(MenuInfoPO::getId, menuInfoPO -> menuInfoPO));
        flat.stream().forEach(menuInfoPO -> {
            if (null != menuInfoPO.getParentId() && -1L == menuInfoPO.getParentId()) {
                menuInfoPOList.add(menuInfoPO);
            } else {
                MenuInfoPO parent = menuMap.get(menuInfoPO.getParentId());
                if (!ObjectUtils.isEmpty(parent)) {
                    parent.addChlidResource(menuInfoPO);
                }
            }
        });
        return menuInfoPOList;
    }

    private List<MenuInfoPO> treeFlat(List<MenuInfoPO> tree, List<MenuInfoPO> flat) {
        if (ObjectUtils.isEmpty(flat)) {
            flat = new ArrayList<>();
        }
        for (MenuInfoPO menuInfoPO : tree) {
            flat.add(menuInfoPO);
            if (!ObjectUtils.isEmpty(menuInfoPO.getChildren())) {
                treeFlat(menuInfoPO.getChildren(), flat);
            }
        }
        // children置为null
        for (MenuInfoPO menuInfoPO : flat) {
            menuInfoPO.setChildren(new ArrayList<>());
        }
        return flat;
    }

    private void setReadOnly(List<MenuInfoPO> menuInfoPOS) {
        if (ObjectUtils.isEmpty(menuInfoPOS)) {
            return;
        }
//        List<Long> appRefMenuId = appRefService.findAppRefMenuId(menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList()));
//        menuInfoPOS.forEach(menuInfoPO -> {
//            menuInfoPO.setCompany_readOnly(appRefMenuId.contains(menuInfoPO.getId()));
//        });
        Set<String> collect = menuInfoPOS.stream().filter(menuInfoPO -> menuInfoPO.getAppId() != null).map(MenuInfoPO::getAppId).collect(Collectors.toSet());
        // 根据app_company_ref判断是否可读
        Set<String> appComRefAppIdList = appCompanyRefService.findAppComRefByAppId(collect);
        // 获取app_company_ref中的app对应的所有菜单
        List<Long> menuIds = menuInfoService.getMenusByAppIds(appComRefAppIdList);
        menuInfoPOS.forEach(menuInfoPO -> {
            if (appComRefAppIdList.contains(menuInfoPO.getAppId()) || menuIds.contains(menuInfoPO.getId())) {
                menuInfoPO.setCompany_readOnly(true);
            }
        });

    }

    public void sortMenuListAndI18n(List<MenuInfoPO> menuInfoPOS) {
        menuInfoPOS.sort(Comparator.comparing(MenuInfoPO::getSort, Comparator.nullsLast(Comparator.nullsLast(Double::compareTo))));
        menuInfoPOS.forEach(menuInfoPO -> {
            menuInfoPO.setNameDisplay(i18nAdapterService.getRemoteMessage(menuInfoPO.getName(), null, LocaleContextHolder.getLocale()));
            if (!ObjectUtils.isEmpty(menuInfoPO.getChildren())) {
                sortMenuListAndI18n(menuInfoPO.getChildren());
            }
        });
    }

    //菜单去重
    private List<MenuInfoPO> filter(List<MenuInfoPO> menuInfoPOS){
        return menuInfoPOS.parallelStream().collect(Collectors.collectingAndThen(Collectors.toCollection(()
                -> new TreeSet<>(Comparator.comparing(MenuInfoPO::getId))), ArrayList::new));
    }

    /**
     * @description: 查询菜单子节点
     * @param: menuInfoPO
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    private List<MenuInfoPO> getChildren(MenuInfoPO menuInfoPO) {
        MenuInfoQuery mq = new MenuInfoQuery();
        mq.setParentId(menuInfoPO.getId());
        mq.setRefCompanyId(UserContext.getUserContext().getCompanyId());
        mq.setEnable(true);
        List<MenuInfoPO> childrenList = menuInfoMapper.findMenus(mq);
        childrenList = this.filter(childrenList);
        setCompanyIds(childrenList);
        sortMenuListAndI18n(childrenList);
        for (MenuInfoPO children : childrenList) {
            List<MenuInfoPO> menuInfoPOList = getChildren(children);
            children.setChildren(menuInfoPOList);
        }
        return childrenList;
    }

    /**
     * @description: 查询菜单列表
     * @param: keyword
     * @param: id
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public List<MenuInfoPO> queryMenuList(String keyword, Long id, String enable) {
        long f1 = System.currentTimeMillis();
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.notIn("mi." + MenuInfoField.id, -1);
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())) {
                queryWrapper.apply("mm." + MenuInfoMneCodeField.mneCode + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR);
            } else {
                queryWrapper.like("mm." + MenuInfoMneCodeField.mneCode, dbStringUtil.getString(keyword));
            }
        }
        if (!ObjectUtils.isEmpty(id)) {
            queryWrapper.eq("mi." + MenuInfoField.id, id);
        }
        if (!ObjectUtils.isEmpty(enable)) {
            queryWrapper.eq("mi." + MenuInfoField.enable, 1);
        }
        queryWrapper.and(queryWrapper1 -> queryWrapper1.eq("mcf." + MenuInfoCompanyRefField.companyId, UserContext.getUserContext().getCompanyId()).or().eq("mcf." + MenuInfoCompanyRefField.companyId, -1L).or().eq("mi." + MenuInfoField.cid, UserContext.getUserContext().getCompanyId()));
        queryWrapper.and(queryWrapper1 -> queryWrapper1.eq("mi." + MenuInfoField.valid, 1));
        queryWrapper.groupBy("mi." + MenuInfoField.id, "mi." + MenuInfoField.sort, "mi." + MenuInfoField.layRec, "mi." + MenuInfoField.app, "mi." + MenuInfoField.code, "mi." + MenuInfoField.name, "mi." + MenuInfoField.parentId, "mi." + MenuInfoField.cid, "mi." + MenuInfoField.memo, "mi." + MenuInfoField.url, "mi." + MenuInfoField.type, "mi." + MenuInfoField.target, "mi." + MenuInfoField.source, "mi." + MenuInfoField.showType, "mi." + MenuInfoField.menuType, "mi." + MenuInfoField.isHide);
        List<MenuInfoPO> menuInfoPOS = menuInfoMapper.findMenusByKeywordNoPage(queryWrapper);
        long f2 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuList===f2-f1================================================={}", f2 - f1);


        long f3 = System.currentTimeMillis();
        menuInfoPOS = getMenuListByLayRec(menuInfoPOS);
        long f4 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuList===f4-f3================================================={}", f4 - f3);

        long f5 = System.currentTimeMillis();
        setCompanyIds(menuInfoPOS);
        long f6 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuList==setCompanyIds=f6-f5================================================={}", f6 - f5);
        //树形转为平铺，引用不变
//        List<MenuInfoPO> flat = this.treeFlat(parentMenu, null);

        long f7 = System.currentTimeMillis();
        // 设置i18n值
        menuInfoPOS = setI18nForkJoin(menuInfoPOS);
        long f8 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuList==setI18nForkJoin=f8-f7================================================={}", f8 - f7);

        long f9 = System.currentTimeMillis();
        //排序
        menuInfoPOS.sort(Comparator.comparing(MenuInfoPO::getSort, Comparator.nullsLast(Comparator.nullsLast(Double::compareTo))));
        long f10 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuList==sort=f9-f8================================================={}", f9 - f8);

        long f11 = System.currentTimeMillis();
        //平铺转树形结构
        List<MenuInfoPO> menuTree = flatToMenuTree(menuInfoPOS);
        long f12 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.queryMenuList==flatToMenuTree=f8-f7================================================={}", f12 - f11);


        return menuTree;
    }

    @Override
    public List<MenuInfoPO> queryMenuConfigureList(String keyword, Long id, String enable) {

        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mi." + MenuInfoField.status, 1);
        queryWrapper.notIn("mi." + MenuInfoField.id, -1);
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())) {
                queryWrapper.apply("mm." + MenuInfoMneCodeField.mneCode + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR);
            } else {
                queryWrapper.like("mm." + MenuInfoMneCodeField.mneCode, dbStringUtil.getString(keyword));
            }
        }
        if (!ObjectUtils.isEmpty(id)) {
            queryWrapper.eq("mi." + MenuInfoField.id, id);
        }
        if (!ObjectUtils.isEmpty(enable)) {
            queryWrapper.eq("mi." + MenuInfoField.enable, 1);
        }
        queryWrapper.and(queryWrapper1 -> queryWrapper1.eq("mcf." + MenuInfoCompanyRefField.companyId, UserContext.getUserContext().getCompanyId()).or().eq("mcf." + MenuInfoCompanyRefField.companyId, -1L).or().eq("mi." + MenuInfoField.cid, UserContext.getUserContext().getCompanyId()));
        queryWrapper.groupBy("mi." + MenuInfoField.id, "mi." + MenuInfoField.sort, "mi." + MenuInfoField.code, "mi." + MenuInfoField.layRec, "mi." + MenuInfoField.name, "mi." + MenuInfoField.parentId, "mi." + MenuInfoField.cid, "mi." + MenuInfoField.memo, "mi." + MenuInfoField.url, "mi." + MenuInfoField.type, "mi." + MenuInfoField.target, "mi." + MenuInfoField.source, "mi." + MenuInfoField.showType, "mi." + MenuInfoField.menuType, "mi." + MenuInfoField.isHide, "mi." + MenuInfoField.app);
        List<MenuInfoPO> menuInfoPOS = menuInfoMapper.findMenusByKeywordNoPage(queryWrapper);
        List<MenuInfoPO> list = filter(menuInfoPOS);
        List<MenuInfoPO> menuListByLayRec = getMenuListByLayRec(list);
        List<MenuInfoPO> menuSetI18nList = setI18nForkJoin(menuListByLayRec);
        setCompanyIds(menuSetI18nList);
        List<MenuInfoPO> menuInfoTree = flatToMenuTree(menuSetI18nList);
        return menuInfoTree;
    }

    /**
     * @description: 新增菜单
     * @param: menuInfoPO
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    @Transactional
    public void addMenu(MenuInfoPO menuInfoPO) {
        if (validateMenuExist(menuInfoPO.getCode())) {
            throw new MenuException(MenuErrorEnum.MENU_IS_EXISTS);
        }
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MenuInfoField.parentId, menuInfoPO.getParentId());

        List<MenuInfoPO> brotherMenuInfoPOList = list(queryWrapper);
        if (CollectionUtils.isEmpty(brotherMenuInfoPOList)) {
            menuInfoPO.setSort(1.0);
        } else {
            menuInfoPO.setSort(brotherMenuInfoPOList.size() + 1.0);
        }
        menuInfoPO.setId(IDGenerator.newInstance().generate().longValue());
        if (menuInfoPO.getParentId().equals(Constants.MENU_LIST_ID)) {
            menuInfoPO.setLayNo(1);
            menuInfoPO.setFullPath(menuInfoPO.getId() + "");
            menuInfoPO.setFullPathName(menuInfoPO.getName() + "");
            menuInfoPO.setLayRec(menuInfoPO.getId() + "");
        } else {
            MenuInfoPO parentMenuInfoPO = getById(menuInfoPO.getParentId());
            menuInfoPO.setLayNo(parentMenuInfoPO.getLayNo() + 1);
            menuInfoPO.setFullPath(parentMenuInfoPO.getFullPath() + HttpConstants.URL_SPLITER + menuInfoPO.getId());
            menuInfoPO.setLayRec(parentMenuInfoPO.getLayRec() + Constants.LAY_REC_SPLIT + menuInfoPO.getId());
            menuInfoPO.setFullPathName(parentMenuInfoPO.getFullPathName() + HttpConstants.URL_SPLITER + menuInfoPO.getName());
        }
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                menuInfoPO.getCode(),
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        this.saveMenuInfo(menuInfoPO);
        menuInfoMneCodeService.createMenuInfoMneCodeI18NKey(menuInfoPO.getName(), menuInfoPO.getId());
        //modify by yy 2020/7/2
        //新增菜单默认添加一条操作
        Boolean noRestrict = menuInfoPO.getNoRestrict();
        if (ObjectUtils.isEmpty(noRestrict) || !noRestrict) {
            MenuOperatePO menuOperatePO = new MenuOperatePO();
            menuOperatePO.setUrl(menuInfoPO.getUrl());
            menuOperatePO.setDefaultOperate(true);
            menuOperatePO.setCode(menuInfoPO.getCode() + "_default");
            menuOperatePO.setSort(0D);
            menuOperatePO.setMenuinfoId(menuInfoPO.getId());
            menuOperatePO.setName("rbac.MENU_OPERATE_DEFAULT_OPERATE");
            menuOperatePO.setNameDisplay("-");
            menuoperateMapper.insert(menuOperatePO);
        }
        //新增菜单的公司关联
        List<MenuInfoCompanyRefPO> menuInfoCompanyRefPOS = menuInfoPO.getCompanyIds().stream().map(id -> {
            MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
            menuInfoCompanyRefPO.setCompanyId(id);
            menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
            if (null != menuInfoPO.getApp()) {
                menuInfoCompanyRefPO.setAppId(menuInfoPO.getApp());
            }
            return menuInfoCompanyRefPO;
        }).collect(Collectors.toList());
        menuInfoCompanyRefService.saveOrUpdateBatch(menuInfoCompanyRefPOS);
    }

    /**
     * @description: 更新菜单
     * @param: menuInfoPO
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void updateMenu(MenuInfoPO menuInfoPO) {
        checkParentMenuCompany(menuInfoPO);
        checkChildMenuCompany(menuInfoPO);
        if (!UserContext.getUserContext().getCompanyId().equals(menuInfoPO.getCid())) {
            throw new MenuException(MenuErrorEnum.ONLY_THE_CREATION_COMPANY_HAS_PERMISSION_TO_MODIFY);
        }
        UpdateWrapper<MenuInfoPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq(MenuInfoField.code, menuInfoPO.getCode());
        this.updateMenuInfo(menuInfoPO, updateWrapper);
        menuInfoMneCodeService.createMenuInfoMneCodeI18NKey(menuInfoPO.getName(), menuInfoPO.getId());
    }

    /**
     * @description: 判断父菜单使用范围是否比子菜单大
     * @param: menuInfoPO
     * @return: void
     * @author: 袁阳
     * @date: 2020/9/8
     */
    private void checkParentMenuCompany(MenuInfoPO menuInfoPO) {
        //判断父节点适用范围是否大于子节点 如果父节点是-1 则无需判断
        QueryWrapper<MenuInfoCompanyRefPO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(menuInfoPO.getParentId()) && menuInfoPO.getParentId() != -1) {
            //查看交集Set
            Set<Long> set = new HashSet<>();
            //查父节点适用范围
            Long parentId = menuInfoPO.getParentId();
            queryWrapper.eq(MENUINFO_ID, parentId);
            queryWrapper.select(COMPANY_ID);
            List<MenuInfoCompanyRefPO> parentCompanies = menuInfoCompanyRefService.list(queryWrapper);
            if (!ObjectUtils.isEmpty(parentCompanies)) {
                set.addAll(parentCompanies.stream().map(MenuInfoCompanyRefPO::getCompanyId).collect(Collectors.toList()));
            }
            //子节点适用范围
            List<Long> childCompanies = menuInfoPO.getCompanyIds();
            boolean b = set.addAll(childCompanies);
            if (b && !set.contains(-1L)) {
                throw new MenuException(MenuErrorEnum.CHILD_COMPANY_MORE_THAN_PARENT_UPDATE);
            }
            if (!childCompanies.contains(menuInfoPO.getCid()) && !childCompanies.contains(-1L)) {
                throw new MenuException(MenuErrorEnum.THE_CREATION_COMPANY_MUST_IN_COMPANYS);
            }
        }
    }

    /**
     * @description: 判断子菜单适用范围是否比父菜单大
     * @param: menuInfoPO
     * @return: void
     * @author: 袁阳
     * @date: 2020/9/8
     */
    private void checkChildMenuCompany(MenuInfoPO menuInfoPO) {
        //判断父节点适用范围是否大于子节点
        QueryWrapper<MenuInfoCompanyRefPO> queryWrapper = new QueryWrapper<>();
        //查询菜单所有子菜单
        List<MenuInfoPO> childMenus = this.list(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.parentId, menuInfoPO.getId()));
        if (!ObjectUtils.isEmpty(childMenus)) {
            //查看交集Set
            Set<Object> set = new HashSet<>(menuInfoPO.getCompanyIds());
            //查子节点适用范围
            queryWrapper.in(MENUINFO_ID, childMenus.stream().map(MenuInfoPO::getId).collect(Collectors.toList()));
            queryWrapper.select(COMPANY_ID);
            queryWrapper.groupBy(COMPANY_ID);
            List<MenuInfoCompanyRefPO> childCompanies = menuInfoCompanyRefService.list(queryWrapper);
            if (!ObjectUtils.isEmpty(childCompanies)) {
                //子节点适用范围
                List<Long> childCompanyIds = childCompanies.stream().map(MenuInfoCompanyRefPO::getCompanyId).collect(Collectors.toList());
                boolean b = set.addAll(childCompanyIds);
                if (b && !set.contains(-1L)) {
                    throw new MenuException(MenuErrorEnum.CHILD_COMPANY_MORE_THAN_PARENT_UPDATE);
                }
                if (!childCompanyIds.contains(menuInfoPO.getCid()) && !childCompanyIds.contains(-1L)) {
                    throw new MenuException(MenuErrorEnum.THE_CREATION_COMPANY_MUST_IN_COMPANYS);
                }
            }
        }
    }

    /**
     * @description: 批量删除菜单
     * @param: list
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void batchDeleteMenus(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            throw new MenuException(MenuErrorEnum.MENU_DELETE_DATA_IS_NOT_EMPTY);
        }

        List<MenuInfoPO> menuInfoPOList = new ArrayList<>();
        for (String code : list) {
            MenuInfoPO menuInfoPO = queryMenuByCode(code);
            if (Objects.isNull(menuInfoPO)) continue;
            // 通过全路径匹配出来当前节点以及所有字节点
            QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.likeRight(MenuInfoField.layRec, menuInfoPO.getLayRec());
            List<MenuInfoPO> menuInfoList = list(queryWrapper);
            menuInfoPOList.addAll(menuInfoList);
        }
        //查询分配了权限的菜单
        List<Long> assignPermissionMenuInfoId = menuoperateMapper.findAssignPermissionMenuInfoId();
        List<Long> ids = menuInfoPOList.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
        Optional<Long> first = ids.stream().filter(assignPermissionMenuInfoId::contains).findFirst();
        if (first.isPresent()) {
            throw new MenuException(MenuErrorEnum.MENU_HAS_GRANT_PERMISSION);
        } else {
            //删除助记码
            menuInfoMneCodeService.remove(new QueryWrapper<MenuInfoMneCodePO>().in(MenuInfoMneCodeField.menuInfoId, ids));
            //删除操作
            log.info("batchDeleteMenus===menuInfoMneCodeService.remove,params:ids={}==============================", ids);
            menuOperateService.remove(new QueryWrapper<MenuOperatePO>().in(MenuOperateField.menuinfoId, ids));
            log.info("batchDeleteMenus===removeMenuInfoByIds,params:ids={}==============================", ids);
            removeMenuInfoByIds(ids);
        }
    }

    /**
     * @description: 菜单排序
     * @param: jsonObject
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void modifyMenuSort(JSONObject jsonObject) {
        String parentIdStr = jsonObject.getString("parentId");
        String prevIdStr = jsonObject.getString("prevId");
        String nextIdStr = jsonObject.getString("nextId");
        String currentIdStr = jsonObject.getString("currentId");
        Boolean fusion = jsonObject.getBoolean("supfusion");


        // 获取上一个节点和下一个节点的排序号
        Double prevSort = 0.0;
        Double nextSort = 0.0;
        if (StringUtils.isNoneBlank(prevIdStr)) {
            MenuInfoPO prevMenuInfoPO = getById(Long.parseLong(prevIdStr));
            prevSort = prevMenuInfoPO.getSort();
        }
        if (StringUtils.isNoneBlank(nextIdStr)) {
            MenuInfoPO nextMenuInfoPO = getById(Long.parseLong(nextIdStr));
            nextSort = nextMenuInfoPO.getSort();
        }

        // 修改当前节点的排序号和父节点的Id
        Long currentId = Long.parseLong(currentIdStr);
        MenuInfoPO currentMenuInfoPO = menuInfoMapper.getMenu(new QueryWrapper<MenuInfoPO>().eq("mi." + MenuInfoField.id, currentId));
        if (Objects.isNull(currentMenuInfoPO)) return;
        setCompanyIds(Collections.singletonList(currentMenuInfoPO));
        Long parentId = Long.parseLong(parentIdStr);
//        if (StringUtils.isNoneBlank(parentIdStr)) {
//            parentId = Long.parseLong(parentIdStr);
//            //将要移动到根目录且要移动的菜单非根目录
//            if (!ObjectUtils.isEmpty(fusion) && fusion && parentId == -1 && -1 != currentMenuInfoPO.getParentId()) {
//                throw new MenuException(MenuErrorEnum.CANT_GENERATE_ROOT_MENU);
//            }
//            //将要移动到非根目录且要移动的菜单为根目录
//            if (!ObjectUtils.isEmpty(fusion) && fusion && -1 == currentMenuInfoPO.getParentId() && -1 != parentId) {
//                throw new MenuException(MenuErrorEnum.ROOT_CON_NOT_REMOVE_TO_ROOT);
//            }
//        }
        // 计算获取当前节点的排序号
        Double newSort = getNewOrder(prevSort, nextSort, parentId);
        currentMenuInfoPO.setSort(newSort);
        currentMenuInfoPO.setParentId(parentId);
        // 根据当前节点的fullPath和layNo获取排序后当前节点以及字节点新的fullPath和layNo
        String oldCurrentMenuInfoFullPath = currentMenuInfoPO.getLayRec();
        String newCurrentMenuInfoFullPath;
        String newCurrentMenuInfoLayRec;
        Integer oldCurrentMenuInfoLayNo = currentMenuInfoPO.getLayNo();
        Integer newCurrentMenuInfoLayNo;
        String newCurrentMenuInfoFullPathName;
        if (Constants.MENU_LIST_ID.equals(parentId)) {
            newCurrentMenuInfoLayNo = 1;
            newCurrentMenuInfoFullPath = currentMenuInfoPO.getId() + "";
            newCurrentMenuInfoLayRec = currentMenuInfoPO.getId() + "";
            newCurrentMenuInfoFullPathName = currentMenuInfoPO.getName();
        } else {
            MenuInfoPO parentMenuInfoPO = menuInfoMapper.getMenu(new QueryWrapper<MenuInfoPO>().eq("mi." + MenuInfoField.id, parentId));
            setCompanyIds(Collections.singletonList(parentMenuInfoPO));
            Integer parentMenuInfoPOLayNo = parentMenuInfoPO.getLayNo();
            newCurrentMenuInfoLayNo = parentMenuInfoPOLayNo + 1;
            newCurrentMenuInfoFullPath = parentMenuInfoPO.getFullPath() + HttpConstants.URL_SPLITER + currentMenuInfoPO.getId();
            newCurrentMenuInfoLayRec = parentMenuInfoPO.getLayRec() + Constants.LAY_REC_SPLIT + currentMenuInfoPO.getId();
            newCurrentMenuInfoFullPathName = parentMenuInfoPO.getFullPathName() + HttpConstants.URL_SPLITER + currentMenuInfoPO.getName();
            //子菜单适用范围不可大于父菜单
            Set<Long> companyIds = new HashSet<>(parentMenuInfoPO.getCompanyIds());
            boolean b = companyIds.addAll(currentMenuInfoPO.getCompanyIds());
            if (b && !companyIds.contains(-1L)) {
                throw new MenuException(MenuErrorEnum.CHILD_COMPANY_MORE_THAN_PARENT);
            }
        }
        currentMenuInfoPO.setFullPath(newCurrentMenuInfoFullPath);
        currentMenuInfoPO.setLayRec(newCurrentMenuInfoLayRec);
        currentMenuInfoPO.setFullPathName(newCurrentMenuInfoFullPathName);
        currentMenuInfoPO.setEdited(true);
        log.info("/menu/sort=====currentMenuInfoPO={}=========================================", currentMenuInfoPO);
        this.updateMenuInfo(currentMenuInfoPO, null);
        Integer diffLayNo = newCurrentMenuInfoLayNo - oldCurrentMenuInfoLayNo;
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight(MenuInfoField.layRec, oldCurrentMenuInfoFullPath);
        queryWrapper.ne(MenuInfoField.layRec, oldCurrentMenuInfoFullPath);
        List<MenuInfoPO> menuInfoPOList = list(queryWrapper);

        menuInfoPOList.forEach(item -> {
            Integer sonLayNo = item.getLayNo() + diffLayNo;
            String sonFullPath = newCurrentMenuInfoFullPath + HttpConstants.URL_SPLITER + item.getId();
            String sonLayRec = newCurrentMenuInfoLayRec + Constants.LAY_REC_SPLIT + item.getId();
            item.setLayNo(sonLayNo);
            item.setLayRec(sonLayRec);
            item.setFullPath(sonFullPath);
            item.setEdited(true);
        });
        if (!ObjectUtils.isEmpty(menuInfoPOList)) {
            log.info("modifyMenuSort.updateBatchMenuInfoById:params=menuInfoPOList={}====================================", menuInfoPOList);
            this.updateBatchMenuInfoById(menuInfoPOList);
        }
    }

    /**
     * @description: 计算菜单新排序号
     * @param: prevOrder
     * @param: nextOrder
     * @return: java.lang.Double
     * @author: 袁阳
     * @date: 2020/8/31
     */
    private Double getNewOrder(Double prevOrder, Double nextOrder, Long parentId) {
        if (prevOrder == 0.0 && nextOrder == 0.0) {
            //父节点下节点，获取父节点下最后一个排序，加一
            List<MenuInfoPO> infoPOS = this.list(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.parentId, parentId).orderByDesc(MenuInfoField.sort));
            if (ObjectUtils.isEmpty(infoPOS)) {
                return 1.0;
            } else {
                return infoPOS.get(0).getSort() + 1;
            }
        }
        if (prevOrder == 0.0) {
            // 移动到第一位
            return nextOrder / 2;
        }
        if (nextOrder == 0.0) {
            // 移动到最后位
            return prevOrder + 10000.0;
        }
        // 移动到中间某个位置
        return (prevOrder + nextOrder) / 2;
    }

    /**
     * @description: 校验菜单是否存在
     * @param: code
     * @return: boolean
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public boolean validateMenuExist(String code) {
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MenuInfoField.code, code);
        int count = count(queryWrapper);
        return count >= 1;
    }

    /**
     * @description: 根据编码查询菜单
     * @param: code
     * @return: com.supcon.supfusion.rbac.dao.po.MenuInfoPO
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public MenuInfoPO queryMenuByCode(String code) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(MenuInfoField.code, code);
        queryWrapper.eq(MenuInfoField.valid, 1);
        return getOne(queryWrapper);
    }

    public MenuInfoPO queryMenuById(Long id) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("ID", id);
        queryWrapper.eq("VALID", 1);
        return getOne(queryWrapper);
    }

    /**
     * @description: 查询登陆用户权限菜单
     * @param: userId
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/6/18
     */
    @Override
    public List<MenuInfoPO> findUserMenu(Long userId, Integer status, Boolean enable) {
        MenuInfoQuery m = new MenuInfoQuery();
        m.setUserId(userId);
        m.setCid(UserContext.getUserContext().getCompanyId());
        m.setStatus(status);
        m.setIsHide(false);
        if (!ObjectUtils.isEmpty(enable)) {
            m.setEnable(enable);
        }
        List<MenuInfoPO> userMenus = menuInfoMapper.findUserMenus(m);
        //查询流程权限表中的操作菜单
        List<MenuInfoPO> flowPermissionMenu = this.findUserFlowPermissionMenu(userId, status);
        //两个List整合
        if (!ObjectUtils.isEmpty(flowPermissionMenu)) {
            userMenus.addAll(flowPermissionMenu);
        }
        //菜单去重
        userMenus = this.filter(userMenus);
        List<MenuInfoPO> parentMenu = findParentMenu(userMenus);
        return parentMenu;
    }


    @Override
    public List<MenuInfoPO> findUserMenuFlat(Long userId, Integer status,Boolean enable) {
        List<MenuInfoSimplePO> mergedMenus = this.getUserSimpleMenus(userId, status, enable);
        //根据lay_rec组装所有菜单
        long a7 = System.currentTimeMillis();
        List<MenuInfoPO> menuPOList = this.getMenusByLayRec(mergedMenus);
        long a8 = System.currentTimeMillis();
        log.info("==> 根据lay_rec从库查询所有菜单，{}ms",a8-a7);

        return menuPOList;
    }

    @Override
    public List<MenuSuposPO> findUserSuposMenuFlat(Long userId, Integer status, Boolean enable) {
        Long cid = UserContext.getUserContext().getCompanyId();
        List<Long> userMenuIds = this.getUserPermissionMenusIds(userId, cid, status, enable);
        List<Long> userFlowMenuIds = this.findUserFlowPermissionMenuIds(userId, status);

        userMenuIds.addAll(userFlowMenuIds);
        Set<Long> mergedMenuIds = new HashSet<>(userMenuIds);
        log.info("==> 菜单id合并后，size={}", mergedMenuIds.size());

        //根据id查询所有菜单
        long a7 = System.currentTimeMillis();
        List<MenuSuposPO> menuPOList = this.getSuposMenusByIds(new ArrayList<>(mergedMenuIds));
        long a8 = System.currentTimeMillis();
        log.info("==> 根据lay_rec从库查询所有菜单，{}ms, size={}",a8-a7, menuPOList.size());

        return menuPOList;
    }

    private List<MenuInfoSimplePO> getUserSimpleMenus(Long userId, Integer status,Boolean enable) {
        //查询用户权限菜单
        List<MenuInfoSimplePO> userMenus = this.findUserPermissionSimpleMenus(userId, status, enable);
        //查询流程权限表中的操作菜单
        List<MenuInfoSimplePO> flowPermissionMenu = this.findUserFlowPermissionSimpleMenus(userId, status);
        //合并去重
        long a5 = System.currentTimeMillis();
        userMenus.addAll(flowPermissionMenu);
        List<MenuInfoSimplePO> mergedMenus = userMenus.stream().distinct().collect(Collectors.toList());
        long a6 = System.currentTimeMillis();
        log.info("==> 合并用户菜单和流程菜单并去重, {}ms",a6-a5);

        return mergedMenus;
    }

    /**
     * 获得用户有权限的所有菜单的id
     *
     * @param userId
     * @param companyId
     * @param status
     * @param enable
     * @return
     */
    private List<Long> getUserPermissionMenusIds(Long userId, Long companyId, Integer status,Boolean enable) {
        MenuInfoQuery parameters = new MenuInfoQuery();
        parameters.setUserId(userId);
        parameters.setCid(UserContext.getUserContext().getCompanyId());
        parameters.setStatus(status);
        parameters.setIsHide(false);
        if (!ObjectUtils.isEmpty(enable)){
            parameters.setEnable(enable);
        }

        long a1 = System.currentTimeMillis();
        List<String> layrecs = this.menuInfoMapper.findUserMenusLayRecs(parameters);
        long a2 = System.currentTimeMillis();
        log.info("==> 从库中查询用户权限菜单Lay_Rec，{}ms, size={}",a2-a1, layrecs.size());
        //
        final Map<Long, Object> idMap = new ConcurrentHashMap<>();
        layrecs.parallelStream().forEach(rec -> Stream.of(rec.split("-")).forEach(r -> idMap.put(Long.valueOf(r), new Object())));

        log.info("==> 用户权限菜单lay_rec转ids并去重后,size={}", idMap.size());

        return new ArrayList<>(idMap.keySet());
    }

    private List<MenuInfoSimplePO> findUserPermissionSimpleMenus(Long userId, Integer status,Boolean enable) {
        long a1 = System.currentTimeMillis();

        MenuInfoQuery parameters = new MenuInfoQuery();
        parameters.setUserId(userId);
        parameters.setCid(UserContext.getUserContext().getCompanyId());
        parameters.setStatus(status);
        parameters.setIsHide(false);
        if (!ObjectUtils.isEmpty(enable)){
            parameters.setEnable(enable);
        }
        List<MenuInfoSimplePO> userMenus = this.menuInfoMapper.findUserSimpleMenus(parameters);

        long a2 = System.currentTimeMillis();
        log.info("==> 从库中查询用户权限菜单，{}ms",a2-a1);

        return userMenus;
    }

    /**
     * 查询用户权限菜单
     *
     * @param userId
     * @param status
     * @param enable
     * @return
     */
    private List<MenuInfoPO> findUserPermissionMenus(Long userId, Integer status,Boolean enable) {
        long a1 = System.currentTimeMillis();

        MenuInfoQuery m = new MenuInfoQuery();
        m.setUserId(userId);
        m.setCid(UserContext.getUserContext().getCompanyId());
        m.setStatus(status);
        m.setIsHide(false);
        if (!ObjectUtils.isEmpty(enable)){
            m.setEnable(enable);
        }
        List<MenuInfoPO> userMenus = menuInfoMapper.findUserMenus(m);

        long a2 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.findUserMenuFlat=====查询当前user的菜单==a2-=a1============================================{}",a2-a1);

        return userMenus;
    }

    /**
     * 查询用户有权限的流程菜单id
     *
     * @param userId
     * @param status
     * @return
     */
    private List<Long> findUserFlowPermissionMenuIds(Long userId, Integer status) {
        long b = System.currentTimeMillis();
        List<String> layrecs = this.menuInfoMapper.findUserPermissionFlowMenuLayrecs(userId, status);
        long e = System.currentTimeMillis();
        log.info("==> 从库中查询用户流程权限菜单ID， user id={}, status={}, time={}ms, size={}", userId, status, e - b, layrecs.size());

        final Map<Long, Object> idMap = new ConcurrentHashMap<>();
        layrecs.parallelStream().forEach(rec -> Stream.of(rec.split("-")).forEach(r -> idMap.put(Long.valueOf(r), new Object())));
        log.info("==> 从库中查询用户流程权限菜单lay_rec转ids并去重后,size={}", idMap.size());

        return new ArrayList<>(idMap.keySet());
    }

    private List<MenuInfoSimplePO>  findUserFlowPermissionSimpleMenus(Long userId, Integer status) {
        long b = System.currentTimeMillis();

        List<MenuInfoSimplePO> pos = this.menuInfoMapper.findUserPermissionFlowSimpleMenus(userId, status);

        long e = System.currentTimeMillis();
        log.info("==> 从库中查询用户流程权限菜单， user id={}, status={}, time={}ms", userId, status, e - b);

        return pos;
    }

    /**
     * 查询流程权限表中的操作对应的菜单
     * @param userId
     * @return
     */
    private List<MenuInfoPO> findUserFlowPermissionMenu(Long userId, Integer status) {
        long b = System.currentTimeMillis();
        log.info("findFlowPermissionMenu==== user id={}, status={}", userId, status);

        List<MenuInfoPO> pos = this.menuInfoMapper.findUserPermissionFlowMenus(userId, status);

        long e = System.currentTimeMillis();
        log.info("findFlowPermissionMenu==== user id={}, status={}, time={}ms", userId, status, e - b);

        return pos;
    }

    private List<MenuInfoPO> getMenusByLayRec(List<MenuInfoSimplePO> menus) {
        //获得所有id
        List<MenuInfoPO> result = new LinkedList<>();
        if (!CollectionUtils.isEmpty(menus)) {
            long b = System.currentTimeMillis();

            Set<Long> ids = new HashSet<>();
            menus.parallelStream().forEach(m -> ids.addAll(m.getFullPathIds()));

            long e = System.currentTimeMillis();
            log.info("==> 根据lay_rec获取所有菜单id，{}ms", e - b);
            //
            return getMenusByIds(new ArrayList<>(ids));
        }
        return result;
    }

    private List<MenuSuposPO> getSuposMenusByLayRec(List<MenuInfoSimplePO> menus) {
        //获得所有id
        List<MenuSuposPO> result = new LinkedList<>();
        if (!CollectionUtils.isEmpty(menus)) {
            long b = System.currentTimeMillis();

            Set<Long> ids = new HashSet<>();
            menus.parallelStream().forEach(m -> ids.addAll(m.getFullPathIds()));

            long e = System.currentTimeMillis();
            log.info("==> 根据lay_rec获取所有菜单id，{}ms", e - b);
            //
            return getSuposMenusByIds(new ArrayList<>(ids));
        }
        return result;
    }

    private List<MenuInfoPO> getMenusByIds(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            int batch = ids.size() / 1000;
            if (batch == 0) {
                return this.menuInfoMapper.findMenusByIds(new ArrayList<>(ids));
            } else {
                List<List<Long>> batchIds = new ArrayList<>();
                for (int i = 0; i < batch; i++) {
                    batchIds.add(ids.subList(i * 1000, i * 1000 + 1000));
                }
                if (ids.size() % 1000 > 0) {
                    batchIds.add(ids.subList(batch * 1000, ids.size()));
                }
                return this.menuInfoMapper.findMenusByBatchIds(batchIds);
            }
        }

        return new ArrayList<>();
    }

    private List<MenuSuposPO> getSuposMenusByIds(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            int batch = ids.size() / 1000;
            if (batch == 0) {
                return this.menuInfoMapper.findSuposMenusByIds(new ArrayList<>(ids));
            } else {
                List<List<Long>> batchIds = new ArrayList<>();
                for (int i = 0; i < batch; i++) {
                    batchIds.add(ids.subList(i * 1000, i * 1000 + 1000));
                }
                if (ids.size() % 1000 > 0) {
                    batchIds.add(ids.subList(batch * 1000, ids.size()));
                }
                return this.menuInfoMapper.findSuposMenusByBatchIds(batchIds);
            }
        }

        return new ArrayList<>();
    }

    /**
     * 通过分解菜单的layRec 获取菜单的所有上层节点
     * 返回平铺结构
     */
    public List<MenuInfoPO> getMenuListByLayRec(List<MenuInfoPO> childMenus) {
        if (CollectionUtils.isEmpty(childMenus)) {
            return new ArrayList<>();
        }
        // 根据lay_rec的值计算出所有菜单的ID
        Set<Long> menuIds = new HashSet<>();
        childMenus.forEach(m -> {
            if (StringUtils.isNotEmpty(m.getLayRec())) {
                menuIds.addAll(Stream.of(m.getLayRec().split("-")).map(Long::valueOf).collect(Collectors.toList()));
            }
            menuIds.add(m.getId());
        });
        // 根据计算得出的id来查询菜单
        int batch = menuIds.size() / 1000;
        if (batch == 0) {
            return this.menuInfoMapper.findMenusByIds(new ArrayList<>(menuIds));
        } else {
            return this.getMenusByIds(new ArrayList<>(menuIds));
        }
    }


    /**
     * @description: 查询菜单父菜单
     * @param: childMenus
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    public List<MenuInfoPO> findParentMenu(List<MenuInfoPO> childMenus) {
        Set<Long> removeId = new HashSet<>();
        //查询所有菜单
        List<MenuInfoPO> all_menus = this.filter(menuInfoMapper.findMenus(new MenuInfoQuery()));
        //构造map,提高查询效率
        Map<Long, MenuInfoPO> all_menus_map = all_menus.stream().collect(Collectors.toMap(MenuInfoPO::getId, menuInfoPO -> menuInfoPO));
        Map<Long, MenuInfoPO> childMenus_map = new HashMap<>();
        childMenus.forEach(menuInfoPO -> childMenus_map.put(menuInfoPO.getId(), menuInfoPO));
        for (MenuInfoPO menuInfoPO : childMenus) {
            if (!ObjectUtils.isEmpty(menuInfoPO.getParentId()) && menuInfoPO.getParentId() != -1) {
                this.findParentTree(menuInfoPO, all_menus_map, childMenus_map, removeId);
            }
        }
        //删除childMenus_map里的子菜单,只留父菜单
        removeId.forEach(childMenus_map::remove);
        if (!ObjectUtils.isEmpty(childMenus_map)) {
            return new ArrayList<>(childMenus_map.values());
        }
        return new ArrayList<>();
    }

    //查找父菜单树
    private void findParentTree(MenuInfoPO menuInfoPO, Map<Long, MenuInfoPO> all_menus_map, Map<Long, MenuInfoPO> childMenus_map, Set<Long> removeId) {
        if (ObjectUtils.isEmpty(menuInfoPO.getParentId()) || menuInfoPO.getParentId() == -1) {
            return;
        }
        MenuInfoPO parentMenu = childMenus_map.get(menuInfoPO.getParentId());
        if (ObjectUtils.isEmpty(parentMenu)) {
            parentMenu = all_menus_map.get(menuInfoPO.getParentId());
            if (!parentMenu.getIsHide()) {
                childMenus_map.put(parentMenu.getId(), parentMenu);
            } else {
                menuInfoPO.setIsHide(true);
            }
        }
        if (ObjectUtils.isEmpty(parentMenu.getChildren())) {
            parentMenu.setChildren(new ArrayList<>());
        }
        parentMenu.getChildren().add(menuInfoPO);
        parentMenu.setChildren(this.filter(parentMenu.getChildren()));
        removeId.add(menuInfoPO.getId());
        if (!ObjectUtils.isEmpty(parentMenu.getParentId()) && parentMenu.getParentId() != -1L) {
            this.findParentTree(parentMenu, all_menus_map, childMenus_map, removeId);
        }

    }

    private void setCompanyIds(List<MenuInfoPO> menuInfoPOS) {
        if (ObjectUtils.isEmpty(menuInfoPOS)) {
            return;
        }
        List<Long> ids = menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
        QueryWrapper<MenuInfoCompanyRefPO> queryWrapper = new QueryWrapper<>();
        int batch = ids.size() / 1000;
        if (batch == 0) {
            queryWrapper.in(MenuInfoCompanyRefField.menuinfoId, ids);
        } else {
            for (int i = 0; i < batch; i++) {
                queryWrapper.or().in(MenuInfoCompanyRefField.menuinfoId, ids.subList(i * 1000, i * 1000 + 1000));
            }
            if (ids.size() % 1000 != 0) {
                queryWrapper.or().in(MenuInfoCompanyRefField.menuinfoId, ids.subList(batch * 1000, ids.size()));
            }
        }
        List<MenuInfoCompanyRefPO> menuInfoCompanyRefPOS = menuInfoCompanyRefService.list(queryWrapper);
        if (ObjectUtils.isEmpty(menuInfoCompanyRefPOS)) {
            return;
        }
        Map<Long, List<MenuInfoCompanyRefPO>> mcMap = menuInfoCompanyRefPOS.stream().collect(Collectors.groupingBy(MenuInfoCompanyRefPO::getMenuinfoId));
        Map<Long, List<Long>> cidMap = new HashMap<>();
        mcMap.forEach((id, list) -> {
            cidMap.put(id, list.stream().map(MenuInfoCompanyRefPO::getCompanyId).distinct().collect(Collectors.toList()));
        });
        menuInfoPOS.forEach(menuInfoPO -> {
            menuInfoPO.setCompanyIds(cidMap.get(menuInfoPO.getId()));
        });
    }

    /**
     * @description: 外部APP调用接口
     * @param: json
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/7
     */
    @Override
    public void saveBachUrlByJson(String json) {
        List<JSONObject> menuInfoJsonBOS = JSON.parseArray(json, JSONObject.class);
        //保存MenuInfo
        List<MenuInfoPO> menuInfoPOS = new ArrayList<>();
        menuInfoJsonBOS.forEach(menuInfoJsonBOJSON -> {
            MenuInfoJsonBO menuInfoJsonBO = menuInfoJsonBOJSON.toJavaObject(MenuInfoJsonBO.class);
            MenuInfoPO menuInfoPO = new MenuInfoPO();
            BeanUtils.copyProperties(menuInfoJsonBO, menuInfoPO);
            //数据库里不存在该菜单时 添加
            MenuInfoPO dbMenuInfoPO = menuInfoMapper.selectOne(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.code, menuInfoJsonBO.getCode()));
            //如果该菜单在页面上修改过，就不进行更新，直接返回数据库里的数据
            if (!ObjectUtils.isEmpty(dbMenuInfoPO) && !ObjectUtils.isEmpty(dbMenuInfoPO.getEdited()) && dbMenuInfoPO.getEdited()) {
                menuInfoPOS.add(dbMenuInfoPO);
                return;
            }
            if (!ObjectUtils.isEmpty(dbMenuInfoPO)) {
                //拷贝数据库数据到新建的PO里，忽略JSONObject里存在的键
                BeanUtils.copyProperties(dbMenuInfoPO, menuInfoPO, set2array(menuInfoJsonBOJSON.keySet()));
            } else {
                menuInfoPO.setId(IDGenerator.newInstance().generate().longValue());
            }
            if (!ObjectUtils.isEmpty(menuInfoJsonBO.getParent())) {
                MenuInfoPO parentMenuInfo = getParentMenuInfo(menuInfoJsonBOJSON.getJSONObject(PARENT));
                menuInfoPO.setParentId(parentMenuInfo.getId());
            } else {
                menuInfoPO.setParentId(-1L);
            }
            if (ObjectUtils.isEmpty(menuInfoPO.getModuleCode())) {
                menuInfoPO.setModuleCode(appName);
            }
            menuInfoPOS.add(menuInfoPO);
        });
        //批量设置全路径和助记码
        menuInfoPOS.forEach(menuInfoPO -> {
            MenuInfoPO parentMenuInfo = menuInfoMapper.selectOne(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.id, menuInfoPO.getParentId()));
            String parentFullPath = !ObjectUtils.isEmpty(parentMenuInfo.getFullPath()) ? parentMenuInfo.getFullPath() + HttpConstants.URL_SPLITER : "";
            String parentLayRec = !ObjectUtils.isEmpty(parentMenuInfo.getLayRec()) ? parentMenuInfo.getLayRec() + Constants.LAY_REC_SPLIT : "";
            String parentFullPathName = !ObjectUtils.isEmpty(parentMenuInfo.getFullPathName()) ? parentMenuInfo.getFullPathName() + HttpConstants.URL_SPLITER : "";
            if (menuInfoPO.getParentId().equals(Constants.MENU_LIST_ID)) {
                menuInfoPO.setFullPath(String.valueOf(menuInfoPO.getId()));
                menuInfoPO.setLayRec(String.valueOf(menuInfoPO.getId()));
                menuInfoPO.setFullPathName(menuInfoPO.getName());
            } else {
                menuInfoPO.setFullPath(parentFullPath + menuInfoPO.getId());
                menuInfoPO.setLayRec(parentLayRec + menuInfoPO.getId());
                menuInfoPO.setFullPathName(parentFullPathName + menuInfoPO.getName());
            }
        });
        menuInfoMneCodeService.createMenuInfoMneCodeI18NKey(menuInfoPOS);
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                "batch save",
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        this.saveBatchMenuInfo(menuInfoPOS);
        List<MenuInfoCompanyRefPO> menuInfoCompanyRefPOS = new ArrayList<>();
        List<MenuOperatePO> menuOperatePOS = new ArrayList<>();
        for (int i = 0; i < menuInfoPOS.size(); i++) {
            MenuInfoPO menuInfoPO = menuInfoPOS.get(i);
            Long companyId = menuInfoPO.getCid();
            JSONObject menuInfoJsonBOJSON = menuInfoJsonBOS.get(i);
            List<MenuOperatePO> operatePOS = saveOperateAndUrl(menuInfoPO, menuInfoJsonBOJSON);
            menuOperatePOS.addAll(operatePOS);
            //保存菜单公司关联
            int count = menuInfoCompanyRefService.count(new QueryWrapper<MenuInfoCompanyRefPO>().eq(COMPANY_ID, companyId).eq(MENUINFO_ID, menuInfoPO.getId()));
            if (count == 0) {
                MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
                menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
                menuInfoCompanyRefPO.setCompanyId(companyId);
                menuInfoCompanyRefPOS.add(menuInfoCompanyRefPO);
            }
        }
        menuOperateService.saveOrUpdateBatch(menuOperatePOS);
        //保存菜单公司关联
        menuInfoCompanyRefService.saveBatch(menuInfoCompanyRefPOS);
        //统一设置菜单排序,设置layNo
        setMenuInfoSort();
        //不受控的过滤掉，不需要创建默认操作
        generateDefaultOperate(menuInfoPOS.stream().filter(menuInfoPO -> !menuInfoPO.getNoRestrict()).map(MenuInfoPO::getId).collect(Collectors.toList()));
    }

    /**
     * @description: 保存操作及其关联URL
     * @param: menuInfoPO
     * @param: menuInfoJsonBOJSON
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    private List<MenuOperatePO> saveOperateAndUrl(MenuInfoPO menuInfoPO, JSONObject menuInfoJsonBOJSON) {
        MenuInfoJsonBO menuInfoJsonBO = menuInfoJsonBOJSON.toJavaObject(MenuInfoJsonBO.class);
        //保存MenuOperate
        if (ObjectUtils.isEmpty(menuInfoJsonBO.getMenuOperates())) {
            return new ArrayList<>();
        }
        List<MenuOperatePO> menuOperatePOS = menuInfoJsonBOJSON.getJSONArray(MENUOPERATES).toJavaList(JSONObject.class).stream().map(menuOperateJsonBOJSON -> {
            MenuOperateJsonBO menuOperateJsonBO = menuOperateJsonBOJSON.toJavaObject(MenuOperateJsonBO.class);
            MenuOperatePO menuOperatePO = new MenuOperatePO();
            BeanUtils.copyProperties(menuOperateJsonBO, menuOperatePO);
            menuOperatePO.setApp(menuInfoPO.getApp());
            MenuOperatePO dbMenuOperatePO = menuoperateMapper.selectOne(new QueryWrapper<MenuOperatePO>().eq(MenuOperateField.code, menuOperateJsonBO.getCode()));
            if (!ObjectUtils.isEmpty(dbMenuOperatePO)) {
                //拷贝数据库数据到新建的PO里，忽略JSONObject里存在的键
                BeanUtils.copyProperties(dbMenuOperatePO, menuOperatePO, set2array(menuOperateJsonBOJSON.keySet()));
            }
            menuOperatePO.setMenuinfoId(menuInfoPO.getId());
            return menuOperatePO;
        }).collect(Collectors.toList());
        //批量保存操作
        for (int j = 0; j < menuOperatePOS.size(); j++) {
            MenuOperateJsonBO menuOperateJsonBO = menuInfoJsonBO.getMenuOperates().get(j);
            JSONObject menuOperateJsonBOJSON = menuInfoJsonBOJSON.getJSONArray(MENUOPERATES).toJavaList(JSONObject.class).get(j);
            //保存url
            if (!ObjectUtils.isEmpty(menuOperateJsonBO.getUrls())) {
                List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS = menuOperateJsonBOJSON.getJSONArray("urls").toJavaList(JSONObject.class).stream().map(menuOperateUrlJsonBOJSON -> {
                    MenuOperateUrlJsonBO menuOperateUrlJsonBO = menuOperateUrlJsonBOJSON.toJavaObject(MenuOperateUrlJsonBO.class);
                    MenuOperateCodeUrlRefPO menuOperateCodeUrlRefPO = new MenuOperateCodeUrlRefPO();
                    BeanUtils.copyProperties(menuOperateUrlJsonBO, menuOperateCodeUrlRefPO);
                    menuOperateCodeUrlRefPO.setUrl(filterPathParams(menuOperateCodeUrlRefPO.getUrl()));
                    MenuOperateCodeUrlRefPO dbMenuOperateCodeUrlRefPO = menuOperateCodeUrlRefMapper.selectOne(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq("url", menuOperateCodeUrlRefPO.getUrl()).eq("METHOD_TYPE", menuOperateCodeUrlRefPO.getMethodType()).eq("MENUOPERATE_CODE", menuOperateCodeUrlRefPO.getMenuoperateCode()).eq("app", menuOperateCodeUrlRefPO.getApp()));
                    menuOperateCodeUrlRefPO.setRegMatch(menuOperateCodeUrlRefPO.getUrl().contains("/[^/^]"));
                    menuOperateCodeUrlRefPO.setImportType(1);
                    if (!ObjectUtils.isEmpty(dbMenuOperateCodeUrlRefPO)) {
                        //拷贝数据库数据到新建的PO里，忽略JSONObject里存在的键
                        BeanUtils.copyProperties(dbMenuOperateCodeUrlRefPO, menuOperateCodeUrlRefPO, set2array(menuOperateUrlJsonBOJSON.keySet()));
                    }
                    return menuOperateCodeUrlRefPO;
                }).collect(Collectors.toList());
                //批量保存url
                menuOperateCodeUrlRefService.saveOrUpdateBatch(menuOperateCodeUrlRefPOS);
            }
        }
        return menuOperatePOS;
    }

    /**
     * @description: 创建更新或查询父菜单
     * @param: menuInfoJsonBOJSON
     * @return: com.supcon.supfusion.rbac.dao.po.MenuInfoPO
     * @author: 袁阳
     * @date: 2020/8/31
     */
    private MenuInfoPO getParentMenuInfo(JSONObject menuInfoJsonBOJSON) {
        MenuInfoJsonBO menuInfoJsonBO = menuInfoJsonBOJSON.toJavaObject(MenuInfoJsonBO.class);
        MenuInfoPO menuInfoPO = new MenuInfoPO();
        BeanUtils.copyProperties(menuInfoJsonBO, menuInfoPO);
        Long cid = menuInfoPO.getCid();
        //数据库里不存在该菜单时 添加
        MenuInfoPO dbMenuInfoPO = menuInfoMapper.selectOne(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.code, menuInfoJsonBO.getCode()));
        if (!ObjectUtils.isEmpty(dbMenuInfoPO)) {
            //拷贝数据库数据到新建的PO里，忽略JSONObject里存在的键
            BeanUtils.copyProperties(dbMenuInfoPO, menuInfoPO, set2array(menuInfoJsonBOJSON.keySet()));
        }
        //被修改过的菜单不需要更新
        if (!ObjectUtils.isEmpty(menuInfoPO.getEdited()) && menuInfoPO.getEdited()) {
            return menuInfoPO;
        }
        if (!ObjectUtils.isEmpty(menuInfoJsonBO.getParent())) {
            MenuInfoPO parentMenuInfo = getParentMenuInfo(menuInfoJsonBOJSON.getJSONObject(PARENT));
            menuInfoPO.setParentId(parentMenuInfo.getId());
        } else {
            menuInfoPO.setParentId(Constants.MENU_LIST_ID);
        }
        if (ObjectUtils.isEmpty(menuInfoPO.getModuleCode())) {
            menuInfoPO.setModuleCode(appName);
        }
        //保存菜单 生成ID
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                menuInfoPO.getCode(),
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        saveOrUpdate(menuInfoPO);
        //设置fullpath
        MenuInfoPO parentMenuInfo = menuInfoMapper.selectOne(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.id, menuInfoPO.getParentId()));
        String parentFullPath = !ObjectUtils.isEmpty(parentMenuInfo.getFullPath()) ? parentMenuInfo.getFullPath() + HttpConstants.URL_SPLITER : "";
        String parentFullPathName = !ObjectUtils.isEmpty(parentMenuInfo.getFullPathName()) ? parentMenuInfo.getFullPathName() + HttpConstants.URL_SPLITER : "";
        String parentLayRec = !ObjectUtils.isEmpty(parentMenuInfo.getLayRec()) ? parentMenuInfo.getLayRec() + Constants.LAY_REC_SPLIT : "";
        //不拼接 -1 菜单的ID
        if (menuInfoPO.getParentId().equals(Constants.MENU_LIST_ID)) {
            menuInfoPO.setFullPath(String.valueOf(menuInfoPO.getId()));
            menuInfoPO.setLayRec(String.valueOf(menuInfoPO.getId()));
            menuInfoPO.setFullPathName(menuInfoPO.getName());
        } else {
            menuInfoPO.setFullPath(parentFullPath + menuInfoPO.getId());
            menuInfoPO.setFullPathName(parentFullPathName + menuInfoPO.getName());
            menuInfoPO.setLayRec(parentLayRec + menuInfoPO.getId());
        }
        //设置助记码
        menuInfoMneCodeService.createMenuInfoMneCodeI18NKey(Collections.singletonList(menuInfoPO));
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                menuInfoPO.getCode(),
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        this.saveMenuInfo(menuInfoPO);
        //保存MenuOperate
        saveOperateAndUrl(menuInfoPO, menuInfoJsonBOJSON);
        List<MenuInfoCompanyRefPO> menuInfoCompanyRefPOS = new ArrayList<>();
        //保存菜单公司关联
        int count = menuInfoCompanyRefService.count(new QueryWrapper<MenuInfoCompanyRefPO>().eq(COMPANY_ID, cid).eq(MENUINFO_ID, menuInfoPO.getId()));
        if (count == 0) {
            MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
            menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
            menuInfoCompanyRefPO.setCompanyId(cid);
            menuInfoCompanyRefPO.setAppId(menuInfoPO.getApp());
            menuInfoCompanyRefPOS.add(menuInfoCompanyRefPO);
        }
        //保存菜单公司关联
        menuInfoCompanyRefService.saveOrUpdateBatch(menuInfoCompanyRefPOS);
        return menuInfoPO;
    }

    /**
     * @description: 外部APP调用接口导入菜单、操作、url对应数据
     * @param: xml
     * @return: void
     * @author: fjh
     * @date: 2020/7/8
     */
    public void saveBachUrlByXml(String xml, Long cid) throws Exception {
        Long companyId = ObjectUtils.isEmpty(cid) ? UserContext.getUserContext().getCompanyId() : cid;
        Document doc = DocumentHelper.parseText(xml);
        Element menuInfosE = doc.getRootElement();
        List<MenuOperatePO> menuOperatePOS = null;
        List<MenuInfoPO> menuInfoPOs = new ArrayList<MenuInfoPO>();
        for (Iterator menuInfosIt = menuInfosE.elementIterator("menuInfo"); menuInfosIt.hasNext(); ) {
            MenuInfoPO menuInfoPO = null;
            Element menuInfoE = (Element) menuInfosIt.next();
            String code = getTagContent(menuInfoE.asXML(), "code");
            menuInfoPO = menuInfoMapper.selectOne(new QueryWrapper<MenuInfoPO>().eq("code", code));
            if (null == menuInfoPO) {
                menuInfoPO = new MenuInfoPO();
            } else {
                menuOperatePOS = menuOperateService.list(new QueryWrapper<MenuOperatePO>().eq("menuinfo_id", menuInfoPO.getId()));
            }
            List<MenuOperatePO> hasEditMenuOperatePOS = new ArrayList<MenuOperatePO>();
            for (Iterator menuInfoIt = menuInfoE.elementIterator(); menuInfoIt.hasNext(); ) {
                Element menuInfoPropertyE = (Element) menuInfoIt.next();
                String menuInfoPropertyName = menuInfoPropertyE.getName();
                if ("parent".equals(menuInfoPropertyName)) {
                    if (menuInfoPO.getId() == null) {
                        if (menuInfoPropertyE.elements().size() > 0) {
                            resImportMenuInfo(menuInfoPO, menuInfoPropertyE);
                        }
                    }
                    // 操作不处理，已有菜单的操作，在模块发布时更新
                } else if ("menuOperates".equals(menuInfoPropertyName)) {
                    for (Iterator menuOperatesIt = menuInfoPropertyE.elementIterator("menuOperate"); menuOperatesIt.hasNext(); ) {
                        Element menuOperateE = (Element) menuOperatesIt.next();
                        MenuOperatePO mo = null;
                        code = getTagContent(menuOperateE.asXML(), "code");
                        if (menuOperatePOS != null && !menuOperatePOS.isEmpty()) {
                            for (MenuOperatePO op : menuOperatePOS) {
                                if (code != null && code.equals(op.getCode())) {
                                    mo = op;
                                    break;
                                }
                            }
                        }
                        if (mo == null) {
                            mo = new MenuOperatePO();
                        }
                        for (Iterator menuOperateIt = menuOperateE.elementIterator(); menuOperateIt.hasNext(); ) {
                            Element menuOperatePropertyE = (Element) menuOperateIt.next();
                            String menuOperatePropertyName = menuOperatePropertyE.getName();
                            String menuOperatePropertyValue = menuOperatePropertyE.getTextTrim();
                            if (!"cid".equals(menuOperatePropertyName) && !"menuOperateCodeUrlRefs".equals(menuOperatePropertyName)) {
                                if (mo.getId() == null) {
                                    if (menuOperatePropertyValue.length() > 0) {
                                        PropertyUtils.setProperty(mo, menuOperatePropertyName,
                                                getObjectValue(mo, menuOperatePropertyName, menuOperatePropertyValue));
                                    }
                                }
                            } else if ("menuOperateCodeUrlRefs".equals(menuOperatePropertyName)) {
                                List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS = menuOperateCodeUrlRefService.list(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq("menuoperate_code", mo.getCode()));
                                List<MenuOperateCodeUrlRefPO> hasEditMenuOperateCodeUrlRefS = new ArrayList<MenuOperateCodeUrlRefPO>();
                                MenuOperateCodeUrlRefPO mourl = null;
                                for (Iterator menuOperateCodeUrlRefsIt = menuOperatePropertyE.elementIterator("menuOperateCodeUrlRef"); menuOperateCodeUrlRefsIt.hasNext(); ) {
                                    Element menuOperateCodeUrlResfE = (Element) menuOperateCodeUrlRefsIt.next();
                                    String url = getTagContent(menuOperateCodeUrlResfE.asXML(), "url");
                                    if (null != menuOperateCodeUrlRefPOS && !menuOperateCodeUrlRefPOS.isEmpty()) {
                                        for (MenuOperateCodeUrlRefPO menuOperateCodeUrlRef : menuOperateCodeUrlRefPOS) {
                                            if (url != null && url.equals(menuOperateCodeUrlRef.getUrl())) {
                                                mourl = menuOperateCodeUrlRef;
                                                break;
                                            }
                                        }
                                    }
                                    if (null == mourl) {
                                        mourl = new MenuOperateCodeUrlRefPO();
                                    }
                                    for (Iterator menuOperateCodeUrlRefIt = menuOperateCodeUrlResfE.elementIterator(); menuOperateCodeUrlRefIt.hasNext(); ) {
                                        Element menuOperateCodeUrlRefPropertyE = (Element) menuOperateCodeUrlRefIt.next();
                                        String menuOperateCodeUrlRefName = menuOperateCodeUrlRefPropertyE.getName();
                                        String menuOperateCodeUrlRefPropertyValue = menuOperateCodeUrlRefPropertyE.getTextTrim();
                                        if (mourl.getId() == null) {
                                            if (menuOperateCodeUrlRefPropertyValue.length() > 0) {
                                                PropertyUtils.setProperty(mourl, menuOperateCodeUrlRefName,
                                                        getObjectValue(mourl, menuOperateCodeUrlRefName, menuOperateCodeUrlRefPropertyValue));
                                            }
                                        }
                                    }
                                    mourl.setMenuoperateCode(mo.getCode());
                                    hasEditMenuOperateCodeUrlRefS.add(mourl);
                                }
                                menuOperateCodeUrlRefService.saveOrUpdateBatch(hasEditMenuOperateCodeUrlRefS);
                            } else {
                                mo.setCid(companyId);
                            }
                        }
                        hasEditMenuOperatePOS.add(mo);
                    }
                } else if (!"cid".equals(menuInfoPropertyName) && !"groupOnly".equals(menuInfoPropertyName)) {
                    // MenuInfo简单属性
                    if (null == menuInfoPO.getId()) {
                        String menuInfoPropertyValue = menuInfoPropertyE.getTextTrim();
                        if (menuInfoPropertyValue.length() > 0) {
                            PropertyUtils.setProperty(menuInfoPO, menuInfoPropertyName,
                                    getObjectValue(menuInfoPO, menuInfoPropertyName, menuInfoPropertyValue));
                        }
                    }
                } else {
                    menuInfoPO.setCid(companyId);
                    //保存菜单公司关联
                    int count = menuInfoCompanyRefService.count(new QueryWrapper<MenuInfoCompanyRefPO>().eq("COMPANY_ID", cid).eq("MENUINFO_ID", menuInfoPO.getId()));
                    if (count == 0) {
                        MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
                        menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
                        menuInfoCompanyRefPO.setCompanyId(cid);
                        menuInfoCompanyRefPO.setAppId(menuInfoPO.getApp());
                        menuInfoCompanyRefService.saveOrUpdate(menuInfoCompanyRefPO);
                    }
                    //保存菜单公司关联
                }
            }
            log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                    menuInfoPO.getCode(),
                    UserContext.getUserContext().getUserName(),
                    RpcContext.getContext().getFromServiceName(),
                    RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
            saveOrUpdate(menuInfoPO);
            for (MenuOperatePO mop : hasEditMenuOperatePOS) {
                mop.setMenuinfoId(menuInfoPO.getId());
            }
            menuOperateService.saveOrUpdateBatch(hasEditMenuOperatePOS);
            menuInfoPOs.add(menuInfoPO);
        }
        generateDefaultOperate(menuInfoPOs.stream().filter(menuInfoPO -> !menuInfoPO.getNoRestrict()).map(MenuInfoPO::getId).collect(Collectors.toList()));
    }

    /**
     * @description: 启停用菜单
     * @param: menuInfo
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void modifyMenuEnableStatus(MenuInfoPO menuInfo) {
        MenuInfoPO menuManage = getOne(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.code, "menumanage"));

        if (!menuInfo.getEnable() && ((Arrays.asList(menuManage.getLayRec().split("-")).contains(String.valueOf(menuInfo.getId())) || menuManage.getId().equals(menuInfo.getId())))) {
            throw new MenuException(MenuErrorEnum.CAN_NOT_DELETE_MENU_MANAGE);
        }
        //停用菜单菜单
        MenuInfoPO menuInfoPO = new MenuInfoPO();
        menuInfoPO.setEnable(menuInfo.getEnable());
        //布尔值有默认值，mybatis plus会将非空或非null的值进行更新，所以需要置空
        //更新 %/{id} %/{id}/% {id}%
        log.info("modifyMenuEnableStatus.updateMenuInfo==menuInfoPO={}===================================================", menuInfoPO);
        this.updateMenuInfo(menuInfoPO, new UpdateWrapper<MenuInfoPO>().and(menuInfoPOUpdateWrapper -> menuInfoPOUpdateWrapper.likeLeft(MenuInfoField.layRec, "-" + menuInfo.getId()).or().like(MenuInfoField.layRec, "-" + menuInfo.getId() + "-").or().likeRight(MenuInfoField.layRec, menuInfo.getId())));
    }

    /**
     * 处理菜单父节点
     *
     * @param mip
     * @param e
     * @throws Exception
     */
    private void resImportMenuInfo(MenuInfoPO mip, Element e) throws Exception {
        String code = getTagContent(e.asXML(), "code");
        MenuInfoPO parent = menuInfoMapper.selectOne(new QueryWrapper<MenuInfoPO>().eq("code", code));
        if (parent == null) {
            parent = new MenuInfoPO();
        }
        for (Iterator menuInfoIt = e.elementIterator(); menuInfoIt.hasNext(); ) {
            Element menuInfoPropertyE = (Element) menuInfoIt.next();
            String menuInfoPropertyName = menuInfoPropertyE.getName();
            if ("parent".equals(menuInfoPropertyName)) {
                if (parent.getId() == null) {
                    if (menuInfoPropertyE.elements().size() > 0) {
                        resImportMenuInfo(parent, menuInfoPropertyE);
                    }
                }
            } else {
                // MenuInfo简单属性
                if (null == parent.getId()) {
                    String menuInfoPropertyValue = menuInfoPropertyE.getTextTrim();
                    if (menuInfoPropertyValue.length() > 0 && !"cid".equals(menuInfoPropertyName)) {
                        if (parent.getId() == null) {
                            PropertyUtils.setProperty(parent, menuInfoPropertyName, getObjectValue(parent, menuInfoPropertyName, menuInfoPropertyValue));
                        }
                    }
                }
            }
        }
        if (null == parent.getParentId()) {
            parent.setParentId(-1l);//创建的父节点默认父节点id为-1
        }
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                parent.getCode(),
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        saveOrUpdate(parent);
        if (mip.getCode() != null && !mip.getCode().equals(parent.getCode())) {
            mip.setParentId(parent.getId());
        }
    }

    /**
     * 截取xml中某个节点的值
     *
     * @param content xml内容
     * @param tagName 节点名称
     * @return
     * @throws Exception
     */
    public String getTagContent(String content, String tagName) throws Exception {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        String cdataStart = "<!\\[CDATA\\[";
        String cdataEnd = "\\]\\]>";
        String returnContent = StringUtils.EMPTY;

        String str = content;
        if (str == null) {
            return null;
        }
        int index = str.indexOf(startTag);
        if (index >= 0) {
            str = str.substring(index + startTag.length());
            index = str.indexOf(endTag);
            if (index >= 0) {
                returnContent = str.substring(0, index).trim();
                returnContent = returnContent.replaceAll(cdataStart, StringUtils.EMPTY);
                returnContent = returnContent.replaceAll(cdataEnd, StringUtils.EMPTY);
            }
        }
        return returnContent;
    }

    /**
     * 获取对应属性的值
     *
     * @param o
     * @param name
     * @param val
     * @return
     * @throws Exception
     */
    private Object getObjectValue(Object o, String name, String val) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class type = PropertyUtils.getPropertyType(o, name);
        if (null == type) {
            return null;
        }
        if (type.isEnum()) {
            return Enum.valueOf(type, val);
        }
        return ConvertUtils.convert(val, type);
    }

    /**
     * @description: 创建默认操作
     * @param: menuInfoIds
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void generateDefaultOperate(List<Long> menuInfoIds) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("mi." + MenuInfoField.id, menuInfoIds);
        queryWrapper.groupBy("mi." + MenuInfoField.id, "mi." + MenuInfoField.code, "mi." + MenuInfoField.url, "mi." + MenuInfoField.cid);
        queryWrapper.having("sum(mo." + MenuOperateField.defaultOperate + ") = {0} or sum(mo." + MenuOperateField.defaultOperate + ") is null", 0);
        List<Map<String, Object>> list = menuoperateMapper.getNoneDefaultOperateMenu(queryWrapper);
        List<MenuOperatePO> menuOperatePOS = list.stream().map(map -> {
            MenuOperatePO menuOperatePO = new MenuOperatePO();
            menuOperatePO.setCode(map.get("code") + "_default");
            menuOperatePO.setDefaultOperate(true);
            menuOperatePO.setMenuinfoId((long) map.get("id"));
            menuOperatePO.setUrl(String.valueOf(map.get("url")));
            if (null != map.get("cid")) {
                menuOperatePO.setCid((long) map.get("cid"));
            }
            menuOperatePO.setEnableNorestrict(true);
            menuOperatePO.setName("rbac.MENU_OPERATE_DEFAULT_OPERATE");
            menuOperatePO.setVersion(0);
            menuOperatePO.setNameDisplay("-");
            return menuOperatePO;
        }).collect(Collectors.toList());
        menuOperateService.saveBatch(menuOperatePOS);
    }

    @Override
    public Page<MenuInfoPO> findMenusByKeyword(String keyword, Integer size, String restrict, String enable) {
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        //不查询默认顶级菜单
        queryWrapper.notIn("mi." + MenuInfoField.id, -1);
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())) {
                queryWrapper.apply("mm." + MenuInfoMneCodeField.mneCode + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR);
            } else {
                queryWrapper.like("mm." + MenuInfoMneCodeField.mneCode, dbStringUtil.getString(keyword));
            }
        }
        //restrict 不为空时为权限左边菜单树查询，仅不查询无限制菜单
        if (!ObjectUtils.isEmpty(restrict)) {
            queryWrapper.eq("mi." + MenuInfoField.noRestrict, 0);
        }
        //enable 不为空时为supFusion菜单菜单树查询，仅查询enable为true的菜单
        if (!ObjectUtils.isEmpty(enable)) {
            queryWrapper.eq("mi." + MenuInfoField.enable, 1);
        }
        queryWrapper.eq("mi.valid", "1");
        queryWrapper.and(queryWrapper1 -> queryWrapper1.eq("mcf." + MenuInfoCompanyRefField.companyId, UserContext.getUserContext().getCompanyId()).or().eq("mcf." + MenuInfoCompanyRefField.companyId, -1L).or().eq("mi." + MenuInfoField.cid, UserContext.getUserContext().getCompanyId()));
        queryWrapper.groupBy("mi." + MenuInfoField.id, "mi." + MenuInfoField.code, "mi." + MenuInfoField.name, "mi." + MenuInfoField.app);
        Page<MenuInfoPO> page = new Page<>(1, ObjectUtils.isEmpty(size) ? 10 : size);
        Page<MenuInfoPO> menusByKeyword = menuInfoMapper.findMenusByKeyword(page, queryWrapper);
        setCompanyIds(menusByKeyword.getRecords());
        return menusByKeyword;
    }

    @Override
    public Page<MenuInfoPO> findMenuConfigureByKeyword(String keyword, Integer size, String restrict, String enable) {
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        //菜单配置中查询运行期菜单
        queryWrapper.eq("mi." + MenuInfoField.status, 1);
        //不查询默认顶级菜单
        queryWrapper.notIn("mi." + MenuInfoField.id, -1);
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())) {
                queryWrapper.apply("mm." + MenuInfoMneCodeField.mneCode + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR);
            } else {
                queryWrapper.like("mm." + MenuInfoMneCodeField.mneCode, dbStringUtil.getString(keyword));
            }
        }
        //restrict 不为空时为权限左边菜单树查询，仅不查询无限制菜单
        if (!ObjectUtils.isEmpty(restrict)) {
            queryWrapper.eq("mi." + MenuInfoField.noRestrict, 0);
        }
        //enable 不为空时为supFusion菜单菜单树查询，仅查询enable为true的菜单
        if (!ObjectUtils.isEmpty(enable)) {
            queryWrapper.eq("mi." + MenuInfoField.enable, 1);
        }
        queryWrapper.eq("mi.valid", "1");
        queryWrapper.and(queryWrapper1 -> queryWrapper1.eq("mcf." + MenuInfoCompanyRefField.companyId, UserContext.getUserContext().getCompanyId()).or().eq("mcf." + MenuInfoCompanyRefField.companyId, -1L).or().eq("mi." + MenuInfoField.cid, UserContext.getUserContext().getCompanyId()));
        queryWrapper.groupBy("mi." + MenuInfoField.id, "mi." + MenuInfoField.code, "mi." + MenuInfoField.name, "mi." + MenuInfoField.app);
        Page<MenuInfoPO> page = new Page<>(1, ObjectUtils.isEmpty(size) ? 10 : size);
        Page<MenuInfoPO> menusByKeyword = menuInfoMapper.findMenusByKeyword(page, queryWrapper);
        setCompanyIds(menusByKeyword.getRecords());
        return menusByKeyword;
    }

    /**
     * 获取所有运行期菜单
     *
     * @return
     */
    @Override
    public List<MenuInfoPO> queryRuntimeMenus(String appId) {
        MenuInfoQuery menuInfoQuery = new MenuInfoQuery();
        menuInfoQuery.setApp(appId);
        if (StringUtils.isNoneBlank(appId)) {
            MenuInfoPO menuInfoPO = getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", appId));
            if (Objects.isNull(menuInfoPO)) {
                return new ArrayList<>();
            }
            menuInfoQuery.setLayRec(menuInfoPO.getLayRec() + "%");
        }
        menuInfoQuery.setCid(UserContext.getUserContext().getCompanyId());
        menuInfoQuery.setEnable(true);
        List<MenuInfoPO> list = menuInfoMapper.findMenus(menuInfoQuery);
        List<MenuInfoPO> result = findParentMenu(list);
        sortMenuListAndI18n(result);
        return result;
    }

    /**
     * @description: set转string[]
     * @param: set
     * @return: java.lang.String[]
     * @author: 袁阳
     * @date: 2020/7/10
     */
    public static String[] set2array(Set<String> set) {
        String[] result = new String[set.size()];
        return set.toArray(result);
    }

    /**
     * @description: 路由参数类型URL转成正则
     * @param: url
     * @return: java.lang.String
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public String filterPathParams(String url) {
        return url.replaceAll("/\\{[^\\}]+\\}", "/[^\\/\\^]+");
    }

    /**
     * @description: 根据编码把菜单的valid设为1
     * @param: code
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void recoverMenuInfo(String code) {
        menuInfoMapper.recoverMenuInfo(new QueryWrapper<MenuInfoPO>().eq("CODE", code));
    }

    /**
     * @description: 根据模块ID查询模块名
     * @param: moduleId
     * @return: java.lang.String
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public String getModuleName(String moduleId) {
        ModuleDTO module = moduleAdapter.getModule(moduleId);
        if (!ObjectUtils.isEmpty(module)) {
            return module.getModuleName();
        } else {
            return null;
        }
    }

    @Override
    public Collection<ModuleDTO> queryModules() {
        return moduleAdapter.queryModules();
    }

    @Override
    public void updateMenuInfoByEntityCode(String entityCode) {
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MenuInfoField.entityCode, entityCode);
        queryWrapper.eq(MenuInfoField.valid, 1);
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                entityCode,
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        menuInfoMapper.updateMenuInfoByEntityCode(queryWrapper);
    }

    @Override
    public JSONObject checkMenu(String url) {
        if (StringUtils.isBlank(url)) {
            throw new MenuException(MenuErrorEnum.CAN_NOT_DELETE_MENU_MANAGE);
        }

        JSONObject jsonObject = new JSONObject();
        // 为个人信息管理下面菜单添加白名单
        if ("/personal/personal-msg".equalsIgnoreCase(url) || "/personal/password".equalsIgnoreCase(url) || "/selfstationletter".equalsIgnoreCase(url)) {
            Integer result = 2;
            jsonObject.put("result", result);
            return jsonObject;
        }
        Integer result = 1;
        Long userId = UserContext.getUserContext().getUserId();
        Long companyId = UserContext.getUserContext().getCompanyId();
        MenuInfoQuery menuInfoQuery = new MenuInfoQuery();
        menuInfoQuery.setUserId(userId);
        menuInfoQuery.setCid(companyId);
        menuInfoQuery.setRoute(url);
        List<MenuInfoPO> userMenuInfoList = menuInfoMapper.findUserMenus(menuInfoQuery);
        userMenuInfoList = this.filter(userMenuInfoList);
        if (!CollectionUtils.isEmpty(userMenuInfoList)) {
            result = 2;
            log.info("userMenuInfoList is not empty=========================================> result = 2");
        } else {
            QueryWrapper<MenuInfoPO> queryUrl = new QueryWrapper<>();
            queryUrl.eq("up.USER_ID", userId);
            queryUrl.eq("up.CID", companyId);
            List<String> urlList = userPermissionMapper.queryUrlList(queryUrl);
            if (CollectionUtils.isEmpty(urlList)) {
                result = 1;
                log.info("urlList is empty=========================================> result = 1");
            } else {
                for (String urlStr : urlList) {
                    if (StringUtils.isNoneBlank(urlStr)) {
                        boolean matches = url.matches(urlStr);
                        if (matches) {
                            result = 2;
                            log.info("urlList is not empty=========================================> result = 2");
                            break;
                        }
                    }
                }
            }
        }
        jsonObject.put("result", result);
        return jsonObject;
    }

    /**
     * @description: 统一设置菜单排序, 设置layNo
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/23
     */
    private void setMenuInfoSort() {
        //查询所有菜单
        List<MenuInfoPO> menuInfoPOS = menuInfoMapper.selectList(new QueryWrapper<MenuInfoPO>());
        //统一排序 根据父节点ID分组 每组排序区分
        Map<Long, List<MenuInfoPO>> collect = menuInfoPOS.stream().filter(menuInfoPO -> menuInfoPO.getParentId() != null && menuInfoPO.getLayRec() != null).collect(Collectors.groupingBy(MenuInfoPO::getParentId));
        collect.forEach((key, list) -> {
            list.sort(Comparator.comparing(MenuInfoPO::getSort, Comparator.nullsLast(Double::compareTo)));
            //排序从1.0开始
            int index = 0;
            for (MenuInfoPO menuInfoPO : list) {
                double sort = 1.0;
                if (menuInfoPO.getSort() == null) {
                    if (index - 1 >= 0) {
                        sort = list.get(index - 1).getSort() + 1.0;
                    }
                    menuInfoPO.setSort(sort);
                }
                index += 1;
                String[] split = menuInfoPO.getLayRec().split(Constants.LAY_REC_SPLIT);
                menuInfoPO.setLayNo(split.length);
            }
        });
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                "batch save",
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        this.saveBatchMenuInfo(menuInfoPOS);
    }

    @Override
    public void updateMenuInfo(MenuInfoPO menuInfoPO) {
        checkParentMenuCompany(menuInfoPO);
        checkChildMenuCompany(menuInfoPO);
        UpdateWrapper<MenuInfoPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("code", menuInfoPO.getCode());
        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                menuInfoPO.getCode(),
                UserContext.getUserContext().getUserName(),
                RpcContext.getContext().getFromServiceName(),
                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
        update(menuInfoPO, updateWrapper);
    }

    @Override
    public void cascadeDeleteMenuById(List<MenuInfoPO> menuInfoPOList) {
        deleteMenuRelevance(menuInfoPOList);
    }

    @Override
    public void deletePhysics(List<Long> ids) {
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in(MenuInfoField.id, ids);
        menuInfoMapper.deletePhysics(queryWrapper);
    }

    @Override
    public List<MenuInfoPO> findUserMenuList(Long userId) {
        MenuInfoQuery m = new MenuInfoQuery();
        m.setUserId(userId);
        m.setCid(UserContext.getUserContext().getCompanyId());
        List<MenuInfoPO> userMenus = menuInfoMapper.findUserMenus(m);
        userMenus = this.filter(userMenus);
        List<MenuInfoPO> menus = new ArrayList<>();
        if (!ObjectUtils.isEmpty(userMenus)) {
            menus.addAll(userMenus);
            userMenus.forEach(menuInfoPO -> getParentMenuInfoSingle(menuInfoPO, menus));
        }
        return menus;
    }

    @Override
    public void deleteByCode(String code, boolean forceDeleteMenuOperate) {
        List<MenuInfoPO> menuInfoPOS = this.list(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.code, code));
        if (ObjectUtils.isEmpty(menuInfoPOS)) {
            return;
        }
        List<MenuInfoPO> menuInfo_child = this.list(new QueryWrapper<MenuInfoPO>().likeRight(MenuInfoField.layRec, menuInfoPOS.get(0).getId()));
        this.remove(new QueryWrapper<MenuInfoPO>().likeRight(MenuInfoField.layRec, menuInfoPOS.get(0).getId()).or().eq(MenuInfoField.code, code));
        if (forceDeleteMenuOperate) {
            QueryWrapper<MenuOperatePO> operateQuery = new QueryWrapper<>();
            operateQuery.eq(MenuOperateField.menuinfoId, menuInfoPOS.get(0).getId());
            if (!ObjectUtils.isEmpty(menuInfo_child)) {
                operateQuery.or().in(MenuOperateField.menuinfoId, menuInfo_child.stream().map(MenuInfoPO::getId).collect(Collectors.toList()));
            }
            menuOperateService.remove(operateQuery);
        }
    }

    @Override
    public List<MenuInfoPO> findUserPermissionMenus(Long userId, Integer status) {
        List<MenuInfoPO> menuInfoPOSFlat = this.findUserPermissionMenusFlat(userId, status);
        //平铺转树形结构
        long b5 = System.currentTimeMillis();
        List<MenuInfoPO> menuInfoPOS = flatToMenuTree(menuInfoPOSFlat);
        long b6 = System.currentTimeMillis();
        log.info("MenuInfoServiceImpl.findUserPermissionMenus=====flatToMenuTree=b6-b5===================================={}",b6-b5);
        return menuInfoPOS;
    }

    @Override
    public List<MenuInfoPO> findUserPermissionMenusFlat(Long userId, Integer status) {
        List<MenuInfoPO> menuInfoPOSFlat = this.findUserMenuFlat(userId, status,true);
        //设置i18n
        long b1 = System.currentTimeMillis();
        menuInfoPOSFlat = setI18nForkJoin(menuInfoPOSFlat);
        long b2 = System.currentTimeMillis();
        log.info("==> 获取并设置菜单国际化值，{}ms",b2-b1);

        return menuInfoPOSFlat;
    }

    @Override
    public List<MenuSuposPO> findUserPermissionMenusTree(Long userId, Integer status) {
        List<MenuSuposPO> menuInfoPOSFlat = this.findUserSuposMenuFlat(userId, status, true);
        // 运行期  个人信息管理及其子菜单做白名单处理
        if (1 == status) {
            QueryWrapper<MenuSuposPO> MenuSuposPOWrapper = new QueryWrapper<>();
            ArrayList<String> whiteMenuCodeList = new ArrayList<>();
            whiteMenuCodeList.add("personalInfo");
            whiteMenuCodeList.add("baseInfo");
            whiteMenuCodeList.add("editPassword");
            whiteMenuCodeList.add("selfstationletter");
            MenuSuposPOWrapper.in("code", whiteMenuCodeList);
            List<MenuSuposPO> whiteListMenus = menuInfoMapper.getWhiteListMenus(MenuSuposPOWrapper);
            menuInfoPOSFlat.addAll(whiteListMenus);
        }
        //菜单去重
        menuInfoPOSFlat = menusfilter(menuInfoPOSFlat);
        //设置i18n
        long b1 = System.currentTimeMillis();
        menuInfoPOSFlat = setI18nForkJoinForSuposMenu(menuInfoPOSFlat);
        long b2 = System.currentTimeMillis();
        log.info("==> 获取并设置菜单国际化值，{}ms, size={}",b2-b1, menuInfoPOSFlat.size());
        //转换成树结构
        long b3 = System.currentTimeMillis();
        menuInfoPOSFlat = flatSuposMenuToTree(menuInfoPOSFlat);
        long b4 = System.currentTimeMillis();
        log.info("==> 平铺转成树结构，{}ms, size={}", b4 - b3, menuInfoPOSFlat.size());

        return menuInfoPOSFlat;
    }
    //菜单去重
    private List<MenuSuposPO> menusfilter(List<MenuSuposPO> menuInfoPOS){
        return menuInfoPOS.parallelStream().collect(Collectors.collectingAndThen(Collectors.toCollection(()
                -> new TreeSet<>(Comparator.comparing(MenuSuposPO::getId))), ArrayList::new));
    }

    private List<MenuSuposPO> flatSuposMenuToTree(List<MenuSuposPO> menus) {
        ArrayList<MenuSuposPO> result = new ArrayList<>();

        final List<MenuSuposPO> sortedMenus = menus.stream().sorted(MenuSuposPO::compareTo).collect(Collectors.toList());
        final Map<Long, MenuSuposPO> menuMap = menus.stream().collect(Collectors.toMap(MenuSuposPO::getId, po -> po));
        sortedMenus.forEach(m -> {
            if (null == m.getParentId() || -1L == m.getParentId()) {
                result.add(m);
            } else {
                MenuSuposPO parent = menuMap.get(m.getParentId());
                if (parent != null) {
                    parent.addChildren(m);
                } else {
                    log.error("==> 菜单[{}]找不到父菜单: {}", m.getId(), m.toString());
                }
            }
        });

        log.info("==> 平铺转树后，根节点数量为{}", result.size());

        return result;
    }


    @Override
    public boolean isAdmin(long userId) {
        long cid = UserContext.getUserContext().getCompanyId();
        int count = menuInfoMapper.countUserPermissionMenus(userId, cid);
        return count > 0;
    }

    @Override
    public void deleteMenuInfoByCode(String code) {
        long b1 = System.currentTimeMillis();
        // 通过code获取菜单的appid
        QueryWrapper<MenuInfoPO> queryAppidWrapper = new QueryWrapper<>();
        queryAppidWrapper.eq(MenuInfoField.code, code);
        queryAppidWrapper.eq(MenuInfoField.valid, 1);
        String appId = menuInfoMapper.getAppIdByCode(queryAppidWrapper);
        ArrayList<String> appIdList = new ArrayList<>();
        appIdList.add(appId);
        long b2 = System.currentTimeMillis();
        log.info("deleteMenuInfoByCode=================================b2-b1========================={}",b2-b1);

        long b3 = System.currentTimeMillis();
        /**根据code查询当前节点下菜单树的菜单id*/
        List<Long> menuIdPOList = new ArrayList<>();
        Map<Long, MenuInfoPO> menuObjectHashMap = new HashMap<>();
        // TODO 此方法需要优化
        this.getMenuIdByRootCode(code, menuIdPOList, menuObjectHashMap);
        log.info("menuIdPOList===当前菜单树id list=>{}", menuIdPOList);
        long b4 = System.currentTimeMillis();
        log.info("getMenuIdByRootCode=========================b4-b3============================================{}",b4-b3);


        long b5 = System.currentTimeMillis();
        /**根据code获取设计器,code节点下的所有原始菜单list*/
        List<String> originalMenuCodeList = new ArrayList<>();
        // TODO 此方法需要优化
        menuAppDesignerRelService.getMenuAppDesignerCodeList(code, originalMenuCodeList);
        long b6 = System.currentTimeMillis();
        log.info("getMenuAppDesignerCodeList=========================b6-b5============================================{}",b6-b5);


        long b7 = System.currentTimeMillis();
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(MenuInfoPO::getCode, originalMenuCodeList);
        List<Long> originalMenuIdList = menuInfoService.list(queryWrapper).stream().map(MenuInfoPO::getId).collect(Collectors.toList());
        log.info("originalMenuIdList===app原始菜单id list==>{}", originalMenuIdList);
        long b8 = System.currentTimeMillis();
        log.info("originalMenuIdList=========================b8-b7============================================{}",b8-b7);


        long b9 = System.currentTimeMillis();
        //将此app的所有菜单都放置在menuObjectHashMap中
        QueryWrapper<MenuInfoPO> queryMenuWrapper = new QueryWrapper<>();
        queryMenuWrapper.eq(MenuInfoField.appId, appId);
        List<MenuInfoPO> allMenuPO = menuInfoService.list(queryMenuWrapper);
        long b10 = System.currentTimeMillis();
        log.info("allMenuPO=========================b10-b9============================================{}",b10-b9);

        for (MenuInfoPO menuInfoPO : allMenuPO) {
            menuObjectHashMap.put(menuInfoPO.getId(), menuInfoPO);
        }

        deleteMenuInfo(originalMenuIdList, menuIdPOList, menuObjectHashMap, appIdList, null);
    }

    /**
     * 1.判断菜单是否被其他app引用
     * 2.判断菜单配置中新增了哪些菜单
     * 3.判断菜单配置中移除类哪些菜单
     *
     * @params originalMenuIdList  app原始菜单id列表
     * @params menuIdPOList        app当前整个菜单树的菜单id列表
     */
    public void deleteMenuInfo(List<Long> originalMenuIdList, List<Long> menuIdPOList, Map<Long, MenuInfoPO> menuObjectHashMap, List<String> appIdList, String source) {
        /**
         * 处理被其他app引用的菜单,将不能删除的菜单id返回
         */
        HashSet<Long> summaryIds = dealMenuRelByOtherApp(originalMenuIdList, menuObjectHashMap, appIdList);
        /**
         * 新增的的菜单,处理
         */
        getAddMenuIds(originalMenuIdList, menuIdPOList, menuObjectHashMap, summaryIds);
        /**
         * 减少的菜单,处理
         */
        getReduceMenuIds(originalMenuIdList, menuIdPOList);

        if (!CollectionUtils.isEmpty(menuIdPOList)) {
            // 删除 app和menu引用关系
            // source 为installer的app可根据appid删除
            if ("installer".equals(source)) {
                QueryWrapper<AppRefPO> queryMenuInfoId = new QueryWrapper<>();
                queryMenuInfoId.in(AppRefField.appId, appIdList);
                appRefService.remove(queryMenuInfoId);
            } else {
                QueryWrapper<AppRefPO> queryMenuInfoId = new QueryWrapper<>();
                queryMenuInfoId.in(AppRefField.menuId, menuIdPOList);
                queryMenuInfoId.in(AppRefField.appId, appIdList);
                appRefService.remove(queryMenuInfoId);
            }
        }

        // 删除 app 公司关联
        QueryWrapper<AppCompanyRefPO> queryAppids = new QueryWrapper<>();
        queryAppids.in(AppCompanyRefField.appId, appIdList);
        appCompanyRefService.remove(queryAppids);

        if (!CollectionUtils.isEmpty(summaryIds)) {
            menuIdPOList.removeAll(summaryIds);
        }
        menuInfoService.deleteMenuRelevanceByMenuIdList(menuIdPOList);
    }

    private HashSet<Long> dealMenuRelByOtherApp(List<Long> originalMenuIdList, Map<Long, MenuInfoPO> menuObjectHashMap, List<String> appIdList) {
        /**
         * 查询要删除的菜单是否被其他app关联
         * 删除菜单数据， 如果菜单被其他app关联则不能删除
         */

        // 获取被其他app关联的菜单的引用关系
        QueryWrapper<AppRefPO> queryAppRefPOWrapper = new QueryWrapper<>();
        queryAppRefPOWrapper.in(AppRefField.menuId, originalMenuIdList);
        queryAppRefPOWrapper.notIn(AppRefField.appId, appIdList);
        List<AppRefPO> appRefPO = appRefMapper.findAppRefByMenuIdAppId(queryAppRefPOWrapper);

        //将不能被删除的id放置hashSet,最后统一处理
        HashSet<Long> summaryIds = new HashSet<>();

        if (!CollectionUtils.isEmpty(appRefPO)) {
            // 将整个不能删除的菜单id收集在set中
            HashSet<Long> ids = new HashSet<>();
            // 被其他app引用的menuIds，遍历并且找到所有父节点，放置set中。set中的菜单都不能被删除
            Set<Long> menuIds = appRefPO.stream().map(AppRefPO::getMenuId).collect(Collectors.toSet());
            log.info("被引用的菜单列表======>{}", menuIds);
            menuIds.stream().forEach(id -> {
                ids.add(id);
                getParentIdByMenuId(id, menuObjectHashMap, ids);

            });
            // 不能删除的菜单收集,统一处理
            summaryIds.addAll(ids);
            // 被引用的菜单,以及它的直系父节点置为folder
            if (!CollectionUtils.isEmpty(ids)) {
                ids.stream().forEach(id -> {
                    // 判断不能被删除的菜单 是否在待删除的app菜单中.如果存在,将其置为folder类型
                    MenuInfoPO menuInfoPO = menuObjectHashMap.get(id);
                    if (null != menuInfoPO) {
                        menuInfoPO.setUrl("");
                        menuInfoPO.setRoute("");
                        menuInfoPO.setMenuType(0);
                        log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                                menuInfoPO.getCode(),
                                UserContext.getUserContext().getUserName(),
                                RpcContext.getContext().getFromServiceName(),
                                RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
                        menuInfoMapper.updateById(menuInfoPO);
                    }
                });
            }

        }
        return summaryIds;
    }

    @Override
    public void getAddMenuIds(List<Long> originalMenuIdList, List<Long> menuIdPOList, Map<Long, MenuInfoPO> menuObjectHashMap, HashSet<Long> summaryIds) {
        /**
         * 对比要删除的originalMenuIdList 与menuIdPOList差异
         * menuIdPOList比originalMenuIdList多的code表示在菜单配置中新增的菜单
         */
        List<Long> addMenuThanParamIds = getAddMenuThanParam(menuIdPOList, originalMenuIdList);
        log.info("getAddMenuIds=====>addMenuThanParamIds={}", addMenuThanParamIds);
        // 将整个不能删除的菜单id收集在set中
        Set<Long> addIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(addMenuThanParamIds)) {
            addMenuThanParamIds.stream().forEach(menuId -> {
                addIds.add(menuId);
                //  查找上级所有节点
                findParentIdByMenuId(menuId, addIds, menuObjectHashMap);
            });
            // 不能删除的菜单收集,统一处理
            summaryIds.addAll(addIds);
//            menuIdPOList.removeAll(addIds);
            // addIds移除新增的菜单，剩余菜单变为folder
            addIds.removeAll(addMenuThanParamIds);
            addIds.stream().forEach(id -> {
                MenuInfoPO menuInfoPO = menuObjectHashMap.get(id);
                // 除app类型菜单 其余置为folder
                if (4 != menuInfoPO.getMenuType()) {
                    // mybatis plus not allowed update NULL
                    menuInfoPO.setUrl("");
                    menuInfoPO.setRoute("");
                    // 所有菜单类型置为folder
                    menuInfoPO.setMenuType(0);
                    log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                            menuInfoPO.getCode(),
                            UserContext.getUserContext().getUserName(),
                            RpcContext.getContext().getFromServiceName(),
                            RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
                    menuInfoMapper.updateById(menuInfoPO);
                }

            });
        }
    }

    @Override
    public void getReduceMenuIds(List<Long> originalMenuIdList, List<Long> menuIdPOList) {
        /**
         *  menuIdPOList比menuIdListParam少的code表示app菜单被移走
         *  将reduceMenuThanParamIds菜单修改成folder:menuType 0
         */
        Map<Long, MenuInfoPO> originalMenuMap = menuInfoService.listByIds(originalMenuIdList).stream().collect(Collectors.toMap(MenuInfoPO::getId, menuInfoPO -> menuInfoPO));
        List<MenuInfoPO> allMenuInfoPOS = menuInfoService.list();
        Map<Long, MenuInfoPO> allMenuInfoPOMap = allMenuInfoPOS.stream().collect(Collectors.toMap(MenuInfoPO::getId, menuInfoPO -> menuInfoPO));

        List<Long> reduceMenuThanParamIds = getReduceMenuThanParam(menuIdPOList, originalMenuIdList);
        log.info("getReduceMenuIds======>reduceMenuThanParamIds={}", reduceMenuThanParamIds);
        if (!CollectionUtils.isEmpty(reduceMenuThanParamIds)) {
            reduceMenuThanParamIds.stream().forEach(menuId -> {
                MenuInfoPO menuPO = originalMenuMap.get(menuId);
                // mybatis not allowed update NULL
                menuPO.setRoute("");
                menuPO.setUrl("");
                menuPO.setMenuType(0);
                log.info("====================更新菜单数据,code {},user {},from {}, url {}====================",
                        menuPO.getCode(),
                        UserContext.getUserContext().getUserName(),
                        RpcContext.getContext().getFromServiceName(),
                        RpcContext.getContext().getRequest() != null ? RpcContext.getContext().getRequest().getRequestURI() : "");
                menuInfoMapper.updateById(menuPO);
            });

            // 获取被移出菜单下的所有菜单
            /**根据id查询当前节点下菜单树的菜单id*/
            HashSet<Long> reduceMenuTreeIds = new HashSet<>();
            for (Long reduceMenuThanParamId : reduceMenuThanParamIds) {
                Set<Long> menuIdByRootMenuIdSet = getMenuIdByRootMenuId(reduceMenuThanParamId);
                if (!CollectionUtils.isEmpty(menuIdByRootMenuIdSet)) {
                    reduceMenuTreeIds.addAll(menuIdByRootMenuIdSet);
                }
            }
            HashSet<Long> ids = new HashSet<>();
            QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.in(MenuInfoField.id, reduceMenuTreeIds);
            List<MenuInfoPO> childMenuList = menuInfoService.list(queryWrapper);
            // 如果有,childMenuList 以及父节点放置ids中
            if (!CollectionUtils.isEmpty(childMenuList)) {
                // 下挂的菜单类型为page  app 类型 则上级不能删除
                List<Long> menuIds = childMenuList.stream().filter(menu -> null == menu.getMenuType() || menu.getMenuType().equals(1)).map(menu -> menu.getId()).collect(Collectors.toList());
                menuIds.stream().forEach(id -> {
                    ids.add(id);
                    findParentIdByMenuId(id, ids, allMenuInfoPOMap);
                });
            }
            // 删除 reduceMenuThanParamIds下的所有 非page节点
            reduceMenuTreeIds.removeAll(ids);
            menuInfoService.deleteMenuRelevanceByMenuIdList(reduceMenuTreeIds);

            //被移走 待删除的菜单list
            reduceMenuThanParamIds.removeAll(ids);
            //将待删除的菜单合并
            menuIdPOList.addAll(reduceMenuThanParamIds);
        }
    }

    /**
     * 根据code查询所有子节点
     */
    public Set<Long> getMenuIdByRootMenuId(Long menuId) {
        Set<Long> idList = new HashSet<>();
        Map<Long, MenuInfoPO> menuObjectHashMap = new HashMap<>();
        //查询节点下所有子节点
        MenuInfoPO menuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("ID", menuId));
        // 保存menuid，menuPo
        idList.add(menuInfoPO.getId());
        menuObjectHashMap.put(menuInfoPO.getId(), menuInfoPO);

        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("PARENT_ID", menuInfoPO.getId());
        List<MenuInfoPO> childMenuInfoPOList = menuInfoService.list(queryWrapper);
        while (!CollectionUtils.isEmpty(childMenuInfoPOList)) {
            List<Long> ids = new ArrayList<>();
            childMenuInfoPOList.stream().forEach(menu -> {
                ids.add(menu.getId());
                idList.add(menu.getId());
                menuObjectHashMap.put(menu.getId(), menu);
            });

            QueryWrapper<MenuInfoPO> subMenuWrapper = new QueryWrapper<>();
            subMenuWrapper.in("PARENT_ID", ids);
            childMenuInfoPOList = menuInfoService.list(subMenuWrapper);
        }
        return idList;
    }

    @Override
    public List<Long> getMenusByAppIds(Set<String> appIds) {
        if (CollectionUtils.isEmpty(appIds)) {
            return new ArrayList<>();
        }
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in(MenuInfoField.appId, appIds);
        List<Long> menuIdByAppIds = menuInfoMapper.getMenuIdByAppIds(queryWrapper);
        return menuIdByAppIds;
    }

    /**
     * 根据code查询所有子节点
     */
    public void getMenuIdByRootCode(String code, List<Long> idList, Map<Long, MenuInfoPO> menuObjectHashMap) {
        //查询节点下所有子节点
        MenuInfoPO menuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", code));
        if (ObjectUtils.isEmpty(menuInfoPO)) {
            log.info("deleteMenuInfoByCodes but code not exits============================> {}", code);
        }
        // 保存menuid，menuPo
        idList.add(menuInfoPO.getId());
        menuObjectHashMap.put(menuInfoPO.getId(), menuInfoPO);

        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("PARENT_ID", menuInfoPO.getId());
        List<MenuInfoPO> childMenuInfoPOList = menuInfoService.list(queryWrapper);
        while (!CollectionUtils.isEmpty(childMenuInfoPOList)) {
            List<Long> ids = new ArrayList<>();
            childMenuInfoPOList.stream().forEach(menu -> {
                ids.add(menu.getId());
                idList.add(menu.getId());
                menuObjectHashMap.put(menu.getId(), menu);
            });

            QueryWrapper<MenuInfoPO> subMenuWrapper = new QueryWrapper<>();
            subMenuWrapper.in("PARENT_ID", ids);
            childMenuInfoPOList = menuInfoService.list(subMenuWrapper);
        }
    }

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

    // 查找上级所有节点
    public void findParentIdByMenuId(Long menuId, Set<Long> ids, Map<Long, MenuInfoPO> menuObjectHashMap) {
        MenuInfoPO menuInfoPO = menuObjectHashMap.get(menuId);
        if (null != menuInfoPO && menuInfoPO.getParentId() != -1L) {
            Long parentId = menuInfoPO.getParentId();
            ids.add(parentId);
            if (null != menuObjectHashMap.get(parentId)) {
                findParentIdByMenuId(parentId, ids, menuObjectHashMap);
            }
        }
    }

    // 与原有菜单树相比，新增的菜单
    public List<Long> getAddMenuThanParam(List<Long> menuIdPOList, List<Long> menuIdListParam) {
        ArrayList<Long> addMenuIdList = new ArrayList<>();
        for (Long menuPOId : menuIdPOList) {
            if (!listContainsElement(menuIdListParam, menuPOId)) {
                addMenuIdList.add(menuPOId);
            }
        }
        return addMenuIdList;
    }

    // 与原有菜单树相比，减少的菜单
    public List<Long> getReduceMenuThanParam(List<Long> menuIdPOList, List<Long> menuIdListParam) {
        ArrayList<Long> reduceMenuIdList = new ArrayList<>();
        for (Long menuIdParam : menuIdListParam) {
            if (!listContainsElement(menuIdPOList, menuIdParam)) {
                reduceMenuIdList.add(menuIdParam);
            }
        }
        return reduceMenuIdList;
    }


    public void getParentMenuInfoSingle(MenuInfoPO menuInfoPO, List<MenuInfoPO> menus) {
        if (!ObjectUtils.isEmpty(menuInfoPO.getParentId()) && -1 != menuInfoPO.getParentId()) {
            MenuInfoPO parentMenu = this.getOne(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.id, menuInfoPO.getParentId()));
            menus.add(parentMenu);
            getParentMenuInfoSingle(parentMenu, menus);
        }
    }

    @Transactional
    @Override
    public void batchDeleteMenusAll(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            throw new MenuException(MenuErrorEnum.MENU_DELETE_DATA_IS_NOT_EMPTY);
        }
        List<MenuInfoPO> menuInfoPOList = new ArrayList<>();
        for (Long id : idList) {
            MenuInfoPO menuInfoPO = queryMenuById(id);
            menuInfoPOList.add(menuInfoPO);
        }
        deleteMenuRelevance(menuInfoPOList);
    }

    public static void getParentIdByMenuId(Long menuId, Map<Long, MenuInfoPO> menuInfoPOMap, Set<Long> ids) {
        MenuInfoPO menuInfoPO = menuInfoPOMap.get(menuId);
        if (null != menuInfoPO && menuInfoPO.getParentId() != -1L) {
            Long parentId = menuInfoPO.getParentId();
            ids.add(parentId);
            if (null != menuInfoPOMap.get(parentId)) {
                getParentIdByMenuId(parentId, menuInfoPOMap, ids);
            }
        }
    }

    public void deleteMenuRelevance(List<MenuInfoPO> menuInfoPOList) {
        if (!ObjectUtils.isEmpty(menuInfoPOList)) {
            List<Long> menuIdList = menuInfoPOList.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
            deleteMenu(menuIdList);
        }
    }

    public void deleteMenuRelevanceByMenuIdList(List<Long> menuIdList) {
        log.info("deleteMenuRelevanceByMenuIdList============>menuIdList={}", menuIdList);
        if (!CollectionUtils.isEmpty(menuIdList)) {
            deleteMenu(menuIdList);
        }
    }

    @Override
    public void deleteMenuRelevanceByMenuIdList(Set<Long> menuIdList) {
        ArrayList<Long> longs = new ArrayList<>(menuIdList);
        log.info("deleteMenuRelevanceByMenuIdList=========>menuIdList={}", menuIdList);
        if (!CollectionUtils.isEmpty(longs)) {
            deleteMenu(longs);
        }
    }

    private void deleteMenu(List<Long> menuIdList) {
        removeMenuInfoByIds(menuIdList);
        // 删除菜单公司关联数据
        QueryWrapper<MenuInfoCompanyRefPO> queryMenuId = new QueryWrapper<>();
        queryMenuId.in(MenuInfoCompanyRefField.menuinfoId, menuIdList);
        menuInfoCompanyRefService.remove(queryMenuId);

        //删除菜单助记码
        menuInfoMneCodeService.remove(new QueryWrapper<MenuInfoMneCodePO>().in(MenuInfoMneCodeField.menuInfoId, menuIdList));

        QueryWrapper<MenuOperatePO> queryOperation = new QueryWrapper<>();
        queryOperation.in(MenuOperateField.menuinfoId, menuIdList);
        List<MenuOperatePO> menuOperatePOList = menuOperateService.list(queryOperation);
        List<Long> operateIdList = new ArrayList<>();
        List<String> operateCodeList = new ArrayList<>();
        for (MenuOperatePO menuOperatePO : menuOperatePOList) {
            operateIdList.add(menuOperatePO.getId());
            operateCodeList.add(menuOperatePO.getCode());
        }
        // 删除菜单操作数据
        menuoperateMapper.delete(queryOperation);
        if (!ObjectUtils.isEmpty(operateIdList)) {
            QueryWrapper<MenuOperateCodeUrlRefPO> queryOperateUrl = new QueryWrapper<>();
            queryOperateUrl.in(MenuOperateCodeUrlRefField.menuoperateCode, operateCodeList);
            // 删除菜单操作和URL关联表数据
            menuOperateCodeUrlRefMapper.delete(queryOperateUrl);
            // 删除用户和角色权限数据
            QueryWrapper<UserPermissionPO> queryOperateIdUser = new QueryWrapper<>();
            QueryWrapper<RolePermissionPO> queryOperateIdRole = new QueryWrapper<>();
            queryOperateIdUser.in(UserPermissionField.menuOperateId, operateIdList);
            queryOperateIdRole.in(RolePermissionField.menuOperateId, operateIdList);
            List<UserPermissionPO> listUserPermission = userPermissionMapper.selectList(queryOperateIdUser);
            List<RolePermissionPO> listRolePermission = rolePermissionMapper.selectList(queryOperateIdRole);
            //删除角色权限岗位,人员关联
            if (!ObjectUtils.isEmpty(listRolePermission)) {
                rolePPositionService.remove(new QueryWrapper<RolePPositionPO>().in(RolePPositionField.rolepermissionId, listRolePermission.stream().map(RolePermissionPO::getId).collect(Collectors.toList())));
                rolePStaffService.remove(new QueryWrapper<RolePStaffPO>().in(RolePStaffField.rolepermissionId, listRolePermission.stream().map(RolePermissionPO::getId).collect(Collectors.toList())));
            }
            //删除用户权限岗位,人员关联
            if (!ObjectUtils.isEmpty(listUserPermission)) {
                userPPositionService.remove(new QueryWrapper<UserPPositionPO>().in(UserPPositionField.userpermissionId, listUserPermission.stream().map(UserPermissionPO::getId).collect(Collectors.toList())));
                userPStaffService.remove(new QueryWrapper<UserPStaffPO>().in(UserPStaffField.userpermissionId, listUserPermission.stream().map(UserPermissionPO::getId).collect(Collectors.toList())));
            }
            userPermissionMapper.delete(queryOperateIdUser);
            rolePermissionMapper.delete(queryOperateIdRole);
        }
    }

    @Override
    public String saveBachUrl(List<MenuInfoJsonDTO> menuInfoJsonDTOS, boolean needBak) {
        if (ObjectUtils.isEmpty(menuInfoJsonDTOS)) {
            return null;
        }
        //一次性查找所需菜单
        List<String> codes = menuInfoJsonDTOS.stream().map(MenuInfoJsonDTO::getCode).collect(Collectors.toList());
        List<MenuInfoPO> old_menu_data = this.list(new QueryWrapper<MenuInfoPO>().in(MenuInfoField.code, codes));
        //回滚数据准备
        String uuid = "";
        if (needBak) {
            uuid = rollBackData(menuInfoJsonDTOS, old_menu_data);
        }
        //一次性查询所需菜单的父菜单
        Set<String> parentCodes = menuInfoJsonDTOS.stream().map(MenuInfoJsonDTO::getParentCode).filter(Objects::nonNull).collect(Collectors.toSet());
        old_menu_data.addAll(this.list(new QueryWrapper<MenuInfoPO>().in(MenuInfoField.code, parentCodes)));
        //构建code->menu map
        Map<String, MenuInfoPO> menuMap = new HashMap<>();
        //构建id->menu map
        Map<Long, MenuInfoPO> menuMapById = new HashMap<>();
        old_menu_data.forEach(menuInfoPO -> {
            menuMap.put(menuInfoPO.getCode(), menuInfoPO);
            menuMapById.put(menuInfoPO.getId(), menuInfoPO);
        });
        //统一收集需要保存的菜单操作
        List<MenuOperatePO> menuOperatePOS = new ArrayList<>();
        //统一收集操作URL
        List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS = new ArrayList<>();

        //CODE为menu_list那条菜单不进行处理 过滤掉
        List<MenuInfoJsonDTO> menuInfoDTOS = menuInfoJsonDTOS.stream().filter(menuInfoJsonDTO -> !menuInfoJsonDTO.getCode().equals(Constants.MENU_LIST)).collect(Collectors.toList());
        //构建菜单PO
        List<MenuInfoPO> menuInfoPOS = menuInfoDTOS.stream().map(menuInfoJsonDTO -> {
            MenuInfoPO menuInfoPO = new MenuInfoPO();
            BeanUtils.copyProperties(menuInfoJsonDTO, menuInfoPO);
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getCode())) {
                MenuInfoPO dbMenuInfoPO = menuMap.get(menuInfoJsonDTO.getCode());
                //如果该菜单在页面上修改过，就不进行更新，直接返回数据库里的数据
                if (!ObjectUtils.isEmpty(dbMenuInfoPO) && !ObjectUtils.isEmpty(dbMenuInfoPO.getEdited()) && dbMenuInfoPO.getEdited()) {
                    //菜单url修改
                    if (StringUtils.isNotEmpty(menuInfoJsonDTO.getUrl())) {
                        dbMenuInfoPO.setUrl(menuInfoJsonDTO.getUrl());
                    }
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
            MenuInfoPO parentMenu;
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getParentCode())) {
                parentMenu = menuMap.get(menuInfoJsonDTO.getParentCode());
            } else {
                parentMenu = new MenuInfoPO();
                parentMenu.setId(Constants.MENU_LIST_ID);
            }
            //存在parentCode 但是数据库中不存在
            if (ObjectUtils.isEmpty(parentMenu)) {
                parentMenu = getParentMenuInfo(menuInfoDTOS, menuInfoJsonDTO.getParentCode(), menuMapById, menuMap);
                if (ObjectUtils.isEmpty(parentMenu)) {
                    throw new MenuException(MenuErrorEnum.PARENT_CON_NOT_FIND);
                } else {
                    //新建出来的parentMenu存放到菜单Map中，防止循环到该菜单时重复创建
                    menuMap.put(menuInfoJsonDTO.getParentCode(), parentMenu);
                    menuMapById.put(parentMenu.getId(), parentMenu);
                }
            }
            menuInfoPO.setParentId(parentMenu.getId());
            setFullPath(menuInfoPO, menuMapById);
            menuMap.put(menuInfoPO.getCode(), menuInfoPO);
            menuMapById.put(menuInfoPO.getId(), menuInfoPO);
            return menuInfoPO;
        }).collect(Collectors.toList());
        //设置菜单排序
        setSort(menuInfoPOS);
        for (int i = 0; i < menuInfoDTOS.size(); i++) {
            //创建操作PO
            List<MenuOperatePO> operatePOS = generateOperates(menuInfoDTOS.get(i).getMenuOperates(), menuInfoPOS.get(i).getId(), menuOperateCodeUrlRefPOS);
            if (!ObjectUtils.isEmpty(operatePOS)) {
                menuOperatePOS.addAll(operatePOS);
            }
        }
        //统一保存
        integrationSave(menuInfoPOS, menuOperatePOS, menuOperateCodeUrlRefPOS);
        //菜单、操作、URL初始化后 将所有需要匹配的URL加入redis
        menuOperateCodeUrlRefService.updateUrl(null);
        return uuid;
    }

    @Transactional
    public void integrationSave(List<MenuInfoPO> menuInfoPOS, List<MenuOperatePO> menuOperatePOS, List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS) {
        //保存菜单
        this.saveOrUpdateBatch(menuInfoPOS);
        //保存菜单助记码 zh_CN
        RpcContext.getContext().setLanguage(Locale.SIMPLIFIED_CHINESE);
        menuInfoMneCodeService.createMenuInfoMneCodeI18NKey(menuInfoPOS);
        //保存操作
        menuOperateService.saveOrUpdateBatch(menuOperatePOS);
        //保存操作URL
        menuOperateCodeUrlRefService.saveOrUpdateBatch(menuOperateCodeUrlRefPOS);
        //创建默认操作
        if (!ObjectUtils.isEmpty(menuInfoPOS)) {
            this.generateDefaultOperate(menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList()));
        }
        //创建菜单公司关联
        saveMenuInfoCompanyRef(menuInfoPOS);
    }

    private void setSort(List<MenuInfoPO> menuInfoPOS) {
        if (ObjectUtils.isEmpty(menuInfoPOS)) {
            return;
        }
        Map<Long, List<MenuInfoPO>> map = menuInfoPOS.stream().collect(Collectors.groupingBy(MenuInfoPO::getParentId));
        List<MenuInfoPO> parentMenuInfos = this.list(new QueryWrapper<MenuInfoPO>().in(MenuInfoField.parentId, map.keySet()));
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

    private String rollBackData(List<MenuInfoJsonDTO> menuInfoJsonDTOS, List<MenuInfoPO> old_menu_data) {
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
        return createTemp(old_menu_data, old_operate_data, old_operate_url_data, menuInfoJsonDTOS);
    }

    @Transactional
    public String createTemp(List<MenuInfoPO> menuInfoPOS, List<MenuOperatePO> menuOperatePOS, List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS, List<MenuInfoJsonDTO> newData) {
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


    @Transactional
    public void saveMenuInfoCompanyRef(List<MenuInfoPO> menuInfoPOS) {
        List<MenuInfoCompanyRefPO> menuInfoCompanyRefPOS = menuInfoPOS.stream().map(menuInfoPO -> {
            int count = menuInfoCompanyRefService.count(new QueryWrapper<MenuInfoCompanyRefPO>().eq("COMPANY_ID", menuInfoPO.getCid()).eq("MENUINFO_ID", menuInfoPO.getId()));
            if (count == 0) {
                MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
                menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
                menuInfoCompanyRefPO.setCompanyId(menuInfoPO.getCid());
                return menuInfoCompanyRefPO;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        menuInfoCompanyRefService.saveBatch(menuInfoCompanyRefPOS);
    }

    private List<MenuOperatePO> generateOperates(List<MenuOperateJsonDTO> menuOperateJsonDTOS, Long menuInfoId, List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS) {
        if (ObjectUtils.isEmpty(menuOperateJsonDTOS)) {
            return null;
        }
        List<MenuOperatePO> menuOperatePOS = menuOperateJsonDTOS.stream().map(menuOperateJsonDTO -> {
            MenuOperatePO menuOperatePO = new MenuOperatePO();
            BeanUtils.copyProperties(menuOperateJsonDTO, menuOperatePO);
            if (!ObjectUtils.isEmpty(menuOperateJsonDTO.getCode())) {
                MenuOperatePO dbOperatePO = menuOperateService.getOne(new QueryWrapper<MenuOperatePO>().eq("CODE", menuOperateJsonDTO.getCode()));
                if (!ObjectUtils.isEmpty(dbOperatePO)) {
                    menuOperatePO.setId(dbOperatePO.getId());
                }
            } else {
                throw new MenuOperateException(MenuOperateErrorEnum.OPERATE_CODE_EMPTY);
            }
            menuOperatePO.setMenuinfoId(menuInfoId);
            return menuOperatePO;
        }).collect(Collectors.toList());
        for (int i = 0; i < menuOperateJsonDTOS.size(); i++) {
            collectUrl(menuOperateJsonDTOS.get(i).getUrls(), menuOperatePOS.get(i).getCode(), menuOperateCodeUrlRefPOS);
        }
        return menuOperatePOS;
    }

    private void collectUrl(List<MenuOperateUrlJsonDTO> menuOperateCodeUrlRefDTOS, String menuOperateCode, List<MenuOperateCodeUrlRefPO> collect) {
        if (ObjectUtils.isEmpty(menuOperateCodeUrlRefDTOS)) {
            return;
        }
        //统一查询URL
        List<MenuOperateCodeUrlRefPO> dbMenuOperateUrls = menuOperateCodeUrlRefService.list(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq(MenuOperateCodeUrlRefField.menuoperateCode, menuOperateCode));
        List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS = menuOperateCodeUrlRefDTOS.stream().map(menuOperateCodeUrlRefDTO -> {
            MenuOperateCodeUrlRefPO menuOperateCodeUrlRefPO = new MenuOperateCodeUrlRefPO();
            BeanUtils.copyProperties(menuOperateCodeUrlRefDTO, menuOperateCodeUrlRefPO);
            if (ObjectUtils.isEmpty(menuOperateCodeUrlRefPO.getUrl())) {
                throw new MenuOperateException(MenuOperateErrorEnum.URL_CONT_NOT_EMPTY);
            }
            if (ObjectUtils.isEmpty(menuOperateCodeUrlRefPO.getMethodType())) {
                throw new MenuOperateException(MenuOperateErrorEnum.METHOD_TYPE_CONT_NOT_EMPTY);
            }
            menuOperateCodeUrlRefPO.setIsCustom(menuOperateCodeUrlRefDTO.getIsCustom() == 1);
            menuOperateCodeUrlRefPO.setMenuoperateCode(menuOperateCode);
            menuOperateCodeUrlRefPO.setUrl(this.filterPathParams(menuOperateCodeUrlRefPO.getUrl()));
            menuOperateCodeUrlRefPO.setRegMatch(menuOperateCodeUrlRefPO.getUrl().contains("/[^/^]"));
            Optional<MenuOperateCodeUrlRefPO> exits = dbMenuOperateUrls.stream().filter(menuOperateCodeUrlRef -> menuOperateCodeUrlRef.getUrl().equals(menuOperateCodeUrlRefPO.getUrl()) && menuOperateCodeUrlRef.getMethodType().equals(menuOperateCodeUrlRefPO.getMethodType())).findFirst();
            exits.ifPresent(operateCodeUrlRefPO -> menuOperateCodeUrlRefPO.setId(operateCodeUrlRefPO.getId()));
            return menuOperateCodeUrlRefPO;
        }).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(menuOperateCodeUrlRefPOS)) {
            collect.addAll(menuOperateCodeUrlRefPOS);
        }
    }

    private MenuInfoPO getParentMenuInfo(List<MenuInfoJsonDTO> menuInfoJsonDTOS, String parentCode, Map<Long, MenuInfoPO> menuMapById, Map<String, MenuInfoPO> menuMap) {
        Optional<MenuInfoJsonDTO> first = menuInfoJsonDTOS.stream().filter(menuInfoJsonDTO -> menuInfoJsonDTO.getCode().equals(parentCode)).findFirst();
        if (first.isPresent()) {
            MenuInfoJsonDTO menuInfoJsonDTO = first.get();
            MenuInfoPO menuInfoPO = new MenuInfoPO();
            BeanUtils.copyProperties(menuInfoJsonDTO, menuInfoPO);
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getCode())) {
                MenuInfoPO dbMenuInfoPO = menuMap.get(menuInfoJsonDTO.getCode());
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
            if (!ObjectUtils.isEmpty(menuInfoJsonDTO.getParentCode()) && menuInfoPO.getId() != Constants.MENU_LIST_ID) {
                parentMenu = menuMap.get(menuInfoJsonDTO.getParentCode());
            } else {
                parentMenu = new MenuInfoPO();
                parentMenu.setId(Constants.MENU_LIST_ID);
            }
            if (ObjectUtils.isEmpty(parentMenu)) {
                parentMenu = getParentMenuInfo(menuInfoJsonDTOS, menuInfoJsonDTO.getParentCode(), menuMapById, menuMap);
                if (ObjectUtils.isEmpty(parentMenu)) {
                    throw new MenuException(MenuErrorEnum.PARENT_CON_NOT_FIND);
                } else {
                    menuMap.put(parentMenu.getCode(), parentMenu);
                    menuMapById.put(parentMenu.getId(), parentMenu);
                }
            }
            menuInfoPO.setParentId(parentMenu.getId());
            setFullPath(menuInfoPO, menuMapById);
            return menuInfoPO;
        }
        return null;
    }


    /**
     * app管理中app卸载后的菜单处理
     * 1.根据appid查询对应菜单
     * 2.根据app根节点查询当前菜单树中所有菜单
     * 3.对比步骤一和二的菜单差异进行处理
     * 4.
     */
    @Transactional
    @Override
    public void batchDeleteMenusByApp(List<String> appIdList, String source) {
        if (CollectionUtils.isEmpty(appIdList)) {
            throw new MenuException(MenuErrorEnum.MENU_DELETE_DATA_IS_NOT_EMPTY);
        }
        if (StringUtils.isEmpty(source)) {
            throw new MenuException(MenuErrorEnum.MENU_DELETE_DATA_IS_NOT_EMPTY);
        }
        appIdList.stream().forEach(appid -> {
            // 查询属于此app的菜单list==>menuInfoPOList
            QueryWrapper queryMenuPOWapper = new QueryWrapper();
            queryMenuPOWapper.in(MenuInfoField.appId, appIdList);
            List<MenuInfoPO> originMenuInfoPOList = menuInfoService.list(queryMenuPOWapper);

            // 从原始菜单中,找到根节点,获取当前整个菜单树列表
            if (!CollectionUtils.isEmpty(originMenuInfoPOList)) {
                // app原始菜单id列表
                List<Long> originMenuIdList = originMenuInfoPOList.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
                //获取app根节点(parentId 为-1L)
                List<Long> menuIdPOList = new ArrayList<>();
                Map<Long, MenuInfoPO> menuObjectHashMap = new HashMap<>();
                originMenuInfoPOList.stream().forEach(menuInfoPO -> {
                    if (menuInfoPO.getParentId().equals(-1L)) {
                        log.info("app-manger......find app root code{}", menuInfoPO.getCode());
                        // TODO 此方法需要优化
                        this.getMenuIdByRootCode(menuInfoPO.getCode(), menuIdPOList, menuObjectHashMap);
                        log.info("menuIdPOList===当前菜单树=>{}", menuIdPOList);
                    }
                });
                deleteMenuInfo(originMenuIdList, menuIdPOList, menuObjectHashMap, appIdList, source);
            } else {
                log.info("待删除列表为空................");
            }

        });
    }

    @Override
    public void setFullPath(MenuInfoPO menuInfoPO, Map<Long, MenuInfoPO> menuMapById) {
        if (ObjectUtils.isEmpty(menuInfoPO.getId()) && menuInfoPO.getId() != -1) {
            return;
        }
        MenuInfoPO parentMenuInfo = new MenuInfoPO();
        if (!ObjectUtils.isEmpty(menuInfoPO.getParentId())) {
            if (!ObjectUtils.isEmpty(menuMapById)) {
                parentMenuInfo = menuMapById.get(menuInfoPO.getParentId());
            }
            if (ObjectUtils.isEmpty(parentMenuInfo)) {
                parentMenuInfo = this.getOne(new QueryWrapper<MenuInfoPO>().eq("ID", menuInfoPO.getParentId()));
            }
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
