package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.po.AppCompanyRefPO;

import java.util.List;
import java.util.Set;

public interface IAppCompanyRefService extends IService<AppCompanyRefPO> {

    void addAppCompanyRef(List<AppCompanyRefPO> appCompanyRefPOList);

    List<AppCompanyRefPO> queryAppCompanyRefList(String appId);

    void deleteAppCompanyRef(String appId);

    /**
     *  根据appid查询app_company关联表数据
     */
    Set<String> findAppComRefByAppId(Set<String> appIds);
}
