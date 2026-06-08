package com.supcon.supfusion.rbac.dao.po;

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
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_role_mnecode", autoResultMap=true)
public class RoleMneCodePO extends PO {


    private static final long serialVersionUID = 2732071779724268864L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 版本
     */
    @TableField("row_version")
    private String version;

    /**
     * 角色ID
     */
    @TableField(value = "ROLE")
    private Long roleId;

    /**
     * 助记码
     */
    @TableField("MNE_CODE")
    private String mneCode;

}
