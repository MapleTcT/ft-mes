package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.util.DateUtil;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMessageUnreadCount;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMsg;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeMessageRecordDao;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeMessageUnreadCountMapper;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.admin.service.NoticeMessageRecordService;
import com.supcon.supfusion.notification.protocol.constants.ProtocolType;
import com.supcon.supfusion.notification.sharding.context.ShardingContext;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.sql.SQLSyntaxErrorException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:05
 */
@Slf4j
@Service("adminNoticeMessageRecordServiceImpl")
public class NoticeMessageRecordServiceImpl extends NoticeBaseServiceImpl<NoticeMessageRecordDao, NoticeMsg> implements NoticeMessageRecordService {
    @Resource(name = "adminNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;
    @Resource(name = "adminNoticeMessageUnreadCountMapper")
    private NoticeMessageUnreadCountMapper noticeMessageUnreadCountMapper;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;

    @Override
    public Page<NoticeMsg> queryPageList(Long startTime, Long endTime, Long protocolId, String receiverName, String taskId, String sendStatus, String readStatus, Page<NoticeMsg> page) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeMsg> queryWrapper = new QueryWrapper<NoticeMsg>();
        if (startTime != null) {
            queryWrapper.ge(NoticeMsg.getShardingTimeFieldName(), startTime);
        }
        if (endTime != null) {
            queryWrapper.le(NoticeMsg.getShardingTimeFieldName(), endTime);
        }
        if (protocolId != null) {
            queryWrapper.eq(NoticeMsg.getNoticeProtocolIdFieldName(), protocolId);
        }
        if (!StringUtils.isEmpty(receiverName)) {
            String[] receiverNameList = receiverName.split(",");
            queryWrapper.and(receiverlb -> {
                String key = dbStringUtil.getString(receiverNameList[0]);
                if ("oracle".equals(dbType)) {
                    receiverlb.apply(NoticeMsg.getStaffNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    receiverlb.like(NoticeMsg.getStaffNameFieldName(), key);
                }
                for (int i = 0; i < receiverNameList.length; i++) {
                    if (i > 0) {
                        key = dbStringUtil.getString(receiverNameList[i]);
                        if ("oracle".equals(dbType)) {
                            receiverlb.or().apply(NoticeMsg.getStaffNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
                        } else {
                            receiverlb.or().like(NoticeMsg.getStaffNameFieldName(), key);
                        }
                    }
                }
            });
        }
        if (!StringUtils.isEmpty(taskId)) {
            String[] taskIdList = taskId.split(",");
            queryWrapper.and(tasklb -> {
                String key = dbStringUtil.getString(taskIdList[0]);
                if ("oracle".equals(dbType)) {
                    tasklb.apply(NoticeMsg.getNoticeTaskIdFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    tasklb.like(NoticeMsg.getNoticeTaskIdFieldName(), key);
                }
                for (int i = 0; i < taskIdList.length; i++) {
                    if (i > 0) {
                        key = dbStringUtil.getString(taskIdList[i]);
                        if ("oracle".equals(dbType)) {
                            tasklb.or().apply(NoticeMsg.getNoticeTaskIdFieldName() + " like {0} escape '\\'", "%" + key + "%");
                        } else {
                            tasklb.or().like(NoticeMsg.getNoticeTaskIdFieldName(), key);
                        }
                    }
                }
            });
        }
        if (sendStatus != null) {
            String[] sendStatuList = sendStatus.split(",");
            queryWrapper.in(NoticeMsg.getSendStatusFieldName(), sendStatuList);
        }
        if (readStatus != null) {
            String[] readStatuList = readStatus.split(",");
            queryWrapper.in(NoticeMsg.getReadStatusFieldName(), readStatuList);
        }
        queryWrapper.orderByDesc(NoticeMsg.getShardingTimeFieldName());
        Page<NoticeMsg> messagePage = this.baseMapper.selectPage(page, queryWrapper);

      /*  page.setRecords(topicList);
        page.setTotal(topicList.size());*/
        return messagePage;
    }

    @Override
    public Page<NoticeMsg> queryStationLetterPage(Long startTime, Long endTime, String readStatus, String staffCode, Page<NoticeMsg> page) {
        QueryWrapper<NoticeMsg> queryWrapper = new QueryWrapper<NoticeMsg>();
        //站内信筛选
        queryWrapper.eq(NoticeMsg.getNoticeProtocolIdFieldName(), ProtocolType.SRATIONLETTER.value());
        if (startTime != null) {
            queryWrapper.ge(NoticeMsg.getShardingTimeFieldName(), startTime);
        }
        if (endTime != null) {
            queryWrapper.le(NoticeMsg.getShardingTimeFieldName(), endTime);
        }
        if (StringUtils.hasText(staffCode)) {
            queryWrapper.eq(NoticeMsg.getStaffCodeFieldName(), staffCode);
        }
        if (readStatus != null) {
            String[] readStatuList = readStatus.split(",");
            queryWrapper.eq(NoticeMsg.getReadStatusFieldName(), readStatus);
        }
        queryWrapper.orderByDesc(NoticeMsg.getShardingTimeFieldName());
        Page<NoticeMsg> messagePage = this.baseMapper.selectPage(page, queryWrapper);

      /*  page.setRecords(topicList);
        page.setTotal(topicList.size());*/
        return messagePage;
    }

    @Override
    public List<NoticeMsg> queryListByKeyword(Long startTime, Long endTime, Long protocolId, String staffName, String taskId) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeMsg> queryWrapper = new QueryWrapper<NoticeMsg>();
        if (startTime != null) {
            queryWrapper.ge(NoticeMsg.getShardingTimeFieldName(), startTime);
        }
        if (endTime != null) {
            queryWrapper.le(NoticeMsg.getShardingTimeFieldName(), endTime);
        }
        if (protocolId != null) {
            queryWrapper.eq(NoticeMsg.getNoticeProtocolIdFieldName(), protocolId);
        }
        if (!StringUtils.isEmpty(staffName)) {
            String key = dbStringUtil.getString(staffName);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeMsg.getStaffNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeMsg.getStaffNameFieldName(), key);
            }
        }
        if (!StringUtils.isEmpty(taskId)) {
            String key = dbStringUtil.getString(taskId);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeMsg.getNoticeTaskIdFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeMsg.getNoticeTaskIdFieldName(), key);
            }
        }
        return super.list(queryWrapper);
    }

    @Transactional
    public void ackAllStationLetter(Long startTime, Long endTime, String protocol) {
        String staffCode = UserContext.getUserContext().getStaffCode();
        if (StringUtils.isEmpty(staffCode)) {
            /**
             * 该用户没有绑定人员,为系统管理员。系统管理员没有个人站内信
             */
            return;
        }
        if (StringUtils.isEmpty(protocol)) {
            protocol = "stationLetter";
        }
        ShardingContext.getContext().setShardingTime((startTime + endTime) / 2);
        ShardingContext.getContext().setProtocol(protocol);
        try {
            //根据主题id分组查询每组修改的数量
            QueryWrapper<NoticeMsg> queryWrapper = new QueryWrapper<NoticeMsg>();
            queryWrapper.select(NoticeMsg.getTopicIdFieldName(), "count(id) as count ")
                    .eq(NoticeMsg.getStaffCodeFieldName(), staffCode)
                    .eq(NoticeMsg.getReadStatusFieldName(), 0)
                    .ge(BaseEntity.Field.createTime, new Date(startTime))
                    .le(NoticeMsg.Field.createTime, new Date(endTime))
                    .groupBy(NoticeMsg.getTopicIdFieldName());
            List<Map<String, Object>> grupByTopicCount = this.baseMapper.selectMaps(queryWrapper);

            NoticeMsg noticeMsg = new NoticeMsg();
            noticeMsg.setReadStatus(1);
            Integer count = this.baseMapper.update(noticeMsg, Wrappers.<NoticeMsg>update()
                    .eq(NoticeMsg.getStaffCodeFieldName(), staffCode)
                    .eq(NoticeMsg.getReadStatusFieldName(), 0)
                    .ge(NoticeMsg.getShardingTimeFieldName(), startTime)
                    .le(NoticeMsg.getShardingTimeFieldName(), endTime));
            //修改统计表
            for (int i = 0; i < grupByTopicCount.size(); i++) {
                Map<String, Object> map = grupByTopicCount.get(i);
                Integer topicCount =  new Integer(map.getOrDefault("count",map.getOrDefault("COUNT","0")).toString());
                Long topicId = Optional.ofNullable(map.getOrDefault(NoticeMsg.getTopicIdFieldName(),map.getOrDefault(NoticeMsg.getTopicIdFieldName().toUpperCase(),null))).map(x->Long.parseLong(x.toString())).orElse(null);
                decrease(staffCode, topicCount, topicId, protocol);
            }
        } catch (MyBatisSystemException e) {
            log.error(e.getMessage(), e);
            /**
             * 更新不存在月份的分表，返回空列表
             */
            if (e.getCause() != null
                    && e.getCause().getCause() != null
                    && e.getCause().getCause().getCause() != null
                    && "42S02".equals(((SQLSyntaxErrorException) (e.getCause().getCause().getCause())).getSQLState())) {
                log.info("数据分表不存在");
            } else {
                throw e;
            }
        }
    }

    @Override
    public void ackStationLetter(Long startTime, Long endTime, List<String> messageIds, String protocol) {
        String staffCode = UserContext.getUserContext().getStaffCode();
        if (StringUtils.isEmpty(staffCode)) {
            /**
             * 该用户没有绑定人员,为系统管理员。系统管理员没有个人站内信
             */
            return;
        }
        if (StringUtils.isEmpty(protocol)) {
            protocol = "stationLetter";
        }
        ShardingContext.getContext().setShardingTime((startTime + endTime) / 2);
        ShardingContext.getContext().setProtocol(protocol);
        try {
            QueryWrapper<NoticeMsg> queryWrapper = new QueryWrapper<NoticeMsg>();
            queryWrapper.select(NoticeMsg.getTopicIdFieldName(), "count(id) as count")
                    .eq(NoticeMsg.getReadStatusFieldName(), 0)
                    .in(NoticeMsg.getIdFieldName(), messageIds)
                    .groupBy(NoticeMsg.getTopicIdFieldName());
            List<Map<String, Object>> grupByTopicCount = this.baseMapper.selectMaps(queryWrapper);


            NoticeMsg noticeMsg = new NoticeMsg();
            noticeMsg.setReadStatus(1);
            Integer count = this.baseMapper.update(noticeMsg,
                    Wrappers.<NoticeMsg>update().eq(NoticeMsg.getReadStatusFieldName(), 0)
                            .in(NoticeMsg.getIdFieldName(), messageIds));
            //修改统计表
            for (int i = 0; i < grupByTopicCount.size(); i++) {
                Map<String, Object> map = grupByTopicCount.get(i);

                Integer topicCount = new Integer(map.getOrDefault("count",map.getOrDefault("COUNT","0")).toString());
                Long topicId = Optional.ofNullable(map.getOrDefault(NoticeMsg.getTopicIdFieldName(),map.getOrDefault(NoticeMsg.getTopicIdFieldName().toUpperCase(),null))).map(x->Long.parseLong(x.toString())).orElse(null);
                decrease(staffCode, topicCount, topicId, protocol);
            }
        } catch (MyBatisSystemException e) {
            log.error(e.getMessage(), e);
            /**
             * 更新不存在月份的分表，返回空列表
             */
            if (e.getCause() != null
                    && e.getCause().getCause() != null
                    && e.getCause().getCause().getCause() != null
                    && "42S02".equals(((SQLSyntaxErrorException) (e.getCause().getCause().getCause())).getSQLState())) {
                log.info("数据分表不存在");
            } else {
                throw e;
            }
        }
    }

    @Override
    public void ackStationLetters(Long shardingTime, Long id, String protocol) {
        String staffCode = UserContext.getUserContext().getStaffCode();
        if (StringUtils.isEmpty(staffCode)) {
            log.error("staffCode: {}", staffCode);
            /**
             * 该用户没有绑定人员,为系统管理员。系统管理员没有个人站内信
             */
            return;
        }
        if (StringUtils.isEmpty(protocol)) {
            protocol = "stationLetter";
        }
        ShardingContext.getContext().setShardingTime((shardingTime));
        ShardingContext.getContext().setProtocol(protocol);
        try {
            NoticeMsg noticeMsgSelect = this.baseMapper.selectById(id);
            NoticeMsg noticeMsg = new NoticeMsg();
            noticeMsg.setReadStatus(1);
            Integer count = this.baseMapper.update(noticeMsg, Wrappers.<NoticeMsg>update()
                    .eq(NoticeMsg.getReadStatusFieldName(), 0)
                    .eq(NoticeMsg.getIdFieldName(), id));
            decrease(staffCode, count, noticeMsgSelect.getTopicId(), protocol);
        } catch (MyBatisSystemException e) {
            log.error(e.getMessage(), e);
            /**
             * 更新不存在月份的分表，返回空列表
             */
            if (e.getCause() != null
                    && e.getCause().getCause() != null
                    && e.getCause().getCause().getCause() != null
                    && "42S02".equals(((SQLSyntaxErrorException) (e.getCause().getCause().getCause())).getSQLState())) {
                log.info("数据分表不存在");
            } else {
                throw e;
            }
        }
    }

    @Override
    public long stationLetterUnreadNum() {

        return getUnreadNum(2L);
    }

    @Override
    public long getUnreadNum(Long protocolId) {
        String staffCode = UserContext.getUserContext().getStaffCode();
        if (StringUtils.isEmpty(staffCode)) {
            /**
             * 该用户没有绑定人员,为系统管理员。系统管理员没有个人站内信
             */
            return 0L;
        }
        Long unreadNumber = noticeMessageUnreadCountMapper.countUnreadNumber(staffCode, protocolId);
        if (null == unreadNumber) {
            return 0L;
        }
        return unreadNumber;
    }

    @Override
    public List<NoticeMessageUnreadCount> getUnreadNumGroup(Long protocolId) {
        String staffCode = UserContext.getUserContext().getStaffCode();
        if (StringUtils.isEmpty(staffCode)) {
            /**
             * 该用户没有绑定人员,为系统管理员。系统管理员没有个人站内信
             */
            return null;
        }
        List<NoticeMessageUnreadCount> noticeMessageUnreadCounts = noticeMessageUnreadCountMapper.getUnreadNumGroup(staffCode, protocolId);
        ;
        return noticeMessageUnreadCounts;
    }

    @Override
    public Page<NoticeMsg> getLatestNewsByTopic(String protocol, Long topicId, Date createTime, String staffName, String staffCode, String readStatus, Page<NoticeMsg> page) {
        if (createTime == null || createTime.getTime() < 1000 * 60 * 60 * 24) {
            createTime = new Date();
        }
        if (page == null) {
            page = new Page<>();
            page.setSize(50);
        }
        Page<NoticeMsg> noticeMsgPage = null;
        try {
            //按当前设定时间查询数据,如果没有查到,连续往前推6月,如果还是没有数据就返回空
            QueryWrapper<NoticeMsg> queryWrapper = new QueryWrapper<>();
            if (topicId != null) {
                queryWrapper.eq(NoticeMsg.getTopicIdFieldName(), topicId);
            }
            String dbType = dataId.getDataId();
            if (!StringUtils.isEmpty(staffName)) {
                String key = dbStringUtil.getString(staffName);
                if ("oracle".equals(dbType)) {
                    queryWrapper.apply(NoticeMsg.getStaffNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    queryWrapper.like(NoticeMsg.getStaffNameFieldName(), key);
                }
            }
            if (!StringUtils.isEmpty(staffCode)) {
                queryWrapper.eq(NoticeMsg.getStaffCodeFieldName(), staffCode);
            }
            if (!StringUtils.isEmpty(readStatus)) {
                queryWrapper.eq(NoticeMsg.getReadStatusFieldName(), readStatus);
            }
//            queryWrapper.eq(NoticeMsg.getSendStatusFieldName(), 1);
            queryWrapper.lt(NoticeMsg.Field.createTime, createTime);
            queryWrapper.orderByDesc(NoticeMsg.Field.createTime);
            for (int i = 0; i > -6; i--) {
                Date date = DateUtil.setMonths(createTime, i);
                if (noticeMsgPage == null || noticeMsgPage.getRecords() == null) {
                    noticeMsgPage = getNoticeMsgPage(protocol, queryWrapper, date, page);
                } else {
                    return noticeMsgPage;
                }
            }
        } catch (MyBatisSystemException e) {
            log.error(e.getMessage(), e);
            /**
             * 更新不存在月份的分表，返回空列表
             */
            if (e.getCause() != null
                    && e.getCause().getCause() != null
                    && e.getCause().getCause().getCause() != null
                    && "42S02".equals(((SQLSyntaxErrorException) (e.getCause().getCause().getCause())).getSQLState())) {
                log.info("数据分表不存在");
            } else {
                throw e;
            }
        }
        return noticeMsgPage;
    }

    private Page<NoticeMsg> getNoticeMsgPage(String protocol, QueryWrapper<NoticeMsg> queryWrapper, Date shardingTime, Page<NoticeMsg> page) {
        ShardingContext.getContext().setShardingTime((shardingTime.getTime()));
        ShardingContext.getContext().setProtocol(protocol);
        return this.baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Integer countByStaffName(Long startTime, Long endTime, Long protocolId, String staffName, String taskId) {
        QueryWrapper<NoticeMsg> queryWrapper = new QueryWrapper<NoticeMsg>();
        if (startTime != null) {
            queryWrapper.ge(NoticeMsg.getShardingTimeFieldName(), startTime);
        }
        if (endTime != null) {
            queryWrapper.le(NoticeMsg.getShardingTimeFieldName(), endTime);
        }
        if (protocolId != null) {
            queryWrapper.eq(NoticeMsg.getNoticeProtocolIdFieldName(), protocolId);
        }
        if (!StringUtils.isEmpty(staffName)) {
            String key = dbStringUtil.getString(staffName);
            queryWrapper.like(NoticeMsg.getStaffNameFieldName(), key);
        }
        if (!StringUtils.isEmpty(taskId)) {
            String key = dbStringUtil.getString(taskId);
            queryWrapper.like(NoticeMsg.getNoticeTaskIdFieldName(), key);

        }
        int count = super.count(queryWrapper);
        return count;
    }

    private void decrease(String staffCode, Integer count, Long topicId, String protocol) {
        if (count <= 0) {
            return;
        }
        NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getProtocolFieldName(), protocol));
        noticeMessageUnreadCountMapper.decrease(staffCode, noticeProtocol.getId(), count, topicId);
    }

}
