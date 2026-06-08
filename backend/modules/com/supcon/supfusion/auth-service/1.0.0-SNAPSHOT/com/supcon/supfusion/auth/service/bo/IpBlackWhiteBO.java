package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * ip黑白名单
 *
 * @author caokele
 */
@Data
public class IpBlackWhiteBO {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 企业Id
     */
    private Long companyId;
    /**
     * 访问IP
     */
    private String ip;
    /**
     * 管控模式 0:黑名单 1:白名单
     */
    private Integer controlType;
    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 当前登录ip
     */
    private String currentIp;
}
