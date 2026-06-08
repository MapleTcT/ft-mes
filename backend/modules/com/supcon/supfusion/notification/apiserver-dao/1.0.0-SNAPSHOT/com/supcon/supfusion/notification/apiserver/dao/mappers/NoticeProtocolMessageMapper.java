package com.supcon.supfusion.notification.apiserver.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeMsg;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 消息表 Mapper 接口
 * </p>
 *
 * @author panzk
 * @since 2020-05-20
 */
@Mapper
@Repository("apiserverNoticeProtocolMessageMapper")
public interface NoticeProtocolMessageMapper extends BaseMapper<NoticeMsg> {

}
