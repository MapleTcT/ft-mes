package com.supcon.supfusion.organization.dao.mapper.mnecode;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.organization.dao.po.mnecode.PositionMnecodePO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位助记码
 */
public interface PositionMneCodeMapper extends BaseMapper<PositionMnecodePO> {
    /**
     * 批量新增岗位助记码
     *
     * @param list
     */
    void insertBatch(@Param("list") List<PositionMnecodePO> list, @Param("dbType") String dbType);

    Integer isChineseMne(@Param("dbType") String dbType);
}
