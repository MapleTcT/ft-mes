package com.supcon.supfusion.organization.dao.mapper.department;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentBaseInfoPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentSynchronizationInfoPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentSynchronizationManagerPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门mapper
 */
public interface DepartmentMapper extends BaseMapper<DepartmentAddPO> {

    /**
     * 查询部门列表
     * @param modifyTime
     * @param current
     * @param pageSize
     * @return
     */
    List<DepartmentSynchronizationInfoPO> getDepartments(@Param("modifyTime") String modifyTime, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    Integer getDepartmentsCount(@Param("modifyTime") String modifyTime, @Param("dbType") String dbType);

    /**
     * 根据部门id批量查询部门负责人
     * @param deptIds
     * @return
     */
    List<DepartmentSynchronizationManagerPO> getManagersByDeptIds(@Param("deptIds") List<Long> deptIds);


    /**
     * 根据公司id查询部门列表
     * @param companyId
     * @param current
     * @param pageSize
     * @param dbType
     * @return
     */
    List<DepartmentBaseInfoPO> getDepartmentsByCompanyId(@Param("companyId") Long companyId, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);


    DepartmentSynchronizationInfoPO getDepartmentByCode(@Param("departmentCode") String departmentCode);

    List<DepartmentSynchronizationManagerPO> getManagerByDeptId (@Param("deptId") Long deptId);


}
