package com.supcon.supfusion.portal.webapi.vo.module;

import lombok.Data;

/**
 * @Author kk.C
 * @Description: 实体子集VO类
 * @Date 2020/10/19 14:06
 */
@Data
public class EntityVO {

    /**
     * 主键
     **/
    private String code;
    /**
     * 是否固有基础类型
     **/
    private boolean isInherentedBase;
    private boolean isPublish;
    private boolean isReadOnly;
    private String name;
    private String nameInternational;
    private int version;
    private String iconSkin;
}
