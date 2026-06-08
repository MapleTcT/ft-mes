package com.supcon.supfusion.rbac.webapi.vo.MenuOperateCodeUrlRef;

import lombok.Data;

/**
 * <p>
 * 菜单操作编码URL关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-22
 */
@Data
public class MenuOperateCodeUrlRefVO {


    /**
     * 主键ID
     */
    private Long id;


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
     * 是否需要正则匹配
     */
    private Boolean regMatch;

    /**
     * 导入形式
     */
    private Integer importType;
}
