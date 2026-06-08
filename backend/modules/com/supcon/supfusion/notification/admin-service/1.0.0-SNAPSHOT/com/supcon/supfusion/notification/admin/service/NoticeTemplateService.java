package com.supcon.supfusion.notification.admin.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;

import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:04
 */
public interface NoticeTemplateService extends NoticeBaseService<NoticeTemplate>{

    /***
     * 验证模板编码是否存在
     * @param code
     * @return
     */
    public Boolean validTemplateCode(String code);
    /***
     * 扩展条件查询列表
     * @param code
     * @param name
     * @param id
     * @param protocolIds
     * @return
     */
    public List<NoticeTemplate> queryList(String code, String name, Long id, String protocolIds);

    /**
     * 根据通知方式分页查询消息模板
     * @param code
     * @param name
     * @param id
     * @param protocolIds
     * @param page
     * @return
     */
    public Page<NoticeTemplate> queryPageList(String code, String name, Long id, String protocolIds, Page<NoticeTemplate> page);

    /***
     * 根据通知方式获取默认模板
     * @param protocolId
     * @return
     */
    public NoticeTemplate defultTmpl(Long protocolId);

    /***
     * 根据主题和通知方式获取通知方式对应的模板
     * @param topic 消息主题
     * @param protocol 通知方式
     * @return
     */
    public Map<NoticeProtocol,NoticeTemplate> queryTopicTmplRel(Long topic, Long protocol);
    
    public List<NoticeTemplate>  queryListByKeyword(String code,String name ,String noticeTypeIds);
	

}
