package com.supcon.supfusion.auth.service.bo.bap;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * bap用户
 *
 * @author caokele
 */
@Data
@Accessors(chain = true)
public class BapUserBO {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 版本号
     */
    private Integer version = 0;
    /**
     * 用户名
     */
    private String name;
    /**
     * 密码
     */
    private String password;
    /**
     * 关联人员
     */
    private BapStaffBO staff;
    /**
     * 用户状态 0：正常 1：系统自动锁定 2:管理员进行锁定
     */
    private Integer locked;
    /**
     * 是否有效
     */
    private Boolean valid;
    /**
     * 时区
     */
    private String timeZone;
}
