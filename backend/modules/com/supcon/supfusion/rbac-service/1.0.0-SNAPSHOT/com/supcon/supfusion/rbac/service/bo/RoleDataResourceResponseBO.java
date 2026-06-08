package com.supcon.supfusion.rbac.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.rbac.dao.po.RbacRoleDataPermissionPO;
import lombok.Data;

import java.util.List;

@Data
public class RoleDataResourceResponseBO extends VO {
    private List<RbacRoleDataPermissionPO> dataResouceVOS;
    private boolean controlled;
}
