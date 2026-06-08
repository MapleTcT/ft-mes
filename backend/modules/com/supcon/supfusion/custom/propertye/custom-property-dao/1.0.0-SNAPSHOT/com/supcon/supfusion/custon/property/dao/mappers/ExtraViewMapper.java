package com.supcon.supfusion.custon.property.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.custon.property.dao.entity.ExtraView;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @author zhang yafei
 */
@Mapper
@Repository
public interface ExtraViewMapper extends BaseMapper<ExtraView> {
    ExtraView selectByShadowCode(String shadowViewCode);
}
