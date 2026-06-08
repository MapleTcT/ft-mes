package com.supcon.supfusion.notification.admin.dao.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolTmpl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 协议基础模板表 Mapper 接口
 * </p>
 *
 * @author panzk
 * @since 2020-05-11
 */
@Mapper
@Repository("adminNoticeProtocolTmplMapper")
public interface NoticeProtocolTmplMapper extends BaseMapper<NoticeProtocolTmpl> {
}
