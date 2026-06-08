package com.supcon.supfusion.notification.admin.dao.mappers;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 17:12
 */
@Mapper
@Repository("adminNoticeTopicDao")
public interface NoticeTopicDao extends BaseMapper<NoticeTopic> {

    List<NoticeTopicList> getListByPage(
            @Param("names") String[] names,
            @Param("codes") String[] codes,
            @Param("topicTree") Long topicTree,
            @Param("protocols") Long[] protocols,
            @Param("templateNames") String[] templateNames,
            @Param("start") Integer start,
            @Param("end") Integer end);

    Integer getListByPageCount(
            @Param("names") String[] names,
            @Param("codes") String[] codes,
            @Param("topicTree") Long topicTree,
            @Param("protocols") Long[] protocols,
            @Param("templateNames") String[] templateNames);


    List<NoticeTopic> getTmplName(@Param(Constants.WRAPPER) QueryWrapper wrapper);

}
