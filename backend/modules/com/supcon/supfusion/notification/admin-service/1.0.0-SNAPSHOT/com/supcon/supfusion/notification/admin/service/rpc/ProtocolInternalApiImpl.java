package com.supcon.supfusion.notification.admin.service.rpc;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.notification.admin.api.ProtocolInternalApi;
import com.supcon.supfusion.notification.admin.api.dto.NoticeProtocolDTO;
import com.supcon.supfusion.notification.admin.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class ProtocolInternalApiImpl implements ProtocolInternalApi {
    @Resource(name = "adminNoticeProtocolServiceImpl")
    private NoticeProtocolService protocolService;
    @Autowired(required = false)
    private MessageResourceWrapper messageResourceWrapper;

    @Override
    public ListResult<NoticeProtocolDTO> protocols() {
        List<NoticeProtocol> noticeProtocols = protocolService.list(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getValidFieldName(), 1));
        List<NoticeProtocolDTO> noticeProtocolDTOS = new ArrayList<NoticeProtocolDTO>();
        for (NoticeProtocol noticeProtocol : noticeProtocols) {
            if (StringUtils.hasText(noticeProtocol.getI18nKey())) {
                noticeProtocol.setName(messageResourceWrapper.getMessageNotBlank(noticeProtocol.getI18nKey()));
            }
            NoticeProtocolDTO noticeProtocolDTO = BeanCopyUtil.copyBeanProperties(noticeProtocol, NoticeProtocolDTO::new);
            noticeProtocolDTOS.add(noticeProtocolDTO);
        }
        return new ListResult<>(noticeProtocolDTOS);
    }
}
