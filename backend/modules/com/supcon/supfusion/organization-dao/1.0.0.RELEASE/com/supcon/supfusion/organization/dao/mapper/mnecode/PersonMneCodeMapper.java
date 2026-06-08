package com.supcon.supfusion.organization.dao.mapper.mnecode;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.organization.dao.po.mnecode.PersonMnecodePO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 人员助记码
 */
public interface PersonMneCodeMapper extends BaseMapper<PersonMnecodePO> {

    /**
     * 批量新增人员助记码
     *
     * @param list
     */
    void insertBatch(@Param("list") List<PersonMnecodePO> list, @Param("dbType") String dbType);

    Integer isChineseMne(@Param("dbType") String dbType);
}
