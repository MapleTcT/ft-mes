package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.bo.FlowPermissionBO;
import com.supcon.supfusion.rbac.dao.po.FlowPermissionPO;
import com.supcon.supfusion.rbac.dao.po.MenuOperatePO;
import com.supcon.supfusion.rbac.dao.po.RolePermissionPO;
import com.supcon.supfusion.rbac.dao.po.UserPermissionPO;
import com.supcon.supfusion.rbac.dao.query.FlowPermissionQuery;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 工作流数据权限表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public interface IFlowPermissionService extends IService<FlowPermissionPO> {
    Map<String, List<Long>> getPendingPowerCondition(Long userId, String menuOperateCode, Long currentCompanyId);

    String generateBaseModelSql(Long userId, String menuOperateCode, Long currentCompanyId, String entityTable);

    //删除工作流权限相关数据
    void deleteFlowPermissionByUserPermission(List<UserPermissionPO> upList);
    //新增工作流权限相关数据
    void insertFlowPermissionByUserPermission(List<UserPermissionPO> upList, Map<Long, MenuOperatePO> operateMap);
    //删除工作流权限相关数据
    void deleteFlowPermissionByRolePermission(List<RolePermissionPO> rpList);
    //新增工作流权限相关数据
    void insertFlowPermissionByRolePermission(List<RolePermissionPO> rpList, Map<Long, MenuOperatePO> operateMap);
    //查询用户工作流权限操作
    List<String> findUserFlowPermissionOperateCode(FlowPermissionQuery flowPermissionQuery);

    List<FlowPermissionBO> findFlowPermissionByUserId(FlowPermissionQuery flowPermissionQuery);

}
