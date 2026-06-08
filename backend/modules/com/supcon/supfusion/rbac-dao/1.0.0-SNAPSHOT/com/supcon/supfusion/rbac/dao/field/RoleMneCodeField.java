package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 角色助记码
 * </p>
 *
 * @author 袁阳
 * @since 2020-10-15
 */
public class RoleMneCodeField{


    /**
     * 主键ID
     */
    public static String id = "ID";

    /**
     * 版本
     */
    public static String version="row_version";

    /**
     * 角色ID
     */
    public static String roleId="ROLE";

    /**
     * 助记码
     */
    public static String mneCode="MNE_CODE";

}
