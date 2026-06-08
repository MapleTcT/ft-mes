package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 标签表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-10
 */
public class TagField extends LogicDeleteBaseEntityField{


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本号
     */
    public static String version="VERSION";

    /**
     * 标签类型
     */
    public static String type="TYPE";

    /**
     * 标签名
     */
    public static String name="NAME";

    /**
     * 公司ID
     */
    public static String cid="CID";

    /**
     * 关联ID
     */
    @TableField("OBJECTID")
    public static String objectid="OBJECTID";


}
