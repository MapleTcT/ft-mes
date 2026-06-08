package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.configuration.services.openapi.vo.MenuOperateVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MenuOperateWrapper extends BaseLocalWrapper<MenuOperate, MenuOperateVO> {
    @Override
    public MenuOperateVO e2v(MenuOperate entity) {
        return BeanUtil.copy(entity, MenuOperateVO.class);
    }

    @Override
    public MenuOperate v2e(MenuOperateVO vo) {
        return BeanUtil.copy(vo, MenuOperate.class);
    }
}
