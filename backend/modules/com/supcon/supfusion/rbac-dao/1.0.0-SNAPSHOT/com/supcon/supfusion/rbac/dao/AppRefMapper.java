package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.supcon.supfusion.rbac.dao.po.AppRefPO;
import com.supcon.supfusion.rbac.dao.po.MenuTempPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public interface AppRefMapper extends BaseMapper<AppRefPO> {


    @Select("select id,menuid,appid from rbac_app_ref ${ew.customSqlSegment}")
    List<AppRefPO> findAppRefByMenuIdAppId(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("select raf.id, raf.menuid, raf.appid from rbac_app_ref raf left join rbac_menuinfo rm on raf.menuid = rm.id ${ew.customSqlSegment}")
    AppRefPO findAppidByMenuCode(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("select menuid from rbac_app_ref ${ew.customSqlSegment}")
    List<Long> getMenuIdsByAppid(@Param(Constants.WRAPPER) Wrapper wrapper);


}
