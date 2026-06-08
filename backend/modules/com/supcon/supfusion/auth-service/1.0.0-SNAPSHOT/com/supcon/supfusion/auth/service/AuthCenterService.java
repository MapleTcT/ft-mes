package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.AuthCenterBO;
import com.supcon.supfusion.auth.service.bo.IdentityProviderQueryBO;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;


/**
 * 身份提供者服务
 *
 * @author caokele
 */
public interface AuthCenterService {

    /**
     * 查询认证提供者列表
     *
     * @param queryParams 认证提供者查询参数
     * @param pagination  分页参数
     */
    PageResult<AuthCenterBO> queryAuthCenters(IdentityProviderQueryBO queryParams, Pagination pagination);
}
