package com.supcon.supfusion.rbac.dao.provider;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import org.apache.ibatis.annotations.Param;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.ObjectUtils;

import java.util.List;

public class MenuInfoProvider {

    public String search(@Param("searchContent") String searchContent, @Param("cid") Long cid, @Param("condition") List<Long> condition) {
        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("SELECT DISTINCT RBAC_MENUINFO.ID, RBAC_MENUINFO.CODE CODE,RBAC_MENUINFO.NAME NAME")
                .append(" FROM  RBAC_MENUINFO,RBAC_MENU_MNECODE,RBAC_MENUINFO_COMPANY_REF WHERE RBAC_MENUINFO.ID=RBAC_MENU_MNECODE.MENU_INFO ")
                .append(" AND RBAC_MENUINFO_COMPANY_REF.MENUINFO_ID = RBAC_MENUINFO.ID")
                .append(" AND RBAC_MENUINFO_COMPANY_REF.COMPANY_ID = #{cid}")
                .append(" AND RBAC_MENU_MNECODE.LANGUAGE='" + RpcContext.getContext().getLanguage().toString() + "' ")
                .append(" AND RBAC_MENU_MNECODE.MNE_CODE LIKE (#{searchContent}) escape '&' AND RBAC_MENUINFO.VALID = 1")
                .append(" AND (BASE_MENUINFO_MNECODE.MENU_INFO NOT IN <foreach collection=\"condition\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">")
                .append("#{id}")
                .append("</foreach>");

        sql.append("</script>");
        return sql.toString();
    }
}
