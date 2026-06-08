package com.supcon.supfusion.organization.service;

import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;

import java.util.List;

public interface CompanyPersonService {

    /**
     * 岗位调离，删除关系
     * @param positionIds
     * @param personId
     */
    void offDepartmentByPersonId(List<Long> positionIds, Long personId);

    /**
     * 删除人员时候，根据人员id删除所有关系
     * @param personId
     */
    void deletePerson(Long personId);

    List<CompanyPersonPO> getCompanyPersonByPersonId(Long personId);

    boolean saveBatchRel(List<CompanyPersonPO> tmpInsertComs);
}
