package com.supcon.supfusion.notification.admin.dao.mappers.protocolconfig;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 17:12
 */
@Mapper
@Repository("adminNoticeProtocolConfigMapper")
public interface NoticeProtocolConfigMapper extends BaseMapper<NoticeProtocolConfig> {

}
