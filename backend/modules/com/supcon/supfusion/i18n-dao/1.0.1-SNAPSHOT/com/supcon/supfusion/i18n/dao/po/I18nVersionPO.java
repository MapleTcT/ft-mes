package com.supcon.supfusion.i18n.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;



/**
 *
 * @Description:  * 不同应用服务的国际化资源版本号表实体
 *                * 表名 supfusion_i18n_version
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = I18nVersionPO.TABLE_NAME, autoResultMap = true)
public class I18nVersionPO extends BaseEntity {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "supfusion_i18n_version";

    //主键
    private Long id;
    //模块名code
    private String moduleCode;
    //应用国际化资源版本号
    private String moduleVersionCode;
    //是否删除 0 不适用 1使用
    private String valid;

}
