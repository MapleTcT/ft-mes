package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.SpecialPermission;
import com.supcon.supfusion.configuration.services.openapi.vo.SpecialPermissionVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SpecialPermissionWrapper extends BaseLocalWrapper<SpecialPermission, SpecialPermissionVO> {
    @Override
    public SpecialPermissionVO e2v(SpecialPermission entity) {
        return BeanUtil.copy(entity, SpecialPermissionVO.class);
    }

    @Override
    public SpecialPermission v2e(SpecialPermissionVO vo) {
        return BeanUtil.copy(vo, SpecialPermission.class);
    }
}
