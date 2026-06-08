package com.supcon.supfusion.organization.dao.mapper.position;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionBaseInfoPO;
import com.supcon.supfusion.organization.dao.po.position.PositionDeptBasePO;
import com.supcon.supfusion.organization.dao.po.position.PositionSynchronizationInfoPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 岗位mapper
 */
public interface PositionMapper extends BaseMapper<PositionAddPO> {

    /**
     * 根据修改时间查询岗位列表
     * @param modifyTime
     * @param current
     * @param pageSize
     * @return
     */
    List<PositionSynchronizationInfoPO> getPositions(@Param("modifyTime") String modifyTime, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    Integer getPositionsCount(@Param("modifyTime") String modifyTime, @Param("dbType") String dbType);

    /**
     * 根据公司id查询岗位列表
     * @param companyId
     * @param current
     * @param pageSize
     * @param dbType
     * @return
     */
    List<PositionBaseInfoPO> getPositionsByCompanyId(@Param("companyId") Long companyId, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);


//    @Select("select parentPos.code as parentCode, curPos.id, curPos.code, curPos.name, curPos.valid, curPos.modify_time as modifyTime, curPos.description, curPos.full_path as fullPath, curPos.lay_no as layNo, curPos.sort,\n" +
//            " dept.code as deptCode, dept.name as deptName, company.code as companyCode, company.full_name as companyFullName, company.short_name as companyShortName\n" +
//            " from org_position curPos left join org_position parentPos on curPos.parent_id = parentPos.id\n" +
//            " left join org_department dept on curPos.dep_id = dept.id\n" +
//            " left join org_company company on curPos.company_id = company.id ${ew.customSqlSegment}")
    PositionSynchronizationInfoPO getPositionByCode(@Param("positionCode") String positionCode);


    @Select("select opr.role_id from org_position_role opr ${ew.customSqlSegment}")
    List<Long> getPositionRoleId(@Param(Constants.WRAPPER) Wrapper wrapper);

    List<PositionDeptBasePO> getPositionsByDeptId(@Param("deptId") Long deptId);
}
