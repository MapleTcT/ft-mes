package com.supcon.supfusion.rbac.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoMneCodePO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 菜单表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface IMenuInfoMneCodeService extends IService<MenuInfoMneCodePO> {

    /**
     * 创建菜单助记码
     * @param property 菜单名 国际化key
     * @param language
     * @param menuInfoId
     */
    void createMenuInfoMneCodeI18NKey(String property,Long menuInfoId);

    /**
     * 创建菜单助记码
     * @param property 菜单名 中文
     * @param language
     * @param menuInfoId
     */
    void createMenuInfoMneCode(String property,Long menuInfoId);

    void createMenuInfoMneCodeI18NKey(List<MenuInfoPO> menuInfoPOS);
}
