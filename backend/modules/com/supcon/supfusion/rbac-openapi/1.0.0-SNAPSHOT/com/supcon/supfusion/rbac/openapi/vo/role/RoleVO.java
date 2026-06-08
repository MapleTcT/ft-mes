package com.supcon.supfusion.rbac.openapi.vo.role;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

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
@ApiModel(description= "角色新增类")
public class RoleVO extends VO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 上级节点ID
     */
    @ApiModelProperty(value = "上级节点ID")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String,Object> parent;

    /**
     * 角色类型
     */
    @ApiModelProperty(value = "角色类型")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String roleType;

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
     * 删除的标签IDs
     */
    @ApiModelProperty(value = "删除的标签IDs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Long> deleteIds;

    /**
     * 标签
     */
    @ApiModelProperty(value = "标签")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> tags;


}
