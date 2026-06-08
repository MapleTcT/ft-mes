package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.SchedulerDatasource;
import com.supcon.supfusion.configuration.services.openapi.vo.SchedulerDatasourceVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SchedulerDatasourceWrapper extends BaseLocalWrapper<SchedulerDatasource, SchedulerDatasourceVO> {
    @Override
    public SchedulerDatasourceVO e2v(SchedulerDatasource entity) {
        return BeanUtil.copy(entity, SchedulerDatasourceVO.class);
    }

    @Override
    public SchedulerDatasource v2e(SchedulerDatasourceVO vo) {
        return BeanUtil.copy(vo, SchedulerDatasource.class);
    }
}
