package com.supcon.supfusion.notification.apiserver.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeTask;
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
@Repository("apiserverNoticeTaskMapper")
public interface NoticeTaskMapper extends BaseMapper<NoticeTask> {

}
