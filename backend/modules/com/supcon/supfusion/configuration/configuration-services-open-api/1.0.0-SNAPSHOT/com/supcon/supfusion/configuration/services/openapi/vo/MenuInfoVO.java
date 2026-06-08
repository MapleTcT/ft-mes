package com.supcon.supfusion.configuration.services.openapi.vo;

import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.enums.OperateTarget;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@ApiModel(value = "MenuInfoVO", description = "MenuInfoVO")
public class MenuInfoVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private int version;
    private String code; // 编码
    private String name; // 名称
    private String memo; // 备注
    private SystemCode securityClass;//密级 三元模式下才启用
    private String url;// 程序URL
    private String namespace;
    private String action;
    private Double sort = (double) 0; // 排序
    private String module;// 模块信息：bundle的SymbolicName组成
    private String cssClass;
    private Boolean systemDefault;// 是否系统默认
    private String moduleCode;// 模型id
    private String ecEntityCode;
    private String entityCode;
    private OperateTarget target;
    private Boolean groupOnly;// 是否系统默认
    private Boolean isHide = false;// 是否隐藏
    private Boolean threeRole = false;
    private Set<MenuOperate> menuOperates = new HashSet<MenuOperate>();
    private Integer menuType;
    private Integer hiddenType;
    private String fromSystem;
    /**
     * S2字段开始
     */
    private Integer stType = 0;
    private Integer stTabtypeid = 0;
    private String stIntro;
    private String stDigitalsignature;
    private SystemCode stFlag = new SystemCode("SYSTEM/BAP"); // SYSTEM/BAP---bap菜单/SYSTEM/S2---S2菜单
    /**
     * 绝对隐藏  true时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏
     */
    private Boolean absoluteHidden = false;
    /**
     * PIMS字段
     */
    private Integer pimsMenuType; //0:文件夹，1:本地流程图，2:远程流程图，3:第三方页面，4:平台页面，255：操作
    private Boolean isFullUrl = false;
    private String defaultMenuId;
    private String iconUrl; //PIMS菜单图片路径
    private Integer remoteId;
    // 新增第三方页面集成请求方式,add by zjf20150610
    private Integer requestType;// 1:直接，2：cas,3:post,4:get
    private String remoteUserNameNamed;//远程映射用户名字段名 exp：user_id:zjf 中user_id,add by zjf20150610
    private String remotePasswordNamed;//远程映射密码字段名,add by zjf20150610
    private Integer showType;//请求方式0:内嵌，1：弹出式
    private Boolean leaf = false;
    private MenuInfo parent;
    private Long parentId;
    private Long cid;
    private String layRec;
    private String fullPathName;
    List<MenuInfo> children = new ArrayList<>();
    private Integer layNo = 0;


}
