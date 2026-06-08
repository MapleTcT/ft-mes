package com.supcon.supfusion.notification.admin.webapi.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTask;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTaskVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

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
public class NoticeTaskWapper {
    @Resource(name = "adminNoticeProtocolMapper")
    private NoticeProtocolMapper protocolMapper;
    @Autowired(required = false)
    MessageResourceWrapper messageResourceWrapper;

    /***
     * 对象拷贝
     * @param task
     * @return
     */
    public NoticeTaskVO entityCP(NoticeTask task) {
        NoticeTaskVO taskVO = new NoticeTaskVO();

        BeanUtils.copyProperties(task, taskVO);
        List<NoticeProtocol> protocols = protocolMapper.getProtocolNamesByTask(task.getId());
        //protocolNames国际化key进行处理
        if (protocols != null && protocols.size() > 0) {
            StringBuffer realProtocolName = new StringBuffer();
            Locale locale = LocaleContextHolder.getLocale();
            for (NoticeProtocol protocol : protocols) {
                if (StringUtils.isNotBlank(protocol.getI18nKey())) {
                    realProtocolName.append(messageResourceWrapper.getMessageNotBlank(protocol.getI18nKey()) + ",");
                } else {
                    realProtocolName.append(protocol.getName() + ",");
                }

            }
            if (realProtocolName.length() > 0) {
                realProtocolName.deleteCharAt(realProtocolName.length() - 1).toString();
            }
            taskVO.setProtocolNames(realProtocolName.toString());
        }
        return taskVO;
    }

    /**
     * 列表拷贝
     *
     * @param TaskList
     * @return
     */
    public List<NoticeTaskVO> listCP(List<NoticeTask> TaskList) {
        List<NoticeTaskVO> TaskVOList = new ArrayList<>();
        int size = TaskList.size();
        for (int i = 0; i < size; i++) {
            TaskVOList.add(entityCP(TaskList.get(i)));
        }
        return TaskVOList;
    }


    /**
     * 分页拷贝
     *
     * @param taskPageRecords
     * @return
     */
    public Page<NoticeTaskVO> pageCP(Page<NoticeTask> taskPageRecords) {
        Page<NoticeTaskVO> taskVOPage = new Page<>();
        List<NoticeTaskVO> noticeTaskVOS = new ArrayList<>();
        if (taskPageRecords == null || taskPageRecords.getRecords() == null || taskPageRecords.getRecords().size() == 0) {
            return taskVOPage;
        }
        taskPageRecords.getRecords().forEach(task -> {
            noticeTaskVOS.add(entityCP(task));
        });
        taskVOPage.setTotal(taskPageRecords.getTotal());
        taskVOPage.setRecords(noticeTaskVOS);
        taskVOPage.setSize(taskPageRecords.getSize());
        taskVOPage.setCurrent(taskPageRecords.getCurrent());
        taskVOPage.setOrders(taskPageRecords.getOrders());
        return taskVOPage;
    }
}
