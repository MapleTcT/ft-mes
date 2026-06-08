package com.supcon.supfusion.notification.mobile.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.mobile.dao.entities.NoticeProtocol;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository("modileNoticeProtocolMapper")
public interface NoticeProtocolMapper extends BaseMapper<NoticeProtocol> {
}
