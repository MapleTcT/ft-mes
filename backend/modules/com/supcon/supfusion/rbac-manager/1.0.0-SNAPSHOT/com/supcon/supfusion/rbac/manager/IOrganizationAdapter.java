package com.supcon.supfusion.rbac.manager;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.dto.CompanyResultDTO;

import java.util.List;

public interface IOrganizationAdapter {

    Result<CompanyResultDTO> findCompany(Long cid);

    List<Long> querySubPositionIdsByPositionId(List<Long> posIds);

    List<Long> querySubDepartmentIdsByDepartmentId(List<Long> deptIds);

    List<Long> queryPersonsDepartmentsByPersonIds(List<Long> personIds);

    List<Long> queryPersonsPositionsByPersonIds(List<Long> personIds);

    List<Long> queryAllCompanies();

}
