package com.supcon.supfusion.i18n.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @Description: 每个模块的国际化资源索引表实体 表名 supfusion_i18n_index
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = I18nIndexPO.TABLE_NAME, autoResultMap = true)
public class I18nIndexPO extends BaseEntity {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "supfusion_i18n_index";

    //主键
    private Long id;
    //应用工程名
    private String moduleCode;
    //国际化资源索引
    private String moduleIndexCode;
    //是否删除  0不使用 1 使用
    private String valid;
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

}
