package com.supcon.supfusion.rbac.dao.bo;

import lombok.Data;


@Data
public class MenuOperatePermissionBO {

    private Long miId;

    private Double sort;

    private Integer layNo;

    private Long parentId;

    private String fullPath;

    private String menuInfoName;

    private Long id;

    private String action;

    private String code;

    private Long deploymentId;

    private String flowVersion;

    private String iconCls;

    private Boolean enableDeptrict;

    private Boolean enableAssignDept;

    private String memo;

    private String menuOperateType;

    private Integer msgAssembled;

    private String name;

    private String namespace;

    private String target;

    private String url;

    private Boolean valid;

    private Integer version;

    private Long cid;

    private Long menuinfoId;

    private Boolean powerFlag;

    private Boolean enableAssignpos;

    private Boolean enableAssignstaff;

    private Boolean enableGrouprestrict;

    private Boolean enableNorestrict;

    private Boolean enablePosrestrict;

    private Boolean enableDealerpermission;

    private Boolean ignorePermission;

    private Boolean enableCustomPermission;

    private Boolean enableDataPermission;

    private String viewCode;

    private Boolean isHidden;

    private Boolean defaultOperate;

    private String nameDisplay;
}

