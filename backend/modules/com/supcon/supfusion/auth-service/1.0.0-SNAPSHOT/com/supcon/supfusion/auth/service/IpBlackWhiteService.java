package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.IpBlackWhiteBO;
import com.supcon.supfusion.auth.service.bo.IpBlackWhiteQueryBO;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;

import java.util.List;

/**
 * IP黑白名单服务
 *
 * @author caokele
 */
public interface IpBlackWhiteService {

    /**
     * 创建IP黑白名单
     *
     * @param ipBlackWhiteBO ip黑白名单实体
     * @param addCurrentIp   是否添加当前登录ip
     */
    IpBlackWhiteBO createIpBlackWhite(IpBlackWhiteBO ipBlackWhiteBO, Boolean addCurrentIp);

    /**
     * 删除IP黑白名单
     *
     * @param ids       IP黑白名单id列表
     * @param currentIp 当前ip
     */
    void removeIpBlackWhiteList(List<Long> ids, String currentIp);

    /**
     * 查询IP黑白名单列表
     *
     * @param queryParams ip黑白名单查询参数
     * @param pagination  分页参数
     */
    PageResult<IpBlackWhiteBO> queryIpBlackWhiteList(IpBlackWhiteQueryBO queryParams, Pagination pagination);

    /**
     * 校验ip
     *
     * @param ip        访问ip
     * @param companyId 企业ID
     */
    boolean verifyIp(String ip, Long companyId);

    /**
     * 检查IP黑白名单操作是否需要提示
     *
     * @param ipBlackWhiteBO ip黑白名单实体
     * @param operateType    操作类型 0:新增，1:删除
     */
    boolean checkOperateNeedTip(IpBlackWhiteBO ipBlackWhiteBO, Integer operateType);
}
