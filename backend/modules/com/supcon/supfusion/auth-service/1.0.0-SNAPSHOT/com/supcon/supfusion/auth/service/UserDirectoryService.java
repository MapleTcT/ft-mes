package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.UserDirectoryBO;
import com.supcon.supfusion.auth.service.bo.UserDirectoryQueryBO;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;

import java.util.List;
import java.util.Set;

/**
 * 用户目录服务
 *
 * @author caokele
 */
public interface UserDirectoryService {

    /**
     * 创建用户目录
     *
     * @param userDirectoryBO 用户目录实体
     */
    UserDirectoryBO createUserDirectory(UserDirectoryBO userDirectoryBO);

    /**
     * 更新用户目录
     *
     * @param userDirectoryBO 用户目录实体
     */
    UserDirectoryBO updateUserDirectory(UserDirectoryBO userDirectoryBO);

    /**
     * 删除用户目录
     *
     * @param ids 用户目录id列表
     */
    void removeUserDirectories(List<Long> ids);

    /**
     * 启用/禁用用户目录
     *
     * @param id      用户目录id
     * @param enabled 是否启用
     */
    void enableUserDirectory(Long id, Boolean enabled);

    /**
     * 更新用户目录排序
     *
     * @param id        用户目录id
     * @param direction 排序方向 0:向上 1:向下
     */
    void sortUserDirectory(Long id, Integer direction);

    /**
     * 测试用户目录连接V1接口
     *
     * @param userDirectoryBO 用户目录实体
     */
    void connectUserDirectory(UserDirectoryBO userDirectoryBO);

    /**
     * 导出用户目录
     *
     * @param ids 用户目录id列表
     */
    void exportUserDirectories(List<Long> ids);

    /**
     * 查询用户目录列表
     *
     * @param queryParams 用户目录查询参数
     * @param pagination  分页参数
     */
    PageResult<UserDirectoryBO> queryUserDirectories(UserDirectoryQueryBO queryParams, Pagination pagination);

    /**
     * 认证LDAP用户
     *
     * @param id       用户目录id
     * @param userName 用户名
     * @param password 密码
     */
    void authenticateUserDirectory(String userName, String password);

    /**
     * 根据ID获取用户目录
     *
     * @param id 用户目录id
     */
    UserDirectoryBO queryUserDirectory(Long id);

    /**
     * 查询筛选字段
     *
     * @param fieldName  字段名称
     * @param fieldValue 字段值
     */
    Set<String> queryUserDirectoryFieldValues(String fieldName, String fieldValue);

}
