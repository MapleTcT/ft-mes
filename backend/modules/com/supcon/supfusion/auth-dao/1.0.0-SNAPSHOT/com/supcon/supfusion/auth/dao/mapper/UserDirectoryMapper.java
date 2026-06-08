package com.supcon.supfusion.auth.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.auth.dao.po.UserDirectoryPO;
import org.apache.ibatis.annotations.Select;

/**
 * @author caokele
 */
public interface UserDirectoryMapper extends BaseMapper<UserDirectoryPO> {

    @Select("SELECT MAX(sort) FROM auth_user_directory")
    Double selectMaxSort();
}
