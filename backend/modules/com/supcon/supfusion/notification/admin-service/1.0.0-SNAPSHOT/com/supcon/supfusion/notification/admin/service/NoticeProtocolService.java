package com.supcon.supfusion.notification.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;

import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:26
 */
public interface NoticeProtocolService extends NoticeBaseService<NoticeProtocol> {
    List<NoticeProtocol> getList(String code, String name, Long id, String dbType, DbStringUtil dbStringUtil, boolean all);

    Page<NoticeProtocol> getPageList(String code, String name, Long id, Page<NoticeProtocol> page, String dbType, DbStringUtil dbStringUtil, boolean all);
}
