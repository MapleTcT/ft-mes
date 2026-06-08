package com.supcon.supfusion.notification.apiserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.notification.apiserver.service.NoticeProtocolMessageService;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeMsg;
import com.supcon.supfusion.notification.apiserver.dao.mappers.NoticeProtocolMessageMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息表 服务实现类
 * </p>
 *
 * @author panzk
 * @since 2020-05-20
 */
@Service("apiserverNoticeProtocolMessageServiceImpl")
public class NoticeProtocolMessageServiceImpl extends ServiceImpl<NoticeProtocolMessageMapper, NoticeMsg> implements NoticeProtocolMessageService {

}
