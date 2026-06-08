package com.supcon.supfusion.organization.service.bo.person;

import lombok.*;

import java.util.List;

/**
 * 人员PO详情类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonDetailBO {
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
     * oldId
     */
    private String oldId;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 人员状态
     */
    private String status;

    /**
     * 描述
     */
    private String description;

    /**
     * 部门路径
     */
    private List<RelationDepartmentBO> departmentFullPath;

    /**
     * 岗位路径
     */
    private List<MainPositionBO> positionFullPath;

    /**
     * 关联的部门信息
     */
    private List<RelationDepartmentBO> department;

    /**
     * 关联的岗位信息
     */
    private List<MainPositionBO> position;

    /**
     * 性别编码值name
     */
    private String gender;

    /**
     * 主岗id
     */
    private Long mainPosition;

    /**
     * 涉密等级的编码值name
     */
    private String classifiedLevel;

    /**
     * 用户名
     */
    private String userName;

    private Boolean valid;

    /**
     * 用户id
     */
    private Long userId;

    private Long directLeaderId;

    private String directLeaderName;

    private Long grandLeaderId;

    private String grandLeaderName;

    /**
     * 人员头像地址
     */
    private String avatarUrl;

    /**
     * 签名地址
     */
    private String signPicUrl;

    private String entryDate;

    private String title;

    private String qualification;

    private String education;

    private String major;

    private String idNumber;
}
