package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.openapi.vo.ViewVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ViewWrapper extends BaseLocalWrapper<View, ViewVO> {
    @Override
    public ViewVO e2v(View entity) {
        return BeanUtil.copy(entity, ViewVO.class);
    }

    @Override
    public View v2e(ViewVO vo) {
        return BeanUtil.copy(vo,View.class);
    }
}
