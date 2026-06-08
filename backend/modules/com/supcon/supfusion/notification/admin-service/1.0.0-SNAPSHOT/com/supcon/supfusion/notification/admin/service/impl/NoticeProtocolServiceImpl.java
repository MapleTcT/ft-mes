package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeBase;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:05
 */
@Service("adminNoticeProtocolServiceImpl")
public class NoticeProtocolServiceImpl extends NoticeBaseServiceImpl<NoticeProtocolMapper, NoticeProtocol> implements NoticeProtocolService {

    public List<NoticeProtocol> getList(String code, String name, Long id, String dbType, DbStringUtil dbStringUtil, boolean all) {
        QueryWrapper<NoticeProtocol> queryWrapper = new QueryWrapper<NoticeProtocol>();
        if (!all) {
            queryWrapper.eq(NoticeBase.getValidFieldName(), 1);
        }
        if (id != null) {
            queryWrapper.eq(NoticeBase.getIdFieldName(), id);
        }
        if (code != null) {
            String key = dbStringUtil.getString(code);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeBase.getCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeBase.getCodeFieldName(), key);
            }
        }
        if (name != null) {
            String key = dbStringUtil.getString(name);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeBase.getNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeBase.getNameFieldName(), key);
            }
        }
        List<NoticeProtocol> result = super.list(queryWrapper);
        return result;
    }


    public Page<NoticeProtocol> getPageList(String code, String name, Long id, Page<NoticeProtocol> page, String dbType, DbStringUtil dbStringUtil, boolean all) {
        QueryWrapper<NoticeProtocol> queryWrapper = new QueryWrapper<NoticeProtocol>();
        if (!all) {
            queryWrapper.eq(NoticeBase.getValidFieldName(), 1);
        }
        if (id != null) {
            queryWrapper.eq(NoticeBase.getIdFieldName(), id);
        }
        if (code != null) {
            String key = dbStringUtil.getString(code);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeBase.getCodeFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeBase.getCodeFieldName(), key);
            }
        }
        if (name != null) {
            String key = dbStringUtil.getString(name);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply(NoticeBase.getNameFieldName() + " like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like(NoticeBase.getNameFieldName(), key);
            }
        }
        Page<NoticeProtocol> page1 = super.page(page, queryWrapper);
        return page;
    }
}
