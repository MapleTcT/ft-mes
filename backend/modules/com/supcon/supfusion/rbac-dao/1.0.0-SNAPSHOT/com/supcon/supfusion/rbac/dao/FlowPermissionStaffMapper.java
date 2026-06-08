package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.FlowPermissionStaffPO;
import com.supcon.supfusion.rbac.dao.po.UserPStaffPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 标签表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-10
 */
public interface FlowPermissionStaffMapper extends BaseMapper<FlowPermissionStaffPO> {


    @Select("SELECT * FROM rbac_flow_permission_staff WHERE FLOWPERMISSION_ID = #{flowPermissionId}")
    List<FlowPermissionStaffPO> findByFlowPermissionId(@Param("flowPermissionId") Long flowPermissionId);
}
