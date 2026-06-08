package com.supcon.supfusion.portal.service.bo;

import com.supcon.supfusion.portal.service.entity.MenuInfo;
import com.supcon.supfusion.portal.service.entity.Module;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author kk.C
 * @Description: 门户编码BO类
 * @Date 2020/10/21 9:45
 */
@Data
public class EcPortletBO implements Serializable {

    private static final long serialVersionUID = 6732470617486163409L;

    private String code;
    private int cid;//公司ID
    private String moduleCode;
    private int menuInfoId;//菜单ID
    private String menuCode;//关联菜单code
    private String height;//高度,iframeFlag为true时有效
    private boolean hidden;//是否隐藏
    private boolean iframeFlag;//是否适用iframe
    private boolean isDefault;
    private String memo;//备注
    private MenuInfo menuInfo; //关联菜单
    private Module module;//模块编码 冗余字段
    private boolean powerFlag;//是否启用权限
    private int scopeNum;//所属范围   0所有公司   1本公司
    private String title;
    private String titleInternational;
    private String titleKey;
    private String titleKeyInternational;
    private String url;
    private String resizeFunc;
    private String onloadFunc;
}
