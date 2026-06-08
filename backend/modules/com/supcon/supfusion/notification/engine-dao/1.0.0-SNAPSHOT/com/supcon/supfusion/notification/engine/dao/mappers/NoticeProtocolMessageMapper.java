package com.supcon.supfusion.notification.engine.dao.mappers;

import com.supcon.supfusion.notification.engine.dao.entities.NoticeMsg;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
@Repository("engineNoticeProtocolMessageMapper")
public interface NoticeProtocolMessageMapper extends BaseMapper<NoticeMsg> {

}
