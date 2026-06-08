package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.OAuthClientBO;
import com.supcon.supfusion.auth.service.bo.OAuthClientQueryBO;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;

import java.util.List;

/**
 * 认证客户端服务
 *
 * @author caokele
 */
public interface OAuthClientService {

    /**
     * 根据ID获取认证客户端
     */
    OAuthClientBO queryOAuthClient(Long id);

    /**
     * 新增认证客户端
     *
     * @param oAuthClientBO 认证客户端实体
     */
    OAuthClientBO createOAuthClient(OAuthClientBO oAuthClientBO);

    /**
     * 更新认证客户端
     *
     * @param oAuthClientBO 认证客户端实体
     */
    OAuthClientBO updateOAuthClient(OAuthClientBO oAuthClientBO);

    /**
     * 删除认证客户端
     *
     * @param ids id列表
     */
    void removeOAuthClients(List<Long> ids);

    /**
     * 启用/禁用认证客户端
     *
     * @param id      认证客户端id
     * @param enabled 是否启用
     */
    void enableOAuthClient(Long id, Boolean enabled);

    /**
     * 查询认证客户端列表
     *
     * @param queryParams 认证客户端查询参数
     * @param pagination  分页参数
     */
    PageResult<OAuthClientBO> queryOAuthClients(OAuthClientQueryBO queryParams, Pagination pagination);
}
