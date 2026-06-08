package com.supcon.supfusion.signature.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.signature.dao.entity.WfDeployment;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @author zhang yafei
 */
@Mapper
@Repository
public interface WfDeploymentMapper extends BaseMapper<WfDeployment> {
}
