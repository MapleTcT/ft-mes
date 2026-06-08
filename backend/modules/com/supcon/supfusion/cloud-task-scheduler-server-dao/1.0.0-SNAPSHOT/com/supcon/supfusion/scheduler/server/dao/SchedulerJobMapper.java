package com.supcon.supfusion.scheduler.server.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobPo;

/**
 * 调度任务 Mapper 接口
 *
 * @author 刘旺
 */

public interface SchedulerJobMapper extends BaseMapper<SchedulerJobPo> {


//    List<SchedulerJobPo> selectUserWrapper(RowBounds rowBounds, @Param("ew") Wrapper<SchedulerJobPo> wrapper);
}
