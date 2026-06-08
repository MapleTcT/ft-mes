package com.supcon.supfusion.rbac.manager.impl;

import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.dto.CompanyResultDTO;
import com.supcon.supfusion.organization.api.dto.DepartmentDetailDTO;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;
import com.supcon.supfusion.rbac.manager.IOrganizationAdapter;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.CompanyDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationAdapterImpl implements IOrganizationAdapter {

    @Autowired
    private PersonApiService personApiService;

    public Result<CompanyResultDTO> findCompany(Long cid){
        return personApiService.findCompany(cid);
    }

    @Override
    public List<Long> querySubPositionIdsByPositionId(List<Long> posIds) {
        return (List<Long>) personApiService.querySubPositionIdsByPositionId(posIds).getList();
    }

    @Override
    public List<Long> querySubDepartmentIdsByDepartmentId(List<Long> deptIds) {
        return (List<Long>) personApiService.querySubDepartmentIdsByDepartmentId(deptIds).getList();
    }

    @Override
    public List<Long> queryPersonsDepartmentsByPersonIds(List<Long> personIds) {
        return personApiService.queryPersonsDepartmentsByPersonIds(personIds).getList().stream().map(DepartmentDetailDTO::getId).collect(Collectors.toList());
    }

    @Override
    public List<Long> queryPersonsPositionsByPersonIds(List<Long> personIds) {
        return personApiService.queryPersonsPositionsByPersonIds(personIds).getList().stream().map(PositionDetailDTO::getId).collect(Collectors.toList());
    }

    @Override
    public List<Long> queryAllCompanies() {
        ListResult<CompanyResultDTO> listResult = personApiService.queryAllCompanies();
        if (!ObjectUtils.isEmpty(listResult.getList())){
            return listResult.getList().stream().map(CompanyResultDTO::getId).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


}
