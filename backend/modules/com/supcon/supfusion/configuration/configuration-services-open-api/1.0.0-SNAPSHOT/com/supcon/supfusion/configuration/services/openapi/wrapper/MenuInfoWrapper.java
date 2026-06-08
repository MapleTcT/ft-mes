package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.configuration.services.openapi.vo.MenuInfoVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MenuInfoWrapper extends BaseLocalWrapper<MenuInfo, MenuInfoVO> {
    @Override
    public MenuInfoVO e2v(MenuInfo entity) {
        return BeanUtil.copy(entity, MenuInfoVO.class);
    }

    @Override
    public MenuInfo v2e(MenuInfoVO vo) {
        return BeanUtil.copy(vo, MenuInfo.class);
    }
}
