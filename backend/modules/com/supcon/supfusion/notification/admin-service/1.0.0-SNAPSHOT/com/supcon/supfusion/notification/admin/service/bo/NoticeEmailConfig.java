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
public class NoticeEmailConfig {
    //smtp服务器
    private String smtpHost;
    //端口
    private String sslPort;
    //发送者邮箱
    private String senderEmail;
    //邮箱登陆令牌
    private String outhToken;
    //邮箱登陆令牌
    private Boolean enableSSL;
    //重试时间
    private String retryInterval;
    //重试次数
    private Integer retryTime;

}
