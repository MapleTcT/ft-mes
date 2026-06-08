package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 基础信息
 * </p>
 *
 * @author 袁阳
 * @since 2020-07-13
 */
@Data
@TableName(value = "rbac_init_verison_info", autoResultMap=true)
public class InitVersionInfoPO implements Serializable {

    private static final long serialVersionUID = -3719711853876379114L;
    /**
     * 主键ID
     */
    @TableId("ID")
    private Long id;

    /**
     * 初始化版本号
     */
    @TableId("INIT_VERSION")
    private Integer initVersion;

    /**
     * 服务名
     */
    @TableId("APP")
    private String app;

}
