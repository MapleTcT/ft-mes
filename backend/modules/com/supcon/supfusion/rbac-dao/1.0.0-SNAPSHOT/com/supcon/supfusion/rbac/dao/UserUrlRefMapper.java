package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.supcon.supfusion.rbac.dao.po.UserUrlRefPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 用户与请求URL关联表 Mapper 接口
 * </p>
 */
public interface UserUrlRefMapper extends BaseMapper<UserUrlRefPO> {

    @Select("SELECT UP.USER_ID,UP.CID,MOC.URL,MOC.APP,MOC.METHOD_TYPE FROM rbac_userpermission UP " +
            "JOIN rbac_menuoperatecode_url_ref MOC ON UP.MENUOPERATE_CODE = MOC.MENUOPERATE_CODE " +
            "JOIN rbac_menuoperate MO ON UP.MENUOPERATE_CODE = MO.CODE " +
            "JOIN rbac_menuinfo MI ON MI.ID = MO.MENUINFO_ID " +
            "${ew.customSqlSegment}")
    List<UserUrlRefPO> getUserUrlRefList(@Param(Constants.WRAPPER) Wrapper wrapper);
}
