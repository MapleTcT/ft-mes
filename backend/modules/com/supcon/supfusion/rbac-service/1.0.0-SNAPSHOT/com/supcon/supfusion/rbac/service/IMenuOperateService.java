package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.rbac.dao.bo.MenuOperatePermissionBO;
import com.supcon.supfusion.rbac.dao.po.MenuOperatePO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 操作表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface IMenuOperateService extends IService<MenuOperatePO> {
    PageResult<MenuOperatePO> getMenuOperates(Long menuinfoId, String keyword, Integer current, Integer pageSize);
    boolean save(MenuOperatePO menuOperate);
    void deleteMenuOperates(String codes);
    List<MenuOperatePermissionBO> getAssignMenuOperateUser(Long menuId);
    List<MenuOperatePermissionBO> getAssignMenuOperateUserFromRole(Long menuId, Long userId);
    List<MenuOperatePO> getMenuOperateWithoutRolePermission(List<String> menuOperateCodes,Long userId);
    MenuOperatePO getOne(String code);
    List<MenuOperatePO> findMenuOperateByMenuInfo(String code, Long id);
    List<MenuOperatePO> findMenuOperateByMenuCodeAndCids(String code, List<Long> cids,Long menuInfoId);
    void deleteByCodePhysics(List<String> codes);
    void cascadeDeleteMenuOperate(List<MenuOperatePO> menuOperatePOS);
    List<MenuOperatePO> getByCode(String code, Long cid);
    List<MenuOperatePO> getOperateListByUserIdMenuId(Long userId, String menuCode);
}
