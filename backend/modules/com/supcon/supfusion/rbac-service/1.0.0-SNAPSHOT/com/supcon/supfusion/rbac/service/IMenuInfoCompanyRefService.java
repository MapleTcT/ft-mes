package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.rbac.dao.po.MenuInfoCompanyRefPO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;

/**
 * <p>
 * 菜单公司关联表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-30
 */
public interface IMenuInfoCompanyRefService extends IService<MenuInfoCompanyRefPO> {

    IPage<MenuInfoCompanyRefPO> findByPage(Long menuInfoId, String keyword, Integer current, Integer pageSize);
}
