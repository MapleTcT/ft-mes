package com.supcon.supfusion.notification.admin.webapi.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeProtocolVO;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTemplateVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/18 15:11
 */
@Service
public class NoticeTemplateWapper {
    @Resource(name = "adminNoticeProtocolServiceImpl")
    private NoticeProtocolService protocolService;
    @Autowired(required = false)
    MessageResourceWrapper messageResourceWrapper;

    /***
     * 对象拷贝
     * @param template
     * @return
     */
    public NoticeTemplateVO entityCP(NoticeTemplate template) {
        NoticeTemplateVO templateVO = new NoticeTemplateVO();
        Locale locale = LocaleContextHolder.getLocale();
        BeanUtils.copyProperties(template, templateVO);
        if (template.getNoticeType() != null) {
            NoticeProtocol noticeProtocol = protocolService.getById(template.getNoticeType());
            if (noticeProtocol != null) {
                NoticeProtocolVO protocolVO = new NoticeProtocolVO();
                BeanUtils.copyProperties(noticeProtocol, protocolVO);
                templateVO.setProtocol(protocolVO);
                templateVO.setProtocol_id(noticeProtocol.getId().toString());
                String name = "";
                if (StringUtils.hasText(noticeProtocol.getI18nKey())) {
                    name = messageResourceWrapper.getMessageNotBlank(noticeProtocol.getI18nKey());
                } else {
                    name = noticeProtocol.getName();
                }
                templateVO.setProtocol_name(name);
                templateVO.setProtocol_code(noticeProtocol.getProtocol());
            }
        }
        return templateVO;
    }

    /**
     * 列表拷贝
     *
     * @param templateList
     * @return
     */
    public List<NoticeTemplateVO> listCP(List<NoticeTemplate> templateList) {
        List<NoticeTemplateVO> templateVOList = new ArrayList<>();
        int size = templateList.size();
        for (int i = 0; i < size; i++) {
            templateVOList.add(entityCP(templateList.get(i)));
        }
        return templateVOList;
    }

    /**
     * 分页拷贝
     *
     * @param templatePage
     * @return
     */
    public Page<NoticeTemplateVO> pageCP(Page<NoticeTemplate> templatePage) {
        Page<NoticeTemplateVO> templateVOPage = new Page<>();
        List<NoticeTemplateVO> templateVOList = new ArrayList<>();
        int size = templatePage.getRecords().size();
        for (int i = 0; i < size; i++) {
            templateVOList.add(entityCP(templatePage.getRecords().get(i)));
        }
        templateVOPage.setTotal(templatePage.getTotal());
        templateVOPage.setRecords(templateVOList);
        templateVOPage.setSize(templatePage.getSize());
        templateVOPage.setCurrent(templatePage.getCurrent());
        templateVOPage.setOrders(templatePage.getOrders());
        return templateVOPage;
    }
}
