package com.supcon.supfusion.notification.admin.webapi.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.util.DateUtil;
import com.supcon.supfusion.notification.admin.service.NoticeTaskService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicService;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeProtocolMessageVO;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMsg;
import com.supcon.supfusion.notification.protocol.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/18 15:11
 */
@Service
public class NoticeProtocolMessageWapper {

    @Resource(name = "adminNoticeTaskServiceImpl")
    private NoticeTaskService taskService;
    @Resource(name = "adminNoticeTopicServiceImpl")
    private NoticeTopicService topicService;

    /***
     * 对象拷贝
     * @param message
     * @return
     */
    public NoticeProtocolMessageVO entityCP(NoticeMsg message) {
        NoticeProtocolMessageVO messageVO = new NoticeProtocolMessageVO();
        BeanUtils.copyProperties(message, messageVO);
        messageVO.setTopic(message.getTopicName());
        messageVO.setNoticeProtocolId(message.getNoticeProtocolId() != null ? message.getNoticeProtocolId().toString() : null);
        messageVO.setNoticeTaskId(message.getNoticeTaskId() != null ? message.getNoticeTaskId().toString() : null);
        messageVO.setNoticeTaskProtocolId(message.getNoticeTaskProtocolId() != null ? message.getNoticeTaskProtocolId().toString() : null);
        messageVO.setSender(message.getBsmodName());
        messageVO.setCreateTime(StringUtils.isEmpty(message.getCreateTime()) ? DateUtil.parse(message.getCreateTime(), DateUtil.DATETIME_FORMAT).getTime() : null );
        //通知方式级联查询出主题名称
//        if(message.getNoticeTaskId()!=null ){
//            NoticeTask task = taskService.getTask(message.getNoticeTaskId());
//            if(task!=null){
//                NoticeTopic topic = topicService.queryEntity(task.getNoticeTopicId());
//                messageVO.setTopic(topic!=null?topic.getName():null);
//            }
//        }
        return messageVO;
    }

    /**
     * 列表拷贝
     *
     * @param messagePage
     * @return
     */
    public List<NoticeProtocolMessageVO> listCP(List<NoticeMsg> messagePage) {
        List<NoticeProtocolMessageVO> TopicVOList = new ArrayList<>();
        for (NoticeMsg message : messagePage) {
            TopicVOList.add(entityCP(message));

        }
        return TopicVOList;
    }

    /**
     * 分页拷贝
     *
     * @param messagePage
     * @return
     */
    public Page<NoticeProtocolMessageVO> pageCP(Page<NoticeMsg> messagePage) {
        Page<NoticeProtocolMessageVO> TopicVOPage = new Page<>();
        List<NoticeProtocolMessageVO> TopicVOList = new ArrayList<>();
        for (NoticeMsg Topic : messagePage.getRecords()) {
            TopicVOList.add(entityCP(Topic));
        }
        TopicVOPage.setTotal(messagePage.getTotal());
        TopicVOPage.setRecords(TopicVOList);
        TopicVOPage.setSize(messagePage.getSize());
        TopicVOPage.setCurrent(messagePage.getCurrent());
        TopicVOPage.setOrders(messagePage.getOrders());
        return TopicVOPage;
    }


}
