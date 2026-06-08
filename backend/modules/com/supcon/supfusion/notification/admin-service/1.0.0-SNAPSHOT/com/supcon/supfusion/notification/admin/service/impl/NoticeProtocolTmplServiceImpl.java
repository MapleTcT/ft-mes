package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolTmpl;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolTmplDao;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolTmplService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:05
 */
@Service("adminNoticeProtocolTmplServiceImpl")
public class NoticeProtocolTmplServiceImpl extends NoticeBaseServiceImpl<NoticeProtocolTmplDao, NoticeProtocolTmpl> implements NoticeProtocolTmplService {
    @Resource(name = "adminNoticeProtocolServiceImpl")
    private NoticeProtocolService protocolService;

    @Override
    public NoticeProtocolTmpl protocolDefaultTmpl(Long protoclId) {
        NoticeProtocol protocol = protocolService.getOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), protoclId).eq(NoticeProtocol.getValidFieldName(), 1));
        QueryWrapper<NoticeProtocolTmpl> queryWrapper = new QueryWrapper<>();
        if (protocol != null) {
            queryWrapper.eq(NoticeProtocolTmpl.getCodeFieldName(), protocol.getDefaultTemplateCode());
        }
        return super.getOne(queryWrapper);
    }

    @Override
    public List<NoticeProtocolTmpl> protocolTmpl(Long protoclId) {
        return super.list(Wrappers.<NoticeProtocolTmpl>query().eq(NoticeProtocolTmpl.getNoticeProtocolIdFieldName(), protoclId).orderByDesc(NoticeProtocolTmpl.getCreateTimeFieldName()));
    }

    /**
     * 防止模板数量大于10，该接口并发量不高直接加锁但是目前没有分布式锁实现 todo
     *
     * @param title
     * @param content
     * @param protocolId
     * @return
     */
    @Override
    public Long addProtocolTemplate(String title, String content, Long protocolId) {
        Integer count = count(Wrappers.<NoticeProtocolTmpl>query().eq(NoticeProtocolTmpl.getNoticeProtocolIdFieldName(), Long.valueOf(protocolId)));
        if (count != null && count >= 10) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_PROTOCOL_TEMPLATE_CANNOT_BE_GREATER_THAN_10);
        }

        NoticeProtocolTmpl noticeProtocolTmpl = new NoticeProtocolTmpl();
        long id = IDGenerator.newInstance().generate().longValue();
        noticeProtocolTmpl.setId(id);
        noticeProtocolTmpl.setCode(UUID.randomUUID().toString());
        noticeProtocolTmpl.setName(title);
        noticeProtocolTmpl.setTemplate(content);
        noticeProtocolTmpl.setNoticeProtocolId(protocolId);
        try {
            save(noticeProtocolTmpl);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof DuplicateKeyException) {
                throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_PROTOCOL_TEMPLATE_NAME);
            } else {
                throw e;
            }
        }
        return id;
    }

    @Override
    public void updateProtocolTemplate(String title, String content, Long templateId) {
        NoticeProtocolTmpl noticeProtocolTmpl = new NoticeProtocolTmpl();
        noticeProtocolTmpl.setId(templateId);
        if (StringUtils.hasText(title)) {
            /**
             * 用户输入后以用户输入为准，清除国际化key
             */
            noticeProtocolTmpl.setName(title);
            noticeProtocolTmpl.setI18nKey("");
        }
        if (StringUtils.hasText(content)) {
            noticeProtocolTmpl.setTemplate(content);
        }
        try {
            updateById(noticeProtocolTmpl);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof DuplicateKeyException) {
                throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_PROTOCOL_TEMPLATE_NAME);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void deleteProtocolTemplate(List<Long> ids) {
        delByCondition(Wrappers.<NoticeProtocolTmpl>query().in(NoticeProtocolTmpl.getIdFieldName(), ids));
    }
}
