package com.supcon.supfusion.notification.admin.service.rpc;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.admin.api.NoticeTemplateApi;
import com.supcon.supfusion.notification.admin.api.dto.NoticeTemplateDTO;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTemplateDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;


import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@ServiceApiService
public class NoticeTemplateApiImp extends BaseController implements NoticeTemplateApi {
    @Resource(name = "adminNoticeTemplateDao")
    private NoticeTemplateDao templateDao;

    @Resource(name = "adminNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;

    @Override
    public void addTemplateORUpdate(@Valid NoticeTemplateDTO noticeTemplateDTO) {
        NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getProtocolFieldName(), noticeTemplateDTO.getProtocol()).eq(NoticeProtocol.getValidFieldName(), 1));
        if (noticeProtocol == null) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_PROTOCOL_DONT_EXIST);
        }
        long templateId = IDGenerator.newInstance().generate().longValue();
        NoticeTemplate entity = new NoticeTemplate();
        entity.setId(templateId);
        entity.setCode(noticeTemplateDTO.getCode());
        entity.setName(noticeTemplateDTO.getName());
        entity.setNoticeType(noticeProtocol.getId());
        entity.setMemo(noticeTemplateDTO.getMemo());
        entity.setCoverSign(1);
        entity.setTemplate(noticeTemplateDTO.getTemplate());
        try {
            templateDao.insert(entity);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof DuplicateKeyException) {
                entity.setId(null);
                int num = templateDao.update(entity, Wrappers.<NoticeTemplate>update().eq(NoticeTemplate.getCodeFieldName(), entity.getCode()).eq(NoticeTemplate.getCoverSignFieldName(), 1));
                if (num == 0) {
                    throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_TEMPLATE);
                }
            } else {
                throw e;
            }
        }
    }


}
