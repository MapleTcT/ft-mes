package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.DataGroup;
import com.supcon.supfusion.configuration.services.openapi.vo.DataGroupVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DataGroupWrapper extends BaseLocalWrapper<DataGroup, DataGroupVO> {

    @Override
    public DataGroupVO e2v(DataGroup entity) {
        return BeanUtil.copy(entity, DataGroupVO.class);
    }

    @Override
    public DataGroup v2e(DataGroupVO vo) {
        return BeanUtil.copy(vo, DataGroup.class);
    }
}
