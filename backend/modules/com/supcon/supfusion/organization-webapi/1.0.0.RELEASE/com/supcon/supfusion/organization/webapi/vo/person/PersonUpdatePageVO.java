package com.supcon.supfusion.organization.webapi.vo.person;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.baseService.BaseInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonPositionBO;
import com.supcon.supfusion.organization.service.bo.person.PersonRoleBO;
import com.supcon.supfusion.organization.webapi.vo.baseService.BaseInfoVO;
import lombok.*;

import java.util.List;

/**
 * 人员修改弹窗的加载数据
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonUpdatePageVO extends VO {

    /**
     * 人员id
     */
    private Long id;

    /**
     * 人员编号
     */
    private String code;

    /**
     * 人员姓名
     */
    private String name;


    /**
     * 当前朱岗位
     */
    private Long mainPosition;
    /**
     * 当前性别
     */
    private String gender;

    /**
     * 当前状态
     */
    private String status;

    /**
     * 当前涉密等级
     */
    private String classifedLevel;

    /**
     * 性别
     */
    //List<SystemCodeBaseBO> genders;

    /**
     * 关联的岗位列表
     */
    List<PersonPositionBO> positions;

    /**
     * 状态
     */
    //List<SystemCodeBaseBO> statuses;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否在创建人员时创建了用户
     */
    private Boolean createUser;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户描述
     */
    private String userDescription;

    /**
     * 角色
     */
    private List<PersonRoleBO> roles;


    private BaseInfoBO directLeader;

    private BaseInfoBO grandLeader;

    /**
     * 人员头像地址
     */
    private String avatarUrl;

    /**
     * 人员签名地址
     */
    private String signPicUrl;

    private String entryDate;

    private String title;

    private String qualification;

    private String education;

    private String major;

    private String idNumber;
}
