package com.supcon.supfusion.notification.admin.service.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 邮件配置
 *
 * @author huangxin2
 * @create 2020/5/7 14:48
 */
@Getter
@Setter
@ToString
public class NoticeDingtalkConfig {
    //钉钉服务发放AK
    private String appKey;
    //钉钉服务发放SK
    private String appSecret;
    //钉钉服务发放应用ID
    private String agentId;
    //应用名称
    private String appName;
    //备注
    private String memo;

}
