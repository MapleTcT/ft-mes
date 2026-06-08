package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * 在线用户查询参数
 *
 * @author caokele
 */
@Data
public class OnlineUserQueryBO extends OnlineUserBO {
    /**
     * 登陆时间-开始
     */
    private String startLoginTime;
    /**
     * 登陆时间-结束
     */
    private String endLoginTime;
}
