package com.supcon.supfusion.rbac.urlscan.bean;

import lombok.Data;

@Data
public class MenuOperateBean {

    private static final long serialVersionUID=1L;

    /**
     * 菜单操作编码
     */
    private String menuoperateCode;

    /**
     * 对应URL
     */
    private String url;

    /**
     * 应用名
     */
    private String appId;

    /**
     * 是否自定义操作
     */
    private String isCustom;

    /**
     * 是否需要正则匹配
     */
    private Boolean regMatch;

    /**
     * 请求方法，0 GET,1 POST,2 PUT,3 DELETE
     */
    private Integer methodType;
}
