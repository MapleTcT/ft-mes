package com.supcon.supfusion.auth.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.auth.dao.po.UserPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author lifangyuan
 */
public interface UserMapper extends BaseMapper<UserPO> {

    @Update("UPDATE auth_user SET current_company_id = #{currentCompanyId, jdbcType=NULL} WHERE USER_NAME = #{userName} AND VALID = 1")
    int updateCurrentCompanyId(@Param("userName") String userName, @Param("currentCompanyId") Long currentCompanyId);

    @Select("SELECT * FROM auth_user WHERE USER_NAME = #{userName}")
    UserPO selectUserName(@Param("userName") String userName);

    @Update("UPDATE auth_user SET third_identity = null,third_source= null  WHERE USER_NAME = #{userName} AND VALID = 1")
    void unBindUserThridIdentitys(@Param("userName") String userName);
}
