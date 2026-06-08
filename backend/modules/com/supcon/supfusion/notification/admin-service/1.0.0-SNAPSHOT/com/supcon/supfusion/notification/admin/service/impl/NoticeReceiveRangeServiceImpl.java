package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.notification.admin.service.NoticeReceiveRangeService;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRange;
import com.supcon.supfusion.notification.admin.dao.mappers.organization.NoticeReceiveRangeDao;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 15:21
 */
@Service("adminNoticeReceiveRangeServiceImpl")
public class NoticeReceiveRangeServiceImpl extends NoticeBaseServiceImpl<NoticeReceiveRangeDao, NoticeRecieveRange> implements NoticeReceiveRangeService {

    @Override
    public List<NoticeRecieveRange> queryListByTopic(Long topicId) {
        QueryWrapper<NoticeRecieveRange> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(NoticeRecieveRange.getTopicIdFieldName(),topicId);
        return super.list(queryWrapper);
    }

    @Override
    public Boolean deleteListByTopic(Long topicId) {
        QueryWrapper<NoticeRecieveRange> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(NoticeRecieveRange.getTopicIdFieldName(),topicId);
        return super.remove(queryWrapper);
    }

}
