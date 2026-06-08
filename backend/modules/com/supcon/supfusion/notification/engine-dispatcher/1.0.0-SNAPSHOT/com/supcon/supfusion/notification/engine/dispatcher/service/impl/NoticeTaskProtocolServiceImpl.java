package com.supcon.supfusion.notification.engine.dispatcher.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeTaskProtocol;
import com.supcon.supfusion.notification.engine.dao.mappers.NoticeTaskProtocolMapper;
import com.supcon.supfusion.notification.engine.dispatcher.service.NoticeTaskProtocolService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 发送任务协议表 服务实现类
 * </p>
 *
 * @author panzk
 * @since 2020-05-20
 */
@Service("engineNoticeTaskProtocolServiceImpl")
public class NoticeTaskProtocolServiceImpl extends ServiceImpl<NoticeTaskProtocolMapper, NoticeTaskProtocol> implements NoticeTaskProtocolService {

}
