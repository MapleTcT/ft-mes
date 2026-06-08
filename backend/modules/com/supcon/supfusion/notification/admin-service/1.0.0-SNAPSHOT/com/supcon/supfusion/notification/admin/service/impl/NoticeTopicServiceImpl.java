package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeBase;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRange;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRangeExt;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTask;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicList;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTmplateRelation;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTopicDao;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import com.supcon.supfusion.notification.admin.service.NoticeReceiveRangeExtService;
import com.supcon.supfusion.notification.admin.service.NoticeReceiveRangeService;
import com.supcon.supfusion.notification.admin.service.NoticeTemplateService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicTmplRelService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicTreeService;
import com.supcon.supfusion.notification.admin.service.bo.NoticeTopicListBO;
import com.supcon.supfusion.notification.admin.service.util.SpecialCharacterUtil;
import com.supcon.supfusion.notification.common.bean.RangeType;
import com.supcon.supfusion.notification.common.util.FreeMarkUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:05
 */
@Service("adminNoticeTopicServiceImpl")
public class NoticeTopicServiceImpl extends NoticeBaseServiceImpl<NoticeTopicDao, NoticeTopic> implements NoticeTopicService {
    @Resource(name = "adminNoticeTopicTreeServieImpl")
    private NoticeTopicTreeService topicTreeService;
    @Resource(name = "adminNoticeTopicTmplRelServiceImpl")
    private NoticeTopicTmplRelService topicTmplRelService;
    @Resource(name = "adminNoticeTemplateServiceImpl")
    private NoticeTemplateService noticeTemplateService;
    @Resource(name = "adminNoticeProtocolServiceImpl")
    private NoticeProtocolService noticeProtocolService;
    @Resource(name = "adminNoticeReceiveRangeServiceImpl")
    private NoticeReceiveRangeService noticeReceiveRangeService;
    @Resource(name = "adminNoticeReceiveRangeExtServiceImpl")
    private NoticeReceiveRangeExtService noticeReceiveRangeExtService;
    @Autowired(required = false)
    private MessageResourceWrapper messageResourceWrapper;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;

