package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

import java.util.List;

@Data
public class RoleUserAddBatchDTO extends DTO{

    private static final long serialVersionUID = -6875355689864428935L;
    private Long userId;

    private String personName;

    private String userName;

    private String personCode;

    private List<Long> roleIds;
}
