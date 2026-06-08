package com.supcon.supfusion.notification.admin.webapi.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTree;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTopicTreeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

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
public class NoticeTopicTreeWapper {
    @Autowired(required = false)
    MessageResourceWrapper messageResourceWrapper;

    /***
     * 对象拷贝
     * @param topicTree
     * @return
     */
    public NoticeTopicTreeVO entityCP(NoticeTopicTree topicTree) {
        NoticeTopicTreeVO topicTreeVO = new NoticeTopicTreeVO();
        Locale locale = LocaleContextHolder.getLocale();
        if (null != topicTree.getI18nKey()) {
            topicTree.setName(messageResourceWrapper.getMessageNotBlank(topicTree.getI18nKey()));
        }
        BeanUtils.copyProperties(topicTree, topicTreeVO);
        /*if(TopicTree()!=null){
            NoticeProtocol noticeProtocol = protocolService.getById(TopicTree.getNoticeType());
        if(noticeProtocol!=null){
            TopicTreeVO.setProtocol(noticeProtocol);
            TopicTreeVO.setProtocol_id(noticeProtocol.getId().toString());
            TopicTreeVO.setProtocol_name(noticeProtocol.getName());
            TopicTreeVO.setProtocol_code(noticeProtocol.getId().toString());
        }
    }*/
        return topicTreeVO;
    }

    /**
     * 列表拷贝
     *
     * @param TopicTreeList
     * @return
     */
    public List<NoticeTopicTreeVO> listCP(List<NoticeTopicTree> TopicTreeList) {
        List<NoticeTopicTreeVO> TopicTreeVOList = new ArrayList<>();
        for (NoticeTopicTree TopicTree : TopicTreeList) {
            TopicTreeVOList.add(entityCP(TopicTree));

        }
        return TopicTreeVOList;
    }

    /**
     * 分页拷贝
     *
     * @param TopicTreePage
     * @return
     */
    public Page<NoticeTopicTreeVO> pageCP(Page<NoticeTopicTree> TopicTreePage) {
        Page<NoticeTopicTreeVO> TopicTreeVOPage = new Page<>();
        List<NoticeTopicTreeVO> TopicTreeVOList = new ArrayList<>();
        for (NoticeTopicTree TopicTree : TopicTreePage.getRecords()) {
            TopicTreeVOList.add(entityCP(TopicTree));
        }
        TopicTreeVOPage.setTotal(TopicTreePage.getTotal());
        TopicTreeVOPage.setRecords(TopicTreeVOList);
        TopicTreeVOPage.setSize(TopicTreePage.getSize());
        TopicTreeVOPage.setCurrent(TopicTreePage.getCurrent());
        TopicTreeVOPage.setOrders(TopicTreePage.getOrders());
        return TopicTreeVOPage;
    }
}
