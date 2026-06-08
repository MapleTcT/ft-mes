package com.supcon.supfusion.auth.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.dao.po.OnlineUserPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author caokele
 */
public interface OnlineUserMapper extends BaseMapper<OnlineUserPO> {

    @Delete("DELETE FROM " + Constants.AUTH_ONLINE_USER)
    void deleteAll();

    @Select("SELECT count(1) FROM " + Constants.AUTH_ONLINE_USER)
    Integer selectTotalCount();

    @Select("SELECT ticket FROM " + Constants.AUTH_ONLINE_USER)
    List<OnlineUserPO> selectAll();


}
