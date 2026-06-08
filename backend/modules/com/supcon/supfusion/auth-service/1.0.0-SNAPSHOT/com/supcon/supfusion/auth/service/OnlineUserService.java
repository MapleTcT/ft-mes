package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.OnlineUserBO;
import com.supcon.supfusion.auth.service.bo.OnlineUserQueryBO;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;

import java.util.List;

/**
 * 在线用户服务
 *
 * @author caokele
 */
public interface OnlineUserService {

    /**
     * 创建在线用户
     *
     * @param onlineUserBO 在线用户实体
     */
    OnlineUserBO createOnlineUser(OnlineUserBO onlineUserBO);

    /**
     * 查询在线用户
     *
     * @param queryParams 在线用户查询参数
     * @param pagination  分页参数
     */
    PageResult<OnlineUserBO> queryOnlineUsers(OnlineUserQueryBO queryParams, Pagination pagination);

    /**
     * 注销在线用户
     *
     * @param ids 在线用户id列表
     */
    void logoutOnlineUsers(List<Long> ids);

    /**
     * 根据ticket 手动退出在线用户记录  logout or kick
     *
     * @param ticket 会话凭证
     * @param tenantId
     */
    void removeOnlineUserByTicketActByManual(String ticket, String tenantId);

    /**
     * 根据ticket删除在线用户记录
     *
     * @param ticket 会话凭证
     * @param tenantId 会话凭证
     */
    void removeSessionByTicketWithKeyCloak(String ticket, String tenantId);

    /**
     * 更新在线用户当前公司
     *
     * @param ticket    会话凭证
     * @param companyId 当前公司ID
     */
    void updateOnlineUserCompany(String ticket, Long companyId);


    /**
     * 更新在线用户当前公司
     *
     * @param ticket    会话凭证
     * @param companyId 当前公司ID
     */
    void updateOnlineUserCompanyId(String ticket, Long companyId, String accessToken);

    /**
     * 根据用户id注销在线用户
     *
     * @param userIds 用户id列表
     */
    void logoutOnlineUsersByUserIds(List<Long> userIds);

    Integer getTotalOnline(String deviceType);

    void deleteOnline(String deviceType);

    List<OnlineUserBO> queryAllOnlinUsers();

    void deleteOnlineUserByIds(List<Long> ids);

}
