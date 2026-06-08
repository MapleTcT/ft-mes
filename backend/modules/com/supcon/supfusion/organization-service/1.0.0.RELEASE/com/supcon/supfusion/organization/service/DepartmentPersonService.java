package com.supcon.supfusion.organization.service;

import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;

import java.util.List;

public interface DepartmentPersonService {
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

    /**
     * 根据人员id查询部门人员信息
     * @param personId
     * @return
     */
    List<DepartmentPersonPO> getdepartmentPersonByPersonId(Long personId);

    boolean saveBatchRel(List<DepartmentPersonPO> tmpInsertDepts);
}
