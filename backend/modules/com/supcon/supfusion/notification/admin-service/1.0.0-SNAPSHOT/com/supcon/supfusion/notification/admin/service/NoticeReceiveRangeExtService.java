package com.supcon.supfusion.notification.admin.service;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRangeExt;

import java.util.Set;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 15:23
 */
public interface NoticeReceiveRangeExtService extends NoticeBaseService<NoticeRecieveRangeExt> {
    /**
     * 根据receiveRangeId批量删除接收范围扩展表
     * @param receiveRangeId
     * @return
     */
    Boolean deleteListByRange(Long[] receiveRangeId);
}
