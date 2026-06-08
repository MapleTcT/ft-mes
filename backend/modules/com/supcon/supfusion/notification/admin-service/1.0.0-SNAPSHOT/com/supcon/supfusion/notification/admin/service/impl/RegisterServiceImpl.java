package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolTmpl;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTmplateRelation;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolTmplMapper;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTemplateDao;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTopicTmplRelDao;
import com.supcon.supfusion.notification.admin.service.RegisterService;
import com.supcon.supfusion.notification.admin.service.bo.ProtocolConfigBO;
import com.supcon.supfusion.notification.admin.service.bo.ProtocolTemplateBO;
import com.supcon.supfusion.notification.admin.service.scheduling.DynamicTableTask;
import com.supcon.supfusion.notification.common.constants.Constants;
import com.supcon.supfusion.notification.common.util.JSONUtil;
import com.supcon.supfusion.notification.kafka.MultiTenantKafKa;
import com.supcon.supfusion.notification.protocol.config.ProtocolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;


@Slf4j
@Service("adminRegisterServiceImpl")
public class RegisterServiceImpl implements RegisterService {
    @Resource(name = "adminNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;
    @Resource(name = "adminNoticeProtocolTmplMapper")
    private NoticeProtocolTmplMapper noticeProtocolTmplMapper;
    @Resource(name = "adminNoticeTemplateDao")
    private NoticeTemplateDao noticeTemplateDao;
    @Resource(name = "adminNoticeTopicTmplRelDao")
    private NoticeTopicTmplRelDao noticeTopicTmplRelDao;
    @Autowired
    private MultiTenantKafKa multiTenantKafKa;
    @Autowired
    private DynamicTableTask dynamicTableTask;

    @Override
    @Transactional
    public Long register(ProtocolConfigBO protocolConfigBO) throws NotificationAdminExecption {
        boolean theSameApp = false;
        List<ProtocolTemplateBO> templates = protocolConfigBO.getTemplates();

        long protocolId = IDGenerator.newInstance().generate().longValue();
        NoticeProtocol noticeProtocol = BeanCopyUtil.copyBeanProperties(protocolConfigBO, NoticeProtocol::new);
        noticeProtocol.setId(protocolId);
        noticeProtocol.setContentType(protocolConfigBO.getProtocolContentType());

        /**
         * 新增协议
         */
        try {
            noticeProtocolMapper.insert(noticeProtocol);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof DuplicateKeyException) {
                NoticeProtocol noticeProtocolInDB = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getProtocolFieldName(), noticeProtocol.getProtocol()));
                protocolId = noticeProtocolInDB.getId();

                noticeProtocol.setId(protocolId);
                if (noticeProtocolInDB.getValid() == 0) {
                    log.info("{} 无效协议激活", noticeProtocol.getProtocol());
                    noticeProtocol.setValid(1);
                    noticeProtocolMapper.update(noticeProtocol, Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getProtocolFieldName(), noticeProtocol.getProtocol()));
                    theSameApp = false;
                } else if (noticeProtocolInDB.getVenderName().equals(noticeProtocol.getVenderName()) && noticeProtocolInDB.getAppName().equals(noticeProtocol.getAppName())) {
                    /**
                     * 判断是否是相同的app重复注册，如果是重复注册修改除venderName, appName以外的所有信息
                     */
                    log.info("{} 相同app重复注册", noticeProtocol.getProtocol());
                    noticeProtocolMapper.update(noticeProtocol, Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getProtocolFieldName(), noticeProtocol.getProtocol()));
                    theSameApp = true;
                } else {
                    throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_PROTOCOL);
                }
            } else {
                throw e;
            }
        }

        /**
         * 新增协议基础模板
         */
        for (ProtocolTemplateBO templateBO : templates) {
            NoticeProtocolTmpl noticeProtocolTmpl = null;
            try {
                long protocolTmplId = IDGenerator.newInstance().generate().longValue();
                noticeProtocolTmpl = BeanCopyUtil.copyBeanProperties(templateBO, NoticeProtocolTmpl::new);
                noticeProtocolTmpl.setNoticeProtocolId(protocolId);
                noticeProtocolTmpl.setId(protocolTmplId);
                noticeProtocolTmplMapper.insert(noticeProtocolTmpl);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (e instanceof DuplicateKeyException) {
                    if (!theSameApp) {
                        throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_PROTOCOL_TEMPLATE);
                    } else if (noticeProtocolTmpl != null) {
                        log.info("{} 相同app重复注册基础模板,code: {}", noticeProtocol.getProtocol(), noticeProtocolTmpl.getCode());
                        noticeProtocolTmplMapper.update(noticeProtocolTmpl, Wrappers.<NoticeProtocolTmpl>query().eq(NoticeProtocolTmpl.getCodeFieldName(), noticeProtocolTmpl.getCode()));
                    }
                } else {
                    throw e;
                }
            }
        }

        /**
         * 动态创建分表
         */
        try {
            dynamicTableTask.createTable(protocolConfigBO.getProtocol());
        } catch (Exception e) {
            log.error("=========================动态创建分表失败===========================");
            log.error(e.getMessage(), e);
//            throw new NotificationAdminExecption(NotificationAdminError.ERROR_ADD_PROTOCOL_FAIL);
        }

        ProtocolConfig protocolConfig = BeanCopyUtil.copyBeanProperties(protocolConfigBO, ProtocolConfig::new);
        multiTenantKafKa.send(Constants.ADD_PROTOCOL, JSONUtil.toJSONObject(protocolConfig));
        return protocolId;
    }

    @Override
    @Transactional
    public void unregister(String appName, String venderName) {
        NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getVenderNameFieldName(), venderName).eq(NoticeProtocol.getAppNameFieldName(), appName).eq(NoticeProtocol.getValidFieldName(), 1));
        if (noticeProtocol == null) return;
        Long protocolId = noticeProtocol.getId();

        noticeProtocol = new NoticeProtocol();
        noticeProtocol.setValid(0);
        noticeProtocolMapper.update(noticeProtocol, Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), protocolId));
        noticeProtocolTmplMapper.delete(Wrappers.<NoticeProtocolTmpl>query().eq(NoticeProtocolTmpl.getNoticeProtocolIdFieldName(), protocolId));
        noticeTemplateDao.delete(Wrappers.<NoticeTemplate>query().eq(NoticeTemplate.getNoticeProtocolIdFieldName(), protocolId));
        noticeTopicTmplRelDao.delete(Wrappers.<NoticeTopicTmplateRelation>query().eq(NoticeTopicTmplateRelation.getNoticeProtocolIdFieldName(), protocolId));
    }
}
