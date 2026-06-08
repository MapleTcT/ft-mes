package com.supcon.supfusion.rbac.dao.provider;

import org.apache.ibatis.annotations.Param;
import org.springframework.util.ObjectUtils;

import java.util.List;

public class UserPermissionProvider {

    public String deleteUserPermission(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId) {
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("DELETE FROM rbac_userpermission WHERE cid = #{cid}");
        if (!ObjectUtils.isEmpty(opIds)) {
            sql.append(" and MENUOPERATE_ID in <foreach collection=\"opIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">");
            sql.append("#{id}");
            sql.append("</foreach>");
        }
        sql.append(" and PURVIEW_TYPE=0 and USER_ID in (select USER_ID from rbac_roleuser ru where ru.ROLE_ID = #{roleId}");
        if (!ObjectUtils.isEmpty(userId)) {
            sql.append(" and ru.USER_ID = #{userId}");
        } else {
            sql.append(" and ru.valid = 1");
        }
        sql.append(")");
        sql.append("</script>");
        return sql.toString();
    }

    public String getNewPositionPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId) {
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("select distinct up.ID as upid,rpp.POSITION_ID pid,max(rpp.INCLUDE_LOWER) as includeLower");
        sql.append(" from rbac_rolepermission rp");
        sql.append(" left join rbac_rolepposition rpp on rpp.ROLEPERMISSION_ID = rp.ID");
        sql.append(" left join rbac_userpermission up on up.MENUOPERATE_ID = rp.MENUOPERATE_ID");
        sql.append(" left join rbac_roleuser ru on ru.USER_ID=up.USER_ID and rp.ROLE_ID = ru.ROLE_ID");
        sql.append(" left join rbac_role r on ru.ROLE_ID=r.ID");
        sql.append(" where ru.valid = 1 and r.CID = #{cid}");
        if (!ObjectUtils.isEmpty(opIds)) {
            sql.append(" and rp.MENUOPERATE_ID in <foreach collection=\"opIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">");
            sql.append("#{id}");
            sql.append("</foreach>");
        }
        sql.append(" and up.USER_ID in (select USER_ID from rbac_roleuser ru where ru.ROLE_ID = #{roleId})");
        if (!ObjectUtils.isEmpty(userId)) {
            sql.append(" and ru.USER_ID = #{userId}");
        }
        sql.append(" and up.ID not in (select upp.USERPERMISSION_ID from rbac_userpposition upp)");
        sql.append(" and up.ID not in (select tempup.ID from rbac_userpermission tempup where tempup.PURVIEW_TYPE=1)");
        sql.append(" and rp.ID = rpp.ROLEPERMISSION_ID  group by up.ID,rpp.POSITION_ID");
        sql.append("</script>");
        return sql.toString();
    }

    public String getNewStaffPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId) {
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("select distinct up.ID as upid,rps.STAFF_ID as sid from rbac_rolepermission rp,rbac_rolepstaff rps,rbac_userpermission up,rbac_roleuser ru,rbac_role r");
        sql.append(" where rps.ROLEPERMISSION_ID=rp.ID and ru.valid = 1 and rp.ROLE_ID = ru.ROLE_ID and r.ID = ru.ROLE_ID");
        sql.append(" and r.cid=#{cid} ");
        if (!ObjectUtils.isEmpty(opIds)) {
            sql.append(" and rp.MENUOPERATE_ID in <foreach collection=\"opIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">");
            sql.append("#{id}");
            sql.append("</foreach>");
        }
        sql.append(" and ru.USER_ID=up.USER_ID and up.MENUOPERATE_ID=rp.MENUOPERATE_ID");
        sql.append(" and up.USER_ID in (select USER_ID from rbac_roleuser ru where ru.ROLE_ID = #{roleId})");
        if (!ObjectUtils.isEmpty(userId)) {
            sql.append(" and ru.USER_ID = #{userId}");
        }
        sql.append(" and up.ID not in (select ups.USERPERMISSION_ID from rbac_userpstaff ups)");
        sql.append(" and up.ID not in (select tempup.ID from rbac_userpermission tempup where tempup.PURVIEW_TYPE=1)");
        sql.append(" and rp.ID = rps.ROLEPERMISSION_ID");
        sql.append("</script>");
        return sql.toString();
    }

    public String getNewDeptPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId) {
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("select distinct up.ID as upid,rpd.DEPARTMENT_ID as did from rbac_rolepermission rp,rbac_rolepdepartment rpd,rbac_userpermission up,rbac_roleuser ru,rbac_role r");
        sql.append(" where rpd.ROLEPERMISSION_ID=rp.ID and ru.valid = 1 and rp.ROLE_ID = ru.ROLE_ID and r.ID = ru.ROLE_ID");
        sql.append(" and r.cid=#{cid} ");
        if (!ObjectUtils.isEmpty(opIds)) {
            sql.append(" and rp.MENUOPERATE_ID in <foreach collection=\"opIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">");
            sql.append("#{id}");
            sql.append("</foreach>");
        }
        sql.append(" and ru.USER_ID=up.USER_ID and up.MENUOPERATE_ID=rp.MENUOPERATE_ID");
        sql.append(" and up.USER_ID in (select USER_ID from rbac_roleuser ru where ru.ROLE_ID = #{roleId})");
        if (!ObjectUtils.isEmpty(userId)) {
            sql.append(" and ru.USER_ID = #{userId}");
        }
        sql.append(" and up.ID not in (select ups.USERPERMISSION_ID from rbac_userpstaff ups)");
        sql.append(" and up.ID not in (select tempup.ID from rbac_userpermission tempup where tempup.PURVIEW_TYPE=1)");
        sql.append(" and rp.ID = rpd.ROLEPERMISSION_ID");
        sql.append("</script>");
        return sql.toString();
    }

    public String getNewCustomPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId) {
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("select distinct up.ID as upid,rps.CUSTOM_PERMISSION_CODE as cpc from rbac_rolepermission rp,rbac_role_custompermission_ref rps,rbac_userpermission up,rbac_roleuser ru,rbac_role r");
        sql.append(" where rps.ROLEPERMISSION_ID=rp.ID and ru.valid = 1 and rp.ROLE_ID = ru.ROLE_ID and r.ID = ru.ROLE_ID");
        sql.append(" and r.cid=#{cid} ");
        if (!ObjectUtils.isEmpty(opIds)) {
            sql.append(" and rp.MENUOPERATE_ID in <foreach collection=\"opIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">");
            sql.append("#{id}");
            sql.append("</foreach>");
        }
        sql.append(" and ru.USER_ID=up.USER_ID and up.MENUOPERATE_ID=rp.MENUOPERATE_ID");
        sql.append(" and up.USER_ID in (select USER_ID from rbac_roleuser ru where ru.ROLE_ID = #{roleId})");
        if (!ObjectUtils.isEmpty(userId)) {
            sql.append(" and ru.USER_ID = #{userId}");
        }
        sql.append(" and up.ID not in (select ups.USERPERMISSION_ID from rbac_user_custompermission_ref ups)");
        sql.append(" and up.ID not in (select tempup.ID from rbac_userpermission tempup where tempup.PURVIEW_TYPE=1)");
        sql.append(" and rp.ID = rps.ROLEPERMISSION_ID");
        sql.append("</script>");
        return sql.toString();
    }

    public String getNewDataPermissionList(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId) {
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("select distinct up.ID as upid,rps.DATA_PERMISSION_CODE as dpc,rps.CONTENT from rbac_rolepermission rp,rbac_role_datapermission rps,rbac_userpermission up,rbac_roleuser ru,rbac_role r");
        sql.append(" where rps.ROLEPERMISSION_ID=rp.ID and ru.valid = 1 and rp.ROLE_ID = ru.ROLE_ID and r.ID = ru.ROLE_ID");
        sql.append(" and r.cid=#{cid} ");
        if (!ObjectUtils.isEmpty(opIds)) {
            sql.append(" and rp.MENUOPERATE_ID in <foreach collection=\"opIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">");
            sql.append("#{id}");
            sql.append("</foreach>");
        }
        sql.append(" and ru.USER_ID=up.USER_ID and up.MENUOPERATE_ID=rp.MENUOPERATE_ID");
        sql.append(" and up.USER_ID in (select USER_ID from rbac_roleuser ru where ru.ROLE_ID = #{roleId})");
        if (!ObjectUtils.isEmpty(userId)) {
            sql.append(" and ru.USER_ID = #{userId}");
        }
        sql.append(" and up.ID not in (select ups.USERPERMISSION_ID from rbac_user_datapermission ups)");
        sql.append(" and up.ID not in (select tempup.ID from rbac_userpermission tempup where tempup.PURVIEW_TYPE=1)");
        sql.append(" and rp.ID = rps.ROLEPERMISSION_ID");
        sql.append("</script>");
        return sql.toString();
    }

    public String getFlowPermissionList(){
        StringBuilder userpmSql = new StringBuilder();
        userpmSql.append(" SELECT FP.ID, MO.ID AS MENUOPERATE_ID,MO.CODE AS MENUOPERATE_CODE, FP.TYPE_ID AS USER_ID, FP.GROUP_POWER_FLAG AS GROUP_FLAG, ");
        userpmSql.append(" FP.POSITION_POWER_FLAG AS POSITION_FLAG, ASSIGN_POS_FLAG AS ASSIGN_POS_FLAG,");
        userpmSql.append(" ASSIGN_STAFF_FLAG AS ASSIGN_STAFF_FLAG, FP.UNLIMITED_POWER AS NO_RESTRICT_FLAG,0 as DEALER_PERMISSION_FLAG,0 AS ASSIGN_CUSTOMPERMISSION_FLAG,0 AS ASSIGN_DATAPERMISSION_FLAG,0 as TYPEFLAG , 1 as PURVIEWTYPE,MO.FLOW_KEY as FLOWKEY ");
        userpmSql.append(" FROM rbac_flow_permission FP JOIN rbac_menuoperate MO ON FP.ACTIVITY_CODE = MO.CODE");
        userpmSql.append(" INNER JOIN rbac_menuinfo MI ON MO.MENUINFO_ID = MI.ID");
        userpmSql.append(" ${ew.customSqlSegment}");
        return userpmSql.toString();
    }
    public String getUserPermissionList(){
        StringBuilder dataSql = new StringBuilder();
        dataSql.append(" SELECT UP.ID, UP.MENUOPERATE_ID,UP.MENUOPERATE_CODE, UP.USER_ID, UP.GROUP_FLAG, UP.POSITION_FLAG, UP.ASSIGN_POS_FLAG, UP.DEPARTMENT_FLAG,UP.ASSIGN_DEPT_FLAG,");
        dataSql.append(" UP.ASSIGN_STAFF_FLAG, UP.NO_RESTRICT_FLAG,UP.DEALER_PERMISSION_FLAG,UP.ASSIGN_CUSTOMPERMISSION_FLAG,UP.ASSIGN_DATAPERMISSION_FLAG,1 as TYPEFLAG, UP.PURVIEW_TYPE as PURVIEWTYPE ,MO.FLOW_KEY as FLOWKEY");
        dataSql.append(" FROM rbac_userpermission  UP INNER JOIN rbac_menuoperate MO ON UP.MENUOPERATE_ID = MO.ID INNER JOIN rbac_menuinfo MI ON MO.MENUINFO_ID = MI.ID");
        dataSql.append(" ${ew.customSqlSegment}");
        return dataSql.toString();
    }

}
