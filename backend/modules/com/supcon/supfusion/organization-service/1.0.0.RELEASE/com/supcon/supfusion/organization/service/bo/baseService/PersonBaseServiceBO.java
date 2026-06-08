package com.supcon.supfusion.organization.service.bo.baseService;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 人员PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonBaseServiceBO {

    /**
     * 人员id
     */
    @JSONField(name = "id")
    private Long id;

    /**
     * 人员编码
     */
    @JSONField(name = "code")
    private String code;

    /**
     * 人员名称
     */
    @JSONField(name = "name")
    private String name;

    /**
     * 旧版人员name
     */
    @JSONField(serialize=false)
    private String oldId;

    /**
     * 性别的编码值name
     */
    @JSONField(name = "sex")
    private String gender;

    /**
     * 主岗id
     */
    @JSONField(name = "mainPositionId")
    private Long mainPosition;

    /**
     * 公司id
     */
    @JSONField(serialize=false)
    private Long companyId;

    /**
     * 人员状态的编码值name
     */
    @JSONField(name = "workStatus")
    private String status;

    /**
     * 是否有效０:无效，１:有效
     */
    @JSONField(name = "valid")
    private Integer valid = 1;

    /**
     * 涉密等级的编码值name
     */
    @JSONField(name = "securityClass")
    private String classifiedLevel;

    /**
     * 手机号码
     */
    @JSONField(name = "mobile")
    private String phone;

    /**
     * 邮箱
     */
    @JSONField(name = "email")
    private String email;

    /**
     * 描述
     */
    @JSONField(name = "memo")
    private String description;

    /**
     * 是否同时创建用户
     */
    @JSONField(serialize=false)
    private Boolean createUser = false;

    /**
     * 人员头像地址
     */
    @JSONField(name = "avatarUrl")
    private String avatarUrl;

    /**
     * 人员签名地址
     */
    @JSONField(name = "signPicUrl")
    private String signPicUrl;

}
