package com.supcon.supfusion.notification.admin.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMsg;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTask;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTaskProtocol;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTaskMapper;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTaskProtocolMapper;
import com.supcon.supfusion.notification.admin.service.NoticeTaskService;
import com.supcon.supfusion.notification.sharding.context.ShardingContext;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:05
 */
@Service("adminNoticeTaskServiceImpl")
public class NoticeTaskServiceImpl extends NoticeBaseServiceImpl<NoticeTaskMapper, NoticeTask> implements NoticeTaskService {
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;
    @Resource(name = "adminNoticeTaskProtocolMapper")
    private NoticeTaskProtocolMapper noticeTaskProtocolMapper;

    @Override
    public Page<NoticeTask> queryTaskPage(String startTime, String endTime, String id, Long noticeTopicId, String bsmodCode, String bsmodName, Page<NoticeTask> page) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeTask> queryWrapper = new QueryWrapper<>();
        if (startTime != null) {
            queryWrapper.ge(NoticeMsg.getShardingTimeFieldName(), startTime);
        }
        if (endTime != null) {
            queryWrapper.le(NoticeMsg.getShardingTimeFieldName(), endTime);
        }
        if (StringUtils.isNotBlank(id)) {
            String[] idList = id.split(",");
            queryWrapper.and(qw -> {
                String key = dbStringUtil.getString(idList[0]);
                if ("oracle".equals(dbType)) {
                    qw.apply(NoticeTask.getIdFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    qw.like(NoticeTask.getIdFieldName(), key);
                }
                for (int i = 0; i < idList.length; i++) {
                    if (i > 0) {
                        key = dbStringUtil.getString(idList[i]);
                        if ("oracle".equals(dbType)) {
                            qw.or().apply(NoticeTask.getIdFieldName() + " like {0} escape '\\'", "%" + key + "%");
                        } else {
                            qw.or().like(NoticeTask.getIdFieldName(), key);
                        }
                    }
                }
            });
        }
        if (noticeTopicId != null) {
            queryWrapper.eq(NoticeTask.getNoticeTopicIdFieldName(), noticeTopicId);
        }
        if (StringUtils.isNotBlank(bsmodCode)) {
            String[] bsmodCodeList = bsmodCode.split(",");
            queryWrapper.and(bsmod -> {
                String key = dbStringUtil.getString(bsmodCodeList[0]);
                if ("oracle".equals(dbType)) {
                    bsmod.apply(NoticeTask.getBsmodCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    bsmod.like(NoticeTask.getBsmodCodeFieldName(), key);
                }
                for (int i = 0; i < bsmodCodeList.length; i++) {
                    if (i > 0) {
                        key = dbStringUtil.getString(bsmodCodeList[i]);
                        if ("oracle".equals(dbType)) {
                            bsmod.or().apply(NoticeTask.getBsmodCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
                        } else {
                            bsmod.or().like(NoticeTask.getBsmodCodeFieldName(), key);
                        }
                    }
                }
            });
        }
        if (StringUtils.isNotBlank(bsmodName)) {
            String[] bsmodNameList = bsmodName.split(",");
            queryWrapper.and(wrapper -> {
                String key = dbStringUtil.getString(bsmodNameList[0]);
                if ("oracle".equals(dbType)) {
                    wrapper.apply(NoticeTask.getBsmodNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    wrapper.like(NoticeTask.getBsmodNameFieldName(), key);
                }
                for (int i = 0; i < bsmodNameList.length; i++) {
                    if (i > 0) {
                        key = dbStringUtil.getString(bsmodNameList[i]);
                        if ("oracle".equals(dbType)) {
                            wrapper.or().apply(NoticeTask.getBsmodNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
                        } else {
                            wrapper.or().like(NoticeTask.getBsmodNameFieldName(), key);
                        }
                    }
                }
            });
        }
        queryWrapper.orderByDesc(NoticeTask.getShardingTimeFieldName());

        ShardingContext.getContext().setShardingTime((Long.valueOf(startTime) + Long.valueOf(endTime)) / 2);
        Page<NoticeTask> messagePage = this.baseMapper.selectPage(page, queryWrapper);
        return messagePage;
    }

    @Override
    public List<NoticeTask> queryListByKeyword(String startTime, String endTime, String id, Long noticeTopicId, String bsmodCode, String bsmodName) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeTask> queryWrapper = new QueryWrapper<>();
        if (startTime != null) {
            queryWrapper.ge(NoticeTask.getShardingTimeFieldName(), startTime);
        }
        if (endTime != null) {
            queryWrapper.le(NoticeTask.getShardingTimeFieldName(), endTime);
        }
        if (noticeTopicId != null) {
            queryWrapper.eq(NoticeTask.getNoticeTopicIdFieldName(), noticeTopicId);
        }
        if (StringUtils.isNotBlank(id)) {
            String key = dbStringUtil.getString(id);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeTask.getIdFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeTask.getIdFieldName(), key);
            }
        }
        if (StringUtils.isNotBlank(bsmodCode)) {
            String key = dbStringUtil.getString(bsmodCode);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeTask.getBsmodCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeTask.getBsmodCodeFieldName(), key);
            }
        }
        if (StringUtils.isNotBlank(bsmodName)) {
            String key = dbStringUtil.getString(bsmodName);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeTask.getBsmodNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeTask.getBsmodNameFieldName(), key);
            }
        }
        return super.list(queryWrapper);
    }

    @Override
    public String getContent(Long noticeTaskProtocolId) {
        NoticeTaskProtocol noticeTaskProtocol = noticeTaskProtocolMapper.selectById(noticeTaskProtocolId);
        if (noticeTaskProtocol == null) {
           return null;
        }

        String content = noticeTaskProtocol.getContent();
        String text = "";
        String url = "";
        try {
            /**
             * 尝试将content转化为JSONObject，以获取消息的正文内容。如果转化失败或者消息正文内容的key不为text，则将content直接展示
             */
            JSONObject jsonObject = JSONObject.parseObject(content);
            text = jsonObject.getString("text");
            if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(text)) {
                text = content;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            text = content;
        }
        return text;
    }

}
