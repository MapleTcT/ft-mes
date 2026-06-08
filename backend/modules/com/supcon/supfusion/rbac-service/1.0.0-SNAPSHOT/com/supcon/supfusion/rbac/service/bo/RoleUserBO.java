package com.supcon.supfusion.rbac.service.bo;

import com.supcon.supfusion.rbac.dao.po.RolePO;
import lombok.Data;

@Data
public class RoleUserBO {


    private Long id;

    private RolePO role;
}
