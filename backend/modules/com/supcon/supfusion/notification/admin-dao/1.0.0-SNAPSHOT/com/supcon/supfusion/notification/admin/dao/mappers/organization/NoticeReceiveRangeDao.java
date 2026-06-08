package com.supcon.supfusion.notification.admin.dao.mappers.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRange;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 15:26
 */
@Mapper
@Repository("adminNoticeReceiveRangeDao")
public interface NoticeReceiveRangeDao extends BaseMapper<NoticeRecieveRange> {
}
