package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.bo.FlowPermissionBO;
import com.supcon.supfusion.rbac.dao.po.FlowPermissionPO;
import com.supcon.supfusion.rbac.dao.query.FlowPermissionQuery;

import java.util.List;

/**
 * <p>
 * 工作流数据权限表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public interface FlowPermissionMapper extends BaseMapper<FlowPermissionPO> {

    List<String> findUserFlowPermissionOperateCode(FlowPermissionQuery flowPermissionQuery);

    List<FlowPermissionBO> findFlowPermissionByUserId(FlowPermissionQuery flowPermissionQuery);
}
