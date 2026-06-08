package com.supcon.supfusion.organization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyTagMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyTagPO;
import com.supcon.supfusion.organization.service.CompanyTagService;
import com.supcon.supfusion.organization.service.bo.company.CompanyTagBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 公司标签实现类
 */
@Slf4j
@Service
public class CompanyTagServiceImpl extends ServiceImpl<CompanyTagMapper, CompanyTagPO> implements CompanyTagService {

    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;
    /**
     * 公司标签管理新增
     */
    @Override
    public void addCompanyTag(List<CompanyTagPO> list) {
        list.stream().forEach(tag -> {
            QueryWrapper<CompanyTagPO> queryWrapper = new QueryWrapper<CompanyTagPO>();
            queryWrapper.eq("tag_type", tag.getType());
            queryWrapper.eq("name", tag.getName());
            saveOrUpdate(tag, queryWrapper);
        });
    }

    @Override
    public void deleteCompanyTag(Long companyId) {
        QueryWrapper<CompanyTagPO> queryWrapper = new QueryWrapper<CompanyTagPO>();
        queryWrapper.eq("company_id", companyId);
        remove(queryWrapper);
    }

    /**
     * 查询公司标签
     * @param keyword
     * @return
     */
    @Override
    public List<CompanyTagBO> getCompanyTags(String keyword) {
        QueryWrapper<CompanyTagPO> queryWrapper = new QueryWrapper<CompanyTagPO>();
        if (keyword != null && !"".equals(keyword.trim())) {
            String key = dbStringUtil.getString(keyword);
            //获取数据库类型
            String dbType = dataId.getDataId();
            //使用queryWrapper形式
            //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
            if ("oracle".equals(dbType)){
                queryWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
            }else{
                queryWrapper.like("name",key);
            }
        }
        List<CompanyTagPO> list = list(queryWrapper);

        List<CompanyTagBO> results = new ArrayList<CompanyTagBO>();
        if (list!= null) {
            list.stream().forEach(tag -> {
                CompanyTagBO companyTagBO = new CompanyTagBO();
                BeanUtils.copyProperties(tag, companyTagBO);
                results.add(companyTagBO);
            });
        }
        return results;
    }

    /**
     * 获取公司tag
     * @param id 公司id
     * @return
     */
    @Override
    public List<String> getCompanyTagById(Long id) {
        QueryWrapper<CompanyTagPO> queryWrapper = new QueryWrapper<CompanyTagPO>();
        queryWrapper.eq("company_id", id);
        List<CompanyTagPO> list = list(queryWrapper);
        List<String> tags = new ArrayList<String>();
        if (list == null) {
            return tags;
        }
        list.stream().forEach(tag -> {
            tags.add(tag.getName());
        });
        return tags;
    }
}
