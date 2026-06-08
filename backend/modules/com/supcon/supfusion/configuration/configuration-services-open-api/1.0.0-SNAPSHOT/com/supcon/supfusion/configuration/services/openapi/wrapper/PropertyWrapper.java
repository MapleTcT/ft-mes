package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.openapi.vo.PropertyVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PropertyWrapper extends BaseLocalWrapper<Property, PropertyVO> {
    @Override
    public PropertyVO e2v(Property entity) {
        return BeanUtil.copy(entity,PropertyVO.class);
    }

    @Override
    public Property v2e(PropertyVO vo) {
        return BeanUtil.copy(vo, Property.class);
    }
}
