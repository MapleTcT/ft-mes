package com.supcon.supfusion.license.dao.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.license.dao.po.LicenseInfoPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface LicenseMapper extends BaseMapper<LicenseInfoPO> {
    @Select("SELECT id, module_code, license_key, value, description, application_name, application_type, time, hash_code, valid, creator, modifier, create_time, modify_time, create_staff_id, modify_staff_id" +
            " FROM license_info  ${ew.customSqlSegment}")
    IPage<LicenseInfoPO> findPage(Page<LicenseInfoPO> page, @Param("ew") Wrapper queryWrapper);
}
