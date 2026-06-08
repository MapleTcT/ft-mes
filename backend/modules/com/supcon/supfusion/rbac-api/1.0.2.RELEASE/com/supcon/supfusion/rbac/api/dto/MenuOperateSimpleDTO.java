package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuOperateSimpleDTO {


    /**
     * 菜单ID
     */
    private Long menuinfoId;


    /**
     * 地址
     */
    private String url;


    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 默认操作标识，默认操作不可删除
     */
    private Boolean defaultOperate;

}
