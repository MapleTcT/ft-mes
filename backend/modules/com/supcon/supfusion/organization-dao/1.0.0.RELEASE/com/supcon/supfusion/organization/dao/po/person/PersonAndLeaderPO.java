package com.supcon.supfusion.organization.dao.po.person;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonAndLeaderPO extends BaseEntity {

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
     * 旧版人员name
     */
    private String oldId;

    /**
     * 性别的编码值name
     */
    private String gender;

    /**
     * 主岗id
     */
    private Long mainPosition;

    /**
     * 公司id
     */
    /*@TableField(jdbcType = JdbcType.BIGINT)
    private Long companyId;*/

    /**
     * 人员状态的编码值name
     */
    private String status;

    /**
     * 是否有效０:无效，１:有效
     */
    private Boolean valid = true;

    /**
     * 涉密等级的编码值name
     */
    private String classifiedLevel;

    /**
     * 手机号码
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
     * 是否同时创建用户
     */
    private Boolean createUser = false;

    private Boolean sysFlag = false;

    private Long directLeaderId;

    private String directLeaderName;

    private Long grandLeaderId;

    private String grandLeaderName;

    private Long userId;

    private String userName;

    private String avatarUrl;

    private String signPicUrl;

    private String entryDate;

    private String title;

    private String qualification;

    private String education;

    private String major;

    private String idNumber;
}
