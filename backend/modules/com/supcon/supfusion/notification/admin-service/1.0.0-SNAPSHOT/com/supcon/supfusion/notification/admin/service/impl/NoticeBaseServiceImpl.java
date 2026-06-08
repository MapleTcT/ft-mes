package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeBase;
import com.supcon.supfusion.notification.admin.service.NoticeBaseService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;


/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 19:36
 */
public class NoticeBaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements NoticeBaseService<T> {
    @Override
    public List<T> queryListCondition(Map<String, Object> customCondition) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<T>();
        for (Map.Entry<String, Object> entry : customCondition.entrySet()) {
            queryWrapper.eq(entry.getKey(), entry.getValue());
        }
        List<T> result = super.list(queryWrapper);
        return result;
    }

    @Override
    public T queryEntity(Long id) {
        if (id != null) {
            T entity = super.getOne(new QueryWrapper<T>().eq(NoticeBase.getIdFieldName(), id));
            return entity;
        } else {
            return null;
        }
    }

    /***
     *
     * @param entity
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public T addEntity(T entity) {
        if (super.save(entity)) {
            return entity;
        } else {
            return null;
        }
    }

    /***
     *
     * @param entity
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public T updateEntity(T entity) {
        if (super.updateById(entity)) {
            return entity;
        }
        return null;
    }

    /****
     *
     * @param code
     * @param name
     * @param id
     * @param page
     * @return
     */
    @Override
    public Page<T> queryPageList(String code, String name, Long id, Page<T> page, String dbType, DbStringUtil dbStringUtil, boolean valid) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<T>();
        if (valid) {
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
        Page<T> page1 = super.page(page, queryWrapper);
        // page1.setTotal(super.count());
        return page;
    }

    @Override
    public List<T> queryList(String code, String name, Long id, String dbType, DbStringUtil dbStringUtil, boolean valid) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<T>();
        if (valid) {
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
        List<T> result = super.list(queryWrapper);
        return result;
    }


    /****
     *
     * @param id
     * @param code
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public String delEntity(Long id, String code) {

        if (id != null) {
            return this.delEntityById(id);
        }
        if (StringUtils.hasText(code)) {
            return this.delEntityByCode(code);
        }
        return null;
    }

    /***
     *多条id删除对象
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public String delEntity(String ids) {
        String result = "";
        if (StringUtils.hasText(ids)) {
            String[] idList = ids.split(",");
            int size = idList.length;
            for (int i = 0; i < size; i++) {
                result += this.delEntityById(Long.valueOf(idList[i]));
            }
        }
        return result;
    }

    @Override
    public Boolean delByCondition(QueryWrapper<T> queryWrapper) {
        return super.remove(queryWrapper);
    }
/***************************************************************************************************/
    /***
     * 具体数据库操作
     * @param id
     * @return
     */
    private String delEntityById(Long id) {
        super.removeById(id);
        return null;
    }

    private String delEntityByCode(String code) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<T>();
        queryWrapper.eq(NoticeBase.getCodeFieldName(), code);
        super.remove(queryWrapper);
        return null;
    }

}
