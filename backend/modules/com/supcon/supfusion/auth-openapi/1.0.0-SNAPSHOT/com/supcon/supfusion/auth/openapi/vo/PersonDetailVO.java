package com.supcon.supfusion.auth.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "人员详情")
public class PersonDetailVO extends VO {
    //    /**
//     * 人员id
//     */
//    private Long id;

    /**
     * 人员编码
     */
    @ApiModelProperty(value = "人员编码", example = "zhangsan")
    private String code;

    /**
     * 人员名称
     */
    @ApiModelProperty(value = "人员名称", example = "张三")
    private String name;


    /**
     * 手机号
     */
    @ApiModelProperty(value = "手机号", example = "157616320354")
    private String phone;

    /**
     * 邮箱
     */
    @ApiModelProperty(value = "邮箱", example = "15761632038@163.com")
    private String email;


    /**
     * 描述
     */
    @ApiModelProperty(value = "描述", example = "这是描述")
    private String description;

//    /**
//     * 部门路径
//     */
//    @ApiModelProperty(value = "部门路径", example = "default/develop")
//    private String departmentFullPath;
//
//    /**
//     * 岗位路径
//     */
//    @ApiModelProperty(value = "岗位路径", example = "dev/develop")
//    private String positionFullPath;

    /**
     * 性别
     */
    @ApiModelProperty(value = "性别")
    private SystemCodeVO gender;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private SystemCodeVO status;

    /**
     * 关键字
     */
    @ApiModelProperty(value = "关键字")
    private String key;

    /**
     * 用户名
     */
    @ApiModelProperty(value = "用户名", example = "admin")
    private String userName;

//    private Boolean valid;

//    /**
//     * 用户id
//     */
//    private Long userId;

}
