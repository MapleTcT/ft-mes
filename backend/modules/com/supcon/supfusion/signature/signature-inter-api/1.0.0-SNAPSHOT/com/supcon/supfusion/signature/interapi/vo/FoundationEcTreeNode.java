package com.supcon.supfusion.signature.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class FoundationEcTreeNode extends VO {

    private static final long serialVersionUID = -2148787281134904613L;

    @ApiModelProperty("节点code")
    private String code;
    @ApiModelProperty("节点名字")
    private String name;
    @ApiModelProperty("是否为父节点")
    private Boolean isParent = false;

}
