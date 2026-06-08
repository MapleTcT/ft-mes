package com.supcon.supfusion.notification.sms.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.sms.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.sms.dao.entities.SmsEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface SmsEntityMapper extends BaseMapper<SmsEntity> {
}