    /***
     * 验证模板编码是否重复
     * @param code
     * @return
     */
    @Override
    public Boolean validTopicCode(String code) {
        Integer count = count(Wrappers.<NoticeTopic>query().eq(NoticeTopic.getCodeFieldName(), code));
        if (count == null || count == 0) {
            return true;
        } else {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_TOPIC_CODE);
        }
    }

    @Override
    public List<NoticeTopic> queryList(String code, String name, Long topicTree) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeTopic> queryWrapper = new QueryWrapper<>();
        if (code != null) {
            String key = dbStringUtil.getString(code);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeBase.getCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeBase.getCodeFieldName(), key);
            }
        }
        if (name != null) {
            String key = dbStringUtil.getString(name);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeBase.getNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeBase.getNameFieldName(), key);
            }
        }
        if (topicTree != null) {
            queryWrapper.eq(NoticeTopic.getTopicTypeName(), topicTree);
        }
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public Page<NoticeTopicListBO> queryPageList(String code, String name, Long topicTree, String protocolIds, String templateName, Integer pageNo, Integer pageSize) {
        Long[] protocols = null;
        if (StringUtils.hasText(protocolIds)) {
            String[] ids = protocolIds.split(",");
            if (ids != null && ids.length > 0) {
                protocols = new Long[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    protocols[i] = Long.valueOf(ids[i]);
                }
            }
        }

        String[] names = null;
        if (StringUtils.hasText(name)) {
            names = SpecialCharacterUtil.convert(name, dbStringUtil);
        }

        String[] codes = null;
        if (StringUtils.hasText(code)) {
            codes = SpecialCharacterUtil.convert(code, dbStringUtil);
        }

        String[] templateNames = null;
        if (StringUtils.hasText(templateName)) {
            templateNames = SpecialCharacterUtil.convert(templateName, dbStringUtil);
        }

        List<NoticeTopicList> noticeTopicGroups = this.baseMapper.getListByPage(names, codes, topicTree, protocols, templateNames, (pageNo - 1) * pageSize, pageNo * pageSize);
        Integer count = this.baseMapper.getListByPageCount(names, codes, topicTree, protocols, templateNames);
        List<NoticeTopicListBO> noticeTopicListBOS = new ArrayList<>();
        if (noticeTopicGroups != null && noticeTopicGroups.size() > 0) {
            noticeTopicGroups.forEach(noticeTopicGroup -> {
                NoticeTopicListBO noticeTopicListBO = new NoticeTopicListBO();
                /**
                 * Set去重
                 */
                Set<String> templateIdSet = new HashSet<>();
                Set<String> rangeIdSet = new HashSet<>();
                String templateIds = noticeTopicGroup.getTemplateIds();
                if (StringUtils.hasText(templateIds)) {
                    String[] templateIdArray = templateIds.split(",");
                    if (templateIdArray != null && templateIdArray.length > 0) {
                        for (String templateId : templateIdArray) {
                            templateIdSet.add(templateId);
                        }
                    }
                }
                String rangeIds = noticeTopicGroup.getRangeIds();
                if (StringUtils.hasText(rangeIds)) {
                    String[] rangeIdArray = rangeIds.split(",");
                    if (rangeIdArray != null && rangeIdArray.length > 0) {
                        for (String rangeId : rangeIdArray) {
                            rangeIdSet.add(rangeId);
                        }
                    }
                }

                /**
                 * 处理模板名称和协议名称
                 */
                StringBuilder temlNames = new StringBuilder();
                StringBuilder protocolNames = new StringBuilder();
                templateIdSet.forEach(templateId -> {
                    NoticeTemplate noticeTemplate = noticeTemplateService.getOne(Wrappers.<NoticeTemplate>query().eq(NoticeTemplate.getIdFieldName(), templateId));
                    temlNames.append(noticeTemplate.getName());
                    temlNames.append(",");

                    NoticeProtocol noticeProtocol = noticeProtocolService.getOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), noticeTemplate.getNoticeType()).eq(NoticeProtocol.getValidFieldName(), 1));
                    String protocolName;
                    if (StringUtils.hasText(noticeProtocol.getI18nKey())) {
                        protocolName = messageResourceWrapper.getMessageNotBlank(noticeProtocol.getI18nKey());
                    } else {
                        protocolName = noticeProtocol.getName();
                    }
                    protocolNames.append(protocolName);
                    protocolNames.append(",");
                });
                if (temlNames.length() > 0) {
                    temlNames.deleteCharAt(temlNames.length() - 1);
                }
                if (protocolNames.length() > 0) {
                    protocolNames.deleteCharAt(protocolNames.length() - 1);
                }

                /**
                 * 处理接收范围
                 */
                List<String> staffCodes = new ArrayList<>();
                List<String> deptCodes = new ArrayList<>();
                List<String> positionCodes = new ArrayList<>();
                List<String> roleCodes = new ArrayList<>();
                rangeIdSet.forEach(rangeId -> {
                    NoticeRecieveRange noticeRecieveRange = noticeReceiveRangeService.getOne(Wrappers.<NoticeRecieveRange>query().eq(NoticeRecieveRange.getIdFieldName(), rangeId));
                    List<NoticeRecieveRangeExt> noticeRecieveRangeExts = noticeReceiveRangeExtService.list(Wrappers.<NoticeRecieveRangeExt>query().eq(NoticeRecieveRangeExt.getRangeIdFieldName(), rangeId));
                    if (RangeType.STAFF.tableValue() == noticeRecieveRange.getRangeType()) {
                        if (noticeRecieveRangeExts != null && noticeRecieveRangeExts.size() > 0) {
                            noticeRecieveRangeExts.forEach(range -> staffCodes.add(range.getReceiverCode()));
                        }
                    } else if (RangeType.DEPARTMENT.tableValue() == noticeRecieveRange.getRangeType()) {
                        if (noticeRecieveRangeExts != null && noticeRecieveRangeExts.size() > 0) {
                            noticeRecieveRangeExts.forEach(range -> deptCodes.add(range.getReceiverCode()));
                        }
                    } else if (RangeType.POSITION.tableValue() == noticeRecieveRange.getRangeType()) {
                        if (noticeRecieveRangeExts != null && noticeRecieveRangeExts.size() > 0) {
                            noticeRecieveRangeExts.forEach(range -> positionCodes.add(range.getReceiverCode()));
                        }
                    } else if (RangeType.ROLE.tableValue() == noticeRecieveRange.getRangeType()) {
                        if (noticeRecieveRangeExts != null && noticeRecieveRangeExts.size() > 0) {
                            noticeRecieveRangeExts.forEach(range -> roleCodes.add(range.getReceiverCode()));
                        }
                    }
                });
                noticeTopicListBO.setId(noticeTopicGroup.getTopicId());
                noticeTopicListBO.setCode(noticeTopicGroup.getTopicCode());
                noticeTopicListBO.setName(noticeTopicGroup.getTopicName());
                noticeTopicListBO.setTemplateName(temlNames.toString());
                noticeTopicListBO.setProtocolName(protocolNames.toString());
                noticeTopicListBO.setStaffCodes(staffCodes);
                noticeTopicListBO.setDeptCodes(deptCodes);
                noticeTopicListBO.setPositionCodes(positionCodes);
                noticeTopicListBO.setRoleCodes(roleCodes);
                noticeTopicListBOS.add(noticeTopicListBO);
            });
        }
        /**
         * 内存分组方案
         */
