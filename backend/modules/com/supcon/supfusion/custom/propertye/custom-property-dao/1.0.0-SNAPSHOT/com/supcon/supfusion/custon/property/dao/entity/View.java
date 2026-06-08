package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.custon.property.common.enums.DialogType;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.common.enums.ShowType;
import com.supcon.supfusion.custon.property.common.enums.ViewType;
import com.supcon.supfusion.custon.property.common.i18n.*;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

import java.util.Date;

/**
 * @author songjiawei
 */
@Data
@TableName(value = "ec_view", autoResultMap = true)
public class View extends LogicBasePO {

    private static final long serialVersionUID = 7119612377276341004L;
    public static final String TABLE_NAME = "EC_VIEW";
    public static final String MOBILE_VIEW_SUFFIX = "__mobile__";
    public static final String MS_SERVICE = "msService";

    @TableId
    private String code;

    private EcEnv ecEnv = EcEnv.product;

    private String name;

    @JsonSerialize(using = DispalyNameInternationalSerialzer.class)
    private String displayName;

    @JsonSerialize(using = TitleInternationalSerialzer.class)
    private String title;

    private ViewType type;


    private Boolean usedForWorkFlow = false; // 是否主列表视图

    private Boolean mainView = false;// 是否主查看视图

    private Boolean mainRef = false;// 是否主参照视图

    private String description;

    private Boolean retrialFlag;// 是否支持弃审--只有启用工作流的查看视图才有弃审

    private String scriptCode;// 弃审对应的脚本

    private String url;

    private Boolean customFlag;

    private DialogType dialogType;

    private Integer height;

    private Integer width;

    private ShowType showType;// 显示方式:片段\单个\布局

    private String layoutCode;

    private Boolean closePageAfterSave = false;// 保存单据后，是否关闭页面

    private Boolean isControl = false;// 是否受控

    private Boolean isAudit = false;// 允许查看日志

    private Boolean hasAttachment = false;// 是否有附件

    private Boolean dealInfoShow = false;// 基础页面是否显示处理意见

    private String dealInfoGroup; // 处理意见分类

    private Boolean usedForTree = false;

    private Boolean includeChildren = false;
    private Boolean isReference;

    private String assTreeModelCode;

    private String assTreeLayRec;

    private String assTreePath;

    private Integer dataGridType;//
    public static final int DATA_GRID_TYPE_NORMAL = 0;
    public static final int DATA_GRID_TYPE_EX = 1;

    private Boolean isShadow = false;

    private Boolean isSign = false;

    private Boolean isHandSign = false;

    private Boolean isPrint = false;// 允许打印

    private Boolean isPermission = false;// 启用

    private Boolean controlPrint = false;

    @JsonSerialize(using = ControlInternationalSerialzer.class)
    private String controlName;// 控件打印操作名称

    private String controlCode;// 操作CODE后缀

    @JsonSerialize(using = ControlSetingInternationalSerialzer.class)
    private String controlSetingName;// 打印控件设置名称

    private String moduleCode;

    private String permissionCode;

    private String operateUrl;

    @JsonSerialize(using = RefOperateInternationalSerialzer.class)
    private String refOperateName;// 参照视图权限名称

    private Boolean onlyForQuery = false; // 仅用于查询，而不用于仅查待办

    private Boolean mobile = false;// 移动视图标志

    private Boolean mobileEnableFlag = false;// 移动视图启用标志

    private Boolean isBatchControlPrint;// 是否允许批量控件打印

    private Boolean enableSimpleDealInfo;// 是否显示简单样式的处理意见

    private Boolean importFlag; // 导入导出配置标志 0用于导出 1用于导入

    private Boolean hasCustomSection = false; // 视图是否包含自定义字段区域

    private Boolean attachmentFlag = Boolean.FALSE; //附件标识，只有查看视图支持
    private Integer editViewType = 0; // 0表示编辑、查看视图普通类型，1表示编辑、查看视图增强型类型

    private Boolean projFlag;

    private Integer inheritType;// 继承类型 半继承 1 全继承 2

    private Date publishTime;

    @JsonSerialize(using = MenuInternationalSerialzer.class)
    private String menuName;

    private Long parentMenuId;

    private Boolean projEnabled;// 工程视图是否启用

    private String parentMenuCode;

    private String openType;

    private String assViewCode;

    private String batchControlPrintViewCode;

    private String extraQueryJson;

    private String shadowViewCode;

    private String referenceViewCode;

    private String entityCode;

    private String assModelCode;

    @TableField(value = "extra_view")
    private String extraViewCode;

    private String fastQueryJsonCode;

    public static String fl(String s) {
        if (null != s) {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
        return null;
    }



}
