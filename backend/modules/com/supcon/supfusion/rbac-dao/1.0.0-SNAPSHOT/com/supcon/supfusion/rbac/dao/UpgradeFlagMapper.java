package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.UpgradeFlagPO;
import org.apache.ibatis.annotations.Select;

public interface UpgradeFlagMapper extends BaseMapper<UpgradeFlagPO> {
    @Select("select count(*) from sys_scripts_version where application_name = 'rbac-upgrade'")
    Integer getUpgradeFlag();

}
