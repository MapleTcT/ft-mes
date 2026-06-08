/**
 * Licensed to the Deep Blue SUPCON
 * @author: zhuangmh
 * @date: 2020年5月19日 上午10:41:37
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年5月19日 上午10:41:37
 */
@Data
public class UserDTO {
    
    public UserDTO(String userName) {
        this.userName = userName;
    }
    
    /**
     * 用户名称 -- 查询展示都是该字段
     */
    private String userName;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 人员信息
     */
    private StaffDTO staff;
    
}
