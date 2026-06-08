package com.supcon.supfusion.notification.admin.manager.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.notification.admin.manager.service.OrganizationService;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.DepartmentDetailDTO;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class OrganizationServiceImpl implements OrganizationService {

    @ServiceApiReference
    private PersonApiService personApiService;

    @Override
    public List<PersonDetailDTO> getStaffs(List<String> codes) {
        log.info("personApiService.queryPersonByCodes param :{}", codes != null ? JSONArray.toJSONString(codes) : null);
        ListResult<PersonDetailDTO> personDetailDTOListResult = personApiService.queryPersonByCodes(codes);
        Collection<PersonDetailDTO> personDetailDTOS = personDetailDTOListResult.getList();
        log.info("personApiService.queryPersonByCodes return :{}", personDetailDTOS != null ? JSONArray.toJSONString(personDetailDTOS) : null);
        return new ArrayList(personDetailDTOS);
    }

    @Override
    public List<DepartmentDetailDTO> getDepartmentNames(List<String> codes) {
        log.info("personApiService.queryDepartmentByCodes param :{}", codes != null ? JSONArray.toJSONString(codes) : null);
        ListResult<DepartmentDetailDTO> departmentDetailDTOListResult = personApiService.queryDepartmentByCodes(codes);
        Collection<DepartmentDetailDTO> departmentDetailDTOS = departmentDetailDTOListResult.getList();
        log.info("personApiService.queryDepartmentByCodes return :{}", departmentDetailDTOS != null ? JSONArray.toJSONString(departmentDetailDTOS) : null);
        return new ArrayList(departmentDetailDTOS);
    }

    @Override
    public List<PositionDetailDTO> getPositionNames(List<String> codes) {
        log.info("personApiService.queryPositionByCodes param :{}", codes != null ? JSONArray.toJSONString(codes) : null);
        ListResult<PositionDetailDTO> positionDetailDTOListResult = personApiService.queryPositionByCodes(codes);
        Collection<PositionDetailDTO> positionDetailDTOS = positionDetailDTOListResult.getList();
        log.info("personApiService.queryPositionByCodes return :{}", positionDetailDTOS != null ? JSONArray.toJSONString(positionDetailDTOS) : null);
        return new ArrayList(positionDetailDTOS);
    }
}
