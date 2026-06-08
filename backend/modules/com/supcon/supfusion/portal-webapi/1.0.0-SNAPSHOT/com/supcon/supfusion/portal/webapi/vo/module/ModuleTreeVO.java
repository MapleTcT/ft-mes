package com.supcon.supfusion.portal.webapi.vo.module;

import lombok.Data;

import java.util.List;

/**
 * @Author kk.C
 * @Description: 模块树状返回类
 * @Date 2020/10/19 13:57
 */
@Data
public class ModuleTreeVO {

    private String artifact;
    private String category;
    /**
     * 树状实体子集
     **/
    List<EntityVO> children;
    /**
     * 主键
     **/
    private String code;
    /**
     * 是否固有基础类型
     **/
    private boolean isInherentedBase;
    /**
     * 是否有子集
     **/
    private boolean isParent;
    private boolean isPublish;
    private String name;
    private String nameInternational;
    private boolean open;
    private int version;
    private String iconSkin;

}
