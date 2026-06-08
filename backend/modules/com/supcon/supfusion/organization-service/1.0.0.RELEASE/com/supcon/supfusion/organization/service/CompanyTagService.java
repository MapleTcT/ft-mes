package com.supcon.supfusion.organization.service;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.organization.dao.po.company.CompanyTagPO;
import com.supcon.supfusion.organization.service.bo.company.CompanyTagBO;

import java.util.List;

public interface CompanyTagService {

    /**
     * 新增公司标签

     */
    void addCompanyTag(List<CompanyTagPO> list);

    /**
     * 删除标签
     * @param CompanyId
     */
    void deleteCompanyTag(Long CompanyId);
    /**
     * 查询公司标签
     * @param keyword
     * @return
     */
    List<CompanyTagBO> getCompanyTags(String keyword);

    /**
     * 获取公司标签
     * @param id 公司id
     * @return
     */
    List<String> getCompanyTagById(Long id);
}
