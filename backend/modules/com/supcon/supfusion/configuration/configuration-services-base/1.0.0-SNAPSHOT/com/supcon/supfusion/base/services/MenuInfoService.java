/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;

import java.util.Collection;
import java.util.List;

public interface MenuInfoService {

    /**
     * 正序获取
     */
    List<MenuInfo> getByAsc();

    /**
     * 根据code获取
     */
    MenuInfo getMenuInfoByCode(String code);

    /**
     * 根据id获取
     */
    MenuInfo getMenuInfoById(Long id);

    /**
     * 根据entityCode获取
     */
    List<MenuInfo> getByEntityCode(String entityCode);

    /**
     * 根据id删除
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据entityCode修改
     */
    void updateByEntityCode(String entityCode);

    /**
     * 根据entityCode删除
     */
    void deleteByEntityCode(String entityCode);

    MenuInfo load(Long id);

    MenuInfo get(String code);

    MenuInfo getMenusTree();

    List<MenuInfo> getMenuInfoByModul(String moduleCode);

    List<MenuOperate> getOperateList(MenuInfo menuInfo);

    List<MenuInfo> getEntityMenus(String entityCode);

    List<MenuInfo> getEntityMenus2(String entityCode);

    List<MenuInfo> getMenuInfoByURL(String url);

    void save(MenuInfo menuInfo);

    void publishRefMenuOperate(String operateCode, String name, String entityCode, String viewCode, String url, boolean enableSpecialPermission, MenuInfo menuInfo);

    void deleteMenuOperateByEntity(String entityCode);

    void deleteMenuOperateByEntityPhysical(String entityCode);

    MenuInfo getParent(Long id);

    boolean isContainsWorkflow(String viewCode);

    void batchDealMenuInfoMne(String moduleCode, String artifact);

    List<MenuInfo> getMenuInfoList(String menuCode);

    Collection<MenuInfo> getChildren(MenuInfo menuInfo, Company... companys);

    List<String> getMenuOperateByFlowKey(String key, String version);

    /**
     * 根据流程删除操作物理删除
     *
     * @param key
     * @param version
     */
    void deleteMenuOperateByFlowPhysical(String key, String version);

    void deleteMenuOperateByFlow(String key, String version);

    MenuOperate getMenuOperateByFlow(String processKey, String processVersion, String activeCode);

    void saveMenuOperate(MenuOperate operate);

    String deleteRbacAllByModuleCode(String code);

    void saveMenuInfoAndOperates(List<MenuInfo> menuInfos);

    /**
     * 查找菜单（valid可能为false）
     * @param code
     * @return
     */
    public MenuInfo getMenuInfo(String code);
}
