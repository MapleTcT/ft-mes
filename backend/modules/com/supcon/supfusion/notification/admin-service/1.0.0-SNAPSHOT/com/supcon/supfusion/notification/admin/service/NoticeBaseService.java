package com.supcon.supfusion.notification.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;

import java.util.List;
import java.util.Map;

/**
 * 基础方法接口
 *
 * @author huangxin2
 * @create 2020/5/7 18:50
 */
public interface NoticeBaseService<T> extends IService<T> {
    /***
     * 根据ID获取实体
     * @param id
     * @return
     */
    public T queryEntity(Long id);

    /***
     * 条件查询实体列表
     * @param customCondition
     * @return
     */
    public List<T> queryListCondition(Map<String, Object> customCondition);

    /***
     * 通用查询条件查询实体列表
     * @param code
     * @param name
     * @param id
     * @return
     */
    public List<T> queryList(String code, String name, Long id, String dbType, DbStringUtil dbStringUtil, boolean valid);

    /***
     * 新增
     * @param entity
     * @return
     */
    public T addEntity(T entity);

    /***
     * 修改
     * @param entity
     * @return
     */
    public T updateEntity(T entity);

    /**
     * 分页查询
     *
     * @param code
     * @param name
     * @param id
     * @param page
     * @return
     */
    Page<T> queryPageList(String code, String name, Long id, Page<T> page, String dbType, DbStringUtil dbStringUtil, boolean valid);

    /***
     * 删除单个实体
     * @param id
     * @param code
     * @return
     */
    public String delEntity(Long id, String code);

    /***
     * 删除多个实体
     * @param ids
     * @return
     */
    public String delEntity(String ids);

    public Boolean delByCondition(QueryWrapper<T> queryWrapper);

}
