package com.supcon.supfusion.notification.apiserver.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeProtocol;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository("apiserverNoticeProtocolMapper")
public interface NoticeProtocolMapper extends BaseMapper<NoticeProtocol> {
}
