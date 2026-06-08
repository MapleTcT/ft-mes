package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.po.UserUrlRefPO;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * 用户与请求URL关联表 服务类
 * </p>
 */
public interface IUserUrlRefService extends IService<UserUrlRefPO> {

    void addUserUrlRefList(List<Long> cid);

    void addUserUrlRefList(List<Long> cid, String tenantId);
    Map<String, Map<String,List<String>>> addUserUrlRefListForUserFlow(List<Long> userIds, Long cid, String tenantId);
    void refreshRedis(List<String> apps);
}
