package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.po.MenuInfoMneCodePO;
import com.supcon.supfusion.rbac.dao.po.RoleMneCodePO;
import com.supcon.supfusion.rbac.dao.po.RolePO;

import java.util.List;

/**
 * <p>
 * 角色助记码
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface IRoleMneCodeService extends IService<RoleMneCodePO> {


    /**
     * 创建角色助记码
     * @param rolePOS
     */
    void createRoleMneCode(List<RolePO> rolePOS);

    void deleteMneCode(Long roleId);

    void deleteMneCodeByIds(List<Long> roleIds);
}
