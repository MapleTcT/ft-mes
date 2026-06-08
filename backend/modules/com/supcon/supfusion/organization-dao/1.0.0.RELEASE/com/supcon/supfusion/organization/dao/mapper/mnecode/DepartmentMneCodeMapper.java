package com.supcon.supfusion.organization.dao.mapper.mnecode;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.organization.dao.po.mnecode.DepartmentMnecodePO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门助记码
 */
public interface DepartmentMneCodeMapper extends BaseMapper<DepartmentMnecodePO> {

    /**
     * 批量新增部门助记码
     *
     * @param list
     */
    void insertBatch(@Param("list") List<DepartmentMnecodePO> list, @Param("dbType") String dbType);

    Integer isChineseMne(@Param("dbType") String dbType);
}
