package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.notification.admin.service.NoticeReceiveRangeExtService;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRangeExt;
import com.supcon.supfusion.notification.admin.dao.mappers.organization.NoticeReceiveRangeExtDao;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 15:28
 */
@Service("adminNoticeReceiveRangeExtServiceImpl")
public class NoticeReceiveRangeExtServiceImpl extends NoticeBaseServiceImpl<NoticeReceiveRangeExtDao, NoticeRecieveRangeExt> implements NoticeReceiveRangeExtService {

    @Override
    public Boolean deleteListByRange(Long[] receiveRangeId) {
        QueryWrapper<NoticeRecieveRangeExt> queryWrapper = new QueryWrapper<>();
        queryWrapper.in(NoticeRecieveRangeExt.getRangeIdFieldName(), receiveRangeId);
        return super.remove(queryWrapper);
    }
}
