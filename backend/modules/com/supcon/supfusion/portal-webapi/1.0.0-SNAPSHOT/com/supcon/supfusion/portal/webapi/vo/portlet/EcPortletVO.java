package com.supcon.supfusion.portal.webapi.vo.portlet;

import com.supcon.supfusion.portal.service.entity.MenuInfo;
import com.supcon.supfusion.portal.service.entity.Module;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * @Author kk.C
 * @Description: 门户模板返回类
 * @Date 2020/10/21 9:45
 */
@Data
public class EcPortletVO implements Serializable {

    private static final long serialVersionUID = 6732470617486163409L;

    @NotBlank(message = "编码必填")
    @Pattern(regexp = "^[a-zA-Z][A-Za-z0-9]{0,25}$", message = "仅支持以字母开头且长度不大于25的字母与数字")
    private String code;
    private int cid;//公司ID
    private String height;//高度,iframeFlag为true时有效
    private String moduleCode;//模板编码
    private boolean hidden;//是否隐藏
    private boolean iframeFlag;//是否适用iframe
    private boolean isDefault;
    private String memo;//备注
    private String menuInfoId;//关联菜单ID
    private MenuInfo menuInfo; //关联菜单
    private Module module;
    private boolean powerFlag;//是否启用权限
    private int scopeNum;//所属范围   0所有公司   1本公司
    private String title;
    private String titleInternational;
    private String titleKey;
    private String titleKeyInternational;
    private String language;//国际化语言类型
    private String url;
    private String resizeFunc;
    private String onloadFunc;
}
