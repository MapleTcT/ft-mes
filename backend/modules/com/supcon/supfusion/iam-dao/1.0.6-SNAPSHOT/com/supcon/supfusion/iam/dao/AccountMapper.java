package com.supcon.supfusion.iam.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.iam.dao.entity.AccountPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午3:45
 */
@Mapper
public interface AccountMapper extends BaseMapper<AccountPO> {}