//        if (noticeTopicGroups != null && noticeTopicGroups.size() > 0) {
//            Map<Long, List<NoticeTopicList>> topicIdNoticeTopicGroups = noticeTopicGroups.stream().collect(groupingBy(noticeTopicGroup -> noticeTopicGroup.getTopicId()));
//            List<NoticeTopicListBO> noticeTopicListBOs = topicIdNoticeTopicGroups.entrySet().stream().map(entrySet -> {
//                Long topicId = entrySet.getKey();
//                List<NoticeTopicList> values = entrySet.getValue();
//                NoticeTopicListBO noticeTopicListBO = new NoticeTopicListBO();
//                noticeTopicListBO.setId(topicId);
//                noticeTopicListBO.setCode(values.get(0).getTopicCode());
//                noticeTopicListBO.setName(values.get(0).getTopicName());
//
//                StringBuilder protocolNames = new StringBuilder();
//                StringBuilder templateNames = new StringBuilder();
//                values.stream().collect(groupingBy(noticeTopicGroup -> noticeTopicGroup.getProtocolId())).values().forEach(protocolGroup -> {
//                    if (protocolGroup == null || protocolGroup.size() == 0) {
//                        return;
//                    }
//                    if (StringUtils.hasText(list.get(0).getProtocolNameI18nKey())) {
//                        protocolNames.append(remoteBundleMessageSource.getMessage(list.get(0).getProtocolNameI18nKey(), null, LocaleContextHolder.getLocale()));
//                    } else {
//                    protocolNames.append(protocolGroup.get(0).getProtocolName());
//                    }
//                    protocolNames.append(",");
//                });
//                values.stream().collect(groupingBy(noticeTopicGroup -> noticeTopicGroup.getTemplateId())).values().forEach(templateGroup -> {
//                    if (templateGroup == null || templateGroup.size() == 0) {
//                        return;
//                    }
//                    templateNames.append(templateGroup.get(0).getTemplateName());
//                    templateNames.append(",");
//                });
//                if (protocolNames.length() > 0) {
//                    protocolNames.deleteCharAt(protocolNames.length() - 1);
//                }
//                if (templateNames.length() > 0) {
//                    templateNames.deleteCharAt(templateNames.length() - 1);
//                }
//                noticeTopicListBO.setProtocolName(protocolNames.toString());
//                noticeTopicListBO.setTemplateName(templateNames.toString());
//
//
//                List<String> staffCodes = new ArrayList<>();
//                List<String> deptCodes = new ArrayList<>();
//                List<String> positionCodes = new ArrayList<>();
//                List<String> roleCodes = new ArrayList<>();
//                values.stream().collect(groupingBy(noticeTopicGroup -> noticeTopicGroup.getRangeType())).entrySet().forEach(rangeGourps -> {
//                    if (RangeType.STAFF.value().equals(rangeGourps.getKey())) {
//                        if (rangeGourps.getValue() != null && rangeGourps.getValue().size() > 0) {
//                            rangeGourps.getValue().forEach(range -> staffCodes.add(range.getReceiverCode()));
//                        }
//                    } else if (RangeType.DEPARTMENT.value().equals(rangeGourps.getKey())) {
//                        if (rangeGourps.getValue() != null && rangeGourps.getValue().size() > 0) {
//                            rangeGourps.getValue().forEach(range -> deptCodes.add(range.getReceiverCode()));
//                        }
//                    } else if (RangeType.POSITION.value().equals(rangeGourps.getKey())) {
//                        if (rangeGourps.getValue() != null && rangeGourps.getValue().size() > 0) {
//                            rangeGourps.getValue().forEach(range -> positionCodes.add(range.getReceiverCode()));
//                        }
//                    } else if (RangeType.ROLE.value().equals(rangeGourps.getKey())) {
//                        if (rangeGourps.getValue() != null && rangeGourps.getValue().size() > 0) {
//                            rangeGourps.getValue().forEach(range -> roleCodes.add(range.getReceiverCode()));
//                        }
//                    }
//                });
//                noticeTopicListBO.setStaffCodes(staffCodes);
//                noticeTopicListBO.setDeptCodes(deptCodes);
//                noticeTopicListBO.setPositionCodes(positionCodes);
//                noticeTopicListBO.setRoleCodes(roleCodes);
//                return noticeTopicListBO;
//            }).collect(Collectors.toList());
//        }
        Page page = new Page();
        page.setRecords(noticeTopicListBOS);
        page.setCurrent(pageNo);
        page.setSize(pageSize);
        page.setTotal(count);
        return page;
    }


    /**
     * 解析关联对象
     * 备注 ，接收范围 由manage模块处理
     *
     * @param entity
     * @return
     */
    @Override
    @Transactional
    public NoticeTopic addEntity(NoticeTopic entity) {
        //编码唯一
        validTopicCode(entity.getCode());
        entity.setId(IDGenerator.newInstance().generate().longValue());
        try {
            NoticeTopic topic = super.addEntity(entity);
            if (entity.getTmpIdList() != null && entity.getTmpIdList().size() > 0) {
                topicTmplRelService.addBatchEntity(topic.getId(), entity.getTmpIdList());
            }
            return topic;
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException) {
                log.error(NotificationAdminError.ERROR_DUPLICATE_TOPIC.getMessage() + e.getMessage(), e);
                throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_TOPIC);
            } else {
                throw e;
            }
        }
    }

    @Override
    @Transactional
    public NoticeTopic updateEntity(NoticeTopic entity) {
        if (super.saveOrUpdate(entity)) {
            //修改时先删除所有关联关系再新建关系
            topicTmplRelService.delByTopic(entity.getId());
            if (null != entity.getTmpIdList() && entity.getTmpIdList().size() > 0) {
                topicTmplRelService.addBatchEntity(entity.getId(), entity.getTmpIdList());
            }
            return entity;
        }
        return null;
    }

    @Override
    public List<String> keywords(String topicCode) {
        NoticeTopic noticeTopic = query().eq(NoticeTopic.getCodeFieldName(), topicCode).one();
        if (noticeTopic == null) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_TOPIC_NOT_EXIST);
        }
        List<NoticeTopicTmplateRelation> noticeTopicTmplateRelations = topicTmplRelService.query().eq(NoticeTopicTmplateRelation.getIdName(), noticeTopic.getId()).list();

        if (noticeTopicTmplateRelations == null || noticeTopicTmplateRelations.size() == 0) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_TOPIC_HAS_NO_TEMPLATE);
        }
        List<Long> templateIds = new ArrayList<>();
        for (NoticeTopicTmplateRelation noticeTopicTmplateRelation : noticeTopicTmplateRelations) {
            templateIds.add(noticeTopicTmplateRelation.getId());
        }
        List<NoticeTemplate> noticeTemplates = noticeTemplateService.query().in(NoticeTemplate.getIdFieldName(), templateIds).list();
        if (noticeTemplates == null || noticeTemplates.size() == 0) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_TOPIC_HAS_NO_TEMPLATE);
        }

        List<String> keywords = new ArrayList<>();
        for (NoticeTemplate noticeTemplate : noticeTemplates) {
            String template = noticeTemplate.getTemplate();
            if (StringUtils.hasText(template)) {
                keywords.addAll(FreeMarkUtil.getVariable(template));
            }
        }
        return keywords;
    }

    /**
     * 关键字查询
     * 因为人员name是调人员的接口，receiver接收人的模糊查询暂时没做
     *
     * @param
     * @return
     */
    @Override
    public List<NoticeTopic> queryListByKeyword(String code, String name, String templateName, String
            receiver, String topicTreeId) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeTopic> queryWrapper = new QueryWrapper<NoticeTopic>();
        Long typeId = 0L;
        //topicTreeId一定存在
        if (topicTreeId != null) {
            typeId = Long.valueOf(topicTreeId);
        }
        if (templateName != null) {
            queryWrapper.eq("topic.notice_topic_type_id", typeId);
            String key = dbStringUtil.getString(templateName);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply("tmpl.name like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like("tmpl.name", key);
            }
            return this.baseMapper.getTmplName(queryWrapper);
        } else {
            queryWrapper.eq(NoticeTopic.getTopicTypeName(), typeId);
            if (code != null) {
                String key = dbStringUtil.getString(code);
                if ("oracle".equals(dbType)) {
                    queryWrapper.apply(NoticeTopic.getCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    queryWrapper.like(NoticeTopic.getCodeFieldName(), key);
                }
            }
            if (name != null) {
                String key = dbStringUtil.getString(name);
                if ("oracle".equals(dbType)) {
                    queryWrapper.apply(NoticeTopic.getNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    queryWrapper.like(NoticeTopic.getNameFieldName(), key);
                }
            }
            return super.list(queryWrapper);
        }

    }

}
