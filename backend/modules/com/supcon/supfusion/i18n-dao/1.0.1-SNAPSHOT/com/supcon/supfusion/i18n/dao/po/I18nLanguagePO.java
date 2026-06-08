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
 * @Description:    * 国际化key对应的语言类型表实体
 *                  * 表名 supfusion_i18n_language
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = I18nLanguagePO.TABLE_NAME, autoResultMap = true)
public class I18nLanguagePO extends BaseEntity {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public static final String TABLE_NAME = "supfusion_i18n_language";


    private Long id;
    //语言code码 eg:zn_CH
    private String languCode;
    //语言类型(中文描述)
    private String languType;
    //语言类型(code自己对应的语言描述)
    private String languName;
    //是否启用 0 不使用 1使用
    private String hasUsed;
    //是否删除
    private String valid;
    // 租户ID
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

}
