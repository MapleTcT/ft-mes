package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.po.AppRefPO;
import com.supcon.supfusion.rbac.dao.po.CustomPermissionPO;

import java.util.List;

/**
 * <p>
 * 自定义权限表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public interface IAppRefService extends IService<AppRefPO> {

    List<Long> findAppRefMenuId(List<Long> menuInfoIds);


    List<Long> queryMenuIdListByAppId(String appId);
}
