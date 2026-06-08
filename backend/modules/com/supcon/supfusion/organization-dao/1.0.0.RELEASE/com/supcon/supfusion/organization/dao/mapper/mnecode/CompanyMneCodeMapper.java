package com.supcon.supfusion.organization.dao.mapper.mnecode;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.organization.dao.po.mnecode.CompanyMnecodePO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公司助记码
 */
public interface CompanyMneCodeMapper extends BaseMapper<CompanyMnecodePO> {
    /**
     * 批量新增公司助记码
     *
     * @param list
     * @param dbType
     */
    void insertBatch(@Param("list") List<CompanyMnecodePO> list, @Param("dbType") String dbType);

    Integer isChineseMne(@Param("dbType") String dbType);
}
