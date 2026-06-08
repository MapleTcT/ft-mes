package com.supcon.supfusion.notification.admin.manager.service;


import com.supcon.supfusion.organization.api.dto.DepartmentDetailDTO;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;

import java.util.List;

public interface OrganizationService {
    List<PersonDetailDTO> getStaffs(List<String> codes);

    List<DepartmentDetailDTO> getDepartmentNames(List<String> codes);

    List<PositionDetailDTO> getPositionNames(List<String> codes);
}
