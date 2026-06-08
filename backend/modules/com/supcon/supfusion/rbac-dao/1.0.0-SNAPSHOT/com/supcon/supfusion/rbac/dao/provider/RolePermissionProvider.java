package com.supcon.supfusion.rbac.dao.provider;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.util.ObjectUtils;

import java.util.List;

public class RolePermissionProvider {

    public String getFlowPermissionList(){
        StringBuilder userpmSql = new StringBuilder();
        userpmSql.append(" SELECT FP.ID, MO.ID AS MENUOPERATE_ID, FP.TYPE_ID AS ROLE_ID, FP.GROUP_POWER_FLAG AS group_flag, ");
        userpmSql.append(" FP.POSITION_POWER_FLAG AS position_flag, ASSIGN_POS_FLAG AS ASSIGN_POS_FLAG,");
        userpmSql.append(" ASSIGN_STAFF_FLAG AS ASSIGN_STAFF_FLAG, FP.UNLIMITED_POWER AS NO_RESTRICT_FLAG,0 as DEALER_PERMISSION_FLAG,0 AS ASSIGN_CUSTOMPERMISSION_FLAG,0 AS ASSIGN_DATAPERMISSION_FLAG,0 as TYPEFLAG ,MO.FLOW_KEY as FLOWKEY ");
        userpmSql.append(" FROM rbac_flow_permission FP JOIN rbac_menuoperate MO ON FP.ACTIVITY_CODE = MO.CODE AND FP.FLOW_KEY = MO.FLOW_KEY");
        userpmSql.append(" INNER JOIN rbac_menuinfo MI ON MO.MENUINFO_ID = MI.ID");
        userpmSql.append(" ${ew.customSqlSegment}");
        return userpmSql.toString();
    }


    public String getRolePermissionList(){
        StringBuilder dataSql = new StringBuilder();
        dataSql.append(" SELECT RP.ID, RP.MENUOPERATE_ID, RP.ROLE_ID, RP.GROUP_FLAG, RP.POSITION_FLAG, RP.ASSIGN_POS_FLAG,RP.DEPARTMENT_FLAG,RP.ASSIGN_DEPT_FLAG,");
        dataSql.append(" RP.ASSIGN_STAFF_FLAG, RP.NO_RESTRICT_FLAG,RP.DEALER_PERMISSION_FLAG,RP.ASSIGN_CUSTOMPERMISSION_FLAG,RP.ASSIGN_DATAPERMISSION_FLAG,1 as TYPEFLAG ,MO.FLOW_KEY as FLOWKEY");
        dataSql.append(" FROM rbac_rolepermission  RP INNER JOIN rbac_menuoperate MO ON RP.MENUOPERATE_ID = MO.ID INNER JOIN rbac_menuinfo MI ON MO.MENUINFO_ID = MI.ID");
        dataSql.append(" ${ew.customSqlSegment}");
        return dataSql.toString();
    }

    public String getNewPermissionList(@Param("cid") Long cid,@Param("opIds") List<Long> opIds,@Param("roleId") Long roleId,@Param("userId") Long userId){
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("select ru.USER_ID,\n" +
                "       rp.MENUOPERATE_ID,\n" +
                "       rp.URL_PATTERN,\n" +
                "       mo.CODE,\n" +
                "       max(rp.GROUP_FLAG) as GROUP_FLAG,\n" +
                "       max(rp.POSITION_FLAG) as POSITION_FLAG,\n" +
                "       max(rp.ASSIGN_POS_FLAG) as ASSIGN_POS_FLAG,\n" +
                "       max(rp.ASSIGN_STAFF_FLAG) as ASSIGN_STAFF_FLAG,\n" +
                "       max(rp.NO_RESTRICT_FLAG) as NO_RESTRICT_FLAG,\n" +
                "       max(rp.DEALER_PERMISSION_FLAG) as DEALER_PERMISSION_FLAG,\n" +
                "       max(rp.ASSIGN_DEPT_FLAG) as ASSIGN_DEPT_FLAG,\n" +
                "       max(rp.DEPARTMENT_FLAG) as DEPARTMENT_FLAG");
        sql.append(" from rbac_rolepermission rp");
        sql.append(" left join rbac_roleuser ru on rp.ROLE_ID = ru.ROLE_ID");
        sql.append(" left join rbac_role r on rp.ROLE_ID = r.ID");
        sql.append(" left join rbac_menuoperate mo on mo.ID = rp.MENUOPERATE_ID");
        sql.append(" where ru.valid = 1 and r.CID = #{cid}");
        if (!ObjectUtils.isEmpty(opIds)){
            sql.append(" and rp.MENUOPERATE_ID in <foreach collection=\"opIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">");
            sql.append("#{id}");
            sql.append("</foreach>");
        }
        sql.append(" group by ru.USER_ID, rp.MENUOPERATE_ID, rp.URL_PATTERN,mo.CODE");
        sql.append("</script>");
        return sql.toString();
    }
}
