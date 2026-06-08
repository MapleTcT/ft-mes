package com.supcon.supfusion.notification.admin.service;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolTmpl;

import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:26
 */
public interface NoticeProtocolTmplService extends NoticeBaseService<NoticeProtocolTmpl> {
    /***
     * 根据通知方式获取默认基础模板信息
     * @param protoclId
     * @return
     */
    public NoticeProtocolTmpl protocolDefaultTmpl(Long protoclId);

    /***
     * 根据通知方式获取对应基础模板信息
     * @param protoclId
     * @return
     */
    public List<NoticeProtocolTmpl> protocolTmpl(Long protoclId);

    Long addProtocolTemplate(String title, String content, Long protocolId);

    void updateProtocolTemplate(String title, String content, Long templateId);

    void deleteProtocolTemplate(List<Long> ids);

}
