package com.supcon.supfusion.organization.dao.po.person;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

/**
 * 人员PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PersonAddPO.TABLE_NAME, autoResultMap = true)
public class PersonAddPO extends BaseEntity {

    public static final String TABLE_NAME = "org_person";

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
    @TableField(jdbcType = JdbcType.BIGINT)
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
    @TableField(value = "phone", updateStrategy = FieldStrategy.NOT_NULL)
    private String phone;

    /**
     * 邮箱
     */
    @TableField(value = "email", updateStrategy = FieldStrategy.NOT_NULL)
    private String email;

    /**
     * 描述
     */
    @TableField(value = "description", updateStrategy = FieldStrategy.NOT_NULL)
    private String description;

    /**
     * 是否同时创建用户
     */
    private Boolean createUser = false;

    private Boolean sysFlag = false;

    @TableField(value = "direct_leader_id", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.BIGINT)
    private Long directLeaderId;

    @TableField(value = "grand_leader_id", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.BIGINT)
    private Long grandLeaderId;

    private Long userId;

    private String userName;

    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String avatarUrl;

    @TableField(value = "sign_pic_url", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String signPicUrl;

    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String entryDate;

    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String title;

    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String qualification;

    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String education;

    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String major;

    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String idNumber;

}
