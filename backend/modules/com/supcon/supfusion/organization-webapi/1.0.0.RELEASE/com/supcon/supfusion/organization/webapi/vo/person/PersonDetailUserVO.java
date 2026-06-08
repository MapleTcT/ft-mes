
package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.person.MainPositionBO;
import com.supcon.supfusion.organization.service.bo.person.RelationDepartmentBO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

/**
 * 人员查询详情
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonDetailUserVO extends VO {
    /**
     * 用户id
     */
    @ApiModelProperty(value = "用户id")
    private Long id;

    /**
     * 人员编码
     */
    @ApiModelProperty(value = "人员编号")
    private String code;

    /**
     * 人员名称
     */
    @ApiModelProperty(value = "用户名")
    private String name;

    /**
     * 手机号
     */
    @ApiModelProperty(value = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @ApiModelProperty(value = "邮箱")
    private String email;

    /**
     * 人员状态
     */
    @ApiModelProperty(value = "状态")
    private String status;

    /**
     * 描述
     */
    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "删除标识，是否有效")
    private Boolean valid;


    /**
     * 部门路径
     */
    private List<RelationDepartmentBO> departmentFullPath;

    /**
     * 岗位路径
     */
    private List<MainPositionBO> positionFullPath;

    @ApiModelProperty(value = "部门信息")
    private List<RelationDepartmentBO> department;

    /**
     * 关联的岗位信息
     */
    @ApiModelProperty(value = "岗位信息")
    private List<MainPositionBO> position;

    /**
     * 性别编码值name
     */
    private String gender;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 人员id
     */
    private Long personId;

    private Long directLeaderId;

    private String directLeaderName;

    private Long grandLeaderId;

    private String grandLeaderName;

}
