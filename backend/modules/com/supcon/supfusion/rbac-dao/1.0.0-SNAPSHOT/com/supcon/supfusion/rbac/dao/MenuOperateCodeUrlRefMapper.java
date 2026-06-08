package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.MenuOperateCodeUrlRefPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 菜单操作编码URL关联表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-22
 */
public interface MenuOperateCodeUrlRefMapper extends BaseMapper<MenuOperateCodeUrlRefPO> {

    @Delete("DELETE FROM rbac_menuoperatecode_url_ref WHERE APP = #{app} AND IS_CUSTOM = 0 AND IMPORT_TYPE = 0")
    void deleteByAppId(@Param("app") String app);

    @Select("SELECT * FROM rbac_menuoperatecode_url_ref WHERE MENUOPERATE_CODE = #{code}")
    List<MenuOperateCodeUrlRefPO> getUrlByOperateCode(@Param("code") String code);
}
