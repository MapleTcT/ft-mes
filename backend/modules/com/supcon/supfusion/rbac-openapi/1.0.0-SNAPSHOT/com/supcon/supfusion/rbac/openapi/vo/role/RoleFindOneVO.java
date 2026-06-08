package com.supcon.supfusion.rbac.openapi.vo.role;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.rbac.openapi.vo.tag.TagVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

/**
 * <p>
 * 角色表
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
@ApiModel(description= "角色根据编码查询返回类")
public class RoleFindOneVO extends VO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    //@ApiModelProperty(value = "主键ID")
    //private Long id;

    /**
     * 角色类型
     */
    //@ApiModelProperty(value = "角色类型")
    //private String roleType;

    /**
     * 描述
    */
    @ApiModelProperty(value = "描述")
    private String description;

    /**
     * 名称
     */
    @ApiModelProperty(value = "名称")
    private String name;

    /**
     * 编码
     */
    @ApiModelProperty(value = "编码")
    private String code;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    private String modifyTime;
}
