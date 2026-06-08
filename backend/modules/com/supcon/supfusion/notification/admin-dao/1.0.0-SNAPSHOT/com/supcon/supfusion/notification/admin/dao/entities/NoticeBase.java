package com.supcon.supfusion.notification.admin.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 14:48
 */
@Getter
@Setter
@ToString
public class NoticeBase extends BaseEntity implements Serializable {

    @TableId(value = "id")
    private Long id;
    //编码
    @TableField(value = "code")
    private String code;
    //名称
    @TableField(value = "name")
    private String name;
    //版本
    @TableField(value = "version")
    private Integer version;
    //排序
    @TableField(value = "sort_value")
    private Integer sort;
    //描述
    @TableField(value = "description")
    private String memo;
    //是否删除
    @TableField(value = "valid")
    private Boolean valid;

    //数据是否允许修改
    @TableField(value = "modify_sign")
    private Boolean modify_sign;
    //数据来源
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
    public static String getVersionFieldName() {
        return "version";
    }
    public static String getSortFieldName() {
        return "sort";
    }
    public static String getMemoFieldName() {
        return "description";
    }
    public static String getValidFieldName() {
        return "valid";
    }
    public static String getMSginFieldName() {
        return "modify_sign";
    }
    public static String getSourceFieldName() {
        return "source";
    }

}
