package com.supcon.supfusion.auth.service.excel.entity;

import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import lombok.Data;

/**
 * @author lifangyuan
 */
@Data
public class UserOrgRbacEntity {
    private UserBO userBO;
    private PersonDetailDTO personDetailDTO;
    private RoleDTO roleDTO;
    private Boolean update;
}
