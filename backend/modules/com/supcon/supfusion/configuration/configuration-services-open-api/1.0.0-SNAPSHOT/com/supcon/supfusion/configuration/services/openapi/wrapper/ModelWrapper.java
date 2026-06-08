package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.openapi.vo.ModelVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ModelWrapper extends BaseLocalWrapper<Model, ModelVO> {
    @Override
    public ModelVO e2v(Model entity) {
        return BeanUtil.copy(entity, ModelVO.class);
    }

    @Override
    public Model v2e(ModelVO vo) {
        return BeanUtil.copy(vo, Model.class);
    }
}
