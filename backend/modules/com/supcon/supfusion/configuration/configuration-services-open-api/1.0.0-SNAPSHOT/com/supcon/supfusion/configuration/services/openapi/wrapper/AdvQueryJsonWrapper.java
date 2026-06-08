package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.AdvQueryJson;
import com.supcon.supfusion.configuration.services.openapi.vo.AdvQueryJsonVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AdvQueryJsonWrapper extends BaseLocalWrapper<AdvQueryJson, AdvQueryJsonVO> {
    @Override
    public AdvQueryJsonVO e2v(AdvQueryJson entity) {
        return BeanUtil.copy(entity,AdvQueryJsonVO.class);
    }

    @Override
    public AdvQueryJson v2e(AdvQueryJsonVO vo) {
        return BeanUtil.copy(vo,AdvQueryJson.class);
    }
}
