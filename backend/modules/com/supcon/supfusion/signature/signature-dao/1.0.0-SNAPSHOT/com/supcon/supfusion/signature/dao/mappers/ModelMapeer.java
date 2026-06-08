package com.supcon.supfusion.signature.dao.mappers;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.signature.dao.dto.ModelTableDto;
import com.supcon.supfusion.signature.dao.entity.Model;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * @author zhang yafei
 */
@Mapper
@Repository
public interface ModelMapeer extends BaseMapper<Model> {

    List<ModelTableDto> getModelTableByTableInfoId(
            @Param("dealInfoTableName") String dealInfoTableName,
            @Param("columnName") String columnName,
            @Param("tableInfoId") Long tableInfoId);

    List<ModelTableDto> getModelTableByDealInfoId(
            @Param("dealInfoTable")String dealInfoTable,
            @Param("tableInfoId")Long tableInfoId);
}
