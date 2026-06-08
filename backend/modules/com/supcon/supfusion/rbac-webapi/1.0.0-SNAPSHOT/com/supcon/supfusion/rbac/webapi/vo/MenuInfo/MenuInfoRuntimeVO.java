package com.supcon.supfusion.rbac.webapi.vo.MenuInfo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MenuInfoRuntimeVO extends VO {

    private static final long serialVersionUID = -4565679045720176762L;

    /**
     * 名称
     */
    private String name;

    /**
     * 主键ID
     */
    private String resourceId;

    private String resourceCode;

    private Double resourceOrder;

    private String parentId;

    private String parentCode;

    private String resourceType;

    private String resourceFunctionType;

    private String url;

    private List<MenuInfoRuntimeVO> childResources;
}
