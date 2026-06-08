package com.supcon.supfusion.notification.apiserver.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeRecieveRangeExt;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 15:26
 */
@Mapper
@Repository("apiserverNoticeReceiveRangeExtMapper")
public interface NoticeReceiveRangeExtMapper extends BaseMapper<NoticeRecieveRangeExt> {

}
