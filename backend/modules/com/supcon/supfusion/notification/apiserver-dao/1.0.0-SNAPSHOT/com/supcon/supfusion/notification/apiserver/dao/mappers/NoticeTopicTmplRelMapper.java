package com.supcon.supfusion.notification.apiserver.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeTopicTmplateRelation;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 17:12
 */
@Mapper
@Repository("apiserverNoticeTopicTmplRelMapper")
public interface NoticeTopicTmplRelMapper extends BaseMapper<NoticeTopicTmplateRelation> {
}
