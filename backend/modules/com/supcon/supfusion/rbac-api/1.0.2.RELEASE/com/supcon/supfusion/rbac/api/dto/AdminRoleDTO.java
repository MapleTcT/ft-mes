package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

@Data
public class AdminRoleDTO extends DTO{
    private static final long serialVersionUID = -5069361211127805140L;

    private String companyCode;

    private Long cid;

    private Long userId;

    private String userName;
}
