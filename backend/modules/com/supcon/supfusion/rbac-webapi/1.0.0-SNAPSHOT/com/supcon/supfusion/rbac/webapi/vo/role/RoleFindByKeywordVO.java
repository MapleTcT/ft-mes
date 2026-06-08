package com.supcon.supfusion.rbac.webapi.vo.role;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.rbac.webapi.vo.tag.TagVO;
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
@ApiModel(description= "根据关键字查询角色返回类")
public class RoleFindByKeywordVO extends VO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;


    /**
     * 角色类型
     */
    @ApiModelProperty(value = "角色类型")
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

    @ApiModelProperty(value = "标签信息")
    private List<TagVO> tags;
}
