package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeBase;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTmplateRelation;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTemplateDao;
import com.supcon.supfusion.notification.admin.service.NoticeTemplateService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicTmplRelService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:05
 */
@Service("adminNoticeTemplateServiceImpl")
public class NoticeTemplateServiceImpl extends NoticeBaseServiceImpl<NoticeTemplateDao, NoticeTemplate> implements NoticeTemplateService {

    @Resource(name = "adminNoticeTopicTmplRelServiceImpl")
    private NoticeTopicTmplRelService topicTmplRelService;
    @Resource(name = "adminNoticeTopicServiceImpl")
    private NoticeTopicService topicService;
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
    public Boolean validTemplateCode(String code) {
        Integer count = count(Wrappers.<NoticeTemplate>query().eq(NoticeTemplate.getCodeFieldName(), code));
        if (count == null || count == 0) {
            return true;
        } else {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_TEMPLATE_CODE);
        }
    }

    /***
     *新增实体
     * @param entity
     * @return
     */
    @Override
    public NoticeTemplate addEntity(NoticeTemplate entity) {
        //编码唯一
        validTemplateCode(entity.getCode());
        //生成ID
        entity.setId(IDGenerator.newInstance().generate().longValue());
        try {
            if (super.save(entity)) {
                return entity;
            } else {
                return null;
            }
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException) {
                log.error(NotificationAdminError.ERROR_DUPLICATE_TEMPLATE.getMessage() + e.getMessage(), e);
                throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_TEMPLATE);
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<NoticeTemplate> queryList(String code, String name, Long id, String protocolIds) {
        QueryWrapper<NoticeTemplate> queryWrapper = new QueryWrapper<NoticeTemplate>();
        if (id != null) {
            queryWrapper.eq(NoticeBase.getIdFieldName(), id);
        } else if (code != null) {
            queryWrapper.eq(NoticeBase.getCodeFieldName(), code);
        }
        if (name != null) {
            queryWrapper.eq(NoticeBase.getNameFieldName(), name);
        }
        if (protocolIds != null) {
            String[] protocolIdList = protocolIds.split(",");
            queryWrapper.in(NoticeTemplate.getNoticeTypeName(), protocolIdList);
        }
        List<NoticeTemplate> result = super.list(queryWrapper);
        return result;
    }


    @Override
    public Page<NoticeTemplate> queryPageList(String code, String name, Long id, String protocolIds, Page<NoticeTemplate> page) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeTemplate> queryWrapper = new QueryWrapper<NoticeTemplate>();
        if (id != null) {
            queryWrapper.eq(NoticeBase.getIdFieldName(), id);
        }
        if (code != null) {
            String[] codeList = code.split(",");
            queryWrapper.and(codelb -> {
                String key = dbStringUtil.getString(codeList[0]);
                if ("oracle".equals(dbType)) {
                    codelb.apply(NoticeBase.getCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    codelb.like(NoticeBase.getCodeFieldName(), key);
                }
                for (int i = 0; i < codeList.length; i++) {
                    if (i > 0) {
                        key = dbStringUtil.getString(codeList[i]);
                        if ("oracle".equals(dbType)) {
                            codelb.or().apply(NoticeBase.getCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
                        } else {
                            codelb.or().like(NoticeBase.getCodeFieldName(), key);
                        }
                    }
                }
            });
        }
        if (name != null) {
            String[] nameList = name.split(",");
            queryWrapper.and(receiverlb -> {
                String key = dbStringUtil.getString(nameList[0]);
                if ("oracle".equals(dbType)) {
                    receiverlb.apply(NoticeBase.getNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
                } else {
                    receiverlb.like(NoticeBase.getNameFieldName(), key);
                }
                for (int i = 0; i < nameList.length; i++) {
                    if (i > 0) {
                        key = dbStringUtil.getString(nameList[i]);
                        if ("oracle".equals(dbType)) {
                            receiverlb.or().apply(NoticeBase.getNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
                        } else {
                            receiverlb.or().like(NoticeBase.getNameFieldName(), key);
                        }
                    }
                }
            });
        }
        if (protocolIds != null) {
            String[] protocolIdList = protocolIds.split(",");
            queryWrapper.in(NoticeTemplate.getNoticeTypeName(), protocolIdList);
        }
        queryWrapper.orderByDesc("create_time");
//        List<NoticeTemplate> templateList  = this.baseMapper.getListByPage(queryWrapper,(page.getCurrent()-1)*page.getSize(), page.getCurrent()*page.getSize());
//        page.setRecords(templateList);
//        page.setTotal(templateList.size());
        Page<NoticeTemplate> page1 = super.page(page, queryWrapper);
        return page;
    }

    @Override
    public Map<NoticeProtocol, NoticeTemplate> queryTopicTmplRel(Long topic, Long protocol) {
        //组装的数据需要按顺序插入，HashMap是无序的
        Map<NoticeProtocol, NoticeTemplate> result = new LinkedHashMap<>();
        //获取关联表
        List<NoticeTopicTmplateRelation> topicTmplateRelations = topicTmplRelService.queryEntityByTopic(topic, protocol);
        //处理关联表得到对象
        List<Map<String, Object>> relationList = topicTmplRelService.dealRelation(topicTmplateRelations);
        //解析对象为需求数据
        int size = relationList.size();
        for (int i = 0; i < size; i++) {
            result.put((NoticeProtocol) relationList.get(i).get("protocol"), (NoticeTemplate) relationList.get(i).get("template"));
        }
        return result;
    }

    @Override
    public NoticeTemplate defultTmpl(Long protocolId) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeTemplate> queryWrapper = new QueryWrapper<NoticeTemplate>();
        queryWrapper.eq(NoticeTemplate.getNoticeTypeName(), protocolId);
        String key = dbStringUtil.getString("defult");
        if ("oracle".equals(dbType)) {
            queryWrapper.apply(NoticeBase.getCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
        } else {
            queryWrapper.like(NoticeBase.getCodeFieldName(), key);
        }
        NoticeTemplate result = super.getOne(queryWrapper);
        return result;
    }

    @Override
    @Transactional
    public String delEntity(String ids) {
        if (validRef(ids)) {
            return super.delEntity(ids);
        }
        return null;
    }

    /***
     * 验证模板是否依赖消息主题，返回依赖结果
     * @param ids
     * @return
     */
    private Boolean validRef(String ids) {
        if (StringUtils.isNotBlank(ids)) {
            String[] idList = ids.split(",");

            QueryWrapper<NoticeTopicTmplateRelation> queryWrapper = new QueryWrapper<>();
            queryWrapper.in(NoticeTopicTmplateRelation.getTemplateIdName(), idList);

            //获取映射关系表
            List<NoticeTopicTmplateRelation> relationList = topicTmplRelService.list(queryWrapper);
            if (relationList != null && relationList.size() > 0) {
                int size = relationList.size();
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    //获取对象信息
                    if (relationList.get(i).getTopic() != null && relationList.get(i).getTemplate() != null) {
                        NoticeTopic topic = topicService.getById(relationList.get(i).getTopic());
                        NoticeTemplate template = super.getById(relationList.get(i).getTemplate());
                        if (topic != null && template != null) {
                            result.append("内容模板：[" + template.getName() + "]被消息主题：[" + topic.getName() + "]引用，不可删除!\n");
                        }
                    }
                }
                if (StringUtils.isNotBlank(result)) {
                    throw new NotificationAdminExecption(NotificationAdminError.ERROR_DELETE_TEMPLATE, result.substring(0, result.length() - 1));
                }
            }
        }
        return true;
    }

    @Override
    public List<NoticeTemplate> queryListByKeyword(String code, String name, String noticeTypeIds) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeTemplate> queryWrapper = new QueryWrapper<NoticeTemplate>();
        if (null != noticeTypeIds) {
            String[] noticeTypeId = noticeTypeIds.split(",");
            queryWrapper.in(NoticeTemplate.getNoticeTypeName(), noticeTypeId);
        }
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

        return super.list(queryWrapper);
    }


}
