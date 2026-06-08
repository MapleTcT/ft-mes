package com.supcon.supfusion.notification.engine.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeMessageUnreadCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * <p>
 * 未读消息数量统计表 Mapper 接口
 * </p>
 *
 * @author panzk
 * @since 2020-08-13
 */
@Mapper
@Repository("engineNoticeMessageUnreadCountMapper")
public interface NoticeMessageUnreadCountMapper extends BaseMapper<NoticeMessageUnreadCount> {
    @Update("update notice_message_unread_count " +
            "set unread_count = unread_count + 1 ,modify_time = #{updateTime} " +
            "where staff_code = #{staffCode} " +
            "and notice_protocol_id = #{noticeProtocolId} " +
            "and topic_id = #{topicId}")
    Integer increase(@Param("staffCode") String staffCode,
                     @Param("noticeProtocolId") Long noticeProtocolId,
                     @Param("topicId") Long topicId,
                     @Param("updateTime") Date updateTime);
}
