package com.supcon.supfusion.rbac.dao.provider;

import org.apache.ibatis.annotations.Param;
import org.springframework.util.ObjectUtils;

import java.util.List;

public class UserPStaffProvider {

    public String deleteUserPStaff(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId){
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("DELETE FROM rbac_userpstaff WHERE USERPERMISSION_ID in (SELECT up.ID FROM rbac_userpermission up WHERE up.CID=#{cid}");
        if (!ObjectUtils.isEmpty(opIds)){
            sql.append(" and up.MENUOPERATE_ID in <foreach collection=\"opIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">");
            sql.append("#{id}");
            sql.append("</foreach>");
        }
        sql.append(" and up.USER_ID in (select USER_ID from rbac_roleuser ru where ru.ROLE_ID = #{roleId}");
        if (!ObjectUtils.isEmpty(userId)){
            sql.append(" and ru.USER_ID = #{userId}");
        }else{
            sql.append(" and ru.valid = 1");
        }
        sql.append(")");
        sql.append(" and up.PURVIEW_TYPE=0)");
        sql.append("</script>");
        return sql.toString();
    }
}
