package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.supcon.supfusion.rbac.dao.bo.UserPermissionBO;
import com.supcon.supfusion.rbac.dao.po.UserPermissionPO;
import com.supcon.supfusion.rbac.dao.provider.UserPermissionProvider;
import com.supcon.supfusion.rbac.dao.query.FlowPermissionQuery;
import com.supcon.supfusion.rbac.dao.query.UserPermissionQuery;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public interface UserPermissionMapper extends BaseMapper<UserPermissionPO> {

    @DeleteProvider(value = UserPermissionProvider.class,method = "deleteUserPermission")
    void deleteUserPermission(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    @SelectProvider(value = UserPermissionProvider.class,method = "getNewPositionPermissionList")
    @Results({
            @Result(property = "uid",column = "upid",javaType = Long.class),
            @Result(property = "pid",column = "pid",javaType = Long.class),
            @Result(property = "includeLower",column = "includeLower",javaType = Integer.class),
    })
    List<Map<String,Object>> getNewPositionPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    @SelectProvider(value = UserPermissionProvider.class,method = "getNewStaffPermissionList")
    @Results({
            @Result(property = "uid",column = "upid",javaType = Long.class),
            @Result(property = "sid",column = "sid",javaType = Long.class),
    })
    List<Map<String,Object>> getNewStaffPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    @SelectProvider(value = UserPermissionProvider.class,method = "getNewDeptPermissionList")
    @Results({
            @Result(property = "did",column = "did",javaType = Long.class),
            @Result(property = "uid",column = "upid",javaType = Long.class),
    })
    List<Map<String,Object>> getNewDeptPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    @SelectProvider(value = UserPermissionProvider.class,method = "getNewCustomPermissionList")
    @Results({
            @Result(property = "uid",column = "upid",javaType = Long.class),
    })
    List<Map<String,Object>> getNewCustomPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    @SelectProvider(value = UserPermissionProvider.class,method = "getNewDataPermissionList")
    @Results({
            @Result(property = "uid",column = "upid",javaType = Long.class),
    })
    List<Map<String,Object>> getNewDataPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

//    List<UserPermissionBO> getFlowPermissionList(FlowPermissionQuery flowPermissionQuery);

    List<UserPermissionBO> getUserPermissionList(UserPermissionQuery userPermissionQuery);

    @Select("SELECT up.ID,mo.MENUINFO_ID FROM rbac_userpermission up LEFT JOIN rbac_menuoperate mo ON up.MENUOPERATE_ID = mo.ID ${ew.customSqlSegment}")
    List<Map<String,Long>> findDefaultOperate(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT COUNT(1) FROM rbac_userpermission up LEFT JOIN rbac_menuoperate mo ON up.MENUOPERATE_ID = mo.ID ${ew.customSqlSegment}")
    Integer findCountByMenuInfoPermission(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT UP.ID ID,UP.DEPARTMENT_FLAG DEPARTMENTFLAG,UP.ASSIGN_DEPT_FLAG ASSIGNDEPTFLAG,UP.POSITION_FLAG POSITIONFLAG,UP.ASSIGN_POS_FLAG ASSIGNPOSFLAG,UP.ASSIGN_STAFF_FLAG ASSIGNSTAFFFLAG,UP.NO_RESTRICT_FLAG NORESTRICTFLAG, UP.CID CID "
            + " FROM rbac_userpermission UP WHERE UP.MENUOPERATE_ID in (SELECT M.ID FROM rbac_menuoperate M WHERE M.CODE=#{menuOperateCode} AND M.VALID=1) AND UP.USER_ID=#{userId}")
    @Results({
            @Result(column = "DEPARTMENTFLAG",property = "DEPARTMENTFLAG",javaType = Integer.class),
            @Result(column = "POSITIONFLAG",property = "POSITIONFLAG",javaType = Integer.class),
            @Result(column = "NORESTRICTFLAG",property = "NORESTRICTFLAG",javaType = Integer.class),
            @Result(column = "ASSIGNPOSFLAG",property = "ASSIGNPOSFLAG",javaType = Integer.class),
            @Result(column = "ASSIGNSTAFFFLAG",property = "ASSIGNSTAFFFLAG",javaType = Integer.class),
            @Result(column = "ASSIGNDEPTFLAG",property = "ASSIGNDEPTFLAG",javaType = Integer.class),
    })
    List<Map<String,Object>> findUserPermissionValidMenuOperate(@Param("menuOperateCode") String menuOperateCode,@Param("userId") Long userId);

    @Select("SELECT mo.CODE FROM rbac_userpermission up " +
            "LEFT JOIN rbac_menuoperate mo ON up.MENUOPERATE_ID = mo.ID " +
            "LEFT JOIN rbac_menuinfo mi ON mo.MENUINFO_ID = mi.ID " +
            "${ew.customSqlSegment}")
    List<String> findUserOperate(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mou.URL FROM rbac_userpermission up " +
            "LEFT JOIN rbac_menuoperatecode_url_ref mou ON up.MENUOPERATE_CODE = mou.MENUOPERATE_CODE " +
            "${ew.customSqlSegment}")
    List<String> queryUrlList(@Param(Constants.WRAPPER) Wrapper wrapper);
}
