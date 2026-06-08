package com.supcon.supfusion.rbac.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.rbac.dao.po.RbacUserDataPermissionPO;
import lombok.Data;

import java.util.List;

@Data
public class UserDataResourceResponseBO extends VO {
    private List<RbacUserDataPermissionPO> dataResouceVOS;
    private boolean controlled;
}
