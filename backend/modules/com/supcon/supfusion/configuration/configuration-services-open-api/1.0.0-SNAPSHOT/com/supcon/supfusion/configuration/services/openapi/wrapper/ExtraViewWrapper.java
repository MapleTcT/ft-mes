package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.ExtraView;
import com.supcon.supfusion.configuration.services.openapi.vo.ExtraViewVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExtraViewWrapper extends BaseLocalWrapper<ExtraView, ExtraViewVO> {
    @Override
    public ExtraViewVO e2v(ExtraView entity) {
        return BeanUtil.copy(entity,ExtraViewVO.class);
    }

    @Override
    public ExtraView v2e(ExtraViewVO vo) {
        return BeanUtil.copy(vo,ExtraView.class);
    }
}
