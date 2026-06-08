package com.supcon.supfusion.organization.openapi.vo.person;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 人员关联的岗位
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonPositionVO extends VO {

//    /**
//     * 岗位id
//     */
//    @ApiModelProperty(value = "岗位id")
//    private Long id;
    /**
     * 岗位名称
     */
    @ApiModelProperty(value = "岗位名称")
    private String name;

    /**
     * 部门名称
     */
    @ApiModelProperty(value = "部门名称")
    private String deptName;

    /**
     * 岗位编码
     */
    @ApiModelProperty(value = "岗位编码")
    private String code;

    /**
     * 是否主岗
     */
    @ApiModelProperty(value = "是否主岗")
    private Boolean mainPosition = false;

    /**
     * 岗位全路径
     */
    @ApiModelProperty(value = "岗位全路径")
    private String fullPath;
}
