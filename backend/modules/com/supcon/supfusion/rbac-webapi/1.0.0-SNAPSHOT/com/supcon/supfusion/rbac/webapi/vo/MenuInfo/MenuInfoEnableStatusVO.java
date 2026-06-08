package com.supcon.supfusion.rbac.webapi.vo.MenuInfo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

/**
 * <p>
 * 菜单表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description= "启停用菜单接收类")
public class MenuInfoEnableStatusVO extends VO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "菜单ID")
    private Long id;

    @ApiModelProperty(value = "当前启停状态")
    private Boolean enable;

}
