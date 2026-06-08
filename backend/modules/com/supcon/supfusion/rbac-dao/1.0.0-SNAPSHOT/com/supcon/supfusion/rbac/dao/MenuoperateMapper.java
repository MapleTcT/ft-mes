package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.rbac.dao.bo.MenuOperatePermissionBO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.dao.po.MenuOperatePO;
import com.supcon.supfusion.rbac.dao.po.RolePermissionPO;
import com.supcon.supfusion.rbac.dao.query.MenuOperateQuery;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 操作表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface MenuoperateMapper extends BaseMapper<MenuOperatePO> {

    @Select("SELECT mo.id,mo.name,mo.code,mo.memo,mo.DEFAULT_OPERATE,mo.ENABLE_CUSTOMPERMISSION,mo.FOR_FLOW_PERMISSION,mo.ENABLE_DEALERPERMISSION,mo.ENABLE_ASSIGNSTAFF,mo.ENABLE_ASSIGNPOS,mo.ENABLE_POSRESTRICT,mo.ENABLE_ASSIGNDEPT,mo.ENABLE_DEPTRICT,mo.ENABLE_GROUPRESTRICT,mo.ICON_CLS FROM rbac_menuoperate mo LEFT JOIN rbac_menuinfo mi ON mi.ID = mo.MENUINFO_ID ${ew.customSqlSegment}")
    @Results({
            @Result(property = "code",column = "CODE"),
            @Result(property = "urls",column = "CODE",many = @Many(select = "com.supcon.supfusion.rbac.dao.MenuOperateCodeUrlRefMapper.getUrlByOperateCode")),
            @Result(property = "fullPathName",column = "MENUINFO_ID",one = @One(select = "com.supcon.supfusion.rbac.dao.MenuInfoMapper.findMenuInfoFullPathName"))
    })
    Page<MenuOperatePO> getMenuOperatePage(Page<MenuOperatePO> page, @Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT * FROM rbac_menuoperate ${ew.customSqlSegment}")
    @Results({
            @Result(property = "code",column = "CODE"),
            @Result(property = "urls",column = "CODE",many = @Many(select = "com.supcon.supfusion.rbac.dao.MenuOperateCodeUrlRefMapper.getUrlByOperateCode"))
    })
    List<MenuOperatePO> getOne(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT * FROM rbac_menuoperate WHERE code = #{code}")
    MenuOperatePO findMenuOperate(@Param("code") String code);

    @Select("select MI.ID AS MIID,MI.SORT,MI.LAY_NO,MI.PARENT_ID,MI.FULL_PATH,MI.NAME AS menuinfoName,MO.ID, MO.ACTION_URL, MO.CODE,MO.DEPLOYMENT_ID,MO.FLOW_VERSION,MO.ICON_CLS,MO.ENABLE_DEPTRICT,MO.ENABLE_ASSIGNDEPT," +
            "MO.MEMO,MO.MENUOPERATETYPE,MO.MODULE_CODE,MO.MSG_ASSEMBLED,MO.NAME,MO.NAMESPACE,MO.SORT,MO.TARGET," +
            "MO.URL,MO.VALID,MO.ROW_VERSION,MO.CID,MO.MENUINFO_ID,MO.POWER_FLAG,MO.ENABLE_ASSIGNPOS," +
            "MO.ENABLE_ASSIGNSTAFF,MO.ENABLE_GROUPRESTRICT,MO.ENABLE_NORESTRICT,MO.ENABLE_POSRESTRICT," +
            "MO.ENABLE_DEALERPERMISSION,MO.IGNORE_PERMISSION,MO.ENABLE_CUSTOMPERMISSION,MO.ENABLE_DATAPERMISSION," +
            "MO.VIEW_CODE,MO.IS_HIDDEN,MO.DEFAULT_OPERATE,MO.NAME_DISPLAY" +
            " from rbac_menuinfo MI" +
            " left join rbac_menuoperate MO on MO.MENUINFO_ID = MI.ID" +
            " left join rbac_menuinfo_company_ref MC on MC.MENUINFO_ID = MI.ID" +
            " ${ew.customSqlSegment}")
    @Results({
            @Result(column = "id",property = "id",javaType = Long.class),
            @Result(column = "sort",property = "sort",javaType = Double.class),
            @Result(column = "LAY_NO",property = "layNo",javaType = Integer.class),
            @Result(column = "PARENT_ID",property = "parentId",javaType = Long.class),
            @Result(column = "MIID",property = "miid",javaType = Long.class),
    })
    List<Map<String,Object>> getAssignedPermissionUser1(@Param(Constants.WRAPPER) Wrapper wrapper);

    List<MenuOperatePermissionBO> getAssignedPermissionUser(MenuOperateQuery menuOperateQuery);

    @Select("select MI.ID AS MIID,MI.SORT,MI.LAY_NO,MI.PARENT_ID,MI.FULL_PATH,MI.NAME AS menuinfoName,MO.ID, MO.ACTION_URL, MO.CODE,MO.DEPLOYMENT_ID,MO.FLOW_VERSION,MO.ICON_CLS,MO.ENABLE_DEPTRICT,MO.ENABLE_ASSIGNDEPT," +
            "MO.MEMO,MO.MENUOPERATETYPE,MO.MODULE_CODE,MO.MSG_ASSEMBLED,MO.NAME,MO.NAMESPACE,MO.SORT,MO.TARGET," +
            "MO.URL,MO.VALID,MO.ROW_VERSION,MO.CID,MO.MENUINFO_ID,MO.POWER_FLAG,MO.ENABLE_ASSIGNPOS," +
            "MO.ENABLE_ASSIGNSTAFF,MO.ENABLE_GROUPRESTRICT,MO.ENABLE_NORESTRICT,MO.ENABLE_POSRESTRICT," +
            "MO.ENABLE_DEALERPERMISSION,MO.IGNORE_PERMISSION,MO.ENABLE_CUSTOMPERMISSION,MO.ENABLE_DATAPERMISSION," +
            "MO.VIEW_CODE,MO.IS_HIDDEN,MO.DEFAULT_OPERATE,MO.NAME_DISPLAY" +
            " from rbac_menuinfo MI" +
            " left join rbac_menuoperate MO on MO.MENUINFO_ID = MI.ID" +
            " left join rbac_menuinfo_company_ref MC on MC.MENUINFO_ID = MI.ID" +
            " ${ew.customSqlSegment}")
    @Results({
            @Result(column = "id",property = "id",javaType = Long.class),
            @Result(column = "sort",property = "sort",javaType = Double.class),
            @Result(column = "LAY_NO",property = "layNo",javaType = Integer.class),
            @Result(column = "PARENT_ID",property = "parentId",javaType = Long.class),
            @Result(column = "MIID",property = "miid",javaType = Long.class),
    })
    List<Map<String,Object>> getAssignedPermissionRole(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mi.id,mi.code,mi.url,mi.cid FROM rbac_menuoperate mo right join rbac_menuinfo mi on mi.ID = mo.MENUINFO_ID ${ew.customSqlSegment}")
    @Results({
            @Result(column = "id",property = "id",javaType = Long.class),
            @Result(column = "code",property = "code"),
            @Result(column = "url",property = "url"),
            @Result(column = "cid",property = "cid",javaType = Long.class),
    })
    List<Map<String,Object>> getNoneDefaultOperateMenu(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mo.ID,mo.CODE,mo.CID FROM rbac_menuoperate mo ${ew.customSqlSegment}")
    List<MenuOperatePO> getMenuOperateWithoutRolePermission(@Param(Constants.WRAPPER) Wrapper wrapper);


    @Select("SELECT mo.* FROM rbac_menuoperate mo LEFT JOIN rbac_menuinfo mi ON mo.MENUINFO_ID = mi.ID ${ew.customSqlSegment}")
    List<MenuOperatePO> findMenuOperateByMenuInfo(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mo.MENUINFO_ID FROM rbac_userpermission up LEFT JOIN rbac_menuoperate mo ON up.MENUOPERATE_ID = mo.ID GROUP BY MENUINFO_ID")
    List<Long> findAssignPermissionMenuInfoId();

    @Delete("DELETE FROM rbac_menuoperate ${ew.customSqlSegment} ")
    void deleteByCodePhysics(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("select rm.id, rm.flow_key, rm.menuinfo_id ,rm.url ,rm.name ,rm.code ,rm.default_operate from rbac_menuoperate rm left join rbac_userpermission ru on rm.id = ru.menuoperate_id WHERE rm.valid = 1 and ru.user_id=#{userId} AND ru.cid = #{cid} AND rm.menuinfo_id IN (select id from rbac_menuinfo rm2 where rm2.code = #{menuCode})")
    List<MenuOperatePO> getOperateListByUserIdMenuId(@Param("userId") Long userId, @Param("menuCode") String menuCode, @Param("cid") Long cid);
}
