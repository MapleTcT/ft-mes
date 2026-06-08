/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.Criterion;

import java.util.List;
import java.util.Map;

public interface MenuOperateService {

    /**
     * 根据entityCode和powerFlag获取
     * @return
     */
    List<MenuOperate> getByEntityCode(String entityCode,Integer powerFlag);

    /**
     * 根据entityCode和id获取
     */
    List<MenuOperate> findByEntityCodeAndNotId(String entityCode, Long id);

    /**
     * 根据menuInfoId或者cid获取
     */
    List<MenuOperate> findByMenuInfoIdOrCids(Long menuInfoId, List<Long> cids);

    /**
     * 根据menuInfoIds删除
     */
    void deleteByMenuInfoIds(List<Long> menuInfoIds);

    MenuOperate load(Long id);

    void save(MenuOperate mo);

    /**
     * 根据id删除
     */
    void delete(Long id);

    List<MenuOperate> getByCode(String code, Long cid);

    /**
     * 根据code和cid
     */
    List<MenuOperate> findMenuOperateByCodeAndCid(String code, List<Long> cids);

    MenuOperate getFlowList(String entityCode);

    List<MenuOperate> findMenuOperates(Criterion... criterions);

    /**
     * 根据code或者menuInfoID获取
     */
    List<MenuOperate> findMenuOperatesByCodeOrMenuInfoId(String code, Long menuInfoId);

    /**
     * 物理删除菜单操作
     *
     * @param code 操作code
     * @return
     */
    void deleteMenuOperateByPhysical(String code);

    List<MenuOperate> getByCodes(List<String> codes, Long companyId);


    List<Map<String, Object>> getS2BillInfo(Long userId, Long menuInfoId);

    List<MenuOperate> getOperateByCodeAndFlowKey(String code, String flowKey, Company company);

    List<MenuOperate> getMenuOperateByNamespace(String nameSpace);

    Page<MenuOperate> getByPage(Page<MenuOperate> page, String sql, Map<String, Object> paramsMap, Object... objects);

    void updateOtherMenuOperate(MenuOperate menuOperate);

    Boolean checkWhetherIsConfiged(String viewCode);

    List<MenuOperate> getAllOperateByMenu(MenuInfo menuInfo, Company company);

    MenuOperate getMenuOperate(Long deploymentId, String code);

    MenuOperate getMenuOperate(String code);

}
