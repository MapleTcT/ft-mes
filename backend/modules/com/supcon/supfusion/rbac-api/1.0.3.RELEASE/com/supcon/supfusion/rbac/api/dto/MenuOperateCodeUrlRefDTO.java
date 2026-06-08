package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
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
public class MenuOperateCodeUrlRefDTO extends DTO {


    private static final long serialVersionUID = 499836689596808172L;
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
