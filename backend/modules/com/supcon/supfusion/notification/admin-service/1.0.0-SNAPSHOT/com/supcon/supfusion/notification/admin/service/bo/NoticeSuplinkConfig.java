package com.supcon.supfusion.notification.admin.service.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
/**
 * suplink配置
 *
 * @create 2020/8/7 09:59
 */
@Getter
@Setter
@ToString
public class NoticeSuplinkConfig {
    private String appId;

    private String secret;

    private String host;

    private String port;
}
