package com.supcon.supfusion.portal.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = EcPortletPO.TABLE_NAME)
public class EcPortletPO extends PO {

    public static final String TABLE_NAME = "ec_portlet";

    @TableId
    private String code;
    private int cid;//公司ID
    private String moduleCode;
    private Long menuInfoId;//菜单ID
    private String menuCode;//关联菜单code
    private String height;//高度,iframeFlag为true时有效
    private boolean iframeFlag;//是否适用iframe
    private boolean isDefault;
    private String memo;//备注
    private Boolean powerFlag;//是否启用权限
    private int scopeNum;//所属范围   0所有公司   1本公司
    private String title;
    private String titleKey;
    private String url;
    private String resizeFunc;
    private String onloadFunc;
}
