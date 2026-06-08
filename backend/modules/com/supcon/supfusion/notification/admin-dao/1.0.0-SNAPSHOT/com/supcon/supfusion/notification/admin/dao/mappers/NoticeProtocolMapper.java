package com.supcon.supfusion.notification.admin.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository("adminNoticeProtocolMapper")
public interface NoticeProtocolMapper extends BaseMapper<NoticeProtocol> {
    List<NoticeProtocol> getProtocolNamesByTask(@Param("taskId") Long taskId);
}
