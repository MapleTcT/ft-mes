package com.supcon.supfusion.notification.engine.dispatcher.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeMsg;
import com.supcon.supfusion.notification.engine.dao.mappers.NoticeProtocolMessageMapper;
import com.supcon.supfusion.notification.engine.dispatcher.service.NoticeProtocolMessageService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息表 服务实现类
 * </p>
 *
 * @author panzk
 * @since 2020-05-20
 */
@Service("engineNoticeProtocolMessageServiceImpl")
public class NoticeProtocolMessageServiceImpl extends ServiceImpl<NoticeProtocolMessageMapper, NoticeMsg> implements NoticeProtocolMessageService {

}
