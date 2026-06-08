package com.supcon.supfusion.notification.admin.webapi.utils;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import com.supcon.supfusion.notification.admin.service.NoticeTemplateService;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeProtocolVO;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTemplateVO;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTopicVO;
import org.aspectj.weaver.ast.Not;
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
public class NoticeTopicWapper {

    @Resource(name = "adminNoticeProtocolServiceImpl")
    private NoticeProtocolService protocolService;
    @Resource(name = "adminNoticeTemplateServiceImpl")
    private NoticeTemplateService templateService;
    @Autowired(required = false)
    MessageResourceWrapper messageResourceWrapper;

    /***
     * 对象拷贝
     * @param topic
     * @return
     */
    public NoticeTopicVO entityCP(NoticeTopic topic) {
        NoticeTopicVO topicVO = new NoticeTopicVO();
        Locale locale = LocaleContextHolder.getLocale();
        BeanUtils.copyProperties(topic, topicVO);
        //通知方式级联查询
        if (topic.getProtocol_id() != null && topic.getProtocol_name() == null) {
            //单个对象
            NoticeProtocol noticeProtocol = protocolService.getOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), topic.getProtocol_id()).eq(NoticeProtocol.getValidFieldName(), 1));
            NoticeProtocolVO protocolVO = new NoticeProtocolVO();
            BeanUtils.copyProperties(noticeProtocol, protocolVO);
            if (noticeProtocol != null) {
                topicVO.setProtocol(protocolVO);
                topicVO.setProtocolId(noticeProtocol.getId().toString());
                if (StringUtils.hasText(noticeProtocol.getI18nKey())) {
                    topicVO.setProtocolName(messageResourceWrapper.getMessageNotBlank(noticeProtocol.getI18nKey()));
                } else {
                    topicVO.setProtocolName(noticeProtocol.getName());
                }
                topicVO.setProtocolCode(noticeProtocol.getId().toString());
            }
        } else {
            //多个对象集合时
            topicVO.setProtocolId(topic.getProtocol_id());
            if (null != topic.getProtocol_name() && !"".equals(topic.getProtocol_name())) {
                String[] protoNames = topic.getProtocol_name().split(",");
                StringBuffer protoNameArray = new StringBuffer();
                for (String protoName : protoNames) {
                    protoNameArray.append(messageResourceWrapper.getMessageNotBlank(protoName) + ",");
                }
                if (protoNameArray.length() == 1) {
                    topicVO.setProtocolName(protoNameArray.toString());
                } else if (protoNameArray.length() > 1) {
                    topicVO.setProtocolName(protoNameArray.deleteCharAt(protoNameArray.length() - 1).toString());
                }
            }

        }
        //消息模板级联查询
        if (topic.getTemplate_id() != null && topic.getTemplate_name() == null) {
            NoticeTemplate template = templateService.getById(topic.getTemplate_id());
            if (template != null) {
                NoticeTemplateVO templateVO = new NoticeTemplateVO();
                BeanUtils.copyProperties(template, templateVO);
                topicVO.setTemplate(templateVO);
                topicVO.setProtocolId(template.getId().toString());
                topicVO.setTemplateName(template.getName());
                topicVO.setTemplateCode(template.getId().toString());
            }
        } else {
            //多个对象集合时
            topicVO.setTemplateId(topic.getTemplate_id());
            topicVO.setTemplateName(topic.getTemplate_name());
        }
        return topicVO;
    }

    /**
     * 列表拷贝
     *
     * @param TopicList
     * @return
     */
    public List<NoticeTopicVO> listCP(List<NoticeTopic> TopicList) {
        List<NoticeTopicVO> TopicVOList = new ArrayList<>();
        for (NoticeTopic Topic : TopicList) {
            TopicVOList.add(entityCP(Topic));

        }
        return TopicVOList;
    }

    /**
     * 分页拷贝
     *
     * @param TopicPage
     * @return
     */
    public Page<NoticeTopicVO> pageCP(Page<NoticeTopic> TopicPage) {
        Page<NoticeTopicVO> TopicVOPage = new Page<>();
        List<NoticeTopicVO> TopicVOList = new ArrayList<>();
        for (NoticeTopic Topic : TopicPage.getRecords()) {
            TopicVOList.add(entityCP(Topic));
        }
        TopicVOPage.setTotal(TopicPage.getTotal());
        TopicVOPage.setRecords(TopicVOList);
        TopicVOPage.setSize(TopicPage.getSize());
        TopicVOPage.setCurrent(TopicPage.getCurrent());
        TopicVOPage.setOrders(TopicPage.getOrders());
        return TopicVOPage;
    }


}
