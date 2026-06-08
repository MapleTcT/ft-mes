package com.supcon.supfusion.notification.admin.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTask;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 发送任务表 Mapper 接口
 * </p>
 *
 * @author panzk
 * @since 2020-05-19
 */
@Mapper
@Repository("adminNoticeTaskMapper")
public interface NoticeTaskMapper extends BaseMapper<NoticeTask> {

}
