package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

@Data
public class MenuOperateUrlJsonDTO {


    private Long id;

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
    private String app;

    /**
     * 请求方法，0 GET,1 POST,2 PUT,3 DELETE
     */
    private Integer methodType;

    /**
     * 是否自定义操作
     */
    private Integer isCustom;
}
