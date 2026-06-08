package com.supcon.supfusion.notification.admin.manager.service;


import com.supcon.supfusion.rbac.api.dto.RoleDTO;

import java.util.List;

public interface RbacService {
    List<RoleDTO> getRoles(List<String> codes);
}
