/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;



import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.rbac.api.dto.UpdateMenuInfoIdDTO;

import java.util.List;

public interface CustomMenuInfoService {

    /**
     * 根据id删除
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据entityCode删除
     */
    void deleteByEntityCode(String entityCode);

    /**
     * 根据entityCode修改
     */
    void updateByEntityCode(String entityCode);

    /**
     * 根据菜单id修改id
     */
    void updateMenuInfoById(UpdateMenuInfoIdDTO updateMenuInfoIdDTO);

    /**
     * 保存菜单
     *
     * @param menuInfo
     */
    void save(MenuInfo menuInfo);

    /**
     * 根据app删除菜单
     */
    String deleteMenuInfoByApps(String app);

    /**
     * 正序获取
     *//*
    List<MenuInfo> getByAsc();

    *//**
     * 根据moduleCode模糊查询
     *//*
    List<MenuInfo> getByLikeModuleCode(String moduleCode);

    *//**
     * 根据code获取
     *//*
    MenuInfo getMenuInfoByCode(String code);

    *//**
     * 根据id获取
     *//*
    MenuInfo getMenuInfoById(Long id);

    *//**
     * 根据entityCode获取
     *//*
    List<MenuInfo> getByEntityCode(String entityCode);


    MenuInfo load(Long id);

    MenuInfo get(String code);

*/

    /*MenuInfo getMenusTree();

     *//**
     * 查询归属模块的下级菜单；
     *
     * @param moduleCode 活动编码串
     * @return List<MenuInfo>
     *//*
    List<MenuInfo> getMenuInfoByModul(String moduleCode);

    List<MenuOperate> getOperateList(MenuInfo menuInfo);

    List<MenuInfo> getEntityMenus(String entityCode);

    List<MenuInfo> getMenuInfoByURL(String url);

    void save(MenuInfo menuInfo);

    void save(MenuInfo menuInfo, boolean isMne);

    void publishRefMenuOperate(String operateCode, String name, String entityCode, String viewCode, String url, boolean enableSpecialPermission, MenuInfo menuInfo);

    void deleteMenuOperateByEntity(String entityCode);

    void deleteMenuOperateByEntityPhysical(String entityCode);

    MenuInfo getParent(Long id);

    boolean isContainsWorkflow(String viewCode);

    void batchDealMenuInfoMne(String moduleCode, String artifact);

    List<MenuInfo> getMenuInfoList(String menuCode);

    Collection<MenuInfo> getChildren(MenuInfo menuInfo, Company... companys);

    List<String> getMenuOperateByFlowKey(String key, String version);

    *//**
     * 根据流程删除操作物理删除
     *
     * @param key
     * @param version
     *//*
    void deleteMenuOperateByFlowPhysical(String key, String version);

    void deleteMenuOperateByFlow(String key, String version);

    MenuOperate getMenuOperateByFlow(String processKey, String processVersion, String activeCode);

    void saveMenuOperate(MenuOperate operate);*/

}
