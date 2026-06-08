package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.FastQueryJson;
import com.supcon.supfusion.configuration.services.openapi.vo.FastQueryJsonVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FastQueryJsonWrapper extends BaseLocalWrapper<FastQueryJson, FastQueryJsonVO> {
    @Override
    public FastQueryJsonVO e2v(FastQueryJson entity) {
        return BeanUtil.copy(entity, FastQueryJsonVO.class);
    }

    @Override
    public FastQueryJson v2e(FastQueryJsonVO vo) {
        return BeanUtil.copy(vo, FastQueryJson.class);
    }
}
