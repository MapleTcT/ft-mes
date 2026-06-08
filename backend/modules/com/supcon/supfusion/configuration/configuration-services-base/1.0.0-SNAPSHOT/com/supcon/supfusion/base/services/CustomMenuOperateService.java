package com.supcon.supfusion.base.services;


import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.rbac.api.dto.MenuOperateGroupRestrictDTO;

import java.util.List;

public interface CustomMenuOperateService {

    /**
     * 保存操作
     *
     * @param mo
     */
    void save(MenuOperate mo);

    /**
     * 根据id删除操作
     */
    void delete(Long id);


    /**
     * 根据menuInfoIds删除操作
     */
    void deleteByMenuInfoIds(List<Long> menuInfoIds);

    /**
     * 根据CODE批量物理删除操作
     */
    void deleteByCodePhysics(List<String> codes);

    /**
     * 根据实体编码和一些有关实体编码的条件更新操作组限制
     */
    void updateOperateGroupRestrictByEntityCodeAndOther(MenuOperateGroupRestrictDTO menuOperateGroupRestrictDTO);

    /**
     * 根据实体编码更新操作组限制
     */
    void updateOperateGroupRestrictByEntityCode(MenuOperateGroupRestrictDTO menuOperateGroupRestrictDTO);

    /**
     * 生成工作流操作URL
     * @param deploymentId 工作流Id
     * @param entityCode 实体编码
     * @param menuId 菜单id
     */
    void generateWFOperateUrl(Long deploymentId, String entityCode, Long menuId);


/**
 * 根据entityCode和powerFlag获取
 *
 * @return
 *//*

    List<MenuOperate> getByEntityCode(String entityCode, Integer powerFlag);

    */
/**
 * 根据entityCode和id获取
 *//*

    List<MenuOperate> findByEntityCodeAndNotId(String entityCode, Long id);

    */
/**
 * 根据menuInfoId或者cid获取
 *//*

    List<MenuOperate> findByMenuInfoIdOrCids(Long menuInfoId, List<Long> cids);


    MenuOperate load(Long id);


    List<MenuOperate> getByCode(String code, Company company);

    */
/**
 * 根据code和cid
 *//*

    List<MenuOperate> findMenuOperateByCodeAndCid(String code, List<Long> cids);

    MenuOperate getFlowList(String entityCode);

    List<MenuOperate> findMenuOperates(Criterion... criterions);

    */
/**
 * 根据code或者menuInfoID获取
 *//*

    List<MenuOperate> findMenuOperatesByCodeOrMenuInfoId(String code, Long menuInfoId);
*/


/**
 * 物理删除菜单操作
 *
 * @param code 操作code
 * @return
 *//*

    void deleteMenuOperateByPhysical(String code);

    List<MenuOperate> getByCodes(List<String> codes, Company company);

*/

  /*  List<Map<String, Object>> getS2BillInfo(Long userId, Long menuInfoId);

    List<MenuOperate> getOperateByCodeAndFlowKey(String code, String flowKey, Company company);

    List<MenuOperate> getMenuOperateByNamespace(String nameSpace);

    Page<MenuOperate> getByPage(Page<MenuOperate> page, String sql, Map<String, Object> paramsMap, Object... objects);

    void updateOtherMenuOperate(MenuOperate menuOperate);

    Boolean checkWhetherIsConfiged(String viewCode);

    List<MenuOperate> getAllOperateByMenu(MenuInfo menuInfo, Company company);

    MenuOperate getMenuOperate(Long deploymentId, String code);*/
}
