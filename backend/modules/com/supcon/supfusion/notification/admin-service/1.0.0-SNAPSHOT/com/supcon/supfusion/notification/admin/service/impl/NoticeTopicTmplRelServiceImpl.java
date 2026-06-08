package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import com.supcon.supfusion.notification.admin.service.NoticeTemplateService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicTmplRelService;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTmplateRelation;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTaskMapper;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTopicTmplRelDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/9 20:38
 */
@Service("adminNoticeTopicTmplRelServiceImpl")
public class NoticeTopicTmplRelServiceImpl extends NoticeBaseServiceImpl<NoticeTopicTmplRelDao, NoticeTopicTmplateRelation> implements NoticeTopicTmplRelService {
    @Resource(name = "adminNoticeTopicServiceImpl")
    private NoticeTopicService topicService;
    @Resource(name = "adminNoticeTemplateServiceImpl")
    private NoticeTemplateService templateService;
    @Resource(name = "adminNoticeProtocolServiceImpl")
    private NoticeProtocolService protocolService;

    @Override
    @Transactional
    public NoticeTopicTmplateRelation addEntity(Long topic, Long template, Long protocol) {
        NoticeTopicTmplateRelation relation = new NoticeTopicTmplateRelation();
        relation.setId(IDGenerator.newInstance().generate().longValue());
        relation.setTopic(topic);
        relation.setTemplate(template);
        relation.setProtocol(protocol);
        if (super.save(relation)) {
            return relation;
        } else {
            return null;
        }
    }

    @Override
    @Transactional
    public List<NoticeTopicTmplateRelation> addBatchEntity(Long topicId, List<Long> tmpIdList) {
        List<NoticeTopicTmplateRelation> relationList = new ArrayList<>();
        for (Long tmpId : tmpIdList) {
            NoticeTemplate temp = templateService.queryEntity(tmpId);
            NoticeTopicTmplateRelation relation = this.addEntity(topicId, tmpId, temp.getNoticeType());
            if (relation != null) {
                relationList.add(relation);
            }
        }
        return relationList;
    }

    @Override
    public List<NoticeTopicTmplateRelation> queryEntityByTopic(Long topic, Long protocol) {
        QueryWrapper<NoticeTopicTmplateRelation> queryWrapper = new QueryWrapper<NoticeTopicTmplateRelation>();
        queryWrapper.eq(NoticeTopicTmplateRelation.getTopicIdName(), topic);
        if (protocol != null) {
            queryWrapper.eq(NoticeTopicTmplateRelation.getNoticeProtocolIdFieldName(), protocol);
        }
        return super.list(queryWrapper);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> dealRelation(List<NoticeTopicTmplateRelation> topicTmplateRelations) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (NoticeTopicTmplateRelation relation : topicTmplateRelations) {
            Map<String, Object> map = new HashMap<>();
            map.put("topic", topicService.queryEntity(relation.getTopic()));
            map.put("template", templateService.queryEntity(relation.getTemplate()));
            map.put("protocol", protocolService.getOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), relation.getProtocol()).eq(NoticeProtocol.getValidFieldName(), 1)));
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public Boolean delByTopic(Long topicId) {
        QueryWrapper<NoticeTopicTmplateRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(NoticeTopicTmplateRelation.getTopicIdName(), topicId);
        return super.remove(queryWrapper);
    }

}
