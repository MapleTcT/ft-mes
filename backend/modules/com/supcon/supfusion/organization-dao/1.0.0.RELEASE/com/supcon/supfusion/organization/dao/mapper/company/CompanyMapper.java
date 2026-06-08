package com.supcon.supfusion.organization.dao.mapper.company;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyDetailInfoPO;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description: 公司mapper
 * @Author:     HUNING
 * @CreateDate: 2020/5/25
 */
public interface CompanyMapper extends BaseMapper<CompanyPO> {

    /**
     * 查询公司列表
     * @param modifyTime
     * @param current
     * @param pageSize
     * @return
     */
    List<CompanyDetailInfoPO> getCompanies(@Param("modifyTime") String modifyTime, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    Integer getCompanyCount(@Param("modifyTime") String modifyTime, @Param("dbType") String dbType);


    CompanyDetailInfoPO getCompanyByCode(@Param("companyCode") String companyCode);

    List<CompanyDetailInfoPO> getSubCompaniesByCode(@Param("fullPath") String fullPath, @Param("keyword") String keyword, @Param("layNo") Integer layNo, @Param("isMultistage") Boolean isMultistage, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    Integer getSubCompaniesTotal(@Param("fullPath") String fullPath, @Param("keyword") String keyword, @Param("layNo") Integer layNo, @Param("isMultistage") Boolean isMultistage);

}
