package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.rbac.dao.AppCompanyRefMapper;
import com.supcon.supfusion.rbac.dao.field.AppCompanyRefField;
import com.supcon.supfusion.rbac.dao.field.AppRefField;
import com.supcon.supfusion.rbac.dao.field.MenuInfoCompanyRefField;
import com.supcon.supfusion.rbac.dao.po.AppCompanyRefPO;
import com.supcon.supfusion.rbac.dao.po.AppRefPO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoCompanyRefPO;
import com.supcon.supfusion.rbac.service.IAppCompanyRefService;
import com.supcon.supfusion.rbac.service.IAppRefService;
import com.supcon.supfusion.rbac.service.IMenuInfoCompanyRefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppCompanyRefServiceImpl extends ServiceImpl<AppCompanyRefMapper, AppCompanyRefPO> implements IAppCompanyRefService {

    @Autowired
    IAppRefService appRefService;

    @Autowired
    IMenuInfoCompanyRefService menuInfoCompanyRefService;

    @Autowired
    IAppCompanyRefService appCompanyRefService;


    @Override
    @Transactional
    public void addAppCompanyRef(List<AppCompanyRefPO> appCompanyRefPOList) {
        log.info("AppCompanyRefServiceImpl.addAppCompanyRef=params:appCompanyRefPOList==========================================={}",appCompanyRefPOList);
        String appId = appCompanyRefPOList.get(0).getAppId();

        // 删除APP关联的公司数据
        QueryWrapper<AppCompanyRefPO> queryWrapper = new QueryWrapper();
        queryWrapper.eq(MenuInfoCompanyRefField.appId, appId);
        remove(queryWrapper);
        // 保存APP授权公司关联关系
        saveBatch(appCompanyRefPOList);

        // 过滤APP关联的公司集合
        List<Long> cidList = appCompanyRefPOList.stream().map(AppCompanyRefPO::getCid).collect(Collectors.toList());

        // 查询APP关联的菜单数据
        List<Long> menuIdList = appRefService.queryMenuIdListByAppId(appId);

        // 保存最新的APP关联的菜单公司关联数据
        List<MenuInfoCompanyRefPO> menuInfoCompanyRefPOList = new ArrayList<>();
        for (Long menuId : menuIdList) {
        	//删除菜单公司关联表
            QueryWrapper<MenuInfoCompanyRefPO> query = new QueryWrapper();
            query.eq(MenuInfoCompanyRefField.menuinfoId, menuId);
            menuInfoCompanyRefService.remove(query);
            for (Long cid : cidList) {
                MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
                Long id = IDGenerator.newInstance().generate().longValue();
                menuInfoCompanyRefPO.setId(id);
                menuInfoCompanyRefPO.setCompanyId(cid);
                menuInfoCompanyRefPO.setMenuinfoId(menuId);
                menuInfoCompanyRefPO.setAppId(appId);
                menuInfoCompanyRefPOList.add(menuInfoCompanyRefPO);
            }
        }
        menuInfoCompanyRefService.saveBatch(menuInfoCompanyRefPOList);
    }

    @Override
    public List<AppCompanyRefPO> queryAppCompanyRefList(String appId) {
        log.info("AppCompanyRefServiceImpl.queryAppCompanyRefList=param:appId======================================={}",appId);
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("APPID", appId);
        List<AppCompanyRefPO> appCompanyRefPOList = list(queryWrapper);
        log.info("AppCompanyRefServiceImpl.queryAppCompanyRefList=response:appCompanyRefPOList======================================={}",appCompanyRefPOList);
        return appCompanyRefPOList;
    }

    @Override
    public void deleteAppCompanyRef(String appId) {
        // 删除APP关联的公司数据
        QueryWrapper<AppCompanyRefPO> queryWrapper = new QueryWrapper();
        queryWrapper.eq(MenuInfoCompanyRefField.appId, appId);
        remove(queryWrapper);
    }

    @Override
    public Set<String> findAppComRefByAppId(Set<String> appIds) {
        if(CollectionUtils.isEmpty(appIds)){
            return new HashSet<>();
        }
        QueryWrapper<AppCompanyRefPO> appCompanyRefPOQueryWrapper = new QueryWrapper<>();
        appCompanyRefPOQueryWrapper.lambda().in(AppCompanyRefPO::getAppId,appIds);
        List<AppCompanyRefPO> appCompanyRefPOList = this.list(appCompanyRefPOQueryWrapper);
        Set<String> appCompanyRefAppIdList = appCompanyRefPOList.stream().map(AppCompanyRefPO::getAppId).collect(Collectors.toSet());
        return appCompanyRefAppIdList;
    }

}
