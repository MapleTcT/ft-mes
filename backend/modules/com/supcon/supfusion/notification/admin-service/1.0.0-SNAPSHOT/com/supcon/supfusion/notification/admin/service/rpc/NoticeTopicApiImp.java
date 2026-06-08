package com.supcon.supfusion.notification.admin.service.rpc;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.notification.admin.api.NoticeTopicApi;
import com.supcon.supfusion.notification.admin.api.dto.NoticeTopicDTO;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTmplateRelation;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTree;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTemplateDao;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTopicDao;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTopicTreeDao;
import com.supcon.supfusion.notification.admin.service.NoticeTopicTmplRelService;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ServiceApiService
public class NoticeTopicApiImp extends BaseController implements NoticeTopicApi {
    @Resource(name = "adminNoticeTopicDao")
    private NoticeTopicDao noticeTopicDao;

    @Resource(name = "adminNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;

    @Resource(name = "adminNoticeTemplateDao")
    private NoticeTemplateDao noticeTemplateDao;

    @Resource(name = "adminNoticeTopicTreeDao")
    private NoticeTopicTreeDao noticeTopicTreeDao;

    @Resource(name = "adminNoticeTopicTmplRelServiceImpl")
    private NoticeTopicTmplRelService noticeTopicTmplRelService;

    @Autowired(required = false)
    private MessageResourceWrapper messageResourceWrapper;

    @Override
    public void topicAddORUpdate(@Valid NoticeTopicDTO noticeTopicDTO) {
        NoticeTopicTree noticeTopicTree = noticeTopicTreeDao.selectOne(Wrappers.<NoticeTopicTree>query().eq(NoticeTopicTree.getCodeFieldName(), noticeTopicDTO.getTopicTypeCode()));
        if (noticeTopicTree == null) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_TOPIC_TREE_DONOT_EXIST);
        }
        List<Long> tempIds = new ArrayList();
        for (String code : noticeTopicDTO.getTemplateCodes()) {
            NoticeTemplate noticeTemplate = noticeTemplateDao.selectOne(Wrappers.<NoticeTemplate>query().eq(NoticeTemplate.getCodeFieldName(), code));
            if (noticeTemplate != null) {
                tempIds.add(noticeTemplate.getId());
            }
        }
        if (tempIds.size() == 0) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_TOPIC_HAS_NO_TEMPLATE);
        }
        long topicId = IDGenerator.newInstance().generate().longValue();
        NoticeTopic entity = new NoticeTopic();
        entity.setId(topicId);
        entity.setCode(noticeTopicDTO.getCode());
        entity.setName(noticeTopicDTO.getName());
        entity.setType(noticeTopicTree.getId());
        entity.setCoverSign(1);
        try {
            noticeTopicDao.insert(entity);
            noticeTopicTmplRelService.addBatchEntity(entity.getId(), tempIds);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof DuplicateKeyException) {
                entity.setId(null);
                int num = noticeTopicDao.update(entity, Wrappers.<NoticeTopic>update().eq(NoticeTopic.getCodeFieldName(), entity.getCode()).eq(NoticeTopic.getCoverSignFieldName(), 1));
                if (num == 0) {
                    throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_TEMPLATE);
                }
                NoticeTopic noticeTopic = noticeTopicDao.selectOne(Wrappers.<NoticeTopic>query().eq(NoticeTopic.getCodeFieldName(), entity.getCode()));
                noticeTopicTmplRelService.delByTopic(noticeTopic.getId());
                noticeTopicTmplRelService.addBatchEntity(noticeTopic.getId(), tempIds);
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<ProtocolDTO> getTopicProtocols(@Valid String topicCode) {
        List<ProtocolDTO> topicProtocols = new ArrayList<>();
        NoticeTopic noticeTopic = noticeTopicDao.selectOne(Wrappers.<NoticeTopic>query().eq(NoticeTopic.getCodeFieldName(), topicCode));
        if (noticeTopic == null) {
            log.info("{} 主题不存在", topicCode);
            return topicProtocols;
        }
        List<NoticeTopicTmplateRelation> noticeTopicTmplateRelations = noticeTopicTmplRelService.list(Wrappers.<NoticeTopicTmplateRelation>query().eq(NoticeTopicTmplateRelation.getTopicIdName(), noticeTopic.getId()));
        if (noticeTopic == null || noticeTopicTmplateRelations.size() == 0) {
            log.info("{} 主题没有绑定模板", topicCode);
            return topicProtocols;
        }

        for (NoticeTopicTmplateRelation noticeTopicTmplateRelation : noticeTopicTmplateRelations) {
            ProtocolDTO protocolDTO = new ProtocolDTO();
            NoticeTemplate noticeTemplate = noticeTemplateDao.selectOne(Wrappers.<NoticeTemplate>query().eq(NoticeTemplate.getIdFieldName(), noticeTopicTmplateRelation.getTemplate()));
            NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), noticeTemplate.getNoticeType()).eq(NoticeProtocol.getValidFieldName(), 1));
            protocolDTO.setProtocol(noticeProtocol.getProtocol());
            if (StringUtils.hasText(noticeProtocol.getI18nKey())) {
                String realName = messageResourceWrapper.getMessageNotBlank(noticeProtocol.getI18nKey());
                protocolDTO.setName(realName);
            } else {
                protocolDTO.setName(noticeProtocol.getName());
            }
            topicProtocols.add(protocolDTO);
        }
        return topicProtocols;
    }

}
