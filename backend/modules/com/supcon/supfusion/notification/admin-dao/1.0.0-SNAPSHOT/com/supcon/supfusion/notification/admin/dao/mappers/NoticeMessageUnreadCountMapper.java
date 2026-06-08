package com.supcon.supfusion.notification.admin.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMessageUnreadCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * 未读消息数量统计表 Mapper 接口
 * </p>
 *
 * @author panzk
 * @since 2020-08-13
 */
@Mapper
@Repository("adminNoticeMessageUnreadCountMapper")
public interface NoticeMessageUnreadCountMapper extends BaseMapper<NoticeMessageUnreadCount> {
    @Update("update notice_message_unread_count set unread_count = unread_count - #{count} " +
            "where " +
            "staff_code = #{staffCode} " +
            "and notice_protocol_id = #{noticeProtocolId} " +
            "and topic_id = #{topicId} " +
            "and unread_count >= #{count}")
    Integer decrease(@Param("staffCode") String staffCode,
                     @Param("noticeProtocolId") Long noticeProtocolId,
                     @Param("count") Integer count,
                     @Param("topicId") Long topicId);

    @Select("select sum(unread_count) from notice_message_unread_count where STAFF_CODE = #{staffCode} and notice_protocol_id = #{protocolId}")
    Long countUnreadNumber(@Param("staffCode") String staffCode,@Param("protocolId") Long protocolId);

    List<NoticeMessageUnreadCount> getUnreadNumGroup( @Param("staffCode") String staffCode,@Param("protocolId") Long protocolId );

}
