package com.supcon.supfusion.i18n.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * @Description: * token表 自动上传同步自己应用服务国际化资源到国际化微服务端时控制用实体
 * * 表名 supfusion_i18n_token
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = I18nTokenPO.TABLE_NAME, autoResultMap = true)
public class I18nTokenPO extends BaseEntity {
	
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "supfusion_i18n_token";

    //主键
    private Long id;
    //应用工程名
    private String moduleCode;
    //是否持有锁
    private String hasLock;
    //token
    private String token;
    //是否有效
    private String valid;

}
