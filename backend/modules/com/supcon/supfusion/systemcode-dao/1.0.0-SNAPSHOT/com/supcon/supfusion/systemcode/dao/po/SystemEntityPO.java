package com.supcon.supfusion.systemcode.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@TableName(value = SystemEntityPO.TABLE_NAME, autoResultMap = true)
public class SystemEntityPO extends BaseEntity {
    private static final long serialVersionUID = -7734723118217343867L;

    public static final String TABLE_NAME = "sys_entity";

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 数据字典编码
     */
    @TableField(value = "code")
    private String code;

    /**
     * 数据字典名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 显示名称
     */
    @TableField(value = "display_name")
    private String displayName;

    /**
     * 版本
     */
    @TableField(value = "row_version")
    private Long rowVersion;

    /**
     * 类型
     */
    @TableField(value = "type")
    private String type;

    /**
     * 所属公司ID
     */
    @TableField(value = "cid")
    private Long cid;

    /**
     * 所属公司名称
     */
    @TableField(exist = false)
    private String companyName;

    /**
     * 模块ID
     */
    @TableField(value = "module_id")
    private String moduleId;

    /**
     * 所属模块名称
     */
    @TableField(exist = false)
    private String moduleName;

    /**
     * 是否有效
     */
    @TableField(value = "valid")
    private Integer valid;

    /**
     * 是否多选
     */
    @TableField(value = "multi_flag")
    private Integer multiFlag;

    /**
     * 是否系统默认
     */
    @TableField(value = "sys_default")
    private Integer sysDefault;

    /**
     * 备注
     */
    @TableField(value = "memo")
    private String memo;

    /**
     * 来源 ：supOS , supIDE
     */
    @TableField(value = "source")
    private String source;

    public static String getIdFieldName() {
        return "id";
    }

    public static String getCodeFieldName() {
        return "code";
    }

    public static String getNameFieldName() {
        return "name";
    }

    public static String getRowVersionFieldName() {
        return "row_version";
    }

    public static String getTypeFieldName() {
        return "type";
    }

    public static String getCidFieldName() {
        return "cid";
    }

    public static String getModuleIdFieldName() {
        return "module_id";
    }

    public static String getValidFieldName() {
        return "valid";
    }

    public static String getCreateTimeFieldName() {
        return "create_time";
    }

    public static String getSourceName(){return "source";}

}
