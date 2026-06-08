package com.supcon.supfusion.signature.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author user
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ViewTreeVo extends VO {

    private static final long serialVersionUID = 2273846087110838776L;

    private Integer id;

    @ApiModelProperty("菜单树层级")
    private Integer level;

    @ApiModelProperty("父节点code")
    private String code;


}
