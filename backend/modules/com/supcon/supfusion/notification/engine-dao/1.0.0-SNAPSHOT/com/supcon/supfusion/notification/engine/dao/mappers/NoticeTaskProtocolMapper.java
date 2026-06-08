package com.supcon.supfusion.notification.engine.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeTaskProtocol;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 发送任务协议表 Mapper 接口
 * </p>
 *
 * @author panzk
 * @since 2020-05-20
 */
@Mapper
@Repository("engineNoticeTaskProtocolMapper")
public interface NoticeTaskProtocolMapper extends BaseMapper<NoticeTaskProtocol> {

}
