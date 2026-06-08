package com.supcon.supfusion.auth.webapi.vo;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonDetailVO extends VO {
    /**
     * 人员id
     */
    private Long id;

    /**
     * 人员编码
     */
    private String code;

    /**
     * 人员名称
     */
    private String name;


    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态
     */
    private String status;

    /**
     * 描述
     */
    private String description;

    /**
     * 部门路径
     */
    private String departmentFullPath;

    /**
     * 岗位路径
     */
    private String positionFullPath;

    /**
     * 性别
     */
    private String gender;

    /**
     * 关键字
     */
    private String key;

    /**
     * 用户名
     */
    private String userName;

    private Boolean valid;

    /**
     * 用户id
     */
    private Long userId;
}

