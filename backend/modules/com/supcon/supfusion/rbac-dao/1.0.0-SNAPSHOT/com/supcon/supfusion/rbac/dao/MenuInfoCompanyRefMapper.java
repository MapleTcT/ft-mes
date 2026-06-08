package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.rbac.dao.po.MenuInfoCompanyRefPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 菜单公司关联表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-30
 */
public interface MenuInfoCompanyRefMapper extends BaseMapper<MenuInfoCompanyRefPO> {

    @Select("SELECT MC.*,MI.NAME AS menuinfoName  FROM rbac_menuinfo_company_ref MC LEFT JOIN rbac_menuinfo MI ON MI.ID = MC.MENUINFO_ID ${ew.customSqlSegment}")
    IPage<MenuInfoCompanyRefPO> findPage(Page<MenuInfoCompanyRefPO> page,@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT COMPANY_ID FROM rbac_menuinfo_company_ref WHERE MENUINFO_ID = #{menuInfoId}")
    List<Long> getMenuInfoCompanyIds(@Param("menuInfoId") Long menuInfoId);
}
