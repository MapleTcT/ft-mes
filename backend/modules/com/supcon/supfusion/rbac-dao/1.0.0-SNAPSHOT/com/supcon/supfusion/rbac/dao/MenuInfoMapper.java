package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoSimplePO;
import com.supcon.supfusion.rbac.dao.po.MenuSuposPO;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.provider.MenuInfoProvider;
import com.supcon.supfusion.rbac.dao.provider.RolePermissionProvider;
import com.supcon.supfusion.rbac.dao.query.MenuInfoQuery;
import org.apache.ibatis.annotations.*;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 菜单表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface MenuInfoMapper extends BaseMapper<MenuInfoPO> {

    /**
     * 根据ids查询简单数据
     *
     * @param ids
     * @return
     */
    List<MenuInfoSimplePO> findSimpleMenusByIds(List<Long> ids);
    /**
     * 根据菜单ids来查询
     *
     * @param ids
     * @return
     */
    List<MenuInfoPO> findMenusByIds(List<Long> ids);
    List<MenuSuposPO> findSuposMenusByIds(List<Long> ids);

    List<MenuInfoPO> findMenusByBatchIds(List<List<Long>> batchIds);
    List<MenuSuposPO> findSuposMenusByBatchIds(List<List<Long>> batchIds);

    /**
     * 查询用户有权限的流程菜单id
     *
     * @param userId
     * @param status
     * @return
     */
    List<String> findUserPermissionFlowMenuLayrecs(@Param("userId") Long userId, @Param("status") Integer status);
    /**
     * 查询用户有权限的流程操作菜单
     *
     * @param userId
     * @param status
     * @return
     */
    List<MenuInfoSimplePO> findUserPermissionFlowSimpleMenus(@Param("userId") Long userId, @Param("status") Integer status);
    /**
     * 获得当前用户的有权限的流程菜单
     *
     * @param userId
     * @return
     */
    List<MenuInfoPO> findUserPermissionFlowMenus(@Param("userId") Long userId, @Param("status") Integer status);
    /**
     * 统计当前用户有权限的菜单数量
     *
     * @param userId
     * @param cid
     * @return
     */
    int countUserPermissionMenus(@Param("userId") Long userId, @Param("cid") Long cid);

    /**
     * 查询用户有权限的菜单的lay_rec
     *
     * @param menuInfoQuery
     * @return
     */
    List<String> findUserMenusLayRecs(MenuInfoQuery menuInfoQuery);
    /**
     * 查询是用户有权限的菜单
     *
     * @param menuInfoQuery
     * @return
     */
    List<MenuInfoSimplePO> findUserSimpleMenus(MenuInfoQuery menuInfoQuery);
    /**
     * @description: 查询用户登陆菜单信息
     * @param: userId
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/6/18
     */
    List<MenuInfoPO> findUserMenus(MenuInfoQuery menuInfoQuery);

    List<MenuInfoPO> findMenus(MenuInfoQuery menuInfoQuery);

    @Select("SELECT mi.* FROM rbac_menuinfo mi ${ew.customSqlSegment}")
    @Results({
            @Result(column = "create_time",property = "createTime",typeHandler = UTCToStringTypeHandler.class),
            @Result(column = "modify_time",property = "modifyTime",typeHandler = UTCToStringTypeHandler.class),
    })
    MenuInfoPO getMenu(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT FULL_PATH_NAME FROM rbac_menuinfo WHERE ID = #{id}")
    String findMenuInfoFullPathName(@Param("id") Long id);

    @Update("UPDATE rbac_menuinfo SET VALID = 1 ${ew.customSqlSegment}")
    void recoverMenuInfo(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Update("UPDATE rbac_menuinfo t SET t.PARENT_ID = NULL ${ew.customSqlSegment}")
    void updateMenuInfoByEntityCode(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mi.ID,mi.PARENT_ID,mo.MENUINFO_ID,mi.CID,mi.CODE,mi.memo,mi.name,mi.url,mi.type,mi.target,mi.source,mi.show_Type,mi.menu_Type,mi.IS_HIDE FROM rbac_userpermission up " +
            "JOIN rbac_menuoperate mo ON up.MENUOPERATE_ID = mo.ID " +
            "RIGHT JOIN rbac_menuinfo mi ON mi.ID = mo.MENUINFO_ID " +
            "LEFT JOIN rbac_menuinfo_company_ref mc ON mi.ID = mc.MENUINFO_ID "+
            "${ew.customSqlSegment}")
    List<MenuInfoPO> checkUserMenus(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mi.ID, mi.NAME,mi.CODE,mi.APP FROM rbac_menuinfo mi LEFT JOIN rbac_menuinfo_company_ref mcf ON mi.ID = mcf.MENUINFO_ID LEFT JOIN rbac_menu_mnecode mm ON mi.ID = mm.MENU_INFO ${ew.customSqlSegment}")
    Page<MenuInfoPO> findMenusByKeyword(Page<MenuInfoPO> page, @Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mi.ID,mi.SORT,mi.LAY_REC,mi.APP,mi.CODE,mi.NAME,mi.PARENT_ID,mi.CID,mi.MEMO,mi.URL,mi.TYPE,mi.TARGET,mi.SOURCE,mi.SHOW_TYPE,mi.MENU_TYPE,mi.IS_HIDE FROM rbac_menuinfo mi LEFT JOIN rbac_menuinfo_company_ref mcf ON mi.ID = mcf.MENUINFO_ID LEFT JOIN rbac_menu_mnecode mm ON mi.ID = mm.MENU_INFO ${ew.customSqlSegment}")
    List<MenuInfoPO> findMenusByKeywordNoPage(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT DISTINCT mi.ID, mi.CODE CODE,mi.NAME NAME, mi.APP, mi.PARENT_ID " +
            "FROM  RBAC_MENUINFO mi " +
            "LEFT JOIN RBAC_MENU_MNECODE mne ON mi.ID = mne.MENU_INFO " +
            "LEFT JOIN RBAC_MENUINFO_COMPANY_REF mcr ON mi.ID = mcr.MENUINFO_ID " +
            "${ew.customSqlSegment}")
    @Results({
            @Result(column = "ID",property = "id",javaType = Long.class),
            @Result(column = "CODE",property = "code",javaType = String.class),
            @Result(column = "NAME",property = "name",javaType = String.class),
            @Result(column = "APP",property = "app",javaType = String.class),
            @Result(column = "PARENT_ID",property = "parentId",javaType = Long.class),
    })
    List<Map<String, Object>> search(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT DISTINCT mi.ID, mi.CODE CODE,mi.NAME NAME, mi.APP, mi.PARENT_ID " +
            "FROM RBAC_MENUINFO mi ${ew.customSqlSegment}")
    @Results({
            @Result(column = "ID",property = "id",javaType = Long.class),
            @Result(column = "CODE",property = "code",javaType = String.class),
            @Result(column = "NAME",property = "name",javaType = String.class),
            @Result(column = "APP",property = "app",javaType = String.class),
            @Result(column = "PARENT_ID",property = "parentId",javaType = Long.class),
    })
    List<Map<String, Object>> findMenusByCde(@Param(Constants.WRAPPER) Wrapper wrapper);

    @DeleteMapping("DELETE FROM rbac_menuinfo ${ew.customSqlSegment}")
    void deletePhysics(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mi.ID,mi.SORT,mi.route,mi.PARENT_ID,mo.MENUINFO_ID,mi.CID,mi.CODE,mi.memo,mi.name,mi.url,mi.type,mi.target,mi.source,mi.show_Type,mi.menu_Type,mi.IS_HIDE" +
            " FROM RBAC_MENUINFO mi" +
            " LEFT JOIN RBAC_MENUOPERATE mo ON mo.MENUINFO_ID =  mi.ID" +
            " ${ew.customSqlSegment}")
    List<MenuInfoPO> findMenuByOperateCode(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mi.* FROM rbac_menuinfo mi ${ew.customSqlSegment}")
    List<MenuInfoPO> getAllMenu(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT mi.* FROM rbac_menuinfo mi left join rbac_app_ref raf on mi.id = raf.menuId ${ew.customSqlSegment}")
    List<MenuInfoPO> getMenuByAppRefAndSource(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT appid from rbac_menuinfo ${ew.customSqlSegment}")
    String getAppIdByCode(@Param(Constants.WRAPPER) Wrapper wrapper);


    @Select("SELECT id from rbac_menuinfo ${ew.customSqlSegment}")
    List<Long> getMenuIdByAppIds(@Param(Constants.WRAPPER) Wrapper wrapper);

    List<MenuInfoPO> findMenuByOperateCode(MenuInfoQuery menuInfoQuery);

    @Select("SELECT id, parent_id, code, name, url, show_type, target, name_display, route, css_class, sort, menu_type, lay_rec, lay_no from rbac_menuinfo ${ew.customSqlSegment}")
    List<MenuSuposPO> getWhiteListMenus(@Param(Constants.WRAPPER) Wrapper wrapper);
}
