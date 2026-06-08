package com.supcon.supfusion.signature.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.signature.base.enums.SignatureColumn;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author zhang yafei
 */
@Mapper
@Repository
public interface SignatureLogMapper extends BaseMapper<SignatureLog> {

    List<SignatureLog> getSignaureLogsBylike(@Param("likeCondition") Map<SignatureColumn, List<String>> likeCondition,
                                             @Param("inCondition") Map<SignatureColumn, List<String>> inCondition,
                                             @Param("timeCondition") Map<SignatureColumn, List<String>> timeCondition,
                                             @Param("offset") long offset,
                                             @Param("limit") long limit);

    Integer signaureLogsBylikeCount(@Param("likeCondition") Map<SignatureColumn, List<String>> likeCondition,
                                    @Param("inCondition") Map<SignatureColumn, List<String>> inCondition,
                                    @Param("timeCondition") Map<SignatureColumn, List<String>> timeCondition);
}
